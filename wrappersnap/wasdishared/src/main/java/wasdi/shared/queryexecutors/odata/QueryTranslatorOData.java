package wasdi.shared.queryexecutors.odata;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.QueryTranslator;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryViewModel;

public class QueryTranslatorOData extends QueryTranslator {
	
	protected String m_sApiBaseUrl;
	
	private static final String sODataEQ = "eq";  
	private static final String sODataAND = "and";
	private static final String sODataGE = "ge"; // greater or equal
	private static final String sODataLE = "lt"; // less or equal
	private static final String sODataFilterOption = "$filter=";
	private static final String sODataSkipOption = "$skip=";
	private static final String sODataTopOption = "$top=";
	private static final String sODataOrderBy = "$orderby=";
	
	// map the polarisation field in the QueryViewModel to the corresponding OData attribute, depending on the platform
	private static final HashMap<String, String> asODATA_POLARISATION_MODE_MAP =  new HashMap<>();
	
	// map the absoluteOrbit field in the QueryViewModel to the corresponding OData attribute, depending on the platform
	private static final HashMap<String, String> asODATA_ABSOLUTE_ORBIT_MAP = new HashMap<>();
	
	// map the relativeOrbit field in the QueryViewModel to the corresponding OData attribute, depending on the platform
	private static final HashMap<String, String> asODATA_RELATIVE_ORBIT_MAP = new HashMap<>();
	
	// map the timeliness field in the QueryViewModel to the corresponding OData attribute, depending on the platform
	private static final HashMap<String, String> asODATA_TIMELINESS_MAP = new HashMap<String, String>();
	
	static {
		asODATA_POLARISATION_MODE_MAP.put(Platforms.ENVISAT, "phaseNumber");
		asODATA_POLARISATION_MODE_MAP.put(Platforms.SENTINEL1, "polarisationChannels");
		
		asODATA_ABSOLUTE_ORBIT_MAP.put(Platforms.SENTINEL5P, "orbitNumber");
		asODATA_ABSOLUTE_ORBIT_MAP.put(Platforms.ENVISAT, "cycleNumber");
		asODATA_ABSOLUTE_ORBIT_MAP.put(Platforms.LANDSAT5, "rowNumber");
		asODATA_ABSOLUTE_ORBIT_MAP.put(Platforms.LANDSAT7, "rowNumber");
	
		asODATA_RELATIVE_ORBIT_MAP.put(Platforms.SENTINEL1, "relativeOrbitNumber");
		asODATA_RELATIVE_ORBIT_MAP.put(Platforms.SENTINEL3, "relativeOrbitNumber");
		asODATA_RELATIVE_ORBIT_MAP.put(Platforms.SENTINEL6, "relativeOrbitNumber");
		asODATA_RELATIVE_ORBIT_MAP.put(Platforms.LANDSAT5, "pathNumber");
		asODATA_RELATIVE_ORBIT_MAP.put(Platforms.LANDSAT7, "pathNumber");

		asODATA_TIMELINESS_MAP.put(Platforms.SENTINEL1, "swathIdentifier");
		asODATA_TIMELINESS_MAP.put(Platforms.SENTINEL3, "timeliness");
		asODATA_TIMELINESS_MAP.put(Platforms.SENTINEL5P, "processingMode");
		asODATA_TIMELINESS_MAP.put(Platforms.SENTINEL6, "timeliness");
	}


	public QueryTranslatorOData() {
		super();
	}

	@Override
	public String getCountUrl(String sQuery) {
		if(Utils.isNullOrEmpty(sQuery)) {
			WasdiLog.debugLog("QueryTranslatorOData.getCountUrl: sQuery is null");
		}
		String sUrl = m_sApiBaseUrl;
		sUrl+=translateAndEncodeParams(sQuery) + "&$count=True";
		
		WasdiLog.debugLog("QueryTranslatorOData.getCountUrl. Generated OData query URL: " + sUrl);
		return sUrl;
	}
	
	@Override
	protected String translate(String sQueryFromClient) {
		Preconditions.checkNotNull(sQueryFromClient, "QueryTranslatorCreoDias2.translate: query is null");
		
		
		String sQuery = this.prepareQuery(sQueryFromClient);
		QueryViewModel oQueryViewModel = parseWasdiClientQuery(sQuery);
		refineQueryViewModel(sQuery, oQueryViewModel);
		
		List<String> asQueryElements = new LinkedList<>();
		
		String sPlatform = oQueryViewModel.platformName;
		String sStartDate = oQueryViewModel.startFromDate;
		String sEndDate = oQueryViewModel.startToDate;
		
		// product name (the one taken in input in the text box) - used for exact matches with a product
		if (!Utils.isNullOrEmpty(oQueryViewModel.productName))
			asQueryElements.add(createProductNameEqFilter(oQueryViewModel.productName));
		
		// platform (e.g. Sentinel-1). This should always be sent by the client
		if (!Utils.isNullOrEmpty(sPlatform)) {
			
			if (sPlatform.equals(Platforms.LANDSAT8)) {
				sPlatform = "LANDSAT-8-ESA";
			}
			
			asQueryElements.add(createCollectionNameEqFilter(sPlatform));
		}
		
		// start of the time interval
		if (!Utils.isNullOrEmpty(sStartDate))
			asQueryElements.add(createSensingDateFilter(sStartDate, true));
		
		// end of the time interval
		if (!Utils.isNullOrEmpty(sEndDate))
			asQueryElements.add(createSensingDateFilter(sEndDate, false));
		
		// bounding box
		if (isBoundingBoxValid(oQueryViewModel))
			asQueryElements.add(createGeographicalFillter(oQueryViewModel.north, oQueryViewModel.south, oQueryViewModel.east, oQueryViewModel.west));
		
		// product type
		if (!Utils.isNullOrEmpty(oQueryViewModel.productType))
			asQueryElements.add(createStringAttribute("productType", oQueryViewModel.productType));
		
		// sensor operational mode
		if (!Utils.isNullOrEmpty(oQueryViewModel.sensorMode))
			asQueryElements.add(createStringAttribute("operationalMode", oQueryViewModel.sensorMode));
		
		// relative orbit number
		// TODO: in creodias, the interval for the relative orbit is higher
		int iRelativeOrbit = oQueryViewModel.relativeOrbit; 
		String sRelativeOrbitOdataAttr = asODATA_RELATIVE_ORBIT_MAP.get(sPlatform);
		if (isValidRelativeOrbit(sPlatform, iRelativeOrbit))
			asQueryElements.add(createIntegerAttribute(sRelativeOrbitOdataAttr, sODataEQ, iRelativeOrbit));
		
		// cloud coverage - from
		Double dCloudCovFrom = oQueryViewModel.cloudCoverageFrom; 
		if (dCloudCovFrom != null && dCloudCovFrom >= 0 && dCloudCovFrom <= 100)
			asQueryElements.add(createDoubleAttribute("cloudCover", sODataGE, dCloudCovFrom));
		
		// cloud coverage - to
		Double dCloudCovTo = oQueryViewModel.cloudCoverageTo;
		if (dCloudCovTo != null && dCloudCovTo >= 0 && dCloudCovTo <= 100)
			asQueryElements.add(createDoubleAttribute("cloudCover", sODataLE, dCloudCovTo));
		
		// product level
		String sProductLevelCode = getProductLevelCode(oQueryViewModel.productLevel, sPlatform);
		if (!Utils.isNullOrEmpty(sProductLevelCode))
			asQueryElements.add(createStringAttribute("processingLevel", sProductLevelCode));
		
		// timeliness
		String sTimelinessODataAttribute = asODATA_TIMELINESS_MAP.get(sPlatform);
		String sTimelinessCode = getTimelinessCode(sPlatform, oQueryViewModel.timeliness);
		if (!Utils.isNullOrEmpty(sTimelinessCode) && !Utils.isNullOrEmpty(sTimelinessODataAttribute))
			asQueryElements.add(createStringAttribute(sTimelinessODataAttribute, sTimelinessCode));
					
		// absolute orbit
		int iAbsoluteOrbit = oQueryViewModel.absoluteOrbit;
		String sAbsoluteOrbitAtt = asODATA_ABSOLUTE_ORBIT_MAP.get(sPlatform);
		if ( isValidAbsoluteOrbit(sPlatform, iAbsoluteOrbit) && !Utils.isNullOrEmpty(sAbsoluteOrbitAtt) ) 
			asQueryElements.add(createIntegerAttribute(sAbsoluteOrbitAtt, sODataEQ, iAbsoluteOrbit));
			
		// polarisation	
		String sODataPolarisationAtt = asODATA_POLARISATION_MODE_MAP.get(sPlatform);
		if (!Utils.isNullOrEmpty(oQueryViewModel.polarisation) && !Utils.isNullOrEmpty(sODataPolarisationAtt))
			asQueryElements.add(createStringAttribute(sODataPolarisationAtt, oQueryViewModel.polarisation));
		
		// instrument
		if (!Utils.isNullOrEmpty(oQueryViewModel.instrument))
			asQueryElements.add(createStringAttribute("instrumentShortName", oQueryViewModel.instrument));
		
		// satellite platform identifier
		String sPlatformId = getPlatformSerialIdentifierCode(oQueryViewModel.platformSerialIdentifier);
		if (!Utils.isNullOrEmpty(sPlatformId))
			asQueryElements.add(createStringAttribute("platformSerialIdentifier", sPlatformId));
		
		// swath identifier
		String sSwathId = oQueryViewModel.timeliness;
		String sODataSwathId = sTimelinessODataAttribute; // if the odata attribute is present, we already found it when checking the timeliness
		if (sPlatform.equals(Platforms.SENTINEL1) && !Utils.isNullOrEmpty(sSwathId) && !Utils.isNullOrEmpty(sODataSwathId)) {
			asQueryElements.add(createStringAttribute(sODataSwathId, sSwathId));
		}

		asQueryElements.add("Online eq true");
		
		String sFilterValue = String.join(" " + sODataAND + " ", asQueryElements);	
		
		return sODataFilterOption + sFilterValue;
	}
	
	/**
	 * Create the filter in OData format to filter results by platform
	 * @param sCollectionName the name of a platform
	 * @return the string that represents the OData filter by platform 
	 */
	private String createCollectionNameEqFilter(String sCollectionName) {
		List<String> asFilterElements = Arrays.asList("Collection/Name", sODataEQ, "'" + sCollectionName.toUpperCase() + "'");
		return String.join(" ", asFilterElements); 
	}
	
	/**
	 * Creates the option in OData format to filter results by name
	 * @param sCollectionName the name of a product
	 * @return the string that represents the OData filter by product name 
	 */
	private String createProductNameEqFilter(String sProductName) {
		return "contains(Name, '" + sProductName.toUpperCase() + "')"; 
	}
	
	/**
	 * Creates the option in OData format to filter results by sensing date
	 * @param sDate the sensing date, in the format yyyy-mm-ddThh:mm:ss.000Z
	 * @param bStartDate if true, the sDate refers to the beginning of the sensing time. If false, the sDate refers to the end of the sensing time.
	 * @return the string that represents the OData filter by sensing date
	 */
	private String createSensingDateFilter(String sDate, boolean bStartDate) {
		String sInclusion = bStartDate ? sODataGE : sODataLE;		// TODO: so far, I consider both intervals as included. 
		String sContentFilterOption = "ContentDate/Start";
		List<String> asFilterElements = Arrays.asList(sContentFilterOption, sInclusion, sDate);
		return String.join(" ", asFilterElements);
	}
	
	/**
	 * Given the four cardinal points, it returns the option in OData format to filter results by footprint
	 * @param sN north
	 * @param sS south
	 * @param sE east
	 * @param sW west
	 * @return the string that represents the OData filter by footprint
	 */
	private String createGeographicalFillter(Double sN, Double sS, Double sE, Double sW) {
		List<String> asCoordinates = new LinkedList<>();
		asCoordinates.add(sW + " " + sS);
		asCoordinates.add(sE + " " + sS);
		asCoordinates.add(sE + " " + sN);
		asCoordinates.add(sW + " " + sN);
		asCoordinates.add(sW + " " + sS); // polygon must start and end with the same point
		return "OData.CSC.Intersects(Footprint=geography'SRID=4326;POLYGON ((" + String.join(", ", asCoordinates) + "))')";
	}
	
	/**
	 * Creates an option in OData format to filter results by some attribute with string value
	 * @param sName the name of the attribute
	 * @param sValue the value of the attribute
	 * @return the string that represent the OData filter by string attribute
	 */
	private String createStringAttribute(String sName, String sValue) {
		return "Attributes/OData.CSC.StringAttribute/any(i0:i0/Name eq '" + sName+ "' and i0/Value eq '" + sValue + "')";
	}
	

	/**
	 * Creates an option in OData format to filter results by some attribute with integer value
	 * @param sName the name of the attribute
	 * @param sValue the value of the attribute
	 * @return the string that represent the OData filter by integer attribute
	 */
	private String createIntegerAttribute(String sName, String sOperator, int sValue) {
		return "Attributes/OData.CSC.IntegerAttribute/any(i0:i0/Name eq '" + sName+ "' and i0/Value " + sOperator +  " " + sValue + ")";
	}
	
	/**
	 * Creates an option in OData format to filter results by some attribute with double value
	 * @param sName the name of the attribute
	 * @param sValue the value of the attribute
	 * @return the string that represent the OData filter by double attribute
	 */
	private String createDoubleAttribute(String sName, String sOperator, double sValue) {
		return "Attributes/OData.CSC.DoubleAttribute/any(i0:i0/Name eq '" + sName+ "' and i0/Value " + sOperator +  " " + sValue + ")";
	}
	
	/**
	 * @param oQueryViewModel the view model representing the WASDI query 
	 * @return true if the view models has valid information for all the four cardinal points, false otherwise 
	 */
	private boolean isBoundingBoxValid(QueryViewModel oQueryViewModel) {
		return oQueryViewModel.north != null 
				&& oQueryViewModel.south != null
				&& oQueryViewModel.west != null
				&& oQueryViewModel.east != null;
	}
	
	/**
	 * Checks if te relative orbit number is valid for a given platform
	 * @param sPlatformCode the platform code
	 * @param iRelativeOrbit the relative orbit number
	 * @return true if the relative orbit number is valid, false otherwise
	 */
	private boolean isValidRelativeOrbit(String sPlatform, int iRelativeOrbit) {
		if (Utils.isNullOrEmpty(sPlatform) || iRelativeOrbit <= 0) 
			return false;
		
		if (sPlatform.equals(Platforms.SENTINEL1) && iRelativeOrbit <= 175)
			return true;
		
		if (sPlatform.equals(Platforms.SENTINEL3) && iRelativeOrbit <= 442)
			return true;
		
		if (sPlatform.equals(Platforms.SENTINEL6) && iRelativeOrbit <= 127)
			return true;
		
		if ((sPlatform.equals(Platforms.LANDSAT5) || sPlatform.equals(Platforms.LANDSAT7)) 
				&& iRelativeOrbit <= 233)
			return true;
				
		return false;
	}
	
	/**
	 * Checks if the absolute orbit number is valid for a given platform
	 * @param sPlatformCode the platform code
	 * @param iAbsoluteOrbit the relative orbit number
	 * @return true if the absolute orbit number is valid, false otherwise
	 */
	private boolean isValidAbsoluteOrbit(String sPlatform, int iAbsoluteOrbit) {
		if (Utils.isNullOrEmpty(sPlatform) || iAbsoluteOrbit <= 0) 
			return false;
		
		if (sPlatform.equals(Platforms.SENTINEL5P) && iAbsoluteOrbit >= 1 && iAbsoluteOrbit <= 30000)
			return true;
		
		if (sPlatform.equals(Platforms.ENVISAT) && iAbsoluteOrbit >= 6 && iAbsoluteOrbit <= 113) 
			return true;
		
		if ( (sPlatform.equals(Platforms.LANDSAT5) || sPlatform.equals(Platforms.LANDSAT7))
				&& iAbsoluteOrbit >= 1 && iAbsoluteOrbit <= 248) 
			return true;
		
		return false;
	}
	
	/**
	 * Converts the string representing the timeliness from the WASDI format to the OData code
	 * @param sPlatform the platform's name
	 * @param sTimeliness the string representing the timeliness in WASDI format
	 * @return the OData code for timeliness if the platform supports that attribute, null otherwise
	 */
	private String getTimelinessCode(String sPlatform, String sTimeliness) {
		if (Utils.isNullOrEmpty(sTimeliness))
			return null;
		if (sPlatform.equalsIgnoreCase(Platforms.SENTINEL3) || sPlatform.equalsIgnoreCase(Platforms.SENTINEL6)) {
			if (sTimeliness.equalsIgnoreCase("Near Real Time")) 
				return  "NR";
			if (sTimeliness.equalsIgnoreCase("Short Time Critical"))
				return "ST";
			if (sTimeliness.equalsIgnoreCase("Non Time Critical"))
				return "NT";
		} else if (sPlatform.equalsIgnoreCase(Platforms.SENTINEL5P)) {
			if (sTimeliness.equalsIgnoreCase("Offline"))
				return "OFFL";
			if (sTimeliness.equalsIgnoreCase("Near Real Time")) 
				return "NRTI";
			if (sTimeliness.equalsIgnoreCase("Reprocessing")) 
				return "RPRO";
		}
		return null;
	}
	
	/**
	 * Converts the string representing the product level from the WASDI format to the OData code
	 * @param sProductLevel the string representing the level in WASDI format
	 * @param sPlatform the platform's name
	 * @return the OData code for the product level if the platform supports that attribute, an empty string otherwise
	 */
	private String getProductLevelCode(String sProductLevel, String sPlatform) {
		if (Utils.isNullOrEmpty(sProductLevel) || Utils.isNullOrEmpty(sPlatform))
			return "";
		if (sPlatform.equals(Platforms.SENTINEL3)) {
			if (sProductLevel.equalsIgnoreCase("L1"))
				return "1";
			if (sProductLevel.equalsIgnoreCase("L2"))
				return "2";
		} else if (sPlatform.equals(Platforms.SENTINEL5P)) {
			if (sProductLevel.equalsIgnoreCase("LEVEL1B"))
				return "L1b";
			if (sProductLevel.equalsIgnoreCase("LEVEL2"))
				return "L2";
		} else if (sPlatform.equals(Platforms.SENTINEL6)) {
			if (sProductLevel.equals("L1") || sProductLevel.equals("L2"))
				return sProductLevel;
		}
		else if (sPlatform.equals(Platforms.ENVISAT)) {
			return sProductLevel;
		}
		return "";
	}
	
	/**
	 * Converts the string representing the platform serial identifier (e.g. A or B for Sentinel-1) to the corresponding OData code
	 * @param sId the string representing the platform serial id in WASDI format
	 * @return the OData code for the platform serial id if the platform has that id, an empty string otherwise
	 */
	private String getPlatformSerialIdentifierCode(String sId) {
		if (Utils.isNullOrEmpty(sId))
			return "";
		if (sId.equalsIgnoreCase("S1A_*") || sId.equalsIgnoreCase("S2A_*"))
			return "A";
		if (sId.equalsIgnoreCase("S1B_*") || sId.equalsIgnoreCase("S2B_*"))
			return "B";
		return "";
	}
	
	/**
	 * Method meant to complete the parseWasdiQuery in the superclass. It fills the query view model with the information that is not parsed by the superclass
	 * @param sQuery the string representing the WASDI query
	 * @param oViewModel the view model
	 */
	private void refineQueryViewModel(String sQuery, QueryViewModel oViewModel) {
		try {
			WasdiLog.debugLog("QueryTranslatorCreoDias2.refineQueryViewModel. Try to fill view model with missing information");
			if (Utils.isNullOrEmpty(oViewModel.polarisation))
				oViewModel.polarisation = extractValue(sQuery, "polarisationmode");
			if (Utils.isNullOrEmpty(oViewModel.platformSerialIdentifier))
				oViewModel.platformSerialIdentifier = extractValue(sQuery, "filename");
			if (Utils.isNullOrEmpty(oViewModel.instrument))
				oViewModel.instrument = extractValue(sQuery, "Instrument");
			if (oViewModel.relativeOrbit < 0) {
				String sRelativeOrbit = extractValue(sQuery, "relativeorbitstart");
				if (!Utils.isNullOrEmpty(sRelativeOrbit)) {
					try {
						oViewModel.relativeOrbit = Integer.parseInt(sRelativeOrbit); 
					} catch (NumberFormatException oEx) {
						WasdiLog.debugLog("QueryTranslatorCreoDias2.refineQueryViewModel. Impossible to parse relative orbit. " + oEx.getMessage());
					}
				}
			}
			findSwathIdentifier(sQuery, oViewModel);			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("QueryTranslatorCreoDias2.refineQueryViewModel: error ", oEx);
		}
	}
	
	/**
	 * Given the WASDI query, looks for the value of the swath identifier filter. If that value is present, then it is set in the query vuiew model.
	 * @param sQuery the WASDI query
	 * @param oViewModel the view model
	 */
	public void findSwathIdentifier(String sQuery, QueryViewModel oViewModel) {
		if (oViewModel == null || Utils.isNullOrEmpty(sQuery) || Utils.isNullOrEmpty(oViewModel.platformName) 
				|| !oViewModel.platformName.equalsIgnoreCase(Platforms.SENTINEL1) || !Utils.isNullOrEmpty(oViewModel.timeliness) )
			return ;
		String sRegex = "swathidentifier:([A-Z][A-Z\\d,]*)";
		try {
			Pattern oPattern = Pattern.compile(sRegex);
			Matcher oMatcher = oPattern.matcher(sQuery);

			if (oMatcher.find() && oMatcher.groupCount() >= 1) {
				oViewModel.timeliness = oMatcher.group(1);
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryTranslatorCreoDias2.findSwathIdentifier: error while retrieving the swath idenfier from regex.", oEx);
		}
	}

	@Override
	public String getSearchUrl(PaginatedQuery oQuery) {
		String sUrl = m_sApiBaseUrl;
		sUrl+= translateAndEncodeParams(oQuery.getQuery());
		
		try {
			
			int iItemsPerPage = Integer.parseInt(oQuery.getOriginalLimit());
			int iActualOffset = Integer.parseInt(oQuery.getOffset());
					
			// handle the offset
			if (iActualOffset > 0)
				sUrl += "&" + sODataSkipOption + iActualOffset; 
			
			// handle the number of results per pages
			sUrl += "&" + sODataTopOption + iItemsPerPage;
			
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("QueryTranslatorCreoDias2.getSearchUrl: exception generating the page parameter  " + oEx.toString());
		}
		
		sUrl += "&" + sODataOrderBy + "ContentDate/Start%20asc&$expand=Attributes"; // TODO: do we get these values from the client or we have a default order?
		
		return sUrl;
	}

}
