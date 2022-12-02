package wasdi.asynch;

import java.util.ArrayList;

import wasdi.processors.DockerUtils;
import wasdi.shared.business.Processor;
import wasdi.shared.config.DockerRegistryConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class PushDockerImagesThread extends Thread {
	
	/**
	 * List of registers where we must push
	 */
	private ArrayList<DockerRegistryConfig> m_aoRegisters = new ArrayList<DockerRegistryConfig>();
	/**
	 * Processor to push
	 */
	private Processor m_oProcessor;
	/**
	 * Docker image name
	 */
	private String m_sDockerImageName;
	
	public ArrayList<DockerRegistryConfig> getRegisters() {
		return m_aoRegisters;
	}

	public Processor getProcessor() {
		return m_oProcessor;
	}

	public void setProcessor(Processor oProcessor) {
		this.m_oProcessor = oProcessor;
	}

	public String getDockerImageName() {
		return m_sDockerImageName;
	}

	public void setDockerImageName(String sDockerImageName) {
		this.m_sDockerImageName = sDockerImageName;
	}

	@Override
	public void run() {
		try {
			
			try {
				
				// And get the processor folder
				String sProcessorFolder = WasdiConfig.Current.paths.downloadRootPath + "/processors/" + m_oProcessor.getName() + "/";
				
				// Create the docker utils
				DockerUtils oDockerUtils = new DockerUtils(m_oProcessor, sProcessorFolder, WasdiConfig.Current.paths.downloadRootPath, "");
				
				
				// For each register: ordered by priority
				for (int iRegisters=0; iRegisters<m_aoRegisters.size(); iRegisters++) {
					
					DockerRegistryConfig oDockerRegistryConfig = m_aoRegisters.get(iRegisters);
					
					WasdiLog.debugLog("EoepcaProcessorEngine.pushImageInRegisters: try to push to " + oDockerRegistryConfig.id);
					
					// Try to login and push
					String sPushedImageAddress = loginAndPush(oDockerUtils, oDockerRegistryConfig, m_sDockerImageName, sProcessorFolder);
					
					if (Utils.isNullOrEmpty(sPushedImageAddress)) {
						WasdiLog.debugLog("EoepcaProcessorEngine.pushImageInRegisters: error in the push");
					}
				}
			}
			catch (Exception oEx) {
				WasdiLog.errorLog("EoepcaProcessorEngine.pushImageInRegisters: error " + oEx.toString());
			}
			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("SaveMetadataThread.run: exception " + oEx.toString());
		}
	}
	
	/**
	 * Log in and Push an image to a Docker Registry
	 * @param oDockerUtils
	 * @param oDockerRegistryConfig
	 * @param sImageName
	 * @return
	 */
	protected String loginAndPush(DockerUtils oDockerUtils, DockerRegistryConfig oDockerRegistryConfig, String sImageName, String sFolder) {
		try {
			boolean bLogged = oDockerUtils.login(oDockerRegistryConfig.address, oDockerRegistryConfig.user, oDockerRegistryConfig.password, sFolder);
			
			if (!bLogged) {
				WasdiLog.debugLog("EoepcaProcessorEngine.loginAndPush: error logging in, return false.");
				return "";
			}
			
			boolean bPushed = oDockerUtils.push(sImageName);
			
			if (!bPushed) {
				WasdiLog.debugLog("EoepcaProcessorEngine.loginAndPush: error in push, return false.");
				return "";				
			}
			
			return sImageName;
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("EoepcaProcessorEngine.loginAndPush: Exception " + oEx.toString());
		}
		
		return "";
	}	
}
