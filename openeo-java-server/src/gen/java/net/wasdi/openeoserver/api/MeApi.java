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
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

@Path("/me")


public class MeApi  {

   public MeApi(@Context ServletConfig servletContext) {
   }

    @javax.ws.rs.GET
    @Produces({ "application/json" })
    public Response describeAccount(@HeaderParam("Authorization") String sAuthorization) {
    	
		if (Utils.isNullOrEmpty(sAuthorization)) {
			WasdiLog.debugLog("CredentialsApi.authenticateBasic: no credentials");
			return Response.status(Status.UNAUTHORIZED).entity(Error.getUnathorizedError()).build();			
		}
		
    	User oUser = WasdiOpenEoServer.getUserFromAuthenticationHeader(sAuthorization);
    	
    	if (oUser == null) {
			WasdiLog.debugLog("CollectionsApi.describeCollection: invalid credentials");
			return Response.status(Status.FORBIDDEN).entity(Error.getForbideenError()).build();    		
    	}      	
    	
    	try {
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("MeApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("MeApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
    	return Response.ok().build();
    }
}
