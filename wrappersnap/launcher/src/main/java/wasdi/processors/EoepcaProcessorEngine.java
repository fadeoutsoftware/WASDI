package wasdi.processors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import wasdi.LauncherMain;
import wasdi.shared.business.Processor;
import wasdi.shared.config.DockerRegistryConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.managers.IPackageManager;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.jinja.JinjaTemplateRenderer;

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
				
		// We read  the registers from the config
		List<DockerRegistryConfig> aoRegisters = WasdiConfig.Current.dockers.eoepca.getRegisters();
		
		if (aoRegisters == null) {
			LauncherMain.s_oLogger.error("EoepcaProcessorEngine.deploy: registers list is null, return false.");
			return false;
		}
		
		if (aoRegisters.size() == 0) {
			LauncherMain.s_oLogger.error("EoepcaProcessorEngine.deploy: registers list is empty, return false.");
			return false;			
		}
		
		LauncherMain.s_oLogger.debug("EoepcaProcessorEngine.deploy: call base class deploy");
		
		// For EOPCA we are going to run the app not on our server, so we do not need the tomcat user
		m_sTomcatUser = "";
		// And we do not need to start after the build
		m_bRunAfterDeploy = false;
		// And we work with our main register
		m_sDockerRegistry = aoRegisters.get(0).address;
		
		// Build the image of the docker
		boolean bResult = super.deploy(oParameter, bFirstDeploy);
		
		if (!bResult) {
			// This is not good
			LauncherMain.s_oLogger.error("EoepcaProcessorEngine.deploy: super class deploy returned false. So we stop here.");
			return false;
		}
		
		// Get Processor Name and Id
		String sProcessorId = oParameter.getProcessorID();
		String sProcessorName = oParameter.getName();
		
		// Read the processor from the db
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
		
		// And get the processor folder
		String sProcessorFolder = getProcessorFolder(sProcessorName);
		
		
		DockerUtils oDockerUtils = new DockerUtils(oProcessor, sProcessorFolder, m_sWorkingRootPath, "");
		
		// Here we keep track of how many registers we tried
		int iAvailableRegisters=0;
		// Here we save the address of the image
		String sPushedImageAddress = "";
		
		// For each register: ordered by priority
		for (; iAvailableRegisters<aoRegisters.size(); iAvailableRegisters++) {
			
			DockerRegistryConfig oDockerRegistryConfig = aoRegisters.get(iAvailableRegisters);
			
			LauncherMain.s_oLogger.debug("EoepcaProcessorEngine.deploy: try to push to " + oDockerRegistryConfig.id);
			
			// Try to login and push
			sPushedImageAddress = loginAndPush(oDockerUtils, oDockerRegistryConfig, m_sDockerImageName, sProcessorFolder);
			
			if (!Utils.isNullOrEmpty(sPushedImageAddress)) {
				LauncherMain.s_oLogger.debug("EoepcaProcessorEngine.deploy: image pushed");
				// Ok we got a valid address!
				break;
			}
		}
		
		// Did we used all the avaialbe options?
		if (iAvailableRegisters<aoRegisters.size()) {
			// TODO: push also in other registers. This would be better in a thread
		}
		
		if (Utils.isNullOrEmpty(sPushedImageAddress)) {
			LauncherMain.s_oLogger.error("EoepcaProcessorEngine.deploy: Impossible to push the image.");
			return false;
		}
		
		// Prepare the args for the j2 template
		String sAppParametersDeclaration = "";
		String sAppParametersAsArgs = "";
		
		// Get the parameters json sample
		String sEncodedJson= oProcessor.getParameterSample();
		String sJsonSample = sEncodedJson;
		
		try {
			sJsonSample = java.net.URLDecoder.decode(sEncodedJson, "UTF-8");
		}
		catch (Exception oEx) {
			LauncherMain.s_oLogger.error("EoepcaProcessorEngine.deploy: Impossible to decode the params sample.");
			return false;
		}
				
		
		try {
			// Translate it in a Map
			Map<String,Object> aoProcessorParams = MongoRepository.s_oMapper.readValue(sJsonSample, Map.class);
			
			// For each parameter
			for (String sKey : aoProcessorParams.keySet()) {
				// Declare the parameter
				sAppParametersDeclaration += "  " + sKey + ":\n";
				sAppParametersDeclaration += "    type: ";
				
				// Set the type
				Object oValue = aoProcessorParams.get(sKey);
				
				if (oValue instanceof String) {
					sAppParametersDeclaration += "string";
				}
				else if (oValue instanceof Integer) {
					sAppParametersDeclaration += "int";
				}
				else if (oValue instanceof Float) {
					sAppParametersDeclaration += "float";
				}
				else if (oValue instanceof Double) {
					sAppParametersDeclaration += "double";
				}
				
				sAppParametersDeclaration += "\n";
				
				// And prepare also the parameter as arg to the second step
				sAppParametersAsArgs += "        " + sKey +": "+ sKey + "\n";
			}
			
			sAppParametersDeclaration = sAppParametersDeclaration.substring(0, sAppParametersDeclaration.length()-1);
			sAppParametersAsArgs = sAppParametersAsArgs.substring(0, sAppParametersAsArgs.length()-1);
			
		}
		catch (Exception oEx) {
			LauncherMain.s_oLogger.error("EoepcaProcessorEngine.deploy: Exception generating the parameters args " + oEx.toString());
			return false;
		}
		
		LauncherMain.s_oLogger.debug("EoepcaProcessorEngine.deploy: generate csw file");
		
		// We need to generate actual files from params
		JinjaTemplateRenderer oJinjaTemplateRenderer = new JinjaTemplateRenderer();
		
		// Start with CWL
		HashMap<String, Object> aoCWLParameters = new HashMap<>();
		
		// Valorize the parameters
		aoCWLParameters.put("wasdiAppId", sProcessorName);
		aoCWLParameters.put("wasdiAppDescription", oProcessor.getDescription());
		aoCWLParameters.put("wasdiAppParametersDeclaration", sAppParametersDeclaration);
		aoCWLParameters.put("wasdiOutputFolder", WasdiConfig.Current.dockers.eoepca.dockerWriteFolder);
		aoCWLParameters.put("wasdiProcessorImage", sPushedImageAddress);
		
		String sCWLTemplateInput = getProcessorFolder(sProcessorName) + "wasdi-processor.cwl.j2";
		String sCWLTemplateOutput = getProcessorFolder(sProcessorName) + sProcessorName + ".cwl";
		
		// Translate
		boolean bTranslateCSW = oJinjaTemplateRenderer.translate(sCWLTemplateInput, sCWLTemplateOutput, aoCWLParameters);
		
		if (!bTranslateCSW) {
			LauncherMain.s_oLogger.error("EoepcaProcessorEngine.deploy: error translating CSW template");
			return false;		
		}
		
		// Generate the body of the descriptor to deploy
		LauncherMain.s_oLogger.debug("EoepcaProcessorEngine.deploy: generate app body deploy json file");
		
		String sBodyTemplateInput = getProcessorFolder(sProcessorName) + "appDeployBody.json.j2";
		String sBodyTemplateOutput = getProcessorFolder(sProcessorName) + "appDeployBody.json";
		
		HashMap<String, Object> aoBodyParameters = new HashMap<>();
		String sCWLLink = WasdiConfig.Current.baseUrl + "processors/getcwl?processorName=" + sProcessorName;
		aoBodyParameters.put("cwlLink", sCWLLink);
		
		boolean bTranslateBody = oJinjaTemplateRenderer.translate(sBodyTemplateInput, sBodyTemplateOutput, aoBodyParameters);
		
		if (!bTranslateBody) {
			LauncherMain.s_oLogger.error("EoepcaProcessorEngine.deploy: error translating Body json template");
			return false;		
		}
		
		WasdiFileUtils.deleteFile(sCWLTemplateInput);
		WasdiFileUtils.deleteFile(sBodyTemplateInput);
		
        return true;
	}
	
	/**
	 * Log in and Push an image to a Docker Registry
	 * @param oDockerUtils
	 * @param oDockerRegistryConfig
	 * @param sImageName
	 * @return
	 */
	protected String loginAndPush(DockerUtils oDockerUtils, DockerRegistryConfig oDockerRegistryConfig, String sImageName, String sFolder) {
		try {
			boolean bLogged = oDockerUtils.login(oDockerRegistryConfig.address, oDockerRegistryConfig.user, oDockerRegistryConfig.password, sFolder);
			
			if (!bLogged) {
				LauncherMain.s_oLogger.debug("EoepcaProcessorEngine.loginAndPush: error logging in, return false.");
				return "";
			}
			
			boolean bPushed = oDockerUtils.push(oDockerRegistryConfig.address, sImageName);
			
			if (!bPushed) {
				LauncherMain.s_oLogger.debug("EoepcaProcessorEngine.loginAndPush: error in push, return false.");
				return "";				
			}
			
			return sImageName;
		}
		catch (Exception oEx) {
			LauncherMain.s_oLogger.debug("EoepcaProcessorEngine.loginAndPush: Exception " + oEx.toString());
		}
		
		return "";
	}

	@Override
	public boolean run(ProcessorParameter oParameter) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delete(ProcessorParameter oParameter) {
		boolean bDeleted = super.delete(oParameter);
		
		// TODO: remove the image. Remove the csw. Undeploy from ADES?
		
		return bDeleted;
	}

	@Override
	public boolean redeploy(ProcessorParameter oParameter) {
		boolean bRes = super.redeploy(oParameter);
		
		return bRes;
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
