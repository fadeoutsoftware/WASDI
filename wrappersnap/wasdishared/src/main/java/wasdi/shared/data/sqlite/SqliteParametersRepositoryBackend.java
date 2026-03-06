package wasdi.shared.data.sqlite;

import java.io.File;

import wasdi.shared.business.ParameterEntity;
import wasdi.shared.config.PathsConfig;
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
				// Maybe is an old one, still in the file system. Lets try
				String sPotentialFile = PathsConfig.getParameterPath(sProcessObjId);

				File oParameterFile = new File(sPotentialFile);

				if (oParameterFile.exists() == false) {
					return null;
				} else {
					return (BaseParameter) SerializationUtils.deserializeXMLToObject(sPotentialFile);
				}
			}

			oParameter = (BaseParameter) SerializationUtils.deserializeStringXMLToObject(oParameterEntity.serializedParameter);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ParametersRepository.getProcessByProcessObjId: exception ", oEx);
		}

		return oParameter;
	}
}
