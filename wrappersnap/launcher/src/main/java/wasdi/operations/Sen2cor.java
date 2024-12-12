package wasdi.operations;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;

import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.Sen2CorParameter;
import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.runtime.RunTimeUtils;

public class Sen2cor extends Operation {
	
	private static final String s_sSAFEExtension = ".SAFE";

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {
		
		WasdiLog.infoLog("Sen2core.executeOperation");
		
		if (oParam == null) {
			WasdiLog.errorLog("Parameter is null");
			return false;
		}
		
		if (oProcessWorkspace == null) {
			WasdiLog.errorLog("Process Workspace is null");
			return false;
		}		
    	
    	try {
    		
    		Sen2CorParameter oSen2CorParameter = (Sen2CorParameter) oParam;
    		
            String sDestinationPath = PathsConfig.getWorkspacePath(oSen2CorParameter);
            String sL1ProductName = oSen2CorParameter.getProductName();
            String sL2ProductName = sL1ProductName.replace("L1C", "L2A");
            

            if (oSen2CorParameter.isValid()) {
                try {
                    WasdiLog.debugLog("Sen2core.executeOperation: Start");
                    
                    updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 25);

                    WasdiLog.debugLog("Sen2core.executeOperation: Extraction of " + sL1ProductName + " product");
                    ZipFileUtils oZipExtractor = new ZipFileUtils(oSen2CorParameter.getProcessObjId());
                    oZipExtractor.unzip(sDestinationPath + sL1ProductName + ".zip", sDestinationPath);

                    // 4 - Convert -> obtain L2A.SAFE
                    String sSen2CorPath = WasdiConfig.Current.paths.sen2CorePath;
                    WasdiLog.debugLog("Sen2core.executeOperation: Extraction completed, begin conversion");
                    updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 50);
                    
                    ArrayList<String> asArgs = new ArrayList<>();
                    asArgs.add(sSen2CorPath);
                    asArgs.add(sDestinationPath + sL1ProductName + s_sSAFEExtension);
                    
                    // Run the tool
                    RunTimeUtils.shellExec(asArgs, true);

                    // 5 - ZipIt -> L2A.zip
                    WasdiLog.debugLog("Sen2core.executeOperation: Conversion done, begin compression of L2 archive");
                    updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 75);
                    oZipExtractor.zipFolder(sDestinationPath + sL2ProductName + s_sSAFEExtension, sDestinationPath + sL2ProductName + ".zip");


                    WasdiLog.debugLog("Sen2core.executeOperation: Done");
                    updateProcessStatus(oProcessWorkspace, ProcessStatus.DONE, 100);
                }
                catch (Exception oe){
                    // if something went wrong delete the zip file and SAFE directories to return to original WorkSpace state
                    FileUtils.deleteQuietly(new File(sDestinationPath + sL2ProductName + ".zip")); // level2 zip, if exists
                    FileUtils.deleteDirectory(new File(sDestinationPath + sL1ProductName + s_sSAFEExtension)); // Level1 .Safe, if exists
                    FileUtils.deleteDirectory(new File(sDestinationPath + sL2ProductName + s_sSAFEExtension)); // Level2 .Safe, if exists
                    
                    WasdiLog.errorLog("Sen2core.executeOperation: exception " + oe.toString());
                    
                    return false;
                }

                if (oSen2CorParameter.isDeleteIntermediateFile()) {
                    // deletes .SAFE directories and keeps the zip files
                    FileUtils.deleteDirectory(new File(sDestinationPath + sL1ProductName + s_sSAFEExtension));
                    FileUtils.deleteDirectory(new File(sDestinationPath + sL2ProductName + s_sSAFEExtension));
                }


                addProductToDbAndWorkspaceAndSendToRabbit(
                        null,
                        sDestinationPath + sL2ProductName + ".zip",
                        oSen2CorParameter.getWorkspace(),
                        oSen2CorParameter.getExchange(),
                        LauncherOperations.SEN2COR.name(),
                        null
                );
                
                return true;

            } 
            else {
            	WasdiLog.errorLog("Sen2core.executeOperation: invalid parameters");
            	return false;
            }    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("Sen2core.executeOperation: exception " + oEx.toString());
		}
    	
		return false;
	}

}
