package wasdi.processors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.net.io.Util;

import wasdi.LauncherMain;
import wasdi.ProcessWorkspaceLogger;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Processor;
import wasdi.shared.business.ProcessorTypes;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.Utils;

public abstract class WasdiProcessorEngine {
	
	protected String m_sWorkingRootPath = "";
	protected String m_sDockerTemplatePath = "";
	protected ProcessWorkspaceLogger m_oProcessWorkspaceLogger = null;
	protected String m_sTomcatUser;
	ProcessorParameter m_oParameter;
	protected ProcessWorkspace m_oProcessWorkspace= null;
	
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
		
		// Set the processor path
		String sDownloadRootPath = m_sWorkingRootPath;
		if (!sDownloadRootPath.endsWith("/")) sDownloadRootPath = sDownloadRootPath + "/";
		
		String sProcessorFolder = sDownloadRootPath+ "processors/" + sProcessorName + "/" ;
		
		File oProcessorFolderFile = new File(sProcessorFolder);
		
		return oProcessorFolderFile.exists();		
	}
	
	/**
	 * Get the standard headers for a WASDI call
	 * @return
	 */
	public static HashMap<String, String> getStandardHeaders(String sSessionId) {
		HashMap<String, String> aoHeaders = new HashMap<String, String>();
		aoHeaders.put("x-session-token", sSessionId);
		aoHeaders.put("Content-Type", "application/json");
		
		return aoHeaders;
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
				System.out.println("sProcessorId must not be null");
				return "";
			}

			if (sProcessorId.equals("")) {
				System.out.println("sProcessorId must not be empty");
				return "";
			}
			
			
			String sBaseUrl = oProcessor.getNodeUrl();
			
			if (Utils.isNullOrEmpty(sBaseUrl)) sBaseUrl = "https://www.wasdi.net/wasdiwebserver/rest";

		    String sUrl = sBaseUrl + "/processors/downloadprocessor?processorId="+sProcessorId;
		    
		    String sOutputFilePath = "";
		    
		    HashMap<String, String> asHeaders = getStandardHeaders(sSessionId);
			
			try {
				URL oURL = new URL(sUrl);
				HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();

				// optional default is GET
				oConnection.setRequestMethod("GET");
				
				if (asHeaders != null) {
					for (String sKey : asHeaders.keySet()) {
						oConnection.setRequestProperty(sKey,asHeaders.get(sKey));
					}
				}
				
				int responseCode =  oConnection.getResponseCode();

 				if(responseCode == 200) {
							
					Map<String, List<String>> aoHeaders = oConnection.getHeaderFields();
					List<String> asContents = null;
					if(null!=aoHeaders) {
						asContents = aoHeaders.get("Content-Disposition");
					}
					String sAttachmentName = null;
					if(null!=asContents) {
						String sHeader = asContents.get(0);
						sAttachmentName = sHeader.split("filename=")[1];
						if(sAttachmentName.startsWith("\"")) {
							sAttachmentName = sAttachmentName.substring(1);
						}
						if(sAttachmentName.endsWith("\"")) {
							sAttachmentName = sAttachmentName.substring(0,sAttachmentName.length()-1);
						}
						System.out.println(sAttachmentName);
						
					}
					
					String sSavePath = m_sWorkingRootPath + "/processors/" + oProcessor.getName()+ "/";
					sOutputFilePath = sSavePath + sProcessorId+".zip";
					
					File oTargetFile = new File(sOutputFilePath);
					File oTargetDir = oTargetFile.getParentFile();
					oTargetDir.mkdirs();

					// opens an output stream to save into file
					try (FileOutputStream oOutputStream = new FileOutputStream(sOutputFilePath)) {
						InputStream oInputStream = oConnection.getInputStream();

						Util.copyStream(oInputStream, oOutputStream);

						if(null!=oOutputStream) {
							oOutputStream.close();
						}
						if(null!=oInputStream) {
							oInputStream.close();
						}						
					}

					
					return sOutputFilePath;
				} else {
					String sMessage = oConnection.getResponseMessage();
					System.out.println(sMessage);
					return "";
				}

			} catch (Exception oEx) {
				oEx.printStackTrace();
				return "";
			}
			
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
	
}
