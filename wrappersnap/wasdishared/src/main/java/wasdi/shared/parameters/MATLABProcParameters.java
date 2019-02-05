package wasdi.shared.parameters;

public class MATLABProcParameters extends BaseParameter {
	private String m_sProcessorName;
	private String m_sConfigFilePath;
	private String m_sParamFilePath;
	
	public String getProcessorName() {
		return m_sProcessorName;
	}
	public void setProcessorName(String sProcessorName) {
		this.m_sProcessorName = sProcessorName;
	}
	public String getConfigFilePath() {
		return m_sConfigFilePath;
	}
	public void setConfigFilePath(String sConfigFilePath) {
		this.m_sConfigFilePath = sConfigFilePath;
	}
	public String getParamFilePath() {
		return m_sParamFilePath;
	}
	public void setParamFilePath(String sParamFilePath) {
		this.m_sParamFilePath = sParamFilePath;
	}

}
