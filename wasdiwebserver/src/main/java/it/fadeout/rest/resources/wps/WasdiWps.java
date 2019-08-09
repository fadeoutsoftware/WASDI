/**
 * Created by Cristiano Nattero on 2019-03-04
 * 
 * Fadeout software
 *
 */
package it.fadeout.rest.resources.wps;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import wasdi.shared.utils.Utils;

/**
 * @author c.nattero
 *
 */
@Path("/wps")
public class WasdiWps {

	public WasdiWps() {
		Utils.debugLog("Hello WPS proxy");
	}
	
	@Inject
	WpsProxyFactory m_oWpsProxyFactory; 
	
	@Context
	ServletConfig m_oServletConfig;

	@GET
	@Produces({"application/xml", "text/xml"})
	public Response singleGet( @Context javax.ws.rs.core.UriInfo oUriInfo,
			@Context javax.ws.rs.core.HttpHeaders oHeaders ) {
		String sProvider = getProviderUrl(oUriInfo, oHeaders);
		WpsProxy oProxy = m_oWpsProxyFactory.get(sProvider);
		//TODO for developing purposes, get rid of this ASAP
		//oProxy.setProviderUrl("http://geoprocessing.demo.52north.org:8080/wps/WebProcessingService");
		String sParam = getParamList(oUriInfo);
		return oProxy.get(sParam, oHeaders);
	}
	
	@POST
	@Produces({"application/xml", "text/xml"})
	@Consumes({"application/xml", "text/xml"})
	public Response singlePost(@Context javax.ws.rs.core.UriInfo oUriInfo,
			@Context javax.ws.rs.core.HttpHeaders oHeaders,
			String sPayload ) {
		String sProvider = getProviderUrl(oUriInfo, oHeaders);
		WpsProxy oProxy = m_oWpsProxyFactory.get(sProvider);
		String sParam = getParamList(oUriInfo);
		//TODO for developing purposes, get rid of this ASAP
		//oProxy.setProviderUrl("http://geoprocessing.demo.52north.org:8080/wps/WebProcessingService");
		return oProxy.post(sParam, oHeaders, sPayload);
	}
	
	
	private String getProviderUrl(UriInfo oUriInfo, HttpHeaders oHeaders) {
		String sResult = null;
		if(null!=oHeaders) {
			//TODO change string into a variable, and read it from servlet config
			sResult = oHeaders.getRequestHeaders().getFirst("wpsProviderName"); 
			if(null==sResult) {
				sResult = getProviderUrl(oUriInfo);
			} //TODO else remove wpsProviderName from oHeaders
		}
		return sResult;
	}
	
	private String getProviderUrl(UriInfo oUriInfo) {
		MultivaluedMap<String, String> oQueryParamsMap = oUriInfo.getQueryParameters();
		String sResult = "";
		Set<String> sKeyset = oQueryParamsMap.keySet();
		//TODO change string into a variable, and read it from servlet config 
		if(sKeyset.contains("wpsProviderName")) {
			sResult = oQueryParamsMap.getFirst("wpsProviderName");
			//TODO remove wpsProviderName from oUriInfo
		}
		return sResult;
	}

	@GET
	@Path("/geoprocessing")
	@Produces({"application/xml", "text/xml"})
	public Response getGeoProcessing(@Context javax.ws.rs.core.UriInfo oUriInfo) {
		try {
			String sPassedBaseUrl = oUriInfo.getAbsolutePath().toString();
			String sBaseURL = getProviderUrl(oUriInfo);
			String sParams = getParamList(oUriInfo);
			
			//String sBaseURL = "http://geoprocessing.demo.52north.org:8080/wps/WebProcessingService";
			String sWpsRequest = sBaseURL+sParams;

			URL oUrl = new URL(sWpsRequest);
			HttpURLConnection oHttpURLConnection = (HttpURLConnection) oUrl.openConnection();
			oHttpURLConnection.setRequestMethod("GET");
			oHttpURLConnection.setRequestProperty("Content-Type", "application/xml");
			oHttpURLConnection.setDoOutput(true);
			int iStatus = oHttpURLConnection.getResponseCode();
			
			
			//TODO forward the response
			
			ResponseBuilder oResponseBuilder = null;
			if(iStatus <= 299) {
				String sResponse = getFullResponse(oHttpURLConnection);
				Utils.debugLog(sResponse);
				oResponseBuilder = Response.ok(sResponse);
			} else {
				oResponseBuilder = Response.status(iStatus);
			}
			return oResponseBuilder.build();
			

		} catch (Exception e) {
			e.printStackTrace();
		}


		//TODO instantiate an appropriate provider handler
		//TODO pass the request to the provider handler (and let it do its job)
		//TODO collect the response and pass it back to the client 
		return null;
	}
	
	@POST
	@Path("/geoprocessing")
	@Produces({"application/xml", "text/xml"})
	@Consumes(MediaType.TEXT_XML)
	public Response postGeoProcessing(@Context javax.ws.rs.core.UriInfo oUriInfo, String sPayload) {
		try {
			String sBaseURL = getProviderUrl(oUriInfo);
			String sParams = getParamList(oUriInfo);			
			String sWpsRequest = sBaseURL+sParams;


			URL oUrl = new URL(sWpsRequest);
			HttpURLConnection oHttpURLConnection = (HttpURLConnection) oUrl.openConnection();
			oHttpURLConnection.setRequestMethod("POST");
			

			//try this one instead of building the entire URL string directly
			//DataOutputStream out = new DataOutputStream(con.getOutputStream());
			//out.writeBytes(sParams);
			//out.flush();
			//out.close();

			oHttpURLConnection.setRequestProperty("Content-Type", "application/xml");
			oHttpURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0" );
			oHttpURLConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
			
			oHttpURLConnection.setDoOutput(true);

			int iStatus = oHttpURLConnection.getResponseCode();
			String sResponse = getFullResponse(oHttpURLConnection);
			Utils.debugLog(sResponse);
			ResponseBuilder oResponseBuilder = Response.ok(sResponse);
			return oResponseBuilder.build();

		} catch (Exception e) {
			e.printStackTrace();
		}


		//TODO instantiate an appropriate provider handler
		//TODO pass the request to the provider handler (and let it do its job)
		//TODO collect the response and pass it back to the client 
		return null;
	}

	protected String getParamList(javax.ws.rs.core.UriInfo oUriInfo) {
		MultivaluedMap<String, String> oQueryParamsMap = oUriInfo.getQueryParameters();
		String sParams = "?";
		Set<String> sKeyset = oQueryParamsMap.keySet();
		for (String sKey : sKeyset) {
			String sValue = oQueryParamsMap.getFirst(sKey);
			if(!sValue.startsWith("http")) {
				sParams+=sKey+"="+sValue+"&";
			}
		}
		if(sParams.endsWith("&")) {
			sParams = sParams.substring(0, sParams.length()-1);
		}
		return sParams;
	}

	public static String getFullResponse(HttpURLConnection oHttpURLConnection) {
		StringBuilder oFullResponseBuilder = new StringBuilder();
		try {
			BufferedReader oBufferedReader = new BufferedReader(
					new InputStreamReader(oHttpURLConnection.getInputStream()));
			String sInputLine = null;
			StringBuffer oStringBuffer = new StringBuffer();
			while ((sInputLine = oBufferedReader.readLine()) != null) {
				oStringBuffer.append(sInputLine);
			}
			oBufferedReader.close();
			oFullResponseBuilder.append(oStringBuffer);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return oFullResponseBuilder.toString();
	}

}
