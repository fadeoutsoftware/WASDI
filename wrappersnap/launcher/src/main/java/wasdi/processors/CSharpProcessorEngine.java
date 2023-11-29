package wasdi.processors;

import wasdi.shared.business.processors.ProcessorTypes;
import wasdi.shared.packagemanagers.IPackageManager;

/**
 * Processor Engine dedicated to a C# Application
 * 
 * @author p.campanella
 *
 */
public class CSharpProcessorEngine extends DockerBuildOnceEngine {
	
	public CSharpProcessorEngine() {
		super();
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += ProcessorTypes.getTemplateFolder(ProcessorTypes.CSHARP);
		
	}

	@Override
	protected IPackageManager getPackageManager(String sUrl) {
		return null;
	}

}
