package wasdi.shared.queryexecutors.sina;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import wasdi.shared.queryexecutors.ResponseTranslator;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;



public class ResponseTranslatorSina extends ResponseTranslator {

	public ResponseTranslatorSina() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<QueryResultViewModel> translateBatch(String sResponse, boolean bFullViewModel) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getCountResult(String sQueryResult) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	protected QueryResultViewModel translate(String sFileName) {
		
		if (Utils.isNullOrEmpty(sFileName)) {
			WasdiLog.warnLog("ResponseTranslatorSina.translate: file name is null or empty");
			return null;
		}
		
		QueryResultViewModel oResult = new QueryResultViewModel();
		oResult.setProvider("SINA");
		
		String sFileId = WasdiFileUtils.getFileNameWithoutLastExtension(sFileName);
		oResult.setId(sFileId);
		oResult.setTitle(sFileId);
		oResult.setLink("https://" + sFileName);
		
		String sFootPrint = "POLYGON ((6.6267 35.4922, 6.6267 47.0920, 18.5205 47.0920, 18.5205 35.4922, 6.6267 35.4922))";
		oResult.setFootprint(sFootPrint);
		
		List<String> asSummaryElements = new ArrayList<>();
		
		String[] asFileParts = sFileId.split("_");
		asSummaryElements.add("Date: " + asFileParts[1] + "-" + asFileParts[2] + "-01T00:00:00.000Z");
		asSummaryElements.add("Instrument: " + asFileParts[0]);
		asSummaryElements.add("Mode: " + asFileParts[0]);
		asSummaryElements.add("Satellite: " + asFileParts[0]);
		asSummaryElements.add("Size: 0GB");
		String sSummary = String.join(", ", asSummaryElements);
		oResult.setSummary(sSummary);
		
		Map<String, String> oProperties = new HashMap<String, String>();
		oProperties.put("fileName", sFileId);
		oProperties.put("sizeInBytes", "0");
		oProperties.put("size", "0 B");
		oProperties.put("platformname", "BIGBANG");
		
		oResult.setProperties(oProperties);
		oResult.setProvider("SINA");
		
		return oResult;
	}
	
	

}
