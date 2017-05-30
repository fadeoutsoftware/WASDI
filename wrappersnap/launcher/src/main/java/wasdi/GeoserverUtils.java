package wasdi;

import org.json.JSONObject;
import wasdi.shared.utils.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;


/**
 * Created by s.adamo on 06/02/2017.
 */
public class GeoserverUtils {

    public static String GetBoundingBox(String sLayerId, String sFormat)
    {
        StringBuffer response = new StringBuffer();
        String sBBox = "";

        if (Utils.isNullOrEmpty(sLayerId))
            return sBBox;

        if (Utils.isNullOrEmpty(sFormat))
            sFormat = "json";

        try {
            String sUrl = ConfigReader.getPropValue("GEOSERVER_ADDRESS") + "/rest/workspaces/" + ConfigReader.getPropValue("GEOSERVER_WORKSPACE") + "/coveragestores/" + sLayerId + "/coverages/" + sLayerId + "." + sFormat;

            LauncherMain.s_oLogger.debug("GeoserverUtils.GetBoundingBox: Geoserver url: " + sUrl);

            final String USER_AGENT = "Mozilla/5.0";

            URL oUrl = new URL(sUrl);
            HttpURLConnection oConn = (HttpURLConnection) oUrl.openConnection();

            // optional default is GET
            oConn.setRequestMethod("GET");

            //add request header
            oConn.setRequestProperty("User-Agent", USER_AGENT);
            String userCredentials = ConfigReader.getPropValue("GEOSERVER_USER") + ":" + ConfigReader.getPropValue("GEOSERVER_PASSWORD");
            String basicAuth = "Basic " + Base64.getEncoder().encodeToString(userCredentials.getBytes("UTF-8"));
            oConn.setRequestProperty ("Authorization", basicAuth);

            int iResponseCode = oConn.getResponseCode();

            if (iResponseCode == 200) {
                LauncherMain.s_oLogger.debug("GeoserverUtils.GetBoundingBox: Connection to geoserver ok");
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(oConn.getInputStream()));
                String inputLine;
                LauncherMain.s_oLogger.debug("GeoserverUtils.GetBoundingBox: get input stream " + response.toString());

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

            }
            LauncherMain.s_oLogger.debug("GeoserverUtils.GetBoundingBox: response " + response.toString());
            String sResponse = response.toString();
            JSONObject oJSONObject = new JSONObject(sResponse);
            if (oJSONObject.has("coverage")) {
                LauncherMain.s_oLogger.debug("GeoserverUtils.GetBoundingBox: Coverage object found");
                JSONObject oCoverage = oJSONObject.getJSONObject("coverage");
                if (oCoverage != null && oCoverage.has("latLonBoundingBox")) {
                    LauncherMain.s_oLogger.debug("GeoserverUtils.GetBoundingBox: latLonBoundingBox object found");
                    JSONObject oBBox = oCoverage.getJSONObject("latLonBoundingBox");
                    sBBox = oBBox.toString();
                    LauncherMain.s_oLogger.debug("GeoserverUtils.GetBoundingBox: latLonBoundingBox " + sBBox);
                }
            }
            else {

                LauncherMain.s_oLogger.debug("GeoserverUtils.GetBoundingBox: Json: " + oJSONObject.toString());
                LauncherMain.s_oLogger.debug("GeoserverUtils.GetBoundingBox: Coverage not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            LauncherMain.s_oLogger.debug("GeoserverUtils.GetBoundingBox: Exception getting bounding box " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e));
        }
        finally {
            return sBBox.toString();
        }

    }

    public static String DeleteLayer(String sLayerId, String sFormat)
    {
        StringBuffer response = new StringBuffer();
        String sBBox = "";

        if (Utils.isNullOrEmpty(sLayerId))
            return sBBox;

        if (Utils.isNullOrEmpty(sFormat))
            sFormat = "json";

        try {
            String sUrl = ConfigReader.getPropValue("GEOSERVER_ADDRESS") + "/rest/layers/" + sLayerId + "." + sFormat;

            LauncherMain.s_oLogger.debug("GeoserverUtils.DeleteLayer: Geoserver url: " + sUrl);

            final String USER_AGENT = "Mozilla/5.0";

            URL oUrl = new URL(sUrl);
            HttpURLConnection oConn = (HttpURLConnection) oUrl.openConnection();

            // optional default is GET
            oConn.setRequestMethod("DELETE");

            //add request header
            oConn.setRequestProperty("User-Agent", USER_AGENT);
            String userCredentials = ConfigReader.getPropValue("GEOSERVER_USER") + ":" + ConfigReader.getPropValue("GEOSERVER_PASSWORD");
            String basicAuth = "Basic " + Base64.getEncoder().encodeToString(userCredentials.getBytes("UTF-8"));
            oConn.setRequestProperty ("Authorization", basicAuth);

            int iResponseCode = oConn.getResponseCode();

            if (iResponseCode == 200) {
                LauncherMain.s_oLogger.debug("GeoserverUtils.DeleteLayer: Connection to geoserver ok");
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(oConn.getInputStream()));
                String inputLine;
                LauncherMain.s_oLogger.debug("GeoserverUtils.DeleteLayer: get input stream " + response.toString());

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

            }
            LauncherMain.s_oLogger.debug("GeoserverUtils.DeleteLayer: response " + response.toString());

        } catch (Exception e) {
            e.printStackTrace();
            LauncherMain.s_oLogger.debug("GeoserverUtils.DeleteLayer: Exception deleting layer " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e));
        }
        finally {
            return response.toString();
        }

    }




}
