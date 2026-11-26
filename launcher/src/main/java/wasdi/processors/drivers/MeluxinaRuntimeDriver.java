package wasdi.processors.drivers;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import wasdi.LauncherMain;
import wasdi.processors.DockerProcessorEngine;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.mongo.MongoRepository;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.LauncherOperationsUtils;
import wasdi.shared.utils.StringUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;

public class MeluxinaRuntimeDriver extends ContainerRuntimeDriver {
	public static final String DRIVER_NAME = "meluxina";

	private final LocalDockerDriver m_oLocalDockerDriver;

	public MeluxinaRuntimeDriver(DockerProcessorEngine oEngine) {
		super(oEngine);
		m_oLocalDockerDriver = new LocalDockerDriver(oEngine);
	}

	@Override
	public String buildImage(ProcessorParameter oParameter) {
		return m_oLocalDockerDriver.buildImage(oParameter);
	}

	@Override
	public String pushImage(ProcessorParameter oParameter, String sImageName) {
		return m_oLocalDockerDriver.pushImage(oParameter, sImageName, true);
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
		
		ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
		String sProcessId = oParameter.getProcessObjId();
		
		ProcessWorkspace oProcess = oRepository.getProcessByProcessObjId(sProcessId);
		Processor oProcessor = oParameter.getProcessor();
		
		if (oProcess == null || oProcessor == null || LauncherOperationsUtils.isTerminalStatus(oProcess.getStatus())) {
			return null;
		}
		
		if (!Utils.isNullOrEmpty(oProcess.getRuntimeReference())) {
			return DRIVER_NAME.equals(oProcess.getRuntimeDriver()) ? oProcess.getRuntimeReference() : null;
		}

		try {
			String sAccount = getOption("account", "");
			String sQos = getOption("qos", "");
			if (m_oDriverConfig == null || Utils.isNullOrEmpty(sAccount)
					|| Utils.isNullOrEmpty(sQos)
					|| Utils.isNullOrEmpty(m_oDriverConfig.workingDirectory)
					|| Utils.isNullOrEmpty(m_oDriverConfig.startScript)) {
				throw new IOException("MeluXina account, QoS, working directory and batch script are required");
			}
			String sRegistry = ((DockerProcessorEngine) m_oEngine).getDockerRegistry();
			
			if (Utils.isNullOrEmpty(sRegistry)) {
				throw new IOException("MeluXina requires a main Docker registry");
			}
			
			String sImage = sRegistry + "/wasdi/" + oProcessor.getName() + ":" + oProcessor.getVersion();
			
			String sEncodedParams = Utils.isNullOrEmpty(sParams) ? "%7B%7D" : sParams;
			
			if (sEncodedParams.stripLeading().startsWith("{")) {
				sEncodedParams = StringUtils.encodeUrl(sEncodedParams).replace("+", "%20");
			}
			
			long lTimeoutMs = oProcessor.getTimeoutMs();
			
			if (lTimeoutMs == -1) {
				WasdiLog.warnLog("MeluxinaRuntimeDriver.run: MeluXina requires a positive processor timeout, set one day");
				lTimeoutMs = 24*60*60*60*1000;
			}
			
			long lTimeoutMinutes = 1 + (lTimeoutMs - 1) / 60000;
			ObjectNode oBody = MongoRepository.s_oMapper.createObjectNode();
			oBody.put("script", m_oDriverConfig.startScript);
			ObjectNode oJob = oBody.putObject("job");
			oJob.put("name", "wasdi-" + sProcessId);
			oJob.put("account", sAccount);
			oJob.put("qos", sQos);
			oJob.put("current_working_directory", m_oDriverConfig.workingDirectory);
			oJob.put("time_limit", lTimeoutMinutes);
			String sPartition = getOption("partition", "");
			if (!Utils.isNullOrEmpty(sPartition)) {
				oJob.put("partition", sPartition);
			}
			String sTresPerNode = getOption("tresPerNode", "");
			if (!Utils.isNullOrEmpty(sTresPerNode)) {
				oJob.put("tres_per_node", sTresPerNode);
			}
			ArrayNode asEnvironment = oJob.putArray("environment");
			asEnvironment.add("WASDI_IMAGE=" + sImage);
			asEnvironment.add("WASDI_PROCESS_WORKSPACE_ID=" + sProcessId);
			asEnvironment.add("WASDI_USER=" + oParameter.getUserId());
			asEnvironment.add("WASDI_SESSION_ID=" + oParameter.getSessionID());
			asEnvironment.add("WASDI_WORKSPACE_ID=" + oParameter.getWorkspace());
			asEnvironment.add("WASDI_WEBSERVER_URL=" + WasdiConfig.Current.baseUrl);
			asEnvironment.add("WASDI_ONESHOT_ON_SERVER=false");
			asEnvironment.add("WASDI_ONESHOT_ENCODED_PARAMS=" + sEncodedParams);

			JsonNode oResponse = sendSlurmRequest("POST", "job/submit", oBody);
			String sJobId = oResponse.path("job_id").asText("");
			
			if (!isValidJobId(sJobId)) {
				throw new IOException("Slurm submission returned no valid job ID; check Slurm before retrying");
			}
			
			WasdiLog.infoLog("MeluxinaRuntimeDriver.run: submitted Slurm job " + sJobId);
			
			oProcess = oRepository.getProcessByProcessObjId(sProcessId);
			
			if (oProcess == null || ProcessStatus.STOPPED.name().equals(oProcess.getStatus())) {
				cancelJob(sJobId);
				return null;
			}
			
			oProcess.setRuntimeDriver(DRIVER_NAME);
			oProcess.setRuntimeReference(sJobId);
			
			if (!oRepository.updateProcess(oProcess)) {
				WasdiLog.errorLog("MeluxinaRuntimeDriver.run: could not persist Slurm job " + sJobId);
				cancelJob(sJobId);
				return null;
			}
			
			processWorkspaceLog("MeluXina job " + sJobId + " submitted to Slurm");
			
			return sJobId;
			
		} 
		catch (IOException oEx) {
			WasdiLog.errorLog("MeluxinaRuntimeDriver.run: " + oEx.getMessage());
			processWorkspaceLog("MeluXina job submission failed: " + oEx.getMessage());
			return null;
		}
	}

	@Override
	public String waitForCompletion(ProcessorParameter oParameter) {
		ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
		String sProcessId = oParameter.getProcessObjId();
		long lPollMs = 10000;
		try {
			long lConfiguredPollMs = Long.parseLong(WasdiConfig.Current.scheduler.processingThreadSleepingTimeMS);
			if (lConfiguredPollMs > 0) lPollMs = lConfiguredPollMs;
		} catch (Exception oEx) {
			WasdiLog.debugLog("MeluxinaRuntimeDriver.waitForCompletion: using default polling interval");
		}
		while (!Thread.currentThread().isInterrupted()) {
			ProcessWorkspace oProcess = oRepository.getProcessByProcessObjId(sProcessId);
			if (oProcess == null) return ProcessStatus.ERROR.name();
			if (LauncherOperationsUtils.isTerminalStatus(oProcess.getStatus())) return oProcess.getStatus();
			try {
				ProcessStatus oStatus = readJobStatus(oProcess.getRuntimeReference());
				if (LauncherOperationsUtils.isTerminalStatus(oStatus.name())) {
					oProcess = oRepository.getProcessByProcessObjId(sProcessId);
					if (oProcess == null) return ProcessStatus.ERROR.name();
					if (LauncherOperationsUtils.isTerminalStatus(oProcess.getStatus())) return oProcess.getStatus();
					LauncherMain.updateProcessStatus(oRepository, oProcess, oStatus, 100);
					return oStatus.name();
				}
			} catch (IOException oEx) {
				WasdiLog.errorLog("MeluxinaRuntimeDriver.waitForCompletion: " + oEx.getMessage());
			}
			try {
				Thread.sleep(lPollMs);
			} catch (InterruptedException oEx) {
				Thread.currentThread().interrupt();
			}
		}
		return ProcessStatus.RUNNING.name();
	}

	@Override
	public String getStatus(String sProcessWorkspaceId) {
		ProcessWorkspace oProcess = new ProcessWorkspaceRepository().getProcessByProcessObjId(sProcessWorkspaceId);
		if (oProcess == null || !DRIVER_NAME.equals(oProcess.getRuntimeDriver())) return ProcessStatus.ERROR.name();
		if (LauncherOperationsUtils.isTerminalStatus(oProcess.getStatus())) return oProcess.getStatus();
		try {
			return readJobStatus(oProcess.getRuntimeReference()).name();
		} catch (IOException oEx) {
			WasdiLog.errorLog("MeluxinaRuntimeDriver.getStatus: " + oEx.getMessage());
			return ProcessStatus.RUNNING.name();
		}
	}

	@Override
	public boolean stop(String sProcessWorkspaceId) {
		ProcessWorkspace oProcess = new ProcessWorkspaceRepository().getProcessByProcessObjId(sProcessWorkspaceId);
		if (oProcess == null || !DRIVER_NAME.equals(oProcess.getRuntimeDriver())) return false;
		try {
			return cancelJob(oProcess.getRuntimeReference());
		} catch (IOException oEx) {
			WasdiLog.errorLog("MeluxinaRuntimeDriver.stop: " + oEx.getMessage());
			return false;
		}
	}

	protected String getSlurmToken() {
		return "";
	}

	private String getOption(String sName, String sDefault) {
		if (m_oDriverConfig == null || m_oDriverConfig.options == null) return sDefault;
		String sValue = m_oDriverConfig.options.get(sName);
		return Utils.isNullOrEmpty(sValue) ? sDefault : sValue;
	}

	private JsonNode sendSlurmRequest(String sMethod, String sPath, ObjectNode oBody) throws IOException {
		String sToken = getSlurmToken();
		if (Utils.isNullOrEmpty(sToken) || sToken.isBlank()) throw new IOException("MeluXina token acquisition is not configured");
		String sApiVersion = getOption("apiVersion", "v0.0.43");
		if (m_oDriverConfig == null || !sApiVersion.matches("v[0-9]+\\.[0-9]+\\.[0-9]+")) {
			throw new IOException("Invalid MeluXina Slurm API configuration");
		}
		String sRuntimeUrl = Utils.isNullOrEmpty(m_oDriverConfig.runtimeUrl)
				? "https://slurm-api.lxp.lu" : m_oDriverConfig.runtimeUrl;
		try {
			URI oUri = URI.create(sRuntimeUrl);
			if (!"https".equalsIgnoreCase(oUri.getScheme()) || oUri.getHost() == null
					|| oUri.getUserInfo() != null || oUri.getQuery() != null || oUri.getFragment() != null) {
				throw new IOException("MeluXina Slurm URL must be an HTTPS URL without credentials or query parameters");
			}
		} catch (IllegalArgumentException oEx) {
			throw new IOException("Invalid MeluXina Slurm URL");
		}
		String sUrl = sRuntimeUrl.replaceAll("/+$", "") + "/slurm/" + sApiVersion + "/" + sPath;
		Map<String, String> asHeaders = new HashMap<>();
		asHeaders.put("X-SLURM-USER-TOKEN", sToken);
		asHeaders.put("Content-Type", "application/json");
		HttpCallResponse oResponse;
		switch (sMethod) {
			case "GET" -> oResponse = HttpUtils.httpGet(sUrl, asHeaders);
			case "POST" -> oResponse = HttpUtils.httpPost(sUrl, oBody.toString(), asHeaders);
			case "DELETE" -> oResponse = HttpUtils.httpDelete(sUrl, asHeaders);
			default -> throw new IOException("Unsupported Slurm HTTP method");
		}
		if (oResponse.getResponseCode() == null || oResponse.getResponseCode() < 200 || oResponse.getResponseCode() >= 300) {
			String sMessage = "Slurm " + sMethod + " failed (HTTP " + oResponse.getResponseCode() + ")";
			if ("POST".equals(sMethod)) sMessage += "; check Slurm before retrying because submission may have succeeded";
			throw new IOException(sMessage);
		}
		if (Utils.isNullOrEmpty(oResponse.getResponseBody())) throw new IOException("Slurm returned an empty response");
		JsonNode oResult;
		try {
			oResult = MongoRepository.s_oMapper.readTree(oResponse.getResponseBody());
		} catch (IOException oEx) {
			throw new IOException("Slurm returned invalid JSON");
		}
		if (oResult == null || !oResult.isObject()) throw new IOException("Slurm returned an invalid response");
		JsonNode aoErrors = oResult.path("errors");
		if (!aoErrors.isMissingNode() && !aoErrors.isArray()) throw new IOException("Slurm returned invalid errors");
		if (aoErrors.isArray() && !aoErrors.isEmpty()) throw new IOException("Slurm reported an error");
		return oResult;
	}

	private boolean cancelJob(String sJobId) throws IOException {
		if (!isValidJobId(sJobId)) throw new IOException("Invalid Slurm job ID");
		JsonNode oResponse = sendSlurmRequest("DELETE", "job/" + sJobId, null);
		JsonNode aoResults = oResponse.path("status");
		if (!aoResults.isMissingNode()) {
			if (!aoResults.isArray()) throw new IOException("Slurm returned an invalid cancellation response");
			for (JsonNode oResult : aoResults) {
				JsonNode oError = oResult.path("error");
				if (oResult.path("error_code").asInt(0) != 0 || oError.path("code").asInt(0) != 0
						|| (oError.isTextual() && !oError.asText().isEmpty())) {
					throw new IOException("Slurm rejected cancellation of job " + sJobId);
				}
			}
		}
		return true;
	}

	private ProcessStatus readJobStatus(String sJobId) throws IOException {
		if (!isValidJobId(sJobId)) throw new IOException("Invalid Slurm job ID");
		JsonNode aoJobs = sendSlurmRequest("GET", "job/" + sJobId, null).path("jobs");
		if (!aoJobs.isArray() || aoJobs.size() != 1) throw new IOException("Slurm returned no unique job for " + sJobId);
		JsonNode aoStates = aoJobs.get(0).path("job_state");
		if (!aoStates.isArray()) throw new IOException("Slurm returned an invalid job state");
		for (JsonNode oState : aoStates) {
			if ("COMPLETING".equals(oState.asText()) || "CONFIGURING".equals(oState.asText())) return ProcessStatus.RUNNING;
		}
		for (JsonNode oState : aoStates) {
			switch (oState.asText()) {
				case "FAILED", "NODE_FAIL", "OUT_OF_MEMORY", "BOOT_FAIL", "DEADLINE", "PREEMPTED", "REVOKED", "TIMEOUT" -> { return ProcessStatus.ERROR; }
				case "CANCELLED" -> { return ProcessStatus.STOPPED; }
			}
		}
		for (JsonNode oState : aoStates) {
			if ("PENDING".equals(oState.asText()) || "RUNNING".equals(oState.asText())
					|| "SUSPENDED".equals(oState.asText())) return ProcessStatus.RUNNING;
		}
		for (JsonNode oState : aoStates) {
			if ("COMPLETED".equals(oState.asText())) return ProcessStatus.DONE;
		}
		throw new IOException("Slurm returned an unknown job state for " + sJobId);
	}

	private static boolean isValidJobId(String sJobId) {
		return sJobId != null && sJobId.matches("[1-9][0-9]*");
	}
}