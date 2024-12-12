/**
 * Created by Cristiano Nattero on 2020-01-13
 * 
 * Fadeout software
 *
 */
package wasdi.dataproviders;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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
import java.util.zip.ZipOutputStream;

import org.apache.commons.net.io.Util;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.creodias.ResponseTranslatorCREODIAS;
import wasdi.shared.utils.MissionUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.utils.log.WasdiLog;

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
		m_sDataProviderCode = "CREODIAS";
	}

	/**
	 * Get the size of the file to download/copy
	 * @see wasdi.dataproviders.ProviderAdapter#GetDownloadFileSize(java.lang.String)
	 */
	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		//todo fail instead
		if (Utils.isNullOrEmpty(sFileURL)) {
			WasdiLog.errorLog("CREODIASProviderAdapter.GetDownloadFileSize: sFileURL is null or Empty");
			return 0l;
		}

		long lSizeInBytes = 0L;

		if (isFileProtocol(m_sDefaultProtocol)) {
			String sPath = null;
			if (isFileProtocol(sFileURL)) {
				sPath = removePrefixFile(sFileURL);
			} else if(isHttpsProtocol(sFileURL)) {
				sPath = extractFilePathFromHttpsUrl(sFileURL);
			} else {
				WasdiLog.debugLog("CREODIASProviderAdapter.getDownloadFileSize: unknown protocol " + sFileURL);
			}

			if (sPath != null) {
				File oSourceFile = new File(sPath);

				if (oSourceFile != null && oSourceFile.exists()) {
					lSizeInBytes = getSourceFileLength(oSourceFile);

					return lSizeInBytes;
				}
			}
		}

		if (isHttpsProtocol(sFileURL)) {
			String sResult = "";
			try {
				String sDownloadUser = m_sProviderUser;
				String sDownloadPassword = m_sProviderPassword;

				String sKeyCloakToken = obtainKeycloakToken(sDownloadUser, sDownloadPassword);
				//reconstruct appropriate url
				StringBuilder oUrl = new StringBuilder(getZipperUrl(sFileURL) );
				oUrl.append("?token=").append(sKeyCloakToken);
				sFileURL = oUrl.toString();

				lSizeInBytes = getDownloadFileSizeViaHttp(sFileURL);
				WasdiLog.debugLog("CREODIASProviderAdapter.getDownloadFileSize: file size is: " + sResult);
			} catch (Exception oE) {
				WasdiLog.debugLog("CREODIASProviderAdapter.getDownloadFileSize: could not extract file size due to " + oE);
			}
		}

		return lSizeInBytes;
	}

	/**
	 * Extract the file-system path of the file out of an HTTPS URL.
	 * @param sHttpsURL the HTTPS URL containing the product identifier (i.e. /eodata/Sentinel-1/SAR/GRD/2021/01/01/S1B_S1_GRDH_1SDH_20210101T152652_20210101T152706_024962_02F890_39B4.SAFE)
	 * @return the file-system path (i.e. C:/temp/wasdi//eodata/Sentinel-1/SAR/GRD/2021/01/01/S1B_S1_GRDH_1SDH_20210101T152652_20210101T152706_024962_02F890_39B4.SAFE)
	 */
	private String extractFilePathFromHttpsUrl(String sHttpsURL) {
		String sProductIdentifier = extractProductIdentifierFromURL(sHttpsURL);
		String filesystemPath = m_sProviderBasePath + sProductIdentifier;

		WasdiLog.debugLog("CREODIASProviderAdapter.extractFilePathFromHttpsUrl: HTTPS URL: " + sProductIdentifier);
		WasdiLog.debugLog("CREODIASProviderAdapter.extractFilePathFromHttpsUrl: file path: " + filesystemPath);

		return filesystemPath;
	}

	/* (non-Javadoc)
	 * @see wasdi.filebuffer.ProviderAdapter#ExecuteDownloadFile(java.lang.String, java.lang.String, java.lang.String, java.lang.String, wasdi.shared.business.ProcessWorkspace)
	 */
	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
		
		if(Utils.isNullOrEmpty(sFileURL)) {
			WasdiLog.errorLog("CREODIASProviderAdapter.ExecuteDownloadFile: URL is null or empty, aborting");
			return null;
		}

		String sResult = null;

		if (isFileProtocol(m_sDefaultProtocol)) {
			String filesystemPath = null;
			if (isFileProtocol(sFileURL)) {
				filesystemPath = removePrefixFile(sFileURL);
			} else if (isHttpsProtocol(sFileURL)) {
				filesystemPath = extractFilePathFromHttpsUrl(sFileURL);
			} else {
				WasdiLog.debugLog("CREODIASProviderAdapter.executeDownloadFile: unknown protocol " + sFileURL);
			}

			if (filesystemPath != null) {
				File oSourceFile = new File(filesystemPath);

				if (oSourceFile != null && oSourceFile.exists()) {
					sResult = copyFile("file:" + filesystemPath, sDownloadUser, sDownloadPassword, sSaveDirOnServer, oProcessWorkspace, iMaxRetry);

					return sResult;
				}
			}
		}

		if(isHttpsProtocol(sFileURL)) {
			try {
				//todo check availability
				boolean bShallWeOrderIt = productShallBeOrdered(sFileURL, sDownloadUser, sDownloadPassword);
				if(bShallWeOrderIt) {
					WasdiLog.infoLog("CREODIASProviderAdapter.ExecuteDownloadFile: requested file is not available, ordering it"); 
					JSONObject oJsonStatus = orderProduct(sFileURL, sDownloadUser, sDownloadPassword);
					if(null==oJsonStatus || !oJsonStatus.has("status") || oJsonStatus.isNull("status")) {
						throw new NullPointerException("Order failed: JSON status is null, aborting");
					}
					WasdiLog.infoLog("CREODIASProviderAdapter.executeDownloadFile: product ordered");
					boolean bLoop = true;
					String sStatus = oJsonStatus.getString("status");
					if(sStatus.equals("done")) {
						WasdiLog.infoLog("CREODIASProviderAdapter.executeDownloadFile: good news! The requested file is already available :-)");
						bLoop = false;
					}
					boolean bInit = true;
				
					int iMaxAttemps = 10;
					int iAttemps = 0;

					while(bLoop) {
					
						if (iAttemps > iMaxAttemps) {
							WasdiLog.infoLog("CREODIASProviderAdapter.ExecuteDownloadFile: made " + iAttemps + ", this is really too much");
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
							WasdiLog.errorLog("CREODIASProviderAdapter.ExecuteDownloadFile: order failed with status: done_with_error: " + oJsonStatus);
							break;
						case "cancelled":
							WasdiLog.errorLog("CREODIASProviderAdapter.ExecuteDownloadFile: order failed: " + oJsonStatus);
							return null;
						case "done":
							WasdiLog.infoLog("CREODIASProviderAdapter.ExecuteDownloadFile: order complete :-) proceeding to download");
							bLoop = false;
							break;
						default:
							WasdiLog.infoLog("CREODIASProviderAdapter.ExecuteDownloadFile: status is: " + sStatus);
							//todo replace this polling using a callback
							//todo set processor status to waiting
							if(bInit) {
								Long lFirstWait = 360l;
								WasdiLog.infoLog("CREODIASProviderAdapter.executeDownloadFile: waiting for order to complete, sleep for " + lFirstWait);
								TimeUnit.SECONDS.sleep(lFirstWait);
								bInit = false;
							} else {
								long lRandomWaitSeconds = new SecureRandom().longs(lLo, lUp).findFirst().getAsLong();
								//prepare to wait longer next time
								lLo = lRandomWaitSeconds;
								lUp += lWaitStep;
								WasdiLog.warnLog("CREODIASProviderAdapter.executeDownloadFile: download failed, trying again after a nap of " + lRandomWaitSeconds +" seconds...");
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

			} 
			catch (InterruptedException oEx) {
				Thread.currentThread().interrupt();
				WasdiLog.errorLog("CREODIASProviderAdapter.ExecuteDownloadFile: current thread was interrupted " , oEx);
			}
			catch (Exception oE) {
				WasdiLog.errorLog("CREODIASProviderAdapter.ExecuteDownloadFile: could not check order status due to: " + oE);
				return null;
			}


			//proceed as usual and download it
			long lWaitStep = 10l;
			long lUp = 10;
			long lLo = 0l;
			int iAttempt = 0;
			while(iAttempt < iMaxRetry) {
				try {
					WasdiLog.debugLog("CREODIASProviderAdapter.executeDownloadFile: attempt " + iAttempt + " at downloading from " + sFileURL ); 
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
						WasdiLog.warnLog("CREODIASProviderAdapter.executeDownloadFile: download failed, trying again after a nap of " + lRandomWaitSeconds +" seconds...");
						//todo set the processor as waiting
						TimeUnit.SECONDS.sleep(lRandomWaitSeconds);
					} else {
						//we're done
						WasdiLog.debugLog("CREODIASProviderAdapter.executeDownloadFile: download completed: " + sResult);
						break;
					}
				} 
				catch (InterruptedException oEx) {
					Thread.currentThread().interrupt();
					WasdiLog.errorLog("CREODIASProviderAdapter.executeDownloadFile. Current thread was interrupted");
				}
				catch (Exception oE) {
					WasdiLog.errorLog("CREODIASProviderAdapter.executeDownloadFile: ", oE);
				}
			}
		}

		return sResult;
	}
	
	private String copyFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {

		String sResult = "";
		// Domain check
		if (Utils.isNullOrEmpty(sFileURL)) {
			WasdiLog.debugLog("CREODIASProviderAdapter.ExecuteDownloadFile: sFileURL is null");
			return "";
		}
		if (Utils.isNullOrEmpty(sSaveDirOnServer)) {
			WasdiLog.debugLog("CREODIASProviderAdapter.ExecuteDownloadFile: sSaveDirOnServer is null");
			return "";
		}
		
		WasdiLog.debugLog("CREODIASProviderAdapter.ExecuteDownloadFile: start");
		
		setProcessWorkspace(oProcessWorkspace);

		if (isFileProtocol(sFileURL)) {
			if (isZipFile(sFileURL)) {
				sResult = localFileCopy(sFileURL, sSaveDirOnServer, iMaxRetry);
			} else if (isSafeDirectory(sFileURL)) {
				String sourceFile = removePrefixFile(sFileURL);
				String destinationFile = getFileName(sFileURL);

				// set the destination folder
				if (sSaveDirOnServer.endsWith("/") == false) sSaveDirOnServer += "/";
				destinationFile = sSaveDirOnServer + destinationFile;

				destinationFile = addZipExtension(removeSafeTermination(destinationFile));

				downloadZipFile(sourceFile, destinationFile);
				
				sResult = destinationFile;
			}
		} 

		return sResult;
	}

	private void downloadZipFile(String sourceFile, String destinationFile) throws Exception {
        FileOutputStream fos = new FileOutputStream(destinationFile);
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        File fileToZip = new File(sourceFile);

        ZipFileUtils.zipFile(fileToZip, fileToZip.getName(), zipOut);
        zipOut.close();
        fos.close();
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
			WasdiLog.errorLog("CREODIASProviderAdapter.checkStatus( " + oJsonOrder + ", ... ): " + oE );
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


		String[] sTokens = sFileURL.split(ResponseTranslatorCREODIAS.SLINK_SEPARATOR_CREODIAS);
		if(sTokens.length < 1) {
			WasdiLog.errorLog("CREODIASProviderAdapter.orderProduct( " + sFileURL + ", ... ): not enough tokens, aborting" ); 
			return null;
		}

		String sProductName = sTokens[ResponseTranslatorCREODIAS.IPOSITIONOF_FILENAME];
		if(sProductName.isEmpty()) {
			WasdiLog.errorLog("CREODIASProviderAdapter.orderProduct( " + sFileURL + ", ... ): product name is empty, aborting" );
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
			
			WasdiLog.infoLog("CREODIASProviderAdapter.orderProduct: payload - " + oJsonRequest.toString());

			return handleResponseStatus(oHttpConn);
		} catch (Exception oE) {
			WasdiLog.warnLog("CREODIASProviderAdapter.orderProduct( " + sFileURL + ", ... ): " + oE );
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
			WasdiLog.errorLog("CREODIASPRoviderAdapter.getRequiredProcessor: " + oE);
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
					WasdiLog.infoLog("CREODIASProviderAdapter.handleResponseStatus: order placed");
					return oJson;
				} catch (Exception oE) {
					WasdiLog.warnLog("CREODIASProviderAdapter.handleResponseStatus: " + oE + " while trying to retrieve response from CREODIAS server" );
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
					WasdiLog.warnLog("CREODIASProviderAdapter.handleResponseStatus: " + oE + " while trying to retrieve error from CREODIAS server" );
				}
			} 
			String sLog = "CREODIASProviderAdapter.handleResponseStatus: could not complete order, server returned status " + iResponseCode;
			if(!Utils.isNullOrEmpty(sError)) {
				sLog += " with message: " + sError;
			}
			WasdiLog.errorLog(sLog);
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
			WasdiLog.errorLog("CREODIASProviderAdapter.checkAvailability: could not check availability due to: " + oE + ", assuming product available, try to do download it anyway");
		}
		//try and download it anyway
		return false;
	}

	private String extractStatusFromURL(String sFileURL) {
		Preconditions.checkNotNull(sFileURL, "URL is null");
		try {
			String[] asParts = sFileURL.split(ResponseTranslatorCREODIAS.SLINK_SEPARATOR_CREODIAS);
			String sStatus = asParts[ResponseTranslatorCREODIAS.IPOSITIONOF_STATUS];
			return sStatus;
		} catch (Exception oE) {
			WasdiLog.errorLog("CREODIASProviderAdapter.extractStatusFromURL: " + oE);
		}
		return null;
	}

	private String extractProductIdentifierFromURL(String sFileURL) {
		Preconditions.checkNotNull(sFileURL, "URL is null");
		try {
			String[] asParts = sFileURL.split(ResponseTranslatorCREODIAS.SLINK_SEPARATOR_CREODIAS);
			String sProductIdentifier = asParts[ResponseTranslatorCREODIAS.IPOSITIONOF_PRODUCTIDENTIFIER];
			return sProductIdentifier;
		} catch (Exception oE) {
			WasdiLog.errorLog("CREODIASProviderAdapter.extractProductIdentifierFromURL: " + oE);
		}
		return null;
	}

	private String getZipperUrl(String sFileURL) {
		String sResult = "";
		try {
			sResult = sFileURL.split(ResponseTranslatorCREODIAS.SLINK_SEPARATOR_CREODIAS)[ResponseTranslatorCREODIAS.IPOSITIONOF_LINK];
		} catch (Exception oE) {
			WasdiLog.errorLog("CREODIASProviderAdapter.getZipperUrl: " + oE);
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
			WasdiLog.errorLog("CREODIASProviderAdapter.GetFileName: sFileURL is null or Empty");
			return "";
		}


		String sResult = "";
		if (isHttpsProtocol(sFileURL)) {
			try {
				String[] asTokens = sFileURL.split(ResponseTranslatorCREODIAS.SLINK_SEPARATOR_CREODIAS); 
				sResult = asTokens[ResponseTranslatorCREODIAS.IPOSITIONOF_FILENAME];
			} catch (Exception oE) {
				WasdiLog.errorLog("CREODIASProviderAdapter.GetFileName: " + oE);
			}
		} else if (isFileProtocol(sFileURL)) {
			String[] asParts = sFileURL.split("/");

			if (asParts != null && asParts.length > 1) {
				sResult = asParts[asParts.length-1];
			}
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
			WasdiLog.debugLog("CREODIASProviderAdapter.obtainKeycloakToken: Response status: " + iStatus);
			if( iStatus == 200) {
				InputStream oInputStream = oConnection.getInputStream();
				ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
				if(null!=oInputStream) {
					Util.copyStream(oInputStream, oBytearrayOutputStream);
					String sResult = oBytearrayOutputStream.toString();
					//WasdiLog.debugLog("CREODIASProviderAdapter.obtainKeycloakToken: json: " + sResult);
					JSONObject oJson = new JSONObject(sResult);
					String sToken = oJson.optString("access_token", null);
					return sToken;
				}
			} else {
				ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
				InputStream oErrorStream = oConnection.getErrorStream();
				Util.copyStream(oErrorStream, oBytearrayOutputStream);
				String sMessage = oBytearrayOutputStream.toString();
				WasdiLog.debugLog("CREODIASProviderAdapter.obtainKeycloakToken:" + sMessage);

			}
		} catch (Exception oE) {
			WasdiLog.debugLog("CREODIASProviderAdapter.obtainKeycloakToken: " + oE);
		}

		return null;
	}

	@Override
	protected void internalReadConfig() {
		
		try {
			m_sDefaultProtocol = m_oDataProviderConfig.defaultProtocol; 
			m_sProviderBasePath = m_oDataProviderConfig.localFilesBasePath;
		} catch (Exception e) {
			WasdiLog.errorLog("CREODIASProvierAdapter: Config reader is null");
		}
	}

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		
		boolean bOnCloud = isWorkspaceOnSameCloud();
		
		if (sPlatformType.equals(Platforms.SENTINEL1)) {
			String sType = MissionUtils.getProductTypeSatelliteImageFileName(sFileName);
			
			if (sType.equals("GRD") || sType.equals("OCN")) {
				if (bOnCloud) return DataProviderScores.FILE_ACCESS.getValue();
				else return DataProviderScores.DOWNLOAD.getValue();
			}
			
			// TODO: SLC in europe are available. Look if we can get the bbox
			return DataProviderScores.LTA.getValue();
		}
		else if (sPlatformType.equals(Platforms.SENTINEL2)) {
			String sType = MissionUtils.getProductTypeSatelliteImageFileName(sFileName);
			
			if (sType.equals("MSIL1C")) {
				if (bOnCloud) return DataProviderScores.FILE_ACCESS.getValue();
				else return DataProviderScores.DOWNLOAD.getValue();				
			}
			
			return DataProviderScores.LTA.getValue();
		}
		else if (sPlatformType.equals(Platforms.ENVISAT)) {
			if (sFileName.startsWith("ASA_")) {
				return -1;
			}
			if (bOnCloud) return DataProviderScores.FILE_ACCESS.getValue();
			else return DataProviderScores.DOWNLOAD.getValue();			
		}
		else if (sPlatformType.equals(Platforms.SENTINEL3) || sPlatformType.equals(Platforms.SENTINEL5P)
				|| sPlatformType.equals(Platforms.LANDSAT8)) {
			if (bOnCloud) return DataProviderScores.FILE_ACCESS.getValue();
			else return DataProviderScores.DOWNLOAD.getValue();
		}
		
		return 0;
	}
}
