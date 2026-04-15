package wasdi.operations;

import java.io.File;
import java.io.IOException;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.FtpUploadParameters;
import wasdi.shared.payloads.FTPUploadPayload;
import wasdi.shared.utils.FtpClient;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class Ftpupload extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {
		
		WasdiLog.infoLog("Ftpupload.executeOperation");
		
		if (oParam == null) {
			WasdiLog.errorLog("Parameter is null");
			return false;
		}
		
		if (oProcessWorkspace == null) {
			WasdiLog.errorLog("Process Workspace is null");
			return false;
		}
		
        try {
        	
        	FtpUploadParameters oParameter = (FtpUploadParameters) oParam;
        	
            try {
                updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 0);

                //check server parameters are OK before trying connection
                if (!Utils.isServerNamePlausible(oParameter.getFtpServer())) {

                    m_oProcessWorkspaceLogger.log("FTP server name not plausible " + oParameter.getFtpServer());

                    throw new Exception("FTP server name \"" + oParameter.getFtpServer() + "\" not plausible");
                }
                if (!Utils.isPortNumberPlausible(oParameter.getPort())) {
                    m_oProcessWorkspaceLogger.log("FTP server port not plausible " + oParameter.getPort().toString());
                    throw new Exception("FTP server port \"" + oParameter.getPort() + "\" not plausible");
                }
                
                updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 2);

                m_oProcessWorkspaceLogger.log("Moving " + oParameter.getLocalFileName() + " to " + oParameter.getFtpServer() + ":" + oParameter.getPort().toString());

                String sFullLocalPath = PathsConfig.getWorkspacePath(oParam) + oParameter.getLocalFileName();

                updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 3);
                File oFile = new File(sFullLocalPath);
                if (!oFile.exists()) {
                    throw new IOException("local file " + oFile.getName() + "does not exist ");
                }

                if (!oParameter.getRemotePath().endsWith("/") && !oParameter.getRemotePath().endsWith("\\")) {
                    oParameter.setRemotePath(oParameter.getRemotePath() + "/");
                }

                m_oProcessWorkspaceLogger.log("Remote path " + oParameter.getRemotePath());

                if (oParameter.getSftp()) {

                    m_oProcessWorkspaceLogger.log("SFTP protocol");

                    WasdiLog.debugLog("Ftpupload.executeOperation: SFTP");
                    try (SSHClient oClient = new SSHClient()) {
                        oClient.addHostKeyVerifier(new PromiscuousVerifier());
                        WasdiLog.debugLog("Ftpupload.executeOperation: SFTP: connecting to " + oParameter.getFtpServer());
                        oClient.connect(oParameter.getFtpServer());
                        WasdiLog.debugLog("Ftpupload.executeOperation: SFTP: authenticating as " + oParameter.getUsername());
                        oClient.authPassword(oParameter.getUsername(), oParameter.getPassword());

                        try (SFTPClient sftpClient = oClient.newSFTPClient()) {
                            updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 4);
                            WasdiLog.debugLog("Ftpupload.executeOperation: SFTP: transferring file");
                            m_oProcessWorkspaceLogger.log("Start transfer");
                            sftpClient.put(sFullLocalPath, oParameter.getRemotePath() + oParameter.getLocalFileName());
                            //todo check that the file is there
                            updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 95);
                            WasdiLog.debugLog("Ftpupload.executeOperation: SFTP: closing SFTP client");
                            sftpClient.close();
                            oClient.disconnect();
                            oClient.close();

                            m_oProcessWorkspaceLogger.log("Transfer done");
                        }
                    }

                } else {
                    m_oProcessWorkspaceLogger.log("FTP Protocol");
                    WasdiLog.debugLog("Ftpupload.executeOperation: FTP");
                    FtpClient oFtpClient = new FtpClient(oParameter.getFtpServer(), oParameter.getPort(), oParameter.getUsername(), oParameter.getPassword());

                    WasdiLog.debugLog("Ftpupload.executeOperation: FTP: opening connection");

                    if (!oFtpClient.open()) {
                        throw new IOException("could not connect to FTP");
                    }
                    updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 4);

                    WasdiLog.debugLog("Ftpupload.executeOperation: FTP: transferring file");
                    m_oProcessWorkspaceLogger.log("Start transfer");

                    // XXX see how to modify FTP client to update status
                    Boolean bPut = oFtpClient.putFileToPath(oFile, oParameter.getRemotePath());
                    if (!bPut) {
                        throw new IOException("put failed");
                    }
                    updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 95);
                    // String sRemotePath = oFtpTransferParameters.getM_sRemotePath();
                    String sRemotePath = ".";
                    WasdiLog.debugLog("Ftpupload.executeOperation: FTP: checking the file is on server");
                    Boolean bCheck = oFtpClient.fileIsNowOnServer(sRemotePath, oFile.getName());

                    if (!bCheck) {
                        m_oProcessWorkspaceLogger.log("Error checking if the file is on the server");
                        throw new IOException("could not find file on server");
                    }
                    WasdiLog.debugLog("Ftpupload.executeOperation: FTP: closing client");
                    oFtpClient.close();

                    m_oProcessWorkspaceLogger.log("Transfer done");
                }

                FTPUploadPayload oPayload = new FTPUploadPayload();
                oPayload.setFile(oParameter.getLocalFileName());
                oPayload.setRemotePath(oParameter.getRemotePath());
                oPayload.setServer(oParameter.getFtpServer());
                oPayload.setPort(oParameter.getPort());
                setPayload(oProcessWorkspace, oPayload);
                
                updateProcessStatus(oProcessWorkspace, ProcessStatus.DONE, 100);
                WasdiLog.infoLog("Ftpupload.executeOperation: completed successfully");
                
                return true;
                
            } catch (Throwable oEx) {
                WasdiLog.errorLog("Ftpupload.executeOperation: could not complete due to: " + oEx);
                oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
            } 
        } catch (Throwable oEx) {
            WasdiLog.errorLog("Ftpupload.executeOperation: " + oEx.toString());
        }		
		return false;
	}

}
