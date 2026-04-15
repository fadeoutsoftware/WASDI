package wasdi.shared.data.sqlite;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.log.WasdiLog;

/**
 * SQLite connection manager.
 *
 * Opens a WAL-mode SQLite database at &lt;downloadRootPath&gt;/sqlitedb/&lt;dbName&gt;.db.
 * WAL journal mode allows multiple processes to safely access the same file
 * simultaneously: readers never block writers and writers never block readers.
 * The busy_timeout pragma makes writers wait up to 5 s for the OS-level write
 * lock instead of failing immediately.
 */
public class SqliteConnection {

	private static String s_sDbName;
	private static String s_sDbFilePath;
	private static Connection s_oConnection;
	private static boolean s_bDriverLoaded;

	private SqliteConnection() {
	}

	public static void readConfig() {
		try {
			if (WasdiConfig.Current == null || WasdiConfig.Current.mongoMain == null) {
				WasdiLog.warnLog("SqliteConnection.readConfig: WasdiConfig.Current.mongoMain is null.");
				return;
			}

			s_sDbName = WasdiConfig.Current.mongoMain.dbName;

			if (s_sDbName == null || s_sDbName.isEmpty()) {
				s_sDbName = "wasdi";
			}

			String sBasePath = "/data/wasdi/";

			if (WasdiConfig.Current.paths != null && WasdiConfig.Current.paths.downloadRootPath != null
					&& !WasdiConfig.Current.paths.downloadRootPath.isEmpty()) {
				sBasePath = WasdiConfig.Current.paths.downloadRootPath;
			}

			Path oSqliteDirectoryPath = Paths.get(sBasePath, "sqlitedb");
			Files.createDirectories(oSqliteDirectoryPath);
			s_sDbFilePath = oSqliteDirectoryPath.resolve(s_sDbName + ".db").toString();

			WasdiLog.debugLog("SqliteConnection.readConfig: SQLite DB file = " + s_sDbFilePath);
		}
		catch (Exception oException) {
			WasdiLog.errorLog("SqliteConnection.readConfig exception", oException);
		}
	}

	public static synchronized void init() {
		try {
			if (s_oConnection != null && !s_oConnection.isClosed()) {
				return;
			}

			if (!s_bDriverLoaded) {
				Class.forName("org.sqlite.JDBC");
				s_bDriverLoaded = true;
			}

			if (s_sDbFilePath == null || s_sDbFilePath.isEmpty()) {
				readConfig();
			}

			if (s_sDbFilePath == null || s_sDbFilePath.isEmpty()) {
				WasdiLog.errorLog("SqliteConnection.init: DB file path is not configured.");
				return;
			}

			s_oConnection = DriverManager.getConnection("jdbc:sqlite:" + s_sDbFilePath);

			try (Statement oStmt = s_oConnection.createStatement()) {
				oStmt.execute("PRAGMA journal_mode=WAL");
				oStmt.execute("PRAGMA busy_timeout=5000");
			}

			WasdiLog.debugLog("SqliteConnection.init: SQLite database opened (WAL mode) at " + s_sDbFilePath);
		}
		catch (Exception oException) {
			WasdiLog.errorLog("SqliteConnection.init exception", oException);
		}
	}

	public static synchronized Connection getConnection() {
		try {
			if (s_oConnection == null || s_oConnection.isClosed()) {
				init();
			}
		}
		catch (Exception oException) {
			WasdiLog.errorLog("SqliteConnection.getConnection: error checking connection state", oException);
			init();
		}

		if (s_oConnection == null) {
			WasdiLog.warnLog("SqliteConnection.getConnection: SQLite connection is not available");
		}

		return s_oConnection;
	}

	public static synchronized void shutdown() {
		try {
			if (s_oConnection != null && !s_oConnection.isClosed()) {
				s_oConnection.close();
				WasdiLog.debugLog("SqliteConnection.shutdown: SQLite connection closed");
			}
		}
		catch (Exception oException) {
			WasdiLog.errorLog("SqliteConnection.shutdown exception", oException);
		}
		finally {
			s_oConnection = null;
		}
	}

	public static String getDbName() {
		return s_sDbName;
	}

	public static String getDbFilePath() {
		return s_sDbFilePath;
	}
}
