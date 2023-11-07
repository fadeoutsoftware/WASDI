/**
 * Created by Cristiano Nattero on 2020-04-27
 * 
 * Fadeout software
 *
 */
package wasdi.shared.queryexecutors.eodc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import wasdi.shared.queryexecutors.ResponseTranslator;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

/**
 * @author c.nattero
 *
 */
public class ResponseTranslatorEODC extends ResponseTranslator {
	
	private static final String s_sGetRecordResponse = "csw:GetRecordsResponse";
	private static final String s_sSearchResults = "csw:SearchResults";
	
	static ObjectMapper s_oMapper = new ObjectMapper();

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.DiasResponseTranslator#translateBatch(java.lang.String, boolean, java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<QueryResultViewModel> translateBatch(String sJson, boolean bFullViewModel) {
		
		ArrayList<QueryResultViewModel> aoResults = new ArrayList<QueryResultViewModel>();
		
		try {
			// Convert the string in a Json
			Map<String, Object> aoJsonResults = s_oMapper.readValue(sJson, new TypeReference<Map<String, Object>>() {});
			
			// Get the childs
			Map<String, Object> aoChilds = (Map<String, Object>) aoJsonResults.get(s_sGetRecordResponse);
			
			// Access the SearchResults child
			Map<String, Object> aoSearchResults = (Map<String, Object>) aoChilds.get(s_sSearchResults);
			
			int iResultCount = 0;
			try {
				iResultCount = Integer.parseInt((String)aoSearchResults.get("@numberOfRecordsReturned"));
			} catch (Exception oE) {
				WasdiLog.debugLog("ResponseTranslatorEODC.translateBatch: cast to int failed: " + oE);
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
				WasdiLog.debugLog("ResponseTranslatorEODC.translateBatch: aoRecords creation failed: " + oE);
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
									WasdiLog.debugLog("ResponseTranslatorEODC.translateBatch: Exception converting EODC Result Relative Orbit: "  + oEx.toString()) ;
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
					WasdiLog.debugLog("ResponseTranslatorEODC.translateBatch: " + oInnerEx.toString());
				}

			}
			
			WasdiLog.debugLog("Conversion done");
			
		} 
		catch (IOException oE) {
			WasdiLog.debugLog("ResponseTranslatorEODC.translateBatch: outer exception: " + oE);
		}
		
		return aoResults;
	}

	@Override
	public int getCountResult(String sQueryResult) {
		//parse response json and extract number

		if(Utils.isNullOrEmpty(sQueryResult)) {
			WasdiLog.debugLog("ResponseTranslatorEODC.getCountResult: response is null, aborting");
			return -1;
		}

		JSONObject oJson = null;
		try {
			oJson = new JSONObject(sQueryResult);
		}
		catch (Exception oE) {
			WasdiLog.debugLog("ResponseTranslatorEODC.getCountResult: could not create JSONObject (syntax error or duplicate key), aborting");
			return -1;
		}

		if(!oJson.has(s_sGetRecordResponse)) {
			WasdiLog.debugLog("ResponseTranslatorEODC.getCountResult: \"csw:GetRecordsResponse\" not found in JSON, aborting");
			return -1;
		}

		JSONObject oCswGetRecordsResponse = oJson.optJSONObject(s_sGetRecordResponse);
		if(null==oCswGetRecordsResponse) {
			WasdiLog.debugLog("ResponseTranslatorEODC.getCountResult: could not access JSONObject at \"csw:GetRecordsResponse\", aborting");
			return -1;
		}

		if(!oCswGetRecordsResponse.has(s_sSearchResults)) {
			WasdiLog.debugLog("ResponseTranslatorEODC.getCountResult: \"csw:SearchResults\" not found in \"csw:GetRecordsResponse\", aborting");
			return -1;
		}

		JSONObject oSearchResults = oCswGetRecordsResponse.optJSONObject(s_sSearchResults);
		if(null==oSearchResults) {
			WasdiLog.debugLog("ResponseTranslatorEODC.getCountResult: could not access JSONObject at \"csw:SearchResults\", aborting");
			return -1;
		}

		if(!oSearchResults.has("@numberOfRecordsMatched")) {
			WasdiLog.debugLog("ResponseTranslatorEODC.getCountResult: \"@numberOfRecordsMatched\" not found int \"csw:SearchResults\", aborting");
			return -1;
		}

		int iResults = oSearchResults.optInt("@numberOfRecordsMatched", -1);
		if(iResults < 0) {
			WasdiLog.debugLog("ResponseTranslatorEODC.getCountResult: could not access \"@numberOfRecordsMatched\", aborting");
			return -1;
		}
		
		return iResults;
	}
}
