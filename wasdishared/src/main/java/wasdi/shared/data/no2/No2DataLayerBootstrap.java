package wasdi.shared.data.no2;

import wasdi.shared.data.factories.IDataLayerBootstrap;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Transitional NO2 bootstrap.
 *
 * Initializes NO2 runtime 
 */
public class No2DataLayerBootstrap implements IDataLayerBootstrap {

	public No2DataLayerBootstrap() {
	}

	@Override
	public void init() {
		No2Connection.readConfig();
		No2Connection.init();
		WasdiLog.warnLog("No2DataLayerBootstrap.init: no2 init done.");
	}

	@Override
	public void shutdown() {
		No2Connection.shutdown();
	}
}
