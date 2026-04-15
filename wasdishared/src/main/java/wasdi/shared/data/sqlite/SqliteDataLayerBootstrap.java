package wasdi.shared.data.sqlite;

import wasdi.shared.data.factories.IDataLayerBootstrap;
import wasdi.shared.utils.log.WasdiLog;

/**
 * SQLite bootstrap — initializes the WAL-mode SQLite connection.
 */
public class SqliteDataLayerBootstrap implements IDataLayerBootstrap {

	public SqliteDataLayerBootstrap() {
	}

	@Override
	public void init() {
		SqliteConnection.readConfig();
		SqliteConnection.init();
		WasdiLog.warnLog("SqliteDataLayerBootstrap.init: SQLite init done.");
	}

	@Override
	public void shutdown() {
		SqliteConnection.shutdown();
	}
}
