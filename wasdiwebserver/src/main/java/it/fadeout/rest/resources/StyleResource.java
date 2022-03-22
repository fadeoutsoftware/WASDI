package it.fadeout.rest.resources;

import static wasdi.shared.utils.WasdiFileUtils.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.sql.Timestamp;
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

import org.glassfish.jersey.media.multipart.FormDataParam;

import it.fadeout.Wasdi;
import it.fadeout.mercurius.business.Message;
import it.fadeout.mercurius.client.MercuriusAPI;
import it.fadeout.rest.resources.largeFileDownload.FileStreamingOutput;
import it.fadeout.threads.styles.StyleAddFileWorker;
import it.fadeout.threads.styles.StyleDeleteFileWorker;
import it.fadeout.threads.styles.StyleUpdateFileWorker;
import wasdi.shared.business.Node;
import wasdi.shared.business.Style;
import wasdi.shared.business.StyleSharing;
import wasdi.shared.business.User;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.StyleRepository;
import wasdi.shared.data.StyleSharingRepository;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.geoserver.GeoServerManager;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.ZipFileUtils;
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
		Utils.debugLog("StyleResource.uploadFile( Name: " + sName + ", Descr: " + sDescription + ", Public: " + bPublic + " )");

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		try {
			// Session checking
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser == null) {
				Utils.debugLog("StyleResource.uploadFile( Session: " + sSessionId + ", Style Name: " + sName + " ): invalid session");
				oResult.setStringValue("Invalid session");
				return oResult;
			}

			String sUserId = oUser.getUserId();
			
			// Check the uniqueness of the name
			if (isStyleNameTaken(sName)) {
				Utils.debugLog("StyleResource.uploadFile( Session: " + sSessionId + ", Style Name: " + sName + " ): name already used");
				oResult.setStringValue("The style's name is already used.");
				return oResult;
			}

			// File checking

			// Checks whether null file is passed
			if (fileInputStream == null) {
				Utils.debugLog("StyleResource.uploadFile( Session: " + sSessionId + ", Style Name: " + sName + " ): invalid file");
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

			Utils.debugLog("StyleResource.uploadFile: style file Path: " + oStyleSldFile.getPath());

			// save uploaded file
			writeFile(fileInputStream, oStyleSldFile);

			try (FileReader oFileReader = new FileReader(oStyleSldFile)) {
				insertStyle(sUserId, sStyleId, sName, sDescription, oStyleSldFile.getPath(), bPublic);
			} catch (Exception oEx) {
				Utils.debugLog("StyleResource.uploadFile: " + oEx);
				oResult.setStringValue("Error saving the style.");
				return oResult;
			}

			//computational-node-side work
//			if (Wasdi.s_sMyNodeCode.equals("wasdi")) {
//				computationalNodesAddStyle(sSessionId, sName, sDescription, bPublic, oStyleSldFile.getPath());
//			}

			//geoserver-side work
			geoServerAddStyle(oStyleSldFile.getPath());
		} catch (Exception oEx2) {
			Utils.debugLog("StyleResource.uploadFile: " + oEx2);
		}

		oResult.setBoolValue(true);
		return oResult;
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

	@POST
	@Path("/updatefile")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response updateFile(@FormDataParam("file") InputStream oFileInputStream,
			@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("styleId") String sStyleId,
			@QueryParam("zipped") Boolean bZipped) {
		Utils.debugLog("StyleResource.updateFile( InputStream, StyleId: " + sStyleId);

		try {
			if (Utils.isNullOrEmpty(sStyleId) || sStyleId.contains("\\") || sStyleId.contains("/")) {

				Utils.debugLog("StyleResource.updateFile( oInputStreamForFile, " + sSessionId + ", " + sStyleId + " ): invalid styleId, aborting");
				return Response.status(Status.BAD_REQUEST).build();
			}


			// Session checking
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser == null) {
				Utils.debugLog("StyleResource.updateFile( Session: " + sSessionId + ", Style: " + sStyleId + " ): invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}


			// File checking
			// Checks whether null file is passed
			if (oFileInputStream == null) {
				Utils.debugLog("StyleResource.updateFile( Session: " + sSessionId + ", Style: " + sStyleId + " ): invalid file");
				return Response.status(400).build();
			}


			// DB-side work
			StyleRepository oStyleRepository = new StyleRepository();
			Style oStyle = oStyleRepository.getStyle(sStyleId);

			if (oStyle == null) {
				Utils.debugLog("StyleResource.updateFile: error in styleId " + sStyleId + " not found on DB");
				return Response.notModified("StyleId not found, please check parameters").build();
			}

			// Checks that user can modify the style
			StyleSharingRepository oStyleSharingRepository = new StyleSharingRepository();

			if (!oUser.getUserId().equals(oStyle.getUserId()) && !oStyleSharingRepository.isSharedWithUser(oUser.getUserId(), oStyle.getStyleId())) {
				Utils.debugLog("StyleResource.updateFile: User " + oUser.getUserId() + " doesn't have rights on style " + oStyle.getName());
				return Response.status(Status.UNAUTHORIZED).build();
			}


			// filesystem-side work

			// Get Download Path
			String sDownloadRootPath = Wasdi.getDownloadPath();

			String sDirectoryPathname = sDownloadRootPath + "styles/";

			createDirectoryIfDoesNotExist(sDirectoryPathname);
			
			// original sld file
			File oStyleSldFile = new File(sDownloadRootPath + "styles/" + oStyle.getName() + ".sld");
			
			if (!oStyleSldFile.exists()) {
				Utils.debugLog("StyleResource.updateFile: style file " + oStyle.getName() + ".sld does not exists in node. Exit");
				return Response.status(404).build();
			}
			
			if (bZipped==null) {
				bZipped = false;
			}
			
			String sTempFileName = sDownloadRootPath + "styles/" + oStyle.getName() + ".sld.temp";
			
			if (bZipped) {
				Utils.debugLog("StyleResource.updateFile: file is zipped");
				sTempFileName = sTempFileName.replace(".temp", ".zip");
			}

			// new sld file
			File oStyleSldFileTemp = new File(sTempFileName);

			//rename the ".temp" file
			// save uploaded file in ".temp" format

			try {
				writeFile(oFileInputStream, oStyleSldFileTemp);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if (bZipped) {
				
				Utils.debugLog("StyleResource.updateFile: unzip the style");
				ZipFileUtils oExtractor = new ZipFileUtils();
				oExtractor.unzip(sTempFileName, sDownloadRootPath+"styles/");
			}
			else {
				// checks that the Style Xml field is valid
				try {
					// Overwrite the old file
					Files.write(oStyleSldFile.toPath(), Files.readAllBytes(oStyleSldFileTemp.toPath()));

					// Delete the temp file
					Files.delete(oStyleSldFileTemp.toPath());
					
					Utils.debugLog("StyleResource.updateFile: style files updated! styleID - " + oStyle.getStyleId());
				} catch (Exception oEx) {
					if (oStyleSldFileTemp.exists())
						oStyleSldFileTemp.delete();

					Utils.debugLog("StyleResource.updateFile: " + oEx);
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
			Utils.debugLog("StyleResource.updateFile: " + oEx2);
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
		Utils.debugLog("StyleResource.getXML( Style Id : " + sStyleId + ");");

		String sXml = "";

		try {
			// Session checking
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser == null) {
				Utils.debugLog("StyleResource.getXML( Session: " + sSessionId + ", Style: " + sStyleId + " ): invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			// Check that the style exists on db
			StyleRepository oStyleRepository = new StyleRepository();
			Style oStyle = oStyleRepository.getStyle(sStyleId);

			if (oStyle == null) {
				Utils.debugLog("StyleResource.getXML: error in styleId " + sStyleId + " not found on DB");
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
			@FormDataParam("styleXml") String sStyleXml) {
		Utils.debugLog("StyleResource.updateXML: StyleId " + sStyleId + " invoke StyleResource.updateXml");

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
		Utils.debugLog("StyleResource.updateParams( StyleId: " + sStyleId);

		// Session checking
		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			Utils.debugLog("StyleResource.updateParams( Session: " + sSessionId + ", Style: " + sStyleId + " ): invalid session");
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
		Utils.debugLog("StyleResource.getStylesByUser");

		// Session checking
		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			Utils.debugLog("StyleResource.getStylesByUser( Session: " + sSessionId + " ): invalid session");
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
			Utils.debugLog("StyleResource.getStylesByUser( " + sSessionId + " ): " + oE);
		}

		Utils.debugLog("StyleResource.getStylesByUser: return " + aoRetStyles.size() + " styles");

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
		Utils.debugLog("StyleResource.deleteStyle( Style: " + sStyleId + " )");

		// This API is allowed ONLY on main nodes
		if (!Wasdi.s_sMyNodeCode.equals("wasdi")) {
			Utils.debugLog("StyleResource.DeleteStyle: this is a computational node, cannot call this API here");

			// if the flow of execution is interrupted at this time
			// this functionality will not work on local environments
//			return Response.status(Status.BAD_REQUEST).build();
		}

		try {
			// Session checking
			User oUser = Wasdi.getUserFromSession(sSessionId);

			// Check the user
			if (oUser == null) {
				Utils.debugLog("StyleResource.deleteStyle( Session: " + sSessionId + ", Style: " + sStyleId + " ): invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			String sUserId = oUser.getUserId();


			// DB-side work
			StyleRepository oStyleRepository = new StyleRepository();
			Style oStyle = oStyleRepository.getStyle(sStyleId);

			if (oStyle == null) {
				Utils.debugLog("StyleResource.deleteStyle: unable to find style " + sStyleId);
				return Response.serverError().build();
			}

			if (!oStyle.getUserId().equals(sUserId)) {
				Utils.debugLog("StyleResource.deleteStyle: style not of user " + oUser.getUserId());
				StyleSharingRepository oStyleSharingRepository = new StyleSharingRepository();

				if (oStyleSharingRepository.isSharedWithUser(sUserId, sStyleId)) {
					Utils.debugLog("StyleResource.deleteStyle: the style wasd shared with " + oUser.getUserId() + ", delete the sharing");
					oStyleSharingRepository.deleteByUserIdStyleId(oUser.getUserId(), sStyleId);

					return Response.ok().build();
				}

				return Response.status(Status.UNAUTHORIZED).build();
			}

			// Delete sharings
			StyleSharingRepository oStyleSharingRepository = new StyleSharingRepository();
			oStyleSharingRepository.deleteByStyleId(sStyleId);

			oStyleRepository.deleteStyle(sStyleId);


			if (Wasdi.s_sMyNodeCode.equals("wasdi")) {
				// computational-node-side work
				computationalNodesDeleteStyle(sSessionId, oStyle.getStyleId(), oStyle.getName());
			}
			
			// filesystem-side work
			filesystemDeleteStyleIfExists(oStyle.getName());
			
			// geoserver-side work
			geoServerRemoveStyleIfExists(oStyle.getName());

			// Trigger the style delete operation on this specific node
			Utils.debugLog("StyleResource.deleteStyle: Style Deleted");

			return Response.ok().build();
		} catch (Exception oEx) {
			Utils.debugLog("StyleResource.deleteStyle: " + oEx);
			return Response.serverError().build();
		}
	}

	@DELETE
	@Path("/nodedelete")
	public Response nodeDeleteStyle(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("styleId") String sStyleId,
			@QueryParam("styleName") String sStyleName) {
		Utils.debugLog("StyleResource.nodeDeleteStyle( Session: " + sSessionId + ", Style: " + sStyleName + " )");

		// This API is allowed ONLY on computed nodes
		if (Wasdi.s_sMyNodeCode.equals("wasdi")) {
			Utils.debugLog("StyleResource.nodeDeleteStyle: this is the main node, cannot call this API here");
			return Response.status(Status.BAD_REQUEST).build();
		}

		try {
			User oUser = Wasdi.getUserFromSession(sSessionId);

			// Check user
			if (oUser == null) {
				Utils.debugLog("StyleResource.nodeDeleteStyle( Session: " + sSessionId + ", Style: " + sStyleName + " ): invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			// Trigger the style delete operation on this specific node
			Utils.debugLog("StyleResource.nodeDeleteStyle: this is a computing node, just execute Delete here");
			
			filesystemDeleteStyleIfExists(sStyleName);

			geoServerRemoveStyleIfExists(sStyleName);

			return Response.ok().build();
		} catch (Exception oEx) {
			Utils.debugLog("ProcessorResource.nodeDeleteProcessor: " + oEx);
			return Response.serverError().build();
		}
	}

	private void filesystemDeleteStyleIfExists(String sName) {
		Utils.debugLog("StyleResource.filesystemDeleteStyleIfExists( " + "Name: " + sName + " )");

		if (Utils.isNullOrEmpty(sName)) {
			Utils.debugLog("StyleResource.filesystemDeleteStyleIfExists: Name is null or empty.");
			return;
		}

		// Get Download Path on the current WASDI instance
		String sBasePath = Wasdi.getDownloadPath();
		sBasePath += "styles/";
		String sStyleFilePath = sBasePath + sName + ".sld";

		File oStyleFile = new File(sStyleFilePath);
		if (oStyleFile.exists()) {
			if (!oStyleFile.delete()) {
				Utils.debugLog("StyleResource.filesystemDeleteStyleIfExists: Error deleting the style file " + sStyleFilePath);
			}
		}
	}

	private void geoServerAddStyle(String sStyleFilePath) throws MalformedURLException {
		Utils.debugLog("StyleResource.geoServerAddStyle( " + "StyleFile: " + sStyleFilePath + " )");

		GeoServerManager oGeoServerManager = new GeoServerManager();

		if (sStyleFilePath != null) {
			oGeoServerManager.publishStyle(sStyleFilePath);
		}
	}

	private void geoServerUpdateStyleIfExists(String sName, String sStyleFilePath) throws MalformedURLException {
		Utils.debugLog("StyleResource.geoServerUpdateStyleIfExists( " + "Name: " + sName + ", StyleFile: " + sStyleFilePath + " )");

		GeoServerManager oGeoServerManager = new GeoServerManager();

		if (oGeoServerManager.styleExists(sName)) {
			Utils.debugLog("StyleResource.geoServerUpdateStyleIfExists: style exists, call remove style");
			if (!oGeoServerManager.removeStyle(sStyleFilePath)) {
				Utils.debugLog("StyleResource.geoServerUpdateStyleIfExists: remove style returned false!!");
			}
		}

		if (sStyleFilePath != null) {
			Utils.debugLog("StyleResource.geoServerUpdateStyleIfExists: call publish style");
			oGeoServerManager.publishStyle(sStyleFilePath);
		}
	}

	private void geoServerRemoveStyleIfExists(String sName) throws MalformedURLException {
		Utils.debugLog("StyleResource.geoServerRemoveStyleIfExists( " + "Name: " + sName + " )");

		GeoServerManager oGeoServerManager = new GeoServerManager();

		if (oGeoServerManager.styleExists(sName)) {
			String sStyleFilePath = Wasdi.getDownloadPath() + "styles/" + sName;
			oGeoServerManager.removeStyle(sStyleFilePath);
		}
	}

	private void computationalNodesUpdateStyle(String sSessionId, String sName, String sFilePath) {
		// In the main node: start a thread to update all the computing nodes
		try {
			Utils.debugLog("StyleResource.computationalNodesUpdateStyle: this is the main node, starting Worker to update computing nodes");

			NodeRepository oNodeRepo = new NodeRepository();
			List<Node> aoNodes = oNodeRepo.getNodesList();

			//This is the main node: forward the request to other nodes
			StyleUpdateFileWorker oUpdateWorker = new StyleUpdateFileWorker(aoNodes, sSessionId, sName, sFilePath);

			oUpdateWorker.start();

			Utils.debugLog("StyleResource.computationalNodesUpdateStyle: Worker started");
		} catch (Exception oEx) {
			Utils.debugLog("StyleResource.computationalNodesUpdateStyle: error starting UpdateWorker " + oEx.toString());
		}
	}

	private void computationalNodesDeleteStyle(String sSessionId, String sStyleId, String sStyleName) {
		// Start a thread to update all the computing nodes
		try {
			Utils.debugLog("StyleResource.computationalNodesDeleteStyle: this is the main node, starting Worker to delete Style also on computing nodes");

			NodeRepository oNodeRepo = new NodeRepository();
			List<Node> aoNodes = oNodeRepo.getNodesList();

			// Util to call the API on computing nodes
			StyleDeleteFileWorker oDeleteWorker = new StyleDeleteFileWorker(aoNodes, sSessionId, sStyleId, sStyleName);
			oDeleteWorker.start();

			Utils.debugLog("StyleResource.computationalNodesDeleteStyle: Worker started");						
		} catch (Exception oEx) {
			Utils.debugLog("StyleResource.computationalNodesDeleteStyle: error starting UpdateWorker " + oEx.getMessage());
		}
	}

	@PUT
	@Path("share/add")
	@Produces({"application/xml", "application/json", "text/xml"})
	public PrimitiveResult shareStyle(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("styleId") String sStyleId, @QueryParam("userId") String sUserId) {
		Utils.debugLog("StyleResource.shareStyle(  Style : " + sStyleId + ", User: " + sUserId + " )");

		//init repositories
		StyleSharingRepository oStyleSharingRepository = new StyleSharingRepository();
		StyleRepository oStyleRepository = new StyleRepository();

		// Validate Session
		User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		if (oRequesterUser == null) {
			Utils.debugLog("StyleResource.shareStyle( Session: " + sSessionId + ", Style: " + sStyleId + ", User: " + sUserId + " ): invalid session");
			oResult.setStringValue("Invalid session.");
			return oResult;
		}

		try {
			// Check if the style exists and is of the user calling this API
			Style oStyle = oStyleRepository.getStyle(sStyleId);

			if (oStyle == null) {
				oResult.setStringValue("Invalid Style");
				return oResult;
			}

			if (oRequesterUser.getUserId().equals(sUserId)) {
				Utils.debugLog("StyleResource.ShareStyle: auto sharing not so smart");
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

			Utils.debugLog("StyleResource.shareStyle: Style" + sStyleId + " Shared from " + oRequesterUser.getUserId() + " to " + sUserId);

			try {
				String sMercuriusAPIAddress = WasdiConfig.Current.notifications.mercuriusAPIAddress;

				if (Utils.isNullOrEmpty(sMercuriusAPIAddress)) {
					Utils.debugLog("StyleResource.shareStyle: sMercuriusAPIAddress is null");
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

					Utils.debugLog("StyleResource.shareStyle: notification sent with result " + iPositiveSucceded);
				}
			} catch (Exception oEx) {
				Utils.debugLog("StyleResource.shareStyle: notification exception " + oEx.toString());
			}
		} catch (Exception oEx) {
			Utils.debugLog("StyleResource.shareStyle: " + oEx);

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
		Utils.debugLog("StyleResource.deleteUserSharedStyle( ProcId: " + sStyleId + ", User:" + sUserId + " )");

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		try {
			// Validate Session
			User oOwnerUser = Wasdi.getUserFromSession(sSessionId);

			if (oOwnerUser == null) {
				Utils.debugLog("StyleResource.deleteUserSharedStyle( Session: " + sSessionId + ", ProcId: " + sStyleId + ", User:" + sUserId + " ): invalid session");
				oResult.setStringValue("Invalid session");
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
				Utils.debugLog("StyleResource.deleteUserSharedStyle: " + oEx);
				oResult.setStringValue("Error deleting style sharing");
				return oResult;
			}

			oResult.setStringValue("Done");
			oResult.setBoolValue(true);
		} catch (Exception oE) {
			Utils.debugLog("StyleResource.deleteUserSharedStyle( Session: " + sSessionId + ", ProcId: " + sStyleId + ", User:" + sUserId + " ): " + oE);
		}

		return oResult;
	}

	@GET
	@Path("share/bystyle")
	@Produces({"application/xml", "application/json", "text/xml"})
	public List<StyleSharingViewModel> getEnableUsersSharedStyle(@HeaderParam("x-session-token") String sSessionId, @QueryParam("styleId") String sStyleId) {
		List<StyleSharingViewModel> oResult = new ArrayList<>();

		Utils.debugLog("StyleResource.getEnableUsersSharedStyle(  Style : " + sStyleId + " )");

		// Validate Session
		User oAskingUser = Wasdi.getUserFromSession(sSessionId);

		if (oAskingUser == null) {
			Utils.debugLog("StyleResource.getEnableUsersSharedStyle( Session: " + sSessionId + ", Style: " + sStyleId + "): invalid session");

			return oResult;
		}

		try {
			// Check if the style exists and is of the user calling this API
			StyleRepository oStyleRepository = new StyleRepository();
			Style oValidateStyle = oStyleRepository.getStyle(sStyleId);

			if (oValidateStyle == null) {
				// return
				Utils.debugLog("StyleResource.getEnableUsersSharedStyle: Style not found");
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
			Utils.debugLog("StyleResource.getEnableUsersSharedStyle: " + oEx);
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
				Utils.debugLog("StyleResource.download( Session: " + sSessionId + ", StyleId: " + sStyleId + " ): invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			StyleRepository oStyleRepository = new StyleRepository();
			Style oStyle = oStyleRepository.getStyle(sStyleId);

			ResponseBuilder oResponseBuilder = null;

			if (oStyle == null) {
				Utils.debugLog("StyleResource.download( Session: " + sSessionId + ", StyleId: " + sStyleId + " ): Style Id not found on DB");
				oResponseBuilder = Response.noContent();
				return oResponseBuilder.build();
			}

			// Take path
			String sDownloadRootPath = Wasdi.getDownloadPath();
			String sStyleSldPath = sDownloadRootPath + "styles/" + sStyleId + ".sld";

			File oFile = new File(sStyleSldPath);

			if (!oFile.exists()) {
				Utils.debugLog("StyleResource.download: file does not exists " + oFile.getPath());
				oResponseBuilder = Response.serverError();
			} else {
				Utils.debugLog("StyleResource.download: returning file " + oFile.getPath());

				FileStreamingOutput oStream;
				oStream = new FileStreamingOutput(oFile);

				oResponseBuilder = Response.ok(oStream);
				oResponseBuilder.header("Content-Disposition", "attachment; filename=" + oStyle.getName() + ".sld");
				oResponseBuilder.header("Content-Length", Long.toString(oFile.length()));
			}

			return oResponseBuilder.build();
				
		} catch (Exception oEx) {
			Utils.debugLog("StyleResource.download: " + oEx);
		}

		return Response.status(404).build();
	}

}
