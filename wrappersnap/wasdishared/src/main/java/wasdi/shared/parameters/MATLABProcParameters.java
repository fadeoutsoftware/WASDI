package wasdi.shared.parameters;

/**
 * Parameter of the RUNMATLAB Operation
 * @author p.campanella
 *
 */
public class MATLABProcParameters extends BaseParameter {
	
	/**
	 * Name of the processor
	 */
	private String m_sProcessorName;
	
	/**
	 * Path of processor config file
	 */
	private String m_sConfigFilePath;
	
	/**
	 * Path of the processor parameter file with the inputs
	 */
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
