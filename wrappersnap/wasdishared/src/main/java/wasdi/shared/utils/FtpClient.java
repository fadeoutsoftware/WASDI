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

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

public class FtpClient {

    private final String m_sServer;
    private final int m_iPort;
    private final String m_sUser;
    private final String m_sPassword;
    private FTPClient m_oFtp;

    public FtpClient(String sServer, int iPort, String sUser, String sPassword) {
        this.m_sServer = sServer;
        this.m_iPort = iPort;
        this.m_sUser = sUser;
        this.m_sPassword = sPassword;
    }

    public void open() throws IOException {
        m_oFtp = new FTPClient();

        m_oFtp.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));

        m_oFtp.connect(m_sServer, m_iPort);
        int ireply = m_oFtp.getReplyCode();
        if (!FTPReply.isPositiveCompletion(ireply)) {
            m_oFtp.disconnect();
            throw new IOException("Exception in connecting to FTP Server");
        }

        m_oFtp.login(m_sUser, m_sPassword);
    }

    public void close() throws IOException {
        m_oFtp.disconnect();
    }

    public Collection<String> listFiles(String sPath) throws IOException {
        FTPFile[] aoFiles = m_oFtp.listFiles(sPath);

        return Arrays.stream(aoFiles)
                .map(FTPFile::getName)
                .collect(Collectors.toList());
    }

    public void putFileToPath(File oFile, String sPath) throws IOException {
        m_oFtp.storeFile(sPath, new FileInputStream(oFile));
    }

    public void downloadFile(String sSource, String sDestination) throws IOException {
        FileOutputStream oOut = new FileOutputStream(sDestination);
        m_oFtp.retrieveFile(sSource, oOut);
    }

	public String pwd() throws IOException {
		return m_oFtp.printWorkingDirectory();

	}

	public Boolean FileIsNowOnServer(String sPath, String sFilename) throws IOException {
		Collection<String> aoFiles = listFiles(sPath);
		for (String sFile : aoFiles) {
			if(sFile.equals(sFilename)) {
				return true;
			}
		}
		
		return false;
	}
}
