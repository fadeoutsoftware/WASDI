package wasdi.io;

import java.io.File;

import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;

import wasdi.shared.utils.log.WasdiLog;

public class HDFProductReader extends SnapProductReader {

	public HDFProductReader(File oProductFile) {
		super(oProductFile);
	}
	
	@Override
	public String getProductBoundingBox() {
		
		String sBoundingBox = "";
		
		try { 
			Product oProduct = getSnapProduct();
			
			if (oProduct == null) {
				WasdiLog.infoLog("HDFProductReader.getProductBoundingBox: snap product is null");
				return sBoundingBox;
			}
			
			// y is the latitude, x is the longitude
			double dMinY = Double.NaN;
			double dMaxY = Double.NaN;
			double dMinX = Double.NaN;
			double dMaxX = Double.NaN;
			
			MetadataElement oMetadataRoot = oProduct.getMetadataRoot();
	        MetadataElement[] aoElements = oMetadataRoot.getElements();
	        
	        for (MetadataElement oMetadataElement : aoElements) {
	        	
	        	// maxX : EAST
	        	if (!Double.isNaN(oMetadataElement.getAttributeDouble("EastBoundingCoord", Double.NaN))) 
	        		dMaxX = oMetadataElement.getAttributeDouble("EastBoundingCoord");
	        	
	        	// minX: WEST
	        	if (!Double.isNaN(oMetadataElement.getAttributeDouble("WestBoundingCoord", Double.NaN))) 
	        		dMinX = oMetadataElement.getAttributeDouble("WestBoundingCoord");
	        	
	          	// maxY: NORTH
	        	if (!Double.isNaN(oMetadataElement.getAttributeDouble("NorthBoundingCoord", Double.NaN))) 
	        		dMaxY = oMetadataElement.getAttributeDouble("NorthBoundingCoord");
	        		
	        	// minY: SOUTH
	        	if (!Double.isNaN(oMetadataElement.getAttributeDouble("SouthBoundingCoord", Double.NaN))) 
	        		dMinY = oMetadataElement.getAttributeDouble("SouthBoundingCoord");
	        	
	        }
			
	        if (Double.isNaN(dMaxX) || Double.isNaN(dMinX) || Double.isNaN(dMaxY) || Double.isNaN(dMinY))
	        	return sBoundingBox;
	        
	        sBoundingBox = String.format("%f,%f,%f,%f,%f,%f,%f,%f,%f,%f", 
					(float) dMinY, (float) dMinX, (float) dMinY, (float) dMaxX, (float) dMaxY, (float) dMaxX, (float) dMaxY, (float) dMinX, (float) dMinY, (float) dMinX);	
		
		} catch (Exception oEx) {
			WasdiLog.errorLog("HDFProductReader.getProductBoundingBox. Error", oEx);
		}
        
		return sBoundingBox;
	}
	
	@Override
	public File getFileForPublishBand(String sBand, String sLayerId, String sPlatform) {
		
		WasdiLog.infoLog("HDFProductReader.getFileForPublishBand. Band: " + sBand + ", layer id: " + sLayerId + ", platform: " + sPlatform);
		
		WasdiLog.infoLog("HDFProductReader.getFileForPublishBand. Absolute file of the product" + m_oProductFile.getAbsolutePath());
		
		return null;
	}
	
	
}
