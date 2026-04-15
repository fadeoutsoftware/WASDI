package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.FluentFilter.where;

import java.io.File;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.NitriteCollection;

import wasdi.shared.business.ParameterEntity;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.data.interfaces.IParametersRepositoryBackend;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * NO2 backend implementation for parameters repository.
 */
public class No2ParametersRepositoryBackend extends No2Repository implements IParametersRepositoryBackend {

	private static final String s_sCollectionName = "parameters";

	@Override
	public String insertParameter(BaseParameter oParameter) {
		if (oParameter == null) {
			return "";
		}

		try {
			ParameterEntity oParameterEntity = new ParameterEntity();
			oParameterEntity.serializedParameter = SerializationUtils.serializeObjectToStringXML(oParameter);
			oParameterEntity.processObjId = oParameter.getProcessObjId();

			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return "";
			}

			oCollection.insert(toDocument(oParameterEntity));
			return oParameterEntity.processObjId;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ParametersRepositoryBackend.insertParameter: exception", oEx);
		}

		return "";
	}

	@Override
	public BaseParameter getParameterByProcessObjId(String sProcessObjId) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection != null) {
				for (Document oDocument : oCollection.find(where("processObjId").eq(sProcessObjId))) {
					ParameterEntity oParameterEntity = fromDocument(oDocument, ParameterEntity.class);
					if (oParameterEntity != null && oParameterEntity.serializedParameter != null) {
						return (BaseParameter) SerializationUtils.deserializeStringXMLToObject(oParameterEntity.serializedParameter);
					}
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ParametersRepositoryBackend.getParameterByProcessObjId: exception", oEx);
		}

		return null;
	}
}
