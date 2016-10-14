package it.fadeout.rest.resources;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import it.fadeout.Wasdi;
import wasdi.shared.LauncherOperations;
import wasdi.shared.parameters.DownloadFileParameter;
import wasdi.shared.parameters.PublishParameters;
import wasdi.shared.utils.SerializationUtils;

@Path("/download")
public class DownloadResource {
	
	@Context
	ServletConfig m_oServletConfig;	

	@GET
	@Path("downloadandpublish")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response DownloadAndPublish(@QueryParam("sFileUrl") String sFileUrl) throws IOException
	{
		try {
			
			String sPath = m_oServletConfig.getInitParameter("SerializationPath") + Wasdi.GetSerializationFileName();
			
			DownloadFileParameter oParameter = new DownloadFileParameter();
			oParameter.setQueue("TestWuaue");
			oParameter.setUrl(sFileUrl);

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
	public Response Download(@QueryParam("sFileUrl") String sFileUrl) throws IOException
	{
		try {
			
			String sPath = m_oServletConfig.getInitParameter("SerializationPath") + Wasdi.GetSerializationFileName();
			
			DownloadFileParameter oParameter = new DownloadFileParameter();
			oParameter.setQueue("TestWuaue");
			oParameter.setUrl(sFileUrl);

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
	public Response Publish(@QueryParam("sFileUrl") String sFileUrl, @QueryParam("sStore") String sStore) throws IOException
	{
		try {
			
			String sPath = m_oServletConfig.getInitParameter("SerializationPath") + Wasdi.GetSerializationFileName();
			
			PublishParameters oParameter = new PublishParameters();
			oParameter.setQueue("TestWuaue");
			oParameter.setFileName(sFileUrl);
			oParameter.setStore(sStore);

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
