package wasdi.operations;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.IngestFileParameter;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Copy to SFTP Operation.
 * 
 * Uses an IngestFileParameter.
 * 
 * This operation makes a copy of a file in a workspace to the user sftp folder on the local node.
 * The user must have created and sftp local account.
 * Destination folder can have a relative path and, also, can support a "special" syntax: user;path
 * to copy the file to another users' sftp account folder.
 * 
 * The operation checks that params are ok and file exists, and makes a copy
 * 
 * 
 * @author p.campanella
 *
 */
public class Copytosftp extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {

        WasdiLog.infoLog("Copytosftp.executeOperation");
        
		if (oParam == null) {
			WasdiLog.errorLog("Parameter is null");
			return false;
		}
		
		if (oProcessWorkspace == null) {
			WasdiLog.errorLog("Process Workspace is null");
			return false;
		}

		
        try {
        	// Convert the parameter
        	IngestFileParameter oParameter = (IngestFileParameter) oParam;
        	
        	// Create the object of the file to move
            File oFileToMovePath = new File(oParameter.getFilePath());
            
            // It must exists and be readable
            if (!oFileToMovePath.canRead()) {
                String sMsg = "Copytosftp.executeOperation: ERROR: unable to access file to Move " + oFileToMovePath.getAbsolutePath();
                WasdiLog.errorLog(sMsg);
                
                return false;
            }
        	
            // get file size and send it to the client
            long lFileSizeByte = oFileToMovePath.length();
            setFileSizeToProcess(lFileSizeByte, oProcessWorkspace);

            updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 5);
            
            // Destination Base Path
            String sDestinationPath = WasdiConfig.Current.paths.sftpRootPath;
            if (!sDestinationPath.endsWith("/")) sDestinationPath += "/";

            // Is there a relative path? Note: relative Path can be a simple path or user;path 
            String sRelativePath = oParameter.getRelativePath();

            if (Utils.isNullOrEmpty(sRelativePath)) {
                // No, just go in the user default folder
                sDestinationPath += oParameter.getUserId();
                sDestinationPath += "/uploads/";
            } else {

                // Yes: this can have also a different user path
                String sUserPath = oParameter.getUserId();
                String sRelativePart = sRelativePath;

                // Do we have the user or not?
                String[] asSplitted = sRelativePath.split(";");
                if (asSplitted != null) {
                    if (asSplitted.length > 1) {
                        sUserPath = asSplitted[0];
                        sRelativePart = asSplitted[1];
                    }
                }

                // Add the user
                sDestinationPath += sUserPath;
                
                // Add the path
                if (!sRelativePart.startsWith("/")) sDestinationPath += "/";
                sDestinationPath += sRelativePart;
                if (!sDestinationPath.endsWith("/")) sDestinationPath += "/";

            }
            
            // Create the destination dir
            File oDstDir = new File(sDestinationPath);

            if (!oDstDir.exists()) {
                oDstDir.mkdirs();
            }

            if (!oDstDir.isDirectory() || !oDstDir.canWrite()) {
                WasdiLog.errorLog("Copytosftp.executeOperation: ERROR: unable to access destination directory " + oDstDir.getAbsolutePath());
                return false;
            }

            updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 50);

            // copy file to ftp directory
            if (!oFileToMovePath.getParent().equals(oDstDir.getAbsolutePath())) {
                WasdiLog.debugLog("Copytosftp.executeOperation: File in another folder make a copy");
                FileUtils.copyFileToDirectory(oFileToMovePath, oDstDir);
            } else {
                WasdiLog.debugLog("Copytosftp.executeOperation: File already in place");
            }

            updateProcessStatus(oProcessWorkspace, ProcessStatus.DONE, 100);

            return true;

        } catch (Throwable oEx) {
        	
            WasdiLog.errorLog("Copytosftp.executeOperation: ERROR: Throwable occurrend moving the file to sftp server");
            String sError = ExceptionUtils.getMessage(oEx);
            WasdiLog.errorLog("Copytosftp.executeOperation: " + sError);

            oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
            m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.COPYTOSFTP.name(), oParam.getWorkspace(), sError, oParam.getExchange());
        } 
        
		return false;
	}

}
