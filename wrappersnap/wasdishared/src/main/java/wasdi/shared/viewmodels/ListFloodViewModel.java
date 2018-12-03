package wasdi.shared.viewmodels;

public class ListFloodViewModel {
	
	private String referenceFile;
	private String postEventFile;
	private String outputMaskFile;
	private String outputFloodMapFile;
	
	
	public String getReferenceFile() {
		return referenceFile;
	}
	public void setReferenceFile(String referenceFile) {
		this.referenceFile = referenceFile;
	}
	public String getPostEventFile() {
		return postEventFile;
	}
	public void setPostEventFile(String postEventFile) {
		this.postEventFile = postEventFile;
	}
	public String getOutputMaskFile() {
		return outputMaskFile;
	}
	public void setOutputMaskFile(String outputMaskFile) {
		this.outputMaskFile = outputMaskFile;
	}
	public String getOutputFloodMapFile() {
		return outputFloodMapFile;
	}
	public void setOutputFloodMapFile(String outputFloodMapFile) {
		this.outputFloodMapFile = outputFloodMapFile;
	}

}
