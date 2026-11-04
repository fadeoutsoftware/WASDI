package wasdi.shared.queryexecutors.meteogate;

import static wasdi.shared.utils.HttpUtils.httpGet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;

//import com.fasterxml.jackson.databind.ObjectMapper;

import wasdi.shared.queryexecutors.PaginatedQuery;

import wasdi.shared.queryexecutors.ogcedr.EDRQueryExecutor;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

public class QueryExecutorMeteoGate extends EDRQueryExecutor{

    String m_sMeteoGateBaseUrl;

    public QueryExecutorMeteoGate() {
		m_oQueryTranslator = new QueryTranslatorMeteoGate();
		m_oResponseTranslator = new ResponseTranslatorMeteoGate();
	}

    @Override 
    public void init() {
        this.m_sMeteoGateBaseUrl = this.m_oDataProviderConfig.link;
    }

    private JSONArray getStations(QueryViewModel oQueryVM) {
        /** 
         * Query the WMO OSCAR Surface service to get a list of stations 
        */

        try {

            // for the moment, we only consider the bbox
            // TODO: understad which other parameters should be included 

            StringBuilder sQueryParametersBuilder = new StringBuilder();

            Double dNorth = oQueryVM.north;
            Double dSouth = oQueryVM.south;
            Double dEast = oQueryVM.east;
            Double dWest = oQueryVM.west;

            if (dNorth == null || dSouth == null || dEast == null || dWest == null) {
                WasdiLog.warnLog("QueryExecutorMeteoGate.getStations. Bounding box not fully specified");
                return null;
            }

            sQueryParametersBuilder.append("bbox=" + dWest + "," + dSouth+"," + dEast+"," + dNorth);

            String sDateFrom = oQueryVM.startFromDate;
            String sDateTo = oQueryVM.endToDate;

            if (sDateFrom == null && sDateTo == null) {
                WasdiLog.warnLog("QueryExecutorMeteoGate.getStations. Dates not specified");
                return null;
            }

            sQueryParametersBuilder.append("&datetime=" + sDateFrom + "/" + sDateTo);

            String sObservation = oQueryVM.productType;

            if (sObservation == null) {
                WasdiLog.warnLog("QueryExecutorMeteoGate.getStations. Observation not specified");
                return null;
            }

            sQueryParametersBuilder.append("&standard_name=" + sObservation);

            String sUrl = this.m_sMeteoGateBaseUrl + "?" + sQueryParametersBuilder.toString();

            HttpCallResponse oResponse = httpGet(sUrl);

            if (oResponse == null) {
                WasdiLog.warnLog("QueryExecutorMeteoGate.getStations. HTTP response is null");
                return null;
            }

            int iResponseCode = oResponse.getResponseCode();
            WasdiLog.debugLog("QueryExecutorMeteoGate.getStations. Response code: " + iResponseCode);

            if (iResponseCode != 200) {
                WasdiLog.warnLog("QueryExecutorMeteoGate.getStations. HTTP call failed with code " + iResponseCode);
                return null;
            }

            JSONArray aoStations = new JSONArray(oResponse.getResponseBody());
            return aoStations;
        
        } catch (Exception oE) {
            WasdiLog.errorLog("QueryExecutorMeteoGate.getStations: Exception", oE);
            return null;
        }
    }

    @Override
    public int executeCount(String sQuery) {

        int iCount = -1;

        try {

            WasdiLog.debugLog("QueryExecutorMeteoGate.executeCount. Received query: " + sQuery);
		
		    QueryViewModel oQueryVM = m_oQueryTranslator.parseWasdiClientQuery(sQuery);
		
            if (!m_asSupportedPlatforms.contains(oQueryVM.platformName)) {
                WasdiLog.debugLog("QueryExecutorMeteoGate.executeCount. Plaform not supported by the data provider for query: " + sQuery);
                return iCount;
            }

            JSONArray aoStations = getStations(oQueryVM);

            if (aoStations != null) {
                iCount = aoStations.length();
                WasdiLog.debugLog("QueryExecutorMeteoGate.executeCount. Count: " + iCount);
            }

        } catch (Exception oE) {
            WasdiLog.errorLog("MeteoGateQueryExecutor.executeCount: Exception", oE);
        }
        return iCount;
    }

    @Override
    public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {

        try {
        
            String sQuery = oQuery.getQuery();
            QueryViewModel oQueryVM = m_oQueryTranslator.parseWasdiClientQuery(sQuery);

            if (!m_asSupportedPlatforms.contains(oQueryVM.platformName)) {
                WasdiLog.debugLog("QueryExecutorMeteoGate.executeCount. Plaform not supported by the data provider for query: " + sQuery);
                return null;
            }

            JSONArray aoStations = getStations(oQueryVM);

            if (aoStations == null) {
                WasdiLog.debugLog("QueryExecutorMeteoGate.executeAndRetrieve. No stations found");
                return null;
            }

            // now I need to make the call to MeteoGate for look for getting the time series available 

                
        } catch (Exception oE) {
            WasdiLog.errorLog("QueryExecutorMeteoGate.executeAndRetrieve: Exception", oE);
        }
        return null;
    }


}
