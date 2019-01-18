package wasdi.shared.parameters;

public class IDLProcParameter extends BaseParameter {
	private String m_sProcessorName;
	private String m_sParameterFile;
	
	public String getProcessorName() {
		return m_sProcessorName;
	}
	public void setProcessorName(String sProcessorName) {
		this.m_sProcessorName = sProcessorName;
	}
	public String getParameterFile() {
		return m_sParameterFile;
	}
	public void setParameterFile(String sParameterFile) {
		this.m_sParameterFile = sParameterFile;
	}
	
}
