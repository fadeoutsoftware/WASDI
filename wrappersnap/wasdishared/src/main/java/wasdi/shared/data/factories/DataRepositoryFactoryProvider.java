package wasdi.shared.data.factories;

import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.no2.No2DataRepositoryFactory;
import wasdi.shared.data.mongo.MongoDataRepositoryFactory;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Singleton provider for repository backend factory.
 */
public class DataRepositoryFactoryProvider {
    private static IDataRepositoryFactory m_oDataRepositoryFactory;

    private DataRepositoryFactoryProvider() {
    }

    public static IDataRepositoryFactory getFactory() {
        if (m_oDataRepositoryFactory == null) {
            m_oDataRepositoryFactory = createFactoryByDbEngine();
        }

        return m_oDataRepositoryFactory;
    }

    private static IDataRepositoryFactory createFactoryByDbEngine() {
        String sDbEngine = "wasdi";

        if (WasdiConfig.Current != null && WasdiConfig.Current.dbEngine != null) {
            sDbEngine = WasdiConfig.Current.dbEngine.trim().toLowerCase();
        }

        if ("no2".equals(sDbEngine)) {
            WasdiLog.warnLog("DataRepositoryFactoryProvider.createFactoryByDbEngine: dbEngine=no2 selected. Using transitional NO2 factory.");
            return new No2DataRepositoryFactory();
        }

        if ("wasdi".equals(sDbEngine) || "mongo".equals(sDbEngine)) {
            return new MongoDataRepositoryFactory();
        }

        WasdiLog.warnLog("DataRepositoryFactoryProvider.createFactoryByDbEngine: unsupported dbEngine='" + sDbEngine + "'. Falling back to Mongo.");
        return new MongoDataRepositoryFactory();
    }
}
