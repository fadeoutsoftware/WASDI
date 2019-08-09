package it.fadeout.rest.resources;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.media.multipart.FormDataParam;

import it.fadeout.Wasdi;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.Counter;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Processor;
import wasdi.shared.business.ProcessorLog;
import wasdi.shared.business.ProcessorTypes;
import wasdi.shared.business.User;
import wasdi.shared.data.CounterRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorLogRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.SerializationUtils;
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
	public Response uploadProcessor(@FormDataParam("file") InputStream oInputStreamForFile, @HeaderParam("x-session-token") String sSessionId, 
			@QueryParam("workspace") String sWorkspaceId, @QueryParam("name") String sName, @QueryParam("version") String sVersion,
			@QueryParam("description") String sDescription, @QueryParam("type") String sType, @QueryParam("paramsSample") String sParamsSample) throws Exception {

		Utils.debugLog("ProcessorsResource.uploadProcessor( oInputStreamForFile, " + sSessionId + ", " + sWorkspaceId + ", " + sName + ", " + sVersion + 
				sDescription + ", " + sType + ", " + sParamsSample + " )");
		try {
			// Check User 
			if (Utils.isNullOrEmpty(sSessionId)) {
				Utils.debugLog("ProcessorsResource.uploadProcessor: session is null or empty, aborting");
				return Response.status(401).build();
			}
			User oUser = Wasdi.GetUserFromSession(sSessionId);
			if (oUser==null) {
				Utils.debugLog("ProcessorsResource.uploadProcessor: user (from session) is null, aborting");
				return Response.status(401).build();
			}
			String sUserId = oUser.getUserId();
			if (Utils.isNullOrEmpty(sUserId)) {
				Utils.debugLog("ProcessorsResource.uploadProcessor: userid of user (from session) is null or empty, aborting");
				return Response.status(401).build();
			}
			
			// Set the processor path
			String sDownloadRootPath = m_oServletConfig.getInitParameter("DownloadRootPath");
			if (!sDownloadRootPath.endsWith("/")) {
				sDownloadRootPath = sDownloadRootPath + "/";
			}
			File oProcessorPath = new File(sDownloadRootPath+ "/processors/" + sName);
			
			// Create folders
			if (!oProcessorPath.exists()) {
				oProcessorPath.mkdirs();
			} // TODO: else: it shouldn't exist. Shall we oaverwrite it?
			
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
				sType = ProcessorTypes.UBUNTU_PYTHON_SNAP;
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
			if (!Utils.isNullOrEmpty(sParamsSample)) {
				oProcessor.setParameterSample(sParamsSample);
			}
			
			// Store in the db
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			oProcessorRepository.InsertProcessor(oProcessor);
			
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
			if (! (sPath.endsWith("/")||sPath.endsWith("\\"))) sPath+="/";
			sPath += sProcessObjId;

			SerializationUtils.serializeObjectToXML(sPath, oDeployProcessorParameter);

			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			ProcessWorkspace oProcessWorkspace = new ProcessWorkspace();
			
			try{
				oProcessWorkspace.setOperationDate(Wasdi.GetFormatDate(new Date()));
				oProcessWorkspace.setOperationType(LauncherOperations.DEPLOYPROCESSOR.name());
				oProcessWorkspace.setProductName(sName);
				oProcessWorkspace.setWorkspaceId(sWorkspaceId);
				oProcessWorkspace.setUserId(sUserId);
				oProcessWorkspace.setProcessObjId(sProcessObjId);
				oProcessWorkspace.setStatus(ProcessStatus.CREATED.name());
				oRepository.InsertProcessWorkspace(oProcessWorkspace);
				
				Utils.debugLog("ProcessorResource.uploadProcessor: Process Scheduled for Launcher");
			} catch(Exception oEx){
				Utils.debugLog("ProcessorsResource.uploadProcessor: Error scheduling the deploy process " + oEx);
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
			}
		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessorsResource.uploadProcessor:" + oEx);
			return Response.serverError().build();
		}
		return Response.ok().build();
	}
	
	@GET
	@Path("/getdeployed")
	public List<DeployedProcessorViewModel> getDeployedProcessors(@HeaderParam("x-session-token") String sSessionId) throws Exception {

		ArrayList<DeployedProcessorViewModel> aoRet = new ArrayList<>(); 
		Utils.debugLog("ProcessorsResource.getDeployedProcessors( " + sSessionId + " )");
		
		try {
			// Check User 
			if (Utils.isNullOrEmpty(sSessionId)) return aoRet;
			User oUser = Wasdi.GetUserFromSession(sSessionId);

			if (oUser==null) return aoRet;
			if (Utils.isNullOrEmpty(oUser.getUserId())) return aoRet;
						
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			List<Processor> aoDeployed = oProcessorRepository.GetDeployedProcessors();
			
			for (int i=0; i<aoDeployed.size(); i++) {
				DeployedProcessorViewModel oVM = new DeployedProcessorViewModel();
				Processor oProcessor = aoDeployed.get(i);
				
				oVM.setProcessorDescription(oProcessor.getDescription());
				oVM.setProcessorId(oProcessor.getProcessorId());
				oVM.setProcessorName(oProcessor.getName());
				oVM.setProcessorVersion(oProcessor.getVersion());
				oVM.setPublisher(oProcessor.getUserId());
				oVM.setParamsSample(oProcessor.getParameterSample());
				
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
			@QueryParam("workspace") String sWorkspaceId) throws Exception {
		Utils.debugLog("ProcessorsResource.run( " + sSessionId + ", " + sName + ", encodedJson, " + sWorkspaceId + " )");

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
			Processor oProcessorToRun = oProcessorRepository.GetProcessorByName(sName);
			
			if (oProcessorToRun == null) {
				Utils.debugLog("ProcessorsResource.run: unable to find processor " + sName);
				oRunningProcessorViewModel.setStatus("ERROR");
				return oRunningProcessorViewModel;
			}

			// Schedule the process to run the processor
			
			String sProcessObjId = Utils.GetRandomName();
			
			String sPath = m_oServletConfig.getInitParameter("SerializationPath");
			if (! (sPath.endsWith("/")||sPath.endsWith("\\"))) sPath+="/";
			sPath += sProcessObjId;

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
			
			SerializationUtils.serializeObjectToXML(sPath, oProcessorParameter);

			
			ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			ProcessWorkspace oProcessWorkspace = new ProcessWorkspace();
			
			try{
				Utils.debugLog("ProcessorsResource.run: create task"); 
				oProcessWorkspace.setOperationDate(Wasdi.GetFormatDate(new Date()));
				oProcessWorkspace.setOperationType(oProcessorParameter.getLauncherOperation());
				oProcessWorkspace.setProductName(sName);
				oProcessWorkspace.setWorkspaceId(sWorkspaceId);
				oProcessWorkspace.setUserId(sUserId);
				oProcessWorkspace.setProcessObjId(sProcessObjId);
				oProcessWorkspace.setStatus(ProcessStatus.CREATED.name());
				oProcessWorkspaceRepository.InsertProcessWorkspace(oProcessWorkspace);
				
				Utils.debugLog("ProcessorResource.run: Process Scheduled for Launcher");
								
				oRunningProcessorViewModel.setJsonEncodedResult("");
				oRunningProcessorViewModel.setName(sName);
				oRunningProcessorViewModel.setProcessingIdentifier(oProcessWorkspace.getProcessObjId());
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
		Utils.debugLog("ProcessorsResource.help( " + sSessionId + ", " + sName + " )");
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
			Processor oProcessorToRun = oProcessorRepository.GetProcessorByName(sName);
			
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
		Utils.debugLog("ProcessorsResource.status( " + sSessionId + ", " + sProcessingId + " )");
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
			ProcessWorkspace oProcessWorkspace = oProcessWorkspaceRepository.GetProcessByProcessObjId(sProcessingId);
			
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
			Processor oProcessor = oProcessorRepository.GetProcessor(oProcessWorkspace.getProductName());
			
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
			String sResult = oProcessorLogRepository.InsertProcessLog(oLog);
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
		Utils.debugLog("ProcessorResource.countLogs( " + sSessionId + ", " + sProcessWorkspaceId + " )");
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
			oCounter = oCounterRepository.GetCounterBySequence(sProcessWorkspaceId);
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
		
		Utils.debugLog("ProcessorsResource.getLogs( " + sSessionId + ", " + sProcessWorkspaceId + ", " + iStartRow + ", " + iEndRow + " )");
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
				aoLogs = oProcessorLogRepository.GetLogsByProcessWorkspaceId(sProcessWorkspaceId);
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
		Utils.debugLog("ProcessorResources.deleteProcessor( " + sSessionId + ", " + sProcessorId + ", " + sWorkspaceId + " )");
		
		try {
			if (Utils.isNullOrEmpty(sSessionId)) return Response.status(Status.UNAUTHORIZED).build();
			User oUser = Wasdi.GetUserFromSession(sSessionId);

			if (oUser==null) return Response.status(Status.UNAUTHORIZED).build();
			if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();

			String sUserId = oUser.getUserId();
			
			Utils.debugLog("ProcessorsResource.deleteProcessor: get Processor");	
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessorToDelete = oProcessorRepository.GetProcessor(sProcessorId);
			
			if (oProcessorToDelete == null) {
				Utils.debugLog("ProcessorsResource.deleteProcessor: unable to find processor " + sProcessorId);
				return Response.serverError().build();
			}

			// Schedule the process to run the processor
			
			String sProcessObjId = Utils.GetRandomName();
			
			String sPath = m_oServletConfig.getInitParameter("SerializationPath");
			if (! (sPath.endsWith("/")||sPath.endsWith("\\"))) sPath+="/";
			sPath += sProcessObjId;

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
			
			SerializationUtils.serializeObjectToXML(sPath, oProcessorParameter);

			
			ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			ProcessWorkspace oProcessWorkspace = new ProcessWorkspace();
			
			try{
				Utils.debugLog("ProcessorsResource.deleteProcessor: create task"); 
				oProcessWorkspace.setOperationDate(Wasdi.GetFormatDate(new Date()));
				oProcessWorkspace.setOperationType(LauncherOperations.DELETEPROCESSOR.name());
				oProcessWorkspace.setProductName(oProcessorToDelete.getName());
				oProcessWorkspace.setWorkspaceId(sWorkspaceId);
				oProcessWorkspace.setUserId(sUserId);
				oProcessWorkspace.setProcessObjId(sProcessObjId);
				oProcessWorkspace.setStatus(ProcessStatus.CREATED.name());
				oProcessWorkspaceRepository.InsertProcessWorkspace(oProcessWorkspace);
				
				Utils.debugLog("ProcessorResource.deleteProcessor: Process Scheduled for Launcher");
								
				Utils.debugLog("ProcessorsResource.deleteProcessor: done"); 
			}
			catch(Exception oEx){
				Utils.debugLog("ProcessorsResource.deleteProcessor: Error scheduling the run process: " + oEx);
				return Response.serverError().build();
			}
			
			
			return Response.ok().build();
		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessorResource.deleteProcessor: " + oEx);
			return Response.serverError().build();
		}
	}
}
