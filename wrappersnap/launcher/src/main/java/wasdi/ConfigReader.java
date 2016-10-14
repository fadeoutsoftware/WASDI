package wasdi; /**
 * Created by s.adamo on 23/09/2016.
 */

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

public class ConfigReader {

    static HashMap<String,String> m_aoProperties;

    private static void loadPropValues() throws IOException {

        InputStream inputStream = null;
        try {
            Properties prop = new Properties();
            String propFileName = "config.properties";

            inputStream = new ConfigReader().getClass().getClassLoader().getResourceAsStream(propFileName);

            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
            }

            //Clear all
            m_aoProperties.clear();
            //get values properties
            m_aoProperties.put("RABBIT_QUEUE_NAME", prop.getProperty("RABBIT_QUEUE_NAME"));
            m_aoProperties.put("RABBIT_HOST", prop.getProperty("RABBIT_HOST"));
            m_aoProperties.put("RABBIT_QUEUE_PORT", prop.getProperty("RABBIT_QUEUE_PORT"));
            m_aoProperties.put("RABBIT_QUEUE_DURABLE", prop.getProperty("RABBIT_QUEUE_DURABLE"));
            m_aoProperties.put("DOWNLOAD_ROOT_PATH", prop.getProperty("DOWNLOAD_ROOT_PATH"));
            m_aoProperties.put("GEOSERVER_ADDRESS", prop.getProperty("GEOSERVER_ADDRESS"));
            m_aoProperties.put("GEOSERVER_USER", prop.getProperty("GEOSERVER_USER"));
            m_aoProperties.put("GEOSERVER_PASSWORD", prop.getProperty("GEOSERVER_PASSWORD"));
            m_aoProperties.put("GEOSERVER_WORKSPACE", prop.getProperty("GEOSERVER_WORKSPACE"));
            m_aoProperties.put("PYRAMID_BASE_FOLDER", prop.getProperty("PYRAMID_BASE_FOLDER"));
            m_aoProperties.put("GDAL_PATH", prop.getProperty("GDAL_PATH"));
            m_aoProperties.put("GDAL_RETILE", prop.getProperty("GDAL_RETILE"));
            m_aoProperties.put("DHUS_USER", prop.getProperty("DHUS_USER"));
            m_aoProperties.put("DHUS_PASSWORD", prop.getProperty("DHUS_PASSWORD"));

        } catch (Exception e) {
            System.out.println("Exception: " + e);
        } finally {
            inputStream.close();
        }
    }

    public static String getPropValue(String sValue) throws IOException
    {
        if (m_aoProperties == null) {
            m_aoProperties = new HashMap<>();
            loadPropValues();
        }

        return m_aoProperties.get(sValue);
    }
}
