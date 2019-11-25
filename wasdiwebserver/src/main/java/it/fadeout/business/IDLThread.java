package it.fadeout.business;

import java.util.Collections;
import java.util.List;

import javax.servlet.ServletConfig;

import it.fadeout.Wasdi;
import wasdi.shared.business.ProcessWorkspace;

public class IDLThread extends ProcessingThread {
	
	public IDLThread(ServletConfig servletConfig) throws Exception {
		super(servletConfig, "ConcurrentIDL");
		m_sLogPrefix = "IDLThread: ";
	}

	@Override
	protected List<ProcessWorkspace> getQueuedProcess() {
		List<ProcessWorkspace> queuedProcess = m_oProcessWorkspaceRepository.GetQueuedIDLByNode(Wasdi.s_sMyNodeCode);
		
		// Reverse the collection, otherwise the older will dead of starvation
		Collections.reverse(queuedProcess);

		return queuedProcess;
		
	}
	
	@Override
	protected void waitForProcessToStart() {
		try {
			sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
