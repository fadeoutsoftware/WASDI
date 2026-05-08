package wasdi.shared.data.interfaces;

import wasdi.shared.business.processors.ProcessorUI;

/**
 * Backend contract for processor UI repository.
 */
public interface IProcessorUIRepositoryBackend {

	boolean insertProcessorUI(ProcessorUI oProcUI);

	ProcessorUI getProcessorUI(String sProcessorId);

	boolean updateProcessorUI(ProcessorUI oProcessorUI);

	int deleteProcessorUIByProcessorId(String sProcessorId);
}
