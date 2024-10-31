package wasdi.operations;

import wasdi.asynch.SaveMetadataThread;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.ReadMetadataParameter;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class Readmetadata extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {
		
		WasdiLog.infoLog("Readmetadata.executeOperation");
		
		if (oParam == null) {
			WasdiLog.errorLog("Parameter is null");
			return false;
		}
		
		if (oProcessWorkspace == null) {
			WasdiLog.errorLog("Process Workspace is null");
			return false;
		}		

        try {
        	ReadMetadataParameter oReadMetadataParameter = (ReadMetadataParameter) oParam;

            String sProductName = oReadMetadataParameter.getProductName();

            if (sProductName == null) {
                WasdiLog.errorLog("Readmetadata.executeOperation: Product Path is null");
                return false;
            }

            DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();

            String sProductPath = PathsConfig.getWorkspacePath(oReadMetadataParameter) + sProductName;

            DownloadedFile oDownloadedFile = oDownloadedFilesRepository.getDownloadedFileByPath(sProductPath);
            
            if (oDownloadedFile == null) {
                WasdiLog.errorLog("Readmetadata.executeOperation: Downloaded file not found for path " + sProductPath);
                return false;
            }

            if (oDownloadedFile.getProductViewModel() == null) {
                WasdiLog.errorLog("Readmetadata.executeOperation: Product View Model is null");
                return false;

            }

            String sInfo = "Read Metadata Operation<br>Retriving File Metadata<br>Try again later";
            m_oSendToRabbit.SendRabbitMessage(true, LauncherOperations.INFO.name(), oProcessWorkspace.getWorkspaceId(), sInfo, oProcessWorkspace.getWorkspaceId());

            if (Utils.isNullOrEmpty(oDownloadedFile.getProductViewModel().getMetadataFileReference())) {
                if (oDownloadedFile.getProductViewModel().getMetadataFileCreated() == false) {

                    WasdiLog.infoLog("Readmetadata.executeOperation: Metadata File still not created. Generate it");

                    oDownloadedFile.getProductViewModel().setMetadataFileCreated(true);
                    oDownloadedFile.getProductViewModel().setMetadataFileReference(asynchSaveMetadata(sProductPath));

                    WasdiLog.debugLog("Readmetadata.executeOperation: Metadata File Creation Thread started. Saving Metadata in path " + oDownloadedFile.getProductViewModel().getMetadataFileReference());

                    oDownloadedFilesRepository.updateDownloadedFile(oDownloadedFile);
                } else {
                    WasdiLog.debugLog("Readmetadata.executeOperation: attemp to create metadata file has already been done");
                }
            } else {
                WasdiLog.debugLog("Readmetadata.executeOperation: metadata file reference already present " + oDownloadedFile.getProductViewModel().getMetadataFileReference());
            }

            WasdiLog.debugLog("Readmetadata.executeOperation: done, bye");
            
            return true;
            
        } catch (Exception oEx) {
            WasdiLog.errorLog("Readmetadata.executeOperation Exception " + oEx.toString());
        }
        
		return false;
	}
	
    public String asynchSaveMetadata(String sProductFile) {

        // Write Metadata to file system
        try {

            // Get Metadata Path a Random File Name
            String sMetadataPath = WasdiConfig.Current.paths.metadataPath;
            if (!sMetadataPath.endsWith("/"))
                sMetadataPath += "/";
            String sMetadataFileName = Utils.getRandomName();

            WasdiLog.debugLog("Readmetadata.asynchSaveMetadata: file = " + sMetadataFileName);

            SaveMetadataThread oThread = new SaveMetadataThread(sMetadataPath + sMetadataFileName, sProductFile);
            oThread.start();

            WasdiLog.debugLog("Readmetadata.asynchSaveMetadata: thread started");

            return sMetadataFileName;

        } catch (Exception e) {
            WasdiLog.errorLog("Readmetadata.asynchSaveMetadata: Exception = ", e);
        }

        // There was an error...
        return "";
    }


}
