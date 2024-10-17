/**
 * Created by Cristiano Nattero on 2020-04-27
 * 
 * Fadeout software
 *
 */
package wasdi.shared.queryexecutors.eodc;

import com.google.common.base.Preconditions;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.QueryTranslator;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * @author c.nattero
 *
 */
public class QueryTranslatorEODC extends QueryTranslator {
	private static final String s_sOFFSET = "offset=";
	private static final String s_sLIMIT = "limit=";
	private static final String s_sCLOUDCOVERPERCENTAGE = "cloudcoverpercentage:[";
	private static final String s_sPLATFORMNAME_SENTINEL_2 = "platformname:Sentinel-2";
	private static final String s_sQueryPrefix ="<csw:GetRecords xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" service=\"CSW\" version=\"2.0.2\" resultType=\"results\" ";
	private static final String s_siOffset = "startPosition=\"";
	private static final String s_sMaxRecords = "maxRecords=\"";
	private static final String s_sRemainingPartOfPrefix = "outputFormat=\"application/json\" outputSchema=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.opengis.net/cat/csw/2.0.2 http://schemas.opengis.net/csw/2.0.2/CSW-discovery.xsd\"><csw:Query typeNames=\"csw:Record\"><csw:ElementSetName>full</csw:ElementSetName><csw:Constraint version=\"1.1.0\"><ogc:Filter><ogc:And>";
	private static final String s_sQuerySuffix = "</ogc:And></ogc:Filter></csw:Constraint><ogc:SortBy><ogc:SortProperty><ogc:PropertyName>dc:title</ogc:PropertyName><ogc:SortOrder>ASC</ogc:SortOrder></ogc:SortProperty></ogc:SortBy></csw:Query></csw:GetRecords>";
	private static final String s_sPLATFORMNAME_SENTINEL_1 = "platformname:Sentinel-1";
	private static final String s_sRELATIVEORBITNUMBER = "relativeorbitnumber:";
	private static final String s_sErrorMessage = "ERROR";

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.DiasQueryTranslator#translate(java.lang.String)
	 */
	@Override
	protected String translate(String sQueryFromClient) {
		String sTranslatedQuery = "";
		try {
			String sQuery = prepareQuery(sQueryFromClient);

			if(!Utils.isNullOrEmpty(sQuery)) {
				sTranslatedQuery = parseCommon(sQuery);
				sTranslatedQuery += parseProductName(sQuery);
				sTranslatedQuery += parseFootPrint(sQuery);
				sTranslatedQuery += parseTimeFrame(sQuery);
				sTranslatedQuery += parseSentinel_1(sQuery);
				sTranslatedQuery += parseSentinel_2(sQuery);
				sTranslatedQuery += s_sQuerySuffix;
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("QueryTranslatorEODC.translate( " + sQueryFromClient + " ): " + oE);
		}
		return sTranslatedQuery;
	}

	/**
	 * @param sTranslatedQuery
	 * @param sQuery
	 * @return
	 */
	protected String parseCommon(String sQuery) {
		String sTranslatedQuery = "";
		try {
			sTranslatedQuery = s_sQueryPrefix;

			int iOffset = -1;
			try {
				iOffset = readInt(sQuery, QueryTranslatorEODC.s_sOFFSET);
			} catch (Exception oE) {
				WasdiLog.debugLog("QueryTranslatorEODC.parseCommon( " + sQuery + " ): could not parse offset: " + oE);
			}
			if(iOffset > 0) {
				sTranslatedQuery += s_siOffset;
				sTranslatedQuery += iOffset;
				sTranslatedQuery += "\" ";
			}

			int iLimit = 10;
			try {
				iLimit = readInt(sQuery, QueryTranslatorEODC.s_sLIMIT);
			} catch (Exception oE) {
				WasdiLog.debugLog("QueryTranslatorEODC.parseCommon( " + sQuery + " ): could not parse limit: " + oE);
			}
			if(iLimit >= 0) {
				sTranslatedQuery += s_sMaxRecords;
				sTranslatedQuery += iLimit;
				sTranslatedQuery += "\" ";
			}

			sTranslatedQuery += s_sRemainingPartOfPrefix;
		} catch (Exception oE) {
			WasdiLog.debugLog("QueryTranslatorEODC.parseCommon( " + sQuery + " ): " + oE);
		}
		return sTranslatedQuery;
	}

	/**
	 * @param sQuery
	 * @param sKeyword
	 * @param iLimit
	 * @return
	 */
	@Override
	protected int readInt(String sQuery, String sKeyword) {
		int iStart;
		int iEnd;
		int iTemp = -1;
		if(sQuery.contains(sKeyword)) {
			iStart = sQuery.indexOf(sKeyword);
			if(iStart <  0) {
				throw new IllegalArgumentException("Couldn't find limit start");
			}
			iStart += sKeyword.length();
			iEnd = iStart;
			while(iEnd < sQuery.length() && Character.isDigit(sQuery.charAt(iEnd))) {
				++iEnd;
			}
			String sTemp = sQuery.substring(iStart, iEnd);
			iTemp = Integer.parseInt(sTemp); 
		}
		return iTemp;
	}

	private String parseSentinel_2(String sQuery) {
		String sSentinel2Query = "";
		try {
			if(sQuery.contains(QueryTranslatorEODC.s_sPLATFORMNAME_SENTINEL_2)) {
				int iStart = sQuery.indexOf(s_sPLATFORMNAME_SENTINEL_2);
				if(iStart < 0) {
					throw new IllegalArgumentException("Could not find the initial index");
				}
				iStart += s_sPLATFORMNAME_SENTINEL_2.length();
				int iEnd = sQuery.indexOf(')', iStart);
				if(iEnd < 0 ) {
					sQuery = sQuery.substring(iStart);
				} else {
					sQuery = sQuery.substring(iStart, iEnd);
				}

				// P.Campanella 2020 07 13: changed the filter for a bug of EODC. Note that 
				// EODC has only S2 MSIL1C data
				//sSentinel2Query += "<ogc:PropertyIsEqualTo><ogc:PropertyName>eodc:platform</ogc:PropertyName><ogc:Literal>Sentinel-2</ogc:Literal></ogc:PropertyIsEqualTo>";

				sSentinel2Query += "<ogc:PropertyIsLike wildCard=\"%\" singleChar=\"#\" escapeChar=\"!\"><ogc:PropertyName>apiso:Identifier</ogc:PropertyName><ogc:Literal>";
				sSentinel2Query += "S2%MSIL1C%";
				sSentinel2Query += "</ogc:Literal></ogc:PropertyIsLike>";				

				//check for cloud coverage
				try {
					if(sQuery.contains(QueryTranslatorEODC.s_sCLOUDCOVERPERCENTAGE)) {
						iStart = sQuery.indexOf(s_sCLOUDCOVERPERCENTAGE);
						if(iStart < 0) {
							throw new IllegalArgumentException("Could not find cloud cover");
						}
						// +1 for the '['
						//iStart += s_sCLOUDCOVERPERCENTAGE.length() + 1;
						//p.campanella 2020 03 06: no, the '[' is already in the static text!
						iStart += s_sCLOUDCOVERPERCENTAGE.length();
						iEnd = sQuery.indexOf(']', iStart);
						String sSubQuery = sQuery.substring(iStart, iEnd);

						String[] asCloudLimits = sSubQuery.split(" TO ");

						//these variables could be omitted, but in this way we check we are reading numbers
						double dLo = Double.parseDouble(asCloudLimits[0]);
						double dUp = Double.parseDouble(asCloudLimits[1]);

						sSentinel2Query += "<ogc:PropertyIsGreaterThanOrEqualTo><ogc:PropertyName>eodc:cloud_coverage</ogc:PropertyName><ogc:Literal>";
						sSentinel2Query += dLo;
						sSentinel2Query += "</ogc:Literal></ogc:PropertyIsGreaterThanOrEqualTo><ogc:PropertyIsLessThanOrEqualTo><ogc:PropertyName>eodc:cloud_coverage</ogc:PropertyName><ogc:Literal>";
						sSentinel2Query += dUp;
						sSentinel2Query += "</ogc:Literal></ogc:PropertyIsLessThanOrEqualTo>";					
					}
				} catch (Exception oE) {
					WasdiLog.debugLog("QueryTranslatorEODC.parseSentinel_2( " + sQuery + " ): could not parse cloud coverage: " + oE);
				}
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("QueryTranslatorEODC.parseSentinel_2( " + sQuery + " ): " + oE);
		}
		return sSentinel2Query;
	}

	private String parseSentinel_1(String sQuery) {
		String sSentinel1Query = "";
		try {
			if(sQuery.contains(QueryTranslatorEODC.s_sPLATFORMNAME_SENTINEL_1)) {
				int iStart = sQuery.indexOf(s_sPLATFORMNAME_SENTINEL_1);
				if(iStart < 0) {
					throw new IllegalArgumentException("Could not find the initial index");
				}
				iStart += s_sPLATFORMNAME_SENTINEL_1.length();
				int iEnd = sQuery.indexOf(')', iStart);
				if(iEnd < 0) {
					sQuery = sQuery.substring(iStart);
				} else {
					sQuery = sQuery.substring(iStart, iEnd);
				}
				sQuery = sQuery.trim();

				// Changed the solution: query for name like and not on platform type
				//sSentinel1Query += "<ogc:PropertyIsEqualTo><ogc:PropertyName>eodc:platform</ogc:PropertyName><ogc:Literal>Sentinel-1</ogc:Literal></ogc:PropertyIsEqualTo>";

				//check for product type
				try {
					//ask for GRD by default, since it's the only type they support, at least so far
					sSentinel1Query += "<ogc:PropertyIsLike wildCard=\"%\" singleChar=\"#\" escapeChar=\"!\"><ogc:PropertyName>apiso:Identifier</ogc:PropertyName><ogc:Literal>";
					sSentinel1Query += "%GRDH%";
					sSentinel1Query += "</ogc:Literal></ogc:PropertyIsLike>";
				} catch (Exception oE) {
					WasdiLog.debugLog("QueryTranslatorEODC.parseSentinel_1( " + sQuery + " ): error while parsing product type: " + oE);
				}

				//check for relative orbit
				if(sQuery.contains(QueryTranslatorEODC.s_sRELATIVEORBITNUMBER)) {
					try {
						iStart = sQuery.indexOf(s_sRELATIVEORBITNUMBER);
						if(iStart < 0) {
							throw new IllegalArgumentException("Could not find relative orbit number");
						}
						iStart += s_sRELATIVEORBITNUMBER.length();
						iEnd = sQuery.indexOf(" AND ", iStart);
						if(iEnd < 0) {
							iEnd = sQuery.indexOf(')');
						}
						if(iEnd < 0) {
							iEnd = sQuery.indexOf(' ', iStart);
						}
						if(iEnd < 0) {
							//if anything else failed, skip digits
							iEnd = iStart;
							while(iEnd < sQuery.length() && Character.isDigit(sQuery.charAt(iEnd))) {
								++iEnd;
							}
						}
						String sOrbit = sQuery.substring(iStart, iEnd);
						int iOrbit = Integer.parseInt(sOrbit); 
						sSentinel1Query += "<ogc:PropertyIsEqualTo><ogc:PropertyName>eodc:rel_orbit_number</ogc:PropertyName><ogc:Literal>";
						sSentinel1Query += iOrbit;
						sSentinel1Query += "</ogc:Literal></ogc:PropertyIsEqualTo>";
					} catch (Exception oE) {
						WasdiLog.debugLog("QueryTranslatorEODC.parseSentinel_1(" + sQuery + " ): error while parsing relative orbit: " + oE );
					}
				}
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("QueryTranslatorEODC.parseSentinel_1( " + sQuery + " ): " + oE);
		}
		return sSentinel1Query;
	}

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.DiasQueryTranslator#parseTimeFrame(java.lang.String)
	 */
	@Override
	protected String parseTimeFrame(String sQuery) {

		String[] asInterval = {null, null};

		//beginPosition:[2020-01-30T00:00:00.000Z TO 2020-02-06T23:59:59.999Z]
		String sKeyword = "beginPosition";
		parseInterval(sQuery, sKeyword, asInterval);
		String sStart = asInterval[0];
		String sEnd = asInterval[1];

		//endPosition:[2020-01-30T00:00:00.000Z TO 2020-02-06T23:59:59.999Z]
		sKeyword = "endPosition";
		parseInterval(sQuery, sKeyword, asInterval);
		if(Utils.isNullOrEmpty(sStart) && !Utils.isNullOrEmpty(asInterval[0])) {
			sStart = asInterval[0];
		}
		if(Utils.isNullOrEmpty(sEnd) && !Utils.isNullOrEmpty(asInterval[1])) {
			sEnd = asInterval[1];
		}

		String sTranslatedTimeFrame = "";
		if(!Utils.isNullOrEmpty(sStart)) {
			sTranslatedTimeFrame += "<ogc:PropertyIsGreaterThanOrEqualTo><ogc:PropertyName>apiso:TempExtent_begin</ogc:PropertyName><ogc:Literal>";
			sTranslatedTimeFrame += sStart;
			sTranslatedTimeFrame += "</ogc:Literal></ogc:PropertyIsGreaterThanOrEqualTo>";
		}
		if(!Utils.isNullOrEmpty(sEnd)) {
			sTranslatedTimeFrame += "<ogc:PropertyIsLessThanOrEqualTo><ogc:PropertyName>apiso:TempExtent_end</ogc:PropertyName><ogc:Literal>";
			sTranslatedTimeFrame += sEnd;
			sTranslatedTimeFrame += "</ogc:Literal></ogc:PropertyIsLessThanOrEqualTo>";
		}

		//todo add interval

		return sTranslatedTimeFrame;
	}

	/**
	 * @param sQuery
	 * @param sKeyword
	 * @param alStartEnd
	 */
	@Override
	protected void parseInterval(String sQuery, String sKeyword, String[] asInterval) {
		Preconditions.checkNotNull(sQuery, "QueryTranslatorEODC.parseInterval: query is null");
		Preconditions.checkNotNull(sKeyword, "QueryTranslatorEODC.parseInterval: field keyword is null");
		Preconditions.checkNotNull(asInterval, "QueryTranslatorEODC.parseInterval: String array is null");
		Preconditions.checkElementIndex(0, asInterval.length, "QueryTranslatorEODC.parseInterval: 0 is not a valid element index");
		Preconditions.checkElementIndex(1, asInterval.length, "QueryTranslatorEODC.parseInterval: 1 is not a valid element index");

		if( sQuery.contains(sKeyword)) {
			int iStart = Math.max(0, sQuery.indexOf(sKeyword));
			iStart = Math.max(iStart, sQuery.indexOf('[', iStart) + 1);
			int iEnd = sQuery.indexOf(']', iStart);
			if(iEnd < 0) {
				iEnd = sQuery.length()-1;
			};
			String[] asTimeQuery= sQuery.substring(iStart, iEnd).trim().split(" TO ");
			asInterval[0] = asTimeQuery[0];
			asInterval[1] = asTimeQuery[1];
		}
	}

	@Override
	protected String parseProductName(String sQuery) {
		String sResult = "";
		try {
			String sFreeText = getProductName(sQuery);
			if(!Utils.isNullOrEmpty(sFreeText)) {
				sResult += "<ogc:PropertyIsEqualTo><ogc:PropertyName>apiso:Identifier</ogc:PropertyName><ogc:Literal>";
				sResult += sFreeText;
				sResult += "</ogc:Literal></ogc:PropertyIsEqualTo>";
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("QueryTranslatorEODC.parseFreeText( " + sQuery + " ): " + oE);
		}
		return sResult;
	}

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.DiasQueryTranslator#parseFootPrint(java.lang.String)
	 */
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
							WasdiLog.errorLog("QueryTranslatorEODC.parseFootprint: issue with current coordinate pair: " + sPair + ": ", oE);
						}
					}
					//todo check coordinates are within bounds
					if(
							-90 <= dNorth && 90 >= dNorth &&
							-90 <= dSouth && 90 >= dSouth &&
							-180 <= dEast && 180 >= dEast &&
							-180 <= dWest && 180 >= dWest
							) {
						sResult += "<ogc:BBOX><ogc:PropertyName>ows:BoundingBox</ogc:PropertyName><gml:Envelope><gml:lowerCorner>";
						sResult += dSouth + " " + dWest;
						sResult += "</gml:lowerCorner><gml:upperCorner>";
						sResult += dNorth + " " + dEast;
						sResult += "</gml:upperCorner></gml:Envelope></ogc:BBOX>";
					}

				} catch (Exception oE) {
					WasdiLog.errorLog("QueryTranslatorEODC.parseFootprint: could not complete: ", oE);
				}
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("QueryTranslatorEODC.parseFootprint: could not identify footprint substring limits: ", oE);
		}
		return sResult;
	}

	public static void main(String[] args) {
		QueryTranslatorEODC oEODC = new QueryTranslatorEODC();

		//S1
		//String sQuery = "( footprint:\"intersects(POLYGON((92.36417183697604 12.654592055231863,92.36417183697604 26.282214356266774,99.48157676962991 26.282214356266774,99.48157676962991 12.654592055231863,92.36417183697604 12.654592055231863)))\" ) AND ( beginPosition:[2019-05-01T00:00:00.000Z TO 2020-04-27T23:59:59.999Z] AND endPosition:[2019-05-01T00:00:00.000Z TO 2020-04-27T23:59:59.999Z] ) AND   (platformname:Sentinel-1 AND producttype:GRD AND relativeorbitnumber:33)&offset=21&limit=8";
		//String sResult = oEODC.translate(sQuery);
		//System.out.println(sResult);

		//S2
		//sQuery = "( footprint:\"intersects(POLYGON((92.36417183697604 12.654592055231863,92.36417183697604 26.282214356266774,99.48157676962991 26.282214356266774,99.48157676962991 12.654592055231863,92.36417183697604 12.654592055231863)))\" ) AND ( beginPosition:[2019-05-01T00:00:00.000Z TO 2020-04-27T23:59:59.999Z] AND endPosition:[2019-05-01T00:00:00.000Z TO 2020-04-27T23:59:59.999Z] ) AND   (platformname:Sentinel-2 AND cloudcoverpercentage:[10 TO 40])&offset=11&limit=5";
		String sQuery = "( footprint:\"intersects(POLYGON((4.772092603111307 46.20728338994605,4.772092603111307 47.17151988062961,7.057722358671709 47.17151988062961,7.057722358671709 46.20728338994605,4.772092603111307 46.20728338994605)))\" ) AND ( beginPosition:[2020-06-16T00:00:00.000Z TO 2020-06-23T23:59:59.999Z] AND endPosition:[2020-06-16T00:00:00.000Z TO 2020-06-23T23:59:59.999Z] ) AND   (platformname:Sentinel-2 AND cloudcoverpercentage:[0 TO 9.4])";
		String sResult = oEODC.translate(sQuery);
		System.out.println(sResult);

	}

	@Override
	public String getCountUrl(String sQuery) {
		
		if(sQuery.contains(s_sOFFSET)) {
			sQuery = sQuery.replace(s_sOFFSET, "OFFSETAUTOMATICALLYREMOVED=");
		}
		if(sQuery.contains(s_sLIMIT)) {
			sQuery = sQuery.replace(s_sLIMIT, "LIMITAUTOMATICALLYREMOVED=");
		}
		//we do not need results now, just the count
		sQuery += "&limit=0";
		
		String sTranslatedQuery = translate(sQuery);
		sTranslatedQuery = sTranslatedQuery.replace("<csw:ElementSetName>full</csw:ElementSetName>", "<csw:ElementSetName>brief</csw:ElementSetName>");
		
		WasdiLog.debugLog("EODC Payload:");
		WasdiLog.debugLog(sTranslatedQuery);
		
		return sTranslatedQuery;
	}

	@Override
	public String getSearchUrl(PaginatedQuery oQuery) {
		// TODO Auto-generated method stub
		return null;
	}

}
