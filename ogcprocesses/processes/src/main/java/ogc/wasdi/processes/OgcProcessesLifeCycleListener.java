package ogc.wasdi.processes;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import wasdi.shared.utils.Utils;

public class OgcProcessesLifeCycleListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		Utils.debugLog("OgcProcessesLifeCycleListener.contextInitialized");
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		Utils.debugLog("Call OgcProcessesLifeCycleListener Shut Down. Bye");
	}

}
