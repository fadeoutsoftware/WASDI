/**
 * Created by Cristiano Nattero on 2020-02-03
 * 
 * Fadeout software
 *
 */
package wasdi.shared.queryexecutors.sobloo;

import org.json.JSONObject;

import com.google.common.base.Preconditions;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.QueryTranslationParser;
import wasdi.shared.queryexecutors.QueryTranslator;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.TimeEpochUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryViewModel;

/**
 * @author c.nattero
 *
 */
public class QueryTranslatorSOBLOO extends QueryTranslator {
	
	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.DiasQueryTranslator#translate(java.lang.String)
	 */
	@Override
	protected String translate(String sQueryFromClient) {
		Preconditions.checkNotNull(sQueryFromClient, "QueryTranslatorSOBLOO.translate: query is null");
		Preconditions.checkNotNull(m_sAppConfigPath, "QueryTranslatorSOBLOO.translate: app config path is null");
		Preconditions.checkNotNull(m_sParserConfigPath, "QueryTranslatorSOBLOO.translate: parser config is null");

		String sResult = null;
		try {
			JSONObject oAppConf = JsonUtils.loadJsonFromFile(m_sAppConfigPath);
			JSONObject oParseConf = JsonUtils.loadJsonFromFile(m_sParserConfigPath);
			String sQuery = this.prepareQuery(sQueryFromClient);
			QueryViewModel oQueryViewModel = parseWasdiClientQuery(sQuery);

			sResult = "";
			if(!Utils.isNullOrEmpty(oQueryViewModel.productName)) {
				sResult += "q=" + oQueryViewModel.productName + "&";
			}
			sResult += parseFootPrint(sQuery);
			sResult += parseTimeFrame(sQuery);


			for (Object oObject : oAppConf.optJSONArray("missions")) {
				JSONObject oJson = (JSONObject) oObject;
				String sName = oJson.optString("indexname", null);
				String sValue = oJson.optString("indexvalue", null);
				String sToken = sName + ':' + sValue; 
				if(sQuery.contains(sToken)) {
					//todo isolate relevant portion of query
					int iStart = Math.max(0, sQuery.indexOf(sToken));
					int iEnd = sQuery.indexOf(')', iStart);
					if(0>iEnd) {
						iEnd = sQuery.length();
					}
					String sQueryPart = sQuery.substring(iStart, iEnd).trim();
					QueryTranslationParser oParser = new QueryTranslationParser(oParseConf.optJSONObject(sValue), oJson);
					String sLocalPart = oParser.parse(sQueryPart); 
					sResult += sLocalPart;
				}
			}

		} catch (Exception oE) {
			WasdiLog.debugLog("QueryTranslatorSOBLOO.translate( " + sQueryFromClient + " ): " + oE);
		}

		return sResult;
	}
	
	
	@Override
	protected String convertRanges(String sQuery) {
		sQuery = sQuery.replaceAll("(\\[[0-9]*) TO ([0-9]*\\])", "$1<$2");
		return sQuery;
	}
	

	@Override
	protected String parseFootPrint(String sQuery) {
		String sResult = "";
		try {
			if(sQuery.contains("footprint")) {
				String sIntro = "( footprint:\"intersects ( POLYGON ( ( ";
				int iStart = sQuery.indexOf(sIntro);
				if(iStart >= 0) {
					iStart += sIntro.length();
				}
				int iEnd = sQuery.indexOf(')', iStart);
				if(0>iEnd) {
					iEnd = sQuery.length();
				}
				Double dNorth = Double.NEGATIVE_INFINITY;
				Double dSouth = Double.POSITIVE_INFINITY;
				Double dEast = Double.NEGATIVE_INFINITY;
				Double dWest = Double.POSITIVE_INFINITY;
				try {
					String[] asCouples = sQuery.substring(iStart, iEnd).trim().split(",");

					for (String sPair: asCouples) {
						try {
							String[] asTwoCoord = sPair.split(" ");
							Double dParallel = Double.parseDouble(asTwoCoord[1]);
							dNorth = Double.max(dNorth, dParallel);
							dSouth = Double.min(dSouth, dParallel);

							Double dMeridian = Double.parseDouble(asTwoCoord[0]);
							dEast = Double.max(dEast, dMeridian);
							dWest = Double.min(dWest, dMeridian);
						} catch (Exception oE) {
							WasdiLog.errorLog("QueryTranslatorSOBLOO.parseFootprint: issue with current coordinate pair: " + sPair + ": ", oE);
						}
					}

					String sCoordinates = "" + dWest + ',' + dSouth + ',' + dEast + ',' + dNorth;
					//todo prefix and suffic according to QueryTranlsationParser
					String sPrefix = "gintersect=";
					if(sQuery.contains("Sentinel-3")) {
						sPrefix = "pwithin=";
					}
					sResult = sPrefix + sCoordinates;
				} catch (Exception oE) {
					WasdiLog.errorLog("QueryTranslatorSOBLOO.parseFootprint: could not complete: ", oE);
				}
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("QueryTranslatorSOBLOO.parseFootprint: could not identify footprint substring limits: ", oE);
		}
		return sResult;
	}
	
	
	@Override
	protected String parseTimeFrame(String sQuery) {
		Long[] alStartEnd = {Long.MAX_VALUE, Long.MIN_VALUE};
		
		String sKeyword = "beginPosition";
		//beginPosition:[2020-01-30T00:00:00.000Z TO 2020-02-06T23:59:59.999Z]
		parseInterval(sQuery, sKeyword, alStartEnd);
		sKeyword = "endPosition";
		//endPosition:[2020-01-30T00:00:00.000Z TO 2020-02-06T23:59:59.999Z]
		parseInterval(sQuery, sKeyword, alStartEnd);

		String sResult = "";
		if(alStartEnd[0].equals(Long.MAX_VALUE)) {
			alStartEnd[0] = Long.MIN_VALUE;
		}
		if(alStartEnd[1].equals(Long.MIN_VALUE)) {
			alStartEnd[1] = System.currentTimeMillis();
		}
		
		//sResult = "&f=timeStamp:range:[" + alStartEnd[0] + '<' + alStartEnd[1] + ']';
		
//		sResult = "&f=acquisition.beginViewingDate:range:[" + alStartEnd[0] + '<' + alStartEnd[1] + ']';
//		sResult += "&f=acquisition.endViewingDate:range:[" + alStartEnd[0] + '<' + alStartEnd[1] + ']';
		
//		sResult = "&f=state.insertionDate:range:[" + alStartEnd[0] + '<' + alStartEnd[1] + ']';
		
		sResult = "&f=acquisition.beginViewingDate:gte:" + alStartEnd[0];
		sResult += "&f=acquisition.endViewingDate:lte:" + alStartEnd[1];
		
		return sResult;
	}

	/**
	 * @param sQuery
	 * @param sKeyword
	 * @param alStartEnd
	 */
	protected void parseInterval(String sQuery, String sKeyword, Long[] alStartEnd) {
		Preconditions.checkNotNull(sQuery, "QueryTranslatorSOBLOO.parseInterval: query is null");
		Preconditions.checkNotNull(sKeyword, "QueryTranslatorSOBLOO.parseInterval: field keyword is null");
		Preconditions.checkNotNull(alStartEnd, "QueryTranslatorSOBLOO.parseInterval: array is null");
		Preconditions.checkElementIndex(0, alStartEnd.length, "QueryTranslatorSOBLOO.parseInterval: 0 is not a valid element index");
		Preconditions.checkElementIndex(1, alStartEnd.length, "QueryTranslatorSOBLOO.parseInterval: 1 is not a valid element index");
		
		String sStart = null;
		String sEnd = null;
		if( sQuery.contains(sKeyword)) {
			int iStart = Math.max(0, sQuery.indexOf(sKeyword));
			iStart = Math.max(iStart, sQuery.indexOf('[', iStart) + 1);
			int iEnd = sQuery.indexOf(']', iStart);
			if(iEnd < 0) {
				iEnd = sQuery.length()-1;
			};
			String[] asTimeQuery= sQuery.substring(iStart, iEnd).trim().split(" TO ");
			sStart = asTimeQuery[0];
			alStartEnd[0] = Long.min(TimeEpochUtils.fromDateStringToEpoch(sStart), alStartEnd[0] );
			sEnd = asTimeQuery[1];
			alStartEnd[1] = Long.max(TimeEpochUtils.fromDateStringToEpoch(sEnd), alStartEnd[1] );
		}
	}


	@Override
	public String getCountUrl(String sQuery) {
		
		String sUrl = "";
		try {
			sUrl = "https://sobloo.eu/api/v1/services/explore/explore/catalog/_count?" + translateAndEncodeParams(sQuery);
		} catch (Exception oE) {
			WasdiLog.errorLog("QueryExecutorSOBLOO.getCountUrl: ", oE);
		}
		return sUrl;
	}


	@Override
	public String getSearchUrl(PaginatedQuery oQuery) {
		return "https://sobloo.eu/api/v1/services/search?" + translateAndEncodeParams(oQuery.getQuery()) +
				"&size=" + oQuery.getLimit()+ "&from=" + oQuery.getOffset() +
				"&pretty=false&flat=false&sort=-timeStamp,uid&max-age-cache=120";
	}
}
