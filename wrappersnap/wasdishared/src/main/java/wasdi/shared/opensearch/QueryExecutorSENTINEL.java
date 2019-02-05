package wasdi.shared.opensearch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Base64;

import org.apache.abdera.Abdera;
import org.apache.abdera.i18n.templates.Template;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Feed;
import org.apache.abdera.parser.Parser;
import org.apache.abdera.parser.ParserOptions;
import org.apache.abdera.protocol.Response.ResponseType;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.RequestOptions;

public class QueryExecutorSENTINEL extends QueryExecutor {

	@Override
	protected Template getTemplate() {
		return new Template("{scheme}://{-append|.|host}scihub.copernicus.eu{-opt|/|path}{-listjoin|/|path}{-prefix|/|page}{-opt|?|q}{-join|&|q,start,rows,orderby}");
	}

	@Override	
	protected String[] getUrlPath() {
		return new String[] {"apihub","search"};
	}

	@Override
	protected String getUrlSchema() {
		return "https";
	}

	@Override
	protected String getCountUrl(String sQuery) {
		//return "https://scihub.copernicus.eu/dhus/api/stub/products/count?filter=" + sQuery;
		//return "https://scihub.copernicus.eu/dhus/odata/v1/Products/$count?$filter=" + sQuery;
		return "https://scihub.copernicus.eu/dhus/search?q=" + sQuery; 
		//return "https://scihub.copernicus.eu/dhus/odata/v1/Products/$count?$filter=startswith(Name,'S2')"; 
	}

	//public int executeCountSentinel(String sQuery) throws IOException {
	public int executeCount(String sQuery) throws IOException {	
		
		String sUrl = buildUrl(sQuery);
		//create abdera client
		Abdera oAbdera = new Abdera();
		AbderaClient oClient = new AbderaClient(oAbdera);
		oClient.setConnectionTimeout(15000);
		oClient.setSocketTimeout(40000);
		oClient.setConnectionManagerTimeout(20000);
		oClient.setMaxConnectionsTotal(200);
		oClient.setMaxConnectionsPerHost(50);
		
		// get default request option
		RequestOptions oOptions = oClient.getDefaultRequestOptions();
		
		// build the parser
		Parser oParser = oAbdera.getParser();
		ParserOptions oParserOptions = oParser.getDefaultParserOptions();
		oParserOptions.setCharset("UTF-8");
		//options.setCompressionCodecs(CompressionCodec.GZIP);
		oParserOptions.setFilterRestrictedCharacterReplacement('_');
		oParserOptions.setFilterRestrictedCharacters(true);
		oParserOptions.setMustPreserveWhitespace(false);
		oParserOptions.setParseFilter(null);
		
		// set authorization
		if (m_sUser!=null && m_sPassword!=null) {
			String sUserCredentials = m_sUser + ":" + m_sPassword;
			String sBasicAuth = "Basic " + Base64.getEncoder().encodeToString(sUserCredentials.getBytes());
			oOptions.setAuthorization(sBasicAuth);			
		}
		
		
//		System.out.println("\nSending 'GET' request to URL : " + sUrl);
		ClientResponse response = oClient.get(sUrl, oOptions);
		
		Document<Feed> oDocument = null;
		
		
		if (response.getType() != ResponseType.SUCCESS) {
			System.out.println("Response ERROR: " + response.getType());
			return -1;
		}

		System.out.println("Response Success");		
		
		// Get The Result as a string
		BufferedReader oBuffRead = new BufferedReader(response.getReader());
		String sResponseLine = null;
		StringBuilder oResponseStringBuilder = new StringBuilder();
		while ((sResponseLine = oBuffRead.readLine()) != null) {
		    oResponseStringBuilder.append(sResponseLine);
		}
		
		String sResultAsString = oResponseStringBuilder.toString();
		
//		System.out.println(sResultAsString);

		oDocument = oParser.parse(new StringReader(sResultAsString), oParserOptions);

		if (oDocument == null) {
			System.out.println("OpenSearchQuery.ExecuteQuery: Document response null");
			return -1;
		}
		
		Feed oFeed = (Feed) oDocument.getRoot();
		String sText = null;
		for (Element element : oFeed.getElements()) 
		{
			
			if (element.getQName().getLocalPart()== "totalResults")
			{
				sText = element.getText();
			}
		} 
		//String sTotalResults = oFeed.getAttributeValue("opensearch:totalResults");
				
		return Integer.parseInt(sText);
	}
}
