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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataParam;

import it.fadeout.Wasdi;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Processor;
import wasdi.shared.business.ProcessorTypes;
import wasdi.shared.business.User;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.parameters.DeployProcessorParameter;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.DeployedProcessorViewModel;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.RunningProcessorViewModel;

@Path("/processors")
public class ProcessorsResource {
	
	@Context
	ServletConfig m_oServletConfig;
	
	/**
	 * Upload a new processor in Wasdi
	 * @param fileInputStream
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
	public Response uploadProcessor(@FormDataParam("file") InputStream fileInputStream, @HeaderParam("x-session-token") String sSessionId, 
			@QueryParam("workspace") String sWorkspaceId, @QueryParam("name") String sName, @QueryParam("version") String sVersion, @QueryParam("description") String sDescription, @QueryParam("type") String sType) throws Exception {

		Wasdi.DebugLog("ProcessorsResource.uploadProcessor");
		
		try {
			// Check User 
			if (Utils.isNullOrEmpty(sSessionId)) return Response.status(401).build();
			User oUser = Wasdi.GetUserFromSession(sSessionId);

			if (oUser==null) return Response.status(401).build();
			if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(401).build();
			
			// Get the user Id
			String sUserId = oUser.getUserId();
			
			// Set the processor path
			String sDownloadRootPath = m_oServletConfig.getInitParameter("DownloadRootPath");
			if (!sDownloadRootPath.endsWith("/")) sDownloadRootPath = sDownloadRootPath + "/";
			
			File oProcessorPath = new File(sDownloadRootPath+ "/processors/" + sName);
			
			// Create folders
			// TODO: Se esiste in effetti non va bene
			if (!oProcessorPath.exists()) {
				oProcessorPath.mkdirs();
			}
			
			// Create file
			String sProcessorId =  UUID.randomUUID().toString();
			File oProcessorFile = new File(sDownloadRootPath+"/processors/" + sName + "/" + sProcessorId + ".zip");
			
			Wasdi.DebugLog("ProcessorsResource.uploadProcessor: Processor file Path: " + oProcessorFile.getPath());
			
			//save uploaded file
			int iRead = 0;
			byte[] ayBytes = new byte[1024];
			OutputStream oOutputStream = new FileOutputStream(oProcessorFile);
			while ((iRead = fileInputStream.read(ayBytes)) != -1) {
				oOutputStream.write(ayBytes, 0, iRead);
			}
			oOutputStream.flush();
			oOutputStream.close();
			
			// TODO: Controllare che sia uno zip file e che contenga almeno myProcessor.py
			// Magari guardiamo anche se ha almeno una run
			
			
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
			
			// Store in the db
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			oProcessorRepository.InsertProcessor(oProcessor);
			
			// Schedule the processworkspace to deploy the processor
			String sProcessId = "";
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			ProcessWorkspace oProcessWorkspace = new ProcessWorkspace();
			
			try
			{
				oProcessWorkspace.setOperationDate(Wasdi.GetFormatDate(new Date()));
				oProcessWorkspace.setOperationType(LauncherOperations.DEPLOYPROCESSOR.name());
				oProcessWorkspace.setProductName(sName);
				oProcessWorkspace.setWorkspaceId(sWorkspaceId);
				oProcessWorkspace.setUserId(sUserId);
				oProcessWorkspace.setProcessObjId(Utils.GetRandomName());
				oProcessWorkspace.setStatus(ProcessStatus.CREATED.name());
				sProcessId = oRepository.InsertProcessWorkspace(oProcessWorkspace);
				
				String sPath = m_oServletConfig.getInitParameter("SerializationPath");
				if (! (sPath.endsWith("/")||sPath.endsWith("\\"))) sPath+="/";
				sPath += oProcessWorkspace.getProcessObjId();

				DeployProcessorParameter oDeployProcessorParameter = new DeployProcessorParameter();
				oDeployProcessorParameter.setName(sName);
				oDeployProcessorParameter.setProcessorID(sProcessorId);
				oDeployProcessorParameter.setWorkspace(sWorkspaceId);
				oDeployProcessorParameter.setUserId(sUserId);
				oDeployProcessorParameter.setExchange(sWorkspaceId);
				oDeployProcessorParameter.setProcessObjId(oProcessWorkspace.getProcessObjId());
				oDeployProcessorParameter.setProcessorType(sType);
				
				//TODO move it before inserting the new process into DB
				SerializationUtils.serializeObjectToXML(sPath, oDeployProcessorParameter);
			}
			catch(Exception oEx){
				System.out.println("ProcessorsResource.uploadProcessor: Error scheduling the deploy process " + oEx.getMessage());
				oEx.printStackTrace();
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
			}


		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return Response.serverError().build();
		}
				
		return Response.ok().build();
	}
	
	@GET
	@Path("/getdeployed")
	public List<DeployedProcessorViewModel> getDeployedProcessors(@HeaderParam("x-session-token") String sSessionId) throws Exception {

		ArrayList<DeployedProcessorViewModel> aoRet = new ArrayList<>(); 
		Wasdi.DebugLog("ProcessorsResource.getDeployedProcessors");
		
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
				
				aoRet.add(oVM);
			}
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return aoRet;
		}		
		return aoRet;
	}
	
	
	@GET
	@Path("/run")
	public RunningProcessorViewModel run(@HeaderParam("x-session-token") String sSessionId, @QueryParam("name") String sName, @QueryParam("encodedJson") String sEncodedJson, @QueryParam("workspace") String sWorkspaceId) throws Exception {


		RunningProcessorViewModel oRunningProcessorViewModel = new RunningProcessorViewModel();
		Wasdi.DebugLog("ProcessorsResource.run");
		
		try {
			// Check User 
			if (Utils.isNullOrEmpty(sSessionId)) return oRunningProcessorViewModel;
			User oUser = Wasdi.GetUserFromSession(sSessionId);

			if (oUser==null) return oRunningProcessorViewModel;
			if (Utils.isNullOrEmpty(oUser.getUserId())) return oRunningProcessorViewModel;
			
			String sUserId = oUser.getUserId();
		
			Wasdi.DebugLog("ProcessorsResource.run: get Processor");
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessorToRun = oProcessorRepository.GetProcessorByName(sName);

			// Schedule the process to run the processor
			ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			ProcessWorkspace oProcessWorkspace = new ProcessWorkspace();
			
			try
			{
				Wasdi.DebugLog("ProcessorsResource.run: create task"); 
				oProcessWorkspace.setOperationDate(Wasdi.GetFormatDate(new Date()));
				oProcessWorkspace.setOperationType(LauncherOperations.RUNPROCESSOR.name());
				oProcessWorkspace.setProductName(sName);
				oProcessWorkspace.setWorkspaceId(sWorkspaceId);
				oProcessWorkspace.setUserId(sUserId);
				oProcessWorkspace.setProcessObjId(Utils.GetRandomName());
				oProcessWorkspace.setStatus(ProcessStatus.CREATED.name());
				oProcessWorkspaceRepository.InsertProcessWorkspace(oProcessWorkspace);
				
				String sPath = m_oServletConfig.getInitParameter("SerializationPath");
				if (! (sPath.endsWith("/")||sPath.endsWith("\\"))) sPath+="/";
				sPath += oProcessWorkspace.getProcessObjId();

				DeployProcessorParameter oDeployProcessorParameter = new DeployProcessorParameter();
				oDeployProcessorParameter.setName(sName);
				oDeployProcessorParameter.setProcessorID(oProcessorToRun.getProcessorId());
				oDeployProcessorParameter.setWorkspace(sWorkspaceId);
				oDeployProcessorParameter.setUserId(sUserId);
				oDeployProcessorParameter.setExchange(sWorkspaceId);
				oDeployProcessorParameter.setProcessObjId(oProcessWorkspace.getProcessObjId());
				oDeployProcessorParameter.setJson(sEncodedJson);
				oDeployProcessorParameter.setProcessorType(oProcessorToRun.getType());
				
				//TODO move it before inserting the new process into DB
				SerializationUtils.serializeObjectToXML(sPath, oDeployProcessorParameter);
				
				oRunningProcessorViewModel.setJsonEncodedResult("");
				oRunningProcessorViewModel.setName(sName);
				oRunningProcessorViewModel.setProcessingIdentifier(oProcessWorkspace.getProcessObjId());
				oRunningProcessorViewModel.setProcessorId(oProcessorToRun.getProcessorId());
				oRunningProcessorViewModel.setStatus("CREATED");
				Wasdi.DebugLog("ProcessorsResource.run: done"); 
			}
			catch(Exception oEx){
				System.out.println("ProcessorsResource.run: Error scheduling the run process " + oEx.getMessage());
				oEx.printStackTrace();
				oRunningProcessorViewModel.setStatus(ProcessStatus.ERROR.toString());
				return oRunningProcessorViewModel;
			}
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			oRunningProcessorViewModel.setStatus(ProcessStatus.ERROR.toString());
			return oRunningProcessorViewModel;
		}
		
		return oRunningProcessorViewModel;
	}
	
	
	@GET
	@Path("/help")
	public PrimitiveResult help(@HeaderParam("x-session-token") String sSessionId, @QueryParam("name") String sName) throws Exception {

		PrimitiveResult oPrimitiveResult = new PrimitiveResult();
		oPrimitiveResult.setBoolValue(false);
		
		Wasdi.DebugLog("ProcessorsResource.help");
		
		try {
			// Check User 
			if (Utils.isNullOrEmpty(sSessionId)) return oPrimitiveResult;
			User oUser = Wasdi.GetUserFromSession(sSessionId);

			if (oUser==null) return oPrimitiveResult;
			if (Utils.isNullOrEmpty(oUser.getUserId())) return oPrimitiveResult;
			
			String sUserId = oUser.getUserId();
		
			Wasdi.DebugLog("ProcessorsResource.help: read Processor " +sName);
			
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessorToRun = oProcessorRepository.GetProcessorByName(sName);
			
			// Call localhost:port
			String sUrl = "http://localhost:"+oProcessorToRun.getPort()+"/run/--help";
			
			Wasdi.DebugLog("ProcessorsResource.help: calling URL = " + sUrl);
			
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
			Wasdi.DebugLog("ProcessorsResource.help: Output from Server .... \n");
			while ((sOutputResult = oBufferedReader.readLine()) != null) {
				Wasdi.DebugLog("ProcessorsResource.help: " + sOutputResult);
				
				if (!Utils.isNullOrEmpty(sOutputResult)) sOutputCumulativeResult += sOutputResult;
			}

			oConnection.disconnect();
			
			oPrimitiveResult.setBoolValue(true);
			oPrimitiveResult.setStringValue(sOutputCumulativeResult);
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return oPrimitiveResult;
		}
		
		return oPrimitiveResult;
	}
	
	@GET
	@Path("/status")
	public RunningProcessorViewModel status(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processingId") String sProcessingId) throws Exception {

		RunningProcessorViewModel oRunning = new RunningProcessorViewModel();
		Wasdi.DebugLog("ProcessorsResource.status");
		oRunning.setStatus(ProcessStatus.ERROR.toString());
		
		try {
			// Check User 
			if (Utils.isNullOrEmpty(sSessionId)) return oRunning;
			User oUser = Wasdi.GetUserFromSession(sSessionId);

			if (oUser==null) return oRunning;
			if (Utils.isNullOrEmpty(oUser.getUserId())) return oRunning;
			
			String sUserId = oUser.getUserId();
			//String sWorkspaceId = "";
		
			Wasdi.DebugLog("ProcessorsResource.status: get Running Processor " + sProcessingId);
			
			// Get Process-Workspace
			ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			ProcessWorkspace oProcessWorkspace = oProcessWorkspaceRepository.GetProcessByProcessObjId(sProcessingId);
			
			// Check not null
			if (oProcessWorkspace == null) {
				Wasdi.DebugLog("ProcessorsResource.status: impossible to find " + sProcessingId);
				return oRunning;
			}
			
			// Check if it is the right user
			if (oProcessWorkspace.getUserId().equals(sUserId) == false) {
				Wasdi.DebugLog("ProcessorsResource.status: processing not of this user");
				return oRunning;				
			}
			
			// Check if it is a processor action
			if (!(oProcessWorkspace.getOperationType().equals(LauncherOperations.DEPLOYPROCESSOR.toString()) || oProcessWorkspace.getOperationType().equals(LauncherOperations.RUNPROCESSOR.toString())) ) {
				Wasdi.DebugLog("ProcessorsResource.status: not a running process ");
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
			System.out.println("ProcessorsResource.run: Error scheduling the deploy process " + oEx.getMessage());
			oEx.printStackTrace();
			oRunning.setStatus(ProcessStatus.ERROR.toString());
		}
		
		return oRunning;
	}
}
