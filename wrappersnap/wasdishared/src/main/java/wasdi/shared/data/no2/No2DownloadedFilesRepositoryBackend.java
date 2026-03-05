package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.FluentFilter.where;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.DocumentCursor;
import org.dizitart.no2.collection.NitriteCollection;

import wasdi.shared.business.DownloadedFile;
import wasdi.shared.data.interfaces.IDownloadedFilesRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * NO2 backend implementation for downloaded files repository.
 */
public class No2DownloadedFilesRepositoryBackend extends No2Repository implements IDownloadedFilesRepositoryBackend {

	private static final String s_sCollectionName = "downloadedfiles";

	@Override
	public boolean insertDownloadedFile(DownloadedFile oFile) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oFile == null) {
				return false;
			}
			oCollection.insert(toDocument(oFile));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2DownloadedFilesRepositoryBackend.insertDownloadedFile: error", oEx);
		}
		return false;
	}

	@Override
	public boolean updateDownloadedFile(DownloadedFile oFile) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oFile == null) {
				return false;
			}
			oCollection.update(where("filePath").eq(oFile.getFilePath()), toDocument(oFile));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2DownloadedFilesRepositoryBackend.updateDownloadedFile: error", oEx);
		}
		return false;
	}

	@Override
	public boolean updateDownloadedFile(DownloadedFile oFile, String sOldPath) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oFile == null) {
				return false;
			}
			oCollection.update(where("filePath").eq(sOldPath), toDocument(oFile));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2DownloadedFilesRepositoryBackend.updateDownloadedFile(oldPath): error", oEx);
		}
		return false;
	}

	@Override
	public DownloadedFile getDownloadedFile(String sFileName) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}
			for (Document oDocument : oCollection.find(where("fileName").eq(sFileName))) {
				return fromDocument(oDocument, DownloadedFile.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2DownloadedFilesRepositoryBackend.getDownloadedFile: error", oEx);
		}
		return null;
	}

	@Override
	public DownloadedFile getDownloadedFileByPath(String sFileFullPath) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}
			for (Document oDocument : oCollection.find(where("filePath").eq(sFileFullPath))) {
				return fromDocument(oDocument, DownloadedFile.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2DownloadedFilesRepositoryBackend.getDownloadedFileByPath: error", oEx);
		}
		return null;
	}

	@Override
	public List<DownloadedFile> getDownloadedFileListByName(String sFileName) {
		return filterBy(d -> d != null && equalsSafe(d.getFileName(), sFileName));
	}

	@Override
	public List<DownloadedFile> getDownloadedFileListByPath(String sFilePath) {
		return filterBy(d -> d != null && containsSafe(d.getFilePath(), sFilePath));
	}

	@Override
	public List<DownloadedFile> search(Date oDateFrom, Date oDateTo, String sFreeText, String sCategory) {
		DateFormat oDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
		String sDateFrom = oDateFrom != null ? oDateFormat.format(oDateFrom) : null;
		String sDateTo = oDateTo != null ? oDateFormat.format(oDateTo) : null;

		return filterBy(d -> {
			if (d == null) {
				return false;
			}
			if (!Utils.isNullOrEmpty(sDateFrom) && !Utils.isNullOrEmpty(sDateTo)) {
				String sRefDate = d.getRefDate();
				if (Utils.isNullOrEmpty(sRefDate) || sRefDate.compareTo(sDateFrom) < 0 || sRefDate.compareTo(sDateTo) > 0) {
					return false;
				}
			}
			if (!Utils.isNullOrEmpty(sFreeText) && !containsSafeIgnoreCase(d.getFileName(), sFreeText)) {
				return false;
			}
			if (!Utils.isNullOrEmpty(sCategory) && !equalsSafe(d.getCategory(), sCategory)) {
				return false;
			}
			return true;
		});
	}

	@Override
	public int deleteByFilePath(String sFilePath) {
		if (Utils.isNullOrEmpty(sFilePath)) {
			return 0;
		}

		int iCount = getDownloadedFileListByPath(sFilePath).size();
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection != null) {
				oCollection.remove(where("filePath").eq(sFilePath));
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2DownloadedFilesRepositoryBackend.deleteByFilePath: error", oEx);
		}
		return iCount;
	}

	@Override
	public List<DownloadedFile> getList() {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			DocumentCursor oCursor = oCollection != null ? oCollection.find() : null;
			return toList(oCursor, DownloadedFile.class);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2DownloadedFilesRepositoryBackend.getList: error", oEx);
		}
		return new ArrayList<>();
	}

	@Override
	public List<DownloadedFile> getByWorkspace(String sWorkspaceId) {
		if (Utils.isNullOrEmpty(sWorkspaceId)) {
			return new ArrayList<>();
		}

		String sBackslash = sWorkspaceId + "\\\\";
		String sSlash = sWorkspaceId + "/";
		return filterBy(d -> d != null && (containsSafe(d.getFilePath(), sBackslash) || containsSafe(d.getFilePath(), sSlash)));
	}

	private interface DownloadedFileFilter {
		boolean test(DownloadedFile oFile);
	}

	private List<DownloadedFile> filterBy(DownloadedFileFilter oFilter) {
		List<DownloadedFile> aoResults = new ArrayList<>();
		for (DownloadedFile oFile : getList()) {
			if (oFilter.test(oFile)) {
				aoResults.add(oFile);
			}
		}
		return aoResults;
	}

	private boolean equalsSafe(String sA, String sB) {
		return sA == null ? sB == null : sA.equals(sB);
	}

	private boolean containsSafe(String sValue, String sLookup) {
		if (sValue == null || sLookup == null) {
			return false;
		}
		return sValue.contains(sLookup);
	}

	private boolean containsSafeIgnoreCase(String sValue, String sLookup) {
		if (sValue == null || sLookup == null) {
			return false;
		}
		return sValue.toLowerCase().contains(sLookup.toLowerCase());
	}
}
