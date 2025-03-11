package wasdi.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.products.BandViewModel;
import wasdi.shared.viewmodels.products.GeorefProductViewModel;
import wasdi.shared.viewmodels.products.MetadataViewModel;
import wasdi.shared.viewmodels.products.NodeGroupViewModel;
import wasdi.shared.viewmodels.products.ProductViewModel;

public class ShapeProductReader extends WasdiProductReader{

	public ShapeProductReader(File oProductFile) {
		
		super(oProductFile);
		
		try {
			if (oProductFile.getName().endsWith(".zip")) {
				String sFileName = m_oProductFile.getCanonicalPath();
				sFileName=sFileName.replace(".zip", ".shp");
				m_oProductFile = new File(sFileName);
			}			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ShapeProductReader.ShapeProductReader: error " + oEx.toString());
		}
	}

	@Override
	public ProductViewModel getProductViewModel() {
		
		if (m_oProductFile == null) return null;
		
    	// Create the return value
    	GeorefProductViewModel oRetViewModel = null;
    	
    	try {
    		
    		// Try to read the shape
            ShapefileDataStore oShapefileDataStore = new ShapefileDataStore(m_oProductFile.toURI().toURL());
            
            // Got it?
            if (oShapefileDataStore!=null) {
            	
            	// Create the Product View Model
            	oRetViewModel = new GeorefProductViewModel();
            	
            	// Set name values
            	oRetViewModel.setFileName(m_oProductFile.getName());
            	oRetViewModel.setName(WasdiFileUtils.getFileNameWithoutLastExtension(m_oProductFile.getName()));
            	oRetViewModel.setProductFriendlyName(oRetViewModel.getName());
            	
            	// Create the sub folder
            	NodeGroupViewModel oNodeGroupViewModel = new NodeGroupViewModel();
            	oNodeGroupViewModel.setNodeName("ShapeFile");
            	
            	// Create the single band representing the shape
            	BandViewModel oBandViewModel = new BandViewModel();
            	oBandViewModel.setPublished(false);
            	oBandViewModel.setGeoserverBoundingBox("");
            	oBandViewModel.setHeight(0);
            	oBandViewModel.setWidth(0);
            	oBandViewModel.setPublished(false);
            	oBandViewModel.setName(oRetViewModel.getName());
            	
            	ArrayList<BandViewModel> oBands = new ArrayList<BandViewModel>();
            	oBands.add(oBandViewModel);
            	
            	oNodeGroupViewModel.setBands(oBands);
            	
            	oRetViewModel.setBandsGroups(oNodeGroupViewModel);
            }
            
            // Clean
            oShapefileDataStore.dispose();    	
            
            String sBbox = getProductBoundingBox();
            oRetViewModel.setBbox(sBbox);
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("WasdiProductReader.getShapeFileProduct: exception reading the shape file " + oEx.toString());
		}
    	
    	return oRetViewModel;
	}

	@Override
	public String getProductBoundingBox() {
		String sBbox = "";
		ShapefileDataStore oShpFileDataStore = null;
		
		if (m_oProductFile == null) return null;
		
		try {
			// Open the data store
			oShpFileDataStore = new ShapefileDataStore(m_oProductFile.toURI().toURL());
			SimpleFeatureCollection oFeatColl = oShpFileDataStore.getFeatureSource().getFeatures();
			
			// Check the coordinate system
			CoordinateReferenceSystem oCrs = oFeatColl.getSchema().getGeometryDescriptor().getCoordinateReferenceSystem();
		    if (oCrs == null) {
		        oCrs = DefaultGeographicCRS.WGS84;
		    }
		    
		    // Get the envelope
			ReferencedEnvelope oBbox = oFeatColl.getBounds();
			double dMinY = oBbox.getMinY();
			double dMinX = oBbox.getMinX();
			double dMaxY = oBbox.getMaxY();
			double dMaxX = oBbox.getMaxX();
			
			// Write the bounding box
			sBbox = String.format("%f,%f,%f,%f,%f,%f,%f,%f,%f,%f", dMinY, dMinX, dMinY, dMaxX, dMaxY, dMaxX, dMaxY, dMinX, dMinY, dMinX);
			
		} 
		catch (IOException e) {
			WasdiLog.errorLog("WasdiProductReader.getProductBoundingBox: Exception " + e.getMessage());
		}
		finally {
			if (oShpFileDataStore != null) {
				oShpFileDataStore.dispose();
			}
		}
		
		return sBbox;
	}

	@Override
	public MetadataViewModel getProductMetadataViewModel() {
		return new MetadataViewModel("Metadata");
	}
	
	@Override
	public File getFileForPublishBand(String sBand, String sLayerId) {
		return m_oProductFile;
	}
	
	@Override
    public String getEPSG() {
		try {
			String sPrjFile = m_oProductFile.getPath();
			sPrjFile = sPrjFile.replace(".shp", ".prj");
			sPrjFile = sPrjFile.replace(".SHP", ".prj");
			
			File oPrjFile = new File(sPrjFile);
			
			String sWKT = "";
			
			if (oPrjFile.exists()) {
				sWKT = WasdiFileUtils.fileToText(sPrjFile);
				CoordinateReferenceSystem oCRS = CRS.parseWKT(sWKT);
				String sEPSG = CRS.lookupIdentifier(oCRS, true);
				return sEPSG;				
			}
			else {
	    		
	    		// Try to read the shape
	            ShapefileDataStore oShapefileDataStore = new ShapefileDataStore(m_oProductFile.toURI().toURL());
	            
				// Check the coordinate system
	            SimpleFeatureCollection oFeatColl = oShapefileDataStore.getFeatureSource().getFeatures();
				CoordinateReferenceSystem oCRS = oFeatColl.getSchema().getGeometryDescriptor().getCoordinateReferenceSystem();
				String sEPSG = CRS.lookupIdentifier(oCRS, true);
				oShapefileDataStore.dispose();
				return sEPSG;
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ShapeProductReader.getEPSG(): exception " + oEx.toString());
		}
		return null;    	
    }

}
