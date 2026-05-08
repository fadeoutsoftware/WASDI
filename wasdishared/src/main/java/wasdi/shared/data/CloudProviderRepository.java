package wasdi.shared.data;

import java.util.List;

import wasdi.shared.business.CloudProvider;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.ICloudProviderRepositoryBackend;

public class CloudProviderRepository {

    private final ICloudProviderRepositoryBackend m_oBackend;
	
	public CloudProviderRepository() {
        m_oBackend = createBackend();
	}

    private ICloudProviderRepositoryBackend createBackend() {
        // For now keep Mongo backend only. Next step will select by config.
        return DataRepositoryFactoryProvider.getFactory().createCloudProviderRepository();
    }
	
	/**
	 * Get the list of Cloud Providers
	 * @return
	 */
    public List<CloudProvider> getCloudProviders() {
        return m_oBackend.getCloudProviders();
    }
    
    /**
     * Get a Cloud Provider by Id
     * @param sCloudProviderId id of the Cloud Provider
     * @return Entity
     */
    public CloudProvider getCloudProviderById(String sCloudProviderId) {
        return m_oBackend.getCloudProviderById(sCloudProviderId);
    }
    
    /**
     * Get a Cloud Provider by Code
     * @param sCode Code of the Cloud Provider
     * @return Entity
     */
    public CloudProvider getCloudProviderByCode(String sCode) {
        return m_oBackend.getCloudProviderByCode(sCode);
    }
}

