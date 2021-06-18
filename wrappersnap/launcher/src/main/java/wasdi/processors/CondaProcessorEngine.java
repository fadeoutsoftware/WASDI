package wasdi.processors;

public class CondaProcessorEngine extends DockerProcessorEngine {

	public CondaProcessorEngine(String sWorkingRootPath, String sDockerTemplatePath, String sTomcatUser) {
		super(sWorkingRootPath, sDockerTemplatePath, sTomcatUser);
		
		m_sDockerTemplatePath = sDockerTemplatePath;		
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += "conda";			
	}
	
}
