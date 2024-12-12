package wasdi.operations;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.lang3.exception.ExceptionUtils;

import wasdi.io.WasdiProductReader;
import wasdi.io.WasdiProductReaderFactory;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.RegridParameter;
import wasdi.shared.parameters.settings.RegridSetting;
import wasdi.shared.utils.gis.GdalInfoResult;
import wasdi.shared.utils.gis.GdalUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.runtime.RunTimeUtils;

public class Regrid extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {

		WasdiLog.infoLog("Regrid.executeOperation");
        
		if (oParam == null) {
			WasdiLog.errorLog("Regrid.executeOperation: Parameter is null");
			return false;
		}
		
		if (oProcessWorkspace == null) {
			WasdiLog.errorLog("Regrid.executeOperation: Process Workspace is null");
			return false;
		}        
        
        try {
        	
        	RegridParameter oParameter = (RegridParameter) oParam;
        	
            String sSourceProduct = oParameter.getSourceProductName();
            String sDestinationProduct = oParameter.getDestinationProductName();

            RegridSetting oSettings = (RegridSetting) oParameter.getSettings();
            String sReferenceProduct = oSettings.getReferenceFile();

            File oReferenceFile = new File( PathsConfig.getWorkspacePath(oParameter) + sReferenceProduct);

			WasdiProductReader oReferenceFileReader = WasdiProductReaderFactory.getProductReader(oReferenceFile);
			
			GdalInfoResult oGdalInfoResult = GdalUtils.getGdalInfoResult(oReferenceFile);
			
			if (oGdalInfoResult == null) {
				WasdiLog.errorLog("Regrid.executeOperation: impossible to get images gdal info");
				m_oProcessWorkspaceLogger.log("Impossible to get reference image info, sorry");
				return false;				
			}            

            double dXOrigin = oGdalInfoResult.topLeftX;
            double dYOrigin = oGdalInfoResult.topLeftY;
            double dXEnd = oGdalInfoResult.topLeftX + oGdalInfoResult.westEastPixelResolution* ( (double) oGdalInfoResult.size.get(0));
            double dYEnd = oGdalInfoResult.topLeftY + oGdalInfoResult.northSouthPixelResolution* ( (double) oGdalInfoResult.size.get(1));

            // Get th
            double dXScale = oGdalInfoResult.westEastPixelResolution;
            double dYScale = Math.abs(oGdalInfoResult.northSouthPixelResolution);

            updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 20);
            
            m_oProcessWorkspaceLogger.log("Regrid " + oParameter.getSourceProductName() + " to " + sReferenceProduct);
            
            m_oProcessWorkspaceLogger.log("XOrigin = " + dXOrigin + "; dYOrigin = " + dYOrigin + "; dXEnd " + dXEnd + "; dYEnd = " + dYEnd + "; XStep " + dXScale + "; YStep " + dYScale);

            // SPAWN, ['gdalwarp', '-r', 'near', '-tr', STRING(xscale, Format='(D)'),
            // STRING(yscale, Format='(D)'), '-te', STRING(xOrigin, Format='(D)'),
            // STRING(yOrigin, Format='(D)'), STRING(xEnd, Format='(D)'), STRING(yEnd,
            // Format='(D)'), flood_in, flood_temp,'-co','COMPRESS=LZW'], /NOSHELL
            String sGdalWarpCommand = "gdalwarp";

            sGdalWarpCommand = GdalUtils.adjustGdalFolder(sGdalWarpCommand);

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

            asArgs.add(PathsConfig.getWorkspacePath(oParameter) + sSourceProduct);
            asArgs.add(PathsConfig.getWorkspacePath(oParameter) + sDestinationProduct);

            asArgs.add("-co");
            asArgs.add("COMPRESS=LZW");

            // Execute the process
            RunTimeUtils.shellExec(asArgs, true);
                        
            String sBBox = oReferenceFileReader.getProductBoundingBox();

            addProductToDbAndWorkspaceAndSendToRabbit(null, PathsConfig.getWorkspacePath(oParameter) + sDestinationProduct,
                    oParameter.getWorkspace(), oParameter.getExchange(), LauncherOperations.REGRID.name(), sBBox, false,
                    true);
            
            m_oProcessWorkspaceLogger.log("Regrid Done");
            
            updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 100);

            m_oSendToRabbit.SendRabbitMessage(true, LauncherOperations.REGRID.name(), oParameter.getWorkspace(), "Regrid Done", oParameter.getExchange());
            
            return true;
        } catch (Exception oEx) {
            WasdiLog.errorLog("Regrid.executeOperation: exception " + ExceptionUtils.getStackTrace(oEx));
            
            String sError = ExceptionUtils.getMessage(oEx);
            
            m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.REGRID.name(), oParam.getWorkspace(), sError, oParam.getExchange());

        }
        
		return false;
	}

}
