package wasdi.snapopearations;

import com.bc.ceres.core.ProgressMonitor;
import com.fasterxml.jackson.core.JsonProcessingException;

import wasdi.LauncherMain;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Wasdi Progres Monitor Listener
 * @author p.campanella
 *
 */
public class WasdiProgressMonitor implements ProgressMonitor {
	
	private ProcessWorkspaceRepository m_oProcessRepository = null;
	private ProcessWorkspace m_oProcess = null;
	private int m_iTotalSteps = 0;
	private int m_iComputedStep = 0;
	private int m_iComputedIntervals = 0;
	private int m_iIntervalPercStep = 5;
	
	public WasdiProgressMonitor(ProcessWorkspaceRepository oProcessRepository, ProcessWorkspace oProcess) {
		super();
		this.m_oProcessRepository = oProcessRepository;
		this.m_oProcess = oProcess;
	}

	@Override
	public void worked(int iWork) {
		m_iComputedStep += iWork;
		if (m_iTotalSteps!=0) {
			int tmp = (int)(((float)m_iComputedStep / (float) m_iTotalSteps) * 100);
			if (tmp%m_iIntervalPercStep == 0 && tmp != m_iComputedIntervals) {
				m_iComputedIntervals = tmp;
				
				
				
				if (m_oProcess != null) {
	            	
	                //get process pid
					m_oProcess.setProgressPerc(m_iComputedIntervals);
					
					if (m_oProcessRepository!=null) {
		                //update the process
		                if (!m_oProcessRepository.updateProcess(m_oProcess)) {
		                	WasdiLog.debugLog("WasdiProgressMonitor: Error during process update");
		                } else {
		                	WasdiLog.debugLog("WasdiProgressMonitor: PROGRESS " + m_iComputedIntervals + "% ProcId: " + m_oProcess.getProcessObjId());
		                }						
					}
	                
	                try {
						if (LauncherMain.s_oSendToRabbit != null) LauncherMain.s_oSendToRabbit.SendUpdateProcessMessage(m_oProcess);
					} 
	                catch (JsonProcessingException e) {
						WasdiLog.errorLog("WasdiProgressMonitor: Error during SendUpdateProcessMessage: ", e);
					}
	                    
	            }
				else {
					WasdiLog.debugLog("WasdiProgressMonitor: process is null");
				}
				
			}
		}
	}

	@Override
	public void setTaskName(String sTaskName) {
		WasdiLog.debugLog("WasdiProgressMonitor: PROGRESS: setTaskName: " + sTaskName);
	}

	@Override
	public void setSubTaskName(String sSubTaskName) {
		WasdiLog.debugLog("WasdiProgressMonitor: PROGRESS: setSubTaskName: " + sSubTaskName);
	}

	@Override
	public void setCanceled(boolean bCanceled) {
		WasdiLog.debugLog("WasdiProgressMonitor: PROGRESS: setCanceled: " + bCanceled);		
	}

	@Override
	public boolean isCanceled() {
		return false;
	}

	@Override
	public void internalWorked(double work) {
	}

	@Override
	public void done() {
		WasdiLog.debugLog("WasdiProgressMonitor: PROGRESS: done");				
	}

	@Override
	public void beginTask(String sTaskName, int iTotalWork) {
		WasdiLog.debugLog("WasdiProgressMonitor: PROGRESS: beginTask: " + sTaskName + " total work: " +iTotalWork );
		m_iTotalSteps = iTotalWork;
	}

}
