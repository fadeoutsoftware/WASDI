/**
 * Created by Cristiano Nattero on 2018-11-28
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch;

import java.util.Arrays;

import com.google.common.base.Preconditions;

import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.QueryViewModel;

/**
 * Generic DIAS Query Translator Convert the WASDI query in a query supported by
 * the specific DIAS
 * 
 * @author c.nattero
 *
 */
public abstract class DiasQueryTranslator {

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
	 * Token of S2 platform
	 */
	private static final String s_sPLATFORMNAME_SENTINEL_2 = "platformname:Sentinel-2";
	/**
	 * Token of S1 platform
	 */
	private static final String s_sPLATFORMNAME_SENTINEL_1 = "platformname:Sentinel-1";
	/**
	 * Token of product type
	 */
	private static final String s_sPRODUCTTYPE = "producttype:";
	/**
	 * Token of the relative orbit
	 */
	private static final String s_sRELATIVEORBITNUMBER = "relativeorbitnumber:";
	/**
	 * Token of sensor mode
	 */
	private static final String s_sSENSORMODE = "sensoroperationalmode:";

	protected String m_sParserConfigPath;
	protected String m_sAppConfigPath;

	public String translateAndEncode(String sQueryFromClient) {
		Utils.debugLog("DiasQueryTranslator.translateAndEncode");
		return encode(translate(sQueryFromClient));
	}

	public void setParserConfigPath(String sParserConfigPath) {
		if (null == sParserConfigPath) {
			Utils.debugLog("DiasQueryTranslator.setParserConfigPath: warning: parser config path is null");
		} else if (sParserConfigPath.isEmpty()) {
			Utils.debugLog("DiasQueryTranslator.setParserConfigPath: warning: parser config path is empty");
		}
		this.m_sParserConfigPath = sParserConfigPath;
	}

	public void setAppconfigPath(String sAppConfigPath) {
		if (null == sAppConfigPath) {
			Utils.debugLog("DiasQueryTranslator.setParserConfigPath: warning: app config path is null");
		} else if (sAppConfigPath.isEmpty()) {
			Utils.debugLog("DiasQueryTranslator.setParserConfigPath: warning: app config path is empty");
		}
		this.m_sAppConfigPath = sAppConfigPath;
	}

	/**
	 * translates from WASDI query (OpenSearch) to <derived class> format
	 * 
	 * @param sQueryFromClient WASDI Query
	 * @return Provider Query
	 */
	protected abstract String translate(String sQueryFromClient);

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
			Utils.debugLog("DiasQueryTranslator.getFreeTextSearch( " + sQuery + " ): " + oE);
		}
		return "";
	}

	protected String parseProductName(String sQuery) {
		return getProductName(sQuery);
	}

	protected String convertRanges(String sQuery) {
		return sQuery;
	}

	protected abstract String parseTimeFrame(String sQuery);

	protected abstract String parseFootPrint(String sQuery);

	/**
	 * Convert the textual WASDI client Query in a View Mode
	 * 
	 * @param sQuery
	 * @return
	 */
	protected QueryViewModel parseWasdiClientQuery(String sQuery) {

		QueryViewModel oResult = new QueryViewModel();

		try {

			// Prepare the text that is uniform
			sQuery = prepareQuery(sQuery);

			// Try to get offset
			int iOffset = -1;
			try {
				iOffset = readInt(sQuery, DiasQueryTranslator.s_sOFFSET);
			} catch (Exception oE) {
				Utils.debugLog(
						"DiasQueryTranslator.parseWasdiClientQuery( " + sQuery + " ): could not parse offset: " + oE);
			}

			oResult.offset = iOffset;

			// Try to get limit
			int iLimit = 10;
			try {
				iLimit = readInt(sQuery, DiasQueryTranslator.s_sLIMIT);
			} catch (Exception oE) {
				Utils.debugLog(
						"DiasQueryTranslator.parseWasdiClientQuery( " + sQuery + " ): could not parse limit: " + oE);
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
										"DiasQueryTranslator.parseWasdiClientQuery: issue with current coordinate pair: "
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
								"DiasQueryTranslatorEODC.parseWasdiClientQuery: could not complete footprint detection: "
										+ oE);
					}
				}
				
				//no platform? see if we can infer it from the product name (provided it's not null)
				if(Utils.isNullOrEmpty(oResult.platformName) && !Utils.isNullOrEmpty(oResult.productName)){
					reverseEngineerQueryFromProductName(oResult, oResult.productName);
				}
			} catch (Exception oE) {
				Utils.log("ERROR",
						"DiasQueryTranslator.parseWasdiClientQuery: could not identify footprint substring limits: "
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
				Utils.debugLog("DiasQueryTranslator.parseWasdiClientQuery: product type: " + oE);
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

		} catch (Exception oEx) {
			Utils.debugLog("DiasQueryTranslator.parseWasdiClientQuery: exception " + oEx.toString());
		}

		return oResult;
	}

	/**
	 * Fills the Query View Model with S1 info
	 * 
	 * @param sQuery
	 * @param oResult
	 */
	private void parseSentinel1(String sQuery, QueryViewModel oResult) {
		try {
			if (sQuery.contains(DiasQueryTranslator.s_sPLATFORMNAME_SENTINEL_1)) {

				int iStart = sQuery.indexOf(s_sPLATFORMNAME_SENTINEL_1);

				if (iStart >= 0) {

					iStart += s_sPLATFORMNAME_SENTINEL_1.length();
					int iEnd = sQuery.indexOf(')', iStart);
					if (iEnd < 0) {
						sQuery = sQuery.substring(iStart);
					} else {
						sQuery = sQuery.substring(iStart, iEnd);
					}
					sQuery = sQuery.trim();

					oResult.platformName = "Sentinel-1";

					// check for product type
					try {
						if (sQuery.contains(DiasQueryTranslator.s_sPRODUCTTYPE)) {
							iStart = sQuery.indexOf(s_sPRODUCTTYPE);
							if (iStart < 0) {
								throw new IllegalArgumentException("Could not find product type");
							}
							iStart += s_sPRODUCTTYPE.length();
							iEnd = sQuery.indexOf(" AND ", iStart);
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
						Utils.debugLog("DiasQueryTranslator.parseWasdiClientQuery( " + sQuery
								+ " ): error while parsing product type: " + oE);
					}

					// check for product type
					try {
						if (sQuery.contains(DiasQueryTranslator.s_sSENSORMODE)) {
							iStart = sQuery.indexOf(s_sSENSORMODE);
							if (iStart < 0) {
								throw new IllegalArgumentException("Could not find sensor mode");
							}
							iStart += s_sSENSORMODE.length();
							iEnd = sQuery.indexOf(" AND ", iStart);
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
						Utils.debugLog("DiasQueryTranslator.parseWasdiClientQuery( " + sQuery
								+ " ): error while parsing product type: " + oE);
					}

					// check for relative orbit
					if (sQuery.contains(DiasQueryTranslator.s_sRELATIVEORBITNUMBER)) {
						try {
							iStart = sQuery.indexOf(s_sRELATIVEORBITNUMBER);
							if (iStart < 0) {
								throw new IllegalArgumentException("Could not find relative orbit number");
							}
							iStart += s_sRELATIVEORBITNUMBER.length();
							iEnd = sQuery.indexOf(" AND ", iStart);
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
							Utils.debugLog("DiasQueryTranslator.parseWasdiClientQuery(" + sQuery
									+ " ): error while parsing relative orbit: " + oE);
						}
					}
				}
			}
		} catch (Exception oE) {
			Utils.debugLog("DiasQueryTranslatorEODC.parseWasdiClientQuery( " + sQuery + " ): " + oE);
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
			if (sQuery.contains(DiasQueryTranslator.s_sPLATFORMNAME_SENTINEL_2)) {
				int iStart = sQuery.indexOf(s_sPLATFORMNAME_SENTINEL_2);

				if (iStart >= 0) {

					iStart += s_sPLATFORMNAME_SENTINEL_2.length();
					int iEnd = sQuery.indexOf(')', iStart);
					if (iEnd < 0) {
						sQuery = sQuery.substring(iStart);
					} else {
						sQuery = sQuery.substring(iStart, iEnd);
					}

					oResult.platformName = "Sentinel-2";

					// check for cloud coverage
					try {
						if (sQuery.contains(DiasQueryTranslator.s_sCLOUDCOVERPERCENTAGE)) {
							iStart = sQuery.indexOf(s_sCLOUDCOVERPERCENTAGE);

							if (iStart >= 0) {

								iStart += s_sCLOUDCOVERPERCENTAGE.length();
								iEnd = sQuery.indexOf(']', iStart);
								String sSubQuery = sQuery.substring(iStart, iEnd);

								String[] asCloudLimits = sSubQuery.split(" TO ");

								// these variables could be omitted, but in this way we check we are reading
								// numbers
								double dLo = Double.parseDouble(asCloudLimits[0]);
								double dUp = Double.parseDouble(asCloudLimits[1]);

								oResult.cloudCoverageFrom = dLo;
								oResult.cloudCoverageTo = dUp;
							}
						}
					} catch (Exception oE) {
						Utils.debugLog("DiasQueryTranslatorEODC.parseSentinel_2( " + sQuery
								+ " ): could not parse cloud coverage: " + oE);
					}
				}
			}
		} catch (Exception oE) {
			Utils.debugLog("DiasQueryTranslatorEODC.parseSentinel_2( " + sQuery + " ): " + oE);
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
		Preconditions.checkNotNull(sQuery, "DiasQueryTranslator.parseInterval: query is null");
		Preconditions.checkNotNull(sKeyword, "DiasQueryTranslator.parseInterval: field keyword is null");
		Preconditions.checkNotNull(asInterval, "DiasQueryTranslator.parseInterval: String array is null");
		Preconditions.checkElementIndex(0, asInterval.length,
				"DiasQueryTranslator.parseInterval: 0 is not a valid element index");
		Preconditions.checkElementIndex(1, asInterval.length,
				"DiasQueryTranslator.parseInterval: 1 is not a valid element index");

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
				Utils.debugLog("DiasQueryTranslator.reverseEngineerQueryFromProductName: query is null or empty");
			}
			//mission
			if(sProductName.startsWith("S1")) {
				oQueryViewModel.platformName = "Sentinel-1";
				
				String[] asTypesA = {"RAW", "GRD", "SLC", "OCN"};
				if(Arrays.stream(asTypesA).anyMatch((sProductName.substring(7, 10))::equals)){
					oQueryViewModel.productType=(sProductName.substring(7, 10));
				} else {
					Utils.debugLog("DiasQueryTranslator.reverseEngineerQueryFromProductName: product type not recognized from Sentinel-1 product " + sProductName + ", skipping");
				}
			} else if (sProductName.startsWith("S2")) {
				oQueryViewModel.platformName = "Sentinel-2";
				String[] asTypesA = {"L1C", "L2A"};
				if(Arrays.stream(asTypesA).anyMatch((sProductName.substring(7, 10))::equals)){
					oQueryViewModel.productType="S2MSI" + sProductName.substring(8, 10);
				} else {
					Utils.debugLog("DiasQueryTranslator.reverseEngineerQueryFromProductName: product type not recognized from Sentinel-1 product " + sProductName + ", skipping");
				}
			} else if(sProductName.startsWith("S3A_") || sProductName.startsWith("S3B_")) {
				oQueryViewModel.platformName = "Sentinel-3";
			} else if(sProductName.startsWith("LC08_")) {
				oQueryViewModel.platformName = "Landsat8";
			}  else {
				//todo add other platforms:
				// Sentinel-5p, Copernicus-marine, Envisat, ...
				Utils.debugLog("DiasQueryTranslator.reverseEngineerQueryFromProductName: platform not recognized (maybe not implemented yet) in product: " + sProductName + ", ignoring");
			}
		} catch (Exception oE) {
			Utils.debugLog("DiasQueryTranslator.reverseEngineerQueryFromProductName: " + oE);
		}
	}
}
