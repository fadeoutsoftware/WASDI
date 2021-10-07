package wasdi.asynch;

import java.io.File;
import java.io.IOException;

import wasdi.io.WasdiProductReader;
import wasdi.shared.utils.SerializationUtils;
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
		m_oReadProduct = new WasdiProductReader();
		m_oProductFile = new File(sProductFile);
	}
	@Override
	public void run() {
		try {
			
			MetadataViewModel oMetadataViewModel = m_oReadProduct.getProductMetadataViewModel(m_oProductFile);
			
			if (oMetadataViewModel != null) {
				SerializationUtils.serializeObjectToXML(m_sMetadataFilePath, oMetadataViewModel);
			}
			
			if (m_oReadProduct.getSnapProduct() != null) {
				m_oReadProduct.getSnapProduct().dispose();
			}
		} 
		catch (IOException oEx) {
			oEx.printStackTrace();
		} 
		catch (Exception oEx) {
			oEx.printStackTrace();
		}
	}
}
