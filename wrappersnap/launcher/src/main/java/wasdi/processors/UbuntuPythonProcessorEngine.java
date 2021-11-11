package wasdi.processors;

public class UbuntuPythonProcessorEngine extends DockerProcessorEngine {
	
	public UbuntuPythonProcessorEngine() {
		super();
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += "python27";		
		
	}

	public UbuntuPythonProcessorEngine(String sWorkingRootPath, String sDockerTemplatePath, String sTomcatUser) {
		super(sWorkingRootPath, sDockerTemplatePath, sTomcatUser);
		
		m_sDockerTemplatePath = sDockerTemplatePath;		
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += "python27";		
	}
}
