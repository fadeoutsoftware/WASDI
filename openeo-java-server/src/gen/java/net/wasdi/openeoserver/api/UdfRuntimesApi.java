package net.wasdi.openeoserver.api;

import javax.servlet.ServletConfig;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.wasdi.openeoserver.WasdiOpenEoServer;
import net.wasdi.openeoserver.viewmodels.Error;
import wasdi.shared.business.users.*;
import wasdi.shared.utils.log.WasdiLog;

@Path("/udf_runtimes")



public class UdfRuntimesApi  {

   public UdfRuntimesApi(@Context ServletConfig servletContext) {
   }

    @javax.ws.rs.GET
    @Produces({ "application/json" })
    public Response listUdfRuntimes(@HeaderParam("Authorization") String sAuthorization) {
    	
    	User oUser = WasdiOpenEoServer.getUserFromAuthenticationHeader(sAuthorization);
    	
    	if (oUser == null) {
			WasdiLog.debugLog("CollectionsApi.describeCollection: invalid credentials");
			return Response.status(Status.UNAUTHORIZED).entity(Error.getUnathorizedError()).build();    		
    	}      	
    	
    	try {
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("UdfRuntimesApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("UdfRuntimesApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
        return Response.ok().build();
    }
}
