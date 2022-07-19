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
import java.util.UUID;
import java.util.Map.Entry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.io.CopyStreamException;
import org.apache.commons.net.io.Util;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import wasdi.shared.viewmodels.HttpCallResponse;

/**
 * Utility class for HTTP operations.
 * 
 * @author PetruPetrescu
 *
 */
public final class HttpUtils {

	/**
	 * Static logger reference
	 */
	public static LoggerWrapper s_oLogger = new LoggerWrapper(Logger.getLogger(HttpUtils.class));

	private HttpUtils() {
		throw new java.lang.UnsupportedOperationException("This is a utility class and cannot be instantiated");
	}

	/**
	 * Standard http get utility function
	 * 
	 * @param sUrl url to call
	 * @param asHeaders headers dictionary
	 * @return server response
	 */
	public static String httpGet(String sUrl, Map<String, String> asHeaders) {
		String sMessage = "";

		if (sUrl == null || sUrl.isEmpty()) {
			Utils.debugLog("Wasdi.httpGet: invalid URL, aborting");
			return sMessage;
		}

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

			sMessage = readHttpResponse(oConnection);

			oConnection.disconnect();
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return sMessage;
	}

	/**
	 * Standard http delete utility function
	 * 
	 * @param sUrl url to call
	 * @param asHeaders headers dictionary
	 * @return server response
	 */
	public static String httpDelete(String sUrl, Map<String, String> asHeaders) {
		String sMessage = "";

		if (sUrl == null || sUrl.isEmpty()) {
			Utils.debugLog("Wasdi.httpDelete: invalid URL, aborting");
			return sMessage;
		}

		try {
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();

			oConnection.setRequestMethod("DELETE");

			if (asHeaders != null) {
				for (Entry<String, String> asEntry : asHeaders.entrySet()) {
					oConnection.setRequestProperty(asEntry.getKey(), asEntry.getValue());
				}
			}

			oConnection.connect();

			int iResponseCode =  oConnection.getResponseCode();

			if (200 <= iResponseCode && 299 >= iResponseCode) {
				BufferedReader oInputBuffer = new BufferedReader(new InputStreamReader(oConnection.getInputStream()));
				String sInputLine;
				StringBuilder oResponse = new StringBuilder();

				while ((sInputLine = oInputBuffer.readLine()) != null) {
					oResponse.append(sInputLine);
				}
				oInputBuffer.close();

				return oResponse.toString();
			} else {
				sMessage = oConnection.getResponseMessage();
				Utils.debugLog("Wasdi.httpDelete:  connection failed, message follows");
				Utils.debugLog(sMessage);

				sMessage = "";
			}

			oConnection.disconnect();
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return sMessage;
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
			oEx.printStackTrace();
		}

		return lLenght;
	}

	public static String downloadFile(String sUrl, Map<String, String> asHeaders, String sOutputFilePath) {

		try {
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();

			// optional default is GET
			oConnection.setRequestMethod("GET");
			for (Entry<String, String> asEntry : asHeaders.entrySet()) {
				oConnection.setRequestProperty(asEntry.getKey(), asEntry.getValue());
			}

			int responseCode =  oConnection.getResponseCode();

			if (responseCode == HttpURLConnection.HTTP_OK) {

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
					Utils.debugLog("Wasdi.downloadWorkflow: attachment name: " + sAttachmentName);
				}
				

				
				File oTargetFile = new File(sOutputFilePath);
				File oTargetDir = oTargetFile.getParentFile();
				oTargetDir.mkdirs();


				try (FileOutputStream oOutputStream = new FileOutputStream(sOutputFilePath);
						InputStream oInputStream = oConnection.getInputStream()) {
					// 	opens an output stream to save into file
					Util.copyStream(oInputStream, oOutputStream);
				} catch (Exception oEx) {
					oEx.printStackTrace();
				}
				return sOutputFilePath;
			} else {
				String sMessage = "Wasdi.downloadWorkflow: response message: " + oConnection.getResponseMessage();
				Utils.debugLog(sMessage);
				return "";
			}

		} catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}
	}

	public static String standardHttpGETQuery(String sUrl, Map<String, String> asHeaders) {

		String sResult = null;
		try {
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();
			// optional default is GET
			oConnection.setRequestMethod("GET");
			oConnection.setRequestProperty("Accept", "*/*");

			if (asHeaders != null) {
				for (Entry<String, String> asEntry : asHeaders.entrySet()) {
					oConnection.setRequestProperty(asEntry.getKey(), asEntry.getValue());
				}
			}

			Utils.debugLog("\nSending 'GET' request to URL : " + sUrl);

			try {
				int iResponseCode = oConnection.getResponseCode();
				Utils.debugLog("HttpUtils.standardHttpGETQuery: Response Code : " + iResponseCode);
				String sResponseExtract = null;
				if (200 == iResponseCode) {
					InputStream oInputStream = oConnection.getInputStream();
					ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
					if (null != oInputStream) {
						Util.copyStream(oInputStream, oBytearrayOutputStream);
						sResult = oBytearrayOutputStream.toString();
					}

					if (sResult != null) {
						if (sResult.length() > 201) {
							sResponseExtract = sResult.substring(0, 200) + "...";
						} else {
							sResponseExtract = new String(sResult);
						}
						Utils.debugLog("HttpUtils.standardHttpGETQuery: Response extract: " + sResponseExtract);
					} else {
						Utils.debugLog("HttpUtils.standardHttpGETQuery: reponse is empty");
					}
				} else {
					Utils.debugLog("HttpUtils.standardHttpGETQuery: provider did not return 200 but "
							+ iResponseCode + " (1/2) and the following message:\n" + oConnection.getResponseMessage());
					ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
					InputStream oErrorStream = oConnection.getErrorStream();
					Util.copyStream(oErrorStream, oBytearrayOutputStream);
					String sMessage = oBytearrayOutputStream.toString();
					if (null != sMessage) {
						sResponseExtract = sMessage.substring(0, Math.min(sMessage.length(), 200)) + "...";
						Utils.debugLog(
								"HttpUtils.standardHttpGETQuery: provider did not return 200 but " + iResponseCode
										+ " (2/2) and this is the content of the error stream:\n" + sResponseExtract);
					}
				}
			} catch (Exception oEint) {
				Utils.debugLog("HttpUtils.standardHttpGETQuery: " + oEint);
			} finally {
				oConnection.disconnect();
			}
		} catch (Exception oE) {
			Utils.debugLog("HttpUtils.standardHttpGETQuery: " + oE);
		}
		return sResult;
	}

	public static HttpCallResponse newStandardHttpGETQuery(String sUrl, Map<String, String> asHeaders) {
		HttpCallResponse oHttpCallResponse = new HttpCallResponse();

		String sResult = null;

		try {
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();
			// optional default is GET
			oConnection.setRequestMethod("GET");
			oConnection.setRequestProperty("Accept", "*/*");

			if (asHeaders != null) {
				for (Entry<String, String> asEntry : asHeaders.entrySet()) {
					oConnection.setRequestProperty(asEntry.getKey(), asEntry.getValue());
				}
			}

			s_oLogger.debug("Sending 'GET' request to URL : " + sUrl);

			try {
				int iResponseCode = oConnection.getResponseCode();
				s_oLogger.debug("HttpUtils.newStandardHttpGETQuery: Response Code : " + iResponseCode);

				oHttpCallResponse.setResponseCode(Integer.valueOf(iResponseCode));

				if (200 <= iResponseCode && 299 >= iResponseCode) {
					InputStream oInputStream = oConnection.getInputStream();
					ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
					if (null != oInputStream) {
						Util.copyStream(oInputStream, oBytearrayOutputStream);
						sResult = oBytearrayOutputStream.toString();
						oHttpCallResponse.setResponseBody(sResult);
					}
				} else {
					s_oLogger.debug("HttpUtils.standardHttpGETQuery: provider did not return 200 but "
							+ iResponseCode + " (1/2) and the following message:\n" + oConnection.getResponseMessage());

					ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
					InputStream oErrorStream = oConnection.getErrorStream();
					Util.copyStream(oErrorStream, oBytearrayOutputStream);

					sResult = oBytearrayOutputStream.toString();
					oHttpCallResponse.setResponseBody(sResult);
				}
			} catch (Exception oEint) {
				s_oLogger.debug("HttpUtils.newStandardHttpGETQuery: " + oEint);
			} finally {
				oConnection.disconnect();
			}
		} catch (Exception oE) {
			s_oLogger.debug("HttpUtils.newStandardHttpGETQuery: " + oE);
		}

		return oHttpCallResponse;
	}

	public static String standardHttpPOSTQuery(String sUrl, Map<String, String> asHeaders, String sPayload) {

		String sResult = null;
		try {
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();
			// optional default is GET
			oConnection.setRequestMethod("POST");
			oConnection.setRequestProperty("Accept", "*/*");

			if (asHeaders != null) {
				for (Entry<String, String> asEntry : asHeaders.entrySet()) {
					oConnection.setRequestProperty(asEntry.getKey(), asEntry.getValue());
				}
			}

			oConnection.setDoOutput(true);
			byte[] ayBytes = sPayload.getBytes();
			oConnection.setFixedLengthStreamingMode(ayBytes.length);
//			oConnection.setRequestProperty("Content-Type", "application/xml");
			oConnection.connect();
			try (OutputStream os = oConnection.getOutputStream()) {
				os.write(ayBytes);
			}

			Utils.debugLog("HttpUtils.standardHttpPOSTQuery: Sending 'POST' request to URL : " + sUrl);

			try {
				int responseCode = oConnection.getResponseCode();
				Utils.debugLog("HttpUtils.httpGetResults: Response Code : " + responseCode);
				String sResponseExtract = null;
				if (200 == responseCode) {
					InputStream oInputStream = oConnection.getInputStream();
					ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
					if (null != oInputStream) {
						Util.copyStream(oInputStream, oBytearrayOutputStream);
						sResult = oBytearrayOutputStream.toString();
					}

					if (sResult != null) {
						if (sResult.length() > 200) {
							sResponseExtract = sResult.substring(0, 200) + "...";
						} else {
							sResponseExtract = new String(sResult);
						}
						Utils.debugLog("HttpUtils.standardHttpPOSTQuery: Response extract: " + sResponseExtract);
					} else {
						Utils.debugLog("HttpUtils.standardHttpPOSTQuery: reponse is empty");
					}
				} else {
					Utils.debugLog("HttpUtils.standardHttpPOSTQuery: provider did not return 200 but "
							+ responseCode + " (1/2) and the following message:\n" + oConnection.getResponseMessage());
					ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
					InputStream oErrorStream = oConnection.getErrorStream();
					Util.copyStream(oErrorStream, oBytearrayOutputStream);
					String sMessage = oBytearrayOutputStream.toString();
					if (null != sMessage) {
						sResponseExtract = sMessage.substring(0, 200) + "...";
						Utils.debugLog(
								"HttpUtils.standardHttpPOSTQuery: provider did not return 200 but " + responseCode
										+ " (2/2) and this is the content of the error stream:\n" + sResponseExtract);
					}
				}
			} catch (Exception oEint) {
				Utils.debugLog("HttpUtils.standardHttpPOSTQuery: " + oEint);
			} finally {
				oConnection.disconnect();
			}

		} catch (Exception oE) {
			Utils.debugLog("HttpUtils.standardHttpPOSTQuery: " + oE);
		}
		return sResult;
	}

	public static HttpCallResponse newStandardHttpPOSTQuery(String sUrl, Map<String, String> asHeaders, String sPayload) {
		HttpCallResponse oHttpCallResponse = new HttpCallResponse();

		String sResult = null;
		try {
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();
			// optional default is GET
			oConnection.setRequestMethod("POST");
			oConnection.setRequestProperty("Accept", "*/*");

			if (asHeaders != null) {
				for (Entry<String, String> asEntry : asHeaders.entrySet()) {
					oConnection.setRequestProperty(asEntry.getKey(), asEntry.getValue());
				}
			}

			oConnection.setDoOutput(true);
			byte[] ayBytes = sPayload.getBytes();
			oConnection.setFixedLengthStreamingMode(ayBytes.length);
//			oConnection.setRequestProperty("Content-Type", "application/xml");
			oConnection.connect();
			try (OutputStream os = oConnection.getOutputStream()) {
				os.write(ayBytes);
			}

			Utils.debugLog("HttpUtils.newStandardHttpPOSTQuery: Sending 'POST' request to URL : " + sUrl);

			try {
				int iResponseCode = oConnection.getResponseCode();
				Utils.debugLog("HttpUtils.newStandardHttpPOSTQuery: Response Code : " + iResponseCode);
				
				oHttpCallResponse.setResponseCode(Integer.valueOf(iResponseCode));

				if (200 == iResponseCode) {
					InputStream oInputStream = oConnection.getInputStream();
					ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();

					if (null != oInputStream) {
						Util.copyStream(oInputStream, oBytearrayOutputStream);
						sResult = oBytearrayOutputStream.toString();
						oHttpCallResponse.setResponseBody(sResult);
					}
				} else {
					Utils.debugLog("HttpUtils.newStandardHttpPOSTQuery: provider did not return 200 but "
							+ iResponseCode + " (1/2) and the following message:\n" + oConnection.getResponseMessage());

					ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
					InputStream oErrorStream = oConnection.getErrorStream();
					Util.copyStream(oErrorStream, oBytearrayOutputStream);

					sResult = oBytearrayOutputStream.toString();
					oHttpCallResponse.setResponseBody(sResult);
				}
			} catch (Exception oEint) {
				Utils.debugLog("HttpUtils.newStandardHttpPOSTQuery: " + oEint);
			} finally {
				oConnection.disconnect();
			}

		} catch (Exception oE) {
			Utils.debugLog("HttpUtils.newStandardHttpPOSTQuery: " + oE);
		}

		return oHttpCallResponse;
	}

	/**
	 * Standard http post utility function
	 * @param sUrl url to call
	 * @param sPayload payload of the post 
	 * @param asHeaders headers dictionary
	 * @return server response
	 */
	public static String httpPost(String sUrl, String sPayload, Map<String, String> asHeaders) {
		return httpPost(sUrl, sPayload, asHeaders, null);
	}

	/**
	 * Standard http post utility function
	 * @param sUrl url to call
	 * @param sPayload payload of the post
	 * @param asHeaders headers dictionary
	 * @param sAuth in the form user:password (i.e., separated by a column: ':')
	 * @return server response
	 */
	public static String httpPost(String sUrl, String sPayload, Map<String, String> asHeaders, String sAuth) {

		try {
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();

			if(!Utils.isNullOrEmpty(sAuth)) {
				String sEncodedAuth = Base64.getEncoder().encodeToString(sAuth.getBytes(StandardCharsets.UTF_8));
				String sAuthHeaderValue = "Basic " + sEncodedAuth;
				oConnection.setRequestProperty("Authorization", sAuthHeaderValue);

			}

			oConnection.setDoOutput(true);
			oConnection.setRequestMethod("POST");

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
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}
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
			Utils.debugLog("Wasdi.httpPostFile: file not found");
			return false;
		}

		String sZippedFile = null;

		// Check if we need to zip this file
		if (!oFile.getName().toUpperCase().endsWith("ZIP")) {

			Utils.debugLog("HttpUtils.httpPostFile: File not zipped, zip it");

			int iRandom = new SecureRandom().nextInt() & Integer.MAX_VALUE;

			String sTemp = "tmp-" + iRandom + File.separator;
			String sTempPath = WasdiFileUtils.fixPathSeparator(oFile.getParentFile().getPath());

			if (!sTempPath.endsWith(File.separator)) {
				sTempPath += File.separator;
			}
			sTempPath += sTemp;

			Path oPath = Paths.get(sTempPath).toAbsolutePath().normalize();
			if (oPath.toFile().mkdir()) {
				Utils.debugLog("HttpUtils.httpPostFile: Temporary directory created");
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
			Utils.debugLog("HttpUtils.httpPostFile: file length is: " + Long.toString(lLen));

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
				Utils.debugLog("HttpUtils.httpPostFile: server returned " + iResponse);

				InputStream oResponseInputStream = null;

				ByteArrayOutputStream oByteArrayOutputStream = new ByteArrayOutputStream();

				if( 200 <= iResponse && 299 >= iResponse ) {
					oResponseInputStream = oConnection.getInputStream();
				} else {
					oResponseInputStream = oConnection.getErrorStream();
				}
				if(null!=oResponseInputStream) {
					Util.copyStream(oResponseInputStream, oByteArrayOutputStream);
					String sMessage = "HttpUtils.uploadFile: " + oByteArrayOutputStream.toString();
					Utils.debugLog(sMessage);
				} else {
					throw new NullPointerException("WasdiLib.uploadFile: stream is null");
				}

				oConnection.disconnect();

			} catch(Exception oE) {
				Utils.debugLog("HttpUtils.uploadFile( " + sUrl + ", " + sFileName + ", ...): internal exception: " + oE);
				return false;
			}
		} catch (Exception oE) {
			Utils.debugLog("HttpUtils.httpPostFile( " + sUrl + ", " + sFileName + ", ...): could not open file due to: " + oE + ", aborting");
			return false;
		}

		if (!Utils.isNullOrEmpty(sZippedFile)) {
			try {
				FileUtils.deleteDirectory(new File(sZippedFile).getParentFile());
			}
			catch (Exception oE) {
				Utils.debugLog("HttpUtils.httpPostFile( " + sUrl + ", " + sFileName + ", ...): could not delete temp zip file: " + oE + "");
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
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();

			if(!Utils.isNullOrEmpty(sAuth)) {
				String sEncodedAuth = Base64.getEncoder().encodeToString(sAuth.getBytes(StandardCharsets.UTF_8));
				String sAuthHeaderValue = "Basic " + sEncodedAuth;
				oConnection.setRequestProperty("Authorization", sAuthHeaderValue);
			}

			oConnection.setDoOutput(true);
			oConnection.setRequestMethod("PUT");

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
			oEx.printStackTrace();
			return "";
		}
	}

	/**
	 * Read http response stream
	 * @param oConnection
	 * @throws IOException
	 * @throws CopyStreamException
	 */
	public static String readHttpResponse(HttpURLConnection oConnection) {
		try {
			// response

			InputStream oResponseInputStream = null;
			try {
				oResponseInputStream = oConnection.getInputStream();
			} catch (Exception oE) {
				Utils.debugLog("HttpUtils.readHttpResponse: could not getInputStream due to: " + oE);
			}

			try {
				if (null == oResponseInputStream) {
					oResponseInputStream = oConnection.getErrorStream();
				}
			} catch (Exception oE) {
				Utils.debugLog("HttpUtils.readHttpResponse: could not getErrorStream due to: " + oE);
			}


			ByteArrayOutputStream oByteArrayOutputStream = new ByteArrayOutputStream();


			Util.copyStream(oResponseInputStream, oByteArrayOutputStream);
			String sMessage = oByteArrayOutputStream.toString();
			if (200 <= oConnection.getResponseCode() && 299 >= oConnection.getResponseCode()) {
				return sMessage;
			} else {
				Utils.debugLog("HttpUtils.readHttpResponse: status: " + oConnection.getResponseCode() + ", error message: " + sMessage);
				return "";
			}

		} catch (Exception oE) {
			Utils.debugLog("HttpUtils.readHttpResponse: exception: " + oE );
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

				Utils.debugLog("HttpUtils.getHttpResponseContentLength: File size = " + lLenght);
			} else {
				Utils.debugLog("HttpUtils.getHttpResponseContentLength: No file to download. Server replied HTTP code: " + responseCode);
			}
		} catch (IOException oE) {
			Utils.debugLog("HttpUtils.getHttpResponseContentLength: exception: " + oE );
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

		Utils.debugLog("HttpUtils." + sMethodName + " performance: " + dMillis + " ms, "
				+ iResponseSize + " B (" + dSpeed + " B/s)");
	}

	public static String obtainOpenidConnectToken(String sUrl, String sDownloadUser, String sDownloadPassword, String sClientId) {
		try {
			URL oURL = new URL(sUrl);

			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();
			oConnection.setRequestMethod("POST");
			oConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			oConnection.setDoOutput(true);
			oConnection.getOutputStream().write(("client_id=" + sClientId + "&password=" + sDownloadPassword + "&username=" + sDownloadUser + "&grant_type=password").getBytes());

			int iStatus = oConnection.getResponseCode();
			Utils.debugLog("HttpUtils.obtainOpenidConnectToken: Response status: " + iStatus);

			if (iStatus == 200) {
				InputStream oInputStream = oConnection.getInputStream();
				ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();

				if (null != oInputStream) {
					Util.copyStream(oInputStream, oBytearrayOutputStream);
					String sResult = oBytearrayOutputStream.toString();

					JSONObject oJson = new JSONObject(sResult);
					String sToken = oJson.optString("access_token", null);

					return sToken;
				}
			} else {
				ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
				InputStream oErrorStream = oConnection.getErrorStream();
				Util.copyStream(oErrorStream, oBytearrayOutputStream);

				String sMessage = oBytearrayOutputStream.toString();
				Utils.debugLog("HttpUtils.obtainOpenidConnectToken:" + sMessage);
			}
		} catch (Exception oE) {
			Utils.debugLog("HttpUtils.obtainOpenidConnectToken: " + oE);
		}

		return null;
	}

	/**
	 * Internal version of get 
	 * @param sUrl
	 * @return
	 */
	public static String httpGetResults(String sUrl) {
		Utils.debugLog("HttpUtils.httpGetResults( " + sUrl + " )");

		Map<String, String> asHeaders = new HashMap<>();

		long lStart = System.nanoTime();
		String sResult = standardHttpGETQuery(sUrl, asHeaders);
		long lEnd = System.nanoTime();

		HttpUtils.logOperationSpeed(sUrl, "httpGetResults", lStart, lEnd, sResult);

		return sResult;
	}

	public static String httpGetResults(String sUrl, String sDownloadUser, String sDownloadPassword) {
		Utils.debugLog("HttpUtils.httpGetResults( " + sUrl + " )");

		// Add the auth header
		String sAuth = sDownloadUser + ":" + sDownloadPassword;
		String sEncodedAuth = Base64.getEncoder().encodeToString(sAuth.getBytes(StandardCharsets.UTF_8));
		String sAuthHeaderValue = "Basic " + sEncodedAuth;

		Map<String, String> asHeaders = new HashMap<>();
		asHeaders.put("Authorization", sAuthHeaderValue);


		long lStart = System.nanoTime();
		String sResult = standardHttpGETQuery(sUrl, asHeaders);
		long lEnd = System.nanoTime();

		HttpUtils.logOperationSpeed(sUrl, "httpGetResults", lStart, lEnd, sResult);

		return sResult;
	}

}
