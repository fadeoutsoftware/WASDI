package it.fadeout;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import wasdi.shared.utils.log.WasdiLog;


public class WasdiLifeCycleListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		WasdiLog.debugLog("WasdiLifeCycleListener.contextInitialized");
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		WasdiLog.debugLog("Call Wasdi Shut Down. Bye");
		Wasdi.shutDown();
	}
}
