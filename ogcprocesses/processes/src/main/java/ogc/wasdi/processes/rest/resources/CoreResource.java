package ogc.wasdi.processes.rest.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import ogc.wasdi.processes.OgcProcesses;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.ogcprocesses.ApiException;
import wasdi.shared.viewmodels.ogcprocesses.Conformance;
import wasdi.shared.viewmodels.ogcprocesses.LandingPage;
import wasdi.shared.viewmodels.ogcprocesses.Link;

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
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_HTML})
    public Response getLandingPage() {
    	try {
    		
    		WasdiLog.debugLog("CoreResource.getLandingPage");
    		
    		// Create the output view model
    		LandingPage oLandingPage = new LandingPage();
    		
    		// Set title and description from configuration    		
    		oLandingPage.setTitle(WasdiConfig.Current.ogcProcessesApi.landingTitle);
    		oLandingPage.setDescription(WasdiConfig.Current.ogcProcessesApi.landingDescription);
    		
    		// Link 1: API Service Definition
    		String sAPIDefinitionLink = WasdiConfig.Current.ogcProcessesApi.landingLinkServiceDefinition;
    		
    		Link oAPIDefinitionLink = new Link();
    		
    		// Default Values
			oAPIDefinitionLink.setHref("https://docs.ogc.org/is/18-062r2/18-062r2.html");
			oAPIDefinitionLink.setRel("service-doc");
			oAPIDefinitionLink.setTitle("APIDefinition");
			
			if (!Utils.isNullOrEmpty(sAPIDefinitionLink)) {
				// Take what we have from config
	    		String []asLinkParts = sAPIDefinitionLink.split(";");
	    		
	    		if (asLinkParts != null) {
	    			if (asLinkParts.length>0) {
	    				oAPIDefinitionLink.setHref(asLinkParts[0]);
	    			}
	    			
	    			if (asLinkParts.length>1) {
	    				oAPIDefinitionLink.setRel(asLinkParts[1]);
	    			}
	    			
	    			if (asLinkParts.length>2) {
	    				oAPIDefinitionLink.setTitle(asLinkParts[2]);
	    			}    			
	    		}				
			}
    		    		
    		oAPIDefinitionLink.setHreflang(WasdiConfig.Current.ogcProcessesApi.defaultLinksLang);
    		oAPIDefinitionLink.setType(WasdiConfig.Current.ogcProcessesApi.defaultLinksType);
    		
    		// Link 2: Conformance
    		String sConformanceLink = WasdiConfig.Current.ogcProcessesApi.landingLinkConformance;
    		
    		Link oConformanceLink = new Link();
    		
    		// Default Values
    		oConformanceLink.setHref(OgcProcesses.s_sBaseAddress + "conformance");
    		oConformanceLink.setRel("http://www.opengis.net/def/rel/ogc/1.0/conformance");
    		oConformanceLink.setTitle("OGC API - Processes conformance classes implemented");
    		
    		if (!Utils.isNullOrEmpty(sConformanceLink)) {
    			// Take what we have from config
        		String [] asLinkParts = sConformanceLink.split(";");
        		
        		if (asLinkParts != null) {
        			if (asLinkParts.length>0) {
        				oConformanceLink.setHref(OgcProcesses.s_sBaseAddress + asLinkParts[0]);
        			}
        			
        			if (asLinkParts.length>1) {
        				oConformanceLink.setRel(asLinkParts[1]);
        			}
        			
        			if (asLinkParts.length>2) {
        				oConformanceLink.setTitle(asLinkParts[2]);
        			}    			
        		}    			
    		}
    		    		
    		oConformanceLink.setHreflang(WasdiConfig.Current.ogcProcessesApi.defaultLinksLang);
    		oConformanceLink.setType(WasdiConfig.Current.ogcProcessesApi.defaultLinksType);
    		
    		// Link 3: Processes
    		String sProcessesLink = WasdiConfig.Current.ogcProcessesApi.landingLinkProcesses;
    		
    		Link oProcessesLink = new Link();
    		
    		// Default Values
    		oProcessesLink.setHref(OgcProcesses.s_sBaseAddress + "processes");
    		oProcessesLink.setRel("http://www.opengis.net/def/rel/ogc/1.0/processes");
    		oProcessesLink.setTitle("Metadata about the processes");
    		
    		if (!Utils.isNullOrEmpty(sProcessesLink)) {    			
    			// Take what we have from config
        		String [] asLinkParts = sProcessesLink.split(";");
        		
        		if (asLinkParts != null) {
        			if (asLinkParts.length>0) {
        				oProcessesLink.setHref(OgcProcesses.s_sBaseAddress + asLinkParts[0]);
        			}
        			
        			if (asLinkParts.length>1) {
        				oProcessesLink.setRel(asLinkParts[1]);
        			}
        			
        			if (asLinkParts.length>2) {
        				oProcessesLink.setTitle(asLinkParts[2]);
        			}    			
        		}
    		}
    		    		
    		oProcessesLink.setHreflang(WasdiConfig.Current.ogcProcessesApi.defaultLinksLang);
    		oProcessesLink.setType(WasdiConfig.Current.ogcProcessesApi.defaultLinksType);
    		
    		// Link 3: Processes
    		String sJobsLink = WasdiConfig.Current.ogcProcessesApi.landingLinkProcesses;
    		
    		Link oJobsLink = new Link();
    		
    		// Default Values
    		oJobsLink.setHref(OgcProcesses.s_sBaseAddress + "processes");
    		oJobsLink.setRel("http://www.opengis.net/def/rel/ogc/1.0/processes");
    		oJobsLink.setTitle("Metadata about the processes");
    		
    		if (!Utils.isNullOrEmpty(sJobsLink)) {    			
    			// Take what we have from config
        		String [] asLinkParts = sJobsLink.split(";");
        		
        		if (asLinkParts != null) {
        			if (asLinkParts.length>0) {
        				oJobsLink.setHref(OgcProcesses.s_sBaseAddress + asLinkParts[0]);
        			}
        			
        			if (asLinkParts.length>1) {
        				oJobsLink.setRel(asLinkParts[1]);
        			}
        			
        			if (asLinkParts.length>2) {
        				oJobsLink.setTitle(asLinkParts[2]);
        			}    			
        		}
    		}
    		    		
    		oJobsLink.setHreflang(WasdiConfig.Current.ogcProcessesApi.defaultLinksLang);
    		oJobsLink.setType(WasdiConfig.Current.ogcProcessesApi.defaultLinksType);    		
    		
    		oLandingPage.getLinks().add(oAPIDefinitionLink);
    		oLandingPage.getLinks().add(oConformanceLink);
    		oLandingPage.getLinks().add(oProcessesLink);
    		oLandingPage.getLinks().add(oJobsLink);
    		
    		// Self link
    		Link oSelfLink = new Link();
    		oSelfLink.setHref(OgcProcesses.s_sBaseAddress);
    		oSelfLink.setRel("self");
    		oSelfLink.setType(WasdiConfig.Current.ogcProcessesApi.defaultLinksType);
    		
    		oLandingPage.getLinks().add(oSelfLink);
    		
    		// Alternate html link
    		Link oHtmlLink = new Link();
    		oHtmlLink.setHref(OgcProcesses.s_sBaseAddress);
    		oHtmlLink.setRel("alternate");
    		oHtmlLink.setType("text/html");
    		
    		oLandingPage.getLinks().add(oHtmlLink);
    		
    		// Link service-desc: while the specification states service-doc OR service-desc, the 
    		String sAPIDescriptionLink = WasdiConfig.Current.ogcProcessesApi.landingLinkServiceDefinition;
    		
    		Link oAPIDescriptionLink = new Link();
    		
    		// Default Values
    		oAPIDescriptionLink.setHref("https://developer.ogc.org/api/processes/swaggerui.html");
    		oAPIDescriptionLink.setRel("service-doc");
    		oAPIDescriptionLink.setTitle("APIDefinition");
			
			if (!Utils.isNullOrEmpty(sAPIDescriptionLink)) {
				// Take what we have from config
	    		String []asLinkParts = sAPIDescriptionLink.split(";");
	    		
	    		if (asLinkParts != null) {
	    			if (asLinkParts.length>0) {
	    				oAPIDefinitionLink.setHref(asLinkParts[0]);
	    			}
	    			
	    			if (asLinkParts.length>1) {
	    				oAPIDescriptionLink.setRel(asLinkParts[1]);
	    			}
	    			
	    			if (asLinkParts.length>2) {
	    				oAPIDescriptionLink.setTitle(asLinkParts[2]);
	    			}    			
	    		}				
			}
    		    		
			oAPIDescriptionLink.setHreflang(WasdiConfig.Current.ogcProcessesApi.defaultLinksLang);
			oAPIDescriptionLink.setType(WasdiConfig.Current.ogcProcessesApi.defaultLinksType);
			oLandingPage.getLinks().add(oAPIDescriptionLink);
    		
    		
    		ResponseBuilder oResponse = Response.status(Status.OK).entity(oLandingPage);
    		oResponse = OgcProcesses.addLinkHeaders(oResponse, oLandingPage.getLinks());
    		return oResponse.build();
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("CoreResource.getLandingPage: exception " + oEx.toString());
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ApiException.getInternalServerError()).build();    		
		}
    }

    /**
     * Get the list of conformances
     * @return Conformance View Model
     */
    @GET
    @Path("conformance")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_HTML})
    public Response getConformance() {
    	try {
    		WasdiLog.debugLog("CoreResource.getConformance");
    		
    		Conformance oConformance = new Conformance();
    		
    		String [] asConforms = WasdiConfig.Current.ogcProcessesApi.conformsTo.split(";");
    		
    		for (String sConformsTo : asConforms) {
    			oConformance.getConformsTo().add(sConformsTo);
			}
    		
    		ResponseBuilder oResponse = Response.status(Status.OK).entity(oConformance);
    		oResponse = OgcProcesses.addLinkHeaders(oResponse, null);
    		return oResponse.build();
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("CoreResource.getConformance: exception " + oEx.toString());
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ApiException.getInternalServerError()).build();    		
		}
    }
}
