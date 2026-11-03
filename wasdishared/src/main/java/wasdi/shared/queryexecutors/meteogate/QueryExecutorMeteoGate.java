package wasdi.shared.queryexecutors.meteogate;

import static wasdi.shared.utils.HttpUtils.httpPost;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import com.fasterxml.jackson.databind.ObjectMapper;

import wasdi.shared.queryexecutors.PaginatedQuery;

import wasdi.shared.queryexecutors.ogcedr.EDRQueryExecutor;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

public class QueryExecutorMeteoGate extends EDRQueryExecutor{

    public QueryExecutorMeteoGate() {
		m_oQueryTranslator = new QueryTranslatorMeteoGate();
		m_oResponseTranslator = new ResponseTranslatorMeteoGate();
	}

    @Override
    public int executeCount(String sQuery) {

        int iCount = -1;

        String sUrl = "http://oscar.wmo.int/surface/rest/api/search/stationSearch";

        try {

            WasdiLog.debugLog("QueryExecutorMeteoGate.executeCount. Received query: " + sQuery);
		
		    QueryViewModel oQueryVM = m_oQueryTranslator.parseWasdiClientQuery(sQuery);
		
            if (!m_asSupportedPlatforms.contains(oQueryVM.platformName)) {
                WasdiLog.debugLog("QueryExecutorMeteoGate.executeCount. Plaform not supported by the data provider for query: " + sQuery);
                return iCount;
            }

            StringBuilder oPayload = new StringBuilder();

            Double dNorth = oQueryVM.north;
            Double dSouth = oQueryVM.south;
            Double dEast = oQueryVM.east;
            Double dWest = oQueryVM.west;

            oPayload.append("{\"operation\":\"AND\"");

            if (dNorth != null && dSouth != null && dEast != null && dWest != null) {
                oPayload.append(",");
                oPayload.append("\"latitudeFrom\":\"" + dSouth.toString() + "\",");
                oPayload.append("\"latitudeTo\":\"" + dNorth.toString() + "\",");
                oPayload.append("\"longitudeFrom\":\"" + dWest.toString() + "\",");
                oPayload.append("\"longitudeTo\":\"" + dEast.toString() + "\"");
            }

            oPayload.append(",");
            oPayload.append("\"nrt\":false");
            oPayload.append(",\"quickSearch\":false"); 
            oPayload.append(",\"operatingStatusId\":10");
            oPayload.append(",\"assessedStatusId\":10");
            oPayload.append(",\"programApprovalStatusId\":5");
            oPayload.append(",\"stationTypeIdList\":[5,6,11]");
            oPayload.append(",\"filetype\":\"csv\"");
            oPayload.append(",\"stationProgramName\":null");
            oPayload.append(",\"variableName\":null");

            oPayload.append("}");

            String sPayload = oPayload.toString();

            WasdiLog.debugLog("QueryExecutorMeteoGate.executeCount. Payload: " + sPayload);

            Map<String, String> oHeaders = new HashMap<>();
            oHeaders.put("Content-Type", "application/json");

            HttpCallResponse oResponse = httpPost(sUrl, sPayload, oHeaders);

            if (oResponse == null) {
                WasdiLog.warnLog("QueryExecutorMeteoGate.executeCount. HTTP response is null");
                return iCount;
            }

            WasdiLog.debugLog("QueryExecutorMeteoGate.executeCount. Response code: " + oResponse.getResponseCode());

            /**
            String sPayload = "{\"operation\":\"AND\",\"nrt\":false,\"quickSearch\":false,\"operatingStatusId\":10,\"assessedStatusId\":10,\"programApprovalStatusId\":5,\"stationTypeIdList\":[5,6,11,12,1,2,9,3,4,10,7,8],\"latitudeTo\":\"50\",\"latitudeFrom\":\"46\",\"longitudeFrom\":\"5\",\"longitudeTo\":\"6\",\"filetype\":\"csv\",\"stationProgramName\":null,\"variableName\":null}";
            
            HttpCallResponse oResponse = HttpUtils.httpPost(sUrl, sPayload);
            
            if (oResponse != null && oResponse.getResponseCode() == 200) {
                String sResponseBody = oResponse.getResponseBody();
                
                ObjectMapper oMapper = new ObjectMapper();
                Object oResult = oMapper.readValue(sResponseBody, Object.class);
                
                // Extract count from response - adjust based on actual response structure
                if (oResult instanceof java.util.Map) {
                    java.util.Map<?, ?> oMap = (java.util.Map<?, ?>) oResult;
                    if (oMap.containsKey("count")) {
                        return ((Number) oMap.get("count")).intValue();
                    }
                }
                
                WasdiLog.warnLog("MeteoGateQueryExecutor.executeCount: Unable to extract count from response");
                return 0;
            } else {
                WasdiLog.errorLog("MeteoGateQueryExecutor.executeCount: HTTP call failed with code " + 
                    (oResponse != null ? oResponse.getResponseCode() : "null"));
                return 0;
            }
            */
        } catch (Exception oE) {
            WasdiLog.errorLog("MeteoGateQueryExecutor.executeCount: Exception", oE);
        }
        return iCount;
    }

    @Override
    public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'executeAndRetrieve'");
    }


}
