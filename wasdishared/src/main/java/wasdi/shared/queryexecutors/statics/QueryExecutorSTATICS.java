package wasdi.shared.queryexecutors.statics;

import java.util.ArrayList;
import java.util.List;

import it.geosolutions.geoserver.rest.decoder.RESTBoundingBox;
import wasdi.shared.config.DataProviderConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.geoserver.GeoServerManager;
import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.gis.BoundingBoxUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

public class QueryExecutorSTATICS extends QueryExecutor {
	
	protected String m_sApiURl = "https://www.wasdi.net/geoserver/ogc?";
	GeoServerManager m_oManager = null;
	String m_sDefaultCrs = "EPSG:4326";
	
	public QueryExecutorSTATICS() {
		this.m_oQueryTranslator = new QueryTranslatorSTATICS();
		this.m_oResponseTranslator = new ResponseTranslatorSTATICS();		
	}
	
	@Override
	public void init() {
		super.init();
		
		DataProviderConfig oDataProviderConfig = WasdiConfig.Current.getDataProviderConfig(m_sDataProviderCode);
		if (oDataProviderConfig != null) {
			m_sApiURl = oDataProviderConfig.urlDomain;
		}
		
		try {
			m_oManager = new GeoServerManager(m_sApiURl, m_sUser, m_sPassword);
		}
		catch (Exception e) {
			WasdiLog.debugLog("QueryExecutorSTATICS: Exception " + e.toString());
		}
		
	}

	@Override
	public int executeCount(String sQuery) {
		
		QueryViewModel oQuery =  m_oQueryTranslator.parseWasdiClientQuery(sQuery);
		
		if (oQuery == null) {
			WasdiLog.debugLog("QueryExecutorSTATICS.executeCount: parsed query is null, return 0");
			return 0;
		}
		
		if (m_oManager == null) {
			WasdiLog.debugLog("QueryExecutorSTATICS.executeCount: geoserver manager is null, return 0");
		}
		
		try {
			
			if (doesQueryMatch(oQuery)) {
				WasdiLog.debugLog("QueryExecutorSTATICS.executeCount: query has a match, return 1");
				return 1;
			}
			else {
				WasdiLog.debugLog("QueryExecutorSTATICS.executeCount: query does not match, return 0");
				return 0;
			}
			
		}
		catch (Exception e) {
			WasdiLog.debugLog("QueryExecutorSTATICS.executeCount: error " + e.toString());
		}
		
		return 0;
	}

	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		
		ArrayList<QueryResultViewModel> aoResults = new ArrayList<QueryResultViewModel>();
		
		QueryViewModel oQueryVM =  m_oQueryTranslator.parseWasdiClientQuery(oQuery.getQuery());
		
		if (oQueryVM == null) {
			WasdiLog.debugLog("QueryExecutorSTATICS.executeCount: parsed query is null, return 0");
			return aoResults;
		}
		
		if (m_oManager == null) {
			WasdiLog.debugLog("QueryExecutorSTATICS.executeCount: geoserver manager is null, return 0");
		}
		
		try {
			
			if (doesQueryMatch(oQueryVM)) {
				WasdiLog.debugLog("QueryExecutorSTATICS.executeCount: query has a match, return 1");
				
				QueryResultViewModel oQueryResultViewModel = new QueryResultViewModel();
				
				String sUniqueId = Utils.getCappedRandomName(4);
				
				// name starting with WASDI_STATIC_[PROD_ID]_[TIMESTAMP] 
				String sFileName = "WASDI_STATIC_" + oQueryVM.productType.toUpperCase() + "_" + sUniqueId + ".tif";
				
				// BBox
				String sBbox = "" + oQueryVM.west + "," + oQueryVM.south + "," + oQueryVM.east + "," + oQueryVM.north;
				
				// Url = geoserver_address;LayerId;CRS;dWest,dSouth,dEast,dNorth;sOutputFileName
				String sUrl = m_sApiURl+";"+oQueryVM.productType+";"+m_sDefaultCrs+";" + sBbox + ";" +sFileName;
				
				// .Title -> Name of the file
				oQueryResultViewModel.setTitle(sFileName);
				// .Link -> Link to download the file
				oQueryResultViewModel.setLink(sUrl);
				// .Id -> Provider unique id
				oQueryResultViewModel.setId(sUniqueId);
				// .Provider -> Provider used to get this info.
				oQueryResultViewModel.setProvider(m_sDataProviderCode);
				
				String sWktFootPrint = BoundingBoxUtils.getWKTPolygon(oQueryVM.south, oQueryVM.east, oQueryVM.north, oQueryVM.west);
				// .Footprint -> Bounding box in WKT
				oQueryResultViewModel.setFootprint(sWktFootPrint);
				
				aoResults.add(oQueryResultViewModel);
				
				return aoResults;
			}
			else {
				WasdiLog.debugLog("QueryExecutorSTATICS.executeCount: query does not match, return 0");
				return aoResults;
			}
			
		}
		catch (Exception e) {
			WasdiLog.debugLog("QueryExecutorSTATICS.executeCount: error " + e.toString());
		}
		
		return aoResults;
	}
	
	@Override
	public String getUriFromProductName(String sProduct, String sProtocol, String sOriginalUrl, String sPlatform) {
		return sOriginalUrl;
	}
	
	/**
	 * Check if the Query matchs a file avaiable in Geoserver
	 * @param oQuery Parsed User Query
	 * @return true if a file is found, false if not
	 */
	protected boolean doesQueryMatch(QueryViewModel oQuery) {
		
		try {
			// from productType get the layer that must be seen
			String sLayerId = oQuery.productType;
			
			// Check if is valid in geoserver
			if (!m_oManager.layerExists(sLayerId)) {
				return false;
			}
			
			// Get the layer bbox
			RESTBoundingBox oBbox = m_oManager.getLayerRESTBBox(sLayerId);
			
			if (oBbox == null) {
				return false;
			}
			
			// Check if there is a match in the bbox
			return BoundingBoxUtils.bboxIntersects(oBbox.getMinY(), oBbox.getMinX(), oBbox.getMaxY(), oBbox.getMaxX(), oQuery.south, oQuery.west, oQuery.north, oQuery.east);			
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("QueryExecutorSTATICS.doesQueryMatch: error " + oEx.toString());
		}
		
		return false;
	}

}
