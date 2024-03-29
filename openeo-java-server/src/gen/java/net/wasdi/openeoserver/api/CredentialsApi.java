package net.wasdi.openeoserver.api;

import javax.servlet.ServletConfig;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import net.wasdi.openeoserver.WasdiOpenEoServer;
import net.wasdi.openeoserver.viewmodels.Error;
import net.wasdi.openeoserver.viewmodels.HTTPBasicAccessToken;
import wasdi.shared.business.users.*;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

@Path("/credentials")
public class CredentialsApi  {

	public CredentialsApi(@Context ServletConfig servletContext) {

	}

	@javax.ws.rs.GET
	@Path("/basic")    
	@Produces({ "application/json" })
	public Response authenticateBasic(@HeaderParam("Authorization") String sAuthorization) {
		
		if (Utils.isNullOrEmpty(sAuthorization)) {
			WasdiLog.debugLog("CredentialsApi.authenticateBasic: no credentials");
			return Response.status(Status.UNAUTHORIZED).entity(Error.getUnathorizedError()).build();			
		}
		
		UserSession oUserSession = WasdiOpenEoServer.getLoginFromBasicAuth(sAuthorization);
		
		if (oUserSession == null) {
			WasdiLog.debugLog("CredentialsApi.authenticateBasic: invalid credentials");
			return Response.status(Status.FORBIDDEN).entity(Error.getForbideenError()).build();
		}
		
    	try {
    		
    		HTTPBasicAccessToken oHTTPBasicAccessToken = new HTTPBasicAccessToken();
    		oHTTPBasicAccessToken.setAccessToken(oUserSession.getSessionId());
    		
    		return Response.ok().entity(oHTTPBasicAccessToken) .build();
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("CredentialsApi.authenticateBasic error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("CredentialsApi.authenticateBasic", "InternalServerError", oEx.getMessage())).build();
		}
	}
	
	@javax.ws.rs.GET
	@Path("/oidc")
	@Produces({ "application/json" })
	public Response authenticateOidc(@Context SecurityContext oSecurityContext) {
		
    	try {
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("CredentialsApi.authenticateOidc error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("CredentialsApi.authenticateOidc", "InternalServerError", oEx.getMessage())).build();
		}
    	
		return Response.ok().build();
	}
}
