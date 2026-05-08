package wasdi.shared.data.interfaces;

import wasdi.shared.business.Counter;

/**
 * Backend contract for counter repository.
 */
public interface ICounterRepositoryBackend {

	int getNextValue(String sSequence);

	String insertCounter(Counter oCounter);

	Counter getCounterBySequence(String sSequence);

	boolean updateCounter(Counter oCounter);
}
