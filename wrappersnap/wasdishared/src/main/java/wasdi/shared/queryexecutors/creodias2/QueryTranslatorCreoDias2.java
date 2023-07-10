package wasdi.shared.queryexecutors.creodias2;

import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;

import org.esa.snap.rcp.actions.raster.CreateGeoCodingDisplacementBandsAction;
import org.json.JSONObject;

import com.google.common.base.Preconditions;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.QueryTranslationParser;
import wasdi.shared.queryexecutors.QueryTranslator;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryViewModel;

public class QueryTranslatorCreoDias2 extends QueryTranslator {
	
	protected String m_sCreoDiasApiBaseUrl = "https://datahub.creodias.eu/odata/v1/Products?";
	private static final String sODataEQ = "eq";  // TODO: check naming convention
	private static final String sODataAND = "and"; // TODO: check naming convention
	private static final String sODataGE = "ge"; // greater or equal
	private static final String sODataLE = "lt"; // less or equal
	private static final String sOdataFilterOption = "$filter=";

 
	@Override
	protected String translate(String sQueryFromClient) {
		Preconditions.checkNotNull(sQueryFromClient, "QueryTranslatorCreoDias2.translate: query is null");
		// probably the two checks below will be removed
		Preconditions.checkNotNull(m_sAppConfigPath, "QueryTranslatorCreoDias2.translate: app config path is null");
		Preconditions.checkNotNull(m_sParserConfigPath, "QueryTranslatorCreoDias2.translate: parser config is null");
		
		
		String sQuery = this.prepareQuery(sQueryFromClient);
		QueryViewModel oQueryViewModel = parseWasdiClientQuery(sQuery);
		
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
		int iRelativeOrbit = oQueryViewModel.relativeOrbit; 
		if (iRelativeOrbit > 0 && iRelativeOrbit < 176)
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
		if (!Utils.isNullOrEmpty(oQueryViewModel.productLevel)) {
			String sLevel = oQueryViewModel.productLevel.equals("L1") ? "1" : "2";
			asQueryElements.add(createStringAttribute("processingLevel", sLevel));
		}
		
		
		
		
		
		
		// TODO: polarisation -- TO BE CONFIRMED (I added the processing of the value in the query translator. It was not supported for Sentinel-1)
		if (!Utils.isNullOrEmpty(oQueryViewModel.polarisation))
			asQueryElements.add(createStringAttribute("polarisationChannels", oQueryViewModel.polarisation));
		
			
		
		// timeliness
//		if (!Utils.isNullOrEmpty(oQueryViewModel.timeliness)
		
		
		// TODO: satellite platform (e.g. Sentinel-2 A or B), not supported by the queryViewModel? To be confirmed.
		// TODO: swath (for Sentinel-1), not supported by the queryViewModel? To be confirmed.
		


			
			
		// to 
		
		/*
		
		// all the other values, is probably better if we can parse them
		String sProductType = createStringAttribute("productType", oQueryViewModel.productType);
		String sPolarisation = ""; // TODO: no support in the parsed query
		String sSensor = createStringAttribute("sensorMode", oQueryViewModel.sensorMode);
		String sRelativeOrbit = createIntegerAttribute("relativeOrbitNumber", "eq", oQueryViewModel.relativeOrbit);
		String sSwathIdentifier = ""; // TODO: ????
		
				String sEndDateFilter = createSensingDateFilter(sEndDate, false); // TODO: do we generally consider the interval as included?
		*/
		
		String sFilterValue = String.join(" " + sODataAND + " ", asQueryElements);	
	

		return sOdataFilterOption + sFilterValue;
	}
	
	private String createCollectionNameEqFilter(String sCollectionName) {
		List<String> asFilterElements = Arrays.asList("Collection/Name", sODataEQ, "'" + sCollectionName.toUpperCase() + "'");
		return String.join(" ", asFilterElements); 
	}
	
	private String createProductNameEqFilter(String sProductName) {
		List<String> asFilterElements = Arrays.asList("Name", sODataEQ, "'" + sProductName.toUpperCase() + "'");
		return "contains(Name, '" + sProductName + "')"; 
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
		return "OData.CSC.Intersects(area=geography'SRID=4326;POLYGON((" + String.join(", ", asCoordinates) + "))')";
	}
	
	private String createStringAttribute(String sName, String sValue) {
		return "Attributes/OData.CSC.StringAttribute/any(i0:i0/Name eq '" + sName+ "' and i0/Value eq '" + sValue + "')";
	}
	

	private String createIntegerAttribute(String sName, String sOperator, int sValue) {
		return "Attributes/OData.CSC.IntegerAttribute/any(i0:i0/Name eq '" + sName+ "' and i0/Value " + sOperator +  " '" + sValue + "')";
	}
	
	private String createDoubleAttribute(String sName, String sOperator, double sValue) {
		return "Attributes/OData.CSC.DoubleAttribute/any(i0:i0/Name eq '" + sName+ "' and i0/Value " + sOperator +  " '" + sValue + "')";
	}
	
	
	private boolean isBoundingBoxValid(QueryViewModel oQueryViewModel) {
		return !(Utils.isNullOrEmpty(oQueryViewModel.north) 
				|| Utils.isNullOrEmpty(oQueryViewModel.south)
				|| Utils.isNullOrEmpty(oQueryViewModel.west)
				|| Utils.isNullOrEmpty(oQueryViewModel.east));
	}

	@Override
	public String getCountUrl(String sQuery) {
		// TODO Auto-generated method stub
		if(Utils.isNullOrEmpty(sQuery)) {
			WasdiLog.debugLog("QueryTranslatorCreoDias2.getCountUrl: sQuery is null");
		}
		String sUrl = m_sCreoDiasApiBaseUrl;
		sUrl+=translateAndEncodeParams(sQuery) + "&$count=True";
		
		return sUrl;
	}

	@Override
	public String getSearchUrl(PaginatedQuery oQuery) {
		// TODO Auto-generated method stub
		String sUrl = m_sCreoDiasApiBaseUrl;
		sUrl+= translateAndEncodeParams(oQuery.getQuery());
		sUrl += "&maxRecords=" + oQuery.getOriginalLimit();
		
		try {
			
			int iItemsPerPage = Integer.parseInt(oQuery.getOriginalLimit());
			int iActualOffset = Integer.parseInt(oQuery.getOffset());
			int iSkipCount = iItemsPerPage * iActualOffset;

			// handle the offset
			if (iSkipCount > 0)
				sUrl = "&skip=" + iSkipCount; 
			
			// handle the number of results per pages
			sUrl += "&top=" + iItemsPerPage;
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("QueryTranslatorCreoDias2.getSearchUrl: exception generating the page parameter  " + oEx.toString());
		}
		
		sUrl += "&$orderby=ContentDate/Start asc"; // TODO: do we get these values from the client or we have a default order?
		
		
		return sUrl;
	}

	public static void main(String[]args) {
		String clientString = "( beginPosition:[2023-06-29T00:00:00.000Z TO 2023-07-06T23:59:59.999Z] AND endPosition:[2023-06-29T00:00:00.000Z TO 2023-07-06T23:59:59.999Z] ) AND   (platformname:Sentinel-1 AND filename:S1A_* AND producttype:SLC AND polarisationmode:VV AND sensoroperationalmode:EW AND relativeorbitnumber:123)";
		QueryTranslatorCreoDias2 tr = new QueryTranslatorCreoDias2();
		tr.setAppconfigPath("C:/WASDI/GIT/WASDI/client/app/config/appconfig.json");
		tr.setParserConfigPath("C:/WASDI/GIT/WASDI/configuration/creodias2ParserConfig.json");
//		String s = tr.getCountUrl(clientString);
//		System.out.println(s);
		
		// try the bounding box
//		String sQuery = "( footprint:\"intersects(POLYGON((5.843719647673683 43.22105226995176,5.843719647673683 46.61479620873007,10.763258396058559 46.61479620873007,10.763258396058559 43.22105226995176,5.843719647673683 43.22105226995176)))\" ) AND ( beginPosition:[2023-06-30T00:00:00.000Z TO 2023-06-30T23:59:59.999Z] AND endPosition:[2023-06-30T00:00:00.000Z TO 2023-06-30T23:59:59.999Z] ) AND   (platformname:Sentinel-1 AND filename:S1A_*)";
//		String sRes = tr.getCountUrl(sQuery);
//		System.out.println(sRes + ""); //OK
		
		// try the search by name
//		sQuery = "S1A_IW_SLC__1SDV_20230630T001327_20230630T001357_049208_05EACA_F030 AND ( beginPosition:[2023-06-30T00:00:00.000Z TO 2023-06-30T23:59:59.999Z] AND endPosition:[2023-06-30T00:00:00.000Z TO 2023-06-30T23:59:59.999Z] ) AND   (platformname:Sentinel-1)";
//		sRes = tr.getCountUrl(sQuery);
//		System.out.println(sRes); // OK
		

		
		String sQuery2 = "( beginPosition:[2023-07-03T00:00:00.000Z TO 2023-07-10T23:59:59.999Z] AND endPosition:[2023-07-03T00:00:00.000Z TO 2023-07-10T23:59:59.999Z] ) AND   (platformname:Sentinel-1 AND producttype:SLC AND polarisationmode:HH AND sensoroperationalmode:IW AND relativeorbitnumber:5)";
		String sRes2 = tr.getCountUrl(sQuery2);
		System.out.println("\nQuery for Sentinel-1. Parameters: [time frame, product type, Polarisation, Sensor mode, Relative orbit number]. Must return 5 results");
		System.out.println(sRes2);
		
		String sQuery3 = "( beginPosition:[2023-07-03T00:00:00.000Z TO 2023-07-10T23:59:59.999Z] AND endPosition:[2023-07-03T00:00:00.000Z TO 2023-07-10T23:59:59.999Z] ) AND   (platformname:Sentinel-2 AND producttype:S2MSI1C AND cloudcoverpercentage:[3 TO 30])";
		String sRes3 = tr.getCountUrl(sQuery3);
		System.out.println("\nQuery for Sentinel-1. Parameters: [time frame, product type, cloud coverage]. Must return 20057 results");
		System.out.println(sRes3);

	}
}
