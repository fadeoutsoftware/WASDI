package wasdi.processors;

import java.io.File;
import java.util.Map;

import wasdi.ProcessWorkspaceLogger;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.business.processors.ProcessorTypes;
import wasdi.shared.config.PathsConfig;
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
	 * Flag to understand if the Processor must be built locally or not
	 */
	protected boolean m_bLocalBuild = false;
	
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
			WasdiLog.debugLog("WasdiProcessorEngine.getProcessorEngine: return processor of type IDL2ProcessorEngine");
			return new IDL2ProcessorEngine();
		}
		else if (sType.equals(ProcessorTypes.UBUNTU_PYTHON37_SNAP)) {
			WasdiLog.debugLog("WasdiProcessorEngine.getProcessorEngine: return processor of type UbuntuPython37ProcessorEngine");
			return new UbuntuPython37ProcessorEngine();
		}
		else if (sType.equals(ProcessorTypes.OCTAVE)) {
			WasdiLog.debugLog("WasdiProcessorEngine.getProcessorEngine: return processor of type OctaveProcessorEngine");
			return new OctaveProcessorEngine();
		}
		else if (sType.equals(ProcessorTypes.CONDA)) {
			WasdiLog.debugLog("WasdiProcessorEngine.getProcessorEngine: return processor of type CondaProcessorEngine");
			return new CondaProcessorEngine();
		}
		else if (sType.equals(ProcessorTypes.JUPYTER_NOTEBOOK)) {
			WasdiLog.debugLog("WasdiProcessorEngine.getProcessorEngine: return processor of type JupyterNotebookProcessorEngine");
			return new JupyterNotebookProcessorEngine();
		}
		else if (sType.equals(ProcessorTypes.CSHARP)) {
			WasdiLog.debugLog("WasdiProcessorEngine.getProcessorEngine: return processor of type CSharpProcessorEngine");
			return new CSharpProcessorEngine();
		}		
		else if (sType.equals(ProcessorTypes.EOEPCA)) {
			WasdiLog.debugLog("WasdiProcessorEngine.getProcessorEngine: return processor of type EoepcaProcessorEngine");
			return new EoepcaProcessorEngine();
		}
		else if (sType.equals(ProcessorTypes.PYTHON_PIP_2)) {
			WasdiLog.debugLog("WasdiProcessorEngine.getProcessorEngine: return processor of type PythonPipProcessorEngine2");
			return new PythonPipProcessorEngine2();
		}
		else if (sType.equals(ProcessorTypes.PIP_ONESHOT)) {
			WasdiLog.debugLog("WasdiProcessorEngine.getProcessorEngine: return processor of type PipOneShotProcessorEngine");
			return new PipOneShotProcessorEngine();
		}		
		else if (sType.equals(ProcessorTypes.PYTHON_PIP_2_UBUNTU_20)) {
			WasdiLog.debugLog("WasdiProcessorEngine.getProcessorEngine: return processor of type PYTHON_PIP_2_UBUNTU_20");
			return new Ubuntu20Pip2ProcessorEngine();
		}
		else if (sType.equals(ProcessorTypes.JAVA_17_UBUNTU_22)) {
			WasdiLog.debugLog("WasdiProcessorEngine.getProcessorEngine: return processor of type JAVA_17_UBUNTU_22");
			return new Java17Ubuntu22ProcessorEngine();
		}		
		else if (sType.equals(ProcessorTypes.PERSONALIZED_DOCKER)) {
			WasdiLog.debugLog("WasdiProcessorEngine.getProcessorEngine: return processor of type PERSONALIZED_DOCKER");
			return new PersonalizedDockerProcessor();
		}		
		else {
			WasdiLog.warnLog("WasdiProcessorEngine.getProcessorEngine: DEFAULT CASE (type "  + sType+ " not recognized) return processor of type UbuntuPython37ProcessorEngine");
			return new UbuntuPython37ProcessorEngine();
		}
	}

	/**
	 * Create a Processor Engine using paths and tomcat user from config
	 */
	public WasdiProcessorEngine() {
		m_sDockerTemplatePath = WasdiConfig.Current.paths.dockerTemplatePath;
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
	 * Stop a running application
	 * @param oParameter the processor parameter
	 * @return true if stopped false in case of problems
	 */
	public abstract boolean stopApplication(ProcessorParameter oParameter);	
	
	/**
	 * Check if a processor exists on actual node
	 * @param oProcessorParameter
	 * @return True if the processor exists
	 */
	public boolean isProcessorOnNode(ProcessorParameter oProcessorParameter) {
		
		if (oProcessorParameter == null) return false;
		
		// First Check if processor exists
		String sProcessorName = oProcessorParameter.getName();
		
		String sProcessorFolder = PathsConfig.getProcessorFolder(sProcessorName);
		
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

			String sSavePath = PathsConfig.getProcessorFolder(oProcessor.getName());
			String sOutputFilePath = sSavePath + sProcessorId + ".zip";

			Map<String, String> asHeaders = HttpUtils.getStandardHeaders(sSessionId);
			
			WasdiLog.debugLog("Downloding from: " + sUrl);
			WasdiLog.debugLog("Downloding to: " + sOutputFilePath);
			
			return HttpUtils.downloadFile(sUrl, asHeaders, sOutputFilePath);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("WasdiProcessorEngine.downloadProcessor: error", oEx);
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
		return PathsConfig.getProcessorFolder(sProcessorName) + "var" + s_sFILE_SEPARATOR + "general_common.env";
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
	        WasdiLog.debugLog("WasdiProcessorEngine.waitForApplicationToStart: wait to let docker start");
	        Thread.sleep(5000);

//	        Integer iNumberOfAttemptsToPingTheServer = WasdiConfig.Current.dockers.numberOfAttemptsToPingTheServer;
//	        Integer iMillisBetweenAttmpts = WasdiConfig.Current.dockers.millisBetweenAttmpts;
//
//	        WasdiLog.debugLog("WasdiProcessorEngine.waitForApplicationToStart: wait " + (iNumberOfAttemptsToPingTheServer * iMillisBetweenAttmpts) + " sec to let docker start");
//	        Thread.sleep(iNumberOfAttemptsToPingTheServer * iMillisBetweenAttmpts);
		}
		catch (InterruptedException oEx) {
			Thread.currentThread().interrupt();
			WasdiLog.errorLog("WasdiProcessorEngine.waitForApplicationToStart: current thread was interrupted ", oEx);
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
                WasdiLog.errorLog("WasdiProcessorEngine.UnzipProcessor: " + oProcessorZipFile.getCanonicalPath() + " does not exist, aborting");
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
                WasdiLog.errorLog("WasdiProcessorEngine.UnzipProcessor: could not unzip " + oProcessorZipFile.getCanonicalPath() + " due to: " + oE + ", aborting");
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
//		    	WasdiLog.errorLog("WasdiProcessorEngine.UnzipProcessor myProcessor.py not present in processor " + sZipFileName);
//		    	//return false;
//		    }

            try {
                // Remove the zip?
                if (!oProcessorZipFile.delete()) {
                    WasdiLog.errorLog("WasdiProcessorEngine.UnzipProcessor error Deleting Zip File");
                }
            } catch (Exception e) {
                WasdiLog.errorLog("WasdiProcessorEngine.UnzipProcessor Exception Deleting Zip File", e);
            }
        } catch (Exception oEx) {
            WasdiLog.errorLog("WasdiProcessorEngine.DeployProcessor Exception", oEx);
            return false;
        }
        return true;
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

	/**
	 * Flag to understand if the processor must be build locally or not
	 * @return
	 */
	public boolean isLocalBuild() {
		return m_bLocalBuild;
	}

	/**
	 * Flag to understand if the processor must be build locally or not
	 * @param bLocalBuild
	 */
	public void setLocalBuild(boolean bLocalBuild) {
		this.m_bLocalBuild = bLocalBuild;
	}	
}
