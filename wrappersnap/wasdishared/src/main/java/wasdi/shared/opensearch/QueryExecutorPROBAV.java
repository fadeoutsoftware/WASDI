package wasdi.shared.opensearch;

import org.apache.abdera.i18n.templates.Template;

public class QueryExecutorPROBAV extends QueryExecutor  {

	@Override
	protected String[] getUrlPath() {
		return new String[] {"openSearch/findProducts"};
	}

	@Override
	protected Template getTemplate() {
		//http://www.vito-eodata.be/openSearch/findProducts?collection=urn:ogc:def:EOP:VITO:PROBAV_S1-TOC_1KM_V001&start=2016-05-05T00:00&end=2016-05-14T23:59&bbox=-180.00,0.00,180.00,90.00
		return new Template("{scheme}://{-append|.|host}www.vito-eodata.be{-opt|/|path}{-listjoin|/|path}{-prefix|/|page}{-opt|?|q}{-join|&|q,start,rows,orderby}");
	}

	@Override
	protected String getCountUrl(String sQuery) {
		//http://www.vito-eodata.be/openSearch/findProducts?collection=urn:ogc:def:EOP:VITO:PROBAV_S1-TOC_1KM_V001&start=2016-05-05T00:00&end=2016-05-14T23:59&bbox=-180.00,0.00,180.00,90.00
		return "http://www.vito-eodata.be/openSearch/count?filter=" + sQuery;
	}

}
