/**
 * Created by Cristiano Nattero on 2020-01-21
 * 
 * Fadeout software
 *
 */
package wasdi.shared.queryexecutors.sobloo;

import wasdi.shared.queryexecutors.http.QueryExecutorHttpGet;

/**
 * Query executor for the SOBLOO DIAS.
 * 
 * SOBLOO uses Opensearch
 * 
 * @author c.nattero
 *
 */
public class QueryExecutorSOBLOO extends QueryExecutorHttpGet {

	public QueryExecutorSOBLOO() {
		this.m_oQueryTranslator = new QueryTranslatorSOBLOO();
		this.m_oResponseTranslator = new ResponseTranslatorSOBLOO();
		
		this.m_sUser = null;
		this.m_sPassword = null;
		
		m_bUseBasicAuthInHttpQuery = false;
		
	}

}