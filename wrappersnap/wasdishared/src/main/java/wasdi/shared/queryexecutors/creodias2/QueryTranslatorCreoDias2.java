package wasdi.shared.queryexecutors.creodias2;

import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.LinkedList;

import com.google.common.base.Preconditions;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.QueryTranslator;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryViewModel;

public class QueryTranslatorCreoDias2 extends QueryTranslator {
	
	protected String m_sCreoDiasApiBaseUrl = "https://datahub.creodias.eu/odata/v1/Products?";
	
	private static final String sODataEQ = "eq";  
	private static final String sODataAND = "and";
	private static final String sODataGE = "ge"; // greater or equal
	private static final String sODataLE = "lt"; // less or equal
	private static final String sODataFilterOption = "$filter=";
	private static final String sODataSkipOption = "$skip=";
	private static final String sODataTopOption = "$top=";
	private static final String sODataOrderBy = "$orderby=";
	
	private static final HashMap<String, String> asODATA_POLARISATION_MODE_MAP =  new HashMap<>();
	
	private static final HashMap<String, String> asODATA_ABSOLUTE_ORBIT_MAP = new HashMap<>();
	
	static {
		asODATA_POLARISATION_MODE_MAP.put(Platforms.ENVISAT, "phaseNumber");
		asODATA_POLARISATION_MODE_MAP.put(Platforms.SENTINEL1, "polarisationChannels");
		
		asODATA_ABSOLUTE_ORBIT_MAP.put(Platforms.SENTINEL5P, "orbitNumber");
		asODATA_ABSOLUTE_ORBIT_MAP.put(Platforms.ENVISAT, "cycleNumber");
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
		if ( iRelativeOrbit > 0 && ((sPlatform.equals(Platforms.SENTINEL1) && iRelativeOrbit <= 175) || (sPlatform.equals(Platforms.SENTINEL3) && iRelativeOrbit <= 385)))
			asQueryElements.add(createIntegerAttribute("relativeOrbitNumber", sODataEQ, iRelativeOrbit));
		
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
		String[] asTimeliness = getTimelinessAttributeAndCode(sPlatform, oQueryViewModel.timeliness);
		if (asTimeliness != null && asTimeliness.length > 0)
			asQueryElements.add(createStringAttribute(asTimeliness[0], asTimeliness[1]));
					
		// absolute orbit
		int iAbsoluteOrbit = oQueryViewModel.absoluteOrbit;
		boolean bIsValueForS5 = sPlatform.equals(Platforms.SENTINEL5P) && iAbsoluteOrbit >= 1 && iAbsoluteOrbit <= 30000;
		boolean bIsValueForENVISAT = sPlatform.equals(Platforms.ENVISAT) && iAbsoluteOrbit >= 6 && iAbsoluteOrbit <= 113;
		String sAbsoluteOrbitAtt = asODATA_ABSOLUTE_ORBIT_MAP.get(sPlatform);
		if ( (bIsValueForS5 || bIsValueForENVISAT) && !Utils.isNullOrEmpty(sAbsoluteOrbitAtt) ) {
			asQueryElements.add(createIntegerAttribute(sAbsoluteOrbitAtt, sODataEQ, iAbsoluteOrbit));
		}
		
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
		
		

		// TODO: swath (for Sentinel-1), not supported by the queryViewModel? To be confirmed. What values can it take? (Laura)
		// TODO - IMPORTANT!!! Envisat filters and the corresponding values have no match with what we currently have in WASDI
		// TODO: cosa facciamo con tutti i valori che sono nuovi in creodias e non sono i Wasdi? - non metterli x il momento
		
		// TODO - DONE: polarisation (sentinel 1)
		// TODO - DONE: satellite platform (e.g. Sentinel-2 A or B), not supported by the queryViewModel? To be confirmed.
		// TODO - DONE: instrument (for Sentinel-3)
		// TODO - DONE: relativeorbitstart (for Sentinel-3)

			
		String sFilterValue = String.join(" " + sODataAND + " ", asQueryElements);	
		
		return sODataFilterOption + sFilterValue;
	}
	
	private String createCollectionNameEqFilter(String sCollectionName) {
		List<String> asFilterElements = Arrays.asList("Collection/Name", sODataEQ, "'" + sCollectionName.toUpperCase() + "'");
		return String.join(" ", asFilterElements); 
	}
	
	private String createProductNameEqFilter(String sProductName) {
		return "contains(Name, '" + sProductName.toUpperCase() + "')"; 
	}
	

	private String createSensingDateFilter(String sDate, boolean bStartDate) {
		String sInclusion = bStartDate ? sODataGE : sODataLE;		// TODO: so far, I consider both intervals as included. 
		String sContentFilterOption = "ContentDate/Start";
		List<String> asFilterElements = Arrays.asList(sContentFilterOption, sInclusion, sDate);
		return String.join(" ", asFilterElements);
	}
	
	private String createGeographicalFillter(Double sN, Double sS, Double sE, Double sW) {
		List<String> asCoordinates = new LinkedList<>();
		asCoordinates.add(sW + " " + sS);
		asCoordinates.add(sE + " " + sS);
		asCoordinates.add(sE + " " + sN);
		asCoordinates.add(sW + " " + sN);
		asCoordinates.add(sW + " " + sS); // polygon must start and end with the same point
		return "OData.CSC.Intersects(Footprint=geography'SRID=4326;POLYGON ((" + String.join(", ", asCoordinates) + "))')";
	}
	
	private String createStringAttribute(String sName, String sValue) {
		return "Attributes/OData.CSC.StringAttribute/any(i0:i0/Name eq '" + sName+ "' and i0/Value eq '" + sValue + "')";
	}
	

	private String createIntegerAttribute(String sName, String sOperator, int sValue) {
		return "Attributes/OData.CSC.IntegerAttribute/any(i0:i0/Name eq '" + sName+ "' and i0/Value " + sOperator +  " " + sValue + ")";
	}
	
	private String createDoubleAttribute(String sName, String sOperator, double sValue) {
		return "Attributes/OData.CSC.DoubleAttribute/any(i0:i0/Name eq '" + sName+ "' and i0/Value " + sOperator +  " " + sValue + ")";
	}
	
	
	private boolean isBoundingBoxValid(QueryViewModel oQueryViewModel) {
		return oQueryViewModel.north != null 
				&& oQueryViewModel.south != null
				&& oQueryViewModel.west != null
				&& oQueryViewModel.east != null;
	}
	
	private String[] getTimelinessAttributeAndCode(String sPlatform, String sTimeliness) {
		if (Utils.isNullOrEmpty(sTimeliness))
			return null;
		if (sPlatform.equalsIgnoreCase(Platforms.SENTINEL3)) {
			String sAttribute = "timeliness";
			if (sTimeliness.equalsIgnoreCase("Near Real Time")) 
				return  new String[]{sAttribute, "NR"};
			if (sTimeliness.equalsIgnoreCase("Short Time Critical"))
				return new String[]{sAttribute, "ST"};
			if (sTimeliness.equalsIgnoreCase("Non Time Critical"))
				return new String[]{sAttribute, "NT"};
		} else if (sPlatform.equalsIgnoreCase(Platforms.SENTINEL5P)) {
			String sAttribute = "processingMode";
			if (sTimeliness.equalsIgnoreCase("Offline"))
				return new String[]{sAttribute, "OFFL"};
			if (sTimeliness.equalsIgnoreCase("Near Real Time")) 
				return new String[]{sAttribute, "NRTI"};
			if (sTimeliness.equalsIgnoreCase("Reprocessing")) 
				return new String[]{sAttribute, "RPRO"};
		}
		return null;
	}
	
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
		} else if (sPlatform.equals(Platforms.ENVISAT)) {
			return sProductLevel;
		}
		return "";
	}
	
	private String getPlatformSerialIdentifierCode(String sId) {
		if (Utils.isNullOrEmpty(sId))
			return "";
		if (sId.equalsIgnoreCase("S1A_*") || sId.equalsIgnoreCase("S2A_*"))
			return "A";
		if (sId.equalsIgnoreCase("S1B_*") || sId.equalsIgnoreCase("S2B_*"))
			return "B";
		return "";
	}
	
	
	private void refineQueryViewModel(String sQuery, QueryViewModel oViewModel) {
		WasdiLog.debugLog("QueryTranslatorCreoDias2.refineQueryViewModel. Try to fill view model with missing information");
		oViewModel.polarisation = extractValue(sQuery, "polarisationmode");
		oViewModel.platformSerialIdentifier = extractValue(sQuery, "filename");
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
		
		/*
		findPolarisation(sQuery, oViewModel);
		findPlatformSerialIdentifier(sQuery, oViewModel);
		findInstrument(sQuery, oViewModel);
		findRelativeOrbit(sQuery, oViewModel);
		*/
	}
	
	
	private void findPolarisation(String sQuery, QueryViewModel oViewModel) {
		if (oViewModel == null || Utils.isNullOrEmpty(oViewModel.platformName) || !oViewModel.platformName.equalsIgnoreCase(Platforms.SENTINEL1))
			return;

		String sRegex = "polarisationmode:([HV\\+]{2,5})[\\s\\)]";
		Pattern oPattern = Pattern.compile(sRegex);
		Matcher oMatcher = oPattern.matcher(sQuery);
		
		if (oMatcher.find() && oMatcher.groupCount() >= 1) 
			oViewModel.polarisation = oMatcher.group(1);
	}
	
	private void findPlatformSerialIdentifier(String sQuery, QueryViewModel oViewModel) {
		if (oViewModel == null || Utils.isNullOrEmpty(oViewModel.platformName) 
				|| (!oViewModel.platformName.equalsIgnoreCase(Platforms.SENTINEL1) && !oViewModel.platformName.equalsIgnoreCase(Platforms.SENTINEL2)))
			return;
		
		String sPlatformName = oViewModel.platformName;

		String sPlatformCode = sPlatformName.equalsIgnoreCase(Platforms.SENTINEL1) ? "S1" : "S2";
		String sRegex = "filename:(" + sPlatformCode + "[AB]" + "\\_\\*)";
		Pattern oPattern = Pattern.compile(sRegex);
		Matcher oMatcher = oPattern.matcher(sQuery);
		
		if (oMatcher.find() && oMatcher.groupCount() >= 1)
			oViewModel.platformSerialIdentifier = oMatcher.group(1);
	}
	
	private void findInstrument(String sQuery, QueryViewModel oViewModel) {
		if (oViewModel == null || Utils.isNullOrEmpty(oViewModel.platformName) || !oViewModel.platformName.equalsIgnoreCase(Platforms.SENTINEL3))
			return;
		
		String sRegex = "[iI]nstrument:([A-Z]+)[\\s\\)]";
		Pattern oPattern = Pattern.compile(sRegex);
		Matcher oMatcher = oPattern.matcher(sQuery);
		
		if (oMatcher.find() && oMatcher.groupCount() >= 1)
			oViewModel.instrument = oMatcher.group(1);
	}
	
	private void findRelativeOrbit(String sQuery, QueryViewModel oViewModel) {
		if (oViewModel == null || Utils.isNullOrEmpty(oViewModel.platformName) 
				|| !oViewModel.platformName.equalsIgnoreCase(Platforms.SENTINEL3) || oViewModel.relativeOrbit >= 0)
			return;
		
		String sRegex = "relativeorbitstart:(\\d\\d?\\d?)[\\s\\)]";
		Pattern oPattern = Pattern.compile(sRegex);
		Matcher oMatcher = oPattern.matcher(sQuery);
		
		if (oMatcher.find() && oMatcher.groupCount() >= 1) 
			oViewModel.relativeOrbit = Integer.parseInt(oMatcher.group(1));
	}
	
	

	@Override
	public String getCountUrl(String sQuery) {
		if(Utils.isNullOrEmpty(sQuery)) {
			WasdiLog.debugLog("QueryTranslatorCreoDias2.getCountUrl: sQuery is null");
		}
		String sUrl = m_sCreoDiasApiBaseUrl;
		sUrl+=translateAndEncodeParams(sQuery) + "&$count=True";
		
		WasdiLog.debugLog("QueryTranslatorCreoDias2.getCountUrl. Generated OData query URL: " + sUrl);
		return sUrl;
	}

	@Override
	public String getSearchUrl(PaginatedQuery oQuery) {
		String sUrl = m_sCreoDiasApiBaseUrl;
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
