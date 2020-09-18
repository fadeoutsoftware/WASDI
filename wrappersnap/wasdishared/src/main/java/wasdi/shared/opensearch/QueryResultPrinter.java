/**
 * Created by Cristiano Nattero on 2019-01-30
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.QueryResultViewModel;

/**
 * @author c.nattero
 *
 */
public class QueryResultPrinter {

	public void print(QueryResultViewModel oVm ) {
		List<String> sListedVm = list(oVm);
		for (String sLine : sListedVm) {
			Utils.debugLog(sLine);
		}
	}

	public void writeToFile(QueryResultViewModel oVm, String sFileName) {
		try {
			List<String> sListedVm = list(oVm);
			try (FileWriter oFileWeriter = new FileWriter(sFileName)) {
				for (String sLine : sListedVm) {
					oFileWeriter.write(sLine);
				}
				oFileWeriter.close();				
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private List<String> list(QueryResultViewModel oVm){
		ArrayList<String> asResult = new ArrayList<>();
		String sLine = null;
		
		sLine = "footprint :" + oVm.getFootprint();
		asResult.add(sLine);
		sLine = "id : " + oVm.getId();
		asResult.add(sLine);
		sLine = "link : " + oVm.getLink();
		asResult.add(sLine);
		sLine = oVm.getPreview();
		if(null != sLine ) {
			sLine = "preview : " + sLine.substring(0, 60) + " ...";
		} else {
			sLine = "preview : null";
		}
		asResult.add(sLine);
		sLine = "provider : " + oVm.getProvider();
		asResult.add(sLine);
		sLine = "summary : " + oVm.getSummary();
		asResult.add(sLine);
		sLine = "title : " + oVm.getTitle();
		asResult.add(sLine);
		sLine = "properties:";
		asResult.add(sLine);
		Set<String> asKeys = oVm.getProperties().keySet();
		for (String sKey : asKeys) {
			sLine = "  " + sKey + " : " + oVm.getProperties().get(sKey);
			asResult.add(sLine);
		}
		return asResult;
	}

}
