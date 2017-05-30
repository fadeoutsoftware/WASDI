package wasdi; /**
 * Created by s.adamo on 23/09/2016.
 */

import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

public class ConfigReader {

    static HashMap<String,String> m_aoProperties;

    private static void loadPropValues() throws IOException {

        InputStream inputStream = null;
        try {
            Properties prop = new Properties();
            String propFileName = "config.properties";

            //inputStream = new ConfigReader().getClass().getClassLoader().getResourceAsStream(propFileName);
            File oCurrentFile = new File(LauncherMain.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            inputStream = new FileInputStream(oCurrentFile.getParentFile().getPath() + "/config.properties");

            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
            }

            Enumeration<String> aoProperties =  (Enumeration<String>) prop.propertyNames();
            //Clear all
            m_aoProperties.clear();

            String sKey = aoProperties.nextElement();

            while (sKey != null) {
                m_aoProperties.put(sKey, prop.getProperty(sKey));
                if (aoProperties.hasMoreElements()) {
                    sKey = aoProperties.nextElement();
                }
                else  {
                    break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
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

    public static String getPropValue(String sValue, String sDefault) throws IOException
    {
        if (m_aoProperties == null) {
            m_aoProperties = new HashMap<>();
            loadPropValues();
        }

        String sRet = m_aoProperties.get(sValue);
		return sRet==null ? sDefault : sRet;
    }
}
