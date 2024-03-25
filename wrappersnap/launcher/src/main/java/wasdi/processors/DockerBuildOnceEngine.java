package wasdi.processors;

import wasdi.shared.business.processors.Processor;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.StringUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.docker.DockerUtils;
import wasdi.shared.utils.log.WasdiLog;

public class DockerBuildOnceEngine extends PipProcessorEngine {
	
	
	/**
	 * Deploy the processor in WASDI.
	 * The method creates the docker
	 * Then it pushes the image in Nexus
	 */
	@Override
	public boolean deploy(ProcessorParameter oParameter, boolean bFirstDeploy) {
		
		// We do not need to start after the build
		m_bRunAfterDeploy = false;
		// And we work with our main register
		m_sDockerRegistry = getDockerRegisterAddress();
		
		if (Utils.isNullOrEmpty(m_sDockerRegistry)) {
			WasdiLog.errorLog("DockerBuildOnceEngine.deploy: register address not found, return false.");
			return false;			
		}
		
		WasdiLog.debugLog("DockerBuildOnceEngine.deploy: Docker Registry = " + m_sDockerRegistry);
		
		// Build the image of the docker
		boolean bResult = super.deploy(oParameter, bFirstDeploy);
		
		if (!bResult) {
			// This is not good
			WasdiLog.errorLog("DockerBuildOnceEngine.deploy: super class deploy returned false. So we stop here.");
			return false;
		}
		
		// Get Processor Id
		String sProcessorId = oParameter.getProcessorID();
		
		// Read the processor from the db
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
		
		// Here we save the address of the image
		String sPushedImageAddress = pushImageInRegisters(oProcessor);
		
		if (Utils.isNullOrEmpty(sPushedImageAddress)) {
			WasdiLog.errorLog("DockerBuildOnceEngine.deploy: Impossible to push the image.");
			return false;
		}
		
		WasdiLog.debugLog("DockerBuildOnceEngine.deploy: deploy done, image pushed");
		
        return true;
	}	
	
	/**
	 * Force the redeploy of an image
	 */
	@Override
	public boolean redeploy(ProcessorParameter oParameter) {
		
		if (!WasdiConfig.Current.isMainNode()) {
			WasdiLog.infoLog("DockerBuildOnceEngine.redeploy: redeploy for this processor is done only on the main node");
			return true;			
		}
		
		// We do not need to start after the build
		m_bRunAfterDeploy = false;
		// And we work with our main register
		m_sDockerRegistry = getDockerRegisterAddress();
		
		if (Utils.isNullOrEmpty(m_sDockerRegistry)) {
			WasdiLog.errorLog("DockerBuildOnceEngine.redeploy: register address not found, return false.");
			return false;			
		}
		
		// Get Processor Name and Id
		String sProcessorId = oParameter.getProcessorID();		
		
		// Read the processor from the db
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
		
		// Create utils
        DockerUtils oDockerUtils = new DockerUtils(oProcessor, m_oParameter, PathsConfig.getProcessorFolder(oProcessor), m_sDockerRegistry);
        
        if (oDockerUtils.isContainerStarted(oProcessor.getName(), oProcessor.getVersion())) {
        	WasdiLog.debugLog("DockerBuildOnceEngine.redeploy: There is the previous version running, stop it");
        	boolean bStop = oDockerUtils.stop(oProcessor);
        	
        	if (!bStop) {
        		WasdiLog.debugLog("DockerBuildOnceEngine.redeploy: stop returned false, we try to proceed anyhow");
        	}
        }
        
		// Increment the version of the processor
		String sNewVersion = oProcessor.getVersion();
		sNewVersion = StringUtils.incrementIntegerString(sNewVersion);
		oProcessor.setVersion(sNewVersion);
		
		// Save it
		oProcessorRepository.updateProcessor(oProcessor);
        
		WasdiLog.debugLog("DockerBuildOnceEngine.redeploy: call base class deploy. New Version " + sNewVersion);
		
		boolean bResult = super.redeploy(oParameter, false);
		
		if (!bResult) {
			// This is not good
			WasdiLog.errorLog("DockerBuildOnceEngine.redeploy: super class deploy returned false. So we stop here.");
			return false;
		}
		else {
			WasdiLog.errorLog("DockerBuildOnceEngine.redeploy: super class deploy returned true lets proceed to clean and push");
		}
				
		try {
			WasdiLog.errorLog("DockerBuildOnceEngine.redeploy: try to clean old images. Last valid version is " + sNewVersion);
			
			String sVersion = oProcessor.getVersion();
			Integer iVersion = Integer.parseInt(sVersion);
			
			if (iVersion>1) {
				iVersion = iVersion - 1;				
				oDockerUtils.delete(oProcessor.getName(), "" + iVersion);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("DockerBuildOnceEngine.run error searching old versions: ", oEx);
		}
		
		// Here we save the address of the image
		String sPushedImageAddress = pushImageInRegisters(oProcessor);
		
		if (Utils.isNullOrEmpty(sPushedImageAddress)) {
			WasdiLog.infoLog("DockerBuildOnceEngine.redeploy: we got an empty address of the pushed image");
			return false;
		}		
		
        return true;
	}
	
	@Override
	public boolean run(ProcessorParameter oParameter) {
        WasdiLog.debugLog("DockerBuildOnceEngine.run: start");

        if (oParameter == null) {
            WasdiLog.errorLog("DockerBuildOnceEngine.run: parameter is null");
            return false;
        }
		
		// And we work with our main register
		m_sDockerRegistry = getDockerRegisterAddress();
		
		if (Utils.isNullOrEmpty(m_sDockerRegistry)) {
			WasdiLog.errorLog("DockerBuildOnceEngine.run: register address not found, return false.");
			return false;			
		}
		
		WasdiLog.debugLog("DockerBuildOnceEngine.run: Docker Registry " + m_sDockerRegistry);
		
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(oParameter.getProcessorID());
		
		DockerUtils oDockerUtils = new DockerUtils(oProcessor, m_oParameter, m_sDockerTemplatePath, m_sDockerRegistry);
		
		if (!oDockerUtils.isContainerStarted(oProcessor)) {
			
			WasdiLog.debugLog("DockerBuildOnceEngine.run: the processor is not started. Check if there are old versions up [Actual Version = " + oProcessor.getVersion() + "]");
			
			try {
				String sVersion = oProcessor.getVersion();
				Integer iVersion = Integer.parseInt(sVersion);
				
				if (iVersion>1) {
					iVersion = iVersion - 1;
					oDockerUtils.delete(oProcessor.getName(), ""+iVersion);
				}
			}
			catch (Exception oEx) {
				WasdiLog.errorLog("DockerBuildOnceEngine.run error searching old versions: ", oEx);
			}
		}

        return super.run(oParameter, false);
	}
	
	@Override
	public void waitForApplicationToStart(ProcessorParameter oParameter) {
		
		try {
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessor(oParameter.getProcessorID());
			
			DockerUtils oDockerUtils = new DockerUtils(oProcessor, m_oParameter, PathsConfig.getProcessorFolder(oProcessor), m_sDockerRegistry);
			
	        WasdiLog.debugLog("DockerBuildOnceEngine.waitForApplicationToStart: wait to let docker start");

	        Integer iNumberOfAttemptsToPingTheServer = WasdiConfig.Current.dockers.numberOfAttemptsToPingTheServer;
	        Integer iMillisBetweenAttmpts = WasdiConfig.Current.dockers.millisBetweenAttmpts;

	        for (int i = 0; i < iNumberOfAttemptsToPingTheServer; i++) {
	        	
	        	Thread.sleep(iMillisBetweenAttmpts);
	        	
	        	if (oDockerUtils.isContainerStarted(oProcessor.getName(), oProcessor.getVersion())) {
	        		return;
	        	}
	        }
		}
		catch(InterruptedException oEx) {
			Thread.currentThread().interrupt();
    		WasdiLog.errorLog("DockerBuildOnceEngine.waitForApplicationToStart: current thread intrrupted", oEx);
    	}
		catch (Exception oEx) {
			WasdiLog.errorLog("DockerBuildOnceEngine.waitForApplicationToStart: exception ", oEx);
		}		
	}
	
	@Override
	public boolean libraryUpdate(ProcessorParameter oParameter) {
		
		if (WasdiConfig.Current.isMainNode()) {
			WasdiLog.debugLog("DockerBuildOnceEngine.libraryUpdate:  for this processor we force a redeploy for lib update");
			return redeploy(oParameter);			
		}
		else {
			WasdiLog.debugLog("DockerBuildOnceEngine.libraryUpdate:  we are not the main node, nothing to do");
			return true;			
		}		
	}
	
	@Override
	public boolean environmentUpdate(ProcessorParameter oParameter) {
		
		// And we work with our main register
		m_sDockerRegistry = getDockerRegisterAddress();
		
		if (Utils.isNullOrEmpty(m_sDockerRegistry)) {
			WasdiLog.errorLog("DockerBuildOnceEngine.environmentUpdate: register address not found, return false.");
			return false;			
		}
		
		WasdiLog.debugLog("DockerBuildOnceEngine.environmentUpdate: Docker Registry = " + m_sDockerRegistry);
		WasdiLog.debugLog("DockerBuildOnceEngine.environmentUpdate: call base class environmentUpdate");

		return super.environmentUpdate(oParameter);
	}
	
	@Override
	public boolean refreshPackagesInfo(ProcessorParameter oParameter) {
		
		// And we work with our main register
		m_sDockerRegistry = getDockerRegisterAddress();
		
		if (Utils.isNullOrEmpty(m_sDockerRegistry)) {
			WasdiLog.errorLog("DockerBuildOnceEngine.environmentUpdate: register address not found, return false.");
			return false;			
		}
		
		WasdiLog.debugLog("DockerBuildOnceEngine.refreshPackagesInfo: Docker Registry = " + m_sDockerRegistry);
		WasdiLog.debugLog("DockerBuildOnceEngine.refreshPackagesInfo: call base class refreshPackagesInfo");
		
		return super.refreshPackagesInfo(oParameter);
	}
	
	@Override
	public boolean delete(ProcessorParameter oParameter) {
		
		// And we work with our main register
		m_sDockerRegistry = getDockerRegisterAddress();
		
		if (Utils.isNullOrEmpty(m_sDockerRegistry)) {
			WasdiLog.errorLog("DockerBuildOnceEngine.delete: register address not found, return false.");
			return false;			
		}
		
		return super.delete(oParameter);
	}	

}
