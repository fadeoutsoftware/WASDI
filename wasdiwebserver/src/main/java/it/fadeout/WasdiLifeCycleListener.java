package it.fadeout;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;


public class WasdiLifeCycleListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		//System.out.println("-----------------------contextInitialized-----------------------------");
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		System.out.println("-----------------------contextDestroyed-----------------------------");
		Wasdi.shutDown();
	}

}
