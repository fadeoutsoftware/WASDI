package wasdi.shared.data;

import java.util.Date;
import java.util.List;

import wasdi.shared.business.DownloadedFile;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.IDownloadedFilesRepositoryBackend;

/**
 * Downloaded File Repo
 * Created by p.campanella on 11/11/2016.
 */
public class DownloadedFilesRepository {

    private final IDownloadedFilesRepositoryBackend m_oBackend;
	
	public DownloadedFilesRepository() {
        m_oBackend = createBackend();
	}

    private IDownloadedFilesRepositoryBackend createBackend() {
        // For now keep Mongo backend only. Next step will select by config.
        return DataRepositoryFactoryProvider.getFactory().createDownloadedFilesRepository();
    }

	/**
	 * Insert new Downloaded File
	 * @param oFile
	 * @return
	 */
    public boolean insertDownloadedFile(DownloadedFile oFile) {
        return m_oBackend.insertDownloadedFile(oFile);
    }

    /**
     * Update Entity
     * @param oFile
     * @return
     */
    public boolean updateDownloadedFile(DownloadedFile oFile) {
        return m_oBackend.updateDownloadedFile(oFile);
    }
    
    public boolean updateDownloadedFile(DownloadedFile oFile, String sOldPath) {
        return m_oBackend.updateDownloadedFile(oFile, sOldPath);
    }

    /**
     * Get Downloaded file by the fulle Path
     * @param sFileName Full Path of the file
     * @return
     */
    public DownloadedFile getDownloadedFile(String sFileName) {
        return m_oBackend.getDownloadedFile(sFileName);
    }


    /**
     * Get Downloaded file by name
     * @param sFileName
     * @return
     */

    public DownloadedFile getDownloadedFileByPath(String sFileFullPath) {
        return m_oBackend.getDownloadedFileByPath(sFileFullPath);
    }          

    /**
     * Get the list of Downloaded files that have the same file name (can be in different paths)
     * @param sFileName FileName to search
     * @return List of found entries
     */
    public List<DownloadedFile> getDownloadedFileListByName(String sFileName) {
        return m_oBackend.getDownloadedFileListByName(sFileName);
    }
    
    /**
     * Get a list of Downloaded files with specified path    
     * @param sFilePath File path to search
     * @return
     */
    public List<DownloadedFile> getDownloadedFileListByPath(String sFilePath) {
        return m_oBackend.getDownloadedFileListByPath(sFilePath);

    }

    /**
     * Search files
     * @param oDateFrom start date
     * @param oDateTo end date
     * @param sFreeText free text to search in name
     * @param sCategory category
     * @return list of entries found
     */
    public List<DownloadedFile> search(Date oDateFrom, Date oDateTo, String sFreeText, String sCategory) {
        return m_oBackend.search(oDateFrom, oDateTo, sFreeText, sCategory);
    }
    
    /**
     * Delete by file path
     * @param sFilePath
     * @return Number of deleted elements
     */
    public int deleteByFilePath(String sFilePath) {
        return m_oBackend.deleteByFilePath(sFilePath);
    }
    
    /**
     * Get full list of Donwloaded files
     * @return All the collection
     */
    public List<DownloadedFile> getList() {
        return m_oBackend.getList();
    }
    
    
    /**
     * Search files
     * @param oDateFrom start date
     * @param oDateTo end date
     * @param sFreeText free text to search in name
     * @param sCategory category
     * @return list of entries found
     */
    public List<DownloadedFile> getByWorkspace(String sWorkspaceId) {
        return m_oBackend.getByWorkspace(sWorkspaceId);
    }    
    
}

