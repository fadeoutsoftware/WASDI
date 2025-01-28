package wasdi.asynch;

import java.util.ArrayList;

import wasdi.shared.business.processors.Processor;
import wasdi.shared.config.DockerRegistryConfig;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.docker.DockerUtils;
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
	
	public ArrayList<DockerRegistryConfig> getRegisters() {
		return m_aoRegisters;
	}

	public Processor getProcessor() {
		return m_oProcessor;
	}

	public void setProcessor(Processor oProcessor) {
		this.m_oProcessor = oProcessor;
	}

	@Override
	public void run() {
		try {
			
			try {
				
				// And get the processor folder
				String sProcessorFolder = PathsConfig.getProcessorFolder(m_oProcessor);
				
				// Create the docker utils
				DockerUtils oDockerUtils = new DockerUtils(m_oProcessor, sProcessorFolder);
				
				// For each register: ordered by priority
				for (int iRegisters=0; iRegisters<m_aoRegisters.size(); iRegisters++) {
					
					DockerRegistryConfig oDockerRegistryConfig = m_aoRegisters.get(iRegisters);
					
					WasdiLog.debugLog("PushDockerImagesThread.run: try to push to " + oDockerRegistryConfig.id);
					
					String sDockerImageName = oDockerRegistryConfig.address + "/wasdi/" + m_oProcessor.getName() + ":" + m_oProcessor.getVersion();
					
					
					// Try to login and push
					String sPushedImageAddress = loginAndPush(oDockerUtils, oDockerRegistryConfig, sDockerImageName);
					
					if (Utils.isNullOrEmpty(sPushedImageAddress)) {
						WasdiLog.debugLog("PushDockerImagesThread.run: error in the push");
					}
				}
			}
			catch (Exception oEx) {
				WasdiLog.errorLog("PushDockerImagesThread.run: error " + oEx.toString());
			}
			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("PushDockerImagesThread.run: exception " + oEx.toString());
		}
	}
	
	/**
	 * Log in and Push an image to a Docker Registry
	 * @param oDockerUtils
	 * @param oDockerRegistryConfig
	 * @param sImageName
	 * @return
	 */
	protected String loginAndPush(DockerUtils oDockerUtils, DockerRegistryConfig oDockerRegistryConfig, String sImageName) {
		try {
			String sToken = oDockerUtils.loginInRegistry(oDockerRegistryConfig);
			
			if (Utils.isNullOrEmpty(sToken)) {
				WasdiLog.debugLog("PushDockerImagesThread.loginAndPush: error logging in, return false.");
				return "";
			}
			
			boolean bPushed = oDockerUtils.push(sImageName, sToken);
			
			if (!bPushed) {
				WasdiLog.debugLog("PushDockerImagesThread.loginAndPush: error in push, return false.");
				return "";				
			}
			
			return sImageName;
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("PushDockerImagesThread.loginAndPush: Exception " + oEx.toString());
		}
		
		return "";
	}	
}
