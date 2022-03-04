package it.fadeout.rest.resources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

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
import wasdi.shared.business.PublishedBand;
import wasdi.shared.business.User;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.PublishedBandsRepository;
import wasdi.shared.parameters.DownloadFileParameter;
import wasdi.shared.parameters.PublishBandParameter;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.RabbitMessageViewModel;
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
	 * Trigger a new import of an image in WASDI.
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
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		try {
			
			Utils.debugLog("FileBufferResource.Download, session: " + sSessionId);
			
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				Utils.debugLog("FileBufferResource.Download(): session is not valid");
				oResult.setIntValue(401);
				return oResult;
			}

			String sUserId = oUser.getUserId();
			
			String sProcessObjId = Utils.getRandomName();

			// if the provider is not specified, we fallback on the node default provider
			DataProvider oProvider = null;
			if (Utils.isNullOrEmpty(sProvider)) {
				oProvider = m_oDataProviderCatalog.getDefaultProvider(Wasdi.s_sMyNodeCode);
			} else {
				oProvider = m_oDataProviderCatalog.getProvider(sProvider);
			}

			Utils.debugLog("FileBufferResource.Download, provider: " + oProvider.getName());

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
			
		} catch (IOException e) {
			Utils.debugLog("DownloadResource.Download: Error updating process list " + e);
		} catch (Exception e) {
			Utils.debugLog("DownloadResource.Download: Error updating process list " + e);
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
			
			Utils.debugLog("FileBufferResource.PublishBand");
			
			// Check Authentication
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if (oUser==null) {
				Utils.debugLog("FileBufferResource.PublishBand: session " + sSessionId + " is invalid"); 
				return oReturnValue;
			}
			
			String sUserId = oUser.getUserId();
			
			Utils.debugLog("FileBufferResource.PublishBand, user: " + sUserId + ", workspace: " + sWorkspaceId);
			
			// Get the full product path
			String sFullProductPath = Wasdi.getWorkspacePath(Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId);
			
			// Get the product
			DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
			DownloadedFile oDownloadedFile = oDownloadedFilesRepository.getDownloadedFileByPath(sFullProductPath+sFileUrl);
			
			if (oDownloadedFile == null) {
				Utils.debugLog("FileBufferResource.PublishBand: cannot find downloaded file, return");
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
				Utils.debugLog("FileBufferResource.PublishBand: band already published: product Name : " + sProductName + " LayerId: " + oPublishBand.getLayerId());
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
				
				Utils.debugLog("FileBufferResource.PublishBand: return published band, user: " + sUserId );
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
			
		}catch (IOException e) {
			Utils.debugLog("DownloadResource.PublishBand: " + e);
			return oReturnValue;

		} catch (Exception e) {
			Utils.debugLog("DownloadResource.PublishBand: " + e);
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

		Utils.debugLog("FileBufferResource.downloadStyleByName( Style: " + sStyle + " )");

		try {

			if( Utils.isNullOrEmpty(sSessionId) == false) {
				sTokenSessionId = sSessionId;
			}

			User oUser = Wasdi.getUserFromSession(sTokenSessionId);

			if (oUser == null) {
				Utils.debugLog("FileBufferResource.downloadStyleByName( Session: " + sSessionId + ", Style: " + sStyle + " ): invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			// Take path
			String sDownloadRootPath = Wasdi.getDownloadPath();
			String sStyleSldPath = sDownloadRootPath + "styles/" + sStyle + ".sld";

			File oFile = new File(sStyleSldPath);

			ResponseBuilder oResponseBuilder = null;

			if(oFile.exists()==false) {
				Utils.debugLog("FileBufferResource.downloadStyleByName: file does not exists " + oFile.getPath());
				oResponseBuilder = Response.serverError();	
			} 
			else {

				Utils.debugLog("FileBufferResource.downloadStyleByName: returning file " + oFile.getPath());

				FileStreamingOutput oStream;
				oStream = new FileStreamingOutput(oFile);

				oResponseBuilder = Response.ok(oStream);
				oResponseBuilder.header("Content-Disposition", "attachment; filename="+ oFile.getName());
				oResponseBuilder.header("Content-Length", Long.toString(oFile.length()));
			}

			return oResponseBuilder.build();

		} 
		catch (Exception oEx) {
			Utils.debugLog("FileBufferResource.downloadStyleByName: " + oEx);
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

		Utils.debugLog("FileBufferResource.uploadStyle( Name: " + sName);

		try {
			// Check authorization
			if (Utils.isNullOrEmpty(sSessionId)) {
				Utils.debugLog("FileBufferResource.uploadStyle: invalid session");
				return Response.status(401).build();
			}
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser == null) return Response.status(401).build();
			if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(401).build();

			// Get Download Path
			String sDownloadRootPath = Wasdi.getDownloadPath();

			File oStylesPath = new File(sDownloadRootPath + "styles/");

			if (!oStylesPath.exists()) {
				oStylesPath.mkdirs();
			}

			// Generate Workflow Id and file
			File oStyleXmlFile = new File(sDownloadRootPath + "styles/" + sName + ".sld");

			Utils.debugLog("FileBufferResource.uploadStyle: style file Path: " + oStyleXmlFile.getPath());

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
			Utils.debugLog("FileBufferResource.uploadStyle: " + oEx);
			return Response.serverError().build();
		}
		
		return Response.ok().build();
	}
	
	
	@GET
	@Path("/styles")	
	@Deprecated
	public Response getStyles(@HeaderParam("x-session-token") String sSessionId)
	{			

		Utils.debugLog("FileBufferResource.getStyles");

		try {

			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser == null) {
				Utils.debugLog("FileBufferResource.getStyles: invalid session");
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
					catch (Exception e) {
					}
				}				
			}

			return Response.ok(asStyles).build();
		} 
		catch (Exception oEx) {
			Utils.debugLog("FileBufferResource.downloadStyleByName: " + oEx);
		}

		return Response.status(Status.INTERNAL_SERVER_ERROR).build();
	}	
	

}
