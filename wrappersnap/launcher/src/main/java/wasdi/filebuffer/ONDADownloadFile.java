/**
 * Created by Cristiano Nattero on 2018-12-18
 * 
 * Fadeout software
 *
 */
package wasdi.filebuffer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;

import org.apache.log4j.Logger;

import wasdi.ConfigReader;
import wasdi.shared.business.ProcessWorkspace;
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
		//file:/mnt/OPTICAL/LEVEL-1C/2018/12/12/S2B_MSIL1C_20181212T010259_N0207_R045_T54PZA_20181212T021706.zip/.value

		m_oLogger.debug("ONDADownloadFile.GetDownloadSize: start " + sFileURL);

		long lLenght = 0L;

		if(sFileURL.startsWith("file:")) {

			m_sPrefix = "file:";
			m_sSuffix = "/.value";
			// Remove the prefix
			int iStart = sFileURL.indexOf(m_sPrefix) +m_sPrefix.length();
			String sPath = sFileURL.substring(iStart);

			// remove the ".value" suffix
			sPath = sPath.substring(0, sPath.lastIndexOf(m_sSuffix));

			// This is the folder: we need the .value file
			String sSourceFilePath = sPath + m_sSuffix;

			m_oLogger.debug("ONDADownloadFile.GetDownloadSize: full path " + sSourceFilePath);
			File oSourceFile = new File(sSourceFilePath);
			lLenght = oSourceFile.length();
			if (!oSourceFile.exists()) {
				m_oLogger.debug("ONDADownloadFile.GetDownloadSize: FILE DOES NOT EXISTS");
			}
			m_oLogger.debug("ONDADownloadFile.GetDownloadSize: Found length " + lLenght);
		} else if(sFileURL.startsWith("https:")) {
			lLenght = getSizeViaHttp(sFileURL);
		}

		return lLenght;
	}



	protected long getSizeViaHttp(String sFileURL) throws IOException {
		long lLength = 0L;

		// Domain check
		if (Utils.isNullOrEmpty(sFileURL)) {
			m_oLogger.debug("ONDADownloadFile.getSizeViaHttp: sFileURL is null");
			return lLength;
		}
		String sUser = "";
		String sPassword = "";
		try {
			sUser = ConfigReader.getPropValue("ONDA_USER");
			sPassword = ConfigReader.getPropValue("ONDA_PASSWORD");
		} catch (IOException e) {
			e.printStackTrace();
		}

		//TODO can we get read of these lines and abort execution instead? Do these members ever really get updated?
		if (!Utils.isNullOrEmpty(m_sProviderUser)) sUser = m_sProviderUser;
		if (!Utils.isNullOrEmpty(m_sProviderPassword)) sPassword = m_sProviderPassword;

		final String sFinalUser = sUser;
		final String sFinalPassword = sPassword;

		// dhus authentication
		Authenticator.setDefault(new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				try{
					return new PasswordAuthentication(sFinalUser, sFinalPassword.toCharArray());
				}
				catch (Exception oEx){
					m_oLogger.error("ONDADownloadFile.GetDownloadSize: exception setting auth " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
				}
				return null;
			}
		});

		m_oLogger.debug("ONDADownloadFile.GetDownloadSize: FileUrl = " + sFileURL);

		try {
			URL oUrl = new URL(sFileURL);
			HttpURLConnection oConnection = (HttpURLConnection) oUrl.openConnection();
			oConnection.setRequestMethod("GET");
			oConnection.setRequestProperty("Accept", "*/*");
			oConnection.setConnectTimeout(100000);
			oConnection.setReadTimeout(100000);
			m_oLogger.debug("ONDADownloadFile.GetDownloadSize: Call get response");
			int responseCode = oConnection.getResponseCode();
			m_oLogger.debug("ONDADownloadFile.GetDownloadSize: Response got");

			// always check HTTP response code first
			if (responseCode == HttpURLConnection.HTTP_OK) {
				lLength = oConnection.getHeaderFieldLong("Content-Length", 0L);
				m_oLogger.debug("ONDADownloadFile.GetDownloadSize: File size = " + lLength);
				return lLength;
			} else {
				m_oLogger.debug("ONDADownloadFile.GetDownloadSize: No file to download. Server replied HTTP code: " + responseCode);
				m_iLastError = responseCode;
			}
			oConnection.disconnect();			
		}
		catch (Exception oEx) {
			m_oLogger.debug("ONDADownloadFile.GetDownloadSize: Exception " + oEx.toString());
		}


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
			m_oLogger.debug("ONDAVDownloadFile.ExecuteDownloadFile: sFileURL is null");
			return "";
		}
		if (Utils.isNullOrEmpty(sSaveDirOnServer)) {
			m_oLogger.debug("ONDADownloadFile.ExecuteDownloadFile: sSaveDirOnServer is null");
			return "";
		}



		if(sFileURL.startsWith("file:")) {
			//file:/mnt/OPTICAL/LEVEL-1C/2018/12/12/S2B_MSIL1C_20181212T010259_N0207_R045_T54PZA_20181212T021706.zip/.value
			m_sPrefix = "file:";
			m_sSuffix = "/.value";
			// Remove the prefix
			int iStart = sFileURL.indexOf(m_sPrefix) +m_sPrefix.length();
			String sPath = sFileURL.substring(iStart);

			// remove the ".value" suffix
			sPath = sPath.substring(0, sPath.lastIndexOf(m_sSuffix));

			// This is the folder: we need the .value file
			String sSourceFilePath = sPath + m_sSuffix;
			File oSourceFile = new File(sSourceFilePath);

			// Destination file name: start from the simple name
			//commented out as it returns null if the file is not in WASDI, use the next one instead
			//String sDestinationFileName = GetFileName(sFileURL);
			String sDestinationFileName = sPath.substring( sPath.lastIndexOf("/") + 1);
			// set the destination folder
			if (sSaveDirOnServer.endsWith("/") == false) sSaveDirOnServer += "/";
			sDestinationFileName = sSaveDirOnServer + sDestinationFileName;

			// copy the product from file system
			//TODO read file as a stream and notify every 10%
			//FileUtils.copyFile(oSourceFile, new File(sDestinationFileName));
			try {
				File oDestionationFile = new File(sDestinationFileName);
				//oDestionationFile.createNewFile();
				InputStream oInputStream = new FileInputStream(oSourceFile);
				OutputStream oOutputStream = new FileOutputStream(oDestionationFile);
				copyStream(oProcessWorkspace, oSourceFile.length(), oInputStream, oOutputStream);


			} catch (Exception e) {
				e.printStackTrace();
				m_oLogger.debug( e.toString() );
			}
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
		m_oLogger.debug("DownloadFile.ExecuteDownloadFile: sDownloadUser = " + sDownloadUser);
		if (sDownloadUser!=null) {
			Authenticator.setDefault(new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					try{
						return new PasswordAuthentication(sDownloadUser, sDownloadPassword.toCharArray());
					} catch (Exception oEx){
						m_oLogger.error("DownloadFile.ExecuteDownloadFile: exception setting auth " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
					}
					return null;
				}
			});        	
		}

		m_oLogger.debug("DownloadFile.ExecuteDownloadFile: FileUrl = " + sFileURL);

		URL oUrl = new URL(sFileURL);
		HttpURLConnection oConnection = (HttpURLConnection) oUrl.openConnection();
		oConnection.setRequestMethod("GET");
		oConnection.setRequestProperty("Accept", "*/*");

		int responseCode = oConnection.getResponseCode();

		// always check HTTP response code first
		if (responseCode == HttpURLConnection.HTTP_OK) {

			m_oLogger.debug("DownloadFile.ExecuteDownloadFile: Connected");

			String sFileName = "";
			String sDisposition = oConnection.getHeaderField("Content-Disposition");
			String sContentType = oConnection.getContentType();
			long  lContentLength = oConnection.getContentLength();

			m_oLogger.debug("ExecuteDownloadFile. ContentLenght: " + lContentLength);

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

			m_oLogger.debug("Content-Type = " + sContentType);
			m_oLogger.debug("Content-Disposition = " + sDisposition);
			m_oLogger.debug("Content-Length = " + lContentLength);
			m_oLogger.debug("fileName = " + sFileName);

			// opens input stream from the HTTP connection
			InputStream oInputStream = oConnection.getInputStream();
			String saveFilePath= sSaveDirOnServer + "/" + sFileName;

			m_oLogger.debug("DownloadFile.ExecuteDownloadFile: Create Save File Path = " + saveFilePath);

			File oTargetFile = new File(saveFilePath);
			File oTargetDir = oTargetFile.getParentFile();
			oTargetDir.mkdirs();

			// opens an output stream to save into file
			FileOutputStream oOutputStream = new FileOutputStream(saveFilePath);

			copyStream(oProcessWorkspace, lContentLength, oInputStream, oOutputStream);

			sReturnFilePath = saveFilePath;

			m_oLogger.debug("File downloaded " + sReturnFilePath);
		} else {
			m_oLogger.debug("No file to download. Server replied HTTP code: " + responseCode);
			m_iLastError = responseCode;
		}
		oConnection.disconnect();
		return  sReturnFilePath;
	}

	protected void copyStream(ProcessWorkspace oProcessWorkspace, long lContentLength, InputStream oInputStream,
			OutputStream oOutputStream) throws IOException {

		// Cumulative Byte Count
		int iTotalBytes = 0;
		// Byte that represent 10% of the file
		long lTenPercent = lContentLength/10;
		// Percent of the completed download
		int iFilePercent = 0 ;

		int iBytesRead = -1;
		byte[] abBuffer = new byte[BUFFER_SIZE];
		int nZeroes = MAX_NUM_ZEORES_DURING_READ;
		while ((iBytesRead = oInputStream.read(abBuffer)) != -1) {

			if (iBytesRead <= 0) {
				m_oLogger.debug("ExecuteDownloadFile. Read 0 bytes from stream. Counter: " + nZeroes);
				nZeroes--;
			} else {
				nZeroes = MAX_NUM_ZEORES_DURING_READ;
			}
			if (nZeroes <=0 ) break;

			//logger.debug("ExecuteDownloadFile. Read " + iBytesRead +  " bytes from stream");

			oOutputStream.write(abBuffer, 0, iBytesRead);

			// Sum bytes
			iTotalBytes += iBytesRead;

			// Overcome a 10% limit?
			if(oProcessWorkspace!=null && lContentLength>BUFFER_SIZE && iTotalBytes>=lTenPercent && iFilePercent<=100) {
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
	}

	/* (non-Javadoc)
	 * @see wasdi.filebuffer.DownloadFile#GetFileName(java.lang.String)
	 */
	@Override
	public String GetFileName(String sFileURL) throws Exception {
		//check whether the file has already been downloaded, else return null

		if (Utils.isNullOrEmpty(sFileURL)) {
			m_oLogger.debug("DownloadFile.GetFileName: sFileURL is null or Empty");
			m_oLogger.fatal("DownloadFile.GetFileName: sFileURL is null or Empty");
			return "";
		}

		if(sFileURL.startsWith("file:")) {

			m_sPrefix = "file:";
			m_sSuffix = "/.value";
			// Remove the prefix
			int iStart = sFileURL.indexOf(m_sPrefix) +m_sPrefix.length();
			String sPath = sFileURL.substring(iStart);

			// remove the ".value" suffix
			sPath = sPath.substring(0, sPath.lastIndexOf(m_sSuffix));

			// Destination file name: start from the simple name
			//commented out as it returns null if the file is not in WASDI, use the next one instead
			//String sDestinationFileName = GetFileName(sFileURL);
			String sDestinationFileName = sPath.substring( sPath.lastIndexOf("/") + 1);
			return sDestinationFileName;

		} else if(sFileURL.startsWith("https://")) {
			return getFileNameViaHttp(sFileURL);
		} 
		return null;	
	}

	protected String getFileNameViaHttp(String sFileURL) throws Exception {
		try {
			// Domain check
			if (Utils.isNullOrEmpty(sFileURL)) {
				m_oLogger.debug("DownloadFile.GetFileName: sFileURL is null or Empty");
				return "";
			}

			String sReturnFilePath = "";

			String sUser = ConfigReader.getPropValue("ONDA_USER");
			String sPassword = ConfigReader.getPropValue("ONDA_PASSWORD");

			if (!Utils.isNullOrEmpty(m_sProviderUser)) sUser = m_sProviderUser;
			if (!Utils.isNullOrEmpty(m_sProviderPassword)) sPassword = m_sProviderPassword;

			final String sFinalUser = sUser;
			final String sFinalPassword = sPassword;

			// dhus authentication
			Authenticator.setDefault(new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					try{
						return new PasswordAuthentication(sFinalUser, sFinalPassword.toCharArray());
					}
					catch (Exception oEx){
						m_oLogger.error("DownloadFile.GetFileName: exception setting auth " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
					}
					return null;
				}
			});

			m_oLogger.debug("DownloadFile.GetFileName: FileUrl = " + sFileURL);

			String sConnectionTimeout = ConfigReader.getPropValue("CONNECTION_TIMEOUT");
			String sReadTimeOut = ConfigReader.getPropValue("READ_TIMEOUT");

			int iConnectionTimeOut = 10000;
			int iReadTimeOut = 10000;

			try {
				iConnectionTimeOut = Integer.parseInt(sConnectionTimeout);
			}
			catch (Exception oEx) {
				m_oLogger.error(org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
			}
			try {
				iReadTimeOut = Integer.parseInt(sReadTimeOut);
			}
			catch (Exception oEx) {
				m_oLogger.error(org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
			}

			URL url = new URL(sFileURL);
			HttpURLConnection oConnection = (HttpURLConnection) url.openConnection();
			oConnection.setRequestMethod("GET");
			oConnection.setRequestProperty("Accept", "*/*");
			m_oLogger.debug("DownloadFile.GetFileName: Connection Created");
			oConnection.setConnectTimeout(iConnectionTimeOut);
			oConnection.setReadTimeout(iReadTimeOut);
			m_oLogger.debug("DownloadFile.GetFileName: Timeout Setted: waiting response");
			int responseCode = oConnection.getResponseCode();

			// always check HTTP response code first
			if (responseCode == HttpURLConnection.HTTP_OK) {

				m_oLogger.debug("DownloadFile.GetFileName: Connected");

				String fileName = "";
				String disposition = oConnection.getHeaderField("Content-Disposition");
				String contentType = oConnection.getContentType();
				int contentLength = oConnection.getContentLength();

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

				m_oLogger.debug("Content-Type = " + contentType);
				m_oLogger.debug("Content-Disposition = " + disposition);
				m_oLogger.debug("Content-Length = " + contentLength);
				m_oLogger.debug("fileName = " + fileName);
			} else {
				m_oLogger.debug("No file to download. Server replied HTTP code: " + responseCode);
				m_iLastError = responseCode;
			}
			oConnection.disconnect();

			return  sReturnFilePath;
		}
		catch (Exception oEx) {
			m_oLogger.error(org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
		}

		return  "";
	}
}
/*

		//check whether the file has already been downloaded, else return null

		if (Utils.isNullOrEmpty(sFileURL)) {
			m_oLogger.debug("DownloadFile.GetFileName: sFileURL is null or Empty");
			m_oLogger.fatal("DownloadFile.GetFileName: sFileURL is null or Empty");
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
			//https://catalogue.onda-dias.eu/dias-catalogue/Products(357ae76d-f1c4-4f25-b535-e278c3f937af)/$value
			m_sPrefix = "https://";
			int iStart = sFileURL.lastIndexOf("/") + 1;
			String sFileName = sFileURL.substring(iStart);
			return sFileName;
		} 
		return null;	
 */