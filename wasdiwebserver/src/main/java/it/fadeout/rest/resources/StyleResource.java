package it.fadeout.rest.resources;

import static wasdi.shared.utils.WasdiFileUtils.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.util.ArrayList;
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

import org.glassfish.jersey.media.multipart.FormDataParam;

import it.fadeout.Wasdi;
import it.fadeout.mercurius.business.Message;
import it.fadeout.mercurius.client.MercuriusAPI;
import it.fadeout.rest.resources.largeFileDownload.FileStreamingOutput;
import wasdi.shared.business.Style;
import wasdi.shared.business.StyleSharing;
import wasdi.shared.business.User;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.StyleRepository;
import wasdi.shared.data.StyleSharingRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.styles.StyleSharingViewModel;
import wasdi.shared.viewmodels.styles.StyleViewModel;

@Path("/styles")
public class StyleResource {

	@POST
	@Path("/uploadfile")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFile(@FormDataParam("file") InputStream fileInputStream,
			@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("name") String sName, @QueryParam("description") String sDescription,
			@QueryParam("public") Boolean bPublic) {
		Utils.debugLog("StyleResource.uploadFile( Name: " + sName + ", Descr: " + sDescription + ", Public: " + bPublic + " )");

		try {
			// Check authorization
			if (Utils.isNullOrEmpty(sSessionId)) {
				Utils.debugLog("StyleResource.uploadFile: invalid session");
				return Response.status(401).build();
			}

			User oUser = Wasdi.getUserFromSession(sSessionId);

			// Checks whether null file is passed
			if (fileInputStream == null)
				return Response.status(400).build();

			if (oUser == null)
				return Response.status(401).build();

			if (Utils.isNullOrEmpty(oUser.getUserId()))
				return Response.status(401).build();

			String sUserId = oUser.getUserId();

			// Get Download Path
			String sDownloadRootPath = Wasdi.getDownloadPath();

			String sDirectoryPathname = sDownloadRootPath + "styles/";

			createDirectoryIfDoesNotExist(sDirectoryPathname);

			// Generate Style Id and file
			String sStyleId = UUID.randomUUID().toString();
			String sFilePathname = sDirectoryPathname + sName + ".sld";
			File oStyleSldFile = new File(sFilePathname);

			Utils.debugLog("StyleResource.uploadFile: style file Path: " + oStyleSldFile.getPath());

			// save uploaded file
			writeFile(fileInputStream, oStyleSldFile);

			// Create Entity
			Style oStyle = new Style();
			oStyle.setName(sName);
			oStyle.setDescription(sDescription);
			oStyle.setFilePath(oStyleSldFile.getPath());
			oStyle.setUserId(sUserId);
			oStyle.setStyleId(sStyleId);

			if (bPublic == null)
				oStyle.setIsPublic(false);
			else
				oStyle.setIsPublic(bPublic.booleanValue());

			try (FileReader oFileReader = new FileReader(oStyleSldFile)) {
				// Save the Style
				StyleRepository oStyleRepository = new StyleRepository();
				oStyleRepository.insertStyle(oStyle);
			} catch (Exception oEx) {
				Utils.debugLog("StyleResource.uploadFile: " + oEx);
				return Response.serverError().build();
			}
			
		} catch (Exception oEx2) {
			Utils.debugLog("StyleResource.uploadFile: " + oEx2);
		}

		return Response.ok().build();
    }

	@POST
	@Path("/updatefile")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response updateFile(@FormDataParam("file") InputStream fileInputStream,
			@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("styleId") String sStyleId) {
		Utils.debugLog("StyleResource.updateFile( InputStream, StyleId: " + sStyleId);

		try {
			// Check authorization
			if (Utils.isNullOrEmpty(sSessionId)) {
				Utils.debugLog("StyleResource.updateFile( InputStream, Session: " + sSessionId + ", style: " + sStyleId + " ): invalid session");
				return Response.status(401).build();
			}

			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser == null)
				return Response.status(401).build();

			if (Utils.isNullOrEmpty(oUser.getUserId()))
				return Response.status(401).build();

			// Get Download Path
			String sDownloadRootPath = Wasdi.getDownloadPath();

			Utils.debugLog("StylesResource.updateFile: download path " + sDownloadRootPath);

			String sDirectoryPathname = sDownloadRootPath + "styles/";

			createDirectoryIfDoesNotExist(sDirectoryPathname);

			// Check that the style exists on db
			StyleRepository oStyleRepository = new StyleRepository();
			Style oStyle = oStyleRepository.getStyle(sStyleId);

			if (oStyle == null) {
				Utils.debugLog("StylesResource.updateFile: error in styleId " + sStyleId + " not found on DB");
				return Response.notModified("StyleId not found, please check parameters").build();
			}

			// Checks that user can modify the style
			StyleSharingRepository oStyleSharingRepository = new StyleSharingRepository();

			if (!oUser.getUserId().equals(oStyle.getUserId()) && !oStyleSharingRepository.isSharedWithUser(oUser.getUserId(), oStyle.getStyleId())) {
				Utils.debugLog("StylesResource.updateFile: User " + oUser.getUserId() + " doesn't have rights on style " + oStyle.getName());
				return Response.status(Status.UNAUTHORIZED).build();
			}

			// original sld file
			File oStyleSldFile = new File(sDownloadRootPath + "styles/" + sStyleId + ".sld");
			// new sld file
			File oStyleSldFileTemp = new File(sDownloadRootPath + "styles/" + sStyleId + ".sld.temp");
			//if the new one is ok delete the old and rename the ".temp" file
			// save uploaded file in ".temp" format

			try {
				writeFile(fileInputStream, oStyleSldFile);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			// checks that the graph field is valid by checking the nodes
			try (FileReader oFileReader = new FileReader(oStyleSldFileTemp)) {				
				// Overwrite the old file
				Files.write(oStyleSldFile.toPath(), Files.readAllBytes(oStyleSldFileTemp.toPath()));
				// Delete the temp file
				Files.delete(oStyleSldFileTemp.toPath());
				
				Utils.debugLog("StylesResource.updateFile: style files updated! styleID" + oStyle.getStyleId());

				// Updates the location on the current server
				oStyle.setFilePath(oStyleSldFile.getPath());
				oStyleRepository.updateStyle(oStyle);
			} catch (Exception oEx) {
				if (oStyleSldFileTemp.exists())
					oStyleSldFileTemp.delete();

				Utils.debugLog("StylesResource.updateFile: " + oEx);
				return Response.status(Status.NOT_MODIFIED).build();
			}

		} catch (Exception oEx2) {
			Utils.debugLog("StylesResource.updateFile: " + oEx2);
		}

		return Response.ok().build();
	}

	@GET
	@Path("/getxml")
	@Produces(MediaType.APPLICATION_XML)
	public Response getXML(
			@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("styleId") String sStyleId) {
		Utils.debugLog("StyleResource.getXML( Style Id : " + sStyleId + ");");

		String sXml = "";

		try {
			// Check authorization
			if (Utils.isNullOrEmpty(sSessionId)) {
				Utils.debugLog("StyleResource.getXML( Workspace Id : " + sStyleId + ");");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser == null)
				return Response.status(Status.UNAUTHORIZED).build();

			if (Utils.isNullOrEmpty(oUser.getUserId()))
				return Response.status(Status.UNAUTHORIZED).build();

			// Check that the stylew exists on db
			StyleRepository oStyleRepository = new StyleRepository();
			Style oStyle = oStyleRepository.getStyle(sStyleId);

			if (oStyle == null) {
				Utils.debugLog("StylesResource.getXML: error in styleId " + sStyleId + " not found on DB");
				return Response.notModified("StyleId not found").build();
			}

			// Get Download Path
			String sDownloadRootPath = Wasdi.getDownloadPath();

			File oStyleFile = new File(sDownloadRootPath + "styles/" + oStyle.getName() + ".sld");

			if (!oStyleFile.exists()) {
				Utils.debugLog("StyleResource.getXML( Style Id : " + sStyleId + ") Error, style file not found;");
				return Response.status(Status.NOT_FOUND).build();
			}

			sXml = new String(Files.readAllBytes(oStyleFile.toPath()));

			return Response.ok(sXml, MediaType.APPLICATION_XML).build();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return Response.status(Status.INTERNAL_SERVER_ERROR).build();
	}

	@POST
	@Path("/updatexml")
	public Response updateXML(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("styleId") String sStyleId,
			@FormDataParam("graphXml") String sGraphXml) {
		Utils.debugLog("StyleResource.updateXML: StyleId " + sStyleId + " invoke StyleResource.updateGraph");

		// convert string to file and invoke updateGraphFile
		return updateFile(new ByteArrayInputStream(sGraphXml.getBytes(Charset.forName("UTF-8"))), sSessionId, sStyleId);
	}

	@POST
	@Path("/updateparams")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response updateParams(
			@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("styleid") String sStyleId,
			@QueryParam("name") String sName,
			@QueryParam("description") String sDescription,
			@QueryParam("public") Boolean bPublic) {
		Utils.debugLog("StylesResource.updateParams( InputStream, Style: " + sName + ", StyleId: " + sStyleId);

		if (Utils.isNullOrEmpty(sSessionId)) {
			Utils.debugLog("StylesResource.updateParams( InputStream, Session: " + sSessionId + ", Ws: " + sStyleId + " ): invalid session");
			return Response.status(401).build();
		}

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null)
			return Response.status(401).build();

		if (Utils.isNullOrEmpty(oUser.getUserId()))
			return Response.status(401).build();

		StyleRepository oStyleRepository = new StyleRepository();
		Style oStyle = oStyleRepository.getStyle(sStyleId);

		if (oStyle == null)
			return Response.status(404).build();

		oStyle.setName(sName);
		oStyle.setDescription(sDescription);
		oStyle.setIsPublic(bPublic);
		oStyleRepository.updateStyle(oStyle);

		return Response.ok().build();
	}

	@GET
	@Path("/getbyuser")
	public List<StyleViewModel> getStylesByUser(@HeaderParam("x-session-token") String sSessionId) {
		Utils.debugLog("StyleResource.getStylesByUser");

		if (Utils.isNullOrEmpty(sSessionId)) {
			Utils.debugLog("StyleResource.getStylesByUser: session null");
			return null;
		}

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			Utils.debugLog("StyleResource.getStylesByUser( " + sSessionId + " ): invalid session");
			return null;
		}

		if (Utils.isNullOrEmpty(oUser.getUserId())) {
			Utils.debugLog("StyleResource.getStylesByUser: user id null");
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
			StyleSharingRepository oStyleSharingRepository = new StyleSharingRepository();
			List<StyleSharing> aoStyleSharing = oStyleSharingRepository.getStyleSharingByUser(sUserId);

			// For all the shared styles
			for (StyleSharing oSharing : aoStyleSharing) {
				// Create the VM
				Style oSharedWithMe = oStyleRepository.getStyle(oSharing.getStyleId());
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
			Utils.debugLog("StylesResource.getStylesByUser( " + sSessionId + " ): " + oE);
		}

		Utils.debugLog("StylesResource.getStylesByUser: return " + aoRetStyles.size() + " styles");

		return aoRetStyles;
	}

	@GET
	@Path("/delete")
	public Response delete(@HeaderParam("x-session-token") String sSessionId, @QueryParam("styleId") String sStyleId) {
		Utils.debugLog("StyleResource.delete( StyleId: " + sStyleId + " )");

		try {
			// Check User
			if (Utils.isNullOrEmpty(sSessionId))
				return Response.status(Status.UNAUTHORIZED).build();

			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser == null) {
				Utils.debugLog("StylesResource.delete( Session: " + sSessionId + ", Style: " + sStyleId + " ): invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (Utils.isNullOrEmpty(oUser.getUserId()))
				return Response.status(Status.UNAUTHORIZED).build();

			String sUserId = oUser.getUserId();

			// Check if the style exists
			StyleRepository oStyleRepository = new StyleRepository();
			Style oStyle = oStyleRepository.getStyle(sStyleId);

			if (oStyle == null)
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();

			// check if the current user is the owner of the style
			if (!oStyle.getUserId().equals(sUserId)) {
				// check if the current user has a sharing of the style
				StyleSharingRepository oStyleSharingRepository = new StyleSharingRepository();

				if (oStyleSharingRepository.isSharedWithUser(sUserId, sStyleId)) {
					oStyleSharingRepository.deleteByUserIdStyleId(sUserId, sStyleId);
					Utils.debugLog("StylesResource.delete: Deleted sharing between user " + sUserId + " and style " + oStyle.getName() + " Style files kept in place");
					return Response.ok().build();
				}

				// not the owner && no sharing with you. You have no power here !
				return Response.status(Status.FORBIDDEN).build();
			}

			// Get Download Path on the current WASDI instance
			String sBasePath = Wasdi.getDownloadPath();
			sBasePath += "styles/";
			String sStyleFilePath = sBasePath + oStyle.getStyleId() + ".sld";

			if (!Utils.isNullOrEmpty(sStyleFilePath)) {
				File oStyleFile = new File(sStyleFilePath);
				if (oStyleFile.exists()) {
					if (!oStyleFile.delete()) {
						Utils.debugLog("StylesResource.delete: Error deleting the style file " + oStyle.getFilePath());
					}
				}
			} else {
				Utils.debugLog("StylesResource.delete: style file path is null or empty.");
			}

			// Delete sharings
			StyleSharingRepository oStyleSharingRepository = new StyleSharingRepository();
			oStyleSharingRepository.deleteByStyleId(sStyleId);

			// Delete the style
			oStyleRepository.deleteStyle(sStyleId);
		} catch (Exception oE) {
			Utils.debugLog("StylesResource.delete( Session: " + sSessionId + ", Style: " + sStyleId + " ): " + oE);
			return Response.serverError().build();
		}

		return Response.ok().build();
	}

	@PUT
	@Path("share/add")
	@Produces({"application/xml", "application/json", "text/xml"})
	public PrimitiveResult shareStyle(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("styleId") String sStyleId, @QueryParam("userId") String sUserId) {
		Utils.debugLog("StylesResource.shareStyle(  Style : " + sStyleId + ", User: " + sUserId + " )");

		//init repositories
		StyleSharingRepository oStyleSharingRepository = new StyleSharingRepository();
		StyleRepository oStyleRepository = new StyleRepository();

		// Validate Session
		User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		if (oRequesterUser == null) {
			Utils.debugLog("StylesResource.shareStyle( Session: " + sSessionId + ", Style: " + sStyleId + ", User: " + sUserId + " ): invalid session");
			oResult.setStringValue("Invalid session.");
			return oResult;
		}

		if (Utils.isNullOrEmpty(oRequesterUser.getUserId())) {
			oResult.setStringValue("Invalid user.");
			return oResult;
		}

		try {
			// Check if the processor exists and is of the user calling this API
			oStyleRepository = new StyleRepository();
			Style oStyle = oStyleRepository.getStyle(sStyleId);

			if (oStyle == null) {
				oResult.setStringValue("Invalid Style");
				return oResult;
			}

			if (oRequesterUser.getUserId().equals(sUserId)) {
				Utils.debugLog("StylesResource.ShareStyle: auto sharing not so smart");
				oResult.setStringValue("Impossible to autoshare.");
				return oResult;
			}

			// Check the destination user
			UserRepository oUserRepository = new UserRepository();
			User oDestinationUser = oUserRepository.getUser(sUserId);
			// Can't find destination user for the sharing
			if (oDestinationUser == null) {
				oResult.setStringValue("Can't find target user of the sharing");
				return oResult;
			}

			//if the requester is not the owner
			if (!oStyle.getUserId().equals(oRequesterUser.getUserId())) {
				// Is he trying to share with the owner?
				if (oStyle.getUserId().equals(sUserId)) {
					oResult.setStringValue("Cannot Share with owner");
					return oResult;
				}

				// the requester has the share?
				if (!oStyleSharingRepository.isSharedWithUser(oRequesterUser.getUserId(), sStyleId)) {
					oResult.setStringValue("Unauthorized");
					return oResult;
				}
			}

			// Check if has been already shared
			if (oStyleSharingRepository.isSharedWithUser(sUserId, sStyleId)) {
				oResult.setStringValue("Already shared");
				return oResult;
			}

			// Create and insert the sharing
			StyleSharing oStyleSharing = new StyleSharing();
			Timestamp oTimestamp = new Timestamp(System.currentTimeMillis());
			oStyleSharing.setOwnerId(oStyle.getUserId());
			oStyleSharing.setUserId(sUserId);
			oStyleSharing.setStyleId(sStyleId);
			oStyleSharing.setShareDate((double) oTimestamp.getTime());
			oStyleSharingRepository.insertStyleSharing(oStyleSharing);

			Utils.debugLog("StylesResource.shareStyle: Style" + sStyleId + " Shared from " + oRequesterUser.getUserId() + " to " + sUserId);

			try {
				String sMercuriusAPIAddress = WasdiConfig.Current.notifications.mercuriusAPIAddress;

				if (Utils.isNullOrEmpty(sMercuriusAPIAddress)) {
					Utils.debugLog("StylesResource.shareStyle: sMercuriusAPIAddress is null");
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

					String sMessage = "The user " + oRequesterUser.getUserId() + " shared with you the Style: " + oStyle.getName();

					oMessage.setMessage(sMessage);

					Integer iPositiveSucceded = 0;

					iPositiveSucceded = oAPI.sendMailDirect(sUserId, oMessage);

					Utils.debugLog("StylesResource.shareStyle: notification sent with result " + iPositiveSucceded);
				}
			} catch (Exception oEx) {
				Utils.debugLog("StylesResource.shareStyle: notification exception " + oEx.toString());
			}
		} catch (Exception oEx) {
			Utils.debugLog("StylesResource.shareStyle: " + oEx);

			oResult.setStringValue("Error in save process");
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
		Utils.debugLog("StylesResource.deleteUserSharedStyle( ProcId: " + sStyleId + ", User:" + sUserId + " )");

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		try {
			// Validate Session
			User oOwnerUser = Wasdi.getUserFromSession(sSessionId);

			if (oOwnerUser == null) {
				Utils.debugLog("StylesResource.deleteUserSharedStyle( Session: " + sSessionId + ", ProcId: " + sStyleId + ", User:" + sUserId + " ): invalid session");
				oResult.setStringValue("Invalid session");
				return oResult;
			}

			if (Utils.isNullOrEmpty(oOwnerUser.getUserId())) {
				oResult.setStringValue("Invalid user.");
				return oResult;
			}

			if (Utils.isNullOrEmpty(sUserId)) {
				oResult.setStringValue("Invalid shared user.");
				return oResult;
			}

			try {
				StyleSharingRepository oStyleSharingRepository = new StyleSharingRepository();

				StyleSharing oStyleShare = oStyleSharingRepository.getStyleSharingByUserIdStyleId(sUserId, sStyleId);

				if (oStyleShare != null) {
					// if the user making the call is the one on the sharing OR
					if (oStyleShare.getUserId().equals(oOwnerUser.getUserId()) ||
							// if the user making the call is the owner of the style
							oStyleShare.getOwnerId().equals(oOwnerUser.getUserId())) {
						// Delete the sharing
						oStyleSharingRepository.deleteByUserIdStyleId(sUserId, sStyleId);
					} else {
						oResult.setStringValue("Unauthorized");
						return oResult;
					}
				} else {
					oResult.setStringValue("Sharing not found");
					return oResult;
				}
			} catch (Exception oEx) {
				Utils.debugLog("StylesResource.deleteUserSharedStyle: " + oEx);
				oResult.setStringValue("Error deleting processor sharing");
				return oResult;
			}

			oResult.setStringValue("Done");
			oResult.setBoolValue(true);
		} catch (Exception oE) {
			Utils.debugLog("StylesResource.deleteUserSharedStyle( Session: " + sSessionId + ", ProcId: " + sStyleId + ", User:" + sUserId + " ): " + oE);
		}

		return oResult;
	}

	@GET
	@Path("share/bystyle")
	@Produces({"application/xml", "application/json", "text/xml"})
	public List<StyleSharingViewModel> getEnableUsersSharedStyle(@HeaderParam("x-session-token") String sSessionId, @QueryParam("styleId") String sStyleId) {
		List<StyleSharingViewModel> oResult = new ArrayList<>();

		Utils.debugLog("StylesResource.getEnableUsersSharedStyle(  Style : " + sStyleId + " )");

		// Validate Session
		User oAskingUser = Wasdi.getUserFromSession(sSessionId);

		if (oAskingUser == null) {
			Utils.debugLog("StylesResource.getEnableUsersSharedStyle( Session: " + sSessionId + ", Style: " + sStyleId + "): invalid session");

			return oResult;
		}

		if (Utils.isNullOrEmpty(oAskingUser.getUserId())) {
			return oResult;
		}

		try {
			// Check if the processor exists and is of the user calling this API
			StyleRepository oStyleRepository = new StyleRepository();
			Style oValidateStyle = oStyleRepository.getStyle(sStyleId);

			if (oValidateStyle == null) {
				// return
				Utils.debugLog("StylesResource.getEnableUsersSharedStyle: Style not found");
				// if something went wrong returns an empty set
				return oResult;
			}

			//Retrieve and returns the sharings
			StyleSharingRepository oStyleSharingRepository = new StyleSharingRepository();
			oStyleSharingRepository.getStyleSharingByStyle(sStyleId).forEach(element -> {
				oResult.add(new StyleSharingViewModel(element));
			});

			return oResult;
		} catch (Exception oEx) {
			Utils.debugLog("StylesResource.getEnableUsersSharedStyle: " + oEx);
			return oResult;
		}
	}

	@GET
	@Path("download")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response download(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("token") String sTokenSessionId,
			@QueryParam("styleId") String sStyleId) {
		Utils.debugLog("StyleResource.download( StyleId: " + sStyleId + " )");

		try {
			if (!Utils.isNullOrEmpty(sSessionId)) {
				sTokenSessionId = sSessionId;
			}

			User oUser = Wasdi.getUserFromSession(sTokenSessionId);

			if (oUser == null) {
				Utils.debugLog("StylesResource.download( Session: " + sSessionId + ", StyleId: " + sStyleId + " ): invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			StyleRepository oStyleRepository = new StyleRepository();
			Style oStyle = oStyleRepository.getStyle(sStyleId);

			ResponseBuilder oResponseBuilder = null;

			if (oStyle == null) {
				Utils.debugLog("StylesResource.download( Session: " + sSessionId + ", StyleId: " + sStyleId + " ): Style Id not found on DB");
				oResponseBuilder = Response.noContent();
				return oResponseBuilder.build();
			}

			// Take path
			String sDownloadRootPath = Wasdi.getDownloadPath();
			String sStyleSldPath = sDownloadRootPath + "styles/" + oStyle.getName() + ".sld";

			File oFile = new File(sStyleSldPath);

			if (!oFile.exists()) {
				Utils.debugLog("StylesResource.download: file does not exists " + oFile.getPath());
				oResponseBuilder = Response.serverError();
			} else {
				Utils.debugLog("StylesResource.download: returning file " + oFile.getPath());

				FileStreamingOutput oStream;
				oStream = new FileStreamingOutput(oFile);

				oResponseBuilder = Response.ok(oStream);
				oResponseBuilder.header("Content-Disposition", "attachment; filename=" + oStyle.getName() + ".sld");
				oResponseBuilder.header("Content-Length", Long.toString(oFile.length()));
			}

			return oResponseBuilder.build();
				
		} catch (Exception oEx) {
			Utils.debugLog("StylesResource.download: " + oEx);
		}

		return Response.status(404).build();
	}

}
