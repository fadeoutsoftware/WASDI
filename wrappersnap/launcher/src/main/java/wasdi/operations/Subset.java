package wasdi.operations;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.File;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dataio.geotiff.GeoTiffProductWriterPlugIn;

import wasdi.io.WasdiProductReader;
import wasdi.io.WasdiProductReaderFactory;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.SubsetParameter;
import wasdi.shared.parameters.settings.SubsetSetting;
import wasdi.shared.utils.log.WasdiLog;

public class Subset extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {
        
		WasdiLog.infoLog("Subset.executeOperation");
        
		if (oParam == null) {
			WasdiLog.errorLog("Parameter is null");
			return false;
		}
		
		if (oProcessWorkspace == null) {
			WasdiLog.errorLog("Process Workspace is null");
			return false;
		}        

        try {
        	
        	SubsetParameter oParameter = (SubsetParameter) oParam;
        	
            String sSourceProduct = oParameter.getSourceProductName();
            String sOutputProduct = oParameter.getDestinationProductName();

            SubsetSetting oSettings = (SubsetSetting) oParameter.getSettings();

			
            File oProductFile = new File(PathsConfig.getWorkspacePath(oParameter) + sSourceProduct);
			WasdiProductReader oReadProduct = WasdiProductReaderFactory.getProductReader(oProductFile);
			Product oInputProduct = oReadProduct.getSnapProduct();

            if (oInputProduct == null) {
                WasdiLog.errorLog("Subset.executeOperation: product is not a SNAP product ");
                updateProcessStatus(oProcessWorkspace, ProcessStatus.ERROR, 0);
                return false;
            }

            updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 30);
            
            // Take the Geo Coding
            final GeoCoding oGeoCoding = oInputProduct.getSceneGeoCoding();

            // Create 2 GeoPos points
            GeoPos oGeoPosNW = new GeoPos(oSettings.getLatN(), oSettings.getLonW());
            GeoPos oGeoPosSE = new GeoPos(oSettings.getLatS(), oSettings.getLonE());

            // Convert to Pixel Position
            PixelPos oPixelPosNW = oGeoCoding.getPixelPos(oGeoPosNW, null);
            if (!oPixelPosNW.isValid()) {
                oPixelPosNW.setLocation(0, 0);
            }

            PixelPos oPixelPosSW = oGeoCoding.getPixelPos(oGeoPosSE, null);
            if (!oPixelPosSW.isValid()) {
                oPixelPosSW.setLocation(oInputProduct.getSceneRasterWidth(), oInputProduct.getSceneRasterHeight());
            }

            // Create the final region
            Rectangle.Float oRegion = new Rectangle.Float();
            oRegion.setFrameFromDiagonal(oPixelPosNW.x, oPixelPosNW.y, oPixelPosSW.x, oPixelPosSW.y);

            // Create the product bound rectangle
            Rectangle.Float oProductBounds = new Rectangle.Float(0, 0, oInputProduct.getSceneRasterWidth(),
                    oInputProduct.getSceneRasterHeight());

            // Intersect
            Rectangle2D oSubsetRegion = oProductBounds.createIntersection(oRegion);

            ProductSubsetDef oSubsetDef = new ProductSubsetDef();
            oSubsetDef.setRegion(oSubsetRegion.getBounds());
            oSubsetDef.setIgnoreMetadata(false);
            oSubsetDef.setSubSampling(1, 1);
            oSubsetDef.setSubsetName("subset");
            oSubsetDef.setTreatVirtualBandsAsRealBands(false);
            oSubsetDef.setNodeNames(oInputProduct.getBandNames());
            oSubsetDef.addNodeNames(oInputProduct.getTiePointGridNames());

            Product oSubsetProduct = oInputProduct.createSubset(oSubsetDef, sOutputProduct, oInputProduct.getDescription());

            
            updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 50);

            String sOutputPath = PathsConfig.getWorkspacePath(oParameter) + sOutputProduct;

            ProductIO.writeProduct(oSubsetProduct, sOutputPath, GeoTiffProductWriterPlugIn.GEOTIFF_FORMAT_NAME);

            WasdiLog.debugLog("Subset.executeOperation done");

            updateProcessStatus(oProcessWorkspace, ProcessStatus.DONE, 100);

            WasdiLog.debugLog("Subset.executeOperation adding product to Workspace");

            addProductToDbAndWorkspaceAndSendToRabbit(null, sOutputPath, oParameter.getWorkspace(), oParameter.getWorkspace(), LauncherOperations.SUBSET.toString(), null);

            WasdiLog.debugLog("Subset.executeOperation: product added to workspace");
            
            return true;

        } catch (Exception oEx) {
            WasdiLog.errorLog("Subset.executeOperation: exception " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
            
            oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());

            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);
            m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.SUBSET.name(), oParam.getWorkspace(), sError, oParam.getExchange());
        }
        
        return false;
	}

}
