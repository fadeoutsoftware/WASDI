package wasdi.processors;

import wasdi.shared.managers.IPackageManager;

public class CSharpProcessorEngine extends DockerProcessorEngine {
	
	public CSharpProcessorEngine() {
		super();
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += "csharp";
		
	}

	@Override
	protected IPackageManager getPackageManager(String sIp, int iPort) {
		throw new UnsupportedOperationException("The functionality is not yet implemented for this processor engine!");
	}

	public CSharpProcessorEngine(String sWorkingRootPath, String sDockerTemplatePath, String sTomcatUser)  {
		super(sWorkingRootPath,sDockerTemplatePath, sTomcatUser);

		m_sDockerTemplatePath = sDockerTemplatePath;		
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += "csharp";
		
	}
}
