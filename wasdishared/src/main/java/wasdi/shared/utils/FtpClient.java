/*
 * Created by Cristiano Nattero - Fadeout Software
 * on 2018.11.13 
 *
 * 
 * class implements FTP
 * 
 * 
 * */


//MAYBE add support for more secure file transfer protocols
// https://en.wikipedia.org/wiki/File_Transfer_Protocol
//FTPS is supported by apache commons
package wasdi.shared.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import wasdi.shared.utils.log.WasdiLog;

public class FtpClient {

    private final String m_sServer;
    private final int m_iPort;
    private final String m_sUser;
    private final String m_sPassword;
    private FTPClient m_oFtp;

    public FtpClient(String sServer, int iPort, String sUser, String sPassword) {
    	WasdiLog.debugLog("FtpClient contructor");
        this.m_sServer = sServer;
        this.m_iPort = iPort;
        this.m_sUser = sUser;
        this.m_sPassword = sPassword;
    }

    public Boolean open() throws IOException {
    	WasdiLog.debugLog("FtpClient.open");
        m_oFtp = new FTPClient();

        m_oFtp.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));

        m_oFtp.connect(m_sServer, m_iPort);
        int ireply = m_oFtp.getReplyCode();
        if (!FTPReply.isPositiveCompletion(ireply)) {
            m_oFtp.disconnect();
            throw new IOException("Exception in connecting to FTP Server");
        }

        return m_oFtp.login(m_sUser, m_sPassword);
    }

    public void close() throws IOException {
    	WasdiLog.debugLog("FtpClient.close");
        m_oFtp.disconnect();
    }
    
    //XXX return directory tree with permissions
    
    
    public Collection<String> listDirs() throws IOException {
    	WasdiLog.debugLog("FtpClient.listDirs()");
		return listDirs(pwd());
	}
    
    public Collection<String> listDirs(String sPath) throws IOException {
    	WasdiLog.debugLog("FtpClient.listDirs( " + sPath + " )");
    	FTPFile[] aoFiles = m_oFtp.listDirectories(sPath);

        return Arrays.stream(aoFiles)
                .map(FTPFile::getName)
                .collect(Collectors.toList());
	}

	public Collection<String> listFiles() throws IOException {
		WasdiLog.debugLog("FtpClient.listFiles()");
		return listFiles(pwd());
	}

    public Collection<String> listFiles(String sPath) throws IOException {
    	WasdiLog.debugLog("FtpClient.listFiles( " + sPath + " )");
        FTPFile[] aoFiles = m_oFtp.listFiles(sPath);

        return Arrays.stream(aoFiles)
                .map(FTPFile::getName)
                .collect(Collectors.toList());
    }
    
    public Boolean putFileOnServer(File oFile) throws IOException {
    	WasdiLog.debugLog("FtpClient.putFileOnServer( File )");
		return putFileToPath(oFile, pwd() );
	}
    
    public Boolean putFileToPath(File oFile, String sRemoteRelativePath) throws IOException {
    	WasdiLog.debugLog("FtpClient.putFileToPath( File, " + sRemoteRelativePath + " )");
    	if(null == oFile || null == sRemoteRelativePath) {
    		throw new IllegalArgumentException();
    	}
    	String sPathName = new String(FilenameUtils.normalize(sRemoteRelativePath, true));
    	/*
		if(!sPathName.endsWith("/")) {
			sPathName+="/";
		}
    	//strip leading "/"s
    	while(sPathName.startsWith("/")) {
    		sPathName = sPathName.substring(1);
    	}*/

    	Boolean bRes = Boolean.valueOf(true);
    	String wd = new String(pwd());
    	if(!wd.equals(sPathName)) {
    		bRes = cd(sPathName);
    		wd = new String(pwd());
    	}
        if(bRes) {
        	sPathName = oFile.getName();
        	//m_oFtp.storeFile
        	bRes = m_oFtp.storeFile(sPathName, new FileInputStream(oFile) );
        }
        return bRes;
    }

    public void downloadFile(String sSource, String sDestination) throws IOException {
    	WasdiLog.debugLog("FtpClient.downloadFile( " + sSource + ",  " + sDestination + " )");
        FileOutputStream oOut = new FileOutputStream(sDestination);
        m_oFtp.retrieveFile(sSource, oOut);
    }

	public String pwd() throws IOException {
		WasdiLog.debugLog("FtpClient.pwd()");
		String wd = null;
		wd = new String(m_oFtp.printWorkingDirectory());
		return wd;
	}

	public Boolean fileIsNowOnServer(String sPath, String sFilename) throws IOException {
		WasdiLog.debugLog("FtpClient.FileIsNowOnServer( " + sPath + ", " + sFilename + " )");
		if(null == sPath || null == sFilename) {
			throw new IllegalArgumentException();
		}
		String sDir = FilenameUtils.normalize(sPath, true); 
		Collection<String> aoFiles = listFiles(sDir);
		for (String sFile : aoFiles) {
			if(sFile.equals(sFilename)) {
				return true;
			}
		}
		return false;
	}

	public Boolean cd(String sPath) throws IOException {
		WasdiLog.debugLog("FtpClient.cd( " + sPath + " )");
		if(null == sPath) {
			throw new IllegalArgumentException();
		}
		String sDir = new String(FilenameUtils.normalize(sPath, true));
		while(sDir.startsWith("/")) {
			sDir = sDir.substring(1);
		}
		Boolean bRes = m_oFtp.changeWorkingDirectory(sDir); 
		return bRes;
		
	}
}
