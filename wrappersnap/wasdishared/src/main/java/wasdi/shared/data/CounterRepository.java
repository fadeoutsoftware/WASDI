package wasdi.shared.data;

import wasdi.shared.business.Counter;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.ICounterRepositoryBackend;


public class CounterRepository {

	private final ICounterRepositoryBackend m_oBackend;
	
	public CounterRepository() {
		m_oBackend = createBackend();
	}

	private ICounterRepositoryBackend createBackend() {
		// For now keep Mongo backend only. Next step will select by config.
		return DataRepositoryFactoryProvider.getFactory().createCounterRepository();
	}
	
	/**
	 * Get the next value of a sequence
	 * @param sSequence
	 * @return
	 */
	public int getNextValue(String sSequence) {
		return m_oBackend.getNextValue(sSequence);
	}

	/**
	 * Create a new sequence counter
	 * @param oCounter Counter Entity
	 * @return Obj Id
	 */
	public String insertCounter(Counter oCounter) {
		return m_oBackend.insertCounter(oCounter);
	}

	/**
	 * Get the counter of a sequence
	 * @param sSequence
	 * @return
	 */
	public Counter getCounterBySequence(String sSequence) {
		return m_oBackend.getCounterBySequence(sSequence);
	}

	/**
	 * Update a counter
	 * @param oCounter Counter to update
	 * @return
	 */
	public boolean updateCounter(Counter oCounter) {
		return m_oBackend.updateCounter(oCounter);
	}
}

