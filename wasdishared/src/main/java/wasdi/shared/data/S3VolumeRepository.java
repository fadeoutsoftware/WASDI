package wasdi.shared.data;

import java.util.List;

import wasdi.shared.business.S3Volume;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.IS3VolumeRepositoryBackend;

public class S3VolumeRepository {

    private final IS3VolumeRepositoryBackend m_oBackend;

	public S3VolumeRepository() {
        m_oBackend = createBackend();
    }

    private IS3VolumeRepositoryBackend createBackend() {
        // For now keep Mongo backend only. Next step will select by config.
        return DataRepositoryFactoryProvider.getFactory().createS3VolumeRepository();
	}
	
	public boolean insert(S3Volume oJob) {
        return m_oBackend.insert(oJob);
	}
	
    public int delete(String sVolumeId) {
        return m_oBackend.delete(sVolumeId);
    }
    
    public List<S3Volume> getVolumesByUser(String sUserId) {
        return m_oBackend.getVolumesByUser(sUserId);
    }
    
    /**
     * Get a volume by Id
     * @param sVolumeId id of the volume
     * @return Entity
     */
    public S3Volume getVolume(String sVolumeId) {
        return m_oBackend.getVolume(sVolumeId);
    }    
    
    
    /**
     * Get a volume by Folder Name
     * @param sVolumeId id of the volume
     * @return Entity
     */
    public S3Volume getVolumeByFolderName(String sMountingFolderName) {
        return m_oBackend.getVolumeByFolderName(sMountingFolderName);
    }        

    public List<S3Volume> getVolumes() {
        return m_oBackend.getVolumes();
    }
    
    /**
     * Delete all the volumes of a user
     * @param sUserId
     * @return
     */
    public int deleteByUserId(String sUserId) {
        return m_oBackend.deleteByUserId(sUserId);
    }    
}

