/**
 * Created by Cristiano Nattero on 2018-12-07
 * 
 * Fadeout software
 *
 */
package wasdi.shared.viewmodels;

/**
 * @author c.nattero
 *
 */
public class QueryResultViewModelONDA extends QueryResultViewModel {

	public QueryResultViewModelONDA() {
		//ONDA key, WASDI key
		asProviderToWasdiKeyMap.put("id", "id");
		asProviderToWasdiKeyMap.put("footprint","footprint");
		asProviderToWasdiKeyMap.put("preview","quicklook");
		asProviderToWasdiKeyMap.put("name","title");
		provider = "ONDA";
	}

	@Override
	public void buildSummary() {
		//example:
		//summary : "Date: 2018-11-01T17:22:56.428Z, Instrument: SAR-C SAR, Mode: VH VV, Satellite: Sentinel-1, Size: 1.6 GB"
		summary = new String ("");
		String sSeparator = ", ";
		//Date: YYYY-MM-DDTHH:MM:SS.123Z
		String sDate = "Date: " + properties.get("creationDate");
		summary = summary+sDate;
		//XXX no instrument so far, however we could get it from the query
		//Instrument: SAR-C SAR
		//String sInstrument = "Instrument: ";
		
	}
}
