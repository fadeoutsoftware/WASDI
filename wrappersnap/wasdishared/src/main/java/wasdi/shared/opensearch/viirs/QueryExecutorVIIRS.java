package wasdi.shared.opensearch.viirs;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.abdera.i18n.templates.Template;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.filter.text.ecql.ECQL;
import org.json.JSONObject;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;

import wasdi.shared.opensearch.PaginatedQuery;
import wasdi.shared.opensearch.Platforms;
import wasdi.shared.opensearch.QueryExecutor;
import wasdi.shared.utils.TimeEpochUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.viewmodels.QueryResultViewModel;
import wasdi.shared.viewmodels.QueryViewModel;

public class QueryExecutorVIIRS extends QueryExecutor {
	
	String m_sShapeMaskPath = "";
	
	public QueryExecutorVIIRS() {
		Utils.debugLog(s_sClassName);
		m_sProvider=s_sClassName;
		this.m_oQueryTranslator = new DiasQueryTranslatorVIIRS();
		this.m_oResponseTranslator = new DiasResponseTranslatorVIIRS();
		
		m_asSupportedPlatforms.add(Platforms.VIIRS);
	}

	@Override
	protected String[] getUrlPath() {
		return null;
	}

	@Override
	protected Template getTemplate() {
		return null;
	}

	@Override
	protected String getCountUrl(String sQuery) {
		return null;
	}
	
	public ArrayList<String> getInvolvedSections(QueryViewModel oVIIRSQuery) {
		
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
	
	@Override
	public int executeCount(String sQuery) throws IOException {
		int iCount = 0;
		
		// arse the query
		QueryViewModel oVIIRSQuery = m_oQueryTranslator.parseWasdiClientQuery(sQuery);
		
		if (m_asSupportedPlatforms.contains(oVIIRSQuery.platformName) == false) {
			return 0;
		}
		
		ArrayList<String> asSections = getInvolvedSections(oVIIRSQuery);
				
		long lStart = TimeEpochUtils.fromDateStringToEpoch(oVIIRSQuery.startFromDate);
		long lEnd  = TimeEpochUtils.fromDateStringToEpoch(oVIIRSQuery.endToDate);
		
		long lDiffInMillies = Math.abs(lEnd - lStart);
	    long lDays = TimeUnit.DAYS.convert(lDiffInMillies, TimeUnit.MILLISECONDS);
	    
	    iCount = asSections.size() * ((int) lDays);
		
		return iCount;
	}
	
	SimpleFeatureCollection grabFeaturesInBoundingBox(double x1, double y1, double x2, double y2, SimpleFeatureSource oFeatureSource) {
		
		try {
			
			Filter oFilter = ECQL.toFilter("BBOX(the_geom, " + x1 + ", " + y1 + ", " +x2 + ", " + y2 + ")");
			
			return oFeatureSource.getFeatures(oFilter);
		}
		catch (Exception oEx) {
			Utils.debugLog("QueryExecutorVIIRS.grabFeaturesInBoundingBox Exception " + oEx.toString());
		}
		
		return null;
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
		long lEnd  = TimeEpochUtils.fromDateStringToEpoch(oVIIRSQuery.endToDate);
	    
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
	    
	    long lActualDay = lStart;
	    
	    DateFormat oDateFormat = new SimpleDateFormat("yyyyMMdd");  
	    
	    for (lActualDay = lStart; lActualDay<=lEnd; lActualDay += 1000*60*60*24) {
	    	
	    	Date oActualDay = new Date(lActualDay);
	    	
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

}
