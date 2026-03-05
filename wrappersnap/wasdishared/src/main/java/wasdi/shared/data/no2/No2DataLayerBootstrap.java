package wasdi.shared.data.no2;

import wasdi.shared.data.factories.IDataLayerBootstrap;
import wasdi.shared.data.mongo.MongoDataLayerBootstrap;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Transitional NO2 bootstrap.
 *
 * Initializes NO2 runtime and keeps Mongo bootstrap active for repositories
 * that are still delegated to Mongo during migration.
 */
public class No2DataLayerBootstrap implements IDataLayerBootstrap {

	private final IDataLayerBootstrap m_oMongoBootstrap;

	public No2DataLayerBootstrap() {
		m_oMongoBootstrap = new MongoDataLayerBootstrap();
	}

	@Override
	public void init() {
		No2Connection.readConfig();
		No2Connection.init();
		WasdiLog.warnLog("No2DataLayerBootstrap.init: transitional mode, Mongo bootstrap kept for non-ported repositories.");
		m_oMongoBootstrap.init();
	}

	@Override
	public void shutdown() {
		No2Connection.shutdown();
		m_oMongoBootstrap.shutdown();
	}
}
