package wasdi.shared.queryexecutors.viirs;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.TimeEpochUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
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
	
	public QueryExecutorVIIRS() {
		
		m_sProvider="VIIRS";
		
		this.m_oQueryTranslator = new QueryTranslatorVIIRS();
		this.m_oResponseTranslator = new ResponseTranslatorVIIRS();
		
		m_asSupportedPlatforms.add(Platforms.VIIRS);
	}
	
	@Override
	public int executeCount(String sQuery) {
		int iCount = 0;
		
		// Parse the query
		QueryViewModel oVIIRSQuery = m_oQueryTranslator.parseWasdiClientQuery(sQuery);
		
		if (m_asSupportedPlatforms.contains(oVIIRSQuery.platformName) == false) {
			return 0;
		}
		
		ArrayList<String> asSections = getInvolvedSections(oVIIRSQuery);

		int iDays = TimeEpochUtils.countDaysIncluding(oVIIRSQuery.startFromDate, oVIIRSQuery.endToDate);

	    iCount = asSections.size() * iDays;
		
		return iCount;
	}
		
	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		
		ArrayList<QueryResultViewModel> aoResults = new ArrayList<QueryResultViewModel>();
		
		int iActualElement = 0;
		
		// Parse the query
		QueryViewModel oVIIRSQuery = m_oQueryTranslator.parseWasdiClientQuery(oQuery.getQuery());
		
		if (m_asSupportedPlatforms.contains(oVIIRSQuery.platformName) == false) {
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
	    	Utils.debugLog("QueryExecutorVIIRS.executeAndRetrieve: " + oE.toString());
		}
	    
	    try {
	    	iLimit = Integer.parseInt(sLimit);
	    }
	    catch (Exception oE) {
	    	Utils.debugLog("QueryExecutorVIIRS.executeAndRetrieve: " + oE.toString());
		}
	    
	    DateFormat oDateFormat = new SimpleDateFormat("yyyyMMdd");

		int iDays = TimeEpochUtils.countDaysIncluding(oVIIRSQuery.startFromDate, oVIIRSQuery.endToDate);
		for (int i = 0; i < iDays; i++) {
			Date oActualDay = TimeEpochUtils.getLaterDate(lStart, i);
	    	
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
	    			oResult.setLink("https://floodlight.ssec.wisc.edu/composite/" + sFileName);
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
	
	@Override
	public void init() {
		super.init();
		
		try {
			JSONObject oAppConf = WasdiFileUtils.loadJsonFromFile(m_sParserConfigPath);
			m_sShapeMaskPath = oAppConf.getString("shapeMaskPath");
			
		}
		catch (Exception oEx) {
			Utils.debugLog("QueryExecutorVIIRS.init(): exception reading parser config file " + m_sParserConfigPath);
		}		
	}
	
	private ArrayList<String> getInvolvedSections(QueryViewModel oVIIRSQuery) {
		
		// Parse the query
		ArrayList<String> asSections = new ArrayList<>();
		
		synchronized (m_sShapeMaskPath) {
			
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
				Utils.debugLog("QueryExecutorVIIRS.getInvolvedSections.Exception " + oEx.toString());
			}

		}
		
		return asSections;
	}
	
	private SimpleFeatureCollection grabFeaturesInBoundingBox(double x1, double y1, double x2, double y2, SimpleFeatureSource oFeatureSource) {
		
		try {
			
			Filter oFilter = ECQL.toFilter("BBOX(the_geom, " + x1 + ", " + y1 + ", " +x2 + ", " + y2 + ")");
			
			return oFeatureSource.getFeatures(oFilter);
		}
		catch (Exception oEx) {
			Utils.debugLog("QueryExecutorVIIRS.grabFeaturesInBoundingBox Exception " + oEx.toString());
		}
		
		return null;
    }
}
