package wasdi.shared.opensearch;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
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
import org.apache.commons.net.io.Util;

import wasdi.shared.utils.AuthenticationCredentials;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.QueryResultViewModel;

public abstract class QueryExecutor {

	protected static String s_sClassName;
	
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
		//Utils.debugLog("QueryExecutor.buildUrl( " + oQuery + " )");
		if(null==oQuery) {
			Utils.debugLog("QueryExecutor.buildUrl: oQuery is null");
			return null;
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

	//todo make this method abstract
	protected void addUrlParams(Map<String, Object> oParamsMap) {
	}


	protected String getUrlSchema() {
		return "http";
	}

	protected abstract String[] getUrlPath();


	protected abstract Template getTemplate();

	protected abstract String getCountUrl(String sQuery);	

	protected ArrayList<QueryResultViewModel> buildResultViewModel(Document<Feed> oDocument, AbderaClient oClient, RequestOptions oOptions) {
		Utils.debugLog("QueryExecutor.buildResultViewModel(3 args)");
		if(oDocument == null) {
			Utils.debugLog("QueryExecutor.buildResultViewModel(3 args): Document<feed> oDocument is null, aborting");
			return null;
		}
		if(oClient == null) {
			Utils.debugLog("QueryExecutor.buildResultViewModel(3 args): AbderaClient oClient is null, aborting");
			return null;
		}
		if(oOptions == null) {
			Utils.debugLog("QueryExecutor.buildResultViewModel(3 args): RequestOptions oOptions is null, aborting");
			return null;
		}
		
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
			//			Utils.debugLog("QueryExecutor.buildResultViewModel: Parsing new Entry");

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
				//				Utils.debugLog("QueryExecutor.buildResultViewModel: Icon Link: " + oLink.getHref().toString());

				try {
					ClientResponse oImageResponse = oClient.get(oLink.getHref().toString(), oOptions);
					//					Utils.debugLog("QueryExecutor.buildResultViewModel: Response Got from the client");
					if (oImageResponse.getType() == ResponseType.SUCCESS) {
						//						Utils.debugLog("QueryExecutor.buildResultViewModel: Success: saving image preview");
						InputStream oInputStreamImage = oImageResponse.getInputStream();
						BufferedImage  oImage = ImageIO.read(oInputStreamImage);
						ByteArrayOutputStream bas = new ByteArrayOutputStream();
						ImageIO.write(oImage, "png", bas);
						oResult.setPreview("data:image/png;base64," + Base64.getEncoder().encodeToString((bas.toByteArray())));
						//						Utils.debugLog("QueryExecutor.buildResultViewModel: Image Saved");
					}				
				}
				catch (Exception e) {
					Utils.debugLog("QueryExecutor.buildResultViewModel: Image Preview Cycle Exception " + e.toString());
				}					
			} else {
				Utils.debugLog("QueryExecutor.buildResultViewModel: Link Not Available" );
			}
			aoResults.add(oResult);
		} 
		Utils.debugLog("QueryExecutor.buildResultViewModel: Search Done: found " + aoResults.size() + " results");
		return aoResults;
	}

	protected ArrayList<QueryResultViewModel> buildResultLightViewModel(Document<Feed> oDocument, AbderaClient oClient, RequestOptions oOptions) {
		Utils.debugLog("QueryExecutor.buildResultLightViewModel(3 args)");
		if(oDocument == null) {
			Utils.debugLog("QueryExecutor.buildResultLightViewModel(3 args): Document<feed> oDocument is null, aborting");
			return null;
		}
		if(oClient == null) {
			Utils.debugLog("QueryExecutor.buildResultLightViewModel(3 args): AbderaClient oClient is null, aborting");
			return null;
		}
		if(oOptions == null) {
			Utils.debugLog("QueryExecutor.buildResultLightViewModel(3 args): RequestOptions oOptions is null, aborting");
			return null;
		}
		Feed oFeed = (Feed) oDocument.getRoot();
		ArrayList<QueryResultViewModel> aoResults = new ArrayList<QueryResultViewModel>();
		for (Entry oEntry : oFeed.getEntries()) {
			QueryResultViewModel oResult = new QueryResultViewModel();
			oResult.setProvider(m_sProvider);
			//retrieve the title
			oResult.setTitle(oEntry.getTitle());			
			//retrieve the summary
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
		Utils.debugLog("QueryExecutor.buildResultLightViewModel: Search Done: found " + aoResults.size() + " results");
		return aoResults;
	}

	public int executeCount(String sQuery) throws IOException {
		try {
			Utils.debugLog("QueryExecutor.executeCount( " + sQuery + " )");
			String sUrl = getCountUrl(URLEncoder.encode(sQuery, "UTF-8"));
			
/*
			//		if (sProvider.equals("SENTINEL"))
			//		sUrl = "https://scihub.copernicus.eu/dhus/api/stub/products/count?filter=";
			//if (sProvider.equals("MATERA"))
			//sUrl = "https://collaborative.mt.asi.it/api/stub/products/count?filter=";
	
			final String sUserAgent = "Mozilla/5.0";
	
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();
	
			// optional default is GET
			oConnection.setRequestMethod("GET");
	
			//add request header
			oConnection.setRequestProperty("User-Agent", sUserAgent);
			if (m_sUser!=null && m_sPassword!=null) {
				String sUserCredentials = m_sUser + ":" + m_sPassword;
				String sBasicAuth = "Basic " + Base64.getEncoder().encodeToString(sUserCredentials.getBytes("UTF-8"));
				oConnection.setRequestProperty ("Authorization", sBasicAuth);
			}
	
			Utils.debugLog("QueryExecutor.executeCount: Sending 'GET' request to URL : " + sUrl);
			//int responseCode = -1;
			//try
			//{
			//	responseCode = oConnection.getResponseCode();
			//}
			//catch(IOException oEx)
			//{
			//	Utils.debugLog("QueryExecutor.executeCount: Exception Get response : " + oEx.getMessage());	
			//}
			int responseCode =  oConnection.getResponseCode();
			Utils.debugLog("QueryExecutor.executeCount: Response Code : " + responseCode);
	
			BufferedReader in = new BufferedReader(new InputStreamReader(oConnection.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
	
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			String sResponse = response.toString();
	
			//print result
			Utils.debugLog("QueryExecutor.executeCount: Count Done: Response " + sResponse);
	
			//for (Element element : oFeed.getElements()) {
			//String sText = element.getText();
			//	if (element.getQName().getLocalPart() 
			//	} 
			//String sTotalResults = oFeed.getAttributeValue("opensearch:totalResults");

*/
			String sResponse = httpGetResults(sUrl, "count");
			int iResult = 0;
			try {
				iResult = Integer.parseInt(sResponse);
			} catch (NumberFormatException oNfe) {
				Utils.debugLog("QueryExecutor.executeCount: the response ( " + sResponse + " ) was not an int: " + oNfe);
				return -1;
			}
			return iResult;
		} catch (Exception oE) {
			Utils.debugLog("QueryExecutor.executeCount: " + oE);
			return -1;
		}
	}

	public ArrayList<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		Utils.debugLog("QueryExecutor.executeAndRetrieve(PaginatedQuery oQuery, " + bFullViewModel + ")");
		if(null == oQuery) {
			Utils.debugLog("QueryExecutor.executeAndRetrieve: PaginatedQuery oQuery is null, aborting");
			return null;
		}
		try {
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
/*	
			Utils.debugLog("QueryExecutor.executeAndRetrieve: Sending 'GET' request to URL : " + sUrl);
			long lStart = System.nanoTime();
			ClientResponse response = oClient.get(sUrl, oOptions);
			long lEnd = System.nanoTime();
			long lTimeElapsed = lEnd - lStart;
			double dMillis = lTimeElapsed / (1000.0 * 1000.0);
	
			if (response.getType() != ResponseType.SUCCESS) {
				Utils.debugLog("QueryExecutor.executeAndRetrieve: Response ERROR after " + dMillis + "ms : " + response.getType());
				return null;
			}
				
	
			// Get The Result as a string
			BufferedReader oBuffRead = null;
			try {
				oBuffRead = new BufferedReader(response.getReader());
			} catch (IOException oe1) {
				Utils.debugLog("QueryExecutor.executeAndRetrieve: " + oe1);
				return null;
			}
			String sResponseLine = null;
			StringBuilder oResponseStringBuilder = new StringBuilder();
			try {
				while ((sResponseLine = oBuffRead.readLine()) != null) {
					oResponseStringBuilder.append(sResponseLine);
				}
			} catch (IOException oe2) {
				Utils.debugLog("QueryExecutor.executeAndRetrieve: " + oe2);
				return null;
			}
			
	
			String sResultAsString = oResponseStringBuilder.toString();
			
			//stats
			int iResponseLength = 0;
			iResponseLength = sResultAsString.length();

			double dSpeed = 0;
			if(iResponseLength > 0) {
				dSpeed = ( (double) iResponseLength ) / dMillis;
				dSpeed *= 1000.0;
			}
			Utils.debugLog("QueryExecutor.executeAndRetrieve success: ([ms,B,B/s]): "+dMillis+"," + iResponseLength + "," + dSpeed);
			
			//		String sTmpFilePath = "insert/a/realistic/path/to/file.xml";
			//		Utils.printToFile(sTmpFilePath, sResultAsString);
	
			//		Utils.debugLog(sResultAsString);
*/

			String sResultAsString = httpGetResults(sUrl, "search");
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
				Utils.debugLog("OpenSearchQuery.executeAndRetrieve: Document response null");
				return null;
			}
	
			if (bFullViewModel) return buildResultViewModel(oDocument, oClient, oOptions);
			else return buildResultLightViewModel(oDocument, oClient, oOptions);
		} catch (NumberFormatException oE) {
			Utils.debugLog("OpenSearchQuery.executeAndRetrieve: " + oE);
			return null;
		}
	}

	public ArrayList<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery) throws IOException {
		Utils.debugLog("QueryExecutor.executeAndRetrieve(PaginatedQuery oQuery)");
		return executeAndRetrieve(oQuery,true);
	}


	public void setDownloadProtocol(String sDownloadProtocol) {
		Utils.debugLog("QueryExecutor.setDownloadProtocol");
		m_sDownloadProtocol = sDownloadProtocol;
		if(null==m_sDownloadProtocol) {
			m_sDownloadProtocol = "https:";
		}
	}

	public void setCredentials(AuthenticationCredentials oCredentials) {
		Utils.debugLog("QueryExecutor.setCredentials");
		if(null!=oCredentials) {
			setUser(oCredentials.getUser());
			setPassword(oCredentials.getPassword());
		} else {
			throw new NullPointerException("QueryExecutor.setCredentials: null oCredentials");
		}

	}

	protected String httpGetResults(String sUrl) {
		String sQueryType = "search";
		if(sUrl.contains("count")) {
			sQueryType ="count";
		}
		return httpGetResults(sUrl, sQueryType);
	}
	
	//todo add parameter to distinguish type of call: search or count
	protected String httpGetResults(String sUrl, String sQueryType) {
		Utils.debugLog("QueryExecutor.httpGetResults( " + sUrl + ", " + sQueryType + " )");
		String sResult = null;
		try {
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();
	
			// optional default is GET
			oConnection.setRequestMethod("GET");
			oConnection.setRequestProperty("Accept", "*/*");
			
			if (m_sUser!=null && m_sPassword!=null) {
				String sUserCredentials = m_sUser + ":" + m_sPassword;
				String sBasicAuth = "Basic " + Base64.getEncoder().encodeToString(sUserCredentials.getBytes("UTF-8"));
				oConnection.setRequestProperty ("Authorization", sBasicAuth);
			}
	
			Utils.debugLog("\nSending 'GET' request to URL : " + sUrl);
	
			long lStart = System.nanoTime();
			int iResponseSize = -1;
			try {
				int responseCode =  oConnection.getResponseCode();
				Utils.debugLog("QueryExecutor.httpGetResults: Response Code : " + responseCode);
				String sResponseExtract = null;
				if(200 == responseCode) {
					InputStream oInputStream = oConnection.getInputStream();
					ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
					if(null!=oInputStream) {
						Util.copyStream(oInputStream, oBytearrayOutputStream);
						sResult = oBytearrayOutputStream.toString();
					}
					
					if(!Utils.isNullOrEmpty(sResult)) {
						if(sResult.length() > 200 ) {
							sResponseExtract = sResult.substring(0, 200) + "...";
						} else {
							sResponseExtract = new String(sResult);
						}
						Utils.debugLog("QueryExecutor.httpGetResults: Response extract: " + sResponseExtract);
						iResponseSize = sResult.length();
					} else {
						Utils.debugLog("QueryExecutor.httpGetResults: reponse is empty");
					}
				} else {
					Utils.debugLog("QueryExecutor.httpGetResults: provider did not return 200 but "+responseCode+
							" (1/2) and the following message:\n" + oConnection.getResponseMessage());
					ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
					InputStream oErrorStream = oConnection.getErrorStream();
					Util.copyStream(oErrorStream, oBytearrayOutputStream);
					String sMessage = oBytearrayOutputStream.toString();
					if(null!=sMessage) {
						sResponseExtract = sMessage.substring(0,  200) + "...";
					}
					Utils.debugLog("QueryExecutor.httpGetResults: provider did not return 200 but "+responseCode+
							" (2/2) and this is the content of the error stream:\n" + sResponseExtract);
					if(iResponseSize <= 0) {
						iResponseSize = sMessage.length();
					}
				}
			} catch (SocketTimeoutException oE) {
				Utils.debugLog("QueryExecutor.httpGetResults: " + oE);
			}
			
			long lEnd = System.nanoTime();
			long lTimeElapsed = lEnd - lStart;
			double dMillis = lTimeElapsed / (1000.0 * 1000.0);
			double dSpeed = 0;
			if(iResponseSize > 0) {
				dSpeed = ( (double) iResponseSize ) / dMillis;
				dSpeed *= 1000.0;
			}
			Utils.debugLog("QueryExecutor.httpGetResults: " + sQueryType+" ([ms,B,B/s]): "+dMillis+"," + iResponseSize + "," + dSpeed);
		}
		catch (Exception oE) {
			Utils.debugLog("QueryExecutor.httpGetResults: " + oE);
		}
		return sResult;
	}

	public static void main(String[] args) {

		QueryExecutorFactory oFactory = new QueryExecutorFactory();
		//change the following with your user and password (and don't commit them!)
		AuthenticationCredentials oCredentials = new AuthenticationCredentials("user", "password");
		QueryExecutor oExecutor = oFactory.getExecutor("MATERA", oCredentials, "", "true");

		try {
			String sQuery = "( beginPosition:[2017-05-15T00:00:00.000Z TO 2017-05-15T23:59:59.999Z] AND endPosition:[2017-05-15T00:00:00.000Z TO 2017-05-15T23:59:59.999Z] ) AND   (platformname:Sentinel-1 AND filename:S1A_* AND producttype:GRD)";

			Utils.debugLog(""+oExecutor.executeCount(sQuery));			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
