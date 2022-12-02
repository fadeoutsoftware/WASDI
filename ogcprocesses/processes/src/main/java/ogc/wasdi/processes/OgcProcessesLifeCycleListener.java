package ogc.wasdi.processes;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import wasdi.shared.utils.log.WasdiLog;

public class OgcProcessesLifeCycleListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		WasdiLog.debugLog("OgcProcessesLifeCycleListener.contextInitialized");
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		WasdiLog.debugLog("Call OgcProcessesLifeCycleListener Shut Down. Bye");
	}

}
