/**
 * Created by Cristiano Nattero on 2019-12-23
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch;

import java.util.ArrayList;

import wasdi.shared.viewmodels.QueryResultViewModel;

/**
 * @author c.nattero
 *
 */
public class DiasResponseTranslatorCREODIAS implements DiasResponseTranslator {

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.DiasResponseTranslator#translate(java.lang.Object, java.lang.String)
	 */
	@Override
	public QueryResultViewModel translate(Object oResponseViewModel, String sProtocol) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<QueryResultViewModel> translateBatch(String sJson, boolean bFullViewModel,
			String sDownloadProtocol) {
		// TODO Auto-generated method stub
		return null;
	}

}
