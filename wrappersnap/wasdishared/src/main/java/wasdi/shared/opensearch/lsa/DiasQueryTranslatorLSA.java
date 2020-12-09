package wasdi.shared.opensearch.lsa;

import wasdi.shared.opensearch.DiasQueryTranslator;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.QueryViewModel;

public class DiasQueryTranslatorLSA extends DiasQueryTranslator {

	@Override
	protected String translate(String sQueryFromClient) {
		
		// Ouptut Query
		String sLSAQuery = "";
		
		// Convert the WASDI Query in the view model
		QueryViewModel oWasdiQuery = parseWasdiClientQuery(sQueryFromClient);
				
		// Set start and end date
		String sTimeStart = oWasdiQuery.startFromDate.substring(0, 10);
		String sTimeEnd = oWasdiQuery.endToDate.substring(0, 10);;
		
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
			sBbox = "&box=" + oWasdiQuery.west + "," + oWasdiQuery.south + "," + oWasdiQuery.east + "," + oWasdiQuery.north;
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
			
			String sCloudCoverage = "&cloudCover=[" + iFrom + "," + iTo+"]"; 
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
		
		if (oWasdiQuery.platformName.equals("Sentinel-1")) {
			if (Utils.isNullOrEmpty(oWasdiQuery.productType)) {
				sParentId = "S1_SAR_GRD";
			}
			else {
				sParentId = "S1_SAR_" + oWasdiQuery.productType;
			}
			
			if (sParentId.contains("GRD")) {
				sParentId += "&processingMode=Fast-24h";
			}
		}
		else if (oWasdiQuery.platformName.equals("Sentinel-2")) {
			if (Utils.isNullOrEmpty(oWasdiQuery.productType)) {
				sParentId = "S2_MSIL2A";
			}
			else {
				sParentId = "S2_" + oWasdiQuery.productType;
			}
		}
		
		String sBaseAddress = "https://collgs.lu/catalog/oseo/search?parentId=" + sParentId;
		
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

}
