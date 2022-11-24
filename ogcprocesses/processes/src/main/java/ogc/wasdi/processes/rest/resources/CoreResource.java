package ogc.wasdi.processes.rest.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.ogcprocesses.ApiException;
import wasdi.shared.viewmodels.ogcprocesses.Conformance;
import wasdi.shared.viewmodels.ogcprocesses.LandingPage;

/**
 * Base Operations
 */
@Path("")
public class CoreResource {


	/**
	 * Get the landing page
	 * @return LandingPage View Model
	 */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLandingPage() {
    	try {
    		LandingPage oLandingPage = new LandingPage();
    		
    		return Response.status(Status.OK).entity(oLandingPage).build();
    	}
    	catch (Exception oEx) {
    		Utils.debugLog("");
    		
    		ApiException oApiException = new ApiException();
    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(oApiException).build();
		}
    }

    /**
     * Get the list of conformances
     * @return Conformance View Model
     */
    @GET
    @Path("conformance")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConformance() {
    	try {
    		Conformance oConformance = new Conformance();
    		
    		return Response.status(Status.OK).entity(oConformance).build();
    	}
    	catch (Exception oEx) {
    		Utils.debugLog("");
    		
    		ApiException oApiException = new ApiException();
    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(oApiException).build();
		}
    }
}
