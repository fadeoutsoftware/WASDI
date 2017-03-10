import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Created by s.adamo on 07/03/2017.
 */
public class Utils {

    public static void SerializeObjectToXML(String xmlFile, Object objectToSerialize) throws Exception {
        FileOutputStream os = new FileOutputStream(xmlFile);
        XMLEncoder encoder = new XMLEncoder(os);
        encoder.writeObject(objectToSerialize);
        encoder.close();
    }

    /**
     * Reads Java Bean Object From XML File
     */
    public static Object DeserializeXMLToObject(String xmlFileLocation) throws Exception {
        FileInputStream os = new FileInputStream(xmlFileLocation);
        XMLDecoder decoder = new XMLDecoder(os);
        Object deSerializedObject = decoder.readObject();
        decoder.close();

        return deSerializedObject;
    }
}
