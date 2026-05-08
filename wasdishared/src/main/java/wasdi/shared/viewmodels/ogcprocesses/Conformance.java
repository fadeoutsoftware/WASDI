package wasdi.shared.viewmodels.ogcprocesses;

import java.util.ArrayList;
import java.util.List;

public class Conformance extends OgcProcessesViewModel  {
	private List<String> conformsTo = new ArrayList<String>();

	public List<String> getConformsTo() {
		return conformsTo;
	}

	public void setConformsTo(List<String> conformsTo) {
		this.conformsTo = conformsTo;
	}

}
