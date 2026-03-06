package wasdi.shared.utils.kubernetes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import wasdi.shared.config.DockerRegistryConfig;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.StringUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class KubernetesUtils {
	
	private String m_sNameSpace;
	private String m_sImagePullPolicy;
	private String m_sRegistry;
	
	/**
	 * Constructors
	 */

	public KubernetesUtils() {
		m_sNameSpace = "default";
		m_sImagePullPolicy = "IfNotPresent";	// options: always, InNotPresent, Never
		m_sRegistry = "";
	}
	
	public KubernetesUtils(String sNamespace, String sImagePullPolicy, String sRegistry) {
		m_sNameSpace = sNamespace;
		m_sImagePullPolicy = sImagePullPolicy;
		m_sRegistry = sRegistry;
	}
	
	
	/**
	 * Getters and setters
	 */
	
	public String getNameSpace() {
		return m_sNameSpace;
	}
	
	public void setNameSpace(String sNameSpace) {
		m_sNameSpace = sNameSpace;
	}
	
	public String getImagePullPolicy() {
		return m_sImagePullPolicy;
	}
	
	public void setImagePullPolicy(String sImagePullPolicy) {
		m_sImagePullPolicy = sImagePullPolicy;
	}
	
	public String getRegistry() {
		return m_sRegistry;
	}
	
	public void setRegistry(String sRegistry) {
		m_sRegistry = sRegistry;
	}
	
	
	
	public String run(String sImageName, String sImageVersion, List<String> asArg, boolean bAlwaysRecreateJob, ArrayList<String> asAdditionalMountPoints, boolean bAutoRemove) {

	    
	    if (Utils.isNullOrEmpty(sImageName)) return "";
	    
	    String sFullImage = sImageName;
	    
	    sFullImage += Utils.isNullOrEmpty(sImageVersion) 
	    		? ":latest" 
	    		: ":" + sImageVersion;
	    
	    if (!Utils.isNullOrEmpty(m_sRegistry)) sFullImage = m_sRegistry + "/" + sFullImage;

	    // Sanitize Job Name
	    String sJobName = ("wasdi-job-" + sImageName).toLowerCase().replaceAll("[^a-z0-9]", "-");
	    if (sJobName.length() > 50) 
	    	sJobName = sJobName.substring(0, 50);
	    sJobName += "-" + Utils.getRandomName().toLowerCase();

	    try (KubernetesClient oClient = new KubernetesClientBuilder().build()) {

	        if (bAlwaysRecreateJob) {
	            oClient.batch().v1().jobs().inNamespace(m_sNameSpace).withName(sJobName).delete();
	        }

	        // manage the mount points
	        
	        List<Volume> aoVolumes = buildVolumes(asAdditionalMountPoints); 
	        List<VolumeMount> aoMounts = buildVolumeMounts(asAdditionalMountPoints);

	        // 5. Costruzione del JOB
	        Job oJob = new JobBuilder()
	            .withNewMetadata()
	                .withName(sJobName)
	            .endMetadata()
	            .withNewSpec()
	                // bAutoRemove: Cancella il Job automaticamente X secondi dopo la fine
	                .withTtlSecondsAfterFinished(bAutoRemove ? 60 : null) 
	                .withNewTemplate() // Il Job definisce un template per il Pod
	                    .withNewSpec()
	                        .addNewContainer()
	                            .withName("worker")
	                            .withImage(sFullImage)
	                            .withArgs(asArg != null ? asArg : new ArrayList<>())
	                            .withVolumeMounts(aoMounts)
	                        .endContainer()
	                        .withVolumes(aoVolumes)
	                        .withRestartPolicy("Never") // Obbligatorio per i Job
	                    .endSpec()
	                .endTemplate()
	            .endSpec()
	            .build();

	        // 6. Invio al Cluster
	        Job oCreatedJob = oClient.batch().v1().jobs().inNamespace(m_sNameSpace).resource(oJob).create();
	        
	        String sJobUid = oCreatedJob.getMetadata().getUid();
	        WasdiLog.debugLog("Job lanciato con successo. UID: " + sJobUid);
	        return sJobUid;

	    } catch (Exception oEx) {
	        WasdiLog.errorLog("Errore nel lancio del Job: ", oEx);
	        return "";
	    }
	}
	
	private List<Volume> buildVolumes(ArrayList<String> aoMountPoints) {
		// we assume the format "pathOfTheVolumeOnHost:pathOfTheVolumeInTheContainer"
	    
		if (aoMountPoints.isEmpty()) {
			WasdiLog.warnLog("KubernetesUtils.buildVolumes. Empty list of mount points");
			return null;
		} 
	    
		List<Volume> aoVolumes = new ArrayList<>();
		
        for (int i = 0; i < aoMountPoints.size(); i++) {
            String sMountEntry = aoMountPoints.get(i);
            
            String[] asParts = sMountEntry.split(":");
            
            if (asParts.length >= 2) {
            	String sHostPath = asParts[0];
                // we create a unique name for each volume
                String sVolName = "vol-" + i;

                aoVolumes.add(new VolumeBuilder()
                    .withName(sVolName)
                    .withNewHostPath()
                        .withPath(sHostPath)
                        .withType("DirectoryOrCreate")
                    .endHostPath()
                    .build());
            }
        }
	    
	    return aoVolumes;
	}
	

	private List<VolumeMount> buildVolumeMounts(ArrayList<String> asAdditionalMountPoints) {
	    List<VolumeMount> loMounts = new ArrayList<>();

	    String sBaseVolName = "wasdi-data-storage";
	    loMounts.add(new VolumeMountBuilder()
	        .withName(sBaseVolName)
	        .withMountPath("/data/wasdi") // Percorso interno al container
	        .build());

	    if (asAdditionalMountPoints != null) {
	        int iIdx = 0;
	        for (String sMount : asAdditionalMountPoints) {
	            // Dividiamo la stringa "C:\temp:/mnt/temp"
	            String[] asParts = sMount.split(":");
	            
	            if (asParts.length >= 2) {
	                // In K8s, ogni mount deve avere un Volume corrispondente.
	                // Per semplicità, qui assumiamo che il Volume venga creato dinamicamente 
	                // o che la logica buildVolumes sia allineata.
	                
	                String sVolName = "vol-extra-" + iIdx++;
	                loMounts.add(new VolumeMountBuilder()
	                    .withName(sVolName)
	                    .withMountPath(asParts[1]) // Il secondo elemento è il path nel container
	                    .build());
	            }
	        }
	    }

	    return loMounts;
	}
	
	/*
	public Job getJobInfoByImageName(String sProcessorName, String sVersion) {
	    
	    try (KubernetesClient oClient = new KubernetesClientBuilder().build()) {
	        
	        // 1. Ricostruzione della stringa immagine (Nexus + Nome + Versione)
	        String sMyImage = sProcessorName + ":" + sVersion;
	        if (!Utils.isNullOrEmpty(m_sRegistry)) {
	            sMyImage = m_sRegistry + "/" + sMyImage;
	        }
	        
	        WasdiLog.debugLog("KubernetesUtils.getJobInfoByImageName: cerco Job con immagine " + sMyImage);

	        // 2. Interroghiamo le API Batch per ottenere la lista dei Job nel namespace
	        List<Job> aoJobs = oClient.batch().v1().jobs().inNamespace(m_sNameSpace).list().getItems();

	        // 3. Iteriamo sui Job per trovare quello corretto
	        for (Job oJob : aoJobs) {
	            // Un Job definisce i container nel suo Template Spec
	            boolean bFound = oJob.getSpec().getTemplate().getSpec().getContainers().stream()
	                .anyMatch(oContainer -> oContainer.getImage().endsWith(sMyImage));

	            if (bFound) {
	                WasdiLog.debugLog("KubernetesUtils.getJobInfoByImageName: trovato Job " + oJob.getMetadata().getName());
	                return oJob;
	            }
	        }
	        
	        return null;
	        
	    } catch (Exception oEx) {
	        WasdiLog.errorLog("KubernetesUtils.getJobInfoByImageName: errore ", oEx);
	        return null;
	    }
	}
	*/
	
	public Map<String, Integer> getJobStatus(String sJobName) {
		
		Map<String, Integer> oJobStatusMap = null;;
		
		if (Utils.isNullOrEmpty(sJobName)) {
			WasdiLog.warnLog("KubernetesUtils.printJobStatus. No job name specified");
			return oJobStatusMap;
		}
		
		
		try (KubernetesClient oClient = new KubernetesClientBuilder().build()) {
		    
		    Job oJob = oClient.batch().v1().jobs().inNamespace(m_sNameSpace).withName(sJobName).get();
		    
		    if (oJob != null && oJob.getStatus() != null) {
		    	oJobStatusMap = new HashMap<>();
		    	
		    	JobStatus oStatus = oJob.getStatus();
		    	
		    	oJobStatusMap.put("active", oStatus.getActive());
		    	oJobStatusMap.put("succeeded", oStatus.getSucceeded());
		    	oJobStatusMap.put("failed", oStatus.getFailed());
		    	
		    	return oJobStatusMap;

		    } else {
		    	WasdiLog.warnLog("KubernetesUtils.printJobStatus. Job not found: " + sJobName);
		    }
		} catch (Exception oEx) {
		    WasdiLog.errorLog("KubernetesUtils.printJobStatus. Error", oEx);
		}
		
		return oJobStatusMap;
	}

	
	public static void main(String[] args) throws Exception {
		KubernetesUtils oUtils = new KubernetesUtils("default", "IfNotPresent", "");
		// oUtils.run("nginx", null, null, false, null, false);
		String sJobName = "wasdi-test-job";
		Map<String, Integer> oJobStatus = oUtils.getJobStatus(sJobName);
		for (Entry<String, Integer> oEntry : oJobStatus.entrySet()) {
			System.out.println(oEntry.getKey() + ": " + oEntry.getValue());
		}
	}

}
