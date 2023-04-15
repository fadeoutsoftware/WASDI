package net.wasdi.openeoserver.api;

import javax.servlet.ServletConfig;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.Response.Status;

import net.wasdi.openeoserver.viewmodels.Error;
import wasdi.shared.utils.log.WasdiLog;

@Path("/conformance")


public class ConformanceApi  {

   public ConformanceApi(@Context ServletConfig servletContext) {
   }

    @javax.ws.rs.GET
    @Produces({ "application/json" })
    public Response conformance(@Context SecurityContext oSecurityContext) {
    	
    	try {
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("ConformanceApi.conformance error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("ConformanceApi.conformance", "InternalServerError", oEx.getMessage())).build();
		}
    	
    	return Response.ok().build();
    }
}
