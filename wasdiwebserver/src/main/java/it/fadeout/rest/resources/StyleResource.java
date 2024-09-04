package it.fadeout.rest.resources;

import static wasdi.shared.utils.WasdiFileUtils.createDirectoryIfDoesNotExist;
import static wasdi.shared.utils.WasdiFileUtils.writeFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
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
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition.FormDataContentDispositionBuilder;
import org.glassfish.jersey.media.multipart.FormDataParam;

import it.fadeout.Wasdi;
import it.fadeout.rest.resources.largeFileDownload.FileStreamingOutput;
import it.fadeout.threads.styles.StyleDeleteFileWorker;
import it.fadeout.threads.styles.StyleUpdateFileWorker;
import wasdi.shared.business.ImagesCollections;
import wasdi.shared.business.Node;
import wasdi.shared.business.Style;
import wasdi.shared.business.users.ResourceTypes;
import wasdi.shared.business.users.User;
import wasdi.shared.business.users.UserAccessRights;
import wasdi.shared.business.users.UserApplicationRole;
import wasdi.shared.business.users.UserResourcePermission;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.StyleRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.geoserver.GeoServerManager;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.MailUtils;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.styles.StyleSharingViewModel;
import wasdi.shared.viewmodels.styles.StyleViewModel;

/**
 * Style Resource
 * Hosts the API to let the user upload and manage SLD styles in WASDI
 * Styles are saved in a WASDI Path that can be obtained using PathsConfig.getStylesPath()
 * 
 * 	.Upload Style SLD XML
 * 	.Edit, Delete Styles
 * 	.Share Styles with other users
 * 	.Download styles
 * 
 * @author p.campanella
 *
 */
@Path("/styles")
public class StyleResource {

	/**
	 * Uploads a SLD Style in WASDI
	 * 
	 * @param oFileInputStream Stream of the SLD file
	 * @param sSessionId Session Id
	 * @param sName Name to assign to the style
	 * @param sDescription Description to assign to the style
	 * @param bPublic True to set the style public
	 * @return Primitive result with bool flag and string value
	 */
	@POST
	@Path("/uploadfile")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFile(@FormDataParam("file") InputStream oFileInputStream,
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
				WasdiLog.warnLog("StyleResource.uploadFile: invalid session");
				oResult.setStringValue("Invalid session");
				return Response.status(Status.UNAUTHORIZED).entity(oResult).build();
			}

			String sUserId = oUser.getUserId();
			
			// Check the uniqueness of the name
			if (isStyleNameTaken(sName)) {
				WasdiLog.warnLog("StyleResource.uploadFile: name already used");
				oResult.setStringValue("The style's name is already used.");
				return Response.status(Status.BAD_REQUEST).entity(oResult).build();
			}

			// File checking

			// Checks whether null file is passed
			if (oFileInputStream == null) {
				WasdiLog.warnLog("StyleResource.uploadFile: invalid file");
				oResult.setStringValue("Invalid file");
				return Response.status(Status.BAD_REQUEST).entity(oResult).build();
			}

			// filesystem-side work

			// Get Download Path
			String sDirectoryPathname = PathsConfig.getStylesPath();

			createDirectoryIfDoesNotExist(sDirectoryPathname);

			// Generate Style Id and file
			String sStyleId = UUID.randomUUID().toString();

			String sFilePathname = sDirectoryPathname + sName + ".sld";
			File oStyleSldFile = new File(sFilePathname);

			WasdiLog.debugLog("StyleResource.uploadFile: style file Path: " + oStyleSldFile.getPath());

			// save uploaded file
			writeFile(oFileInputStream, oStyleSldFile);

			try (FileReader oFileReader = new FileReader(oStyleSldFile)) {
				insertStyle(sUserId, sStyleId, sName, sDescription, oStyleSldFile.getPath(), bPublic);
			} 
			catch (Exception oEx) {
				WasdiLog.errorLog("StyleResource.uploadFile: " + oEx);
				oResult.setStringValue("Error saving the style.");
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(oResult).build();
			}

			//geoserver-side work
			geoServerAddStyle(oStyleSldFile.getPath());
			
			updateStylePreview(sName, sSessionId);
						
			oResult.setBoolValue(true);
			return Response.ok().entity(oResult).build();
			
		} catch (Exception oEx2) {
			WasdiLog.errorLog("StyleResource.uploadFile: " + oEx2);
			oResult.setBoolValue(true);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(oResult).build();
			
		}

    }

	/**
	 * Updates a SLD file
	 * @param oFileInputStream stream of the updated file
	 * @param sSessionId Session Id
	 * @param sStyleId Style Id 
	 * @param bZipped True if the file is zipped
	 * @return http ok or other status
	 */
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

				WasdiLog.warnLog("StyleResource.updateFile: invalid styleId, aborting");
				return Response.status(Status.BAD_REQUEST).build();
			}


			// Session checking
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser == null) {
				WasdiLog.warnLog("StyleResource.updateFile: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}


			// File checking
			// Checks whether null file is passed
			if (oFileInputStream == null) {
				WasdiLog.warnLog("StyleResource.updateFile: invalid file");
				return Response.status(Status.BAD_REQUEST).build();
			}

			// DB-side work
			StyleRepository oStyleRepository = new StyleRepository();
			Style oStyle = oStyleRepository.getStyle(sStyleId);

			if (oStyle == null) {
				WasdiLog.warnLog("StyleResource.updateFile: style not found on DB");
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			if (!PermissionsUtils.canUserWriteStyle(oUser.getUserId(), sStyleId)) {
				WasdiLog.warnLog("StyleResource.updateFile: User doesn't have rights to write the style");
				return Response.status(Status.FORBIDDEN).build();
			}

			// filesystem-side work

			// Get Download Path
			String sDirectoryPathname = PathsConfig.getStylesPath() + "styles/";

			createDirectoryIfDoesNotExist(sDirectoryPathname);
			
			// original sld file
			File oStyleSldFile = new File(PathsConfig.getStylesPath()+ oStyle.getName() + ".sld");
			
			if (!oStyleSldFile.exists()) {
				GeoServerManager oManager = new GeoServerManager();
				
				if (oManager.styleExists(oStyle.getName()) == false) {
					WasdiLog.warnLog("StyleResource.updateFile: style file " + oStyle.getName() + ".sld does not exists in node. Exit");
					return Response.status(404).build();					
				}
			}
			
			if (bZipped==null) {
				bZipped = false;
			}
			
			String sTempFileName = PathsConfig.getStylesPath() + oStyle.getName() + ".sld.temp";
			
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
				oExtractor.unzip(sTempFileName, PathsConfig.getStylesPath());
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
					if (oStyleSldFileTemp.exists()) {
						boolean bIsDeleted = oStyleSldFileTemp.delete();
						WasdiLog.debugLog("StyleResource.updateFile. Result of the deletion of the style file: " + bIsDeleted);
					}

					WasdiLog.errorLog("StyleResource.updateFile: ", oEx);
					return Response.status(Status.NOT_MODIFIED).build();
				}
			}

			//computational-node-side work
			if (WasdiConfig.Current.isMainNode()) {
				computationalNodesUpdateStyle(sSessionId, sStyleId, oStyleSldFile.getPath());
			}

			//geoserver-side work
			geoServerUpdateStyleIfExists(oStyle.getName(), oStyleSldFile.getPath());
			
			updateStylePreview(oStyle.getName(), sSessionId);
		} catch (Exception oEx2) {
			WasdiLog.errorLog("StyleResource.updateFile: " + oEx2);
			return Response.serverError().build();
		}

		return Response.ok().build();
	}

	/**
	 * Get the XML content of a Style
	 * @param sSessionId Session Id
	 * @param sStyleId Style Id
	 * @return XML content of the SLD
	 */
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
				WasdiLog.warnLog("StyleResource.getXML: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			if (!PermissionsUtils.canUserAccessStyle(oUser.getUserId(), sStyleId)) {
				WasdiLog.warnLog("StyleResource.getXML: user cannot access style");
				return Response.status(Status.FORBIDDEN).build();				
			}

			// Check that the style exists on db
			StyleRepository oStyleRepository = new StyleRepository();
			Style oStyle = oStyleRepository.getStyle(sStyleId);

			if (oStyle == null) {
				WasdiLog.warnLog("StyleResource.getXML: style not found on DB");
				return Response.notModified("StyleId not found").build();
			}

			// Get Download Path
			File oStyleFile = new File(PathsConfig.getStylesPath() + oStyle.getName() + ".sld");

			if (!oStyleFile.exists()) {
				WasdiLog.warnLog("StyleResource.getXML: style file not found;");
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

	/**
	 * Updates the style using the text xml
	 * @param sSessionId Session Id
	 * @param sStyleId Style Id
	 * @param sStyleXml Updated XML (SLD)
	 * @return http response
	 */
	@POST
	@Path("/updatexml")
	public Response updateXML(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("styleId") String sStyleId,
			@FormDataParam("styleXml") String sStyleXml) {
		WasdiLog.debugLog("StyleResource.updateXML: StyleId " + sStyleId + " invoke StyleResource.updateFile");

		// convert string to file and invoke updateGraphFile
		return updateFile(new ByteArrayInputStream(sStyleXml.getBytes(Charset.forName("UTF-8"))), sSessionId, sStyleId, false);
	}

	/**
	 * Update the attributies of a style
	 * @param sSessionId Session Id
	 * @param sStyleId Style Id
	 * @param sDescription Description 
	 * @param bPublic True to set it public
	 * @return http response
	 */
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
			WasdiLog.warnLog("StyleResource.updateParams: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		if (!PermissionsUtils.canUserWriteStyle(oUser.getUserId(), sStyleId)) {
			WasdiLog.warnLog("StyleResource.updateParams: user cannot write the style");
			return Response.status(Status.FORBIDDEN).build();				
		}		

		StyleRepository oStyleRepository = new StyleRepository();
		Style oStyle = oStyleRepository.getStyle(sStyleId);

		if (oStyle == null) {
			WasdiLog.errorLog("StyleResource.updateParams: style not found");
			return Response.status(Status.BAD_REQUEST).build();
		}
			

		oStyle.setDescription(sDescription);
		oStyle.setIsPublic(bPublic);
		oStyleRepository.updateStyle(oStyle);

		return Response.ok().build();
	}
	
	/**
	 * Get the link to access the style image
	 * @param sImageName
	 * @param sSessionId
	 * @return
	 */
	protected String getStyleImageLink(String sImageName, String sSessionId) {
		//https://test.wasdi.net/wasdiwebserver/rest/images/get?collection=processors&folder=hellowasdi&name=logo.jpg&token=32f32b02-7c34-41e8-9990-ef003031a4ed
		String sImageLink = WasdiConfig.Current.baseUrl+"images/get?collection=" + ImagesCollections.STYLES.getFolder()+"&name=" + sImageName + "&token="+sSessionId;
		return sImageLink;
	}

	@GET
	@Path("/getbyuser")
	public List<StyleViewModel> getStylesByUser(@HeaderParam("x-session-token") String sSessionId) {
		WasdiLog.debugLog("StyleResource.getStylesByUser");

		// Session checking
		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("StyleResource.getStylesByUser: invalid session");
			return null;
		}

		String sUserId = oUser.getUserId();

		StyleRepository oStyleRepository = new StyleRepository();
		List<StyleViewModel> aoRetStyles = new ArrayList<>();

		try {
			
			ImagesResource oImageResource = new ImagesResource();
			
			// Here we take the list of all the users' styles + the public ones
			List<Style> aoDbStyles = oStyleRepository.getStylePublicAndByUser(sUserId);
			
			for (Style oCurrentStyle : aoDbStyles) {
				// Convert the style in view model
				StyleViewModel oStyleViewModel = StyleViewModel.getFromStyle(oCurrentStyle);
				
				// Are we the owner?
				if (oCurrentStyle.getUserId().equals(oUser.getUserId())) {
					// Yes: not shared, our own, not read only
					oStyleViewModel.setSharedWithMe(false);
					oStyleViewModel.setReadOnly(false);
				}
				else {
					// For now lets assume is read only
					oStyleViewModel.setReadOnly(true);
				}
				
				String sImageName = oStyleViewModel.getName() + ".png";
				
				Response oResponse = oImageResource.existsImage(sSessionId, sSessionId, ImagesCollections.STYLES.getFolder(), "", sImageName);
				
				if (oResponse.getStatus() == 200) {
					oStyleViewModel.setImgLink(getStyleImageLink(sImageName, sSessionId));					
				}
				
				aoRetStyles.add(oStyleViewModel);
			}

			// find sharings by userId
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			List<UserResourcePermission> aoStyleSharing = oUserResourcePermissionRepository.getStyleSharingsByUserId(sUserId);

			// For all the shared styles
			for (UserResourcePermission oSharing : aoStyleSharing) {
				
				// Create the View Model of the style
				Style oSharedWithMe = oStyleRepository.getStyle(oSharing.getResourceId());
				StyleViewModel oStyleViewModel = StyleViewModel.getFromStyle(oSharedWithMe);

				if (!oStyleViewModel.isPublic()) {
					// This is shared and not public: add to return list
					oStyleViewModel.setSharedWithMe(true);
					// Keep if read only or not
					oStyleViewModel.setReadOnly(!oSharing.canWrite());
					
					String sImageName = oStyleViewModel.getName() + ".png";
					
					Response oResponse = oImageResource.existsImage(sSessionId, sSessionId, ImagesCollections.STYLES.getFolder(), "", sImageName);
					
					if (oResponse.getStatus() == 200) {
						oStyleViewModel.setImgLink(getStyleImageLink(sImageName, sSessionId));					
					}
					
					aoRetStyles.add(oStyleViewModel);
					
				} 
				else {
					// This is shared but public, so this must be already in our return list
					for (StyleViewModel oStyle : aoRetStyles) {
						// Find it and set shared flag = true
						if (oSharedWithMe.getStyleId().equals(oStyle.getStyleId())) {
							// So is shared and we can correct if read only ro not
							oStyle.setSharedWithMe(true);
							oStyleViewModel.setReadOnly(!oSharing.canWrite());
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
		if (!WasdiConfig.Current.isMainNode()) {
			WasdiLog.warnLog("StyleResource.DeleteStyle: this is a computational node, cannot call this API here");
			return Response.status(Status.BAD_REQUEST).build();
		}

		try {
			// Session checking
			User oUser = Wasdi.getUserFromSession(sSessionId);

			// Check the user
			if (oUser == null) {
				WasdiLog.warnLog("StyleResource.deleteStyle: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			String sUserId = oUser.getUserId();

			// DB-side work
			StyleRepository oStyleRepository = new StyleRepository();
			Style oStyle = oStyleRepository.getStyle(sStyleId);

			if (oStyle == null) {
				WasdiLog.warnLog("StyleResource.deleteStyle: unable to find style");
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			if (!PermissionsUtils.canUserAccessStyle(sUserId, sStyleId)) {
				WasdiLog.warnLog("StyleResource.deleteStyle: user cannot access the style");
				return Response.status(Status.FORBIDDEN).build();
			}

			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

			if (oUserResourcePermissionRepository.isStyleSharedWithUser(sUserId, sStyleId)) {
				WasdiLog.debugLog("StyleResource.deleteStyle: the style was shared with " + oUser.getUserId() + ", delete the sharing");
				oUserResourcePermissionRepository.deletePermissionsByUserIdAndStyleId(oUser.getUserId(), sStyleId);

				return Response.ok().build();
			}
			else {
				// Delete sharings
				oUserResourcePermissionRepository.deletePermissionsByStyleId(sStyleId);

				oStyleRepository.deleteStyle(sStyleId);


				if (WasdiConfig.Current.isMainNode()) {
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
		if (WasdiConfig.Current.isMainNode()) {
			WasdiLog.warnLog("StyleResource.nodeDeleteStyle: this is the main node, cannot call this API here");
			return Response.status(Status.BAD_REQUEST).build();
		}

		try {
			User oUser = Wasdi.getUserFromSession(sSessionId);

			// Check user
			if (oUser == null) {
				WasdiLog.warnLog("StyleResource.nodeDeleteStyle: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			if (!PermissionsUtils.canUserWriteStyle(oUser.getUserId(), sStyleId)) {
				WasdiLog.warnLog("StyleResource.nodeDeleteStyle: user cannot write style");
				return Response.status(Status.FORBIDDEN).build();
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
			@QueryParam("styleId") String sStyleId, @QueryParam("userId") String sUserId, @QueryParam("rights") String sRights) {
		WasdiLog.debugLog("StyleResource.shareStyle(  Style : " + sStyleId + ", User: " + sUserId + " )");

		//init repositories
		UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
		StyleRepository oStyleRepository = new StyleRepository();

		// Validate Session
		User oRequestingUser = Wasdi.getUserFromSession(sSessionId);
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		if (oRequestingUser == null) {
			WasdiLog.warnLog("StyleResource.shareStyle: invalid session");
			oResult.setStringValue("Invalid session.");
			return oResult;
		}
		
		// Use Read By default
		if (!UserAccessRights.isValidAccessRight(sRights)) {
			sRights = UserAccessRights.READ.getAccessRight();
		}

		try {
			// Check if the style exists and is of the user calling this API
			Style oStyle = oStyleRepository.getStyle(sStyleId);

			if (oStyle == null) {
				WasdiLog.warnLog("StyleResource.shareStyle: invalid style");
				oResult.setStringValue("Invalid Style");
				return oResult;
			}

			if (oRequestingUser.getUserId().equals(sUserId) && !UserApplicationRole.isAdmin(oRequestingUser)) {
				WasdiLog.warnLog("StyleResource.shareStyle: auto sharing not so smart");
				oResult.setStringValue("Impossible to autoshare.");
				return oResult;
			}

			// Check the destination user
			UserRepository oUserRepository = new UserRepository();
			User oDestinationUser = oUserRepository.getUser(sUserId);
			
			// Can't find destination user for the sharing
			if (oDestinationUser == null) {
				WasdiLog.warnLog("StyleResource.shareStyle: invalid target user");
				oResult.setStringValue("Can't find target user of the sharing");
				return oResult;
			}
			
			if (!PermissionsUtils.canUserWriteStyle(oRequestingUser.getUserId(), sStyleId) && !UserApplicationRole.isAdmin(oRequestingUser)) {
				WasdiLog.warnLog("StyleResource.shareStyle: requesting user cannot write the style");
				oResult.setStringValue("Can't write this style");
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
						&& !UserApplicationRole.isAdmin(oRequestingUser)) {
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
			UserResourcePermission oStyleSharing = new UserResourcePermission(ResourceTypes.STYLE.getResourceType(), sStyleId, sUserId, oStyle.getUserId(), oRequestingUser.getUserId(), sRights);

			oUserResourcePermissionRepository.insertPermission(oStyleSharing);

			WasdiLog.debugLog("StyleResource.shareStyle: Style" + sStyleId + " Shared from " + oRequestingUser.getUserId() + " to " + sUserId);

			try {
				String sTitle = "Style " + oStyle.getName() + " Shared";
				String sMessage = "The user " + oRequestingUser.getUserId() + " shared with you the Style: " + oStyle.getName();
				MailUtils.sendEmail(WasdiConfig.Current.notifications.sftpManagementMailSender, sUserId, sTitle, sMessage);
				
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
				WasdiLog.warnLog("StyleResource.deleteUserSharedStyle: invalid session");
				oResult.setStringValue("Invalid session");
				return oResult;
			}

			try {
				UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
				UserResourcePermission oStyleShare = oUserResourcePermissionRepository.getStyleSharingByUserIdAndStyleId(sUserId, sStyleId);
				
				if (oStyleShare == null) {
					WasdiLog.warnLog("StyleResource.deleteUserSharedStyle: invalid sharing");
					oResult.setStringValue("Sharing not found");
					return oResult;					
				}

				if (!PermissionsUtils.canUserAccessStyle(oOwnerUser.getUserId(), sStyleId)) {
					WasdiLog.warnLog("StyleResource.deleteUserSharedStyle: user cannot access style");
					oResult.setStringValue("Cannot access the style");
					return oResult;					
				}
				
				// if the user making the call is the one on the sharing OR
				if (oStyleShare.getUserId().equals(oOwnerUser.getUserId()) ||
						// if the user making the call is the owner of the style OR
						oStyleShare.getOwnerId().equals(oOwnerUser.getUserId())
						// if the user has ADMIN rights
						|| UserApplicationRole.isAdmin(oOwnerUser)) {
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
	public List<StyleSharingViewModel> getEnabledUsersSharedStyle(@HeaderParam("x-session-token") String sSessionId, @QueryParam("styleId") String sStyleId) {
		List<StyleSharingViewModel> oResult = new ArrayList<>();

		WasdiLog.debugLog("StyleResource.getEnabledUsersSharedStyle(  Style : " + sStyleId + " )");

		// Validate Session
		User oAskingUser = Wasdi.getUserFromSession(sSessionId);

		if (oAskingUser == null) {
			WasdiLog.warnLog("StyleResource.getEnabledUsersSharedStyle: invalid session");
			return oResult;
		}

		try {
			// Check if the style exists and is of the user calling this API
			StyleRepository oStyleRepository = new StyleRepository();
			Style oValidateStyle = oStyleRepository.getStyle(sStyleId);

			if (oValidateStyle == null) {
				// return
				WasdiLog.warnLog("StyleResource.getEnabledUsersSharedStyle: Style not found");
				// if something went wrong returns an empty set
				return oResult;
			}

			//Retrieve and returns the sharings
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			oUserResourcePermissionRepository.getStyleSharingsByStyleId(sStyleId).forEach(oElement -> {
				oResult.add(new StyleSharingViewModel(oElement));
			});

			return oResult;
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog("StyleResource.getEnabledUsersSharedStyle error: " + oEx);
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
				WasdiLog.warnLog("StyleResource.download: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			StyleRepository oStyleRepository = new StyleRepository();
			Style oStyle = oStyleRepository.getStyle(sStyleId);

			return internalDownload(oUser, oStyle);
				
		} catch (Exception oEx) {
			WasdiLog.errorLog("StyleResource.download error: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@GET
	@Path("downloadbyname")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadByName(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("token") String sTokenSessionId,
			@QueryParam("style") String sStyle) {
		WasdiLog.debugLog("StyleResource.downloadByName( StyleId: " + sStyle + " )");

		try {
			if (!Utils.isNullOrEmpty(sSessionId)) {
				sTokenSessionId = sSessionId;
			}

			User oUser = Wasdi.getUserFromSession(sTokenSessionId);

			if (oUser == null) {
				WasdiLog.warnLog("StyleResource.downloadByName: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			StyleRepository oStyleRepository = new StyleRepository();
			Style oStyle = oStyleRepository.getStyleByName(sStyle);

			return internalDownload(oUser, oStyle);
				
		} catch (Exception oEx) {
			WasdiLog.errorLog("StyleResource.downloadByName error: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}	
	
	/**
	 * Internal Download operation, starting from user and style
	 * @param oUser User requesting the download
	 * @param oStyle Style entity
	 * @return http response
	 */
	public Response internalDownload(User oUser, Style oStyle) {

		try {

			if (oUser == null) {
				WasdiLog.warnLog("StyleResource.internalDownload: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			ResponseBuilder oResponseBuilder = null;

			if (oStyle == null) {
				WasdiLog.warnLog("StyleResource.internalDownload: Style Id not found on DB");
				oResponseBuilder = Response.noContent();
				return oResponseBuilder.build();
			}
			
			//check the user can access the style
			if (!PermissionsUtils.canUserAccessStyle(oUser.getUserId(), oStyle.getStyleId())) {
				WasdiLog.warnLog("StyleResource.internalDownload: user cannot access style");
				return Response.status(Status.FORBIDDEN).build();
			}			

			// Take path
			String sStyleSldPath = PathsConfig.getStylesPath() + oStyle.getName() + ".sld";

			File oFile = new File(sStyleSldPath);

			if (!oFile.exists()) {
				WasdiLog.warnLog("StyleResource.internalDownload: file does not exists " + oFile.getPath());
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
			WasdiLog.errorLog("StyleResource.internalDownload error: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}	

	/**
	 * Check if the name of the style is already in use or not
	 */
	private boolean isStyleNameTaken(String sName) {
		StyleRepository oStyleRepository = new StyleRepository();
		return oStyleRepository.isStyleNameTaken(sName);
	}

	/**
	 * Inserts a Style entity in the db
	 * @param sUserId User
	 * @param sStyleId Style Id
	 * @param sName Style Name
	 * @param sDescription Style description
	 * @param sFilePath File Path of the sld
	 * @param bPublic Flag to know if it is public or not
	 */
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
	
	/**
	 * Deletes the sld file from the server
	 * @param sName Name of the style
	 */
	private void filesystemDeleteStyleIfExists(String sName) {
		WasdiLog.debugLog("StyleResource.filesystemDeleteStyleIfExists( " + "Name: " + sName + " )");

		if (Utils.isNullOrEmpty(sName)) {
			WasdiLog.debugLog("StyleResource.filesystemDeleteStyleIfExists: Name is null or empty.");
			return;
		}

		// Get Download Path on the current WASDI instance
		String sStyleFilePath = PathsConfig.getStylesPath() + sName + ".sld";

		File oStyleFile = new File(sStyleFilePath);
		if (oStyleFile.exists()) {
			if (!oStyleFile.delete()) {
				WasdiLog.debugLog("StyleResource.filesystemDeleteStyleIfExists: Error deleting the style file " + sStyleFilePath);
			}
		}
	}

	/**
	 * Adds a style in geoserver
	 * @param sStyleFilePath Path of the sld file
	 * @throws MalformedURLException
	 */
	private void geoServerAddStyle(String sStyleFilePath) throws MalformedURLException {
		WasdiLog.debugLog("StyleResource.geoServerAddStyle( " + "StyleFile: " + sStyleFilePath + " )");

		GeoServerManager oGeoServerManager = new GeoServerManager();

		if (sStyleFilePath != null) {
			oGeoServerManager.publishStyle(sStyleFilePath);
		}
	}
	
	/**
	 * Updates a style in geoserver (if it has been published before)
	 * @param sName Name of the style
	 * @param sStyleFilePath path of the sld file
	 * @throws MalformedURLException
	 */
	private void geoServerUpdateStyleIfExists(String sName, String sStyleFilePath) throws MalformedURLException {
		WasdiLog.debugLog("StyleResource.geoServerUpdateStyleIfExists( " + "Name: " + sName + ", StyleFile: " + sStyleFilePath + " )");
		
		try {
			GeoServerManager oGeoServerManager = new GeoServerManager();

			if (oGeoServerManager.styleExists(sName)) {
				WasdiLog.debugLog("StyleResource.geoServerUpdateStyleIfExists: style exists, update it");
				
				
				if (!oGeoServerManager.updateStyle(sName, sStyleFilePath)) {
					WasdiLog.debugLog("StyleResource.geoServerUpdateStyleIfExists: update style returned false!!");
				}
			}
			else {
				
				WasdiLog.debugLog("StyleResource.geoServerUpdateStyleIfExists: style does not exists");
				
				if (WasdiConfig.Current.isMainNode()) {
					WasdiLog.debugLog("StyleResource.geoServerUpdateStyleIfExists: this is the main server, we need the style! Adding it");
					geoServerAddStyle(sStyleFilePath);
				}
			}			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("StyleResource.geoServerUpdateStyleIfExists: error " + oEx.toString());
		}

	}
	
	/**
	 * Removes a style from geoserver if it exists
	 * @param sName Name of the style
	 * @throws MalformedURLException
	 */
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
			
			
			String sStyleFilePath = PathsConfig.getStylesPath() + sName + ".sld";
			
			WasdiLog.debugLog("StyleResource.geoServerRemoveStyleIfExists: remove the style " + sStyleFilePath);
			
			oGeoServerManager.removeStyle(sName);
		}
	}

	/**
	 * Triggers the update of the style in all the computational nodes
	 * @param sSessionId User Session Id
	 * @param sName Name of the style 
	 * @param sFilePath Path of the sld
	 */
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
 
	/**
	 * Triggers the delete of a style in the computational nodes
	 * @param sSessionId Session Id
	 * @param sStyleId Style Id
	 * @param sStyleName Name of the style
	 */
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
	
	/**
	 * Create the style preview from geoserver and uploads it in the WASDI images collection
	 * @param sName Style Name
	 * @param sSessionId User Session Id
	 */
	protected void updateStylePreview(String sName, String sSessionId) {
		
		
		try {		
			// Now try to get the preview			
			HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(WasdiConfig.Current.geoserver.address + "/wms?request=GetLegendGraphic&STYLE=" + sName + "&format=image/png&WIDTH=12&HEIGHT=12&LAYER=" + WasdiConfig.Current.geoserver.defaultLayerToGetStyleImages + "&legend_options=fontAntiAliasing:true;fontSize:10&LEGEND_OPTIONS=forceRule:True");
	
			if (oHttpCallResponse.getResponseCode()==200) {
				WasdiLog.debugLog("StyleResource.updateStylePreview: wms call done, creating image");
				ByteArrayInputStream oByteArrayInputStream = new ByteArrayInputStream(oHttpCallResponse.getResponseBytes());
				ImagesResource oImagesResource = new ImagesResource();
				
				String sImageName = sName + ".png";
				FormDataContentDispositionBuilder oFormDataContentDispositionBuilder = FormDataContentDisposition.name(sImageName);
				
				
				oImagesResource.uploadImage(oByteArrayInputStream, oFormDataContentDispositionBuilder.fileName(sImageName).build(), sSessionId, ImagesCollections.STYLES.getFolder(), "", sImageName, true, true);
			}
			else {
				WasdiLog.errorLog("StyleResource.updateStylePreview: the WMS request returned " + oHttpCallResponse.getResponseCode());
				WasdiLog.errorLog("StyleResource.updateStylePreview: Message " + oHttpCallResponse.getResponseBody());
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("StyleResource.updateStylePreview: error " + oEx.getMessage());
		}
		
	}
}
