package it.fadeout.rest.resources;

import java.io.IOException;

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
import wasdi.shared.business.UserSession;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.parameters.DownloadFileParameter;
import wasdi.shared.parameters.PublishParameters;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;

@Path("/download")
public class DownloadResource {
	
	@Context
	ServletConfig m_oServletConfig;	

	@GET
	@Path("downloadandpublish")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response DownloadAndPublish(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sFileUrl") String sFileUrl, @QueryParam("sWorkspace") String sWorkspace) throws IOException
	{
		try {
			
			if (Utils.isNullOrEmpty(sSessionId)) return Response.status(401).build();
			
			SessionRepository oSessionRepo = new SessionRepository();
			UserSession oSession = oSessionRepo.GetSession(sSessionId);
			
			if (oSession == null) return Response.status(401).build();
			
			// TODO Check if the session is too old?
			
			String sUserId = oSession.getUserId();
			
			
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
	public Response Download(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sFileUrl") String sFileUrl, @QueryParam("sWorkspace") String sWorkspace) throws IOException
	{
		try {
			
			if (Utils.isNullOrEmpty(sSessionId)) return Response.status(401).build();
			
			SessionRepository oSessionRepo = new SessionRepository();
			UserSession oSession = oSessionRepo.GetSession(sSessionId);
			
			if (oSession == null) return Response.status(401).build();
			
			// TODO Check if the session is too old?
			
			String sUserId = oSession.getUserId();
			
			
			String sPath = m_oServletConfig.getInitParameter("SerializationPath") + Wasdi.GetSerializationFileName();
			
			DownloadFileParameter oParameter = new DownloadFileParameter();
			oParameter.setQueue("TestWuaue");
			oParameter.setUrl(sFileUrl);
			oParameter.setWorkspace(sWorkspace);
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
	public Response Publish(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sFileUrl") String sFileUrl, @QueryParam("sWorkspace") String sWorkspace) throws IOException
	{
		try {
			
			if (Utils.isNullOrEmpty(sSessionId)) return Response.status(401).build();
			
			SessionRepository oSessionRepo = new SessionRepository();
			UserSession oSession = oSessionRepo.GetSession(sSessionId);
			
			if (oSession == null) return Response.status(401).build();
			
			// TODO Check if the session is too old?
			
			String sUserId = oSession.getUserId();
			
			
			String sPath = m_oServletConfig.getInitParameter("SerializationPath") + Wasdi.GetSerializationFileName();
			
			PublishParameters oParameter = new PublishParameters();
			oParameter.setQueue("TestWuaue");
			oParameter.setFileName(sFileUrl);
			oParameter.setWorkspace(sWorkspace);
			oParameter.setUserId(sUserId);
			
			SerializationUtils.serializeObjectToXML(sPath, oParameter);
			
			String sLauncherPath = m_oServletConfig.getInitParameter("LauncherPath");
			
			String sShellExString = "java -jar " + sLauncherPath +" -operation " + LauncherOperations.PUBLISH + " -parameter " + sPath;
			
			System.out.println("DownloadResource.DownloadAndPublish: shell exec " + sShellExString);

			Process oProc = Runtime.getRuntime().exec(sShellExString);
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return Response.ok().build();

	}		

}
