package wasdi.shared.queryexecutors.lake3d;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.json.JSONObject;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.queryexecutors.globathy.LakeInfo;
import wasdi.shared.queryexecutors.globathy.QueryExecutorGlobathy;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

public class QueryExecutorLake3d extends QueryExecutor {
	
	private String m_sLakesShapeFilePath = "";	// path to the shp file

	public QueryExecutorLake3d() {
		this.m_oQueryTranslator = new QueryTranslatoryLake3d();
		this.m_oResponseTranslator = new ResponseTranslatorLake3d();
		this.m_asSupportedPlatforms.add(Platforms.LAKE_3D);
	}
	
	
	@Override
	public void init() {
		super.init();
		
		try {
			JSONObject oAppConf = JsonUtils.loadJsonFromFile(m_sParserConfigPath);
			m_sLakesShapeFilePath = oAppConf.getString("lakesShapeFilePath");
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorLake3d.init. Exception reading parser config file " + m_sParserConfigPath, oEx);
		}
		
	}
	
	@Override
	public String getUriFromProductName(String sProduct, String sProtocol, String sOriginalUrl, String sPlatform) {
		if (sProduct.toLowerCase().endsWith("_L2.csv")) {
			return sOriginalUrl;
		}
		return null;
	}

	
	@Override
	public int executeCount(String sQuery) {
		
		int iCount = -1;
		
		try {
			QueryViewModel oQueryVM = m_oQueryTranslator.parseWasdiClientQuery(sQuery);
			
			if (!m_asSupportedPlatforms.contains(oQueryVM.platformName)) {
				return iCount;
			}
			
			// as a first approximation, we only want the lakes within a specific bounding box.
			// Nothing else is required
			
			Double dNorth = oQueryVM.north;
			Double dEast = oQueryVM.east;
			Double dSouth = oQueryVM.south;
			Double dWest = oQueryVM.west;
			
			if (dNorth == null)
				dNorth = 90d;
			if (dEast == null)
				dEast = 180d;
			if (dSouth == null)
				dSouth = -90d;
			if (dWest == null)
				dWest = -180d;
			
			iCount = countFeatures(dNorth, dEast, dSouth, dWest);
			
			
		} 
		catch (Exception oE) {
			WasdiLog.errorLog("QueryExecutorLake3d.executeCount. Exception ", oE);
		}
		
		return iCount;
	}

	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		
		List<QueryResultViewModel> aoResults = null;
		
		try {
			QueryViewModel oQueryVM = m_oQueryTranslator.parseWasdiClientQuery(oQuery.getQuery());
			
			if (!m_asSupportedPlatforms.contains(oQueryVM.platformName)) {
				return aoResults;
			}
			
			// as a first approximation, we only want the lakes within a specific bounding box.
			// Nothing else is required
			
			int iOffset = -1;
			int iLimit = -1;
			
			try {
		    	iOffset = Integer.parseInt(oQuery.getOffset());
		    }
		    catch (Exception oE) {
		    	WasdiLog.warnLog("QueryExecutorLake3d.executeAndRetrieve: " + oE.toString());
			}
		    
		    try {
		    	iLimit = Integer.parseInt(oQuery.getLimit());
		    }
		    catch (Exception oE) {
		    	WasdiLog.warnLog("QueryExecutorLake3d.executeAndRetrieve: " + oE.toString());
			}
			
			Double dNorth = oQueryVM.north;
			Double dEast = oQueryVM.east;
			Double dSouth = oQueryVM.south;
			Double dWest = oQueryVM.west;
			
			if (dNorth == null)
				dNorth = 90d;
			if (dEast == null)
				dEast = 180d;
			if (dSouth == null)
				dSouth = -90d;
			if (dWest == null)
				dWest = -180d;
			
			ArrayList<LakeInfo> aoLakes = getFeatures(dNorth, dEast, dSouth, dWest);
			
			if (aoLakes == null) {
				WasdiLog.warnLog("QueryExecutorLake3d.executeAndRetrieve. Error retrieving the products");
				return aoResults;
			}
			
			if (iOffset > -1 && iLimit > -1) {
				int iStart = iOffset;
				int iEnd = iStart + iLimit;
				if (iEnd > aoLakes.size()) iEnd = aoLakes.size();
				aoLakes = new ArrayList<>(aoLakes.subList(iStart, iEnd));
			}
			
			aoResults = aoLakes.stream().map(this::getResultVM).collect(Collectors.toList());		
			
		} 
		catch (Exception oE) {
			WasdiLog.errorLog("QueryExecutorLake3d.executeAndRetrieve. Exception ", oE);
		}
		
		
		return aoResults;
	}
	
	
	private int countFeatures(double dNorth, double dEast, double dSouth, double dWest) {
		
		int iCount = -1;
		
		synchronized (QueryExecutorGlobathy.s_oShapeFileLock){
			
			try {
				// Get the Data Store
				FileDataStore oStore = FileDataStoreFinder.getDataStore(new File(m_sLakesShapeFilePath));
				
				try {
					SimpleFeatureSource oFeatureSource = oStore.getFeatureSource();		
					SimpleFeatureCollection oIntersectedFeatures = intersectBoundingBox(dNorth, dEast, dSouth, dWest, oFeatureSource);
					iCount = oIntersectedFeatures.size();
				}
				finally {
					oStore.dispose();
				}
			}
			catch (Exception oEx) {
				WasdiLog.errorLog("QueryExecutorLake3d.countFeatures.Exception ", oEx);
			}
		}
		
		return iCount;	
	}
	
	private ArrayList<LakeInfo> getFeatures(double dNorth, double dEast, double dSouth, double dWest) {
		
		ArrayList<LakeInfo> aoLakeInfo = null;
		
		synchronized (QueryExecutorGlobathy.s_oShapeFileLock){
			
			try {
				// Get the Data Store
				FileDataStore oStore = FileDataStoreFinder.getDataStore(new File(m_sLakesShapeFilePath));
				
				
				try {
					SimpleFeatureSource oFeatureSource = oStore.getFeatureSource();
					
					SimpleFeatureCollection oIntersectedFeatures = intersectBoundingBox(dNorth, dEast, dSouth, dWest, oFeatureSource);
					
					if (oIntersectedFeatures != null) {
						try (SimpleFeatureIterator oIterator = oIntersectedFeatures.features()) {
						
							aoLakeInfo = new ArrayList<>();
							
							while (oIterator.hasNext()) {
								SimpleFeature oFeature = oIterator.next();
								
								LakeInfo oInfo = getLakeInfo(oFeature);
								
								aoLakeInfo.add(oInfo);
								
							}	
						}
					}
					
				}
				finally {
					if (oStore != null)
						oStore.dispose();
				}
			}
			catch (Exception oEx) {
				WasdiLog.errorLog("QueryExecutorLake3d.countFeatures.Exception ", oEx);
				aoLakeInfo = null;
			}
		}
		
		return aoLakeInfo;	
	}
	
	private LakeInfo getLakeInfo(SimpleFeature oFeature) {
		
		LakeInfo oInfo = new LakeInfo();
		oInfo.setHylak(oFeature.getAttribute("Hylak_id").toString());
		oInfo.setLakeName(oFeature.getAttribute("Lake_name").toString());
		oInfo.setCountry(oFeature.getAttribute("Country").toString());
		oInfo.setContinent(oFeature.getAttribute("Continent").toString());
		oInfo.setPolySrc(oFeature.getAttribute("Poly_src").toString());
		oInfo.setLakeType(oFeature.getAttribute("Lake_type").toString());
		oInfo.setGrandId(oFeature.getAttribute("Grand_id").toString());
		oInfo.setLakeArea(oFeature.getAttribute("Lake_area").toString());
		oInfo.setShoreLen(oFeature.getAttribute("Shore_len").toString());
		oInfo.setShoreDev(oFeature.getAttribute("Shore_dev").toString());
		oInfo.setVolTotal(oFeature.getAttribute("Vol_total").toString());
		oInfo.setVolRes(oFeature.getAttribute("Vol_res").toString());
		oInfo.setVolSrc(oFeature.getAttribute("Vol_src").toString());
		oInfo.setDepthAvg(oFeature.getAttribute("Depth_avg").toString());
		oInfo.setDisAvg(oFeature.getAttribute("Dis_avg").toString());
		oInfo.setResTime(oFeature.getAttribute("Res_time").toString());
		oInfo.setElevation(oFeature.getAttribute("Elevation").toString());
		oInfo.setSlope100(oFeature.getAttribute("Slope_100").toString());
		oInfo.setWshdArea(oFeature.getAttribute("Wshd_area").toString());
		oInfo.setPourLong(oFeature.getAttribute("Pour_long").toString());
		oInfo.setPourLat(oFeature.getAttribute("Pour_lat").toString());
		oInfo.setGeometry(oFeature.getAttribute("the_geom").toString());
		
		return oInfo;
	}
	
	private SimpleFeatureCollection intersectBoundingBox(double dNorth, double dEast, double dSouth, double dWest, SimpleFeatureSource oFeatureSource) {
		try {
			// get the actual name of the geometry
			
			String sGeometryName = oFeatureSource.getSchema().getGeometryDescriptor().getLocalName();
			WasdiLog.debugLog("QueryExecutorLake3d.intersectBoundingBox. Geometry name: " + sGeometryName);

			FilterFactory2 oFilterFactory = CommonFactoryFinder.getFilterFactory2();

			ReferencedEnvelope oBbox = new ReferencedEnvelope(
			    dWest, dEast,
			    dSouth, dNorth,
			    DefaultGeographicCRS.WGS84
			);

			Filter oFilter = oFilterFactory.bbox(
					oFilterFactory.property(sGeometryName),
					oBbox
			);
			
			return oFeatureSource.getFeatures(oFilter);
			
		} catch (Exception oE) {
			WasdiLog.errorLog("QueryExecutorLake3d.intersectBoundingBox. Error ", oE);
		}
		
		return null;
	}
	
	
	private QueryResultViewModel getResultVM(LakeInfo oLakeInfo) {
		
		QueryResultViewModel oResVM = new QueryResultViewModel();
		
		String sLakeId = oLakeInfo.getHylak();
		String sTitle = sLakeId + "_L2.csv";
		oResVM.setId(sLakeId);
		oResVM.setTitle(sTitle);
		
		
		// get size
		long lFileSize = 4000L;
		
		oResVM.setLink("https://" + sTitle + "," + lFileSize);
		oResVM.setSummary("");
		oResVM.setProvider("GLOBATHY");
		String sFootprint = simplifyLake(oLakeInfo.getGeometry());
		oResVM.setFootprint(sFootprint);
		
		
		
		HashMap<String, String> aoPropertiesMap = new HashMap<>();
		
		if(!Utils.isNullOrEmpty(oLakeInfo.getCountry())) {
			aoPropertiesMap.put("country", oLakeInfo.getCountry());
			oResVM.setSummary("Mode: " + oLakeInfo.getCountry());
		}
		
		if (!Utils.isNullOrEmpty(oLakeInfo.getLakeName())) aoPropertiesMap.put("lakeName", oLakeInfo.getLakeName());
		if(!Utils.isNullOrEmpty(oLakeInfo.getContinent())) aoPropertiesMap.put("continent", oLakeInfo.getContinent());
		if(!Utils.isNullOrEmpty(oLakeInfo.getPolySrc())) aoPropertiesMap.put("polySrc", oLakeInfo.getPolySrc());
		if(!Utils.isNullOrEmpty(oLakeInfo.getLakeType())) aoPropertiesMap.put("lakeType", oLakeInfo.getLakeType());
		if(!Utils.isNullOrEmpty(oLakeInfo.getGrandId())) aoPropertiesMap.put("grandId", oLakeInfo.getGrandId());
		if(!Utils.isNullOrEmpty(oLakeInfo.getLakeArea())) aoPropertiesMap.put("lakeArea", oLakeInfo.getLakeArea());
		if(!Utils.isNullOrEmpty(oLakeInfo.getShoreLen())) aoPropertiesMap.put("shoreLen", oLakeInfo.getShoreLen());
		if(!Utils.isNullOrEmpty(oLakeInfo.getShoreDev())) aoPropertiesMap.put("shoreDev", oLakeInfo.getShoreDev());
		if(!Utils.isNullOrEmpty(oLakeInfo.getVolTotal())) aoPropertiesMap.put("volTotal", oLakeInfo.getVolTotal());
		if(!Utils.isNullOrEmpty(oLakeInfo.getVolRes())) aoPropertiesMap.put("volRes", oLakeInfo.getVolRes());
		if(!Utils.isNullOrEmpty(oLakeInfo.getVolSrc())) aoPropertiesMap.put("volSrc", oLakeInfo.getVolSrc());
		if(!Utils.isNullOrEmpty(oLakeInfo.getDepthAvg())) aoPropertiesMap.put("depthAvg", oLakeInfo.getDepthAvg());
		if(!Utils.isNullOrEmpty(oLakeInfo.getDisAvg())) aoPropertiesMap.put("disAvg", oLakeInfo.getDisAvg());
		if(!Utils.isNullOrEmpty(oLakeInfo.getResTime())) aoPropertiesMap.put("resTime", oLakeInfo.getResTime());
		if(!Utils.isNullOrEmpty(oLakeInfo.getElevation())) aoPropertiesMap.put("elevation", oLakeInfo.getElevation());
		if(!Utils.isNullOrEmpty(oLakeInfo.getSlope100())) aoPropertiesMap.put("slope100", oLakeInfo.getSlope100());
		if(!Utils.isNullOrEmpty(oLakeInfo.getWshdArea())) aoPropertiesMap.put("wshdArea", oLakeInfo.getWshdArea());
		if(!Utils.isNullOrEmpty(oLakeInfo.getPourLong())) aoPropertiesMap.put("pourLong", oLakeInfo.getPourLong());
		if(!Utils.isNullOrEmpty(oLakeInfo.getPourLat())) aoPropertiesMap.put("pourLat", oLakeInfo.getPourLat());
		if(!Utils.isNullOrEmpty(oLakeInfo.getGeometry())) aoPropertiesMap.put("geometry", oLakeInfo.getGeometry());
		
		oResVM.setProperties(aoPropertiesMap);
		
		return oResVM;
		
	}
	
	public String simplifyLake(String sWKTGeometry) {
	    
		WKTReader oReader = new WKTReader();
	    // Precision of 5 is plenty for a bounding box
	    WKTWriter oWriter = new WKTWriter(4); 
	    
	    try {
	        // 1. Parse the complex lake WKT into a Geometry object
	        Geometry oGeom = oReader.read(sWKTGeometry); 
	        
	        // 2. Get the Envelope (the bounding box)
	        // This returns a Geometry representing the rectangular extent
	        Geometry oEnvelope = oGeom.getEnvelope();
	        
	        // 3. Convert back to WKT String
	        return oWriter.write(oEnvelope);
	    }
	    catch (Exception oE) {
	        WasdiLog.errorLog("Error calculating bounding box: " + oE.getMessage());
	        return null;
	    }
	}


}
