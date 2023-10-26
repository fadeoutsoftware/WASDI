package wasdi.processors;

import java.util.List;

import wasdi.shared.business.processors.Processor;
import wasdi.shared.business.processors.ProcessorTypes;
import wasdi.shared.config.DockerRegistryConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.packagemanagers.IPackageManager;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class PipOneShotProcessorEngine extends DockerProcessorEngine {
	
	public PipOneShotProcessorEngine() {
		super();
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += ProcessorTypes.getTemplateFolder(ProcessorTypes.PIP_ONESHOT);
		
	}
	
	@Override
	public boolean deploy(ProcessorParameter oParameter, boolean bFirstDeploy) {
		WasdiLog.debugLog("PipOneShotProcessorEngine.deploy");
		
		// We read  the registers from the config
		List<DockerRegistryConfig> aoRegisters = WasdiConfig.Current.dockers.getRegisters();
		
		if (aoRegisters == null) {
			WasdiLog.errorLog("PipOneShotProcessorEngine.deploy: registers list is null, return false.");
			return false;
		}
		
		if (aoRegisters.size() == 0) {
			WasdiLog.errorLog("PipOneShotProcessorEngine.deploy: registers list is empty, return false.");
			return false;			
		}
		
		WasdiLog.debugLog("PipOneShotProcessorEngine.deploy: call base class deploy");
		
		// For EOPCA we are going to run the app not on our server, so we do not need the tomcat user
		//m_sTomcatUser = "";
		// And we do not need to start after the build
		m_bRunAfterDeploy = false;
		// And we work with our main register
		m_sDockerRegistry = aoRegisters.get(0).address;
		
		// Build the image of the docker
		boolean bResult = super.deploy(oParameter, bFirstDeploy);
		
		if (!bResult) {
			// This is not good
			WasdiLog.errorLog("PipOneShotProcessorEngine.deploy: super class deploy returned false. So we stop here.");
			return false;
		}
		
		// Get Processor Name and Id
		String sProcessorId = oParameter.getProcessorID();
		
		// Read the processor from the db
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
		
		// Here we save the address of the image
		String sPushedImageAddress = pushImageInRegisters(oProcessor);
		
		if (Utils.isNullOrEmpty(sPushedImageAddress)) {
			WasdiLog.errorLog("PipOneShotProcessorEngine.deploy: Impossible to push the image.");
			return false;
		}
		
        return true;		
	}
	
	@Override
	public boolean run(ProcessorParameter oParameter) {
		WasdiLog.debugLog("PipOneShotProcessorEngine.run");
		return super.run(oParameter, false);
	}
	

	@Override
	protected IPackageManager getPackageManager(String sIp, int iPort) {
		return null;
	}

}
