/**
 * Created by Cristiano Nattero on 2020-01-13
 * 
 * Fadeout software
 *
 */
package wasdi.filebuffer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.net.io.Util;
import org.apache.log4j.Logger;

import wasdi.LoggerWrapper;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.opensearch.DiasResponseTranslatorCREODIAS;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.QueryResultViewModel;

/**
 * @author c.nattero
 *
 */
public class CREODIASProviderAdapter extends ProviderAdapter {

	/**
	 * 
	 */
	//TODO uncapitalize method
	public CREODIASProviderAdapter() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param logger
	 */
	public CREODIASProviderAdapter(LoggerWrapper logger) {
		super(logger);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see wasdi.filebuffer.ProviderAdapter#GetDownloadFileSize(java.lang.String)
	 */
	@Override
	public long GetDownloadFileSize(String sFileURL) throws Exception {
		//todo fail instead
		if (Utils.isNullOrEmpty(sFileURL)) {
			m_oLogger.error("CREODIASProviderAdapter.GetDownloadFileSize: sFileURL is null or Empty");
			return 0l;
		}
		
		long lSizeInBytes = 0;
		String sResult = "";
		try {
		sResult = sFileURL.split(DiasResponseTranslatorCREODIAS.SLINK_SEPARATOR_CREODIAS)[DiasResponseTranslatorCREODIAS.IPOSITIONOF_SIZEINBYTES];
		lSizeInBytes = Long.parseLong(sResult);
		} catch (Exception oE) {
			this.m_oLogger.error(oE.toString());
		}
		
		return lSizeInBytes;
	}

	/* (non-Javadoc)
	 * @see wasdi.filebuffer.ProviderAdapter#ExecuteDownloadFile(java.lang.String, java.lang.String, java.lang.String, java.lang.String, wasdi.shared.business.ProcessWorkspace)
	 */
	@Override
	public String ExecuteDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace) throws Exception {
		// TODO fail instead
		if(Utils.isNullOrEmpty(sFileURL)) {
			m_oLogger.error("CREODIASProviderAdapter.ExecuteDownloadFile: URL is null or empty");
			return null;
		}
		//todo check remaining parameters (can assume default values?)
		try {
			String sKeyCloakToken = obtainKeycloakToken(sDownloadUser, sDownloadPassword);
			//reconstruct appropriate url
			StringBuilder oUrl = new StringBuilder(getZipperUrl(sFileURL) );
			oUrl.append("?token=").append(sKeyCloakToken);
			return downloadViaHttp(oUrl.toString(), sDownloadUser, sDownloadPassword, sSaveDirOnServer);
		} catch (Exception oE) {
			m_oLogger.error(oE.toString());
		}
		return null;
	}

	private String getZipperUrl(String sFileURL) {
		String sResult = "";
		try {
		sResult = sFileURL.split(DiasResponseTranslatorCREODIAS.SLINK_SEPARATOR_CREODIAS)[DiasResponseTranslatorCREODIAS.IPOSITIONOF_LINK];
		} catch (Exception oE) {
			this.m_oLogger.error(oE.toString());
		}
		return sResult;
	}

	/* (non-Javadoc)
	 * @see wasdi.filebuffer.ProviderAdapter#GetFileName(java.lang.String)
	 */
	@Override
	public String GetFileName(String sFileURL) throws Exception {
		//todo fail instead
		if (Utils.isNullOrEmpty(sFileURL)) {
			m_oLogger.error("CREODIASProviderAdapter.GetFileName: sFileURL is null or Empty");
			return "";
		}
		
		
		String sResult = "";
		try {
		sResult = sFileURL.split(DiasResponseTranslatorCREODIAS.SLINK_SEPARATOR_CREODIAS)[DiasResponseTranslatorCREODIAS.IPOSITIONOF_FILENAME];
		} catch (Exception oE) {
			this.m_oLogger.error(oE.toString());
		}
		
		return sResult;
	}
	
	private String obtainKeycloakToken(String sDownloadUser, String sDownloadPassword) {
		try {
			URL oURL = new URL("https://auth.creodias.eu/auth/realms/dias/protocol/openid-connect/token");

			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();
			oConnection.setRequestMethod("POST");
			oConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			oConnection.setDoOutput(true);
			oConnection.getOutputStream().write(("client_id=CLOUDFERRO_PUBLIC&password=" + sDownloadPassword + "&username=" + sDownloadUser + "&grant_type=password").getBytes());
			int iStatus = oConnection.getResponseCode();
			System.out.println("Response status: " + iStatus);
			if( iStatus == 200) {
				InputStream oInputStream = oConnection.getInputStream();
				ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
				if(null!=oInputStream) {
					Util.copyStream(oInputStream, oBytearrayOutputStream);
					String sResult = oBytearrayOutputStream.toString();
					System.out.println(sResult);
					return sResult;
				}
			} else {
				ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
				InputStream oErrorStream = oConnection.getErrorStream();
				Util.copyStream(oErrorStream, oBytearrayOutputStream);
				String sMessage = oBytearrayOutputStream.toString();
				System.out.println(sMessage);

			}


		} catch (Exception oE) {
			// TODO Auto-generated catch block
			oE.printStackTrace();
		}

		return null;
	}

}
