package it.fadeout.rest.resources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.media.multipart.FormDataParam;

import it.fadeout.Wasdi;
import it.fadeout.rest.resources.largeFileDownload.FileStreamingOutput;
import it.fadeout.services.ProvidersCatalog;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.DataProvider;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.ProductWorkspace;
import wasdi.shared.business.PublishedBand;
import wasdi.shared.business.User;
import wasdi.shared.business.Workspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.ProductWorkspaceRepository;
import wasdi.shared.data.PublishedBandsRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.parameters.DownloadFileParameter;
import wasdi.shared.parameters.PublishBandParameter;
import wasdi.shared.parameters.ShareFileParameter;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.RabbitMessageViewModel;
import wasdi.shared.viewmodels.processors.ImageImportViewModel;
import wasdi.shared.viewmodels.products.PublishBandResultViewModel;


/**
 * File Buffer Resource.
 * Hosts API for:
 * 	.import new files in WASDI from data providers
 *  .publish bands on geoserver
 *  .handle geoserver styles
 *  
 * @author p.campanella
 *
 */
@Path("/filebuffer")
public class FileBufferResource {
		
	/**
	 * Providers Catalogue
	 */
	@Inject
	ProvidersCatalog m_oDataProviderCatalog;

	/**
	 * Trigger a sharing of an image (from one workspace to another) in WASDI.
	 * The method checks the input, create the parameter and call WASDI.runProcess
	 * 
	 * @param sSessionId User Session
	 * @param sFileUrl Url of the file to import
	 * @param sProvider Data Provider
	 * @param sWorkspaceId Workspace Id
	 * @param sBoundingBox if available, bbox of the product to import
	 * @param sParentProcessWorkspaceId Proc Id of the Parent Process
	 * @return
	 * @throws IOException
	 */
	@GET
	@Path("share")
	@Produces({"application/xml", "application/json", "text/xml"})
	public PrimitiveResult share(@HeaderParam("x-session-token") String sSessionId,
									@QueryParam("originWorkspaceId") String sOriginWorkspaceId,
									@QueryParam("destinationWorkspaceId") String sDestinationWorkspaceId,
									@QueryParam("productName") String sProductName,
									@QueryParam("parent") String sParentProcessWorkspaceId)
			throws IOException {
		WasdiLog.debugLog("FileBufferResource.share, sOriginWorkspaceId: " + sOriginWorkspaceId + "sDestinationWorkspaceId: " + sDestinationWorkspaceId + "sProductName: " + sProductName);

		// find the origin workspace. make sure it is valid. 
		// find the destination workspace. make sure it is valid
		// make sure that the origin workspace contains the product
		// make sure that the destination workspace does not already contain a product with the same name
		// copy the file (local copy or http download)
		// duplicate the record/document on the productworkspace table/collection
		// duplicate the record/document on the downloadedfiles table/collection
		// duplicate the record/document on the processworkspace table/collection
		// duplicate the record/document on the publishedbands table/collection

		PrimitiveResult oResult = new PrimitiveResult();

		oResult.setBoolValue(false);
		oResult.setIntValue(500);
		
		try {
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser == null) {
				WasdiLog.debugLog("FileBufferResource.share: invalid session");
				oResult.setIntValue(Status.UNAUTHORIZED.getStatusCode());
				return oResult;
			}

			String sUserId = oUser.getUserId();

			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();


			if (Utils.isNullOrEmpty(sOriginWorkspaceId)) {
				WasdiLog.debugLog("FileBufferResource.share: invalid origin workspace");
				oResult.setStringValue("Invalid origin workspace.");

				return oResult;
			}

			if (sOriginWorkspaceId.contains("/") || sOriginWorkspaceId.contains("\\")) {
				WasdiLog.debugLog("FileBufferResource.share: Injection attempt from users");
				oResult.setStringValue("Invalid origin workspace.");

				return oResult;
			}
			
			//check the user can access the workspace
			if (!PermissionsUtils.canUserAccessWorkspace(sUserId, sOriginWorkspaceId)) {
				WasdiLog.debugLog("FileBufferResource.share: the user cannot access the origin ws");
				oResult.setIntValue(Status.FORBIDDEN.getStatusCode());
				oResult.setStringValue("Invalid origin workspace.");

				return oResult;
			}			

			Workspace oOriginWorkspace = oWorkspaceRepository.getWorkspace(sOriginWorkspaceId);

			if (oOriginWorkspace == null) {
				WasdiLog.debugLog("FileBufferResource.share: invalid origin workspace");
				oResult.setStringValue("Invalid origin workspace.");

				return oResult;
			}

			String sOriginWorkspaceNode = oOriginWorkspace.getNodeCode();
			String sOriginWorkspaceUserId = oOriginWorkspace.getUserId();


			if (Utils.isNullOrEmpty(sDestinationWorkspaceId)) {
				WasdiLog.debugLog("FileBufferResource.share: invalid destination workspace");
				oResult.setStringValue("Invalid destination workspace.");

				return oResult;
			}

			if (sDestinationWorkspaceId.contains("/") || sDestinationWorkspaceId.contains("\\")) {
				WasdiLog.debugLog("FileBufferResource.share: Injection attempt from users");
				oResult.setStringValue("Invalid destination workspace.");

				return oResult;
			}
			
			//check the user can access the workspace
			if (!PermissionsUtils.canUserAccessWorkspace(sUserId, sDestinationWorkspaceId)) {
				WasdiLog.debugLog("FileBufferResource.share: the user cannot access the destination ws");
				oResult.setIntValue(Status.FORBIDDEN.getStatusCode());
				oResult.setStringValue("Invalid destination workspace.");

				return oResult;
			}					

			Workspace oDestinationWorkspace = oWorkspaceRepository.getWorkspace(sDestinationWorkspaceId);

			if (oDestinationWorkspace == null) {
				WasdiLog.debugLog("FileBufferResource.share: invalid destination workspace");
				oResult.setStringValue("Invalid destination workspace.");

				return oResult;
			}

			String sDestinationWorkspaceNode = oDestinationWorkspace.getNodeCode();
			String sDestinationWorkspaceUserId = oDestinationWorkspace.getUserId();



			ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();

			if (Utils.isNullOrEmpty(sProductName)) {
				WasdiLog.debugLog("FileBufferResource.share: invalid product name");
				oResult.setStringValue("Invalid product name.");

				return oResult;
			}

			if (sProductName.contains("/") || sProductName.contains("\\")) {
				WasdiLog.debugLog("FileBufferResource.share( Product Name: " + sProductName + " ): Injection attempt from users");
				oResult.setStringValue("Invalid product name.");

				return oResult;
			}

			String sOriginFilePath = WasdiFileUtils.fixPathSeparator(WasdiConfig.Current.paths.downloadRootPath + sOriginWorkspaceUserId + "/" + sOriginWorkspaceId + "/" + sProductName);
			ProductWorkspace oProductOriginWorkspace = oProductWorkspaceRepository.getProductWorkspace(sOriginFilePath, sOriginWorkspaceId);

			if (oProductOriginWorkspace == null) {
				WasdiLog.debugLog("FileBufferResource.share: invalid product name");
				oResult.setStringValue("Invalid product name.");

				return oResult;
			}

			String sDestinationFilePath = WasdiFileUtils.fixPathSeparator(WasdiConfig.Current.paths.downloadRootPath + sDestinationWorkspaceUserId + "/" + sDestinationWorkspaceId + "/" + sProductName);
			ProductWorkspace oProductDestinationWorkspace = oProductWorkspaceRepository.getProductWorkspace(sProductName, sDestinationWorkspaceId);

			if (oProductDestinationWorkspace != null) {
				WasdiLog.debugLog("FileBufferResource.share: product already present in the destination workspace");
				oResult.setStringValue("The product is already present in the destination workspace.");

				return oResult;
			}

			

			DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();

			List<DownloadedFile> aoDownloadedFileList = oDownloadedFilesRepository.getDownloadedFileListByName(sProductName);

			if (aoDownloadedFileList == null || aoDownloadedFileList.isEmpty()) {
				WasdiLog.debugLog("FileBufferResource.share: invalid product name");
				oResult.setStringValue("Invalid product name.");

				return oResult;
			}

			boolean bIsProductInOriginWorkspace = false;
			boolean bIsProductInDestinationWorkspace = false;

//			String sOriginFilePath = null;
			String sBoundingBox = null;

			for (DownloadedFile oDownloadedFile : aoDownloadedFileList) {
				if (oDownloadedFile.getFilePath().contains(sOriginWorkspaceId)) {
//					sOriginFilePath = oDownloadedFile.getFilePath();
					sBoundingBox = oDownloadedFile.getBoundingBox();
					bIsProductInOriginWorkspace = true;
				}

				if (oDownloadedFile.getFilePath().contains(sDestinationWorkspaceId)) {
					bIsProductInDestinationWorkspace = true;
				}
			}

			if (!bIsProductInOriginWorkspace) {
				WasdiLog.debugLog("FileBufferResource.share: invalid product name");
				oResult.setStringValue("Invalid product name.");

				return oResult;
			}

			if (bIsProductInDestinationWorkspace) {
				WasdiLog.debugLog("FileBufferResource.share: product already present in the destination workspace");
				oResult.setStringValue("The product is already present in the destination workspace.");

				return oResult;
			}


			String sProcessObjId = Utils.getRandomName();

//			DataProvider oProvider = m_oDataProviderCatalog.getDefaultProvider(Wasdi.s_sMyNodeCode);
//			String sProvider = oProvider.getName();

//			String sDestinationFilePath = sOriginFilePath
//					.replace(sOriginWorkspaceUserId, sDestinationWorkspaceUserId)
//					.replace(sOriginWorkspaceId, sDestinationWorkspaceId);

			ShareFileParameter oParameter = new ShareFileParameter();

//			oParameter.setQueue(sSessionId);
//			oParameter.setUrl(sFileUrl);
			oParameter.setProductName(sProductName);
			oParameter.setOriginFilePath(sOriginFilePath);
			oParameter.setWorkspace(sDestinationWorkspaceId);
			oParameter.setDestinationWorkspaceNode(sDestinationWorkspaceNode);
			oParameter.setDestinationFilePath(sDestinationFilePath);
			oParameter.setOriginWorkspaceId(sOriginWorkspaceId);
			oParameter.setOriginWorkspaceNode(sOriginWorkspaceNode);
			oParameter.setUserId(sUserId);
			oParameter.setExchange(sDestinationWorkspaceId);
			oParameter.setBoundingBox(sBoundingBox);
//			oParameter.setDownloadUser(oProvider.getOSUser());
//			oParameter.setDownloadPassword(oProvider.getOSPassword());
//			oParameter.setProvider(oProvider.getName());
			oParameter.setWorkspaceOwnerId(sDestinationWorkspaceUserId);
			//set the process object Id to params
			oParameter.setProcessObjId(sProcessObjId);

			String sPath = WasdiConfig.Current.paths.serializationPath;

			return Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.SHARE.name(), /*sProvider.toUpperCase(),*/ sProductName, sPath, oParameter, sParentProcessWorkspaceId);
		} catch (Exception oEx) {
			WasdiLog.errorLog("FileBufferResource.share: " + oEx);
		}

		return oResult;
	}
	
	/**
	 * Trigger a new import of an image in WASDI.
	 * The method has been deprecated to use the POST Version.
	 * This method is for retro compatibility and just calls the POST Version
	 * 
	 * @param sSessionId User Session
	 * @param sFileUrl Url of the file to import
	 * @param sProvider Data Provider
	 * @param sWorkspaceId Workspace Id
	 * @param sBoundingBox if available, bbox of the product to import
	 * @param sParentProcessWorkspaceId Proc Id of the Parent Process
	 * @return
	 * @throws IOException
	 */
	@GET
	@Path("download")
	@Produces({"application/xml", "application/json", "text/xml"})
	public PrimitiveResult download(@HeaderParam("x-session-token") String sSessionId,
									@QueryParam("fileUrl") String sFileUrl,
									@QueryParam("name") String sFileName,
									@QueryParam("provider") String sProvider,
									@QueryParam("workspace") String sWorkspaceId,
									@QueryParam("bbox") String sBoundingBox,
									@QueryParam("parent") String sParentProcessWorkspaceId)
			throws IOException
	{
		WasdiLog.debugLog("FileBufferResource.download, session: " + sSessionId + " fileName: " + sFileName);

		ImageImportViewModel oImageImportViewModel = new ImageImportViewModel();

		oImageImportViewModel.setFileUrl(sFileUrl);
		oImageImportViewModel.setName(sFileName);
		oImageImportViewModel.setProvider(sProvider);
		oImageImportViewModel.setWorkspace(sWorkspaceId);
		oImageImportViewModel.setBbox(sBoundingBox);
		oImageImportViewModel.setParent(sParentProcessWorkspaceId);

		return this.imageImport(sSessionId, oImageImportViewModel);
	}

	/**
	 * Trigger a new import of an image in WASDI.
	 * The method checks the input, create the parameter and call WASDI.runProcess
	 * 
	 * @param sSessionId User Session
	 * @param oImageImportViewModel the details of the request
	 * @return a primitive result containing the outcome of the operation
	 * @throws IOException in case of IO issues
	 */
	@POST
	@Path("download")
	@Produces({"application/xml", "application/json", "text/xml"})
	public PrimitiveResult imageImport(@HeaderParam("x-session-token") String sSessionId, ImageImportViewModel oImageImportViewModel)
			throws IOException {
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		try {
			
			WasdiLog.debugLog("FileBufferResource.imageImport, session: " + sSessionId);
			
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				WasdiLog.debugLog("FileBufferResource.imageImport: session is not valid");
				oResult.setIntValue(401);
				return oResult;
			}
			
			if (!PermissionsUtils.userHasValidSubscription(oUser)) {
				WasdiLog.debugLog("FileBufferResource.imageImport: No valid Subscription");
				oResult.setIntValue(401);
				return oResult;				
			}

			String sUserId = oUser.getUserId();
			
			String sProcessObjId = Utils.getRandomName();

			if (oImageImportViewModel == null) {
				WasdiLog.debugLog("FileBufferResource.imageImport: request is not valid");
				oResult.setIntValue(401);
				return oResult;
			}

			String sFileUrl = null;
			if (oImageImportViewModel.getFileUrl() != null) {
				try {
					sFileUrl = java.net.URLDecoder.decode(oImageImportViewModel.getFileUrl(), StandardCharsets.UTF_8.name());
				} catch (UnsupportedEncodingException e) {
					WasdiLog.errorLog("FileBufferResource.imageImport excepion decoding the fileUrl");
				}
			}

			String sFileName = oImageImportViewModel.getName();
			String sProvider = oImageImportViewModel.getProvider();
			String sWorkspaceId = oImageImportViewModel.getWorkspace();
			String sBoundingBox = oImageImportViewModel.getBbox();
			String sParentProcessWorkspaceId = oImageImportViewModel.getParent();
			
			//check the user can access the workspace
			if (!PermissionsUtils.canUserAccessWorkspace(sUserId, sWorkspaceId)) {
				WasdiLog.debugLog("FileBufferResource.imageImport: user cannot access workspace");
				oResult.setIntValue(Status.FORBIDDEN.getStatusCode());
				return oResult;
			}

			// if the provider is not specified, we fallback on the node default provider
			DataProvider oProvider = null;
			if (Utils.isNullOrEmpty(sProvider)) {
				oProvider = m_oDataProviderCatalog.getDefaultProvider(Wasdi.s_sMyNodeCode);
			} else {
				oProvider = m_oDataProviderCatalog.getProvider(sProvider);
			}

			WasdiLog.debugLog("FileBufferResource.imageImport, provider: " + oProvider.getName());

			DownloadFileParameter oParameter = new DownloadFileParameter();
			oParameter.setQueue(sSessionId);
			oParameter.setUrl(sFileUrl);
			oParameter.setName(sFileName);
			oParameter.setWorkspace(sWorkspaceId);
			oParameter.setUserId(sUserId);
			oParameter.setExchange(sWorkspaceId);
			oParameter.setBoundingBox(sBoundingBox);
			oParameter.setDownloadUser(oProvider.getOSUser());
			oParameter.setDownloadPassword(oProvider.getOSPassword());
			oParameter.setProvider(oProvider.getName());
			oParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));
			//set the process object Id to params
			oParameter.setProcessObjId(sProcessObjId);

			String sPath = WasdiConfig.Current.paths.serializationPath;
			
			return Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.DOWNLOAD.name(), sProvider.toUpperCase(), sFileName, sPath, oParameter, sParentProcessWorkspaceId);
			
		} catch (IOException oEx) {
			WasdiLog.errorLog("DownloadResource.imageImport: Error updating process list " + oEx);
		} catch (Exception oEx) {
			WasdiLog.errorLog("DownloadResource.imageImport: Error updating process list " + oEx);
		}
		
		oResult.setIntValue(500);
		return oResult;
	}

	/**
	 * Publish band: if the band had already been published, it returns immediatly the View Model required by rabbit.
	 * If it is not, it creates the parameter and call Wasdi.runProcess to publish the band
	 * 
	 * @param sSessionId User Session
	 * @param sFileUrl file name
	 * @param sWorkspaceId Workspace id
	 * @param sBand band to publish
	 * @param sStyle default style
	 * @param sParentProcessWorkspaceId Proc Id of the parent process
	 * @return Rabbit Message View Model: empty if the band must be published. If it is alredy published, the same view model that the launcher send to the client after the publish is done
	 * @throws IOException
	 */
	@GET
	@Path("publishband")
	@Produces({"application/xml", "application/json", "text/xml"})
	public RabbitMessageViewModel publishBand(	@HeaderParam("x-session-token") String sSessionId,
												@QueryParam("fileUrl") String sFileUrl,
												@QueryParam("workspace") String sWorkspaceId,
												@QueryParam("band") String sBand,
												@QueryParam("style") String sStyle, 
												@QueryParam("parent") String sParentProcessWorkspaceId) throws IOException {
		RabbitMessageViewModel oReturnValue = null;
		try {
			
			WasdiLog.debugLog("FileBufferResource.publishBand");
			
			// Check Authentication
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if (oUser==null) {
				WasdiLog.debugLog("FileBufferResource.publishBand: invalid session"); 
				return oReturnValue;
			}
			
			String sUserId = oUser.getUserId();
			
			//check the user can access the workspace
			if (!PermissionsUtils.canUserAccessWorkspace(sUserId, sWorkspaceId)) {
				WasdiLog.debugLog("FileBufferResource.PublishBand: user cannot access workspace");
				return oReturnValue;
			}
			
			// Get the full product path
			String sFullProductPath = Wasdi.getWorkspacePath(Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId);
			
			// Get the product
			DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
			DownloadedFile oDownloadedFile = oDownloadedFilesRepository.getDownloadedFileByPath(sFullProductPath+sFileUrl);

            if (oDownloadedFile == null) {
            	oDownloadedFile = oDownloadedFilesRepository.getDownloadedFileByPath(WasdiFileUtils.fixPathSeparator(sFullProductPath) + sFileUrl);
            }
			
			if (oDownloadedFile == null) {
				WasdiLog.debugLog("FileBufferResource.PublishBand: cannot find downloaded file, return");
				return oReturnValue;
			}
			
			// If the style is not set, look if there is a default one in the downloaded file
			if (Utils.isNullOrEmpty(sStyle)) {
				if (!Utils.isNullOrEmpty(oDownloadedFile.getDefaultStyle())) {
					sStyle = oDownloadedFile.getDefaultStyle();
				}
			}
			
			// Is there this publish band?
			PublishedBandsRepository oPublishedBandsRepository = new PublishedBandsRepository();
			
			PublishedBand oPublishBand = oPublishedBandsRepository.getPublishedBand(oDownloadedFile.getFilePath(), sBand);
			
			oReturnValue = new RabbitMessageViewModel();			

			if (oPublishBand != null)
			{
				String sProductName = Utils.getFileNameWithoutLastExtension(oPublishBand.getProductName());
				WasdiLog.debugLog("FileBufferResource.publishBand: band already published: product Name : " + sProductName + " LayerId: " + oPublishBand.getLayerId());
				PublishBandResultViewModel oPublishBandResultViewModel = new PublishBandResultViewModel();
				oPublishBandResultViewModel.setBoundingBox(oPublishBand.getBoundingBox());
				oPublishBandResultViewModel.setBandName(oPublishBand.getBandName());
				oPublishBandResultViewModel.setLayerId(oPublishBand.getLayerId());
				
				if (oDownloadedFile.getProductViewModel() != null) {
					oPublishBandResultViewModel.setProductName(oDownloadedFile.getProductViewModel().getName());
				}
				else {
					oPublishBandResultViewModel.setProductName(sProductName);
				}
				
				oPublishBandResultViewModel.setGeoserverBoundingBox(oPublishBand.getGeoserverBoundingBox());
				oPublishBandResultViewModel.setGeoserverUrl(oPublishBand.getGeoserverUrl());
				oReturnValue.setMessageCode(LauncherOperations.PUBLISHBAND.name());
				oReturnValue.setPayload(oPublishBandResultViewModel);
				
				WasdiLog.debugLog("FileBufferResource.publishBand: return published band, user: " + sUserId );
				return oReturnValue;
			}
			
			String sProcessObjId = Utils.getRandomName();

			PublishBandParameter oParameter = new PublishBandParameter();
			oParameter.setQueue(sSessionId);
			oParameter.setFileName(sFileUrl);
			oParameter.setWorkspace(sWorkspaceId);
			oParameter.setUserId(sUserId);
			oParameter.setBandName(sBand);
			oParameter.setStyle(sStyle);
			oParameter.setExchange(sWorkspaceId);
			oParameter.setProcessObjId(sProcessObjId);
			oParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));

			String sPath = WasdiConfig.Current.paths.serializationPath;
			
			Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.PUBLISHBAND.name(), sFileUrl, sPath, oParameter, sParentProcessWorkspaceId);
			oReturnValue.setPayload(sProcessObjId);
		}catch (IOException oEx) {
			WasdiLog.errorLog("DownloadResource.publishBand: " + oEx);
			return oReturnValue;

		} catch (Exception oEx) {
			WasdiLog.errorLog("DownloadResource.publishBand: " + oEx);
			return oReturnValue;
		}

		oReturnValue.setMessageCode("WAITFORRABBIT");
		return oReturnValue;

	}
	
	/**
	 * Download a SLD style
	 * @param sSessionId Session Token
	 * @param sTokenSessionId Alternative session token to allow client download
	 * @param sStyle Name of the style to download
	 * @return SLD file
	 */
	@GET
	@Path("downloadstyle")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadStyleByName(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("token") String sTokenSessionId,
			@QueryParam("style") String sStyle)
	{			

		WasdiLog.debugLog("FileBufferResource.downloadStyleByName( Style: " + sStyle + " )");

		try {

			if( Utils.isNullOrEmpty(sSessionId) == false) {
				sTokenSessionId = sSessionId;
			}

			User oUser = Wasdi.getUserFromSession(sTokenSessionId);

			if (oUser == null) {
				WasdiLog.debugLog("FileBufferResource.downloadStyleByName: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			//check the user can access the workspace
			if (!PermissionsUtils.canUserAccessStyle(oUser.getUserId(), sStyle)) {
				WasdiLog.debugLog("FileBufferResource.downloadStyleByName: user cannot access workspace");
				return Response.status(Status.UNAUTHORIZED).build();
			}			

			// Take path
			String sDownloadRootPath = Wasdi.getDownloadPath();
			String sStyleSldPath = sDownloadRootPath + "styles/" + sStyle + ".sld";

			File oFile = new File(sStyleSldPath);

			ResponseBuilder oResponseBuilder = null;

			if(oFile.exists()==false) {
				WasdiLog.debugLog("FileBufferResource.downloadStyleByName: file does not exists " + oFile.getPath());
				oResponseBuilder = Response.serverError();	
			} 
			else {

				WasdiLog.debugLog("FileBufferResource.downloadStyleByName: returning file " + oFile.getPath());

				FileStreamingOutput oStream;
				oStream = new FileStreamingOutput(oFile);

				oResponseBuilder = Response.ok(oStream);
				oResponseBuilder.header("Content-Disposition", "attachment; filename="+ oFile.getName());
				oResponseBuilder.header("Content-Length", Long.toString(oFile.length()));
			}

			return oResponseBuilder.build();

		} 
		catch (Exception oEx) {
			WasdiLog.errorLog("FileBufferResource.downloadStyleByName: " + oEx);
		}

		return null;
	}
	
	/**
	 * Uplad a .sld file
	 * @param fileInputStream File Input Stream
	 * @param sSessionId User id
	 * @param sName style name
	 * @return std http response
	 */
	@POST
	@Path("/uploadstyle")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadStyle(@FormDataParam("file") InputStream fileInputStream,
			@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("name") String sName)  {

		WasdiLog.debugLog("FileBufferResource.uploadStyle( Name: " + sName);

		try {
			// Check authorization
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser == null) {
				WasdiLog.debugLog("FileBufferResource.uploadStyle: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			if (!PermissionsUtils.userHasValidSubscription(oUser)) {
				WasdiLog.debugLog("FileBufferResource.uploadStyle: No valid Subscription");
				return Response.status(Status.UNAUTHORIZED).build();				
			}			

			// Get Download Path
			String sDownloadRootPath = Wasdi.getDownloadPath();

			File oStylesPath = new File(sDownloadRootPath + "styles/");

			if (!oStylesPath.exists()) {
				oStylesPath.mkdirs();
			}

			// Generate Workflow Id and file
			File oStyleXmlFile = new File(sDownloadRootPath + "styles/" + sName + ".sld");

			WasdiLog.debugLog("FileBufferResource.uploadStyle: style file Path: " + oStyleXmlFile.getPath());

			// save uploaded file
			int iRead = 0;
			byte[] ayBytes = new byte[1024];

			try(OutputStream oOutStream = new FileOutputStream(oStyleXmlFile)) {
				while ((iRead = fileInputStream.read(ayBytes)) != -1) {
					oOutStream.write(ayBytes, 0, iRead);
				}
				oOutStream.flush();
			}
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("FileBufferResource.uploadStyle: " + oEx);
			return Response.serverError().build();
		}
		
		return Response.ok().build();
	}
	
	
	@GET
	@Path("/styles")	
	public Response getStyles(@HeaderParam("x-session-token") String sSessionId)
	{			

		WasdiLog.debugLog("FileBufferResource.getStyles");

		try {

			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser == null) {
				WasdiLog.debugLog("FileBufferResource.getStyles: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			// Take path
			String sDownloadRootPath = Wasdi.getDownloadPath();
			String sStyleSldPath = sDownloadRootPath + "styles/";

			File oFile = new File(sStyleSldPath);
			
			File [] aoStyleFiles = oFile.listFiles(new FilenameFilter() {
			    @Override
			    public boolean accept(File dir, String sName) {
			        return sName.toLowerCase().endsWith(".sld");
			    }
			});
			
			ArrayList<String> asStyles = new ArrayList<>(); 

			if (aoStyleFiles != null) {
				for (File oSldFile : aoStyleFiles) {
					try {
						String sFileName = oSldFile.getName();
						
						String sStyle = WasdiFileUtils.getFileNameWithoutExtensionsAndTrailingDots(sFileName);
						
						asStyles.add(sStyle);
					}
					catch (Exception oEx) {
						WasdiLog.errorLog("FileBufferResource.getStyles: error " + oEx.toString());
					}
				}				
			}

			return Response.ok(asStyles).build();
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog("FileBufferResource.getStyles: " + oEx);
		}

		return Response.status(Status.INTERNAL_SERVER_ERROR).build();
	}	
	

}
