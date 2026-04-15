package wasdi.dataproviders;

public enum DataProviderScores {
	FILE_ACCESS(100), SAME_CLOUD_DOWNLOAD(90), DOWNLOAD(80), SLOW_DOWNLOAD(50), LTA(10);
	
	private final int m_iValue;
	
	private DataProviderScores(int iValue) {
		this.m_iValue = iValue;
	}
	
	public int getValue() {
		return m_iValue;
	}
}
