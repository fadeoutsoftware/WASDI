package wasdi.shared.data.sqlite;

import java.util.List;

import wasdi.shared.business.CloudProvider;
import wasdi.shared.data.interfaces.ICloudProviderRepositoryBackend;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Mongo backend implementation for cloud provider repository.
 */
public class SqliteCloudProviderRepositoryBackend extends SqliteRepository implements ICloudProviderRepositoryBackend {

	public SqliteCloudProviderRepositoryBackend() {
		m_sThisCollection = "cloudproviders";
		this.ensureTable(m_sThisCollection);
	}

	@Override
	public List<CloudProvider> getCloudProviders() {
		
		try {
			return findAll(m_sThisCollection, CloudProvider.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteCloudProviderRepositoryBackend.getCloudProviders: error", oEx);
		}

		return new java.util.ArrayList<>();
	}

	@Override
	public CloudProvider getCloudProviderById(String sCloudProviderId) {
		
		try {
			return findOneWhere(m_sThisCollection, "id", sCloudProviderId, CloudProvider.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteCloudProviderRepositoryBackend.getCloudProviderById: error", oEx);
		}

		return null;
	}

	@Override
	public CloudProvider getCloudProviderByCode(String sCode) {
		
		try {
			return findOneWhere(m_sThisCollection, "code", sCode, CloudProvider.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteCloudProviderRepositoryBackend.getCloudProviderById: error", oEx);
		}

		return null;		
	}
}
