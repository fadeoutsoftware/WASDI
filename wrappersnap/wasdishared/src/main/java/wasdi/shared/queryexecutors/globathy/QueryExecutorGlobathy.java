package wasdi.shared.queryexecutors.globathy;

import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.json.JSONObject;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

public class QueryExecutorGlobathy extends QueryExecutor {
	
	private String m_sLakesShapeFilePath = "";	// path to the shp file
	private String m_sGlobathyFolderPath = "";  // path to the root folder, containing other sub-folder
	
	private static final Object s_oShapeFileLock = new Object();


	public QueryExecutorGlobathy() {
		this.m_oQueryTranslator = new QueryTranslatoryGlobathy();
		this.m_oResponseTranslator = new ResponseTranslatorGlobathy();
		this.m_asSupportedPlatforms.add(Platforms.GLOBATHY);
	}
	
	
	@Override
	public void init() {
		super.init();
		
		try {
			JSONObject oAppConf = JsonUtils.loadJsonFromFile(m_sParserConfigPath);
			m_sLakesShapeFilePath = oAppConf.getString("lakesShapeFilePath");
			m_sGlobathyFolderPath = oAppConf.getString("globathyRootFolderPath");
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorGlobathy.init. Exception reading parser config file " + m_sParserConfigPath, oEx);
		}
		
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
			
			double dNorth = oQueryVM.north;
			double dEast = oQueryVM.east;
			double dSouth = oQueryVM.south;
			double dWest = oQueryVM.west;
			
			if (Utils.isNullOrEmpty(dNorth))
				dNorth = 90d;
			if (Utils.isNullOrEmpty(dEast))
				dEast = 180d;
			if (Utils.isNullOrEmpty(dSouth))
				dSouth = -90d;
			if (Utils.isNullOrEmpty(dWest))
				dWest = -180d;
			
			iCount = countFeatures(dNorth, dEast, dSouth, dWest);
			
			
		} 
		catch (Exception oE) {
			WasdiLog.errorLog("QueryExecutorGlobathy.executeCount. Exception ", oE);
		}
		
		return iCount;
	}

	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		
		ArrayList<QueryResultViewModel> aoResults = null;
		
		try {
			QueryViewModel oQueryVM = m_oQueryTranslator.parseWasdiClientQuery(oQuery.getQuery());
			
			if (!m_asSupportedPlatforms.contains(oQueryVM.platformName)) {
				return aoResults;
			}
			
			// as a first approximation, we only want the lakes within a specific bounding box.
			// Nothing else is required
			
			double dNorth = oQueryVM.north;
			double dEast = oQueryVM.east;
			double dSouth = oQueryVM.south;
			double dWest = oQueryVM.west;
			
			if (Utils.isNullOrEmpty(dNorth))
				dNorth = 90d;
			if (Utils.isNullOrEmpty(dEast))
				dEast = 180d;
			if (Utils.isNullOrEmpty(dSouth))
				dSouth = -90d;
			if (Utils.isNullOrEmpty(dWest))
				dWest = -180d;
			
			ArrayList<LakeInfo> aoLakes = getFeatures(dNorth, dEast, dSouth, dWest);
			
			if (aoLakes == null) {
				WasdiLog.warnLog("QueryExecutorGlobathy.executeAndRetrieve. Error retrieving the products");
				return aoResults;
			}
			
			
			
		} 
		catch (Exception oE) {
			WasdiLog.errorLog("QueryExecutorGlobathy.executeAndRetrieve. Exception ", oE);
		}
		
		
		return aoResults;
	}
	
	
	private int countFeatures(double dNorth, double dEast, double dSouth, double dWest) {
		
		int iCount = -1;
		
		synchronized (s_oShapeFileLock){
			
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
				WasdiLog.errorLog("QueryExecutorGlobathy.countFeatures.Exception ", oEx);
			}
		}
		
		return iCount;	
	}
	
	private ArrayList<LakeInfo> getFeatures(double dNorth, double dEast, double dSouth, double dWest) {
		
		ArrayList<LakeInfo> aoLakeInfo = null;
		
		synchronized (s_oShapeFileLock){
			
			try {
				// Get the Data Store
				FileDataStore oStore = FileDataStoreFinder.getDataStore(new File(m_sLakesShapeFilePath));
				
				try {
					SimpleFeatureSource oFeatureSource = oStore.getFeatureSource();		
					SimpleFeatureCollection oIntersectedFeatures = intersectBoundingBox(dNorth, dEast, dSouth, dWest, oFeatureSource);
					
					if (oIntersectedFeatures != null) {
						SimpleFeatureIterator oIterator = oIntersectedFeatures.features();
						
						aoLakeInfo = new ArrayList<>();
						
						while (oIterator.hasNext()) {
							SimpleFeature oFeature = oIterator.next();
							
							LakeInfo oInfo = getLakeInfo(oFeature);
							
							aoLakeInfo.add(oInfo);
							
						}
					}			
					
				}
				finally {
					oStore.dispose();
				}
			}
			catch (Exception oEx) {
				WasdiLog.errorLog("QueryExecutorGlobathy.countFeatures.Exception ", oEx);
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
		oInfo.setGeometry(oFeature.getAttribute("geometry").toString());
		
		return oInfo;
	}
	
	private SimpleFeatureCollection intersectBoundingBox(double dNorth, double dEast, double dSouth, double dWest, SimpleFeatureSource oFeatureSource) {
		try {
			// get the actual name of the geometry
			String sGeometryName = oFeatureSource.getSchema().getGeometryDescriptor().getLocalName();
			WasdiLog.debugLog("QueryExecutorGlobathy.intersectBoundingBox. Geometry name: " + sGeometryName);
			
			String sFilter = String.format("BBOX(%s, %f, %f, %f, %f)", 
	                sGeometryName, dWest, dSouth, dEast, dNorth);
		
			Filter oFilter = ECQL.toFilter(sFilter);
			
			return oFeatureSource.getFeatures(oFilter);
			
		} catch (Exception oE) {
			WasdiLog.errorLog("QueryExecutorGlobathy.intersectBoundingBox. Error ", oE);
		}
		
		return null;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
