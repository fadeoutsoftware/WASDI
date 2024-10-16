package wasdi.operations;

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
        
		if (oParam == null) {
			WasdiLog.errorLog("Parameter is null");
			return false;
		}
		
		if (oProcessWorkspace == null) {
			WasdiLog.errorLog("Process Workspace is null");
			return false;
		}

        try {
        	MosaicParameter oParameter = (MosaicParameter) oParam;
        	
        	m_oProcessWorkspaceLogger.log("Running Mosaic");
        	
            try {
                // Create the payload
                MosaicPayload oMosaicPayload = new MosaicPayload();
                // Get the settings
                MosaicSetting oSettings = (MosaicSetting) oParameter.getSettings();
                oMosaicPayload.setOutput(oParameter.getDestinationProductName());
                if (oSettings.getSources()!=null)   {
                	oMosaicPayload.setInputs(oSettings.getSources().toArray(new String[0]));
                }
                
                setPayload(oProcessWorkspace, oMosaicPayload);
                
            } catch (Exception oPayloadException) {
                WasdiLog.errorLog("Mosaic.executeOperation: Exception creating operation payload: ", oPayloadException);
            }        	
        	
            // Run the gdal mosaic
            if (GdalUtils.runGDALMosaic(oParameter)) {
            	
                WasdiLog.infoLog("Mosaic.executeOperation done");
                
                oProcessWorkspace.setProgressPerc(100);
                oProcessWorkspace.setStatus(ProcessStatus.DONE.name());

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
                
                return true;
            } 
            else {
            	
            	m_oProcessWorkspaceLogger.log("The mosaic operation returned false, there was an error");
                // error
                WasdiLog.warnLog("Mosaic.executeOperation: error");
                m_oProcessWorkspaceLogger.log(":( " + new EndMessageProvider().getBad());
                
                return false;
            }

        } 
        catch (Exception oEx) {
            WasdiLog.errorLog("Mosaic.executeOperation: exception " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
            
            oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());

            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);
            
            m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.MOSAIC.name(), oParam.getWorkspace(), sError, oParam.getExchange());

        }
        
		return false;
	}

}
