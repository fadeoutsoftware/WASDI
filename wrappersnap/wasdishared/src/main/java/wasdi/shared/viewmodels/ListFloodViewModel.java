package wasdi.shared.viewmodels;

public class ListFloodViewModel {
	
	private String referenceFile;
	private String postEventFile;
	private String outputMaskFile;
	private String outputFloodMapFile;
	
	private int hsbaStartDepth;
	private double bimodalityCoeff;
	private int minTileDimension;
	private int minBlobRemoval;
	
	
	
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
	public int getHsbaStartDepth() {
		return hsbaStartDepth;
	}
	public void setHsbaStartDepth(int hsbaStartDepth) {
		this.hsbaStartDepth = hsbaStartDepth;
	}
	public double getBimodalityCoeff() {
		return bimodalityCoeff;
	}
	public void setBimodalityCoeff(double bimodalityCoeff) {
		this.bimodalityCoeff = bimodalityCoeff;
	}
	public int getMinTileDimension() {
		return minTileDimension;
	}
	public void setMinTileDimension(int minTileDimension) {
		this.minTileDimension = minTileDimension;
	}
	public int getMinBlobRemoval() {
		return minBlobRemoval;
	}
	public void setMinBlobRemoval(int minBlobRemoval) {
		this.minBlobRemoval = minBlobRemoval;
	}

}
