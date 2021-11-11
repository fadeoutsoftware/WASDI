package wasdi.operations;

import java.io.File;
import java.util.ArrayList;

import wasdi.LauncherMain;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.MultiSubsetParameter;
import wasdi.shared.parameters.settings.MultiSubsetSetting;
import wasdi.shared.payloads.MultiSubsetPayload;
import wasdi.shared.utils.EndMessageProvider;

public class Multisubset extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {
        
		m_oLocalLogger.debug("Multisubset.executeOperation");
        
		if (oParam == null) {
			m_oLocalLogger.error("Parameter is null");
			return false;
		}
		
		if (oProcessWorkspace == null) {
			m_oLocalLogger.error("Process Workspace is null");
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
                m_oLocalLogger.error("Multisubset.executeOperation: More than 15 tiles: it hangs, need to refuse");
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
                    m_oProcessWorkspaceLogger.log("Invalid coordinates, jump");
                    m_oLocalLogger.debug("Multisubset.executeOperation: Lat N List does not have " + iTiles + " element. continue");
                    continue;
                }

                if (oSettings.getLatSList().size() <= iTiles) {
                    m_oProcessWorkspaceLogger.log("Invalid coordinates, jump");
                    m_oLocalLogger.debug("Multisubset.executeOperation: Lat S List does not have " + iTiles + " element. continue");
                    continue;
                }

                if (oSettings.getLonEList().size() <= iTiles) {
                    m_oProcessWorkspaceLogger.log("Invalid coordinates, jump");
                    m_oLocalLogger.debug("Multisubset.executeOperation: Lon E List does not have " + iTiles + " element. continue");
                    continue;
                }

                if (oSettings.getLonWList().size() <= iTiles) {
                    m_oProcessWorkspaceLogger.log("Invalid coordinates, jump");
                    m_oLocalLogger.debug("Multisubset.executeOperation: Lon W List does not have " + iTiles + " element. continue");
                    continue;
                }

                m_oLocalLogger.debug("Multisubset.executeOperation: Computing tile " + sOutputProduct);

                // Translate
                String sGdalTranslateCommand = "gdal_translate";

                sGdalTranslateCommand = LauncherMain.adjustGdalFolder(sGdalTranslateCommand);

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

                asArgs.add(LauncherMain.getWorkspacePath(oParameter) + sSourceProduct);
                asArgs.add(LauncherMain.getWorkspacePath(oParameter) + sOutputProduct);

                // Execute the process
                ProcessBuilder oProcessBuidler = new ProcessBuilder(asArgs.toArray(new String[0]));
                Process oProcess;

                String sCommand = "";
                for (String sArg : asArgs) {
                    sCommand += sArg + " ";
                }

                m_oLocalLogger.debug("Multisubset.executeOperation Command Line " + sCommand);

                // oProcessBuidler.redirectErrorStream(true);
                oProcess = oProcessBuidler.start();

                oProcess.waitFor();

                File oTileFile = new File(LauncherMain.getWorkspacePath(oParameter) + sOutputProduct);

                if (oTileFile.exists()) {
                    String sOutputPath = LauncherMain.getWorkspacePath(oParameter) + sOutputProduct;

                    m_oLocalLogger.debug("Multisubset.executeOperation done for index " + iTiles);

                    m_oProcessWorkspaceLogger.log("adding output to the workspace");

                    addProductToDbAndWorkspaceAndSendToRabbit(null, sOutputPath, oParameter.getWorkspace(), oParameter.getWorkspace(), LauncherOperations.MULTISUBSET.toString(), null, false, false);

                    m_oLocalLogger.debug("Multisubset.executeOperation: product added to workspace");

                } else {
                    m_oLocalLogger.debug("Multisubset.executeOperation Subset null for index " + iTiles);
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
                m_oLocalLogger.error("Multisubset.executeOperation: Error creating operation payload: ", oPayloadException);
            }


            m_oSendToRabbit.SendRabbitMessage(true, LauncherOperations.MULTISUBSET.name(), oParameter.getWorkspace(), "Multisubset Done", oParameter.getExchange());
            
            return true;
        } catch (Exception oEx) {

            m_oLocalLogger.error("Multisubset.executeOperation: exception " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));

            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);

            m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.MULTISUBSET.name(), oParam.getWorkspace(), sError, oParam.getExchange());
        } 
        
        return false;
	}

}
