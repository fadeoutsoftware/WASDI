package wasdi.processors;

import wasdi.shared.parameters.ProcessorParameter;

public class EoepcaProcessorEngine extends WasdiProcessorEngine {
	
	public EoepcaProcessorEngine(String sWorkingRootPath, String sDockerTemplatePath, String sTomcatUser)  {
		super(sWorkingRootPath,sDockerTemplatePath, sTomcatUser);

		m_sDockerTemplatePath = sDockerTemplatePath;		
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += "eoepca";
		
	}	
	
	public EoepcaProcessorEngine() {
		super();
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += "eoepca";
		
	}	

	@Override
	public boolean deploy(ProcessorParameter oParameter) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean run(ProcessorParameter oParameter) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delete(ProcessorParameter oParameter) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean redeploy(ProcessorParameter oParameter) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean libraryUpdate(ProcessorParameter oParameter) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean environmentUpdate(ProcessorParameter oParameter) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean refreshPackagesInfo(ProcessorParameter oParameter) {
		// TODO Auto-generated method stub
		return false;
	}

}
