package wasdi.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import wasdi.LauncherMain;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.BandViewModel;
import wasdi.shared.viewmodels.GeorefProductViewModel;
import wasdi.shared.viewmodels.MetadataViewModel;
import wasdi.shared.viewmodels.NodeGroupViewModel;
import wasdi.shared.viewmodels.ProductViewModel;

public class ShapeProductReader extends WasdiProductReader{

	public ShapeProductReader(File oProductFile) {
		super(oProductFile);
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
            	oRetViewModel.setName(Utils.getFileNameWithoutLastExtension(m_oProductFile.getName()));
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
            	
            	
            	// Bounding Box
            	oRetViewModel.setBbox("");
            	
            }
            /*
            // Sample code to read the features
            SimpleFeatureIterator features = oShapefileDataStore.getFeatureSource().getFeatures().features();

            while (features.hasNext()) {
            	SimpleFeature shp = features.next();
            	String name = (String)shp.getAttribute("");
            	MultiPolygon geom = (MultiPolygon) shp.getDefaultGeometry();
            }
            
            features.close();
            */
            
            // Clean
            oShapefileDataStore.dispose();    	
            
            String sBbox = getProductBoundingBox();
            oRetViewModel.setBbox(sBbox);
    	}
    	catch (Exception oEx) {
    		LauncherMain.s_oLogger.debug("WasdiProductReader.getShapeFileProduct: exception reading the shape file");
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
			LauncherMain.s_oLogger.error("WasdiProductReader.getProductBoundingBox: Exception " + e.getMessage());
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
	

}