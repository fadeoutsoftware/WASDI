package it.fadeout;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;


public class WasdiLifeCycleListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		Wasdi.debugLog("WasdiLifeCycleListener.contextInitialized");
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		Wasdi.debugLog("Call Wasdi Shut Down. Bye");
		Wasdi.shutDown();
	}
}
