package wasdi.shared.data.interfaces;

import java.util.List;

import wasdi.shared.business.processors.Processor;

/**
 * Backend contract for processor repository.
 */
public interface IProcessorRepositoryBackend {

	boolean insertProcessor(Processor oProcessor);

	Processor getProcessor(String sProcessorId);

	Processor getProcessorByName(String sName);

	boolean updateProcessor(Processor oProcessor);

	List<Processor> getProcessorByUser(String sUserId);

	List<Processor> findProcessorsByPartialName(String sPartialName);

	int getNextProcessorPort();

	boolean deleteProcessor(String sProcessorId);
	
	public boolean deleteProcessorByName(String sProcessorName);

	int deleteProcessorByUser(String sUserId);

	List<Processor> getDeployedProcessors();

	List<Processor> getDeployedProcessors(String sOrderBy);

	List<Processor> getDeployedProcessors(String sOrderBy, int iDirection);

	/**
	 * Get deployed processors with a lightweight payload.
	 */
	List<Processor> getDeployedProcessorsLightweight();

	/**
	 * Get processors candidate for marketplace listing using backend-side filtering/projection.
	 */
	List<Processor> getMarketplaceProcessors(String sOrderBy, int iDirection);

	/**
	 * Get a paginated marketplace processor list using backend-side filtering.
	 * Score filtering is intentionally not part of this query.
	 */
	List<Processor> getMarketplaceProcessorsPage(
			String sUserId,
			List<String> asSharedProcessorIds,
			String sName,
			List<String> asCategories,
			List<String> asPublishers,
			float fMaxPrice,
			String sOrderBy,
			int iDirection,
			int iPage,
			int iItemsPerPage);

	long countProcessors();

	long countProcessors(boolean bPublicOnly);

	long countProcessors(boolean bInAppStoreOnly, boolean bPublicOnly);
}
