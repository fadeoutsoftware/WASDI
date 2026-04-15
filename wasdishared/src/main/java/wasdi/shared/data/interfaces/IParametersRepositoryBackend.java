package wasdi.shared.data.interfaces;

import wasdi.shared.parameters.BaseParameter;

/**
 * Backend contract for parameters repository.
 */
public interface IParametersRepositoryBackend {

	String insertParameter(BaseParameter oParameter);

	BaseParameter getParameterByProcessObjId(String sProcessObjId);
}
