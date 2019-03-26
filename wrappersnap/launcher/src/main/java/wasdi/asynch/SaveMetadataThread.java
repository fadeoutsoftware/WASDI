package wasdi.asynch;

import java.io.File;
import java.io.IOException;

import wasdi.shared.utils.SerializationUtils;
import wasdi.snapopearations.ReadProduct;

/**
 * Save Metadata file Thread
 * @author p.campanella
 *
 */
public class SaveMetadataThread extends Thread {
	String m_sMetadataFilePath;
	ReadProduct m_oReadProduct;
	File m_oProductFile;
	
	public SaveMetadataThread(String sMetadataFilePath, String sProductFile) {
		m_sMetadataFilePath = sMetadataFilePath;
		m_oReadProduct = new ReadProduct();
		m_oProductFile = new File(sProductFile);
	}
	
	public void run() {
		try {
			//LauncherMain.s_oLogger.debug("Start SaveMetadataThread");
			SerializationUtils.serializeObjectToXML(m_sMetadataFilePath, m_oReadProduct.getProductMetadataViewModel(m_oProductFile));
			//LauncherMain.s_oLogger.debug("SaveMetadataThread: metadata saved, bye");
		} catch (IOException e) {
			//LauncherMain.s_oLogger.error("SaveMetadataThread Exception " + e.toString());
			e.printStackTrace();
		} catch (Exception e) {
			//LauncherMain.s_oLogger.error("SaveMetadataThread Exception " + e.toString());
			e.printStackTrace();
		}
	}
}
