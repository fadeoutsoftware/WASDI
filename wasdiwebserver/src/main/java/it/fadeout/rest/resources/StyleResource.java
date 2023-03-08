package it.fadeout.rest.resources;

import static wasdi.shared.business.UserApplicationPermission.ADMIN_DASHBOARD;
import static wasdi.shared.utils.WasdiFileUtils.createDirectoryIfDoesNotExist;
import static wasdi.shared.utils.WasdiFileUtils.writeFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;

import it.fadeout.Wasdi;
import it.fadeout.mercurius.business.Message;
import it.fadeout.mercurius.client.MercuriusAPI;
import it.fadeout.rest.resources.largeFileDownload.FileStreamingOutput;
import it.fadeout.threads.styles.StyleDeleteFileWorker;
import it.fadeout.threads.styles.StyleUpdateFileWorker;
import wasdi.shared.business.Node;
import wasdi.shared.business.Style;
import wasdi.shared.business.User;
import wasdi.shared.business.UserApplicationPermission;
import wasdi.shared.business.UserApplicationRole;
import wasdi.shared.business.UserResourcePermission;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.StyleRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.geoserver.GeoServerManager;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.styles.StyleSharingViewModel;
import wasdi.shared.viewmodels.styles.StyleViewModel;

@Path("/styles")
public class StyleResource {

	@POST
	@Path("/uploadfile")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public PrimitiveResult uploadFile(@FormDataParam("file") InputStream fileInputStream,
			@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("name") String sName, @QueryParam("description") String sDescription,
			@QueryParam("public") Boolean bPublic) {
		WasdiLog.debugLog("StyleResource.uploadFile( Name: " + sName + ", Descr: " + sDescription + ", Public: " + bPublic + " )");

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		try {
			// Session checking
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser == null) {
				WasdiLog.debugLog("StyleResource.uploadFile: invalid session");
				oResult.setStringValue("Invalid session");
				return oResult;
			}

			String sUserId = oUser.getUserId();
			
			// Check the uniqueness of the name
			if (isStyleNameTaken(sName)) {
				WasdiLog.debugLog("StyleResource.uploadFile: name already used");
				oResult.setStringValue("The style's name is already used.");
				return oResult;
			}

			// File checking

			// Checks whether null file is passed
			if (fileInputStream == null) {
				WasdiLog.debugLog("StyleResource.uploadFile: invalid file");
				oResult.setStringValue("Invalid file");
				return oResult;
			}

			// filesystem-side work

			// Get Download Path
			String sDownloadRootPath = Wasdi.getDownloadPath();

			String sDirectoryPathname = sDownloadRootPath + "styles/";

			createDirectoryIfDoesNotExist(sDirectoryPathname);

			// Generate Style Id and file
			String sStyleId = UUID.randomUUID().toString();

			String sFilePathname = sDirectoryPathname + sName + ".sld";
			File oStyleSldFile = new File(sFilePathname);

			WasdiLog.debugLog("StyleResource.uploadFile: style file Path: " + oStyleSldFile.getPath());

			// save uploaded file
			writeFile(fileInputStream, oStyleSldFile);

			try (FileReader oFileReader = new FileReader(oStyleSldFile)) {
				insertStyle(sUserId, sStyleId, sName, sDescription, oStyleSldFile.getPath(), bPublic);
			} catch (Exception oEx) {
				WasdiLog.errorLog("StyleResource.uploadFile: " + oEx);
				oResult.setStringValue("Error saving the style.");
				return oResult;
			}

			//geoserver-side work
			geoServerAddStyle(oStyleSldFile.getPath());
		} catch (Exception oEx2) {
			WasdiLog.errorLog("StyleResource.uploadFile: " + oEx2);
		}

		oResult.setBoolValue(true);
		return oResult;
    }

	@POST
	@Path("/updatefile")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response updateFile(@FormDataParam("file") InputStream oFileInputStream,
			@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("styleId") String sStyleId,
			@QueryParam("zipped") Boolean bZipped) {
		WasdiLog.debugLog("StyleResource.updateFile( InputStream, StyleId: " + sStyleId);

		try {
			if (Utils.isNullOrEmpty(sStyleId) || sStyleId.contains("\\") || sStyleId.contains("/")) {

				WasdiLog.debugLog("StyleResource.updateFile: invalid styleId, aborting");
				return Response.status(Status.BAD_REQUEST).build();
			}


			// Session checking
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser == null) {
				WasdiLog.debugLog("StyleResource.updateFile: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}


			// File checking
			// Checks whether null file is passed
			if (oFileInputStream == null) {
				WasdiLog.debugLog("StyleResource.updateFile: invalid file");
				return Response.status(Status.BAD_REQUEST).build();
			}

			// DB-side work
			StyleRepository oStyleRepository = new StyleRepository();
			Style oStyle = oStyleRepository.getStyle(sStyleId);

			if (oStyle == null) {
				WasdiLog.debugLog("StyleResource.updateFile: style not found on DB");
				return Response.notModified("StyleId not found, please check parameters").build();
			}
			
			if (!PermissionsUtils.canUserAccessStyle(oUser.getUserId(), sStyleId)) {
				WasdiLog.debugLog("StyleResource.updateFile: User doesn't have rights on style");
				return Response.status(Status.FORBIDDEN).build();
			}

			// filesystem-side work

			// Get Download Path
			String sDownloadRootPath = Wasdi.getDownloadPath();

			String sDirectoryPathname = sDownloadRootPath + "styles/";

			createDirectoryIfDoesNotExist(sDirectoryPathname);
			
			// original sld file
			File oStyleSldFile = new File(sDownloadRootPath + "styles/" + oStyle.getName() + ".sld");
			
			if (!oStyleSldFile.exists()) {
				GeoServerManager oManager = new GeoServerManager();
				
				if (oManager.styleExists(oStyle.getName()) == false) {
					WasdiLog.debugLog("StyleResource.updateFile: style file " + oStyle.getName() + ".sld does not exists in node. Exit");
					return Response.status(404).build();					
				}
			}
			
			if (bZipped==null) {
				bZipped = false;
			}
			
			String sTempFileName = sDownloadRootPath + "styles/" + oStyle.getName() + ".sld.temp";
			
			if (bZipped) {
				WasdiLog.debugLog("StyleResource.updateFile: file is zipped");
				sTempFileName = sTempFileName.replace(".temp", ".zip");
			}

			// new sld file
			File oStyleSldFileTemp = new File(sTempFileName);

			//rename the ".temp" file
			// save uploaded file in ".temp" format

			try {
				writeFile(oFileInputStream, oStyleSldFileTemp);
			} 
			catch (Exception oEx) {
				WasdiLog.errorLog("StyleResource.updateFile: error writing file: " + oEx);
			}
			
			if (bZipped) {
				
				WasdiLog.debugLog("StyleResource.updateFile: unzip the style");
				ZipFileUtils oExtractor = new ZipFileUtils();
				oExtractor.unzip(sTempFileName, sDownloadRootPath+"styles/");
				// Delete the zip file
				FileUtils.deleteQuietly(oStyleSldFileTemp);
			}
			else {
				// checks that the Style Xml field is valid
				try {
					// Overwrite the old file
					Files.write(oStyleSldFile.toPath(), Files.readAllBytes(oStyleSldFileTemp.toPath()));

					// Delete the temp file
					Files.delete(oStyleSldFileTemp.toPath());
					
					WasdiLog.debugLog("StyleResource.updateFile: style files updated! styleID - " + oStyle.getStyleId());
				} catch (Exception oEx) {
					if (oStyleSldFileTemp.exists())
						oStyleSldFileTemp.delete();

					WasdiLog.errorLog("StyleResource.updateFile: " + oEx);
					return Response.status(Status.NOT_MODIFIED).build();
				}
			}

			//computational-node-side work
			if (Wasdi.s_sMyNodeCode.equals("wasdi")) {
				computationalNodesUpdateStyle(sSessionId, sStyleId, oStyleSldFile.getPath());
			}


			//geoserver-side work
			geoServerUpdateStyleIfExists(oStyle.getName(), oStyleSldFile.getPath());
		} catch (Exception oEx2) {
			WasdiLog.errorLog("StyleResource.updateFile: " + oEx2);
			return Response.serverError().build();
		}

		return Response.ok().build();
	}

	@GET
	@Path("/getxml")
	@Produces(MediaType.APPLICATION_XML)
	public Response getXML(
			@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("styleId") String sStyleId) {
		
		WasdiLog.debugLog("StyleResource.getXML( Style Id : " + sStyleId + ");");

		String sXml = "";

		try {
			// Session checking
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser == null) {
				WasdiLog.debugLog("StyleResource.getXML: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			if (!PermissionsUtils.canUserAccessStyle(oUser.getUserId(), sStyleId)) {
				WasdiLog.debugLog("StyleResource.getXML: user cannot access style");
				return Response.status(Status.FORBIDDEN).build();				
			}

			// Check that the style exists on db
			StyleRepository oStyleRepository = new StyleRepository();
			Style oStyle = oStyleRepository.getStyle(sStyleId);

			if (oStyle == null) {
				WasdiLog.debugLog("StyleResource.getXML: style not found on DB");
				return Response.notModified("StyleId not found").build();
			}

			// Get Download Path
			String sDownloadRootPath = Wasdi.getDownloadPath();

			File oStyleFile = new File(sDownloadRootPath + "styles/" + oStyle.getName() + ".sld");

			if (!oStyleFile.exists()) {
				WasdiLog.debugLog("StyleResource.getXML: style file not found;");
				return Response.status(Status.NOT_FOUND).build();
			}

			sXml = new String(Files.readAllBytes(oStyleFile.toPath()));

			return Response.ok(sXml, MediaType.APPLICATION_XML).build();
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog("StyleResource.getXML error : " + oEx);
		} 

		return Response.status(Status.INTERNAL_SERVER_ERROR).build();
	}

	@POST
	@Path("/updatexml")
	public Response updateXML(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("styleId") String sStyleId,
			@FormDataParam("styleXml") String sStyleXml) {
		WasdiLog.debugLog("StyleResource.updateXML: StyleId " + sStyleId + " invoke StyleResource.updateFile");

		// convert string to file and invoke updateGraphFile
		return updateFile(new ByteArrayInputStream(sStyleXml.getBytes(Charset.forName("UTF-8"))), sSessionId, sStyleId, false);
	}

	@POST
	@Path("/updateparams")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response updateParams(
			@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("styleId") String sStyleId,
			@QueryParam("description") String sDescription,
			@QueryParam("public") Boolean bPublic) {
		WasdiLog.debugLog("StyleResource.updateParams( StyleId: " + sStyleId);

		// Session checking
		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("StyleResource.updateParams: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		if (!PermissionsUtils.canUserAccessStyle(oUser.getUserId(), sStyleId)) {
			WasdiLog.debugLog("StyleResource.updateParams: user cannot access style");
			return Response.status(Status.UNAUTHORIZED).build();				
		}		

		StyleRepository oStyleRepository = new StyleRepository();
		Style oStyle = oStyleRepository.getStyle(sStyleId);

		if (oStyle == null)
			return Response.status(404).build();

		oStyle.setDescription(sDescription);
		oStyle.setIsPublic(bPublic);
		oStyleRepository.updateStyle(oStyle);

		return Response.ok().build();
	}

	@GET
	@Path("/getbyuser")
	public List<StyleViewModel> getStylesByUser(@HeaderParam("x-session-token") String sSessionId) {
		WasdiLog.debugLog("StyleResource.getStylesByUser");

		// Session checking
		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("StyleResource.getStylesByUser: invalid session");
			return null;
		}

		String sUserId = oUser.getUserId();

		StyleRepository oStyleRepository = new StyleRepository();
		List<StyleViewModel> aoRetStyles = new ArrayList<>();

		try {
			List<Style> aoDbStyles = oStyleRepository.getStylePublicAndByUser(sUserId);

			for (Style oCurWF : aoDbStyles) {
				StyleViewModel oVM = StyleViewModel.getFromStyle(oCurWF);
				// check if it was shared, if so, set shared with me to true
				oVM.setSharedWithMe(false);
				aoRetStyles.add(oVM);
			}

			// find sharings by userId
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			List<UserResourcePermission> aoStyleSharing = oUserResourcePermissionRepository.getStyleSharingsByUserId(sUserId);

			// For all the shared styles
			for (UserResourcePermission oSharing : aoStyleSharing) {
				// Create the VM
				Style oSharedWithMe = oStyleRepository.getStyle(oSharing.getResourceId());
				StyleViewModel oVM = StyleViewModel.getFromStyle(oSharedWithMe);

				if (!oVM.isPublic()) {
					// This is shared and not public: add to return list
					oVM.setSharedWithMe(true);
					aoRetStyles.add(oVM);
				} else {
					// This is shared but public, so this is already in our return list
					for (StyleViewModel oStyle : aoRetStyles) {
						// Find it and set shared flag = true
						if (oSharedWithMe.getStyleId().equals(oStyle.getStyleId())) {
							oStyle.setSharedWithMe(true);
						}
					}
				}
			}

		} catch (Exception oE) {
			WasdiLog.errorLog("StyleResource.getStylesByUser error: " + oE);
		}

		aoRetStyles.sort(Comparator.comparing(StyleViewModel::getName, String.CASE_INSENSITIVE_ORDER));

		return aoRetStyles;
	}

	/**
	 * Delete a style. The API must be called on the main server.
	 * @param sSessionId
	 * @param sStyleId
	 * @return
	 */
	@DELETE
	@Path("/delete")
	public Response deleteStyle(@HeaderParam("x-session-token") String sSessionId, @QueryParam("styleId") String sStyleId) {
		
		WasdiLog.debugLog("StyleResource.deleteStyle( Style: " + sStyleId + " )");

		// This API is allowed ONLY on main nodes
		if (!Wasdi.s_sMyNodeCode.equals("wasdi")) {
			WasdiLog.debugLog("StyleResource.DeleteStyle: this is a computational node, cannot call this API here");
			return Response.status(Status.BAD_REQUEST).build();
		}

		try {
			// Session checking
			User oUser = Wasdi.getUserFromSession(sSessionId);

			// Check the user
			if (oUser == null) {
				WasdiLog.debugLog("StyleResource.deleteStyle: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			String sUserId = oUser.getUserId();

			// DB-side work
			StyleRepository oStyleRepository = new StyleRepository();
			Style oStyle = oStyleRepository.getStyle(sStyleId);

			if (oStyle == null) {
				WasdiLog.debugLog("StyleResource.deleteStyle: unable to find style");
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			if (!PermissionsUtils.canUserAccessStyle(sUserId, sStyleId)) {
				WasdiLog.debugLog("StyleResource.deleteStyle: user cannot access the style");
				return Response.status(Status.FORBIDDEN).build();
			}

			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

			if (oUserResourcePermissionRepository.isStyleSharedWithUser(sUserId, sStyleId)) {
				WasdiLog.debugLog("StyleResource.deleteStyle: the style wasd shared with " + oUser.getUserId() + ", delete the sharing");
				oUserResourcePermissionRepository.deletePermissionsByUserIdAndStyleId(oUser.getUserId(), sStyleId);

				return Response.ok().build();
			}
			else {
				// Delete sharings
				oUserResourcePermissionRepository.deletePermissionsByStyleId(sStyleId);

				oStyleRepository.deleteStyle(sStyleId);


				if (Wasdi.s_sMyNodeCode.equals("wasdi")) {
					// computational-node-side work
					computationalNodesDeleteStyle(sSessionId, oStyle.getStyleId(), oStyle.getName());
				}
							
				// geoserver-side work
				geoServerRemoveStyleIfExists(oStyle.getName());

				// filesystem-side work
				filesystemDeleteStyleIfExists(oStyle.getName());

				// Trigger the style delete operation on this specific node
				WasdiLog.debugLog("StyleResource.deleteStyle: Style Deleted");

				return Response.ok().build();				
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("StyleResource.deleteStyle error: " + oEx);
			return Response.serverError().build();
		}
	}

	@DELETE
	@Path("/nodedelete")
	public Response nodeDeleteStyle(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("styleId") String sStyleId,
			@QueryParam("styleName") String sStyleName) {
		WasdiLog.debugLog("StyleResource.nodeDeleteStyle( Session: " + sSessionId + ", Style: " + sStyleName + " )");

		// This API is allowed ONLY on computed nodes
		if (Wasdi.s_sMyNodeCode.equals("wasdi")) {
			WasdiLog.debugLog("StyleResource.nodeDeleteStyle: this is the main node, cannot call this API here");
			return Response.status(Status.BAD_REQUEST).build();
		}

		try {
			User oUser = Wasdi.getUserFromSession(sSessionId);

			// Check user
			if (oUser == null) {
				WasdiLog.debugLog("StyleResource.nodeDeleteStyle: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			// Trigger the style delete operation on this specific node
			WasdiLog.debugLog("StyleResource.nodeDeleteStyle: this is a computing node, just execute Delete here");

			geoServerRemoveStyleIfExists(sStyleName);
			
			filesystemDeleteStyleIfExists(sStyleName);

			return Response.ok().build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorResource.nodeDeleteProcessor: " + oEx);
			return Response.serverError().build();
		}
	}

	@PUT
	@Path("share/add")
	@Produces({"application/xml", "application/json", "text/xml"})
	public PrimitiveResult shareStyle(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("styleId") String sStyleId, @QueryParam("userId") String sUserId) {
		WasdiLog.debugLog("StyleResource.shareStyle(  Style : " + sStyleId + ", User: " + sUserId + " )");

		//init repositories
		UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
		StyleRepository oStyleRepository = new StyleRepository();

		// Validate Session
		User oRequestingUser = Wasdi.getUserFromSession(sSessionId);
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		if (oRequestingUser == null) {
			WasdiLog.debugLog("StyleResource.shareStyle: invalid session");
			oResult.setStringValue("Invalid session.");
			return oResult;
		}

		try {
			// Check if the style exists and is of the user calling this API
			Style oStyle = oStyleRepository.getStyle(sStyleId);

			if (oStyle == null) {
				WasdiLog.debugLog("StyleResource.shareStyle: invalid style");
				oResult.setStringValue("Invalid Style");
				return oResult;
			}

			if (oRequestingUser.getUserId().equals(sUserId)) {
				WasdiLog.debugLog("StyleResource.shareStyle: auto sharing not so smart");
				oResult.setStringValue("Impossible to autoshare.");
				return oResult;
			}

			// Check the destination user
			UserRepository oUserRepository = new UserRepository();
			User oDestinationUser = oUserRepository.getUser(sUserId);
			
			// Can't find destination user for the sharing
			if (oDestinationUser == null) {
				WasdiLog.debugLog("StyleResource.shareStyle: invalid target user");
				oResult.setStringValue("Can't find target user of the sharing");
				return oResult;
			}

			//if the requester is not the owner
			if (!oStyle.getUserId().equals(oRequestingUser.getUserId())) {
				// Is he trying to share with the owner?
				if (oStyle.getUserId().equals(sUserId)) {
					oResult.setStringValue("Cannot Share with owner");
					return oResult;
				}

				// the requester has the share?
				if (!oUserResourcePermissionRepository.isStyleSharedWithUser(oRequestingUser.getUserId(), sStyleId)
						&& !UserApplicationRole.userHasRightsToAccessApplicationResource(oRequestingUser.getRole(), ADMIN_DASHBOARD)) {
					oResult.setStringValue("Unauthorized");
					return oResult;
				}
			}

			// Check if has been already shared
			if (oUserResourcePermissionRepository.isStyleSharedWithUser(sUserId, sStyleId)) {
				oResult.setStringValue("Already shared");
				return oResult;
			}

			// Create and insert the sharing
			UserResourcePermission oStyleSharing =
					new UserResourcePermission("style", sStyleId, sUserId, oStyle.getUserId(), oRequestingUser.getUserId(), "write");

			oUserResourcePermissionRepository.insertPermission(oStyleSharing);

			WasdiLog.debugLog("StyleResource.shareStyle: Style" + sStyleId + " Shared from " + oRequestingUser.getUserId() + " to " + sUserId);

			try {
				String sMercuriusAPIAddress = WasdiConfig.Current.notifications.mercuriusAPIAddress;

				if (Utils.isNullOrEmpty(sMercuriusAPIAddress)) {
					WasdiLog.debugLog("StyleResource.shareStyle: sMercuriusAPIAddress is null");
				} else {
					MercuriusAPI oAPI = new MercuriusAPI(sMercuriusAPIAddress);
					Message oMessage = new Message();

					String sTitle = "Style " + oStyle.getName() + " Shared";

					oMessage.setTilte(sTitle);

					String sSender = WasdiConfig.Current.notifications.sftpManagementMailSender;
					if (sSender == null) {
						sSender = "wasdi@wasdi.net";
					}

					oMessage.setSender(sSender);

					String sMessage = "The user " + oRequestingUser.getUserId() + " shared with you the Style: " + oStyle.getName();

					oMessage.setMessage(sMessage);

					Integer iPositiveSucceded = 0;

					iPositiveSucceded = oAPI.sendMailDirect(sUserId, oMessage);

					WasdiLog.debugLog("StyleResource.shareStyle: notification sent with result " + iPositiveSucceded);
				}
			} catch (Exception oEx) {
				WasdiLog.errorLog("StyleResource.shareStyle: notification exception " + oEx.toString());
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("StyleResource.shareStyle error: " + oEx);

			oResult.setStringValue("Error sharing the style");
			oResult.setBoolValue(false);

			return oResult;
		}

		oResult.setStringValue("Done");
		oResult.setBoolValue(true);

		return oResult;
	}

	@DELETE
	@Path("share/delete")
	@Produces({"application/xml", "application/json", "text/xml"})
	public PrimitiveResult deleteUserSharingStyle(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("styleId") String sStyleId, @QueryParam("userId") String sUserId) {
		WasdiLog.debugLog("StyleResource.deleteUserSharedStyle( ProcId: " + sStyleId + ", User:" + sUserId + " )");

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		try {
			// Validate Session
			User oOwnerUser = Wasdi.getUserFromSession(sSessionId);

			if (oOwnerUser == null) {
				WasdiLog.debugLog("StyleResource.deleteUserSharedStyle: invalid session");
				oResult.setStringValue("Invalid session");
				return oResult;
			}

			try {
				UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
				UserResourcePermission oStyleShare = oUserResourcePermissionRepository.getStyleSharingByUserIdAndStyleId(sUserId, sStyleId);
				
				if (oStyleShare == null) {
					WasdiLog.debugLog("StyleResource.deleteUserSharedStyle: invalid sharing");
					oResult.setStringValue("Sharing not found");
					return oResult;					
				}

				
				// if the user making the call is the one on the sharing OR
				if (oStyleShare.getUserId().equals(oOwnerUser.getUserId()) ||
						// if the user making the call is the owner of the style OR
						oStyleShare.getOwnerId().equals(oOwnerUser.getUserId())
						// if the user has ADMIN rights
						|| UserApplicationRole.userHasRightsToAccessApplicationResource(oOwnerUser.getRole(), UserApplicationPermission.ADMIN_DASHBOARD)) {
					// Delete the sharing
					oUserResourcePermissionRepository.deletePermissionsByUserIdAndStyleId(sUserId, sStyleId);
				} else {
					oResult.setStringValue("Unauthorized");
					return oResult;
				}
				
			} catch (Exception oEx) {
				WasdiLog.errorLog("StyleResource.deleteUserSharedStyle error: " + oEx);
				oResult.setStringValue("Error deleting style sharing");
				return oResult;
			}

			oResult.setStringValue("Done");
			oResult.setBoolValue(true);
		} catch (Exception oE) {
			WasdiLog.errorLog("StyleResource.deleteUserSharedStyle error: " + oE);
		}

		return oResult;
	}

	@GET
	@Path("share/bystyle")
	@Produces({"application/xml", "application/json", "text/xml"})
	public List<StyleSharingViewModel> getEnableUsersSharedStyle(@HeaderParam("x-session-token") String sSessionId, @QueryParam("styleId") String sStyleId) {
		List<StyleSharingViewModel> oResult = new ArrayList<>();

		WasdiLog.debugLog("StyleResource.getEnableUsersSharedStyle(  Style : " + sStyleId + " )");

		// Validate Session
		User oAskingUser = Wasdi.getUserFromSession(sSessionId);

		if (oAskingUser == null) {
			WasdiLog.debugLog("StyleResource.getEnableUsersSharedStyle: invalid session");
			return oResult;
		}

		try {
			// Check if the style exists and is of the user calling this API
			StyleRepository oStyleRepository = new StyleRepository();
			Style oValidateStyle = oStyleRepository.getStyle(sStyleId);

			if (oValidateStyle == null) {
				// return
				WasdiLog.debugLog("StyleResource.getEnableUsersSharedStyle: Style not found");
				// if something went wrong returns an empty set
				return oResult;
			}

			//Retrieve and returns the sharings
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			oUserResourcePermissionRepository.getStyleSharingsByStyleId(sStyleId).forEach(element -> {
				oResult.add(new StyleSharingViewModel(element));
			});

			return oResult;
		} catch (Exception oEx) {
			WasdiLog.errorLog("StyleResource.getEnableUsersSharedStyle error: " + oEx);
			return oResult;
		}
	}

	@GET
	@Path("download")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response download(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("token") String sTokenSessionId,
			@QueryParam("styleId") String sStyleId) {
		WasdiLog.debugLog("StyleResource.download( StyleId: " + sStyleId + " )");

		try {
			if (!Utils.isNullOrEmpty(sSessionId)) {
				sTokenSessionId = sSessionId;
			}

			User oUser = Wasdi.getUserFromSession(sTokenSessionId);

			if (oUser == null) {
				WasdiLog.debugLog("StyleResource.download: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			StyleRepository oStyleRepository = new StyleRepository();
			Style oStyle = oStyleRepository.getStyle(sStyleId);

			ResponseBuilder oResponseBuilder = null;

			if (oStyle == null) {
				WasdiLog.debugLog("StyleResource.download: Style Id not found on DB");
				oResponseBuilder = Response.noContent();
				return oResponseBuilder.build();
			}

			// Take path
			String sDownloadRootPath = Wasdi.getDownloadPath();
			String sStyleSldPath = sDownloadRootPath + "styles/" + sStyleId + ".sld";

			File oFile = new File(sStyleSldPath);

			if (!oFile.exists()) {
				WasdiLog.debugLog("StyleResource.download: file does not exists " + oFile.getPath());
				oResponseBuilder = Response.serverError();
			} 
			else {

				FileStreamingOutput oStream;
				oStream = new FileStreamingOutput(oFile);

				oResponseBuilder = Response.ok(oStream);
				oResponseBuilder.header("Content-Disposition", "attachment; filename=" + oStyle.getName() + ".sld");
				oResponseBuilder.header("Content-Length", Long.toString(oFile.length()));
			}

			return oResponseBuilder.build();
				
		} catch (Exception oEx) {
			WasdiLog.errorLog("StyleResource.download error: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}


	private boolean isStyleNameTaken(String sName) {
		StyleRepository oStyleRepository = new StyleRepository();
		return oStyleRepository.isStyleNameTaken(sName);
	}

	private void insertStyle(String sUserId, String sStyleId, String sName, String sDescription, String sFilePath, Boolean bPublic) {
		// Create Entity
		Style oStyle = new Style();
		oStyle.setStyleId(sStyleId);
		oStyle.setName(sName);
		oStyle.setDescription(sDescription);
		oStyle.setFilePath(sFilePath);
		oStyle.setUserId(sUserId);

		if (bPublic != null) {
			oStyle.setIsPublic(bPublic.booleanValue());
		}

		// Save the Style
		StyleRepository oStyleRepository = new StyleRepository();
		oStyleRepository.insertStyle(oStyle);
	}
	
	private void filesystemDeleteStyleIfExists(String sName) {
		WasdiLog.debugLog("StyleResource.filesystemDeleteStyleIfExists( " + "Name: " + sName + " )");

		if (Utils.isNullOrEmpty(sName)) {
			WasdiLog.debugLog("StyleResource.filesystemDeleteStyleIfExists: Name is null or empty.");
			return;
		}

		// Get Download Path on the current WASDI instance
		String sBasePath = Wasdi.getDownloadPath();
		sBasePath += "styles/";
		String sStyleFilePath = sBasePath + sName + ".sld";

		File oStyleFile = new File(sStyleFilePath);
		if (oStyleFile.exists()) {
			if (!oStyleFile.delete()) {
				WasdiLog.debugLog("StyleResource.filesystemDeleteStyleIfExists: Error deleting the style file " + sStyleFilePath);
			}
		}
	}

	private void geoServerAddStyle(String sStyleFilePath) throws MalformedURLException {
		WasdiLog.debugLog("StyleResource.geoServerAddStyle( " + "StyleFile: " + sStyleFilePath + " )");

		GeoServerManager oGeoServerManager = new GeoServerManager();

		if (sStyleFilePath != null) {
			oGeoServerManager.publishStyle(sStyleFilePath);
		}
	}

	private void geoServerUpdateStyleIfExists(String sName, String sStyleFilePath) throws MalformedURLException {
		WasdiLog.debugLog("StyleResource.geoServerUpdateStyleIfExists( " + "Name: " + sName + ", StyleFile: " + sStyleFilePath + " )");

		GeoServerManager oGeoServerManager = new GeoServerManager();

		if (oGeoServerManager.styleExists(sName)) {
			WasdiLog.debugLog("StyleResource.geoServerUpdateStyleIfExists: style exists, update it");
			
			
			if (!oGeoServerManager.updateStyle(sStyleFilePath)) {
				WasdiLog.debugLog("StyleResource.geoServerUpdateStyleIfExists: update style returned false!!");
			}
		}
	}

	private void geoServerRemoveStyleIfExists(String sName) throws MalformedURLException {
		WasdiLog.debugLog("StyleResource.geoServerRemoveStyleIfExists( " + "Name: " + sName + " )");

		GeoServerManager oGeoServerManager = new GeoServerManager();

		if (oGeoServerManager.styleExists(sName)) {
			
			WasdiLog.debugLog("StyleResource.geoServerRemoveStyleIfExists: style exists. Search for layers with the style assigned");
			
			List<String> asLayers = oGeoServerManager.getLayers();
			
			for (String sLayer : asLayers) {
				if (sLayer.startsWith("wasdi:")) {
					sLayer = sLayer.replace("wasdi:", "");
					String sStyle = oGeoServerManager.getLayerStyle(sLayer);
					
					if (sStyle.equals(sName)) {
						WasdiLog.debugLog("StyleResource.geoServerRemoveStyleIfExists: removing style from " + sLayer);
						oGeoServerManager.configureLayerStyle(sLayer, "raster");
					}
				}
			}			
			
			
			String sStyleFilePath = Wasdi.getDownloadPath() + "styles/" + sName + ".sld";
			
			WasdiLog.debugLog("StyleResource.geoServerRemoveStyleIfExists: remove the style " + sStyleFilePath);
			
			oGeoServerManager.removeStyle(sName);
		}
	}

	private void computationalNodesUpdateStyle(String sSessionId, String sName, String sFilePath) {
		// In the main node: start a thread to update all the computing nodes
		try {
			WasdiLog.debugLog("StyleResource.computationalNodesUpdateStyle: this is the main node, starting Worker to update computing nodes");

			NodeRepository oNodeRepo = new NodeRepository();
			List<Node> aoNodes = oNodeRepo.getNodesList();

			//This is the main node: forward the request to other nodes
			StyleUpdateFileWorker oUpdateWorker = new StyleUpdateFileWorker(aoNodes, sSessionId, sName, sFilePath);

			oUpdateWorker.start();

			WasdiLog.debugLog("StyleResource.computationalNodesUpdateStyle: Worker started");
		} catch (Exception oEx) {
			WasdiLog.errorLog("StyleResource.computationalNodesUpdateStyle: error starting UpdateWorker " + oEx.toString());
		}
	}
 
	private void computationalNodesDeleteStyle(String sSessionId, String sStyleId, String sStyleName) {
		// Start a thread to update all the computing nodes
		try {
			WasdiLog.debugLog("StyleResource.computationalNodesDeleteStyle: this is the main node, starting Worker to delete Style also on computing nodes");

			NodeRepository oNodeRepo = new NodeRepository();
			List<Node> aoNodes = oNodeRepo.getNodesList();

			// Util to call the API on computing nodes
			StyleDeleteFileWorker oDeleteWorker = new StyleDeleteFileWorker(aoNodes, sSessionId, sStyleId, sStyleName);
			oDeleteWorker.start();

			WasdiLog.debugLog("StyleResource.computationalNodesDeleteStyle: Worker started");						
		} catch (Exception oEx) {
			WasdiLog.errorLog("StyleResource.computationalNodesDeleteStyle: error starting UpdateWorker " + oEx.getMessage());
		}
	}	
}
