package wasdi.shared.data;

import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.IParametersRepositoryBackend;
import wasdi.shared.parameters.BaseParameter;

public class ParametersRepository {

	private final IParametersRepositoryBackend m_oBackend;

	public ParametersRepository() {
		m_oBackend = createBackend();
	}

	private IParametersRepositoryBackend createBackend() {
		// For now keep Mongo backend only. Next step will select by config.
		return DataRepositoryFactoryProvider.getFactory().createParametersRepository();
	}

	/**
	 * Insert a new Parameter 
	 * @param oProcessWorkspace Process Workpsace to insert
	 * @return Mongo obj id
	 */
    public String insertParameter(BaseParameter oParameter) {
		return m_oBackend.insertParameter(oParameter);
    }
    
    public BaseParameter getParameterByProcessObjId(String sProcessObjId) {
		return m_oBackend.getParameterByProcessObjId(sProcessObjId);
    }

}

