package wasdi.shared.utils;

import com.bc.ceres.core.ProgressMonitor;

public class WasdiProgressMonitorStub implements ProgressMonitor {
	String m_sTaskName = "";

	@Override
	public void beginTask(String taskName, int totalWork) {
		System.out.println("WasdiProgressMonitorStub: begin Task " + taskName);
		m_sTaskName = taskName;
	}

	@Override
	public void done() {
		System.out.println("WasdiProgressMonitorStub: Task done " + m_sTaskName);
	}

	@Override
	public void internalWorked(double work) {
		
	}

	@Override
	public boolean isCanceled() {
		
		return false;
	}

	@Override
	public void setCanceled(boolean canceled) {
		
	}

	@Override
	public void setTaskName(String taskName) {
		
	}

	@Override
	public void setSubTaskName(String subTaskName) {
		
	}

	@Override
	public void worked(int work) {
		System.out.println("WasdiProgressMonitorStub: [" + m_sTaskName+ "] Worked " + work);
	}

}
