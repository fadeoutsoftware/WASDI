package wasdi.shared.data.sqlite;

import wasdi.shared.business.ParameterEntity;
import wasdi.shared.data.interfaces.IParametersRepositoryBackend;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.log.WasdiLog;

public class SqliteParametersRepositoryBackend extends SqliteRepository implements IParametersRepositoryBackend {

	public SqliteParametersRepositoryBackend() {
		m_sThisCollection = "parameters";
		this.ensureTable(m_sThisCollection);
	}

	@Override
	public String insertParameter(BaseParameter oParameter) {
		if (oParameter == null) {
			return "";
		}

		try {
			ParameterEntity oParameterEntity = new ParameterEntity();
			oParameterEntity.serializedParameter = SerializationUtils.serializeObjectToStringXML(oParameter);
			oParameterEntity.processObjId = oParameter.getProcessObjId();

			insert(m_sThisCollection, oParameterEntity.processObjId, oParameterEntity);
			return oParameterEntity.processObjId;
		} catch (Exception oEx) {
			WasdiLog.errorLog("ParametersRepository.insertParamenter: exception ", oEx);
		}

		return "";
	}

	@Override
	public BaseParameter getParameterByProcessObjId(String sProcessObjId) {
		BaseParameter oParameter = null;
		try {
			ParameterEntity oParameterEntity = findOneWhere(m_sThisCollection, "processObjId", sProcessObjId, ParameterEntity.class);

			if (oParameterEntity == null) {
				return null;
			}

			oParameter = (BaseParameter) SerializationUtils.deserializeStringXMLToObject(oParameterEntity.serializedParameter);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ParametersRepository.getProcessByProcessObjId: exception ", oEx);
		}

		return oParameter;
	}
}
