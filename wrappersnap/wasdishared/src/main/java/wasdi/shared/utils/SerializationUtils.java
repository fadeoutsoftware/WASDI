package wasdi.shared.utils;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Created by s.adamo on 10/10/2016.
 */
public class SerializationUtils {
	private SerializationUtils() {
		// / private constructor to hide the public implicit one 
	}

    /**
     * This method saves (serializes) any java bean object into xml file
     */
    public static void serializeObjectToXML(String sXmlFileLocation, Object oObjectToSerialize) throws Exception {
        FileOutputStream oOutputStream = new FileOutputStream(sXmlFileLocation);
        XMLEncoder oEncoder = new XMLEncoder(oOutputStream);
        oEncoder.writeObject(oObjectToSerialize);
        oEncoder.close();
    }

    /**
     * Reads Java Bean Object From XML File
     */
    public static Object deserializeXMLToObject(String sXmlFileLocation) throws Exception {
        FileInputStream oOutputStream = new FileInputStream(sXmlFileLocation);
        XMLDecoder oDecoder = new XMLDecoder(oOutputStream);
        Object oDeSerializedObject = oDecoder.readObject();
        oDecoder.close();

        return oDeSerializedObject;
    }

    
    /**
     * This method serializes any java bean object into a String
     */
    public static String serializeObjectToStringXML(Object oObjectToSerialize) {
        ByteArrayOutputStream aoOutputStream = new ByteArrayOutputStream();
        XMLEncoder oEncoder = new XMLEncoder(aoOutputStream);
        oEncoder.writeObject(oObjectToSerialize);
        oEncoder.close();
        
        return aoOutputStream.toString();
    }

    /**
     * Reads Java Bean Object From XML File
     */
    public static Object deserializeStringXMLToObject(String sXMLObject) {
    	
    	InputStream oTargetStream = new ByteArrayInputStream(sXMLObject.getBytes());
        XMLDecoder oDecoder = new XMLDecoder(oTargetStream);
        Object oDeSerializedObject = oDecoder.readObject();
        oDecoder.close();

        return oDeSerializedObject;
    }
    

}
