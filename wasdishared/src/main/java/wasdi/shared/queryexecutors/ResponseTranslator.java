/**
 * Created by Cristiano Nattero on 2018-12-11
 * 
 * Fadeout software
 *
 */
package wasdi.shared.queryexecutors;

import java.util.List;

import com.google.common.base.Preconditions;

import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

/**
 * Response translator Abstract class.
 * 
 * The goal of this class is to implement for a specific provider two services:
 * 
 *  	.Translate the provider response to a search query in a list of QueryResultViewModel
 *  	.Translate the provider response to a count query in a number that is the total count of results for that query
 * 
 * @author c.nattero
 *
 */
public abstract class ResponseTranslator {
	
	/**
	 * Converts the response to a search query of the provider in a list of QueryResultViewModel
	 * @param sResponse Response received from the Provider
	 * @param bFullViewModel True if the method should return a full view model, false for a light one
	 * @return List of QueryResultViewModel each representing a result obtained by the provider
	 */
	public abstract List<QueryResultViewModel> translateBatch(String sResponse, boolean bFullViewModel);
	
	/**
	 * Converts the response of the provider to a count query in a number that is the total count of results for that query
	 * @param sQueryResult
	 * @return
	 */
	public abstract int getCountResult(String sQueryResult);

	/**
	 * Safe Add property to a QueryResultViewModel
	 * @param oResult QueryResultViewModel to fill
	 * @param sKey Prop key
	 * @param sValue Prop value
	 */
	protected void addToProperties(QueryResultViewModel oResult, String sKey, String sValue) {
		Preconditions.checkNotNull(oResult, "DiasResponseTranslator.addToProperties: QueryResultViewModel is null");
		Preconditions.checkNotNull(oResult.getProperties(), "DiasResponseTranslator.addToProperties: null properties");
		try {
			if(!Utils.isNullOrEmpty(sKey) && null!=sValue){
				oResult.getProperties().put(sKey, sValue);
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("ResponseTranslator.addToProperties: " + oE);
		}
	}
}
