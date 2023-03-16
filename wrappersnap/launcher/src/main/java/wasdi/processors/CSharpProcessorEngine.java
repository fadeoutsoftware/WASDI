package wasdi.processors;

import wasdi.shared.business.ProcessorTypes;
import wasdi.shared.managers.IPackageManager;

/**
 * Processor Engine dedicated to a C# Application
 * 
 * @author p.campanella
 *
 */
public class CSharpProcessorEngine extends DockerProcessorEngine {
	
	public CSharpProcessorEngine() {
		super();
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += ProcessorTypes.getTemplateFolder(ProcessorTypes.CSHARP);
		
	}

	@Override
	protected IPackageManager getPackageManager(String sIp, int iPort) {
		throw new UnsupportedOperationException("The functionality is not yet implemented for this processor engine!");
	}

}
