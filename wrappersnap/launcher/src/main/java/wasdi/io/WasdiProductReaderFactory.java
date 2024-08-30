package wasdi.io;

import java.io.File;

import wasdi.shared.queryexecutors.jrc.ResponseTranslatorJRC;
import wasdi.shared.utils.MissionUtils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;

public class WasdiProductReaderFactory {
	
	private WasdiProductReaderFactory() {
		
	};
	
	public static WasdiProductReader getProductReader(File oFile) {
		if (oFile == null) {
			return null;
		}
		
		if(WasdiFileUtils.isShapeFile(oFile) || WasdiFileUtils.isShapeFileZipped(oFile.getAbsolutePath(), 100)) {
			WasdiLog.debugLog("Creating Shape File Reader for " + oFile.getName());
			return new ShapeProductReader(oFile);
		}
		
		if (oFile.getName().toLowerCase().endsWith("vrt")) { 
			WasdiLog.debugLog("Creating VRT File Reader for " + oFile.getName());
			return new VrtProductReader(oFile);
		}
		
		if (MissionUtils.isSentinel5PFile(oFile)) { 
			WasdiLog.debugLog("Creating S5P File Reader for " + oFile.getName());
			return new Sentinel5ProductReader(oFile);
		}

		if (MissionUtils.isGpmZipFile(oFile)) {
			WasdiLog.debugLog("Creating GPM File Reader for " + oFile.getName());
			return new GpmZipProductReader(oFile);
		}

		if (oFile.getName().toLowerCase().startsWith("adaptor.mars.internal")
				|| oFile.getName().toLowerCase().contains("era5")
				|| oFile.getName().toLowerCase().contains("cams")) { 
			if (oFile.getName().toLowerCase().endsWith(".netcdf")) {
				WasdiLog.debugLog("Creating CDS NefCDF File Reader for " + oFile.getName());
				return new CdsNetcdfProductReader(oFile);
			} else if (oFile.getName().toLowerCase().endsWith(".grib")) {
				WasdiLog.debugLog("Creating CDS GRIB File Reader for " + oFile.getName());
				return new CdsGribProductReader(oFile);
			} else if (oFile.getName().toLowerCase().endsWith(".netcdf_zip")) {
				WasdiLog.debugLog("Creating ADS NetCDF File Reader for " + oFile.getName());
				return new AdsNetcdfZipProductReader(oFile);
			}
		}
		
		if(MissionUtils.isSentinel3Name(oFile)) {
			WasdiLog.debugLog("Creating S3 File Reader for " + oFile.getName());
			return new Sentinel3ProductReader(oFile);
		}
		
		if (MissionUtils.isSentinel6File(oFile)) {
			WasdiLog.debugLog("Creating S6 File Reader for " + oFile.getName());
			return new Sentinel6ProductReader(oFile);
		}
		
		if (MissionUtils.isLandsat5File(oFile) 
				|| MissionUtils.isLandsat7File(oFile)) {
			WasdiLog.debugLog("Creating Landsat File Reader for " + oFile.getName());
			return new LandsatProductReader(oFile);
		}

		if (oFile.getName().toLowerCase().endsWith(".nc")) {
			WasdiLog.debugLog("Creating CM NetCDF File Reader for " + oFile.getName());
			return new CmNcProductReader(oFile);
		}
		
		if (WasdiFileUtils.isDocumentFormatFile(oFile)) { 
			WasdiLog.debugLog("Creating Documents File Reader for " + oFile.getName());
			return new DocumentFormatsProductReader(oFile);
		}
		
		if (WasdiFileUtils.isImageFile(oFile)) {
			WasdiLog.debugLog("Creating Images File Reader for " + oFile.getName());
			return new ImagesProductReader(oFile);
		}

		if (oFile.getName().toLowerCase().endsWith(".hdf")) {
			WasdiLog.debugLog("Creating Modis File Reader for " + oFile.getName());
			return new ModisProductReader(oFile);
		}
		
		if (oFile.getName().toUpperCase().startsWith(ResponseTranslatorJRC.s_sFileNamePrefix) && oFile.getName().endsWith(".zip")) {
			WasdiLog.debugLog("Creating GHSL Tiles File Reader for " + oFile.getName());
			return new GHSLTilesProductReader(oFile);
		}
		
		if (oFile.getName().startsWith("SPEI") || oFile.getName().startsWith("https://SPEI")) {
			WasdiLog.debugLog("Creating SPEI File Reader for " + oFile.getName());
			return new BigBangProductReader(oFile);
		}
		
		if (oFile.getName().endsWith(".pth") || oFile.getName().endsWith(".pt")) {
			WasdiLog.debugLog("Creating PyTorchModel File Reader for " + oFile.getName());
			return new PyTorchModelReader(oFile);
		}
		
		WasdiLog.debugLog("Creating SNAP File Reader for " + oFile.getName());

		return new SnapProductReader(oFile);
	}
	

}
