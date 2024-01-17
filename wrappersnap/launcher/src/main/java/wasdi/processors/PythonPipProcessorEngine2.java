package wasdi.processors;

import wasdi.shared.business.processors.ProcessorTypes;

/**
 * Python Pip 2 Engine: creates a WASDI Processor developed in Python-Pip
 * and push the image to the register. To start the application the nodes 
 * will take directly the image from the registry.
 * 
 * @author p.campanella
 *
 */
public class PythonPipProcessorEngine2 extends DockerBuildOnceEngine {
		
	/**
	 * Default constructor
	 */
	public PythonPipProcessorEngine2() {
		super();
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += ProcessorTypes.getTemplateFolder(ProcessorTypes.PYTHON_PIP_2);
		
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
