package net.wasdi.openeoserver;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import wasdi.shared.utils.log.WasdiLog;

public class WasdiOpenEoServerLifeCycleListener implements ServletContextListener {
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		WasdiLog.debugLog("WasdiOpenEoServerLifeCycleListener");
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		WasdiLog.debugLog("Call WasdiOpenEoServer Shut Down. Bye");
		WasdiOpenEoServer.shutDown();
	}	

}
