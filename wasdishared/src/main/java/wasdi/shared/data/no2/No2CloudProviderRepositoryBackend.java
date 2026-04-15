package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.FluentFilter.where;

import java.util.ArrayList;
import java.util.List;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.DocumentCursor;
import org.dizitart.no2.collection.NitriteCollection;

import wasdi.shared.business.CloudProvider;
import wasdi.shared.data.interfaces.ICloudProviderRepositoryBackend;
import wasdi.shared.utils.log.WasdiLog;

/**
 * NO2 backend implementation for cloud provider repository.
 */
public class No2CloudProviderRepositoryBackend extends No2Repository implements ICloudProviderRepositoryBackend {

	private static final String s_sCollectionName = "cloudproviders";

	@Override
	public List<CloudProvider> getCloudProviders() {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			DocumentCursor oCursor = oCollection != null ? oCollection.find() : null;
			return toList(oCursor, CloudProvider.class);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2CloudProviderRepositoryBackend.getCloudProviders: error", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public CloudProvider getCloudProviderById(String sCloudProviderId) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}

			for (Document oDocument : oCollection.find(where("cloudProviderId").eq(sCloudProviderId))) {
				return fromDocument(oDocument, CloudProvider.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2CloudProviderRepositoryBackend.getCloudProviderById: error", oEx);
		}

		return null;
	}

	@Override
	public CloudProvider getCloudProviderByCode(String sCode) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}

			for (Document oDocument : oCollection.find(where("code").eq(sCode))) {
				return fromDocument(oDocument, CloudProvider.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2CloudProviderRepositoryBackend.getCloudProviderByCode: error", oEx);
		}

		return null;
	}
}
