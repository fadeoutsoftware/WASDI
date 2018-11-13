package wasdi.shared.opensearch;

import org.apache.abdera.i18n.templates.Template;

public class QueryExecutorSENTINEL extends QueryExecutor {

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
		//return "https://scihub.copernicus.eu/dhus/api/stub/products/count?filter=" + sQuery;
		//return "https://scihub.copernicus.eu/dhus/odata/v1/Products/$count?$filter=" + sQuery;
		return "https://scihub.copernicus.eu/dhus/search?q=" + sQuery; 
		//return "https://scihub.copernicus.eu/dhus/odata/v1/Products/$count?$filter=startswith(Name,'S2')"; 
	}
	
}
