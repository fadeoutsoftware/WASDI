package wasdi.io;

import java.io.File;

import wasdi.shared.utils.WasdiFileUtils;

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
		
		if (WasdiFileUtils.isSentinel5PFile(oFile)) { 
			return new Sentinel5ProductReader(oFile);
		}

		if (WasdiFileUtils.isGpmZipFile(oFile)) { 
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
		
		if(WasdiFileUtils.isSentinel3Name(oFile)) {
			return new Sentinel3ProductReader(oFile);
		}

		if (oFile.getName().toLowerCase().endsWith(".nc")) {
			return new CmNcProductReader(oFile);
		}

		if (oFile.getName().toLowerCase().endsWith(".hdf")) {
			return new ModisProductReader(oFile);
		}

		return new SnapProductReader(oFile);
	}
	

}
