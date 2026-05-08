package wasdi.shared.utils.log;

import org.apache.logging.log4j.Logger;

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
		
		if (m_oLogger!=null) {
			m_oLogger.debug(m_sPrefix + " " + sText);
		} 
		else {
			System.out.println(m_sPrefix + " " + sText);
		}
	}
	
	public void warn(String sText) {
		
		if (m_oLogger!=null) {
			m_oLogger.warn(m_sPrefix + " " + sText);
		} 
		else {
			System.out.println(m_sPrefix + " " + sText);
		}		
	}
	
	public void info(String sText) {
		if (m_oLogger!=null) {
			m_oLogger.info(m_sPrefix + " " + sText);
		} 
		else {
			System.out.println(m_sPrefix + " " + sText);
		}		
		
	}
	
	public void error(String sText) {
		if (m_oLogger!=null) {
			m_oLogger.error(m_sPrefix + " " + sText);
		} 
		else {
			System.out.println(m_sPrefix + " " + sText);
		}		
	}
	
	public void error(String sText, Exception oEx) {
		if (m_oLogger!=null) {
			m_oLogger.error(m_sPrefix + " " + sText, oEx);
		} 
		else {
			System.out.println(m_sPrefix + " " + sText);
		}		
	}
}
