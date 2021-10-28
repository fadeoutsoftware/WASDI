package wasdi.operations;

import wasdi.LauncherMain;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.MosaicParameter;
import wasdi.shared.parameters.settings.MosaicSetting;
import wasdi.shared.payload.MosaicPayload;
import wasdi.shared.utils.EndMessageProvider;

public class Mosaic extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {

		m_oLocalLogger.debug("Mosaic.executeOperation");
        
		if (oParam == null) {
			m_oLocalLogger.error("Parameter is null");
			return false;
		}
		
		if (oProcessWorkspace == null) {
			m_oLocalLogger.error("Process Workspace is null");
			return false;
		}

        try {
        	MosaicParameter oParameter = (MosaicParameter) oParam;
        	
            if (oProcessWorkspace != null) {
                updateProcessStatus(m_oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);
            }

            m_oProcessWorkspaceLogger.log("Creating Mosaic Util");

            wasdi.snapopearations.Mosaic oMosaic = new wasdi.snapopearations.Mosaic(oParameter);
            // Set the proccess workspace logger
            oMosaic.setProcessWorkspaceLogger(m_oProcessWorkspaceLogger);

            // Run the gdal mosaic
            if (oMosaic.runGDALMosaic()) {
                m_oLocalLogger.debug("Mosaic.executeOperation done");
                if (oProcessWorkspace != null) {
                    oProcessWorkspace.setProgressPerc(100);
                    oProcessWorkspace.setStatus(ProcessStatus.DONE.name());
                }

                // Log here and to the user
                m_oLocalLogger.debug("Mosaic.executeOperation adding product to Workspace");
                m_oProcessWorkspaceLogger.log("Adding output file to the workspace");

                // Get the full path of the output
                String sFileOutputFullPath = LauncherMain.getWorkspacePath(oParameter) + oParameter.getDestinationProductName();

                // And add it to the db
                addProductToDbAndWorkspaceAndSendToRabbit(null, sFileOutputFullPath, oParameter.getWorkspace(),
                        oParameter.getWorkspace(), LauncherOperations.MOSAIC.toString(), null);

                m_oProcessWorkspaceLogger.log("Done " + new EndMessageProvider().getGood());

                try {
                    // Create the payload
                    MosaicPayload oMosaicPayload = new MosaicPayload();
                    // Get the settings
                    MosaicSetting oSettings = (MosaicSetting) oParameter.getSettings();
                    oMosaicPayload.setOutput(oParameter.getDestinationProductName());
                    oMosaicPayload.setInputs(oSettings.getSources().toArray(new String[0]));
                    oProcessWorkspace.setPayload(LauncherMain.s_oMapper.writeValueAsString(oMosaicPayload));
                } catch (Exception oPayloadException) {
                    m_oLocalLogger.error("Mosaic.executeOperation: Exception creating operation payload: ", oPayloadException);
                }

                m_oLocalLogger.debug("Mosaic.executeOperation: product added to workspace");
                
                return true;
            } else {
                // error
                m_oLocalLogger.debug("Mosaic.executeOperation: error");
                if (oProcessWorkspace != null)
                    oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
                
                return false;
            }

        } catch (Exception oEx) {
            m_oLocalLogger.error("Mosaic.executeOperation: exception "
                    + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
            if (oProcessWorkspace != null)
                oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());

            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);
            if (m_oSendToRabbit != null)
                m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.MOSAIC.name(), oParam.getWorkspace(),
                        sError, oParam.getExchange());

        }
        
		return false;
	}

}
