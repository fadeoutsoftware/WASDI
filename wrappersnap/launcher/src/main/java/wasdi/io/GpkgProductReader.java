package wasdi.io;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.geotools.geopkg.TileEntry;
import org.opengis.geometry.Envelope;

import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.products.BandViewModel;
import wasdi.shared.viewmodels.products.GeorefProductViewModel;
import wasdi.shared.viewmodels.products.MetadataViewModel;
import wasdi.shared.viewmodels.products.NodeGroupViewModel;
import wasdi.shared.viewmodels.products.ProductViewModel;

public class GpkgProductReader extends WasdiProductReader {
	
	public GpkgProductReader(File oProductFile) {
		super(oProductFile);
	}
	

	@Override
	public ProductViewModel getProductViewModel() {

		// Create the View Model
		GeorefProductViewModel oGpkgViewModel = new GeorefProductViewModel();
		
		// Set the name
		oGpkgViewModel.setFileName(m_oProductFile.getName());
		oGpkgViewModel.setName(m_oProductFile.getName());
		
		// Create the node group
		NodeGroupViewModel oNodeGroupViewModel = new NodeGroupViewModel();
		
		// Here we will add all our view models
		List<BandViewModel> aoBandsViewModels = new ArrayList<>();
		
		try {
			// Create the getools reader
			GeoPackage oGeoPackage = new GeoPackage(m_oProductFile);
			oGeoPackage.init();
			
			// Here we will try to read the bbox
			ReferencedEnvelope oGlobalEnv = null;
			
			// Lets check if the file includes vectors and/or rasters
			boolean bHasVectors = !oGeoPackage.features().isEmpty();
			boolean bHasRasters = !oGeoPackage.tiles().isEmpty();
			
			if (bHasVectors) {
				
				// We create a band for the vector layer
				BandViewModel oBandViewModel = new BandViewModel();
				oBandViewModel.setName("Vectors");
            	oBandViewModel.setPublished(false);
            	oBandViewModel.setGeoserverBoundingBox("");
            	oBandViewModel.setHeight(0);
            	oBandViewModel.setWidth(0);
            	oBandViewModel.setPublished(false);
            	
            	aoBandsViewModels.add(oBandViewModel);
				
            	// We search the outer bbox
				for (FeatureEntry oFeature : oGeoPackage.features()) {
	            	
	            	Envelope oEnvelope = oFeature.getBounds();
	            	
	            	if (oEnvelope!=null) {
	            		ReferencedEnvelope oLocalRefEnv = new ReferencedEnvelope(oEnvelope);
	            		if (oGlobalEnv==null) oGlobalEnv = new ReferencedEnvelope(oLocalRefEnv);
	            		else {
	            			oGlobalEnv.expandToInclude(oLocalRefEnv);
	            		}
	            	}
				}
			}
			
			if (bHasRasters) {
				
				BandViewModel oBandViewModel = new BandViewModel();
				oBandViewModel.setName("Raster");
            	oBandViewModel.setPublished(false);
            	oBandViewModel.setGeoserverBoundingBox("");
            	oBandViewModel.setHeight(0);
            	oBandViewModel.setWidth(0);
            	oBandViewModel.setPublished(false);
            	
            	aoBandsViewModels.add(oBandViewModel);
				
				for (TileEntry oTile : oGeoPackage.tiles()) {
	            	Envelope oEnvelope = oTile.getBounds();
	            	
	            	if (oEnvelope!=null) {
	            		ReferencedEnvelope oLocalRefEnv = new ReferencedEnvelope(oEnvelope);
	            		if (oGlobalEnv==null) oGlobalEnv = new ReferencedEnvelope(oLocalRefEnv);
	            		else {
	            			oGlobalEnv.expandToInclude(oLocalRefEnv);
	            		}
	            	}
	            	
				}
			}
			
			// Try to set the bbox
			if (oGlobalEnv!=null) {
				String sBbox = "";
				sBbox = oGlobalEnv.getMinY() + "," + oGlobalEnv.getMinX() + "," + oGlobalEnv.getMinY() + "," + oGlobalEnv.getMaxX() + "," + oGlobalEnv.getMaxY() + "," + oGlobalEnv.getMaxX() + "," + oGlobalEnv.getMaxY() + "," + oGlobalEnv.getMinX() + "," + oGlobalEnv.getMinY() + "," + oGlobalEnv.getMinX();
				oGpkgViewModel.setBbox(sBbox);
			}
			
			// Close the reader
			oGeoPackage.close();
			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("GpkgProductReader.getProductViewModel: error reading gpkg file ", oEx);
		}
		
		oNodeGroupViewModel.setNodeName("Bands");
		oNodeGroupViewModel.setBands(aoBandsViewModels);
		oGpkgViewModel.setBandsGroups(oNodeGroupViewModel);
		
		return oGpkgViewModel;		
	}

	@Override
	public String getProductBoundingBox() {
		
		String sBbox = "";

		try {
			// Create the getools reader
			GeoPackage oGeoPackage = new GeoPackage(m_oProductFile);
			oGeoPackage.init();
			
			// Here we will try to read the bbox
			ReferencedEnvelope oGlobalEnv = null;
			
			// Lets check if the file includes vectors and/or rasters
			boolean bHasVectors = !oGeoPackage.features().isEmpty();
			boolean bHasRasters = !oGeoPackage.tiles().isEmpty();
			
			if (bHasVectors) {
								
            	// We search the outer bbox
				for (FeatureEntry oFeature : oGeoPackage.features()) {
	            	
	            	Envelope oEnvelope = oFeature.getBounds();
	            	
	            	if (oEnvelope!=null) {
	            		ReferencedEnvelope oLocalRefEnv = new ReferencedEnvelope(oEnvelope);
	            		if (oGlobalEnv==null) oGlobalEnv = new ReferencedEnvelope(oLocalRefEnv);
	            		else {
	            			oGlobalEnv.expandToInclude(oLocalRefEnv);
	            		}
	            	}
				}
			}
			
			if (bHasRasters) {
				
				// We search the outer bbox
				for (TileEntry oTile : oGeoPackage.tiles()) {
	            	Envelope oEnvelope = oTile.getBounds();
	            	
	            	if (oEnvelope!=null) {
	            		ReferencedEnvelope oLocalRefEnv = new ReferencedEnvelope(oEnvelope);
	            		if (oGlobalEnv==null) oGlobalEnv = new ReferencedEnvelope(oLocalRefEnv);
	            		else {
	            			oGlobalEnv.expandToInclude(oLocalRefEnv);
	            		}
	            	}
	            	
				}
			}
			
			// Try to set the bbox
			if (oGlobalEnv!=null) {
				sBbox = oGlobalEnv.getMinY() + "," + oGlobalEnv.getMinX() + "," + oGlobalEnv.getMinY() + "," + oGlobalEnv.getMaxX() + "," + oGlobalEnv.getMaxY() + "," + oGlobalEnv.getMaxX() + "," + oGlobalEnv.getMaxY() + "," + oGlobalEnv.getMinX() + "," + oGlobalEnv.getMinY() + "," + oGlobalEnv.getMinX();
			}
			
			// Close the reader
			oGeoPackage.close();
			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("GpkgProductReader.getProductBoundingBox: error reading gpkg file ", oEx);
		}
		
		return sBbox;
	}

	@Override
	public MetadataViewModel getProductMetadataViewModel() {
		return new MetadataViewModel("Metadata");
	}

	@Override
	public String getEPSG() {
		String sEPSG = "";
		try {
			
			// Create the getools reader
			GeoPackage oGeoPackage = new GeoPackage(m_oProductFile);
			oGeoPackage.init();
						
			// Lets check if the file includes vectors and/or rasters
			boolean bHasVectors = !oGeoPackage.features().isEmpty();
			boolean bHasRasters = !oGeoPackage.tiles().isEmpty();
			
			if (bHasVectors) {
								
            	// We search the outer bbox
				for (FeatureEntry oFeature : oGeoPackage.features()) {
					
					int iSrid = oFeature.getSrid();
					
					sEPSG = "EPSG:"+iSrid;
					break;
				}
	            	
			}
			
			if (bHasRasters) {
				
				// We search the outer bbox
				for (TileEntry oTile : oGeoPackage.tiles()) {
					
					int iSrid = oTile.getSrid();
					
					sEPSG = "EPSG:"+iSrid;
					break;					
				}
			}
						
			// Close the reader
			oGeoPackage.close();			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("GpkgProductReader.getEPSG: error reading EPSF of GeoPackage file ", oEx);
		}
		
		return sEPSG;
	}
}
