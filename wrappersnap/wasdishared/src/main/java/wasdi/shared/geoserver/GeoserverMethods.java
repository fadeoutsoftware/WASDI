package wasdi.shared.geoserver;

import org.apache.log4j.Logger;
import wasdi.shared.utils.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

/**
 * Created by s.adamo on 14/02/2017.
 */
public class GeoserverMethods {

    static Logger s_oLogger = Logger.getLogger(GeoserverMethods.class);

    public static String GEOSERVER_ADDRESS = "http://178.22.66.96:8080/geoserver";
    public static String GEOSERVER_WORKSPACE = "wasdi";
    public static String GEOSERVER_USER = "admin";
    public static String GEOSERVER_PASSWORD = "geoserver";


    public static String DeleteLayer(String sLayerId, String sFormat)
    {
        StringBuffer response = new StringBuffer();
        String sBBox = "";

        if (Utils.isNullOrEmpty(sLayerId))
            return sBBox;

        if (Utils.isNullOrEmpty(sFormat))
            sFormat = "json";

        try {
            String sUrl = GEOSERVER_ADDRESS + "/rest/layers/" + sLayerId + "." + sFormat;

            s_oLogger.debug("GeoserverUtils.DeleteLayer: Geoserver url: " + sUrl);

            final String USER_AGENT = "Mozilla/5.0";

            URL oUrl = new URL(sUrl);
            HttpURLConnection oConn = (HttpURLConnection) oUrl.openConnection();

            // optional default is GET
            oConn.setRequestMethod("DELETE");

            //add request header
            oConn.setRequestProperty("User-Agent", USER_AGENT);
            String userCredentials = GEOSERVER_USER + ":" + GEOSERVER_PASSWORD;
            String basicAuth = "Basic " + Base64.getEncoder().encodeToString(userCredentials.getBytes("UTF-8"));
            oConn.setRequestProperty ("Authorization", basicAuth);

            int iResponseCode = oConn.getResponseCode();

            if (iResponseCode == 200) {
                System.out.println("GeoserverUtils.DeleteLayer: Connection to geoserver ok");
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(oConn.getInputStream()));
                String inputLine;
                s_oLogger.debug("GeoserverUtils.DeleteLayer: get input stream " + response.toString());

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

            }
            s_oLogger.debug("GeoserverUtils.DeleteLayer: response " + response.toString());

        } catch (IOException oEx) {
            oEx.printStackTrace();
            s_oLogger.debug("GeoserverUtils.DeleteLayer: Exception deleting layer " + oEx.toString());
            try {
                throw oEx;
            } catch (IOException oIOEx) {
                oIOEx.printStackTrace();
            }
        }

        return response.toString();

    }
}
