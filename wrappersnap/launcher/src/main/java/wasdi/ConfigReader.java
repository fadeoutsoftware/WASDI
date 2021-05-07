package wasdi; /**
 * Created by s.adamo on 23/09/2016.
 */

import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

/**
 * Launcher Configuration Reader
 * @author p.campanella
 *
 */
public class ConfigReader {
	
	private ConfigReader() {
		// / private constructor to hide the public implicit one 
	}

    static HashMap<String,String> m_aoProperties;

    @SuppressWarnings("unchecked")
	private static void loadPropValues() throws IOException {

        InputStream inputStream = null;
        try {
            Properties prop = new Properties();
            String propFileName = "config.properties";

            System.out.println(LauncherMain.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            
            //inputStream = new ConfigReader().getClass().getClassLoader().getResourceAsStream(propFileName);
            File oCurrentFile = new File(LauncherMain.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            
            
            
            inputStream = new FileInputStream(oCurrentFile.getParentFile().getPath() + "/" + propFileName);

            if (inputStream != null) {
                prop.load(inputStream);
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
        	if (inputStream != null) inputStream.close();
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
