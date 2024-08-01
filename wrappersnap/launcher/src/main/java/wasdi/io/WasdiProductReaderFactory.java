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
		
		if(WasdiFileUtils.isShapeFile(oFile)) {
			return new ShapeProductReader(oFile);
		}
		
		if (oFile.getName().toLowerCase().endsWith("vrt")) { 
			return new VrtProductReader(oFile);
		}
		
		if (MissionUtils.isSentinel5PFile(oFile)) { 
			return new Sentinel5ProductReader(oFile);
		}

		if (MissionUtils.isGpmZipFile(oFile)) { 
			return new GpmZipProductReader(oFile);
		}

		if (oFile.getName().toLowerCase().startsWith("adaptor.mars.internal")
				|| oFile.getName().toLowerCase().contains("era5")
				|| oFile.getName().toLowerCase().contains("cams")) { 
			if (oFile.getName().toLowerCase().endsWith(".netcdf")) {
				return new CdsNetcdfProductReader(oFile);
			} else if (oFile.getName().toLowerCase().endsWith(".grib")) {
				return new CdsGribProductReader(oFile);
			} else if (oFile.getName().toLowerCase().endsWith(".netcdf_zip")) {
				return new AdsNetcdfZipProductReader(oFile);
			}
		}
		
		if(MissionUtils.isSentinel3Name(oFile)) {
			return new Sentinel3ProductReader(oFile);
		}
		
		if (MissionUtils.isSentinel6File(oFile)) {
			return new Sentinel6ProductReader(oFile);
		}
		
		if (MissionUtils.isLandsat5File(oFile) 
				|| MissionUtils.isLandsat7File(oFile)
				|| oFile.getName().toUpperCase().startsWith("LC08_L2SP_")) {
			return new LandsatProductReader(oFile);
		}

		if (oFile.getName().toLowerCase().endsWith(".nc")) {
			return new CmNcProductReader(oFile);
		}
		
		if (WasdiFileUtils.isDocumentFormatFile(oFile)) { 
			return new DocumentFormatsProductReader(oFile);
		}
		
		if (WasdiFileUtils.isImageFile(oFile)) {
			return new ImagesProductReader(oFile);
		}

		if (oFile.getName().toLowerCase().endsWith(".hdf")) {
			return new ModisProductReader(oFile);
		}
		
		if (oFile.getName().toUpperCase().startsWith(ResponseTranslatorJRC.s_sFileNamePrefix) && oFile.getName().endsWith(".zip")) {
			return new GHSLTilesProductReader(oFile);
		}
		
		if (oFile.getName().startsWith("SPEI") || oFile.getName().startsWith("https://SPEI")) {
			return new BigBangProductReader(oFile);
		}
		
		if (oFile.getName().endsWith(".pth") || oFile.getName().endsWith(".pt")) {
			return new PyTorchModelReader(oFile);
		}

		return new SnapProductReader(oFile);
	}
	

}
