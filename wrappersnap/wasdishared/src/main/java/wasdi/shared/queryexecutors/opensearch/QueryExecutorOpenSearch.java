package wasdi.shared.queryexecutors.opensearch;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.RequestOptions;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

/**
 * Base class for Proviers supporting Open Search.
 * 
 * 
 * 
 * @author p.campanella
 *
 */
public abstract class QueryExecutorOpenSearch extends QueryExecutor {

	protected static String s_sClassName;
	
	@Override
	public int executeCount(String sQuery) {
		try {
			sQuery = encodeAsRequired(sQuery); 
			String sUrl = getCountUrl(sQuery);
			String sResponse = standardHttpGETQuery(sUrl);
			int iResult = 0;
			try {
				iResult = Integer.parseInt(extractNumberOfResults(sResponse));
			} catch (NumberFormatException oNfe) {
				WasdiLog.debugLog("QueryExecutorOpenSearch.executeCount: the response ( " + sResponse + " ) was not an int: " + oNfe);
				return -1;
			}
			return iResult;
		} catch (Exception oE) {
			WasdiLog.debugLog("QueryExecutorOpenSearch.executeCount: " + oE);
			return -1;
		}
	}
	
	/**
	 * @param sUrl
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	protected String encodeAsRequired(String sUrl) throws UnsupportedEncodingException {
		String sResult = URLEncoder.encode(sUrl, "UTF-8");
		return sResult;
	}
	
	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		if(null == oQuery) {
			WasdiLog.debugLog("QueryExecutorOpenSearch.executeAndRetrieve: PaginatedQuery oQuery is null, aborting");
			return null;
		}
		try {
			
			String sUrl = getSearchUrl(oQuery );

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
	
			if (m_bUseBasicAuthInHttpQuery) {
				// set authorization
				if (m_sUser!=null && m_sPassword!=null) {
					String sUserCredentials = m_sUser + ":" + m_sPassword;
					String sBasicAuth = "Basic " + Base64.getEncoder().encodeToString(sUserCredentials.getBytes());
					oOptions.setAuthorization(sBasicAuth);			
				}				
			}

			String sResultAsString = standardHttpGETQuery(sUrl);
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
				WasdiLog.debugLog("OpenSearchQuery.executeAndRetrieve: Document response null");
				return null;
			}
	
			if (bFullViewModel) {
				return buildResultViewModel(oDocument, oClient, oOptions);
			} else {
				return buildResultLightViewModel(oDocument, oClient, oOptions);
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("OpenSearchQuery.executeAndRetrieve: " + oE);
			return null;
		}
	}	
	
	protected String extractNumberOfResults(String sResponse) {
		return sResponse;
	}
	
	
	protected String getSearchListUrl(PaginatedQuery oQuery) {
		return getSearchUrl(oQuery);
	}
	

	protected String getSearchUrl(PaginatedQuery oQuery) {
		//WasdiLog.debugLog("QueryExecutorOpenSearch.buildUrl( " + oQuery + " )");
		if(null==oQuery) {
			WasdiLog.debugLog("QueryExecutorOpenSearch.buildUrl: oQuery is null");
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
		return oTemplate.expand(oParamsMap);

	}

	protected String getUrlSchema() {
		return "http";
	}

	protected abstract String[] getUrlPath();


	protected abstract Template getTemplate();

	protected abstract String getCountUrl(String sQuery);	

	protected List<QueryResultViewModel> buildResultViewModel(Document<Feed> oDocument, AbderaClient oClient, RequestOptions oOptions) {
		WasdiLog.debugLog("QueryExecutorOpenSearch.buildResultViewModel(3 args)");
		if(oDocument == null) {
			WasdiLog.debugLog("QueryExecutorOpenSearch.buildResultViewModel(3 args): Document<feed> oDocument is null, aborting");
			return null;
		}
		if(oClient == null) {
			WasdiLog.debugLog("QueryExecutorOpenSearch.buildResultViewModel(3 args): AbderaClient oClient is null, aborting");
			return null;
		}
		if(oOptions == null) {
			WasdiLog.debugLog("QueryExecutorOpenSearch.buildResultViewModel(3 args): RequestOptions oOptions is null, aborting");
			return null;
		}
		
		//int iStreamSize = 1000000;
		Feed oFeed = (Feed) oDocument.getRoot();

		//set new connction timeout
		oClient.setConnectionTimeout(2000);
		//oClient.setSocketTimeout(2000);
		oClient.setConnectionManagerTimeout(2000);

		List<QueryResultViewModel> aoResults = new ArrayList<QueryResultViewModel>();
		
		WasdiLog.debugLog("QueryExecutorOpenSearch.buildResultViewModel: Entries = " + oFeed.getEntries().size());

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
			
			/*
			if (oLink != null) {

				try {
					ClientResponse oImageResponse = oClient.get(oLink.getHref().toString(), oOptions);

					if (oImageResponse.getType() == ResponseType.SUCCESS) {

						InputStream oInputStreamImage = oImageResponse.getInputStream();
						BufferedImage  oImage = ImageIO.read(oInputStreamImage);
						ByteArrayOutputStream bas = new ByteArrayOutputStream();
						ImageIO.write(oImage, "png", bas);
						oResult.setPreview("data:image/png;base64," + Base64.getEncoder().encodeToString((bas.toByteArray())));
					}				
				}
				catch (Exception e) {
					WasdiLog.debugLog("QueryExecutorOpenSearch.buildResultViewModel: Image Preview Cycle Exception " + e.toString());
				}					
			}
			*/
			aoResults.add(oResult);
		} 
		WasdiLog.debugLog("QueryExecutorOpenSearch.buildResultViewModel: Search Done: found " + aoResults.size() + " results");
		return aoResults;
	}

	protected List<QueryResultViewModel> buildResultLightViewModel(Document<Feed> oDocument, AbderaClient oClient, RequestOptions oOptions) {
		WasdiLog.debugLog("QueryExecutorOpenSearch.buildResultLightViewModel(3 args)");
		if(oDocument == null) {
			WasdiLog.debugLog("QueryExecutorOpenSearch.buildResultLightViewModel(3 args): Document<feed> oDocument is null, aborting");
			return null;
		}
		if(oClient == null) {
			WasdiLog.debugLog("QueryExecutorOpenSearch.buildResultLightViewModel(3 args): AbderaClient oClient is null, aborting");
			return null;
		}
		if(oOptions == null) {
			WasdiLog.debugLog("QueryExecutorOpenSearch.buildResultLightViewModel(3 args): RequestOptions oOptions is null, aborting");
			return null;
		}
		Feed oFeed = (Feed) oDocument.getRoot();
		List<QueryResultViewModel> aoResults = new ArrayList<QueryResultViewModel>();
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
		WasdiLog.debugLog("QueryExecutorOpenSearch.buildResultLightViewModel: Search Done: found " + aoResults.size() + " results");
		return aoResults;
	}

	protected List<QueryResultViewModel> buildResultViewModel(String sJson, boolean bFullViewModel){
		return null;
	}
	
	protected List<QueryResultViewModel> buildResultLightViewModel(String sJson, boolean bFullViewModel){
		return null;
	}	
}
