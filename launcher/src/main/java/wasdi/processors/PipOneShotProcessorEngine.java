package wasdi.processors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.json.JSONObject;

import com.google.common.io.Files;

import wasdi.LauncherMain;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.business.processors.ProcessorTypes;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.packagemanagers.IPackageManager;
import wasdi.shared.packagemanagers.PipOneShotPackageManager;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class PipOneShotProcessorEngine extends OneShotProcessorEngine {
	
	public PipOneShotProcessorEngine() {
		super();
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += ProcessorTypes.getTemplateFolder(ProcessorTypes.PIP_ONESHOT);
		
	}

	@Override
	protected IPackageManager getPackageManager(String sUrl) {
		
		PipOneShotPackageManager oPipOneShotPackageManager = new PipOneShotPackageManager(sUrl);
		
		return oPipOneShotPackageManager;
	}
	
    /**
     * Updates the processor environment
     */
	@Override
	public boolean environmentUpdate(ProcessorParameter oParameter) {

		if (oParameter == null) {
			WasdiLog.errorLog("PipOneShotProcessorEngine.environmentUpdate: oParameter is null");
			return false;
		}

		if (Utils.isNullOrEmpty(oParameter.getJson())) {
			WasdiLog.errorLog("PipOneShotProcessorEngine.environmentUpdate: update command is null or empty");
			return false;
		}
		
		if (!WasdiConfig.Current.isMainNode()) {
			WasdiLog.errorLog("PipOneShotProcessorEngine.environmentUpdate: this processor manipulate environment only on the main node");
			return true;			
		}

		ProcessWorkspaceRepository oProcessWorkspaceRepository = null;
		ProcessWorkspace oProcessWorkspace = null;

		try {
			oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			oProcessWorkspace = m_oProcessWorkspace;

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);

			// First Check if processor exists
			String sProcessorName = oParameter.getName();
			String sProcessorId = oParameter.getProcessorID();

			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

			// Check processor
			if (oProcessor == null) {
				WasdiLog.errorLog("PipOneShotProcessorEngine.environmentUpdate: oProcessor is null [" + sProcessorId + "]");
				return false;
			}

			WasdiLog.infoLog("PipOneShotProcessorEngine.environmentUpdate: update env for " + sProcessorName);

			String sJson = oParameter.getJson();
			WasdiLog.debugLog("PipOneShotProcessorEngine.environmentUpdate: sJson: " + sJson);
			JSONObject oJsonItem = new JSONObject(sJson);

			// Get the requested command
			Object oUpdateCommand = oJsonItem.get("updateCommand");
			
			// If the command is null, we will just run the environment update
			if (oUpdateCommand == null || oUpdateCommand.equals(org.json.JSONObject.NULL)) {
				WasdiLog.debugLog("PipOneShotProcessorEngine.environmentUpdate: refresh of the list of libraries.");
			} 
			else {
				// We have a command
				String sUpdateCommand = (String) oUpdateCommand;
				WasdiLog.debugLog("PipOneShotProcessorEngine.environmentUpdate: sUpdateCommand: " + sUpdateCommand);
				
				String [] asParts = sUpdateCommand.split("/");
				
				String sPackage = "";
				boolean bAdd = true;
				
				if (asParts != null) {
					if (asParts.length>=2) {
						sPackage = asParts[1];
						
						if (asParts[0].equals("removePackage")) {
							bAdd = false;
						}
					}
				}
				
				if (Utils.isNullOrEmpty(sPackage)) {
					WasdiLog.debugLog("PipOneShotProcessorEngine.environmentUpdate: cannot extract the name of the package, error");
					return false;
				}
				
				// A version, if present, pins the exact release; otherwise pip resolves the latest one
				String sVersion = asParts.length >= 3 ? asParts[2] : "";
				String sDependencySpec = Utils.isNullOrEmpty(sVersion) ? sPackage : sPackage + "==" + sVersion;
				
				String sMessage = "PipOneShotProcessorEngine.environmentUpdate: ";
				if (bAdd) sMessage += "Adding Package ";
				else sMessage += "Removing Package ";
				sMessage += sDependencySpec;
				
				WasdiLog.debugLog(sMessage);
				
				String sProcessorPath = PathsConfig.getProcessorFolder(sProcessorName);
				
				File oPipFile = new File(sProcessorPath+"pip.txt");
				
				// we re-read all the actions line per line
				ArrayList<String> asPipLines = new ArrayList<>();
				
				if (oPipFile.exists()) {
			        try (java.util.stream.Stream<String> oLinesStream = java.nio.file.Files.lines(oPipFile.toPath())) {
			        	oLinesStream.forEach(sLine -> {
			        		asPipLines.add(sLine);
			            });
			        }					
				}
		        
		        ArrayList<String> asCleanPipLines = Utils.removeDuplicates(asPipLines);
		        
		        // Drop any existing line for this package (whatever version it was pinned to) before re-adding it
		        final String sPackageName = sPackage;
		        asCleanPipLines.removeIf(sLine -> getPipPackageName(sLine).equalsIgnoreCase(sPackageName));
		        
		        if (bAdd) {
		        	WasdiLog.debugLog("PipOneShotProcessorEngine.environmentUpdate: package " +  sDependencySpec + " added");
		        	asCleanPipLines.add(sDependencySpec);
		        }
		        else {
		        	WasdiLog.debugLog("PipOneShotProcessorEngine.environmentUpdate: package " +  sPackage + " removed");
		        }
		        
		        // Re-write pip.txt
		        WasdiLog.debugLog("PipOneShotProcessorEngine.environmentUpdate: writing new pip.txt");
				try (OutputStream oOutStream = new FileOutputStream(oPipFile, false)) {
					for (String sActualLine : asCleanPipLines) {
						String sWriteLine = sActualLine + "\n";
						byte[] ayBytes = sWriteLine.getBytes();
						oOutStream.write(ayBytes);
					}
				}		        
		        
		        WasdiLog.debugLog("PipOneShotProcessorEngine.environmentUpdate: starting re-deploy");
		        this.redeploy(oParameter);
			}

			return true;
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("PipOneShotProcessorEngine.environmentUpdate Exception", oEx);
			try {

				if (oProcessWorkspace != null) {
					// Check and set the operation end-date
					if (Utils.isNullOrEmpty(oProcessWorkspace.getOperationEndTimestamp())) {
						oProcessWorkspace.setOperationEndTimestamp(Utils.nowInMillis());
					}

					LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
				}
			} catch (Exception e) {
				WasdiLog.errorLog("PipOneShotProcessorEngine.environmentUpdate Exception", e);
			}

			return false;
		}
	}
	
	/**
	 * Extracts the bare package name from a pip dependency spec (e.g. "numpy==1.26.4" -> "numpy")
	 */
	private String getPipPackageName(String sSpec) {
		return sSpec.split("[<>=!~\\s]")[0].trim();
	}
	
	@Override
	protected void onAfterUnzipProcessor(String sProcessorFolder) {
		WasdiLog.infoLog("PipOneShotProcessorEngine.onAfterUnzipProcessor: calling base class method");
		
		super.onAfterUnzipProcessor(sProcessorFolder);
		
		try {

			// Check the pip file
			File oPipFile = new File(sProcessorFolder+"pip.txt");

			if (!oPipFile.exists()) {
				WasdiLog.infoLog("PipProcessorEngine.onAfterUnzipProcessor: pip file not present, done");
				return;
			}
			else {
				WasdiLog.infoLog("PipOneShotProcessorEngine.onAfterUnzipProcessor: Make a copy of orginal/adjusted pip file");
				File oDestinationPipFile = new File(sProcessorFolder + "pip_original.txt");
				Files.copy(oPipFile, oDestinationPipFile);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("PipOneShotProcessorEngine.onAfterUnzipProcessor: exception ", oEx);
		}
	}
	
}
