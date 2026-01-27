/**
 * Created by Cristiano Nattero on 2018-11-28
 * 
 * Fadeout software
 *
 */
package wasdi.shared.queryexecutors;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.google.common.base.Preconditions;

import wasdi.shared.utils.MissionUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryViewModel;

/**
 * Generic Query Translator Convert the WASDI query in a query supported by
 * the specific DIAS.
 * 
 * The class must parse the textual WASDI query and convert it in a equivalent query for the provider.
 * 
 * There is a generic method called parseWASDIClient Query that converts the text-query in a View Model.
 * It is suggested to use it for all the new implementations.
 * Legacy implementations still parse the text in their own methods.
 * 
 * Each derived class MUST implement:
 * 	.getCountUrl: convert the WASDI query in a valid provider query to get the count of results
 * 	.getSearchUrl: convert the WASDI query in a valid provider query to get the results
 * 
 * 
 * 
 * @author c.nattero
 *
 */
public abstract class QueryTranslator {

	/**
	 * token of offset
	 */
	private static final String s_sOFFSET = "offset=";
	/**
	 * token of limit
	 */
	private static final String s_sLIMIT = "limit=";
	/**
	 * Token of cloud coverage
	 */
	private static final String s_sCLOUDCOVERPERCENTAGE = "cloudcoverpercentage:[";
	/**
	 * Token of S1 platform
	 */
	private static final String s_sPLATFORMNAME_SENTINEL_1 = "platformname:Sentinel-1";	
	/**
	 * Token of S2 platform
	 */
	private static final String s_sPLATFORMNAME_SENTINEL_2 = "platformname:Sentinel-2";
	/**
	 * Token of S3 platform
	 */
	private static final String s_sPLATFORMNAME_SENTINEL_3 = "platformname:Sentinel-3";	
	/**
	 * Token of S5P platform
	 */
	private static final String s_sPLATFORMNAME_SENTINEL_5P = "platformname:Sentinel-5P";	
	/**
	 * Token of S5P platform
	 */
	private static final String s_sPLATFORMNAME_SENTINEL_6 = "platformname:Sentinel-6";	
	/**
	 * Token of Landsat-5 platform
	 */
	private static final String s_sPLATFORMNAME_LANDSAT_5 = "platformname:Landsat-5";
	/**
	 * Token of Landsat-7 platform
	 */
	private static final String s_sPLATFORMNAME_LANDSAT_7 = "platformname:Landsat-7";
	/**
	 * Token of Landsat platform
	 */
	private static final String s_sPLATFORMNAME_LANDSAT = "platformname:Landsat-*";
	/**
	 * Token of Prova-V platform
	 */
	private static final String s_sPLATFORMNAME_PROBAV = "platformname:Proba-V";
	/**
	 * Token of Envisat platform
	 */
	private static final String s_sPLATFORMNAME_ENVISAT = "platformname:Envisat";
	/**
	 * Token of Copernicus Marine platform
	 */
	private static final String s_sPLATFORMNAME_COPERNICUS_MARINE = "productMainClass:Copernicus-marine";
	/**
	 * Token of VIIRS platform
	 */
	private static final String s_sPLATFORMNAME_VIIRS = "platformname:VIIRS";
	/**
	 * Token of ERA5 platform
	 */
	private static final String s_sPLATFORMNAME_ERA5 = "platformname:ERA5";

	/**
	 * Token of CAMS platform
	 */
	private static final String s_sPLATFORMNAME_CAMS = "platformname:CAMS";

	/**
	 * Token of DEM platform
	 */
	private static final String s_sPLATFORMNAME_DEM = "platformname:DEM";

	/**
	 * Token of WorldCover platform
	 */
	private static final String s_sPLATFORMNAME_WORLD_COVER = "platformname:WorldCover";

	/**
	 * Token of PLANET platform
	 */
	private static final String s_sPLATFORMNAME_PLANET = "platformname:PLANET";
	
	/**
	 * Token of STATICS platform
	 */
	private static final String s_sPLATFORMNAME_STATICS = "platformname:StaticFiles";
	
	/**
	 * Token of STATIC TILES platform
	 */
	private static final String s_SPLATFORMNAME_JRC_GHSL = "platformname:StaticTiles";

	/**
	 * Token of IMERG platform
	 */
	private static final String s_sPLATFORMNAME_IMERG = "platformname:IMERG";

	/**
	 * Token of IMERG platform
	 */
	private static final String s_sPLATFORMNAME_CM = "platformname:CM";

	/**
	 * Token of ECOSTRESS platform
	 */
	private static final String s_sPLATFORMNAME_ECOSTRESS = "platformname:ECOSTRESS";

	/**
	 * Token of ERA5 platform
	 */
	private static final String S_SPLATFORMNAME_EARTHCACHE = "platformname:Earthcache";
	
	/**
	 * Token of TERRA platform
	 */
	private static final String S_SPLATFORMNAME_TERRA = "platformname:TERRA";
	
	/**
	 * Token of WSF platform
	 */
	private static final String S_SPLATFORMNAME_WSF = "platformname:WSF";
	
	/**
	 * Token of WSF platform
	 */
	private static final String S_SPLATFORMNAME_SWOT = "platformname:SWOT";

	/**
	 * Token of WSF platform
	 */
	private static final String S_SPLATFORMNAME_NASA_ALL = "platformname:NASA_ALL";
	
	/**
	 * Token of TERRA platform
	 */
	private static final String s_sPLATFORMNAME_BIGBANG = "platformname:BIGBANG";
	
	/**
	 *  Token of ERS platform
	 */
	private static final String s_sPLATFORMNAME_ERS = "platformname:ERS";
	
	/**
	 * Token of TERRA platform
	 */
	private static final String s_sPLATFORMNAME_METEOCEAN = "platformname:MeteOcean";

	/**
	 * Token of product type
	 */
	private static final String s_sPRODUCTTYPE = "producttype:";
	/**
	 * Token of product level (S5)
	 */
	private static final String s_sPRODUCTLEVEL = "productlevel:";
	/**
	 * Token of Landsat product type
	 */
	private static final String s_sLANDSATPRODUCTTYPE = "name:";
	/**
	 * Token of Proba-V Collection
	 */
	private static final String s_sPROBAVCOLLECTION = "collection:";
	/**
	 * Token of the relative orbit
	 */
	private static final String s_sRELATIVEORBITNUMBER = "relativeorbitnumber:";
	/**
	 * Token of the absolute orbit
	 */
	private static final String s_sABSOLUTEORBITNUMBER = "absoluteorbit:";
	/**
	 * Token of sensor mode
	 */
	private static final String s_sSENSORMODE = "sensoroperationalmode:";
	/**
	 * Token of timeliness
	 */
	private static final String s_s_TIMELINESS = "timeliness:";
	/**
	 * Token of Landsat product type
	 */
	private static final String s_sENVISATORBITDIRECTION= "orbitDirection:";
	
	/**
	 * Link to the Parser JSON config file
	 */
	protected String m_sParserConfigPath;
	
	/**
	 * Link to the app config JSON File
	 */
	protected String m_sAppConfigPath;
	
	/**
	 * Convert the WASDI query in an equivalent Provider query to get the total results numbers.	
	 * @param sQuery Wasdi Query
	 * @return Valid Provider query to get the total count
	 */
	public abstract String getCountUrl(String sQuery);
	
	/**
	 * Convert the WASDI query in an equivalent Provider query to get the actual results.
	 * @param oQuery Paginated query (query string + info about pagination
	 * @return Valid Provider query to get results
	 */
	public abstract String getSearchUrl(PaginatedQuery oQuery);
	
	
	/**
	 * Intermediate method that converts and encode the Query Parmeters for the provider 
	 * @param sQueryFromClient
	 * @return
	 */
	public String translateAndEncodeParams(String sQueryFromClient) {
		return encode(translate(sQueryFromClient));
	}

	/**
	 * Set Parser config file path
	 * @param sParserConfigPath
	 */
	public void setParserConfigPath(String sParserConfigPath) {
		this.m_sParserConfigPath = sParserConfigPath;
	}

	/**
	 * Set app config file path
	 * @param sAppConfigPath
	 */
	public void setAppconfigPath(String sAppConfigPath) {
		this.m_sAppConfigPath = sAppConfigPath;
	}

	/**
	 * translates from WASDI query (OpenSearch) to <derived class> format
	 * 
	 * @param sQueryFromClient WASDI Query
	 * @return Provider Query
	 */
	protected String translate(String sQueryFromClient) {
		return "";
	}
	
	protected String parseTimeFrame(String sQuery) {
		return "";
	}

	protected String parseFootPrint(String sQuery) {
		return "";
	}
	

	/**
	 * Encodes a Query
	 * 
	 * @param sDecoded
	 * @return
	 */
	protected String encode(String sDecoded) {
		String sResult = sDecoded;
		sResult = sResult.replace(" ", "%20");
		sResult = sResult.replaceAll("\"", "%22");
		return sResult;
	}

	/**
	 * Ensure that the input query has right spaces
	 * 
	 * @param sInput
	 * @return
	 */
	protected String prepareQuery(String sInput) {
		String sQuery = new String(sInput);
		// insert space before and after round brackets
		sQuery = sQuery.replaceAll("\\(", " \\( ");
		sQuery = sQuery.replaceAll("\\)", " \\) ");
		// remove space before and after square brackets
		sQuery = sQuery.replaceAll(" \\[", "\\[");
		sQuery = sQuery.replaceAll("\\[ ", "\\[");
		sQuery = sQuery.replaceAll(" \\]", "\\]");
		sQuery = sQuery.replaceAll("\\] ", "\\]");
		sQuery = sQuery.replaceAll("POLYGON", "POLYGON ");
		// remove space before and after colons
		sQuery = sQuery.replaceAll("\\: ", "\\:");
		sQuery = sQuery.replaceAll(" \\:", "\\:");

		sQuery = sQuery.replaceAll("AND", " AND ");
		sQuery = sQuery.trim().replaceAll(" +", " ");
		sQuery = convertRanges(sQuery);
		return sQuery;
	}

	protected String getProductName(String sQuery) {
		try {
			int iEnd = sQuery.length();

			int iBeginPosition = sQuery.indexOf("beginPosition");
			if(iBeginPosition >= 0) {
				iEnd = iBeginPosition;
			}

			int iEndPosition = sQuery.indexOf("endPosition");
			if(iEndPosition >= 0 && iEndPosition < iEnd) {
				iEnd = iEndPosition;
			}

			int iFootprint = sQuery.indexOf("footprint");
			if(iFootprint >= 0 && iFootprint < iEnd) {
				iEnd = iFootprint;
			}

			int iAnd = sQuery.indexOf("AND");
			if(iAnd > 0 && iAnd < iEnd) {
				iEnd = iAnd;
			}

			int iBracket = sQuery.indexOf("(");
			if(iBracket >= 0 && iBracket < iEnd) {
				iEnd = iBracket;
			}

			int iSpace = sQuery.indexOf(" ");
			if(iSpace > 0 && iSpace < iEnd) {
				iEnd = iSpace;
			}

			if(iEnd == 0) {
				return "";
			}

			sQuery = sQuery.substring(0, iEnd);

			//remove leading and trailing spaces
			sQuery = sQuery.trim();

			return sQuery;

		} catch (Exception oE) {
			WasdiLog.debugLog("QueryTranslator.getProductName( " + sQuery + " ): " + oE);
		}
		return "";
	}

	protected String parseProductName(String sQuery) {
		return getProductName(sQuery);
	}

	protected String convertRanges(String sQuery) {
		return sQuery;
	}
	
	/**
	 * Convert the textual WASDI client Query in a View Mode
	 * 
	 * @param sQuery
	 * @return
	 */
	public QueryViewModel parseWasdiClientQuery(String sQuery) {

		QueryViewModel oResult = new QueryViewModel();

		try {
			
			String sOriginalQuery = sQuery;
			
			// Prepare the text that is uniform
			sQuery = prepareQuery(sQuery);

			// Try to get offset
			int iOffset = -1;
			try {
				iOffset = readInt(sQuery, QueryTranslator.s_sOFFSET);
			} catch (Exception oE) {
				WasdiLog.debugLog("QueryTranslator.parseWasdiClientQuery( " + sQuery + " ): could not parse offset: " + oE);
			}

			oResult.offset = iOffset;

			// Try to get limit
			int iLimit = 10;
			try {
				iLimit = readInt(sQuery, QueryTranslator.s_sLIMIT);
			} catch (Exception oE) {
				WasdiLog.debugLog( "QueryTranslator.parseWasdiClientQuery( " + sQuery + " ): could not parse limit: " + oE);
			}

			oResult.limit = iLimit;

			// Try to get the product name
			String sFreeText = getProductName(sQuery);
			if (!Utils.isNullOrEmpty(sFreeText)) {
				oResult.productName = sFreeText;
			}

			// Try to get footprint

			try {
				if (sQuery.contains("footprint")) {
					String sIntro = "( footprint:\"intersects ( POLYGON ( ( ";
					int iStart = sQuery.indexOf(sIntro);
					if (iStart >= 0) {
						iStart += sIntro.length();
					}
					int iEnd = sQuery.indexOf(')', iStart);
					if (0 > iEnd) {
						iEnd = sQuery.length();
					}
					Double dNorth = Double.NEGATIVE_INFINITY;
					Double dSouth = Double.POSITIVE_INFINITY;
					Double dEast = Double.NEGATIVE_INFINITY;
					Double dWest = Double.POSITIVE_INFINITY;
					try {
						String[] asCouples = sQuery.substring(iStart, iEnd).trim().split(",");

						for (String sPair : asCouples) {
							try {
								String[] asTwoCoord = sPair.split(" ");
								Double dParallel = Double.parseDouble(asTwoCoord[1]);
								dNorth = Double.max(dNorth, dParallel);
								dSouth = Double.min(dSouth, dParallel);

								Double dMeridian = Double.parseDouble(asTwoCoord[0]);
								dEast = Double.max(dEast, dMeridian);
								dWest = Double.min(dWest, dMeridian);
							} catch (Exception oE) {
								WasdiLog.errorLog("QueryTranslator.parseWasdiClientQuery: issue with current coordinate pair: " + sPair + ": ", oE);
							}
						}
						// todo check coordinates are within bounds
						if (-90 <= dNorth && 90 >= dNorth && -90 <= dSouth && 90 >= dSouth && -180 <= dEast
								&& 180 >= dEast && -180 <= dWest && 180 >= dWest) {

							oResult.north = dNorth;
							oResult.south = dSouth;
							oResult.east = dEast;
							oResult.west = dWest;
						} else {
							oResult.north = null;
							oResult.south = null;
							oResult.east = null;
							oResult.west = null;
						}

					} catch (Exception oE) {
						WasdiLog.errorLog("QueryTranslator.parseWasdiClientQuery: could not complete footprint detection: ", oE);
					}
				}
				
			} catch (Exception oE) {
				WasdiLog.errorLog("QueryTranslator.parseWasdiClientQuery: could not identify footprint substring limits: ", oE);
			}
			
			try {
				//productType
				if(sQuery.contains("producttype")) {
					int iStart = sQuery.indexOf(":", sQuery.indexOf("producttype")) + 1;
				
					int iEnd = sQuery.length();
					int iTemp = sQuery.indexOf(" ", iStart);
					if(iTemp > 0 && iTemp < iEnd) {
						iEnd = iTemp;
					}
					iTemp = sQuery.indexOf(")", iStart);
					if(iTemp > 0 && iTemp < iEnd) {
						iEnd = iTemp;
					}
					oResult.productType = sQuery.substring(iStart, iEnd);
				}
				
			}catch (Exception oE) {
				WasdiLog.debugLog("QueryTranslator.parseWasdiClientQuery: product type: " + oE);
			}
			
			try {
				//productLevel
				if(sQuery.contains("productlevel")) {
					int iStart = sQuery.indexOf(":", sQuery.indexOf("productlevel")) + 1;
				
					int iEnd = sQuery.length();
					int iTemp = sQuery.indexOf(" ", iStart);
					if(iTemp > 0 && iTemp < iEnd) {
						iEnd = iTemp;
					}
					iTemp = sQuery.indexOf(")", iStart);
					if(iTemp > 0 && iTemp < iEnd) {
						iEnd = iTemp;
					}
					oResult.productLevel = sQuery.substring(iStart, iEnd);
				}
				
			}catch (Exception oE) {
				WasdiLog.debugLog("QueryTranslator.parseWasdiClientQuery: product type: " + oE);
			}			

			// Try to get time filters
			String[] asInterval = { null, null };

			// beginPosition:[2020-01-30T00:00:00.000Z TO 2020-02-06T23:59:59.999Z]
			String sKeyword = "beginPosition";
			parseInterval(sQuery, sKeyword, asInterval);
			oResult.startFromDate = asInterval[0];
			oResult.startToDate = asInterval[1];

			// endPosition:[2020-01-30T00:00:00.000Z TO 2020-02-06T23:59:59.999Z]
			sKeyword = "endPosition";
			parseInterval(sQuery, sKeyword, asInterval);
			oResult.endFromDate = asInterval[0];
			oResult.endToDate = asInterval[1];
			
			boolean bFound = false;

			// Try to get info about S1
			bFound = parseSentinel1(sQuery, oResult);

			if (!bFound) {
				// Try to get Info About S2
				bFound = parseSentinel2(sQuery, oResult);				
			}

			if (!bFound) {
				// Try get Info about VIIRS
				parseVIIRS(sQuery, oResult);				
			}
			
			if (!bFound) {
				// Try get Info about ERA5
				parseERA5(sQuery, oResult);				
			}

			if (!bFound) {
				// Try get Info about CAMS
				parseCAMS(sQuery, oResult);				
			}
			
			if (!bFound) {
				// Try to get info about Landsat-5 or Landsat-7
				parseLandsat5And7(sQuery, oResult);				
			}

			if (!bFound) {
				// Try to get info about Landsat-8
				parseLandsat(sQuery, oResult);				
			}
			
			if (!bFound) {
				// Try to get Info about ProbaV
				parseProbaV(sQuery, oResult);				
			}
			
			if (!bFound) {
				// Try to get Info about Sentinel 3
				parseSentinel3(sQuery, oResult);				
			}
			
			if (!bFound) {
				// Try to get info about Envisat
				parseEnvisat(sQuery, oResult);				
			}
			
			if (!bFound) {
				// Try to get info about Copernicus Marine
				parseCopernicusMarine(sQuery, oResult);				
			}
			
			
			if (!bFound) {
				// Try to get Info about Sentinel 5P				
				bFound = parseSentinel5P(sQuery, oResult);
			}			
			
			if (!bFound) {
				bFound = parseSentinel6(sQuery, oResult); 
			}			
			
			
			if (!bFound) {
				bFound = parsePlanet(sQuery, oResult); 
			}			
			
			if (!bFound) {
				// Try get Info about DEM				
				bFound =parseDEM(sQuery, oResult); 
			}			

			if (!bFound) {
				// Try get Info about WorldCover				
				bFound = parseWorldCover(sQuery, oResult); 
			}
			
			if (!bFound) {
				// Try get Info about Statics: needs the original query to avoid
				// Conflits with the "AND" literal				
				bFound = parseStatics(sOriginalQuery, oResult); 
			}			

			if (!bFound) {
				// Try get Info about IMERG				
				bFound = parseIMERG(sQuery, oResult); 
			}			

			if (!bFound) {
				// Try get Info about CM				
				bFound = parseCM(sQuery, oResult); 
			}			

			if (!bFound) {
				// Try get Info about ECOSTRESS				
				bFound = parseECOSTRESS(sQuery, oResult); 
			}			
			
			if (!bFound) {
				// Try get Info about Earthcache				
				bFound = parseEarthcache(sQuery, oResult); 
			}			
			
			if (!bFound) {
				// Try to get info about Terra				
				bFound = parseTerra(sQuery, oResult); 
			}		
			
			if (!bFound) {
				// Try to get the info for semi-static provided files				
				bFound = parseStaticTiles(sQuery, oResult); 
			}			

			if (!bFound) {
				bFound = parseWFS(sQuery, oResult); 
			}	
			
			if (!bFound) {
				bFound = parseSWOT(sQuery, oResult);
			}
						
			if (!bFound) {
				bFound = parseBIGBANG(sQuery, oResult); 
			}			
			
			if (!bFound) {
				bFound = parseERS(sQuery, oResult); 
			}			
			
			if (!bFound) {
				bFound = parseMeteOcean(sQuery, oResult); 
			}
						
			if (Utils.isNullOrEmpty(oResult.platformName)) {
				WasdiLog.debugLog("QueryTranslator.parseWasdiClientQuery: platformName not found: try to read the generic one");
				
				int iStartIndex = sQuery.indexOf("platformname");
				
				if (iStartIndex>=0) {
					int iEndIndex = sQuery.substring(iStartIndex).indexOf("AND");
					
					if (iEndIndex<0) iEndIndex = sQuery.substring(iStartIndex).indexOf(")");
					
					if (iEndIndex>=0) {
						int iStartIndex2 = iStartIndex + "platformname".length() + 1;
						String sPlatform = sQuery.substring(iStartIndex2, iStartIndex+iEndIndex);
						sPlatform = sPlatform.trim();
						oResult.platformName = sPlatform;
						WasdiLog.debugLog("QueryTranslator.parseWasdiClientQuery: found platformName: " + sPlatform);
					}
				}
			}
			
			if ( Utils.isNullOrEmpty(oResult.platformName) && !Utils.isNullOrEmpty(oResult.productName) ) {
				WasdiLog.debugLog("QueryTranslator.parseWasdiClientQuery: platformName still not found: try to get it from the file name");
				oResult.platformName = MissionUtils.getPlatformFromSatelliteImageFileName(oResult.productName);
				
				if (!Utils.isNullOrEmpty(oResult.platformName))  {
					WasdiLog.debugLog("QueryTranslator.parseWasdiClientQuery: found platformName: " + oResult.platformName);
				}
			}
			
			fillFilters(sOriginalQuery, oResult);
			
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryTranslator.parseWasdiClientQuery: exception " + oEx.toString());
			String sStack = ExceptionUtils.getStackTrace(oEx);
			WasdiLog.errorLog("QueryTranslator.parseWasdiClientQuery: stack " + sStack);
		}

		return oResult;
	}
	
	private void fillFilters(String sQuery, QueryViewModel oQueryViewModel) {
		
		try {
			
			if (Utils.isNullOrEmpty(sQuery)) return;
			
			sQuery = sQuery.replace("(", "");
			sQuery = sQuery.replace(")", "");
			
			String [] asQueryParts = sQuery.split("AND");
			
			for (String sFilter : asQueryParts) {
				try {
					 sFilter =  sFilter.strip();
					 String [] asNameValue = sFilter.split(":");
					 if (asNameValue.length>1) {
						 oQueryViewModel.filters.put(asNameValue[0].strip(), asNameValue[1].strip());
					 }
				}
				catch (Exception oEx) {
					WasdiLog.errorLog("QueryTranslator.fillFilters: inner exception " + oEx.toString());
				}						
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("QueryTranslator.fillFilters: exception " + oEx.toString());
		}		
		
	}
	
	

	/**
	 * Remove the platform token from the query.
	 * 
	 * @param sQuery the query
	 * @param sPlatformToken the platform token
	 * @return
	 */
	private static String removePlatformToken(String sQuery, String sPlatformToken) {
		int iStart = sQuery.indexOf(sPlatformToken);

		if (iStart >= 0) {
			iStart += sPlatformToken.length();
			int iEnd = sQuery.indexOf(')', iStart);
			if (iEnd < 0) {
				sQuery = sQuery.substring(iStart);
			} else {
				sQuery = sQuery.substring(iStart, iEnd);
			}
			sQuery = sQuery.trim();
		}

		return sQuery;
	}

	/**
	 * Fills the Query View Model with S1 info
	 * 
	 * @param sQuery
	 * @param oResult
	 */
	private boolean parseSentinel1(String sQuery, QueryViewModel oResult) {
		try {
			if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_SENTINEL_1)) {
				
				sQuery = removePlatformToken(sQuery, s_sPLATFORMNAME_SENTINEL_1);

				oResult.platformName = Platforms.SENTINEL1;

				// check for product type
				try {
					if (sQuery.contains(QueryTranslator.s_sPRODUCTTYPE)) {
						int iStart = sQuery.indexOf(s_sPRODUCTTYPE);
						if (iStart < 0) {
							throw new IllegalArgumentException("Could not find product type");
						}
						iStart += s_sPRODUCTTYPE.length();
						int iEnd = sQuery.indexOf(" AND ", iStart);
						if (iEnd < 0) {
							iEnd = sQuery.indexOf(')', iStart);
						}
						if (iEnd < 0) {
							iEnd = sQuery.indexOf(' ', iStart);
						}
						if (iEnd < 0) {
							// the types can be OCN, GRD, SLC, all of three letters
							iEnd = iStart + 3;
						}
						String sType = sQuery.substring(iStart, iEnd);
						sType = sType.trim();

						oResult.productType = sType;
					}
				} catch (Exception oE) {
					WasdiLog.debugLog("QueryTranslator.parseWasdiClientQuery( " + sQuery + " ): error while parsing product type: " + oE);
				}

				// check for sensor mode
				try {
					if (sQuery.contains(QueryTranslator.s_sSENSORMODE)) {
						int iStart = sQuery.indexOf(s_sSENSORMODE);
						if (iStart < 0) {
							throw new IllegalArgumentException("Could not find sensor mode");
						}
						iStart += s_sSENSORMODE.length();
						int iEnd = sQuery.indexOf(" AND ", iStart);
						if (iEnd < 0) {
							iEnd = sQuery.indexOf(')', iStart);
						}
						if (iEnd < 0) {
							iEnd = sQuery.indexOf(' ', iStart);
						}
						if (iEnd < 0) {
							// the types can be SM, IW, EW, WV all of two letters
							iEnd = iStart + 2;
						}
						String sMode = sQuery.substring(iStart, iEnd);
						sMode = sMode.trim();

						oResult.sensorMode = sMode;
					}
				} catch (Exception oE) {
					WasdiLog.debugLog("QueryTranslator.parseWasdiClientQuery( " + sQuery
							+ " ): error while parsing product type: " + oE);
				}

				// check for relative orbit
				if (sQuery.contains(QueryTranslator.s_sRELATIVEORBITNUMBER)) {
					try {
						int iStart = sQuery.indexOf(s_sRELATIVEORBITNUMBER);
						if (iStart < 0) {
							throw new IllegalArgumentException("Could not find relative orbit number");
						}
						iStart += s_sRELATIVEORBITNUMBER.length();
						int iEnd = sQuery.indexOf(" AND ", iStart);
						if (iEnd < 0) {
							iEnd = sQuery.indexOf(')');
						}
						if (iEnd < 0) {
							iEnd = sQuery.indexOf(' ', iStart);
						}
						if (iEnd < 0) {
							// if anything else failed, skip digits
							iEnd = iStart;
							while (iEnd < sQuery.length() && Character.isDigit(sQuery.charAt(iEnd))) {
								iEnd++;
							}
						}
						String sOrbit = sQuery.substring(iStart, iEnd);
						int iOrbit = Integer.parseInt(sOrbit);

						oResult.relativeOrbit = iOrbit;

					} catch (Exception oE) {
						WasdiLog.debugLog("QueryTranslator.parseWasdiClientQuery(" + sQuery
								+ " ): error while parsing relative orbit: " + oE);
					}
				}				
				
				return true;
			}
		} 
		catch (Exception oE) {
			WasdiLog.debugLog("QueryTranslator.parseWasdiClientQuery( " + sQuery + " ): " + oE);
		}
		return false;
	}

	/**
	 * Fill the Query View Model with the S2 values
	 * 
	 * @param sQuery
	 * @param oResult
	 */
	private boolean parseSentinel2(String sQuery, QueryViewModel oResult) {

		try {
			if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_SENTINEL_2)) {
				sQuery = removePlatformToken(sQuery, s_sPLATFORMNAME_SENTINEL_2);

				oResult.platformName = Platforms.SENTINEL2;

				// check for cloud coverage
				parseCloudCoverage(sQuery, oResult);
				
				return true;
			}
		} 
		catch (Exception oE) {
			WasdiLog.debugLog("QueryTranslator.parseSentinel_2( " + sQuery + " ): " + oE);
		}
		
		return false;
	}
	
	/**
	 * Parse Cloud Coverage
	 * @param sQuery
	 * @param oResult
	 */
	private void parseCloudCoverage(String sQuery, QueryViewModel oResult) {
		
		// check for cloud coverage
		try {
			if (sQuery.toLowerCase().contains(QueryTranslator.s_sCLOUDCOVERPERCENTAGE)) {
				int iStart = sQuery.toLowerCase().indexOf(s_sCLOUDCOVERPERCENTAGE);

				if (iStart >= 0) {

					iStart += s_sCLOUDCOVERPERCENTAGE.length();
					int iEnd = sQuery.indexOf(']', iStart);
					String sSubQuery = sQuery.substring(iStart, iEnd);

					String[] asCloudLimits = sSubQuery.split(" TO ");
					
					if (asCloudLimits.length == 1) {
						asCloudLimits = sSubQuery.split("<");
					}

					// these variables could be omitted, but in this way we check we are reading
					// numbers
					double dLo = Double.parseDouble(asCloudLimits[0]);
					double dUp = Double.parseDouble(asCloudLimits[1]);

					oResult.cloudCoverageFrom = dLo;
					oResult.cloudCoverageTo = dUp;
				}
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("QueryTranslator.parseCloudCoverage( " + sQuery + " ): could not parse cloud coverage: " + oE);
		}
		
	}
	
	
	/**
	 * Fills the Query View Model with S1 info
	 * 
	 * @param sQuery
	 * @param oResult
	 */
	private boolean parseVIIRS(String sQuery, QueryViewModel oResult) {
		try {
			if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_VIIRS)) {
				// sQuery = removePlatformToken(sQuery, s_sPLATFORMNAME_VIIRS);

				oResult.platformName = Platforms.VIIRS;

				// check for product type
				try {
					if (sQuery.contains(QueryTranslator.s_sPRODUCTTYPE)) {
						String sProductType = extractValue(sQuery, "producttype");
						if (sProductType.endsWith(" )")) {
							sProductType = sProductType.substring(0, sProductType.length() - 1).trim();
						}
						oResult.productType = sProductType;
					}
				} catch (Exception oE) {
					WasdiLog.warnLog("QueryTranslator.parseVIIRS( " + sQuery + " ): error while parsing product type: " + oE);
				}
				
				return true;
			}
		} 
		catch (Exception oE) {
			WasdiLog.warnLog("QueryTranslator.parseWasdiClientQuery( " + sQuery + " ): " + oE);
		}
		return false;
	}

	/**
	 * Fills the Query View Model with ERA5 info
	 * 
	 * @param sQuery the query
	 * @param oResult the resulting Query View Model
	 */
	private boolean parseERA5(String sQuery, QueryViewModel oResult) {
		if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_ERA5)) {
			sQuery = removePlatformToken(sQuery, s_sPLATFORMNAME_ERA5);

			oResult.platformName = Platforms.ERA5;

			oResult.productName = extractValue(sQuery, "dataset");
			oResult.productType = extractValue(sQuery, "productType");
			oResult.productLevel = extractValue(sQuery, "pressureLevels");
			oResult.sensorMode = extractValue(sQuery, "variables");
			oResult.timeliness = extractValue(sQuery, "format");
			oResult.startToDate = extractValue(sQuery, "aggregation");
			oResult.instrument = extractValue(sQuery, "version");
			return true;
		}
		
		return false;
	}

	/**
	 * Fills the Query View Model with CAMS info
	 * 
	 * @param sQuery the query
	 * @param oResult the resulting Query View Model
	 */
	private boolean parseCAMS(String sQuery, QueryViewModel oResult) {
		if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_CAMS)) {
			sQuery = removePlatformToken(sQuery, s_sPLATFORMNAME_CAMS);

			oResult.platformName = Platforms.CAMS;

			oResult.productName = extractValue(sQuery, "dataset");
			oResult.productType = extractValue(sQuery, "type");
			oResult.sensorMode = extractValue(sQuery, "variables");
			oResult.timeliness = extractValue(sQuery, "format");
			
			return true;
		}
		
		return false;
	}

	/**
	 * Fills the Query View Model with Earthcache info
	 * 
	 * @param sQuery the query
	 * @param oResult the resulting Query View Model
	 */
	private boolean parseEarthcache(String sQuery, QueryViewModel oResult) {
		if (sQuery.contains(QueryTranslator.S_SPLATFORMNAME_EARTHCACHE)) {
			sQuery = removePlatformToken(sQuery, S_SPLATFORMNAME_EARTHCACHE);

			oResult.platformName = Platforms.EARTHCACHE;

			oResult.productType = extractValue(sQuery, "resolution");

			if (sQuery.contains("coverage")) {
				String sCoverage = extractValue(sQuery, "coverage");
				try {
					double dCoverage = Double.parseDouble(sCoverage);
					oResult.cloudCoverageFrom = dCoverage;
					oResult.cloudCoverageTo = dCoverage;
				} catch (Exception oE) {
					WasdiLog.debugLog("QueryTranslator.parseEarthcache( " + sQuery  + " ): error while parsing coverage: " + sCoverage);
				}
			}
			
			return true;
		}
		
		return false;
	}

	/**
	 * Fills the Query View Model with DEM info
	 * 
	 * @param sQuery the query
	 * @param oResult the resulting Query View Model
	 */
	private boolean parseDEM(String sQuery, QueryViewModel oResult) {
		
		if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_DEM)) {
			sQuery = removePlatformToken(sQuery, s_sPLATFORMNAME_DEM);

			oResult.platformName = Platforms.DEM;

			oResult.productType = extractValue(sQuery, "dataset");
			
			return true;
		}
		
		return false;
	}

	/**
	 * Fills the Query View Model with ECOSTRESS info
	 * 
	 * @param sQuery the query
	 * @param oResult the resulting Query View Model
	 */
	private boolean parseECOSTRESS(String sQuery, QueryViewModel oResult) {
		if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_ECOSTRESS)) {
			sQuery = removePlatformToken(sQuery, s_sPLATFORMNAME_ECOSTRESS);

			oResult.platformName = Platforms.ECOSTRESS;

			oResult.productType = extractValue(sQuery, "dataset");

			oResult.timeliness = extractValue(sQuery, "dayNightFlag");
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Fills the Query View Model with TERRA info
	 * 
	 * @param sQuery the query
	 * @param oResult the resulting Query View Model
	 */
	private boolean parseTerra(String sQuery, QueryViewModel oResult) {
		if (sQuery.contains(QueryTranslator.S_SPLATFORMNAME_TERRA)) {
			sQuery = removePlatformToken(sQuery, QueryTranslator.S_SPLATFORMNAME_TERRA);

			oResult.platformName = Platforms.TERRA;
			
			oResult.productType = extractValue(sQuery, "producttype");
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Fills the Query View Model with WSF (World Settlement Footprint) info
	 * 
	 * @param sQuery the query
	 * @param oResult the resulting Query View Model
	 */
	private boolean parseWFS(String sQuery, QueryViewModel oResult) {
		if (sQuery.contains(QueryTranslator.S_SPLATFORMNAME_WSF)) {
			sQuery = removePlatformToken(sQuery, QueryTranslator.S_SPLATFORMNAME_WSF);

			oResult.platformName = Platforms.WSF;
			
			return true;
		}
		
		return false;
	}
	
	
	/**
	 * Fills the Query View Model with parse SWOT info
	 * 
	 * @param sQuery the query
	 * @param oResult the resulting Query View Model
	 */
	private boolean parseSWOT(String sQuery, QueryViewModel oResult) {
		if (sQuery.contains(QueryTranslator.S_SPLATFORMNAME_SWOT)) {
			sQuery = removePlatformToken(sQuery, QueryTranslator.S_SPLATFORMNAME_SWOT);

			oResult.platformName = Platforms.SWOT;
			
			oResult.productLevel = extractValue(sQuery, "productlevel");
			
			oResult.polarisation = extractValue(sQuery, "variables"); // data rate
			
			oResult.productType = extractValue(sQuery, "resolution");
			
			return true;
		}
		
		return false;
	}
	
	
	/**
	 * Fills the Query View Model with STATIC TILES info
	 * 
	 * @param sQuery the query
	 * @param oResult the resulting Query View Model
	 */
	private boolean parseStaticTiles(String sQuery, QueryViewModel oResult) {
		if (sQuery.contains(QueryTranslator.s_SPLATFORMNAME_JRC_GHSL)) {
			sQuery = removePlatformToken(sQuery, QueryTranslator.s_SPLATFORMNAME_JRC_GHSL);

			oResult.platformName = Platforms.JRC_GHSL;
			
			return true;
		}
		
		return false;
	}

	/**
	 * Fills the Query View Model with WorldCover info
	 * 
	 * @param sQuery the query
	 * @param oResult the resulting Query View Model
	 */
	private boolean parseWorldCover(String sQuery, QueryViewModel oResult) {
		if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_WORLD_COVER)) {
			sQuery = removePlatformToken(sQuery, s_sPLATFORMNAME_WORLD_COVER);

			oResult.platformName = Platforms.WORLD_COVER;

			oResult.productType = extractValue(sQuery, "dataset");
			
			return true;
		}
		return false;
	}
	
	/**
	 * Fills the Query View Model with Statics Info
	 * @param sQuery
	 * @param oResult
	 */
	private boolean parseStatics(String sQuery, QueryViewModel oResult) {
		if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_STATICS)) {
			sQuery = removePlatformToken(sQuery, s_sPLATFORMNAME_STATICS);

			oResult.platformName = Platforms.STATICS;
			oResult.productType = extractValue(sQuery, "producttype");
			
			return true;
		}
		
		return false;
	}

	/**
	 * Fills the Query View Model with IMERG info
	 * 
	 * @param sQuery the query
	 * @param oResult the resulting Query View Model
	 */
	private boolean parseIMERG(String sQuery, QueryViewModel oResult) {
		//WasdiLog.debugLog("QueryTranslator.parseIMERG | sQuery: " + sQuery);

		if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_IMERG)) {
			sQuery = removePlatformToken(sQuery, s_sPLATFORMNAME_IMERG);

			oResult.platformName = Platforms.IMERG;

			oResult.productName = extractValue(sQuery, "latency");
			oResult.productType = extractValue(sQuery, "duration");
			oResult.productLevel = extractValue(sQuery, "accumulation");
			
			return true;
		}
		
		return false;
	}

	/**
	 * Fills the Query View Model with CM info
	 * 
	 * @param sQuery the query
	 * @param oResult the resulting Query View Model
	 */
	private boolean parseCM(String sQuery, QueryViewModel oResult) {

		if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_CM)) {
			sQuery = removePlatformToken(sQuery, s_sPLATFORMNAME_CM);

			oResult.platformName = Platforms.CM;

			oResult.productType= extractValue(sQuery, "producttype");
			oResult.productLevel = extractValue(sQuery, "protocol");
			oResult.productName = extractValue(sQuery, "dataset");
			oResult.sensorMode = extractValue(sQuery, "variables");

			if (sQuery.contains("startDepth")) {
				String sStartDepth = extractValue(sQuery, "startDepth");
				try {
					oResult.cloudCoverageFrom = Double.parseDouble(sStartDepth);
				} catch (Exception oE) {
					WasdiLog.debugLog("QueryTranslator.parseCM( " + sQuery  + " ): error while parsing startDepth: " + sStartDepth);
				}
			}

			if (sQuery.contains("endDepth")) {
				String sEndDepth = extractValue(sQuery, "endDepth");
				try {
					oResult.cloudCoverageTo = Double.parseDouble(sEndDepth);
				} catch (Exception oE) {
					WasdiLog.debugLog("QueryTranslator.parseCM( " + sQuery  + " ): error while parsing endDepth: " + sEndDepth);
				}
			}
			
			return true;
		}
		
		return false;
	}
	
	
	private boolean parseBIGBANG(String sQuery, QueryViewModel oResult) {
		if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_BIGBANG)) {
			sQuery = removePlatformToken(sQuery, s_sPLATFORMNAME_BIGBANG);
			
			oResult.platformName = Platforms.BIGBANG;
			oResult.sensorMode = extractValue(sQuery, "dataset");
			
			return true;
		}
		
		return false;
	}
	
	private boolean parseERS(String sQuery, QueryViewModel oResult) {
		if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_ERS)) {
			sQuery = removePlatformToken(sQuery, s_sPLATFORMNAME_ERS);
			
			oResult.platformName = Platforms.ERS;
			
			return true;
		}
		
		return false;
	}
	
	private boolean parseMeteOcean(String sQuery, QueryViewModel oResult) {
		if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_METEOCEAN)) {
			sQuery = removePlatformToken(sQuery, s_sPLATFORMNAME_METEOCEAN);
			
			oResult.platformName = Platforms.METEOCEAN;
			oResult.productLevel = extractValue(sQuery, "productlevel");
			oResult.sensorMode = extractValue(sQuery, "sensorMode");
			oResult.instrument = extractValue(sQuery, "Instrument");
			if (!Utils.isNullOrEmpty(oResult.productLevel) && oResult.productLevel.equals("hs")) {
				oResult.timeliness = extractValue(sQuery, "timeliness");
			}
			if (!Utils.isNullOrEmpty(oResult.productType) 
					&& (oResult.productType.equals("rcp85_mid") || oResult.productType.equals("rcp85_end") || oResult.productType.equals("historical"))) {
				oResult.polarisation = extractValue(sQuery, "polarisationmode");
			}
			
			// more complex regex, not handled well from the extractValue method
			String sRegex = "month:(1[0-2]|[1-9])";
			Pattern oPattern = Pattern.compile(sRegex);
			Matcher oMatcher = oPattern.matcher(sQuery);
			
			if (oMatcher.find()) {
				String sMonth = oMatcher.group().split(":")[1];
				oResult.startFromDate = sMonth;
			} else {
				oResult.startFromDate = null;
			}
			
			sRegex = "season:(DJF|JJA|MAM|SON)";
			oPattern = Pattern.compile(sRegex);
			oMatcher = oPattern.matcher(sQuery);
			
			if (oMatcher.find()) {
				String sSeason = oMatcher.group(1);;
				oResult.endFromDate = sSeason;
			} else {
				oResult.endFromDate = null;
			}
			
			sRegex = "quantile:(0\\.99|0\\.95|0\\.9|0\\.5|0\\.1)";
			oPattern = Pattern.compile(sRegex);
			oMatcher = oPattern.matcher(sQuery);
			
			if (oMatcher.find()) {
				String sQuantile = oMatcher.group(1);;
				oResult.platformSerialIdentifier = sQuantile;
			} else {
				oResult.platformName = null;
			}
			
			return true;
		}
		
		return false;
	}

	/**
	 * Extract the value corresponding to the key from the simplifiedquery.
	 * <br><br>
	 * For example, using the query <i>AND dataset:reanalysis-era5-pressure-levels AND productType:Reanalysis AND pressureLevels:1 hPa AND variables:U V AND format:netcdf</i>
	 * and the key <i>dataset</i>, it should return <i>reanalysis-era5-pressure-levels</i>.
	 * 
	 * @param sQuery the simplified query
	 * @param sKey the key
	 * @return the corresponding value
	 */
	protected static String extractValue(String sQuery, String sKey) {
		int iStart = -1;
		int iEnd = -1;

		try {
			if (sQuery.contains(sKey)) {
				iStart = sQuery.indexOf(sKey);

				iStart += (sKey.length() + 1);
				iEnd = sQuery.indexOf(" AND ", iStart);

				if (iEnd < 0) {
					
					iEnd = sQuery.indexOf(" )", iStart);
					
					if (iEnd < 0) {
						iEnd = sQuery.length();
					}
				}

				String sType = sQuery.substring(iStart, iEnd);
				sType = sType.trim();

				return sType;
			}			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("QueryTranslator.extractValue: error ", oEx);
		}

		return null;
	}
	
	/**
	 * Parse Landsat-5 and Landsat-7 filters
	 * @param sQuery
	 * @param oResult
	 */
	private boolean parseLandsat5And7(String sQuery, QueryViewModel oResult) {
		try {
			boolean bIsLandsatProduct = false;
			
			if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_LANDSAT_5)) {
				sQuery = removePlatformToken(sQuery, s_sPLATFORMNAME_LANDSAT_5);
				oResult.platformName = Platforms.LANDSAT5;
				bIsLandsatProduct = true;
			} else if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_LANDSAT_7)) {
				sQuery = removePlatformToken(sQuery, s_sPLATFORMNAME_LANDSAT_7);
				oResult.platformName = Platforms.LANDSAT7;
				bIsLandsatProduct = true;
			}
			
			if (bIsLandsatProduct) {
				oResult.productType = extractValue(sQuery, "producttype");
				oResult.sensorMode = extractValue(sQuery, "sensoroperationalmode");
				
				try {
					String sPathNumber = extractValue(sQuery, "relativeorbitnumber");
					if (!Utils.isNullOrEmpty(sPathNumber))
						oResult.relativeOrbit = Integer.parseInt(sPathNumber);
					
					String sRowNumber = extractValue(sQuery, "absoluteorbit");
					if (!Utils.isNullOrEmpty(sRowNumber)) {
						oResult.absoluteOrbit = Integer.parseInt(sRowNumber);
					}
				} catch (NumberFormatException oEx) {
					WasdiLog.errorLog("QueryTranslator.parseLandsat5And7: error parsing filters with integer value " + sQuery, oEx);
				}	
				
				return true;
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("QueryTranslator.parseLandsat5And7 ( " + sQuery + " ): ", oE);
		}
		
		return false;
	}
	
	/**
	 * Parse Landsat-8 filters
	 * @param sQuery
	 * @param oResult
	 */
	private boolean parseLandsat(String sQuery, QueryViewModel oResult) {
		try {
			if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_LANDSAT)) {
				sQuery = removePlatformToken(sQuery, s_sPLATFORMNAME_LANDSAT);

				oResult.platformName = Platforms.LANDSAT8;

				// check for product type
				try {
					if (sQuery.contains(s_sLANDSATPRODUCTTYPE)) {
						int iStart = sQuery.indexOf(s_sLANDSATPRODUCTTYPE);
						if (iStart < 0) {
							throw new IllegalArgumentException("Could not find product type");
						}
						iStart += s_sLANDSATPRODUCTTYPE.length();
						int iEnd = sQuery.indexOf(" AND ", iStart);
						if (iEnd < 0) {
							iEnd = sQuery.indexOf(')', iStart);
						}
						if (iEnd < 0) {
							iEnd = sQuery.indexOf(' ', iStart);
						}
						if (iEnd < 0) {
							// the types can be of three or four letters
							iEnd = iStart + 4;
						}
						String sType;
						if (iEnd > sQuery.length()) {
							sType = sQuery.substring(iStart);
						} else {
							sType = sQuery.substring(iStart, iEnd);
						}

						sType = sType.trim();

						oResult.productType = sType;
					}
				} catch (Exception oE) {
					WasdiLog.warnLog("QueryTranslator.parseLandsat( " + sQuery + " ): error while parsing product type: " + oE);
				}
				
				parseCloudCoverage(sQuery, oResult);
				
				return true;
			}
		} 
		catch (Exception oE) {
			WasdiLog.warnLog("QueryTranslator.parseLandsat( " + sQuery + " ): " + oE);
		}
		
		return false;
	}
	
	private boolean parseProbaV(String sQuery, QueryViewModel oResult) {
		try {
			if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_PROBAV)) {
				sQuery = removePlatformToken(sQuery, s_sPLATFORMNAME_PROBAV);

				oResult.platformName = Platforms.PROBAV;

				// check for product type
				try {
					if (sQuery.contains(s_sPROBAVCOLLECTION)) {
						int iStart = sQuery.indexOf(s_sPROBAVCOLLECTION);
						if (iStart < 0) {
							throw new IllegalArgumentException("Could not find product type");
						}
						iStart += s_sPROBAVCOLLECTION.length();
						int iEnd = sQuery.indexOf(" AND ", iStart);
						if (iEnd < 0) {
							iEnd = sQuery.indexOf(')', iStart);
						}
						if (iEnd < 0) {
							iEnd = sQuery.indexOf(' ', iStart);
						}
						if (iEnd < 0) {
							// the types can be of different letters, try this medium if we are lucky
							iEnd = iStart + 42;
						}
						String sType = sQuery.substring(iStart, iEnd);
						sType = sType.trim();

						oResult.productType = sType;
					}
				} catch (Exception oE) {
					WasdiLog.warnLog("QueryTranslator.parseProbaV( " + sQuery + " ): error while parsing product type: " + oE);
				}
				
				parseCloudCoverage(sQuery, oResult);
				
				return true;
			}
		} 
		catch (Exception oE) {
			WasdiLog.warnLog("QueryTranslator.parseProbaV( " + sQuery + " ): " + oE);
		}
		return false;
	}	
	
	/**
	 * Parse Sentinel 3 info
	 * @param sQuery
	 * @param oResult
	 */
	private boolean parseSentinel3(String sQuery, QueryViewModel oResult) {
		try {
			if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_SENTINEL_3)) {
				sQuery = removePlatformToken(sQuery, s_sPLATFORMNAME_SENTINEL_3);

				oResult.platformName = Platforms.SENTINEL3;

				// check for product type
				try {
					if (Utils.isNullOrEmpty(oResult.productType) && sQuery.contains(QueryTranslator.s_sPRODUCTTYPE)) {
						int iStart = sQuery.indexOf(s_sPRODUCTTYPE);
						if (iStart < 0) {
							throw new IllegalArgumentException("Could not find product type");
						}
						iStart += s_sPRODUCTTYPE.length();
						int iEnd = sQuery.indexOf(" AND ", iStart);
						if (iEnd < 0) {
							iEnd = sQuery.indexOf(')', iStart);
						}
						if (iEnd < 0) {
							iEnd = sQuery.indexOf(' ', iStart);
						}
						if (iEnd < 0) {
							// the types can be OCN, GRD, SLC, all of three letters
							iEnd = iStart + 3;
						}
						String sType = sQuery.substring(iStart, iEnd);
						sType = sType.trim();

						oResult.productType = sType;
					}
				} catch (Exception oE) {
					WasdiLog.warnLog("QueryTranslator.parseSentinel3( " + sQuery + " ): error while parsing product type: " + oE);
				}

				// check for timeliness
				try {
					if (sQuery.contains(QueryTranslator.s_s_TIMELINESS)) {
						
						String sSearchKey = QueryTranslator.s_s_TIMELINESS;
						if (sSearchKey.endsWith(":")) {
							sSearchKey = sSearchKey.substring(0, sSearchKey.length() - 1);
						}
						String sTimeliness = extractValue(sQuery, sSearchKey);
						
						if (!Utils.isNullOrEmpty(sTimeliness)) {
							oResult.timeliness = sTimeliness;
						}
					}
				} catch (Exception oE) {
					WasdiLog.warnLog("QueryTranslator.parseSentinel3( " + sQuery + " ): error while parsing timeliness: " + oE);
				}
				
				// check for processing level
				try {
					if (sQuery.contains(QueryTranslator.s_sPRODUCTLEVEL)) {
						
						String sSearchKey = QueryTranslator.s_sPRODUCTLEVEL;
						if (sSearchKey.endsWith(":")) {
							sSearchKey =  sSearchKey.substring(0, sSearchKey.length() - 1);
						}
						
						String sProcessingLevel = extractValue(sQuery, sSearchKey);
						
						if (!Utils.isNullOrEmpty(sProcessingLevel)) {
							oResult.productLevel = sProcessingLevel;
						}
					}
					
				} catch (Exception oE) {
					WasdiLog.warnLog("QueryTranslator.parseSentinel3( " + sQuery + "): error while parsing processing level: " + oE);
				}
				
				return true;
			}
		} 
		catch (Exception oE) {
			WasdiLog.warnLog("QueryTranslator.parseSentinel3( " + sQuery + " ): " + oE);
		}
		return false;
	}
	
	/**
	 * Parse Sentinel 5P Info
	 * @param sQuery
	 * @param oResult
	 */
	private boolean parseSentinel5P(String sQuery, QueryViewModel oResult) {
		try {
			if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_SENTINEL_5P)) {
				sQuery = removePlatformToken(sQuery, s_sPLATFORMNAME_SENTINEL_5P);

				oResult.platformName = Platforms.SENTINEL5P;

				// check for product type
				try {
					if (sQuery.contains(QueryTranslator.s_sPRODUCTLEVEL)) {
						int iStart = sQuery.indexOf(s_sPRODUCTLEVEL);
						if (iStart < 0) {
							throw new IllegalArgumentException("Could not find product level");
						}
						iStart += s_sPRODUCTLEVEL.length();
						int iEnd = sQuery.indexOf(" AND ", iStart);
						if (iEnd < 0) {
							iEnd = sQuery.indexOf(')', iStart);
						}
						if (iEnd < 0) {
							iEnd = sQuery.indexOf(' ', iStart);
						}
						if (iEnd < 0) {
							// the types are all of ten letters
							iEnd = iStart + 10;
						}
						String sLevel = sQuery.substring(iStart, iEnd);
						sLevel = sLevel.trim();

						oResult.productLevel = sLevel;
					}
				} catch (Exception oE) {
					WasdiLog.warnLog("QueryTranslator.parseSentinel5P( " + sQuery + " ): error while parsing product level: " + oE);
				}

				// check for timeliness
				try {
					if (sQuery.contains(QueryTranslator.s_s_TIMELINESS)) {
						int iStart = sQuery.indexOf(s_s_TIMELINESS);
						if (iStart < 0) {
							throw new IllegalArgumentException("Could not find sensor mode");
						}
						iStart += s_s_TIMELINESS.length();
						int iEnd = sQuery.indexOf(" AND ", iStart);
						if (iEnd < 0) {
							iEnd = sQuery.indexOf(')', iStart);
						}
						if (iEnd < 0) {
							iEnd = sQuery.length();
						}
						String sTimeliness = sQuery.substring(iStart, iEnd);
						sTimeliness = sTimeliness.trim();

						oResult.timeliness = sTimeliness;
					}
				} catch (Exception oE) {
					WasdiLog.warnLog("QueryTranslator.parseSentinel5P( " + sQuery + " ): error while parsing product type: " + oE);
				}
				
				
				try {
					// check for relative orbit
					if (sQuery.contains(QueryTranslator.s_sABSOLUTEORBITNUMBER)) {
						try {
							int iStart = sQuery.indexOf(s_sABSOLUTEORBITNUMBER);
							if (iStart < 0) {
								throw new IllegalArgumentException("Could not find absolute orbit number");
							}
							iStart += s_sABSOLUTEORBITNUMBER.length();
							int iEnd = sQuery.indexOf(" AND ", iStart);
							if (iEnd < 0) {
								iEnd = sQuery.indexOf(')');
							}
							if (iEnd < 0) {
								iEnd = sQuery.indexOf(' ', iStart);
							}
							if (iEnd < 0) {
								// if anything else failed, skip digits
								iEnd = iStart;
								while (iEnd < sQuery.length() && Character.isDigit(sQuery.charAt(iEnd))) {
									iEnd++;
								}
							}
							String sOrbit = sQuery.substring(iStart, iEnd);
							int iOrbit = Integer.parseInt(sOrbit);

							oResult.absoluteOrbit = iOrbit;

						} catch (Exception oE) {
							WasdiLog.warnLog("QueryTranslator.parseSentinel5P(" + sQuery + " ): error while parsing absolute orbit: " + oE);
						}
					}						
				}
				catch (Exception oE) {
					WasdiLog.warnLog("QueryTranslator.parseSentinel5P( " + sQuery + " ): error while parsing absolute orbit: " + oE);
				}
				
				return true;
			}
		} 
		catch (Exception oE) {
			WasdiLog.warnLog("QueryTranslator.parseSentinel3( " + sQuery + " ): " + oE);
		}
		
		return false;
	}
	
	/**
	 * Parse Sentinel-6 Info
	 * @param sQuery
	 * @param oResult
	 */
	private boolean parseSentinel6(String sQuery, QueryViewModel oResult) {
		try {
			
			if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_SENTINEL_6)) {
				sQuery = removePlatformToken(sQuery, s_sPLATFORMNAME_SENTINEL_6);

				oResult.platformName = Platforms.SENTINEL6;
				
				oResult.productLevel = extractValue(sQuery, "productlevel");
				oResult.instrument = extractValue(sQuery, "Instrument");
				oResult.timeliness = extractValue(sQuery, "timeliness");
				
				try {
					String sOrbit = extractValue(sQuery, "absoluteorbit");
					if (!Utils.isNullOrEmpty(sOrbit))
						oResult.relativeOrbit = Integer.parseInt(sOrbit);
				} catch (NumberFormatException oEx) {
					WasdiLog.errorLog("QueryTranslator.parseSentinel6: error parsing absolute orbit in query " + sQuery, oEx);
				}		
				
				return true;
			}
		} 
		catch (Exception oE) {
			WasdiLog.errorLog("QueryTranslator.parseSentinel6. Error parsing query: " + sQuery, oE);
		}
		return false;
	}
	
	/**
	 * Parse Envisat
	 * @param sQuery
	 * @param oResult
	 */
	private boolean parseEnvisat(String sQuery, QueryViewModel oResult) {
		try {
			if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_ENVISAT)) {
				sQuery = removePlatformToken(sQuery, s_sPLATFORMNAME_ENVISAT);

				oResult.platformName = Platforms.ENVISAT;

				// check for product type
				try {
					if (sQuery.contains(s_sENVISATORBITDIRECTION)) {
						int iStart = sQuery.indexOf(s_sENVISATORBITDIRECTION);
						if (iStart < 0) {
							throw new IllegalArgumentException("Could not find orbit direction");
						}
						iStart += s_sENVISATORBITDIRECTION.length();
						int iEnd = sQuery.indexOf(" AND ", iStart);
						if (iEnd < 0) {
							iEnd = sQuery.indexOf(')', iStart);
						}
						if (iEnd < 0) {
							iEnd = sQuery.indexOf(' ', iStart);
						}
						if (iEnd < 0) {
							iEnd = iStart + 9;
						}
						String sType = sQuery.substring(iStart, iEnd);
						sType = sType.trim();

						oResult.productType = sType;
					}
				} 
				catch (Exception oE) {
					WasdiLog.warnLog("QueryTranslator.parseEnvisat( " + sQuery + " ): error while parsing product type: " + oE);
				}
				return true;
			}
		} catch (Exception oE) {
			WasdiLog.warnLog("QueryTranslator.parseEnvisat( " + sQuery + " ): " + oE);
		}
		
		return false;
	}	

	
	/**
	 * Parse Copernicus Marine
	 * @param sQuery
	 * @param oResult
	 */
	private boolean parseCopernicusMarine(String sQuery, QueryViewModel oResult) {
		try {
			if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_COPERNICUS_MARINE)) {
				sQuery = removePlatformToken(sQuery, s_sPLATFORMNAME_COPERNICUS_MARINE);

				oResult.platformName = Platforms.COPERNICUS_MARINE;
				
				return true;
			}
		} 
		catch (Exception oE) {
			WasdiLog.debugLog("QueryTranslator.parseCopernicusMarine( " + sQuery + " ): " + oE);
		}
		
		return false;
	}
	
	/**
	 * Parse Planet Data
	 * @param sQuery
	 * @param oResult
	 */
	private boolean parsePlanet(String sQuery, QueryViewModel oResult) {
		try {
			if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_PLANET)) {
				oResult.platformName = Platforms.PLANET;
				
				return true;
			}
		} 
		catch (Exception oE) {
			WasdiLog.warnLog("QueryTranslator.parseEnvisat( " + sQuery + " ): " + oE);
		}
		
		return false;
	}	
	
	/**
	 * read a int from the query
	 * 
	 * @param sQuery
	 * @param sKeyword
	 * @return
	 */
	protected int readInt(String sQuery, String sKeyword) {
		int iStart;
		int iEnd;
		int iTemp = -1;
		if (sQuery.contains(sKeyword)) {
			iStart = sQuery.indexOf(sKeyword);
			if (iStart < 0) {
				throw new IllegalArgumentException("Couldn't find limit start");
			}
			iStart += sKeyword.length();
			iEnd = iStart;
			while (iEnd < sQuery.length() && Character.isDigit(sQuery.charAt(iEnd))) {
				++iEnd;
			}
			String sTemp = sQuery.substring(iStart, iEnd);
			iTemp = Integer.parseInt(sTemp);
		}
		return iTemp;
	}

	/**
	 * Parse Query text Interval
	 * 
	 * @param sQuery
	 * @param sKeyword
	 * @param alStartEnd
	 */
	protected void parseInterval(String sQuery, String sKeyword, String[] asInterval) {
		Preconditions.checkNotNull(sQuery, "QueryTranslator.parseInterval: query is null");
		Preconditions.checkNotNull(sKeyword, "QueryTranslator.parseInterval: field keyword is null");
		Preconditions.checkNotNull(asInterval, "QueryTranslator.parseInterval: String array is null");
		Preconditions.checkElementIndex(0, asInterval.length,
				"QueryTranslator.parseInterval: 0 is not a valid element index");
		Preconditions.checkElementIndex(1, asInterval.length,
				"QueryTranslator.parseInterval: 1 is not a valid element index");

		if (sQuery.contains(sKeyword)) {
			int iStart = Math.max(0, sQuery.indexOf(sKeyword));
			iStart = Math.max(iStart, sQuery.indexOf('[', iStart) + 1);
			int iEnd = sQuery.indexOf(']', iStart);
			if (iEnd < 0) {
				iEnd = sQuery.length() - 1;
			}
			;
			String[] asTimeQuery = sQuery.substring(iStart, iEnd).trim().split(" TO ");
			asInterval[0] = asTimeQuery[0];
			asInterval[1] = asTimeQuery[1];
		}
	}
}
