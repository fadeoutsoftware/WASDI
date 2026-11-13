package wasdi.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.products.BandViewModel;
import wasdi.shared.viewmodels.products.MetadataViewModel;
import wasdi.shared.viewmodels.products.NodeGroupViewModel;
import wasdi.shared.viewmodels.products.ProductViewModel;

/**
 * CsvProductReader
 */
public class CsvProductReader extends WasdiProductReader {

    public CsvProductReader(File oProductFile) {
		super(oProductFile);
	}

    @Override
    public ProductViewModel getProductViewModel() {

        ProductViewModel oViewModel = new ProductViewModel();

        String sFileName = m_oProductFile != null ? m_oProductFile.getName() : "no_file_name";
        
    	oViewModel.setFileName(sFileName);
        String sFileNameNoExt = WasdiFileUtils.getFileNameWithoutLastExtension(sFileName);
    	oViewModel.setName(sFileNameNoExt);
		oViewModel.setProductFriendlyName(sFileNameNoExt);
        
        NodeGroupViewModel oNodeGroupViewModel = new NodeGroupViewModel();
    	oNodeGroupViewModel.setNodeName("Columns");
    	
    	List<BandViewModel> oBands = new ArrayList<>();
        try {
            List<String> sBandNames = readFileFirstLine(m_oProductFile);
            for (String sBandName : sBandNames) {
                BandViewModel oBand = new BandViewModel();
                oBand.setName(sBandName);
                oBands.add(oBand);
            }
        } catch (Exception oE) {
            WasdiLog.errorLog("CsvProductReader.getProductViewModel. Exception ", oE);
        }

    	oNodeGroupViewModel.setBands(oBands);
    	oViewModel.setBandsGroups(oNodeGroupViewModel);
        	
		return oViewModel;
	}

    public List<String> readFileFirstLine(File m_oProductFile) throws IOException {
        if (m_oProductFile == null || !m_oProductFile.isFile()) {
            WasdiLog.debugLog("CsvProductReader.readFileFirstLine. No product file");
            return new ArrayList<>();
        }
        // Read first line
        try (BufferedReader oReader = new BufferedReader(new FileReader(m_oProductFile))) {
            String sFirstLine = oReader.readLine();   
            if (sFirstLine != null) 
                return Arrays.asList(sFirstLine.split(","));
        }
        WasdiLog.debugLog("CsvProductReader.readFileFirstLine. Empty file");
        return new ArrayList<>();
    }

    @Override
    public String getProductBoundingBox() {
        try {
            CoordinateBounds oBounds = getCoordinateExtremes();
            String boundingBoxPolygon = String.format(
                "%f, %f, %f, %f, %f, %f, %f, %f, %f, %f",
                oBounds.minLat, oBounds.minLon,  // minY, minX (Sud-Ovest)
                oBounds.minLat, oBounds.maxLon,  // minY, maxX (Sud-Est)
                oBounds.maxLat, oBounds.maxLon,  // maxY, maxX (Nord-Est)
                oBounds.maxLat, oBounds.minLon,  // maxY, minX (Nord-Ovest)
                oBounds.minLat, oBounds.minLon   // minY, minX (closing loop)
            );
            return boundingBoxPolygon;
        } catch (Exception oE) {
            WasdiLog.errorLog("CsvProductReader.getProductBoundingBox. Error ", oE);
            return "";
        }
    }

    @Override
    public MetadataViewModel getProductMetadataViewModel() {
        WasdiLog.warnLog("CsvProductReader.getProductMetadataViewModel. No metadata to read in a CSV file");
        return null;
    }

    @Override
	public File getFileForPublishBand(String sBand, String sLayerId, String sPlatform) {
		return null;
	}

    // Holds min/max lat/lon values for a CSV file
    public static class CoordinateBounds {
        public double minLat = Double.MAX_VALUE;
        public double maxLat = -Double.MAX_VALUE;
        public double minLon = Double.MAX_VALUE;
        public double maxLon = -Double.MAX_VALUE;
        
        public boolean hasData() {
            return minLat != Double.MAX_VALUE; // True if we successfully read at least one valid row
        }
    }

    public CoordinateBounds getCoordinateExtremes() throws IOException {
        CoordinateBounds oBounds = new CoordinateBounds();

        if (m_oProductFile == null || !m_oProductFile.isFile()) {
            return oBounds; 
        }

        try (BufferedReader oReader = new BufferedReader(new FileReader(m_oProductFile))) {
            String sHeaderLine = oReader.readLine();
            if (sHeaderLine == null) {
                return oBounds; // File is empty
            }

            // determine the column names and their positions (indices)
            String[] asHeaders = sHeaderLine.split(",");
            int iLatIndex = -1;
            int iLonIndex = -1;

            for (int i = 0; i < asHeaders.length; i++) {
                String sHeader = asHeaders[i].trim().toLowerCase();                
                if (sHeader.equals("latitude") || sHeader.equals("lat")) 
                    iLatIndex = i;
                else if (sHeader.equals("longitude") || sHeader.equals("lon"))                    iLonIndex = i;
                
            }

            // if we couldn't find the required columns, throw an exception or return empty
            if (iLatIndex == -1 || iLonIndex == -1) {
                WasdiLog.warnLog("CsvProductReader.getCoordinateExtremes: missing lat/lon");
                return oBounds;
            }

            // read the values and calculate min/max
            String sLine;
            while ((sLine = oReader.readLine()) != null) {
                if (sLine.trim().isEmpty()) continue;

                String[] values = sLine.split(",");
                
                // Ensure the row actually has enough columns to avoid ArrayIndexOutOfBoundsException
                if (values.length <= Math.max(iLatIndex, iLonIndex)) {
                    continue; 
                }

                try {
                    double dLat = Double.parseDouble(values[iLatIndex].trim());
                    double dLon = Double.parseDouble(values[iLonIndex].trim());

                    // Update Latitude extremes
                    if (dLat < oBounds.minLat) oBounds.minLat = dLat;
                    if (dLat > oBounds.maxLat) oBounds.maxLat = dLat;

                    // Update Longitude extremes
                    if (dLon < oBounds.minLon) oBounds.minLon = dLon;
                    if (dLon > oBounds.maxLon) oBounds.maxLon = dLon;
                    
                } catch (NumberFormatException e) {
                    // Log or simply skip rows where coordinates are missing or not valid numbers
                    continue;
                }
            }
        }

        return oBounds;
    }

    public static void main(String[] args) throws Exception {
        File oFile = new File("C:/Users/valentina.leone/Downloads/output.csv");
        CsvProductReader oReader = new CsvProductReader(oFile);
        ProductViewModel oView = oReader.getProductViewModel();
        System.out.println(oReader.getProductBoundingBox());
    }

}
