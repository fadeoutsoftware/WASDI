package wasdi.shared.opensearch.viirs;

import wasdi.shared.opensearch.DiasQueryTranslator;

public class DiasQueryTranslatorVIIRS extends DiasQueryTranslator {

	@Override
	protected String translate(String sQueryFromClient) {

		return sQueryFromClient;
	}

	@Override
	protected String parseTimeFrame(String sQuery) {
		return null;
	}

	@Override
	protected String parseFootPrint(String sQuery) {
		return null;
	}

}
