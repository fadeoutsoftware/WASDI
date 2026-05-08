package wasdi.processors;

import wasdi.shared.business.processors.ProcessorTypes;
import wasdi.shared.config.WasdiConfig;

public class Python312Ubuntu24ProcessorEngine extends PipOneShotProcessorEngine {
	
	public Python312Ubuntu24ProcessorEngine() {
		m_sDockerTemplatePath = WasdiConfig.Current.paths.dockerTemplatePath;
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
        m_sDockerTemplatePath += ProcessorTypes.getTemplateFolder(ProcessorTypes.PYTHON312_UBUNTU24);
         
        m_asDockerTemplatePackages = new String[7];
        m_asDockerTemplatePackages[0] = "flask";
        m_asDockerTemplatePackages[1] = "gunicorn";
        m_asDockerTemplatePackages[2] = "requests";
        m_asDockerTemplatePackages[3] = "wheel";
        m_asDockerTemplatePackages[4] = "wasdi";
        m_asDockerTemplatePackages[5] = "time";
        m_asDockerTemplatePackages[6] = "datetime";
    }
}
