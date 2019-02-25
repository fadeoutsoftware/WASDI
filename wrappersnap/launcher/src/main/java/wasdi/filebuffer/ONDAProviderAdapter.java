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
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.utils.Utils;

/**
 * @author c.nattero
 *
 */
public class ONDAProviderAdapter extends ProviderAdapter {

	/**
	 * 
	 */
	public ONDAProviderAdapter() {

	}

	/**
	 * @param logger
	 */
	public ONDAProviderAdapter(Logger logger) {
		super(logger);
	}

	/* (non-Javadoc)
	 * @see wasdi.filebuffer.DownloadFile#GetDownloadFileSize(java.lang.String)
	 */
	@Override
	public long GetDownloadFileSize(String sFileURL) throws Exception {
		//file:/mnt/OPTICAL/LEVEL-1C/2018/12/12/S2B_MSIL1C_20181212T010259_N0207_R045_T54PZA_20181212T021706.zip/.value

		m_oLogger.debug("ONDAProviderAdapter.GetDownloadSize: start " + sFileURL);

		long lLenght = 0L;

		if(sFileURL.startsWith("file:")) {

			String sPrefix = "file:";
			// Remove the prefix
			int iStart = sFileURL.indexOf(sPrefix) +sPrefix.length();
			String sPath = sFileURL.substring(iStart);

			m_oLogger.debug("ONDAProviderAdapter.GetDownloadSize: full path " + sPath);
			File oSourceFile = new File(sPath);
			lLenght = oSourceFile.length();
			if (!oSourceFile.exists()) {
				m_oLogger.debug("ONDAProviderAdapter.GetDownloadSize: FILE DOES NOT EXISTS");
			}
			m_oLogger.debug("ONDAProviderAdapter.GetDownloadSize: Found length " + lLenght);
		} else if(sFileURL.startsWith("https:")) {
			//lLenght = getSizeViaHttp(sFileURL);
			lLenght = getDownloadFileSizeViaHttp(sFileURL);
		}

		return lLenght;
	}



//	protected long getSizeViaHttp(String sFileURL) throws IOException {
//		long lLength = 0L;
//
//		// Domain check
//		if (Utils.isNullOrEmpty(sFileURL)) {
//			m_oLogger.debug("ONDAProviderAdapter.getSizeViaHttp: sFileURL is null");
//			return lLength;
//		}
//		String sUser = "";
//		String sPassword = "";
//		try {
//			sUser = ConfigReader.getPropValue("ONDA_USER");
//			sPassword = ConfigReader.getPropValue("ONDA_PASSWORD");
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		if (!Utils.isNullOrEmpty(m_sProviderUser)) sUser = m_sProviderUser;
//		if (!Utils.isNullOrEmpty(m_sProviderPassword)) sPassword = m_sProviderPassword;
//		final String sFinalUser = sUser;
//		final String sFinalPassword = sPassword;
//
//		// dhus authentication
//		Authenticator.setDefault(new Authenticator() {
//			protected PasswordAuthentication getPasswordAuthentication() {
//				try{
//					return new PasswordAuthentication(sFinalUser, sFinalPassword.toCharArray());
//				}
//				catch (Exception oEx){
//					m_oLogger.error("ONDAProviderAdapter.GetDownloadSize: exception setting auth " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
//				}
//				return null;
//			}
//		});
//
//		m_oLogger.debug("ONDAProviderAdapter.GetDownloadSize: FileUrl = " + sFileURL);
//
//		try {
//			URL oUrl = new URL(sFileURL);
//			HttpURLConnection oConnection = (HttpURLConnection) oUrl.openConnection();
//			oConnection.setRequestMethod("GET");
//			oConnection.setRequestProperty("Accept", "*/*");
//			oConnection.setConnectTimeout(100000);
//			oConnection.setReadTimeout(100000);
//			m_oLogger.debug("ONDAProviderAdapter.GetDownloadSize: Call get response");
//			int responseCode = oConnection.getResponseCode();
//			m_oLogger.debug("ONDAProviderAdapter.GetDownloadSize: Response got");
//
//			// always check HTTP response code first
//			if (responseCode == HttpURLConnection.HTTP_OK) {
//				lLength = oConnection.getHeaderFieldLong("Content-Length", 0L);
//				m_oLogger.debug("ONDAProviderAdapter.GetDownloadSize: File size = " + lLength);
//				return lLength;
//			} else {
//				m_oLogger.debug("ONDAProviderAdapter.GetDownloadSize: No file to download. Server replied HTTP code: " + responseCode);
//				m_iLastError = responseCode;
//			}
//			oConnection.disconnect();			
//		}
//		catch (Exception oEx) {
//			m_oLogger.debug("ONDAProviderAdapter.GetDownloadSize: Exception " + oEx.toString());
//		}
//
//
//		return lLength;
//	}

	/* (non-Javadoc)
	 * @see wasdi.filebuffer.DownloadFile#ExecuteDownloadFile(java.lang.String, java.lang.String, java.lang.String, java.lang.String, wasdi.shared.business.ProcessWorkspace)
	 */
	@Override
	public String ExecuteDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace) throws Exception {
		
		
		// Domain check
		if (Utils.isNullOrEmpty(sFileURL)) {
			m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: sFileURL is null");
			return "";
		}
		if (Utils.isNullOrEmpty(sSaveDirOnServer)) {
			m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: sSaveDirOnServer is null");
			return "";
		}
		
		m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: start");
		
		setProcessWorkspace(oProcessWorkspace);

		if(sFileURL.startsWith("file:")) {
			//  file:/mnt/OPTICAL/LEVEL-1C/2018/12/12/S2B_MSIL1C_20181212T010259_N0207_R045_T54PZA_20181212T021706.zip/.value		
			m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: this is a \"file:\" protocol, get file name");
			
			String sPrefix = "file:";
			// Remove the prefix
			int iStart = sFileURL.indexOf(sPrefix) +sPrefix.length();
			String sPath = sFileURL.substring(iStart);

			m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: source file: " + sPath);
			File oSourceFile = new File(sPath);
			
			// Destination file name: start from the simple name
			String sDestinationFileName = GetFileName(sFileURL);
			// set the destination folder
			if (sSaveDirOnServer.endsWith("/") == false) sSaveDirOnServer += "/";
			sDestinationFileName = sSaveDirOnServer + sDestinationFileName;
			
			m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: destination file: " + sDestinationFileName);

			// copy the product from file system
			try {
				File oDestionationFile = new File(sDestinationFileName);
				
				if (oDestionationFile.getParentFile() != null) { 
					if (oDestionationFile.getParentFile().exists() == false) {
						oDestionationFile.getParentFile().mkdirs();
					}
				}
				
				InputStream oInputStream = new FileInputStream(oSourceFile);
				OutputStream oOutputStream = new FileOutputStream(oDestionationFile);
				
				m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: start copy stream");
				
				//TODO change method signature and check result: if it fails try https or retry if timeout
				//MAYBE pass the entire pseudopath so that if one does not work, another one can be tried
				copyStream(oProcessWorkspace, oSourceFile.length(), oInputStream, oOutputStream);

			} catch (Exception e) {
				m_oLogger.debug( "ONDAProviderAdapter.Exception: " + e.toString());
			}
			//TODO else - i.e., if it fails - try get the file from https instead
			//	- in this case the sUrl must be modified in order to include http, so that it can be retrieved  
			return sDestinationFileName;
		} else if(sFileURL.startsWith("https://")) {
			//  https://catalogue.onda-dias.eu/dias-catalogue/Products(357ae76d-f1c4-4f25-b535-e278c3f937af)/$value
			String sResult = downloadViaHttp(sFileURL, sDownloadUser, sDownloadPassword, sSaveDirOnServer);
			//TODO else - i.e., if it fails - try get the file from the file system or retry if timeout
			//  - in this case the sUrl must be modified in order to include file id, so that it can be retrieved			
			return sResult;
			

		}
		return "";
	}

	/* (non-Javadoc)
	 * @see wasdi.filebuffer.DownloadFile#GetFileName(java.lang.String)
	 */
	@Override
	public String GetFileName(String sFileURL) throws Exception {
		//check whether the file has already been downloaded, else return null

		if (Utils.isNullOrEmpty(sFileURL)) {
			m_oLogger.debug("ONDAProviderAdapter.GetFileName: sFileURL is null or Empty");
			m_oLogger.fatal("ONDAProviderAdapter.GetFileName: sFileURL is null or Empty");
			return "";
		}

		if(sFileURL.startsWith("file:")) {
			
			// In Onda, the real file is .value but here we need the name of Satellite image that, in ONDA is the parent folder name
			String sPrefix = "file:";
			String sSuffix = "/.value";
			// Remove the prefix
			int iStart = sFileURL.indexOf(sPrefix) +sPrefix.length();
			String sPath = sFileURL.substring(iStart);

			// remove the ".value" suffix
			sPath = sPath.substring(0, sPath.lastIndexOf(sSuffix));

			// Destination file name: start from the simple name, i.e., exclude the containing dir, slash included:
			String sDestinationFileName = sPath.substring( sPath.lastIndexOf("/") + 1);
			return sDestinationFileName;

		} else if(sFileURL.startsWith("https://")) {
			return getFileNameViaHttp(sFileURL);
		} 
		return null;	
	}
	
}
