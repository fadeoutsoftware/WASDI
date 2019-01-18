package wasdi.filebuffer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import wasdi.ConfigReader;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;

public class PROBAVProviderAdapter extends ProviderAdapter {

	HashMap<String, LocalFileDescriptor> m_asCollectionsFolders = new HashMap<>();

	public PROBAVProviderAdapter() {
		super();

		try {
			String sFile = ConfigReader.getPropValue("PROBAV_FILE_DESCRIPTORS");

			m_asCollectionsFolders = (HashMap<String, LocalFileDescriptor>) SerializationUtils
					.deserializeXMLToObject(sFile);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public PROBAVProviderAdapter(Logger logger) {
		super(logger);
	}

	@Override
	public long GetDownloadFileSize(String sFileURL) throws Exception {
		
		/*
		long lLenght = 0L;

		// Domain check
		if (Utils.isNullOrEmpty(sFileURL)) {
			m_oLogger.debug("PROBAVProviderAdapter.GetDownloadSize: sFileURL is null");
			return lLenght;
		}

		String sUser = "";
		String sPassword = "";
		try {
			sUser = ConfigReader.getPropValue("DHUS_USER");
			sPassword = ConfigReader.getPropValue("DHUS_PASSWORD");
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (!Utils.isNullOrEmpty(m_sProviderUser))
			sUser = m_sProviderUser;
		if (!Utils.isNullOrEmpty(m_sProviderPassword))
			sPassword = m_sProviderPassword;

		final String sFinalUser = sUser;
		final String sFinalPassword = sPassword;

		// dhus authentication
		Authenticator.setDefault(new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				try {
					return new PasswordAuthentication(sFinalUser, sFinalPassword.toCharArray());
				} catch (Exception oEx) {
					m_oLogger.error("PROBAVProviderAdapter.GetDownloadSize: exception setting auth "
							+ org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
				}
				return null;
			}
		});

		m_oLogger.debug("PROBAVProviderAdapter.GetDownloadSize: FileUrl = " + sFileURL);

		URL url = new URL(sFileURL);
		HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
		int responseCode = httpConn.getResponseCode();

		// always check HTTP response code first
		if (responseCode == HttpURLConnection.HTTP_OK) {

			lLenght = httpConn.getHeaderFieldLong("Content-Length", 0L);

			m_oLogger.debug("PROBAVProviderAdapter.GetDownloadSize: File size = " + lLenght);

			return lLenght;

		} else {
			m_oLogger.debug("PROBAVProviderAdapter.GetDownloadSize: No file to download. Server replied HTTP code: "
					+ responseCode);
			m_iLastError = responseCode;
		}
		httpConn.disconnect();

		return lLenght;
		*/
		
		return getDownloadFileSizeViaHttp(sFileURL);
	}

	@Override
	public String ExecuteDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword, String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace) throws Exception {
		// Domain check
		if (Utils.isNullOrEmpty(sFileURL)) {
			m_oLogger.debug("PROBAVProviderAdapter.ExecuteDownloadFile: sFileURL is null");
			return "";
		}
		if (Utils.isNullOrEmpty(sSaveDirOnServer)) {
			m_oLogger.debug("PROBAVProviderAdapter.ExecuteDownloadFile: sSaveDirOnServer is null");
			return "";
		}
		setProcessWorkspace(oProcessWorkspace);

		String sReturnFilePath = CopyLocalFile(sFileURL, sDownloadUser, sDownloadPassword, sSaveDirOnServer, oProcessWorkspace);

		if (!Utils.isNullOrEmpty(sReturnFilePath)) {
			m_oLogger.debug("PROBAVProviderAdapter.ExecuteDownloadFile: File found in local repo. Return");

			return sReturnFilePath;
		} else {
			m_oLogger.debug( "PROBAVProviderAdapter.ExecuteDownloadFile: File NOT found in local repo, try to donwload from provider");
			
			return downloadViaHttp(sFileURL, sDownloadUser, sDownloadPassword, sSaveDirOnServer);
		}
		
		/*
		// authentication
		m_oLogger.debug("PROBAVProviderAdapter.ExecuteDownloadFile: sDownloadUser = " + sDownloadUser);

		if (sDownloadUser != null) {
			Authenticator.setDefault(new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					try {
						return new PasswordAuthentication(sDownloadUser, sDownloadPassword.toCharArray());
					} catch (Exception oEx) {
						m_oLogger.error("PROBAVProviderAdapter.ExecuteDownloadFile: exception setting auth "
								+ org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
					}
					return null;
				}
			});
		}

		m_oLogger.debug("PROBAVProviderAdapter.ExecuteDownloadFile: FileUrl = " + sFileURL);

		URL url = new URL(sFileURL);
		HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
		int responseCode = httpConn.getResponseCode();

		// always check HTTP response code first
		if (responseCode == HttpURLConnection.HTTP_OK) {

			m_oLogger.debug("PROBAVProviderAdapter.ExecuteDownloadFile: Connected");

			String sFileName = "";
			String sDisposition = httpConn.getHeaderField("Content-Disposition");
			String sContentType = httpConn.getContentType();
			int iContentLength = httpConn.getContentLength();

			m_oLogger.debug("PROBAVProviderAdapter.ExecuteDownloadFile. ContentLenght: " + iContentLength);

			if (sDisposition != null) {
				// extracts file name from header field
				int index = sDisposition.indexOf("filename=");
				if (index > 0) {
					sFileName = sDisposition.substring(index + 9, sDisposition.length());
				}
			} else {
				// extracts file name from URL
				sFileName = sFileURL.substring(sFileURL.lastIndexOf("/") + 1, sFileURL.length());
			}

			m_oLogger.debug("Content-Type = " + sContentType);
			m_oLogger.debug("Content-Disposition = " + sDisposition);
			m_oLogger.debug("Content-Length = " + iContentLength);
			m_oLogger.debug("fileName = " + sFileName);

			// opens input stream from the HTTP connection
			InputStream oInputStream = httpConn.getInputStream();
			String saveFilePath = sSaveDirOnServer + "/" + sFileName;

			m_oLogger.debug("PROBAVProviderAdapter.ExecuteDownloadFile: Create Save File Path = " + saveFilePath);

			File oTargetFile = new File(saveFilePath);
			File oTargetDir = oTargetFile.getParentFile();
			oTargetDir.mkdirs();

			// opens an output stream to save into file
			FileOutputStream oOutputStream = new FileOutputStream(saveFilePath);

			// Cumulative Byte Count
			int iTotalBytes = 0;
			// Byte that represent 10% of the file
			int iTenPercent = iContentLength / 10;
			// Percent of the completed download
			int iFilePercent = 0;

			int iBytesRead = -1;
			byte[] abBuffer = new byte[BUFFER_SIZE];
			int nZeroes = MAX_NUM_ZEORES_DURING_READ;
			while ((iBytesRead = oInputStream.read(abBuffer)) != -1) {

				if (iBytesRead <= 0) {
					m_oLogger.debug(
							"PROBAVProviderAdapter.ExecuteDownloadFile. Read 0 bytes from stream. Counter: " + nZeroes);
					nZeroes--;
				} else {
					nZeroes = MAX_NUM_ZEORES_DURING_READ;
				}
				if (nZeroes <= 0)
					break;

				// logger.debug("ExecuteDownloadFile. Read " + iBytesRead + " bytes from
				// stream");

				oOutputStream.write(abBuffer, 0, iBytesRead);

				// Sum bytes
				iTotalBytes += iBytesRead;

				// Overcome a 10% limit?
				if (oProcessWorkspace != null && iContentLength > BUFFER_SIZE && iTotalBytes >= iTenPercent
						&& iFilePercent <= 100) {
					// Increase the file
					iFilePercent += 10;
					if (iFilePercent > 100)
						iFilePercent = 100;
					// Reset the count
					iTotalBytes = 0;
					// Update the progress
					if (nZeroes == MAX_NUM_ZEORES_DURING_READ)
						UpdateProcessProgress(iFilePercent);
				}
			}

			oOutputStream.close();
			oInputStream.close();

			sReturnFilePath = saveFilePath;

			m_oLogger.debug("PROBAVProviderAdapter File downloaded " + sReturnFilePath);
		} else {
			m_oLogger.debug("PROBAVDownloadFile No file to download. Server replied HTTP code: " + responseCode);
			m_iLastError = responseCode;
		}
		httpConn.disconnect();

		return sReturnFilePath;
		*/
	}

	/**
	 * Copy a file from a local Repo to the workspace. If the file is not available
	 * returns empty or null other wise the new file path
	 * 
	 * @param sFileURL
	 *            Url to download the file
	 * @param sDownloadUser
	 *            User
	 * @param sDownloadPassword
	 *            Password
	 * @param sSaveDirOnServer
	 *            Destination Folder
	 * @param oProcessWorkspace
	 *            Object to update user interface
	 * @return File Path if present, null or empty otherwise
	 * @throws Exception
	 */
	public String CopyLocalFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace) throws Exception {

		// File Descriptor
		LocalFileDescriptor oDescriptor = null;

		// Search the right Descriptor
		for (String sKey : m_asCollectionsFolders.keySet()) {
			if (sFileURL.contains(sKey)) {
				oDescriptor = m_asCollectionsFolders.get(sKey);
				break;
			}
		}

		if (oDescriptor != null) {

			// Get Source Folder
			String sSourceFolder = oDescriptor.m_sFolder;

			// Is it a single file?
			if (oDescriptor.m_bSingleFile) {

				// Yes single file

				// Get the final name: should be like PROBAV_L1C_20180429_230542_1_V101.HDF5
				String sFileName = GetFileName(sFileURL);

				// Split on _
				String[] asSplittedFileName = sFileName.split("_");

				// Safe check
				if (asSplittedFileName == null) {
					m_oLogger.error("PROBAVProviderAdapter.CopyLocalFile: splitted file name is null");
					return "";
				}
				if (asSplittedFileName.length == 0) {
					m_oLogger.error("PROBAVProviderAdapter.CopyLocalFile: splitted file name is empyt");
					return "";
				}
				if (asSplittedFileName.length < 3) {
					m_oLogger.error("PROBAVProviderAdapter.CopyLocalFile: splitted file name length < 3");
					return "";
				}

				// Get the date
				String sDateString = asSplittedFileName[2];

				if (sDateString.length() < 8) {
					m_oLogger.error("PROBAVProviderAdapter.CopyLocalFile: Date String lenght < 8");
					return "";
				}

				// Extract Year
				String sYear = sDateString.substring(0, 4);
				// Extract Month
				String sMonth = sDateString.substring(4, 6);

				// Split again, on "." this time
				asSplittedFileName = sFileName.split("\\.");

				// Safe check
				if (asSplittedFileName == null) {
					m_oLogger.error("PROBAVProviderAdapter.CopyLocalFile: splitted file name 2 is null");
					return "";
				}
				if (asSplittedFileName.length == 0) {
					m_oLogger.error("PROBAVProviderAdapter.CopyLocalFile: splitted file name 2 is empyt");
					return "";
				}
				if (asSplittedFileName.length < 2) {
					m_oLogger.error("PROBAVProviderAdapter.CopyLocalFile: splitted file name 2 length < 2");
					return "";
				}

				// Get file name without extension
				String sFileFolder = asSplittedFileName[0];

				// Final Source: base + year + month + name + fulldate + filefolder + file
				sSourceFolder = sSourceFolder + "/" + sYear + "/" + sMonth + "/" + sDateString + "/" + sFileFolder + "/"
						+ sFileName;

				m_oLogger.debug("PROBAVProviderAdapter.CopyLocalFile: Source File" + sSourceFolder);

				// Final destination: base + file
				if (!sSaveDirOnServer.endsWith("/"))
					sSaveDirOnServer += "/";
				sSaveDirOnServer += sFileName;

				m_oLogger.debug("PROBAVProviderAdapter.CopyLocalFile: Destination File" + sSaveDirOnServer);

				File oSourceFile = new File(sSourceFolder);

				if (oSourceFile.exists() == false) {
					m_oLogger.warn("PROBAVProviderAdapter.CopyLocalFile: Source File not available. exit");
					return "";
				}

				// Update user
				UpdateProcessProgress(20);

				// Copy
				FileUtils.copyFile(oSourceFile, new File(sSaveDirOnServer));

				// Update user
				UpdateProcessProgress(100);

				return sSaveDirOnServer;
			} else {
				
				if (sSourceFolder == null) return null;
				
				File oSourceFolder = new File(sSourceFolder);
				
				if (!oSourceFolder.exists()) return null;
				
				// Copy all the folder
				FileUtils.copyDirectory(oSourceFolder, new File(sSaveDirOnServer));
			}

			return "";
		} else {
			return null;
		}
	}

	@Override
	public String GetFileName(String sFileURL) throws Exception {

		return getFileNameViaHttp(sFileURL);
		
		/*
		try {
			// Domain check
			if (Utils.isNullOrEmpty(sFileURL)) {
				m_oLogger.debug("PROBAVProviderAdapter.GetFileName: sFileURL is null or Empty");
				return "";
			}

			String sReturnFilePath = "";

			String sUser = ConfigReader.getPropValue("DHUS_USER");
			String sPassword = ConfigReader.getPropValue("DHUS_PASSWORD");

			if (!Utils.isNullOrEmpty(m_sProviderUser))
				sUser = m_sProviderUser;
			if (!Utils.isNullOrEmpty(m_sProviderPassword))
				sPassword = m_sProviderPassword;

			final String sFinalUser = sUser;
			final String sFinalPassword = sPassword;

			// dhus authentication
			Authenticator.setDefault(new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					try {
						return new PasswordAuthentication(sFinalUser, sFinalPassword.toCharArray());
					} catch (Exception oEx) {
						m_oLogger.error("PROBAVProviderAdapter.GetFileName: exception setting auth "
								+ org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
					}
					return null;
				}
			});

			m_oLogger.debug("PROBAVProviderAdapter.GetFileName: FileUrl = " + sFileURL);

			String sConnectionTimeout = ConfigReader.getPropValue("CONNECTION_TIMEOUT");
			String sReadTimeOut = ConfigReader.getPropValue("READ_TIMEOUT");

			int iConnectionTimeOut = 10000;
			int iReadTimeOut = 10000;

			try {
				iConnectionTimeOut = Integer.parseInt(sConnectionTimeout);
			} catch (Exception oEx) {
				m_oLogger.error(org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
			}
			try {
				iReadTimeOut = Integer.parseInt(sReadTimeOut);
			} catch (Exception oEx) {
				m_oLogger.error(org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
			}

			URL url = new URL(sFileURL);
			HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
			m_oLogger.debug("PROBAVProviderAdapter.GetFileName: Connection Created");
			httpConn.setConnectTimeout(iConnectionTimeOut);
			httpConn.setReadTimeout(iReadTimeOut);
			m_oLogger.debug("PROBAVProviderAdapter.GetFileName: Timeout Setted: waiting response");
			int responseCode = httpConn.getResponseCode();

			// always check HTTP response code first
			if (responseCode == HttpURLConnection.HTTP_OK) {

				m_oLogger.debug("PROBAVProviderAdapter.GetFileName: Connected");

				String fileName = "";
				String disposition = httpConn.getHeaderField("Content-Disposition");
				String contentType = httpConn.getContentType();
				int contentLength = httpConn.getContentLength();

				if (disposition != null) {
					// extracts file name from header field
					int index = disposition.indexOf("filename=");
					if (index > 0) {
						fileName = disposition.substring(index + 9, disposition.length());
					}
				} else {
					// extracts file name from URL
					fileName = sFileURL.substring(sFileURL.lastIndexOf("/") + 1, sFileURL.length());
				}

				sReturnFilePath = fileName;

				m_oLogger.debug("PROBAVProviderAdapter.GetFileName: Content-Type = " + contentType);
				m_oLogger.debug("PROBAVProviderAdapter.GetFileName: Content-Disposition = " + disposition);
				m_oLogger.debug("PROBAVProviderAdapter.GetFileName: Content-Length = " + contentLength);
				m_oLogger.debug("PROBAVProviderAdapter.GetFileName: fileName = " + fileName);
			} else {
				m_oLogger.debug("PROBAVDownloadFile.GetFileName: No file to download. Server replied HTTP code: " + responseCode);
				m_iLastError = responseCode;
			}
			httpConn.disconnect();

			return sReturnFilePath;
		} catch (Exception oEx) {
			m_oLogger.error(org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
		}

		return "";
		*/
	}

}
