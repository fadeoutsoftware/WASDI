package net.wasdi.openeoserver.api;

import javax.servlet.ServletConfig;
import javax.validation.constraints.Min;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.wasdi.openeoserver.WasdiOpenEoServer;
import net.wasdi.openeoserver.viewmodels.Error;
import net.wasdi.openeoserver.viewmodels.Processes;
import wasdi.shared.business.users.User;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;

@Path("/processes")


public class ProcessesApi  {

   public ProcessesApi(@Context ServletConfig servletContext) {
   }

    @javax.ws.rs.GET
    @Produces({ "application/json" })
    public Response listProcesses(@QueryParam("limit")  @Min(1) Integer limit,@HeaderParam("Authorization") String sAuthorization) {
    	
    	User oUser = WasdiOpenEoServer.getUserFromAuthenticationHeader(sAuthorization);
    	
    	if (oUser == null) {
			WasdiLog.debugLog("CollectionsApi.describeCollection: invalid credentials");
			return Response.status(Status.UNAUTHORIZED).entity(Error.getUnathorizedError()).build();    		
    	} 
    	
    	try {
    		String sProcessesDescription  = WasdiConfig.Current.openEO.processes_config;
    		String sProcessesJson = WasdiFileUtils.fileToText(sProcessesDescription);
    		Processes oProcesses = (Processes) MongoRepository.s_oMapper.readValue(sProcessesJson, Processes.class);
    		
    		return Response.ok().entity(oProcesses).build();
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("ProcessesApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("ProcessesApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    }
}
