package wasdi.shared.queryexecutors.jrc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap ;
import java.util.List;
import java.util.Map;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.json.JSONObject;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

public class QueryExecutorJRC extends QueryExecutor {

	private String m_sShapeMaskPath;
	
	private static final Object s_oShapeFileLock = new Object();

	
	public QueryExecutorJRC() {
		m_sProvider = "JRC";
		m_oQueryTranslator = new QueryTranslatorJRC();
		m_oResponseTranslator = new ResponseTranslatorJRC();
		m_asSupportedPlatforms.add(Platforms.JRC_GHSL);
	}
	
	@Override
	public void init() {
		super.init();
		
		try {
			JSONObject oAppConf = JsonUtils.loadJsonFromFile(m_sParserConfigPath);
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
		
		try {
			
			Map<String, String> oTilesMap = getTilesInArea(oQueryVM.west, oQueryVM.north, oQueryVM.east, oQueryVM.south, oQueryVM.productName);
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
	
		
		try {
			
			Map<String, String> oTilesMap = getTilesInArea(oQueryVM.west, oQueryVM.north, oQueryVM.east, oQueryVM.south, oQueryVM.productName);
			
			aoResults = ((ResponseTranslatorJRC) m_oResponseTranslator).translateResults(oTilesMap, oQuery.getLimit(), oQuery.getOffset());
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorJRC.executeAndRetrieve. Error when trying to retrieve the tiles for the given query: " + oQuery.getQuery());
		}
		
		return aoResults;
	}
	
	private Map<String, String> getTilesInArea(Double oWest, Double oNorth, Double oEast, Double oSouth, String sProductName) {
		
		Map<String, String> aooTiles = new LinkedHashMap <String, String>(); // we choose an implementation that maintains the insertion order (useful for paginated queries)
				
		synchronized (s_oShapeFileLock) {
			
			WasdiLog.debugLog("QueryExecutorJRC.getTilesInArea. Reading shape file: " + m_sShapeMaskPath);

			FileDataStore oStore = null;
			
			FeatureIterator<SimpleFeature> aoFeaturesIterator = null;
			
			// Get the Data Store
			try {
				oStore = FileDataStoreFinder.getDataStore(new File(m_sShapeMaskPath));
				SimpleFeatureSource aoSource = oStore.getFeatureSource();
				Filter oFilter = getFilter(oWest, oNorth, oEast, oSouth, sProductName);
				
				
				if (oFilter != null) {
					
					WasdiLog.debugLog("QueryExecutorJRC.getTilesInArea. Generated filter: " + oFilter.toString());
				
					SimpleFeatureCollection oFilteredFeatures = aoSource.getFeatures(oFilter);
				
					
					if (oFilteredFeatures != null) {
					
						WasdiLog.debugLog("QueryExecutorJRC.getTilesInArea. Some results after filtering features");
						WasdiLog.debugLog("QueryExecutorJRC.getTilesInArea. Number of filtered features: " + oFilteredFeatures.size());

						aoFeaturesIterator = oFilteredFeatures.features();
						
						while (aoFeaturesIterator.hasNext()) {
							
							SimpleFeature oFeature = aoFeaturesIterator.next();
			                List<Object> aoAttributes = oFeature.getAttributes();
			                String sBoundingBoxESRI = aoAttributes.get(0).toString();
			                String sTileId = aoAttributes.get(1).toString();
			                aooTiles.put(sTileId, sBoundingBoxESRI);
			                
						}	
					}
				} else {
					WasdiLog.debugLog("QueryExecutorJRC.getTilesInArea. The filter is null. No tiles retrieved.");
				}
				
			} catch (IOException oEx) {
				WasdiLog.errorLog("QueryExecutorJRC.getTilesInArea. Error reading the shape file. " + oEx.getMessage() );
			} finally {
				if (aoFeaturesIterator != null) {
					aoFeaturesIterator.close();
				}
				if (oStore != null) {
					oStore.dispose();
				}
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
			else if (asFilterElements.size() == 1) {
				oFilter = ECQL.toFilter(asFilterElements.get(0));
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
	
	@Override
    public String getUriFromProductName(String sProduct, String sProtocol, String sOriginalUrl) {

        if (Utils.isNullOrEmpty(sOriginalUrl)) {
            WasdiLog.warnLog("QueryExecutorJRC.getUriFromProductName: sOriginalUrl is null, try to recover with the base implementation");
            return super.getUriFromProductName(sProduct, sProtocol, sOriginalUrl);
        }
        else {
            String [] asParts = sOriginalUrl.split(";");
            if (asParts != null) {
                if (asParts.length>0) {
                    return asParts[0];
                }
            }
        }

        return "";
    }

}
