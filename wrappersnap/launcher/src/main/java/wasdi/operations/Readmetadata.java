package wasdi.operations;

import java.io.IOException;

import wasdi.ConfigReader;
import wasdi.LauncherMain;
import wasdi.asynch.SaveMetadataThread;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.ReadMetadataParameter;
import wasdi.shared.utils.Utils;

public class Readmetadata extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {
		
		m_oLocalLogger.debug("Readmetadata.executeOperation");
		
		if (oParam == null) {
			m_oLocalLogger.error("Parameter is null");
			return false;
		}
		
		if (oProcessWorkspace == null) {
			m_oLocalLogger.error("Process Workspace is null");
			return false;
		}		

        try {
        	ReadMetadataParameter oReadMetadataParameter = (ReadMetadataParameter) oParam;

            String sProductName = oReadMetadataParameter.getProductName();

            if (sProductName == null) {
                m_oLocalLogger.error("Readmetadata.executeOperation: Product Path is null");
                return false;
            }

            DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();

            String sProductPath = LauncherMain.getWorkspacePath(oReadMetadataParameter) + sProductName;

            DownloadedFile oDownloadedFile = oDownloadedFilesRepository.getDownloadedFileByPath(sProductPath);
            
            if (oDownloadedFile == null) {
                m_oLocalLogger.error("Readmetadata.executeOperation: Downloaded file not found for path " + sProductPath);
                return false;
            }

            if (oDownloadedFile.getProductViewModel() == null) {
                m_oLocalLogger.error("Readmetadata.executeOperation: Product View Model is null");
                return false;

            }

            String sInfo = "Read Metadata Operation<br>Retriving File Metadata<br>Try again later";
            m_oSendToRabbit.SendRabbitMessage(true, LauncherOperations.INFO.name(), oProcessWorkspace.getWorkspaceId(), sInfo, oProcessWorkspace.getWorkspaceId());

            if (Utils.isNullOrEmpty(oDownloadedFile.getProductViewModel().getMetadataFileReference())) {
                if (oDownloadedFile.getProductViewModel().getMetadataFileCreated() == false) {

                    m_oLocalLogger.info("Readmetadata.executeOperation: Metadata File still not created. Generate it");

                    oDownloadedFile.getProductViewModel().setMetadataFileCreated(true);
                    oDownloadedFile.getProductViewModel().setMetadataFileReference(asynchSaveMetadata(sProductPath));

                    m_oLocalLogger.info("Readmetadata.executeOperation: Metadata File Creation Thread started. Saving Metadata in path " + oDownloadedFile.getProductViewModel().getMetadataFileReference());

                    oDownloadedFilesRepository.updateDownloadedFile(oDownloadedFile);
                } else {
                    m_oLocalLogger.info("Readmetadata.executeOperation: attemp to create metadata file has already been done");
                }
            } else {
                m_oLocalLogger.info("Readmetadata.executeOperation: metadata file reference already present " + oDownloadedFile.getProductViewModel().getMetadataFileReference());
            }

            m_oLocalLogger.info("Readmetadata.executeOperation: done, bye");
            
            return true;
            
        } catch (Exception oEx) {
            m_oLocalLogger.error("Readmetadata.executeOperation Exception " + oEx.toString());
        }
        
		return false;
	}
	
    public String asynchSaveMetadata(String sProductFile) {

        // Write Metadata to file system
        try {

            // Get Metadata Path a Random File Name
            String sMetadataPath = WasdiConfig.s_oConfig.paths.METADATA_PATH;
            if (!sMetadataPath.endsWith("/"))
                sMetadataPath += "/";
            String sMetadataFileName = Utils.GetRandomName();

            m_oLocalLogger.debug("Readmetadata.asynchSaveMetadata: file = " + sMetadataFileName);

            SaveMetadataThread oThread = new SaveMetadataThread(sMetadataPath + sMetadataFileName, sProductFile);
            oThread.start();

            m_oLocalLogger.debug("Readmetadata.asynchSaveMetadata: thread started");

            return sMetadataFileName;

        } catch (Exception e) {
            m_oLocalLogger.debug("Readmetadata.asynchSaveMetadata: Exception = " + e.toString());
            e.printStackTrace();
        }

        // There was an error...
        return "";
    }


}
