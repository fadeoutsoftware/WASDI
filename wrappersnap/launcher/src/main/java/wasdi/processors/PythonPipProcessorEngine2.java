package wasdi.processors;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import wasdi.LauncherMain;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Processor;
import wasdi.shared.business.ProcessorTypes;
import wasdi.shared.config.DockerRegistryConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.StringUtils;
import wasdi.shared.utils.Utils;
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
	}
	
	
	/**
	 * Deploy the processor in ADES.
	 * The method creates the docker in "single run mode".
	 * Then it pushes the image in Nexus
	 * Then creates the CWL and the body to post to the ades api to deploy a processor.
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
		
		WasdiLog.debugLog("PythonPipProcessorEngine2.deploy: call base class deploy");
		
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
		
		WasdiLog.debugLog("PythonPipProcessorEngine2.redeploy: call base class deploy");
		
		// We do not need to start after the build
		m_bRunAfterDeploy = false;
		// And we work with our main register
		m_sDockerRegistry = aoRegisters.get(0).address;
		
		// Get Processor Name and Id
		String sProcessorId = oParameter.getProcessorID();		
		
		// Read the processor from the db
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
		
		// Increment the version of the processor
		String sNewVersion = oProcessor.getVersion();
		sNewVersion = StringUtils.incrementIntegerString(sNewVersion);
		oProcessor.setVersion(sNewVersion);
		// Save it
		oProcessorRepository.updateProcessor(oProcessor);
		
		boolean bResult = super.redeploy(oParameter);
		
		if (!bResult) {
			// This is not good
			WasdiLog.errorLog("PythonPipProcessorEngine2.redeploy: super class deploy returned false. So we stop here.");
			return false;
		}
						
		// Here we save the address of the image
		String sPushedImageAddress = pushImageInRegisters(oProcessor);
		
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
			WasdiLog.errorLog("PythonPipProcessorEngine2.deploy: registers list is null, return false.");
			return false;
		}
		
		if (aoRegisters.size() == 0) {
			WasdiLog.errorLog("PythonPipProcessorEngine2.deploy: registers list is empty, return false.");
			return false;			
		}
		
		// And we work with our main register
		m_sDockerRegistry = aoRegisters.get(0).address;
		
		WasdiLog.debugLog("PythonPipProcessorEngine2.deploy: Docker Registry " + m_sDockerRegistry);

        return super.run(oParameter, false);
	}
	

}
