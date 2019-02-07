/**
 * Created by Cristiano Nattero on 2019-01-31
 * 
 * Fadeout software
 *
 */
package wasdi.shared.utils;

/**
 * @author c.nattero
 *
 */
public class QueryUtils {

	public boolean sameFootprint(String sFootprint0, String sFootprint1 ) {
		//TODO implement. It's not enough for strings to equal each other, they must be semantically the same 
		if(sFootprint0.equals(sFootprint1)) {
			return true;
		}
		return true;
	}  
	
	public boolean sameFootprintOrNull(String sCurrentFootprint, String sCanBeNullFootprint ) {
		if(Utils.isNullOrEmpty(sCanBeNullFootprint)) {
			return true;
		} else {
			return sameFootprint(sCurrentFootprint, sCanBeNullFootprint);
		}
	}

}
