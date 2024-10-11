package wasdi.processors;

import wasdi.shared.business.processors.ProcessorTypes;

public class Java17Ubuntu22ProcessorEngine extends DockerBuildOnceEngine {
	
	public Java17Ubuntu22ProcessorEngine() {
		super();
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += ProcessorTypes.getTemplateFolder(ProcessorTypes.JAVA_17_UBUNTU_22);
		
		m_asDockerTemplatePackages = new String[8];
		m_asDockerTemplatePackages[0] = "flask";
		m_asDockerTemplatePackages[1] = "gunicorn";
		m_asDockerTemplatePackages[2] = "requests";
		m_asDockerTemplatePackages[3] = "numpy";
		m_asDockerTemplatePackages[4] = "wheel";
		m_asDockerTemplatePackages[5] = "wasdi";
		m_asDockerTemplatePackages[6] = "time";
		m_asDockerTemplatePackages[7] = "datetime";
		
	}

}
