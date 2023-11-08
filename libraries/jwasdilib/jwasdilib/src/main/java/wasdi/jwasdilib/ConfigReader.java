package wasdi.jwasdilib; /**
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

    static HashMap<String,String> m_aoProperties;
    
    static String s_sConfigFilePath = null;
    
    public static void setConfigFilePath (String sConfigPath) {
    	s_sConfigFilePath = sConfigPath;
    }

    @SuppressWarnings("unchecked")
	private static void loadPropValues() throws IOException {

        InputStream oInputStream = null;
        
        try {
            Properties oProp = new Properties();
            
            
            String sPropFileName = "config.properties";

            //inputStream = new ConfigReader().getClass().getClassLoader().getResourceAsStream(propFileName);
            File oCurrentFile = new File(App.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            
            
            String sConfigFilePath = oCurrentFile.getParentFile().getPath() + "/" + sPropFileName;
            
            if (s_sConfigFilePath != null) {
            	if (s_sConfigFilePath.equals("")==false) {
            		File oUserConfig = new File(s_sConfigFilePath);
            		if (oUserConfig.exists()) {
            			sConfigFilePath = s_sConfigFilePath;
            		}
            	}
            }
            
            oInputStream = new FileInputStream(sConfigFilePath);

            if (oInputStream != null) {
                oProp.load(oInputStream);
            }

            Enumeration<String> aoProperties =  (Enumeration<String>) oProp.propertyNames();
            //Clear all
            m_aoProperties.clear();

            String sKey = aoProperties.nextElement();

            while (sKey != null) {
                m_aoProperties.put(sKey, oProp.getProperty(sKey));
                if (aoProperties.hasMoreElements()) {
                    sKey = aoProperties.nextElement();
                }
                else  {
                    break;
                }
            }

        } catch (Exception e) {
        	System.out.println("ConfigReader.loadPropValues: error " + e.toString());
        } finally {
        	if (oInputStream != null) oInputStream.close();
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
