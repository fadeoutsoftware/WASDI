package wasdi.shared.utils.log;

import java.time.LocalDateTime;

import wasdi.shared.utils.Utils;

public class WasdiLog {
	//////////////////////// Log short cuts for the lib
	
	/**
	 * Debug Log
	 * 
	 * @param sMessage
	 */
	public static void debugLog(String sMessage) {
		log("DEBUG", sMessage);
	}
	
	/**
	 * Info log
	 * @param sMessage
	 */
	public static void infoLog(String sMessage) {
		log("INFO", sMessage);
	}
	
	/**
	 * Warning log
	 * @param sMessage
	 */
	public static void warnLog(String sMessage) {
		log("WARNING", sMessage);
	}
	
	/**
	 * Error log
	 * @param sMessage
	 */
	public static void errorLog(String sMessage) {
		log("ERROR", sMessage);
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
		log("ERROR", sMessage + sException);
	}	
	
	public static LoggerWrapper s_oLoggerWrapper = null;
	
	/**
	 * Log
	 * @param sLevel
	 * @param sMessage
	 */
	public static void log(String sLevel, String sMessage) {
		String sPrefix = "";
		if(!Utils.isNullOrEmpty(sLevel)) {
			sPrefix = "[" + sLevel + "] ";
		}
		
		LocalDateTime oNow = LocalDateTime.now();
		
		if (s_oLoggerWrapper != null) {
			
			if (sLevel.equals("DEBUG")) {
				s_oLoggerWrapper.debug(sMessage);	
			}
			else if (sLevel.equals("INFO")) {
				s_oLoggerWrapper.info(sMessage);
			}
			else if ( sLevel.equals("WARNING")) {
				s_oLoggerWrapper.warn(sMessage);
			}
			else if (sLevel.equals("ERROR")) {
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
