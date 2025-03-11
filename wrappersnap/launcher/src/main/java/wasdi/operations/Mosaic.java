package wasdi.operations;


import java.io.File;

import org.apache.commons.lang3.exception.ExceptionUtils;

import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.MosaicParameter;
import wasdi.shared.parameters.settings.MosaicSetting;
import wasdi.shared.payloads.MosaicPayload;
import wasdi.shared.utils.EndMessageProvider;
import wasdi.shared.utils.gis.GdalUtils;
import wasdi.shared.utils.log.WasdiLog;

public class Mosaic extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {

		WasdiLog.infoLog("Mosaic.executeOperation");
		
		// Check if we have a valid param        
		if (oParam == null) {
			WasdiLog.errorLog("Parameter is null");
			return false;
		}
		
		// And Process Workspace
		if (oProcessWorkspace == null) {
			WasdiLog.errorLog("Process Workspace is null");
			return false;
		}

        try {
        	MosaicParameter oParameter = (MosaicParameter) oParam;
        	
        	// Update the payload
            try {
                // Create the payload
                MosaicPayload oMosaicPayload = new MosaicPayload();
                // Get the settings
                MosaicSetting oSettings = (MosaicSetting) oParameter.getSettings();
                
                // Set output to payload
                oMosaicPayload.setOutput(oParameter.getDestinationProductName());
                
                // And sources, if available
                if (oSettings.getSources()!=null)   {
                	oMosaicPayload.setInputs(oSettings.getSources().toArray(new String[0]));
                }
                
                // Safe set payload
                setPayload(oProcessWorkspace, oMosaicPayload);
                
                // And update it in the db
                m_oProcessWorkspaceRepository.updateProcess(oProcessWorkspace);
                
            } 
            catch (Exception oPayloadException) {
                WasdiLog.errorLog("Mosaic.executeOperation: Exception creating operation payload: ", oPayloadException);
            }
            
            MosaicSetting oSettings = (MosaicSetting) oParameter.getSettings();
            
            boolean bMissingInputs = false;
            
			// for each product
			for (int iProducts = 0; iProducts<oSettings.getSources().size(); iProducts ++) {
				
				// Get full path
				String sProductFile = PathsConfig.getWorkspacePath(oParameter) + oSettings.getSources().get(iProducts);
				
				// Check if the file exists
				File oFile = new File(sProductFile);
								
				// This is not promising
				if (!oFile.exists()) {
					WasdiLog.warnLog("Mosaic.executeOperation: Missing input file " + oSettings.getSources().get(iProducts));
					m_oProcessWorkspaceLogger.log("Missing input file " + oSettings.getSources().get(iProducts));
					bMissingInputs = true;
				}
			}
			
			// If we are missing inputs, we cannot proceed
			if (bMissingInputs) {
				
				if (oSettings.getSources().size() == 1) {
	            	m_oProcessWorkspaceLogger.log("Impossible to run the mosaic due to one single and missing input");
	                WasdiLog.warnLog("Mosaic.executeOperation: Impossible to run the mosaic due to one single and missing input");
	                m_oProcessWorkspaceLogger.log(":( " + new EndMessageProvider().getBad());
	                return false;
				}
				else {
	            	m_oProcessWorkspaceLogger.log("Try to execute the mosaic with the inputs we have");
	                WasdiLog.warnLog("Mosaic.executeOperation: Try to execute the mosaic with the inputs we have");					
				}
			}
			
			
			// Set the output name of the mosaic
            oProcessWorkspace.setProductName(oParameter.getDestinationProductName());
            
            // update the process
            m_oProcessWorkspaceRepository.updateProcess(oProcessWorkspace);
            
            // Send Rabbit notification
            m_oSendToRabbit.SendUpdateProcessMessage(oProcessWorkspace);
			
			m_oProcessWorkspaceLogger.log("Running Mosaic");
        	
            // Run the gdal mosaic
            if (GdalUtils.runGDALMosaic(oParameter)) {
                // Log here and to the user
                WasdiLog.debugLog("Mosaic.executeOperation adding product to Workspace");
                m_oProcessWorkspaceLogger.log("Adding output file to the workspace");

                // Get the full path of the output
                String sFileOutputFullPath = PathsConfig.getWorkspacePath(oParameter) + oParameter.getDestinationProductName();

                // And add it to the db
                addProductToDbAndWorkspaceAndSendToRabbit(null, sFileOutputFullPath, oParameter.getWorkspace(), oParameter.getWorkspace(), LauncherOperations.MOSAIC.toString(), null);

                m_oProcessWorkspaceLogger.log("Done " + new EndMessageProvider().getGood());

                WasdiLog.debugLog("Mosaic.executeOperation: product added to workspace");
                
                updateProcessStatus(oProcessWorkspace, ProcessStatus.DONE, 100);
                
                WasdiLog.infoLog("Mosaic.executeOperation done");
                
                return true;
            } 
            else {
            	
            	m_oProcessWorkspaceLogger.log("The mosaic operation returned false, there was an error");
                // error
                WasdiLog.warnLog("Mosaic.executeOperation: runGDALMosaic returned false");
                
                m_oProcessWorkspaceLogger.log(":( " + new EndMessageProvider().getBad());
                
                return false;
            }

        } 
        catch (Exception oEx) {
            WasdiLog.errorLog("Mosaic.executeOperation: exception " + ExceptionUtils.getStackTrace(oEx));
            
            m_oProcessWorkspaceLogger.log("The mosaic operation had an exception");
            m_oProcessWorkspaceLogger.log(":( " + new EndMessageProvider().getBad());
            
            oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());

            String sError = ExceptionUtils.getMessage(oEx);
            
            m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.MOSAIC.name(), oParam.getWorkspace(), sError, oParam.getExchange());
        }
        
		return false;
	}

}
