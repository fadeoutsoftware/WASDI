package wasdi.processors.drivers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import wasdi.LauncherMain;
import wasdi.processors.DockerProcessorEngine;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class RunPodRuntimeDriver extends ContainerRuntimeDriver {
	public static final String DRIVER_NAME = "runpod";

	private static final URI s_oServerlessUri = URI.create("https://api.runpod.io/v2/serverless");
	private static final String s_sJobsUrl = "https://api.runpod.ai/v2/";
	private static final ObjectMapper s_oMapper = new ObjectMapper();
	private final LocalDockerDriver m_oLocalDockerDriver;

	public RunPodRuntimeDriver(DockerProcessorEngine oEngine) {
		super(oEngine);
		m_oLocalDockerDriver = new LocalDockerDriver(oEngine);
	}

	private boolean publishDeployment(ProcessorParameter oParameter, String sPushedImage) {
		
		if (m_oDriverConfig == null || Utils.isNullOrEmpty(m_oDriverConfig.apiKey) || Utils.isNullOrEmpty(m_oDriverConfig.registryId) || Utils.isNullOrEmpty(m_oDriverConfig.gpuPool)) {
			WasdiLog.errorLog("RunPodRuntimeDriver.publishDeployment: Runpod API key, registry ID and GPU pool are required");
			return false;
		}
		
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(oParameter.getProcessorID());
		if (oProcessor == null) {
			WasdiLog.errorLog("RunPodRuntimeDriver.publishDeployment: processor not found");
			return false;
		}
		
		try {
			if (!Utils.isNullOrEmpty(oProcessor.getRunpodEndpointId())) {
				updateEndpointImage(oProcessor.getRunpodEndpointId(), sPushedImage);
				return true;
			}
			String sEndpointId = createEndpoint("wasdi-" + oProcessor.getProcessorId(), sPushedImage);
			oProcessor.setRunpodEndpointId(sEndpointId);
			if (!oProcessorRepository.updateProcessor(oProcessor)) {
				WasdiLog.errorLog("RunPodRuntimeDriver.publishDeployment: could not persist endpoint " + sEndpointId);
				return false;
			}
			return true;
		} catch (InterruptedException oEx) {
			Thread.currentThread().interrupt();
			WasdiLog.errorLog("RunPodRuntimeDriver.publishDeployment: endpoint request interrupted");
		} catch (IOException oEx) {
			WasdiLog.errorLog("RunPodRuntimeDriver.publishDeployment: " + oEx.getMessage());
		}
		return false;
	}

	private String createEndpoint(String sEndpointName, String sImageName) throws IOException, InterruptedException {
		ObjectMapper oMapper = new ObjectMapper();
		ObjectNode oBody = oMapper.createObjectNode();
		oBody.put("name", sEndpointName);
		oBody.put("image", sImageName);
		oBody.put("registry", m_oDriverConfig.registryId);
		oBody.put("type", "QUEUE");
		oBody.putArray("cmd").add("../venv/bin/python3").add("run_pod_executor.py");
		oBody.putObject("gpu").put("count", 1).putArray("pools").add(m_oDriverConfig.gpuPool);
		oBody.putObject("workers").put("min", 0).put("max", 1).put("idleTimeout", 1);
		oBody.putObject("scaling").put("type", "QUEUE_DELAY").put("queueDelay", 4);

		String sResponse = sendEndpointRequest("POST", s_oServerlessUri, oBody, 201);
		String sEndpointId = oMapper.readTree(sResponse).path("id").asText("");
		if (Utils.isNullOrEmpty(sEndpointId)) {
			throw new IOException("Runpod endpoint creation returned no endpoint ID");
		}
		return sEndpointId;
	}

	private void updateEndpointImage(String sEndpointId, String sImageName) throws IOException, InterruptedException {
		if (!sEndpointId.matches("[a-zA-Z0-9_-]+")) {
			throw new IOException("Invalid Runpod endpoint ID");
		}
		ObjectNode oBody = new ObjectMapper().createObjectNode().put("image", sImageName);
		oBody.putObject("workers").put("min", 0).put("idleTimeout", 1);
		sendEndpointRequest("PATCH", URI.create(s_oServerlessUri + "/" + sEndpointId), oBody, 200);
	}

	private String sendEndpointRequest(String sMethod, URI oUri, ObjectNode oBody, int iExpectedStatus)
			throws IOException, InterruptedException {
		HttpRequest oRequest = HttpRequest.newBuilder(oUri)
				.timeout(Duration.ofSeconds(30))
				.header("Authorization", "Bearer " + m_oDriverConfig.apiKey)
				.header("Content-Type", "application/json")
				.method(sMethod, oBody == null ? HttpRequest.BodyPublishers.noBody()
						: HttpRequest.BodyPublishers.ofString(oBody.toString()))
				.build();

		try (HttpClient oClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()) {
			HttpResponse<String> oResponse = oClient.send(oRequest, HttpResponse.BodyHandlers.ofString());
			if (oResponse.statusCode() != iExpectedStatus) {
				throw new IOException("Runpod endpoint " + sMethod + " failed (HTTP " + oResponse.statusCode() + ")");
			}
			return oResponse.body();
		}
	}

	@Override
	public String buildImage(ProcessorParameter oParameter) {
		return m_oLocalDockerDriver.buildImage(oParameter);
	}

	@Override
	public String pushImage(ProcessorParameter oParameter, String sImageName) {
		
		String sAddress = m_oLocalDockerDriver.pushImage(oParameter, sImageName, true);
		
		return !Utils.isNullOrEmpty(sAddress) && publishDeployment(oParameter, sAddress) ? sAddress : null;
	}

	@Override
	public boolean retainsPreviousImagesOnRedeploy() {
		return true;
	}

	@Override
	public ContainerRuntimeDriver getPackageRefreshDriver() {
		return m_oLocalDockerDriver;
	}

	@Override
	public String run(ProcessorParameter oParameter, String sTag, String sParams) {
		try {
			Processor oProcessor = oParameter.getProcessor();
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			String sProcessId = oParameter.getProcessObjId();
			ProcessWorkspace oProcess = oRepository.getProcessByProcessObjId(sProcessId);
			if (oProcessor == null || oProcess == null || !isValidId(oProcessor.getRunpodEndpointId())) {
				WasdiLog.errorLog("RunPodRuntimeDriver.run: missing processor, process workspace, or Runpod endpoint");
				return null;
			}
			if (ProcessStatus.STOPPED.name().equals(oProcess.getStatus())) return null;
			if (!Utils.isNullOrEmpty(oProcess.getRuntimeReference())) {
				return DRIVER_NAME.equals(oProcess.getRuntimeDriver()) ? oProcess.getRuntimeReference() : null;
			}
			String sEndpointId = oProcessor.getRunpodEndpointId();
			ObjectNode oBody = s_oMapper.createObjectNode();
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
				cancelJob(sReference);
				return null;
			}
			oProcess.setRuntimeDriver(DRIVER_NAME);
			oProcess.setRuntimeReference(sReference);
			if (!oRepository.updateProcess(oProcess)) {
				WasdiLog.errorLog("RunPodRuntimeDriver.run: unable to persist Runpod job " + sReference);
				cancelJob(sReference);
				return null;
			}
			return sReference;
		} catch (InterruptedException oEx) {
			Thread.currentThread().interrupt();
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
		try {
			while (true) {
				ProcessWorkspace oProcess = oRepository.getProcessByProcessObjId(sProcessId);
				if (oProcess == null) return ProcessStatus.ERROR.name();
				if (isTerminal(oProcess.getStatus())) return oProcess.getStatus();
				try {
					ProcessStatus oStatus = readJobStatus(oProcess.getRuntimeReference());
					if (isTerminal(oStatus.name())) {
						return finishJob(oRepository, sProcessId, oStatus);
					}
					iPollFailures = 0;
				} catch (IOException oEx) {
					if (++iPollFailures >= 3) throw oEx;
				}
				Thread.sleep(3000);
			}
		} catch (InterruptedException oEx) {
			Thread.currentThread().interrupt();
		} catch (IOException oEx) {
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
		if (isTerminal(oProcess.getStatus())) return oProcess.getStatus();
		try {
			return readJobStatus(oProcess.getRuntimeReference()).name();
		} catch (InterruptedException oEx) {
			Thread.currentThread().interrupt();
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
		} catch (InterruptedException oEx) {
			Thread.currentThread().interrupt();
		} catch (IOException oEx) {
			WasdiLog.errorLog("RunPodRuntimeDriver.stop: " + oEx.getMessage());
		}
		return false;
	}

	private JsonNode sendJobRequest(String sMethod, String sPath, ObjectNode oBody) throws IOException, InterruptedException {
		if (m_oDriverConfig == null || Utils.isNullOrEmpty(m_oDriverConfig.apiKey)) {
			throw new IOException("Runpod API key is required");
		}
		return s_oMapper.readTree(sendEndpointRequest(sMethod, URI.create(s_sJobsUrl + sPath), oBody, 200));
	}

	private boolean cancelJob(String sReference) throws IOException, InterruptedException {
		return "CANCELLED".equals(sendJobRequest("POST", jobPath(sReference, "cancel"), null).path("status").asText());
	}

	private ProcessStatus readJobStatus(String sReference) throws IOException, InterruptedException {
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

	private static boolean isTerminal(String sStatus) {
		return ProcessStatus.DONE.name().equals(sStatus) || ProcessStatus.ERROR.name().equals(sStatus)
				|| ProcessStatus.STOPPED.name().equals(sStatus);
	}

	private String finishJob(ProcessWorkspaceRepository oRepository, String sProcessId, ProcessStatus oStatus) {
		ProcessWorkspace oProcess = oRepository.getProcessByProcessObjId(sProcessId);
		if (oProcess == null) return ProcessStatus.ERROR.name();
		if (isTerminal(oProcess.getStatus())) return oProcess.getStatus();
		LauncherMain.updateProcessStatus(oRepository, oProcess, oStatus, 100);
		return oStatus.name();
	}

}
