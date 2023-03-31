package net.wasdi.openeoserver.api;

import javax.servlet.ServletConfig;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.wasdi.openeoserver.viewmodels.Error;
import wasdi.shared.utils.log.WasdiLog;

@Path("/.well-known/openeo")
public class WellKnownApi  {

   public WellKnownApi(@Context ServletConfig servletContext) {
   }

    @javax.ws.rs.GET
    @Produces({ "application/json" })
    public Response connect() {
    	
    	try {
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("WellKnownApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("WellKnownApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
        return Response.ok().build();
    }
}
