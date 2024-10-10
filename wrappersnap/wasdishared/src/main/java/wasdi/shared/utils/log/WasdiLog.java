package wasdi.shared.utils.log;

import java.time.LocalDateTime;

import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.Utils;

/**
 * Wasdi loggers utils.
 * The class offers static methods for a basic loggin functionality.
 * Different levels are foreseen: DEBUG, INFO, WARNING, ERROR.
 * 
 * The different projects using the shared lib should log with these methods.
 * 
 * The users can set a LoggerWrapper to use for logging.
 * Anywhow this logger will print on the stdout stream.
 * 
 * @author p.campanella
 *
 */
public class WasdiLog {
	
	/**
	 * Reference to the logger wrapper to use
	 */
	protected static LoggerWrapper s_oLoggerWrapper = null;
	
	/**
	 * Prefix to the log strings
	 */
	protected static String s_sPrefix = "";
	
	/**
	 * Set the active logger wrapper
	 * @param oLoggerWrapper
	 */
	public static void setLoggerWrapper(LoggerWrapper oLoggerWrapper) {
		s_oLoggerWrapper = oLoggerWrapper;
	}	
	
	/**
	 * Debug Log
	 * 
	 * @param sMessage
	 */
	public static void debugLog(String sMessage) {
		log(WasdiLogLevels.DEBUG, sMessage);
	}
	
	/**
	 * Info log
	 * @param sMessage
	 */
	public static void infoLog(String sMessage) {
		log(WasdiLogLevels.INFO, sMessage);
	}
	
	/**
	 * Warning log
	 * @param sMessage
	 */
	public static void warnLog(String sMessage) {
		log(WasdiLogLevels.WARNING, sMessage);
	}
	
	/**
	 * Error log
	 * @param sMessage
	 */
	public static void errorLog(String sMessage) {
		log(WasdiLogLevels.ERROR, sMessage);
	}
	
	/**
	 * Error log
	 * @param sMessage
	 */
	public static void errorLog(String sMessage, Exception oEx) {
		String sException = "";
		
		if (oEx != null)  {
			sException = " - " + oEx.toString();
		}
		log(WasdiLogLevels.ERROR, sMessage + sException);
	}	
	
	/**
	 * Log
	 * @param oLevel Log Level
	 * @param sMessage Log Message
	 */
	public static void log(WasdiLogLevels oLevel, String sMessage) {
		log(oLevel.name(), sMessage);
	}
	
	/**
	 * Log
	 * @param sLevel Log Level
	 * @param sMessage Log Message
	 */
	public static void log(String sLevel, String sMessage) {
		
		// Add safe code to avoid empty messages
		if (Utils.isNullOrEmpty(sMessage)) return;
		
		String sPrefix = "";
		if(!Utils.isNullOrEmpty(sLevel)) {
			sPrefix = "[" + sLevel + "] ";
		}
		
		LocalDateTime oNow = LocalDateTime.now();
		
		if (s_oLoggerWrapper != null) {
			
			s_oLoggerWrapper.setPrefix(s_sPrefix);
			
			if (sLevel.equals(WasdiLogLevels.DEBUG.name())) {
				synchronized (s_oLoggerWrapper) {
					s_oLoggerWrapper.debug(sMessage);
				}
			}
			else if (sLevel.equals(WasdiLogLevels.INFO.name())) {
				synchronized (s_oLoggerWrapper) {
					s_oLoggerWrapper.info(sMessage);
				}
			}
			else if (sLevel.equals(WasdiLogLevels.WARNING.name())) {
				synchronized (s_oLoggerWrapper) {
					s_oLoggerWrapper.warn(sMessage);
				}
			}
			else if (sLevel.equals(WasdiLogLevels.ERROR.name())) {
				synchronized (s_oLoggerWrapper) {
					s_oLoggerWrapper.error(sMessage);
				}
			}
			else {
				synchronized (s_oLoggerWrapper) {
					s_oLoggerWrapper.info(sMessage);
				}
			}
		}
		else {
			String sFinalLine = sPrefix;
			
			if (WasdiConfig.Current!=null) {
				if (WasdiConfig.Current.addDateTimeToLogs) {
					sFinalLine += "" + oNow + " "; 
				}				
			}
			
			sFinalLine+= s_sPrefix + ": " + sMessage;
			System.out.println(sFinalLine);
		}
	}

	public static String getPrefix() {
		return s_sPrefix;
	}

	public static void setPrefix(String s_sPrefix) {
		WasdiLog.s_sPrefix = s_sPrefix;
	}
}
