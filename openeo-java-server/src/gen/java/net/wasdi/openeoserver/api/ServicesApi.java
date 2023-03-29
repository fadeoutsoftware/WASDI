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

import io.swagger.annotations.ApiParam;
import net.wasdi.openeoserver.viewmodels.Error;
import net.wasdi.openeoserver.viewmodels.StoreSecondaryWebServiceRequest;
import net.wasdi.openeoserver.viewmodels.UpdateSecondaryWebServiceRequest;
import wasdi.shared.utils.log.WasdiLog;

@Path("/services")


public class ServicesApi  {

   public ServicesApi(@Context ServletConfig servletContext) {
   }

    @javax.ws.rs.POST    
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public Response createService(@NotNull @Valid  StoreSecondaryWebServiceRequest storeSecondaryWebServiceRequest,@Context SecurityContext securityContext) {
    	return Response.ok().build();
    }
    
    @javax.ws.rs.GET
    @Path("/{service_id}/logs")    
    @Produces({ "application/json" })
    public Response debugService(@PathParam("service_id") @NotNull  @Pattern(regexp="^[\\w\\-\\.~]+$") String serviceId,@ApiParam(value = "The last identifier (property `id` of a log entry) the client has received. If provided, the back-ends only sends the entries that occurred after the specified identifier. If not provided or empty, start with the first entry.") @QueryParam("offset")  String offset,@ApiParam(value = "This parameter enables pagination for the endpoint and specifies the maximum number of elements that arrays in the top-level object (e.g. collections, processes, batch jobs, secondary services, log entries, etc.) are allowed to contain. The `links` array MUST NOT be paginated like the resources, but instead contain links related to the paginated resources or the pagination itself (e.g. a link to the next page). If the parameter is not provided or empty, all elements are returned.  Pagination is OPTIONAL: back-ends or clients may not support it. Therefore it MUST be implemented in a way that clients not supporting pagination get all resources regardless. Back-ends not supporting pagination MUST return all resources.  If the response is paginated, the `links` array MUST be used to communicate the links for browsing the pagination with pre-defined `rel` types. See the `links` array schema for supported `rel` types. Backend implementations can, unless specified otherwise, use all kind of pagination techniques, depending on what is supported best by their infrastructure: page-based, offset-based, token-based or something else. The clients SHOULD use whatever is specified in the links with the corresponding `rel` types.") @QueryParam("limit")  @Min(1) Integer limit,@Context SecurityContext securityContext) {
    	
    	try {
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("ServicesApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("ServicesApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
    	return Response.ok().build();
    }
    
    @javax.ws.rs.DELETE
    @Path("/{service_id}")
    @Produces({ "application/json" })
    public Response deleteService(@PathParam("service_id") @NotNull  @Pattern(regexp="^[\\w\\-\\.~]+$") String serviceId,@Context SecurityContext securityContext) {
    	
    	try {
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("ServicesApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("ServicesApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
    	return Response.ok().build();
    }
    
    @javax.ws.rs.GET
    @Path("/{service_id}")    
    @Produces({ "application/json" })
    public Response describeService(@PathParam("service_id") @NotNull  @Pattern(regexp="^[\\w\\-\\.~]+$") String serviceId,@Context SecurityContext securityContext) {
    	
    	try {
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("ServicesApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("ServicesApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
    	return Response.ok().build();
    }
    
    @javax.ws.rs.GET
    @Produces({ "application/json" })
    public Response listServices(@QueryParam("limit")  @Min(1) Integer limit,@Context SecurityContext securityContext) {
    	
    	try {
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("ServicesApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("ServicesApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
    	return Response.ok().build();
    }
    @javax.ws.rs.PATCH
    @Path("/{service_id}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public Response updateService(@PathParam("service_id") @NotNull  @Pattern(regexp="^[\\w\\-\\.~]+$") String serviceId,@ApiParam(value = "The data to change for the specified secondary web service.", required = true) @NotNull @Valid  UpdateSecondaryWebServiceRequest updateSecondaryWebServiceRequest,@Context SecurityContext securityContext) {
    	
    	try {
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("ServicesApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("ServicesApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
    	return Response.ok().build();
    }
}
