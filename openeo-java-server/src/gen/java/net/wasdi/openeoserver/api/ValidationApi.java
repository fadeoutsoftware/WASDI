package net.wasdi.openeoserver.api;

import javax.servlet.ServletConfig;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.Response.Status;

import net.wasdi.openeoserver.viewmodels.Error;
import net.wasdi.openeoserver.viewmodels.ProcessGraphWithMetadata;
import wasdi.shared.utils.log.WasdiLog;

@Path("/validation")


public class ValidationApi  {

   public ValidationApi(@Context ServletConfig servletContext) {
   }

    @javax.ws.rs.POST    
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public Response validateCustomProcess(@NotNull @Valid  ProcessGraphWithMetadata processGraphWithMetadata,@Context SecurityContext securityContext) {
    	
    	try {
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("ValidationApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("ValidationApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
        return Response.ok().build();
    }
}
