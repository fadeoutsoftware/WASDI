package wasdi.shared.utils;

import java.util.Date;

import wasdi.shared.business.processors.ProcessorLog;
import wasdi.shared.data.ProcessorLogRepository;
import wasdi.shared.utils.log.WasdiLog;

public class ProcessWorkspaceLogger {
	
	String m_sProcessWorkspaceId;
	ProcessorLogRepository m_oProcessorLogRepository = new ProcessorLogRepository();
	
	public ProcessWorkspaceLogger(String sProcessWorkspaceId) {
		m_sProcessWorkspaceId = sProcessWorkspaceId;
	}
	
	public void log(String sLogLine) {
		
		if (Utils.isNullOrEmpty(m_sProcessWorkspaceId)) {
			WasdiLog.warnLog("ProcessWorkspaceLogger: Proc WS Id not valid. Log on console - " + sLogLine);
		}
		else {
			ProcessorLog oProcessorLog = new ProcessorLog();
			oProcessorLog.setLogRow(sLogLine);
			oProcessorLog.setLogDate(Utils.getFormatDate(new Date()));
			oProcessorLog.setProcessWorkspaceId(m_sProcessWorkspaceId);
			
			m_oProcessorLogRepository.insertProcessLog(oProcessorLog);			
		}
	}

}
