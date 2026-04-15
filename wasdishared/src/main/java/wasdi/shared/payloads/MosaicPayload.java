package wasdi.shared.payloads;

import wasdi.shared.LauncherOperations;

/**
 * Payload of the Mosaic Operation
 * 
 * @author p.campanella
 *
 */
public class MosaicPayload extends OperationPayload {
	
	/**
	 * List of input files
	 */
	private String [] inputs;
	
	/**
	 * Output file
	 */
	private String output;
	
	public MosaicPayload() {
		this.operation= LauncherOperations.MOSAIC.name();
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
