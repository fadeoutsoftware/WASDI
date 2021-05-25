package it.fadeout.rest.resources;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import it.fadeout.Wasdi;
import it.fadeout.business.Provider;
import it.fadeout.services.ProvidersCatalog;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.PublishedBand;
import wasdi.shared.business.User;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.PublishedBandsRepository;
import wasdi.shared.parameters.DownloadFileParameter;
import wasdi.shared.parameters.PublishBandParameter;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.PublishBandResultViewModel;
import wasdi.shared.viewmodels.RabbitMessageViewModel;


@Path("/filebuffer")
public class FileBufferResource {

	@Context
	ServletConfig m_oServletConfig;

	@Inject
	ProvidersCatalog m_oProviderCatalog;
	
	@GET
	@Path("download")
	@Produces({"application/xml", "application/json", "text/xml"})
	public PrimitiveResult download(@HeaderParam("x-session-token") String sSessionId,
									@QueryParam("sFileUrl") String sFileUrl,
									@QueryParam("sProvider") String sProvider,
									@QueryParam("sWorkspaceId") String sWorkspaceId,
									@QueryParam("sBoundingBox") String sBoundingBox,
									@QueryParam("parent") String sParentProcessWorkspaceId)
			throws IOException
	{
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		try {
			
			Utils.debugLog("FileBufferResource.Download, session: " + sSessionId);

			Boolean bSessionIsValid = !Utils.isNullOrEmpty(sSessionId); 
			if (!bSessionIsValid) {
				oResult.setIntValue(401);
				return oResult;
			}

			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				Utils.debugLog("FileBufferResource.Download(): session is not valid");
				oResult.setIntValue(401);
				return oResult;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				oResult.setIntValue(401);
				return oResult;
			}

			String sUserId = oUser.getUserId();
			
			String sProcessObjId = Utils.GetRandomName();

			// if the provider is not specified, we fallback on the node default provider
			Provider oProvider = null;
			if (Utils.isNullOrEmpty(sProvider)) {
				oProvider = m_oProviderCatalog.getDefaultProvider(Wasdi.s_sMyNodeCode);
			} else {
				oProvider = m_oProviderCatalog.getProvider(sProvider);
			}

			Utils.debugLog("FileBufferResource.Download, provider: " + oProvider.getName());

			DownloadFileParameter oParameter = new DownloadFileParameter();
			oParameter.setQueue(sSessionId);
			oParameter.setUrl(sFileUrl);
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

			String sPath = m_oServletConfig.getInitParameter("SerializationPath");
			
			return Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.DOWNLOAD.name(), sProvider.toUpperCase(), sFileUrl, sPath, oParameter, sParentProcessWorkspaceId);
			
		} catch (IOException e) {
			Utils.debugLog("DownloadResource.Download: Error updating process list " + e);
		} catch (Exception e) {
			Utils.debugLog("DownloadResource.Download: Error updating process list " + e);
		}
		
		oResult.setIntValue(500);
		return oResult;
	}

	@GET
	@Path("publishband")
	@Produces({"application/xml", "application/json", "text/xml"})
	public RabbitMessageViewModel publishBand(	@HeaderParam("x-session-token") String sSessionId,
												@QueryParam("sFileUrl") String sFileUrl,
												@QueryParam("sWorkspaceId") String sWorkspaceId,
												@QueryParam("sBand") String sBand,
												@QueryParam("sStyle") String sStyle, @QueryParam("parent") String sParentProcessWorkspaceId) throws IOException {
		RabbitMessageViewModel oReturnValue = null;
		try {
			
			Utils.debugLog("FileBufferResource.PublishBand");
			
			// Check Authentication
			if (Utils.isNullOrEmpty(sSessionId)) return oReturnValue;
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if (oUser==null) {
				Utils.debugLog("FileBufferResource.PublishBand: session " + sSessionId + " is invalid"); 
				return oReturnValue;
			}
			String sUserId = oUser.getUserId();
			if (Utils.isNullOrEmpty(sUserId)) return oReturnValue;
			
			Utils.debugLog("FileBufferResource.PublishBand, user: " + oUser.getUserId() + ", workspace: " + sWorkspaceId);
			
			// Get the full product path
			String sFullProductPath = Wasdi.getWorkspacePath(m_oServletConfig, Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId);
			
			// Get the product
			DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
			DownloadedFile oDownloadedFile = oDownloadedFilesRepository.getDownloadedFileByPath(sFullProductPath+sFileUrl);
			
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
				Utils.debugLog("FileBufferResource.PublishBand: band already published " );
				PublishBandResultViewModel oPublishBandResultViewModel = new PublishBandResultViewModel();
				oPublishBandResultViewModel.setBoundingBox(oPublishBand.getBoundingBox());
				oPublishBandResultViewModel.setBandName(oPublishBand.getBandName());
				oPublishBandResultViewModel.setLayerId(oPublishBand.getLayerId());
				oPublishBandResultViewModel.setProductName(oPublishBand.getProductName());
				oPublishBandResultViewModel.setGeoserverBoundingBox(oPublishBand.getGeoserverBoundingBox());
				oPublishBandResultViewModel.setGeoserverUrl(oPublishBand.getGeoserverUrl());
				oReturnValue.setMessageCode(LauncherOperations.PUBLISHBAND.name());
				oReturnValue.setPayload(oPublishBandResultViewModel);
				
				Utils.debugLog("FileBufferResource.PublishBand: return published band, user: " + sUserId );
				return oReturnValue;
			}
			
			String sProcessObjId = Utils.GetRandomName();

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

			String sPath = m_oServletConfig.getInitParameter("SerializationPath");
			
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
	
	
	@GET
	@Path("getbandlayerid")
	@Produces({"application/xml", "application/json", "text/xml"})
	public PrimitiveResult getBandLayerId(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sFileUrl") String sFileUrl,
			@QueryParam("sWorkspaceId") String sWorkspaceId, @QueryParam("sBand") String sBand) throws IOException
	{
		Utils.debugLog("FileBufferResource.GetBandLayerId");
		PrimitiveResult oReturnValue = null;
		try {
			if (Utils.isNullOrEmpty(sSessionId)) return oReturnValue;
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if (oUser==null) {
				Utils.debugLog("FileBufferResource.GetBandLayerId( " +
						sSessionId + ", " +
						sFileUrl + ", " + 
						sWorkspaceId + ", " +
						sBand + " ): session invalid");
				return oReturnValue;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) return oReturnValue;

			Utils.debugLog("FileBufferResource.GetBandLayerId: read product workspaces " + sWorkspaceId);

			oReturnValue = new PrimitiveResult();
			// Get Product List
			DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
			
			String sFullPath = Wasdi.getWorkspacePath(m_oServletConfig, Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId);
			
			DownloadedFile oDownloadedFile = oDownloadedFilesRepository.getDownloadedFileByPath(sFullPath+sFileUrl);
			PublishedBandsRepository oPublishedBandsRepository = new PublishedBandsRepository();
			PublishedBand oPublishBand = oPublishedBandsRepository.getPublishedBand(oDownloadedFile.getProductViewModel().getName(), sBand);

			if (oPublishBand != null){
				Utils.debugLog("FileBufferResource.GetBandLayerId: band already published return " +oPublishBand.getLayerId() );
				oReturnValue.setStringValue(oPublishBand.getLayerId());
			} else {
				Utils.debugLog("FileBufferResource.GetBandLayerId: band never published");
			}
		} catch (Exception e) {
			Utils.debugLog("FileBufferResource.GetBandLayerId: " + e);
			return oReturnValue;
		}

		return oReturnValue;
	}	

}
