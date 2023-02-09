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
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.Node;
import wasdi.shared.business.OgcProcessesTask;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Processor;
import wasdi.shared.business.User;
import wasdi.shared.business.Workspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.OgcProcessesTaskRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.TimeEpochUtils;
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
    		@QueryParam("offset") Integer oiOffset, @QueryParam("type") List<String> asTypes, @QueryParam("processID") List<String> asProcesses, 
    		@QueryParam("status") List<String> asStatus, @QueryParam("datetime") String sDateTime, 
    		@QueryParam("minDuration") List<Integer> aiMinDuration, @QueryParam("maxDuration") List<Integer> aiMaxDuration) {
    	try {
    		WasdiLog.debugLog("JobsResource.getJobsList");
    		
			// Check User 
			User oUser = OgcProcesses.getUserFromSession(sSessionId, sAuthorization);

			if (oUser==null) {
				WasdiLog.debugLog("JobsResource.getJobsList: invalid session");
	    		return Response.status(Status.UNAUTHORIZED).entity(ApiException.getUnauthorized()).header("WWW-Authenticate", "Basic").build();
			}
			
			if (Utils.isNullOrEmpty(sSessionId)) {
				// We need to valorize the sessionId
				sSessionId = OgcProcesses.updateSessionId(sSessionId, sAuthorization);
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
    			filterProcessWorkspaceListFromProcesses(aoAllProcs, asProcesses.toArray(new String[0]));
    			filterProcessWorkspaceListFromStatus(aoAllProcs, asStatus.toArray(new String[0]));
    			filterProcessWorkspaceListFromDateTime(aoAllProcs, sDateTime);
    			filterProcessWorkspaceListFromMinDuration(aoAllProcs, aiMinDuration.toArray(new Integer[0]));
    			filterProcessWorkspaceListFromMaxDuration(aoAllProcs, aiMaxDuration.toArray(new Integer[0]));
    			
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
    		
    		if (oiOffset+oiLimit<aoAllProcs.size()) {
        		// Next Link
        		Link oNextLink = new Link();
        		
        		int iNewOffset = oiOffset + oiLimit;
        		
        		String sAddress = getJobsListNextLinkAddress(oiLimit, iNewOffset, asTypes.toArray(new String[0]), asProcesses.toArray(new String[0]), asStatus.toArray(new String[0]), sDateTime, aiMinDuration.toArray(new Integer[0]), aiMaxDuration.toArray(new Integer[0]));
        		
        		oNextLink.setHref(sAddress);
        		oNextLink.setRel("self");
        		oNextLink.setType(WasdiConfig.Current.ogcProcessesApi.defaultLinksType);
        		
        		oJobList.getLinks().add(oNextLink);
        		
        		if (oiOffset>0) {
            		// Prev Link
            		Link oPrevLink = new Link();
            		
            		String sPrevAddress = getJobsListNextLinkAddress(oiLimit, oiOffset, asTypes.toArray(new String[0]), asProcesses.toArray(new String[0]), asStatus.toArray(new String[0]), sDateTime, aiMinDuration.toArray(new Integer[0]), aiMaxDuration.toArray(new Integer[0]));
            		
            		oPrevLink.setHref(sPrevAddress);
            		oPrevLink.setRel("self");
            		oPrevLink.setType(WasdiConfig.Current.ogcProcessesApi.defaultLinksType);
            		
            		oJobList.getLinks().add(oPrevLink);        			
        		}
    		}
    		
    		// Self link
    		Link oSelfLink = new Link();
    		oSelfLink.setHref(OgcProcesses.s_sBaseAddress+"jobs/");
    		oSelfLink.setRel("self");
    		oSelfLink.setType(WasdiConfig.Current.ogcProcessesApi.defaultLinksType);
    		
    		oJobList.getLinks().add(oSelfLink);
    		
    		// Alternate html link
    		Link oHtmlLink = new Link();
    		oHtmlLink.setHref(OgcProcesses.s_sBaseAddress+"jobs/");
    		oHtmlLink.setRel("alternate");
    		oHtmlLink.setType("text/html");
    		
    		oJobList.getLinks().add(oHtmlLink);        		
    		
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
			if (!oOgcProcessesTask.getUserId().equals(oUser.getUserId())) {
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
			
			if (Utils.isNullOrEmpty(sSessionId)) {
				// We need to valorize the sessionId
				sSessionId = OgcProcesses.updateSessionId(sSessionId, sAuthorization);
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
    		oSelfLink.setType(WasdiConfig.Current.ogcProcessesApi.defaultLinksType);
    		
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
			if (!oOgcProcessesTask.getUserId().equals(oUser.getUserId())) {
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
			
			if (Utils.isNullOrEmpty(sSessionId)) {
				// We need to valorize the sessionId
				sSessionId = OgcProcesses.updateSessionId(sSessionId, sAuthorization);
			}
			    		
    		// Security check: in case of no node we assume main node
    		if (Utils.isNullOrEmpty(oWorkspace.getNodeCode())) {
    			oWorkspace.setNodeCode("wasdi");
    		}
    		
    		// We need the process Workspace View Model
    		ProcessWorkspaceViewModel oProcWsViewModel = null;
    		
			NodeRepository oNodeRepository = new NodeRepository();
			Node oNode = oNodeRepository.getNodeByCode(oWorkspace.getNodeCode());
    		
    		updateProcessWorkspaceFromNode(sJobId,oNode,sSessionId);
    		
			// Ask the Proc Workspace to the right node
			oProcWsViewModel = readProcessWorkspaceFromNode(sJobId, oNode, sSessionId);
			
    		StatusInfo oStatusInfo = new StatusInfo();
    		
    		oStatusInfo.setJobID(sJobId);
    		oStatusInfo.setStatus(StatusCode.DISMISSED);
    		oStatusInfo.setMessage("Job Dismissed");
    		oStatusInfo.setProgress(oProcWsViewModel.getProgressPerc());
    		
    		Link oUpLink = new Link();
    		oUpLink.setHref(OgcProcesses.s_sBaseAddress+"jobs");
    		oUpLink.setRel("up");
    		oUpLink.setType(WasdiConfig.Current.ogcProcessesApi.defaultLinksType);
    		oUpLink.setTitle("Server job list");
    		
    		
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
    		
			// Check User 
			User oUser = OgcProcesses.getUserFromSession(sSessionId, sAuthorization);

			if (oUser==null) {
				WasdiLog.debugLog("JobsResource.getJobResults: invalid session");
	    		return Response.status(Status.UNAUTHORIZED).entity(ApiException.getUnauthorized()).header("WWW-Authenticate", "Basic").build();
			}

			// Read the index of OGC processes to check if we have it
			OgcProcessesTaskRepository oOgcProcessesTaskRepository = new OgcProcessesTaskRepository();
			OgcProcessesTask oOgcProcessesTask = oOgcProcessesTaskRepository.getOgcProcessesTask(sJobId);
			
			// Null means not found
			if (oOgcProcessesTask == null) {
				WasdiLog.debugLog("JobsResource.getJobResults: OGC Processes Task not found");
				ApiException oApiException = getNotFoundException(sJobId);
				return Response.status(Status.NOT_FOUND).entity(oApiException).build();
			}
			
			// Maybe we found it, but can the user access it also?
			if (!oOgcProcessesTask.getUserId().equals(oUser.getUserId())) {
				WasdiLog.debugLog("JobsResource.getJobResults: OGC Processes Task exists but is not of the user " + oUser.getUserId());
				ApiException oApiException = getNotFoundException(sJobId);
				return Response.status(Status.NOT_FOUND).entity(oApiException).build();				
			}
			
			// This must be in a valid workspace
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			Workspace oWorkspace = oWorkspaceRepository.getWorkspace(oOgcProcessesTask.getWorkspaceId());
			
			if (oWorkspace == null)  {
				WasdiLog.debugLog("JobsResource.getJobResults: The workspace does not exists " + oOgcProcessesTask.getProcessWorkspaceId() );
				ApiException oApiException = getNotFoundException(sJobId);
				return Response.status(Status.NOT_FOUND).entity(oApiException).build();				
			}		
			
			if (Utils.isNullOrEmpty(sSessionId)) {
				// We need to valorize the sessionId
				sSessionId = OgcProcesses.updateSessionId(sSessionId, sAuthorization);
			}
			    		
    		// Security check: in case of no node we assume main node
    		if (Utils.isNullOrEmpty(oWorkspace.getNodeCode())) {
    			oWorkspace.setNodeCode("wasdi");
    		}
    		
    		// We need the process Workspace View Model
    		ProcessWorkspaceViewModel oProcWsViewModel = null;

			NodeRepository oNodeRepository = new NodeRepository();
			Node oNode = oNodeRepository.getNodeByCode(oWorkspace.getNodeCode());
			
    		// Check the node
    		if (oWorkspace.getNodeCode().equals(WasdiConfig.Current.nodeCode)) {
    			// We are in this node: we can read directly from database
    			ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
    			ProcessWorkspace oProcessWorkspace = oProcessWorkspaceRepository.getProcessByProcessObjId(sJobId);
    			
    			if (oProcessWorkspace == null) {
    				WasdiLog.debugLog("JobsResource.getJobResults: The corresponding processes workspace has not been found in node " + oWorkspace.getNodeCode());
    				ApiException oApiException = getNotFoundException(sJobId);
    				return Response.status(Status.NOT_FOUND).entity(oApiException).build();    				
    			}
    			
    			// Build the view model
    			oProcWsViewModel = ProcessWorkspaceViewModel.buildProcessWorkspaceViewModel(oProcessWorkspace);
    		}
    		else {
    			// Ask the Proc Workspace to the right node
    			oProcWsViewModel = readProcessWorkspaceFromNode(sJobId, oNode, sSessionId);
    		}
    		
    		if (oProcWsViewModel == null) {
    			WasdiLog.debugLog("JobsResource.getJobResults: Impossible to find the corresponding Process Workspace");
				ApiException oApiException = getNotFoundException(sJobId);
				return Response.status(Status.NOT_FOUND).entity(oApiException).build();    			
    		}
    		
    		Results oResults = new Results();
    		
    		if (oProcWsViewModel.getStatus().equals(ProcessStatus.DONE.name())) {
    			oResults.put("workspaceId", oProcWsViewModel.getWorkspaceId());
    			oResults.put("payload", oProcWsViewModel.getPayload());
    			
    			DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
    			List<DownloadedFile> aoFiles = oDownloadedFilesRepository.getByWorkspace(oProcWsViewModel.getWorkspaceId());
    			
    			String sBaseUrl = WasdiConfig.Current.baseUrl;
    			
    			if (oNode!=null) sBaseUrl = oNode.getNodeBaseAddress();
    			
    			String[] asFiles = new String[aoFiles.size()];
    			
    			for (int iFiles = 0; iFiles<aoFiles.size(); iFiles++) {
					String sLink = sBaseUrl + "/catalog/downloadbyname?token=" + sSessionId + "&filename=" + aoFiles.get(iFiles).getFileName() + "&workspace=" + oWorkspace.getWorkspaceId();
					asFiles[iFiles] = sLink;
				}
    			
    			oResults.put("files", asFiles);
    		}
    		else if (oProcWsViewModel.getStatus().equals(ProcessStatus.ERROR.name())) {
    			WasdiLog.debugLog("JobsResource.getJobResults: Job is in error");
				ApiException oApiException = getNotFoundException(sJobId);
				oApiException.setType("InvalidParameterValue");
				return Response.status(Status.NOT_FOUND).entity(oApiException).build();    			
    			
    		}
    		else {
    			WasdiLog.debugLog("JobsResource.getJobResults: Results not ready");
				ApiException oApiException = getNotFoundException(sJobId);
				oApiException.setType("http://www.opengis.net/def/exceptions/ogcapi-processes-1/1.0/result-not-ready");
				return Response.status(Status.NOT_FOUND).entity(oApiException).build();    			
    			
    		}
    		
    		return Response.status(Status.OK).entity(oResults).build();
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("JobsResource.getJobResults: exception " + oEx.toString());
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ApiException.getInternalServerError("There was an error getting the job results")).build();    		
		}
    }
    
    
    /**
     * Converts a WASDI ProcessWorkspace View Model in an OGC Processes API Status Info
     * 
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
	 * Stop a remote process
	 * @param sProcessWorkspaceId
	 * @param oNode
	 * @param sSessionId
	 * @return
	 */
	protected void updateProcessWorkspaceFromNode(String sProcessWorkspaceId, Node oNode, String sSessionId) {
		try {
			if (oNode.getActive()==false) return;
			
			String sUrl = oNode.getNodeBaseAddress();
			
			if (!sUrl.endsWith("/")) sUrl += "/";
			
			sUrl += "process/delete?procws="+sProcessWorkspaceId+"&treeKill=true";
			
			Map<String, String> asHeaders = new HashMap<String, String>();
			asHeaders.put("x-session-token", sSessionId);
			
			WasdiLog.debugLog("JobsResource.updateProcessWorkspaceFromNode: calling url: " + sUrl);
			
			HttpUtils.httpGet(sUrl, asHeaders);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("JobsResource.readProcessWorkspaceFromNode: exception contacting computing node: " + oEx.toString());
		}		
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
			
			List<String> asProcessorIdentifiers = new ArrayList<>();
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			
			for (String sProcId : asProcesses) {
				
				Processor oProcessor = oProcessorRepository.getProcessor(sProcId);
				asProcessorIdentifiers.add(oProcessor.getName());
			}			
			
			
			
			for (int iProcIndex = aoProcWsList.size()-1; iProcIndex>=0; iProcIndex--) {
				ProcessWorkspaceViewModel oVM = aoProcWsList.get(iProcIndex);
				
				String sAppName = oVM.getProductName();
				
				if (!asProcessorIdentifiers.contains(sAppName)) {
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
	 * Date time can be a single value or an interval.
	 * 
	 * @param aoProcWsList List of all the OGC Processes
	 * @param sDateTime
	 */
	protected void filterProcessWorkspaceListFromDateTime(List<ProcessWorkspaceViewModel> aoProcWsList, String sDateTime) {
		try {
			// Check the inputs
			if (aoProcWsList == null) return;
			if (aoProcWsList.size()==0) return;
			
			if (Utils.isNullOrEmpty(sDateTime)) return;
			
			// We assume in general to have a start and an end date
			Date oStartDate = null;
			Date oEndDate = null;
			
			// If / is in the string this should be an interval
			if (sDateTime.contains("/")) {
				String [] asDates = sDateTime.split("/");
				
				if (asDates!=null) {
					if (asDates.length==2) {
						
						// Assume both are valid
						String sStart = asDates[0];
						String sEnd= asDates[1];
						
						// Convert to date: if it is not working we will get null
						oStartDate = TimeEpochUtils.fromISO8061DateStringToDate(sStart);
						oEndDate = TimeEpochUtils.fromISO8061DateStringToDate(sEnd);
					}
				}
			}
			else {
				// Convert both start and end to the same date: if it is not working we will get null
				oStartDate = TimeEpochUtils.fromISO8061DateStringToDate(sDateTime);
				oEndDate = TimeEpochUtils.fromISO8061DateStringToDate(sDateTime);
			}
			
			// We need at least one to filter
			if (oStartDate == null && oEndDate == null) return;
						
			// Now lets see our list
			for (int iProcIndex = aoProcWsList.size()-1; iProcIndex>=0; iProcIndex--) {
				
				ProcessWorkspaceViewModel oVM = aoProcWsList.get(iProcIndex);
				// Get the operation date
				Date oOperationDate = Utils.getWasdiDate(oVM.getOperationDate());			
				
				// Is there a start date?
				if (oStartDate != null) {					
					
					if (oOperationDate!=null) {
						long lStartMs = oStartDate.getTime();
						long lOpMs = oOperationDate.getTime();
						
						// We need processes in the range
						if (lOpMs<lStartMs) {
							aoProcWsList.remove(iProcIndex);
							continue;
						}
					}
				}
				
				// Is there an end date?
				if (oEndDate != null) {
					
					if (oOperationDate!=null) {
						long lStartMs = oEndDate.getTime();
						long lOpMs = oOperationDate.getTime();
						
						// We need processes in the range
						if (lOpMs>lStartMs) {
							aoProcWsList.remove(iProcIndex);
							continue;
						}
					}
				}				
			}

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
			
			if (aiMinDuration == null) return;
			if (aiMinDuration.length == 0) return;
			
			// Now lets see our list
			for (int iProcIndex = aoProcWsList.size()-1; iProcIndex>=0; iProcIndex--) {
				ProcessWorkspaceViewModel oVM = aoProcWsList.get(iProcIndex);
				
				// Get the operation date
				Date oStartDate = Utils.getWasdiDate(oVM.getOperationStartDate());
				
				Date oEndDate = null;
				
				if (oVM.getStatus().equals(ProcessStatus.DONE.name()) || oVM.getStatus().equals(ProcessStatus.ERROR.name()) || oVM.getStatus().equals(ProcessStatus.STOPPED.name())) {
					oEndDate = Utils.getWasdiDate(oVM.getOperationEndDate());
				}
				else if (oVM.getStatus().equals(ProcessStatus.RUNNING.name()) || oVM.getStatus().equals(ProcessStatus.WAITING.name()) || oVM.getStatus().equals(ProcessStatus.READY.name())) {
					oEndDate = new Date();
				}
				
				if (oStartDate!=null && oEndDate != null) {
					long lDuration = oEndDate.getTime() - oStartDate.getTime();
					
					int iDuration = (int) lDuration/1000;
					
					if (iDuration<aiMinDuration[0]) {
						aoProcWsList.remove(iProcIndex);
					}
				}
			}

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
			if (aoProcWsList == null) return;
			if (aoProcWsList.size()==0) return;
			
			if (aiMaxDuration == null) return;
			if (aiMaxDuration.length == 0) return;
			
			// Now lets see our list
			for (int iProcIndex = aoProcWsList.size()-1; iProcIndex>=0; iProcIndex--) {
				ProcessWorkspaceViewModel oVM = aoProcWsList.get(iProcIndex);
				
				// Get the operation date
				Date oStartDate = Utils.getWasdiDate(oVM.getOperationStartDate());
				
				Date oEndDate = null;
				
				if (oVM.getStatus().equals(ProcessStatus.DONE.name()) || oVM.getStatus().equals(ProcessStatus.ERROR.name()) || oVM.getStatus().equals(ProcessStatus.STOPPED.name())) {
					oEndDate = Utils.getWasdiDate(oVM.getOperationEndDate());
				}
				else if (oVM.getStatus().equals(ProcessStatus.RUNNING.name()) || oVM.getStatus().equals(ProcessStatus.WAITING.name()) || oVM.getStatus().equals(ProcessStatus.READY.name())) {
					oEndDate = new Date();
				}
				
				if (oStartDate!=null && oEndDate != null) {
					long lDuration = oEndDate.getTime() - oStartDate.getTime();
					
					int iDuration = (int) lDuration/1000;
					
					if (iDuration>aiMaxDuration[0]) {
						aoProcWsList.remove(iProcIndex);
					}
				}
			}			
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
	
	/**
	 * Get the link to the next page for a Job List query
	 * @param oiLimit limit parameter
	 * @param iNewOffset offset parameter
	 * @param asTypes original types parameter
	 * @param asProcesses original processes parameter
	 * @param asStatus original status parameter
	 * @param sDateTime original date time parameter
	 * @param aiMinDuration original min duration parameter
	 * @param aiMaxDuration original max durartion parameter
	 * @return the link to the next page
	 */
	protected String getJobsListNextLinkAddress(Integer oiLimit, int iNewOffset, String []asTypes, String []asProcesses, String []asStatus, String sDateTime, Integer [] aiMinDuration, Integer [] aiMaxDuration) {
		
		String sAddress = OgcProcesses.s_sBaseAddress+"jobs/";
		
		try {
			sAddress += "limit=" + oiLimit + "&offset" + iNewOffset;
			
			if (asTypes!=null) {
				if (asTypes.length>0) {
					sAddress += "&";
					
					for (String sType : asTypes) {
						sAddress += "type=" + sType + "&";
					}
					
					sAddress = sAddress.substring(0,sAddress.length()-1);
				}
			}
			
			if (asProcesses!=null) {
				if (asProcesses.length>0) {
					sAddress += "&";
					
					for (String sProcess : asProcesses) {
						sAddress +="processID=" + sProcess + "&";
					}
					
					sAddress = sAddress.substring(0,sAddress.length()-1);
				}
			}
			
			if (asStatus!=null) {
				if (asStatus.length>0) {
					sAddress += "&";
					
					for (String sStatus : asStatus) {
						sAddress += "status=" + sStatus + "&";
					}
					
					sAddress = sAddress.substring(0, sAddress.length()-1);
				}
			}
			
			if (!Utils.isNullOrEmpty(sDateTime)) {
				sAddress += "&datetime=" + sDateTime;
			}
			
			if (aiMinDuration!=null) {
				
				if (aiMinDuration.length>0) {
					
					sAddress += "&";
					
					for (Integer iMin : aiMinDuration) {
						sAddress += "&minduration" + iMin + "&";
					}
					
					sAddress = sAddress.substring(0, sAddress.length()-1);
				}
			}
			
			if (aiMaxDuration!=null) {
				if (aiMaxDuration.length>0) {
					
					sAddress += "&";
					
					for (Integer iMax : aiMaxDuration) {
						sAddress += "&maxduration" + iMax + "&";
					}
					
					sAddress = sAddress.substring(0, sAddress.length()-1);
				}
			}			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("JobsResource.getJobsListNextLinkAddress: exception : " + oEx.toString());
		}
		
		return sAddress;
	}

}
