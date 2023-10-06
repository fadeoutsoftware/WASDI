package wasdi.shared.utils;

import java.io.*;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.net.io.Util;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient.Request;
import com.github.dockerjava.transport.DockerHttpClient.Response;

import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;


public class SocketUtils {
	
	
	/**
	 * @return a basic configuration of the docker client, mainly to manage the differences in the way we communicate with the
	 * docker daemon in different operating systems (named pipes in Windows, unix sockets in Linux) 
	 */
	private static DockerClientConfig getDockerClientConfig() {
		String osName = System.getProperty("os.name");
		
		String sDockerHost = "unix:///var/run/docker.sock";
		if (osName.toLowerCase().startsWith("windows")) {
			sDockerHost = "npipe:////./pipe/docker_engine";
		}
		
		// this is a very basic configuration of the docker client. The builder supports more options (see: https://github.com/docker-java/docker-java/blob/main/docs/getting_started.md)
		return DefaultDockerClientConfig.createDefaultConfigBuilder()
			    .withDockerHost(sDockerHost) 
			    .withDockerTlsVerify(false)
			    .build();			
	}
	

	private static DockerHttpClient getDockerHttpClient(DockerClientConfig oDockerClientConfig) {
		DockerHttpClient oHttpClient = new ApacheDockerHttpClient.Builder()
			    .dockerHost(oDockerClientConfig.getDockerHost())
			    .connectionTimeout(Duration.ofMillis(10000L)) //TODO: this can be replaced with: WasdiConfig.Current.connectionTimeout
			    .responseTimeout(Duration.ofMillis(10000L)) // TODO: this is a parameter that we do not set in the HttpUtils: do we want that?
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
	public static Request createRequest(Request.Method eMethod, String sPath, Map<String, String> asHeaders, byte [] ayPayloadBytes) {
		
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
	 * @param asHeaders Map of the headers to add to the http call
	 * @param aoOutputHeaders Map of response headers
	 * @return HttpCallResponse object that contains the Response Body and the Response Code
	 */
	public static HttpCallResponse httpGet(String sPath, Map<String, String> asHeaders, Map<String, List<String>> aoOutputHeaders) {
		
		HttpCallResponse oHttpCallResponse = new HttpCallResponse();

		String sResult = null;
		
		if (Utils.isNullOrEmpty(sPath)) {
			WasdiLog.errorLog("SocketUtils.httpGet: No Docker enpoint specified. Returning an empty result");
			return oHttpCallResponse;
		}
		
		DockerClientConfig oConfig = getDockerClientConfig();
		
		DockerHttpClient oHttpClient = getDockerHttpClient(oConfig);
			
		Request oRequest = createRequest(Request.Method.GET, sPath, asHeaders, null);

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
			WasdiLog.debugLog("SocketUtils.httpGet: Exception " + oEx);
		} finally {
			try {
				oHttpClient.close();
			} catch (IOException oEx) {
				WasdiLog.debugLog("SocketUtils.httpGet: Impossible to close the connection " + oEx);
			}
		}
		
		return oHttpCallResponse;
		
	}
	
	/**
	 * @param sPath a path to an endpoint in the Docker Engine API (see: https://docs.docker.com/engine/api/v1.42/). Eg. "containers/json")
	 * @param ayBytes the array of bytes to write in the payload of the request
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
		
		DockerClientConfig oConfig = getDockerClientConfig();
		
		DockerHttpClient oHttpClient = getDockerHttpClient(oConfig);
		
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
			WasdiLog.debugLog("SocketUtils.httpPost: Exception " + oEx);
		} finally {
			try {
				oHttpClient.close();
			} catch (IOException oEx) {
				WasdiLog.debugLog("SocketUtils.httpPost: Impossible to close the connection " + oEx);
			}
		}
		return oHttpCallResponse;
		
	}
	
	/**
	 * @param sPath a path to an endpoint in the Docker Engine API (see: https://docs.docker.com/engine/api/v1.42/). Eg. "containers/json")
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
		
		DockerClientConfig oConfig = getDockerClientConfig();
		
		DockerHttpClient oHttpClient = getDockerHttpClient(oConfig);	

		Request oRequest = createRequest(Request.Method.DELETE, sPath, asHeaders, null);
		
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
			WasdiLog.debugLog("SocketUtils.httpDelete: Exception " + oEx);
		} finally {
			try {
				oHttpClient.close();
			} catch (IOException oEx) {
				WasdiLog.debugLog("SocketUtils.httpDelete: Impossible to close the connection " + oEx);
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
			WasdiLog.errorLog("SocketUtils.httpDelete: No Docker enpoint specified. Returning an empty result");
			return oHttpCallResponse;
		}
		
		DockerClientConfig oConfig = getDockerClientConfig();
		
		DockerHttpClient oHttpClient = getDockerHttpClient(oConfig);	

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
			WasdiLog.debugLog("SocketUtils.httpDelete: Exception " + oEx);
		} finally {
			try {
				oHttpClient.close();
			} catch (IOException oEx) {
				WasdiLog.debugLog("SocketUtils.httpDelete: Impossible to close the connection " + oEx);
			}
		}

		return oHttpCallResponse;
	}
	
	
	public static void main (String [] args) throws Exception {
		
		// TRY GET
		
		// get the list or running container
		HttpCallResponse oRes = httpGet("/containers/json", null, null);
		System.out.println(oRes.getResponseCode());
		System.out.println(oRes.getResponseBody());
		
		// get details about a specific container
		String sContainerId = "59104ddc2870b7f98bbef12866b3a26845df1b50c7d7c9a18c33ce806547ec21";	
		HttpCallResponse oResInspect = httpGet("/containers/" + sContainerId + "/json", null, null);
		System.out.println(oResInspect.getResponseCode());
		System.out.println(oResInspect.getResponseBody());
		
		
		// TRY POST
		
		// we try to modify the value of the CPU quota for the container...
		Map<String, String> asHeaders = getStandardHeaderForRequestWithPayload();
		String sJsonBody = "{ \"CpuQuota\": 40000 }";
		byte[] oyBodyBytes = sJsonBody.getBytes();
		HttpCallResponse oResUpdate = httpPost("/containers/" + sContainerId + "/update", oyBodyBytes, asHeaders, null);
		System.out.println(oResUpdate.getResponseCode());
		System.out.println(oResUpdate.getResponseBody());
		
		
	}

}
