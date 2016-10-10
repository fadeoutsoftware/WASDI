package it.fadeout.rest.resources;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/download")
public class DownloadResource {

	
	@GET
	@Path("")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response Search(@QueryParam("sFileUrl") String sFileUrl) throws IOException
	{
		new Thread() {
		     public void run() {
		    	 try {
					@SuppressWarnings("unused")
					Process oProc = Runtime.getRuntime().exec("java -jar C:\\Codice\\esa\\wasdi\\wrappersnap\\out\\artifacts\\launcher_jar\\launcher.jar -downloadFileUrl " + sFileUrl);
				} catch (IOException e) { 
					e.printStackTrace();
				}
		     }
		  }.start();
		
		return Response.ok().build();
		
	}
	
	
}
