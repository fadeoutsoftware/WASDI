package wasdi.shared.queryexecutors.lsa;

import java.time.LocalDate;

import wasdi.shared.queryexecutors.QueryTranslator;
import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.search.QueryViewModel;

public class QueryTranslatorLSA extends QueryTranslator {
	
	private boolean m_bEnableFast24 = true;

	public boolean getEnableFast24() {
		return m_bEnableFast24;
	}


	public void setEnableFast24(boolean bEnableFast24) {
		this.m_bEnableFast24 = bEnableFast24;
	}


	@Override
	protected String translate(String sQueryFromClient) {
		
		// Ouptut Query
		String sLSAQuery = "";
		
		// Convert the WASDI Query in the view model
		QueryViewModel oWasdiQuery = parseWasdiClientQuery(sQueryFromClient);
		
		if (!Utils.isNullOrEmpty(oWasdiQuery.platformName)) {
			if (oWasdiQuery.platformName.equals("Sentinel-1") == false && oWasdiQuery.platformName.equals("Sentinel-2") == false && oWasdiQuery.platformName.isEmpty() == false) return "";
		}
		
		// Set start and end date
		String sTimeStart = oWasdiQuery.startFromDate.substring(0, 10);
		String sTimeEnd = oWasdiQuery.endToDate.substring(0, 10);
		//increment the end day by one, because the upper limit is excluded:
		//apparently LSA uses it as a <, not as a <=
		sTimeEnd = LocalDate.parse(sTimeEnd).plusDays(1).toString();

		
		String sTimePeriod = "&timeStart=" + sTimeStart + "&timeEnd="+ sTimeEnd;
		
		sLSAQuery = sLSAQuery + sTimePeriod;
				
		if (oWasdiQuery.limit>0) {
			String sCount = "&count=" + oWasdiQuery.limit;
			sLSAQuery = sCount + sTimePeriod;
		}
		
		// Set the start index:
		if (oWasdiQuery.offset>=0) {
			int iOffset = oWasdiQuery.offset + 1;
			String sOffset= "&startIndex="+	iOffset;
			sLSAQuery = sOffset + sLSAQuery;
		}
		
		// Set the Bbox
		String sBbox="";
		
		if (oWasdiQuery.north!=null && oWasdiQuery.south!=null && oWasdiQuery.east!=null && oWasdiQuery.west!=null) {
			// P.Campanella 16/02/2021: change geographic filter to avoid geoserver intersection bug.
			//sBbox = "&box=" + oWasdiQuery.west + "," + oWasdiQuery.south + "," + oWasdiQuery.east + "," + oWasdiQuery.north;
			sBbox = "&geometry=POLYGON((" + oWasdiQuery.west  +" " + oWasdiQuery.south + ", " + oWasdiQuery.east +" " + oWasdiQuery.south + ", " + oWasdiQuery.east + " " + oWasdiQuery.north + ", " + oWasdiQuery.west + " " + oWasdiQuery.north+ ", " + oWasdiQuery.west  +" " + oWasdiQuery.south + "))";
		}
		
		sLSAQuery += sBbox;
		
		// Set Sensor Mode
		if (Utils.isNullOrEmpty(oWasdiQuery.sensorMode) == false) {
			String sSensorMode = "&SensorMode=" + oWasdiQuery.sensorMode;
			sLSAQuery += sSensorMode;
		}
		
		// Set Cloud Coverage
		if (oWasdiQuery.cloudCoverageFrom != null && oWasdiQuery.cloudCoverageTo != null) {
			int iFrom = oWasdiQuery.cloudCoverageFrom.intValue();
			int iTo = oWasdiQuery.cloudCoverageTo.intValue();
			
			String sCloudCoverage = "&cloudCover=%5B" + iFrom + "," + iTo+"%5D"; 
			sLSAQuery += sCloudCoverage;
		}
		
		// Set Relative Orbit
		if (oWasdiQuery.relativeOrbit >=0) {
			String sOrbitNumber = "&orbitNumber=" + oWasdiQuery.relativeOrbit;
			sLSAQuery += sOrbitNumber;
		}
		
		// Set Parent Id
		// S1_SAR_RAW, S1_SAR_SLC, S1_SAR_GRD, S1_SAR_OCN 
		// S2_MSIL1C, S2_MSIL2A
		String sParentId = "";		
		
		if (oWasdiQuery.platformName != null) {
			if (oWasdiQuery.platformName.equals("Sentinel-1")) {
				if (Utils.isNullOrEmpty(oWasdiQuery.productType)) {
					
					sParentId = "S1_SAR_GRD";
					
					if (!Utils.isNullOrEmpty(oWasdiQuery.productName)) {
						if (oWasdiQuery.productName.contains("_SLC_")) {
							sParentId = "S1_SAR_SLC";
						}
						else if (oWasdiQuery.productName.contains("_OCN_")) {
							sParentId = "S1_SAR_OCN";
						} 
					}
					
				}
				else {
					sParentId = "S1_SAR_" + oWasdiQuery.productType;
				}
				
				if (sParentId.contains("GRD")) {
					if (m_bEnableFast24) {
						sParentId += "&processingMode=Fast-24h";
					}
				}
			}
			else if (oWasdiQuery.platformName.equals("Sentinel-2")) {
				if (Utils.isNullOrEmpty(oWasdiQuery.productType)) {
					sParentId = "S2_MSIL2A";
					
					if (!Utils.isNullOrEmpty(oWasdiQuery.productName)) {
						if (oWasdiQuery.productName.contains("_MSIL1C_")) {
							sParentId = "S2_MSIL1C";
						}
					}
				}
				else {
					if(oWasdiQuery.productType.equals("S2MSI2A")) {
						sParentId = "S2_MSIL2A";
					} else if(oWasdiQuery.productType.equals("S2MSI1C")) {
						sParentId = "S2_MSIL1C";
					} else {	
						sParentId = "S2_MSIL2A";
					}
				}
			}			
		}
		
		//add free text search, assuming it's the product id
		if(!Utils.isNullOrEmpty(oWasdiQuery.productName)) {
			sLSAQuery += "&uid=" + oWasdiQuery.productName;
		}
		
		String sBaseAddress = "https://collgs.lu/catalog/oseo/search?parentIdentifier=" + sParentId;
		
		sLSAQuery = sBaseAddress + sLSAQuery;
		
		return sLSAQuery;
	}

	
	@Override
	protected String parseTimeFrame(String sQuery) {
		return null;
	}

	@Override
	protected String parseFootPrint(String sQuery) {
		return null;
	}


	@Override
	public String getCountUrl(String oQuery) {
		return null;
	}


	@Override
	public String getSearchUrl(PaginatedQuery oQuery) {
		return null;
	}

}
