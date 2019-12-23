/**
 * Created by Cristiano Nattero on 2019-12-20
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch;

import org.apache.abdera.i18n.templates.Template;

import wasdi.shared.utils.Utils;

/**
 * @author c.nattero
 *
 */
public class QueryExecutorCREODIAS extends QueryExecutor {

	/**
	 * 
	 */
	public QueryExecutorCREODIAS() {
		Utils.debugLog(s_sClassName);
		m_sProvider="CREODIAS";
		this.m_oQueryTranslator = new DiasQueryTranslatorCREODIAS();
		this.m_oResponseTranslator = new DiasResponseTranslatorCREODIAS();
	}

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
		// TODO Auto-generated method stub
		return null;
	}

}
