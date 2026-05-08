package wasdi.shared.data.mongo;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.FindIterable;

import wasdi.shared.business.CloudProvider;
import wasdi.shared.data.interfaces.ICloudProviderRepositoryBackend;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Mongo backend implementation for cloud provider repository.
 */
public class MongoCloudProviderRepositoryBackend extends MongoRepository implements ICloudProviderRepositoryBackend {

	public MongoCloudProviderRepositoryBackend() {
		m_sThisCollection = "cloudproviders";
	}

	@Override
	public List<CloudProvider> getCloudProviders() {

		final ArrayList<CloudProvider> aoReturnList = new ArrayList<CloudProvider>();
		try {

			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find();
			fillList(aoReturnList, oWSDocuments, CloudProvider.class);

		} catch (Exception oEx) {
			WasdiLog.errorLog("CloudProviderRepository.getCloudProviderByCode : error", oEx);
		}

		return aoReturnList;
	}

	@Override
	public CloudProvider getCloudProviderById(String sCloudProviderId) {

		try {
			Document oWSDocument = getCollection(m_sThisCollection).find(new Document("cloudProviderId", sCloudProviderId)).first();

			if (null != oWSDocument) {
				String sJSON = oWSDocument.toJson();
				return s_oMapper.readValue(sJSON, CloudProvider.class);
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("CloudProviderRepository.getCloudProviderByCode : error", oEx);
		}

		return null;
	}

	@Override
	public CloudProvider getCloudProviderByCode(String sCode) {

		try {
			Document oWSDocument = getCollection(m_sThisCollection).find(new Document("code", sCode)).first();

			if (null != oWSDocument) {
				String sJSON = oWSDocument.toJson();
				return s_oMapper.readValue(sJSON, CloudProvider.class);
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("CloudProviderRepository.getCloudProviderByCode : error", oEx);
		}

		return null;
	}
}
