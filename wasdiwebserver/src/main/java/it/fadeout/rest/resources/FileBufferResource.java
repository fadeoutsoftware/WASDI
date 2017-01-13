package it.fadeout.rest.resources;

import java.io.IOException;
import java.util.List;

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
import wasdi.shared.business.ProductWorkspace;
import wasdi.shared.business.PublishedBand;
import wasdi.shared.business.User;
import wasdi.shared.business.UserSession;
import wasdi.shared.data.ProductWorkspaceRepository;
import wasdi.shared.data.PublishedBandsRepository;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.parameters.DownloadFileParameter;
import wasdi.shared.parameters.PublishBandParameter;
import wasdi.shared.parameters.PublishParameters;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.RabbitMessageViewModel;

@Path("/filebuffer")
public class FileBufferResource {
	
	@Context
	ServletConfig m_oServletConfig;	

	@GET
	@Path("downloadandpublish")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response DownloadAndPublish(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sFileUrl") String sFileUrl, @QueryParam("sWorkspace") String sWorkspace) throws IOException
	{
		try {
			
			if (Utils.isNullOrEmpty(sSessionId)) return Response.status(401).build();
			
			User oUser = Wasdi.GetUserFromSession(sSessionId);
			
			if (oUser==null) return Response.status(401).build();
			if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(401).build();
			
			String sUserId = oUser.getUserId();			
			
			String sPath = m_oServletConfig.getInitParameter("SerializationPath") + Wasdi.GetSerializationFileName();
			
			DownloadFileParameter oParameter = new DownloadFileParameter();
			oParameter.setQueue("TestWuaue");
			oParameter.setUrl(sFileUrl);
			oParameter.setWorkspace(sWorkspace);
			oParameter.setUserId(sUserId);

			SerializationUtils.serializeObjectToXML(sPath, oParameter);
			
			String sLauncherPath = m_oServletConfig.getInitParameter("LauncherPath");
			
			String sShellExString = "java -jar " + sLauncherPath +" -operation " + LauncherOperations.DOWNLOADANDPUBLISH + " -parameter " + sPath;
			
			System.out.println("DownloadResource.DownloadAndPublish: shell exec " + sShellExString);

			Process oProc = Runtime.getRuntime().exec(sShellExString);
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return Response.ok().build();

	}

	@GET
	@Path("download")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response Download(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sFileUrl") String sFileUrl, @QueryParam("sWorkspaceId") String sWorkspaceId) throws IOException
	{
		try {
			
			if (Utils.isNullOrEmpty(sSessionId)) return Response.status(401).build();
			
			User oUser = Wasdi.GetUserFromSession(sSessionId);
			
			if (oUser==null) return Response.status(401).build();
			if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(401).build();
			
			String sUserId = oUser.getUserId();
			
			
			String sPath = m_oServletConfig.getInitParameter("SerializationPath") + Wasdi.GetSerializationFileName();
			
			DownloadFileParameter oParameter = new DownloadFileParameter();
			oParameter.setQueue(sWorkspaceId);
			oParameter.setUrl(sFileUrl);
			oParameter.setWorkspace(sWorkspaceId);
			oParameter.setUserId(sUserId);

			SerializationUtils.serializeObjectToXML(sPath, oParameter);
			
			String sLauncherPath = m_oServletConfig.getInitParameter("LauncherPath");
			
			String sShellExString = "java -jar " + sLauncherPath +" -operation " + LauncherOperations.DOWNLOAD + " -parameter " + sPath;
			
			System.out.println("DownloadResource.DownloadAndPublish: shell exec " + sShellExString);

			Process oProc = Runtime.getRuntime().exec(sShellExString);
			
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
			
			if (Utils.isNullOrEmpty(sSessionId)) return Response.status(401).build();
			
			User oUser = Wasdi.GetUserFromSession(sSessionId);
			
			if (oUser==null) return Response.status(401).build();
			if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(401).build();
			
			String sUserId = oUser.getUserId();
			
			
			String sPath = m_oServletConfig.getInitParameter("SerializationPath") + Wasdi.GetSerializationFileName();
			
			PublishParameters oParameter = new PublishParameters();
			oParameter.setQueue(sWorkspaceId);
			oParameter.setFileName(sFileUrl);
			oParameter.setWorkspace(sWorkspaceId);
			oParameter.setUserId(sUserId);
			
			SerializationUtils.serializeObjectToXML(sPath, oParameter);
			
			String sLauncherPath = m_oServletConfig.getInitParameter("LauncherPath");
			
			String sShellExString = "java -jar " + sLauncherPath +" -operation " + LauncherOperations.PUBLISH + " -parameter " + sPath;
			
			System.out.println("DownloadResource.Publish: shell exec " + sShellExString);

			Process oProc = Runtime.getRuntime().exec(sShellExString);
			
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
	public RabbitMessageViewModel PublishBand(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sFileUrl") String sFileUrl, @QueryParam("sWorkspaceId") String sWorkspaceId, @QueryParam("sBand") String sBand) throws IOException
	{
		RabbitMessageViewModel oReturnValue = null;
		try {
			
			if (Utils.isNullOrEmpty(sSessionId)) return oReturnValue;
			
			User oUser = Wasdi.GetUserFromSession(sSessionId);
			
			if (oUser==null) return oReturnValue;
			if (Utils.isNullOrEmpty(oUser.getUserId())) return oReturnValue;
			
			System.out.println("FileBufferResource.PublishBand: read product workspaces " + sWorkspaceId);
			
			oReturnValue = new RabbitMessageViewModel();
			// Get Product List
			PublishedBandsRepository oPublishedBandsRepository = new PublishedBandsRepository();
			PublishedBand oPublishBand = oPublishedBandsRepository.GetPublishedBand(sFileUrl, sBand);
			
			if (oPublishBand != null)
			{
				oReturnValue.setMessageCode(LauncherOperations.PUBLISHBAND);
				oReturnValue.setPayload(oPublishBand.getLayerId());
				return oReturnValue;
			}
			
			String sUserId = oUser.getUserId();
			
			String sPath = m_oServletConfig.getInitParameter("SerializationPath") + Wasdi.GetSerializationFileName();
			
			PublishBandParameter oParameter = new PublishBandParameter();
			oParameter.setQueue(sWorkspaceId);
			oParameter.setFileName(sFileUrl);
			oParameter.setWorkspace(sWorkspaceId);
			oParameter.setUserId(sUserId);
			oParameter.setBandName(sBand);
			
			SerializationUtils.serializeObjectToXML(sPath, oParameter);
			
			String sLauncherPath = m_oServletConfig.getInitParameter("LauncherPath");
			
			String sShellExString = "java -Xmx4g -Xms256m -jar " + sLauncherPath +" -operation " + LauncherOperations.PUBLISHBAND + " -parameter " + sPath;
			
			System.out.println("DownloadResource.PublishBand: shell exec " + sShellExString);

			Process oProc = Runtime.getRuntime().exec(sShellExString);
			
		} catch (IOException e) {
			e.printStackTrace();
			return oReturnValue;
			
		} catch (Exception e) {
			e.printStackTrace();
			return oReturnValue;
		}
		
		oReturnValue.setMessageCode("WAITFORRABBIT");
		return oReturnValue;

	}		

}
