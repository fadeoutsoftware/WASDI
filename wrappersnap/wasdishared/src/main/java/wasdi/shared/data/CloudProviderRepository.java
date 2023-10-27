package wasdi.shared.data;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.FindIterable;

import wasdi.shared.business.CloudProvider;
import wasdi.shared.utils.log.WasdiLog;

public class CloudProviderRepository extends MongoRepository {
	
	public CloudProviderRepository() {
		m_sThisCollection = "cloudproviders";
	}
	
	/**
	 * Get the list of Cloud Providers
	 * @return
	 */
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
    
    /**
     * Get a Cloud Provider by Id
     * @param sCloudProviderId id of the Cloud Provider
     * @return Entity
     */
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

        return  null;
    }
    
    /**
     * Get a Cloud Provider by Code
     * @param sCode Code of the Cloud Provider
     * @return Entity
     */
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

        return  null;
    }
}
