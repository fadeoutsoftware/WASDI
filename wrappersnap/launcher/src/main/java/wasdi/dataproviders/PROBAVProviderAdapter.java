package wasdi.dataproviders;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.DataProviderConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.LoggerWrapper;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;

public class PROBAVProviderAdapter extends ProviderAdapter {

	HashMap<String, LocalFileDescriptor> m_asCollectionsFolders = new HashMap<>();

	@SuppressWarnings("unchecked")
	public PROBAVProviderAdapter() {
		super();
		
		m_sDataProviderCode = "PROBAV";
		
		try {
			DataProviderConfig oConfig = WasdiConfig.Current.getDataProviderConfig(m_sDataProviderCode);
			
			String sFile = oConfig.fileDescriptors;
			
			if (new File(sFile).exists()) {
				m_asCollectionsFolders = (HashMap<String, LocalFileDescriptor>) SerializationUtils.deserializeXMLToObject(sFile);				
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public PROBAVProviderAdapter(LoggerWrapper logger) {
		super(logger);
		
		m_sDataProviderCode = "PROBAV";
	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		long lLenght = 0L;

	    // Domain check
	    if (Utils.isNullOrEmpty(sFileURL)) {
	    	m_oLogger.debug("PROBAVProviderAdapter.getDownloadFileSizeViaHttp: sFileURL is null");
	        return lLenght;
	    }

	    final String sFinalUser = m_sProviderUser;
	    final String sFinalPassword = m_sProviderPassword;
	        
	    m_oLogger.debug("PROBAVProviderAdapter.getDownloadFileSizeViaHttp: FileUrl = " + sFileURL);

	    URL oUrl = new URL(sFileURL);
	    HttpURLConnection oHttpConn = (HttpURLConnection) oUrl.openConnection();
	    oHttpConn.setRequestMethod("GET");
		oHttpConn.setRequestProperty("Accept", "*/*");
	    oHttpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:57.0) Gecko/20100101 Firefox/57.0");
	        
		// Basic HTTP Authentication "by hand"
		String sBasicAuth = sFinalUser + ":" + sFinalPassword;
		String sEncoded = Base64.getEncoder().encodeToString(sBasicAuth.getBytes());
		sBasicAuth = "Basic " + sEncoded;
		oHttpConn.setRequestProperty("Authorization",sBasicAuth);

	    int responseCode = oHttpConn.getResponseCode();

	    // always check HTTP response code first
	    if (responseCode == HttpURLConnection.HTTP_OK) {
	    	lLenght = oHttpConn.getHeaderFieldLong("Content-Length", 0L);
	    	m_oLogger.debug("PROBAVProviderAdapter.getDownloadFileSizeViaHttp: File size = " + lLenght);
	    	return lLenght;
	    } 
	    else {
	    	m_oLogger.debug("PROBAVProviderAdapter.getDownloadFileSizeViaHttp: No file to download. Server replied HTTP code: " + responseCode);
	        m_iLastError = responseCode;
	    }
	        
	    oHttpConn.disconnect();
	    return lLenght;
	}
	

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword, String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
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
		
	}
	
	@Override
	protected String downloadViaHttp(String sFileURL, String sDownloadUser, String sDownloadPassword, String sSaveDirOnServer) throws IOException {
		
		String sReturnFilePath = "";
		
		m_oLogger.debug("PROBAVProviderAdapter.downloadViaHttp: sDownloadUser = " + sDownloadUser);
		
		m_oLogger.debug("PROBAVProviderAdapter.downloadViaHttp: FileUrl = " + sFileURL);

		URL oUrl = new URL(sFileURL);
		HttpURLConnection oHttpConn = (HttpURLConnection) oUrl.openConnection();
		oHttpConn.setRequestMethod("GET");
		oHttpConn.setRequestProperty("Accept", "*/*");
		oHttpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:57.0) Gecko/20100101 Firefox/57.0");
		
		// Basic HTTP Authentication "by hand"
		String sBasicAuth = sDownloadUser + ":" + sDownloadPassword;
		String sEncoded = Base64.getEncoder().encodeToString(sBasicAuth.getBytes());
		sBasicAuth = "Basic " + sEncoded;
		oHttpConn.setRequestProperty("Authorization",sBasicAuth);
		
		int responseCode = oHttpConn.getResponseCode();

		// always check HTTP response code first
		if (responseCode == HttpURLConnection.HTTP_OK) {

			m_oLogger.debug("PROBAVProviderAdapter.downloadViaHttp: Connected");

			String sFileName = "";
			String sDisposition = oHttpConn.getHeaderField("Content-Disposition");
			String sContentType = oHttpConn.getContentType();
			long lContentLength = oHttpConn.getContentLengthLong();

			m_oLogger.debug("PROBAVProviderAdapter.downloadViaHttp. ContentLenght: " + lContentLength);

			if (sDisposition != null) {
				// extracts file name from header field
				int index = sDisposition.indexOf("filename=");
				if (index > 0) {
					sFileName = sDisposition.substring(index + 9, sDisposition.length() - 1);
				}
			} else {
				// extracts file name from URL
				sFileName = sFileURL.substring(sFileURL.lastIndexOf("/") + 1, sFileURL.length());
			}

			m_oLogger.debug("Content-Type = " + sContentType);
			m_oLogger.debug("Content-Disposition = " + sDisposition);
			m_oLogger.debug("Content-Length = " + lContentLength);
			m_oLogger.debug("fileName = " + sFileName);

			// opens input stream from the HTTP connection
			InputStream oInputStream = oHttpConn.getInputStream();
			
			if (!sSaveDirOnServer.endsWith("/")) sSaveDirOnServer+="/";
			
			String sSaveFilePath = sSaveDirOnServer + sFileName;

			m_oLogger.debug("PROBAVProviderAdapter.downloadViaHttp: Create Save File Path = " + sSaveFilePath);

			File oTargetFile = new File(sSaveFilePath);
			File oTargetDir = oTargetFile.getParentFile();
			oTargetDir.mkdirs();

			// opens an output stream to save into file
			FileOutputStream oOutputStream = new FileOutputStream(sSaveFilePath);

			//TODO take countermeasures in case of failure, e.g. retry if timeout. Here or in copyStream?
			copyStream(m_oProcessWorkspace, lContentLength, oInputStream, oOutputStream);

			sReturnFilePath = sSaveFilePath;

			m_oLogger.debug("PROBAVProviderAdapter.downloadViaHttp File downloaded " + sReturnFilePath);
		} else {
			m_oLogger.debug("PROBAVProviderAdapter.downloadViaHttp No file to download. Server replied HTTP code: " + responseCode);
			m_iLastError = responseCode;
		}
		oHttpConn.disconnect();
		return sReturnFilePath;		
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
				String sFileName = getFileName(sFileURL);

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
				updateProcessProgress(20);

				// Copy
				FileUtils.copyFile(oSourceFile, new File(sSaveDirOnServer));

				// Update user
				updateProcessProgress(100);

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
	public String getFileName(String sFileURL) throws Exception {
		
		try {
			// Domain check
			if (Utils.isNullOrEmpty(sFileURL)) {
				m_oLogger.debug("PROBAVProviderAdapter.getFileNameViaHttp: sFileURL is null or Empty");
				return "";
			}

			String sReturnFilePath = "";

			final String sFinalUser = m_sProviderUser;
			final String sFinalPassword = m_sProviderPassword;

			m_oLogger.debug("PROBAVProviderAdapter.getFileNameViaHttp: FileUrl = " + sFileURL);
			
			int iConnectionTimeOut = WasdiConfig.Current.connectionTimeout;
			int iReadTimeOut = WasdiConfig.Current.readTimeout;
			
			URL oUrl = new URL(sFileURL);
			HttpURLConnection oHttpConn = (HttpURLConnection) oUrl.openConnection();
			m_oLogger.debug("PROBAVProviderAdapter.getFileNameViaHttp: Connection Created");
			
			//NOTE: the DhUS version did not set GET and Accept. ONDA did. TEST 
			oHttpConn.setRequestMethod("GET");
			oHttpConn.setRequestProperty("Accept", "*/*");
			oHttpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:57.0) Gecko/20100101 Firefox/57.0");
			
			// Basic HTTP Authentication "by hand"
			String sBasicAuth = sFinalUser + ":" + sFinalPassword;
			String sEncoded = Base64.getEncoder().encodeToString(sBasicAuth.getBytes());
			sBasicAuth = "Basic " + sEncoded;
			
			oHttpConn.setRequestProperty("Authorization",sBasicAuth);
			oHttpConn.setConnectTimeout(iConnectionTimeOut);
			oHttpConn.setReadTimeout(iReadTimeOut);
			m_oLogger.debug("PROBAVProviderAdapter.getFileNameViaHttp: Timeout Setted: waiting response");
			int responseCode = oHttpConn.getResponseCode();

			// always check HTTP response code first
			if (responseCode == HttpURLConnection.HTTP_OK) {

				m_oLogger.debug("PROBAVProviderAdapter.getFileNameViaHttp: Connected");

				String sFileName = "";
				String sDisposition = oHttpConn.getHeaderField("Content-Disposition");
				String sContentType = oHttpConn.getContentType();
				int sContentLength = oHttpConn.getContentLength();

				if (sDisposition != null) {
					// extracts file name from header field
					int iIndex = sDisposition.indexOf("filename=");
					if (iIndex > 0) {
						if (sDisposition.endsWith("\"")) {
							sFileName = sDisposition.substring(iIndex + 10, sDisposition.length() - 1);
						}
						else {
							sFileName = sDisposition.substring(iIndex + 9, sDisposition.length());
						}
						
					}
				} else {
					// extracts file name from URL
					sFileName = sFileURL.substring(sFileURL.lastIndexOf("/") + 1, sFileURL.length());
				}

				sReturnFilePath = sFileName;

				m_oLogger.debug("Content-Type = " + sContentType);
				m_oLogger.debug("Content-Disposition = " + sDisposition);
				m_oLogger.debug("Content-Length = " + sContentLength);
				m_oLogger.debug("fileName = " + sFileName);
			} else {
				m_oLogger.debug("PROBAVProviderAdapter.getFileNameViaHttp No file to download. Server replied HTTP code: " + responseCode);
				m_iLastError = responseCode;
			}
			oHttpConn.disconnect();

			return sReturnFilePath;
			
		} catch (Exception oEx) {
			m_oLogger.error(org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
		}

		return "";
	}

	@Override
	protected void internalReadConfig() {
		
	}

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		if (sPlatformType.equals(Platforms.PROVAV)) {
			if (isWorkspaceOnSameCloud()) {
				return DataProviderScores.SAME_CLOUD_DOWNLOAD.getValue();
			}
			else {
				return DataProviderScores.DOWNLOAD.getValue();
			}
		}
		
		return 0;
	}

}
