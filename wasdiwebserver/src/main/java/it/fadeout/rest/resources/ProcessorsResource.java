package it.fadeout.rest.resources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
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
import wasdi.shared.business.User;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.parameters.DeployProcessorParameter;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.DeployedProcessorViewModel;
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
			@QueryParam("workspace") String sWorkspaceId, @QueryParam("name") String sName, @QueryParam("version") String sVersion, @QueryParam("description") String sDescription) throws Exception {

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
			int read = 0;
			byte[] bytes = new byte[1024];
			OutputStream out = new FileOutputStream(oProcessorFile);
			while ((read = fileInputStream.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}
			out.flush();
			out.close();
			
			// TODO: Controllare che sia uno zip file e che contenga almeno myProcessor.py
			// Magari guardiamo anche se ha almeno una run
			
			// Create processor entity
			Processor oProcessor = new Processor();
			oProcessor.setName(sName);
			oProcessor.setDescription(sDescription);
			oProcessor.setUserId(sUserId);
			oProcessor.setProcessorId(sProcessorId);
			oProcessor.setVersion(sVersion);
			oProcessor.setPort(-1);
			
			// Store in the db
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			oProcessorRepository.InsertProcessor(oProcessor);
			
			// Schedule the process to deploy the processor
			String sProcessId = "";
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			ProcessWorkspace oProcess = new ProcessWorkspace();
			
			try
			{
				oProcess.setOperationDate(Wasdi.GetFormatDate(new Date()));
				oProcess.setOperationType(LauncherOperations.DEPLOYPROCESSOR.name());
				oProcess.setProductName(sName);
				oProcess.setWorkspaceId(sWorkspaceId);
				oProcess.setUserId(sUserId);
				oProcess.setProcessObjId(Utils.GetRandomName());
				oProcess.setStatus(ProcessStatus.CREATED.name());
				sProcessId = oRepository.InsertProcessWorkspace(oProcess);
				
				String sPath = m_oServletConfig.getInitParameter("SerializationPath");
				if (! (sPath.endsWith("/")||sPath.endsWith("\\"))) sPath+="/";
				sPath += oProcess.getProcessObjId();

				DeployProcessorParameter oParameter = new DeployProcessorParameter();
				oParameter.setName(sName);
				oParameter.setProcessorID(sProcessorId);
				oParameter.setWorkspace(sWorkspaceId);
				oParameter.setUserId(sUserId);
				oParameter.setExchange(sWorkspaceId);
				oParameter.setProcessObjId(oProcess.getProcessObjId());
				
				SerializationUtils.serializeObjectToXML(sPath, oParameter);
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
	public RunningProcessorViewModel run(@HeaderParam("x-session-token") String sSessionId, @QueryParam("name") String sName, @QueryParam("encodedJson") String sEncodedJson) throws Exception {


		RunningProcessorViewModel oRunning = new RunningProcessorViewModel();
		Wasdi.DebugLog("ProcessorsResource.run");
		
		try {
			// Check User 
			if (Utils.isNullOrEmpty(sSessionId)) return oRunning;
			User oUser = Wasdi.GetUserFromSession(sSessionId);

			if (oUser==null) return oRunning;
			if (Utils.isNullOrEmpty(oUser.getUserId())) return oRunning;
			
			String sUserId = oUser.getUserId();
			String sWorkspaceId = "";
		
			Wasdi.DebugLog("ProcessorsResource.run: get Processor");
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessorToRun = oProcessorRepository.GetProcessorByName(sName);

			// Schedule the process to run the processor
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			ProcessWorkspace oProcess = new ProcessWorkspace();
			
			try
			{
				Wasdi.DebugLog("ProcessorsResource.run: create task"); 
				oProcess.setOperationDate(Wasdi.GetFormatDate(new Date()));
				oProcess.setOperationType(LauncherOperations.RUNPROCESSOR.name());
				oProcess.setProductName(sName);
				oProcess.setWorkspaceId(sWorkspaceId);
				oProcess.setUserId(sUserId);
				oProcess.setProcessObjId(Utils.GetRandomName());
				oProcess.setStatus(ProcessStatus.CREATED.name());
				oRepository.InsertProcessWorkspace(oProcess);
				
				String sPath = m_oServletConfig.getInitParameter("SerializationPath");
				if (! (sPath.endsWith("/")||sPath.endsWith("\\"))) sPath+="/";
				sPath += oProcess.getProcessObjId();

				DeployProcessorParameter oParameter = new DeployProcessorParameter();
				oParameter.setName(sName);
				oParameter.setProcessorID(oProcessorToRun.getProcessorId());
				oParameter.setWorkspace(sWorkspaceId);
				oParameter.setUserId(sUserId);
				oParameter.setExchange(sWorkspaceId);
				oParameter.setProcessObjId(oProcess.getProcessObjId());
				oParameter.setJson(sEncodedJson);
				
				SerializationUtils.serializeObjectToXML(sPath, oParameter);
				
				oRunning.setJsonEncodedResult("");
				oRunning.setName(sName);
				oRunning.setProcessingIdentifier(oProcess.getProcessObjId());
				oRunning.setProcessorId(oProcessorToRun.getProcessorId());
				oRunning.setStatus("CREATED");
				Wasdi.DebugLog("ProcessorsResource.run: done"); 
			}
			catch(Exception oEx){
				System.out.println("ProcessorsResource.run: Error scheduling the deploy process " + oEx.getMessage());
				oEx.printStackTrace();
				oRunning.setStatus(ProcessStatus.ERROR.toString());
				return oRunning;
			}
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			oRunning.setStatus(ProcessStatus.ERROR.toString());
			return oRunning;
		}
		
		return oRunning;
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
