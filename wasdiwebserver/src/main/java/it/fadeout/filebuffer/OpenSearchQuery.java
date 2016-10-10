package it.fadeout.filebuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.parser.Parser;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.RequestOptions;
import org.apache.abdera.writer.Writer;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.json.JSONObject;
import org.json.XML;

public class OpenSearchQuery{

	public static String ExecuteQuery(String sQuery) throws URISyntaxException, IOException
	{
		Abdera oAbdera = new Abdera();
		String sJsonResult = new String("");
		//	create url passing search parameters
		String sUrl = OpenSearchTemplate.getHttpUrl("footprint:%22Intersects(41.9000,%2012.5000)%22");
		//sUrl += "&offset=0&limit=25";
		//sUrl = "https://scihub.copernicus.eu/dhus/api/stub/products?filter=(%20beginPosition:[2016-10-03T00:00:00.000Z%20TO%202016-10-06T23:59:59.999Z]%20AND%20endPosition:[2016-10-03T00:00:00.000Z%20TO%202016-10-06T23:59:59.999Z]%20)%20AND%20%20%20(platformname:Sentinel-1%20AND%20filename:S1A_*%20AND%20producttype:SLC)&offset=0&limit=25";
		//	create abdera client
		AbderaClient oClient = new AbderaClient(oAbdera);
		//	create credentials (username e password SSO ESA)
		UsernamePasswordCredentials oCredential = new UsernamePasswordCredentials("sadamo","***REMOVED***");
		// get default request option
		RequestOptions oOptions = oClient.getDefaultRequestOptions();
		oOptions.setUseChunked(false);
		// set content type accepted
		oOptions.setAccept("application/json, text/plain, */*");
	    // set accept encoding
		oOptions.setAcceptEncoding("gzip, deflate, sdch, br");
	    // set accept language
		oOptions.setAcceptLanguage("it-IT,it;q=0.8,en-US;q=0.6,en;q=0.4");
	    // set cache
		oOptions.setCacheControl("max-age=0");
	    // add credentials to request
	    oClient.addCredentials("http://scihub.copernicus.eu",null,null, oCredential);
	    // execute request and get the document (atom+xml format)
	    Parser parser = oAbdera.getParser();
	    ClientResponse response = oClient.get(sUrl, oOptions);
	    Document<Feed> oDocument = parser.parse(response.getInputStream());
	    Feed oFeed = (Feed) oDocument.getRoot();
	    int iStreamSize = 400000;
	    sJsonResult = Atom2Json(oAbdera, new ByteArrayOutputStream(iStreamSize), oFeed);
		return sJsonResult;
	}
	
	private static String Atom2Json(Abdera oAbdera, ByteArrayOutputStream oOutputStream, Feed oFeed) throws IOException{
		Writer writer = oAbdera.getWriterFactory().getWriter("prettyxml");
		writer.writeTo(oFeed, oOutputStream);
		JSONObject oJson = XML.toJSONObject(oOutputStream.toString("UTF-8"));
		return oJson.toString();
	}
	
}
