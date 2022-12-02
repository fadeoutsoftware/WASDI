package wasdi.asynch;

import java.io.File;

import wasdi.io.WasdiProductReader;
import wasdi.io.WasdiProductReaderFactory;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.products.MetadataViewModel;

/**
 * Save Metadata file Thread
 * @author p.campanella
 *
 */
public class SaveMetadataThread extends Thread {
	String m_sMetadataFilePath;
	WasdiProductReader m_oReadProduct;
	File m_oProductFile;
	
	public SaveMetadataThread(String sMetadataFilePath, String sProductFile) {
		m_sMetadataFilePath = sMetadataFilePath;
		m_oProductFile = new File(sProductFile);
		m_oReadProduct = WasdiProductReaderFactory.getProductReader(m_oProductFile);
	}
	@Override
	public void run() {
		try {
			
			MetadataViewModel oMetadataViewModel = m_oReadProduct.getProductMetadataViewModel();
			
			if (oMetadataViewModel != null) {
				SerializationUtils.serializeObjectToXML(m_sMetadataFilePath, oMetadataViewModel);
			}
			
			if (m_oReadProduct.getSnapProduct() != null) {
				m_oReadProduct.getSnapProduct().dispose();
			}
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog("SaveMetadataThread.run: exception " + oEx.toString());
		}
	}
}
