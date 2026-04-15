package wasdi.shared.data;

import java.util.Date;
import java.util.List;

import wasdi.shared.business.processors.Processor;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.IProcessorRepositoryBackend;

public class ProcessorRepository {

    private final IProcessorRepositoryBackend m_oBackend;
	
	public ProcessorRepository() {
        m_oBackend = createBackend();
    }

    private IProcessorRepositoryBackend createBackend() {
        // For now keep Mongo backend only. Next step will select by config.
        return DataRepositoryFactoryProvider.getFactory().createProcessorRepository();
	}
	
	/**
	 * Create a new processor
	 * @param oProcessor Processor Entity
	 * @return True or False in case of exception
	 */
    public boolean insertProcessor(Processor oProcessor) {
		return m_oBackend.insertProcessor(oProcessor);
    }
    
    /**
     * Get a processor from the WASDI Id
     * @param sProcessorId WASDI id of the processor
     * @return Entity
     */
    public Processor getProcessor(String sProcessorId) {
		return m_oBackend.getProcessor(sProcessorId);
    }
    
    /**
     * Get a Processor by Name
     * @param sName Name of the processor
     * @return Processor Entity
     */
    public Processor getProcessorByName(String sName) {
		return m_oBackend.getProcessorByName(sName);
    }

    /**
     * Update a processor
     * @param oProcessor Entity to update
     * @return True or False in case of exception
     */
    public boolean updateProcessor(Processor oProcessor) {
		return m_oBackend.updateProcessor(oProcessor);
    }

    /**
     * Get the processor owned by sUserId
     * @param sUserId Id User of the processor's owner
     * @return List of processors of the user
     */
    public List<Processor> getProcessorByUser(String sUserId) {
		return m_oBackend.getProcessorByUser(sUserId);
    }

	public List<Processor> findProcessorsByPartialName(String sPartialName) {
        return m_oBackend.findProcessorsByPartialName(sPartialName);
	}
    
    /**
     * Get the next http port available for a processor
     * @return New Port. 
     */
    public int getNextProcessorPort() {
		return m_oBackend.getNextProcessorPort();
    }
    
    /**
     * Delete a processor from WASDI Id
     * @param sProcessorId Processor Id
     * @return True or false in case of exception
     */
    public boolean deleteProcessor(String sProcessorId) {
		return m_oBackend.deleteProcessor(sProcessorId);
    }
    
    /**
     * Delete all the processors of a user
     * @param sUserId Owner of the procs to delete
     * @return Number of deleted processors
     */
    public int deleteProcessorByUser(String sUserId) {
		return m_oBackend.deleteProcessorByUser(sUserId);
    }
    
    /**
     * Get the list of all the deployed processors
     * @return List of all the processors
     */
    public List<Processor> getDeployedProcessors() {
		return m_oBackend.getDeployedProcessors();
    }
    
    public List<Processor> getDeployedProcessors(String sOrderBy) {
		return m_oBackend.getDeployedProcessors(sOrderBy);
    }
    
    public List<Processor> getDeployedProcessors(String sOrderBy, int iDirection) {
		return m_oBackend.getDeployedProcessors(sOrderBy, iDirection);
    }    

	public void updateProcessorDate(Processor oProcessor){
		Date oDate = new Date();
		oProcessor.setUpdateDate( (double) oDate.getTime());
		updateProcessor(oProcessor);
	}

	public long countProcessors() {
        return m_oBackend.countProcessors();
	}
	
	public long countProcessors(boolean bPublicOnly) {
        return m_oBackend.countProcessors(bPublicOnly);
	}
	
	public long countProcessors(boolean bInAppStoreOnly, boolean bPublicOnly) {
        return m_oBackend.countProcessors(bInAppStoreOnly, bPublicOnly);
	}
	
}

