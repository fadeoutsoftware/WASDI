package wasdi.shared.data;

import java.util.List;

import wasdi.shared.business.PublishedBand;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.IPublishedBandsRepositoryBackend;

/**
 * Created by p.campanella on 17/11/2016.
 */
public class PublishedBandsRepository {

    private final IPublishedBandsRepositoryBackend m_oBackend;
	
	public PublishedBandsRepository() {
        m_oBackend = createBackend();
    }

    private IPublishedBandsRepositoryBackend createBackend() {
        // For now keep Mongo backend only. Next step will select by config.
        return DataRepositoryFactoryProvider.getFactory().createPublishedBandsRepository();
	}
	
	/**
	 * Insert a published band
	 * @param oFile
	 * @return
	 */
    public boolean insertPublishedBand(PublishedBand oFile) {
		return m_oBackend.insertPublishedBand(oFile);
    }

    /**
     * Get a published band by product and band name
     * @param sProductName
     * @param sBandName
     * @return
     */
    public PublishedBand getPublishedBand(String sProductName, String sBandName) {
		return m_oBackend.getPublishedBand(sProductName, sBandName);
    }
    
    /**
     * Get all the published bands of a product
     * @param sProductName
     * @return
     */
    public List<PublishedBand> getPublishedBandsByProductName(String sProductName) {
		return m_oBackend.getPublishedBandsByProductName(sProductName);
    }
    
    /**
     * Get all the published bands
     * @return
     */
    public List<PublishedBand> getList() {
		return m_oBackend.getList();
    }

    /**
     * Delete all the Published Bands of a Product
     * @param sProductName
     * @return
     */
    public int deleteByProductName(String sProductName) {
		return m_oBackend.deleteByProductName(sProductName);
    }
    
    /**
     * Delete a specific Published band
     * @param sProductName
     * @param sLayerId
     * @return
     */
    public int deleteByProductNameLayerId(String sProductName, String sLayerId) {
		return m_oBackend.deleteByProductNameLayerId(sProductName, sLayerId);
    }

}

