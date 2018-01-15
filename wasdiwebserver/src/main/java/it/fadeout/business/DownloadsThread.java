package it.fadeout.business;

import java.util.List;

import javax.servlet.ServletConfig;

import wasdi.shared.business.ProcessWorkspace;

public class DownloadsThread extends ProcessingThread {

	public DownloadsThread(ServletConfig servletConfig) throws Exception {
		super(servletConfig, "ConcurrentDownloads");
	}

	@Override
	protected List<ProcessWorkspace> getQueuedProcess() {
		List<ProcessWorkspace> queuedProcess = repo.GetQueuedDownloads();
		
//		System.out.println("DownloadsThread: read download queue. size: " + queuedProcess.size());
//		for (ProcessWorkspace p : queuedProcess) {
//			System.out.println("DownloadsThread:      " + p.getProcessObjId());
//		}

		return queuedProcess;
		
	}
}
