/**
 * Created by Cristiano Nattero on 2019-03-04
 * 
 * Fadeout software
 *
 */
package it.fadeout.rest.resources.wps;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

/**
 * @author c.nattero
 *
 */
@Path("/wps")
public class WasdiWps {

	@Context
	ServletConfig m_oServletConfig;

	@GET
	@Path("geoprocessing")
	@Produces({"application/xml", "text/xml"})
	public Response getGeoProcessing(@Context javax.ws.rs.core.UriInfo oUriInfo) {
		try {
			String sParams = getParamList(oUriInfo);

			String sBaseURL = "http://geoprocessing.demo.52north.org:8080/wps/WebProcessingService";
			String sWpsRequest = sBaseURL+sParams;


			URL oUrl = new URL(sWpsRequest);
			HttpURLConnection oHttpURLConnection = (HttpURLConnection) oUrl.openConnection();
			oHttpURLConnection.setRequestMethod("GET");
			oHttpURLConnection.setDoOutput(true);

			//try this one instead of building the entire URL string directly
			//DataOutputStream out = new DataOutputStream(con.getOutputStream());
			//out.writeBytes(sParams);
			//out.flush();
			//out.close();

			oHttpURLConnection.setRequestProperty("Content-Type", "application/xml");

			int iStatus = oHttpURLConnection.getResponseCode();
			String sResponse = getFullResponse(oHttpURLConnection);
			System.out.println(sResponse);
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
			sParams+=sKey+"="+sValue+"&";
		}
		if(sParams.endsWith("&")) {
			sParams = sParams.substring(0, sParams.length()-1);
		}
		return sParams;
	}

	public static String getFullResponse(HttpURLConnection oHttpURLConnection) {
		StringBuilder oFullResponseBuilder = new StringBuilder();
		try {
/*
			// read status and message
			oFullResponseBuilder.append(oHttpURLConnection.getResponseCode())
			.append(" ")
			.append(oHttpURLConnection.getResponseMessage())
			.append("\n");

			
			// read headers
			oHttpURLConnection.getHeaderFields().entrySet().stream()
			.filter(entry -> entry.getKey() != null)
			.forEach(entry -> {
				oFullResponseBuilder.append(entry.getKey()).append(": ");
				List<String> asHeaderValues = entry.getValue();
				Iterator<String> oIterator = asHeaderValues.iterator();
				if (oIterator.hasNext()) {
					oFullResponseBuilder.append(oIterator.next());
					while (oIterator.hasNext()) {
						oFullResponseBuilder.append(", ").append(oIterator.next());
					}
				}
				oFullResponseBuilder.append("\n");
			});
*/
			// read response content
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

	//TODO replicate for each WPS provider


	//  Most of the query params are:	
	//	
	//	@QueryParam("request") String sRequest, //Mandatory: GetCapabilities, 
	//	@QueryParam("service") String sService, //Mandatory: GetCapabilities, 
	//	
	//	@QueryParam("version") String sVersion, //Optional: GetCapabilities,
	//	@QueryParam("AcceptVersions") String sAcceptVersions, //Optional
	//	@QueryParam("Language") String sLanguage, //
	//	@QueryParam("Identifier") String sIdentifier, //
	//	@QueryParam("DataInputs") String sDataInputs, //
	//	@QueryParam("ResponseDocument") String sResponseDocument, //
	//	@QueryParam("storeExecuteResponse") String sStoreExecuteResponse, //
	//	@QueryParam("lineage") String sLineage, //
	//	@QueryParam("status") String sStatus //



}
