/**
 * Created by Cristiano Nattero on 2020-04-27
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch.eodc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import wasdi.shared.opensearch.DiasResponseTranslator;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.QueryResultViewModel;

/**
 * @author c.nattero
 *
 */
public class DiasResponseTranslatorEODC extends DiasResponseTranslator {
	
	static ObjectMapper s_oMapper = new ObjectMapper();

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.DiasResponseTranslator#translateBatch(java.lang.String, boolean, java.lang.String)
	 */
	@Override
	public List<QueryResultViewModel> translateBatch(String sJson, boolean bFullViewModel, String sDownloadProtocol) {
		
		ArrayList<QueryResultViewModel> aoResults = new ArrayList<QueryResultViewModel>();
		
		try {
			// Convert the string in a Json
			Map<String, Object> aoJsonResults = s_oMapper.readValue(sJson, new TypeReference<Map<String, Object>>() {});
			
			// Get the childs
			Map<String, Object> aoChilds = (Map<String, Object>) aoJsonResults.get("csw:GetRecordsResponse");
			
			// Access the SearchResults child
			Map<String, Object> aoSearchResults = (Map<String, Object>) aoChilds.get("csw:SearchResults");
			
			int iResultCount = 0;
			try {
				iResultCount = Integer.parseInt((String)aoSearchResults.get("@numberOfRecordsReturned"));
			} catch (Exception oE) {
				Utils.debugLog("DiasResponseTranslatorEODC.translateBatch: cast to int failed: " + oE);
			}
			
			ArrayList<Map<String, Object>> aoRecords = new ArrayList<Map<String, Object>>();
			
			try {
				if(iResultCount > 1) {
				// Access the list of records
					aoRecords = (ArrayList<Map<String, Object>>) aoSearchResults.get("csw:Record");
				} else {
					aoRecords.add((Map<String, Object>)aoSearchResults.get("csw:Record"));
				}
			}
			catch (Exception oE) {
				Utils.debugLog("DiasResponseTranslatorEODC.translateBatch: aoRecords creation failed: " + oE);
			}
			
			// For each record
			for (Map<String, Object> oRecord : aoRecords) {
				
				try {
					ArrayList<Map<String, Object>> aoReferences = (ArrayList<Map<String, Object>>) oRecord.get("dct:references");
					
					String sLink = "";
					
					for (Map<String, Object> oReference : aoReferences) {
						
						String sScheme = (String) oReference.get("@scheme");
						if (sScheme.equals("offlineAccess")) {
							sLink = (String) oReference.get("#text");
							break;
						}
					}
					
					Map<String, Object> oBoundingBox = (Map<String,Object>) oRecord.get("ows:BoundingBox");
					
					String sLowerCorner = (String) oBoundingBox.get("ows:LowerCorner");
					String sUpperCorner = (String) oBoundingBox.get("ows:UpperCorner");
					
					String [] asLatLonLower = sLowerCorner.split(" ");
					String [] asLatLonUpper = sUpperCorner.split(" ");
					
					String sFootPrint = "MULTIPOLYGON (((" + asLatLonLower[1] + " " + asLatLonLower [0] + "," + asLatLonLower[1] + " " + asLatLonUpper[0] + "," + asLatLonUpper[1] + " " + asLatLonUpper[0] + "," + asLatLonUpper[1] + " " + asLatLonLower [0] + "," + asLatLonLower[1] + " " + asLatLonLower [0] + ")))";
					
					// Create WASDI View Model
					QueryResultViewModel oResViewModel = new QueryResultViewModel();
					
					oResViewModel.setTitle( (String) oRecord.get("dc:title"));
					oResViewModel.setProvider("EODC");
					oResViewModel.setSummary("");
					oResViewModel.setLink("file://" + sLink);
					oResViewModel.setFootprint(sFootPrint);
					oResViewModel.getProperties().put("format", (String) oRecord.get("dc:format"));
					oResViewModel.getProperties().put("creationDate", (String) oRecord.get("dc:date"));					
					
					aoResults.add(oResViewModel);
				}
				catch (Exception oInnerEx) {
					Utils.debugLog("Exception:" + oInnerEx.toString());
				}

			}
			
			Utils.debugLog("Conversion done");
			
		} 
		catch (IOException e) {
			
			e.printStackTrace();
		}
		
		return aoResults;
	}
}
