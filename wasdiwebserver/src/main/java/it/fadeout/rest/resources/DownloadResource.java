package it.fadeout.rest.resources;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import wasdi.*;

@Path("/download")
public class DownloadResource {

	@GET
	@Path("")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response DownloadAndPublish(@QueryParam("sFileUrl") String sFileUrl) throws IOException
	{
		try {
			String sPath = "c:/temp/wasdi/serialization.xml";
			DownloadFileParameter oParameter = new DownloadFileParameter();
			oParameter.setQueue("TestWuaue");
			oParameter.setUrls(sFileUrl);

			SerializationUtils.serializeObjectToXML(sPath, oParameter);
			
			String sShellExString = "java -jar C:\\Codice\\esa\\wasdi\\wrappersnap\\out\\artifacts\\launcher_jar\\launcher.jar -operation DOWNLOAD -parameter " + sPath;
			
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
