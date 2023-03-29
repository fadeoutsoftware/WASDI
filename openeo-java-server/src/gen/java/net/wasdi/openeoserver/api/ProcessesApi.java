package net.wasdi.openeoserver.api;

import javax.servlet.ServletConfig;
import javax.validation.constraints.Min;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.Response.Status;

import net.wasdi.openeoserver.viewmodels.Error;
import wasdi.shared.utils.log.WasdiLog;

@Path("/processes")


public class ProcessesApi  {

   public ProcessesApi(@Context ServletConfig servletContext) {
   }

    @javax.ws.rs.GET
    @Produces({ "application/json" })
    public Response listProcesses(@QueryParam("limit")  @Min(1) Integer limit,@Context SecurityContext securityContext) {
    	
    	try {
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("ProcessesApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("ProcessesApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
    	return Response.ok().build();
    }
}
