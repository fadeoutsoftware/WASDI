package wasdi.processors;

import java.io.File;
import java.util.Map;

import wasdi.LauncherMain;
import wasdi.ProcessWorkspaceLogger;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Processor;
import wasdi.shared.business.ProcessorTypes;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.rabbit.Send;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;

public abstract class WasdiProcessorEngine {

	protected static final String FILE_SEPARATOR = System.getProperty("file.separator");
	
	protected String m_sWorkingRootPath = "";
	protected String m_sDockerTemplatePath = "";
	protected ProcessWorkspaceLogger m_oProcessWorkspaceLogger = null;
	protected String m_sTomcatUser;
	ProcessorParameter m_oParameter;
	protected ProcessWorkspace m_oProcessWorkspace= null;
	protected Send m_oSendToRabbit = new Send(null);

	/**
	 * Create an instance of a Processor Engine
	 * @param sType Type of Processor
	 * @return
	 */
	public static WasdiProcessorEngine getProcessorEngine(String sType) { 
		return getProcessorEngine(sType, WasdiConfig.Current.paths.downloadRootPath, WasdiConfig.Current.paths.dockerTemplatePath, WasdiConfig.Current.tomcatUser);
	}
	
	
	/**
	 * Create an instance of a Processor Engine
	 * @param sType Type of Processor
	 * @param sWorkingRootPath WASDI Working Path
	 * @param sDockerTemplatePath Docker Template Path
	 * @param sTomcatUser Tomcat user to impersonate
	 * @return
	 */
	public static WasdiProcessorEngine getProcessorEngine(String sType,String sWorkingRootPath, String sDockerTemplatePath, String sTomcatUser) {
		
		if (Utils.isNullOrEmpty(sType)) {
			sType = ProcessorTypes.UBUNTU_PYTHON37_SNAP;
		}
		
		if (sType.equals(ProcessorTypes.IDL)) {
			return new IDL2ProcessorEngine(sWorkingRootPath,sDockerTemplatePath, sTomcatUser);
		}
		else if (sType.equals(ProcessorTypes.UBUNTU_PYTHON37_SNAP)) {
			return new UbuntuPython37ProcessorEngine(sWorkingRootPath,sDockerTemplatePath, sTomcatUser);
		}
		else if (sType.equals(ProcessorTypes.OCTAVE)) {
			return new OctaveProcessorEngine(sWorkingRootPath, sDockerTemplatePath, sTomcatUser);
		}
		else if (sType.equals(ProcessorTypes.CONDA)) {
			return new CondaProcessorEngine(sWorkingRootPath, sDockerTemplatePath, sTomcatUser);
		}
		else if (sType.equals(ProcessorTypes.JUPYTER_NOTEBOOK)) {
			return new JupyterNotebookProcessorEngine(sWorkingRootPath, sDockerTemplatePath, sTomcatUser);
		}
		else if (sType.equals(ProcessorTypes.CSHARP)) {
			return new CSharpProcessorEngine(sWorkingRootPath, sDockerTemplatePath, sTomcatUser);
		}		
		else if (sType.equals(ProcessorTypes.EOEPCA)) {
			return new EoepcaProcessorEngine(sWorkingRootPath, sDockerTemplatePath, sTomcatUser);
		}
		else {
			return new UbuntuPython37ProcessorEngine(sWorkingRootPath, sDockerTemplatePath, sTomcatUser);
		}
	}

	/**
	 * Create a Processor Engine using paths and tomcat user from config
	 */
	public WasdiProcessorEngine() {
		m_sWorkingRootPath = WasdiConfig.Current.paths.downloadRootPath;
		m_sDockerTemplatePath = WasdiConfig.Current.paths.dockerTemplatePath;
		m_sTomcatUser = WasdiConfig.Current.tomcatUser;
	}
	
	/**
	 * Create a Processor Engine
	 * @param sWorkingRootPath Main working path
	 * @param sDockerTemplatePath Docker template path
	 * @param sTomcatUser Tomcat user
	 */
	public WasdiProcessorEngine(String sWorkingRootPath, String sDockerTemplatePath, String sTomcatUser) {
		m_sWorkingRootPath = sWorkingRootPath;
		m_sDockerTemplatePath = sDockerTemplatePath;
		m_sTomcatUser = sTomcatUser;
	}
		
	/**
	 * Deploy a new Processor in WASDI
	 * @param oParameter
	 */
	public abstract boolean deploy(ProcessorParameter oParameter);
	
	/**
	 * Run a processor
	 * @param oParameter
	 * @return
	 */
	public abstract boolean run(ProcessorParameter oParameter);
	
	/**
	 * Deletes a processor
	 * @param oParameter
	 * @return
	 */
	public abstract boolean delete(ProcessorParameter oParameter);
	
	/**
	 * Deploy again a processor to update it
	 * @param oParameter
	 * @return
	 */
	public abstract boolean redeploy(ProcessorParameter oParameter);
	
	/**
	 * Force the library update
	 * @param oParameter
	 * @return
	 */
	public abstract boolean libraryUpdate(ProcessorParameter oParameter);
	
	/**
	 * Force the environment update
	 * @param oParameter
	 * @return
	 */
	public abstract boolean environmentUpdate(ProcessorParameter oParameter);

	/**
	 * Force the refresh of the packagesInfo.json file. Ideally, the file should be refreshed after every update operation.
	 * @param oParameter the processor parameter
	 * @param iPort port of the processor server
	 * * @return
	 */
	public abstract boolean refreshPackagesInfo(ProcessorParameter oParameter);
	
	/**
	 * Check if a processor exists on actual node
	 * @param oProcessorParameter
	 * @return True if the processor exists
	 */
	public boolean isProcessorOnNode(ProcessorParameter oProcessorParameter) {
		
		if (oProcessorParameter == null) return false;
		
		// First Check if processor exists
		String sProcessorName = oProcessorParameter.getName();
		
		String sProcessorFolder = getProcessorFolder(sProcessorName);
		
		File oProcessorFolderFile = new File(sProcessorFolder);
		
		return oProcessorFolderFile.exists();		
	}


	/**
	 * Download Processor on the local PC
	 * @param oProcessor
	 * @param sSessionId
	 * @return
	 */
	protected String downloadProcessor(Processor oProcessor, String sSessionId) {
		try {
			
			String sProcessorId = oProcessor.getProcessorId();
			
			if (sProcessorId == null) {
				Utils.debugLog("sProcessorId must not be null");
				return "";
			}

			if (sProcessorId.equals("")) {
				Utils.debugLog("sProcessorId must not be empty");
				return "";
			}

			String sBaseUrl = oProcessor.getNodeUrl();
			
			if (Utils.isNullOrEmpty(sBaseUrl)) sBaseUrl = WasdiConfig.Current.baseUrl;

			String sUrl = sBaseUrl + "/processors/downloadprocessor?processorId=" + sProcessorId;

			String sSavePath = m_sWorkingRootPath + "/processors/" + oProcessor.getName() + "/";
			String sOutputFilePath = sSavePath + sProcessorId + ".zip";

			Map<String, String> asHeaders = HttpUtils.getStandardHeaders(sSessionId);
			return HttpUtils.downloadFile(sUrl, asHeaders, sOutputFilePath);
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}		
	}

	/**
	 * Get the workspace logger
	 * @return
	 */
	public ProcessWorkspaceLogger geProcessWorkspaceLogger() {
		return m_oProcessWorkspaceLogger;
	}

	/**
	 * Set the workspace logger
	 * @param oProcessWorkspaceLogger
	 */
	public void setProcessWorkspaceLogger(ProcessWorkspaceLogger oProcessWorkspaceLogger) {
		this.m_oProcessWorkspaceLogger = oProcessWorkspaceLogger;
	}
	
	/**
	 * Safe Processor log
	 * @param sLog
	 */
	protected void processWorkspaceLog(String sLog) {
		if (m_oProcessWorkspaceLogger!=null) {
			m_oProcessWorkspaceLogger.log(sLog);
		}
	}
	
	/**
	 * Get the reference to the process workspace
	 * @return
	 */
	public ProcessWorkspace getProcessWorkspace() {
		return m_oProcessWorkspace;
	}

	/**
	 * Set the reference to the process workspace
	 * @param oProcessWorkspace
	 */
	public void setProcessWorkspace(ProcessWorkspace oProcessWorkspace) {
		this.m_oProcessWorkspace = oProcessWorkspace;
	}

	/**
	 * Get the associated ProcessorParameter
	 * @return
	 */
	public ProcessorParameter getParameter() {
		return m_oParameter;
	}

	/**
	 * Set the associated Processor Parameter
	 * @param oParameter
	 */
	public void setParameter(ProcessorParameter oParameter) {
		this.m_oParameter = oParameter;
	}
	
	/**
	 * Get the send to rabbit object
	 * @return
	 */
	public Send getSendToRabbit() {
		return m_oSendToRabbit;
	}

	/**
	 * Set the sent to rabbit object
	 * @param m_oSendToRabbit
	 */
	public void setSendToRabbit(Send m_oSendToRabbit) {
		this.m_oSendToRabbit = m_oSendToRabbit;
	}

	/**
	 * Get the processor folder by processor name
	 * @param sProcessorName the name of the processor
	 * @return the folder of the processor
	 */
	public String getProcessorFolder(String sProcessorName) {
		// Set the processor path
		String sDownloadRootPath = m_sWorkingRootPath;

		if (!sDownloadRootPath.endsWith(File.separator)) sDownloadRootPath = sDownloadRootPath + File.separator;

		String sProcessorFolder = sDownloadRootPath + "processors" + File.separator + sProcessorName + File.separator;

		return sProcessorFolder;
	}

	/**
	 * Get the template folder of the processor by processor name
	 * @param sProcessorName the name of the processor
	 * @return the folder of the template of the processor
	 */
	public String getProcessorTemplateFolder(String sProcessorName) {
		// Set the processor template path
		String sDockerTemplatePath = m_sDockerTemplatePath;

		if (!sDockerTemplatePath.endsWith(File.separator)) sDockerTemplatePath = sDockerTemplatePath + File.separator;

		String sProcessorTemplateFolder = sDockerTemplatePath + sProcessorName + File.separator;

		return sProcessorTemplateFolder;
	}

	/**
	 * Get the path of the general common environment file path of the processor
	 * @param sProcessorName
	 * @return
	 */
	public String getProcessorGeneralCommonEnvFilePath(String sProcessorName) {
		return getProcessorFolder(sProcessorName) + "var" + FILE_SEPARATOR + "general_common.env";
	}

	/**
	 * 
	 * @param sProcessorName
	 * @return
	 */
	public String getProcessorTemplateGeneralCommonEnvFilePath(String sProcessorName) {
		return getProcessorTemplateFolder(sProcessorName) + "var" + FILE_SEPARATOR + "general_common.env";
	}
	
	/**
	 * Re-executes all the actions that the user made in the environment of an application 
	 * after the initial deploy. This is used when an application is deployed on a node or forced to 
	 * be redeployed.
	 * 
	 * @param oParameter Processor Parameter with the command (run or redeploy)
	 * @param iPort port of the processor
	 * @return true if every thing is ok, false otherwise
	 */
	protected boolean reconstructEnvironment(ProcessorParameter oParameter, int iPort) {
		return true;
	}

	/**
	 * Waits some time to let application start
	 */
	public void waitForApplicationToStart(ProcessorParameter oParameter) {
		try {
	        LauncherMain.s_oLogger.debug("DockerProcessorEngine.waitForApplicationToStart: wait 5 sec to let docker start");
	        Thread.sleep(5000);

//	        Integer iNumberOfAttemptsToPingTheServer = WasdiConfig.Current.dockers.numberOfAttemptsToPingTheServer;
//	        Integer iMillisBetweenAttmpts = WasdiConfig.Current.dockers.millisBetweenAttmpts;
//
//	        LauncherMain.s_oLogger.debug("DockerProcessorEngine.waitForApplicationToStart: wait " + (iNumberOfAttemptsToPingTheServer * iMillisBetweenAttmpts) + " sec to let docker start");
//	        Thread.sleep(iNumberOfAttemptsToPingTheServer * iMillisBetweenAttmpts);
		}
		catch (Exception oEx) {
			LauncherMain.s_oLogger.debug("DockerProcessorEngine.waitForApplicationToStart: exception " + oEx.toString());
		}
	}
}
