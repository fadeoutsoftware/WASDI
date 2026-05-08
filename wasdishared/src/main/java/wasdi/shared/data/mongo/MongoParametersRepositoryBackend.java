package wasdi.shared.data.mongo;

import java.io.IOException;

import org.bson.Document;

import wasdi.shared.business.ParameterEntity;
import wasdi.shared.data.interfaces.IParametersRepositoryBackend;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Mongo backend implementation for parameters repository.
 */
public class MongoParametersRepositoryBackend extends MongoRepository implements IParametersRepositoryBackend {

	public MongoParametersRepositoryBackend() {
		m_sThisCollection = "parameters";
		m_sRepoDb = "local";
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

			return this.add(oParameterEntity);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ParametersRepository.insertParamenter: exception ", oEx);
		}

		return "";
	}

	@Override
	public BaseParameter getParameterByProcessObjId(String sProcessObjId) {
		BaseParameter oParameter = null;
		try {
			Document oWSDocument = getCollection(m_sThisCollection)
					.find(new Document("processObjId", sProcessObjId))
					.first();

			if (oWSDocument == null) return null;

			String sJSON = oWSDocument.toJson();
			try {
				ParameterEntity oParameterEntity = new ParameterEntity();
				oParameterEntity = s_oMapper.readValue(sJSON, ParameterEntity.class);

				oParameter = (BaseParameter) SerializationUtils
						.deserializeStringXMLToObject(oParameterEntity.serializedParameter);
			} catch (IOException e) {
				WasdiLog.errorLog("ParametersRepository.getProcessByProcessObjId: exception ", e);
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("ParametersRepository.getProcessByProcessObjId: exception ", oEx);
		}

		return oParameter;
	}
}
