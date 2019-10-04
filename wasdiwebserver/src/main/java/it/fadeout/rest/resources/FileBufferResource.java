package it.fadeout.rest.resources;

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import it.fadeout.Wasdi;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.PublishedBand;
import wasdi.shared.business.User;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.PublishedBandsRepository;
import wasdi.shared.parameters.DownloadFileParameter;
import wasdi.shared.parameters.PublishBandParameter;
import wasdi.shared.parameters.PublishParameters;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.PublishBandResultViewModel;
import wasdi.shared.viewmodels.RabbitMessageViewModel;


@Path("/filebuffer")
public class FileBufferResource {

	@Context
	ServletConfig m_oServletConfig;		
	
	@GET
	@Path("download")
	@Produces({"application/xml", "application/json", "text/xml"})
	public PrimitiveResult download(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sFileUrl") String sFileUrl, @QueryParam("sProvider") String sProvider, 
			@QueryParam("sWorkspaceId") String sWorkspaceId, @QueryParam("sBoundingBox") String sBoundingBox) throws IOException
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

			User oUser = Wasdi.GetUserFromSession(sSessionId);

			if (oUser==null) {
				oResult.setIntValue(401);
				return oResult;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				oResult.setIntValue(401);
				return oResult;
			}

			String sUserId = oUser.getUserId();
			
			String sProcessObjId = Utils.GetRandomName();
			
			DownloadFileParameter oParameter = new DownloadFileParameter();
			oParameter.setQueue(sSessionId);
			oParameter.setUrl(sFileUrl);
			oParameter.setWorkspace(sWorkspaceId);
			oParameter.setUserId(sUserId);
			oParameter.setExchange(sWorkspaceId);
			oParameter.setBoundingBox(sBoundingBox);
			oParameter.setDownloadUser(m_oServletConfig.getInitParameter(sProvider+".OSUser"));
			oParameter.setDownloadPassword(m_oServletConfig.getInitParameter(sProvider+".OSPwd"));
			oParameter.setProvider(sProvider);
			oParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));
			//set the process object Id to params
			oParameter.setProcessObjId(sProcessObjId);
			
			String sPath = m_oServletConfig.getInitParameter("SerializationPath");
			
			
			return Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.DOWNLOAD.name(), sFileUrl, sPath, oParameter);
			
		} catch (IOException e) {
			Utils.debugLog("DownloadResource.Download: Error updating process list " + e);
		} catch (Exception e) {
			Utils.debugLog("DownloadResource.Download: Error updating process list " + e);
		}
		
		oResult.setIntValue(500);
		return oResult;
	}	

	
	@GET
	@Path("publish")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response publish(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sFileUrl") String sFileUrl, @QueryParam("sWorkspaceId") String sWorkspaceId) throws IOException
	{
		try {
			
			Utils.debugLog("FileBufferResource.Publish, session: " + sSessionId);

			if (Utils.isNullOrEmpty(sSessionId)) return Response.status(401).build();

			User oUser = Wasdi.GetUserFromSession(sSessionId);

			if (oUser==null) return Response.status(401).build();
			if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(401).build();

			String sUserId = oUser.getUserId();
			
			String sProcessObjId = Utils.GetRandomName();
			
			PublishParameters oParameter = new PublishParameters();
			oParameter.setQueue(sSessionId);
			oParameter.setFileName(sFileUrl);
			oParameter.setWorkspace(sWorkspaceId);
			oParameter.setUserId(sUserId);
			oParameter.setExchange(sWorkspaceId);
			oParameter.setProcessObjId(sProcessObjId);
			oParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));

			String sPath = m_oServletConfig.getInitParameter("SerializationPath");
			PrimitiveResult oRes = Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.PUBLISH.name(), sFileUrl, sPath, oParameter);
			
			if (oRes.getBoolValue()) {
				return Response.ok().build();
			}
			else {
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
			}
			
		} catch (IOException e) {
			Utils.debugLog("DownloadResource.Publish: " + e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		} catch (Exception e) {
			Utils.debugLog("DownloadResource.Publish: " + e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}

	}	

	@GET
	@Path("publishband")
	@Produces({"application/xml", "application/json", "text/xml"})
	public RabbitMessageViewModel publishBand(	@HeaderParam("x-session-token") String sSessionId,
												@QueryParam("sFileUrl") String sFileUrl,
												@QueryParam("sWorkspaceId") String sWorkspaceId,
												@QueryParam("sBand") String sBand,
												@QueryParam("sStyle") String sStyle) throws IOException {
		RabbitMessageViewModel oReturnValue = null;
		try {
			
			Utils.debugLog("FileBufferResource.PublishBand, session: " + sSessionId);

			if (Utils.isNullOrEmpty(sSessionId)) return oReturnValue;
			User oUser = Wasdi.GetUserFromSession(sSessionId);
			if (oUser==null) return oReturnValue;
			String sUserId = oUser.getUserId();
			if (Utils.isNullOrEmpty(sUserId)) return oReturnValue;

			Utils.debugLog("FileBufferResource.PublishBand, user: " + oUser.getUserId() + ", read product workspaces " + sWorkspaceId);

			oReturnValue = new RabbitMessageViewModel();
			
			String sFullProductPath = Wasdi.getWorkspacePath(m_oServletConfig, Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId);
			
			// Get Product List
			DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
			DownloadedFile oDownloadedFile = oDownloadedFilesRepository.GetDownloadedFileByPath(sFullProductPath+sFileUrl);
			
			PublishedBandsRepository oPublishedBandsRepository = new PublishedBandsRepository();
			PublishedBand oPublishBand = oPublishedBandsRepository.GetPublishedBand(oDownloadedFile.getProductViewModel().getName(), sBand);

			if (oPublishBand != null)
			{
				Utils.debugLog("FileBufferResource.PublishBand: band already published " );
				PublishBandResultViewModel oPublishViewModel = new PublishBandResultViewModel();
				oPublishViewModel.setBoundingBox(oPublishBand.getBoundingBox());
				oPublishViewModel.setBandName(oPublishBand.getBandName());
				oPublishViewModel.setLayerId(oPublishBand.getLayerId());
				oPublishViewModel.setProductName(oPublishBand.getProductName());
				oPublishViewModel.setGeoserverBoundingBox(oPublishBand.getGeoserverBoundingBox());
				oReturnValue.setMessageCode(LauncherOperations.PUBLISHBAND.name());
				oReturnValue.setPayload(oPublishViewModel);
				
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
			
			Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.PUBLISHBAND.name(), sFileUrl, sPath, oParameter);
			
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
	public PrimitiveResult getBandLayerId(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sFileUrl") String sFileUrl, @QueryParam("sWorkspaceId") String sWorkspaceId, @QueryParam("sBand") String sBand) throws IOException
	{
		Utils.debugLog("FileBufferResource.GetBandLayerId, session: "+sSessionId);
		PrimitiveResult oReturnValue = null;
		try {
			if (Utils.isNullOrEmpty(sSessionId)) return oReturnValue;
			User oUser = Wasdi.GetUserFromSession(sSessionId);
			if (oUser==null) return oReturnValue;
			if (Utils.isNullOrEmpty(oUser.getUserId())) return oReturnValue;

			Utils.debugLog("FileBufferResource.GetBandLayerId: read product workspaces " + sWorkspaceId);

			oReturnValue = new PrimitiveResult();
			// Get Product List
			DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
			
			String sFullPath = Wasdi.getWorkspacePath(m_oServletConfig, Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId);
			
			DownloadedFile oDownloadedFile = oDownloadedFilesRepository.GetDownloadedFileByPath(sFullPath+sFileUrl);
			PublishedBandsRepository oPublishedBandsRepository = new PublishedBandsRepository();
			PublishedBand oPublishBand = oPublishedBandsRepository.GetPublishedBand(oDownloadedFile.getProductViewModel().getName(), sBand);

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
