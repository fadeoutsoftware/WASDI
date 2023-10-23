package wasdi.shared.queryexecutors.jrc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap ;

import org.apache.logging.log4j.util.Strings;
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
import org.osgeo.proj4j.CRSFactory;
import org.osgeo.proj4j.CoordinateReferenceSystem;
import org.osgeo.proj4j.CoordinateTransform;
import org.osgeo.proj4j.CoordinateTransformFactory;
import org.osgeo.proj4j.ProjCoordinate;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

public class QueryExecutorJRC extends QueryExecutor {
	
	protected final static String s_sESRI54009 = "ESRI:54009";
	protected final static String s_sEPSG4326 = "EPSG:4326";
	private String m_sShapeMaskPath;
	
	public QueryExecutorJRC() {
		m_sProvider = "JRC";
		m_oQueryTranslator = new QueryTranslatorJRC();
		m_oResponseTranslator = new ResponseTranslatorJRC();
		m_asSupportedPlatforms.add(Platforms.STATICS_TILES);
	}
	
	@Override
	public void init() {
		super.init();
		
		try {
			JSONObject oAppConf = WasdiFileUtils.loadJsonFromFile(m_sParserConfigPath);
			m_sShapeMaskPath = oAppConf.getString("shapeMaskPath");
			
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("QueryExecutorVIIRS.init(): exception reading parser config file " + m_sParserConfigPath);
		}		
	}

	@Override
	public int executeCount(String sQuery) {
		
		WasdiLog.debugLog("QueryExecutorJRC.executeCount. Received query: " + sQuery);
		
		int iCount = -1;
		
		QueryViewModel oQueryVM = m_oQueryTranslator.parseWasdiClientQuery(sQuery);
		
		if (!m_asSupportedPlatforms.contains(oQueryVM.platformName)) {
			WasdiLog.debugLog("QueryExecutorJRC.executeCount. Plaform not supported by the data provider for query: " + sQuery);
			return iCount;
		}
		
		// convert the coordinate system before accessing the shape file 
		Double oW = oQueryVM.west != null ? translateLongitude(oQueryVM.west, s_sEPSG4326, s_sESRI54009) : null;
		Double oE = oQueryVM.east != null ? translateLongitude(oQueryVM.east, s_sEPSG4326, s_sESRI54009) : null;
		Double oS = oQueryVM.south != null ? translateLatitude(oQueryVM.south, s_sEPSG4326, s_sESRI54009) : null;
		Double oN = oQueryVM.north != null ? translateLatitude(oQueryVM.north, s_sEPSG4326, s_sESRI54009) : null;
		
		try {
			
			Map<String, String> oTilesMap = getTilesInArea(oW, oN, oE, oS, oQueryVM.productName);
			iCount = oTilesMap.size();
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorJRC.executeCount. Error when trying to retrieve the tiles for the given query: " + sQuery);
			iCount = -1;
		}
		
		return iCount;
	}

	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		
		WasdiLog.debugLog("QueryExecutorJRC.executeAndRetrieve. Received query: " + oQuery.getQuery());

		
		List<QueryResultViewModel> aoResults = new ArrayList<>();
		
		QueryViewModel oQueryVM = m_oQueryTranslator.parseWasdiClientQuery(oQuery.getQuery());
		
		if (!m_asSupportedPlatforms.contains(oQueryVM.platformName)) {
			WasdiLog.debugLog("QueryExecutorJRC.executeAndRetrieve. Plaform not supported by the data provider for query: " + oQuery.getQuery());
			return aoResults;
		}
		
		// convert the coordinate system before accessing the shape file 
		Double oW = oQueryVM.west != null ? translateLongitude(oQueryVM.west, s_sEPSG4326, s_sESRI54009) : null;
		Double oE = oQueryVM.east != null ? translateLongitude(oQueryVM.east, s_sEPSG4326, s_sESRI54009) : null;
		Double oS = oQueryVM.south != null ? translateLatitude(oQueryVM.south, s_sEPSG4326, s_sESRI54009) : null;
		Double oN = oQueryVM.north != null ? translateLatitude(oQueryVM.north, s_sEPSG4326, s_sESRI54009) : null;
		
		
		try {
			
			Map<String, String> oTilesMap = getTilesInArea(oW, oN, oE, oS, oQueryVM.productName);
			
			aoResults = ((ResponseTranslatorJRC) m_oResponseTranslator).translateResults(oTilesMap, oQuery.getLimit(), oQuery.getOffset());
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorJRC.executeAndRetrieve. Error when trying to retrieve the tiles for the given query: " + oQuery.getQuery());
		}
		
		return aoResults;
	}
	
	private Map<String, String> getTilesInArea(Double oWest, Double oNorth, Double oEast, Double oSouth, String sProductName) {
		
		Map<String, String> aooTiles = new LinkedHashMap <String, String>(); // we choose an implementation that maintains the insertion order (useful for paginated queries)
		
//		String sShapeFileMask = "C:/Users/valentina.leone/Desktop/WORK/GHS/GHSL_data_54009_shapefile/GHSL2_0_MWD_L1_tile_schema_land.shp"; // TODO: questo parametro dovrà essere letto da qualche parte
		
		synchronized (m_sShapeMaskPath) {
			
			WasdiLog.debugLog("QueryExecutorJRC.getTilesInArea. Reading shape file: " + m_sShapeMaskPath);

			FileDataStore oStore = null;
			
			// Get the Data Store
			try {
				oStore = FileDataStoreFinder.getDataStore(new File(m_sShapeMaskPath));
				
				FeatureSource<SimpleFeatureType, SimpleFeature> aoSource = oStore.getFeatureSource();
				
				Filter oFilter = getFilter(oWest, oNorth, oEast, oSouth, sProductName);
				
				WasdiLog.debugLog("QueryExecutorJRC.getTilesInArea. Generated filter: " + oFilter.toString());
				
				if (oFilter != null) {
				
					FeatureCollection<SimpleFeatureType, SimpleFeature> oCollection = aoSource.getFeatures(oFilter);
					
					FeatureIterator<SimpleFeature> aoFeatures = oCollection.features();
					
					
					while (aoFeatures.hasNext()) {
						SimpleFeature oFeature = aoFeatures.next();
		                
		                List<Object> aoAttributes = oFeature.getAttributes();
		                
		                String sBoundingBoxESRI = aoAttributes.get(0).toString();
		                
		                String sTileId = aoAttributes.get(1).toString();
		                
		                aooTiles.put(sTileId, sBoundingBoxESRI);
		                
					}	
				} 
				
			} catch (IOException oEx) {
				WasdiLog.errorLog("QueryExecutorJRC.getTilesInArea. Error reading the shape file. " + oEx.getMessage() );
			} 
			
		}
		
		return aooTiles;
	}
	
	
	private Filter getFilter(Double oWest, Double oNorth, Double oEast, Double oSouth, String sProductName) {
		Filter oFilter = null;
		
		List<String> asFilterElements = new ArrayList<>(); 
		
		if (oWest != null && oNorth != null && oEast != null && oSouth != null ) {
			double sMinLong = oWest.doubleValue();
			double sMinLat = oSouth.doubleValue();
			double sMaxLong = oEast.doubleValue();
			double sMaxLat = oNorth.doubleValue();
			
			String sQueryBbox = "BBOX(the_geom, " + sMinLong + ", " + sMinLat + ", " +sMaxLong + ", " + sMaxLat + ")";
			asFilterElements.add(sQueryBbox);
		}
		
		if (!Utils.isNullOrEmpty(sProductName)) {
			String sNameWithoutPrefix = sProductName.replace(ResponseTranslatorJRC.s_sFileNamePrefix, "");
			String sTileId = WasdiFileUtils.removeZipExtension(sNameWithoutPrefix);
			
			String sQueryTileId = "tile_id='" + sTileId + "'";
			asFilterElements.add(sQueryTileId);
		}
		

		try { 
			if (asFilterElements.isEmpty()) {
				oFilter = Filter.INCLUDE;
			}
			else {
				String sFilter = String.join(" AND ", asFilterElements);
				oFilter = ECQL.toFilter(sFilter);
			}	
			
			
		} catch (CQLException oEx) {
			WasdiLog.debugLog("QueryExecutorJRC.getFilter. Error while creating the filter for the shape mask. " + oEx.getMessage());
		}
		
		return oFilter;
	}
	
	public static double translateLongitude(double dLongitude, String sSourceEncoding, String sTargetEncoding) {
		return translateCoordinate(dLongitude, 0, sSourceEncoding, sTargetEncoding)[0];
	}
	
	
	public static double translateLatitude(double dLatitude, String sSourceEncoding, String sTargetEncoding) {
		return translateCoordinate(0, dLatitude, sSourceEncoding, sTargetEncoding)[1];
	}
	
	
    public static double[] translateCoordinate(double dLongitude, double dLatitude, String sSourceEncoding, String sTargetEncoding) {

        // Create CRS instances
        CRSFactory oCrsFactory = new CRSFactory();
        CoordinateReferenceSystem oSourceCRSRS = oCrsFactory.createFromName(sSourceEncoding);
        CoordinateReferenceSystem oTargetCRSRS = oCrsFactory.createFromName(sTargetEncoding);

        // Create a CoordinateTransform instance
        CoordinateTransform transform = new CoordinateTransformFactory().createTransform(oSourceCRSRS, oTargetCRSRS);


        // Transform the coordinates
        ProjCoordinate oSourceCoord = new ProjCoordinate(dLongitude, dLatitude);
        ProjCoordinate oTargetCoord = new ProjCoordinate();
        transform.transform(oSourceCoord, oTargetCoord);

        // Extract the transformed coordinates
        double dTargetLong = oTargetCoord.x;
        double dTargetLat = oTargetCoord.y;
        
        return new double[] {dTargetLong, dTargetLat};
    }
    


}
