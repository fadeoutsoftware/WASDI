package wasdi.shared.utils;

import java.io.*;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.io.Util;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient.Request;
import com.github.dockerjava.transport.DockerHttpClient.Response;

import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.config.WasdiConfig;


public class SocketUtils {
	
	
	/**
	 * Create an instance of a docker http client using the default configuration base address
	 * @return
	 */
	private static DockerHttpClient initializeDockerClient() {
		return initializeDockerClient(null);
	}
	
	/**
	 * Created an instance of a docker http client 
	 * @param sDockerHostAddress docker host address
	 * @return an instance of a docker HTTP client. Null if an error occurs.
	 */
	private static DockerHttpClient initializeDockerClient(String sDockerHostAddress) {
		
		DockerHttpClient oHttpClient = null;
		
		try {
			DockerClientConfig oConfig =  (Utils.isNullOrEmpty(sDockerHostAddress)) 
					? getDockerClientConfig()
					: getDockerClientConfig(sDockerHostAddress);
			
			oHttpClient = getDockerHttpClient(oConfig);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SocketUtils.initializeDockerClient. Error in the initialization of the Docker HTTP client." + oEx.getMessage());
		}
		
		return oHttpClient;
	}
	
	/**
	 * created a DockerClientConfig reading the docker host address from the wasdi config file
	 */
	private static DockerClientConfig getDockerClientConfig() {
		String sDockerHost = WasdiConfig.Current.dockers.internalDockerAPIAddress;
		return getDockerClientConfig(sDockerHost);
	}
	
	
	/**
	 * @param sDockerHost: the socket address of the docker host
	 * @return a basic configuration of the docker client, with the host set to the address being passed as parameter
	 */
	private static DockerClientConfig getDockerClientConfig(String sDockerHost) {
		// this is a very basic configuration of the docker client. The builder supports more options (see: https://github.com/docker-java/docker-java/blob/main/docs/getting_started.md)
		return DefaultDockerClientConfig.createDefaultConfigBuilder()
			    .withDockerHost(sDockerHost) 
			    .withDockerTlsVerify(false)
			    .build();			
	}
	
	
	/**
	 * Create an instance of the Docker Http Client based on Apache HttpClient package
	 * @param oDockerClientConfig the configuration for the client
	 * @return an instance of the Docker Http client
	 */
	private static DockerHttpClient getDockerHttpClient(DockerClientConfig oDockerClientConfig) {
		DockerHttpClient oHttpClient = new ApacheDockerHttpClient.Builder()
			    .dockerHost(oDockerClientConfig.getDockerHost())
			    .connectionTimeout(Duration.ofMillis(WasdiConfig.Current.connectionTimeout))
			    .build();
		
		return oHttpClient;
	}
	
	/**
	 * standard header that must be specified when preparing a POST request with body
	 * @return the map containing the header
	 */
	public static Map<String, String> getStandardHeaderForRequestWithPayload() {
		Map<String, String> asHeaders = new HashMap<>();
		asHeaders.put("Content-Type", "application/json");
		return asHeaders;
	}
	
	
	/**
	 * Build a request to send to the Docker Engine
	 * @param eMethod http method
	 * @param sPath path to the endpoint
	 * @param asHeaders map with the request headers
	 * @asParams asQueryParams map with query parameters
	 * @param ayPayloadBytes bytes of the payload 
	 * @return
	 */
	protected static Request createRequest(Request.Method eMethod, String sPath, Map<String, String> asHeaders, byte [] ayPayloadBytes) {
				
		Request.Builder oRequestBuilder = Request.builder()
			    .method(eMethod)
			    .path(sPath); // e:g: "/containers/json"
		
		if (asHeaders != null && !asHeaders.isEmpty()) {
			oRequestBuilder.headers(asHeaders);
		}
		
		if (ayPayloadBytes != null && ayPayloadBytes.length > 0) {
			oRequestBuilder.bodyBytes(ayPayloadBytes);
		}
		
		return oRequestBuilder.build();
	}
	
	/**
	 * Build a request to send to the Docker Engine
	 * @param eMethod http method
	 * @param sPath path to the endpoint
	 * @param asHeaders map with the request headers
	 * @asParams asQueryParams map with query parameters
	 * @param ayPayloadInStream input stream of the payload 
	 * @return
	 */
	protected static Request createRequest(Request.Method eMethod, String sPath, Map<String, String> asHeaders, InputStream ayPayloadInStream) {
				
		Request.Builder oRequestBuilder = Request.builder()
			    .method(eMethod)
			    .path(sPath); // e:g: "/containers/json"
		
		if (asHeaders != null && !asHeaders.isEmpty()) {
			oRequestBuilder.headers(asHeaders);
		}
		
		if (ayPayloadInStream != null) {
			oRequestBuilder.body(ayPayloadInStream);
		}
		
		return oRequestBuilder.build();
	}
	
	
	public static void copyHeaders(Map<String, List<String>> aoReceivedHeaders, Map<String, List<String>> aoOutputHeaders) {
		try {		
			if (aoReceivedHeaders != null && !aoReceivedHeaders.isEmpty()) {
				// Copy in the ouput dictionary
				for (Map.Entry<String, List<String>> oEntry : aoReceivedHeaders.entrySet()) {
					aoOutputHeaders.put(oEntry.getKey(), oEntry.getValue());
				}
			}		
		} catch(Exception oEx) {
			WasdiLog.errorLog("SocketUtils.copyHeaders: Exception getting the output headers ", oEx);
		}
	}
	
	/**
	 * @param sPath a path to an endpoint in the Docker Engine API (see: https://docs.docker.com/engine/api/v1.42/). Eg. "containers/json")
	 * @return HttpCallResponse object that contains the Response Body and the Response Code
	 */
	public static HttpCallResponse httpGet(String sPath) {
		return httpGet(sPath, null, null);
	}
	
	/**
	 * @param sPath a path to an endpoint in the Docker Engine API (see: https://docs.docker.com/engine/api/v1.42/). Eg. "containers/json")
	 * @param sDockerHostAddress unix socket address of the docker host
	 * @param asHeaders Map of the headers to add to the http call
	 * @param aoOutputHeaders Map of response headers
	 * @return HttpCallResponse object that contains the Response Body and the Response Code
	 */
	public static HttpCallResponse httpGet(String sPath, Map<String, String> asHeaders, Map<String, List<String>> aoOutputHeaders) {
		
		HttpCallResponse oHttpCallResponse = new HttpCallResponse();

		String sResult = null;
		
		if (Utils.isNullOrEmpty(sPath)) {
			WasdiLog.errorLog("SocketUtils.httpGet: No Docker enpoint specified. Returning an empty result.");
			return oHttpCallResponse;
		}
		
		DockerHttpClient oHttpClient = initializeDockerClient();
		
		if (oHttpClient == null) {
			WasdiLog.errorLog("SocketUtils.httpGet. Docker HTTP client is null. Returning an empty response.");
			return oHttpCallResponse;
		}
		
		String sBaseAddress = WasdiConfig.Current.dockers.internalDockerAPIAddress;
		if (sBaseAddress.endsWith("/")) sBaseAddress = sBaseAddress.substring(0, sBaseAddress.length()-1);
		sPath = sPath.substring(sBaseAddress.length());		
			
		byte[] ayEmptyPayload = {};
		Request oRequest = createRequest(Request.Method.GET, sPath, asHeaders, ayEmptyPayload);

		try (Response oResponse = oHttpClient.execute(oRequest)) {
			int iResponseCode = oResponse.getStatusCode();
			
			if (WasdiConfig.Current.logHttpCalls || (iResponseCode<200||iResponseCode>299)) {
				WasdiLog.debugLog("SocketUtils.httpGet: Response code " + iResponseCode);
			}
			
			oHttpCallResponse.setResponseCode(Integer.valueOf(iResponseCode));
			
			if (aoOutputHeaders != null) {
				copyHeaders(oResponse.getHeaders(), aoOutputHeaders);
			}

			// here we are not making a difference between a successful code or an error code. 
			InputStream oInputStream = oResponse.getBody();
			
			ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
			
			if (oInputStream != null) {
				Util.copyStream(oInputStream, oBytearrayOutputStream);
				sResult = oBytearrayOutputStream.toString();
				oHttpCallResponse.setResponseBody(sResult);
				oInputStream.close();
			}
		
		} catch (Exception oEx) {
			WasdiLog.errorLog("SocketUtils.httpGet: Exception ", oEx);
		} finally {
			try {
				oHttpClient.close();
			} catch (IOException oEx) {
				WasdiLog.errorLog("SocketUtils.httpGet: Impossible to close the connection ", oEx);
			}
		}
		
		return oHttpCallResponse;
		
	}
	
	/**
	 * @param sPath a path to an endpoint in the Docker Engine API (see: https://docs.docker.com/engine/api/v1.42/). Eg. "containers/json")
	 * @param sPayload the POST request' body
	 * @param asHeaders map of request headers 
	 * @return HttpCallResponse object that contains the Response Body and the Response Code
	 */
	public static HttpCallResponse httpPost(String sPath, String sPayload, Map<String, String> asHeaders) {
		if (sPayload == null) sPayload = "";
		return httpPost(sPath, sPayload.getBytes(), asHeaders, null);
	}
	
	/**
	 * @param sPath a path to an endpoint in the Docker Engine API (see: https://docs.docker.com/engine/api/v1.42/). Eg. "containers/json")
	 * @param ayBytes the array of bytes to write in the POST request's body
	 * @param asHeaders map of request headers 
	 * @return HttpCallResponse object that contains the Response Body and the Response Code
	 */
	public static HttpCallResponse httpPost(String sPath, byte []ayBytes, Map<String, String> asHeaders) {
		return httpPost(sPath, ayBytes, asHeaders, null);
	}
	
	/**
	 * @param sPath a path to an endpoint in the Docker Engine API (see: https://docs.docker.com/engine/api/v1.42/). Eg. "containers/json")
	 * @param sPayload the POST request' body
	 * @return HttpCallResponse object that contains the Response Body and the Response Code
	 */
	public static HttpCallResponse httpPost(String sPath, String sPayload) {
		if (sPayload == null) sPayload = "";
		return httpPost(sPath, sPayload.getBytes(), null, null);
	}
	
	
	public static HttpCallResponse httpPost(String sPath, File oFile, Map<String, String> asHeaders) {
		HttpCallResponse oHttpCallResponse = new HttpCallResponse();
		
		String sResult = null;
		
		if (Utils.isNullOrEmpty(sPath)) {
			WasdiLog.errorLog("SocketUtils.httpPost: No Docker enpoint specified. Returning an empty result");
			return oHttpCallResponse;
		}
		
		DockerHttpClient oHttpClient = initializeDockerClient();
		
		if (oHttpClient == null) {
			WasdiLog.errorLog("SocketUtils.httpPost. Docker HTTP client is null. Returning an empty response.");
			return oHttpCallResponse;
		}
		
		String sBaseAddress = WasdiConfig.Current.dockers.internalDockerAPIAddress;
		if (sBaseAddress.endsWith("/")) sBaseAddress = sBaseAddress.substring(0, sBaseAddress.length()-1);
		sPath = sPath.substring(sBaseAddress.length());
		
		// WasdiLog.debugLog("SocketUtils.httpPost: computed path " + sPath);
		
		Request.Builder oRequestBuilder = Request.builder()
			    .method(Request.Method.POST)
			    .path(sPath);
		
		if (asHeaders != null && !asHeaders.isEmpty()) {
			oRequestBuilder.headers(asHeaders);
		}
		
		InputStream oFileInputStream = null;
		
		try {
			oFileInputStream = FileUtils.openInputStream(oFile);
		} catch (IOException e) {
			WasdiLog.errorLog("SocketUtils.httpPost. Error providing the input stream. Returning an empty response.");
			return oHttpCallResponse;
		}
		
		oRequestBuilder = oRequestBuilder.body(oFileInputStream);
		
		Request oRequest = oRequestBuilder.build();
				
		try (Response oResponse = oHttpClient.execute(oRequest)) {
			
			int iResponseCode = oResponse.getStatusCode();
						
			oHttpCallResponse.setResponseCode(Integer.valueOf(iResponseCode));

			// here we are not making a difference between a successful code or an error code. 
			InputStream oResponseStream = oResponse.getBody();
			
			ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
						
			if (oResponseStream != null) {
				Util.copyStream(oResponseStream, oBytearrayOutputStream);
				sResult = oBytearrayOutputStream.toString();
				oHttpCallResponse.setResponseBody(sResult);
				oResponseStream.close();
			}
						
		} catch (Exception oEx) {
			WasdiLog.debugLog("SocketUtils.httpPost: Exception " + oEx.getMessage());
		} finally {
			try {
				oHttpClient.close();
			} catch (IOException oEx) {
				WasdiLog.errorLog("SocketUtils.httpPost: Impossible to close the connection ", oEx);
			}
		}
		return oHttpCallResponse;		
	}
	/**
	 * @param sPath a path to an endpoint in the Docker Engine API (see: https://docs.docker.com/engine/api/v1.42/). Eg. "containers/json")
	 * @param sDockerHostAddress unix socket address of the docker host
	 * @param ayBytes the array of bytes to write in the POST request's body
	 * @param asHeaders map of request headers 
	 * @param aoOutputHeaders Map of response headers
	 * @return HttpCallResponse object that contains the Response Body and the Response Code
	 */
	public static HttpCallResponse httpPost(String sPath, byte []ayBytes, Map<String, String> asHeaders, Map<String, List<String>> aoOutputHeaders) {
		
		HttpCallResponse oHttpCallResponse = new HttpCallResponse();
		
		String sResult = null;
		
		if (Utils.isNullOrEmpty(sPath)) {
			WasdiLog.errorLog("SocketUtils.httpPost: No Docker enpoint specified. Returning an empty result");
			return oHttpCallResponse;
		}
		
		DockerHttpClient oHttpClient = initializeDockerClient();
		
		if (oHttpClient == null) {
			WasdiLog.errorLog("SocketUtils.httpPost. Docker HTTP client is null. Returning an empty response.");
			return oHttpCallResponse;
		}
		
		String sBaseAddress = WasdiConfig.Current.dockers.internalDockerAPIAddress;
		if (sBaseAddress.endsWith("/")) sBaseAddress = sBaseAddress.substring(0, sBaseAddress.length()-1);
		sPath = sPath.substring(sBaseAddress.length());
		
		// WasdiLog.debugLog("SocketUtils.httpPost: computed path " + sPath);
		
		Request oRequest = createRequest(Request.Method.POST, sPath, asHeaders, ayBytes);
				
		try (Response oResponse = oHttpClient.execute(oRequest)) {
			int iResponseCode = oResponse.getStatusCode();
						
			oHttpCallResponse.setResponseCode(Integer.valueOf(iResponseCode));
			
			if (aoOutputHeaders != null) {
				copyHeaders(oResponse.getHeaders(), aoOutputHeaders);
			}

			// here we are not making a difference between a successful code or an error code. 
			InputStream oInputStream = oResponse.getBody();
			
			ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
						
			if (oInputStream != null) {
				Util.copyStream(oInputStream, oBytearrayOutputStream);
				sResult = oBytearrayOutputStream.toString();
				oHttpCallResponse.setResponseBody(sResult);
				oInputStream.close();
			}
						
		} catch (Exception oEx) {
			WasdiLog.debugLog("SocketUtils.httpPost: Exception " + oEx.getMessage());
		} finally {
			try {
				oHttpClient.close();
			} catch (IOException oEx) {
				WasdiLog.errorLog("SocketUtils.httpPost: Impossible to close the connection ", oEx);
			}
		}
		return oHttpCallResponse;
		
	}
	
	/**
	 * @param sPath a path to an endpoint in the Docker Engine API (see: https://docs.docker.com/engine/api/v1.42/). Eg. "containers/json")
	 * @return HttpCallResponse object that contains the Response Body and the Response Code
	 */
	public static HttpCallResponse httpDelete(String sPath) {
		return httpDelete(sPath, null);
	}
	
	/**
	 * @param sPath a path to an endpoint in the Docker Engine API (see: https://docs.docker.com/engine/api/v1.42/). Eg. "containers/json")
	 * @param sDockerHostAddress unix socket address of the docker host
	 * @param asHeaders headers dictionary
	 * @return HttpCallResponse object that contains the Response Body and the Response Code
	 */
	public static HttpCallResponse httpDelete(String sPath, Map<String, String> asHeaders) {
		
		HttpCallResponse oHttpCallResponse = new HttpCallResponse();
		
		String sResult = null;
		
		if (Utils.isNullOrEmpty(sPath)) {
			WasdiLog.errorLog("SocketUtils.httpDelete: No Docker enpoint specified. Returning an empty result");
			return oHttpCallResponse;
		}
		
		DockerHttpClient oHttpClient = initializeDockerClient();
		
		if (oHttpClient == null) {
			WasdiLog.errorLog("SocketUtils.httpDelete. Docker HTTP client is null. Returning an empty response.");
			return oHttpCallResponse;
		}	
		
		String sBaseAddress = WasdiConfig.Current.dockers.internalDockerAPIAddress;
		if (sBaseAddress.endsWith("/")) sBaseAddress = sBaseAddress.substring(0, sBaseAddress.length()-1);
		sPath = sPath.substring(sBaseAddress.length());		

		byte[] ayEmptyPayload = {};
		Request oRequest = createRequest(Request.Method.DELETE, sPath, asHeaders, ayEmptyPayload);
		
		try (Response oResponse = oHttpClient.execute(oRequest)) {
			int iResponseCode = oResponse.getStatusCode();
			
			oHttpCallResponse.setResponseCode(Integer.valueOf(iResponseCode));
			
			
			// here we are not making a difference between a successful code or an error code. 
			InputStream oInputStream = oResponse.getBody();
			
			ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
			
			if (oInputStream != null) {
				Util.copyStream(oInputStream, oBytearrayOutputStream);
				sResult = oBytearrayOutputStream.toString();
				oHttpCallResponse.setResponseBody(sResult);
				oInputStream.close();
			}

			
		} catch (Exception oEx) {
			WasdiLog.debugLog("SocketUtils.httpDelete: Exception " + oEx.getMessage());
		} finally {
			try {
				oHttpClient.close();
			} catch (IOException oEx) {
				WasdiLog.errorLog("SocketUtils.httpDelete: Impossible to close the connection " + oEx);
			}
		}

		return oHttpCallResponse;
	}
	
	/**
	 * @param sPath a path to an endpoint in the Docker Engine API (see: https://docs.docker.com/engine/api/v1.42/). Eg. "containers/json")
	 * @param asHeaders headers dictionary
	 * @return HttpCallResponse object that contains the Response Body and the Response Code
	 */
	public static HttpCallResponse httpPut(String sPath, Map<String, String> asHeaders, byte[] ayBytes) {
		
		HttpCallResponse oHttpCallResponse = new HttpCallResponse();
		
		String sResult = null;
		
		if (Utils.isNullOrEmpty(sPath)) {
			WasdiLog.errorLog("SocketUtils.httpPut: No Docker enpoint specified. Returning an empty result");
			return oHttpCallResponse;
		}
		
		DockerHttpClient oHttpClient = initializeDockerClient();
		
		if (oHttpClient == null) {
			WasdiLog.errorLog("SocketUtils.httpPut. Docker HTTP client is null. Returning an empty response.");
			return oHttpCallResponse;
		}	

		String sBaseAddress = WasdiConfig.Current.dockers.internalDockerAPIAddress;
		if (sBaseAddress.endsWith("/")) sBaseAddress = sBaseAddress.substring(0, sBaseAddress.length()-1);
		sPath = sPath.substring(sBaseAddress.length());
		
		Request oRequest = createRequest(Request.Method.PUT, sPath, asHeaders, ayBytes);
		
		try (Response oResponse = oHttpClient.execute(oRequest)) {
			int iResponseCode = oResponse.getStatusCode();
			
			oHttpCallResponse.setResponseCode(Integer.valueOf(iResponseCode));
			
			
			// here we are not making a difference between a successful code or an error code. 
			InputStream oInputStream = oResponse.getBody();
			
			ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
			
			if (oInputStream != null) {
				Util.copyStream(oInputStream, oBytearrayOutputStream);
				sResult = oBytearrayOutputStream.toString();
				oHttpCallResponse.setResponseBody(sResult);
				oInputStream.close();
			}

			
		} catch (Exception oEx) {
			WasdiLog.errorLog("SocketUtils.httpPut: Exception ", oEx);
		} finally {
			try {
				oHttpClient.close();
			} catch (IOException oEx) {
				WasdiLog.errorLog("SocketUtils.httpPut: Impossible to close the connection ", oEx);
			}
		}

		return oHttpCallResponse;
	}
	
	/**
	 * 
	 * @param sPath path to the Docker API Engine endpoint
	 * @param sFileName path to the TAR file, containing the DockerFile in its root
	 * @param asHeaders
	 * @return HttpCallResponse object that contains the Response Body and the Response Code
	 */
	public static HttpCallResponse httpPostFile(String sPath, String sFileName, Map<String, String> asHeaders) {
		
		HttpCallResponse oHttpResponse = new HttpCallResponse();
		
		File oFile = new File(sFileName);
		if (!oFile.exists()) {
			WasdiLog.errorLog("SocketUtils.httpPostFile: file not found.");
			return oHttpResponse;
		}

		DockerHttpClient oHttpClient = null;
		
		try {
			
			oHttpClient = initializeDockerClient();
			
			if (oHttpClient == null) {
				WasdiLog.errorLog("SocketUtils.httpPostFile. Docker HTTP client is null. Returning an empty response.");
				return oHttpResponse;
			}	
			
			try (InputStream oFileInStream = new FileInputStream(oFile)) {
			
				String sBaseAddress = WasdiConfig.Current.dockers.internalDockerAPIAddress;
				if (sBaseAddress.endsWith("/")) sBaseAddress = sBaseAddress.substring(0, sBaseAddress.length()-1);
				sPath = sPath.substring(sBaseAddress.length());			
				
				Request oRequest = createRequest(Request.Method.POST, sPath, null, oFileInStream);
			
				try (Response oResponse = oHttpClient.execute(oRequest)) {
					
					int iResponseCode = oResponse.getStatusCode();
					
					oHttpResponse.setResponseCode(Integer.valueOf(iResponseCode));
					
					
					// here we are not making a difference between a successful code or an error code. 
					InputStream oInputStreamBody = oResponse.getBody();
					
					ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
					
					String sResult = "";
					if (oInputStreamBody != null) {
						Util.copyStream(oInputStreamBody, oBytearrayOutputStream);
						sResult = oBytearrayOutputStream.toString();
						oHttpResponse.setResponseBody(sResult);
						oInputStreamBody.close();
					}
	
					
				} catch (Exception oEx) {
					WasdiLog.errorLog("SocketUtils.httpPostFile: Exception when trying to execute the request", oEx);
				} 	
			}
			
		} catch (Exception oE) {
			WasdiLog.debugLog("SocketUtils.httpPostFile: could not open file due to: " + oE.getMessage() + ", aborting");
		} finally {
			try {
				if (oHttpClient != null) oHttpClient.close();
			} catch (IOException oEx) {
				WasdiLog.errorLog("SocketUtils.httpPut: Impossible to close the connection ", oEx);
			}
		}	
		
		return oHttpResponse;
		
	}
	
	
}
