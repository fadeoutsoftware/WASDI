package wasdi.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.io.FileUtils;
import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.json.JSONObject;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import wasdi.shared.config.WasdiConfig;
import wasdi.shared.queryexecutors.jrc.ResponseTranslatorJRC;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.products.BandViewModel;
import wasdi.shared.viewmodels.products.MetadataViewModel;
import wasdi.shared.viewmodels.products.NodeGroupViewModel;
import wasdi.shared.viewmodels.products.ProductViewModel;

public class GHSLTilesProductReader extends WasdiProductReader {
	
	private static final Object s_oShapeFileLock = new Object();

	public GHSLTilesProductReader(File oProductFile) {
		super(oProductFile);
	}

	@Override
	public ProductViewModel getProductViewModel() {
		ProductViewModel oViewModel = new ProductViewModel();

        String sFileName = m_oProductFile != null ? m_oProductFile.getName() : "no_file_name";
        
    	oViewModel.setFileName(sFileName);
    	oViewModel.setName(WasdiFileUtils.getFileNameWithoutLastExtension(sFileName));
		oViewModel.setProductFriendlyName(WasdiFileUtils.getFileNameWithoutLastExtension(sFileName));
        
        NodeGroupViewModel oNodeGroupViewModel = new NodeGroupViewModel();
    	oNodeGroupViewModel.setNodeName("Bands");
    	
    	// so far, we do not try to read the bands
    	List<BandViewModel> oBands = new ArrayList<>();
    	oNodeGroupViewModel.setBands(oBands);
    	oViewModel.setBandsGroups(oNodeGroupViewModel);
        	
		return oViewModel;
	}
	
	/**
	 * Extract the tile id from the name of the file
	 * @return the id of the tile the file refers to
	 */
	private String getTileId() {
		String sFileName = m_oProductFile.getName();
		String sPrefix = ResponseTranslatorJRC.s_sFileNamePrefix;
		String sTileId = "";
		if (sFileName.toUpperCase().startsWith(sFileName)) {
			// remove the file extension
			sFileName = FileUtils.getFilenameWithoutExtension(sFileName);
			sTileId = sFileName.replace(sPrefix, "").trim();
		} else {
			WasdiLog.debugLog("GHSLTilesReader.getTileId. The file prefix does not match the product name prefix. Prdouct name: " + sFileName);
		}
		
		return sTileId;
	}

	
	@Override
	public String getProductBoundingBox() {
		// to get the bounding box, we use the same shape file that we use to read and search bands.e 
		
		List<String> asBBoxes = new ArrayList<>();
		
		String sParserConfigPath = WasdiConfig.Current.getDataProviderConfig("JRC").parserConfig;
		
		JSONObject oAppConf = JsonUtils.loadJsonFromFile(sParserConfigPath);
		String sShapeMaskPath = oAppConf.getString("shapeMaskPath");
		
		String sTileId = getTileId();
		
		String sRes = "";
		
		synchronized (s_oShapeFileLock) {
			
			FileDataStore oStore = null;
			FeatureIterator<SimpleFeature> aoFeaturesIterator = null;
			
			// Get the Data Store
			try {
				oStore = FileDataStoreFinder.getDataStore(new File(sShapeMaskPath));
				
				FeatureSource<SimpleFeatureType, SimpleFeature> aoSource = oStore.getFeatureSource();
				
				Filter oFilter = ECQL.toFilter("tile_id='" + sTileId +  "'");
				
				FeatureCollection<SimpleFeatureType, SimpleFeature> oCollection = aoSource.getFeatures(oFilter);
				
				aoFeaturesIterator = oCollection.features();
				
				
				while (aoFeaturesIterator.hasNext()) {
					SimpleFeature oFeature = aoFeaturesIterator.next();
	                
	                List<Object> aoAttributes = oFeature.getAttributes();
	                	                
	             // the first attribute in the list represents the bounding box of the product
	                asBBoxes.add(aoAttributes.get(0).toString()); 
				}	
				
			} catch (IOException oEx) {
				WasdiLog.errorLog("GHSLTilesReader.getProductBoundingBox. Error reading the shape file. " + oEx.getMessage() );
			} catch (CQLException oEx) {
				WasdiLog.errorLog("GHSLTilesReader.getProductBoundingBox. Error while creating  " + oEx.getMessage() );
			}
			finally {
				if (aoFeaturesIterator != null) {
					aoFeaturesIterator.close();
				}
				
				if (oStore != null) 
					oStore.dispose();
			}
		} // from here on, we do not access the shape file anymore, so we can release the lock.
		
		if (asBBoxes.size() == 1) {
			String sBBoxFormat = asBBoxes.get(0);
			
			List<String> asCoordinates = Arrays.asList(sBBoxFormat.split(", "));
			
			List<Double> adLongitude = asCoordinates.stream()
					.map(sPair -> sPair.split(" "))
					.map(aCoordinates -> aCoordinates[0].trim())
					.map(sLongitude -> Double.parseDouble(sLongitude))
					.collect(Collectors.toList());
			
			List<Double> adLatitude = asCoordinates.stream()
					.map(sPair -> sPair.split(" "))
					.map(aCoordinates -> aCoordinates[1].trim())
					.map(sLatitude -> Double.parseDouble(sLatitude))
					.collect(Collectors.toList());
			
			// y is the latitude, x is the longitude
			double dMinY = Collections.min(adLatitude);
			double dMaxY = Collections.max(adLatitude);
			double dMinX = Collections.min(adLongitude);
			double dMaxX = Collections.max(adLongitude);
			
			// expected format: "%f,%f,%f,%f,%f,%f,%f,%f,%f,%f", minY, minX, minY, maxX, maxY, maxX, maxY, minX, minY, minX
			sRes = String.format("%f,%f,%f,%f,%f,%f,%f,%f,%f,%f", 
					(float) dMinY, (float) dMinX, (float) dMinY, (float) dMaxX, (float) dMaxY, (float) dMaxX, (float) dMaxY, (float) dMinX, (float) dMinY, (float) dMinX);	
		} 
		else {
			WasdiLog.errorLog("GHSLTilesReader.getProductBoundingBox. Zero or more than one tile was found. Returning empty bounding box. Tile id: " + sTileId);
			sRes = "";
		}
			
		return sRes;
	}

	@Override
	public MetadataViewModel getProductMetadataViewModel() {
		return new MetadataViewModel("Metadata");
	}


	@Override
	public File getFileForPublishBand(String sBand, String sLayerId, String sPlatform) {
    	WasdiLog.debugLog("GHSLTilesProductReader.readSnapProduct: publishing of bands is not yet supported");
		return null;
	}
	
	@Override
	protected Product readSnapProduct() {
    	WasdiLog.debugLog("GHSLTilesProductReader.readSnapProduct: we do not want SNAP to read GHS products, return null ");
    	return null;        	
	}

}
