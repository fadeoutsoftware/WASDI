package wasdi.shared.utils.kubernetes;

import java.util.ArrayList;
import java.util.List;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
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
	    
	    String sFullImage = sImageName + (Utils.isNullOrEmpty(sImageVersion) ? ":latest" : ":" + sImageVersion);
	    
	    if (!Utils.isNullOrEmpty(m_sRegistry)) sFullImage = m_sRegistry + "/" + sFullImage;

	    // Sanitize Job Name
	    String sJobName = ("wasdi-job-" + sImageName).toLowerCase().replaceAll("[^a-z0-9]", "-");
	    if (sJobName.length() > 50) sJobName = sJobName.substring(0, 50);
	    sJobName += "-" + Utils.getRandomName().toLowerCase();

	    try (KubernetesClient oClient = new KubernetesClientBuilder().build()) {

	        // 3. Cleanup preventivo
	        if (bAlwaysRecreateJob) {
	            oClient.batch().v1().jobs().inNamespace(m_sNameSpace).withName(sJobName).delete();
	        }

	        // 4. Configurazione Volumi (logica identica ai Pod)
	        List<Volume> loVolumes = buildVolumes(); // Supponiamo una funzione helper per brevità
	        List<VolumeMount> loMounts = buildVolumeMounts(asAdditionalMountPoints);

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
	                            .withVolumeMounts(loMounts)
	                        .endContainer()
	                        .withVolumes(loVolumes)
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
	
	private List<Volume> buildVolumes() {
	    List<Volume> loVolumes = new ArrayList<>();

	    // mandatory volume for wasdi
	    String sBaseVolName = "wasdi-data-storage";
	    loVolumes.add(new VolumeBuilder()
	        .withName(sBaseVolName)
	        .withNewHostPath()
	            .withPath(PathsConfig.getWasdiBasePath()) 
	        .endHostPath()
	        .build());
	    
	    return loVolumes;
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

	
	public static void main(String[] args) throws Exception {
		KubernetesUtils oUtils = new KubernetesUtils("default", "IfNotPresent", "");
		oUtils.run("nginx", null, null, false, null, false);
	}

}
