package wasdi.operations;

import java.io.File;

import org.apache.commons.io.FileUtils;

import wasdi.LauncherMain;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.Sen2CorParameter;
import wasdi.shared.utils.ZipExtractor;

public class Sen2core extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {
		
		m_oLocalLogger.debug("Sen2core.executeOperation");
		
		if (oParam == null) {
			m_oLocalLogger.error("Parameter is null");
			return false;
		}
		
		if (oProcessWorkspace == null) {
			m_oLocalLogger.error("Process Workspace is null");
			return false;
		}		
    	
    	try {
    		
    		Sen2CorParameter oSen2CorParameter = (Sen2CorParameter) oParam;
    		
            String sDestinationPath = LauncherMain.getWorkspacePath(oSen2CorParameter);
            String sL1ProductName = oSen2CorParameter.getProductName();
            String sL2ProductName = sL1ProductName.replace("L1C", "L2A");
            

            if (oSen2CorParameter.isValid()) {
                try {
                    m_oLocalLogger.debug("Sen2core.executeOperation: Start");
                    
                    updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 25);

                    m_oLocalLogger.debug("Sen2core.executeOperation: Extraction of " + sL1ProductName + " product");
                    ZipExtractor oZipExtractor = new ZipExtractor(oSen2CorParameter.getProcessObjId());
                    oZipExtractor.unzip(sDestinationPath + sL1ProductName + ".zip", sDestinationPath);

                    // 4 - Convert -> obtain L2A.SAFE
                    String sSen2CorPath = WasdiConfig.Current.paths.sen2CorePath;
                    m_oLocalLogger.debug("Sen2core.executeOperation: Extraction completed, begin conversion");
                    updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 50);
                    ProcessBuilder oProcessBuilder = new ProcessBuilder(sSen2CorPath, sDestinationPath + sL1ProductName + ".SAFE");

                    Process oProcess = oProcessBuilder
                            .inheritIO() // this is enabled for debugging
                            .start();
                    // Wait for the process to complete
                    oProcess.waitFor();


                    // 5 - ZipIt -> L2A.zip
                    m_oLocalLogger.debug("Sen2core.executeOperation: Conversion done, begin compression of L2 archive");
                    updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 75);
                    oZipExtractor.zip(sDestinationPath + sL2ProductName + ".SAFE", sDestinationPath + sL2ProductName + ".zip");


                    m_oLocalLogger.debug("Sen2core.executeOperation: Done");
                    updateProcessStatus(oProcessWorkspace, ProcessStatus.DONE, 100);
                }
                catch (Exception oe){
                    // if something went wrong delete the zip file and SAFE directories to return to original WorkSpace state
                    FileUtils.deleteQuietly(new File(sDestinationPath + sL2ProductName + ".zip")); // level2 zip, if exists
                    FileUtils.deleteDirectory(new File(sDestinationPath + sL1ProductName + ".SAFE")); // Level1 .Safe, if exists
                    FileUtils.deleteDirectory(new File(sDestinationPath + sL2ProductName + ".SAFE")); // Level2 .Safe, if exists
                    
                    m_oLocalLogger.error("Sen2core.executeOperation: exception " + oe.toString());
                    
                    return false;
                }

                if (oSen2CorParameter.isDeleteIntermediateFile()) {
                    // deletes .SAFE directories and keeps the zip files
                    FileUtils.deleteDirectory(new File(sDestinationPath + sL1ProductName + ".SAFE"));
                    FileUtils.deleteDirectory(new File(sDestinationPath + sL2ProductName + ".SAFE"));
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
            	m_oLocalLogger.error("Sen2core.executeOperation: invalid parameters");
            	return false;
            }    		
    	}
    	catch (Exception oEx) {
    		m_oLocalLogger.error("Sen2core.executeOperation: exception " + oEx.toString());
		}
    	
		return false;
	}

}
