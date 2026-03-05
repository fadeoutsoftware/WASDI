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

	int deleteProcessorByUser(String sUserId);

	List<Processor> getDeployedProcessors();

	List<Processor> getDeployedProcessors(String sOrderBy);

	List<Processor> getDeployedProcessors(String sOrderBy, int iDirection);

	long countProcessors();

	long countProcessors(boolean bPublicOnly);

	long countProcessors(boolean bInAppStoreOnly, boolean bPublicOnly);
}
