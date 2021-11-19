package wasdi.io;

import java.io.File;

public class WasdiProductReaderFactory {
	
	private WasdiProductReaderFactory() {
		
	};
	
	public static WasdiProductReader getProductReader(File oFile) {
		if (oFile == null) {
			return null;
		}
		
		if (oFile.getName().toLowerCase().endsWith("shp")) { 
			return new ShapeProductReader(oFile);
		}
		
		if (oFile.getName().toLowerCase().endsWith("vrt")) { 
			return new VrtProductReader(oFile);
		}
		
		if (oFile.getName().toLowerCase().startsWith("s5p")) { 
			return new Sentinel5ProductReader(oFile);
		}
		
		return new SnapProductReader(oFile);
	}
	

}
