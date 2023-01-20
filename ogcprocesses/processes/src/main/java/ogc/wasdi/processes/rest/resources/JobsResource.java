package ogc.wasdi.processes.rest.resources;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.core.type.TypeReference;

import ogc.wasdi.processes.OgcProcesses;
import wasdi.shared.business.Node;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.Processor;
import wasdi.shared.business.User;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.ogcprocesses.ApiException;
import wasdi.shared.viewmodels.ogcprocesses.JobList;
import wasdi.shared.viewmodels.ogcprocesses.Results;
import wasdi.shared.viewmodels.ogcprocesses.StatusCode;
import wasdi.shared.viewmodels.ogcprocesses.StatusInfo;
import wasdi.shared.viewmodels.ogcprocesses.StatusInfo.TypeEnum;
import wasdi.shared.viewmodels.processworkspace.ProcessWorkspaceViewModel;

@Path("jobs")
public class JobsResource {
	

	/**
	 * Get a list of the OGC jobs for this user.
	 * The API will call all the active WASDI nodes to retrive users' ogc processes
	 * convert the results in OGC view model and return
	 * 
	 * @param sAuthorization Http basic authorization header value
	 * @param sSessionId WASDI standard session token
	 * @param oiLimit Optional limit to return list
	 * @param oiOffset Optional offset to return list
	 * @return Job List View Model 
	 */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_HTML})
    public Response getJobsList(@HeaderParam("Authorization") String sAuthorization, @HeaderParam("x-session-token") String sSessionId, @QueryParam("limit") Integer oiLimit, @QueryParam("offset") Integer oiOffset) {
    	try {
    		
			// Check User 
			User oUser = OgcProcesses.getUserFromSession(sSessionId, sAuthorization);

			if (oUser==null) {
				WasdiLog.debugLog("ProcessesResource.getProcesses: invalid session");
	    		return Response.status(Status.UNAUTHORIZED).entity(ApiException.getUnauthorized()).header("WWW-Authenticate", "Basic").build();
			}
			
			// Initialize the pagination
			if (oiLimit==null) oiLimit = 20;
			if (oiOffset==null) oiOffset = 0;
			
			NodeRepository oNodeRepo = new NodeRepository();
			List<Node> aoNodes = oNodeRepo.getNodesList();
			
			ArrayList<ProcessWorkspaceViewModel> aoAllProcs = new ArrayList<>();
			
			for (Node oNode : aoNodes) {
				
				//if (oNode.getNodeCode().equals("wasdi")) continue;					
				if (oNode.getActive() == false) continue;
				
				try {
					String sUrl = oNode.getNodeBaseAddress();
					
					if (!sUrl.endsWith("/")) sUrl += "/";
					
					sUrl += "process/byusr?ogc=true";
					
					Map<String, String> asHeaders = new HashMap<String, String>();
					asHeaders.put("x-session-token", sSessionId);
					
					WasdiLog.debugLog("ProcessesResource.getProcesses: calling url: " + sUrl);
					
					String sResponse = HttpUtils.httpGet(sUrl, asHeaders);
					
					if (Utils.isNullOrEmpty(sResponse)==false) {
						ArrayList<ProcessWorkspaceViewModel> aoProcWs = MongoRepository.s_oMapper.readValue(sResponse, new TypeReference<ArrayList<ProcessWorkspaceViewModel>>(){});
						aoAllProcs.addAll(aoProcWs);
					}
					
				}
				catch (Exception e) {
					WasdiLog.errorLog("ProcessWorkspaceResource.getProcessByApplication: exception contacting computing node: " + e.toString());
				}
			}
						
			
    		JobList oJobList = new JobList();
    		
    		if (aoAllProcs.size()>0) {
    			for (int iProcs = oiOffset; iProcs<oiOffset+oiLimit; oiOffset++) {
    				
    				if (iProcs>=aoAllProcs.size()) break;
    				
    				ProcessWorkspaceViewModel oProcWs = aoAllProcs.get(iProcs);
    				
    				StatusInfo oStatusInfo = getStatusInfoFromProcessWorkspaceViewModel(oProcWs);
    				
    				if (oStatusInfo!=null) {
    					oJobList.getJobs().add(oStatusInfo);
    				}    				
    			}
    		}
    		
    		ResponseBuilder oResponse = Response.status(Status.OK).entity(oJobList);
    		oResponse = OgcProcesses.addLinkHeaders(oResponse, oJobList.getLinks());
    		return oResponse.build();
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("JobsResource.getJobsList: exception " + oEx.toString());
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ApiException.getInternalServerError("There was an error getting the job list")).build();    		
		}
    }		
	
	/**
	 * 
	 * @return
	 */
    @GET
    @Path("/{jobId}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_HTML})
    public Response getJobStatus(@HeaderParam("Authorization") String sAuthorization, @HeaderParam("x-session-token") String sSessionId, @PathParam("jobId") String sJobId) {
    	try {
			// Check User 
			User oUser = OgcProcesses.getUserFromSession(sSessionId, sAuthorization);

			if (oUser==null) {
				WasdiLog.debugLog("ProcessesResource.getProcesses: invalid session");
	    		return Response.status(Status.UNAUTHORIZED).entity(ApiException.getUnauthorized()).header("WWW-Authenticate", "Basic").build();
			}
			
    		StatusInfo oStatusInfo = new StatusInfo();
    		
    		ResponseBuilder oResponse = Response.status(Status.OK).entity(oStatusInfo);
    		oResponse = OgcProcesses.addLinkHeaders(oResponse, oStatusInfo.getLinks());
    		return oResponse.build();
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("JobsResource.getJobStatus: exception " + oEx.toString());
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ApiException.getInternalServerError("There was an error getting the job status")).build();    		
		}
    }	
    
	/**
	 * 
	 * @return
	 */
    @DELETE
    @Path("/{jobId}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_HTML})
    public Response deleteJob(@HeaderParam("Authorization") String sAuthorization, @HeaderParam("x-session-token") String sSessionId, @PathParam("jobId") String sJobId) {
    	try {
			// Check User 
			User oUser = OgcProcesses.getUserFromSession(sSessionId, sAuthorization);

			if (oUser==null) {
				WasdiLog.debugLog("ProcessesResource.getProcesses: invalid session");
	    		return Response.status(Status.UNAUTHORIZED).entity(ApiException.getUnauthorized()).header("WWW-Authenticate", "Basic").build();
			}

			
    		StatusInfo oStatusInfo = new StatusInfo();
    		
    		ResponseBuilder oResponse = Response.status(Status.OK).entity(oStatusInfo);
    		oResponse = OgcProcesses.addLinkHeaders(oResponse, oStatusInfo.getLinks());
    		return oResponse.build();    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("JobsResource.deleteJob: exception " + oEx.toString());
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ApiException.getInternalServerError("There was an error deleting the job")).build();    		
		}
    }
    
	/**
	 * 
	 * @return
	 */
    @GET
    @Path("/{jobId}/results")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_HTML})
    public Response getJobResults(@HeaderParam("Authorization") String sAuthorization, @HeaderParam("x-session-token") String sSessionId, @PathParam("jobId") String sJobId) {
    	try {
    		Results oResults = new Results();
    		
    		return Response.status(Status.OK).entity(oResults).build();
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("JobsResource.getJobResults: exception " + oEx.toString());
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ApiException.getInternalServerError("There was an error getting the job results")).build();    		
		}
    }
    
    
    /**
     * Converts a WASDI ProcessWorkspace View Model in an OGC Processes API Status Info
     * @param oProcWs WASDI ProcessWorkspace View Model
     * @return OGC Processes API Status Info
     */
	protected StatusInfo getStatusInfoFromProcessWorkspaceViewModel(ProcessWorkspaceViewModel oProcWs) {
		
		
    	try {
    		StatusInfo oInfo = new StatusInfo();
    		
    		oInfo.setCreated(Utils.getWasdiDate(oProcWs.getOperationDate()));
    		
    		if (!Utils.isNullOrEmpty(oProcWs.getOperationEndDate())) {
    			oInfo.setFinished(Utils.getWasdiDate(oProcWs.getOperationEndDate()));
    		}
    		
    		oInfo.setJobID(oProcWs.getProcessObjId());
    		oInfo.setMessage(oProcWs.getProductName());
    		
    		ProcessorRepository oProcessorRepository = new ProcessorRepository();
    		Processor oProcessor = oProcessorRepository.getProcessorByName(oProcWs.getProductName());
    		
    		if (oProcessor!=null) {
    			oInfo.setProcessID(oProcessor.getProcessorId());
    		}
    		
    		oInfo.setProgress(oProcWs.getProgressPerc());
    		
    		if (Utils.isNullOrEmpty(oProcWs.getOperationStartDate())) {
    			oInfo.setStarted(Utils.getWasdiDate(oProcWs.getOperationStartDate()));
    		}
    		StatusCode eStatus = StatusCode.ACCEPTED;
    		
    		String sWasdiStatus = oProcWs.getStatus();
    		
    		if (sWasdiStatus.equals(ProcessStatus.READY.name()) || sWasdiStatus.equals(ProcessStatus.WAITING.name()) || sWasdiStatus.equals(ProcessStatus.RUNNING.name())) {
    			eStatus = StatusCode.RUNNING;
    		}
    		else if (sWasdiStatus.equals(ProcessStatus.DONE.name())) {
    			eStatus = StatusCode.SUCCESSFUL;
    		}
    		else if (sWasdiStatus.equals(ProcessStatus.ERROR.name())) {
    			eStatus = StatusCode.FAILED;
    		}
    		else if (sWasdiStatus.equals(ProcessStatus.STOPPED.name())) {
    			eStatus = StatusCode.DISMISSED;
    		}
    		
    		oInfo.setStatus(eStatus);
    		oInfo.setType(TypeEnum.PROCESS);
    		oInfo.setUpdated(new Date());
    		
    		return oInfo;

    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("JobsResource.getStatusInfoFromProcessWorkspaceViewModel: exception " + oEx.toString());
		}
    	
		return null;
	}


}
