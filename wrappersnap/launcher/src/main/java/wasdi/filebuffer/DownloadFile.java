package wasdi.filebuffer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;

import org.apache.log4j.Logger;

import wasdi.ConfigReader;
import wasdi.LauncherMain;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.utils.Utils;

/**
 * Created by s.adamo on 06/10/2016.
 */
public class DownloadFile {

    private final int BUFFER_SIZE = 4096;
    private final int MAX_NUM_ZEORES_DURING_READ = 20;
	private Logger logger;

    public DownloadFile() {
		logger = LauncherMain.s_oLogger;
	}
    
    public DownloadFile(Logger logger) {
		this.logger = logger;
	}

	public long GetDownloadFileSize(String sFileURL) throws IOException {

        long lLenght = 0L;

        // Domain check
        if (Utils.isNullOrEmpty(sFileURL)) {
            logger.debug("DownloadFile.GetDownloadSize: sFileURL is null");
            return lLenght;
        }

        // TODO: Here we are assuming dhus authentication. But we have to find a general solution
        Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                try{
                    return new PasswordAuthentication(ConfigReader.getPropValue("DHUS_USER"), ConfigReader.getPropValue("DHUS_PASSWORD").toCharArray());
                }
                catch (Exception oEx){
                    logger.error("DownloadFile.GetDownloadSize: exception setting auth " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
                }
                return null;
            }
        });

        logger.debug("DownloadFile.GetDownloadSize: FileUrl = " + sFileURL);

        URL url = new URL(sFileURL);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        int responseCode = httpConn.getResponseCode();

        // always check HTTP response code first
        if (responseCode == HttpURLConnection.HTTP_OK) {

            lLenght = httpConn.getHeaderFieldLong("Content-Length", 0L);

            logger.debug("DownloadFile.GetDownloadSize: File size = " + lLenght);

            return lLenght;

        } else {
            logger.debug("DownloadFile.GetDownloadSize: No file to download. Server replied HTTP code: " + responseCode);
        }
        httpConn.disconnect();

        return lLenght;
    }

    //https://scihub.copernicus.eu/dhus/odata/v1/Products('18f7993d-eae1-4f7f-9d81-d7cf19c18378')/$value
    public String ExecuteDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword, String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace) throws IOException {

        // Domain check
        if (Utils.isNullOrEmpty(sFileURL)) {
            logger.debug("DownloadFile.ExecuteDownloadFile: sFileURL is null");
            return "";
        }
        if (Utils.isNullOrEmpty(sSaveDirOnServer)) {
            logger.debug("DownloadFile.ExecuteDownloadFile: sSaveDirOnServer is null");
            return "";
        }

        String sReturnFilePath = "";

        // TODO: Here we are assuming dhus authentication. But we have to find a general solution
        logger.debug("DownloadFile.ExecuteDownloadFile: sDownloadUser = " + sDownloadUser + " - sDownloadPassword = " + sDownloadPassword);
        
        if (sDownloadUser!=null) {
            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    try{
//                        return new PasswordAuthentication(ConfigReader.getPropValue("DHUS_USER"), ConfigReader.getPropValue("DHUS_PASSWORD").toCharArray());
                    	return new PasswordAuthentication(sDownloadUser, sDownloadPassword.toCharArray());
                    }
                    catch (Exception oEx){
                        logger.error("DownloadFile.ExecuteDownloadFile: exception setting auth " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
                    }
                    return null;
                }
            });        	
        }

        logger.debug("DownloadFile.ExecuteDownloadFile: FileUrl = " + sFileURL);

        URL url = new URL(sFileURL);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        int responseCode = httpConn.getResponseCode();

        // always check HTTP response code first
        if (responseCode == HttpURLConnection.HTTP_OK) {

            logger.debug("DownloadFile.ExecuteDownloadFile: Connected");

            String sFileName = "";
            String sDisposition = httpConn.getHeaderField("Content-Disposition");
            String sContentType = httpConn.getContentType();
            int iContentLength = httpConn.getContentLength();
            
            logger.debug("ExecuteDownloadFile. ContentLenght: " + iContentLength);

            if (sDisposition != null) {
                // extracts file name from header field
                int index = sDisposition.indexOf("filename=");
                if (index > 0) {
                    sFileName = sDisposition.substring(index + 10, sDisposition.length() - 1);
                }
            } else {
                // extracts file name from URL
                sFileName = sFileURL.substring(sFileURL.lastIndexOf("/") + 1,sFileURL.length());
            }

            logger.debug("Content-Type = " + sContentType);
            logger.debug("Content-Disposition = " + sDisposition);
            logger.debug("Content-Length = " + iContentLength);
            logger.debug("fileName = " + sFileName);

            // opens input stream from the HTTP connection
            InputStream oInputStream = httpConn.getInputStream();
            String saveFilePath= sSaveDirOnServer + "/" + sFileName;

            logger.debug("DownloadFile.ExecuteDownloadFile: Create Save File Path = " + saveFilePath);

            File oTargetFile = new File(saveFilePath);
            File oTargetDir = oTargetFile.getParentFile();
            oTargetDir.mkdirs();

            // opens an output stream to save into file
            FileOutputStream oOutputStream = new FileOutputStream(saveFilePath);

            // Cumulative Byte Count
            int iTotalBytes = 0;
            // Byte that represent 10% of the file
            int iTenPercent = iContentLength/10;
            // Percent of the completed download
            int iFilePercent = 0 ;
            
            int iBytesRead = -1;
            byte[] abBuffer = new byte[BUFFER_SIZE];
            int nZeroes = MAX_NUM_ZEORES_DURING_READ;
            while ((iBytesRead = oInputStream.read(abBuffer)) != -1) {
            	
            	if (iBytesRead <= 0) {
            		logger.debug("ExecuteDownloadFile. Read 0 bytes from stream. Counter: " + nZeroes);
            		nZeroes--;
            	} else {
            		nZeroes = MAX_NUM_ZEORES_DURING_READ;
            	}
            	if (nZeroes <=0 ) break;
            	
//            	logger.debug("ExecuteDownloadFile. Read " + iBytesRead +  " bytes from stream");
            	
                oOutputStream.write(abBuffer, 0, iBytesRead);
                
                // Sum bytes
                iTotalBytes += iBytesRead;
                
                // Overcome a 10% limit?
                if(oProcessWorkspace!=null && iContentLength>BUFFER_SIZE && iTotalBytes>=iTenPercent && iFilePercent<=100) {
                	// Increase the file
                	iFilePercent += 10;
                	if (iFilePercent>100) iFilePercent = 100;
                	// Reset the count
                	iTotalBytes = 0;
                	// Update the progress
                	if (nZeroes == MAX_NUM_ZEORES_DURING_READ) UpdateProcessProgress(oProcessWorkspace, iFilePercent);
                }
            }

            oOutputStream.close();
            oInputStream.close();

            sReturnFilePath = saveFilePath;

            logger.debug("File downloaded " + sReturnFilePath);
        } else {
            logger.debug("No file to download. Server replied HTTP code: " + responseCode);
        }
        httpConn.disconnect();

        return  sReturnFilePath;
    }
    
    public void UpdateProcessProgress(ProcessWorkspace oProcessWorkspace, int iProgress) {
        try {    	
	    	ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
	    	
	        oProcessWorkspace.setProgressPerc(iProgress);
	        //update the process
	        if (!oProcessWorkspaceRepository.UpdateProcess(oProcessWorkspace))
	            logger.debug("LauncherMain.DownloadFile: Error during process update with process Perc");
	
	        //send update process message

			if (!LauncherMain.s_oSendToRabbit.SendUpdateProcessMessage(oProcessWorkspace)) {
				logger.debug("LauncherMain.DownloadFile: Error sending rabbitmq message to update process list");
			}
		} catch (Exception oEx) {
			logger.error("LauncherMain.DownloadFile: Exception " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
			oEx.printStackTrace();
		}
    }



    //https://scihub.copernicus.eu/dhus/odata/v1/Products('18f7993d-eae1-4f7f-9d81-d7cf19c18378')/$value
    public String GetFileName(String sFileURL) throws IOException {

        try {
            // Domain check
            if (Utils.isNullOrEmpty(sFileURL)) {
                logger.debug("DownloadFile.GetFileName: sFileURL is null or Empty");
                return "";
            }

            String sReturnFilePath = "";

            // TODO: Here we are assuming dhus authentication. But we have to find a general solution
            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    try{
                        return new PasswordAuthentication(ConfigReader.getPropValue("DHUS_USER"), ConfigReader.getPropValue("DHUS_PASSWORD") .toCharArray());
                    }
                    catch (Exception oEx){
                        logger.error("DownloadFile.GetFileName: exception setting auth " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
                    }
                    return null;
                }
            });

            logger.debug("DownloadFile.GetFileName: FileUrl = " + sFileURL);

            String sConnectionTimeout = ConfigReader.getPropValue("CONNECTION_TIMEOUT");
            String sReadTimeOut = ConfigReader.getPropValue("READ_TIMEOUT");

            int iConnectionTimeOut = 10000;
            int iReadTimeOut = 10000;

            try {
                iConnectionTimeOut = Integer.parseInt(sConnectionTimeout);
            }
            catch (Exception oEx) {
                logger.error(org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
            }
            try {
                iReadTimeOut = Integer.parseInt(sReadTimeOut);
            }
            catch (Exception oEx) {
                logger.error(org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
            }

            URL url = new URL(sFileURL);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            logger.debug("DownloadFile.GetFileName: Connection Created");
            httpConn.setConnectTimeout(iConnectionTimeOut);
            httpConn.setReadTimeout(iReadTimeOut);
            logger.debug("DownloadFile.GetFileName: Timeout Setted: waiting response");
            int responseCode = httpConn.getResponseCode();

            // always check HTTP response code first
            if (responseCode == HttpURLConnection.HTTP_OK) {

                logger.debug("DownloadFile.GetFileName: Connected");

                String fileName = "";
                String disposition = httpConn.getHeaderField("Content-Disposition");
                String contentType = httpConn.getContentType();
                int contentLength = httpConn.getContentLength();

                if (disposition != null) {
                    // extracts file name from header field
                    int index = disposition.indexOf("filename=");
                    if (index > 0) {
                        fileName = disposition.substring(index + 10,  disposition.length() - 1);
                    }
                } else {
                    // extracts file name from URL
                    fileName = sFileURL.substring(sFileURL.lastIndexOf("/") + 1, sFileURL.length());
                }

                sReturnFilePath = fileName;

                logger.debug("Content-Type = " + contentType);
                logger.debug("Content-Disposition = " + disposition);
                logger.debug("Content-Length = " + contentLength);
                logger.debug("fileName = " + fileName);
            } else {
                logger.debug("No file to download. Server replied HTTP code: " + responseCode);
            }
            httpConn.disconnect();

            return  sReturnFilePath;
        }
        catch (Exception oEx) {
            logger.error(org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
        }

        return  "";
    }
}
