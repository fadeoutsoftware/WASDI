package wasdi.shared.utils.log;

import java.time.LocalDateTime;

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
	 * Reference to the logger wrapper to use
	 */
	public static LoggerWrapper s_oLoggerWrapper = null;
	
	/**
	 * Set the active logger wrapper
	 * @param oLoggerWrapper
	 */
	public static void setLoggerWrapper(LoggerWrapper oLoggerWrapper) {
		s_oLoggerWrapper = oLoggerWrapper;
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
		String sPrefix = "";
		if(!Utils.isNullOrEmpty(sLevel)) {
			sPrefix = "[" + sLevel + "] ";
		}
		
		LocalDateTime oNow = LocalDateTime.now();
		
		if (s_oLoggerWrapper != null) {
			
			if (sLevel.equals(WasdiLogLevels.DEBUG.name())) {
				s_oLoggerWrapper.debug(sMessage);	
			}
			else if (sLevel.equals(WasdiLogLevels.INFO.name())) {
				s_oLoggerWrapper.info(sMessage);
			}
			else if (sLevel.equals(WasdiLogLevels.WARNING.name())) {
				s_oLoggerWrapper.warn(sMessage);
			}
			else if (sLevel.equals(WasdiLogLevels.ERROR.name())) {
				s_oLoggerWrapper.error(sMessage);
			}
			else {
				s_oLoggerWrapper.info(sMessage);
			}
			
		}
		
		String sFinalLine = sPrefix + oNow + ": " + sMessage;
		System.out.println(sFinalLine);
	}
	
	///////// end log
}
