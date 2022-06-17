package wasdi.shared.queryexecutors.cloudferro;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.QueryTranslator;

public class QueryTranslatorCloudferro extends QueryTranslator {

	@Override
	protected String parseTimeFrame(String sQuery) {
		return null;
	}

	@Override
	protected String parseFootPrint(String sQuery) {
		return null;
	}

	@Override
	public String getCountUrl(String oQuery) {
		return null;
	}

	@Override
	public String getSearchUrl(PaginatedQuery oQuery) {
		return null;
	}

	public String encode(String sDecoded) {
		return super.encode(sDecoded);
	}

}
