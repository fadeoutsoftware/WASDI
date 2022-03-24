/**
 * Created by Cristiano Nattero on 2018-11-28
 * 
 * Fadeout software
 *
 */
package wasdi.shared.queryexecutors;

import java.util.Arrays;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.google.common.base.Preconditions;

import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
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
	 * Token of Landsat platform
	 */
	private static final String s_sPLATFORMNAME_LANDSAT = "platformname:Landsat-*";
	/**
	 * Token of Landsat platform
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
		// sResult = java.net.URLEncoder.encode(sDecoded, m_sEnconding);
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
			Utils.debugLog("QueryTranslator.getProductName( " + sQuery + " ): " + oE);
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
				Utils.debugLog("QueryTranslator.parseWasdiClientQuery( " + sQuery + " ): could not parse offset: " + oE);
			}

			oResult.offset = iOffset;

			// Try to get limit
			int iLimit = 10;
			try {
				iLimit = readInt(sQuery, QueryTranslator.s_sLIMIT);
			} catch (Exception oE) {
				Utils.debugLog( "QueryTranslator.parseWasdiClientQuery( " + sQuery + " ): could not parse limit: " + oE);
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
								Utils.log("ERROR",
										"QueryTranslator.parseWasdiClientQuery: issue with current coordinate pair: "
												+ sPair + ": " + oE);
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
						Utils.log("ERROR",
								"QueryTranslator.parseWasdiClientQuery: could not complete footprint detection: " + oE);
					}
				}
				
				//no platform? see if we can infer it from the product name (provided it's not null)
				if(Utils.isNullOrEmpty(oResult.platformName) && !Utils.isNullOrEmpty(oResult.productName)){
					reverseEngineerQueryFromProductName(oResult, oResult.productName);
				}
			} catch (Exception oE) {
				Utils.log("ERROR",
						"QueryTranslator.parseWasdiClientQuery: could not identify footprint substring limits: "
								+ oE);
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
				Utils.debugLog("QueryTranslator.parseWasdiClientQuery: product type: " + oE);
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

			// Try to get info about S1
			parseSentinel1(sQuery, oResult);

			// Try to get Info About S2
			parseSentinel2(sQuery, oResult);
			
			// Try get Info about VIIRS
			parseVIIRS(sQuery, oResult);
			
			// Try get Info about ERA5
			parseERA5(sQuery, oResult);
			
			// Try to get info about Landsat
			parseLandsat(sQuery, oResult);
			
			// Try to get Info about ProbaV
			parseProbaV(sQuery, oResult);
			
			// Try to get Info about Sentinel 3
			parseSentinel3(sQuery, oResult);
			
			// Try to get info about Envisat
			parseEnvisat(sQuery, oResult);
			
			// Try to get info about Copernicus Marine
			parseCopernicusMarine(sQuery, oResult);
			
			// Try to get Info about Sentinel 5P
			parseSentinel5P(sQuery, oResult);
			
			parsePlanet(sQuery, oResult);

			// Try get Info about DEM
			parseDEM(sQuery, oResult);

			// Try get Info about WorldCover
			parseWorldCover(sQuery, oResult);
			
			// Try get Info about Statics: needs the original query to avoid
			// Conflits with the "AND" literal
			parseStatics(sOriginalQuery, oResult);

		} catch (Exception oEx) {
			Utils.debugLog("QueryTranslator.parseWasdiClientQuery: exception " + oEx.toString());
			String sStack = ExceptionUtils.getStackTrace(oEx);
			Utils.debugLog("QueryTranslator.parseWasdiClientQuery: stack " + sStack);
		}

		return oResult;
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
	private void parseSentinel1(String sQuery, QueryViewModel oResult) {
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
					Utils.debugLog("QueryTranslator.parseWasdiClientQuery( " + sQuery
							+ " ): error while parsing product type: " + oE);
				}

				// check for product type
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
					Utils.debugLog("QueryTranslator.parseWasdiClientQuery( " + sQuery
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
						Utils.debugLog("QueryTranslator.parseWasdiClientQuery(" + sQuery
								+ " ): error while parsing relative orbit: " + oE);
					}
				}
			}
		} catch (Exception oE) {
			Utils.debugLog("QueryTranslator.parseWasdiClientQuery( " + sQuery + " ): " + oE);
		}
	}

	/**
	 * Fill the Query View Model with the S2 values
	 * 
	 * @param sQuery
	 * @param oResult
	 */
	private void parseSentinel2(String sQuery, QueryViewModel oResult) {

		try {
			if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_SENTINEL_2)) {
				sQuery = removePlatformToken(sQuery, s_sPLATFORMNAME_SENTINEL_2);

				oResult.platformName = Platforms.SENTINEL2;

				// check for cloud coverage
				parseCloudCoverage(sQuery, oResult);
			}
		} catch (Exception oE) {
			Utils.debugLog("QueryTranslator.parseSentinel_2( " + sQuery + " ): " + oE);
		}
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
			Utils.debugLog("QueryTranslator.parseCloudCoverage( " + sQuery + " ): could not parse cloud coverage: " + oE);
		}
		
	}
	
	
	/**
	 * Fills the Query View Model with S1 info
	 * 
	 * @param sQuery
	 * @param oResult
	 */
	private void parseVIIRS(String sQuery, QueryViewModel oResult) {
		try {
			if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_VIIRS)) {
				sQuery = removePlatformToken(sQuery, s_sPLATFORMNAME_VIIRS);

				oResult.platformName = Platforms.VIIRS;

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
							// the types can be VIIRS_1d_composite, VIIRS_5d_composite all 18 letters
							iEnd = iStart + 18;
						}
						String sType = sQuery.substring(iStart, iEnd);
						sType = sType.trim();

						oResult.productType = sType;
					}
				} catch (Exception oE) {
					Utils.debugLog("QueryTranslator.parseVIIRS( " + sQuery + " ): error while parsing product type: " + oE);
				}
			}
		} catch (Exception oE) {
			Utils.debugLog("QueryTranslator.parseWasdiClientQuery( " + sQuery + " ): " + oE);
		}
	}

	/**
	 * Fills the Query View Model with ERA5 info
	 * 
	 * @param sQuery the query
	 * @param oResult the resulting Query View Model
	 */
	private void parseERA5(String sQuery, QueryViewModel oResult) {
		if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_ERA5)) {
			sQuery = removePlatformToken(sQuery, s_sPLATFORMNAME_ERA5);

			oResult.platformName = Platforms.ERA5;

			oResult.productName = extractValue(sQuery, "dataset");
			oResult.productType = extractValue(sQuery, "productType");
			oResult.productLevel = extractValue(sQuery, "pressureLevels");
			oResult.sensorMode = extractValue(sQuery, "variables");
			oResult.timeliness = extractValue(sQuery, "format");
		}
	}

	/**
	 * Fills the Query View Model with DEM info
	 * 
	 * @param sQuery the query
	 * @param oResult the resulting Query View Model
	 */
	private void parseDEM(String sQuery, QueryViewModel oResult) {
		if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_DEM)) {
			sQuery = removePlatformToken(sQuery, s_sPLATFORMNAME_DEM);

			oResult.platformName = Platforms.DEM;

			oResult.productType = extractValue(sQuery, "dataset");
		}
	}

	/**
	 * Fills the Query View Model with WorldCover info
	 * 
	 * @param sQuery the query
	 * @param oResult the resulting Query View Model
	 */
	private void parseWorldCover(String sQuery, QueryViewModel oResult) {
		if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_WORLD_COVER)) {
			sQuery = removePlatformToken(sQuery, s_sPLATFORMNAME_WORLD_COVER);

			oResult.platformName = Platforms.WORLD_COVER;

			oResult.productType = extractValue(sQuery, "dataset");
		}
	}
	
	/**
	 * Fills the Query View Model with Statics Info
	 * @param sQuery
	 * @param oResult
	 */
	private void parseStatics(String sQuery, QueryViewModel oResult) {
		if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_STATICS)) {
			sQuery = removePlatformToken(sQuery, s_sPLATFORMNAME_STATICS);

			oResult.platformName = Platforms.STATICS;
			oResult.productType = extractValue(sQuery, "producttype");
		}
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
	private static String extractValue(String sQuery, String sKey) {
		int iStart = -1;
		int iEnd = -1;

		if (sQuery.contains(sKey)) {
			iStart = sQuery.indexOf(sKey);

			iStart += (sKey.length() + 1);
			iEnd = sQuery.indexOf(" AND ", iStart);

			if (iEnd < 0) {
				iEnd = sQuery.length();
			}

			String sType = sQuery.substring(iStart, iEnd);
			sType = sType.trim();

			return sType;
		}

		return null;
	}
	
	/**
	 * Parse Landsat filters
	 * @param sQuery
	 * @param oResult
	 */
	private void parseLandsat(String sQuery, QueryViewModel oResult) {
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
						String sType = sQuery.substring(iStart, iEnd);
						sType = sType.trim();

						oResult.productType = sType;
					}
				} catch (Exception oE) {
					Utils.debugLog("QueryTranslator.parseLandsat( " + sQuery
							+ " ): error while parsing product type: " + oE);
				}
				
				parseCloudCoverage(sQuery, oResult);
			}
		} catch (Exception oE) {
			Utils.debugLog("QueryTranslator.parseLandsat( " + sQuery + " ): " + oE);
		}
	}
	
	private void parseProbaV(String sQuery, QueryViewModel oResult) {
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
					Utils.debugLog("QueryTranslator.parseProbaV( " + sQuery
							+ " ): error while parsing product type: " + oE);
				}
				
				parseCloudCoverage(sQuery, oResult);
			}
		} catch (Exception oE) {
			Utils.debugLog("QueryTranslator.parseProbaV( " + sQuery + " ): " + oE);
		}
	}	
	
	/**
	 * Parse Sentinel 3 info
	 * @param sQuery
	 * @param oResult
	 */
	private void parseSentinel3(String sQuery, QueryViewModel oResult) {
		try {
			if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_SENTINEL_3)) {
				sQuery = removePlatformToken(sQuery, s_sPLATFORMNAME_SENTINEL_3);

				oResult.platformName = Platforms.SENTINEL3;

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
					Utils.debugLog("QueryTranslator.parseSentinel3( " + sQuery + " ): error while parsing product type: " + oE);
				}

				// check for product type
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
							iEnd = sQuery.indexOf(' ', iStart);
						}
						if (iEnd < 0) {
							iEnd = iStart + 12;
						}
						String sTimeliness = sQuery.substring(iStart, iEnd);
						sTimeliness = sTimeliness.trim();

						oResult.timeliness = sTimeliness;
					}
				} catch (Exception oE) {
					Utils.debugLog("QueryTranslator.parseSentinel3( " + sQuery + " ): error while parsing product type: " + oE);
				}
			}
		} catch (Exception oE) {
			Utils.debugLog("QueryTranslator.parseSentinel3( " + sQuery + " ): " + oE);
		}
	}
	
	/**
	 * Parse Sentinel 5P Info
	 * @param sQuery
	 * @param oResult
	 */
	private void parseSentinel5P(String sQuery, QueryViewModel oResult) {
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
					Utils.debugLog("QueryTranslator.parseSentinel5P( " + sQuery + " ): error while parsing product level: " + oE);
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
							iEnd = sQuery.indexOf(' ', iStart);
						}
						if (iEnd < 0) {
							iEnd = iStart + 12;
						}
						String sTimeliness = sQuery.substring(iStart, iEnd);
						sTimeliness = sTimeliness.trim();

						oResult.timeliness = sTimeliness;
					}
				} catch (Exception oE) {
					Utils.debugLog("QueryTranslator.parseSentinel5P( " + sQuery + " ): error while parsing product type: " + oE);
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
							Utils.debugLog("QueryTranslator.parseSentinel5P(" + sQuery + " ): error while parsing absolute orbit: " + oE);
						}
					}						
				}
				catch (Exception oE) {
					Utils.debugLog("QueryTranslator.parseSentinel5P( " + sQuery + " ): error while parsing absolute orbit: " + oE);
				}
			}
		} catch (Exception oE) {
			Utils.debugLog("QueryTranslator.parseSentinel3( " + sQuery + " ): " + oE);
		}
	}
	
	/**
	 * Parse Envisat
	 * @param sQuery
	 * @param oResult
	 */
	private void parseEnvisat(String sQuery, QueryViewModel oResult) {
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
				} catch (Exception oE) {
					Utils.debugLog("QueryTranslator.parseEnvisat( " + sQuery + " ): error while parsing product type: " + oE);
				}
			}
		} catch (Exception oE) {
			Utils.debugLog("QueryTranslator.parseEnvisat( " + sQuery + " ): " + oE);
		}
	}	

	
	/**
	 * Parse Copernicus Marine
	 * @param sQuery
	 * @param oResult
	 */
	private void parseCopernicusMarine(String sQuery, QueryViewModel oResult) {
		try {
			if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_COPERNICUS_MARINE)) {
				sQuery = removePlatformToken(sQuery, s_sPLATFORMNAME_COPERNICUS_MARINE);

				oResult.platformName = Platforms.COPERNICUS_MARINE;
			}
		} catch (Exception oE) {
			Utils.debugLog("QueryTranslator.parseCopernicusMarine( " + sQuery + " ): " + oE);
		}
	}
	
	/**
	 * Parse Planet Data
	 * @param sQuery
	 * @param oResult
	 */
	private void parsePlanet(String sQuery, QueryViewModel oResult) {
		try {
			if (sQuery.contains(QueryTranslator.s_sPLATFORMNAME_PLANET)) {
				
				oResult.platformName = Platforms.PLANET;
			}
		} catch (Exception oE) {
			Utils.debugLog("QueryTranslator.parseEnvisat( " + sQuery + " ): " + oE);
		}
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

	protected void reverseEngineerQueryFromProductName(QueryViewModel oQueryViewModel, String sProductName) {
		try {
			if(Utils.isNullOrEmpty(sProductName)) {
				Utils.debugLog("QueryTranslator.reverseEngineerQueryFromProductName: query is null or empty");
			}
			//mission
			if(sProductName.startsWith("S1")) {
				oQueryViewModel.platformName = Platforms.SENTINEL1;
				
				String[] asTypesA = {"RAW", "GRD", "SLC", "OCN"};
				if(Arrays.stream(asTypesA).anyMatch((sProductName.substring(7, 10))::equals)){
					oQueryViewModel.productType=(sProductName.substring(7, 10));
				} else {
					Utils.debugLog("QueryTranslator.reverseEngineerQueryFromProductName: product type not recognized from Sentinel-1 product " + sProductName + ", skipping");
				}
			} else if (sProductName.startsWith("S2")) {
				oQueryViewModel.platformName = Platforms.SENTINEL2;
				String[] asTypesA = {"L1C", "L2A"};
				if(Arrays.stream(asTypesA).anyMatch((sProductName.substring(7, 10))::equals)){
					oQueryViewModel.productType="S2MSI" + sProductName.substring(8, 10);
				} else {
					Utils.debugLog("QueryTranslator.reverseEngineerQueryFromProductName: product type not recognized from Sentinel-1 product " + sProductName + ", skipping");
				}
			} else if(sProductName.startsWith("S3A_") || sProductName.startsWith("S3B_")) {
				oQueryViewModel.platformName = Platforms.SENTINEL3;
			} else if(sProductName.startsWith("LC08_")) {
				oQueryViewModel.platformName = Platforms.LANDSAT8;
			}  else {
				
				String sPlatformName = WasdiFileUtils.getPlatformFromSatelliteImageFileName(sProductName);
				
				if (!Utils.isNullOrEmpty(sPlatformName)) {
					oQueryViewModel.platformName = sPlatformName;
				}
				else {
					Utils.debugLog("QueryTranslator.reverseEngineerQueryFromProductName: platform not recognized (maybe not implemented yet) in product: " + sProductName + ", ignoring");
				}
				
			}
		} catch (Exception oE) {
			Utils.debugLog("QueryTranslator.reverseEngineerQueryFromProductName: " + oE);
		}
	}
}
