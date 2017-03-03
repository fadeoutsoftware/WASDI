package it.fadeout.rest.resources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import it.fadeout.Wasdi;
import wasdi.shared.business.User;
import wasdi.shared.utils.Utils;

public class CatalogResources {
	
	@Context
	ServletConfig m_oServletConfig;
	
	@POST
	@Path("/pdf")
	@Consumes({MediaType.MULTIPART_FORM_DATA})
	public Response uploadPdfFile(@HeaderParam("x-session-token") String sSessionId, @FormDataParam("file") InputStream fileInputStream,
	                                @FormDataParam("file") FormDataContentDisposition fileMetaData) throws Exception
	{
	    User oUser = Wasdi.GetUserFromSession(sSessionId);
	    if (oUser==null) return null;
		if (Utils.isNullOrEmpty(oUser.getUserId())) return null;
		
		String sDownloadPath = m_oServletConfig.getInitParameter("DOWNLOAD_ROOT_PATH") + oUser.getUserId()+"/" + "CONTINUUM";
		
	    try
	    {
	        int read = 0;
	        byte[] bytes = new byte[1024];
	 
	        OutputStream out = new FileOutputStream(new File(sDownloadPath + fileMetaData.getFileName()));
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
	    return Response.ok("Data uploaded successfully !!").build();
	}

}
