package wasdi.shared.data.sqlite;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import wasdi.shared.business.DownloadedFile;
import wasdi.shared.data.interfaces.IDownloadedFilesRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * SQLite backend implementation for downloaded files repository.
 */
public class SqliteDownloadedFilesRepositoryBackend extends SqliteRepository implements IDownloadedFilesRepositoryBackend {

	public SqliteDownloadedFilesRepositoryBackend() {
		m_sThisCollection = "downloadedfiles";
		ensureTable(m_sThisCollection);
	}

	@Override
	public boolean insertDownloadedFile(DownloadedFile oFile) {
		if (oFile == null) {
			return false;
		}

		try {
			return insert(m_sThisCollection, oFile.getFilePath(), oFile);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteDownloadedFilesRepositoryBackend.insertDownloadedFile: error", oEx);
		}

		return false;
	}

	@Override
	public boolean updateDownloadedFile(DownloadedFile oFile) {
		if (oFile == null) {
			return false;
		}

		try {
			return updateWhere(m_sThisCollection, "filePath", oFile.getFilePath(), oFile);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteDownloadedFilesRepositoryBackend.updateDownloadedFile: error", oEx);
		}

		return false;
	}

	@Override
	public boolean updateDownloadedFile(DownloadedFile oFile, String sOldPath) {
		if (oFile == null) {
			return false;
		}

		try {
			return updateWhere(m_sThisCollection, "filePath", sOldPath, oFile);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteDownloadedFilesRepositoryBackend.updateDownloadedFile(oldPath): error", oEx);
		}

		return false;
	}

	@Override
	public DownloadedFile getDownloadedFile(String sFileName) {
		try {
			return findOneWhere(m_sThisCollection, "fileName", sFileName, DownloadedFile.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteDownloadedFilesRepositoryBackend.getDownloadedFile: error", oEx);
		}

		return null;
	}

	@Override
	public DownloadedFile getDownloadedFileByPath(String sFileFullPath) {
		try {
			return findOneWhere(m_sThisCollection, "filePath", sFileFullPath, DownloadedFile.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteDownloadedFilesRepositoryBackend.getDownloadedFileByPath: error", oEx);
		}

		return null;
	}

	@Override
	public List<DownloadedFile> getDownloadedFileListByName(String sFileName) {
		try {
			return findAllWhere(m_sThisCollection, "fileName", sFileName, DownloadedFile.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteDownloadedFilesRepositoryBackend.getDownloadedFileListByName: error", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public List<DownloadedFile> getDownloadedFileListByPath(String sFilePath) {
		try {
			String sSql = "SELECT data FROM " + m_sThisCollection
					+ " WHERE json_extract(data, '$.filePath') LIKE ?";
			return queryList(sSql, Arrays.asList("%" + sFilePath + "%"), DownloadedFile.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteDownloadedFilesRepositoryBackend.getDownloadedFileListByPath: error", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public List<DownloadedFile> search(Date oDateFrom, Date oDateTo, String sFreeText, String sCategory) {
		try {
			StringBuilder oSb = new StringBuilder("SELECT data FROM ").append(m_sThisCollection);
			List<Object> aoParams = new ArrayList<>();
			boolean bFirst = true;

			if (oDateFrom != null && oDateTo != null) {
				DateFormat oFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
				oSb.append(bFirst ? " WHERE " : " AND ");
				oSb.append("json_extract(data, '$.refDate') >= ? AND json_extract(data, '$.refDate') <= ?");
				aoParams.add(oFmt.format(oDateFrom));
				aoParams.add(oFmt.format(oDateTo));
				bFirst = false;
			}

			if (!Utils.isNullOrEmpty(sFreeText)) {
				oSb.append(bFirst ? " WHERE " : " AND ");
				oSb.append("LOWER(json_extract(data, '$.fileName')) LIKE LOWER(?)");
				aoParams.add("%" + sFreeText + "%");
				bFirst = false;
			}

			if (!Utils.isNullOrEmpty(sCategory)) {
				oSb.append(bFirst ? " WHERE " : " AND ");
				oSb.append("json_extract(data, '$.category') = ?");
				aoParams.add(sCategory);
			}

			return queryList(oSb.toString(), aoParams, DownloadedFile.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteDownloadedFilesRepositoryBackend.search: error", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public int deleteByFilePath(String sFilePath) {
		if (Utils.isNullOrEmpty(sFilePath)) {
			return 0;
		}

		try {
			return deleteWhere(m_sThisCollection, "filePath", sFilePath);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteDownloadedFilesRepositoryBackend.deleteByFilePath: error", oEx);
		}

		return 0;
	}

	@Override
	public List<DownloadedFile> getList() {
		try {
			return findAll(m_sThisCollection, DownloadedFile.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteDownloadedFilesRepositoryBackend.getList: error", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public List<DownloadedFile> getByWorkspace(String sWorkspaceId) {
		if (Utils.isNullOrEmpty(sWorkspaceId)) {
			return new ArrayList<>();
		}

		try {
			String sSql = "SELECT data FROM " + m_sThisCollection
					+ " WHERE json_extract(data, '$.filePath') LIKE ?"
					+ " OR json_extract(data, '$.filePath') LIKE ?";
			return queryList(sSql, Arrays.asList(
					"%" + sWorkspaceId + "/%",
					"%" + sWorkspaceId + "\\%"), DownloadedFile.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteDownloadedFilesRepositoryBackend.getByWorkspace: error", oEx);
		}

		return new ArrayList<>();
	}
}

