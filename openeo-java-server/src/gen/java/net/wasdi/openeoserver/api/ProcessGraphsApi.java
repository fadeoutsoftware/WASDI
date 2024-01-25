package net.wasdi.openeoserver.api;

import javax.servlet.ServletConfig;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.wasdi.openeoserver.WasdiOpenEoServer;
import net.wasdi.openeoserver.viewmodels.Error;
import net.wasdi.openeoserver.viewmodels.ProcessGraphWithMetadata;
import wasdi.shared.business.users.*;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

@Path("/process_graphs")


public class ProcessGraphsApi  {

   public ProcessGraphsApi(@Context ServletConfig servletContext) {
   }

    @javax.ws.rs.DELETE
    @Path("/{process_graph_id}")    
    @Produces({ "application/json" })
    public Response deleteCustomProcess(@PathParam("process_graph_id") @NotNull  @Pattern(regexp="^\\w+$") String processGraphId,@HeaderParam("Authorization") String sAuthorization) {
    	
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
    		WasdiLog.errorLog("ProcessGraphsApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("ProcessGraphsApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
    	
    	return Response.ok().build();
    }
    @javax.ws.rs.GET
    @Path("/{process_graph_id}")
    @Produces({ "application/json" })
    public Response describeCustomProcess(@PathParam("process_graph_id") @NotNull  @Pattern(regexp="^\\w+$") String processGraphId,@HeaderParam("Authorization") String sAuthorization) {
    	
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
    		WasdiLog.errorLog("ProcessGraphsApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("ProcessGraphsApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
    	return Response.ok().build();
    }
    
    @javax.ws.rs.GET
    @Produces({ "application/json" })
    public Response listCustomProcesses(@QueryParam("limit")  @Min(1) Integer limit,@HeaderParam("Authorization") String sAuthorization) {
    	
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
    		WasdiLog.errorLog("ProcessGraphsApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("ProcessGraphsApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
    	return Response.ok().build();
    }
    @javax.ws.rs.PUT
    @Path("/{process_graph_id}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public Response storeCustomProcess(@PathParam("process_graph_id") @NotNull  @Pattern(regexp="^\\w+$") String processGraphId, @NotNull @Valid  ProcessGraphWithMetadata processGraphWithMetadata,@HeaderParam("Authorization") String sAuthorization) {
    	
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
    		WasdiLog.errorLog("ProcessGraphsApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("ProcessGraphsApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
    	
    	return Response.ok().build();
    }
}
