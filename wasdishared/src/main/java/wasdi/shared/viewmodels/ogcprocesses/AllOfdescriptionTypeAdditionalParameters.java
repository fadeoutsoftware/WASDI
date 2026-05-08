package wasdi.shared.viewmodels.ogcprocesses;

import java.util.ArrayList;
import java.util.List;

public class AllOfdescriptionTypeAdditionalParameters extends Metadata {
	
	private List<AdditionalParameter> parameters = new ArrayList<AdditionalParameter>();

	public List<AdditionalParameter> getParameters() {
		return parameters;
	}

	public void setParameters(List<AdditionalParameter> parameters) {
		this.parameters = parameters;
	}
}
