/**
 * Created by Cristiano Nattero on 2018-12-04
 * 
 * Fadeout software
 *
 */
package wasdi.shared.queryexecutors.onda;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.QueryTranslator;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * @author c.nattero
 *
 */
public class QueryTranslatorONDA extends QueryTranslator {


	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.DiasQueryTranslator#translate(java.lang.String)
	 * 
	 * translates from WASDI query (OpenSearch) to OpenData format used by ONDA DIAS
	 */
	
	private static final String s_sAND =  " AND ";
	@Override
	protected String translate(String sQueryFromClient) {
		if(Utils.isNullOrEmpty(sQueryFromClient)) {
			return new String("");
		}

		String sQuery = prepareQuery(sQueryFromClient);

		String sResult = new String("");
		String sTmp = "";

		//MAYBE refactor using QueryTranslationParser

		sTmp = parseProductName(sQuery);
		if(!Utils.isNullOrEmpty(sTmp)) {
			sResult += sTmp;
		}

		sTmp = parseSentinel1(sQuery);
		if(!Utils.isNullOrEmpty(sTmp)) {
			if(!Utils.isNullOrEmpty(sResult) && !sResult.endsWith(s_sAND)) {
				sResult += s_sAND;
			}
			sResult += sTmp;
		}

		sTmp = parseSentinel2(sQuery);
		if(!Utils.isNullOrEmpty(sTmp)) {
			if(!Utils.isNullOrEmpty(sResult) && !sResult.endsWith(s_sAND)) {
				sResult += s_sAND;
			}
			sResult += sTmp;
		}

		sTmp = parseSentinel3(sQuery);
		if(!Utils.isNullOrEmpty(sTmp)) {
			if(!Utils.isNullOrEmpty(sResult) && !sResult.endsWith(s_sAND)) {
				sResult += s_sAND;
			}
			sResult += sTmp;
		}

		sTmp = parseEnvisat(sQuery);
		if(!Utils.isNullOrEmpty(sTmp)) {
			if(!Utils.isNullOrEmpty(sResult) && !sResult.endsWith(s_sAND)) {
				sResult += s_sAND;
			}
			sResult += sTmp;
		}


		sTmp = parseLandsat(sQuery);
		if(!Utils.isNullOrEmpty(sTmp)) {
			if(!Utils.isNullOrEmpty(sResult) && !sResult.endsWith(s_sAND)) {
				sResult += s_sAND;
			}
			sResult += sTmp;
		}

		sTmp = parseCopernicusMarine(sQuery);
		if(!Utils.isNullOrEmpty(sTmp)) {
			if(!Utils.isNullOrEmpty(sResult) && !sResult.endsWith(s_sAND)) {
				sResult += s_sAND;
			}
			sResult += sTmp;
		}

		if(!Utils.isNullOrEmpty(sResult)) {
			sResult = "( " + sResult + " )";
		}

		String sTimeFrame = parseTimeFrame(sQuery);
		if(!Utils.isNullOrEmpty(sTimeFrame)) {
			if(!Utils.isNullOrEmpty(sResult) && !sResult.endsWith(s_sAND)) {
				sResult += s_sAND;
			}
			sResult += "( ( " + sTimeFrame +" ) )";
		}

		sTmp = parseFootPrint(sQuery);
		if(!Utils.isNullOrEmpty(sTmp)) {
			if(!Utils.isNullOrEmpty(sResult) && !sResult.endsWith(s_sAND)) {
				sResult += s_sAND;
			}
			sResult += sTmp;
		}

		//Proba-V
		if(sQuery.toLowerCase().contains("proba-v")) {
			//ignore this case
			String stmpQuerySoFar = sResult.toLowerCase();
			if(!(stmpQuerySoFar.contains("name") || stmpQuerySoFar.contains("platformname") ||
					stmpQuerySoFar.contains("s1") || stmpQuerySoFar.contains("s2") || stmpQuerySoFar.contains("s3") ||
					stmpQuerySoFar.contains("sentinel") || stmpQuerySoFar.contains("landsat") || stmpQuerySoFar.contains("envisat") ||
					stmpQuerySoFar.contains("productmainclass") || stmpQuerySoFar.contains("copernicus") || stmpQuerySoFar.contains("marine"))) {
				WasdiLog.debugLog("QueryTranslatorONDA.translate: abort search: Proba-V is not supported by ONDA");
				//sResult += "AND (_Proba-V_are_Not_Supported_by_ONDA,_then_invalidate_query_with_this_text_to_return_zero_results_)";
				return null;
			} else {
				WasdiLog.debugLog("QueryTranslatorONDA.translate: ignoring Proba-V as not supported by ONDA");
			}

		}


		return sResult;
	}

	@Override
	protected String parseProductName(String sQuery) {
		String sFreeText = getProductName(sQuery);
		if(Utils.isNullOrEmpty(sFreeText)) {
			return null;
		}


		//remove leading asterisks ('*')
		while(sFreeText.startsWith("*")) {
			sFreeText = sFreeText.substring(1);
		}
		//remove trailing asterisks ('*')
		while(sFreeText.endsWith("*")) {
			sFreeText = sFreeText.substring(0,  sFreeText.length() - 1);
		}
		
		sFreeText = WasdiFileUtils.getFileNameWithoutExtensionsAndTrailingDots(sFreeText);
		
		//again, remove trailing asterisks ('*')
		while(sFreeText.endsWith("*")) {
			sFreeText = sFreeText.substring(0,  sFreeText.length() - 1);
		}
		
		return sFreeText;
	}

	protected String parseCopernicusMarine(String sQuery) {
		String sResult = "";
		if(sQuery.contains("Copernicus-marine")) {
			sResult += "( productMainClass:Copernicus-marine )";
		}
		return sResult;
	}

	protected String parseAdditionalQuery(String sQuery) {
		String sResult = "";
		int iStop = Math.min( Math.max(sQuery.indexOf(s_sAND),0), sQuery.length() );
		String sSubQuery = sQuery.substring(0, iStop);
		if(
				!sSubQuery.contains("Sentinel") &&
				!sSubQuery.contains("Landsat") &&
				!sSubQuery.contains("Envisat") &&
				!sSubQuery.contains("Proba") &&
				!sSubQuery.contains("Copernicus-marine") &&
				!sSubQuery.contains("footprint") &&
				!sSubQuery.contains("beginPosition") &&
				!sSubQuery.contains("endPosition")
				) {
			sResult += sSubQuery.trim();
			if(!sResult.endsWith("*")) {
				sResult += '*';
			}

			if(!Utils.isNullOrEmpty(sResult)) {
				if( !( sResult.startsWith("(") && sResult.endsWith(")") ) ){
					sResult = "( " + sResult + " )";
				}
			}
		}
		return sResult;
	}

	protected String parseLandsat(String sQuery) {
		String sResult = "";
		if(sQuery.contains("Landsat")) {
			sResult += "( platformName:Landsat-*";
			int iStart = sQuery.indexOf("Landsat");
			if(sQuery.substring(iStart).contains("L1T")) {
				sResult+=" AND name:*L1T*";
			} else if(sQuery.substring(iStart).contains("L1G")) {
				sResult+=" AND name:*L1G*";
			} else if(sQuery.substring(iStart).contains("L1GT")) {
				sResult+=" AND name:*L1GT*";
			} else if(sQuery.substring(iStart).contains("L1GS")) {
				sResult+=" AND name:*L1GS*";
			} else if(sQuery.substring(iStart).contains("L1TP")) {
				sResult+=" AND name:*L1TP*";
			}
			sResult += " )";

			if(sQuery.substring(iStart).contains("cloudcoverpercentage")) {
				//cloudcoverpercentage:[16.33 TO 95.6]
				iStart = sQuery.indexOf("cloudcoverpercentage:");
				iStart = sQuery.indexOf("[", iStart);
				int iStop = sQuery.indexOf("]", iStart);
				sResult += " AND cloudCoverPercentage:";
				sResult+=sQuery.substring(iStart, iStop+1);

			}
		}
		return sResult;
	}

	protected String parseEnvisat(String sQuery) {
		String sResult = "";
		if(sQuery.contains("Envisat")) {
			sResult +="( platformName:Envisat AND name:*";
			int iStart = sQuery.indexOf("Envisat");
			if(sQuery.substring(iStart).contains("ASA_IM__0P")) {
				sResult+="ASA_IM__0P*";
			} else if(sQuery.substring(iStart).contains("ASA_WS__0P")) {
				sResult+="ASA_WS__0P*";
			}

			if(sQuery.substring(iStart).contains("orbitDirection:")) {
				sResult+=" AND orbitDirection:";
				iStart = sQuery.indexOf("orbitDirection:");
				if(sQuery.substring(iStart).contains("ASCENDING")) {
					sResult += "ASCENDING";
				} else {
					sResult += "DESCENDING";
				}
			} 

			sResult +=" )";
		}
		return sResult;
	}

	protected String parseSentinel3(String sQuery) {
		String sResult = "";
		if(sQuery.contains("Sentinel-3")) {
			sResult += "( name:S3* AND name:*";

			int iStart = sQuery.indexOf("Sentinel-3");
			if(sQuery.substring(iStart).contains("SR_1_SRA___")) {
				sResult += "SR_1_SRA___*";
			} else if(sQuery.substring(iStart).contains("SR_1_SRA_A_")) {
				sResult += "SR_1_SRA_A_*";
			} else if(sQuery.substring(iStart).contains("SR_1_SRA_BS")) {
				sResult += "SR_1_SRA_BS*";
			} else if(sQuery.substring(iStart).contains("SR_2_LAN___")) {
				sResult += "SR_2_LAN___*";
			} else if(sQuery.substring(iStart).contains("SL_1_RBT___")) {
				sResult += "SL_1_RBT___*";
			} else if(sQuery.substring(iStart).contains("SL_2_WST___")) {
				sResult += "SL_2_WST___*";
			} else if(sQuery.substring(iStart).contains("SL_2_LST___")) {
				sResult += "SL_2_LST___*";
			} else if (sQuery.substring(iStart).contains("OL_1_EFR___")) {
				sResult += "OL_1_EFR___*";
			} else if (sQuery.substring(iStart).contains("OL_1_ERR___")) {
				sResult += "OL_1_ERR___*";
			} else if (sQuery.substring(iStart).contains("OL_2_LFR___")) {
				sResult += "OL_2_LFR___*";
			} else if (sQuery.substring(iStart).contains("OL_2_LRR___")) {
				sResult += "OL_2_LRR___*";
			} else if (sQuery.substring(iStart).contains("OL_2_WFR___")) {
				sResult += "OL_2_WFR___*";
			} else if (sQuery.substring(iStart).contains("OL_2_WRR___")) {
				sResult += "OL_2_WRR___*";
			}

			if(sQuery.substring(iStart).contains("timeliness")) {
				sResult += " AND timeliness:";
				iStart = sQuery.indexOf("timeliness",iStart);
				if(sQuery.substring(iStart).contains("Near Real Time") ){
					sResult += "NRT";
				} else if(sQuery.substring(iStart).contains("Short Time Critical")) {
					sResult += "STC";
				} else if(sQuery.substring(iStart).contains("Non Time Critical") ) {
					sResult += "NTC";
				}
			}
			sResult += " )";
		}
		return sResult;
	}


	protected String parseSentinel2(String sQuery) {
		String sSentinel2 = "";
		if(sQuery.contains("platformname:Sentinel-2")) {
			//sSentinel2 = "( name:S2* AND ";
			sSentinel2 = "( platformName:Sentinel-2 AND ";
			//platformSerialIdentifier
			if(sQuery.contains("filename:S2A_*")){
				//sSentinel2+="name:S2A_* AND ";
				sSentinel2+="platformSerialIdentifier:2A* AND ";
			} else if(sQuery.contains("filename:S2B_*")){
				//sSentinel2+="name:S2B_* AND ";
				sSentinel2+="platformSerialIdentifier:2B* AND ";
			} else {
				sSentinel2+="platformSerialIdentifier:* AND ";
			}

			if(sQuery.contains("producttype:S2MSI1C")) {
				//sSentinel2+="name:*MSI1C*";
				sSentinel2+="productType:S2MSI1C";
			} else if(sQuery.contains("producttype:S2MSI2Ap")) {
				//sSentinel2+="name:*MSIL2Ap*";
				sSentinel2+="productType:S2MSIL2Ap";
			} else if(sQuery.contains("producttype:S2MSI2A")) {
				//sSentinel2+="name:*MSIL2A*";
				sSentinel2+="productType:S2MSI2A";
			} else {
				sSentinel2+="name:*";
			}


			//cloudcoverpercentage make sure to read the query for s2 and not for landsat
			int iFrom = sQuery.indexOf("platformname:Sentinel-2");
			int iTo = sQuery.length();
			int iLand = sQuery.indexOf("platformname:Landsat");
			if(iLand > 0 ) {
				iTo = Math.min(iTo, iLand);
			}
			String sSearchSubString = sQuery.substring(iFrom, iTo);
			String sCloud = "cloudcoverpercentage";
			if(sSearchSubString.contains(sCloud)) {
				iFrom = sSearchSubString.indexOf(sCloud) + sCloud.length()+2;
				iTo = sSearchSubString.indexOf("]");
				sSearchSubString = sSearchSubString.substring(iFrom, iTo);
				String[] sInterval = sSearchSubString.split(" TO ");
				String sLow = sInterval[0];
				String sHi = sInterval[1];
				sSentinel2 +=" AND cloudCoverPercentage:[ " + sLow + " TO " + sHi + " ]";

			}

			sSentinel2+=" )";
		}

		return sSentinel2;
	}

	protected String parseSentinel1(String sQuery) {
		String sSentinel1 = "";
		if(sQuery.contains("platformname:Sentinel-1")) {
			sSentinel1 = "( name:S1* AND ";

			//filename:[S1A_*|S1B_*] (optional)
			if(sQuery.contains(" filename:S1A_* ")){
				sSentinel1 += "name:S1A_*";
			} else if(sQuery.contains(" filename:S1B_* ")) {
				sSentinel1 += "name:S1B_*";
			} else {
				sSentinel1 += "name:*";
			}
			sSentinel1+=s_sAND;
			//producttype:[SLC|GRD|OCN] (optional)
			if(sQuery.contains(" producttype:SLC ")) {
				sSentinel1+= "name:*SLC*";
			} else if( sQuery.contains(" producttype:GRD ") ) {
				sSentinel1+="name:*GRD*";
			} else if( sQuery.contains(" producttype:OCN ") ) {
				sSentinel1+="name:*OCN*";
			} else {
				sSentinel1+="name:*";
			}
			sSentinel1+=s_sAND;
			//the next token w/ wildcard is always added by ONDA
			sSentinel1+="name:*";

			//relativeorbitnumber/relativeOrbitNumber:[integer in [1-175]] (optional)
			if(sQuery.contains("relativeorbitnumber")) {
				sSentinel1+=" AND relativeOrbitNumber:";
				String sPattern = "(?<= relativeorbitnumber\\:)(\\d*.\\d*)(?= )";
				Pattern oPattern = Pattern.compile(sPattern);
				Matcher oMatcher = oPattern.matcher(sQuery);
				String sIntRelativeOrbit = "";
				if(oMatcher.find()) {
					//its a number with decimal digits
					sIntRelativeOrbit = oMatcher.group(1);
					sIntRelativeOrbit = sIntRelativeOrbit.split("\\.")[0];
					sSentinel1+= sIntRelativeOrbit;
				} else {
					//it's an integer
					sPattern = "(?<= relativeorbitnumber\\:)(\\d*)(?= )";
					oPattern = Pattern.compile(sPattern);
					oMatcher = oPattern.matcher(sQuery);
					if(oMatcher.find()) {
						sIntRelativeOrbit = oMatcher.group(1);
						sSentinel1+= sIntRelativeOrbit;
					} //don't add the query otherwise
				} 
			}
			//Sensor Mode:[SM|IW|EW|WV] (optional)
			if(sQuery.contains(" sensoroperationalmode:SM ")) {
				sSentinel1+=" AND sensorOperationalMode:SM";
			} else if(sQuery.contains(" sensoroperationalmode:IW ")) {
				sSentinel1+=" AND sensorOperationalMode:IW";
			} else if(sQuery.contains(" sensoroperationalmode:EW ")) {
				sSentinel1+=" AND sensorOperationalMode:EW";
			} else if(sQuery.contains(" sensoroperationalmode:WV ")) {
				sSentinel1+=" AND sensorOperationalMode:WV";
			}
			sSentinel1 +=" )";
		}

		return sSentinel1;
	}

	private String getNextDateTime(String sSubQuery) {
		String sDateTime = "";
		String sDateTimePattern = "(\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d\\:\\d\\d:\\d\\d\\.\\d\\d\\dZ)"; 
		Pattern oDateTimePattern = Pattern.compile(sDateTimePattern);
		Matcher oMatcher = oDateTimePattern.matcher(sSubQuery);
		if(oMatcher.find()) {
			sDateTime = oMatcher.group(1);
		}
		return sDateTime;
	}

	//time frame
	//WASDI OpenSearch format
	//( beginPosition:[2018-12-10T00:00:00.000Z TO 2018-12-17T23:59:59.999Z] AND endPosition:[2018-12-10T00:00:00.000Z TO 2018-12-17T23:59:59.999Z] )
	//ONDA format
	//( ( beginPosition:[2018-01-01T00:00:00.000Z TO 2018-12-01T23:59:59.999Z] AND endPosition:[2018-01-01T00:00:00.000Z TO 2018-12-01T23:59:59.999Z] ) )
	@Override
	protected String parseTimeFrame(String sQuery) {
		String sResult = "";
		int iStart = 0;
		if(sQuery.contains("beginPosition") ) {
			sResult += "beginPosition:[";
			//start
			iStart = sQuery.indexOf("beginPosition");
			String sDateTime = getNextDateTime(sQuery.substring(iStart));	
			sResult += sDateTime;
			sResult += " TO ";
			//end
			iStart = sQuery.indexOf(" TO ", iStart);
			sDateTime = getNextDateTime(sQuery.substring(iStart));	
			sResult += sDateTime;
			sResult += "]";
		}	
		if( sQuery.contains("endPosition")) {
			if(!Utils.isNullOrEmpty(sResult)) {
				sResult += s_sAND;
			}
			//start
			sResult += "endPosition:[";
			iStart = sQuery.indexOf("endPosition", iStart);
			String sDateTime = getNextDateTime(sQuery.substring(iStart));	
			sResult += sDateTime;
			sResult += " TO ";
			//end
			iStart = sQuery.indexOf(" TO ", iStart);
			sDateTime = getNextDateTime(sQuery.substring(iStart));	
			sResult += sDateTime;
			sResult += "]";
		}
		return sResult;
	}

	//footprint
	//WASDI OpenSearch format
	//footprint:"intersects(POLYGON((-13.535156250000002 18.97902595325528,-13.535156250000002 60.23981116999893,62.92968750000001 60.23981116999893,62.92968750000001 18.97902595325528,-13.535156250000002 18.97902595325528)))"
	//ONDA format
	//footprint:"Intersects(POLYGON((-13.535156250000002 18.97902595325528,-13.535156250000002 60.23981116999893,62.92968750000001 60.23981116999893,62.92968750000001 18.97902595325528,-13.535156250000002 18.97902595325528)))"
	@Override
	protected String  parseFootPrint(String sQuery) {
		String sFootprint = "";
		if(sQuery.contains("footprint")) {
			sFootprint += "footprint:\"Intersects(POLYGON(("; //no leading spaces
			//parse polygon
			String sPolygonLabel = "POLYGON (";
			int iStart = sQuery.indexOf(sPolygonLabel) + sPolygonLabel.length() + 1;
			Character cCurrent = sQuery.charAt(iStart);
			while(cCurrent.equals('(') || cCurrent.equals(' ')) {
				iStart++;
				cCurrent = sQuery.charAt(iStart);
			}
			int iEnd = sQuery.indexOf(" )", iStart);
			String sPolygonCoordinates = sQuery.substring(iStart, iEnd);
			sFootprint += sPolygonCoordinates;
			sFootprint+=")))\""; //no trailing spaces
		}
		return sFootprint;
	}

	@Override
	public String getCountUrl(String oQuery) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSearchUrl(PaginatedQuery oQuery) {
		// TODO Auto-generated method stub
		return null;
	}
}
