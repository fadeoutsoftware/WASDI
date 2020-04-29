/**
 * Created by Cristiano Nattero on 2020-04-27
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch.eodc;

import java.io.IOException;
import java.util.List;

import org.apache.abdera.i18n.templates.Template;
import org.json.JSONObject;

import com.google.common.base.Preconditions;

import wasdi.shared.opensearch.PaginatedQuery;
import wasdi.shared.opensearch.QueryExecutor;
import wasdi.shared.opensearch.creodias.DiasQueryTranslatorCREODIAS;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.QueryResultViewModel;

/**
 * @author c.nattero
 *
 */
public class QueryExecutorEODC extends QueryExecutor {

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.QueryExecutor#getUrlPath()
	 */
	@Override
	protected String[] getUrlPath() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.QueryExecutor#getTemplate()
	 */
	@Override
	protected Template getTemplate() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.QueryExecutor#getCountUrl(java.lang.String)
	 */
	@Override
	protected String getCountUrl(String sQuery) {
		String sUrl = "https://csw.eodc.eu/";
		return sUrl;
	}
	
	
	@Override
	protected String extractNumberOfResults(String sResponse) {
		//todo parse response json and extract number
		String sCount = "";
		if(!Utils.isNullOrEmpty(sResponse)) {
			JSONObject oJson = new JSONObject(sResponse);
			if(null!=oJson) {
				if(oJson.has("csw:GetRecordsResponse")) {
					JSONObject oCswGetRecordsResponse = oJson.optJSONObject("csw:GetRecordsResponse");
					if(null!=oCswGetRecordsResponse) {
						if(oCswGetRecordsResponse.has("csw:SearchResults")) {
							JSONObject oSearchResults = oCswGetRecordsResponse.optJSONObject("csw:SearchResults");
							if(null!=oSearchResults) {
								if(oSearchResults.has("@numberOfRecordsMatched")) {
									int iResults = oSearchResults.optInt("@numberOfRecordsMatched", -1);
									sCount = "" + iResults;
								}
							}
						}
					}
				}
			}
		}
		
		return sCount;
	}

	public int executeCount(String sQuery) throws IOException {
		try {
			Utils.debugLog("QueryExecutor.executeCount( " + sQuery + " )");
			//sQuery = encodeAsRequired(sQuery); 
			String sUrl = getCountUrl(sQuery);
			
			
			// ( footprint:"intersects(POLYGON((92.36417183697604 12.654592055231863,92.36417183697604 26.282214356266774,99.48157676962991 26.282214356266774,99.48157676962991 12.654592055231863,92.36417183697604 12.654592055231863)))" ) AND ( beginPosition:[2019-05-01T00:00:00.000Z TO 2020-04-27T23:59:59.999Z] AND endPosition:[2019-05-01T00:00:00.000Z TO 2020-04-27T23:59:59.999Z] ) AND   (platformname:Sentinel-1 AND producttype:GRD AND relativeorbitnumber:33)&providers=ONDA
			DiasQueryTranslatorEODC oEODC = new DiasQueryTranslatorEODC();
			//String sTranslatedQuery = oEODC.translateAndEncode(sQuery);
			String sTranslatedQuery = oEODC.translate(sQuery);
			sTranslatedQuery = sTranslatedQuery.replace("<csw:ElementSetName>full</csw:ElementSetName>", "<csw:ElementSetName>brief</csw:ElementSetName>");
			
			String sResponse = httpPostResults(sUrl, "count", sTranslatedQuery);
			int iResult = 0;
			try {
				iResult = Integer.parseInt(extractNumberOfResults(sResponse));
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
	
	
	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
//		Preconditions.checkNotNull(this.m_sAppConfigPath, "QueryExecutorEODC.executeAndRetrieve: app config path is null");
//		Preconditions.checkNotNull(this.m_sParserConfigPath, "QueryExecutorEODC.executeAndRetrieve: parser config path is null");
		
//		this.m_oQueryTranslator.setParserConfigPath(this.m_sParserConfigPath);
//		this.m_oQueryTranslator.setAppconfigPath(this.m_sAppConfigPath);
		
		Utils.debugLog(s_sClassName + ".executeAndRetrieve(" + oQuery + ", " + bFullViewModel + ")");
		String sResult = null;
		String sUrl = null;
		try {
			sUrl = "https://csw.eodc.eu/";
			String sPayload = "";
//			if(bFullViewModel) {
//				sPayload = getSearchUrl(oQuery);
//			} else {
//				sUrl = getSearchListUrl(oQuery);
//			}
			DiasQueryTranslatorEODC oEODC = new DiasQueryTranslatorEODC();
			sPayload = oEODC.translate(oQuery.getQuery());
//			this.m_sUser = null;
//			this.m_sPassword = null;
			sResult = httpPostResults(sUrl, "search", sPayload);		
			List<QueryResultViewModel> aoResult = null;
			if(!Utils.isNullOrEmpty(sResult)) {
				//todo build result view model
				aoResult = buildResultViewModel(sResult, bFullViewModel);
				if(null==aoResult) {
					throw new NullPointerException(s_sClassName + ".executeAndRetrieve: aoResult is null"); 
				}
			} else {
				Utils.debugLog(s_sClassName + ".executeAndRetrieve(2 args): could not fetch results for url: " + sUrl);
			}
			return aoResult;
		} catch (Exception oE) {
			Utils.debugLog(s_sClassName + ".executeAndRetrieve(2 args, with sUrl=" + sUrl + "): " + oE);
		}
		return null;
	}
	
	
	@Override
	protected List<QueryResultViewModel> buildResultViewModel(String sJson, boolean bFullViewModel){
		DiasResponseTranslatorEODC oEODC = new DiasResponseTranslatorEODC();
		return oEODC.translateBatch(sJson, bFullViewModel, "file");
	}
	
	
	public static void main(String[] args) {
		QueryExecutorEODC oEODC = new QueryExecutorEODC();
		int iResults = -2;
		try {
			iResults = oEODC.executeCount("( footprint:\"intersects(POLYGON((92.36417183697604 12.654592055231863,92.36417183697604 26.282214356266774,99.48157676962991 26.282214356266774,99.48157676962991 12.654592055231863,92.36417183697604 12.654592055231863)))\" ) AND ( beginPosition:[2019-05-01T00:00:00.000Z TO 2020-04-27T23:59:59.999Z] AND endPosition:[2019-05-01T00:00:00.000Z TO 2020-04-27T23:59:59.999Z] ) AND   (platformname:Sentinel-1 AND producttype:GRD AND relativeorbitnumber:33)&providers=ONDA");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(iResults);
		
	}
}

