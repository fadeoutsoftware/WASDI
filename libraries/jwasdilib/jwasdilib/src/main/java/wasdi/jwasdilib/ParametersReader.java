package wasdi.jwasdilib;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

public class ParametersReader {
	
    HashMap<String,String> m_aoProperties = new HashMap<>();
    String m_sParameterFilePath ="./parameters.txt";
    
    public ParametersReader(String sParameterFilePath) {
    	m_sParameterFilePath = sParameterFilePath;
    	loadPropValues();
    }

    @SuppressWarnings("unchecked")
	private void loadPropValues() {

        InputStream oInputStream = null;
        try {
            Properties oProp = new Properties();
            
            if (m_sParameterFilePath == null) {
            	System.out.println("Parameter file not set");
            	return;
            }
            
            if (m_sParameterFilePath.equals("")) {
            	System.out.println("Parameter file not set");
            	return;
            }

            File oParamFile = new File(m_sParameterFilePath);
            if (oParamFile.exists() == false) {
            	System.out.println("Parameter file " + m_sParameterFilePath + " does not exists");
            	return;            	
            }
            
            oInputStream = new FileInputStream(oParamFile);

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
        	System.out.println("ParametersReader.loadPropValues error " + e.toString());
        } finally {
        	try {
        		if (oInputStream != null) oInputStream.close();
        	}
        	catch (Exception oInEx) {
        		System.out.println("ParametersReader.loadPropValues error " + oInEx.toString());
			}
        	
        }
    }
    
    public void refresh() {
    	loadPropValues();
    }

    public String getPropValue(String sValue)
    {
        if (m_aoProperties == null) {
            m_aoProperties = new HashMap<>();
            loadPropValues();
        }

        return m_aoProperties.get(sValue);
    }

    public String getPropValue(String sValue, String sDefault)
    {
        if (m_aoProperties == null) {
            m_aoProperties = new HashMap<>();
            loadPropValues();
        }

        String sRet = m_aoProperties.get(sValue);
		return sRet==null ? sDefault : sRet;
    }
    
    public HashMap<String,String> getParameters() {
    	return m_aoProperties;
    }
}
