package it.fadeout.rest.resources;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.media.multipart.FormDataParam;

import it.fadeout.Wasdi;
import it.fadeout.rest.resources.largeFileDownload.ZipStreamingOutput;
import it.fadeout.threads.UpdateProcessorFilesWorker;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.Counter;
import wasdi.shared.business.Node;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Processor;
import wasdi.shared.business.ProcessorLog;
import wasdi.shared.business.ProcessorSharing;
import wasdi.shared.business.ProcessorTypes;
import wasdi.shared.business.User;
import wasdi.shared.data.CounterRepository;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorLogRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.ProcessorSharingRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.DeployedProcessorViewModel;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.ProcessorLogViewModel;
import wasdi.shared.viewmodels.RunningProcessorViewModel;

@Path("/processors")
public class ProcessorsResource {
	
	@Context
	ServletConfig m_oServletConfig;
	
	/**
	 * Upload a new processor in Wasdi
	 * @param oInputStreamForFile
	 * @param sSessionId
	 * @param sName
	 * @param sVersion
	 * @param sDescription
	 * @return
	 * @throws Exception
	 */
	@POST
	@Path("/uploadprocessor")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public PrimitiveResult uploadProcessor(@FormDataParam("file") InputStream oInputStreamForFile, @HeaderParam("x-session-token") String sSessionId, 
			@QueryParam("workspace") String sWorkspaceId, @QueryParam("name") String sName, @QueryParam("version") String sVersion,
			@QueryParam("description") String sDescription, @QueryParam("type") String sType, @QueryParam("paramsSample") String sParamsSample, @QueryParam("public") Integer iPublic) throws Exception {

		Utils.debugLog("ProcessorsResource.uploadProcessor( oInputStreamForFile, Session: " + sSessionId + ", WS: " + sWorkspaceId + ", Name: " + sName + ", Version: " + sVersion + ", Description" 
				+ sDescription + ", Type" + sType + ", ParamsSample: " + sParamsSample + " )");
		
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		
		try {
			// Check User 
			if (Utils.isNullOrEmpty(sSessionId)) {
				Utils.debugLog("ProcessorsResource.uploadProcessor: session is null or empty, aborting");
				oResult.setIntValue(401);
				return oResult;
			}
			User oUser = Wasdi.GetUserFromSession(sSessionId);
			if (oUser==null) {
				Utils.debugLog("ProcessorsResource.uploadProcessor: user (from session) is null, aborting");
				oResult.setIntValue(401);
				return oResult;
			}
			String sUserId = oUser.getUserId();
			if (Utils.isNullOrEmpty(sUserId)) {
				Utils.debugLog("ProcessorsResource.uploadProcessor: userid of user (from session) is null or empty, aborting");
				oResult.setIntValue(401);
				return oResult;
			}
			
			// Put the processor as Public by default
			if (iPublic == null) {
				iPublic = new Integer(1);
			}
			
			// Force the name to lower case
			sName = sName.toLowerCase();
			// Remove spaces at beginning or end
			sName.trim();
			
			// There are still spaces?
			if (sName.contains(" ")) {
				// Error
				oResult.setIntValue(500);
				oResult.setStringValue("Processor Name MUST be lower case and without any space");
				return oResult;				
			}
			
			// Set the processor path
			String sDownloadRootPath = Wasdi.getDownloadPath(m_oServletConfig);
			
			File oProcessorPath = new File(sDownloadRootPath+ "/processors/" + sName);
			
			// Create folders
			if (!oProcessorPath.exists()) {
				oProcessorPath.mkdirs();
			}
			else {
				// If the path exists, the name of the processor is already used
				oResult.setIntValue(500);
				oResult.setStringValue("Processor Name Already in Use");
				return oResult;
			}
			
			// Create file
			String sProcessorId =  UUID.randomUUID().toString();
			File oProcessorFile = new File(sDownloadRootPath+"/processors/" + sName + "/" + sProcessorId + ".zip");
			Utils.debugLog("ProcessorsResource.uploadProcessor: Processor file Path: " + oProcessorFile.getPath());
			
			//save uploaded file
			int iRead = 0;
			byte[] ayBytes = new byte[1024];
			OutputStream oOutputStream = new FileOutputStream(oProcessorFile);
			while ((iRead = oInputStreamForFile.read(ayBytes)) != -1) {
				oOutputStream.write(ayBytes, 0, iRead);
			}
			oOutputStream.flush();
			oOutputStream.close();
			
			// TODO: check it is a zip file
			// TODO: check it contains at least myProcessor.py
			// XXX: check it also has a run
			
			if (Utils.isNullOrEmpty(sType)) {
				sType = ProcessorTypes.UBUNTU_PYTHON27_SNAP;
			}
			
			// Create processor entity
			Processor oProcessor = new Processor();
			oProcessor.setName(sName);
			oProcessor.setDescription(sDescription);
			oProcessor.setUserId(sUserId);
			oProcessor.setProcessorId(sProcessorId);
			oProcessor.setVersion(sVersion);
			oProcessor.setPort(-1);
			oProcessor.setType(sType);
			oProcessor.setIsPublic(iPublic);
			
			// Add info about the deploy node
			Node oNode = Wasdi.getActualNode();
			
			if (oNode != null) {
				oProcessor.setNodeCode(oNode.getNodeCode());
				oProcessor.setNodeUrl(oNode.getNodeBaseAddress());
			}
			
			if (!Utils.isNullOrEmpty(sParamsSample)) {
				oProcessor.setParameterSample(sParamsSample);
			}
			
			// Store in the db
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			oProcessorRepository.insertProcessor(oProcessor);
			
			// Schedule the processworkspace to deploy the processor
			
			String sProcessObjId = Utils.GetRandomName();
			
			ProcessorParameter oDeployProcessorParameter = new ProcessorParameter();
			oDeployProcessorParameter.setName(sName);
			oDeployProcessorParameter.setProcessorID(sProcessorId);
			oDeployProcessorParameter.setWorkspace(sWorkspaceId);
			oDeployProcessorParameter.setUserId(sUserId);
			oDeployProcessorParameter.setExchange(sWorkspaceId);
			oDeployProcessorParameter.setProcessObjId(sProcessObjId);
			oDeployProcessorParameter.setProcessorType(sType);
			oDeployProcessorParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));
			
			String sPath = m_oServletConfig.getInitParameter("SerializationPath");
			
			PrimitiveResult oRes = Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.DEPLOYPROCESSOR.name(), sName, sPath, oDeployProcessorParameter);
			
			if (oRes.getBoolValue()) {
				oResult.setIntValue(200);
				oResult.setBoolValue(true);
				return oResult;
			}
			else {
				oResult.setIntValue(500);
				return oResult;				
			}
		}
		
		catch (Exception oEx) {
			Utils.debugLog("ProcessorsResource.uploadProcessor:" + oEx);
			oResult.setIntValue(500);
			return oResult;
		}
		
	}
	
	@GET
	@Path("/getdeployed")
	public List<DeployedProcessorViewModel> getDeployedProcessors(@HeaderParam("x-session-token") String sSessionId) throws Exception {

		ArrayList<DeployedProcessorViewModel> aoRet = new ArrayList<>(); 
		Utils.debugLog("ProcessorsResource.getDeployedProcessors( Session: " + sSessionId + " )");
		
		try {
			// Check User 
			if (Utils.isNullOrEmpty(sSessionId)) return aoRet;
			User oUser = Wasdi.GetUserFromSession(sSessionId);

			if (oUser==null) return aoRet;
			if (Utils.isNullOrEmpty(oUser.getUserId())) return aoRet;
						
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			ProcessorSharingRepository oProcessorSharingRepository = new ProcessorSharingRepository();
			List<Processor> aoDeployed = oProcessorRepository.getDeployedProcessors();
			
			for (int i=0; i<aoDeployed.size(); i++) {
				DeployedProcessorViewModel oVM = new DeployedProcessorViewModel();
				Processor oProcessor = aoDeployed.get(i);

				ProcessorSharing oSharing = oProcessorSharingRepository.getProcessorSharingByUserIdProcessorId(oUser.getUserId(), oProcessor.getProcessorId());

				if (oProcessor.getIsPublic() != 1) {
					if (oProcessor.getUserId().equals(oUser.getUserId()) == false) {
						if (oSharing == null) continue;
					}
				}
				
				if (oSharing != null) oVM.setSharedWithMe(true);
				
				oVM.setProcessorDescription(oProcessor.getDescription());
				oVM.setProcessorId(oProcessor.getProcessorId());
				oVM.setProcessorName(oProcessor.getName());
				oVM.setProcessorVersion(oProcessor.getVersion());
				oVM.setPublisher(oProcessor.getUserId());
				oVM.setParamsSample(oProcessor.getParameterSample());
				oVM.setIsPublic(oProcessor.getIsPublic());
				oVM.setType(oProcessor.getType());
				oVM.setiTimeoutMs(oProcessor.getTimeoutMs());
				
				aoRet.add(oVM);
			}
		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessorsResource.getDeployedProcessors: " + oEx);
			return aoRet;
		}		
		return aoRet;
	}
	
	
	@GET
	@Path("/run")
	public RunningProcessorViewModel run(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("name") String sName, @QueryParam("encodedJson") String sEncodedJson,
			@QueryParam("workspace") String sWorkspaceId,
			@QueryParam("parent") String sParentProcessWorkspaceId) throws Exception {
		Utils.debugLog("ProcessorsResource.run( Session: " + sSessionId + ", Name: " + sName + ", encodedJson:" + sEncodedJson + ", WS: " + sWorkspaceId + " )");

		RunningProcessorViewModel oRunningProcessorViewModel = new RunningProcessorViewModel();
		
		try {
			// Check User 
			if (Utils.isNullOrEmpty(sSessionId)) return oRunningProcessorViewModel;
			User oUser = Wasdi.GetUserFromSession(sSessionId);

			if (oUser==null) return oRunningProcessorViewModel;
			if (Utils.isNullOrEmpty(oUser.getUserId())) return oRunningProcessorViewModel;
			
			String sUserId = oUser.getUserId();
		
			Utils.debugLog("ProcessorsResource.run: get Processor");	
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessorToRun = oProcessorRepository.getProcessorByName(sName);
			
			if (oProcessorToRun == null) { 
				Utils.debugLog("ProcessorsResource.run: unable to find processor " + sName);
				oRunningProcessorViewModel.setStatus("ERROR");
				return oRunningProcessorViewModel;
			}
			
			if (Utils.isNullOrEmpty(sEncodedJson)) {
				sEncodedJson = "%7B%7D";
			}

			// Schedule the process to run the processor
			
			String sProcessObjId = Utils.GetRandomName();
			
			String sPath = m_oServletConfig.getInitParameter("SerializationPath");

			ProcessorParameter oProcessorParameter = new ProcessorParameter();
			oProcessorParameter.setName(sName);
			oProcessorParameter.setProcessorID(oProcessorToRun.getProcessorId());
			oProcessorParameter.setWorkspace(sWorkspaceId);
			oProcessorParameter.setUserId(sUserId);
			oProcessorParameter.setExchange(sWorkspaceId);
			oProcessorParameter.setProcessObjId(sProcessObjId);
			oProcessorParameter.setJson(sEncodedJson);
			oProcessorParameter.setProcessorType(oProcessorToRun.getType());
			oProcessorParameter.setSessionID(sSessionId);
			oProcessorParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));
			
			PrimitiveResult oResult = Wasdi.runProcess(sUserId, sSessionId, oProcessorParameter.getLauncherOperation(), sName, sPath, oProcessorParameter, sParentProcessWorkspaceId);
			
			try{
				Utils.debugLog("ProcessorsResource.run: create task");
				
				if (oResult.getBoolValue()==false) {
					throw new Exception();
				}
								
				oRunningProcessorViewModel.setJsonEncodedResult("");
				oRunningProcessorViewModel.setName(sName);
				oRunningProcessorViewModel.setProcessingIdentifier(oResult.getStringValue());
				oRunningProcessorViewModel.setProcessorId(oProcessorToRun.getProcessorId());
				oRunningProcessorViewModel.setStatus("CREATED");
				Utils.debugLog("ProcessorsResource.run: done"); 
			}
			catch(Exception oEx){
				Utils.debugLog("ProcessorsResource.run: Error scheduling the run process " + oEx);
				oRunningProcessorViewModel.setStatus(ProcessStatus.ERROR.toString());
				return oRunningProcessorViewModel;
			}
		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessorsResource.run: " + oEx );
			oRunningProcessorViewModel.setStatus(ProcessStatus.ERROR.toString());
			return oRunningProcessorViewModel;
		}
		
		return oRunningProcessorViewModel;
	}
	
	
	@GET
	@Path("/help")
	public PrimitiveResult help(@HeaderParam("x-session-token") String sSessionId, @QueryParam("name") String sName) throws Exception {
		Utils.debugLog("ProcessorsResource.help( Session: " + sSessionId + ", Name: " + sName + " )");
		PrimitiveResult oPrimitiveResult = new PrimitiveResult();
		oPrimitiveResult.setBoolValue(false);
		
		try {
			// Check User 
			if (Utils.isNullOrEmpty(sSessionId)) return oPrimitiveResult;
			User oUser = Wasdi.GetUserFromSession(sSessionId);

			if (oUser==null) return oPrimitiveResult;
			if (Utils.isNullOrEmpty(oUser.getUserId())) return oPrimitiveResult;
			
			//String sUserId = oUser.getUserId();
			
			Utils.debugLog("ProcessorsResource.help: read Processor " +sName);
			
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessorToRun = oProcessorRepository.getProcessorByName(sName);
			
			// Call localhost:port
			String sUrl = "http://localhost:"+oProcessorToRun.getPort()+"/run/--help";
			
			Utils.debugLog("ProcessorsResource.help: calling URL = " + sUrl);
			
			URL oProcessorUrl = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oProcessorUrl.openConnection();
			oConnection.setDoOutput(true);
			oConnection.setRequestMethod("POST");
			oConnection.setRequestProperty("Content-Type", "application/json");

			OutputStream oOutputStream = oConnection.getOutputStream();
			oOutputStream.write("{}".getBytes());
			oOutputStream.flush();
			
			if (! (oConnection.getResponseCode() == HttpURLConnection.HTTP_OK || oConnection.getResponseCode() == HttpURLConnection.HTTP_CREATED )) {
				throw new RuntimeException("Failed : HTTP error code : " + oConnection.getResponseCode());
			}

			BufferedReader oBufferedReader = new BufferedReader(new InputStreamReader((oConnection.getInputStream())));

			String sOutputResult;
			String sOutputCumulativeResult = "";
			Utils.debugLog("ProcessorsResource.help: Output from Server .... \n");
			while ((sOutputResult = oBufferedReader.readLine()) != null) {
				Utils.debugLog("ProcessorsResource.help: " + sOutputResult);
				
				if (!Utils.isNullOrEmpty(sOutputResult)) sOutputCumulativeResult += sOutputResult;
			}

			oConnection.disconnect();
			
			oPrimitiveResult.setBoolValue(true);
			oPrimitiveResult.setStringValue(sOutputCumulativeResult);
		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessorsResource.help: " + oEx);
			return oPrimitiveResult;
		}
		
		return oPrimitiveResult;
	}
	
	@GET
	@Path("/status")
	public RunningProcessorViewModel status(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("processingId") String sProcessingId) throws Exception {
		Utils.debugLog("ProcessorsResource.status( Session: " + sSessionId + ", ProcessingId: " + sProcessingId + " )");
		RunningProcessorViewModel oRunning = new RunningProcessorViewModel();
		oRunning.setStatus(ProcessStatus.ERROR.toString());
		try {
			// Check User 
			if (Utils.isNullOrEmpty(sSessionId)) return oRunning;
			User oUser = Wasdi.GetUserFromSession(sSessionId);

			if (oUser==null) return oRunning;
			if (Utils.isNullOrEmpty(oUser.getUserId())) return oRunning;
			
			String sUserId = oUser.getUserId();
			//String sWorkspaceId = "";
		
			Utils.debugLog("ProcessorsResource.status: get Running Processor " + sProcessingId);
			
			// Get Process-Workspace
			ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			ProcessWorkspace oProcessWorkspace = oProcessWorkspaceRepository.getProcessByProcessObjId(sProcessingId);
			
			// Check not null
			if (oProcessWorkspace == null) {
				Utils.debugLog("ProcessorsResource.status: impossible to find " + sProcessingId);
				return oRunning;
			}
			
			// Check if it is the right user
			if (oProcessWorkspace.getUserId().equals(sUserId) == false) {
				Utils.debugLog("ProcessorsResource.status: processing not of this user");
				return oRunning;				
			}
			
			// Check if it is a processor action
			if (!(oProcessWorkspace.getOperationType().equals(LauncherOperations.DEPLOYPROCESSOR.toString()) || oProcessWorkspace.getOperationType().equals(LauncherOperations.RUNPROCESSOR.toString())) ) {
				Utils.debugLog("ProcessorsResource.status: not a running process ");
				return oRunning;								
			}
			
			// Get the processor from the db
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessor(oProcessWorkspace.getProductName());
			
			// Set name, id, running id and status
			oRunning.setName(oProcessor.getName());
			oRunning.setProcessingIdentifier(sProcessingId);
			oRunning.setProcessorId(oProcessor.getProcessorId());
			oRunning.setStatus(oProcessWorkspace.getStatus());
			
			// Is this done?
			if (oRunning.getStatus().equals(ProcessStatus.DONE.toString())) {
				// Do we have a payload?
				if (oProcessWorkspace.getPayload() != null) {
					// Give result to the caller
					oRunning.setJsonEncodedResult(oProcessWorkspace.getPayload());
				}
			}
		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessorsResource.run: Error scheduling the deploy process " + oEx);
			oRunning.setStatus(ProcessStatus.ERROR.toString());
		}
		return oRunning;
	}
	
	
	
	@POST
	@Path("/logs/add")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response addLog(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processworkspace") String sProcessWorkspaceId, String sLog) {
		//Wasdi.DebugLog("ProcessorsResource.addLog( " + sSessionId + ", " + sProcessWorkspaceId + ", " + sLog + " )");
		try {
			// Check User 
			if (Utils.isNullOrEmpty(sSessionId)) {
				Utils.debugLog("ProcessorResource.addLog: 401 session id null");
				return Response.status(401).build();
			}
			User oUser = Wasdi.GetUserFromSession(sSessionId);

			if (oUser==null) {
				Utils.debugLog("ProcessorResource.addLog: user null");
				return Response.status(401).build();
			}
			
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				Utils.debugLog("ProcessorResource.addLog: userId null");
				return Response.status(401).build();
			}
			
			ProcessorLog oLog = new ProcessorLog();
			
			oLog.setLogDate(Wasdi.GetFormatDate(new Date()));
			oLog.setProcessWorkspaceId(sProcessWorkspaceId);
			oLog.setLogRow(sLog);
			
			ProcessorLogRepository oProcessorLogRepository = new ProcessorLogRepository();
			String sResult = oProcessorLogRepository.insertProcessLog(oLog);
			if(!Utils.isNullOrEmpty(sResult)) {
				Utils.debugLog("ProcessorResource.addLog: added log row to processid " + sProcessWorkspaceId);
			} else {
				Utils.debugLog("ProcessorResource.addLog: failed to add log row");
			}
		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessorResource.addLog" + oEx);
			return Response.serverError().build();
		}
		return Response.ok().build();
	 }
	
	
	@GET
	@Path("/logs/count")
	public int countLogs(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("processworkspace") String sProcessWorkspaceId){
		Utils.debugLog("ProcessorResource.countLogs( Session: " + sSessionId + ", ProcWsId: " + sProcessWorkspaceId + " )");
		int iResult = -1;
		try {
			if (Utils.isNullOrEmpty(sSessionId)) {
				Utils.debugLog("ProcessorResource.countLogs: 401 session id null");
				return iResult;
			}
			User oUser = Wasdi.GetUserFromSession(sSessionId);
	
			if (oUser==null) {
				Utils.debugLog("ProcessorResource.countLogs: user null");
				return iResult;
			}
			
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				Utils.debugLog("ProcessorResource.countLogs: userId null");
				return iResult;
			}
			
			Utils.debugLog("ProcessorResource.countLogs: get log count for process " + sProcessWorkspaceId);
			
			CounterRepository oCounterRepository = new CounterRepository();
			Counter oCounter = null;
			oCounter = oCounterRepository.getCounterBySequence(sProcessWorkspaceId);
			if(null == oCounter) {
				Utils.debugLog("ProcessorResource.countLogs( " + sSessionId + ", " + sProcessWorkspaceId +
						" ): CounterRepository returned a null Counter");
				return iResult;
			}
			iResult = oCounter.getValue();
		} catch (Exception oEx) {
			Utils.debugLog("ProcessorResource.countLogs( " + sSessionId + ", " + sProcessWorkspaceId + " ): " + oEx);
		}
		return iResult;
	}
	
	@GET
	@Path("/logs/list")
	public ArrayList<ProcessorLogViewModel> getLogs(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("processworkspace") String sProcessWorkspaceId,
			//note: range extremes are included
			@QueryParam("startrow") Integer iStartRow, @QueryParam("endrow") Integer iEndRow) {
		
		Utils.debugLog("ProcessorsResource.getLogs( Session: " + sSessionId + ", ProcWsId: " + sProcessWorkspaceId + ", Start: " + iStartRow + ", End: " + iEndRow + " )");
		ArrayList<ProcessorLogViewModel> aoRetList = new ArrayList<>();
		try {
			
			// Check User 
			if (Utils.isNullOrEmpty(sSessionId)) {
				Utils.debugLog("ProcessorResource.getLogs: addLog: 401 session id null");
				return aoRetList;
			}
			User oUser = Wasdi.GetUserFromSession(sSessionId);

			if (oUser==null) {
				Utils.debugLog("ProcessorResource.getLogs: addLog: user null");
				return aoRetList;
			}
			
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				Utils.debugLog("ProcessorResource.getLogs: addLog: userId null");
				return aoRetList;
			}
			
			Utils.debugLog("ProcessorResource.getLogs: get log for process " + sProcessWorkspaceId);
			
			ProcessorLogRepository oProcessorLogRepository = new ProcessorLogRepository();
			List<ProcessorLog> aoLogs = null;
			if(null==iStartRow || null==iEndRow) {
				aoLogs = oProcessorLogRepository.getLogsByProcessWorkspaceId(sProcessWorkspaceId);
			} else {
				aoLogs = oProcessorLogRepository.getLogsByWorkspaceIdInRange(sProcessWorkspaceId, iStartRow, iEndRow);
			}
			
			for(int iLogs = 0; iLogs<aoLogs.size(); iLogs++) {
				ProcessorLog oLog = aoLogs.get(iLogs);
				
				ProcessorLogViewModel oLogVM = new ProcessorLogViewModel();
				oLogVM.setLogDate(oLog.getLogDate());
				oLogVM.setLogRow(oLog.getLogRow());
				oLogVM.setProcessWorkspaceId(sProcessWorkspaceId);
				oLogVM.setRowNumber(oLog.getRowNumber());
				aoRetList.add(oLogVM);
			}
			
		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessorResource.getLogs: " + oEx);
			return aoRetList;
		}
		
		return aoRetList;

	 }
	
	@GET
	@Path("/delete")
	public Response deleteProcessor(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("processorId") String sProcessorId,
			@QueryParam("workspaceId") String sWorkspaceId) {
		Utils.debugLog("ProcessorResources.deleteProcessor( Session: " + sSessionId + ", Processor: " + sProcessorId + ", WS: " + sWorkspaceId + " )");
		
		try {
			if (Utils.isNullOrEmpty(sSessionId)) return Response.status(Status.UNAUTHORIZED).build();
			User oUser = Wasdi.GetUserFromSession(sSessionId);

			if (oUser==null) return Response.status(Status.UNAUTHORIZED).build();
			if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();

			String sUserId = oUser.getUserId();
			
			Utils.debugLog("ProcessorsResource.deleteProcessor: get Processor");	
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessorToDelete = oProcessorRepository.getProcessor(sProcessorId);
			
			if (oProcessorToDelete == null) {
				Utils.debugLog("ProcessorsResource.deleteProcessor: unable to find processor " + sProcessorId);
				return Response.serverError().build();
			}
			
			if (!oProcessorToDelete.getUserId().equals(oUser.getUserId())) {
				Utils.debugLog("ProcessorsResource.deleteProcessor: processor not of user " + oProcessorToDelete.getUserId());
				return Response.status(Status.UNAUTHORIZED).build();
			}

			// Schedule the process to run the processor
			
			String sProcessObjId = Utils.GetRandomName();
			
			String sPath = m_oServletConfig.getInitParameter("SerializationPath");
			
			ProcessorParameter oProcessorParameter = new ProcessorParameter();
			oProcessorParameter.setName(oProcessorToDelete.getName());
			oProcessorParameter.setProcessorID(oProcessorToDelete.getProcessorId());
			oProcessorParameter.setWorkspace(sWorkspaceId);
			oProcessorParameter.setUserId(sUserId);
			oProcessorParameter.setExchange(sWorkspaceId);
			oProcessorParameter.setProcessObjId(sProcessObjId);
			oProcessorParameter.setJson("{}");
			oProcessorParameter.setProcessorType(oProcessorToDelete.getType());
			oProcessorParameter.setSessionID(sSessionId);
			oProcessorParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));
			
			PrimitiveResult oRes = Wasdi.runProcess(sUserId,sSessionId, LauncherOperations.DELETEPROCESSOR.name(),oProcessorToDelete.getName(), sPath,oProcessorParameter);			
			
			if (oRes.getBoolValue()) {
				return Response.ok().build();
			}
			else {
				return Response.serverError().build();
			}
		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessorResource.deleteProcessor: " + oEx);
			return Response.serverError().build();
		}
	}
	
	@POST
	@Path("/update")
	public Response updateProcessor(DeployedProcessorViewModel oUpdatedProcessorVM, @HeaderParam("x-session-token") String sSessionId,
			@QueryParam("processorId") String sProcessorId,
			@QueryParam("workspaceId") String sWorkspaceId) {
		
		Utils.debugLog("ProcessorResources.updateProcessor( Session: " + sSessionId + ", Processor: " + sProcessorId + ", WS: " + sWorkspaceId + " )");
		
		try {
			if (Utils.isNullOrEmpty(sSessionId)) return Response.status(Status.UNAUTHORIZED).build();
			User oUser = Wasdi.GetUserFromSession(sSessionId);

			if (oUser==null) return Response.status(Status.UNAUTHORIZED).build();
			if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();
			
			Utils.debugLog("ProcessorsResource.updateProcessor: get Processor " + sProcessorId);	
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessorToUpdate = oProcessorRepository.getProcessor(sProcessorId);
			
			if (oProcessorToUpdate == null) {
				Utils.debugLog("ProcessorsResource.updateProcessor: unable to find processor " + sProcessorId);
				return Response.serverError().build();
			}
			
			if (!oProcessorToUpdate.getUserId().equals(oUser.getUserId())) {
				
				
				ProcessorSharingRepository oProcessorSharingRepository = new ProcessorSharingRepository();
				
				ProcessorSharing oSharing = oProcessorSharingRepository.getProcessorSharingByUserIdProcessorId(oUser.getUserId(), sProcessorId);
				
				if (oSharing == null) {
					Utils.debugLog("ProcessorsResource.updateProcessor: processor not of user " + oUser.getUserId());
					return Response.status(Status.UNAUTHORIZED).build();					
				}
				else {
					Utils.debugLog("ProcessorsResource.updateProcessor: processor of user " + oProcessorToUpdate.getUserId() + " is shared with " + oUser.getUserId());
				}
				
			}

			oProcessorToUpdate.setDescription(oUpdatedProcessorVM.getProcessorDescription());
			oProcessorToUpdate.setIsPublic(oUpdatedProcessorVM.getIsPublic());
			oProcessorToUpdate.setParameterSample(oUpdatedProcessorVM.getParamsSample());
			oProcessorToUpdate.setTimeoutMs(oUpdatedProcessorVM.getiTimeoutMs());
			oProcessorToUpdate.setVersion(oUpdatedProcessorVM.getProcessorVersion());
			
			oProcessorRepository.updateProcessor(oProcessorToUpdate);
			
			Utils.debugLog("ProcessorsResource.updateProcessor: Updated Processor " + sProcessorId);
			
			return Response.ok().build();
		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessorResource.deleteProcessor: " + oEx);
			return Response.serverError().build();
		}
	}	
	
	
	@POST
	@Path("/updatefiles")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response updateProcessorFiles(@FormDataParam("file") InputStream oInputStreamForFile, @HeaderParam("x-session-token") String sSessionId, 
			@QueryParam("processorId") String sProcessorId,
			@QueryParam("workspace") String sWorkspaceId) throws Exception {

		Utils.debugLog("ProcessorsResource.updateProcessorFiles( oInputStreamForFile, Session: " + sSessionId + ", WS: " + sWorkspaceId + ", Processor: " + sProcessorId + " )");
		try {
			// Check User 
			if (Utils.isNullOrEmpty(sSessionId)) {
				Utils.debugLog("ProcessorsResource.updateProcessorFiles: session is null or empty, aborting");
				return Response.status(401).build();
			}
			User oUser = Wasdi.GetUserFromSession(sSessionId);
			if (oUser==null) {
				Utils.debugLog("ProcessorsResource.updateProcessorFiles: user (from session) is null, aborting");
				return Response.status(401).build();
			}
			
			String sUserId = oUser.getUserId();
			if (Utils.isNullOrEmpty(sUserId)) {
				Utils.debugLog("ProcessorsResource.updateProcessorFiles: userid of user (from session) is null or empty, aborting");
				return Response.status(401).build();
			}
			
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessorToUpdate = oProcessorRepository.getProcessor(sProcessorId);
			
			if (oProcessorToUpdate == null) {
				Utils.debugLog("ProcessorsResource.updateProcessor: unable to find processor " + sProcessorId);
				return Response.serverError().build();
			}
			
			if (!oProcessorToUpdate.getUserId().equals(oUser.getUserId())) {
				Utils.debugLog("ProcessorsResource.updateProcessor: processor not of user " + oProcessorToUpdate.getUserId());
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			// Set the processor path
			String sDownloadRootPath = Wasdi.getDownloadPath(m_oServletConfig);
			File oProcessorPath = new File(sDownloadRootPath+ "/processors/" + oProcessorToUpdate.getName());
			
			// Create folders
			if (!oProcessorPath.exists()) {
				Utils.debugLog("ProcessorsResource.updateProcessorFiles: Processor Path " + oProcessorPath.getPath() + " does not exists. No update");
				return Response.serverError().build();
			}
			
			// Create file
			File oProcessorFile = new File(sDownloadRootPath+"/processors/" + oProcessorToUpdate.getName() + "/" + sProcessorId + ".zip");
			Utils.debugLog("ProcessorsResource.updateProcessorFiles: Processor file Path: " + oProcessorFile.getPath());
			
			//save uploaded file
			int iRead = 0;
			byte[] ayBytes = new byte[1024];
			OutputStream oOutputStream = new FileOutputStream(oProcessorFile);
			while ((iRead = oInputStreamForFile.read(ayBytes)) != -1) {
				oOutputStream.write(ayBytes, 0, iRead);
			}
			oOutputStream.flush();
			oOutputStream.close();
			
			Utils.debugLog("ProcessorsResource.updateProcessorFiles: unzipping the file");
			
			if (unzipProcessor(oProcessorFile, false)) {
				Utils.debugLog("ProcessorsResource.updateProcessorFiles: update done");
				
				if (Wasdi.s_sMyNodeCode.equals("wasdi")) {
					
					try {
						Utils.debugLog("ProcessorsResource.updateProcessorFiles: this is the main node, starting Worker to update computing nodes");
						
						//This is the main node: forward the request to other nodes
						UpdateProcessorFilesWorker oUpdateWorker = new UpdateProcessorFilesWorker();
						
						NodeRepository oNodeRepo = new NodeRepository();
						List<Node> aoNodes = oNodeRepo.getNodesList();
						
						oUpdateWorker.init(aoNodes, oProcessorFile.getPath(), sSessionId, sWorkspaceId, sProcessorId);
						oUpdateWorker.start();
						
						Utils.debugLog("ProcessorsResource.updateProcessorFiles: Worker started");						
					}
					catch (Exception oEx) {
						Utils.debugLog("ProcessorsResource.updateProcessorFiles: error starting UpdateWorker " + oEx.toString());
					}
					
				}
				else {
					
					Utils.debugLog("ProcessorsResource.updateProcessorFiles: this is a computing node, delete local zip file");
					
					// Computational Node: delete the file
					try {
						oProcessorFile.delete();
					}
					catch (Exception oEx) {
						Utils.debugLog("ProcessorsResource.updateProcessorFiles: error deleting zip " + oEx);
					}
				}
			}
			else {
				Utils.debugLog("ProcessorsResource.updateProcessorFiles: error in unzip");
				return Response.serverError().build();
			}

		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessorsResource.updateProcessorFiles:" + oEx);
			return Response.serverError().build();
		}
		return Response.ok().build();
	}
	
	@GET
	@Path("downloadprocessor")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadProcessor(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("token") String sTokenSessionId,
			@QueryParam("processorId") String sProcessorId)
	{			

		Utils.debugLog("ProcessorsResource.downloadProcessor( Session: " + sSessionId + ", processorId: " + sProcessorId);
		
		try {
			
			if( Utils.isNullOrEmpty(sSessionId) == false) {
				sTokenSessionId = sSessionId;
			}
			
			User oUser = Wasdi.GetUserFromSession(sTokenSessionId);

			if (oUser == null) {
				Utils.debugLog("ProcessorsResource.downloadProcessor: user not authorized");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
			
			if (oProcessor == null) {
				Utils.debugLog("ProcessorsResource.downloadProcessor: processor does not exists");
				return Response.status(Status.NO_CONTENT).build();				
			}
			
			String sProcessorName = oProcessor.getName();
			
			// Take path
			String sDownloadRootPath = Wasdi.getDownloadPath(m_oServletConfig);
			String sProcessorZipPath = sDownloadRootPath + "processors/" + sProcessorName + "/" + sProcessorId + ".zip";
			
			File oFile = new File(sProcessorZipPath);
			
			return zipProcessor(oFile, oProcessor);			
		} 
		catch (Exception oEx) {
			Utils.debugLog("ProcessorsResource.downloadProcessor: " + oEx);
		}
		
		return null;
	}	
	
	/**
	 * Zip a full processor
	 * @param oInitialFile
	 * @return
	 */
	private Response zipProcessor(File oInitialFile, Processor oProcessor) {
		
		// Create a stack of files
		Stack<File> aoFileStack = new Stack<File>();
		String sBasePath = oInitialFile.getParent();
		
		Utils.debugLog("ProcessorsResource.zipProcessor: sDir = " + sBasePath);
		
		// Get the processor folder
		File oFile = new File(sBasePath);
		aoFileStack.push(oFile);
				
		if(!sBasePath.endsWith("/") && !sBasePath.endsWith("\\")) {
			sBasePath = sBasePath + "/";
		}
		
		int iBaseLen = sBasePath.length();
		
		String sProcTemplatePath = Wasdi.getDownloadPath(m_oServletConfig);
		sProcTemplatePath += "dockertemplate/";
		sProcTemplatePath += ProcessorTypes.getTemplateFolder(oProcessor.getType()) + "/";
		
		ArrayList<String> asTemplateFiles = new ArrayList<String>();
		File oProcTemplateFolder = new File(sProcTemplatePath);
		
		Utils.debugLog("ProcessorsResource.zipProcessor: Proc Template Path " + sProcTemplatePath);
		
		File[] aoTemplateChildren = oProcTemplateFolder.listFiles();
		for (File oChild : aoTemplateChildren) {
			asTemplateFiles.add(oChild.getName());
		}
		
		
		// Create a map of the files to zip
		Map<String, File> aoFileEntries = new HashMap<>();
		
		while(aoFileStack.size()>=1) {
			
			oFile = aoFileStack.pop();
			String sAbsolutePath = oFile.getAbsolutePath();
			
			Utils.debugLog("ProcessorsResource.zipProcessor: sAbsolute Path " + sAbsolutePath);

			if(oFile.isDirectory()) {
				if(!sAbsolutePath.endsWith("/") && !sAbsolutePath.endsWith("\\")) {
					sAbsolutePath = sAbsolutePath + "/";
				}
				File[] aoChildren = oFile.listFiles();
				for (File oChild : aoChildren) {
					
					if (!asTemplateFiles.contains(oChild.getName())) {
						aoFileStack.push(oChild);
					}
					else {
						Utils.debugLog("ProcessorsResource.zipProcessor: jumping template file " + oChild.getName());
					}
					
				}
			}
			
			String sRelativePath = sAbsolutePath.substring(iBaseLen);
			
			if (!Utils.isNullOrEmpty(sRelativePath)) {
				Utils.debugLog("ProcessorsResource.zipProcessor: adding file " + sRelativePath +" for compression");
				aoFileEntries.put(sRelativePath,oFile);				
			}
			else {
				Utils.debugLog("ProcessorsResource.zipProcessor: jumping empty file");
			}
		}
		
		Utils.debugLog("ProcessorsResource.zipProcessor: done preparing map, added " + aoFileEntries.size() + " files");
					
		ZipStreamingOutput oStream = new ZipStreamingOutput(aoFileEntries);

		// Set response headers and return 
		ResponseBuilder oResponseBuilder = Response.ok(oStream);
		String sFileName = oInitialFile.getName();
				
		Utils.debugLog("ProcessorsResource.zipProcessor: sFileName " + sFileName);
		
		oResponseBuilder.header("Content-Disposition", "attachment; filename=\""+ sFileName +"\"");
		Long lLength = 0L;
		for (String sFile : aoFileEntries.keySet()) {
			File oTempFile = aoFileEntries.get(sFile);
			if(!oTempFile.isDirectory()) {
				//NOTE: this way we are cheating, it is an upper bound, not the real size!
				lLength += oTempFile.length();
			}
		}
		oResponseBuilder.header("Content-Length", lLength);
		Utils.debugLog("ProcessorsResource.zipProcessor: done");
		return oResponseBuilder.build();
	}
	
	public boolean unzipProcessor(File oProcessorZipFile, boolean bDeleteFile) {
		try {
						
			// Unzip the file and, meanwhile, check if a pro file with the same name exists
			String sProcessorFolder = oProcessorZipFile.getParent();
			sProcessorFolder += "/";
			
			byte[] ayBuffer = new byte[1024];
		    ZipInputStream oZipInputStream = new ZipInputStream(new FileInputStream(oProcessorZipFile));
		    ZipEntry oZipEntry = oZipInputStream.getNextEntry();
		    while(oZipEntry != null){
		    	
		    	String sZippedFileName = oZipEntry.getName();
		    	
		    	if (sZippedFileName.toUpperCase().endsWith(".PRO")) {
		    		
		    		// Force extension case
		    	
		    		sZippedFileName = sZippedFileName.replace(".Pro", ".pro");
		    		sZippedFileName = sZippedFileName.replace(".PRO", ".pro");
		    		sZippedFileName = sZippedFileName.replace(".PRo", ".pro");
		    	}
		    	
		    	String sUnzipFilePath = sProcessorFolder+sZippedFileName;
		    	
		    	if (oZipEntry.isDirectory()) {
		    		File oUnzippedDir = new File(sUnzipFilePath);
	                oUnzippedDir.mkdir();
		    	}
		    	else {
			    	
			        File oUnzippedFile = new File(sProcessorFolder + sZippedFileName);
			        FileOutputStream oOutputStream = new FileOutputStream(oUnzippedFile);
			        int iLen;
			        while ((iLen = oZipInputStream.read(ayBuffer)) > 0) {
			        	oOutputStream.write(ayBuffer, 0, iLen);
			        }
			        oOutputStream.close();		    		
		    	}
		        oZipEntry = oZipInputStream.getNextEntry();
	        }
		    oZipInputStream.closeEntry();
		    oZipInputStream.close();
		    
		    
		    try {
			    // Remove the zip?
			    if (bDeleteFile) oProcessorZipFile.delete();		    	
		    }
		    catch (Exception e) {
				Utils.debugLog("ProcessorsResource.UnzipProcessor Exception Deleting Zip File " + e.toString());
				return false;
			}
		    
		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessorsResource.DeployProcessor Exception " + oEx.toString());
			return false;
		}
		
		return true;
	}
	
	
	
	
	@PUT
	@Path("share/add")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult shareProcessor(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId, @QueryParam("userId") String sUserId) {

		Utils.debugLog("ProcessorsResource.shareProcessor( Session: " + sSessionId + ", WS: " + sProcessorId + ", User: " + sUserId + " )");

		// Validate Session
		User oOwnerUser = Wasdi.GetUserFromSession(sSessionId);
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		if (oOwnerUser == null) {
			oResult.setStringValue("Invalid user.");
			return oResult;
		}

		if (Utils.isNullOrEmpty(oOwnerUser.getUserId())) {
			oResult.setStringValue("Invalid user.");
			return oResult;
		}
		
		try {
			
			// Check if the processor exists and is of the user calling this API
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oValidateProcessor = oProcessorRepository.getProcessor(sProcessorId);
			
			if (oValidateProcessor == null) {
				oResult.setStringValue("Invalid processor");
				return oResult;		
			}
			
			if (!oValidateProcessor.getUserId().equals(oOwnerUser.getUserId())) {
				oResult.setStringValue("Unauthorized");
				return oResult;				
			}
			
			// Check the destination user
			UserRepository oUserRepository = new UserRepository();
			User oDestinationUser = oUserRepository.getUser(sUserId);
			
			if (oDestinationUser == null) {
				oResult.setStringValue("Unauthorized");
				return oResult;				
			}
			
			// Check if has been already shared
			ProcessorSharingRepository oProcessorSharingRepository = new ProcessorSharingRepository();
			
			ProcessorSharing oAlreadyExists = oProcessorSharingRepository.getProcessorSharingByUserIdProcessorId(sUserId, sProcessorId);
			
			if (oAlreadyExists != null) {
				oResult.setStringValue("Already shared");
				return oResult;					
			}
			
			// Create and insert the sharing
			ProcessorSharing oProcessorSharing = new ProcessorSharing();
			Timestamp oTimestamp = new Timestamp(System.currentTimeMillis());
			oProcessorSharing.setOwnerId(oOwnerUser.getUserId());
			oProcessorSharing.setUserId(sUserId);
			oProcessorSharing.setProcessorId(sProcessorId);
			oProcessorSharing.setShareDate((double) oTimestamp.getTime());
			oProcessorSharingRepository.insertProcessorSharing(oProcessorSharing);
			
			Utils.debugLog("Processor " + sProcessorId + " Shared from " + oOwnerUser.getUserId() + " to " + sUserId);
			
		} catch (Exception oEx) {
			Utils.debugLog("ProcessorsResource.shareProcessor: " + oEx);

			oResult.setStringValue("Error in save proccess");
			oResult.setBoolValue(false);

			return oResult;
		}

		oResult.setStringValue("Done");
		oResult.setBoolValue(true);

		return oResult;
	}
	
	
	
	@GET
	@Path("share/byprocessor")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public List<ProcessorSharing> getEnableUsersSharedProcessor(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId) {

		Utils.debugLog("ProcessorsResource.getEnableUsersSharedProcessor( Session: " + sSessionId + ", Processor: " + sProcessorId + " )");

		// Validate Session
		User oOwnerUser = Wasdi.GetUserFromSession(sSessionId);
		List<ProcessorSharing> aoProcessorSharing = null;

		if (oOwnerUser == null) {
			Utils.debugLog("ProcessorsResource.getEnableUsersSharedProcessor: Owner null return");
			return null;
		}

		if (Utils.isNullOrEmpty(oOwnerUser.getUserId())) {
			Utils.debugLog("ProcessorsResource.getEnableUsersSharedProcessor: User Id null return");
			return null;
		}
		
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
		
		if (oProcessor == null) {
			Utils.debugLog("ProcessorsResource.getEnableUsersSharedProcessor: Unable to find processor return");
			return null;
		}


		try {
			ProcessorSharingRepository oProcessorSharingRepository = new ProcessorSharingRepository();
			
			aoProcessorSharing = oProcessorSharingRepository.getProcessorSharingByProcessorId(sProcessorId);
			
		} catch (Exception oEx) {
			Utils.debugLog("ProcessorsResource.getEnableUsersSharedProcessor: " + oEx);
			return null;
		}

		return aoProcessorSharing;

	}

	@DELETE
	@Path("share/delete")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult deleteUserSharingProcessor(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId, @QueryParam("userId") String sUserId) {

		Utils.debugLog("ProcessorsResource.deleteUserSharedProcessor( Session: " + sSessionId + ", WS: " + sProcessorId + ", User:" + sUserId + " )");
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		// Validate Session
		User oOwnerUser = Wasdi.GetUserFromSession(sSessionId);

		if (oOwnerUser == null) {
			oResult.setStringValue("Invalid user.");
			return oResult;
		}

		if (Utils.isNullOrEmpty(oOwnerUser.getUserId())) {
			oResult.setStringValue("Invalid user.");
			return oResult;
		}
		
		if (Utils.isNullOrEmpty(sUserId)) {
			oResult.setStringValue("Invalid shared user.");
			return oResult;			
		}

		try {
			ProcessorSharingRepository oProcessorSharingRepository = new ProcessorSharingRepository();
			
			ProcessorSharing oProcShare = oProcessorSharingRepository.getProcessorSharingByUserIdProcessorId(sUserId, sProcessorId);
			
			if (oProcShare!= null) {
				if (oProcShare.getUserId().equals(oOwnerUser.getUserId()) || oProcShare.getOwnerId().equals(oOwnerUser.getUserId())) {
					oProcessorSharingRepository.deleteByUserIdProcessorId(sUserId, sProcessorId);
				}
				else {
					oResult.setStringValue("Unauthorized");
					return oResult;					
				}
			}
			else {
				oResult.setStringValue("Sharing not found");
				return oResult;				
			}
		} 
		catch (Exception oEx) {
			Utils.debugLog("ProcessorsResource.deleteUserSharedProcessor: " + oEx);
			oResult.setStringValue("Error deleting processor sharing");
			return oResult;
		}

		oResult.setStringValue("Done");
		oResult.setBoolValue(true);
		return oResult;

	}
}
