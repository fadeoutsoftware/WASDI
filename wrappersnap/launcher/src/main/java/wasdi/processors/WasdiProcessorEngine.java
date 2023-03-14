package wasdi.processors;

import java.io.File;
import java.util.Map;

import wasdi.ProcessWorkspaceLogger;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Processor;
import wasdi.shared.business.ProcessorTypes;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.rabbit.Send;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.utils.log.WasdiLog;

public abstract class WasdiProcessorEngine {
	
	/**
	 * File Separator
	 */
	protected static final String s_sFILE_SEPARATOR = System.getProperty("file.separator");
	
	/**
	 * Base Working path of WASDI
	 */
	protected String m_sWorkingRootPath = "";
	
	/**
	 * Folder of the docker templates
	 */
	protected String m_sDockerTemplatePath = "";
	
	/**
	 * Util to log in the WADSI end user interface
	 */
	protected ProcessWorkspaceLogger m_oProcessWorkspaceLogger = null;
	
	/**
	 * User to mount on Docker. If "" will not be added
	 */
	protected String m_sTomcatUser;
	
	/**
	 * Processor Parameter of this operation
	 */
	ProcessorParameter m_oParameter;
	
	/**
	 * Actual process workspace
	 */
	protected ProcessWorkspace m_oProcessWorkspace= null;
	
	/**
	 * Utility to send rabbit messages
	 */
	protected Send m_oSendToRabbit = new Send(null);
	
	/**
	 * Flag to decide if the system must run the docker after the deploy or not
	 */
	protected boolean m_bRunAfterDeploy = true;
	
	/**
	 * Create an instance of a Processor Engine
	 * @param sType Type of Processor
	 * @return
	 */
	public static WasdiProcessorEngine getProcessorEngine(String sType) { 
		if (Utils.isNullOrEmpty(sType)) {
			sType = ProcessorTypes.UBUNTU_PYTHON37_SNAP;
		}
		
		if (sType.equals(ProcessorTypes.IDL)) {
			return new IDL2ProcessorEngine();
		}
		else if (sType.equals(ProcessorTypes.UBUNTU_PYTHON37_SNAP)) {
			return new UbuntuPython37ProcessorEngine();
		}
		else if (sType.equals(ProcessorTypes.OCTAVE)) {
			return new OctaveProcessorEngine();
		}
		else if (sType.equals(ProcessorTypes.CONDA)) {
			return new CondaProcessorEngine();
		}
		else if (sType.equals(ProcessorTypes.JUPYTER_NOTEBOOK)) {
			return new JupyterNotebookProcessorEngine();
		}
		else if (sType.equals(ProcessorTypes.CSHARP)) {
			return new CSharpProcessorEngine();
		}		
		else if (sType.equals(ProcessorTypes.EOEPCA)) {
			return new EoepcaProcessorEngine();
		}
		else {
			return new UbuntuPython37ProcessorEngine();
		}
	}

	/**
	 * Create a Processor Engine using paths and tomcat user from config
	 */
	public WasdiProcessorEngine() {
		m_sDockerTemplatePath = WasdiConfig.Current.paths.dockerTemplatePath;
		m_sTomcatUser = WasdiConfig.Current.tomcatUser;
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
				WasdiLog.debugLog("sProcessorId must not be null");
				return "";
			}

			if (sProcessorId.equals("")) {
				WasdiLog.debugLog("sProcessorId must not be empty");
				return "";
			}

			String sBaseUrl = oProcessor.getNodeUrl();
			
			if (Utils.isNullOrEmpty(sBaseUrl)) sBaseUrl = WasdiConfig.Current.baseUrl;
			
			if (!sBaseUrl.endsWith("/"))  {
				sBaseUrl += "/";
			}

			String sUrl = sBaseUrl + "processors/downloadprocessor?processorId=" + sProcessorId;

			String sSavePath = getProcessorFolder(oProcessor.getName());
			String sOutputFilePath = sSavePath + sProcessorId + ".zip";

			Map<String, String> asHeaders = HttpUtils.getStandardHeaders(sSessionId);
			
			WasdiLog.debugLog("Downloding from: " + sUrl);
			WasdiLog.debugLog("Downloding to: " + sOutputFilePath);
			
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
	 * Get the processor folder by Processor
	 * @param oProcessor Processor Object
	 * @return the folder of the processor
	 */
	public String getProcessorFolder(Processor oProcessor) {
		if (oProcessor == null) return null;
		return getProcessorFolder(oProcessor.getName());
	}

	/**
	 * Get the processor folder by processor name
	 * @param sProcessorName the name of the processor
	 * @return the folder of the processor
	 */
	public String getProcessorFolder(String sProcessorName) {
		// Set the processor path
		String sDownloadRootPath = WasdiConfig.Current.paths.downloadRootPath;

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
		return getProcessorFolder(sProcessorName) + "var" + s_sFILE_SEPARATOR + "general_common.env";
	}

	/**
	 * 
	 * @param sProcessorName
	 * @return
	 */
	public String getProcessorTemplateGeneralCommonEnvFilePath(String sProcessorName) {
		return getProcessorTemplateFolder(sProcessorName) + "var" + s_sFILE_SEPARATOR + "general_common.env";
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
	        WasdiLog.debugLog("DockerProcessorEngine.waitForApplicationToStart: wait 5 sec to let docker start");
	        Thread.sleep(5000);

//	        Integer iNumberOfAttemptsToPingTheServer = WasdiConfig.Current.dockers.numberOfAttemptsToPingTheServer;
//	        Integer iMillisBetweenAttmpts = WasdiConfig.Current.dockers.millisBetweenAttmpts;
//
//	        WasdiLog.debugLog("DockerProcessorEngine.waitForApplicationToStart: wait " + (iNumberOfAttemptsToPingTheServer * iMillisBetweenAttmpts) + " sec to let docker start");
//	        Thread.sleep(iNumberOfAttemptsToPingTheServer * iMillisBetweenAttmpts);
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("DockerProcessorEngine.waitForApplicationToStart: exception " + oEx.toString());
		}
	}
	
    /**
     * Unzip the processor
     *
     * @param sProcessorFolder
     * @param sZipFileName
     * @return
     */
    public boolean unzipProcessor(String sProcessorFolder, String sZipFileName, String sProcessObjId) {
        try {

            sProcessorFolder = WasdiFileUtils.fixPathSeparator(sProcessorFolder);
            if (!sProcessorFolder.endsWith(File.separator)) {
                sProcessorFolder += File.separator;
            }

            // Create the file
            File oProcessorZipFile = new File(sProcessorFolder + sZipFileName);
            if (!oProcessorZipFile.exists()) {
                WasdiLog.errorLog("DockerProcessorEngine.UnzipProcessor: " + oProcessorZipFile.getCanonicalPath() + " does not exist, aborting");
                return false;
            }
            try {
                ZipFileUtils oZipExtractor = new ZipFileUtils(sProcessObjId);
                oZipExtractor.unzip(oProcessorZipFile.getCanonicalPath(), sProcessorFolder);

                // fix https://github.com/fadeoutsoftware/WASDI/issues/635
                // New application: zip file with a folder containing the actual data does not work
                File oProcessorFolder = new File(sProcessorFolder);
                if (oProcessorFolder.exists()) {
                    if (oProcessorFolder.isDirectory()) {
                         String[] asFileNames = oProcessorFolder.list();

                         if (asFileNames != null) {
                             if (asFileNames.length == 2) {
                                 if (asFileNames[0].equalsIgnoreCase(sZipFileName)
                                         || asFileNames[1].equalsIgnoreCase(sZipFileName)) {
                                     for (String sFileName : asFileNames) {
                                         if (sFileName.equalsIgnoreCase(sZipFileName)) {
                                             continue;
                                         } else {
                                             String sExtractedFilePath = sProcessorFolder + sFileName;
                                             File oExtractedFile = new File(sExtractedFilePath);

                                             if (oExtractedFile.exists()) {
                                                 if (oExtractedFile.isDirectory()) {
                                                     sExtractedFilePath += File.separator;

                                                    String[] asExtractedFileNames = oExtractedFile.list();

                                                    if (asExtractedFileNames != null) {
                                                        for (String sExtractedFileName : asExtractedFileNames) {
                                                            WasdiFileUtils.moveFile(sExtractedFilePath + sExtractedFileName, sProcessorFolder);
                                                        }

                                                        asExtractedFileNames = oExtractedFile.list();
                                                        if (asExtractedFileNames != null && asExtractedFileNames.length == 0) {
                                                            WasdiFileUtils.deleteFile(sExtractedFilePath);
                                                        }
                                                    }
                                                 }
                                             }
                                         }
                                     }
                                 }
                             }
                         }
                    }
                }
            } catch (Exception oE) {
                WasdiLog.errorLog("DockerProcessorEngine.UnzipProcessor: could not unzip " + oProcessorZipFile.getCanonicalPath() + " due to: " + oE + ", aborting");
                return false;
            }

            //check myProcessor exists:
            // This class is generic. to use this code we need before to adapt it to run with all the different processor types
//			AtomicBoolean oMyProcessorExists = new AtomicBoolean(false);
//			try(Stream<Path> oWalk = Files.walk(Paths.get(sProcessorFolder));){
//				oWalk.map(Path::toFile).forEach(oFile->{
//					if(oFile.getName().equals("myProcessor.py")) {
//						oMyProcessorExists.set(true);
//					}
//				});
//			}
//		    if (!oMyProcessorExists.get()) {
//		    	WasdiLog.errorLog("DockerProcessorEngine.UnzipProcessor myProcessor.py not present in processor " + sZipFileName);
//		    	//return false;
//		    }

            try {
                // Remove the zip?
                if (!oProcessorZipFile.delete()) {
                    WasdiLog.errorLog("DockerProcessorEngine.UnzipProcessor error Deleting Zip File");
                }
            } catch (Exception e) {
                WasdiLog.errorLog("DockerProcessorEngine.UnzipProcessor Exception Deleting Zip File", e);
            }
        } catch (Exception oEx) {
            WasdiLog.errorLog("DockerProcessorEngine.DeployProcessor Exception", oEx);
            return false;
        }
        return true;
    }

    /**
     * Get the user to mount of the docker
     * @return
     */
	public String getTomcatUser() {
		return m_sTomcatUser;
	}

	/**
	 * Set the user to mount of the docker
	 * @param sTomcatUser
	 */
	public void setTomcatUser(String sTomcatUser) {
		this.m_sTomcatUser = sTomcatUser;
	}

	/**
	 * Flag to know if this application type needs to be ran after the deploy or not
	 * @return true to run, false otherwise
	 */
	public boolean isRunAfterDeploy() {
		return m_bRunAfterDeploy;
	}

	/**
	 * Flag to know if this application type needs to be ran after the deploy or not
	 * @param m_bRunAfterDeploy true to run after deploy, false otherwise
	 */
	public void setRunAfterDeploy(boolean bRunAfterDeploy) {
		this.m_bRunAfterDeploy = bRunAfterDeploy;
	}	
}
