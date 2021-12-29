/**
 * Created by Cristiano Nattero on 2019-12-20
 * 
 * Fadeout software
 *
 */
package wasdi.shared.queryexecutors.creodias;

import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.http.QueryExecutorHttpGet;

/**
 * Query Executor for the CREODIAS DIAS.
 * 
 * 
 * @author c.nattero
 *
 */
public class QueryExecutorCREODIAS extends QueryExecutorHttpGet {

	public QueryExecutorCREODIAS() {
		m_sProvider="CREODIAS";
		this.m_oQueryTranslator = new QueryTranslatorCREODIAS();
		this.m_oResponseTranslator = new ResponseTranslatorCREODIAS();
	}
	
}
