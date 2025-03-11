package wasdi.shared.queryexecutors.viirs;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.filter.text.ecql.ECQL;
import org.json.JSONObject;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.TimeEpochUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

/**
 * VIIRS Query Executor.
 * This class queries the VIIRS Flood Map Catalogue from https://floodlight.ssec.wisc.edu
 * 
 * The query in this case is local: the catalogue does not have API.
 * VIIRS are daily maps in a pre-defined world grid. 
 * Maps are available one per day per each tile of the grid.
 * 
 * The query is obtained with a local shape file that reproduces the VIIRS grid
 * It intersects the users' AOI, find involved tiles and returns one result per day per tile
 * 
 * @author p.campanella
 *
 */
public class QueryExecutorVIIRS extends QueryExecutor {
	
	String m_sShapeMaskPath = "";
	
	private String m_sSearchIntervalStartDate = "";
	private String m_sSearchGapStartDate = "";
	private String m_sSearchGapEndDate = "";
	
	private static final Object s_oShapeFileLock = new Object();
	public static final String s_sLINK_PREFIX = "https://floodlight.ssec.wisc.edu/composite/";

	
	public QueryExecutorVIIRS() {
		this.m_oQueryTranslator = new QueryTranslatorVIIRS();
		this.m_oResponseTranslator = new ResponseTranslatorVIIRS();
	}
	
	@Override
	public int executeCount(String sQuery) {
		
		try {
			int iCount = 0;
			
			// Parse the query
			QueryViewModel oVIIRSQuery = m_oQueryTranslator.parseWasdiClientQuery(sQuery);
			
			if (m_asSupportedPlatforms.contains(oVIIRSQuery.platformName) == false) {
				return 0;
			}
			
			
			if (!Utils.isNullOrEmpty(oVIIRSQuery.productType) 
					&& !oVIIRSQuery.productType.equals("VIIRS_1d_composite") 
					&& !oVIIRSQuery.productType.equals("VIIRS_5d_composite")) {
				return -1;
			}
			
			ArrayList<String> asSections = getInvolvedSections(oVIIRSQuery);

			int iDays = Utils.isNullOrEmpty(m_sSearchIntervalStartDate) 
						? TimeEpochUtils.countDaysIncluding(oVIIRSQuery.startFromDate, oVIIRSQuery.endToDate)
						: countDaysWithGap(oVIIRSQuery.startFromDate, oVIIRSQuery.endToDate); 

		    iCount = iDays >= 0 
		    		?  asSections.size() * iDays
		    		: -1;
			
			return iCount;			
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("QueryExecutorVIIRS.executeCount: error " + oEx.toString());
		}
		
		return -1;
	}
		
	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		
		try {
			ArrayList<QueryResultViewModel> aoResults = new ArrayList<QueryResultViewModel>();
			
			int iActualElement = 0;
			
			// Parse the query
			QueryViewModel oVIIRSQuery = m_oQueryTranslator.parseWasdiClientQuery(oQuery.getQuery());
			
			if (m_asSupportedPlatforms.contains(oVIIRSQuery.platformName) == false) {
				return aoResults;
			}
			
			if (!Utils.isNullOrEmpty(oVIIRSQuery.productType) 
					&& !oVIIRSQuery.productType.equals("VIIRS_1d_composite") 
					&& !oVIIRSQuery.productType.equals("VIIRS_5d_composite")) {
				return null;
			}
			
			// If the user is requesting a specific file
			if (!Utils.isNullOrEmpty(oVIIRSQuery.productName)) {
    			QueryResultViewModel oResult = new QueryResultViewModel();
    			    			
    			oResult.setId(oVIIRSQuery.productName);
    			oResult.setTitle(oVIIRSQuery.productName);
    			oResult.setLink("https://floodlight.ssec.wisc.edu/composite/" + oVIIRSQuery.productName);
    			oResult.setProvider("VIIRS");
    			oResult.getProperties().put("platformname", "VIIRS");
    			
    			aoResults.add(oResult);
    			
    			return aoResults;
			}
			
			ArrayList<String> asSections = getInvolvedSections(oVIIRSQuery);
			
			long lStart = TimeEpochUtils.fromDateStringToEpoch(oVIIRSQuery.startFromDate);
		    
		    String sOffset = oQuery.getOffset();
		    String sLimit = oQuery.getLimit();
		    
		    int iOffset = 0;
		    int iLimit = 10;
		    
		    try {
		    	iOffset = Integer.parseInt(sOffset);
		    }
		    catch (Exception oE) {
		    	WasdiLog.debugLog("QueryExecutorVIIRS.executeAndRetrieve: " + oE.toString());
			}
		    
		    try {
		    	iLimit = Integer.parseInt(sLimit);
		    }
		    catch (Exception oE) {
		    	WasdiLog.debugLog("QueryExecutorVIIRS.executeAndRetrieve: " + oE.toString());
			}
		    
		    DateFormat oDateFormat = new SimpleDateFormat("yyyyMMdd");

			int iDays = TimeEpochUtils.countDaysIncluding(oVIIRSQuery.startFromDate, oVIIRSQuery.endToDate);
			
			// first day with available VIIRS results
			long lStartSearchDate = !Utils.isNullOrEmpty(m_sSearchIntervalStartDate) 
							? TimeEpochUtils.fromDateStringToEpoch(m_sSearchIntervalStartDate)
							:-1L;
			
			// last day with available VIIRS results (i.e. the current date)
			long lEndSearchDate = lStartSearchDate >= 0L
							? getEndOfCurrentDay()
							: -1L ;
			
			// first days of "gap" in VIIRS results
			long lStartSearchGap = !Utils.isNullOrEmpty(m_sSearchGapStartDate) 
							? TimeEpochUtils.fromDateStringToEpoch(m_sSearchGapStartDate) 
							: -1L;
			
			// last days of "gap" in VIIRS results
			long lEndSearchGap = !Utils.isNullOrEmpty(m_sSearchGapEndDate) 
							? TimeEpochUtils.fromDateStringToEpoch(m_sSearchGapEndDate) 
							: -1L;
			
			
			for (int i = 0; i < iDays; i++) {
				Date oActualDay = TimeEpochUtils.getLaterDate(lStart, i, "GMT");
				
				
				if (!isValidSearchDate(oActualDay, lStartSearchDate, lEndSearchDate, lStartSearchGap, lEndSearchGap)) {
					continue;
				}
		    	
		    	for (String sSection : asSections) {
		    		
		    		String asSectionParts [] = sSection.split(";");
		    		String sFootPrint = asSectionParts[1];
		    		sSection = asSectionParts[0];
		    		
		    		String sFormattedSection = sSection;
		    		while (sFormattedSection.length()<3) {
		    			sFormattedSection = "0" + sFormattedSection;
		    		}
					
		    		if (iActualElement>=iOffset && iActualElement< iOffset + iLimit) {
		    			QueryResultViewModel oResult = new QueryResultViewModel();
		    			
		    			String sFileName = "RIVER-FLDglobal-composite1_";
		    			
		    			if (oVIIRSQuery.productType.equals("VIIRS_5d_composite")) {
		    				sFileName = "RIVER-FLDglobal-composite_";
		    			}
		    			
		    			sFileName += oDateFormat.format(oActualDay);
		    			sFileName += "_000000";
		    			sFileName += ".part" + sFormattedSection;
		    			sFileName += ".tif";
		    			
		    			oResult.setId(sFileName);
		    			oResult.setTitle(sFileName);
		    			oResult.setLink(s_sLINK_PREFIX + sFileName);
		    			oResult.setSummary("Date: "  + TimeEpochUtils.fromEpochToDateString(oActualDay.getTime()) +  ", Mode: Earth Observation, Satellite: VIIRS");
		    			oResult.setProvider("VIIRS");
		    			oResult.setFootprint(sFootPrint);
		    			oResult.getProperties().put("platformname", "VIIRS");
		    			
		    			aoResults.add(oResult);
		    		}
		    		
		    		iActualElement ++;
		    		
		    		if (iActualElement>iOffset+iLimit) break;
				}
		    	
		    }
			
			return aoResults;			
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("QueryExecutorVIIRS.executeAndRetrieve: error " + oEx.toString());
		}
		
		return null;
	}
	
	@Override
	public void init() {
		super.init();
		
		try {
			JSONObject oAppConf = JsonUtils.loadJsonFromFile(m_sParserConfigPath);
			m_sShapeMaskPath = oAppConf.getString("shapeMaskPath");
			m_sSearchIntervalStartDate = oAppConf.optString("searchIntervalStartDate");
			m_sSearchGapStartDate = oAppConf.optString("searchGapStartDate");
			m_sSearchGapEndDate = oAppConf.optString("searchGapEndDate");
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("QueryExecutorVIIRS.init(): exception reading parser config file " + m_sParserConfigPath);
		}		
	}
	
	private ArrayList<String> getInvolvedSections(QueryViewModel oVIIRSQuery) {
		
		// Parse the query
		ArrayList<String> asSections = new ArrayList<>();
		
		synchronized (s_oShapeFileLock){
			
			try {
				// Get the Data Store
				FileDataStore oStore = FileDataStoreFinder.getDataStore(new File(m_sShapeMaskPath));
				
				try {
					SimpleFeatureSource oFeatureSource = oStore.getFeatureSource();
					
					// Intersect the features
					SimpleFeatureCollection oIntersectedFeatures = grabFeaturesInBoundingBox(oVIIRSQuery.west, oVIIRSQuery.south, oVIIRSQuery.east, oVIIRSQuery.north, oFeatureSource);
					
					if (oIntersectedFeatures != null) {
						SimpleFeatureIterator oIterator = oIntersectedFeatures.features();
						
						while (oIterator.hasNext()) {
							SimpleFeature oFeature = oIterator.next();
							asSections.add(oFeature.getAttribute("name").toString() + ";" + oFeature.getAttribute("the_geom").toString());
						}
					}			
				}
				finally {
					oStore.dispose();
				}
			}
			catch (Exception oEx) {
				WasdiLog.debugLog("QueryExecutorVIIRS.getInvolvedSections.Exception " + oEx.toString());
			}

		}
		
		return asSections;
	}
	
	private boolean isValidSearchDate(Date oSearchDate, long lStartSearchInterval, long lEndSearchInterval, long lStartSearchGap, long lEndSearchGap) {
		long lSearchDate = oSearchDate.getTime();
		
		boolean isInSearchRange = false;
		boolean isInGapRange = false;
		
		if (lStartSearchInterval < 0L || lEndSearchInterval < 0L || lStartSearchGap < 0L || lEndSearchGap < 0L) {
			return true;
		}
		
		if (lSearchDate >= lStartSearchInterval  && lSearchDate <= lEndSearchInterval ) 
			isInSearchRange = true;
		
		
		if (lSearchDate >= lStartSearchGap && lSearchDate <= lEndSearchGap) {
			isInGapRange = true;
		} 
		
		return isInSearchRange && !isInGapRange;
	}
	
	private SimpleFeatureCollection grabFeaturesInBoundingBox(double x1, double y1, double x2, double y2, SimpleFeatureSource oFeatureSource) {
		
		try {
			
			Filter oFilter = ECQL.toFilter("BBOX(the_geom, " + x1 + ", " + y1 + ", " +x2 + ", " + y2 + ")");
			
			return oFeatureSource.getFeatures(oFilter);
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("QueryExecutorVIIRS.grabFeaturesInBoundingBox Exception " + oEx.toString());
		}
		
		return null;
    }
	
	/**
	 * Get the time corresponding to 23:59 of the current day, in milliseconds
	 * @return the milliseconds representing 23:59 of the current day
	 */
	private long getEndOfCurrentDay() {
		java.time.LocalDate oCurrentDate = java.time.LocalDate.now();

        // Get the time 23:59:59 of the current date
        Instant oInstant = oCurrentDate
                .atTime(LocalTime.MAX)
                .atZone(ZoneOffset.UTC)
                .toInstant();

        // Get the epoch time in milliseconds
        long lEpochMilliseconds = oInstant.toEpochMilli();
        
        return lEpochMilliseconds;
	}
	
	/**
	 * The VIIRS data provider could have some gap in the data. The method uses the information stored in the configuration to
	 * compute the actual days covered by the data providers, keeping into account eventual gaps.
	 * @param sStartDate the start date of the WASDI query, in the format YYYY-MM-dd'T'HH:MM:SS.mmm'Z'
	 * @param sEndDate the end date of the WASDI query, in the format YYYY-MM-dd'T'HH:MM:SS.mmm'Z'
	 * @return the actual number of days covered by the data provider, given a certain time range
	 */
	private int countDaysWithGap(String sStartDate, String sEndDate) {
		try {
			long lStartInputEpoch = TimeEpochUtils.fromDateStringToEpoch(sStartDate);
			long lEndInputEpoch = TimeEpochUtils.fromDateStringToEpoch(sEndDate);
			
			if (lEndInputEpoch <= lStartInputEpoch) {
				WasdiLog.warnLog("QueryExecutorVIIRS.countDaysWithGap: invalid time range");
				return -1;
			}
			
			String sStartSearchDate = "1970-01-01T00:00:00.000Z";
			if (!Utils.isNullOrEmpty(m_sSearchIntervalStartDate)) { 
				sStartSearchDate = m_sSearchIntervalStartDate;
			}
			long lStartSearchEpoch = TimeEpochUtils.fromDateStringToEpoch(sStartSearchDate);	
			
			long lEndSearchEpoch = getEndOfCurrentDay();
			String sEndSeachDate = TimeEpochUtils.fromEpochToDateString(lEndSearchEpoch);
			
			String sStartGap = m_sSearchGapStartDate;
			String sEndGap = m_sSearchGapEndDate;
			long lStartGapEpoch = !Utils.isNullOrEmpty(sStartGap) 
							? TimeEpochUtils.fromDateStringToEpoch(sStartGap) 
							: -1L;
			long lEndGapEpoch = !Utils.isNullOrEmpty(sEndGap) 
							? TimeEpochUtils.fromDateStringToEpoch(sEndGap)
							: -1L;
			
			// input dates before the beginning of the search interval
			if (lStartInputEpoch < lStartSearchEpoch && lEndInputEpoch < lStartSearchEpoch) {
				WasdiLog.debugLog("QueryExecutorVIIRS.countDaysWithGap: WASDI query time range is before the time range covered by the data provider");
				return 0;
			}
			
			// input dates after the end of the search interval
			if (lStartInputEpoch > lEndSearchEpoch && lEndInputEpoch > lEndSearchEpoch) {
				WasdiLog.debugLog("QueryExecutorVIIRS.countDaysWithGap: WASDI query time range is after the time range covered by the data provider");
				return 0;
			}
			
			String sModifiedStartDate = sStartDate;
			if (lStartInputEpoch < lStartSearchEpoch)  {
				sModifiedStartDate = sStartSearchDate;
				lStartInputEpoch = lStartSearchEpoch;
			}
				
			String sModifiedEndDate = sEndDate;
			if (lEndInputEpoch > lEndSearchEpoch) {
				sModifiedEndDate = sEndSeachDate;
				lEndInputEpoch = lEndSearchEpoch;
			}
					
			if (lStartGapEpoch < 0 || lEndGapEpoch < 0) {
				WasdiLog.debugLog("QueryExecutorVIIRS.countDaysWithGap: no time gap in data provider");
				return TimeEpochUtils.countDaysIncluding(sModifiedStartDate, sModifiedEndDate);	
			}
			
				
			if (isIntervalIncludedInRange(lStartInputEpoch, lEndInputEpoch, lStartGapEpoch, lEndGapEpoch)) {
				WasdiLog.debugLog("QueryExecutorVIIRS.countDaysWithGap: WASDI query time range included in the time gap of missing products");
				return 0;	
			}
				
			// at this point, we know we have two time intervals:
			// INTERVAL1: from the date of the older product in the data provider to the start date of the "gap" of results (exclusive)
			// INTERVAL2: from the end date of the "gap" of results, until now
			
			// check if both intervals are in the same time interval
			if (isIntervalIncludedInRange(lStartInputEpoch, lEndInputEpoch, lStartSearchEpoch, lStartGapEpoch - 1000)
				|| isIntervalIncludedInRange(lStartInputEpoch, lEndInputEpoch, lEndGapEpoch + 1000, lEndSearchEpoch)) {
				WasdiLog.debugLog("QueryExecutorVIIRS.countDaysWithGap: start and end date of the query are in the same search interval");
				return TimeEpochUtils.countDaysIncluding(sModifiedStartDate, sModifiedEndDate);	
			} 
				
			// at this point, we know that the query start date and end date belong to different intervals
			int iDaysInterval1 = 0;
			
			if (lStartInputEpoch >= lStartSearchEpoch && lStartInputEpoch < lStartGapEpoch) {		
				String sEndInterval1 =  TimeEpochUtils.fromEpochToDateString(lStartGapEpoch - 1000);
				iDaysInterval1 = TimeEpochUtils.countDaysIncluding(sModifiedStartDate, sEndInterval1);
			}
			
			int iDaysInterval2 = 0;
			if (lEndInputEpoch > lEndGapEpoch && lEndInputEpoch <= lEndSearchEpoch) {
				String sStartInterval2 =  TimeEpochUtils.fromEpochToDateString(lEndGapEpoch + 1000);
				iDaysInterval2  = TimeEpochUtils.countDaysIncluding(sStartInterval2, sModifiedEndDate);
			}
			return iDaysInterval1 + iDaysInterval2;
		} catch(Exception oEx) {
			WasdiLog.errorLog("QueryExecutorVIIRS.countDaysWithGap: exception while counting the effective number of search days", oEx);
			return -1;
		}
		
	}
	
	/**
	 * Given the start and the end (in milliseconds) of two intervals, checks it the first interval is included in the second interval
	 * @param sStartInterval start date of the first interval
	 * @param sEndInterval end date of the first interval
	 * @param sStartRange start date of the second interval
	 * @param sEndRange end date of the second interval
	 * @return true if the first time interval is included in the second one, false otherwise
	 */
	private boolean isIntervalIncludedInRange(long lStartInterval, long lEndInterval, long lStartRange, long lEndRange) {
		return lStartInterval >= lStartRange 
				&& lStartInterval <= lEndRange 
				&& lEndInterval >= lStartRange 
				&& lEndInterval <= lEndRange;
	}

}
 