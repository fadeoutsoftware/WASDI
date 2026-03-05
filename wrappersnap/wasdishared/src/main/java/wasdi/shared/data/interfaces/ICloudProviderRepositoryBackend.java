package wasdi.shared.data.interfaces;

import java.util.List;

import wasdi.shared.business.CloudProvider;

/**
 * Backend contract for cloud provider repository.
 */
public interface ICloudProviderRepositoryBackend {

	List<CloudProvider> getCloudProviders();

	CloudProvider getCloudProviderById(String sCloudProviderId);

	CloudProvider getCloudProviderByCode(String sCode);
}
