package wasdi.shared.queryexecutors.jrc;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;

import wasdi.shared.config.DataProviderConfig;
import wasdi.shared.config.WasdiConfig;
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
	private String m_sBaseUrl = "";
	
	private static final Object s_oShapeFileLock = new Object();

	
	public QueryExecutorJRC() {
		m_oQueryTranslator = new QueryTranslatorJRC();
		m_oResponseTranslator = new ResponseTranslatorJRC();
	}
	
	@Override
	public void init() {
		super.init();
		
		try {
			JSONObject oAppConf = JsonUtils.loadJsonFromFile(m_sParserConfigPath);
			m_sShapeMaskPath = oAppConf.getString("shapeMaskPath");
			DataProviderConfig oDataProviderConfig = WasdiConfig.Current.getDataProviderConfig(m_sDataProviderCode);
			m_sBaseUrl = oDataProviderConfig.link + "CEMS-GLOFAS/flood_hazard";
			
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("QueryExecutorJRC.init(): exception reading parser config file " + m_sParserConfigPath);
		}		
	}
	
	@Override
    public String getUriFromProductName(String sProduct, String sProtocol, String sOriginalUrl, String sPlatform) {

        if (Utils.isNullOrEmpty(sOriginalUrl)) {
            WasdiLog.warnLog("QueryExecutorJRC.getUriFromProductName: sOriginalUrl is null, try to recover with the base implementation");
            return super.getUriFromProductName(sProduct, sProtocol, sOriginalUrl, sPlatform);
        }
        else {
        	if (sPlatform.equals(Platforms.JRC_GHSL)) {
	            String [] asParts = sOriginalUrl.split(";");
	            if (asParts != null) {
	                if (asParts.length>0) {
	                    return asParts[0];
	                }
	            }
        	} 
        	else if (sPlatform.equals(Platforms.GLOFAS)) {
        		return sOriginalUrl;
        	}
        }

        return "";
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
			
			if (oQueryVM.platformName.equals(Platforms.JRC_GHSL)) {
			
				Map<String, String> oTilesMap = getTilesInArea(oQueryVM.west, oQueryVM.north, oQueryVM.east, oQueryVM.south, oQueryVM.productName);
				iCount = oTilesMap.size();
					
			} 
			else if (oQueryVM.platformName.equals(Platforms.GLOFAS)) {
				
				double dN = oQueryVM.north.doubleValue();
				double dW = oQueryVM.west.doubleValue();
				double dS = oQueryVM.south.doubleValue();
				double dE = oQueryVM.east.doubleValue();
				
				iCount = getIntersectingTiles(dW, dS,dE, dN).size();
			}
			
		} 
		catch (Exception oEx) {
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
			return null;
		}
	
		
		try {
			// GHSL
			if (oQueryVM.platformName.equals(Platforms.JRC_GHSL)) {
			
				Map<String, String> oTilesMap = getTilesInArea(oQueryVM.west, oQueryVM.north, oQueryVM.east, oQueryVM.south, oQueryVM.productName);
			
				aoResults = ((ResponseTranslatorJRC) m_oResponseTranslator).translateGHSLResults(oTilesMap, oQuery.getLimit(), oQuery.getOffset());
			}
			// GLOFAS
			else if (oQueryVM.platformName.equals(Platforms.GLOFAS)) {
				String sReturnPeriods = oQueryVM.sensorMode;
				String sProductType = oQueryVM.productType;
				
				double dN = oQueryVM.north.doubleValue();
				double dW = oQueryVM.west.doubleValue();
				double dS = oQueryVM.south.doubleValue();
				double dE = oQueryVM.east.doubleValue();
				
				int iOffset = Integer.parseInt(oQuery.getOffset());
				int iLimit = Integer.parseInt(oQuery.getLimit());
				
				
				List<TileInfo> asIntersectingTilesNames = getIntersectingTiles(dW, dS,dE, dN);
				
				if (iOffset > -1 && iLimit > -1) {
					int iEnd = iOffset + iLimit < asIntersectingTilesNames.size() 
							? iOffset + iLimit
							: asIntersectingTilesNames.size() ;
					asIntersectingTilesNames = asIntersectingTilesNames.subList(iOffset, iEnd);
				}
				
				// TODO: need to paginate the queries
				
				if(asIntersectingTilesNames.size() > 0) {
					for (int i = 0; i < asIntersectingTilesNames.size(); i++) {
						TileInfo oTile = asIntersectingTilesNames.get(i);
						String sTitle = "ID" + oTile.id + "_" + oTile.name  + "_RP" + sReturnPeriods + "_" + sProductType + ".tif";
						String sLink = m_sBaseUrl + "/RP" + sReturnPeriods + "/" + sTitle;
						String sFootPrint = asIntersectingTilesNames.get(i).footprint;
						
						QueryResultViewModel oResVM = new QueryResultViewModel();
						oResVM.setId(sTitle);
						oResVM.setTitle(sTitle);
						oResVM.setLink(sLink);
						oResVM.setFootprint(sFootPrint);
						oResVM.setProvider("JRC");
						oResVM.setPlatform("GLOFAS");
						
						aoResults.add(oResVM);
					}
				} else {
					WasdiLog.debugLog("QueryExecutorJRC.GLOFAS. No matching tiles found for GLOFAS search query.");
				}
			}
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorJRC.executeAndRetrieve. Error when trying to retrieve the tiles for the given query: ", oEx);
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
	
	public List<TileInfo> getIntersectingTiles(double dWest, double dSouth, double dEast, double dNorth) throws Exception {
	    WasdiLog.debugLog("QueryExecutorJRC.getIntersectingTiles. Downloading and parsing tile_extents.geojson");
	    
	    HttpRequest oRequest = HttpRequest.newBuilder().uri(URI.create(m_sBaseUrl + "/tile_extents.geojson")).build();
	    HttpClient oClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
	    HttpResponse<String> oResponse = oClient.send(oRequest, HttpResponse.BodyHandlers.ofString());
	    
	    JSONObject oGeojson = new JSONObject(oResponse.body());
	    JSONArray oFeatures = oGeojson.getJSONArray("features");
	    
	    List<TileInfo> aoMatchingTiles = new ArrayList<>();
	    
	    List<String> asMatchingTiles = new ArrayList<>();

	    for (int i = 0; i < oFeatures.length(); i++) {
	        JSONObject oFeature = oFeatures.getJSONObject(i);
	        JSONObject oProperties = oFeature.getJSONObject("properties");
	        
	        String sId = "";
	        String sName = "";
	        
	        if (oProperties != null) {
	        	sId = oProperties.optString("id", oFeature.optString("id"));
		        sName = oFeature.getJSONObject("properties").optString("name", sId);	
	        }
	        else 
	        	continue;
	        
	        // Extract coordinates to calculate the tile's bounding box
	        JSONArray arrCoordinates = oFeature.getJSONObject("geometry").getJSONArray("coordinates").getJSONArray(0);
	        
	        double dTileWest = Double.MAX_VALUE;
	        double dTileEast = -Double.MAX_VALUE;
	        double dTileSouth = Double.MAX_VALUE;
	        double dTileNorth = -Double.MAX_VALUE;
	        
	        // StringBuilder to format the footprint as WKT POLYGON
	        StringBuilder sbFootprint = new StringBuilder("POLYGON((");
	        
	        for (int j = 0; j < arrCoordinates.length(); j++) {
	            JSONArray arrCoord = arrCoordinates.getJSONArray(j);
	            double dLon = arrCoord.getDouble(0);
	            double dLat = arrCoord.getDouble(1);
	            
	            if (dLon < dTileWest) dTileWest = dLon;
	            if (dLon > dTileEast) dTileEast = dLon;
	            if (dLat < dTileSouth) dTileSouth = dLat;
	            if (dLat > dTileNorth) dTileNorth = dLat;
	            
	            // Append to POLYGON string (Format: LON LAT)
	            sbFootprint.append(dLon).append(" ").append(dLat);
	            if (j < arrCoordinates.length() - 1) {
	                sbFootprint.append(", ");
	            }
	        }
	        
	        sbFootprint.append("))");
	        
	        // AABB (Axis-Aligned Bounding Box) intersection test
	        boolean bIntersects = (dWest <= dTileEast && dEast >= dTileWest 
	        						&& dSouth <= dTileNorth && dNorth >= dTileSouth);
	                              
	        if (bIntersects && !sId.isEmpty()) {
	        	TileInfo objTile = new TileInfo(sId, sName, sbFootprint.toString());
	            aoMatchingTiles.add(objTile);
	        }
	    }
	    
	    WasdiLog.debugLog("QueryExecutorJRC.getIntersectingTiles. Found " + asMatchingTiles.size() + " overlapping tile(s): " + asMatchingTiles);
	    return aoMatchingTiles;
	}
	


}
