package it.fadeout.rest.resources;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;
import javax.servlet.ServletConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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

import org.apache.commons.io.FilenameUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import it.fadeout.Wasdi;
import it.fadeout.business.BaseResource;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.AppCategory;
import wasdi.shared.business.Counter;
import wasdi.shared.business.ImageFile;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Processor;
import wasdi.shared.business.ProcessorLog;
import wasdi.shared.business.ProcessorTypes;
import wasdi.shared.business.Review;
import wasdi.shared.business.User;
import wasdi.shared.data.AppsCategoriesRepository;
import wasdi.shared.data.CounterRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorLogRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.ReviewRepository;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.AppCategoryViewModel;
import wasdi.shared.viewmodels.DeployedProcessorViewModel;
import wasdi.shared.viewmodels.ListReviewsViewModel;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.ProcessorLogViewModel;
import wasdi.shared.viewmodels.ReviewViewModel;
import wasdi.shared.viewmodels.RunningProcessorViewModel;

@Path("/processors")
public class ProcessorsResource extends BaseResource{
	//TODO CHANGE IT
	final String PROCESSORS_PATH =  "C:\\temp\\wasdi\\data\\processors\\";
	final String LOGO_PROCESSORS_PATH = "\\logo\\";
	final String IMAGES_PROCESSORS_PATH = "\\images\\";
	final String[] IMAGE_PROCESSORS_EXTENSIONS = {"jpg", "png", "svg"};
	final String DEFAULT_LOGO_PROCESSOR_NAME = "logo";
	final Integer LOGO_SIZE = 180;
	final Integer NUMB_MAX_OF_IMAGES = 5;
	final String[] IMAGES_NAME = { "1", "2", "3", "4", "5" };
	final String[] RANGE_OF_VOTES = { "1", "2", "3", "4", "5" };
	AppsCategoriesRepository m_oAppCategoriesRepository = new AppsCategoriesRepository();
	ReviewRepository m_oReviewRepository = new ReviewRepository();
	
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
	public PrimitiveResult uploadProcessor( @FormDataParam("file") InputStream oInputStreamForFile, @HeaderParam("x-session-token") String sSessionId, 
											@QueryParam("workspace") String sWorkspaceId, @QueryParam("name") String sName,
											@QueryParam("version") String sVersion,	@QueryParam("description") String sDescription,
											@QueryParam("type") String sType, @QueryParam("paramsSample") String sParamsSample,
											@QueryParam("public") Integer iPublic, @QueryParam("timeout") Integer iTimeout		, 
											@QueryParam("link") String sLink , @QueryParam("email") String sEmail, @QueryParam("price") Integer iPrice) throws Exception {

//		,
//		@QueryParam("categories") String[] asCategoriesId
//		
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
			String sDownloadRootPath = m_oServletConfig.getInitParameter("DownloadRootPath");
			if (!sDownloadRootPath.endsWith("/")) {
				sDownloadRootPath = sDownloadRootPath + "/";
			}
			
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
			oProcessor.setLink(sLink);
			oProcessor.setEmail(sEmail);
			oProcessor.setPrice(iPrice);
		//	oProcessor.setCategoriesId(asCategoriesId);
			
			if( iTimeout != null ){
				oProcessor.setTimeoutMs(iTimeout);
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
			List<Processor> aoDeployed = oProcessorRepository.getDeployedProcessors();
			
			for (int i=0; i<aoDeployed.size(); i++) {
				DeployedProcessorViewModel oVM = new DeployedProcessorViewModel();
				Processor oProcessor = aoDeployed.get(i);
				
				if (oProcessor.getIsPublic() != 1) {
					if (oProcessor.getUserId().equals(oUser.getUserId()) == false) continue;
				}
				
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
				Utils.debugLog("ProcessorsResource.updateProcessor: processor not of user " + oProcessorToUpdate.getUserId());
				return Response.status(Status.UNAUTHORIZED).build();
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
			String sDownloadRootPath = m_oServletConfig.getInitParameter("DownloadRootPath");
			if (!sDownloadRootPath.endsWith("/")) {
				sDownloadRootPath = sDownloadRootPath + "/";
			}
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
			
			if (UnzipProcessor(oProcessorFile)) {
				Utils.debugLog("ProcessorsResource.updateProcessorFiles: update done");
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
	
	
	@POST
	@Path("/uploadProcessorLogo")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadProcessorLogo(@FormDataParam("image") InputStream fileInputStream, @FormDataParam("image") FormDataContentDisposition fileMetaData,
										@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId ) {
		String sExt;
		String sFileName;
		User oUser = getUser(sSessionId);
		// Check the user session
		if(oUser == null){
			return Response.status(401).build();
		}
		
		String sUserId = oUser.getUserId();
		Processor oProcessor = getProcessor(sProcessorId);
		
		if(oProcessor != null && Utils.isNullOrEmpty(oProcessor.getName()) ) {
			return Response.status(400).build();
		}
		
		//check if the user is the owner of the processor 
		if( oProcessor.getUserId().equals( oUser.getId() ) == false ){
			return Response.status(401).build();
		}
		
		//get filename and extension 
		if(fileMetaData != null && Utils.isNullOrEmpty(fileMetaData.getFileName()) == false){
			sFileName = fileMetaData.getFileName();
			sExt = FilenameUtils.getExtension(sFileName);
		} else {
			return Response.status(400).build();
		}
		
		
		if( isValidExtension(sExt) == false ){
			return Response.status(400).build();
		}

		// Take path
		String sPath = PROCESSORS_PATH + oProcessor.getName() + LOGO_PROCESSORS_PATH;
		
		String sExtensionOfSavedLogo = getExtensionOfSavedLogo(sPath);
		
		//if there is a saved logo with a different extension remove it 
		if( sExtensionOfSavedLogo.isEmpty() == false && sExtensionOfSavedLogo.equalsIgnoreCase(sExt) == false ){
		    File oOldLogo = new File(sPath + DEFAULT_LOGO_PROCESSOR_NAME + "." + sExtensionOfSavedLogo);
		    oOldLogo.delete();
		}
			
		createDirectory(sPath);
	    
	    String sOutputFilePath = sPath + DEFAULT_LOGO_PROCESSOR_NAME + "." + sExt.toLowerCase();
	    ImageFile oOutputLogo = new ImageFile(sOutputFilePath);
	    
	    boolean bIsSaved =  oOutputLogo.saveImage(fileInputStream);
	    if(bIsSaved == false){
	    	return Response.status(400).build();
	    }
	    
	    boolean bIsResized = oOutputLogo.resizeImage(LOGO_SIZE, LOGO_SIZE);
	    if(bIsResized == false){
	    	return Response.status(400).build();
	    }
	    
		return Response.status(200).build();
	}


	
	@GET
	@Path("/getlogo")
	public Response getProcessorLogo(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId ) {


		Processor oProcessor = getProcessor(sProcessorId);
		if(oProcessor == null){
			return Response.status(401).build();
		}
			
		User oUser = getUser(sSessionId);
		// Check the user session
		if(oUser == null){
			return Response.status(401).build();
		}
		
		String sPathLogoFolder = PROCESSORS_PATH + oProcessor.getName() + LOGO_PROCESSORS_PATH;
		ImageFile oLogo = getLogoInFolder(sPathLogoFolder);
		String sLogoExtension = getExtensionOfSavedLogo(sPathLogoFolder);
		
		//Check the logo and extension
		if(oLogo == null || sLogoExtension.isEmpty() ){
			return Response.status(204).build();
		}
		//prepare buffer and send the logo to the client 
		ByteArrayInputStream abImageLogo = oLogo.getByteArrayImage();
		
	    return Response.ok(abImageLogo).build();

	}
	
	
	@GET
	@Path("/getappimage")
	public Response getAppImage(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId,
								@QueryParam("imageName") String sImageName) {


		Processor oProcessor = getProcessor(sProcessorId);
		if(oProcessor == null){
			return Response.status(401).build();
		}
			
		User oUser = getUser(sSessionId);
		// Check the user session
		if(oUser == null){
			return Response.status(401).build();
		}
		
		String sPathLogoFolder = PROCESSORS_PATH + oProcessor.getName() + IMAGES_PROCESSORS_PATH;
		ImageFile oImage = getImageInFolder(sPathLogoFolder,sImageName);
		String sLogoExtension = getExtensionOfSavedImage(sPathLogoFolder,sImageName);
		
		//Check the logo and extension
		if(oImage == null || sLogoExtension.isEmpty() ){
			return Response.status(204).build();
		}
		//prepare buffer and send the logo to the client 
		ByteArrayInputStream abImage = oImage.getByteArrayImage();
		
	    return Response.ok(abImage).build();

	}
	
	@DELETE
	@Path("/deleteprocessorimage")
	public Response deleteProcessorImage(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId, @QueryParam("imageName") String sImageName ) {
		User oUser = getUser(sSessionId);
		// Check the user session
		if(oUser == null){
			return Response.status(401).build();
		}
		
		String sUserId = oUser.getUserId();
		Processor oProcessor = getProcessor(sProcessorId);
		
		if( oProcessor != null && Utils.isNullOrEmpty(oProcessor.getName()) ) {
			return Response.status(400).build();
		}
		
		if( sImageName== null || sImageName.isEmpty() ) {
			return Response.status(400).build();
		}

		//check if the user is the owner of the processor 
		if( oProcessor.getUserId().equals( oUser.getId() ) == false ){
			return Response.status(401).build();
		}
		
		String sPathFolder = PROCESSORS_PATH + oProcessor.getName() + IMAGES_PROCESSORS_PATH;
		deleteFileInFolder(sPathFolder,sImageName);
		
		return Response.status(200).build();
	}
	
	
	@POST
	@Path("/uploadProcessorImage")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadProcessorImage(@FormDataParam("image") InputStream fileInputStream, @FormDataParam("image") FormDataContentDisposition fileMetaData,
										@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId ) {
	
		String sExt;
		String sFileName;

		
		User oUser = getUser(sSessionId);
		// Check the user session
		if(oUser == null){
			return Response.status(401).build();
		}
	
		String sUserId = oUser.getUserId();
		Processor oProcessor = getProcessor(sProcessorId);
		
		if(oProcessor != null && Utils.isNullOrEmpty(oProcessor.getName()) ) {
			return Response.status(400).build();
		}
		
		//check if the user is the owner of the processor 
		if( oProcessor.getUserId().equals( oUser.getId() ) == false ){
			return Response.status(401).build();
		}
		
		//get filename and extension 
		if(fileMetaData != null && Utils.isNullOrEmpty(fileMetaData.getFileName()) == false){
			sFileName = fileMetaData.getFileName();
			sExt = FilenameUtils.getExtension(sFileName);
		} else {
			return Response.status(400).build();
		}
		
		if( isValidExtension(sExt) == false ){
			return Response.status(400).build();
		}
		// Take path
		String sPathFolder = PROCESSORS_PATH + oProcessor.getName() + IMAGES_PROCESSORS_PATH;
		createDirectory(sPathFolder);
		String sAvaibleFileName = getAvaibleFileName(sPathFolder);
		
		if(sAvaibleFileName.isEmpty()){
			//the user have reach the max number of images 
	    	return Response.status(400).build();
		}
		
		String sPathImage = sPathFolder + sAvaibleFileName + "." + sExt.toLowerCase();
		ImageFile oNewImage = new ImageFile(sPathImage);

		//TODO SCALE IMAGE ?
		boolean bIsSaved = oNewImage.saveImage(fileInputStream);
	    if(bIsSaved == false){
	    	return Response.status(400).build();
	    }
	    
		double bytes = oNewImage.length();
		double kilobytes = (bytes / 1024);
		double megabytes = (kilobytes / 1024);
		if( megabytes > 2 ){		
			oNewImage.delete();
	    	return Response.status(400).build();
		}
		
		return Response.status(200).build();
	}
	
	@GET
	@Path("/getcategories")
	public Response getCategories(@HeaderParam("x-session-token") String sSessionId) {


		User oUser = getUser(sSessionId);
		// Check the user session
		if(oUser == null){
			return Response.status(401).build();
		}
		
		List<AppCategory> aoAppCategories = m_oAppCategoriesRepository.getCategories();
		ArrayList<AppCategoryViewModel> aoAppCategoriesViewModel = getCategoriesViewModel(aoAppCategories);
		
	    return Response.ok(aoAppCategoriesViewModel).build();

	}
	
	@POST
	@Path("/addreview")
//	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response addReview(@HeaderParam("x-session-token") String sSessionId, ReviewViewModel oReviewViewModel) {//
	
		User oUser = getUser(sSessionId);
		// Check the user session
		if(oUser == null){
			return Response.status(401).build();
		}
		
		String sUserId = oUser.getUserId();
		//CHEK USER ID TOKEN AND USER ID IN VIEW MODEL ARE === 
		if(oReviewViewModel.getUserId().toLowerCase().equals(sUserId.toLowerCase()) == false){
			return Response.status(400).build();
		}
		
		if(oReviewViewModel == null ){
			return Response.status(400).build();
		}
		
		//CHECK THE VALUE OF THE VOTE === 1 - 5
		if( isValidVote(oReviewViewModel.getVote()) == false ){
			return Response.status(400).build();
		}
		
		Review oReview = getReviewModel(oReviewViewModel);
		
		//LIMIT THE NUMBER OF COMMENTS
		if(m_oReviewRepository.alreadyVoted(oReview) == true){
			return Response.status(400).build();
		}
		
		//ADD DATE 
		Date oDate = new Date();
		oReview.setDate(oDate);
		
		// REFACTORING REPOSITORY 
		
		m_oReviewRepository.addReview(oReview);
		
		return Response.status(200).build();
	}
	
	@GET
	@Path("/getreviews")
	public Response getReview (@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId ) {


		User oUser = getUser(sSessionId);
		// Check the user session
		if(oUser == null){
			return Response.status(401).build();
		}
		Processor oProcessor = getProcessor(sProcessorId);
		
		if(oProcessor != null && Utils.isNullOrEmpty(oProcessor.getName()) ) {
			return Response.status(400).build();
		}
		
		List<Review> aoReviewRepository = m_oReviewRepository.getReviews(sProcessorId);
		ListReviewsViewModel oListReviewsViewModel = getListReviewsViewModel(aoReviewRepository);
		
	    return Response.ok(oListReviewsViewModel).build();

	}
	
	
	private boolean isValidVote(String sVote){
		boolean bIsValid = false;
		for(String sValidVote : RANGE_OF_VOTES){
			
			if(sValidVote.equals(sVote.toLowerCase())){
				bIsValid = true;
			}
		}
		return bIsValid;
	}
	
	private Review getReviewModel(ReviewViewModel oReviewViewModel){
		if(oReviewViewModel != null){
			Review oReview = new Review();
			oReview.setComment(oReviewViewModel.getComment());
			oReview.setDate(oReviewViewModel.getDate());
			oReview.setId(oReviewViewModel.getId());//TODO GENERATE ID 
			oReview.setProcessorId(oReviewViewModel.getProcessorId());
			oReview.setUserId(oReviewViewModel.getUserId());
			oReview.setVote(oReviewViewModel.getVote());
			return oReview;
		}
		return null;
	}
	
	private ListReviewsViewModel getListReviewsViewModel(List<Review> aoReviewRepository ){
		ListReviewsViewModel oListReviews = new ListReviewsViewModel();
		List<ReviewViewModel> aoReviews = new ArrayList<ReviewViewModel>();
		if(aoReviewRepository == null){
			return null; 
		}
		
		//CHECK VALUE VOTE policy 1 - 5
		int iSumVotes = 0;

		for(Review oReview: aoReviewRepository){
			ReviewViewModel oReviewViewModel = new ReviewViewModel();
			oReviewViewModel.setComment(oReview.getComment());
			oReviewViewModel.setDate(oReview.getDate());
			oReviewViewModel.setId(oReview.getId());
			oReviewViewModel.setUserId(oReview.getUserId());
			oReviewViewModel.setProcessorId(oReview.getUserId());
			oReviewViewModel.setVote(oReview.getVote());
			iSumVotes = iSumVotes + Integer.parseInt(oReview.getVote());
			
			aoReviews.add(oReviewViewModel);
		}
		
		float avgVote = (float)iSumVotes / aoReviews.size();
		
		oListReviews.setReviews(aoReviews);
		oListReviews.setAvgVote(avgVote);
		oListReviews.setNumberOfOneStarVotes(getNumberOfVotes(aoReviews , 1));
		oListReviews.setNumberOfTwoStarVotes(getNumberOfVotes(aoReviews , 2));
		oListReviews.setNumberOfThreeStarVotes(getNumberOfVotes(aoReviews , 3));
		oListReviews.setNumberOfFourStarVotes(getNumberOfVotes(aoReviews , 4));
		oListReviews.setNumberOfFiveStarVotes(getNumberOfVotes(aoReviews , 5));

		return oListReviews;
	}
	
	private int getNumberOfVotes(List<ReviewViewModel> aoReviews, int iVotes ){
		int iNumberOfVotes = 0;
		for(ReviewViewModel oReview : aoReviews){
			if( Integer.parseInt(oReview.getVote()) == iVotes){
				iNumberOfVotes++;
			}
			
		}
		return iNumberOfVotes;
	}
	
	private ArrayList<AppCategoryViewModel> getCategoriesViewModel(List<AppCategory> aoAppCategories ){
		
		ArrayList<AppCategoryViewModel> aoAppCategoriesViewModel = new ArrayList<AppCategoryViewModel>();
		
		for(AppCategory oCategory:aoAppCategories){
			AppCategoryViewModel oAppCategoryViewModel = new AppCategoryViewModel();
			oAppCategoryViewModel.setId(oCategory.getId());
			oAppCategoryViewModel.setCategory(oCategory.getCategory());
			aoAppCategoriesViewModel.add(oAppCategoryViewModel);
		}
		
		return aoAppCategoriesViewModel;
	} 
	
	private void deleteFileInFolder(String sPathFolder,String sDeleteFileName){
		File oFolder = new File(sPathFolder);
		File[] aoListOfFiles = oFolder.listFiles();
		for (File oImage : aoListOfFiles){ 
			String sName = oImage.getName();
			String sFileName = FilenameUtils.removeExtension(sName);	
			
			if(sDeleteFileName.equalsIgnoreCase(sFileName)){
				oImage.delete();
				break;
			} 
			
		}
	}
	
	private void createDirectory(String sPath){
		File oDirectory = new File(sPath);
		//create directory
	    if (! oDirectory.exists()){
	    	oDirectory.mkdir();
	    }
	} 

	
	// return a free name for the image (if is possible) 
	private String getAvaibleFileName(String sPathFolder) {
		File oFolder = new File(sPathFolder);
		File[] aoListOfFiles = oFolder.listFiles();

		String sReturnValueName = "";
		boolean bIsAvaibleName = false; 
		for (String sAvaibleFileName : IMAGES_NAME){
			bIsAvaibleName = true;
			sReturnValueName = sAvaibleFileName;
			
			for (File oImage : aoListOfFiles){ 
				String sName = oImage.getName();
				String sFileName = FilenameUtils.removeExtension(sName);	
				
				if(sAvaibleFileName.equalsIgnoreCase(sFileName)){
					bIsAvaibleName = false;
					break;
				} 
				
			}
			
			if(bIsAvaibleName == true){
				break;
			}
			sReturnValueName = "";
		 }

		return sReturnValueName;
	}
	
	//return null if there isn't any saved logo
	private ImageFile getImageInFolder(String sPathLogoFolder, String sImageName){
		ImageFile oImage = null;
		String sLogoExtension = getExtensionOfSavedImage(sPathLogoFolder,sImageName);
		if(sLogoExtension.isEmpty() == false){
			oImage = new ImageFile(sPathLogoFolder + sImageName + "." + sLogoExtension );
		}
		return oImage;
		
	}
	
	//return empty string if there isn't any saved logo
	private String getExtensionOfSavedImage (String sPathLogoFolder , String sImageName){
		File oLogo = null;
		String sExtensionReturnValue = "";
		for (String sValidExtension : IMAGE_PROCESSORS_EXTENSIONS) {
			oLogo = new File(sPathLogoFolder + sImageName + "." + sValidExtension );
		    if (oLogo.exists()){
		    	sExtensionReturnValue = sValidExtension;
		    	break;
		    }

		}
		return sExtensionReturnValue;
	}
	
	//return null if there isn't any saved logo
	private ImageFile getLogoInFolder(String sPathLogoFolder){
		ImageFile oLogo = null;
		String sLogoExtension = getExtensionOfSavedLogo(sPathLogoFolder);
		if(sLogoExtension.isEmpty() == false){
			oLogo = new ImageFile(sPathLogoFolder + DEFAULT_LOGO_PROCESSOR_NAME + "." + sLogoExtension );
		}
		return oLogo;
		
	}
	
	
	//return empty string if there isn't any saved logo
	private String getExtensionOfSavedLogo (String sPathLogoFolder){
		File oLogo = null;
		String sExtensionReturnValue = "";
		for (String sValidExtension : IMAGE_PROCESSORS_EXTENSIONS) {
			oLogo = new File(sPathLogoFolder + DEFAULT_LOGO_PROCESSOR_NAME + "." + sValidExtension );
		    if (oLogo.exists()){
		    	sExtensionReturnValue = sValidExtension;
		    	break;
		    }

		}
		return sExtensionReturnValue;
	}
	

	
	private boolean isValidExtension(String sExt){
		//Check if the extension is valid
		for (String sValidExtension : IMAGE_PROCESSORS_EXTENSIONS) {
			  if(sValidExtension.equals(sExt.toLowerCase()) ){
				  return true;
			  }
		}
		return false;
	}
	
	private Processor getProcessor(String sProcessorId){
	
		if(Utils.isNullOrEmpty(sProcessorId)) {
			return null;
		}
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
		return oProcessor;
	}
	
    
	public boolean UnzipProcessor(File oProcessorZipFile) {
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
			    oProcessorZipFile.delete();		    	
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
	
	
}
