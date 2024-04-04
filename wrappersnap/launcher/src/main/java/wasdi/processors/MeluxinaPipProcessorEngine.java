package wasdi.processors;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import wasdi.LauncherMain;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.business.processors.ProcessorTypes;
import wasdi.shared.config.EnvironmentVariableConfig;
import wasdi.shared.config.MeluxinaConfig;
import wasdi.shared.config.ProcessorTypeConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.OgcProcessesClient;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.ogcprocesses.Execute;
import wasdi.shared.viewmodels.ogcprocesses.StatusCode;
import wasdi.shared.viewmodels.ogcprocesses.StatusInfo;

public class MeluxinaPipProcessorEngine extends DockerBuildOnceEngine {

	/**
	 * Default constructor
	 */
	public MeluxinaPipProcessorEngine() {
		super();
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += ProcessorTypes.getTemplateFolder(ProcessorTypes.PYTHON_PIP_2);
		
		m_asDockerTemplatePackages = new String[8];
		m_asDockerTemplatePackages[0] = "flask";
		m_asDockerTemplatePackages[1] = "gunicorn";
		m_asDockerTemplatePackages[2] = "requests";
		m_asDockerTemplatePackages[3] = "numpy";
		m_asDockerTemplatePackages[4] = "wheel";
		m_asDockerTemplatePackages[5] = "wasdi";
		m_asDockerTemplatePackages[6] = "time";
		m_asDockerTemplatePackages[7] = "datetime";
	}

	/**
	 * Run a processor in Meluxina
	 */
	@Override
	public boolean run(ProcessorParameter oParameter) {
        WasdiLog.debugLog("MeluxinaPipProcessorEngine.run: start");

        if (oParameter == null) {
            WasdiLog.errorLog("MeluxinaPipProcessorEngine.run: parameter is null");
            return false;
        }
        
        // Get Repo and Process Workspace
        ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
        ProcessWorkspace oProcessWorkspace = m_oProcessWorkspace;
        
        ProcessorRepository oProcessorRepository = new ProcessorRepository();
        Processor oProcessor = oProcessorRepository.getProcessor(oParameter.getProcessorID());
        
        if (oProcessor == null) {
            WasdiLog.errorLog("MeluxinaPipProcessorEngine.run: processor is null");
            return false;        	
        }
        
		try {
			
			ProcessorTypeConfig oConfig = WasdiConfig.Current.dockers.getProcessorTypeConfig(ProcessorTypes.PYTHON_PIP_MELUXINA);
			
			
			String sBaseAddress = getMeluxinaBaseAddress();
			sBaseAddress += "job/submit";
			
			HashMap<String, String> asHeaders = new HashMap<>();
			asHeaders.put("X-SLURM-USER-NAME", WasdiConfig.Current.dockers.meluxina.user);
			asHeaders.put("X-SLURM-USER-TOKEN", WasdiConfig.Current.dockers.meluxina.token);
			asHeaders.put("Content-Type", "application/json");
			
			String sPayload = getSubmitJobPayload(oConfig, oProcessor, oParameter);
			
			HttpCallResponse oResponse = HttpUtils.httpPost(sBaseAddress, sPayload, asHeaders, "", null);
			
			Map<String,Object> aoJobReturn= null;

			try {
				aoJobReturn = (Map<String, Object>) MongoRepository.s_oMapper.readValue(oResponse.getResponseBody(), Map.class);			    
			} 
			catch (Exception oEr) {
			    WasdiLog.errorLog("MeluxinaPipProcessorEngine.run: error reading the Json response");
			}
			
			Integer iJobId = (Integer) aoJobReturn.get("job_id");
			String sJobId = "" + iJobId;
			
            long lTimeSpentMs = 0;
            int iThreadSleepMs = 2000;
            
            String sStatus = "RUNNING";
            ProcessStatus oWasdiStatus = ProcessStatus.CREATED;
			
			while(true) {
				
				sStatus = getStatus(sJobId);
				
				oWasdiStatus = getWasdiStatusFromMeluxinaStatus(sStatus);
				
				if (oWasdiStatus == ProcessStatus.DONE || oWasdiStatus == ProcessStatus.ERROR || oWasdiStatus == ProcessStatus.STOPPED) {
					WasdiLog.infoLog("MeluxinaPipProcessorEngine.run: got WASDI end Status " + oWasdiStatus.name());
					break;
				}
				
                try {
                    Thread.sleep(iThreadSleepMs);
                } catch (InterruptedException oEx) {
                    WasdiLog.errorLog("MeluxinaPipProcessorEngine.run: Thread sleep exception ", oEx);
                    Thread.currentThread().interrupt();
                }        

                // Increase the time
                lTimeSpentMs += iThreadSleepMs;

                if (oProcessor.getTimeoutMs() > 0) {
                    if (lTimeSpentMs > oProcessor.getTimeoutMs()) {
                        // Timeout
                        WasdiLog.debugLog("MeluxinaPipProcessorEngine.run: Timeout of Processor with ProcId " + oProcessWorkspace.getProcessObjId() + " Time spent [ms] " + lTimeSpentMs);

                        // Update process and rabbit users
                        LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
                        break;
                    }
                }				
			}
			
			if (oWasdiStatus!=null) {
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, oWasdiStatus, 100);
			}
			
            // Check and set the operation end-date
            if (Utils.isNullOrEmpty(oProcessWorkspace.getOperationEndTimestamp())) {
                oProcessWorkspace.setOperationEndTimestamp(Utils.nowInMillis());
            }			
			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("MeluxinaPipProcessorEngine.run: error " + oEx.toString());
			return false;
		}
		
		return true;		
	}
	
	/**
	 * Converts Meluxina Status to WASDI Status
	 * @param sMeluxinaStatus
	 * @return
	 */
	ProcessStatus getWasdiStatusFromMeluxinaStatus(String sMeluxinaStatus) {
		ProcessStatus oStatus = ProcessStatus.ERROR;
		
		if (!Utils.isNullOrEmpty(sMeluxinaStatus)) {
			if (sMeluxinaStatus.equals("BOOT_FAIL") ||
				sMeluxinaStatus.equals("FAILED") ||
				sMeluxinaStatus.equals("NODE_FAIL") ||
				sMeluxinaStatus.equals("OUT_OF_MEMORY")) {
				oStatus = ProcessStatus.ERROR;
			}
			else if (sMeluxinaStatus.equals("CANCELLED") ||
					sMeluxinaStatus.equals("DEADLINE") ||
					sMeluxinaStatus.equals("PREEMPTED") ||
					sMeluxinaStatus.equals("REVOKED") ||
					sMeluxinaStatus.equals("STOPPED") ||
					sMeluxinaStatus.equals("TIMEOUT")) {
					oStatus = ProcessStatus.STOPPED;
			}
			else if (sMeluxinaStatus.equals("COMPLETED") ||
					sMeluxinaStatus.equals("SPECIAL_EXIT")) {
					oStatus = ProcessStatus.DONE;
			}
			else if (sMeluxinaStatus.equals("CONFIGURING") ||
					sMeluxinaStatus.equals("COMPLETING") ||
					sMeluxinaStatus.equals("RUNNING") ||
					sMeluxinaStatus.equals("RESIZING") ||
					sMeluxinaStatus.equals("SIGNALING") ||
					sMeluxinaStatus.equals("STAGE_OUT")) {
					oStatus = ProcessStatus.RUNNING;
			}
			else if (sMeluxinaStatus.equals("PENDING")) {
					oStatus = ProcessStatus.CREATED;
			}
			else if (sMeluxinaStatus.equals("RESV_DEL_HOLD") ||
					sMeluxinaStatus.equals("REQUEUE_FED") ||
					sMeluxinaStatus.equals("REQUEUE_HOLD") ||
					sMeluxinaStatus.equals("REQUEUED") ||
					sMeluxinaStatus.equals("SUSPENDED")) {
					oStatus = ProcessStatus.WAITING;
			}			
		}
		
		
		return oStatus;
	}


	/**
	 * Get the payload to submit a job in Meluxina
	 * @param oMeluxinaConfig Meluxina Config
	 * @param oProcessorTypeConfig Processor Type Config
	 * @param oProcessor Processor Entity
	 * @param oProcessorParameter Parameter
	 * @return String that can be sent as payload to Meluxina APIs
	 */
	@SuppressWarnings("unchecked")
	private String getSubmitJobPayload(ProcessorTypeConfig oProcessorTypeConfig, Processor oProcessor, ProcessorParameter oProcessorParameter) {
		
		// The script that physically run the container
		String sScript = "#!/bin/bash -l\\n\\nset -e\\n\\nsContainerRegistry=\\\"\\${APPTAINER_DOCKER_REGISTRY}\\\"\\nsContainerImageName=\\\"\\${APPTAINER_DOCKER_IMAGE_NAME}\\\"\\nsContainerImageVersion=\\\"\\${APPTAINER_DOCKER_IMAGE_VERSION}\\\"\\nsContainerImageFile=\\\"$(echo \\${sContainerRegistry}/\\${sContainerImageName}:\\${sContainerImageVersion} | sha256sum | awk '{print $1}').sif\\\"\\nsWasdiProcessorWorkingDirectory=\\\"\\${WASDI_PROCESSOR_WORKING_DIRECTORY}\\\"\\n\\n\\n## LOAD THE NEEDED MODULE ##\\nmodule load Apptainer/1.2.4-GCCcore-12.3.0\\n\\n\\n## CLEANING ##\\nrm --force \\${sContainerImageFile}\\n\\n\\n## EXECUTION\\napptainer pull \\${sContainerImageFile} docker://\\${sContainerRegistry}/\\${sContainerImageName}:\\${sContainerImageVersion}\\napptainer run --pwd \\${sWasdiProcessorWorkingDirectory} \\${sContainerImageFile}\\n\\n\\n## CLEANING ##\\nrm --force \\${sContainerImageFile}";
		
		// We need the Payload
		HashMap<String, Object> aoPayload = new HashMap<>();
		// The job
		HashMap<String, Object> aoJob = new HashMap<>();
		// And the environment objects
		HashMap<String, Object> aoEnvironment = new HashMap<>();
		
		// We se the home folder
		String sHomeFolder = WasdiConfig.Current.dockers.meluxina.home;
		if (!sHomeFolder.endsWith("/")) sHomeFolder += "/";
		sHomeFolder += WasdiConfig.Current.dockers.meluxina.user;
		
		// Initialized Environment
		aoEnvironment.put("APPTAINER_DOCKER_REGISTRY", WasdiConfig.Current.dockers.meluxina.registryAddress);
		aoEnvironment.put("APPTAINER_DOCKER_USERNAME", WasdiConfig.Current.dockers.meluxina.registryUserName);
		aoEnvironment.put("APPTAINER_DOCKER_PASSWORD", WasdiConfig.Current.dockers.meluxina.registryPassword);
		aoEnvironment.put("APPTAINER_DOCKER_IMAGE_NAME", "wasdi/" + oProcessor.getName());
		aoEnvironment.put("APPTAINER_DOCKER_IMAGE_VERSION", oProcessor.getVersion());
		aoEnvironment.put("APPTAINERENV_WASDI_SESSION_ID", oProcessorParameter.getSessionID());
		
		// Get the JSON
		String sJsonParams = oProcessorParameter.getJson();
		
		Map<String,Object> aoInputParams= null;

		try {
			// Are already encoded or not?
			aoInputParams = (Map<String, Object>) MongoRepository.s_oMapper.readValue(sJsonParams, Map.class);
		    
		} catch (Exception oEr) {
			// Yes, lets try to decode
		    WasdiLog.errorLog("EoepcaProcessorEngine.run: error decoding the Json Parameters, try to decode");
		    try {
		    	String sDecodedParams = java.net.URLDecoder.decode(sJsonParams, StandardCharsets.UTF_8.name());
		    	aoInputParams = (Map<String, Object>) MongoRepository.s_oMapper.readValue(sDecodedParams, Map.class);
		    	WasdiLog.debugLog("MeluxinaProcessorEngine.getSubmitJobPayload: ok decode done");
		    }
		    catch (Exception oExInner) {
		    	WasdiLog.errorLog("MeluxinaProcessorEngine.getSubmitJobPayload: error decoding the Json Parameters, also after the decode", oEr);
		    }
		}
		
		try {
			// Now, in any case, we encode
			sJsonParams = JsonUtils.stringify(aoInputParams);
			sJsonParams = java.net.URLEncoder.encode(sJsonParams, StandardCharsets.UTF_8.name());			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("MeluxinaProcessorEngine.getSubmitJobPayload: error encoding the Json Parameters, not sure will work", oEx);
		}

		aoEnvironment.put("APPTAINERENV_WASDI_ONESHOT_ENCODED_PARAMS", sJsonParams);
		aoEnvironment.put("APPTAINERENV_WASDI_PROCESS_WORKSPACE_ID", oProcessorParameter.getProcessObjId());
		aoEnvironment.put("APPTAINERENV_WASDI_USER", oProcessorParameter.getUserId());
		aoEnvironment.put("APPTAINERENV_WASDI_WORKSPACE_ID", oProcessorParameter.getWorkspace());
		aoEnvironment.put("APPTAINERENV_WASDI_WORKSPACE_ID", "/mnt/tier2/project/" + WasdiConfig.Current.dockers.meluxina.project);
		
		// Add all the others
		for (EnvironmentVariableConfig oEnvVar: oProcessorTypeConfig.environmentVariables) {
			aoEnvironment.put(oEnvVar.key, oEnvVar.value);
		}
		
		
		// Fill Job params
		aoJob.put("current_working_directory", sHomeFolder);
		aoJob.put("qos", "default");
		aoJob.put("time_limit", 600);
		aoJob.put("account", WasdiConfig.Current.dockers.meluxina.project);
		aoJob.put("standard_input", "/dev/null");
		aoJob.put("standard_output", sHomeFolder + "/execution_%j.out");
		aoJob.put("standard_error", sHomeFolder + "/execution_%j.out");
		
		// Add environment to the Jobs
		aoJob.put("environment", aoEnvironment);
		
		// Add to the payload the script and the job
		aoPayload.put("script", sScript);
		aoPayload.put("job", aoJob);
		
		// Here we will store the result
		String sPaylod = "{}";
		
		try {
			// Get the JSON
			sPaylod = JsonUtils.stringify(aoPayload);			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("MeluxinaProcessorEngine.getSubmitJobPayload: error encoding the Json Parameters, not sure will work", oEx);
		}
		
		// Done!
		return sPaylod;
	}
	
	/**
	 * Get the Status of the Job in Meluxina
	 * @param oMeluxinaConfig Meluxina Config
	 * @param sJobId Job Id
	 * @return String representing the status in the SLURM format
	 */
	@SuppressWarnings("unchecked")
	protected String getStatus(String sJobId) {
		String sBaseAddress = getMeluxinaBaseAddress();
		sBaseAddress += "job/" + sJobId;
		
		String sStatus = "FAILED";
		
		try {
			
			HashMap<String, String> asHeaders = new HashMap<>();
			asHeaders.put("X-SLURM-USER-NAME", WasdiConfig.Current.dockers.meluxina.user);
			asHeaders.put("X-SLURM-USER-TOKEN", WasdiConfig.Current.dockers.meluxina.token);
			
			HttpCallResponse oResponse = HttpUtils.httpGet(sBaseAddress, asHeaders);
			
			Map<String, Object> oStatusResponse = JsonUtils.jsonToMapOfObjects(oResponse.getResponseBody());
			
			List<Map<String, Object>> aoJobs = (List<Map<String, Object>>) oStatusResponse.get("jobs");
			for (Map<String, Object> oJob : aoJobs) {
				Integer iActualJobId = (Integer) oJob.get("job_id");
				String sActualJobId = "" + iActualJobId;
				
				if (sActualJobId.equals(sJobId)) {
					sStatus = (String) oJob.get("job_state");
					WasdiLog.debugLog("MeluxinaProcessorEngine.getStatus: got status " + sStatus + " for Job " + sJobId);
					break;
				}
			}
			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("MeluxinaProcessorEngine.getStatus: error checking the status", oEx);
		}
		
		return sStatus;
	}
	
	/**
	 * Get the base address of the Meluxina API
	 * @param oMeluxinaConfig
	 * @return
	 */
	protected String getMeluxinaBaseAddress() {
		String sBaseAddress = WasdiConfig.Current.dockers.meluxina.apiUrl;
		if (!sBaseAddress.endsWith("/")) sBaseAddress += "/";
		sBaseAddress = sBaseAddress + WasdiConfig.Current.dockers.meluxina.apiVersion + "/";
		return sBaseAddress;
	}
	
}
