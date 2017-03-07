package it.fadeout.rest.resources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.servlet.ServletConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import it.fadeout.Wasdi;
import wasdi.shared.business.User;
import wasdi.shared.utils.Utils;

@Path("/catalog")
public class CatalogResources {
	
	@Context
	ServletConfig m_oServletConfig;
	
	@POST
	@Path("/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadMapFile(@FormDataParam("file") InputStream fileInputStream,
	                                @FormDataParam("file") FormDataContentDisposition fileMetaData, @HeaderParam("x-session-token") String sSessionId) throws Exception
	{ 
		User oUser = Wasdi.GetUserFromSession(sSessionId);
	    if (oUser==null) return null;
		if (Utils.isNullOrEmpty(oUser.getUserId())) return null;
		
		String sDownloadRootPath = m_oServletConfig.getInitParameter("DownloadRootPath");
		if (!sDownloadRootPath.endsWith("/"))
			sDownloadRootPath += "/";
		
		String sDownloadPath = sDownloadRootPath + oUser.getUserId()+"/" + "CONTINUUM";
		
		if(!Files.exists(Paths.get(sDownloadPath)))
		{
			if (Files.createDirectories(Paths.get(sDownloadPath))== null)
			{
				System.out.println("CatalogResources.uploadMapFile: Directory " + sDownloadPath + " not created");
				return null;
			}
			
		}
		
	    try
	    {
	        int read = 0;
	        byte[] bytes = new byte[1024];
	 
	        OutputStream out = new FileOutputStream(new File(sDownloadPath + "/" + fileMetaData.getFileName()));
	        while ((read = fileInputStream.read(bytes)) != -1) 
	        {
	            out.write(bytes, 0, read);
	        }
	        out.flush();
	        out.close();
	    } catch (IOException e) 
	    {
	        throw new WebApplicationException("Error while uploading file. Please try again !!");
	    }
	    return Response.ok().build();
	}

}
