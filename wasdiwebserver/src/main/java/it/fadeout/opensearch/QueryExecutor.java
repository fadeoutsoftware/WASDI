package it.fadeout.opensearch;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
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

import it.fadeout.viewmodels.QueryResultViewModel;

public abstract class QueryExecutor {

	public static QueryExecutor newInstance(String sProvider, String sUser, String sPassword, String sOffset, String sLimit, String sSortedBy, String sOrder) {
		
		String sClassName = QueryExecutor.class.getName() + sProvider;
		
		try {
			Object o = Class.forName(sClassName).newInstance();
			if (o instanceof QueryExecutor) {
				QueryExecutor oExecutor = (QueryExecutor) o;
				oExecutor.setProvider(sProvider);
				oExecutor.setUser(sUser);
				oExecutor.setPassword(sPassword);
				oExecutor.setOffset(sOffset);
				oExecutor.setLimit(sLimit);
				oExecutor.setSortedBy(sSortedBy);
				oExecutor.setOrder(sOrder);
				return oExecutor;
			}
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	protected String m_sProvider; 
	protected String m_sUser; 
	protected String m_sPassword; 
	protected String m_sOffset; 
	protected String m_sLimit; 
	protected String m_sSortedBy; 
	protected String m_sOrder;	
	

	public void setProvider(String m_sProvider) {
		this.m_sProvider = m_sProvider;
	}


	public void setUser(String m_sUser) {
		this.m_sUser = m_sUser;
	}


	public void setPassword(String m_sPassword) {
		this.m_sPassword = m_sPassword;
	}


	public void setOffset(String m_sOffset) {
		this.m_sOffset = m_sOffset;
	}


	public void setLimit(String m_sLimit) {
		this.m_sLimit = m_sLimit;
	}


	public void setSortedBy(String m_sSortedBy) {
		this.m_sSortedBy = m_sSortedBy;
	}


	public void setOrder(String m_sOrder) {
		this.m_sOrder = m_sOrder;
	}


	private String buildUrl(String sQuery) {
		Template oTemplate = getTemplate();
		Map<String,Object> oParamsMap = new HashMap<String, Object>();		
		oParamsMap.put("scheme", getUrlSchema());
		oParamsMap.put("path", getUrlPath());
		oParamsMap.put("start", m_sOffset);
		oParamsMap.put("rows", m_sLimit);
		oParamsMap.put("orderby", m_sSortedBy + " " + m_sOrder);
		oParamsMap.put("q", sQuery);
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
		int iStreamSize = 1000000;
		Feed oFeed = (Feed) oDocument.getRoot();

		//set new connction timeout
		oClient.setConnectionTimeout(2000);
		//oClient.setSocketTimeout(2000);
		oClient.setConnectionManagerTimeout(2000);

		ArrayList<QueryResultViewModel> aoResults = new ArrayList<QueryResultViewModel>();
		
		for (Entry oEntry : oFeed.getEntries()) {

			QueryResultViewModel oResult = new QueryResultViewModel();
			oResult.setProvider(m_sProvider);
//			System.out.println("Parsing new Entry");
			
			//retrive the title
			oResult.setTitle(oEntry.getTitle());			
			
			//retrive the summary
			oResult.setSummary(oEntry.getSummary());
			
			//retrieve the id
			oResult.setId(oEntry.getId().toString());
			
			//retrieve the link
//			List<Link> aoLinks = oEntry.getLinks();
			Link oLink = oEntry.getAlternateLink();
			if (oLink != null)oResult.setLink(oLink.getHref().toString()); //TODO
			
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
//				System.out.println("Icon Link: " + oLink.getHref().toString());

				try {
					ClientResponse oImageResponse = oClient.get(oLink.getHref().toString(), oOptions);
//					System.out.println("Response Got from the client");
					if (oImageResponse.getType() == ResponseType.SUCCESS)
					{
//						System.out.println("Success: saving image preview");
						InputStream oInputStreamImage = oImageResponse.getInputStream();
						BufferedImage  oImage = ImageIO.read(oInputStreamImage);
						ByteArrayOutputStream bas = new ByteArrayOutputStream();
						ImageIO.write(oImage, "png", bas);
						oResult.setPreview("data:image/png;base64," + Base64.getEncoder().encodeToString((bas.toByteArray())));
//						System.out.println("Image Saved");
					}				
				}
				catch (Exception e) {
					System.out.println("Image Preview Cycle Exception " + e.toString());
				}					
			}
			else {
				System.out.println("Link Not Available" );
			}
			
			aoResults.add(oResult);
		} 

		System.out.println("Search Done: found " + aoResults.size() + " results");

		return aoResults;
	}
	
	
	public int executeCount(String sQuery) throws IOException {
		
		String sUrl = getCountUrl(URLEncoder.encode(sQuery, "UTF-8"));
//		if (sProvider.equals("SENTINEL"))
//			sUrl = "https://scihub.copernicus.eu/dhus/api/stub/products/count?filter=";
//		if (sProvider.equals("MATERA"))
//			sUrl = "https://collaborative.mt.asi.it/api/stub/products/count?filter=";

		final String USER_AGENT = "Mozilla/5.0";

		URL obj = new URL(sUrl);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");

		//add request header
		con.setRequestProperty("User-Agent", USER_AGENT);
		if (m_sUser!=null && m_sPassword!=null) {
			String sUserCredentials = m_sUser + ":" + m_sPassword;
			String sBasicAuth = "Basic " + Base64.getEncoder().encodeToString(sUserCredentials.getBytes("UTF-8"));
			con.setRequestProperty ("Authorization", sBasicAuth);
		}
		
		System.out.println("\nSending 'GET' request to URL : " + sUrl);
		int responseCode = con.getResponseCode();
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		//print result
		System.out.println("Count Done: Response " + response.toString());
				
		return Integer.parseInt(response.toString());
	}
	
	public ArrayList<QueryResultViewModel> execute(String sQuery) throws IOException {
		
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
		
		System.out.println("\nSending 'GET' request to URL : " + sUrl);
		ClientResponse response = oClient.get(sUrl, oOptions);
		
		Document<Feed> oDocument = null;
		
		
		if (response.getType() != ResponseType.SUCCESS) {
			System.out.println("Response ERROR: " + response.getType());
			return null;
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
			return null;
		}
		
		return buildResultViewModel(oDocument, oClient, oOptions);
	}

	

	public static void main(String[] args) {
//		QueryExecutor oExecutor = QueryExecutor.newInstance("SENTINEL", "sadamo", "***REMOVED***", "0", "10", "ingestiondate", "asc"); 
		QueryExecutor oExecutor = QueryExecutor.newInstance("MATERA", "p.campanella", "***REMOVED***", "0", "10", "ingestiondate", "asc");
//		QueryExecutor oExecutor = QueryExecutor.newInstance("FEDEO", null, null, "0", "10", "ingestiondate", "asc");
		
		try {
			String sQuery = "( beginPosition:[2017-05-15T00:00:00.000Z TO 2017-05-15T23:59:59.999Z] AND endPosition:[2017-05-15T00:00:00.000Z TO 2017-05-15T23:59:59.999Z] ) AND   (platformname:Sentinel-1 AND filename:S1A_* AND producttype:GRD)";
			
			System.out.println(oExecutor.executeCount(sQuery));
			
//			String sParameter = URLEncoder.encode(sQuery, "UTF-8");
//			ArrayList<QueryResultViewModel> aoResults = oExecutor.execute(sQuery);
//			if (aoResults!=null) {
//				for (QueryResultViewModel oResult : aoResults) {
//					System.out.println(oResult.getTitle());
//					System.out.println("    ID: " + oResult.getId());
//					System.out.println("    SUMMARY: " + oResult.getSummary());
//					System.out.println("    LINK: " + oResult.getLink());
//					if (oResult.getPreview()!=null) System.out.println("    PREVIEW-LEN:" + oResult.getPreview().length());
//					System.out.println("    FOOTPPRINT:" + oResult.getFootprint());
//					for (String sKey : oResult.getProperties().keySet()) {
//						System.out.println("        PROPERTY: " + sKey + " --> " + oResult.getProperties().get(sKey));
//					}
//				}				
//			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
