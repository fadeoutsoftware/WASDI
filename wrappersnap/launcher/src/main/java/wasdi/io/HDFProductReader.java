package wasdi.io;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;

import wasdi.shared.business.ecostress.EcoStressItemForReading;
import wasdi.shared.data.ecostress.EcoStressRepository;
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
		
		String sProductName = m_oProduct.getName();
		
		if (!m_oProduct.getName().toUpperCase().startsWith("EEH2TES_L2_LSTE")) {
			return super.getFileForPublishBand(sBand, sLayerId, sPlatform);
		}
		
		// we need to publish the LST bands of LSTE ECOSTRESS products
		
		// first of all, we need to find the dedicated L1_GEO file
		String sProductInfo = extractProductInfo(sProductName);
		
		String sGEOProductNamePrefix = "ECOv002_L1B_GEO_" + sProductInfo;
		
		EcoStressRepository oRepo = new EcoStressRepository();
		
		EcoStressItemForReading oEcostressItem = oRepo.getEcoStressByFileNamePrefix(sGEOProductNamePrefix);
		
		if (oEcostressItem == null) {
			WasdiLog.errorLog("HDFProductReader.getFileForPublishBand. No GEO product found for file " + sProductName);
		}
		
		// from here I can download the GEO product in  the temp folder
		
		
		
		
		return null;
	}
	
	private String extractProductInfo(String sFileName) {
        String sRegex = "(\\d{5}_\\d{3}_\\d{8}T\\d{6})";
        Pattern oPattern = Pattern.compile(sRegex);
        Matcher oMatcher = oPattern.matcher(sFileName);

        if (oMatcher.find()) 
            return oMatcher.group(1);
        
        return null;
	}
	
	
}
