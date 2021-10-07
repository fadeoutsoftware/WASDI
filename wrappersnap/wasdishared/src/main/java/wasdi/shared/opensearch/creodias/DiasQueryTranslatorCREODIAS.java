/**
 * Created by Cristiano Nattero on 2019-12-23
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch.creodias;

import java.util.NoSuchElementException;

import org.json.JSONObject;

import com.google.common.base.Preconditions;

import wasdi.shared.opensearch.DiasQueryTranslator;
import wasdi.shared.opensearch.QueryTranslationParser;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.viewmodels.search.QueryViewModel;

/**
 * @author c.nattero
 *
 */
public class DiasQueryTranslatorCREODIAS extends DiasQueryTranslator {

	private static final String SCLOUDCOVERPERCENTAGE = "cloudcoverpercentage";
	
	//TODO check it, it is taken from sobloo

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.DiasQueryTranslator#translate(java.lang.String)
	 */
	@Override
	protected String translate(String sQueryFromClient) {
		Preconditions.checkNotNull(sQueryFromClient, "DiasQueryTranslatorCREODIAS.translate: query is null");
		Preconditions.checkNotNull(m_sAppConfigPath, "DiasQueryTranslatorCREODIAS.translate: app config path is null");
		Preconditions.checkNotNull(m_sParserConfigPath, "DiasQueryTranslatorCREODIAS.translate: parser config is null");

		String sResult = null;
		try {
			JSONObject oAppConf = WasdiFileUtils.loadJsonFromFile(m_sAppConfigPath);
			JSONObject oParseConf = WasdiFileUtils.loadJsonFromFile(m_sParserConfigPath);
			//from:
			//( footprint:"intersects(POLYGON((91.76001774389503 9.461419178814332,91.76001774389503 29.23273110342357,100.90070010891878 29.23273110342357,100.90070010891878 9.461419178814332,91.76001774389503 9.461419178814332)))" ) AND ( beginPosition:[2020-07-24T00:00:00.000Z TO 2020-07-31T23:59:59.999Z] AND endPosition:[2020-07-24T00:00:00.000Z TO 2020-07-31T23:59:59.999Z] ) AND   (platformname:Sentinel-1 AND producttype:GRD AND relativeorbitnumber:99)
			//to:
			//https://finder.creodias.eu/resto/api/collections/Sentinel1/search.json?maxRecords=10&startDate=2020-07-01T00%3A00%3A00Z&completionDate=2020-07-31T23%3A59%3A59Z&productType=GRD&relativeOrbitNumber=9&sortParam=startDate&sortOrder=descending&status=all&geometry=POLYGON((92.65416040497227+26.088955768777822%2C99.6675662125083+26.233334945401936%2C99.79625057598854+16.91245056850053%2C93.04021840440677+16.881668352246322%2C92.65416040497227+26.088955768777822))&dataset=ESA-DATASET
			String sQuery = this.prepareQuery(sQueryFromClient);
			//todo reformulate cloud cover intervals
			//			int iStart = 0;
			//			String sQueryPart = sQuery;
			//			while(sQueryPart.contains(DiasQueryTranslatorCREODIAS.SCLOUDCOVERPERCENTAGE)) {
			//				iStart = sQuery.indexOf(DiasQueryTranslatorCREODIAS.SCLOUDCOVERPERCENTAGE, iStart);
			//				iStart += DiasQueryTranslatorCREODIAS.SCLOUDCOVERPERCENTAGE.length();
			//				iStart = sQuery.indexOf('[', iStart);
			//				int iEnd = sQuery.indexOf(" TO ", iStart);
			//				
			//			}
			String sCloud = "cloudcoverpercentage:[";
			int iCloudStart = sQuery.indexOf(sCloud);
			if(iCloudStart > 0) {
				iCloudStart += sCloud.length();
				iCloudStart = sQuery.indexOf("<", iCloudStart);
				if(iCloudStart > 0) {
					StringBuilder oBuilder = new StringBuilder();
					oBuilder.append(sQuery.substring(0, iCloudStart));
					oBuilder.append(",");
					oBuilder.append(sQuery.substring(iCloudStart+1));
					sQuery = oBuilder.toString();
				}
			}
			sResult = "";
			QueryViewModel oQueryViewModel = parseWasdiClientQuery(sQueryFromClient);
			
			if(!oAppConf.has("missions")) {
				//infer collection from free text
				if(Utils.isNullOrEmpty(oQueryViewModel.platformName)) {
					throw new NoSuchElementException("No free text and could not find \"mission\" array in json configuration, aborting");
				} else {
					//since mission is not specified in the query, add /search.json here
					sResult += oQueryViewModel.platformName.replace("-", "") + "/search.json?";
				}
			}
			//first things first: append mission name + /search.json? 

			for (Object oMissionObject : oAppConf.optJSONArray("missions")) {
				JSONObject oWasdiMissionJson = (JSONObject) oMissionObject;
				String sName = oWasdiMissionJson.optString("indexname", null);
				String sValue = oWasdiMissionJson.optString("indexvalue", null);
				String sToken = sName + ':' + sValue; 
				if(sQuery.contains(sToken)) {
					//todo isolate relevant portion of query
					int iStart = Math.max(0, sQuery.indexOf(sToken));
					int iEnd = sQuery.indexOf(')', iStart);
					if(0>iEnd) {
						iEnd = sQuery.length();
					}
					String sQueryPart = sQuery.substring(iStart, iEnd).trim();
					
					try {
						QueryTranslationParser oParser = new QueryTranslationParser(oParseConf.optJSONObject(sValue), oWasdiMissionJson);
						String sLocalPart = oParser.parse(sQueryPart); 
						sResult += sLocalPart;						
					}
					catch (Exception oQueryException) {
						// Try to continue
					}

				}
			}
			if(Utils.isNullOrEmpty(sResult)) {
				sResult = oQueryViewModel.platformName.replace("-", "")+ "/search.json?";
			}
			sResult += parseFootPrint(sQuery);
			sResult += parseTimeFrame(sQuery);
			sResult += "&status=all";

//			if (sResult.contains("Sentinel1") && sResult.contains("productType=GRD")) {
//				sResult += "&timeliness=Fast-24h";
//			}
			String sFree = parseProductName(sQueryFromClient);
			if(!Utils.isNullOrEmpty(sFree)) {
				sResult = sResult + "&productIdentifier=%25" + sFree + "%25";
			}

		} catch (Exception oE) {
			Utils.debugLog("DiasQueryTranslatorCREODIAS.translate( " + sQueryFromClient + " ): " + oE);
		}

		return sResult;
	}

	@Override
	protected String parseProductName(String sQuery) {
		String sOld = getProductName(sQuery);

		if(Utils.isNullOrEmpty(sOld)) {
			return "";
		}
		
		while(sOld.startsWith("*")) {
			sOld = sOld.substring(1);
		}
		while(sOld.endsWith("*")) {
			sOld = sOld.substring(0, sOld.length()-1);
		}

		String sResult = WasdiFileUtils.getFileNameWithoutExtensionsAndTrailingDots(sOld);
		boolean bAddDot = !sOld.equals(sResult);

		//again, remove trailing asterisks
		while(sOld.endsWith("*")) {
			sOld = sOld.substring(0, sOld.length()-1);
		}

		//make sure we do not add "null"
		if(sResult == null) {
			sResult = "";
		}
		if(bAddDot) {
			sResult += ".";
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
				} else {
					throw new IllegalArgumentException("Footprint formatted in an unexpected way: " + sQuery + ", ignoring it"); 
				}
				int iEnd = sQuery.indexOf(')', iStart);
				if(0>iEnd) {
					iEnd = sQuery.length();
				}
				sResult ="&geometry=POLYGON((" + sQuery.substring(iStart, iEnd).replaceAll(" ", "+") + "))";
			}
		} catch (Exception oE) {
			Utils.log("ERROR", "DiasQueryTranslatorCREODIAS.parseFootprint: could not identify footprint substring limits: " + oE);
		}
		return sResult;
	}


	@Override
	protected String parseTimeFrame(String sQuery) {
		Preconditions.checkNotNull(sQuery, "Null query");
		String sResult = "";
		try {
			String[] asStartEnd = {"", ""};

			String sKeyword = "beginPosition";
			//beginPosition:[2020-01-30T00:00:00.000Z TO 2020-02-06T23:59:59.999Z]
			parseInterval(sQuery, sKeyword, asStartEnd);
			sKeyword = "endPosition";
			//endPosition:[2020-01-30T00:00:00.000Z TO 2020-02-06T23:59:59.999Z]
			parseInterval(sQuery, sKeyword, asStartEnd);
			if(Utils.isNullOrEmpty(asStartEnd[0]) || Utils.isNullOrEmpty(asStartEnd[1])) {

			}
			sResult = "&startDate=" + asStartEnd[0];
			sResult += "&completionDate=" + asStartEnd[1];

		}catch (Exception oE) {
			Utils.debugLog("DiasQueryTranslatorCREODIAS.parseTimeFrame: " + oE);
		}

		return sResult;
	}

	/**
	 * @param sQuery
	 * @param sKeyword
	 * @param asStartEnd
	 */
	@Override
	protected void parseInterval(String sQuery, String sKeyword, String[] asStartEnd) {
		Preconditions.checkNotNull(sQuery, "DiasQueryTranslatorCREODIAS.parseInterval: query is null");
		Preconditions.checkNotNull(sKeyword, "DiasQueryTranslatorCREODIAS.parseInterval: field keyword is null");
		Preconditions.checkNotNull(asStartEnd, "DiasQueryTranslatorCREODIAS.parseInterval: array is null");
		Preconditions.checkElementIndex(0, asStartEnd.length, "DiasQueryTranslatorCREODIAS.parseInterval: 0 is not a valid element index");
		Preconditions.checkElementIndex(1, asStartEnd.length, "DiasQueryTranslatorCREODIAS.parseInterval: 1 is not a valid element index");

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

			sStart = asTimeQuery[0].trim();
			asStartEnd[0] = sStart;
			sEnd = asTimeQuery[1].trim();
			asStartEnd[1] = sEnd;
		}
	}


	public static void main(String[] args) {
		DiasQueryTranslatorCREODIAS oDQT = new DiasQueryTranslatorCREODIAS();
		String sSuffix = " AND ( beginPosition:[2021-01-10T00:00:00.000Z TO 2021-01-12T23:59:59.999Z] AND endPosition:[2021-01-10T00:00:00.000Z TO 2021-01-12T23:59:59.999Z] )";
		String[] asFullNameWO = {
				"S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423",
				"S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423*",
				"*S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423",
				"*S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423*"
		}; 

		for (String sFreeText : asFullNameWO) {
			String sQuery = sFreeText + sSuffix;
			String sResult = oDQT.parseProductName(sQuery); 
			if(!sResult.equals("S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423")) {
				System.out.println("full name without extension: failed translating: " + sFreeText + ", got: " + sResult);
			}
		}

		String[] asFullNameW = {
				"S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.zip",
				"S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.",

				"S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.zip*",
				"S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.*",

				"*S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.zip",
				"*S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.",

				"*S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.zip*",
				"*S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.*",
		}; 

		for (String sFreeText : asFullNameW) {
			String sQuery = sFreeText + sSuffix;
			String sResult = oDQT.parseProductName(sQuery); 
			if(!sResult.equals("S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.")) {
				System.out.println("full name with extension: failed translating: " + sFreeText + ", got: " + sResult);
			}
		}

		String[] asNoHeadWO = {
				"_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423",
				"*_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423*",
				"*_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423",
				"_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423*"
		};

		for (String sFreeText : asNoHeadWO) {
			String sQuery = sFreeText + sSuffix;
			String sResult = oDQT.parseProductName(sQuery); 
			if(!sResult.equals("_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423")) {
				System.out.println("no head without: failed translating: " + sFreeText + ", got: " + sResult);
			}
		}

		String[] asNoHeadW = {
				"_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.zip",
				"_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.",
				"*_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.zip*",
				"*_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.*",
				"*_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.zip",
				"*_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.",
				"_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.zip*",
				"_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.*"
		};

		for (String sFreeText : asNoHeadW) {
			String sQuery = sFreeText + sSuffix;
			String sResult = oDQT.parseProductName(sQuery); 
			if(!sResult.equals("_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.")) {
				System.out.println("no head with: failed translating: " + sFreeText + ", got: " + sResult);
			}
		}

		String[] asNoTail = {
				"S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D*",
				"S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D",
				"*S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D",
				"*S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D*"
		};

		for (String sFreeText : asNoTail) {
			String sQuery = sFreeText + sSuffix;
			String sResult = oDQT.parseProductName(sQuery); 
			if(!sResult.equals("S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D")) {
				System.out.println("no tail: failed translating: " + sFreeText + ", got: " + sResult);
			}
		}


		String[] asNoHeadNoTail = {
				"*_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D*",
				"_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D*",
				"*_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D",
				"_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D"
		};
		for (String sFreeText : asNoHeadNoTail) {
			String sQuery = sFreeText + sSuffix;
			String sResult = oDQT.parseProductName(sQuery); 
			if(!sResult.equals("_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D")) {
				System.out.println("no head, no tail: failed translating: " + sFreeText + ", got: " + sResult);
			}
		}

		System.out.println("DONE :-)");
	}

}
