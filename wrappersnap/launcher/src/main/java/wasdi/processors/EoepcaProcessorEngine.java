package wasdi.processors;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import wasdi.LauncherMain;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Processor;
import wasdi.shared.business.ProcessorTypes;
import wasdi.shared.config.DockerRegistryConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.packagemanagers.IPackageManager;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.OgcProcessesClient;
import wasdi.shared.utils.StringUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.jinja.JinjaTemplateRenderer;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.ogcprocesses.Execute;
import wasdi.shared.viewmodels.ogcprocesses.StatusCode;
import wasdi.shared.viewmodels.ogcprocesses.StatusInfo;

/**
 * EOEPCA Processor Engine.
 * This processor engine is designed to deploy and run applications in
 * and EOEPCA reference architecture server installation. 
 * 
 * @author p.campanella
 *
 */
public class EoepcaProcessorEngine extends DockerProcessorEngine {
		
	/**
	 * Default constructor
	 */
	public EoepcaProcessorEngine() {
		super();
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += ProcessorTypes.getTemplateFolder(ProcessorTypes.EOEPCA);
		
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
			WasdiLog.errorLog("EoepcaProcessorEngine.deploy: registers list is null, return false.");
			return false;
		}
		
		if (aoRegisters.size() == 0) {
			WasdiLog.errorLog("EoepcaProcessorEngine.deploy: registers list is empty, return false.");
			return false;			
		}
		
		WasdiLog.debugLog("EoepcaProcessorEngine.deploy: call base class deploy");
		
		// For EOPCA we are going to run the app not on our server, so we do not need the tomcat user
		m_sTomcatUser = "";
		// And we do not need to start after the build
		m_bRunAfterDeploy = false;
		// And we work with our main register
		m_sDockerRegistry = aoRegisters.get(0).address;
		
		// Build the image of the docker
		boolean bResult = super.deploy(oParameter, bFirstDeploy);
		//boolean bResult = true;
		
		if (!bResult) {
			// This is not good
			WasdiLog.errorLog("EoepcaProcessorEngine.deploy: super class deploy returned false. So we stop here.");
			return false;
		}
		
		// Get Processor Name and Id
		String sProcessorId = oParameter.getProcessorID();
		
		// Read the processor from the db
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
		
		// Here we save the address of the image
		String sPushedImageAddress = pushImageInRegisters(oProcessor);
		//String sPushedImageAddress = "ImageAddress";
		
		if (Utils.isNullOrEmpty(sPushedImageAddress)) {
			WasdiLog.errorLog("EoepcaProcessorEngine.deploy: Impossible to push the image.");
			return false;
		}
		
		// Prepare the args for the j2 template
		List<Map<String,Object>> aoProcessorParameters = getParametersInputDescription(oProcessor);

		if (aoProcessorParameters == null) {
			WasdiLog.errorLog("EoepcaProcessorEngine.deploy: empty args, not good");
			return false;
		}
		
		// Render the template for CWL
		
		boolean bTemplates = renderCWLTemplates(oProcessor, aoProcessorParameters);
		
		if (!bTemplates) {
			WasdiLog.errorLog("EoepcaProcessorEngine.deploy: problems rendering the template");
			return false;
		}
		
		// Now we need to post: start from reading the appDeployBody.json file
		String sDeployBodyFilePath = getProcessorFolder(oProcessor) + "appDeployBody.json";

		
		String sDeployBody = WasdiFileUtils.fileToText(sDeployBodyFilePath);
		
		if (Utils.isNullOrEmpty(sDeployBody)) {
			WasdiLog.errorLog("EoepcaProcessorEngine.deploy: appDeployBody empty or not found at path " + sDeployBodyFilePath);
			return false;
		}
		
		String sBaseAddress = WasdiConfig.Current.dockers.eoepca.adesServerAddress;
		
		if (!sBaseAddress.endsWith("/")) sBaseAddress += "/";
		
		if (!Utils.isNullOrEmpty(WasdiConfig.Current.dockers.eoepca.user)) {
			sBaseAddress += WasdiConfig.Current.dockers.eoepca.user + "/";
		}
		
		sBaseAddress += "wps3/";
		
		OgcProcessesClient oOgcProcessesClient = new OgcProcessesClient(sBaseAddress);
		
		// Login
		if (loginInEOEpca(oOgcProcessesClient)) {
			// Call the deploy function: is a post of the App Deploy Body
			boolean bApiAnswer = oOgcProcessesClient.deployProcess(sDeployBody);
	        return bApiAnswer;			
		}
		else {
			WasdiLog.debugLog("EoepcaProcessorEngine.deploy: error logging in Eoepca Server"); 
			return false;
		}		
	}	
	
	/**
	 * Logs in the EOEPCA server.
	 * It adds the token headers to the OgcProcessesClient object
	 * 
	 * @param oOgcProcessesClient OgcProcessesClient object to initialize
	 */
	protected boolean loginInEOEpca(OgcProcessesClient oOgcProcessesClient) {
		
		if (oOgcProcessesClient==null) return true;
		
		try {
			// Is this istance under authentication?		
			if (!Utils.isNullOrEmpty(WasdiConfig.Current.dockers.eoepca.user) && !Utils.isNullOrEmpty(WasdiConfig.Current.dockers.eoepca.password)) {
				// Authenticate to the eoepca installation
				String sScope = "openid user_name is_operator";
				
				Map<String, String> asNoCacheHeaders = new HashMap<>();
				asNoCacheHeaders.put("Cache-Control", "no-cache");
				asNoCacheHeaders.put("Accept", "application/json");
				
				WasdiLog.debugLog("EoepcaProcessorEngin.loginInEOEpca: calling obtainOpenidConnectToken");
				
				// We need an openId Connection Token
				String sToken = HttpUtils.obtainOpenidConnectToken(WasdiConfig.Current.dockers.eoepca.authServerAddress, WasdiConfig.Current.dockers.eoepca.user, WasdiConfig.Current.dockers.eoepca.password
						, WasdiConfig.Current.dockers.eoepca.clientId, sScope, WasdiConfig.Current.dockers.eoepca.clientSecret, asNoCacheHeaders, "id_token");
				
				// And the relative headers
				Map<String, String> asHeaders = HttpUtils.getOpenIdConnectHeaders(sToken);
				
				// That we inject in all the call to ADES/OGC Processes API
				oOgcProcessesClient.setHeaders(asHeaders);
			}			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("EoepcaProcessorEngine.loginInEOEpca Exception ", oEx);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Renders the CWL (and app body) templates
	 * @param oProcessor Processor
	 * @param sAppParametersDeclaration Description of the parameters
	 * @return true or false
	 */
	protected boolean renderCWLTemplates(Processor oProcessor, List<Map<String,Object>> aoProcessorParameters) {
		WasdiLog.debugLog("EoepcaProcessorEngine.deploy: generate csw file");
		
		String sProcessorName = oProcessor.getName();
		
		// We need to generate actual files from params
		JinjaTemplateRenderer oJinjaTemplateRenderer = new JinjaTemplateRenderer();
		
		// Start with CWL
		HashMap<String, Object> aoCWLParameters = new HashMap<>();
		
		// Valorize the parameters
		aoCWLParameters.put("wasdiAppId", sProcessorName);
		aoCWLParameters.put("wasdiAppDescription", oProcessor.getDescription());
		aoCWLParameters.put("wasdiAppParametersDeclaration", aoProcessorParameters);
		aoCWLParameters.put("wasdiOutputFolder", WasdiConfig.Current.dockers.eoepca.dockerWriteFolder);
		aoCWLParameters.put("wasdiProcessorImage", m_sDockerImageName);
		
		String sCWLTemplateInput = getProcessorFolder(sProcessorName) + "wasdi-processor.cwl.j2";
		String sCWLTemplateOutput = getProcessorFolder(sProcessorName) + sProcessorName + ".cwl";
		
		// Translate
		boolean bTranslateCSW = oJinjaTemplateRenderer.translate(sCWLTemplateInput, sCWLTemplateOutput, aoCWLParameters);
		
		if (!bTranslateCSW) {
			WasdiLog.errorLog("EoepcaProcessorEngine.deploy: error translating CSW template");
			return false;		
		}
		
		// Generate the body of the descriptor to deploy
		WasdiLog.debugLog("EoepcaProcessorEngine.deploy: generate app body deploy json file");
		
		String sBodyTemplateInput = getProcessorFolder(sProcessorName) + "appDeployBody.json.j2";
		String sBodyTemplateOutput = getProcessorFolder(sProcessorName) + "appDeployBody.json";
		
		HashMap<String, Object> aoBodyParameters = new HashMap<>();
		String sCWLLink = WasdiConfig.Current.baseUrl + "processors/getcwl?processorName=" + sProcessorName;
		aoBodyParameters.put("cwlLink", sCWLLink);
		
		boolean bTranslateBody = oJinjaTemplateRenderer.translate(sBodyTemplateInput, sBodyTemplateOutput, aoBodyParameters);
		
		if (!bTranslateBody) {
			WasdiLog.errorLog("EoepcaProcessorEngine.deploy: error translating Body json template");
			return false;		
		}
		
		WasdiFileUtils.deleteFile(sCWLTemplateInput);
		WasdiFileUtils.deleteFile(sBodyTemplateInput);
		
		return true;
	}
	
	/**
	 * Translates the WASDI JSON Parameters of this application in
	 * the equivalent text for the CSW Yaml template.
	 * The output can be used to fill the csw j2 template 
	 * @param oProcessor Processor that takes the inputs
	 * @return String with yaml representation of these inputs
	 */
	protected List<Map<String,Object>> getParametersInputDescription(Processor oProcessor) {
		// Prepare the args for the j2 template
		
		// Get the parameters json sample
		String sEncodedJson= oProcessor.getParameterSample();
		String sJsonSample = sEncodedJson;
		
		ArrayList<Map<String,Object>> aoCWLParameters = new ArrayList<Map<String, Object>>();
		
		try {
			sJsonSample = java.net.URLDecoder.decode(sEncodedJson, "UTF-8");
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("EoepcaProcessorEngine.deploy: Impossible to decode the params sample.");
			return null;
		}
				
		
		try {
			// Translate it in a Map
			Map<String,Object> aoProcessorParams = MongoRepository.s_oMapper.readValue(sJsonSample, Map.class);
			
			// For each parameter
			for (String sKey : aoProcessorParams.keySet()) {
				
				Map<String,Object> oOutputParam = new HashMap<String, Object>();
				oOutputParam.put("key", sKey);
				
				String sType = "string";
				
				// Set the type
				Object oValue = aoProcessorParams.get(sKey);
				
				if (oValue instanceof String) {
					sType = "string";
				}
				else if (oValue instanceof Integer) {
					sType = "int";
				}
				else if (oValue instanceof Float) {
					sType = "float";
				}
				else if (oValue instanceof Double) {
					sType = "double";
				}
				
				oOutputParam.put("type", sType);
				
				aoCWLParameters.add(oOutputParam);
			}			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("EoepcaProcessorEngine.deploy: Exception generating the parameters args " + oEx.toString());
			return null;
		}
		
		return aoCWLParameters;
	}	
	
	/**
	 * Translates the WASDI JSON Parameters of this application in
	 * the equivalent text for the CSW Yaml template.
	 * The output can be used to fill the csw j2 template 
	 * @param oProcessor Processor that takes the inputs
	 * @return String with yaml representation of these inputs
	 */
	protected String getParametersInputDescription3(Processor oProcessor) {
		// Prepare the args for the j2 template
		String sAppParametersDeclaration = "";
		
		// Get the parameters json sample
		String sEncodedJson= oProcessor.getParameterSample();
		String sJsonSample = sEncodedJson;
		
		try {
			sJsonSample = java.net.URLDecoder.decode(sEncodedJson, "UTF-8");
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("EoepcaProcessorEngine.deploy: Impossible to decode the params sample.");
			return sAppParametersDeclaration;
		}
				
		
		try {
			// Translate it in a Map
			Map<String,Object> aoProcessorParams = MongoRepository.s_oMapper.readValue(sJsonSample, Map.class);
			
			sAppParametersDeclaration = "[\n";
			
			// For each parameter
			for (String sKey : aoProcessorParams.keySet()) {
				// Declare the parameter
				sAppParametersDeclaration += "\t{\n";
				sAppParametersDeclaration += "\t\t\"key\": \"" + sKey + "\",\n";
				sAppParametersDeclaration += "\t\t\"type\": \"";
				
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
				
				sAppParametersDeclaration += "\"\n},\n";
			}
			
			sAppParametersDeclaration = sAppParametersDeclaration.substring(0, sAppParametersDeclaration.length()-2);
			sAppParametersDeclaration += "\n]";
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("EoepcaProcessorEngine.deploy: Exception generating the parameters args " + oEx.toString());
			return "";
		}
		
		return sAppParametersDeclaration;
	}
	
	protected String getParametersInputDescription2(Processor oProcessor) {
		// Prepare the args for the j2 template
		String sAppParametersDeclaration = "";
		
		// Get the parameters json sample
		String sEncodedJson= oProcessor.getParameterSample();
		String sJsonSample = sEncodedJson;
		
		try {
			sJsonSample = java.net.URLDecoder.decode(sEncodedJson, "UTF-8");
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("EoepcaProcessorEngine.deploy: Impossible to decode the params sample.");
			return sAppParametersDeclaration;
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
			}
			
			sAppParametersDeclaration = sAppParametersDeclaration.substring(0, sAppParametersDeclaration.length()-1);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("EoepcaProcessorEngine.deploy: Exception generating the parameters args " + oEx.toString());
			return "";
		}
		
		return sAppParametersDeclaration;
	}
	
	/**
	 * Executes an EOEPCA Processor
	 */
	@Override
	public boolean run(ProcessorParameter oParameter) {
		
        WasdiLog.debugLog("EoepcaProcessorEngine.run: start");

        if (oParameter == null) {
            WasdiLog.errorLog("EoepcaProcessorEngine.run: parameter is null");
            return false;
        }
        
        // Get Repo and Process Workspace
        ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
        ProcessWorkspace oProcessWorkspace = m_oProcessWorkspace;
        
        ProcessorRepository oProcessorRepository = new ProcessorRepository();
        Processor oProcessor = oProcessorRepository.getProcessor(oParameter.getProcessorID());
        
        if (oProcessor == null) {
            WasdiLog.errorLog("EoepcaProcessorEngine.run: processor is null");
            return false;        	
        }
        
		try {
			String sProcessorFolder = getProcessorFolder(oParameter.getName());
			
			File oProcFolder = new File(sProcessorFolder);
			
//			if (!oProcFolder.exists()) {
//				this.deploy(oParameter);
//			}
			
			String sBaseAddress = WasdiConfig.Current.dockers.eoepca.adesServerAddress;
			
			if (!sBaseAddress.endsWith("/")) sBaseAddress += "/";
			
			if (!Utils.isNullOrEmpty(WasdiConfig.Current.dockers.eoepca.user)) {
				sBaseAddress += WasdiConfig.Current.dockers.eoepca.user + "/";
			}
			
			sBaseAddress += "wps3/";
			
			OgcProcessesClient oOgcProcessesClient = new OgcProcessesClient(sBaseAddress);
			
			// Login
			if (!loginInEOEpca(oOgcProcessesClient)) {
				WasdiLog.debugLog("EoepcaProcessorEngine.run: error logging in Eoepca Server"); 
				return false;
			}
			
			String sJsonParams = oParameter.getJson();
			
			String sDecodedParams = sJsonParams;
			
			try {
			    sDecodedParams = java.net.URLDecoder.decode(sJsonParams, StandardCharsets.UTF_8.name());
			} catch (Exception oEr) {
			    WasdiLog.errorLog("EoepcaProcessorEngine.run: error decoding the Json Parameters ", oEr);
			}
			
			Map<String,Object> aoInputParams= (Map<String, Object>) MongoRepository.s_oMapper.readValue(sDecodedParams, Map.class);
			
			aoInputParams.put("wasdi__ws__id", oParameter.getWorkspace());
			aoInputParams.put("wasdi__session__id", oParameter.getSessionID());
			aoInputParams.put("wasdi__user__id", oParameter.getUserId());
			aoInputParams.put("wasdi__proc__id", oParameter.getProcessObjId());
			
			//Execute
			Execute oExecute = new Execute();
			oExecute.setInputs(aoInputParams);
			
			if (oOgcProcessesClient.getHeaders() != null) {
				oOgcProcessesClient.getHeaders().put("Content-Type", "application/json");
				oOgcProcessesClient.getHeaders().put("Prefer", "respond-async");				
			}
			
			StatusInfo oStatusInfo = oOgcProcessesClient.executeProcess(oParameter.getName()+"-1_0", oExecute);
			String sJobId = oStatusInfo.getJobID();
			
			if (Utils.isNullOrEmpty(sJobId)) {
				WasdiLog.errorLog("EoepcaProcessorEngine.run: unable to get a valid Job Id");
				return false;
			}
			
            long lTimeSpentMs = 0;
            int iThreadSleepMs = 2000;
            boolean bForcedError = false;
            
            
			
			while(true) {
				
				oStatusInfo = oOgcProcessesClient.getStatus(sJobId);
				
				if (oStatusInfo == null) {
					WasdiLog.debugLog("EoepcaProcessorEngine.run: Status Info returned null. Try to login again ");
					
					if (loginInEOEpca(oOgcProcessesClient)) {
						WasdiLog.debugLog("EoepcaProcessorEngine.run: new login done, try again");
						
						oStatusInfo = oOgcProcessesClient.getStatus(sJobId);
						
						if (oStatusInfo == null) {
							WasdiLog.errorLog("EoepcaProcessorEngine.run: Status Info null also after new login");
							break;
						}
					}
					else {
						WasdiLog.debugLog("EoepcaProcessorEngine.run: impossible to login again");
						break;
					}
				}
				
				if (oStatusInfo.getStatus() == StatusCode.DISMISSED || oStatusInfo.getStatus() == StatusCode.FAILED || oStatusInfo.getStatus() == StatusCode.SUCCESSFUL) {
					break;
				}
				
                try {
                    Thread.sleep(iThreadSleepMs);
                } catch (InterruptedException oEx) {
                    WasdiLog.errorLog("EoepcaProcessorEngine.run: Thread sleep exception ", oEx);
                    Thread.currentThread().interrupt();
                }                

                // Increase the time
                lTimeSpentMs += iThreadSleepMs;

                if (oProcessor.getTimeoutMs() > 0) {
                    if (lTimeSpentMs > oProcessor.getTimeoutMs()) {
                        // Timeout
                        WasdiLog.debugLog("EoepcaProcessorEngine.run: Timeout of Processor with ProcId " + oProcessWorkspace.getProcessObjId() + " Time spent [ms] " + lTimeSpentMs);

                        // Update process and rabbit users
                        LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
                        bForcedError = true;
                        break;
                    }
                }				
			}
			
			if (oStatusInfo!=null) {
				ProcessStatus oStatus = ProcessStatus.DONE;
				
				if (oStatusInfo.getStatus() == StatusCode.FAILED) oStatus = ProcessStatus.ERROR;
				if (oStatusInfo.getStatus() == StatusCode.DISMISSED) oStatus = ProcessStatus.STOPPED;
				
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, oStatus, 100);
			}
			
            // Check and set the operation end-date
            if (Utils.isNullOrEmpty(oProcessWorkspace.getOperationEndTimestamp())) {
                oProcessWorkspace.setOperationEndTimestamp(Utils.nowInMillis());
                // P.Campanella 20200115: I think this is to add, but I cannot test it now :( ...
                //LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.valueOf(oProcessWorkspace.getStatus()), oProcessWorkspace.getProgressPerc());
            }			
			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("EoepcaProcessorEngine.run: error " + oEx.toString());
			return false;
		}
		
		return true;
	}
	
	/**
	 * Deletes an EOEPCA Processor
	 */
	@Override
	public boolean delete(ProcessorParameter oParameter) {

		// We read  the registers from the config
		List<DockerRegistryConfig> aoRegisters = WasdiConfig.Current.dockers.getRegisters();
		
		if (aoRegisters == null) {
			WasdiLog.errorLog("EoepcaProcessorEngine.delete: registers list is null, return false.");
			return false;
		}
		
		if (aoRegisters.size() == 0) {
			WasdiLog.errorLog("EoepcaProcessorEngine.delete: registers list is empty, return false.");
			return false;			
		}

		// For EOPCA we are going to run the app not on our server, so we do not need the tomcat user
		m_sTomcatUser = "";
		// And we do not need to start after the build
		m_bRunAfterDeploy = false;
		// And we work with our main register
		m_sDockerRegistry = aoRegisters.get(0).address;
		
		boolean bDeleted = super.delete(oParameter);
		
		// TODO: remove the image from the registers 
		// Undeploy from ADES?
		
		return bDeleted;
	}
	
	/**
	 * Force the redeploy of an image
	 */
	@Override
	public boolean redeploy(ProcessorParameter oParameter) {
		// We read  the registers from the config
		List<DockerRegistryConfig> aoRegisters = WasdiConfig.Current.dockers.getRegisters();
		
		if (aoRegisters == null) {
			WasdiLog.errorLog("EoepcaProcessorEngine.redeploy: registers list is null, return false.");
			return false;
		}
		
		if (aoRegisters.size() == 0) {
			WasdiLog.errorLog("EoepcaProcessorEngine.redeploy: registers list is empty, return false.");
			return false;			
		}
		
		WasdiLog.debugLog("EoepcaProcessorEngine.redeploy: call base class deploy");
		
		// For EOPCA we are going to run the app not on our server, so we do not need the tomcat user
		m_sTomcatUser = "";
		// And we do not need to start after the build
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
			WasdiLog.errorLog("EoepcaProcessorEngine.redeploy: super class deploy returned false. So we stop here.");
			return false;
		}
						
		// Here we save the address of the image
		String sPushedImageAddress = pushImageInRegisters(oProcessor);
		
		if (Utils.isNullOrEmpty(sPushedImageAddress)) {
			WasdiLog.errorLog("EoepcaProcessorEngine.redeploy: Impossible to push the image.");
			return false;
		}
		
		// Prepare the args for the j2 template
		List<Map<String,Object>> aoProcessorParameters = getParametersInputDescription(oProcessor);

		if (aoProcessorParameters == null) {
			WasdiLog.errorLog("EoepcaProcessorEngine.deploy: empty args, not good");
			return false;
		}
		
		// Render the template for CWL
		boolean bTemplates = renderCWLTemplates(oProcessor, aoProcessorParameters);
		
		if (!bTemplates) {
			WasdiLog.errorLog("EoepcaProcessorEngine.redeploy: problems rendering the template");
			return false;
		}
		
        return true;
	}
	
	/**
	 * Force the library update
	 */
	@Override
	public boolean libraryUpdate(ProcessorParameter oParameter) {
		// Library update is not possible in EOEPCA. It needs a full rebuild
		return true;
	}
	
	/**
	 * Force an environment update
	 */
	@Override
	public boolean environmentUpdate(ProcessorParameter oParameter) {
		// TODO Auto-generated method stub
		return false;
	}
	
	/**
	 * Refreshes the packages info
	 */
	@Override
	public boolean refreshPackagesInfo(ProcessorParameter oParameter) {
		// TODO Auto-generated method stub
		return false;
	}
	
	/**
	 * Get the related package manager
	 */
	@Override
	protected IPackageManager getPackageManager(String sIp, int iPort) {
		return null;
	}

}
