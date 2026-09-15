package wasdi.processors;

import wasdi.shared.business.processors.ProcessorTypes;
import wasdi.shared.packagemanagers.CondaPackageManagerImpl;
import wasdi.shared.packagemanagers.IPackageManager;

/**
 * Processor Engine dedicated to a python Conda Application
 * @author p.campanella
 *
 */
public class CondaProcessorEngine extends DockerBuildOnceEngine {
	
	public CondaProcessorEngine() {
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += ProcessorTypes.getTemplateFolder(ProcessorTypes.CONDA);		
	}
	
	@Override
	protected IPackageManager getPackageManager(String sUrl) {
		IPackageManager oPackageManager = new CondaPackageManagerImpl(sUrl);

		return oPackageManager;
	}
}
