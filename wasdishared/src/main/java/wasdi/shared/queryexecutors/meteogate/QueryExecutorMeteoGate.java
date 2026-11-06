package wasdi.shared.queryexecutors.meteogate;

import static wasdi.shared.utils.HttpUtils.httpGet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

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

    private JSONObject getStations(QueryViewModel oQueryVM) {
        /** 
         * Query MeteoGate to get a list of stations 
        */

        try {

            // for the moment, we only consider the bbox
            // TODO: understad which other parameters should be included 

            StringBuilder sQueryParametersBuilder = new StringBuilder();

            Double dNorth = oQueryVM.north;
            Double dSouth = oQueryVM.south;
            Double dEast = oQueryVM.east;
            Double dWest = oQueryVM.west;

            // mandatory bbox
            if (dNorth == null || dSouth == null || dEast == null || dWest == null) {
                WasdiLog.warnLog("QueryExecutorMeteoGate.getStations. Bounding box not fully specified");
                return null;
            }

            sQueryParametersBuilder.append("bbox=" + dWest + "," + dSouth+"," + dEast+"," + dNorth);

            String sDateFrom = oQueryVM.startFromDate;
            String sDateTo = oQueryVM.endToDate;

            // mandatory dates (usually only date close to the present moment are supported)
            // TODO: understand if only dates in the past are supported or also in the near future
            if (sDateFrom == null && sDateTo == null) {
                WasdiLog.warnLog("QueryExecutorMeteoGate.getStations. Dates not specified");
                return null;
            }

            sQueryParametersBuilder.append("&datetime=" + sDateFrom + "/" + sDateTo);

            // TODO: remove once the check is done
            WasdiLog.debugLog("QueryExecutorMeteoGate.getStations. Start date " + sDateFrom + ", end date: " + sDateTo);

            String sObservation = oQueryVM.productType;

            if (sObservation == null) {
                WasdiLog.warnLog("QueryExecutorMeteoGate.getStations. Observation not specified");
                return null;
            }

            sQueryParametersBuilder.append("&standard_name=" + sObservation);

            String sUrl = this.m_sMeteoGateBaseUrl + "/collections/observations/locations?" + sQueryParametersBuilder.toString();
            WasdiLog.debugLog("QueryExecutorMeteoGate.getStations. URL: " + sUrl);

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

            JSONObject aoStations = new JSONObject(oResponse.getResponseBody());
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

            JSONObject aoStations = getStations(oQueryVM);

            if (aoStations == null) {
                
                WasdiLog.warnLog("QueryExecutorMeteoGate.executeCount. No stations returned");
                return iCount;
            }

            JSONArray aoFeatures = aoStations.optJSONArray("features");

            if (aoFeatures == null) {
                WasdiLog.warnLog("QueryExecutorMeteoGate.executeCount. Array 'feature' not present in the JSON response");
                return iCount;
            }

            iCount = aoFeatures.length();
            WasdiLog.debugLog("QueryExecutorMeteoGate.executeCount. Count: " + iCount);
            

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

            JSONObject oStations = getStations(oQueryVM);

            if (oStations == null) {
                WasdiLog.debugLog("QueryExecutorMeteoGate.executeAndRetrieve. No stations found");
                return null;
            }

            // now I need to make the call to MeteoGate for look for getting the time series available
            JSONArray aoFeatures = oStations.optJSONArray("features");

            if (aoFeatures == null) {
                WasdiLog.warnLog("QueryExecutorMeteoGate.executeAndRetrieve. Array 'features' missing");
                return null;
            }

            WasdiLog.debugLog("QueryExecutorMeteoGate.executeAndRetrieve. Features count: " + aoFeatures.length());

            List<QueryResultViewModel> aoResults = new ArrayList<>();

            for (int i = 0; i < aoFeatures.length(); i++) {
                JSONObject oFeature = aoFeatures.optJSONObject(i);
                QueryResultViewModel oResult = new QueryResultViewModel();

                JSONObject oPropsJson = oFeature.optJSONObject("properties");
                if (oPropsJson == null) {
                    WasdiLog.warnLog("QueryExecutorMeteoGate.executeAndRetrieve. No properties, impossible to get info about station");
                    return null;
                }

                String sStationName =oPropsJson.optString("name");
                JSONArray aoParametersNames = oPropsJson.optJSONArray("parameter-name");
                String sTimeseriesLink = oPropsJson.optString("timeseries-link");
                JSONArray aoParameterNames = oPropsJson.optJSONArray("parameter-name");
                String sParameterName = "";
                if (aoParameterNames != null && !aoParameterNames.isEmpty()) sParameterName = aoParameterNames.optString(0);
                String sTimeseriesDataLink = getDataUrl(sTimeseriesLink, sParameterName);
                if (sTimeseriesDataLink == null) {
                    WasdiLog.warnLog("QueryExecutorMeteoGate.executeAndRetrieve. No data link for station " + sStationName);
                    continue;
                }
                String sWMOOscarStationId = oFeature.optString("id");

                if (aoParametersNames == null || aoParametersNames.isEmpty() || sTimeseriesLink == null) {
                    WasdiLog.warnLog("QueryExecutorMeteoGate.executeAndRetrieve. Observation not available for station " + sStationName + "(id: " + sWMOOscarStationId + ")");
                    continue;
                }

                String sObservation = oQueryVM.productType;
                String sStartDate = oQueryVM.startFromDate.substring(0, 10);
                String sEndDate = oQueryVM.endFromDate.substring(0, 10);
                String sWasdiId = sStationName + "_" + sObservation + "_" + sStartDate + "_" + sEndDate;

                JSONObject oGeometry = oFeature.optJSONObject("geometry");
                if (oGeometry==null || !oGeometry.optString("type").equals("Point")) {
                    WasdiLog.warnLog("QueryExecutorMeteoGate.executeAndRetrieve. Geometry not a point");
                    continue;
                }

                // TODO: controllare eventualmente che sia una bbox

                JSONArray aoCoords = oGeometry.optJSONArray("coordinates");
                Double dLongitude = aoCoords.getDouble(0);
                Double dLatitude = aoCoords.getDouble(1);

                // we transform a point to a small bbox around it
                Double dLonWest = dLongitude - 0.014;
                Double dLonEast = dLongitude + 0.014;
                Double dLatSouth = dLatitude - 0.009;
                Double dLatNorth = dLatitude + 0.009;

                String sFootprint = "POLYGON((" + 
                    dLonWest + " " + dLatSouth + ", " + 
                    dLonEast + " " + dLatSouth + ", " + 
                    dLonEast + " " + dLatNorth + ", " + 
                    dLonWest + " " + dLatNorth + ", " + 
                    dLonWest + " " + dLatSouth + 
                "))";

                oResult.setId(sWasdiId);
                oResult.setTitle(sWasdiId);
                oResult.setSummary("");
                oResult.setLink(sTimeseriesDataLink);       
                oResult.setFootprint(sFootprint);
                oResult.setPlatform("MeteoGate");
                oResult.setProvider("MeteoGate");
                Map<String, String> oProps = new HashMap<>();
                oProps.put("pointCoordinate", dLongitude + "," + dLatitude);
                oProps.put("sensoroperationalmode", sParameterName);
                oProps.put("WMOOscarId", sWMOOscarStationId);
                oResult.setProperties(oProps);
                
                aoResults.add(oResult);
            }

            return aoResults;

                
        } catch (Exception oE) {
            WasdiLog.errorLog("QueryExecutorMeteoGate.executeAndRetrieve: Exception", oE);
        }
        return null;
    }

    private String getDataUrl(String sUrl, String sParameterName) {

        String sDataUrl = "";

        try {

            HttpCallResponse oResponse =httpGet(sUrl);

            if (oResponse == null) {
                    WasdiLog.warnLog("QueryExecutorMeteoGate.getDataUrl. HTTP response is null");
                    return null;
                }

                int iResponseCode = oResponse.getResponseCode();
                WasdiLog.debugLog("QueryExecutorMeteoGate.getDataUrl. Response code: " + iResponseCode);

                if (iResponseCode != 200) {
                    WasdiLog.warnLog("QueryExecutorMeteoGate.getDataUrl. HTTP call failed with code " + iResponseCode);
                    return null;
                }

                JSONObject oObservationsItems = new JSONObject(oResponse.getResponseBody());
                JSONArray aoFeatures = oObservationsItems.optJSONArray("features");

                if (aoFeatures == null) {
                    WasdiLog.warnLog("QueryExecutorMeteoGate.getDataUrl. No features array");
                    return sDataUrl;
                }

                for (int i = 0; i < aoFeatures.length(); i++) {
                    JSONObject oFeature = aoFeatures.getJSONObject(i);
                    JSONObject oProperties = oFeature.optJSONObject("properties");
                    if (oProperties == null) continue;
                    String sParameterNameString = oProperties.optString("parameter_name");
                    if (sParameterNameString.equals(sParameterName)) {
                        sDataUrl = oProperties.optString("data");
                        if (sDataUrl.isEmpty()) continue;
                        break;
                    }
                }
        } catch (Exception oE) {
            WasdiLog.errorLog("QueryExecutorMeteoGate.getDataUrl: Exception", oE);
        }

        return sDataUrl;

    }



}
