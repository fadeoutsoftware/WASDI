package it.fadeout;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import wasdi.shared.utils.Utils;


public class WasdiLifeCycleListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		Utils.debugLog("WasdiLifeCycleListener.contextInitialized");
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		Utils.debugLog("Call Wasdi Shut Down. Bye");
		Wasdi.shutDown();
	}
}
