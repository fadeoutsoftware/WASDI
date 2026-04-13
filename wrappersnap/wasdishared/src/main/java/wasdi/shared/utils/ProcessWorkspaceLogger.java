package wasdi.shared.utils;

import java.util.Date;

import wasdi.shared.business.processors.ProcessorLog;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ProcessorLogRepository;
import wasdi.shared.utils.log.WasdiLog;

public class ProcessWorkspaceLogger {
	
	String m_sProcessWorkspaceId;
	ProcessorLogRepository m_oProcessorLogRepository = new ProcessorLogRepository();
	
	public ProcessWorkspaceLogger(String sProcessWorkspaceId) {
		m_sProcessWorkspaceId = sProcessWorkspaceId;
	}
	
	public void log(String sLogLine) {
		
		try {
			if (Utils.isNullOrEmpty(m_sProcessWorkspaceId)) {
				WasdiLog.warnLog("ProcessWorkspaceLogger.log: Proc WS Id not valid. Log on console - " + sLogLine);
			}
			else if (WasdiConfig.Current.logAppsOnDb==false && WasdiConfig.Current.useLauncherApiLogger == true) {
				// Mini-Wasdi Condition: we do not use the process workspace logger in this case!
				return;
			}
			else {
				ProcessorLog oProcessorLog = new ProcessorLog();
				oProcessorLog.setLogRow(sLogLine);
				oProcessorLog.setLogDate(Utils.getFormatDate(new Date()));
				oProcessorLog.setProcessWorkspaceId(m_sProcessWorkspaceId);
				
				m_oProcessorLogRepository.insertProcessLog(oProcessorLog);			
			}			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessWorkspaceLogger.log: Exception ", oEx);
		}
	}

}
