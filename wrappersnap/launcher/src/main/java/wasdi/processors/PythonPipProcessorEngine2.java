package wasdi.processors;

import java.util.List;

import wasdi.processors.dockerUtils.DockerUtils;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.business.processors.ProcessorTypes;
import wasdi.shared.config.DockerRegistryConfig;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.StringUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Python Pip 2 Engine: creates a WASDI Processor developed in Python-Pip
 * and push the image to the register. To start the application the nodes 
 * will take directly the image from the registry.
 * 
 * @author p.campanella
 *
 */
public class PythonPipProcessorEngine2 extends PipProcessorEngine {
		
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
	
	
	/**
	 * Deploy the processor in WASDI.
	 * The method creates the docker
	 * Then it pushes the image in Nexus
	 */
	@Override
	public boolean deploy(ProcessorParameter oParameter, boolean bFirstDeploy) {
				
		// We read  the registers from the config
		List<DockerRegistryConfig> aoRegisters = WasdiConfig.Current.dockers.getRegisters();
		
		if (aoRegisters == null) {
			WasdiLog.errorLog("PythonPipProcessorEngine2.deploy: registers list is null, return false.");
			return false;
		}
		
		if (aoRegisters.size() == 0) {
			WasdiLog.errorLog("PythonPipProcessorEngine2.deploy: registers list is empty, return false.");
			return false;			
		}
		
		// We do not need to start after the build
		m_bRunAfterDeploy = false;
		// And we work with our main register
		m_sDockerRegistry = aoRegisters.get(0).address;
		
		WasdiLog.debugLog("PythonPipProcessorEngine2.deploy: Docker Registry = " + m_sDockerRegistry);
		
		// Build the image of the docker
		boolean bResult = super.deploy(oParameter, bFirstDeploy);
		
		if (!bResult) {
			// This is not good
			WasdiLog.errorLog("PythonPipProcessorEngine2.deploy: super class deploy returned false. So we stop here.");
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
			WasdiLog.errorLog("PythonPipProcessorEngine2.deploy: Impossible to push the image.");
			return false;
		}
		
		WasdiLog.debugLog("PythonPipProcessorEngine2.deploy: deploy done, image pushed");
		
        return true;
	}	
	
	/**
	 * Force the redeploy of an image
	 */
	@Override
	public boolean redeploy(ProcessorParameter oParameter) {
		
		if (!WasdiConfig.Current.isMainNode()) {
			WasdiLog.infoLog("PythonPipProcessorEngine2.redeploy: redeploy for this processor is done only on the main node");
			return true;			
		}
		
		// We read  the registers from the config
		List<DockerRegistryConfig> aoRegisters = WasdiConfig.Current.dockers.getRegisters();
		
		if (aoRegisters == null) {
			WasdiLog.errorLog("PythonPipProcessorEngine2.redeploy: registers list is null, return false.");
			return false;
		}
		
		if (aoRegisters.size() == 0) {
			WasdiLog.errorLog("PythonPipProcessorEngine2.redeploy: registers list is empty, return false.");
			return false;			
		}		
		
		// We do not need to start after the build
		m_bRunAfterDeploy = false;
		// And we work with our main register
		m_sDockerRegistry = aoRegisters.get(0).address;
		
		// Get Processor Name and Id
		String sProcessorId = oParameter.getProcessorID();		
		
		// Read the processor from the db
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
		
		WasdiLog.infoLog("PythonPipProcessorEngine2.redeploy: delete run script. It will be recreated at the right moment");
		String sProcFolder = PathsConfig.getProcessorFolder(oProcessor);
		WasdiFileUtils.deleteFile(sProcFolder+"runwasdidocker.sh");

		// Create utils
        DockerUtils oDockerUtils = new DockerUtils(oProcessor, PathsConfig.getProcessorFolder(oProcessor), m_sTomcatUser, m_sDockerRegistry);
        
        if (oDockerUtils.isContainerStarted(oProcessor.getName(), oProcessor.getVersion())) {
        	WasdiLog.debugLog("PythonPipProcessorEngine2.redeploy: There is the previous version running, stop it");
        	boolean bStop = oDockerUtils.stop(oProcessor);
        	
        	if (!bStop) {
        		WasdiLog.debugLog("PythonPipProcessorEngine2.redeploy: stop returned false, we try to proceed anyhow");
        	}
        }
        
        
		// Increment the version of the processor
		String sNewVersion = oProcessor.getVersion();
		sNewVersion = StringUtils.incrementIntegerString(sNewVersion);
		oProcessor.setVersion(sNewVersion);
		
		// Save it
		oProcessorRepository.updateProcessor(oProcessor);
        
		WasdiLog.debugLog("PythonPipProcessorEngine2.redeploy: call base class deploy. New Version " + sNewVersion);
		
		boolean bResult = super.redeploy(oParameter, false);
		
		if (!bResult) {
			// This is not good
			WasdiLog.errorLog("PythonPipProcessorEngine2.redeploy: super class deploy returned false. So we stop here.");
			return false;
		}
						
		// Here we save the address of the image
		String sPushedImageAddress = pushImageInRegisters(oProcessor);
		
		if (Utils.isNullOrEmpty(sPushedImageAddress)) {
			WasdiLog.infoLog("PythonPipProcessorEngine2.redeploy: we got an empty address of the pushed image");
		}
		
        return true;
	}
	
	@Override
	public boolean run(ProcessorParameter oParameter) {
        WasdiLog.debugLog("PythonPipProcessorEngine2.run: start");

        if (oParameter == null) {
            WasdiLog.errorLog("PythonPipProcessorEngine2.run: parameter is null");
            return false;
        }
        
		// We read  the registers from the config
		List<DockerRegistryConfig> aoRegisters = WasdiConfig.Current.dockers.getRegisters();
		
		if (aoRegisters == null) {
			WasdiLog.errorLog("PythonPipProcessorEngine2.run: registers list is null, return false.");
			return false;
		}
		
		if (aoRegisters.size() == 0) {
			WasdiLog.errorLog("PythonPipProcessorEngine2.run: registers list is empty, return false.");
			return false;			
		}
		
		// And we work with our main register
		m_sDockerRegistry = aoRegisters.get(0).address;
		
		WasdiLog.debugLog("PythonPipProcessorEngine2.run: Docker Registry " + m_sDockerRegistry);
		
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(oParameter.getProcessorID());
		
		DockerUtils oDockerUtils = new DockerUtils(oProcessor, m_sDockerTemplatePath, m_sTomcatUser);
		
		if (!oDockerUtils.isContainerStarted(oProcessor)) {
			
			WasdiLog.debugLog("PythonPipProcessorEngine2.run: the processor is not started. Check if there are old versions up [Actual Version = " + oProcessor.getVersion() + "]");
			
			try {
				String sVersion = oProcessor.getVersion();
				Integer iVersion = Integer.parseInt(sVersion);
				
				while (iVersion>1) {
					iVersion = iVersion - 1;
					
					if (oDockerUtils.isContainerStarted(oProcessor.getName(), "" + iVersion)) {
						WasdiLog.debugLog("PythonPipProcessorEngine2.run: found version " + iVersion + " running, stop it");
						oDockerUtils.stop(oProcessor.getName(), "" + iVersion);
						break;
					}
				}
			}
			catch (Exception oEx) {
				WasdiLog.errorLog("PythonPipProcessorEngine2.run error searching old versions: ", oEx);
			}
		}

        return super.run(oParameter, false);
	}
	
	@Override
	public void waitForApplicationToStart(ProcessorParameter oParameter) {
		
		try {
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessor(oParameter.getProcessorID());
			
			DockerUtils oDockerUtils = new DockerUtils(oProcessor, PathsConfig.getProcessorFolder(oProcessor), m_sTomcatUser, m_sDockerRegistry);
			
	        WasdiLog.debugLog("PythonPipProcessorEngine2.waitForApplicationToStart: wait to let docker start");

	        Integer iNumberOfAttemptsToPingTheServer = WasdiConfig.Current.dockers.numberOfAttemptsToPingTheServer;
	        Integer iMillisBetweenAttmpts = WasdiConfig.Current.dockers.millisBetweenAttmpts;

	        for (int i = 0; i < iNumberOfAttemptsToPingTheServer; i++) {
	        	
	        	Thread.sleep(iMillisBetweenAttmpts);
	        	
	        	if (oDockerUtils.isContainerStarted(oProcessor.getName(), oProcessor.getVersion())) {
	        		return;
	        	}
	        }
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("PythonPipProcessorEngine2.waitForApplicationToStart: exception " + oEx.toString());
		}		
	}
	
	@Override
	public boolean libraryUpdate(ProcessorParameter oParameter) {
		
		if (WasdiConfig.Current.isMainNode()) {
			WasdiLog.debugLog("PythonPipProcessorEngine2.libraryUpdate:  for this processor we force a redeploy for lib update");
			return redeploy(oParameter);			
		}
		else {
			WasdiLog.debugLog("PythonPipProcessorEngine2.libraryUpdate:  we are not the main node, nothing to do");
			return true;			
		}
		
	}
	
	@Override
	public boolean environmentUpdate(ProcessorParameter oParameter) {
		
		// We read  the registers from the config
		List<DockerRegistryConfig> aoRegisters = WasdiConfig.Current.dockers.getRegisters();
		
		if (aoRegisters == null) {
			WasdiLog.errorLog("PythonPipProcessorEngine2.environmentUpdate: registers list is null, return false.");
			return false;
		}
		
		if (aoRegisters.size() == 0) {
			WasdiLog.errorLog("PythonPipProcessorEngine2.environmentUpdate: registers list is empty, return false.");
			return false;			
		}
		
		// And we work with our main register
		m_sDockerRegistry = aoRegisters.get(0).address;
		
		WasdiLog.debugLog("PythonPipProcessorEngine2.environmentUpdate: Docker Registry = " + m_sDockerRegistry);
		WasdiLog.debugLog("PythonPipProcessorEngine2.environmentUpdate: call base class environmentUpdate");

		return super.environmentUpdate(oParameter);
	}
	
	@Override
	public boolean refreshPackagesInfo(ProcessorParameter oParameter) {
		// We read  the registers from the config
		List<DockerRegistryConfig> aoRegisters = WasdiConfig.Current.dockers.getRegisters();
		
		if (aoRegisters == null) {
			WasdiLog.errorLog("PythonPipProcessorEngine2.refreshPackagesInfo: registers list is null, return false.");
			return false;
		}
		
		if (aoRegisters.size() == 0) {
			WasdiLog.errorLog("PythonPipProcessorEngine2.refreshPackagesInfo: registers list is empty, return false.");
			return false;			
		}
		
		// And we work with our main register
		m_sDockerRegistry = aoRegisters.get(0).address;
		
		WasdiLog.debugLog("PythonPipProcessorEngine2.refreshPackagesInfo: Docker Registry = " + m_sDockerRegistry);
		WasdiLog.debugLog("PythonPipProcessorEngine2.refreshPackagesInfo: call base class refreshPackagesInfo");
		
		return super.refreshPackagesInfo(oParameter);
	}
	
	@Override
	public boolean delete(ProcessorParameter oParameter) {
		
		// We read  the registers from the config
		List<DockerRegistryConfig> aoRegisters = WasdiConfig.Current.dockers.getRegisters();
		
		if (aoRegisters == null) {
			WasdiLog.errorLog("PythonPipProcessorEngine2.delete: registers list is null, return false.");
			return false;
		}
		
		if (aoRegisters.size() == 0) {
			WasdiLog.errorLog("PythonPipProcessorEngine2.delete: registers list is empty, return false.");
			return false;			
		}
		
		
		WasdiLog.debugLog("PythonPipProcessorEngine2.delete: Registry set, call base class delete.");
		
		// And we work with our main register
		m_sDockerRegistry = aoRegisters.get(0).address;	
		
		return super.delete(oParameter);
	}
	

}
