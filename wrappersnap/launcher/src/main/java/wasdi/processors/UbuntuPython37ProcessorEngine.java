package wasdi.processors;

public class UbuntuPython37ProcessorEngine extends PipProcessorEngine {
	
	public UbuntuPython37ProcessorEngine() {
		super();
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += "python37";
		
	}

	public UbuntuPython37ProcessorEngine(String sWorkingRootPath, String sDockerTemplatePath, String sTomcatUser)  {
		super(sWorkingRootPath,sDockerTemplatePath, sTomcatUser);

		m_sDockerTemplatePath = sDockerTemplatePath;		
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += "python37";
		
	}

}
