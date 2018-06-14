package it.fadeout;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;


public class WasdiLifeCycleListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent sce) {
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		System.out.println("Call Wasdi Shut Down. Bye");
		Wasdi.shutDown();
	}

}
