package wasdi.shared.opensearch.sentinelhub;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.abdera.Abdera;
import org.apache.abdera.i18n.templates.Template;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Feed;
import org.apache.abdera.parser.Parser;
import org.apache.abdera.parser.ParserOptions;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.RequestOptions;

import wasdi.shared.opensearch.DiasQueryTranslator;
import wasdi.shared.opensearch.PaginatedQuery;
import wasdi.shared.opensearch.Platforms;
import wasdi.shared.opensearch.QueryExecutor;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

public class QueryExecutorSENTINEL extends QueryExecutor {

	
	static {
		s_sClassName = "QueryExecutorSENTINEL";
	}
	
	public QueryExecutorSENTINEL() {
		Utils.debugLog(s_sClassName);
		m_sProvider = "SENTINEL";
		m_oQueryTranslator = new DiasQueryTranslator();
		
		m_asSupportedPlatforms.add(Platforms.SENTINEL1);
		m_asSupportedPlatforms.add(Platforms.SENTINEL2);
		m_asSupportedPlatforms.add(Platforms.SENTINEL3);
	}
	
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
		return "https://scihub.copernicus.eu/dhus/search?q=" + sQuery; 
	}

	@Override
	public int executeCount(String sQuery) {
		try {
			
			
			QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(sQuery);
			
			if (m_asSupportedPlatforms.contains(oQueryViewModel.platformName) == false) {
				return 0;
			}
			
			
			Utils.debugLog(s_sClassName + ".executeCount ( " + sQuery + " )");
			PaginatedQuery oQuery = new PaginatedQuery(sQuery, "0", "1", "ingestiondate", "asc");
			String sUrl = getSearchUrl(oQuery);
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
			Document<Feed> oDocument = null;
			String sResultAsString = null;
			
			sResultAsString = httpGetResults(sUrl, "count");
			oDocument = oParser.parse(new StringReader(sResultAsString), oParserOptions);
			if (oDocument == null) {
				Utils.debugLog(s_sClassName + ".executeCount: Document response null, aborting");
				return -1;
			}
			
			Feed oFeed = (Feed) oDocument.getRoot();
			String sText = null;
			for (Element element : oFeed.getElements()) {
				if (element.getQName().getLocalPart().equals("totalResults")) {
					sText = element.getText();
				}
			} 
			//String sTotalResults = oFeed.getAttributeValue("opensearch:totalResults");
					
			return Integer.parseInt(sText);
		} catch (NullPointerException oE) {
			Utils.debugLog(s_sClassName + "executeCount: " + oE);
		}
		return -1;
	}
	
	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		
		QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(oQuery.getQuery());
		
		if (m_asSupportedPlatforms.contains(oQueryViewModel.platformName) == false) {
			return new ArrayList<QueryResultViewModel>();
		}
		
		return super.executeAndRetrieve(oQuery, bFullViewModel);
	}
}
