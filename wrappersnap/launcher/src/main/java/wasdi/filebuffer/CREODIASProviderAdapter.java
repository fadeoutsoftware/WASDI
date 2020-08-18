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
import java.util.concurrent.TimeUnit;
import java.util.Random;

import org.apache.commons.net.io.Util;
import org.json.JSONObject;

import com.google.common.base.Preconditions;

import wasdi.LoggerWrapper;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.opensearch.creodias.DiasResponseTranslatorCREODIAS;
import wasdi.shared.utils.Utils;

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
	public long getDownloadFileSize(String sFileURL) throws Exception {
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
			m_oLogger.debug("CREODIASProviderAdapter.getDownloadFileSize: file size is: " + sResult);
		} catch (Exception oE) {
			m_oLogger.debug("CREODIASProviderAdapter.getDownloadFileSize: could not extract file size due to " + oE);
		}

		return lSizeInBytes;
	}

	/* (non-Javadoc)
	 * @see wasdi.filebuffer.ProviderAdapter#ExecuteDownloadFile(java.lang.String, java.lang.String, java.lang.String, java.lang.String, wasdi.shared.business.ProcessWorkspace)
	 */
	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
		// TODO fail instead
		if(Utils.isNullOrEmpty(sFileURL)) {
			m_oLogger.error("CREODIASProviderAdapter.ExecuteDownloadFile: URL is null or empty, aborting");
			return null;
		}
		//todo check availability
		boolean bShallWeOrderIt = productShallBeOrdered(sFileURL, sDownloadUser, sDownloadPassword);
		if(bShallWeOrderIt) {
			m_oLogger.warn("CREODIASProviderAdapter.ExecuteDownloadFile: ordering products not implemented yet"); 
			// order
			//see https://creodias.eu/faq-all/-/asset_publisher/SIs09LQL6Gct/content/how-to-order-products-using-finder-api-?inheritRedirect=true
			  // do
	  		    // go to sleep for the expected period (if specified)
			    // check again
			  //while not available
		}

		  
		//proceed as usual and download it
		String sResult = null;
		long lWaitStep = 10l;
		long lUp = 10;
		long lLo = 0l;
		int iAttempt = 0;
		while(iAttempt < iMaxRetry) {
			try {
				m_oLogger.debug("CREODIASProviderAdapter.executeDownloadFile: attempt " + iAttempt + " at downloading from " + sFileURL ); 
				//todo check product availability
				String sKeyCloakToken = obtainKeycloakToken(sDownloadUser, sDownloadPassword);
				//reconstruct appropriate url
				StringBuilder oUrl = new StringBuilder(getZipperUrl(sFileURL) );
				oUrl.append("?token=").append(sKeyCloakToken);
				sResult = downloadViaHttp(oUrl.toString(), sDownloadUser, sDownloadPassword, sSaveDirOnServer);
				if(Utils.isNullOrEmpty(sResult)) {
					//try again
					++iAttempt;
					long lRandomWaitSeconds = new Random().longs(lLo, lUp).findFirst().getAsLong();
					//prepare to wait longer next time
					lLo = lRandomWaitSeconds;
					lUp += lWaitStep;
					m_oLogger.warn("CREODIASProviderAdapter.executeDownloadFile: download failed, trying again after a nap of " + lRandomWaitSeconds +" seconds...");
					TimeUnit.SECONDS.sleep(lRandomWaitSeconds);
				} else {
					//we're done
					m_oLogger.debug("CREODIASProviderAdapter.executeDownloadFile: download completed: " + sResult);
					break;
				}
			} catch (Exception oE) {
				m_oLogger.error("CREODIASProviderAdapter.executeDownloadFile: " + oE);
			}
		}
		return sResult;
	}

	private boolean productShallBeOrdered(String sFileURL, String sDownloadUser, String sDownloadPassword) {
		Preconditions.checkNotNull(sFileURL, "URL is null");
		Preconditions.checkNotNull(sDownloadUser, "User is null");
		Preconditions.checkNotNull(sDownloadPassword, "Password is null");
		try {
			String sStatus = extractStatusFromURL(sFileURL);
			switch(sStatus.toLowerCase()) {
				//order if...
			    //31 means that product is orderable and waiting for download to our cache,
				case "31":
					return true;
				//37 means that product is processed by our platform,
				case "37":
					return true;
				//do not order if...
				//34 means that product is downloaded in cache,
				case "34":
					return false;
				//0 means that already processed product is waiting in our platform
				case "0":
					return false;
				default:
					return false;
			}
		} catch (Exception oE) {
			m_oLogger.error("CREODIASProviderAdapter.checkAvailability: could not check availability due to: " + oE + ", assuming product available, try to do download it anyway");
		}
		//try and download it anyway
		return false;
	}

	private String extractStatusFromURL(String sFileURL) {
		Preconditions.checkNotNull(sFileURL, "URL is null");
		try {
			String[] asParts = sFileURL.split(DiasResponseTranslatorCREODIAS.SLINK_SEPARATOR_CREODIAS);
			String sStatus = asParts[DiasResponseTranslatorCREODIAS.IPOSITIONOF_STATUS];
			return sStatus;
		} catch (Exception oE) {
			m_oLogger.error("CREODIASProviderAdapter.extractStatusFromURL: " + oE);
		}
		return null;
	}

	private String getZipperUrl(String sFileURL) {
		String sResult = "";
		try {
			sResult = sFileURL.split(DiasResponseTranslatorCREODIAS.SLINK_SEPARATOR_CREODIAS)[DiasResponseTranslatorCREODIAS.IPOSITIONOF_LINK];
		} catch (Exception oE) {
			m_oLogger.error("CREODIASProviderAdapter.getZipperUrl: " + oE);
		}
		return sResult;
	}

	/* (non-Javadoc)
	 * @see wasdi.filebuffer.ProviderAdapter#GetFileName(java.lang.String)
	 */
	@Override
	public String getFileName(String sFileURL) throws Exception {
		//todo fail instead
		if (Utils.isNullOrEmpty(sFileURL)) {
			m_oLogger.error("CREODIASProviderAdapter.GetFileName: sFileURL is null or Empty");
			return "";
		}


		String sResult = "";
		try {
			String[] asTokens = sFileURL.split(DiasResponseTranslatorCREODIAS.SLINK_SEPARATOR_CREODIAS); 
			sResult = asTokens[DiasResponseTranslatorCREODIAS.IPOSITIONOF_FILENAME];
		} catch (Exception oE) {
			m_oLogger.error("CREODIASProviderAdapter.GetFileName: " + oE);
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
			m_oLogger.debug("CREODIASProviderAdapter.obtainKeycloakToken: Response status: " + iStatus);
			if( iStatus == 200) {
				InputStream oInputStream = oConnection.getInputStream();
				ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
				if(null!=oInputStream) {
					Util.copyStream(oInputStream, oBytearrayOutputStream);
					String sResult = oBytearrayOutputStream.toString();
					m_oLogger.debug("CREODIASProviderAdapter.obtainKeycloakToken: json: " + sResult);
					JSONObject oJson = new JSONObject(sResult);
					String sToken = oJson.optString("access_token", null);
					return sToken;
				}
			} else {
				ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
				InputStream oErrorStream = oConnection.getErrorStream();
				Util.copyStream(oErrorStream, oBytearrayOutputStream);
				String sMessage = oBytearrayOutputStream.toString();
				m_oLogger.debug("CREODIASProviderAdapter.obtainKeycloakToken:" + sMessage);

			}
		} catch (Exception oE) {
			m_oLogger.debug("CREODIASProviderAdapter.obtainKeycloakToken: " + oE);
		}

		return null;
	}

}
