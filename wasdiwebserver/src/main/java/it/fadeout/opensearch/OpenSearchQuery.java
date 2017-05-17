package it.fadeout.opensearch;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Base64;

import javax.imageio.ImageIO;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Base;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;
import org.apache.abdera.parser.Parser;
import org.apache.abdera.parser.ParserOptions;
import org.apache.abdera.protocol.Response.ResponseType;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.RequestOptions;
import org.apache.abdera.writer.Writer;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

public class OpenSearchQuery{


	//String sParameter = URLEncoder.encode("( beginPosition:[2016-10-03T00:00:00.000Z TO 2016-10-06T23:59:59.999Z] AND endPosition:[2016-10-03T00:00:00.000Z TO 2016-10-06T23:59:59.999Z] ) AND   (platformname:Sentinel-1 AND filename:S1A_* AND producttype:SLC)&offset=0&limit=25", "UTF-8");
	//String sUrl = "https://scihub.copernicus.eu/dhus/api/stub/products/count?filter=";

	public static String ExecuteQuery(String sQuery, HashMap<String, String> asParams) throws URISyntaxException, IOException
	{
		try {
//			String sParameter = URLEncoder.encode(sQuery, "UTF-8");
			//String sParameter = "'( beginPosition:[2016-10-03T00:00:00.000Z TO 2016-10-06T23:59:59.999Z] AND endPosition:[2016-10-03T00:00:00.000Z TO 2016-10-06T23:59:59.999Z] ) AND   (platformname:Sentinel-1 AND filename:S1A_* AND producttype:SLC)'";
//			sParameter = URLEncoder.encode(sParameter, "UTF-8");
			Abdera oAbdera = new Abdera();
//			String sJsonResult = new String("");
			//create url passing search parameters
			String sUrl = OpenSearchTemplate.getHttpUrl(sQuery, 
					asParams.getOrDefault("offset", "0"), 
					asParams.getOrDefault("limit", "25"), 
					asParams.getOrDefault("sortedby", "ingestiondate"),
					asParams.getOrDefault("order", "asc"),
					asParams.getOrDefault("provider", "scihub.copernicus.eu"));
			//create abdera client
			AbderaClient oClient = new AbderaClient(oAbdera);
			oClient.setConnectionTimeout(15000);
			oClient.setSocketTimeout(40000);
			oClient.setConnectionManagerTimeout(20000);
			oClient.setMaxConnectionsTotal(200);
			oClient.setMaxConnectionsPerHost(50);

			// get default request option
			RequestOptions oOptions = oClient.getDefaultRequestOptions();			
			
			// execute request and get the document (atom+xml format)
			
			Parser oParser = oAbdera.getParser();
			ParserOptions oParserOptions = oParser.getDefaultParserOptions();

			oParserOptions.setCharset("UTF-8");
			//options.setCompressionCodecs(CompressionCodec.GZIP);
			oParserOptions.setFilterRestrictedCharacterReplacement('_');
			oParserOptions.setFilterRestrictedCharacters(true);
			oParserOptions.setMustPreserveWhitespace(false);
			oParserOptions.setParseFilter(null);
			//ListParseFilter filter = new WhiteListParseFilter();
			// set authorization
			String sUserCredentials = asParams.getOrDefault("OSUser", "") + ":" + asParams.getOrDefault("OSPwd", "");
			String sBasicAuth = "Basic " + Base64.getEncoder().encodeToString(sUserCredentials.getBytes());
			oOptions.setAuthorization(sBasicAuth);
			
			
			System.out.println("\nSending 'GET' request to URL : " + sUrl);
			ClientResponse response = oClient.get(sUrl, oOptions);
			
			
			Document<Feed> oDocument = null;
			
			
			if (response.getType() == ResponseType.SUCCESS)
			{
				System.out.println("Response Success");
				
				
				// Get The Result as a string
				BufferedReader oBuffRead = new BufferedReader(response.getReader());
				String sResponseLine = null;
				StringBuilder oResponseStringBuilder = new StringBuilder();
				while ((sResponseLine = oBuffRead.readLine()) != null) {
				    oResponseStringBuilder.append(sResponseLine);
				}
				
				String sResultAsString = oResponseStringBuilder.toString();
				
				System.out.println(sResultAsString);

				oDocument = oParser.parse(new StringReader(sResultAsString), oParserOptions);
				//oDocument = oParser.parse(response.getInputStream(), oParserOptions);

				if (oDocument == null) {
					System.out.println("OpenSearchQuery.ExecuteQuery: Document response null");
					return null;
				}
			}
			else
			{
				System.out.println("Response ERROR: " + response.getType());
				return null;
			}

			int iStreamSize = 1000000;
			Feed oFeed = (Feed) oDocument.getRoot();

			//set new connction timeout
			oClient.setConnectionTimeout(2000);
			//oClient.setSocketTimeout(2000);
			oClient.setConnectionManagerTimeout(2000);

			for (Entry oEntry : oFeed.getEntries()) {

				System.out.println("Parsing new Entry");

				Link oLink = oEntry.getLink("icon");
				
				if (oLink != null) {
					System.out.println("Icon Link: " + oLink.getHref().toString());

					try {
						ClientResponse oImageResponse = oClient.get(oLink.getHref().toString(), oOptions);
						System.out.println("Response Got from the client");
						if (oImageResponse.getType() == ResponseType.SUCCESS)
						{
							System.out.println("Success: saving image preview");
							InputStream oInputStreamImage = oImageResponse.getInputStream();
							BufferedImage  oImage = ImageIO.read(oInputStreamImage);
							ByteArrayOutputStream bas = new ByteArrayOutputStream();
							ImageIO.write(oImage, "png", bas);
							oLink.addSimpleExtension(new javax.xml.namespace.QName("image"), "data:image/png;base64," + Base64.getEncoder().encodeToString((bas.toByteArray())));
							System.out.println("Image Saved");
						}				
					}
					catch (Exception e) {
						System.out.println("Image Preview Cycle Exception " + e.toString());
					}					
				}
				else {
					System.out.println("Link Not Available" );
				}
			} 
			JSONObject oFeedJSON = Atom2Json(oAbdera, new ByteArrayOutputStream(iStreamSize), oFeed);

			System.out.println("Search Done");

			return oFeedJSON.toString();

		}
		catch (Exception e) {
			System.out.println("ExecuteQuery Exception " + e.toString());
		}

		return "";
	}

	
	/*
	public static String ExecuteQuerySentinel(String sQuery, String[] aoParams) throws URISyntaxException, IOException
	{

		HttpClient oClient = null;
		try {
			String sUrl = "https://scihub.copernicus.eu/dhus/api/stub/products?filter=";
			String sParameter = URLEncoder.encode(sQuery, "UTF-8");
			sUrl += sParameter;  
			//Add params
			for(int iParamCount = 0; iParamCount < aoParams.length;iParamCount++)
			{
				sUrl+='&' + aoParams[iParamCount];
			}
			oClient = new DefaultHttpClient();
			HttpGet request = new HttpGet(sUrl);
			request.addHeader("Authorization", "Basic c2FkYW1vOmVzYTE1YWRhbQ==");
			HttpResponse response = oClient.execute(request);
			StringBuffer responseBuffer = new StringBuffer();
			// Get the response
			BufferedReader rd = new BufferedReader
			        (new InputStreamReader(
			        response.getEntity().getContent()));

			String line = "";
			while ((line = rd.readLine()) != null) {
				responseBuffer.append(line);
			}

			return responseBuffer.toString();			
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return "";
	}
	 */

	private static JSONObject Atom2Json(Abdera oAbdera, ByteArrayOutputStream oOutputStream, Base oFeed) throws IOException{
		Writer writer = oAbdera.getWriterFactory().getWriter("prettyxml");
		writer.writeTo(oFeed, oOutputStream);
		
		String sXML = oOutputStream.toString();
		System.out.println(sXML);
		
		JSONObject oJson = XML.toJSONObject(sXML);
		return oJson;
	}

	public static String ExecuteQueryCount(String sQuery, String sOSUser, String sOSPwd, String sProvider) throws URISyntaxException, IOException
	{
		String sParameter = URLEncoder.encode(sQuery, "UTF-8");
		//String sParameter = "'( beginPosition:[2016-10-03T00:00:00.000Z TO 2016-10-06T23:59:59.999Z] AND endPosition:[2016-10-03T00:00:00.000Z TO 2016-10-06T23:59:59.999Z] ) AND   (platformname:Sentinel-1 AND filename:S1A_* AND producttype:SLC)&offset=0&limit=25'";
		String sUrl = "";
		if (sProvider.equals("SENTINEL"))
			sUrl = "https://scihub.copernicus.eu/dhus/api/stub/products/count?filter=";
		if (sProvider.equals("MATERA"))
			sUrl = "https://collaborative.mt.asi.it/api/stub/products/count?filter=";

		final String USER_AGENT = "Mozilla/5.0";

		URL obj = new URL(sUrl + sParameter);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");

		//add request header
		con.setRequestProperty("User-Agent", USER_AGENT);
		String sUserCredentials = sOSUser + ":" + sOSPwd;
		String sBasicAuth = "Basic " + Base64.getEncoder().encodeToString(sUserCredentials.getBytes("UTF-8"));
		con.setRequestProperty ("Authorization", sBasicAuth);

		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'GET' request to URL : " + sUrl + sParameter);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(
				new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		//print result
		System.out.println("Count Done: Response " + response.toString());


		return response.toString();
	}





}
