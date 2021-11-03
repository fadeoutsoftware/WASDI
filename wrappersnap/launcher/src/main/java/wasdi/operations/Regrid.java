package wasdi.operations;

import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;

import wasdi.LauncherMain;
import wasdi.io.WasdiProductReader;
import wasdi.io.WasdiProductReaderFactory;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.RegridParameter;
import wasdi.shared.parameters.settings.RegridSetting;

public class Regrid extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {

		m_oLocalLogger.debug("Regrid.executeOperation");
        
		if (oParam == null) {
			m_oLocalLogger.error("Parameter is null");
			return false;
		}
		
		if (oProcessWorkspace == null) {
			m_oLocalLogger.error("Process Workspace is null");
			return false;
		}        
        
        try {
        	
        	RegridParameter oParameter = (RegridParameter) oParam;
        	
            String sSourceProduct = oParameter.getSourceProductName();
            String sDestinationProduct = oParameter.getDestinationProductName();

            RegridSetting oSettings = (RegridSetting) oParameter.getSettings();
            String sReferenceProduct = oSettings.getReferenceFile();

            File oReferenceFile = new File( LauncherMain.getWorkspacePath(oParameter) + sReferenceProduct);

			WasdiProductReader oRead = WasdiProductReaderFactory.getProductReader(oReferenceFile);

            if (oRead.getSnapProduct() == null) {
                m_oLocalLogger.error("Regrid.executeOperation: product is not a SNAP product ");
                return false;
            }

            // minY, minX, minY, maxX, maxY, maxX, maxY, minX, minY, minX
            String sBBox = oRead.getProductBoundingBox();

            String[] asBBox = sBBox.split(",");
            double[] adBBox = new double[10];

            // It it is null, we will have handled excpetion
            if (asBBox.length >= 10) {
                for (int iStrings = 0; iStrings < 10; iStrings++) {
                    try {
                        adBBox[iStrings] = Double.parseDouble(asBBox[iStrings]);
                    } catch (Exception e) {
                        m_oLocalLogger.error("Regrid.executeOperation: error convering bbox " + e.toString());
                        adBBox[iStrings] = 0.0;
                    }
                }
            }

            double dXOrigin = adBBox[1];
            double dYOrigin = adBBox[0];
            double dXEnd = adBBox[3];
            double dYEnd = adBBox[4];

            Dimension oDim = oRead.getSnapProduct().getSceneRasterSize();

            // STILL HAVE TO FIND THE SCALE: THIS IS NOT PRECISE
            double dXScale = (dXEnd - dXOrigin) / oDim.getWidth();
            double dYScale = (dYEnd - dYOrigin) / oDim.getHeight();

            if (oProcessWorkspace != null) {
                updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 20);
            }

            // SPAWN, ['gdalwarp', '-r', 'near', '-tr', STRING(xscale, Format='(D)'),
            // STRING(yscale, Format='(D)'), '-te', STRING(xOrigin, Format='(D)'),
            // STRING(yOrigin, Format='(D)'), STRING(xEnd, Format='(D)'), STRING(yEnd,
            // Format='(D)'), flood_in, flood_temp,'-co','COMPRESS=LZW'], /NOSHELL
            String sGdalWarpCommand = "gdalwarp";

            sGdalWarpCommand = LauncherMain.adjustGdalFolder(sGdalWarpCommand);

            ArrayList<String> asArgs = new ArrayList<String>();
            asArgs.add(sGdalWarpCommand);

            // Output format
            asArgs.add("-r");
            asArgs.add("near");

            asArgs.add("-tr");
            asArgs.add("" + dXScale);
            asArgs.add("" + dYScale);

            asArgs.add("-te");
            asArgs.add("" + dXOrigin);
            asArgs.add("" + dYOrigin);
            asArgs.add("" + dXEnd);
            asArgs.add("" + dYEnd);

            asArgs.add(LauncherMain.getWorkspacePath(oParameter) + sSourceProduct);
            asArgs.add(LauncherMain.getWorkspacePath(oParameter) + sDestinationProduct);

            asArgs.add("-co");
            asArgs.add("COMPRESS=LZW");

            // Execute the process
            ProcessBuilder oProcessBuidler = new ProcessBuilder(asArgs.toArray(new String[0]));
            Process oProcess;

            String sCommand = "";
            for (String sArg : asArgs) {
                sCommand += sArg + " ";
            }

            m_oLocalLogger.debug("Regrid.executeOperation: Command Line " + sCommand);

            oProcess = oProcessBuidler.start();

            oProcess.waitFor();

            addProductToDbAndWorkspaceAndSendToRabbit(null, LauncherMain.getWorkspacePath(oParameter) + sDestinationProduct,
                    oParameter.getWorkspace(), oParameter.getExchange(), LauncherOperations.REGRID.name(), sBBox, false,
                    true);
            
            updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 100);

            m_oSendToRabbit.SendRabbitMessage(true, LauncherOperations.MULTISUBSET.name(), oParameter.getWorkspace(), "Regrid Done", oParameter.getExchange());
            
            return true;
        } catch (Exception oEx) {
            m_oLocalLogger.error("Regrid.executeOperation: exception " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
            
            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);
            
            m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.MULTISUBSET.name(), oParam.getWorkspace(), sError, oParam.getExchange());

        }
        
		return false;
	}

}
