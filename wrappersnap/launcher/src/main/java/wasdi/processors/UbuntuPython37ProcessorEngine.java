package wasdi.processors;

import wasdi.shared.business.processors.ProcessorTypes;

public class UbuntuPython37ProcessorEngine extends PipProcessorEngine {
	
	public UbuntuPython37ProcessorEngine() {
		super();
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += ProcessorTypes.getTemplateFolder(ProcessorTypes.UBUNTU_PYTHON37_SNAP);
		
	}
}
