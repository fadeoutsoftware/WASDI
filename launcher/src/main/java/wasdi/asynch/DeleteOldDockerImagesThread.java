package wasdi.asynch;

import wasdi.shared.business.processors.Processor;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.docker.DockerUtils;
import wasdi.shared.utils.log.WasdiLog;

public class DeleteOldDockerImagesThread extends Thread {
	
	Processor m_oProcessor;
	ProcessorParameter m_oParameter;
	String m_sDockerRegistry;
	String m_sNewVersion;
	
	
	@Override
	public void run() {
		try {
			DockerUtils oDockerUtils = new DockerUtils(m_oProcessor, m_oParameter, PathsConfig.getProcessorFolder(m_oProcessor), m_sDockerRegistry, null);
			
			WasdiLog.infoLog("DockerBuildOnceEngine.redeploy: try to clean old images. Last valid version is " + m_sNewVersion);
			
			String sVersion = m_oProcessor.getVersion();
			Integer iVersion = Integer.parseInt(sVersion);
			
			if (iVersion>1) {
				iVersion = iVersion - 1;				
				oDockerUtils.delete(m_oProcessor.getName(), "" + iVersion);
			}

		}
		catch (Exception oEx) {
			WasdiLog.errorLog("DeleteOldDockerImagesThread.run: exception " + oEx.toString());
		}		
	}


	public Processor getProcessor() {
		return m_oProcessor;
	}


	public void setProcessor(Processor oProcessor) {
		this.m_oProcessor = oProcessor;
	}


	public ProcessorParameter getParameter() {
		return m_oParameter;
	}


	public void setParameter(ProcessorParameter oParameter) {
		this.m_oParameter = oParameter;
	}


	public String getDockerRegistry() {
		return m_sDockerRegistry;
	}


	public void setDockerRegistry(String sDockerRegistry) {
		this.m_sDockerRegistry = sDockerRegistry;
	}


	public String getNewVersion() {
		return m_sNewVersion;
	}


	public void setNewVersion(String sNewVersion) {
		this.m_sNewVersion = sNewVersion;
	}
}
