package wasdi.shared.data.interfaces;

import java.util.Date;
import java.util.List;

import wasdi.shared.business.DownloadedFile;

/**
 * Backend contract for downloaded files repository.
 */
public interface IDownloadedFilesRepositoryBackend {

	boolean insertDownloadedFile(DownloadedFile oFile);

	boolean updateDownloadedFile(DownloadedFile oFile);

	boolean updateDownloadedFile(DownloadedFile oFile, String sOldPath);

	DownloadedFile getDownloadedFile(String sFileName);

	DownloadedFile getDownloadedFileByPath(String sFileFullPath);

	List<DownloadedFile> getDownloadedFileListByName(String sFileName);

	List<DownloadedFile> getDownloadedFileListByPath(String sFilePath);

	List<DownloadedFile> search(Date oDateFrom, Date oDateTo, String sFreeText, String sCategory);

	int deleteByFilePath(String sFilePath);

	List<DownloadedFile> getList();

	List<DownloadedFile> getByWorkspace(String sWorkspaceId);
}
