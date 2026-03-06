package wasdi.shared.data.no2;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.dizitart.no2.Nitrite;
import org.dizitart.no2.collection.NitriteCollection;
import org.dizitart.no2.mvstore.MVStoreModule;

import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.log.WasdiLog;

/**
 * NO2 (Nitrite) connection manager.
 */
public class No2Connection {

	private static String s_sDbName;
	private static String s_sDbUser;
	private static String s_sDbPassword;
	private static String s_sDbFilePath;
	private static Nitrite s_oNo2Db;

	private No2Connection() {
	}

	public static void readConfig() {
		try {
			if (WasdiConfig.Current == null || WasdiConfig.Current.mongoMain == null) {
				WasdiLog.warnLog("No2Connection.readConfig: WasdiConfig.Current.mongoMain is null.");
				return;
			}

			s_sDbName = WasdiConfig.Current.mongoMain.dbName;
			s_sDbUser = WasdiConfig.Current.mongoMain.user;
			s_sDbPassword = WasdiConfig.Current.mongoMain.password;

			if (s_sDbName == null || s_sDbName.isEmpty()) {
				s_sDbName = "wasdi";
			}

			if (s_sDbUser == null) {
				s_sDbUser = "";
			}

			if (s_sDbPassword == null) {
				s_sDbPassword = "";
			}

			String sBasePath = "/data/wasdi/";

			if (WasdiConfig.Current.paths != null && WasdiConfig.Current.paths.downloadRootPath != null
					&& !WasdiConfig.Current.paths.downloadRootPath.isEmpty()) {
				sBasePath = WasdiConfig.Current.paths.downloadRootPath;
			}

			Path oNo2DirectoryPath = Paths.get(sBasePath, "no2db");
			Files.createDirectories(oNo2DirectoryPath);
			s_sDbFilePath = oNo2DirectoryPath.resolve(s_sDbName + ".db").toString();

			WasdiLog.debugLog("No2Connection.readConfig: NO2 DB file = " + s_sDbFilePath);
		}
		catch (Exception oException) {
			WasdiLog.errorLog("No2Connection.readConfig exception", oException);
		}
	}

	public static String getDbName() {
		return s_sDbName;
	}

	public static String getDbUser() {
		return s_sDbUser;
	}

	public static String getDbPassword() {
		return s_sDbPassword;
	}

	public static synchronized void init() {
		try {
			if (s_oNo2Db != null && !s_oNo2Db.isClosed()) {
				return;
			}

			if (s_sDbFilePath == null || s_sDbFilePath.isEmpty()) {
				readConfig();
			}

			MVStoreModule oStoreModule = MVStoreModule.withConfig()
					.filePath(s_sDbFilePath)
					.compress(true)
					.build();

			s_oNo2Db = Nitrite.builder()
					.loadModule(oStoreModule)
					.openOrCreate(s_sDbUser, s_sDbPassword);

			WasdiLog.infoLog("No2Connection.init: NO2 database opened");
		}
		catch (Exception oException) {
			WasdiLog.errorLog("No2Connection.init exception", oException);
		}
	}

	public static synchronized NitriteCollection getCollection(String sCollectionName) {
		init();

		if (s_oNo2Db == null || s_oNo2Db.isClosed()) {
			WasdiLog.warnLog("No2Connection.getCollection: NO2 DB is not available");
			return null;
		}

		return s_oNo2Db.getCollection(sCollectionName);
	}

	public static synchronized void shutdown() {
		try {
			if (s_oNo2Db != null && !s_oNo2Db.isClosed()) {
				s_oNo2Db.close();
				WasdiLog.infoLog("No2Connection.shutdown: NO2 database closed");
			}
		}
		catch (Exception oException) {
			WasdiLog.errorLog("No2Connection.shutdown exception", oException);
		}
	}
}
