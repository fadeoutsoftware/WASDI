package ogc.wasdi.processes.rest.resources;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.ogcprocesses.ApiException;
import wasdi.shared.viewmodels.ogcprocesses.Execute;
import wasdi.shared.viewmodels.ogcprocesses.ProcessList;
import wasdi.shared.viewmodels.ogcprocesses.StatusInfo;

@Path("processes")
public class ProcessesResource {
	
	/**
	 *  Get the list of available processes
	 * @return
	 */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProcesses() {
    	try {
    		ProcessList oProcessList = new ProcessList();
    		
    		return Response.status(Status.OK).entity(oProcessList).build();
    	}
    	catch (Exception oEx) {
    		Utils.debugLog("");
    		
    		ApiException oApiException = new ApiException();
    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(oApiException).build();
		}
    }
    
	/**
	 * 
	 * @return
	 */
    @GET
    @Path("/{processID}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProcessDescription(@PathParam("processID") String sProcessID) {
    	try {
    		// nota http 404 se non esiste
    		wasdi.shared.viewmodels.ogcprocesses.Process oProcess = new wasdi.shared.viewmodels.ogcprocesses.Process();
    		
    		return Response.status(Status.OK).entity(oProcess).build();
    	}
    	catch (Exception oEx) {
    		Utils.debugLog("");
    		
    		ApiException oApiException = new ApiException();
    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(oApiException).build();
		}
    }
    
	/**
	 * 
	 * @return
	 */
    @POST
    @Path("/{processID}/execution")
    @Produces(MediaType.APPLICATION_JSON)
    public Response executeApplication(@PathParam("processID") String sProcessID, Execute oExecute) {
    	try {
    		StatusInfo oStatusInfo = new StatusInfo();
    		// Nota 404 se non esiste 200 per sincrono 500 errore
    		
    		return Response.status(Status.CREATED).entity(oStatusInfo).build();
    	}
    	catch (Exception oEx) {
    		Utils.debugLog("");
    		
    		ApiException oApiException = new ApiException();
    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(oApiException).build();
		}
    }    

}
