package net.wasdi.openeoserver.api;

import java.io.File;

import javax.servlet.ServletConfig;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import io.swagger.annotations.ApiParam;
import net.wasdi.openeoserver.WasdiOpenEoServer;
import net.wasdi.openeoserver.viewmodels.Error;
import wasdi.shared.business.users.*;
import wasdi.shared.utils.log.WasdiLog;

@Path("/files")


public class FilesApi  {

	public FilesApi(@Context ServletConfig servletContext) {
	}

	@javax.ws.rs.DELETE
	@Path("/{path}")
	@Produces({ "application/json" })
	public Response deleteFile(@PathParam("path") @NotNull  String path, @HeaderParam("Authorization") String sAuthorization) {
		
    	User oUser = WasdiOpenEoServer.getUserFromAuthenticationHeader(sAuthorization);
    	
    	if (oUser == null) {
			WasdiLog.debugLog("CollectionsApi.describeCollection: invalid credentials");
			return Response.status(Status.UNAUTHORIZED).entity(Error.getUnathorizedError()).build();    		
    	}
    	
    	try {
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("FilesApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("FilesApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
		return Response.ok().build();
	}
	
	@javax.ws.rs.GET
	@Path("/{path}")
	@Produces({ "application/octet-stream", "application/json" })
	public Response downloadFile(@PathParam("path") @NotNull  String path,@HeaderParam("Authorization") String sAuthorization) {
		
    	User oUser = WasdiOpenEoServer.getUserFromAuthenticationHeader(sAuthorization);
    	
    	if (oUser == null) {
			WasdiLog.debugLog("CollectionsApi.describeCollection: invalid credentials");
			return Response.status(Status.UNAUTHORIZED).entity(Error.getUnathorizedError()).build();    		
    	}
    	
    	try {
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("FilesApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("FilesApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
		return Response.ok().build();
	}

	@javax.ws.rs.GET
	@Produces({ "application/json" })
	public Response listFiles(@QueryParam("limit")  @Min(1) Integer limit,@HeaderParam("Authorization") String sAuthorization) {
		
    	User oUser = WasdiOpenEoServer.getUserFromAuthenticationHeader(sAuthorization);
    	
    	if (oUser == null) {
			WasdiLog.debugLog("CollectionsApi.describeCollection: invalid credentials");
			return Response.status(Status.UNAUTHORIZED).entity(Error.getUnathorizedError()).build();    		
    	}
    	
		
    	try {
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("FilesApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("FilesApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
		return Response.ok().build();
	}
	
	@javax.ws.rs.PUT
	@Path("/{path}")
	@Consumes({ "application/octet-stream" })
	@Produces({ "application/json" })
	public Response uploadFile(@PathParam("path") @NotNull  String path,@ApiParam(value = "", required = true) @NotNull  File body,@HeaderParam("Authorization") String sAuthorization) {
		
    	User oUser = WasdiOpenEoServer.getUserFromAuthenticationHeader(sAuthorization);
    	
    	if (oUser == null) {
			WasdiLog.debugLog("CollectionsApi.describeCollection: invalid credentials");
			return Response.status(Status.UNAUTHORIZED).entity(Error.getUnathorizedError()).build();    		
    	}
		
    	try {
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("FilesApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("FilesApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
		return Response.ok().build();
	}
}
