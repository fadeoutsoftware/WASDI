package wasdi.shared.payloads;

/**
 * Payload of the Multi Sub set operation
 * 
 * @author p.campanella
 *
 */
public class MultiSubsetPayload extends OperationPayload {
	
	String inputFile;
	String [] outputFiles;

	public MultiSubsetPayload() {
		this.operation ="MULTISUBSET";
	}

	public String getInputFile() {
		return inputFile;
	}

	public void setInputFile(String inputFile) {
		this.inputFile = inputFile;
	}

	public String[] getOutputFiles() {
		return outputFiles;
	}

	public void setOutputFiles(String[] outputFiles) {
		this.outputFiles = outputFiles;
	}

	
}
