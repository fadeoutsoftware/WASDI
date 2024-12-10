package wasdi.shared.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.zip.ZipOutputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.io.CopyStreamException;
import org.apache.commons.net.io.Util;
import org.json.JSONObject;

import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;

/**
 * Utility class for HTTP operations.
 * 
 * @author PetruPetrescu
 *
 */
public final class HttpUtils {
	
	/**
	 * Private constructor
	 */
	private HttpUtils() {
	}
	
	/**
	 * Http Get Call 
	 * 
	 * @param sUrl Url to call
	 * @return HttpCallResponse object that contains the Response Body and the Response Code 
	 */
	public static HttpCallResponse httpGet(String sUrl) {
		return httpGet(sUrl, null);
	}
	
	/**
	 * Http Get Call
	 * 
	 * @param sUrl Url to call
	 * @param asHeaders Map of headers to add to the http call
	 * @return  HttpCallResponse object that contains the Response Body and the Response Code
	 */
	public static HttpCallResponse httpGet(String sUrl, Map<String, String> asHeaders) {
		return httpGet(sUrl, asHeaders, null);
	}

	/**
	 * Http Get Call
	 * 
	 * @param sUrl Url to call
	 * @param asHeaders Map of headers to add to the http call
	 * @param aoOutputHeaders Map of response headers 
	 * @return  HttpCallResponse object that contains the Response Body and the Response Code
	 */
	public static HttpCallResponse httpGet(String sUrl, Map<String, String> asHeaders, Map<String, List<String>> aoOutputHeaders) {
		// Create the Return View Model: we return both http code and body received
		HttpCallResponse oHttpCallResponse = new HttpCallResponse();

		String sResult = null;

		try {
			
			if (sUrl.startsWith("unix:///")) {
				return SocketUtils.httpGet(sUrl, asHeaders, aoOutputHeaders);
			}
			
			// Create the Url and relative Connection
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();
			
			// Optional: default is GET
			oConnection.setRequestMethod("GET");
			// We accept all
			oConnection.setRequestProperty("Accept", "*/*");
			
			// Set Read Timeout
			oConnection.setReadTimeout(WasdiConfig.Current.readTimeout);
			// Set Connection Timeout
			oConnection.setConnectTimeout(WasdiConfig.Current.connectionTimeout);
			
			// Do we have input headers?
			if (asHeaders != null) {
				// Yes: add all to our request
				for (Entry<String, String> asEntry : asHeaders.entrySet()) {
					oConnection.setRequestProperty(asEntry.getKey(), asEntry.getValue());
				}
			}
			
			boolean bLog = true;
			
			if (WasdiConfig.Current.filterInternalHttpCalls) {
				if (sUrl.contains(WasdiConfig.Current.keycloack.introspectAddress)) {
					bLog = false;
				}
				if (sUrl.contains(WasdiConfig.Current.dockers.internalDockerAPIAddress)) {
					bLog = false;
				}				
			}
			
			if (WasdiConfig.Current.logHttpCalls && bLog) {
				WasdiLog.debugLog("HttpUtils.httpGet: Sending 'GET' request to URL : " + sUrl);
			}

			try {
				// Read server response code
				int iResponseCode = oConnection.getResponseCode();
				
				if (WasdiConfig.Current.logHttpCalls && bLog) {
					WasdiLog.debugLog("HttpUtils.httpGet: Response Code : " + iResponseCode);
				}
				
				// Save it in our Return Object
				oHttpCallResponse.setResponseCode(Integer.valueOf(iResponseCode));
				
				// Do we need to report also output headers?
				if (aoOutputHeaders!=null) {
					try {
						
						// Get the Response headers
						Map<String, List<String>> aoReceivedHeaders = oConnection.getHeaderFields();
						
						// Copy in the ouput dictionary
						for (Map.Entry<String, List<String>> oEntry : aoReceivedHeaders.entrySet()) {
							aoOutputHeaders.put(oEntry.getKey(), oEntry.getValue());
						}
					}
					catch (Exception oEx) {
						WasdiLog.errorLog("HttpUtils.httpGet: Exception getting the output headers ", oEx);
					}
				}
				
				// Check for a valid response
				if (iResponseCode >= 200 && iResponseCode <= 299) {
					
					InputStream oInputStream = oConnection.getInputStream();
					
					ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
					
					// Copy the response body in our return object
					if (oInputStream != null) {
						Util.copyStream(oInputStream, oBytearrayOutputStream);
						sResult = oBytearrayOutputStream.toString();
						oHttpCallResponse.setResponseBody(sResult);
						oHttpCallResponse.setResponseBytes(oBytearrayOutputStream.toByteArray());
					}
				} 
				else {
					
					// Not valid response
					WasdiLog.debugLog("HttpUtils.httpGet: provider did not return 200 but " + iResponseCode + " (1/2) and the following message:\n" + oConnection.getResponseMessage());

					ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
					InputStream oErrorStream = oConnection.getErrorStream();
					Util.copyStream(oErrorStream, oBytearrayOutputStream);

					sResult = oBytearrayOutputStream.toString();
					oHttpCallResponse.setResponseBody(sResult);
				}
			} catch (Exception oEint) {
				WasdiLog.debugLog("HttpUtils.httpGet: Exception " + oEint);
			} finally {
				oConnection.disconnect();
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("HttpUtils.httpGet: Exception " + oE);
		}

		return oHttpCallResponse;
	}
	
	/**
	 * Standard http post utility function
	 * @param sUrl url to call
	 * @param sPayload payload of the post 
	 * @return HttpCallResponse object that contains the Response Body and the Response Code
	 */
	public static HttpCallResponse httpPost(String sUrl, String sPayload) {
		return httpPost(sUrl, sPayload, null, null);
	}
	
	/**
	 * Standard http post utility function
	 * @param sUrl url to call
	 * @param sPayload payload of the post 
	 * @param asHeaders headers dictionary
	 * @return HttpCallResponse object that contains the Response Body and the Response Code
	 */
	public static HttpCallResponse httpPost(String sUrl, String sPayload, Map<String, String> asHeaders) {
		return httpPost(sUrl, sPayload, asHeaders, null);
	}
	
	/**
	 * Standard http post utility function
	 * @param sUrl url to call
	 * @param sPayload payload of the post
	 * @param asHeaders headers dictionary
	 * @param sAuth in the form user:password (i.e., separated by a column: ':')
	 * @return object that contains the Response Body and the Response Code
	 */
	public static HttpCallResponse httpPost(String sUrl, String sPayload, Map<String, String> asHeaders, String sAuth) {
		return httpPost(sUrl, sPayload, asHeaders, sAuth, null);
	}
	
	/**
	 * Standard http post utility function
	 * @param sUrl url to call
	 * @param sPayload payload of the post
	 * @param asHeaders headers dictionary
	 * @param sAuth in the form user:password (i.e., separated by a column: ':')
	 * @return HttpCallResponse object that contains the Response Body and the Response Code
	 */
	public static HttpCallResponse httpPost(String sUrl, String sPayload, Map<String, String> asHeaders, String sAuth, Map<String, List<String>> aoOutputHeaders) {
		return httpPost(sUrl, sPayload.getBytes(), asHeaders, sAuth, aoOutputHeaders);
	}
	
	public static HttpCallResponse httpPost(String sUrl, byte []ayBytes, Map<String, String> asHeaders) {
		return httpPost(sUrl, ayBytes, asHeaders, "", null);
	}
	
	public static HttpCallResponse httpPost(String sUrl, File oFile, Map<String, String> asHeaders) {
		HttpCallResponse oHttpCallResponse = new HttpCallResponse();

		String sResult = null;
		try {
			
			if (sUrl.startsWith("unix:///")) {
				return SocketUtils.httpPost(sUrl, oFile, asHeaders);
			}
			
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();
			
			// Set Read Timeout
			oConnection.setReadTimeout(WasdiConfig.Current.readTimeout);
			// Set Connection Timeout
			oConnection.setConnectTimeout(WasdiConfig.Current.connectionTimeout);
			
			// optional default is GET
			oConnection.setRequestMethod("POST");
			oConnection.setRequestProperty("Accept", "*/*");
			oConnection.setDoOutput(true);

			if (asHeaders != null) {
				for (Entry<String, String> asEntry : asHeaders.entrySet()) {
					oConnection.setRequestProperty(asEntry.getKey(), asEntry.getValue());
				}
			}
			
			oConnection.setFixedLengthStreamingMode(oFile.length());
			oConnection.connect();
			
			try (OutputStream oOutputStream = oConnection.getOutputStream()) {
				InputStream oFileStream = FileUtils.openInputStream(oFile);
				Util.copyStream(oFileStream, oOutputStream);
			}			

			// Avoid log spam when we call local addresses
			boolean bLog = true;
			
			if (WasdiConfig.Current.filterInternalHttpCalls) {
				if (sUrl.contains(WasdiConfig.Current.keycloack.introspectAddress)) {
					bLog = false;
				}
				if (sUrl.contains(WasdiConfig.Current.dockers.internalDockerAPIAddress)) {
					bLog = false;
				}				
			}
			
			if (WasdiConfig.Current.logHttpCalls && bLog) {
				WasdiLog.debugLog("HttpUtils.httpPost: Sending 'POST' request to URL : " + sUrl);
			}

			try {
				int iResponseCode = oConnection.getResponseCode();
				
				if (WasdiConfig.Current.logHttpCalls && bLog) {
					WasdiLog.debugLog("HttpUtils.httpPost: Response Code : " + iResponseCode);
				}
				
				oHttpCallResponse.setResponseCode(Integer.valueOf(iResponseCode));

				if (iResponseCode >= 200 && iResponseCode <=299) {
					InputStream oInputStream = oConnection.getInputStream();
					ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();

					if (null != oInputStream) {
						Util.copyStream(oInputStream, oBytearrayOutputStream);
						sResult = oBytearrayOutputStream.toString();
						oHttpCallResponse.setResponseBody(sResult);
					}
				} else {
					WasdiLog.debugLog("HttpUtils.httpPost: provider did not return 200 but "
							+ iResponseCode + " (1/2) and the following message:\n" + oConnection.getResponseMessage());

					ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
					InputStream oErrorStream = oConnection.getErrorStream();
					
					if (oErrorStream!=null) {
						Util.copyStream(oErrorStream, oBytearrayOutputStream);

						if (oBytearrayOutputStream!=null) {
							sResult = oBytearrayOutputStream.toString();
							oHttpCallResponse.setResponseBody(sResult);							
						}
					}
				}
			} catch (Exception oEint) {
				WasdiLog.debugLog("HttpUtils.httpPost error internal: " + oEint);
			} finally {
				oConnection.disconnect();
			}

		} catch (Exception oE) {
			WasdiLog.debugLog("HttpUtils.httpPost error external: " + oE);
		}

		return oHttpCallResponse;		
	}
	
	/**
	 * Standard http post utility function
	 * @param sUrl url to call
	 * @param sPayload payload of the post
	 * @param asHeaders headers dictionary
	 * @param sAuth in the form user:password (i.e., separated by a column: ':')
	 * @return HttpCallResponse object that contains the Response Body and the Response Code
	 */
	public static HttpCallResponse httpPost(String sUrl, byte []ayBytes, Map<String, String> asHeaders, String sAuth, Map<String, List<String>> aoOutputHeaders) {
		
		HttpCallResponse oHttpCallResponse = new HttpCallResponse();

		String sResult = null;
		try {
			
			if (sUrl.startsWith("unix:///")) {
				return SocketUtils.httpPost(sUrl, ayBytes, asHeaders, aoOutputHeaders);
			}
			
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();
			
			// Set Read Timeout
			oConnection.setReadTimeout(WasdiConfig.Current.readTimeout);
			// Set Connection Timeout
			oConnection.setConnectTimeout(WasdiConfig.Current.connectionTimeout);			
			
			if(!Utils.isNullOrEmpty(sAuth)) {
				String sEncodedAuth = Base64.getEncoder().encodeToString(sAuth.getBytes(StandardCharsets.UTF_8));
				String sAuthHeaderValue = "Basic " + sEncodedAuth;
				oConnection.setRequestProperty("Authorization", sAuthHeaderValue);
			}
			
			// optional default is GET
			oConnection.setRequestMethod("POST");
			oConnection.setRequestProperty("Accept", "*/*");
			oConnection.setDoOutput(true);

			if (asHeaders != null) {
				for (Entry<String, String> asEntry : asHeaders.entrySet()) {
					oConnection.setRequestProperty(asEntry.getKey(), asEntry.getValue());
				}
			}
			
			oConnection.setFixedLengthStreamingMode(ayBytes.length);
			oConnection.connect();
			
			try (OutputStream oOutputStream = oConnection.getOutputStream()) {
				oOutputStream.write(ayBytes);
			}			

			// Avoid log spam when we call local addresses
			boolean bLog = true;
			
			if (WasdiConfig.Current.filterInternalHttpCalls) {
				if (sUrl.contains(WasdiConfig.Current.keycloack.introspectAddress)) {
					bLog = false;
				}
				if (sUrl.contains(WasdiConfig.Current.dockers.internalDockerAPIAddress)) {
					bLog = false;
				}				
			}
			
			if (WasdiConfig.Current.logHttpCalls && bLog) {
				WasdiLog.debugLog("HttpUtils.httpPost: Sending 'POST' request to URL : " + sUrl);
			}

			try {
				int iResponseCode = oConnection.getResponseCode();
				
				if (WasdiConfig.Current.logHttpCalls && bLog) {
					WasdiLog.debugLog("HttpUtils.httpPost: Response Code : " + iResponseCode);
				}
				
				oHttpCallResponse.setResponseCode(Integer.valueOf(iResponseCode));
				
				if (aoOutputHeaders!=null) {
					try {
						
						// Get the Response headers
						Map<String, List<String>> aoReceivedHeaders = oConnection.getHeaderFields();
						
						// Copy in the ouput dictionary
						for (Map.Entry<String, List<String>> oEntry : aoReceivedHeaders.entrySet()) {
							aoOutputHeaders.put(oEntry.getKey(), oEntry.getValue());
						}
					}
					catch (Exception oEx) {
						WasdiLog.errorLog("HttpUtils.httpPost: Exception getting the output headers ", oEx);
					}
				}
				

				if (iResponseCode >= 200 && iResponseCode <=299) {
					InputStream oInputStream = oConnection.getInputStream();
					ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();

					if (null != oInputStream) {
						Util.copyStream(oInputStream, oBytearrayOutputStream);
						sResult = oBytearrayOutputStream.toString();
						oHttpCallResponse.setResponseBody(sResult);
					}
				} else {
					WasdiLog.debugLog("HttpUtils.httpPost: provider did not return 200 but "
							+ iResponseCode + " (1/2) and the following message:\n" + oConnection.getResponseMessage());

					ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
					InputStream oErrorStream = oConnection.getErrorStream();
					
					if (oErrorStream!=null) {
						Util.copyStream(oErrorStream, oBytearrayOutputStream);

						if (oBytearrayOutputStream!=null) {
							sResult = oBytearrayOutputStream.toString();
							oHttpCallResponse.setResponseBody(sResult);							
						}
					}
				}
			} catch (Exception oEint) {
				WasdiLog.debugLog("HttpUtils.httpPost error internal: " + oEint);
			} finally {
				oConnection.disconnect();
			}

		} catch (Exception oE) {
			WasdiLog.debugLog("HttpUtils.httpPost error external: " + oE);
		}

		return oHttpCallResponse;
	}
	

	/**
	 * Standard http post file utility function
	 * @param sUrl destination url
	 * @param sFileName full path of the file to post
	 * @param asHeaders headers to use
	 * @throws IOException
	 */
	public static boolean httpPostFile(String sUrl, String sFileName, Map<String, String> asHeaders) throws IOException {
		//local file -> automatically checks for null
		File oFile = new File(sFileName);
		if (!oFile.exists()) {
			WasdiLog.errorLog("Wasdi.httpPostFile: file not found");
			return false;
		}
		
		if (!Utils.isNullOrEmpty(sUrl)) {
			if (sUrl.startsWith("unix:///")) {
				HttpCallResponse oResponse = SocketUtils.httpPostFile(sUrl, sFileName, asHeaders);
				
				if (oResponse != null) {
					if (oResponse.getResponseCode()>=200 && oResponse.getResponseCode()<=299) {
						return true;
					}
					return false;
				}
				else {
					return false;
				}
			}
		}

		String sZippedFile = null;

		// Check if we need to zip this file
		if (!oFile.getName().toUpperCase().endsWith("ZIP")) {

			WasdiLog.debugLog("HttpUtils.httpPostFile: File not zipped, zip it");

			int iRandom = new SecureRandom().nextInt() & Integer.MAX_VALUE;

			String sTemp = "tmp-" + iRandom + File.separator;
			String sTempPath = WasdiFileUtils.fixPathSeparator(oFile.getParentFile().getPath());

			if (!sTempPath.endsWith(File.separator)) {
				sTempPath += File.separator;
			}
			sTempPath += sTemp;

			Path oPath = Paths.get(sTempPath).toAbsolutePath().normalize();
			if (oPath.toFile().mkdir()) {
				WasdiLog.debugLog("HttpUtils.httpPostFile: Temporary directory created");
			} else {
				throw new IOException("HttpUtils.httpPostFile: Can't create temporary dir " + sTempPath);
			}

			sZippedFile = sTempPath+iRandom + ".zip";

			File oZippedFile = new File(sTempPath+iRandom + ".zip");
			ZipOutputStream oOutZipStream = new ZipOutputStream(new FileOutputStream(oZippedFile));
			ZipFileUtils.zipFile(oFile, oFile.getName(), oOutZipStream);

			oOutZipStream.close();

			String sOldFileName = oFile.getName();

			oFile = new File(sZippedFile);

			sUrl = sUrl.replace(sOldFileName, oFile.getName());

			sFileName = oFile.getName();
		}
		
		boolean bLog = true;
		
		if (WasdiConfig.Current.filterInternalHttpCalls) {
			if (sUrl.contains(WasdiConfig.Current.keycloack.introspectAddress)) {
				bLog = false;
			}
			if (sUrl.contains(WasdiConfig.Current.dockers.internalDockerAPIAddress)) {
				bLog = false;
			}				
		}
		
		if (WasdiConfig.Current.logHttpCalls && bLog) {
			WasdiLog.debugLog("HttpUtils.httpPostFile: calling url " + sUrl);
		}

		String sBoundary = "**WASDIlib**" + UUID.randomUUID().toString() + "**WASDIlib**";
		try (FileInputStream oInputStream = new FileInputStream(oFile)) {
			//request
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();
			oConnection.setDoOutput(true);
			oConnection.setDoInput(true);
			oConnection.setUseCaches(false);
			int iBufferSize = 8192;//8*1024*1024
			oConnection.setChunkedStreamingMode(iBufferSize);
			Long lLen = oFile.length();
			WasdiLog.debugLog("HttpUtils.httpPostFile: file length is: " + Long.toString(lLen));

			if (asHeaders != null) {
				for (String sKey : asHeaders.keySet()) {
					oConnection.setRequestProperty(sKey, asHeaders.get(sKey));
				}
			}
			oConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + sBoundary);
			oConnection.setRequestProperty("Connection", "Keep-Alive");
			oConnection.setRequestProperty("User-Agent", "WasdiLib.Java");
			oConnection.connect();

			try (DataOutputStream oOutputStream = new DataOutputStream(oConnection.getOutputStream())) {

				oOutputStream.writeBytes( "--" + sBoundary + "\r\n" );
				oOutputStream.writeBytes( "Content-Disposition: form-data; name=\"" + "file" + "\"; filename=\"" + sFileName + "\"" + "\r\n");
				oOutputStream.writeBytes( "Content-Type: " + URLConnection.guessContentTypeFromName(sFileName) + "\r\n");
				oOutputStream.writeBytes( "Content-Transfer-Encoding: binary" + "\r\n");
				oOutputStream.writeBytes("\r\n");

				Util.copyStream(oInputStream, oOutputStream);

				oOutputStream.flush();
				oInputStream.close();
				oOutputStream.writeBytes("\r\n");
				oOutputStream.flush();
				oOutputStream.writeBytes("\r\n");
				oOutputStream.writeBytes("--" + sBoundary + "--"+"\r\n");

				// response
				int iResponse = oConnection.getResponseCode();
				
				if (WasdiConfig.Current.logHttpCalls && bLog) {
					WasdiLog.debugLog("HttpUtils.httpPostFile: server returned " + iResponse);
				}

				InputStream oResponseInputStream = null;

				ByteArrayOutputStream oByteArrayOutputStream = new ByteArrayOutputStream();

				if( 200 <= iResponse && 299 >= iResponse ) {
					oResponseInputStream = oConnection.getInputStream();
				} else {
					oResponseInputStream = oConnection.getErrorStream();
				}
				if(null!=oResponseInputStream) {
					Util.copyStream(oResponseInputStream, oByteArrayOutputStream);
					
					if (WasdiConfig.Current.logHttpCalls) {
						String sMessage = "HttpUtils.uploadFile: " + oByteArrayOutputStream.toString();
						WasdiLog.debugLog(sMessage);
					}
				} else {
					throw new NullPointerException("WasdiLib.uploadFile: stream is null");
				}

				oConnection.disconnect();

			} catch(Exception oE) {
				WasdiLog.debugLog("HttpUtils.uploadFile( " + sUrl + ", " + sFileName + ", ...): internal exception: " + oE);
				return false;
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("HttpUtils.httpPostFile( " + sUrl + ", " + sFileName + ", ...): could not open file due to: " + oE + ", aborting");
			return false;
		}

		if (!Utils.isNullOrEmpty(sZippedFile)) {
			try {
				FileUtils.deleteDirectory(new File(sZippedFile).getParentFile());
			}
			catch (Exception oE) {
				WasdiLog.debugLog("HttpUtils.httpPostFile( " + sUrl + ", " + sFileName + ", ...): could not delete temp zip file: " + oE + "");
			}
		}

		return true;
	}

	/**
	 * Standard http put utility function
	 * @param sUrl url to call
	 * @param sPayload payload of the post
	 * @param asHeaders headers dictionary
	 * @return server response
	 */
	public static String httpPut(String sUrl, String sPayload, Map<String, String> asHeaders) {
		return httpPut(sUrl, sPayload, asHeaders, null);
	}

	/**
	 * Standard http put utility function
	 * @param sUrl url to call
	 * @param sPayload payload of the post
	 * @param asHeaders headers dictionary
	 * @param sAuth in the form user:password (i.e., separated by a column: ':')
	 * @return server response
	 */
	public static String httpPut(String sUrl, String sPayload, Map<String, String> asHeaders, String sAuth) {

		try {
			
			if (sUrl.startsWith("unix:///")) {
				HttpCallResponse oResponse = SocketUtils.httpPut(sUrl, asHeaders, sPayload.getBytes());
				
				if (oResponse != null) {
					return oResponse.getResponseBody();
				}
				else {
					return "";
				}
			}
			
			URL oURL = new URL(sUrl);
			
			boolean bLog = true;
			
			if (WasdiConfig.Current.filterInternalHttpCalls) {
				if (sUrl.contains(WasdiConfig.Current.keycloack.introspectAddress)) {
					bLog = false;
				}
				if (sUrl.contains(WasdiConfig.Current.dockers.internalDockerAPIAddress)) {
					bLog = false;
				}				
			}
			
			if (WasdiConfig.Current.logHttpCalls && bLog) {
				WasdiLog.debugLog("HttpUtils.httpPut: calling url " + sUrl);
			}
			
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();

			if(!Utils.isNullOrEmpty(sAuth)) {
				String sEncodedAuth = Base64.getEncoder().encodeToString(sAuth.getBytes(StandardCharsets.UTF_8));
				String sAuthHeaderValue = "Basic " + sEncodedAuth;
				oConnection.setRequestProperty("Authorization", sAuthHeaderValue);
			}

			oConnection.setDoOutput(true);
			oConnection.setRequestMethod("PUT");
			
			// Set Read Timeout
			oConnection.setReadTimeout(WasdiConfig.Current.readTimeout);
			// Set Connection Timeout
			oConnection.setConnectTimeout(WasdiConfig.Current.connectionTimeout);			

			if (asHeaders != null) {
				for (Entry<String, String> asEntry : asHeaders.entrySet()) {
					oConnection.setRequestProperty(asEntry.getKey(), asEntry.getValue());
				}
			}

			OutputStream oPostOutputStream = oConnection.getOutputStream();
			OutputStreamWriter oStreamWriter = new OutputStreamWriter(oPostOutputStream, "UTF-8");  
			if (sPayload!= null) oStreamWriter.write(sPayload);
			oStreamWriter.flush();
			oStreamWriter.close();
			oPostOutputStream.close();

			oConnection.connect();

			String sMessage = readHttpResponse(oConnection);
			oConnection.disconnect();

			return sMessage;
		} catch (Exception oEx) {
			WasdiLog.errorLog("HttpUtils.httpPut: error", oEx);
			return "";
		}
	}	
	
	/**
	 * Standard http delete utility function
	 * 
	 * @param sUrl url to call
	 * @return server response
	 */
	public static HttpCallResponse httpDelete(String sUrl) {
		return httpDelete(sUrl, null);
	}
	
	/**
	 * Standard http delete utility function
	 * 
	 * @param sUrl url to call
	 * @param asHeaders headers dictionary
	 * @return server response
	 */
	public static HttpCallResponse httpDelete(String sUrl, Map<String, String> asHeaders) {
		HttpCallResponse oHttpCallResponse = new HttpCallResponse();
		String sMessage = "";

		if (sUrl == null || sUrl.isEmpty()) {
			WasdiLog.debugLog("HttpUtils.httpDelete: invalid URL, aborting");
			return oHttpCallResponse;
		}

		try {
			
			if (sUrl.startsWith("unix:///")) {
				return SocketUtils.httpDelete(sUrl, asHeaders);
			}
			
			
			boolean bLog = true;
			
			if (WasdiConfig.Current.filterInternalHttpCalls) {
				if (sUrl.contains(WasdiConfig.Current.keycloack.introspectAddress)) {
					bLog = false;
				}
				if (sUrl.contains(WasdiConfig.Current.dockers.internalDockerAPIAddress)) {
					bLog = false;
				}				
			}
			
			if (WasdiConfig.Current.logHttpCalls && bLog) {
				WasdiLog.debugLog("HttpUtils.httpDelete: calling url " + sUrl);
			}
			
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();

			oConnection.setRequestMethod("DELETE");
			
			// Set Read Timeout
			oConnection.setReadTimeout(WasdiConfig.Current.readTimeout);
			// Set Connection Timeout
			oConnection.setConnectTimeout(WasdiConfig.Current.connectionTimeout);			

			if (asHeaders != null) {
				for (Entry<String, String> asEntry : asHeaders.entrySet()) {
					oConnection.setRequestProperty(asEntry.getKey(), asEntry.getValue());
				}
			}

			oConnection.connect();

			int iResponseCode =  oConnection.getResponseCode();
			
			if (WasdiConfig.Current.logHttpCalls && bLog) {
				WasdiLog.debugLog("HttpUtils.httpDelete: response code " + iResponseCode);
			}
			
			oHttpCallResponse.setResponseCode(Integer.valueOf(iResponseCode));

			if (200 <= iResponseCode && 299 >= iResponseCode) {
				BufferedReader oInputBuffer = new BufferedReader(new InputStreamReader(oConnection.getInputStream()));
				String sInputLine;
				StringBuilder oResponse = new StringBuilder();

				while ((sInputLine = oInputBuffer.readLine()) != null) {
					oResponse.append(sInputLine);
				}
				oInputBuffer.close();
				
				oHttpCallResponse.setResponseBody(oResponse.toString());
				
			} else {
				sMessage = oConnection.getResponseMessage();
				WasdiLog.debugLog("HttpUtils.httpDelete:  connection failed, message follows");
				WasdiLog.debugLog(sMessage);
				
				oHttpCallResponse.setResponseBody(sMessage);
			}

			oConnection.disconnect();
		} catch (Exception oEx) {
			WasdiLog.errorLog("HttpUtils.httpDelete: Exception " + oEx.toString());
		}

		return oHttpCallResponse;
	}
	
	/**
	 * Get the http headers for a basic http authentication
	 * 
	 * @param sDownloadUser
	 * @param sDownloadPassword
	 * @return
	 */
	public static Map<String, String> getBasicAuthorizationHeaders(String sDownloadUser, String sDownloadPassword) {
		
		try {
			// Add the auth header
			String sAuth = sDownloadUser + ":" + sDownloadPassword;
			String sEncodedAuth = Base64.getEncoder().encodeToString(sAuth.getBytes(StandardCharsets.UTF_8));
			String sAuthHeaderValue = "Basic " + sEncodedAuth;

			Map<String, String> asHeaders = new HashMap<>();
			asHeaders.put("Authorization", sAuthHeaderValue);
			
			return asHeaders;			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("HttpUtils.getBasicAuthorizationHeaders Exception " + oEx.toString());
			return new HashMap<String, String>();
		}
	}


	/**
	 * Get the size of a file to be downloaded via HTTP.
	 * 
	 * @param sUrl url to call
	 * @param asHeaders headers dictionary
	 * @return the size of the file
	 */
	public static long getDownloadFileSizeViaHttp(String sUrl, Map<String, String> asHeaders) {
		long lLenght = 0L;

		try {
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();
			oConnection.setConnectTimeout(2000);
			oConnection.setReadTimeout(2000);

			oConnection.setDoOutput(true);
			oConnection.setRequestMethod("GET");

			if (asHeaders != null) {
				for (Entry<String, String> asEntry : asHeaders.entrySet()) {
					oConnection.setRequestProperty(asEntry.getKey(), asEntry.getValue());
				}
			}

			oConnection.connect();

			lLenght = getHttpResponseContentLength(oConnection);

			oConnection.disconnect();
		} catch (Exception oEx) {
			WasdiLog.errorLog("HttpUtils.getDownloadFileSizeViaHttp: Exception " + oEx.toString());
		}

		return lLenght;
	}
	
	/**
	 * Downloads a file from a specified url and saves it in a specified path
	 * @param sUrl Url of the file to download
	 * @param asHeaders Map of key-value that will be added as headers to the request
	 * @param sOutputFilePath Output path where to save the file
	 * @return The output path if all ok, empty string in case of error
	 */
	public static String downloadFile(String sUrl, Map<String, String> asHeaders, String sOutputFilePath) {

		try {
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();

			// optional default is GET
			oConnection.setRequestMethod("GET");
			for (Entry<String, String> asEntry : asHeaders.entrySet()) {
				oConnection.setRequestProperty(asEntry.getKey(), asEntry.getValue());
			}

			int iResponseCode =  oConnection.getResponseCode();

			if (iResponseCode == HttpURLConnection.HTTP_OK) {

				Map<String, List<String>> aoHeaders = oConnection.getHeaderFields();
				List<String> asContents = null;
				if (null != aoHeaders) {
					asContents = aoHeaders.get("Content-Disposition");
				}
				String sAttachmentName = null;
				if (null != asContents) {
					String sHeader = asContents.get(0);
					sAttachmentName = sHeader.split("filename=")[1];
					if (sAttachmentName.startsWith("\"")) {
						sAttachmentName = sAttachmentName.substring(1);
					}
					if(sAttachmentName.endsWith("\"")) {
						sAttachmentName = sAttachmentName.substring(0, sAttachmentName.length() - 1);
					}
					WasdiLog.debugLog("HttpUtils.downloadFile: attachment name: " + sAttachmentName);
				}
				
				File oTargetFile = new File(sOutputFilePath);
				File oTargetDir = oTargetFile.getParentFile();

				
				if (oTargetDir != null) {
					// If the targetDir exists but it is not a directory, delete it as it prevents the creation of the actual directory
					if (oTargetDir.exists() && !oTargetDir.isDirectory()) {
						boolean bIsFileDeleted = oTargetDir.delete();
						if (!bIsFileDeleted)
							WasdiLog.warnLog("HttpUtils.downloadFile: the file has not been deleted, path: " + oTargetDir.getAbsolutePath());
					}
					oTargetDir.mkdirs();
				} else {
					WasdiLog.warnLog("HttpUtils.downloadFile: the target directory was not created, because it is null");
				}

				try (FileOutputStream oOutputStream = new FileOutputStream(sOutputFilePath);
						InputStream oInputStream = oConnection.getInputStream()) {
					// 	opens an output stream to save into file
					Util.copyStream(oInputStream, oOutputStream);
				} catch (Exception oEx) {
					WasdiLog.errorLog("HttpUtils.downloadFile: error ", oEx);
				}
				return sOutputFilePath;
			} else {
				String sMessage = "HttpUtils.downloadFile: response message: " + oConnection.getResponseMessage();
				WasdiLog.warnLog(sMessage);
				return "";
			}

		} catch (Exception oEx) {
			WasdiLog.errorLog("HttpUtils.downloadFile: Exception " + oEx.toString());
			return "";
		}
	}
		
	/**
	 * Read http response stream
	 * @param oConnection Http Connection
	 * @return
	 */
	public static String readHttpResponse(HttpURLConnection oConnection) {
		return readHttpResponse(oConnection, null);
	}

	/**
	 * Read http response stream
	 * @param oConnection Http Connection
	 * @param aoOutputHeaders Map of response headers 
	 * @throws IOException
	 * @throws CopyStreamException
	 */
	public static String readHttpResponse(HttpURLConnection oConnection, Map<String, List<String>> aoOutputHeaders) {
		try {
			
			if (aoOutputHeaders!=null) {
				try {
					
					Map<String, List<String>> aoReceivedHeaders = oConnection.getHeaderFields();
					
					for (Map.Entry<String, List<String>> oEntry : aoReceivedHeaders.entrySet()) {
						aoOutputHeaders.put(oEntry.getKey(), oEntry.getValue());
					}
				}
				catch (Exception oEx) {
					WasdiLog.errorLog("exception getting the output headers ", oEx);
				}
			}
			
			// response

			InputStream oResponseInputStream = null;
			try {
				oResponseInputStream = oConnection.getInputStream();
			} catch (Exception oE) {
				WasdiLog.debugLog("HttpUtils.readHttpResponse: could not getInputStream due to: " + oE);
			}

			try {
				if (null == oResponseInputStream) {
					oResponseInputStream = oConnection.getErrorStream();
				}
			} catch (Exception oE) {
				WasdiLog.debugLog("HttpUtils.readHttpResponse: could not getErrorStream due to: " + oE);
			}


			ByteArrayOutputStream oByteArrayOutputStream = new ByteArrayOutputStream();


			Util.copyStream(oResponseInputStream, oByteArrayOutputStream);
			String sMessage = oByteArrayOutputStream.toString();
			
			if (200 <= oConnection.getResponseCode() && 299 >= oConnection.getResponseCode()) {
				return sMessage;
			} else {
				WasdiLog.debugLog("HttpUtils.readHttpResponse: status: " + oConnection.getResponseCode() + ", error message: " + sMessage);
				return "";
			}

		} catch (Exception oE) {
			WasdiLog.debugLog("HttpUtils.readHttpResponse: exception: " + oE );
		}
		return "";
	}

	/**
	 * Get the length of the content returned by the call.
	 * 
	 * @param oConnection the connection
	 * @return the length of the content
	 */
	private static long getHttpResponseContentLength(HttpURLConnection oConnection) {
		long lLenght = 0L;

		try {
			int responseCode = oConnection.getResponseCode();

			// always check HTTP response code first
			if (responseCode == HttpURLConnection.HTTP_OK) {
				lLenght = oConnection.getHeaderFieldLong("Content-Length", 0L);

				WasdiLog.debugLog("HttpUtils.getHttpResponseContentLength: File size = " + lLenght);
			} else {
				WasdiLog.debugLog("HttpUtils.getHttpResponseContentLength: No file to download. Server replied HTTP code: " + responseCode);
			}
		} catch (IOException oE) {
			WasdiLog.debugLog("HttpUtils.getHttpResponseContentLength: exception: " + oE );
		}

		return lLenght;
	}

	/**
	 * Get the standard headers for a WASDI call
	 * 
	 * @param sSessionId the sessionId
	 * @return the map containing the headers
	 */
	public static Map<String, String> getStandardHeaders(String sSessionId) {
		Map<String, String> asHeaders = new HashMap<>();
		asHeaders.put("x-session-token", sSessionId);
		asHeaders.put("Content-Type", "application/json");

		return asHeaders;
	}

	/**
	 * Get the OpenId Connect header for a external-services call
	 * 
	 * @param sOpenidConnectToken the OpenId Connect token
	 * @return the map containing the headers
	 */
	public static Map<String, String> getOpenIdConnectHeaders(String sOpenidConnectToken) {
		Map<String, String> asHeaders = new HashMap<>();
		asHeaders.put("Authorization", "Bearer " + sOpenidConnectToken);

		return asHeaders;
	}

	/**
	 * Log the operation speed.
	 * @param sUrl the url of the operation
	 * @param sMethodName the originating method name
	 * @param lStart the start in millis
	 * @param lEnd the end in millis
	 * @param sResult the content of the response
	 */
	public static void logOperationSpeed(String sUrl, String sMethodName, long lStart, long lEnd, String sResult) {
		int iResponseSize = sResult == null ? 0 : sResult.length();

		long lTimeElapsed = lEnd - lStart;
		double dMillis = lTimeElapsed / (1000.0 * 1000.0);
		double dSpeed = 0;
		if (iResponseSize > 0) {
			dSpeed = ((double) iResponseSize) / dMillis;
			dSpeed *= 1000.0;
		}

		WasdiLog.debugLog("HttpUtils." + sMethodName + " performance: " + dMillis + " ms, "
				+ iResponseSize + " B (" + dSpeed + " B/s)");
	}

	/**
	 * Obtains an OpenId Connection Token
	 * @param sUrl Url to call
	 * @param sUser User
	 * @param sPassword Password
	 * @param sClientId Client Id
	 * @return The token, or null in case of errors
	 */	
	public static String obtainOpenidConnectToken(String sUrl, String sUser, String sPassword, String sClientId) {
		return obtainOpenidConnectToken(sUrl, sUser, sPassword, sClientId, null, null, null);
	}
	
	
	/**
	 * 
	 * Obtains an OpenId Connection Token
	 * @param sUrl Url to call
	 * @param sUser User
	 * @param sPassword Password
	 * @param sClientId Client Id
	 * @param sScope Scope 
	 * @param sClientSecret Client Secret
	 * @return The token, or null in case of errors
	 * 
	 * @return The token, or null in case of errors
	 */
	public static String obtainOpenidConnectToken(String sUrl, String sUser, String sPassword, String sClientId, String sScope, String sClientSecret, Map<String, String> asOtherHeaders) {
		return obtainOpenidConnectToken(sUrl, sUser, sPassword, sClientId, sScope, sClientSecret, asOtherHeaders, "access_token");
	}
	
	/**
	 * 
	 * Obtains an OpenId Connection Token
	 * @param sUrl Url to call
	 * @param sUser User
	 * @param sPassword Password
	 * @param sClientId Client Id
	 * @param sScope Scope 
	 * @param sClientSecret Client Secret
	 * @param sTokeKey key of the json where to read the token
	 * 
	 * @return The token, or null in case of errors
	 */
	public static String obtainOpenidConnectToken(String sUrl, String sUser, String sPassword, String sClientId, String sScope, String sClientSecret, Map<String, String> asOtherHeaders, String sTokeKey) {
		try {
			URL oURL = new URL(sUrl);

			 // Create all-trusting host name verifier
	        HostnameVerifier oAllHostsValid = new HostnameVerifier() {
	        	@Override
	            public boolean verify(String hostname, SSLSession session) {
	                return true;
	            }
	        };
	        
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();
			
			if (oConnection instanceof HttpsURLConnection) {
			    HttpsURLConnection oHttpsConn = (HttpsURLConnection) oConnection;
			    oHttpsConn.setHostnameVerifier(oAllHostsValid);
			    SSLContext oSc = SSLContext.getInstance("SSL");
			    oHttpsConn.setSSLSocketFactory(oSc.getSocketFactory());
			}
			
			oConnection.setRequestMethod("POST");
			oConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			
			// Set Read Timeout
			oConnection.setReadTimeout(WasdiConfig.Current.readTimeout);
			// Set Connection Timeout
			oConnection.setConnectTimeout(WasdiConfig.Current.connectionTimeout);			
			
			if (asOtherHeaders!=null) {
				for (String sKey : asOtherHeaders.keySet()) {
					oConnection.setRequestProperty(sKey, asOtherHeaders.get(sKey));					
				}
			}
			
			oConnection.setDoOutput(true);
			
			String sBody = "client_id=" + sClientId + "&password=" + sPassword + "&username=" + sUser + "&grant_type=password";
			
			if (!Utils.isNullOrEmpty(sScope)) {
				sBody = "scope=" + sScope + "&" + sBody;
			}
			
			if (!Utils.isNullOrEmpty(sClientSecret)) {
				sBody = sBody + "&client_secret=" + sClientSecret;
			}
			
			oConnection.getOutputStream().write(sBody.getBytes());

			int iStatus = oConnection.getResponseCode();
			WasdiLog.debugLog("HttpUtils.obtainOpenidConnectToken: Response status: " + iStatus);

			if (iStatus == 200) {
				InputStream oInputStream = oConnection.getInputStream();
				ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();

				if (null != oInputStream) {
					Util.copyStream(oInputStream, oBytearrayOutputStream);
					String sResult = oBytearrayOutputStream.toString();
					
					WasdiLog.debugLog("HttpUtils.obtainOpenidConnectToken: got result: "  + sResult);

					JSONObject oJson = new JSONObject(sResult);
					String sToken = oJson.optString(sTokeKey, null);

					return sToken;
				}
			} else {
				ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
				InputStream oErrorStream = oConnection.getErrorStream();
				Util.copyStream(oErrorStream, oBytearrayOutputStream);

				String sMessage = oBytearrayOutputStream.toString();
				WasdiLog.debugLog("HttpUtils.obtainOpenidConnectToken:" + sMessage);
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("HttpUtils.obtainOpenidConnectToken: " + oE);
		}

		return null;
	}


}
