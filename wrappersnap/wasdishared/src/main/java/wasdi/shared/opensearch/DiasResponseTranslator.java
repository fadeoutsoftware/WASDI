/**
 * Created by Cristiano Nattero on 2018-12-11
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch;

import java.util.List;

import com.google.common.base.Preconditions;

import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

/**
 * @author c.nattero
 *
 */
public abstract class DiasResponseTranslator {
	
	public abstract List<QueryResultViewModel> translateBatch(String sJson, boolean bFullViewModel, String sDownloadProtocol);

	
	protected void addToProperties(QueryResultViewModel oResult, String sKey, String sValue) {
		Preconditions.checkNotNull(oResult, "DiasResponseTranslator.addToProperties: QueryResultViewModel is null");
		Preconditions.checkNotNull(oResult.getProperties(), "DiasResponseTranslator.addToProperties: null properties");
		try {
			if(!Utils.isNullOrEmpty(sKey) && null!=sValue){
				oResult.getProperties().put(sKey, sValue);
			}
		} catch (Exception oE) {
			Utils.debugLog("DiasResponseTranslator.addToProperties: " + oE);
		}
	}
}
