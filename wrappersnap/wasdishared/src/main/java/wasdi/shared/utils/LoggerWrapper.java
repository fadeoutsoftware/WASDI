package wasdi.shared.utils;

import org.apache.log4j.Logger;

public class LoggerWrapper {
	
	private Logger m_oLogger;
	
	private String m_sPrefix = "";
	
	public String getPrefix() {
		return m_sPrefix;
	}

	public void setPrefix(String sPrefix) {
		this.m_sPrefix = sPrefix;
	}

	public LoggerWrapper(Logger oLog4jLogger) {
		m_oLogger = oLog4jLogger;
	}
	
	public void debug(String sText) {
		m_oLogger.debug(m_sPrefix + " " + sText);
	}
	
	public void warn(String sText) {
		m_oLogger.warn(m_sPrefix + " " + sText);
	}
	
	public void info(String sText) {
		m_oLogger.info(m_sPrefix + " " + sText);
	}
	
	public void error(String sText) {
		m_oLogger.error(m_sPrefix + " " + sText);
	}
	
	public void error(String sText, Exception oEx) {
		m_oLogger.error(m_sPrefix + " " + sText, oEx);
	}
}
