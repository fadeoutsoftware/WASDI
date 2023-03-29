package net.wasdi.openeoserver.api;

import javax.servlet.ServletConfig;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.Response.Status;

import net.wasdi.openeoserver.viewmodels.Collection;
import net.wasdi.openeoserver.viewmodels.Collections;
import net.wasdi.openeoserver.viewmodels.Error;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.config.openEO.OpenEOCollection;
import wasdi.shared.utils.log.WasdiLog;

@Path("/collections")
public class CollectionsApi  {

   public CollectionsApi(@Context ServletConfig servletContext) {
   }

    @javax.ws.rs.GET
    @Path("/{collection_id}")
    @Produces({ "application/json" })
    public Response describeCollection(@PathParam("collection_id") @NotNull  @Pattern(regexp="^[\\w\\-\\.~/]+$") String sCollectionId, @HeaderParam("Authorization") String sAuthorization) {
    	
    	try {
    		OpenEOCollection oConfigCollection = WasdiConfig.Current.openEO.getCollection(sCollectionId);
    		
    		if (oConfigCollection == null) {
    			WasdiLog.debugLog("CollectionsApi.describeCollection: unable to find collection " + sCollectionId);
    			return Response.status(Status.NOT_FOUND).entity(Error.getError("CollectionsApi.describeCollection.Invalid", "404", "Invalid Collection")).build();
    		}
    		
    		Collection oCollection = Collections.getCollectionVMFromConfig(oConfigCollection, false);
    	
    		return Response.ok().entity(oCollection).build();
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("CollectionsApi.describeCollection error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("CollectionsApi.describeCollection", "InternalServerError", oEx.getMessage())).build();
		}
    	
    	
    }
    
    @javax.ws.rs.GET
    @Produces({ "application/json" })
    public Response listCollections(@QueryParam("limit")  @Min(1) Integer iLimit, @HeaderParam("Authorization") String sAuthorization) {
    	
    	try {
    		
    		Collections oCollections = Collections.getCollectionsFromConfig();
    		
    		return Response.ok().entity(oCollections).build();
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("CollectionsApi.describeCollection error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("CollectionsApi.describeCollection", "InternalServerError", oEx.getMessage())).build();
		}
    	
    	
    }
}
