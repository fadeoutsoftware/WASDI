package wasdi.shared.payloads;

import wasdi.shared.LauncherOperations;

/**
 * Payload of the Multi Sub set operation
 * 
 * @author p.campanella
 *
 */
public class MultiSubsetPayload extends OperationPayload {
	
	/**
	 * Input file
	 */
	String inputFile;
	
	/**
	 * List of output files
	 */
	String [] outputFiles;

	public MultiSubsetPayload() {
		this.operation =LauncherOperations.MULTISUBSET.name();
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
