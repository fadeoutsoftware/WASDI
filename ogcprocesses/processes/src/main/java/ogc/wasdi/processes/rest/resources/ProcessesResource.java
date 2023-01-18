package ogc.wasdi.processes.rest.resources;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import ogc.wasdi.processes.OgcProcesses;
import wasdi.shared.business.Processor;
import wasdi.shared.business.User;
import wasdi.shared.business.UserResourcePermission;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.ogcprocesses.ApiException;
import wasdi.shared.viewmodels.ogcprocesses.Execute;
import wasdi.shared.viewmodels.ogcprocesses.JobControlOptions;
import wasdi.shared.viewmodels.ogcprocesses.Link;
import wasdi.shared.viewmodels.ogcprocesses.ProcessList;
import wasdi.shared.viewmodels.ogcprocesses.ProcessSummary;
import wasdi.shared.viewmodels.ogcprocesses.StatusInfo;
import wasdi.shared.viewmodels.ogcprocesses.TransmissionMode;

@Path("processes")
public class ProcessesResource {
	
	/**
	 *  Get the list of available processes
	 * @return
	 */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProcesses(@HeaderParam("Authorization") String sAuthorization, @HeaderParam("x-session-token") String sSessionId, @QueryParam("limit") Integer oiLimit, @QueryParam("offset") Integer oiOffset) {
    	try {
    		
    		// Check if we have our session header
    		if (Utils.isNullOrEmpty(sSessionId)) {
    			// Try to get it from the basic http auth
    			sSessionId = OgcProcesses.getSessionIdFromBasicAuthentication(sAuthorization);
    		}
    		
			// Check User 
			User oUser = OgcProcesses.getUserFromSession(sSessionId);

			if (oUser==null) {
				WasdiLog.debugLog("ProcessesResource.getProcesses: invalid session");
				
	    		ApiException oApiException = new ApiException();
	    		oApiException.setType("Unauhtorized");
	    		oApiException.setTitle("Unauthorized");
	    		oApiException.setDetail("The user is not valid or the authentication data is not provided. You can use wasdi auth header or http basic auth passing userId:sessionId");
	    		oApiException.setInstance("");
	    		
	    		return Response.status(Status.UNAUTHORIZED).entity(oApiException).header("WWW-Authenticate", "Basic") .build();
			}
			
			// Initialize the pagination
			if (oiLimit==null) oiLimit = 20;
			if (oiOffset==null) oiOffset = 0;
			
    		
			// Create an instance of the return View Model
    		ProcessList oProcessList = new ProcessList();
    		
    		// Self link
    		Link oSelfLink = new Link();
    		oSelfLink.setHref(OgcProcesses.s_sBaseAddress+"processes");
    		oSelfLink.setRel("self");
    		oSelfLink.setType(WasdiConfig.Current.ogcpProcessesApi.defaultLinksType);
    		
    		oProcessList.getLinks().add(oSelfLink);
    		
    		// We will use this to verify if the user can access the processor
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			
    		// Take all the processors
    		ProcessorRepository oProcessorRepository = new ProcessorRepository();
			
			// Initialize ascending direction
			List<Processor> aoDeployed = oProcessorRepository.getDeployedProcessors("friendlyName", 1);
			
			int iAvailableApps = 0;
			
			for (int i=0; i<aoDeployed.size(); i++) {
				
				Processor oProcessor = aoDeployed.get(i);
				
				// See if this is a processor the user can access to
				if (oProcessor.getIsPublic() != 1) {
					if (oProcessor.getUserId().equals(oUser.getUserId()) == false) {
						UserResourcePermission oSharing = oUserResourcePermissionRepository.getProcessorSharingByUserIdAndProcessorId(oUser.getUserId(), oProcessor.getProcessorId());
						if (oSharing == null) continue;
					}
				}
				
				// Run until the actual offset
				if (iAvailableApps<oiOffset) {
					iAvailableApps++;
					continue; 
				}
				
				// If we are in the range offset+limit add the processor to the return list
				if (oProcessList.getProcesses().size()<oiOffset+oiLimit) {
					
					// Create the view model
					ProcessSummary oSummary = new ProcessSummary();
					
					// Copy the attributes
					oSummary.setId(oProcessor.getProcessorId());
					oSummary.setTitle(oProcessor.getName());
					oSummary.setDescription(oProcessor.getDescription());
					oSummary.setVersion(oProcessor.getVersion());
					oSummary.getJobControlOptions().add(JobControlOptions.ASYNC_EXECUTE);
					oSummary.getOutputTransmission().add(TransmissionMode.REFERENCE);
					
					for (String sCategory : oProcessor.getCategories()) {
						oSummary.getKeywords().add(sCategory);
					}
					
					// Add a link to the specific description
		    		Link oProcessorLink = new Link();
		    		oProcessorLink.setHref(OgcProcesses.s_sBaseAddress+"processes/"+oSummary.getId());
		    		oProcessorLink.setRel("self");
		    		oProcessorLink.setType(WasdiConfig.Current.ogcpProcessesApi.defaultLinksType);
		    		oProcessorLink.setTitle("Process " + oProcessor.getName() + "  Description");
		    		
		    		oSummary.getLinks().add(oProcessorLink);
					
		    		// Add the processor to our return list
					oProcessList.getProcesses().add(oSummary);
				}
				
				// Increment the number of available apps
				iAvailableApps++;
			}
			
			// Did we finish all the pages?
			if (iAvailableApps>oiOffset+oiLimit) {
				
				int iNextOffset = oiOffset+oiLimit;
				
	    		// Next link
	    		Link oNextLink = new Link();
	    		oNextLink.setHref(OgcProcesses.s_sBaseAddress+"processes?offset="+ iNextOffset +"&limit="+oiLimit.toString());
	    		oNextLink.setRel("next");
	    		oNextLink.setType(WasdiConfig.Current.ogcpProcessesApi.defaultLinksType);
	    		
	    		oProcessList.getLinks().add(oNextLink);
	    		
	    		// Prev link
	    		Link oPrevLink = new Link();
	    		oPrevLink.setHref(OgcProcesses.s_sBaseAddress+"processes?offset="+ oiOffset.toString() +"&limit="+oiLimit.toString());
	    		oPrevLink.setRel("prev");
	    		oPrevLink.setType(WasdiConfig.Current.ogcpProcessesApi.defaultLinksType);
	    		
	    		oProcessList.getLinks().add(oPrevLink);	    		
			}
    		
    		return Response.status(Status.OK).entity(oProcessList).build();
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("ProcessesResource.getProcesses: exception " + oEx.toString());
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ApiException.getInternalServerError("There was an exception getting the list of processes")).build();
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
    		WasdiLog.errorLog("ProcessesResource.getProcessDescription: exception " + oEx.toString());
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ApiException.getInternalServerError("There was an exception getting the detail of a processes")).build();
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
    		WasdiLog.errorLog("ProcessesResource.executeApplication: exception " + oEx.toString());
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ApiException.getInternalServerError("There was an exception executing the processes")).build();    		
		}
    }    

}
