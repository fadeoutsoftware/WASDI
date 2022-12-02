package ogc.wasdi.processes.rest.resources;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.ogcprocesses.ApiException;
import wasdi.shared.viewmodels.ogcprocesses.JobList;
import wasdi.shared.viewmodels.ogcprocesses.Results;
import wasdi.shared.viewmodels.ogcprocesses.StatusInfo;

@Path("jobs")
public class JobsResource {
	
	/**
	 * 
	 * @return
	 */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJobsList() {
    	try {
    		JobList oJobList = new JobList();
    		
    		return Response.status(Status.OK).entity(oJobList).build();
    	}
    	catch (Exception oEx) {
    		WasdiLog.debugLog("");
    		
    		ApiException oApiException = new ApiException();
    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(oApiException).build();
		}
    }		
	
	/**
	 * 
	 * @return
	 */
    @GET
    @Path("/{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJobStatus(@PathParam("jobId") String sJobId) {
    	try {
    		StatusInfo oStatusInfo = new StatusInfo();
    		
    		return Response.status(Status.OK).entity(oStatusInfo).build();
    	}
    	catch (Exception oEx) {
    		WasdiLog.debugLog("");
    		
    		ApiException oApiException = new ApiException();
    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(oApiException).build();
		}
    }	
    
	/**
	 * 
	 * @return
	 */
    @DELETE
    @Path("/{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteJob(@PathParam("jobId") String sJobId) {
    	try {
    		StatusInfo oStatusInfo = new StatusInfo();
    		
    		return Response.status(Status.OK).entity(oStatusInfo).build();
    	}
    	catch (Exception oEx) {
    		WasdiLog.debugLog("");
    		
    		ApiException oApiException = new ApiException();
    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(oApiException).build();
		}
    }
    
	/**
	 * 
	 * @return
	 */
    @GET
    @Path("/{jobId}/results")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJobResults(@PathParam("jobId") String sJobId) {
    	try {
    		Results oResults = new Results();
    		
    		return Response.status(Status.OK).entity(oResults).build();
    	}
    	catch (Exception oEx) {
    		WasdiLog.debugLog("");
    		
    		ApiException oApiException = new ApiException();
    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(oApiException).build();
		}
    }	    

}
