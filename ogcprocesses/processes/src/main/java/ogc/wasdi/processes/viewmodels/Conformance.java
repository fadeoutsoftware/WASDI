package ogc.wasdi.processes.viewmodels;

import java.util.ArrayList;
import java.util.List;

public class Conformance {
	private List<String> conformsTo = new ArrayList<String>();

	public List<String> getConformsTo() {
		return conformsTo;
	}

	public void setConformsTo(List<String> conformsTo) {
		this.conformsTo = conformsTo;
	}

}
