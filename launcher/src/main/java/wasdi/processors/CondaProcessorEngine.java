package wasdi.processors;

import wasdi.shared.business.processors.ProcessorTypes;
import wasdi.shared.packagemanagers.CondaOneShotPackageManager;
import wasdi.shared.packagemanagers.IPackageManager;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Processor Engine dedicated to a python Conda Application, run as a one-shot container
 * @author p.campanella
 *
 */
public class CondaProcessorEngine extends OneShotProcessorEngine {
	
	public CondaProcessorEngine() {
		super();
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += ProcessorTypes.getTemplateFolder(ProcessorTypes.CONDA);		
	}
	
	@Override
	protected IPackageManager getPackageManager(String sUrl) {
		IPackageManager oPackageManager = new CondaOneShotPackageManager(sUrl);

		return oPackageManager;
	}
	
	/**
	 * Package add/remove via env.yml is not implemented yet: refreshPackagesInfo (read-only) already works via the executor.
	 */
	@Override
	public boolean environmentUpdate(ProcessorParameter oParameter) {
		WasdiLog.warnLog("CondaProcessorEngine.environmentUpdate: not supported yet for the Conda one-shot engine");
		return false;
	}
}
