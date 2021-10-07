/**
 * Created by Cristiano Nattero on 2020-01-13
 * 
 * Fadeout software
 *
 */
package wasdi.filebuffer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.net.io.Util;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;

import wasdi.ConfigReader;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.opensearch.creodias.DiasResponseTranslatorCREODIAS;
import wasdi.shared.utils.LoggerWrapper;
import wasdi.shared.utils.Utils;

/**
 * @author c.nattero
 *
 */
public class CREODIASProviderAdapter extends ProviderAdapter {
	
	private String m_sOrderName = "";
	
	/**
	 * Base path of the folder mounted with EO Data
	 */
	private String m_sProviderBasePath = "";

	/**
	 * Basic constructor
	 */
	public CREODIASProviderAdapter() {
	}

	/**
	 * @param logger
	 */
	public CREODIASProviderAdapter(LoggerWrapper logger) {
		super(logger);
	}

	/**
	 * Get the size of the file to download/copy
	 * @see wasdi.filebuffer.ProviderAdapter#GetDownloadFileSize(java.lang.String)
	 */
	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		//todo fail instead
		if (Utils.isNullOrEmpty(sFileURL)) {
			m_oLogger.error("CREODIASProviderAdapter.GetDownloadFileSize: sFileURL is null or Empty");
			return 0l;
		}

		long lSizeInBytes = 0L;

		if (isFileProtocol(sFileURL)) {
			String sPath = removePrefixFile(sFileURL);
			File oSourceFile = new File(sPath);

			lSizeInBytes = getSourceFileLength(oSourceFile);
		} else if(isHttpsProtocol(sFileURL)) {
			String sResult = "";
			try {
				sResult = sFileURL.split(DiasResponseTranslatorCREODIAS.SLINK_SEPARATOR_CREODIAS)[DiasResponseTranslatorCREODIAS.IPOSITIONOF_SIZEINBYTES];
				lSizeInBytes = Long.parseLong(sResult);
				m_oLogger.debug("CREODIASProviderAdapter.getDownloadFileSize: file size is: " + sResult);
			} catch (Exception oE) {
				m_oLogger.debug("CREODIASProviderAdapter.getDownloadFileSize: could not extract file size due to " + oE);
			}
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

		String sResult = null;

		if (isFileProtocol(sFileURL)) {
			// implement the local-copy of the file
		} else if(isHttpsProtocol(sFileURL)) {
			try {
				//todo check availability
				boolean bShallWeOrderIt = productShallBeOrdered(sFileURL, sDownloadUser, sDownloadPassword);
				if(bShallWeOrderIt) {
					m_oLogger.info("CREODIASProviderAdapter.ExecuteDownloadFile: requested file is not available, ordering it"); 
					JSONObject oJsonStatus = orderProduct(sFileURL, sDownloadUser, sDownloadPassword);
					if(null==oJsonStatus || !oJsonStatus.has("status") || oJsonStatus.isNull("status")) {
						throw new NullPointerException("Order failed: JSON status is null, aborting");
					}
					m_oLogger.info("CREODIASProviderAdapter.executeDownloadFile: product ordered");
					boolean bLoop = true;
					String sStatus = oJsonStatus.getString("status");
					if(sStatus.equals("done")) {
						m_oLogger.info("CREODIASProviderAdapter.executeDownloadFile: good news! The requested file is already available :-)");
						bLoop = false;
					}
					boolean bInit = true;
				
					int iMaxAttemps = 10;
					int iAttemps = 0;

					while(bLoop) {
					
						if (iAttemps > iMaxAttemps) {
							m_oLogger.info("CREODIASProviderAdapter.ExecuteDownloadFile: made " + iAttemps + ", this is really too much");
							break;
						}
						//todo tune waiting times
						long lWaitStep = 60l;
						long lUp = 300l;
						long lLo = 60l;
						//get, not opt: we want it to throw an exception if it is not present
						sStatus = oJsonStatus.getString("status");
						switch(sStatus) {
						case "done_with_error":
							//todo shall we download it anyway?
							m_oLogger.error("CREODIASProviderAdapter.ExecuteDownloadFile: order failed with status: done_with_error: " + oJsonStatus);
							break;
						case "cancelled":
							m_oLogger.error("CREODIASProviderAdapter.ExecuteDownloadFile: order failed: " + oJsonStatus);
							return null;
						case "done":
							m_oLogger.info("CREODIASProviderAdapter.ExecuteDownloadFile: order complete :-) proceeding to download");
							bLoop = false;
							break;
						default:
							m_oLogger.info("CREODIASProviderAdapter.ExecuteDownloadFile: status is: " + sStatus);
							//todo replace this polling using a callback
							//todo set processor status to waiting
							if(bInit) {
								Long lFirstWait = 360l;
								m_oLogger.info("CREODIASProviderAdapter.executeDownloadFile: waiting for order to complete, sleep for " + lFirstWait);
								TimeUnit.SECONDS.sleep(lFirstWait);
								bInit = false;
							} else {
								long lRandomWaitSeconds = new SecureRandom().longs(lLo, lUp).findFirst().getAsLong();
								//prepare to wait longer next time
								lLo = lRandomWaitSeconds;
								lUp += lWaitStep;
								m_oLogger.warn("CREODIASProviderAdapter.executeDownloadFile: download failed, trying again after a nap of " + lRandomWaitSeconds +" seconds...");
								TimeUnit.SECONDS.sleep(lRandomWaitSeconds);
							}
							oJsonStatus = checkStatus(oJsonStatus, sDownloadUser, sDownloadPassword);
							if(null==oJsonStatus || !oJsonStatus.has("status") || oJsonStatus.isNull("status")) {
								throw new NullPointerException("JSON status is null, aborting");
							}
						}
					
						iAttemps ++;
					}
				}

			} catch (Exception oE) {
				m_oLogger.error("CREODIASProviderAdapter.ExecuteDownloadFile: could not check order status due to: " + oE);
				return null;
			}


			//proceed as usual and download it
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
						long lRandomWaitSeconds = new SecureRandom().longs(lLo, lUp).findFirst().getAsLong();
						//prepare to wait longer next time
						lLo = lRandomWaitSeconds;
						lUp += lWaitStep;
						m_oLogger.warn("CREODIASProviderAdapter.executeDownloadFile: download failed, trying again after a nap of " + lRandomWaitSeconds +" seconds...");
						//todo set the processor as waiting
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
		}

		return sResult;
	}

	private JSONObject checkStatus(JSONObject oJsonOrder, String sDownloadUser, String sDownloadPassword) {
		Preconditions.checkNotNull(oJsonOrder, "url is null");
		Preconditions.checkNotNull(sDownloadUser, "user is null");
		Preconditions.checkArgument(!sDownloadUser.isEmpty(), "user is empty");
		Preconditions.checkNotNull(sDownloadPassword, "password is null");
		Preconditions.checkArgument(!sDownloadPassword.isEmpty(), "password is empty");

		try {
			String sCheckUrl =  "https://finder.creodias.eu/api/order/";
			//don't check, it's in a try/catch
			int sId = oJsonOrder.getInt("id");
			sCheckUrl += sId + "/";

			URL oUrl = new URL(sCheckUrl);

			HttpURLConnection oHttpConn = (HttpURLConnection) oUrl.openConnection();
			oHttpConn.setRequestMethod("GET");
			oHttpConn.addRequestProperty("Content-Type","application/json");
			oHttpConn.addRequestProperty("Keycloak-Token", obtainKeycloakToken(sDownloadUser, sDownloadPassword));

			return handleResponseStatus(oHttpConn);


		} catch (Exception oE) {
			m_oLogger.error("CREODIASProviderAdapter.checkStatus( " + oJsonOrder + ", ... ): " + oE );
		}
		return null;
	}


	private JSONObject orderProduct(String sFileURL, String sDownloadUser, String sDownloadPassword) {
		Preconditions.checkNotNull(sFileURL, "url is null");
		Preconditions.checkArgument(!sFileURL.isEmpty(), "url is empty");
		Preconditions.checkNotNull(sDownloadUser, "user is null");
		Preconditions.checkArgument(!sDownloadUser.isEmpty(), "user is empty");
		Preconditions.checkNotNull(sDownloadPassword, "password is null");
		Preconditions.checkArgument(!sDownloadPassword.isEmpty(), "password is empty");


		String[] sTokens = sFileURL.split(DiasResponseTranslatorCREODIAS.SLINK_SEPARATOR_CREODIAS);
		if(sTokens.length < 1) {
			m_oLogger.error("CREODIASProviderAdapter.orderProduct( " + sFileURL + ", ... ): not enough tokens, aborting" ); 
			return null;
		}

		String sProductName = sTokens[DiasResponseTranslatorCREODIAS.IPOSITIONOF_FILENAME];
		if(sProductName.isEmpty()) {
			m_oLogger.error("CREODIASProviderAdapter.orderProduct( " + sFileURL + ", ... ): product name is empty, aborting" );
			return null;
		}

		String sOrderUrl = "https://finder.creodias.eu/api/order/";

		try {
			
			JSONObject oJsonRequest = new JSONObject();


			//https://finder.creodias.eu/api/doc/#operation/order_create
			/*
	        REQUEST BODY SCHEMA: application/json

			order_name: string (Order name) [ 1 .. 255 ] characters
			priority: integer (Priority) [ 0 .. 2147483647 ] Nullable
			destination: string (Destination) <= 255 characters: DIAS Collection or user bucket
			callback: string (Callback) <= 255 characters: Callback URL ie. http://yourdomain.com/callback_handler
			identifier_list: Array of strings
			processor (*required): string (Processor) non-empty
			resto_query: string (Resto query) non-empty
			extra: object (Extra)
			 */

			//order_name
			//255 is the maximum length
			m_sOrderName = Utils.getCappedRandomName(255);
			oJsonRequest.put("order_name", m_sOrderName);

			//priority
			//todo set priority to the highest possible value, according to the application. Is it 0 or 1?
			int iPriority = 1;
			oJsonRequest.put("priority", iPriority);

			//maybe destination

			//maybe use a callback

			//identifier_list
			List<String> asProducts = new ArrayList<String>();
			asProducts.add(sProductName);
			JSONArray oJsonProductsToOrder = new JSONArray(asProducts);
			oJsonRequest.put("identifier_list", oJsonProductsToOrder);

			//processor
			oJsonRequest.put("processor", getRequiredProcessor(sFileURL));			
			
			URL oUrl = new URL(sOrderUrl);

			HttpURLConnection oHttpConn = (HttpURLConnection) oUrl.openConnection();
			oHttpConn.setDoOutput(true);
			oHttpConn.setRequestMethod("POST");
			oHttpConn.addRequestProperty("Content-Type","application/json");
			//oHttpConn.setRequestProperty("Accept", "*/*");
			//oHttpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:57.0) Gecko/20100101 Firefox/57.0");

			oHttpConn.addRequestProperty("Keycloak-Token", obtainKeycloakToken(sDownloadUser, sDownloadPassword));

			
			//oHttpConn.connect();
			OutputStream oOutputStream = oHttpConn.getOutputStream();
			oOutputStream.write(oJsonRequest.toString().getBytes());
			
			m_oLogger.info("CREODIASProviderAdapter.orderProduct: payload - " + oJsonRequest.toString());

			return handleResponseStatus(oHttpConn);
		} catch (Exception oE) {
			m_oLogger.warn("CREODIASProviderAdapter.orderProduct( " + sFileURL + ", ... ): " + oE );
		}
		return null;
	}

	private String getRequiredProcessor(String sFileURL) {
		//todo identify necessary processor according to the type of image required
		try {
			String sProduct = getFileName(sFileURL);
			if(sProduct.startsWith("S1")) {
				if (sProduct.contains("_SLC_") || sProduct.contains("_RAW_")) {
					return "download";
				}
				else {
					return "";
				}
			}
		} catch (Exception oE) {
			m_oLogger.error("CREODIASPRoviderAdapter.getRequiredProcessor: " + oE);
		}
		return "sen2cor";
	}

	/**
	 * @param oHttpConn
	 * @return
	 * @throws IOException
	 */
	private JSONObject handleResponseStatus(HttpURLConnection oHttpConn) throws IOException {
		int iResponseCode = oHttpConn.getResponseCode();

		if (iResponseCode == HttpURLConnection.HTTP_CREATED ||
				//actually it's just 201 that we expect, but just in case...
				iResponseCode == HttpURLConnection.HTTP_OK) {
			//good to go
			String sResponse = "";
			InputStream oInputStream = oHttpConn.getInputStream();
			if(null!=oInputStream) {
				try (Reader oReader = new InputStreamReader(oInputStream)){
					sResponse = CharStreams.toString(oReader);
					int iStatusPosition = sResponse.indexOf("status");
					int iStart = sResponse.lastIndexOf('{', iStatusPosition);
					int iEnd = Math.min(sResponse.indexOf('}', iStart)+1, sResponse.length());
					sResponse = sResponse.substring(iStart, iEnd);
					sResponse = sResponse.trim();
					if(!sResponse.endsWith("}")) {
						sResponse += "}";
					}
					JSONObject oJson = new JSONObject(sResponse);
					m_oLogger.info("CREODIASProviderAdapter.handleResponseStatus: order placed");
					return oJson;
				} catch (Exception oE) {
					m_oLogger.warn("CREODIASProviderAdapter.handleResponseStatus: " + oE + " while trying to retrieve response from CREODIAS server" );
				}
			}
		} else {
			//not good
			String sError = "";
			InputStream oInputStream = oHttpConn.getErrorStream();
			if(null!=oInputStream) {
				try (Reader oReader = new InputStreamReader(oInputStream)){
					sError = CharStreams.toString(oReader);
					//todo check: is it a json object? If yes, parse and return it
				} catch (Exception oE) {
					m_oLogger.warn("CREODIASProviderAdapter.handleResponseStatus: " + oE + " while trying to retrieve error from CREODIAS server" );
				}
			} 
			String sLog = "CREODIASProviderAdapter.handleResponseStatus: could not complete order, server returned status " + iResponseCode;
			if(!Utils.isNullOrEmpty(sError)) {
				sLog += " with message: " + sError;
			}
			m_oLogger.error(sLog);
		}
		return null;
	}

	private boolean productShallBeOrdered(String sFileURL, String sDownloadUser, String sDownloadPassword) {
		Preconditions.checkNotNull(sFileURL, "URL is null");
		Preconditions.checkNotNull(sDownloadUser, "User is null");
		Preconditions.checkNotNull(sDownloadPassword, "Password is null");
		try {
			String sStatus = extractStatusFromURL(sFileURL);
			
			if (sStatus == null) sStatus = "";
			
			switch(sStatus.toLowerCase()) {
			//order if...
			//	31 means that product is orderable and waiting for download to our cache,
			case "31":
				//37 means that product is processed by our platform,
			case "37":
				return true;

				//do not order if...
				//34 means that product is downloaded in cache,
			case "34":
				//0 means that already processed product is waiting in our platform
			case "0":
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
					//m_oLogger.debug("CREODIASProviderAdapter.obtainKeycloakToken: json: " + sResult);
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

	@Override
	public void readConfig() {
		
		try {
			m_sDefaultProtocol = ConfigReader.getPropValue("CREODIAS_DEFAULT_PROTOCOL", "https://");
		} catch (IOException e) {
			m_oLogger.error("CREODIASProvierAdapter: Config reader is null");
		}
		
		try {
			m_sProviderBasePath = ConfigReader.getPropValue("CREODIAS_BASE_PATH", "/eodata/");
		} catch (IOException e) {
			m_oLogger.error("CREODIASProvierAdapter: Config reader is null");
		}
		
	}
}
