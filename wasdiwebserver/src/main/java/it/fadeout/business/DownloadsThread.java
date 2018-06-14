package it.fadeout.business;

import java.util.Collections;
import java.util.List;

import javax.servlet.ServletConfig;

import wasdi.shared.business.ProcessWorkspace;

public class DownloadsThread extends ProcessingThread {

	public DownloadsThread(ServletConfig servletConfig) throws Exception {
		super(servletConfig, "ConcurrentDownloads");
		m_sLogPrefix = "DownloadsThread: ";
	}

	@Override
	protected List<ProcessWorkspace> getQueuedProcess() {
		List<ProcessWorkspace> queuedProcess = m_oProcessWorkspaceRepository.GetQueuedDownloads();
		
//		System.out.println("DownloadsThread: read download queue. size: " + queuedProcess.size());
//		for (ProcessWorkspace p : queuedProcess) {
//			System.out.println("DownloadsThread:      " + p.getProcessObjId());
//		}
		// Reverse the collection, otherwise the older will dead of starvation
		Collections.reverse(queuedProcess);

		return queuedProcess;
		
	}
}
