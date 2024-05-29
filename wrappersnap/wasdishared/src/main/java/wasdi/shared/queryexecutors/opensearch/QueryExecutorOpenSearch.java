package wasdi.shared.queryexecutors.opensearch;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.StringUtils;
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
		String sResult = StringUtils.encodeUrl(sUrl);
		return sResult;
	}
	
	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		if(null == oQuery) {
			WasdiLog.debugLog("QueryExecutorOpenSearch.executeAndRetrieve: PaginatedQuery oQuery is null, aborting");
			return null;
		}
		try {
			
			// Get the Search Url
			String sUrl = getSearchUrl(oQuery);
			
			// Execute the http query
			String sResultAsString = standardHttpGETQuery(sUrl);
			
			if (sResultAsString == null) return null;
			
			DocumentBuilderFactory oDocBuildFactory = DocumentBuilderFactory.newInstance();
			
			DocumentBuilder oDocBuilder;
			try {
				
				oDocBuilder = oDocBuildFactory.newDocumentBuilder();
				Document oDocument = oDocBuilder.parse(new InputSource(new StringReader(sResultAsString)));
				
				if (oDocument == null) {
					WasdiLog.debugLog("OpenSearchQuery.executeAndRetrieve: Document response null");
					return null;
				}
				
				oDocument.getDocumentElement().normalize();
				
		
				if (bFullViewModel) {
					return buildResultViewModel(oDocument, sResultAsString);
				} else {
					return buildResultLightViewModel(oDocument, sResultAsString);
				}
				
			}
			catch (Exception oEx) {
				WasdiLog.errorLog("QueryExecutorOpenSearch.executeAndRetrieve: " + oEx);
				return null;
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
		if(null==oQuery) {
			WasdiLog.debugLog("QueryExecutorOpenSearch.buildUrl: oQuery is null");
			return null;
		}
		return null;

	}

	protected String getUrlSchema() {
		return "http";
	}

	protected abstract String[] getUrlPath();

	protected abstract String getCountUrl(String sQuery);	

	protected List<QueryResultViewModel> buildResultViewModel(Document oDocument, String sXML) {
		WasdiLog.debugLog("QueryExecutorOpenSearch.buildResultViewModel(3 args)");
		if(oDocument == null) {
			WasdiLog.debugLog("QueryExecutorOpenSearch.buildResultViewModel(3 args): Document<feed> oDocument is null, aborting");
			return null;
		}
		
		List<QueryResultViewModel> aoResults = new ArrayList<QueryResultViewModel>();
		
		//WasdiLog.debugLog("QueryExecutorOpenSearch.buildResultViewModel: Entries = " + oFeed.getEntries().size());

//		for (Entry oEntry : oFeed.getEntries()) {
//
//			QueryResultViewModel oResult = new QueryResultViewModel();
//			oResult.setProvider(m_sProvider);
//
//			//retrive the title
//			oResult.setTitle(oEntry.getTitle());			
//
//			//retrive the summary
//			oResult.setSummary(oEntry.getSummary());
//
//			//retrieve the id
//			oResult.setId(oEntry.getId().toString());
//
//			//retrieve the link
//			//			List<Link> aoLinks = oEntry.getLinks();
//			Link oLink = oEntry.getAlternateLink();
//			if (oLink != null)oResult.setLink(oLink.getHref().toString());
//
//			//retrieve the footprint and all others properties
//			List<Element> aoElements = oEntry.getElements();
//			for (Element element : aoElements) {
//				String sName = element.getAttributeValue("name");
//				if (sName != null) {
//					if (sName.equals("footprint")) {
//						oResult.setFootprint(element.getText());
//					} else {
//						oResult.getProperties().put(sName, element.getText());
//					}
//				}
//			}
//			
//			aoResults.add(oResult);
//		} 
		WasdiLog.debugLog("QueryExecutorOpenSearch.buildResultViewModel: Search Done: found " + aoResults.size() + " results");
		return aoResults;
	}

	protected List<QueryResultViewModel> buildResultLightViewModel(Document oDocument, String sXML) {
		WasdiLog.debugLog("QueryExecutorOpenSearch.buildResultLightViewModel(3 args)");
		if(oDocument == null) {
			WasdiLog.debugLog("QueryExecutorOpenSearch.buildResultLightViewModel(3 args): Document<feed> oDocument is null, aborting");
			return null;
		}
		
		List<QueryResultViewModel> aoResults = new ArrayList<QueryResultViewModel>();
		
//		Feed oFeed = (Feed) oDocument.getRoot();
		
//		for (Entry oEntry : oFeed.getEntries()) {
//			QueryResultViewModel oResult = new QueryResultViewModel();
//			oResult.setProvider(m_sProvider);
//			//retrieve the title
//			oResult.setTitle(oEntry.getTitle());			
//			//retrieve the summary
//			oResult.setSummary(oEntry.getSummary());
//			//retrieve the id
//			oResult.setId(oEntry.getId().toString());
//			//retrieve the link
//			Link oLink = oEntry.getAlternateLink();
//			if (oLink != null)oResult.setLink(oLink.getHref().toString());
//
//			//retrieve the footprint and all others properties
//			List<Element> aoElements = oEntry.getElements();
//			for (Element element : aoElements) {
//				String sName = element.getAttributeValue("name");
//				if (sName != null) {
//					if (sName.equals("footprint")) {
//						oResult.setFootprint(element.getText());
//					} else {
//						oResult.getProperties().put(sName, element.getText());
//					}
//				}
//			}
//			oResult.setPreview(null);
//			aoResults.add(oResult);
//		} 
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
