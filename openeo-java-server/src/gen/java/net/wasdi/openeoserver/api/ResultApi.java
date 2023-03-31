package net.wasdi.openeoserver.api;

import javax.servlet.ServletConfig;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.wasdi.openeoserver.WasdiOpenEoServer;
import net.wasdi.openeoserver.viewmodels.Error;
import net.wasdi.openeoserver.viewmodels.SynchronousResultRequest;
import wasdi.shared.business.User;
import wasdi.shared.utils.log.WasdiLog;

@Path("/result")


public class ResultApi  {

   public ResultApi(@Context ServletConfig servletContext) {
   }

    @javax.ws.rs.POST    
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public Response computeResult(@NotNull @Valid  SynchronousResultRequest synchronousResultRequest,@HeaderParam("Authorization") String sAuthorization) {
    	
    	User oUser = WasdiOpenEoServer.getUserFromAuthenticationHeader(sAuthorization);
    	
    	if (oUser == null) {
			WasdiLog.debugLog("CollectionsApi.describeCollection: invalid credentials");
			return Response.status(Status.UNAUTHORIZED).entity(Error.getUnathorizedError()).build();    		
    	}      	
    	
    	try {
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("ResultApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("ResultApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
    	return Response.ok().build();
    }
}
