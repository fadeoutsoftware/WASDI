package wasdi.shared.viewmodels;

public class JRCTestViewModel {
	private String inputFileName;
	private String epsg;
	private String outputFileName;
	private String preprocess;
	
	public String getPreprocess() {
		return preprocess;
	}
	public void setPreprocess(String preprocess) {
		this.preprocess = preprocess;
	}
	public String getInputFileName() {
		return inputFileName;
	}
	public void setInputFileName(String inputFileName) {
		this.inputFileName = inputFileName;
	}
	public String getEpsg() {
		return epsg;
	}
	public void setEpsg(String epsg) {
		this.epsg = epsg;
	}
	public String getOutputFileName() {
		return outputFileName;
	}
	public void setOutputFileName(String outputFileName) {
		this.outputFileName = outputFileName;
	}
}
