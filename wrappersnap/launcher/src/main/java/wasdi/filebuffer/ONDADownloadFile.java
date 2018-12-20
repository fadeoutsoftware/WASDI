/**
 * Created by Cristiano Nattero on 2018-12-18
 * 
 * Fadeout software
 *
 */
package wasdi.filebuffer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.utils.Utils;

/**
 * @author c.nattero
 *
 */
public class ONDADownloadFile extends DownloadFile {

	String m_sPrefix = "";
	String m_sSuffix = "";

	/**
	 * 
	 */
	public ONDADownloadFile() {

	}

	/**
	 * @param logger
	 */
	public ONDADownloadFile(Logger logger) {
		super(logger);
	}

	/* (non-Javadoc)
	 * @see wasdi.filebuffer.DownloadFile#GetDownloadFileSize(java.lang.String)
	 */
	@Override
	public long GetDownloadFileSize(String sFileURL) throws Exception {
		//http:file:/mnt/OPTICAL/LEVEL-1C/2018/12/12/S2B_MSIL1C_20181212T010259_N0207_R045_T54PZA_20181212T021706.zip.value
		long lLenght = 0L;

		if(sFileURL.startsWith("file:")) {

			// Remove the prefix
			int iStart = sFileURL.indexOf(m_sPrefix) +m_sPrefix.length();
			String sSourceFilePath = sFileURL.substring(iStart);

			// remove the .value
			sSourceFilePath = sSourceFilePath.substring(0, sSourceFilePath.lastIndexOf('.'));

			// This is the folder: we need the .value file
			sSourceFilePath += "/.value";
			File oSourceFile = new File(sSourceFilePath);
			lLenght = oSourceFile.length();
		} else if(sFileURL.startsWith("https:")) {
			lLenght = getSizeViaHttp(sFileURL);
		}

		return lLenght;
	}

	

	protected long getSizeViaHttp(String sFileURL) {
		long lLength = 0L;
		//TODO implement
		return lLength;
	}

	/* (non-Javadoc)
	 * @see wasdi.filebuffer.DownloadFile#ExecuteDownloadFile(java.lang.String, java.lang.String, java.lang.String, java.lang.String, wasdi.shared.business.ProcessWorkspace)
	 */
	@Override
	public String ExecuteDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace) throws Exception {
		// Domain check
		if (Utils.isNullOrEmpty(sFileURL)) {
			logger.debug("ONDAVDownloadFile.ExecuteDownloadFile: sFileURL is null");
			return "";
		}
		if (Utils.isNullOrEmpty(sSaveDirOnServer)) {
			logger.debug("ONDADownloadFile.ExecuteDownloadFile: sSaveDirOnServer is null");
			return "";
		}



		if(sFileURL.startsWith("file:")) {
			//file:/mnt/OPTICAL/LEVEL-1C/2018/12/12/S2B_MSIL1C_20181212T010259_N0207_R045_T54PZA_20181212T021706.zip.value
			m_sPrefix = "file:";
			m_sSuffix = ".value";
			// Remove the prefix
			int iStart = sFileURL.indexOf(m_sPrefix) +m_sPrefix.length();
			String sPath = sFileURL.substring(iStart);

			// remove the ".value" suffix
			sPath = sPath.substring(0, sPath.lastIndexOf(m_sSuffix));

			// This is the folder: we need the .value file
			String sSourceFilePath = sPath + "/" + m_sSuffix;
			File oSourceFile = new File(sSourceFilePath);

			// Destination file name: start from the simple name
			//commented out as it returns null if the file is not in WASDI, use the next one instead
			//String sDestinationFileName = GetFileName(sFileURL);
			String sDestinationFileName = sPath.substring( sPath.lastIndexOf("/") + 1);
			// set the destination folder
			if (sSaveDirOnServer.endsWith("/") == false) sSaveDirOnServer += "/";
			sDestinationFileName = sSaveDirOnServer + sDestinationFileName;

			// copy the product from file system
			FileUtils.copyFile(oSourceFile, new File(sDestinationFileName));
			return sDestinationFileName;
		} else if(sFileURL.startsWith("https://")) {
			return downloadViaHttp(sFileURL, sDownloadUser, sDownloadPassword, sSaveDirOnServer, oProcessWorkspace);

		}
		return "";
	}

	protected String downloadViaHttp(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace) throws IOException {
		//TODO move this code into superclass, see DhUSDownloadFile
		String sReturnFilePath = "";

        // TODO: Here we are assuming dhus authentication. But we have to find a general solution
        logger.debug("DownloadFile.ExecuteDownloadFile: sDownloadUser = " + sDownloadUser);
		if (sDownloadUser!=null) {
			Authenticator.setDefault(new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					try{
						return new PasswordAuthentication(sDownloadUser, sDownloadPassword.toCharArray());
					} catch (Exception oEx){
						logger.error("DownloadFile.ExecuteDownloadFile: exception setting auth " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
					}
					return null;
				}
			});        	
		}

		logger.debug("DownloadFile.ExecuteDownloadFile: FileUrl = " + sFileURL);

		URL oUrl = new URL(sFileURL);
		HttpURLConnection oConnection = (HttpURLConnection) oUrl.openConnection();
		oConnection.setRequestMethod("GET");
		oConnection.setRequestProperty("Accept", "*/*");
		
		int responseCode = oConnection.getResponseCode();
		
		// always check HTTP response code first
		if (responseCode == HttpURLConnection.HTTP_OK) {

			logger.debug("DownloadFile.ExecuteDownloadFile: Connected");

			String sFileName = "";
			String sDisposition = oConnection.getHeaderField("Content-Disposition");
			String sContentType = oConnection.getContentType();
			int iContentLength = oConnection.getContentLength();

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
			InputStream oInputStream = oConnection.getInputStream();
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
			m_iLastError = responseCode;
		}
		oConnection.disconnect();
		return  sReturnFilePath;
	}

	/* (non-Javadoc)
	 * @see wasdi.filebuffer.DownloadFile#GetFileName(java.lang.String)
	 */
	@Override
	public String GetFileName(String sFileURL) throws Exception {
		//check whether the file has already been downloaded, else return null

		if (Utils.isNullOrEmpty(sFileURL)) {
			logger.debug("DownloadFile.GetFileName: sFileURL is null or Empty");
			logger.fatal("DownloadFile.GetFileName: sFileURL is null or Empty");
			return "";
		}

		if(sFileURL.startsWith("file:")) {
			String sSourceFilePath = "";
			m_sPrefix = "file:";
			m_sSuffix = ".value";
			int iStart = sFileURL.indexOf(m_sPrefix) + m_sPrefix.length();
			sSourceFilePath += sFileURL.substring(iStart);
			File oInputFile = new File(sSourceFilePath);
			String sOnlyName = oInputFile.getName(); 
			sOnlyName = sOnlyName.substring(0, sOnlyName.lastIndexOf(m_sSuffix)); 
			return sOnlyName;

		} else if(sFileURL.startsWith("https://")) {
			m_sPrefix = "https://";
			int iStart = sFileURL.lastIndexOf("/") + 1;
			String sFileName = sFileURL.substring(iStart);
			return sFileName;
		} 
		return null;	
	}

}
