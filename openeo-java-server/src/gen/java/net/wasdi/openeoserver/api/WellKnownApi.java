package net.wasdi.openeoserver.api;

import java.net.URI;

import javax.servlet.ServletConfig;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.wasdi.openeoserver.viewmodels.APIInstance;
import net.wasdi.openeoserver.viewmodels.Error;
import net.wasdi.openeoserver.viewmodels.WellKnownDiscovery;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.log.WasdiLog;

@Path("/.well-known/openeo")
public class WellKnownApi  {

   public WellKnownApi(@Context ServletConfig servletContext) {
   }

    @javax.ws.rs.GET
    @Produces({ "application/json" })
    public Response connect() {
    	
    	try {
    		WellKnownDiscovery oWellKnownDiscovery = new WellKnownDiscovery();
    		APIInstance oAPIInstance = new APIInstance();
    		oAPIInstance.apiVersion(WasdiConfig.Current.openEO.api_version);
    		oAPIInstance.setUrl(new URI(WasdiConfig.Current.openEO.baseAddress));
    		oWellKnownDiscovery.addVersionsItem(oAPIInstance);
    		
    		return Response.status(Status.OK).entity(oWellKnownDiscovery).build();
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("WellKnownApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("WellKnownApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    }
}
