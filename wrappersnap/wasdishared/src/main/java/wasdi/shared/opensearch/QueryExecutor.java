package wasdi.shared.opensearch;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.abdera.Abdera;
import org.apache.abdera.i18n.templates.Template;
import org.apache.abdera.i18n.text.io.CompressionUtil.CompressionCodec;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;
import org.apache.abdera.parser.Parser;
import org.apache.abdera.parser.ParserOptions;
import org.apache.abdera.protocol.Response.ResponseType;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.RequestOptions;

import wasdi.shared.utils.AuthenticationCredentials;
import wasdi.shared.viewmodels.QueryResultViewModel;

public abstract class QueryExecutor {

	protected String m_sDownloadProtocol;
	protected boolean m_bMustCollectMetadata;
	protected String m_sProvider; 
	protected String m_sUser; 
	protected String m_sPassword; 

	protected DiasQueryTranslator m_oQueryTranslator;
	protected DiasResponseTranslator m_oResponseTranslator;


	void setMustCollectMetadata(boolean bGetMetadata) {
		m_bMustCollectMetadata = bGetMetadata;

	}	

	public void setUser(String m_sUser) {
		this.m_sUser = m_sUser;
	}

	public void setPassword(String m_sPassword) {
		this.m_sPassword = m_sPassword;
	}




	protected String buildUrl(PaginatedQuery oQuery) {
		System.out.println("QueryExecutor.buildUrl");
		if(null==oQuery) {
			System.out.println("QueryExecutor.buildUrl: oQuery is null");
		}
		Template oTemplate = getTemplate();
		Map<String,Object> oParamsMap = new HashMap<String, Object>();		
		oParamsMap.put("scheme", getUrlSchema());
		oParamsMap.put("path", getUrlPath());
		oParamsMap.put("start", oQuery.getOffset() );
		oParamsMap.put("rows", oQuery.getLimit() );
		oParamsMap.put("orderby", oQuery.getSortedBy() + " " + oQuery.getOrder() );
		oParamsMap.put("q", oQuery.getQuery() );
		addUrlParams(oParamsMap);
		return oTemplate.expand(oParamsMap);

	}

	protected void addUrlParams(Map<String, Object> oParamsMap) {
	}


	protected String getUrlSchema() {
		return "http";
	}

	protected abstract String[] getUrlPath();


	protected abstract Template getTemplate();

	protected abstract String getCountUrl(String sQuery);	

	protected ArrayList<QueryResultViewModel> buildResultViewModel(Document<Feed> oDocument, AbderaClient oClient, RequestOptions oOptions) {
		System.out.println("QueryExecutor.buildResultViewModel(3 args)");
		//int iStreamSize = 1000000;
		Feed oFeed = (Feed) oDocument.getRoot();

		//set new connction timeout
		oClient.setConnectionTimeout(2000);
		//oClient.setSocketTimeout(2000);
		oClient.setConnectionManagerTimeout(2000);

		ArrayList<QueryResultViewModel> aoResults = new ArrayList<QueryResultViewModel>();

		for (Entry oEntry : oFeed.getEntries()) {

			QueryResultViewModel oResult = new QueryResultViewModel();
			oResult.setProvider(m_sProvider);
			//			System.out.println("QueryExecutor.buildResultViewModel: Parsing new Entry");

			//retrive the title
			oResult.setTitle(oEntry.getTitle());			

			//retrive the summary
			oResult.setSummary(oEntry.getSummary());

			//retrieve the id
			oResult.setId(oEntry.getId().toString());

			//retrieve the link
			//			List<Link> aoLinks = oEntry.getLinks();
			Link oLink = oEntry.getAlternateLink();
			if (oLink != null)oResult.setLink(oLink.getHref().toString());

			//retrieve the footprint and all others properties
			List<Element> aoElements = oEntry.getElements();
			for (Element element : aoElements) {
				String sName = element.getAttributeValue("name");
				if (sName != null) {
					if (sName.equals("footprint")) {
						oResult.setFootprint(element.getText());
					} else {
						oResult.getProperties().put(sName, element.getText());
					}
				}
			}

			//retrieve the icon
			oLink = oEntry.getLink("icon");			
			if (oLink != null) {
				//				System.out.println("QueryExecutor.buildResultViewModel: Icon Link: " + oLink.getHref().toString());

				try {
					ClientResponse oImageResponse = oClient.get(oLink.getHref().toString(), oOptions);
					//					System.out.println("QueryExecutor.buildResultViewModel: Response Got from the client");
					if (oImageResponse.getType() == ResponseType.SUCCESS)
					{
						//						System.out.println("QueryExecutor.buildResultViewModel: Success: saving image preview");
						InputStream oInputStreamImage = oImageResponse.getInputStream();
						BufferedImage  oImage = ImageIO.read(oInputStreamImage);
						ByteArrayOutputStream bas = new ByteArrayOutputStream();
						ImageIO.write(oImage, "png", bas);
						oResult.setPreview("data:image/png;base64," + Base64.getEncoder().encodeToString((bas.toByteArray())));
						//						System.out.println("QueryExecutor.buildResultViewModel: Image Saved");
					}				
				}
				catch (Exception e) {
					System.out.println("QueryExecutor.buildResultViewModel: Image Preview Cycle Exception " + e.toString());
				}					
			}
			else {
				System.out.println("QueryExecutor.buildResultViewModel: Link Not Available" );
			}

			aoResults.add(oResult);
		} 

		System.out.println("QueryExecutor.buildResultViewModel: Search Done: found " + aoResults.size() + " results");

		return aoResults;
	}

	protected ArrayList<QueryResultViewModel> buildResultLightViewModel(Document<Feed> oDocument, AbderaClient oClient, RequestOptions oOptions) {

		System.out.println("QueryExecutor.buildResultLightViewModel(3 args)");
		Feed oFeed = (Feed) oDocument.getRoot();

		ArrayList<QueryResultViewModel> aoResults = new ArrayList<QueryResultViewModel>();

		for (Entry oEntry : oFeed.getEntries()) {

			QueryResultViewModel oResult = new QueryResultViewModel();
			oResult.setProvider(m_sProvider);

			//retrive the title
			oResult.setTitle(oEntry.getTitle());			

			//retrive the summary
			oResult.setSummary(oEntry.getSummary());

			//retrieve the id
			oResult.setId(oEntry.getId().toString());

			//retrieve the link
			Link oLink = oEntry.getAlternateLink();
			if (oLink != null)oResult.setLink(oLink.getHref().toString());

			//retrieve the footprint and all others properties
			List<Element> aoElements = oEntry.getElements();
			for (Element element : aoElements) {
				String sName = element.getAttributeValue("name");
				if (sName != null) {
					if (sName.equals("footprint")) {
						oResult.setFootprint(element.getText());
					} else {
						oResult.getProperties().put(sName, element.getText());
					}
				}
			}

			oResult.setPreview(null);

			aoResults.add(oResult);
		} 

		System.out.println("QueryExecutor.buildResultLightViewModel: Search Done: found " + aoResults.size() + " results");

		return aoResults;
	}

	public int executeCount(String sQuery) throws IOException {
		System.out.println("QueryExecutor.executeCount");

		String sUrl = getCountUrl(URLEncoder.encode(sQuery, "UTF-8"));
		//		if (sProvider.equals("SENTINEL"))
		//		sUrl = "https://scihub.copernicus.eu/dhus/api/stub/products/count?filter=";
		//if (sProvider.equals("MATERA"))
		//sUrl = "https://collaborative.mt.asi.it/api/stub/products/count?filter=";

		final String USER_AGENT = "Mozilla/5.0";

		URL oURL = new URL(sUrl);
		HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();

		// optional default is GET
		oConnection.setRequestMethod("GET");

		//add request header
		oConnection.setRequestProperty("User-Agent", USER_AGENT);
		if (m_sUser!=null && m_sPassword!=null) {
			String sUserCredentials = m_sUser + ":" + m_sPassword;
			String sBasicAuth = "Basic " + Base64.getEncoder().encodeToString(sUserCredentials.getBytes("UTF-8"));
			oConnection.setRequestProperty ("Authorization", sBasicAuth);
		}

		System.out.println("\nQueryExecutor.executeCount: Sending 'GET' request to URL : " + sUrl);
		//int responseCode = -1;
		//try
		//{
		//	responseCode = oConnection.getResponseCode();
		//}
		//catch(IOException oEx)
		//{
		//	System.out.println("QueryExecutor.executeCount: Exception Get response : " + oEx.getMessage());	
		//}
		int responseCode =  oConnection.getResponseCode();
		System.out.println("QueryExecutor.executeCount: Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(oConnection.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		//print result
		System.out.println("QueryExecutor.executeCount: Count Done: Response " + response.toString());

		//for (Element element : oFeed.getElements()) {
		//String sText = element.getText();
		//	if (element.getQName().getLocalPart() 
		//	} 
		//String sTotalResults = oFeed.getAttributeValue("opensearch:totalResults");

		return Integer.parseInt(response.toString());
	}

	public ArrayList<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) throws IOException {
		//XXX log instead
		System.out.println("QueryExecutor.executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel)");
		String sUrl = buildUrl(oQuery );

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

		// set authorization
		if (m_sUser!=null && m_sPassword!=null) {
			String sUserCredentials = m_sUser + ":" + m_sPassword;
			String sBasicAuth = "Basic " + Base64.getEncoder().encodeToString(sUserCredentials.getBytes());
			oOptions.setAuthorization(sBasicAuth);			
		}

		System.out.println("\nQueryExecutor.executeAndRetrieve: Sending 'GET' request to URL : " + sUrl);
		ClientResponse response = oClient.get(sUrl, oOptions);


		if (response.getType() != ResponseType.SUCCESS) {
			System.out.println("QueryExecutor.executeAndRetrieve: Response ERROR: " + response.getType());
			return null;
		}

		System.out.println("QueryExecutor.executeAndRetrieve: Response Success");		

		// Get The Result as a string
		BufferedReader oBuffRead = new BufferedReader(response.getReader());
		String sResponseLine = null;
		StringBuilder oResponseStringBuilder = new StringBuilder();
		while ((sResponseLine = oBuffRead.readLine()) != null) {
			oResponseStringBuilder.append(sResponseLine);
		}

		String sResultAsString = oResponseStringBuilder.toString();
		//		String sTmpFilePath = "insert/a/realistic/path/to/file.xml";
		//		Utils.printToFile(sTmpFilePath, sResultAsString);

		//		System.out.println(sResultAsString);

		// build the parser
		Parser oParser = oAbdera.getParser();
		ParserOptions oParserOptions = oParser.getDefaultParserOptions();
		oParserOptions.setCharset("UTF-8");
		oParserOptions.setCompressionCodecs(CompressionCodec.GZIP);
		oParserOptions.setFilterRestrictedCharacterReplacement('_');
		oParserOptions.setFilterRestrictedCharacters(true);
		oParserOptions.setMustPreserveWhitespace(false);
		oParserOptions.setParseFilter(null);


		Document<Feed> oDocument = oParser.parse(new StringReader(sResultAsString), oParserOptions);
		if (oDocument == null) {
			System.out.println("OpenSearchQuery.executeAndRetrieve: Document response null");
			return null;
		}

		if (bFullViewModel) return buildResultViewModel(oDocument, oClient, oOptions);
		else return buildResultLightViewModel(oDocument, oClient, oOptions);
	}

	public ArrayList<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery) throws IOException {
		System.out.println("QueryExecutor.executeAndRetrieve(PaginatedQuery oQuery)");
		return executeAndRetrieve(oQuery,true);
	}


	public void setDownloadProtocol(String sDownloadProtocol) {
		System.out.println("QueryExecutor.setDownloadProtocol");
		m_sDownloadProtocol = sDownloadProtocol;
		if(null==m_sDownloadProtocol) {
			m_sDownloadProtocol = "https:";
		}
	}

	public void setCredentials(AuthenticationCredentials oCredentials) {
		System.out.println("QueryExecutor.setCredentials");
		if(null!=oCredentials) {
			setUser(oCredentials.getUser());
			setPassword(oCredentials.getPassword());
		} else {
			throw new NullPointerException("QueryExecutor.setCredentials: null oCredentials");
		}

	}

	public static void main(String[] args) {

		QueryExecutorFactory oFactory = new QueryExecutorFactory();
		//change the following with your user and password (and don't commit them!)
		AuthenticationCredentials oCredentials = new AuthenticationCredentials("user", "password");
		QueryExecutor oExecutor = oFactory.getExecutor("MATERA", oCredentials, "", "true");

		try {
			String sQuery = "( beginPosition:[2017-05-15T00:00:00.000Z TO 2017-05-15T23:59:59.999Z] AND endPosition:[2017-05-15T00:00:00.000Z TO 2017-05-15T23:59:59.999Z] ) AND   (platformname:Sentinel-1 AND filename:S1A_* AND producttype:GRD)";

			System.out.println(oExecutor.executeCount(sQuery));			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
