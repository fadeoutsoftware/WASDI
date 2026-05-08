package wasdi.shared.data.mongo;

import wasdi.shared.data.factories.IDataLayerBootstrap;

/**
 * Default data layer bootstrap based on Mongo repositories.
 */
public class MongoDataLayerBootstrap implements IDataLayerBootstrap {

	@Override
	public void init() {
		MongoRepository.readConfig();
	}

	@Override
	public void shutdown() {
		MongoRepository.shutDownConnection();
	}
}
