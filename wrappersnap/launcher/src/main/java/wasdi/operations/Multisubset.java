package wasdi.operations;

import java.io.File;
import java.util.ArrayList;

import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.MultiSubsetParameter;
import wasdi.shared.parameters.settings.MultiSubsetSetting;
import wasdi.shared.payloads.MultiSubsetPayload;
import wasdi.shared.utils.EndMessageProvider;
import wasdi.shared.utils.gis.GdalUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.runtime.RunTimeUtils;

public class Multisubset extends Operation {
	
	private static final String s_sLogMessage = "Invalid coordinates, jump";

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {
        
		WasdiLog.infoLog("Multisubset.executeOperation");
        
		if (oParam == null) {
			WasdiLog.errorLog("Parameter is null");
			return false;
		}
		
		if (oProcessWorkspace == null) {
			WasdiLog.errorLog("Process Workspace is null");
			return false;
		}        

        try {
        	
        	MultiSubsetParameter oParameter = (MultiSubsetParameter) oParam;
        	
            m_oProcessWorkspaceLogger.log("Starting multisubset");

            String sSourceProduct = oParameter.getSourceProductName();
            MultiSubsetSetting oSettings = (MultiSubsetSetting) oParameter.getSettings();

            int iTileCount = oSettings.getOutputNames().size();

            if (iTileCount > 15) {
                m_oProcessWorkspaceLogger.log("Sorry, no more than 15 tiles... " + new EndMessageProvider().getBad());
                WasdiLog.errorLog("Multisubset.executeOperation: More than 15 tiles: it hangs, need to refuse");
                return false;
            }

            int iStepPerTile = 100;

            if (iTileCount > 0) {
                iStepPerTile = 100 / iTileCount;
            }

            int iProgress = 0;

            // For all the tiles
            for (int iTiles = 0; iTiles < oSettings.getOutputNames().size(); iTiles++) {

                // Get the output name
                String sOutputProduct = oSettings.getOutputNames().get(iTiles);

                m_oProcessWorkspaceLogger.log("Generating tile " + sOutputProduct);

                // Check th bbox
                if (oSettings.getLatNList().size() <= iTiles) {
                    m_oProcessWorkspaceLogger.log(s_sLogMessage);
                    WasdiLog.warnLog("Multisubset.executeOperation: Lat N List does not have " + iTiles + " element. continue");
                    continue;
                }

                if (oSettings.getLatSList().size() <= iTiles) {
                    m_oProcessWorkspaceLogger.log(s_sLogMessage);
                    WasdiLog.warnLog("Multisubset.executeOperation: Lat S List does not have " + iTiles + " element. continue");
                    continue;
                }

                if (oSettings.getLonEList().size() <= iTiles) {
                    m_oProcessWorkspaceLogger.log(s_sLogMessage);
                    WasdiLog.warnLog("Multisubset.executeOperation: Lon E List does not have " + iTiles + " element. continue");
                    continue;
                }

                if (oSettings.getLonWList().size() <= iTiles) {
                    m_oProcessWorkspaceLogger.log(s_sLogMessage);
                    WasdiLog.warnLog("Multisubset.executeOperation: Lon W List does not have " + iTiles + " element. continue");
                    continue;
                }

                WasdiLog.debugLog("Multisubset.executeOperation: Computing tile " + sOutputProduct);

                // Translate
                String sGdalTranslateCommand = "gdal_translate";

                sGdalTranslateCommand = GdalUtils.adjustGdalFolder(sGdalTranslateCommand);

                ArrayList<String> asArgs = new ArrayList<String>();
                asArgs.add(sGdalTranslateCommand);

                // Output format
                asArgs.add("-of");
                asArgs.add("GTiff");
                asArgs.add("-co");
                // TO BE TESTED
                asArgs.add("COMPRESS=LZW");

                asArgs.add("-projwin");
                // ulx uly lrx lry:
                asArgs.add(oSettings.getLonWList().get(iTiles).toString());
                asArgs.add(oSettings.getLatNList().get(iTiles).toString());
                asArgs.add(oSettings.getLonEList().get(iTiles).toString());
                asArgs.add(oSettings.getLatSList().get(iTiles).toString());

                m_oProcessWorkspaceLogger.log("Tile LonW= " + oSettings.getLonWList().get(iTiles).toString() + " LatN= " + oSettings.getLatNList().get(iTiles).toString() + " LonE=" + oSettings.getLonEList().get(iTiles).toString() + " LatS= " + oSettings.getLatSList().get(iTiles).toString());

                if (oSettings.getBigTiff()) {
                    asArgs.add("-co");
                    asArgs.add("BIGTIFF=YES");
                }

                asArgs.add(PathsConfig.getWorkspacePath(oParameter) + sSourceProduct);
                asArgs.add(PathsConfig.getWorkspacePath(oParameter) + sOutputProduct);

                // Execute the process
                RunTimeUtils.shellExec(asArgs, true);

                File oTileFile = new File(PathsConfig.getWorkspacePath(oParameter) + sOutputProduct);

                if (oTileFile.exists()) {
                    String sOutputPath = PathsConfig.getWorkspacePath(oParameter) + sOutputProduct;

                    WasdiLog.debugLog("Multisubset.executeOperation done for index " + iTiles);

                    m_oProcessWorkspaceLogger.log("adding output to the workspace");

                    addProductToDbAndWorkspaceAndSendToRabbit(null, sOutputPath, oParameter.getWorkspace(), oParameter.getWorkspace(), LauncherOperations.MULTISUBSET.toString(), null, false, false);

                    WasdiLog.debugLog("Multisubset.executeOperation: product added to workspace");

                } else {
                    WasdiLog.debugLog("Multisubset.executeOperation Subset null for index " + iTiles);
                }

                iProgress = iProgress + iStepPerTile;
                if (iProgress > 100) iProgress = 100;
                updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, iProgress);

            }

            m_oProcessWorkspaceLogger.log("All tiles done");
            
            updateProcessStatus(oProcessWorkspace, ProcessStatus.DONE, 100);
            
            try {
                MultiSubsetPayload oMultiSubsetPayload = new MultiSubsetPayload();
                oMultiSubsetPayload.setInputFile(sSourceProduct);
                oMultiSubsetPayload.setOutputFiles(oSettings.getOutputNames().toArray(new String[0]));

                setPayload(oProcessWorkspace, oMultiSubsetPayload);
            } catch (Exception oPayloadException) {
                WasdiLog.errorLog("Multisubset.executeOperation: Error creating operation payload: ", oPayloadException);
            }


            m_oSendToRabbit.SendRabbitMessage(true, LauncherOperations.MULTISUBSET.name(), oParameter.getWorkspace(), "Multisubset Done", oParameter.getExchange());
            
            updateProcessStatus(oProcessWorkspace, ProcessStatus.DONE, 100);
            
            return true;
        } catch (Exception oEx) {

            WasdiLog.errorLog("Multisubset.executeOperation: exception " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));

            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);

            m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.MULTISUBSET.name(), oParam.getWorkspace(), sError, oParam.getExchange());
        } 
        
        return false;
	}

}
