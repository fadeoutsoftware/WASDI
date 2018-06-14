package wasdi.asynch;

import java.io.File;
import java.io.IOException;

import wasdi.LauncherMain;
import wasdi.shared.utils.SerializationUtils;
import wasdi.snapopearations.ReadProduct;

public class SaveMetadataThread extends Thread {
	String m_sMetadataFilePath;
	ReadProduct m_oReadProduct;
	File m_oProductFile;
	
	public SaveMetadataThread(String sMetadataFilePath, ReadProduct oReadProduct, File oProductFile) {
		m_sMetadataFilePath = sMetadataFilePath;
		m_oReadProduct = oReadProduct;
		m_oProductFile = oProductFile;
	}
	
	public void run() {
		try {
			LauncherMain.s_oLogger.debug("Start SaveMetadataThread");
			SerializationUtils.serializeObjectToXML(m_sMetadataFilePath, m_oReadProduct.getProductMetadataViewModel(m_oProductFile));
			LauncherMain.s_oLogger.debug("SaveMetadataThread: metadata saved, bye");
		} catch (IOException e) {
			LauncherMain.s_oLogger.error("SaveMetadataThread Exception " + e.toString());
			e.printStackTrace();
		} catch (Exception e) {
			LauncherMain.s_oLogger.error("SaveMetadataThread Exception " + e.toString());
			e.printStackTrace();
		}
	}
}
