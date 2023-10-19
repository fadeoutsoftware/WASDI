package wasdi.shared.queryexecutors.jrc;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import wasdi.shared.queryexecutors.ResponseTranslator;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

public class ResponseTranslatorJRC extends ResponseTranslator {
	
	private static final String s_sTilesDownloadLink = "https://jeodpp.jrc.ec.europa.eu/ftp/jrc-opendata/GHSL/GHS_BUILT_S_GLOBE_R2023A/GHS_BUILT_S_E2018_GLOBE_R2023A_54009_10/V1-0/tiles/";
	public static final String s_sLinkSeparator = ";";
	public static final int s_iLinkIndex = 0;
	public static final int s_iFileNameIndex = 1;
	public static final int s_iBoundingBox = 2;
	
	@Override
	public List<QueryResultViewModel> translateBatch(String sResponse, boolean bFullViewModel) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getCountResult(String sQueryResult) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	protected List<QueryResultViewModel> translateResults(Map<String, String> aoTilesMap, String sLimit, String sOffset) {
		
		int iOffset = Integer.parseInt(sOffset);
		int iLimit = Integer.parseInt(sLimit);
		
		List<QueryResultViewModel>  aoResults = new ArrayList<>();
		
		int iNumInsertions = 0;
		int iStartingIndex = iOffset * iLimit;
		int iCurrentEntry = 0;
		for (Entry<String, String> oEntry : aoTilesMap.entrySet()) {
			
			if (iCurrentEntry >= iStartingIndex && iNumInsertions < iLimit) {
				QueryResultViewModel oEntryVM = buildResult(oEntry.getKey(), oEntry.getValue());
				aoResults.add(oEntryVM);
				iNumInsertions ++;
			}
			if (iNumInsertions >= iLimit)
				break;
			if (iCurrentEntry >= iStartingIndex + iLimit)
				break;
			iCurrentEntry ++;
		}
		
		return aoResults;
		
	}
	
	
	private QueryResultViewModel buildResult(String sTileId, String sTileFootprint) {
		QueryResultViewModel oRes = new QueryResultViewModel();
		
		try {
			String sTitle = "GHS_BUILT_S_E2018_GLOBE_R2023A_54009_10_V1_0_" + sTileId; // TODO: prendere i risultati in modo più intelligente
			String sFootprint = getWasdiFormatFootPrint(sTileFootprint);
			String sLink = getLink(sTitle, sFootprint);
			
			oRes.setTitle(sTitle);
			oRes.setSummary("");
			oRes.setId(sTitle);
			oRes.setLink(sLink);
			oRes.setFootprint(sFootprint);
			oRes.setProvider("JRC");
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("ResponseTranslatorJRC.buildResult. Error while creating the view model: " + oEx.getMessage());
		}
		return oRes;
		
	}
	
	private String getWasdiFormatFootPrint(String sESRI54009Footprint) {
		String sPrefix = sESRI54009Footprint.substring(0, 16);
		String sSuffix = sESRI54009Footprint.substring(sESRI54009Footprint.length() - 3, sESRI54009Footprint.length());
		
		String sSanitizedESRIFootprint = sESRI54009Footprint.substring(16, sESRI54009Footprint.length() - 3);
		
		List<String> asPairs = Arrays.asList(sSanitizedESRIFootprint.split(", "));
		
		List<Integer> asLong = asPairs.stream()
				.map(sPair -> sPair.split(" "))
				.map(asArray -> asArray[0])
				.map(sLong -> Integer.parseInt(sLong))
				.collect(Collectors.toList());
		List<Integer> asLat = asPairs.stream()
				.map(sPair -> sPair.split(" "))
				.map(asArray -> asArray[1])
				.map(sLong -> Integer.parseInt(sLong))
				.collect(Collectors.toList());
		
		int iMaxLong = Collections.max(asLong);
		int iMinLong = Collections.min(asLong);
		
		int iMaxLat = Collections.max(asLat);
		int iMinLat = Collections.min(asLat);
		
		double dMaxLong = QueryExecutorJRC.translateLatitude(iMaxLong, QueryExecutorJRC.s_sESRI54009, QueryExecutorJRC.s_sEPSG4326);
		double dMinLong = QueryExecutorJRC.translateLatitude(iMinLong, QueryExecutorJRC.s_sESRI54009, QueryExecutorJRC.s_sEPSG4326);
		
		double dMaxLat = QueryExecutorJRC.translateLatitude(iMaxLat, QueryExecutorJRC.s_sESRI54009, QueryExecutorJRC.s_sEPSG4326);
		double dMinLat = QueryExecutorJRC.translateLatitude(iMinLat, QueryExecutorJRC.s_sESRI54009, QueryExecutorJRC.s_sEPSG4326);
		
		
//		List<String> asTranslatedCoordinates = asPairs.stream()
//				.map(sPair -> sPair.split(" "))
//				.map(asCoords -> getEPSGCoordinates(asCoords[0], asCoords[1]))
//				.collect(Collectors.toList());
		
		String sCoord = dMinLong + " " + dMaxLat + ", " + dMaxLong + " " + dMaxLat + ", " + dMaxLong + " " + dMinLat + ", " + dMinLong + " " + dMinLat + ", " + dMinLong + " " + dMaxLat;
		
		
		// now we can for the footprint in wasdi format
//		String sTargetCoordinates = String.join(", ", asTranslatedCoordinates);
		return sPrefix + sCoord + sSuffix;
		
	}

	
	private String getEPSGCoordinates(String sESRILongitude, String sESRILatitude) {
		double dLongitude = Integer.parseInt(sESRILongitude.trim());
		double dLatitude = Integer.parseInt(sESRILatitude.trim());
		double[] adEPGSCoordinates = QueryExecutorJRC.translateCoordinate(dLongitude, dLatitude, QueryExecutorJRC.s_sESRI54009, QueryExecutorJRC.s_sEPSG4326);
		return adEPGSCoordinates[0] + " " + adEPGSCoordinates[1];
		
	}
	
	private String getLink(String sTitle, String sBoundingBox) {
		StringBuilder oBuilder = new StringBuilder();
		
		// 0 - DOWNLAOD LINK
		String sLink = s_sTilesDownloadLink + sTitle + ".zip";
		oBuilder.append(sLink);
		oBuilder.append(s_sLinkSeparator);
		
		// 1 - FILE NAME
		oBuilder.append(sTitle);
		oBuilder.append(s_sLinkSeparator);
		
		// 2 - BOUNDING BOX
		String sBBox = "";
		if (!Utils.isNullOrEmpty(sBoundingBox))
			sBBox = sBoundingBox;
		oBuilder.append(sBBox);
		
		return oBuilder.toString();
	
	}
	
	
	
	
	

}
