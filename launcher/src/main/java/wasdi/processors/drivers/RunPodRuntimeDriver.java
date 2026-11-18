package wasdi.processors.drivers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import wasdi.processors.DockerProcessorEngine;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class RunPodRuntimeDriver extends ContainerRuntimeDriver {

	private static final URI s_oServerlessUri = URI.create("https://api.runpod.io/v2/serverless");
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
		oBody.putObject("workers").put("min", 0).put("max", 1);
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
		sendEndpointRequest("PATCH", URI.create(s_oServerlessUri + "/" + sEndpointId), oBody, 200);
	}

	private String sendEndpointRequest(String sMethod, URI oUri, ObjectNode oBody, int iExpectedStatus)
			throws IOException, InterruptedException {
		HttpRequest oRequest = HttpRequest.newBuilder(oUri)
				.timeout(Duration.ofSeconds(30))
				.header("Authorization", "Bearer " + m_oDriverConfig.apiKey)
				.header("Content-Type", "application/json")
				.method(sMethod, HttpRequest.BodyPublishers.ofString(oBody.toString()))
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
	public String run(ProcessorParameter oParameter, String sTag, String sParams) {
		return m_oLocalDockerDriver.run(oParameter, sTag, sParams);
	}

	@Override
	public String waitForCompletion(ProcessorParameter oParameter) {
		return m_oLocalDockerDriver.waitForCompletion(oParameter);
	}

	@Override
	public String getStatus(String sProcessWorkspaceId) {
		return m_oLocalDockerDriver.getStatus(sProcessWorkspaceId);
	}

	@Override
	public boolean stop(String sProcessWorkspaceId) {
		return m_oLocalDockerDriver.stop(sProcessWorkspaceId);
	}

}
