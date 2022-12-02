package ogc.wasdi.processes;

import javax.annotation.PostConstruct;

import org.glassfish.jersey.server.ResourceConfig;

import wasdi.shared.utils.log.WasdiLog;

public class OgcProcesses extends ResourceConfig {
	
	public OgcProcesses() {
		packages(true, "ogc.wasdi.processes.rest.resources");
	}

	/**
	 * Web Server intialization: it loads the main web-server configuration
	 */
	@PostConstruct
	public void initOgcProcesses() {
		WasdiLog.debugLog("WASDI OGC-Processes Server start");
	}
}
