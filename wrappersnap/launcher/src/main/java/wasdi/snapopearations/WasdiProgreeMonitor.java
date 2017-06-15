package wasdi.snapopearations;

import com.bc.ceres.core.ProgressMonitor;
import com.fasterxml.jackson.core.JsonProcessingException;

import wasdi.LauncherMain;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.data.ProcessWorkspaceRepository;

public class WasdiProgreeMonitor implements ProgressMonitor {
	
	private ProcessWorkspaceRepository processRepository = null;
	private ProcessWorkspace process = null;
	private int totalSteps = 0;
	private int computedStep = 0;
	private int computedIntervals = 0;
	private int intervalPercStep = 5;
	
	public WasdiProgreeMonitor(ProcessWorkspaceRepository processRepository, ProcessWorkspace process) {
		super();
		this.processRepository = processRepository;
		this.process = process;
	}

	@Override
	public void worked(int work) {
		computedStep += work;
		if (totalSteps!=0) {
			int tmp = (int)(((float)computedStep / (float) totalSteps) * 100);
			if (tmp%intervalPercStep == 0 && tmp != computedIntervals) {
				computedIntervals = tmp;
				
				LauncherMain.s_oLogger.debug("WasdiProgreeMonitor: PROGRESS " + computedIntervals + "%");
				
				if (process != null) {
	            	
	                //get process pid
					process.setProgressPerc(computedIntervals);
	                //update the process
	                if (!processRepository.UpdateProcess(process)) {
	                	LauncherMain.s_oLogger.debug("WasdiProgreeMonitor: Error during process update (starting)");
	                } else {
	                	LauncherMain.s_oLogger.debug("WasdiProgreeMonitor: updated progress for process " + process.getProcessObjId() + " : " + computedIntervals);
	                }
	                
	                try {
						LauncherMain.s_oSendToRabbit.SendUpdateProcessMessage(process);
					} catch (JsonProcessingException e) {
						e.printStackTrace();
						LauncherMain.s_oLogger.error("WasdiProgreeMonitor: Error during SendUpdateProcessMessage: " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e));
					}
	                    
	            }        	
				
			}
		}
	}

	@Override
	public void setTaskName(String taskName) {
		LauncherMain.s_oLogger.debug("WasdiProgreeMonitor: PROGRESS: setTaskName: " + taskName);
	}

	@Override
	public void setSubTaskName(String subTaskName) {
		LauncherMain.s_oLogger.debug("WasdiProgreeMonitor: PROGRESS: setSubTaskName: " + subTaskName);
	}

	@Override
	public void setCanceled(boolean canceled) {
		LauncherMain.s_oLogger.debug("WasdiProgreeMonitor: PROGRESS: setCanceled: " + canceled);		
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
		LauncherMain.s_oLogger.debug("WasdiProgreeMonitor: PROGRESS: done");				
	}

	@Override
	public void beginTask(String taskName, int totalWork) {
		LauncherMain.s_oLogger.debug("WasdiProgreeMonitor: PROGRESS: beginTask: " + taskName + " total work: " +totalWork );
		totalSteps = totalWork;
	}

}
