package wasdi.shared.payload;

public class MosaicPayload extends OperationPayload {
	private String [] inputs;
	private String output;
	
	public MosaicPayload() {
		this.operation="MOSAIC";
	}

	public String[] getInputs() {
		return inputs;
	}

	public void setInputs(String[] inputs) {
		this.inputs = inputs;
	}

	public String getOutput() {
		return output;
	}

	public void setOutput(String output) {
		this.output = output;
	}
	
}
