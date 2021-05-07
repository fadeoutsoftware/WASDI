/**
 * Created by Cristiano Nattero on 2020-04-27
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch.eodc;

import java.io.IOException;
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
	@SuppressWarnings("unchecked")
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
					
					String sTitle =  (String) oRecord.get("dc:identifier");
					
					oResViewModel.setTitle(sTitle);
					oResViewModel.setProvider("EODC");
					oResViewModel.setSummary("");
					oResViewModel.setLink("file://" + sLink);
					oResViewModel.setFootprint(sFootPrint);
					oResViewModel.getProperties().put("format", (String) oRecord.get("dc:format"));
					oResViewModel.getProperties().put("creationDate", (String) oRecord.get("dc:date"));
					
					// If this is a S1, try to get the relative Orbit (that is not in the data obtained from the provider)
					
					if (sTitle.startsWith("S1")) {
						
						// Split the title
						String [] asTitleParts = sTitle.split("_");
						
						// Safe check
						if (asTitleParts != null) {
							
							if (asTitleParts.length >= 9) {
								
								// Get the absolute orbit
								String sAbsoluteOrbit = asTitleParts[6];
								
								// Cast to int
								int iAbsoluteOrbit = -1;
								try {
									iAbsoluteOrbit = Integer.parseInt(sAbsoluteOrbit);
								}
								catch (Exception oEx) {
									Utils.debugLog("DiasResponseTranslatorEODC.translateBatch: Exception converting EODC Result Relative Orbit: "  + oEx.toString()) ;
								}
								
								if (iAbsoluteOrbit != -1) {
									
									// Init the relative orbit
									int iRelativeOrbit = -1;
									
									// S1A or S1B?
									if (sTitle.startsWith("S1A")) {
										iRelativeOrbit = (iAbsoluteOrbit-73)%175;
										iRelativeOrbit ++;
									}
									else if (sTitle.startsWith("S1B")) {
										iRelativeOrbit = (iAbsoluteOrbit-27)%175;
										iRelativeOrbit ++;										
									}
									
									// Set the relative orbit to the WASDI View Model
									oResViewModel.getProperties().put("relativeorbitnumber", ""+iRelativeOrbit);
								}
							}
						}
					}
					
					aoResults.add(oResViewModel);
				}
				catch (Exception oInnerEx) {
					Utils.debugLog("DiasResponseTranslatorEODC.translateBatch: " + oInnerEx.toString());
				}

			}
			
			Utils.debugLog("Conversion done");
			
		} 
		catch (IOException oE) {
			Utils.debugLog("DiasResponseTranslatorEODC.translateBatch: outer exception: " + oE);
		}
		
		return aoResults;
	}
}
