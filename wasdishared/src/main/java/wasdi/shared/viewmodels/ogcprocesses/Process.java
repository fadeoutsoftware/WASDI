package wasdi.shared.viewmodels.ogcprocesses;

import java.util.HashMap;
import java.util.Map;

public class Process extends ProcessSummary {
	private Map<String, InputDescription> inputs = new HashMap<String, InputDescription>();
	private Map<String, OutputDescription> outputs = new HashMap<String, OutputDescription>();
	
	public Map<String, InputDescription> getInputs() {
		return inputs;
	}
	public void setInputs(Map<String, InputDescription> inputs) {
		this.inputs = inputs;
	}
	public Map<String, OutputDescription> getOutputs() {
		return outputs;
	}
	public void setOutputs(Map<String, OutputDescription> outputs) {
		this.outputs = outputs;
	}

}
