package wasdi.shared.utils;

import java.io.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;

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
	
	private static DockerClientConfig getDockerClientConfig() {
		String osName = System.getProperty("os.name");
		
		String sDockerHost = "unix:///var/run/docker.sock";
		if (osName.toLowerCase().startsWith("windows")) {
			sDockerHost = "npipe:////./pipe/docker_engine";
		}
		
		return DefaultDockerClientConfig.createDefaultConfigBuilder()
			    .withDockerHost(sDockerHost) 
			    .withDockerTlsVerify(false)
			    .build();
				
	}
	
	
	public static HttpCallResponse httpGet(String sPath, Map<String, String> asHeaders, Map<String, List<String>> aoOutputHeaders) {
		
		HttpCallResponse oHttpCallResponse = new HttpCallResponse();

		String sResult = null;
		
		DockerClientConfig config = getDockerClientConfig();
		
		DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
			    .dockerHost(config.getDockerHost())
			    .connectionTimeout(Duration.ofMillis(10000L))
			    .responseTimeout(Duration.ofMillis(10000L))
			    .build();
		
		Request.Builder oRequestBuilder = Request.builder()
			    .method(Request.Method.GET)
			    .path(sPath); // e:g: "/containers/json"
		
				
		if (asHeaders != null) {
			oRequestBuilder.headers(asHeaders);
		}
		
		Request oRequest = oRequestBuilder.build();

		try (Response oResponse = httpClient.execute(oRequest)) {
			int iResponseCode = oResponse.getStatusCode();
			
			oHttpCallResponse.setResponseCode(Integer.valueOf(iResponseCode));
			
			
			if (aoOutputHeaders != null) {
				try {
					// get the response headers
					Map<String, List<String>> aoReceivedHeaders = oResponse.getHeaders();
					
					// Copy in the ouput dictionary
					for (Map.Entry<String, List<String>> oEntry : aoReceivedHeaders.entrySet()) {
						aoOutputHeaders.put(oEntry.getKey(), oEntry.getValue());
					}
					
				} catch(Exception oEx) {
					WasdiLog.errorLog("SocketUtils.httpGet: Exception getting the output headers ", oEx);
				}
			}

				// here we are not making a difference between a successful code or an error code. 
				InputStream oInputStream = oResponse.getBody();
				
				ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
				
				if (oInputStream != null) {
					Util.copyStream(oInputStream, oBytearrayOutputStream);
					sResult = oBytearrayOutputStream.toString();
					oHttpCallResponse.setResponseBody(sResult);
				}

			
		} catch (Exception oEx) {
			WasdiLog.debugLog("SocketUtils.httpGet: Exception " + oEx);
		} finally {
			try {
				httpClient.close();
			} catch (IOException oEx) {
				WasdiLog.debugLog("SocketUtils.httpGet: Impossible to close the connection " + oEx);
			}
		}
		
		return oHttpCallResponse;
		
	}
	
	public static void main (String [] args) throws Exception {
		HttpCallResponse oRes = httpGet("/containers/json", null, null);
		System.out.println(oRes.getResponseCode());
		System.out.println(oRes.getResponseBody());
	}

}
