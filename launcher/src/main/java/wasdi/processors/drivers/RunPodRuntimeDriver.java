package wasdi.processors.drivers;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import wasdi.LauncherMain;
import wasdi.processors.DockerProcessorEngine;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.mongo.MongoRepository;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.LauncherOperationsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;

public class RunPodRuntimeDriver extends ContainerRuntimeDriver {
	public static final String DRIVER_NAME = "runpod";

	private static final URI s_oServerlessUri = URI.create("https://api.runpod.io/v2/serverless");
	private static final String s_sJobsUrl = "https://api.runpod.ai/v2/";
	
	private final LocalDockerDriver m_oLocalDockerDriver;

	public RunPodRuntimeDriver(DockerProcessorEngine oEngine) {
		super(oEngine);
		m_oLocalDockerDriver = new LocalDockerDriver(oEngine);
	}

	/**
	 * Creates or updates the Serverless endpoint in Run Pod, after a deploy or a re-deploy
	 * @param oParameter
	 * @param sPushedImage
	 * @return
	 */
	private boolean publishDeployment(ProcessorParameter oParameter, String sPushedImage) {
		
		if (m_oDriverConfig == null || Utils.isNullOrEmpty(m_oDriverConfig.apiKey) || Utils.isNullOrEmpty(m_oDriverConfig.registryId) || Utils.isNullOrEmpty(m_oDriverConfig.gpuPool)) {
			WasdiLog.errorLog("RunPodRuntimeDriver.publishDeployment: Runpod API key, registry ID and GPU pool are required");
			return false;
		}
		
		// Get the processor
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(oParameter.getProcessorID());
		if (oProcessor == null) {
			WasdiLog.errorLog("RunPodRuntimeDriver.publishDeployment: processor not found");
			return false;
		}
		
		try {
			// Check if this is a create or an update
			if (!Utils.isNullOrEmpty(oProcessor.getRunpodEndpointId())) {
				WasdiLog.debugLog("RunPodRuntimeDriver.publishDeployment: end point exists, we update it");
				
				processWorkspaceLog("Application will be executed in RunPod: updating the endpoint");
				
				// Update end-point
				updateEndpointImage(oProcessor.getRunpodEndpointId(), sPushedImage);
				
				processWorkspaceLog("Run pod endpoint updated");
				
				return true;
			}
			
			WasdiLog.debugLog("RunPodRuntimeDriver.publishDeployment: end point does not exists. we create it");
			
			
			processWorkspaceLog("Application will be executed in RunPod: creating the end point image");
			
			// Create end point
			String sEndpointId = createEndpoint("wasdi-" + oProcessor.getProcessorId(), sPushedImage);
			oProcessor.setRunpodEndpointId(sEndpointId);
			
			// Set the endpoint to the processor
			if (!oProcessorRepository.updateProcessor(oProcessor)) {
				WasdiLog.errorLog("RunPodRuntimeDriver.publishDeployment: could not persist endpoint " + sEndpointId);
				return false;
			}
			
			processWorkspaceLog("Run pod endpoint created");
			
			return true;
		} catch (IOException oEx) {
			WasdiLog.errorLog("RunPodRuntimeDriver.publishDeployment: " + oEx.getMessage());
		}
		
		return false;
	}
	
	/**
	 * Creates a Serverless run pod endpoint
	 * @param sEndpointName
	 * @param sImageName
	 * @return
	 * @throws IOException
	 */
	private String createEndpoint(String sEndpointName, String sImageName) throws IOException {
		
		// Create the Body as Json
		ObjectNode oBody = MongoRepository.s_oMapper.createObjectNode();
		
		oBody.put("name", sEndpointName);
		oBody.put("image", sImageName);
		oBody.put("registry", m_oDriverConfig.registryId);
		oBody.put("type", "QUEUE");
		oBody.putArray("cmd").add("../venv/bin/python3").add("run_pod_executor.py");
		oBody.putObject("gpu").put("count", 1).putArray("pools").add(m_oDriverConfig.gpuPool);
		oBody.putObject("workers").put("min", 0).put("max", 1).put("idleTimeout", 1);
		oBody.putObject("scaling").put("type", "QUEUE_DELAY").put("queueDelay", 4);
		
		// Send the POST
		String sResponse = sendEndpointRequest("POST", s_oServerlessUri, oBody, 201);
		
		// Get The response
		String sEndpointId = MongoRepository.s_oMapper.readTree(sResponse).path("id").asText("");
		
		if (Utils.isNullOrEmpty(sEndpointId)) {
			throw new IOException("Runpod endpoint creation returned no endpoint ID");
		}
		
		return sEndpointId;
	}

	/**
	 * Updates the serverless runpod Endpoint
	 * @param sEndpointId
	 * @param sImageName
	 * @throws IOException
	 */
	private void updateEndpointImage(String sEndpointId, String sImageName) throws IOException {
		
		// We need a valid end point
		if (!isValidId(sEndpointId)) {
			throw new IOException("Invalid Runpod endpoint ID");
		}
		
		// Create tje body to update the image, and fix workers and idle timeout
		ObjectNode oBody = MongoRepository.s_oMapper.createObjectNode().put("image", sImageName);
		oBody.putObject("workers").put("min", 0).put("idleTimeout", 1);
		
		// Send the PATCH request
		sendEndpointRequest("PATCH", URI.create(s_oServerlessUri + "/" + sEndpointId), oBody, 200);
	}

	/**
	 * Wrapper to send Http requests and check results
	 * @param sMethod
	 * @param oUri
	 * @param oBody
	 * @param iExpectedStatus
	 * @return
	 * @throws IOException
	 */
	private String sendEndpointRequest(String sMethod, URI oUri, ObjectNode oBody, int iExpectedStatus) throws IOException {
		
		// We add the headers
		Map<String, String> asHeaders = new HashMap<>();
		asHeaders.put("Authorization", "Bearer " + m_oDriverConfig.apiKey);
		asHeaders.put("Content-Type", "application/json");
		
		// We read the body
		String sPayload = oBody == null ? "" : oBody.toString();
		
		HttpCallResponse oResponse;
		
		// Call the right method
		switch (sMethod) {
			case "GET" -> oResponse = HttpUtils.httpGet(oUri.toString(), asHeaders);
			case "POST" -> oResponse = HttpUtils.httpPost(oUri.toString(), sPayload, asHeaders);
			case "PATCH" -> oResponse = HttpUtils.httpPatch(oUri.toString(), sPayload, asHeaders);
			default -> throw new IOException("Unsupported Runpod HTTP method: " + sMethod);
		}
		
		// Is the answer we wanted?
		if (oResponse.getResponseCode() == null || oResponse.getResponseCode() != iExpectedStatus) {
			
			// No, log the error
			String sError = "Runpod " + sMethod + " " + oUri + " failed (HTTP " + oResponse.getResponseCode() + "): " + oResponse.getResponseBody();
			WasdiLog.errorLog("RunPodRuntimeDriver.sendEndpointRequest: " + sError);
			throw new IOException(sError);
		}
		
		// Yes, return the response
		return oResponse.getResponseBody();
	}

	@Override
	public String buildImage(ProcessorParameter oParameter) {
		// Build are done locally
		return m_oLocalDockerDriver.buildImage(oParameter);
	}

	@Override
	public String pushImage(ProcessorParameter oParameter, String sImageName) {
		// Push the image in the WASDI registers
		String sAddress = m_oLocalDockerDriver.pushImage(oParameter, sImageName, true);
		
		if (Utils.isNullOrEmpty(sAddress)) {
			WasdiLog.errorLog("RunPodRuntimeDriver.pushImage: Impossible to get the address of the pushed image");
			return null;
		}
		
		if (publishDeployment(oParameter, sAddress)) {
			return sAddress;
		}
		else {
			WasdiLog.errorLog("RunPodRuntimeDriver.pushImage: publishDeployment returned false, runpod not updated");
			return null;
		}
	}

	@Override
	public boolean retainsPreviousImagesOnRedeploy() {
		return true;
	}

	@Override
	public ContainerRuntimeDriver getPackageRefreshDriver() {
		// To refresh package, we rely on internal docker
		return m_oLocalDockerDriver;
	}

	@Override
	public String run(ProcessorParameter oParameter, String sTag, String sParams) {
		try {
			// Get the processor
			Processor oProcessor = oParameter.getProcessor();
			
			// And get the Process Workspace
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			String sProcessId = oParameter.getProcessObjId();
			ProcessWorkspace oProcess = oRepository.getProcessByProcessObjId(sProcessId);
			
			if (oProcessor == null) {
				WasdiLog.errorLog("RunPodRuntimeDriver.run: missing processor");
				return null;
			}

			if (oProcess == null) {
				WasdiLog.errorLog("RunPodRuntimeDriver.run: missing process workspace");
				return null;
			}
			
			// Do we already have the runpod endpoint?
			if (Utils.isNullOrEmpty(oProcessor.getRunpodEndpointId())) {
				
				// Not yet! Get the registry address
				String sDockerRegistry = ((DockerProcessorEngine) m_oEngine).getDockerRegistry();
				
				if (Utils.isNullOrEmpty(sDockerRegistry)) {
					WasdiLog.errorLog("RunPodRuntimeDriver.run: cannot recover the endpoint without the main Docker registry");
					return null;
				}
				
				// Create the image name
				String sImageName = sDockerRegistry + "/wasdi/" + oProcessor.getName() + ":" + oProcessor.getVersion();
				
				// Create the endpoint
				if (!publishDeployment(oParameter, sImageName)) {
					WasdiLog.errorLog("RunPodRuntimeDriver.run: failed to create the missing Runpod endpoint");
					return null;
				}
				
				// Re-read the processor: we should have the end point id now
				oProcessor = new ProcessorRepository().getProcessor(oParameter.getProcessorID());
			}
			
			
			if (oProcessor == null || !isValidId(oProcessor.getRunpodEndpointId())) {
				WasdiLog.errorLog("RunPodRuntimeDriver.run: missing or invalid Runpod endpoint");
				return null;
			}
			
			if (ProcessStatus.STOPPED.name().equals(oProcess.getStatus())) {
				processWorkspaceLog("Application is already stopped, we exit here");
				return null;
			}
			
			// Do we already have the runtime reference?			
			if (!Utils.isNullOrEmpty(oProcess.getRuntimeReference())) {
				processWorkspaceLog("Application was not executed on RunPod, something went wrong..");
				return DRIVER_NAME.equals(oProcess.getRuntimeDriver()) ? oProcess.getRuntimeReference() : null;
			}
			
			processWorkspaceLog("Waiting for a RunPod worker");
			
			String sEndpointId = oProcessor.getRunpodEndpointId();
			
			ObjectNode oBody = MongoRepository.s_oMapper.createObjectNode();
			oBody.putObject("input")
					.put("WASDI_PROCESS_WORKSPACE_ID", sProcessId)
					.put("WASDI_USER", oParameter.getUserId())
					.put("WASDI_SESSION_ID", oParameter.getSessionID())
					.put("WASDI_WORKSPACE_ID", oParameter.getWorkspace())
					.put("WASDI_WEBSERVER_URL", WasdiConfig.Current.baseUrl)
					.put(sParams != null && !sParams.stripLeading().startsWith("{") ? "encodedParams" : "params", sParams);
			
			long lTimeoutMs = oProcessor.getTimeoutMs();
			
			if (lTimeoutMs > 0) {
				oBody.putObject("policy").put("executionTimeout", Math.max(5000, Math.min(lTimeoutMs, 604800000)));
			}
			
			String sJobId = sendJobRequest("POST", sEndpointId + "/run", oBody).path("id").asText("");
			
			if (!isValidId(sJobId)) {
				throw new IOException("Runpod submission returned no valid job ID");
			}
			
			String sReference = sEndpointId + "/" + sJobId;
			
			oProcess = oRepository.getProcessByProcessObjId(sProcessId);
			
			if (oProcess == null || ProcessStatus.STOPPED.name().equals(oProcess.getStatus())) {
				processWorkspaceLog("The process looks already stopped. We exit");
				cancelJob(sReference);
				return null;
			}
			oProcess.setRuntimeDriver(DRIVER_NAME);
			oProcess.setRuntimeReference(sReference);
			
			if (!oRepository.updateProcess(oProcess)) {
				processWorkspaceLog("Error persisting RunPod Job");
				WasdiLog.errorLog("RunPodRuntimeDriver.run: unable to persist Runpod job " + sReference);
				cancelJob(sReference);
				return null;
			}
			
			processWorkspaceLog("RunPod: the worker is available, waiting for the instance to be up");
			
			return sReference;
		} catch (IOException oEx) {
			WasdiLog.errorLog("RunPodRuntimeDriver.run: " + oEx.getMessage());
		}
		return null;
	}

	@Override
	public String waitForCompletion(ProcessorParameter oParameter) {
		ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
		String sProcessId = oParameter.getProcessObjId();
		
		int iPollFailures = 0;
		
		long iThreadSleepMs = 2000;

		try {
			long iThreadSleep = Long.parseLong(WasdiConfig.Current.scheduler.processingThreadSleepingTimeMS);
			if (iThreadSleep > 0) {
				iThreadSleepMs = iThreadSleep;
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("LocalDockerDriver.waitForCompletion: Could not read processingThreadSleepingTimeMS config: " + oEx);
		}		
		
		
		try {
			while (true) {
				
				// Refresh the process workspace
				ProcessWorkspace oProcess = oRepository.getProcessByProcessObjId(sProcessId);
				// Should not be null...
				if (oProcess == null) return ProcessStatus.ERROR.name();
				
				// If it is an end state we can exit
				if (LauncherOperationsUtils.isTerminalStatus(oProcess.getStatus())) return oProcess.getStatus();
				
				try {
					// Read the job status from run pod
					ProcessStatus oStatus = readJobStatus(oProcess.getRuntimeReference());
					
					// Check again if it is a terminal one
					if (LauncherOperationsUtils.isTerminalStatus(oStatus.name())) {
						return finishJob(oRepository, sProcessId, oStatus);
					}
					
					// Reset the failure count
					iPollFailures = 0;
				} 
				catch (IOException oEx) {
					// Increment failure count
					if (++iPollFailures >= 3) throw oEx;
				}
				
				// Take a nap
				Thread.sleep(iThreadSleepMs);
			}
		} catch (InterruptedException oEx) {
			Thread.currentThread().interrupt();
		} catch (IOException oEx) {
			WasdiLog.errorLog("RunPodRuntimeDriver.waitForCompletion: " + oEx.getMessage());
		} catch (Exception oEx) {
			WasdiLog.errorLog("RunPodRuntimeDriver.waitForCompletion: " + oEx.getMessage());
		}		
		
		return finishJob(oRepository, sProcessId, ProcessStatus.ERROR);
	}

	@Override
	public String getStatus(String sProcessWorkspaceId) {
		
		ProcessWorkspace oProcess = new ProcessWorkspaceRepository().getProcessByProcessObjId(sProcessWorkspaceId);
		
		if (oProcess == null || !DRIVER_NAME.equals(oProcess.getRuntimeDriver())) {
			return m_oLocalDockerDriver.getStatus(sProcessWorkspaceId);
		}
		
		if (LauncherOperationsUtils.isTerminalStatus(oProcess.getStatus())) return oProcess.getStatus();
		
		try {
			return readJobStatus(oProcess.getRuntimeReference()).name();
		} catch (IOException oEx) {
			WasdiLog.errorLog("RunPodRuntimeDriver.getStatus: " + oEx.getMessage());
		}
		return ProcessStatus.ERROR.name();
	}

	@Override
	public boolean stop(String sProcessWorkspaceId) {
		ProcessWorkspace oProcess = new ProcessWorkspaceRepository().getProcessByProcessObjId(sProcessWorkspaceId);
		if (oProcess == null || !DRIVER_NAME.equals(oProcess.getRuntimeDriver())) {
			return m_oLocalDockerDriver.stop(sProcessWorkspaceId);
		}
		try {
			return cancelJob(oProcess.getRuntimeReference());
		} catch (IOException oEx) {
			WasdiLog.errorLog("RunPodRuntimeDriver.stop: " + oEx.getMessage());
		}
		return false;
	}

	private JsonNode sendJobRequest(String sMethod, String sPath, ObjectNode oBody) throws IOException {
		if (m_oDriverConfig == null || Utils.isNullOrEmpty(m_oDriverConfig.apiKey)) {
			throw new IOException("Runpod API key is required");
		}
		
		String sResponse = sendEndpointRequest(sMethod, URI.create(s_sJobsUrl + sPath), oBody, 200);
		
		if (!Utils.isNullOrEmpty(sResponse)) {
			return MongoRepository.s_oMapper.readTree(sResponse);	
		}
		else {
			return null;
		}
	}

	private boolean cancelJob(String sReference) throws IOException {
		return "CANCELLED".equals(sendJobRequest("POST", jobPath(sReference, "cancel"), null).path("status").asText());
	}

	private ProcessStatus readJobStatus(String sReference) throws IOException {
		String sStatus = sendJobRequest("GET", jobPath(sReference, "status"), null).path("status").asText();
		return switch (sStatus) {
			case "IN_QUEUE", "IN_PROGRESS" -> ProcessStatus.RUNNING;
			case "COMPLETED" -> ProcessStatus.DONE;
			case "CANCELLED", "TIMED_OUT" -> ProcessStatus.STOPPED;
			case "FAILED", "ERROR" -> ProcessStatus.ERROR;
			default -> throw new IOException("Runpod returned an unknown job status");
		};
	}

	private static String jobPath(String sReference, String sOperation) throws IOException {
		String[] asIds = sReference == null ? new String[0] : sReference.split("/", -1);
		if (asIds.length != 2 || !isValidId(asIds[0]) || !isValidId(asIds[1])) {
			throw new IOException("Invalid Runpod job reference");
		}
		return asIds[0] + "/" + sOperation + "/" + asIds[1];
	}

	private static boolean isValidId(String sId) {
		return sId != null && sId.matches("[a-zA-Z0-9_-]+");
	}


	private String finishJob(ProcessWorkspaceRepository oRepository, String sProcessId, ProcessStatus oStatus) {
		ProcessWorkspace oProcess = oRepository.getProcessByProcessObjId(sProcessId);
		if (oProcess == null) return ProcessStatus.ERROR.name();
		if (LauncherOperationsUtils.isTerminalStatus(oProcess.getStatus())) return oProcess.getStatus();
		LauncherMain.updateProcessStatus(oRepository, oProcess, oStatus, 100);
		return oStatus.name();
	}

}
