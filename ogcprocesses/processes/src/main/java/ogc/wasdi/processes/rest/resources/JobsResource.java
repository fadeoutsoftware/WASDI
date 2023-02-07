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
import wasdi.shared.business.OgcProcessesTask;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Processor;
import wasdi.shared.business.User;
import wasdi.shared.business.Workspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.OgcProcessesTaskRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.ogcprocesses.ApiException;
import wasdi.shared.viewmodels.ogcprocesses.JobList;
import wasdi.shared.viewmodels.ogcprocesses.Link;
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
    public Response getJobsList(@HeaderParam("Authorization") String sAuthorization, @HeaderParam("x-session-token") String sSessionId, @QueryParam("limit") Integer oiLimit, 
    		@QueryParam("offset") Integer oiOffset, @QueryParam("type") String [] asTypes, @QueryParam("processID") String [] asProcesses, 
    		@QueryParam("status") String [] asStatus, @QueryParam("datetime") String sDateTime, 
    		@QueryParam("minDuration") Integer [] aiMinDuration, @QueryParam("maxDuration") Integer [] aiMaxDuration) {
    	try {
    		WasdiLog.debugLog("JobsResource.getJobsList");
    		
			// Check User 
			User oUser = OgcProcesses.getUserFromSession(sSessionId, sAuthorization);

			if (oUser==null) {
				WasdiLog.debugLog("JobsResource.getJobsList: invalid session");
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
					
					WasdiLog.debugLog("ProcessesResource.getJobsList: calling url: " + sUrl);
					
					String sResponse = HttpUtils.httpGet(sUrl, asHeaders);
					
					if (Utils.isNullOrEmpty(sResponse)==false) {
						ArrayList<ProcessWorkspaceViewModel> aoProcWs = MongoRepository.s_oMapper.readValue(sResponse, new TypeReference<ArrayList<ProcessWorkspaceViewModel>>(){});
						aoAllProcs.addAll(aoProcWs);
					}
					
				}
				catch (Exception e) {
					WasdiLog.errorLog("JobsResource.getJobsList: exception contacting computing node: " + e.toString());
				}
			}
						
			
    		JobList oJobList = new JobList();
    		
    		if (aoAllProcs.size()>0) {
    			
    			// Apply the requierd filters
    			filterProcessWorkspaceListFromProcesses(aoAllProcs, asProcesses);
    			filterProcessWorkspaceListFromStatus(aoAllProcs, asStatus);
    			filterProcessWorkspaceListFromDateTime(aoAllProcs, sDateTime);
    			filterProcessWorkspaceListFromMinDuration(aoAllProcs, aiMinDuration);
    			filterProcessWorkspaceListFromMaxDuration(aoAllProcs, aiMaxDuration);
    			
    			// Make a cycle to the filtered results to extract the valid ones
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
     * Get the status of a single job
	 * @param sAuthorization Http basic authorization header value
	 * @param sSessionId WASDI standard session token
     * @param sJobId Job/Process Workspace Identifier
     * @return Status Info View Model with info about the specific Job
     */
    @GET
    @Path("/{jobId}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_HTML})
    public Response getJobStatus(@HeaderParam("Authorization") String sAuthorization, @HeaderParam("x-session-token") String sSessionId, @PathParam("jobId") String sJobId) {
    	try {
    		if (sJobId==null) sJobId ="";
    		WasdiLog.debugLog("JobsResource.getJobStatus jobid=" + sJobId);
    		
			// Check User 
			User oUser = OgcProcesses.getUserFromSession(sSessionId, sAuthorization);

			if (oUser==null) {
				WasdiLog.debugLog("JobsResource.getJobStatus: invalid session");
	    		return Response.status(Status.UNAUTHORIZED).entity(ApiException.getUnauthorized()).header("WWW-Authenticate", "Basic").build();
			}
			
			// Read the index of OGC processes to check if we have it
			OgcProcessesTaskRepository oOgcProcessesTaskRepository = new OgcProcessesTaskRepository();
			OgcProcessesTask oOgcProcessesTask = oOgcProcessesTaskRepository.getOgcProcessesTask(sJobId);
			
			// Null means not found
			if (oOgcProcessesTask == null) {
				WasdiLog.debugLog("JobsResource.getJobStatus: OGC Processes Task not found");
				ApiException oApiException = getNotFoundException(sJobId);
				return Response.status(Status.NOT_FOUND).entity(oApiException).build();
			}
			
			// Maybe we found it, but can the user access it also?
			if (oOgcProcessesTask.getUserId().equals(oUser.getUserId())) {
				WasdiLog.debugLog("JobsResource.getJobStatus: OGC Processes Task exists but is not of the user " + oUser.getUserId());
				ApiException oApiException = getNotFoundException(sJobId);
				return Response.status(Status.NOT_FOUND).entity(oApiException).build();				
			}
			
			// This must be in a valid workspace
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			Workspace oWorkspace = oWorkspaceRepository.getWorkspace(oOgcProcessesTask.getWorkspaceId());
			
			if (oWorkspace == null)  {
				WasdiLog.debugLog("JobsResource.getJobStatus: The workspace does not exists " + oOgcProcessesTask.getProcessWorkspaceId() );
				ApiException oApiException = getNotFoundException(sJobId);
				return Response.status(Status.NOT_FOUND).entity(oApiException).build();				
			}		
			    		
    		// Security check: in case of no node we assume main node
    		if (Utils.isNullOrEmpty(oWorkspace.getNodeCode())) {
    			oWorkspace.setNodeCode("wasdi");
    		}
    		
    		// We need the process Workspace View Model
    		ProcessWorkspaceViewModel oProcWsViewModel = null;
    		
    		// Check the node
    		if (oWorkspace.getNodeCode().equals(WasdiConfig.Current.nodeCode)) {
    			// We are in this node: we can read directly from database
    			ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
    			ProcessWorkspace oProcessWorkspace = oProcessWorkspaceRepository.getProcessByProcessObjId(sJobId);
    			
    			if (oProcessWorkspace == null) {
    				WasdiLog.debugLog("JobsResource.getJobStatus: The corresponding processes workspace has not been found in node " + oWorkspace.getNodeCode());
    				ApiException oApiException = getNotFoundException(sJobId);
    				return Response.status(Status.NOT_FOUND).entity(oApiException).build();    				
    			}
    			
    			// Build the view model
    			oProcWsViewModel = ProcessWorkspaceViewModel.buildProcessWorkspaceViewModel(oProcessWorkspace);
    		}
    		else {
    			// Ask the Proc Workspace to the right node
    			NodeRepository oNodeRepository = new NodeRepository();
    			Node oNode = oNodeRepository.getNodeByCode(oWorkspace.getNodeCode());
    			oProcWsViewModel = readProcessWorkspaceFromNode(sJobId, oNode, sSessionId);
    		}
    		
    		if (oProcWsViewModel == null) {
    			WasdiLog.debugLog("JobsResource.getJobStatus: Impossible to find the corresponding Process Workspace");
				ApiException oApiException = getNotFoundException(sJobId);
				return Response.status(Status.NOT_FOUND).entity(oApiException).build();    			
    		}
    		
			// Here all should be ok: create the return View Model
    		StatusInfo oStatusInfo = getStatusInfoFromProcessWorkspaceViewModel(oProcWsViewModel);
    		
    		if (oStatusInfo == null) {
    			WasdiLog.debugLog("JobsResource.getJobStatus: error creating the status info");
				ApiException oApiException = getNotFoundException(sJobId);
				return Response.status(Status.NOT_FOUND).entity(oApiException).build();        			
    		}

    		// Self link
    		Link oSelfLink = new Link();
    		oSelfLink.setHref(OgcProcesses.s_sBaseAddress+"jobs/"+sJobId);
    		oSelfLink.setRel("self");
    		oSelfLink.setType(WasdiConfig.Current.ogcpProcessesApi.defaultLinksType);
    		
    		oStatusInfo.getLinks().add(oSelfLink);
    		
    		// Alternate html link
    		Link oHtmlLink = new Link();
    		oHtmlLink.setHref(OgcProcesses.s_sBaseAddress+"jobs/"+sJobId);
    		oHtmlLink.setRel("alternate");
    		oHtmlLink.setType("text/html");
    		
    		oStatusInfo.getLinks().add(oHtmlLink);    		
    		
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
     * Delete/Dismiss a running job
     *  
	 * @param sAuthorization Http basic authorization header value
	 * @param sSessionId WASDI standard session token
     * @param sJobId Job/Process Workspace Identifier
     * @return Status Info View Model with info about the specific Job
     */
    @DELETE
    @Path("/{jobId}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_HTML})
    public Response deleteJob(@HeaderParam("Authorization") String sAuthorization, @HeaderParam("x-session-token") String sSessionId, @PathParam("jobId") String sJobId) {
    	try {
    		if (sJobId==null) sJobId ="";
    		WasdiLog.debugLog("JobsResource.deleteJob jobid=" +sJobId);
    		
			// Check User 
			User oUser = OgcProcesses.getUserFromSession(sSessionId, sAuthorization);

			if (oUser==null) {
				WasdiLog.debugLog("JobsResource.deleteJob: invalid session");
	    		return Response.status(Status.UNAUTHORIZED).entity(ApiException.getUnauthorized()).header("WWW-Authenticate", "Basic").build();
			}
			
			// Read the index of OGC processes to check if we have it
			OgcProcessesTaskRepository oOgcProcessesTaskRepository = new OgcProcessesTaskRepository();
			OgcProcessesTask oOgcProcessesTask = oOgcProcessesTaskRepository.getOgcProcessesTask(sJobId);
			
			// Null means not found
			if (oOgcProcessesTask == null) {
				WasdiLog.debugLog("JobsResource.deleteJob: OGC Processes Task not found");
				ApiException oApiException = getNotFoundException(sJobId);
				return Response.status(Status.NOT_FOUND).entity(oApiException).build();
			}
			
			// Maybe we found it, but can the user access it also?
			if (oOgcProcessesTask.getUserId().equals(oUser.getUserId())) {
				WasdiLog.debugLog("JobsResource.deleteJob: OGC Processes Task exists but is not of the user " + oUser.getUserId());
				ApiException oApiException = getNotFoundException(sJobId);
				return Response.status(Status.NOT_FOUND).entity(oApiException).build();				
			}
			
			// This must be in a valid workspace
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			Workspace oWorkspace = oWorkspaceRepository.getWorkspace(oOgcProcessesTask.getWorkspaceId());
			
			if (oWorkspace == null)  {
				WasdiLog.debugLog("JobsResource.deleteJob: The workspace does not exists " + oOgcProcessesTask.getProcessWorkspaceId() );
				ApiException oApiException = getNotFoundException(sJobId);
				return Response.status(Status.NOT_FOUND).entity(oApiException).build();				
			}		
			    		
    		// Security check: in case of no node we assume main node
    		if (Utils.isNullOrEmpty(oWorkspace.getNodeCode())) {
    			oWorkspace.setNodeCode("wasdi");
    		}
    		
    		// We need the process Workspace View Model
    		ProcessWorkspaceViewModel oProcWsViewModel = null;
    		
    		// Check the node
    		if (oWorkspace.getNodeCode().equals(WasdiConfig.Current.nodeCode)) {
    			// We are in this node: we can read directly from database
    			ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
    			ProcessWorkspace oProcessWorkspace = oProcessWorkspaceRepository.getProcessByProcessObjId(sJobId);
    			
    			if (oProcessWorkspace == null) {
    				WasdiLog.debugLog("JobsResource.deleteJob: The corresponding processes workspace has not been found in node " + oWorkspace.getNodeCode());
    				ApiException oApiException = getNotFoundException(sJobId);
    				return Response.status(Status.NOT_FOUND).entity(oApiException).build();    				
    			}
    			
    			// Build the view model
    			oProcWsViewModel = ProcessWorkspaceViewModel.buildProcessWorkspaceViewModel(oProcessWorkspace);
    		}
    		else {
    			// Ask the Proc Workspace to the right node
    			NodeRepository oNodeRepository = new NodeRepository();
    			Node oNode = oNodeRepository.getNodeByCode(oWorkspace.getNodeCode());
    			oProcWsViewModel = readProcessWorkspaceFromNode(sJobId, oNode, sSessionId);
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
    		if (sJobId==null) sJobId ="";
    		WasdiLog.debugLog("JobsResource.getJobResults jobid=" + sJobId);
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

	/**
	 * Get the job-not-found API Exception 
	 * @param sJobId Process Workspace Id 
	 * @return ApiException with code 404 not found
	 */
	protected ApiException getNotFoundException(String sJobId) {
		ApiException oApiException = new ApiException();
		oApiException.setTitle("no-such-process");
		oApiException.setType("http://www.opengis.net/def/exceptions/ogcapi-processes-1/1.0/no-such-job");
		
		if (Utils.isNullOrEmpty(sJobId)) sJobId = "";
		
		String sDetail = "Processor with id \"" + sJobId + "\" not found";
		oApiException.setDetail(sDetail);
		oApiException.setStatus(404);
		return oApiException;		
	}
	
	/**
	 * Reas a single Process Workspace from a Node using WASDI API
	 * @param sProcessWorkspaceId Id of the process Workspace
	 * @param oNode Node Entity
	 * @param sSessionId Actual Session Id
	 * @return The Process Workspace View Model if available or null
	 */
	protected ProcessWorkspaceViewModel readProcessWorkspaceFromNode(String sProcessWorkspaceId, Node oNode, String sSessionId) {
		try {
			if (oNode.getActive()==false) return null;
			
			String sUrl = oNode.getNodeBaseAddress();
			
			if (!sUrl.endsWith("/")) sUrl += "/";
			
			sUrl += "process/byid?procws="+sProcessWorkspaceId;
			
			Map<String, String> asHeaders = new HashMap<String, String>();
			asHeaders.put("x-session-token", sSessionId);
			
			WasdiLog.debugLog("JobsResource.readProcessWorkspaceFromNode: calling url: " + sUrl);
			
			String sResponse = HttpUtils.httpGet(sUrl, asHeaders);
			
			if (Utils.isNullOrEmpty(sResponse)==false) {
				ProcessWorkspaceViewModel oProcWs = MongoRepository.s_oMapper.readValue(sResponse, ProcessWorkspaceViewModel.class);
				return oProcWs;
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("JobsResource.readProcessWorkspaceFromNode: exception contacting computing node: " + oEx.toString());
		}		
		
		return null;
	}
	
	/**
	 * Filters the full list of OGC Processes based on a list of specific proc id
	 * @param aoProcWsList List of all the OGC Processes
	 * @param asProcesses List of allowed Process Workspace Id
	 */
	protected void filterProcessWorkspaceListFromProcesses(List<ProcessWorkspaceViewModel> aoProcWsList, String [] asProcesses) {
		try {
			if (aoProcWsList == null) return;
			if (aoProcWsList.size()==0) return;
			if (asProcesses == null) return;
			if (asProcesses.length == 0) return;
			
			for (int iProcIndex = aoProcWsList.size()-1; iProcIndex>=0; iProcIndex--) {
				ProcessWorkspaceViewModel oVM = aoProcWsList.get(iProcIndex);
				
				boolean bFound = false;
				
				for (String sProcId : asProcesses) {
					if (oVM.getProcessObjId().equals(sProcId)) {
						bFound = true;
						break;
					}
				}
				
				if (!bFound) {
					aoProcWsList.remove(iProcIndex);
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("JobsResource.filterProcessWorkspaceListFromProcesses: exception : " + oEx.toString());
		}
	}
	
	/**
	 * Filters the full list of OGC Processes based on the status filter
	 * @param aoProcWsList List of all the OGC Processes
	 * @param asProcesses List of allowed Process Workspace Id
	 */
	protected void filterProcessWorkspaceListFromStatus(List<ProcessWorkspaceViewModel> aoProcWsList, String [] asStatus) {
		try {
			if (aoProcWsList == null) return;
			if (aoProcWsList.size()==0) return;
			
			if (asStatus == null) {
				asStatus = new String[4];
				asStatus[0] = StatusCode.RUNNING.name();
				asStatus[1] = StatusCode.SUCCESSFUL.name();
				asStatus[2] = StatusCode.FAILED.name();
				asStatus[3] = StatusCode.DISMISSED.name();
			}
			
			ArrayList<String> asWasdiAllowedStatusCodes = convertStatuses(asStatus);
			
			for (int iProcIndex = aoProcWsList.size()-1; iProcIndex>=0; iProcIndex--) {
				ProcessWorkspaceViewModel oVM = aoProcWsList.get(iProcIndex);
				
				boolean bFound = false;
				
				for (String sWasdiStatus : asWasdiAllowedStatusCodes) {
					if (oVM.getStatus().equals(sWasdiStatus)) {
						bFound = true;
						break;
					}
				}
				
				if (!bFound) {
					aoProcWsList.remove(iProcIndex);
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("JobsResource.filterProcessWorkspaceListFromProcesses: exception : " + oEx.toString());
		}
	}

	/**
	 * Filters the full list of OGC Processes based on date time
	 * @param aoProcWsList List of all the OGC Processes
	 * @param sDateTime
	 */
	protected void filterProcessWorkspaceListFromDateTime(List<ProcessWorkspaceViewModel> aoProcWsList, String sDateTime) {
		try {
			if (aoProcWsList == null) return;
			if (aoProcWsList.size()==0) return;
			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("JobsResource.filterProcessWorkspaceListFromDateTime: exception : " + oEx.toString());
		}
	}
	

	/**
	 * Filters the full list of OGC Processes based on the min duration
	 * @param aoProcWsList List of all the OGC Processes
	 * @param aiMinDuration
	 */
	protected void filterProcessWorkspaceListFromMinDuration(List<ProcessWorkspaceViewModel> aoProcWsList, Integer [] aiMinDuration) {
		try {
			if (aoProcWsList == null) return;
			if (aoProcWsList.size()==0) return;

		}
		catch (Exception oEx) {
			WasdiLog.errorLog("JobsResource.filterProcessWorkspaceListFromMinDuration: exception : " + oEx.toString());
		}
	}

	/**
	 * Filters the full list of OGC Processes based on the max duration 
	 * @param aoProcWsList List of all the OGC Processes
	 * @param aiMaxDuration
	 */
	protected void filterProcessWorkspaceListFromMaxDuration(List<ProcessWorkspaceViewModel> aoProcWsList, Integer [] aiMaxDuration) {
		try {
			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("JobsResource.filterProcessWorkspaceListFromMaxDuration: exception : " + oEx.toString());
		}
	}
	/**
	 * Converts an array of OGC Status Codes in the corresponding ArrayList of WASDI Process Status
	 * @param asOgcStatus String array with Ogc Status codes
	 * @return Arraylist of corresponding status in WASDI
	 */
	protected ArrayList<String> convertStatuses(String [] asOgcStatus) {
		
		ArrayList<String> asWasdiStatusList = new ArrayList<>();
		
		try {
			
			if (asOgcStatus == null) {
				asWasdiStatusList.add(ProcessStatus.RUNNING.name());
				asWasdiStatusList.add(ProcessStatus.DONE.name());
				asWasdiStatusList.add(ProcessStatus.ERROR.name());
				asWasdiStatusList.add(ProcessStatus.READY.name());
				asWasdiStatusList.add(ProcessStatus.STOPPED.name());
				asWasdiStatusList.add(ProcessStatus.WAITING.name());
			}
			else {
				for (String sOgcStatus : asOgcStatus) {
					
					if (sOgcStatus.equals(StatusCode.ACCEPTED.name())) {
						asWasdiStatusList.add(ProcessStatus.CREATED.name());
					}
					else if (sOgcStatus.equals(StatusCode.DISMISSED.name())) {
						asWasdiStatusList.add(ProcessStatus.STOPPED.name());
					}
					else if (sOgcStatus.equals(StatusCode.FAILED.name())) {
						asWasdiStatusList.add(ProcessStatus.ERROR.name());
					}
					else if (sOgcStatus.equals(StatusCode.SUCCESSFUL.name())) {
						asWasdiStatusList.add(ProcessStatus.DONE.name());
					}					
					else if (sOgcStatus.equals(StatusCode.RUNNING.name())) {
						asWasdiStatusList.add(ProcessStatus.RUNNING.name());
						asWasdiStatusList.add(ProcessStatus.WAITING.name());
						asWasdiStatusList.add(ProcessStatus.READY.name());
					}					
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("JobsResource.convertStatuses: exception : " + oEx.toString());
		}
		
		return asWasdiStatusList;
	}

}
