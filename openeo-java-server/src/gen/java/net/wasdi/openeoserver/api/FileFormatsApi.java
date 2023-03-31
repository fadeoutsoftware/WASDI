package net.wasdi.openeoserver.api;

import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.ServletConfig;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.wasdi.openeoserver.WasdiOpenEoServer;
import net.wasdi.openeoserver.viewmodels.Error;
import net.wasdi.openeoserver.viewmodels.FileFormat;
import net.wasdi.openeoserver.viewmodels.FileFormat.GisDataTypesEnum;
import net.wasdi.openeoserver.viewmodels.FileFormats;
import net.wasdi.openeoserver.viewmodels.ResourceParameter;
import wasdi.shared.business.User;
import wasdi.shared.utils.log.WasdiLog;

@Path("/file_formats")


public class FileFormatsApi  {

   public FileFormatsApi(@Context ServletConfig servletContext) {
   }

    @javax.ws.rs.GET
    
    
    @Produces({ "application/json" })
    public Response listFileTypes(@HeaderParam("Authorization") String sAuthorization) {
    	
    	User oUser = WasdiOpenEoServer.getUserFromAuthenticationHeader(sAuthorization);
    	
    	if (oUser == null) {
			WasdiLog.debugLog("CollectionsApi.describeCollection: invalid credentials");
			return Response.status(Status.UNAUTHORIZED).entity(Error.getUnathorizedError()).build();    		
    	}    	
    	
    	try {
    		FileFormats oVM = new FileFormats();
    		
    		FileFormat oTiff = new FileFormat();
    		oTiff.setTitle("GeoTiff");
    		oTiff.setDescription("Really you need a description for GeoTIFF? Is a Raster");
    		oTiff.setGisDataTypes(new ArrayList<GisDataTypesEnum>());
    		oTiff.getGisDataTypes().add(GisDataTypesEnum.RASTER);
    		oTiff.setParameters(new HashMap<String, ResourceParameter>());
    		
    		ResourceParameter oRes1 = new ResourceParameter();
    		oRes1.setDefault(false);
    		oTiff.getParameters().put("tiled", oRes1);
    		
    		oVM.setInput(new HashMap<String, FileFormat>());
    		oVM.getInput().put("GTiff", oTiff);
    		
    		oVM.setOutput(new HashMap<String, FileFormat>());
    		oVM.getOutput().put("GTiff", oTiff);
    		
    		
    		return Response.ok().entity(oVM).build();
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("FileFormatsApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("FileFormatsApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
    }
}
