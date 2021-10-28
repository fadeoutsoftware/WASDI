package wasdi.operations;

import java.io.File;

import org.apache.commons.io.FileUtils;

import wasdi.ConfigReader;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.IngestFileParameter;
import wasdi.shared.utils.Utils;

public class Copytosftp extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {

        m_oLocalLogger.debug("Copytosftp.executeOperation");
        
		if (oParam == null) {
			m_oLocalLogger.error("Parameter is null");
			return false;
		}
		
		if (oProcessWorkspace == null) {
			m_oLocalLogger.error("Process Workspace is null");
			return false;
		}

        
		IngestFileParameter oParameter = (IngestFileParameter) oParam;
		
        File oFileToMovePath = new File(oParameter.getFilePath());

        if (!oFileToMovePath.canRead()) {
            String sMsg = "Copytosftp.executeOperation: ERROR: unable to access file to Move " + oFileToMovePath.getAbsolutePath();
            m_oLocalLogger.error(sMsg);
            return false;
        }

        try {
            // get file size
            long lFileSizeByte = oFileToMovePath.length();

            // set file size
            setFileSizeToProcess(lFileSizeByte, oProcessWorkspace);

            updateProcessStatus(m_oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 5);

            String sDestinationPath = ConfigReader.getPropValue("SFTP_ROOT_PATH", "/data/sftpuser");
            if (!sDestinationPath.endsWith("/")) sDestinationPath += "/";

            // Is there a relative path?
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


            File oDstDir = new File(sDestinationPath);

            if (!oDstDir.exists()) {
                oDstDir.mkdirs();
            }

            if (!oDstDir.isDirectory() || !oDstDir.canWrite()) {
                m_oLocalLogger.error("Copytosftp.executeOperation: ERROR: unable to access destination directory " + oDstDir.getAbsolutePath());
                return false;
            }

            updateProcessStatus(m_oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 50);

            // copy file to workspace directory
            if (!oFileToMovePath.getParent().equals(oDstDir.getAbsolutePath())) {
                m_oLocalLogger.debug("Copytosftp.executeOperation: File in another folder make a copy");
                FileUtils.copyFileToDirectory(oFileToMovePath, oDstDir);
            } else {
                m_oLocalLogger.debug("Copytosftp.executeOperation: File already in place");
            }

            updateProcessStatus(m_oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);

            return true;

        } catch (Exception e) {
            String sMsg = "Copytosftp.executeOperation: ERROR: Exception in copy file to sftp";
            System.out.println(sMsg);
            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(e);
            m_oLocalLogger.error(sMsg);
            m_oLocalLogger.error(sError);
            e.printStackTrace();

            if (oProcessWorkspace != null) oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
            if (m_oSendToRabbit != null)
                m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.INGEST.name(), oParameter.getWorkspace(), sError, oParameter.getExchange());

        } catch (Throwable e) {
            String sMsg = "Copytosftp.executeOperation: ERROR: Throwable occurrend during file ingestion";
            System.out.println(sMsg);
            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(e);
            m_oLocalLogger.error(sMsg);
            m_oLocalLogger.error(sError);
            e.printStackTrace();

            if (oProcessWorkspace != null) oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
            if (m_oSendToRabbit != null)
                m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.INGEST.name(), oParam.getWorkspace(), sError, oParam.getExchange());
        } 
        
		return false;
	}

}
