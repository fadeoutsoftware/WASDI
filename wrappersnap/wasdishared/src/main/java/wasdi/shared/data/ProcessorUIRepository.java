package wasdi.shared.data;

import wasdi.shared.business.processors.ProcessorUI;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.IProcessorUIRepositoryBackend;

public class ProcessorUIRepository {

    private final IProcessorUIRepositoryBackend m_oBackend;
	
	public ProcessorUIRepository() {
        m_oBackend = createBackend();
    }

    private IProcessorUIRepositoryBackend createBackend() {
        // For now keep Mongo backend only. Next step will select by config.
        return DataRepositoryFactoryProvider.getFactory().createProcessorUIRepository();
	}
	
	/**
	 * Create a new processor UI
	 * @param ProcessorUI oProcUI
	 * @return True of False in case of exception
	 */
    public boolean insertProcessorUI(ProcessorUI oProcUI) {
		return m_oBackend.insertProcessorUI(oProcUI);
    }
    
    /**
     * Get a processor UI from the processor Id
     * @param sProcessorId WASDI id of the processor
     * @return ProcessorUI interface of the processor
     */
    public ProcessorUI getProcessorUI(String sProcessorId) {
		return m_oBackend.getProcessorUI(sProcessorId);
    }
    
    /**
     * Update a processor UI
     * @param oProcessorUI Entity to update
     * @return True or False in case of exception
     */
    public boolean updateProcessorUI(ProcessorUI oProcessorUI) {
		return m_oBackend.updateProcessorUI(oProcessorUI);
    }    
    
	/**
	 * Delete all the comments of a specific user
	 * @param sUserId
	 * @return
	 */
    public int deleteProcessorUIByProcessorId(String sProcessorId) {
        return m_oBackend.deleteProcessorUIByProcessorId(sProcessorId);
    }

}

