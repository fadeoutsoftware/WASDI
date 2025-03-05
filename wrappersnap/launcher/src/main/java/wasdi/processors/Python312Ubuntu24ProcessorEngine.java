package wasdi.processors;

import wasdi.shared.business.processors.ProcessorTypes;

public class Python312Ubuntu24ProcessorEngine extends DockerBuildOnceEngine {
	
	public Python312Ubuntu24ProcessorEngine() {
		super();
		 if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
         m_sDockerTemplatePath += ProcessorTypes.getTemplateFolder(ProcessorTypes.PYTHON312_UBUNTU24);
         
         m_asDockerTemplatePackages = new String[8];
         m_asDockerTemplatePackages[0] = "flask";
         m_asDockerTemplatePackages[1] = "gunicorn";
         m_asDockerTemplatePackages[2] = "requests";
         m_asDockerTemplatePackages[4] = "wheel";
         m_asDockerTemplatePackages[5] = "wasdi";
         m_asDockerTemplatePackages[6] = "time";
         m_asDockerTemplatePackages[7] = "datetime";         
	}
}
