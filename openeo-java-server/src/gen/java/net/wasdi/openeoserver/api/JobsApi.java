package net.wasdi.openeoserver.api;

import javax.servlet.ServletConfig;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.Response.Status;

import net.wasdi.openeoserver.viewmodels.Error;
import net.wasdi.openeoserver.viewmodels.StoreBatchJobRequest;
import net.wasdi.openeoserver.viewmodels.UpdateBatchJobRequest;
import wasdi.shared.utils.log.WasdiLog;

@Path("/jobs")


public class JobsApi  {

   public JobsApi(@Context ServletConfig servletContext) {
   }

    @javax.ws.rs.POST    
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public Response createJob(@NotNull @Valid  StoreBatchJobRequest storeBatchJobRequest,@Context SecurityContext securityContext) {
    	
    	try {
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("JobsApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("JobsApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
    	return Response.ok().build();
    }
    
    @javax.ws.rs.GET
    @Path("/{job_id}/logs")
    @Produces({ "application/json" })
    public Response debugJob(@NotNull  @Pattern(regexp="^[\\w\\-\\.~]+$") String jobId, @QueryParam("offset")  String offset, @QueryParam("limit")  @Min(1) Integer limit,@Context SecurityContext securityContext) {
    	
    	try {
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("JobsApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("JobsApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	return Response.ok().build();
    }
    
    @javax.ws.rs.DELETE
    @Path("/{job_id}")
    @Produces({ "application/json" })
    public Response deleteJob(@PathParam("job_id") @NotNull  @Pattern(regexp="^[\\w\\-\\.~]+$") String jobId,@Context SecurityContext securityContext) {
    	
    	try {
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("JobsApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("JobsApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
    	return Response.ok().build();
    }
    
    @javax.ws.rs.GET
    @Path("/{job_id}")
    @Produces({ "application/json" })
    public Response describeJob(@PathParam("job_id") @NotNull  @Pattern(regexp="^[\\w\\-\\.~]+$") String jobId,@Context SecurityContext securityContext) {
    	
    	try {
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("JobsApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("JobsApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
    	return Response.ok().build();
    }
    
    @javax.ws.rs.GET
    @Path("/{job_id}/estimate")
    @Produces({ "application/json" })
    public Response estimateJob(@PathParam("job_id") @NotNull  @Pattern(regexp="^[\\w\\-\\.~]+$") String jobId,@Context SecurityContext securityContext) {
    	
    	try {
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("JobsApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("JobsApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
    	return Response.ok().build();
    }
    
    @javax.ws.rs.GET
    @Produces({ "application/json" })
    public Response listJobs(@QueryParam("limit")  @Min(1) Integer limit,@Context SecurityContext securityContext) {
    	
    	try {
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("JobsApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("JobsApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
    	return Response.ok().build();
    }
    
    @javax.ws.rs.GET
    @Path("/{job_id}/results")
    @Produces({ "application/json", "application/geo+json" })
    public Response listResults(@PathParam("job_id") @NotNull  @Pattern(regexp="^[\\w\\-\\.~]+$") String jobId,@Context SecurityContext securityContext) {
    	
    	try {
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("JobsApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("JobsApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
    	return Response.ok().build();
    }
    
    @javax.ws.rs.POST
    @Path("/{job_id}/results")
    @Produces({ "application/json" })
    public Response startJob(@PathParam("job_id") @NotNull  @Pattern(regexp="^[\\w\\-\\.~]+$") String jobId,@Context SecurityContext securityContext) {
    	
    	try {
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("JobsApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("JobsApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
    	return Response.ok().build();
    }
    
    @javax.ws.rs.DELETE
    @Path("/{job_id}/results")
    @Produces({ "application/json" })
    public Response stopJob(@PathParam("job_id") @NotNull  @Pattern(regexp="^[\\w\\-\\.~]+$") String jobId,@Context SecurityContext securityContext) {
    	
    	try {
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("JobsApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("JobsApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
    	return Response.ok().build();
    }
    @javax.ws.rs.PATCH
    public Response updateJob(@PathParam("job_id") @NotNull  @Pattern(regexp="^[\\w\\-\\.~]+$") String jobId, @NotNull @Valid  UpdateBatchJobRequest updateBatchJobRequest,@Context SecurityContext securityContext) {
    	
    	try {
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("JobsApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("JobsApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
    	return Response.ok().build();
    }
}
