package wasdi.processors;

import wasdi.LauncherMain;
import wasdi.shared.business.Processor;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.managers.IPackageManager;
import wasdi.shared.parameters.ProcessorParameter;

public class EoepcaProcessorEngine extends DockerProcessorEngine {
	
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
	public boolean deploy(ProcessorParameter oParameter, boolean bFirstDeploy) {
		// For EOPCA we are going to run the app not on our server, so we do not need the tomcat user
		m_sTomcatUser = "";
		boolean bResult = super.deploy(oParameter, bFirstDeploy);
		
		if (!bResult) {
			LauncherMain.s_oLogger.debug("EoepcaProcessorEngine.DeployProcessor: super class deploy returned false. So we stop here.");
			return false;
		}
		
		String sProcessorId = oParameter.getProcessorID();
		String sProcessorName = oParameter.getName(); 
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
		String sProcessorFolder = getProcessorFolder(sProcessorName);
		
		DockerUtils oDockerUtils = new DockerUtils(oProcessor, sProcessorFolder, m_sWorkingRootPath, "");
		//oDockerUtils.
		
		
        return true;
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

	@Override
	protected IPackageManager getPackageManager(String sIp, int iPort) {
		return null;
	}

}
