package wasdi.shared.data.factories;

import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.mongo.MongoDataLayerBootstrap;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Single entry point used by applications to initialize the active data layer.
 */
public class DataLayerManager {

	private static IDataLayerBootstrap s_oBootstrap;

	private DataLayerManager() {
	}

	/**
	 * Allows overriding the default bootstrap (Mongo) with another implementation.
	 *
	 * @param oBootstrap bootstrap implementation to use
	 */
	public static void setBootstrap(IDataLayerBootstrap oBootstrap) {
		if (oBootstrap != null) {
			s_oBootstrap = oBootstrap;
		}
	}

	/**
	 * Initialize active data layer.
	 */
	public static void init() {
		if (s_oBootstrap == null) {
			s_oBootstrap = createBootstrapByDbEngine();
		}

		s_oBootstrap.init();
	}

	/**
	 * Shutdown active data layer.
	 */
	public static void shutdown() {
		if (s_oBootstrap == null) {
			s_oBootstrap = createBootstrapByDbEngine();
		}

		s_oBootstrap.shutdown();
	}

	private static IDataLayerBootstrap createBootstrapByDbEngine() {
		String sDbEngine = "wasdi";

		if (WasdiConfig.Current != null && WasdiConfig.Current.dbEngine != null) {
			sDbEngine = WasdiConfig.Current.dbEngine.trim().toLowerCase();
		}

		if ("no2".equals(sDbEngine)) {
			WasdiLog.warnLog("DataLayerManager.createBootstrapByDbEngine: dbEngine=no2 is configured but NO2 bootstrap is not available yet. Falling back to Mongo.");
			return new MongoDataLayerBootstrap();
		}

		if ("wasdi".equals(sDbEngine) || "mongo".equals(sDbEngine)) {
			return new MongoDataLayerBootstrap();
		}

		WasdiLog.warnLog("DataLayerManager.createBootstrapByDbEngine: unsupported dbEngine='" + sDbEngine + "'. Falling back to Mongo.");
		return new MongoDataLayerBootstrap();
	}
}
