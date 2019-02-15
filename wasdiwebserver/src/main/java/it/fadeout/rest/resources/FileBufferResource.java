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
	public Response Download(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sFileUrl") String sFileUrl, @QueryParam("sProvider") String sProvider, 
			@QueryParam("sWorkspaceId") String sWorkspaceId, @QueryParam("sBoundingBox") String sBoundingBox) throws IOException
	{
		try {
			
			Wasdi.DebugLog("FileBufferResource.Download, session: " + sSessionId);

			Boolean bSessionIsValid = !Utils.isNullOrEmpty(sSessionId); 
			if (!bSessionIsValid) {
				return Response.status(401).build();
			}

			User oUser = Wasdi.GetUserFromSession(sSessionId);

			if (oUser==null) return Response.status(401).build();
			if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(401).build();

			String sUserId = oUser.getUserId();

			//Update process list
			ProcessWorkspace oProcess = null;
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			
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
			//set the process object Id to params
			oParameter.setProcessObjId(sProcessObjId);
			
			String sPath = m_oServletConfig.getInitParameter("SerializationPath");
			if (! (sPath.endsWith("\\") || sPath.endsWith("/")) ) sPath += "/";
			sPath += sProcessObjId;
			SerializationUtils.serializeObjectToXML(sPath, oParameter);

			try
			{
				oProcess = new ProcessWorkspace();
				oProcess.setOperationDate(Wasdi.GetFormatDate(new Date()));
				oProcess.setOperationType(LauncherOperations.DOWNLOAD.name());
				oProcess.setProductName(sFileUrl);
				oProcess.setWorkspaceId(sWorkspaceId);
				oProcess.setUserId(sUserId);
				oProcess.setProcessObjId(sProcessObjId);
				oProcess.setStatus(ProcessStatus.CREATED.name());
				oRepository.InsertProcessWorkspace(oProcess);
				Wasdi.DebugLog("FileBufferResource.Download: Process Scheduled for Launcher, user: " + sUserId + ", process ID: " + sProcessObjId);
			}
			catch(Exception oEx){
				Wasdi.DebugLog("DownloadResource.Download: Error updating process list " + oEx.getMessage());
				oEx.printStackTrace();
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
			}
						
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return Response.ok().build();

	}	

	
	@GET
	@Path("publish")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response Publish(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sFileUrl") String sFileUrl, @QueryParam("sWorkspaceId") String sWorkspaceId) throws IOException
	{
		try {
			
			Wasdi.DebugLog("FileBufferResource.Publish, session: " + sSessionId);

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

			String sPath = m_oServletConfig.getInitParameter("SerializationPath") + sProcessObjId;
			SerializationUtils.serializeObjectToXML(sPath, oParameter);
			

			//Update process list
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			ProcessWorkspace oProcess = null;
			try
			{
				
				oProcess = new ProcessWorkspace();
				oProcess.setOperationDate(Wasdi.GetFormatDate(new Date()));
				oProcess.setOperationType(LauncherOperations.PUBLISH.name());
				oProcess.setProductName(sFileUrl);
				oProcess.setWorkspaceId(sWorkspaceId);
				oProcess.setUserId(sUserId);
				oProcess.setProcessObjId(sProcessObjId);
				oProcess.setStatus(ProcessStatus.CREATED.name());
				oRepository.InsertProcessWorkspace(oProcess);
				Wasdi.DebugLog("FileBufferResource.Publish: Process Scheduled for Launcher, user: " + sUserId + ", process ID: " + sProcessObjId);
			}
			catch(Exception oEx){
				Wasdi.DebugLog("DownloadResource.Publish: Error updating process list " + oEx.getMessage());
				oEx.printStackTrace();
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return Response.ok().build();

	}	

	@GET
	@Path("publishband")
	@Produces({"application/xml", "application/json", "text/xml"})
	public RabbitMessageViewModel PublishBand(	@HeaderParam("x-session-token") String sSessionId,
												@QueryParam("sFileUrl") String sFileUrl,
												@QueryParam("sWorkspaceId") String sWorkspaceId,
												@QueryParam("sBand") String sBand) throws IOException {
		RabbitMessageViewModel oReturnValue = null;
		try {
			
			Wasdi.DebugLog("FileBufferResource.PublishBand, session: " + sSessionId);

			if (Utils.isNullOrEmpty(sSessionId)) return oReturnValue;
			User oUser = Wasdi.GetUserFromSession(sSessionId);
			if (oUser==null) return oReturnValue;
			String sUserId = oUser.getUserId();
			if (Utils.isNullOrEmpty(sUserId)) return oReturnValue;

			Wasdi.DebugLog("FileBufferResource.PublishBand, user: " + oUser.getUserId() + ", read product workspaces " + sWorkspaceId);

			oReturnValue = new RabbitMessageViewModel();
			// Get Product List
			DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
			DownloadedFile oDownloadedFile = oDownloadedFilesRepository.GetDownloadedFile(sFileUrl);
			
			PublishedBandsRepository oPublishedBandsRepository = new PublishedBandsRepository();
			PublishedBand oPublishBand = oPublishedBandsRepository.GetPublishedBand(oDownloadedFile.getProductViewModel().getName(), sBand);

			if (oPublishBand != null)
			{
				Wasdi.DebugLog("FileBufferResource.PublishBand: band already published " );
				PublishBandResultViewModel oPublishViewModel = new PublishBandResultViewModel();
				oPublishViewModel.setBoundingBox(oPublishBand.getBoundingBox());
				oPublishViewModel.setBandName(oPublishBand.getBandName());
				oPublishViewModel.setLayerId(oPublishBand.getLayerId());
				oPublishViewModel.setProductName(oPublishBand.getProductName());
				oPublishViewModel.setGeoserverBoundingBox(oPublishBand.getGeoserverBoundingBox());
				oReturnValue.setMessageCode(LauncherOperations.PUBLISHBAND.name());
				oReturnValue.setPayload(oPublishViewModel);
				
				Wasdi.DebugLog("FileBufferResource.PublishBand: return published band, user: " + sUserId );
				return oReturnValue;
			}
			
			String sProcessObjId = Utils.GetRandomName();

			PublishBandParameter oParameter = new PublishBandParameter();
			oParameter.setQueue(sSessionId);
			oParameter.setFileName(sFileUrl);
			oParameter.setWorkspace(sWorkspaceId);
			oParameter.setUserId(sUserId);
			oParameter.setBandName(sBand);
			oParameter.setExchange(sWorkspaceId);
			oParameter.setProcessObjId(sProcessObjId);

			String sPath = m_oServletConfig.getInitParameter("SerializationPath");
			if (!(sPath.endsWith("\\") || sPath.endsWith("/"))) sPath += "/";
			sPath += sProcessObjId;
			
			SerializationUtils.serializeObjectToXML(sPath, oParameter);

			//Update process list
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			ProcessWorkspace oProcess = null;
			try
			{
				oProcess = new ProcessWorkspace();
				oProcess.setOperationDate(Wasdi.GetFormatDate(new Date()));
				oProcess.setOperationType(LauncherOperations.PUBLISHBAND.name());
				oProcess.setProductName(sFileUrl);
				oProcess.setWorkspaceId(sWorkspaceId);
				oProcess.setUserId(oUser.getUserId());
				oProcess.setProcessObjId(sProcessObjId);
				oProcess.setStatus(ProcessStatus.CREATED.name());
				oRepository.InsertProcessWorkspace(oProcess);
				Wasdi.DebugLog("FileBufferResource.PublishBand: Process Scheduled for Launcher, user: " + sUserId +", process ID: " + sProcessObjId);
			}
			catch(Exception oEx){
				Wasdi.DebugLog("DownloadResource.PublishBand: Error updating process list " + oEx.getMessage());
				oEx.printStackTrace();
				return oReturnValue;
			}

		}catch (IOException e) {
			e.printStackTrace();
			return oReturnValue;

		} catch (Exception e) {
			e.printStackTrace();
			return oReturnValue;
		}

		oReturnValue.setMessageCode("WAITFORRABBIT");
		return oReturnValue;

	}
	
	
	@GET
	@Path("getbandlayerid")
	@Produces({"application/xml", "application/json", "text/xml"})
	public PrimitiveResult GetBandLayerId(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sFileUrl") String sFileUrl, @QueryParam("sWorkspaceId") String sWorkspaceId, @QueryParam("sBand") String sBand) throws IOException
	{
		Wasdi.DebugLog("FileBufferResource.GetBandLayerId, session: "+sSessionId);
		PrimitiveResult oReturnValue = null;
		try {
			if (Utils.isNullOrEmpty(sSessionId)) return oReturnValue;
			User oUser = Wasdi.GetUserFromSession(sSessionId);
			if (oUser==null) return oReturnValue;
			if (Utils.isNullOrEmpty(oUser.getUserId())) return oReturnValue;

			Wasdi.DebugLog("FileBufferResource.GetBandLayerId: read product workspaces " + sWorkspaceId);

			oReturnValue = new PrimitiveResult();
			// Get Product List
			DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
			DownloadedFile oDownloadedFile = oDownloadedFilesRepository.GetDownloadedFile(sFileUrl);
			PublishedBandsRepository oPublishedBandsRepository = new PublishedBandsRepository();
			PublishedBand oPublishBand = oPublishedBandsRepository.GetPublishedBand(oDownloadedFile.getProductViewModel().getName(), sBand);

			if (oPublishBand != null){
				Wasdi.DebugLog("FileBufferResource.GetBandLayerId: band already published return " +oPublishBand.getLayerId() );
				oReturnValue.setStringValue(oPublishBand.getLayerId());
			} else {
				Wasdi.DebugLog("FileBufferResource.GetBandLayerId: band never published");
			}
		} catch (Exception e) {
			e.printStackTrace();
			return oReturnValue;
		}

		return oReturnValue;
	}	

}
