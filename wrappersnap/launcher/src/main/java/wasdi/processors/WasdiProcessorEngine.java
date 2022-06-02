package wasdi.processors;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
	
	protected String m_sWorkingRootPath = "";
	protected String m_sDockerTemplatePath = "";
	protected ProcessWorkspaceLogger m_oProcessWorkspaceLogger = null;
	protected String m_sTomcatUser;
	ProcessorParameter m_oParameter;
	protected ProcessWorkspace m_oProcessWorkspace= null;
	protected Send m_oSendToRabbit = new Send(null);

	public static WasdiProcessorEngine getProcessorEngine(String sType) { 
		return getProcessorEngine(sType, WasdiConfig.Current.paths.downloadRootPath, WasdiConfig.Current.paths.dockerTemplatePath, WasdiConfig.Current.tomcatUser);
	}
	
	
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
		else if (sType.equals(ProcessorTypes.CSHARP)) {
			return new CSharpProcessorEngine(sWorkingRootPath, sDockerTemplatePath, sTomcatUser);
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
	 * Execute a system task
	 * @param sCommand
	 * @param asArgs
	 */
	public static void shellExec(String sCommand, List<String> asArgs) {
		shellExec(sCommand,asArgs,true);
	}
	
	/**
	 * Execute a system task
	 * @param sCommand
	 * @param asArgs
	 * @param bWait
	 */	
	public static void shellExec(String sCommand, List<String> asArgs, boolean bWait) {
		try {
			if (asArgs==null) asArgs = new ArrayList<String>();
			asArgs.add(0, sCommand);
			
			String sCommandLine = "";
			
			for (String sArg : asArgs) {
				sCommandLine += sArg + " ";
			}
			
			LauncherMain.s_oLogger.debug("ShellExec CommandLine: " + sCommandLine);
			
			ProcessBuilder oProcessBuilder = new ProcessBuilder(asArgs.toArray(new String[0]));
			Process oProcess = oProcessBuilder.start();
			
			if (bWait) {
				int iProcOuptut = oProcess.waitFor();				
				LauncherMain.s_oLogger.debug("ShellExec CommandLine RETURNED: " + iProcOuptut);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Check if a processor exists on actual node
	 * @param oProcessorParameter
	 * @return True if the processor exists
	 */
	protected boolean isProcessorOnNode(ProcessorParameter oProcessorParameter) {
		
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

}
