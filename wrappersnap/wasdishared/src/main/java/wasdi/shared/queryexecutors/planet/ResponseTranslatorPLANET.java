package wasdi.shared.queryexecutors.planet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;

import wasdi.shared.data.MongoRepository;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.ResponseTranslator;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

public class ResponseTranslatorPLANET extends ResponseTranslator {

	@SuppressWarnings("unchecked")
	@Override
	public List<QueryResultViewModel> translateBatch(String sResponse, boolean bFullViewModel) {
		
		try {
			
			// Create the return List
			ArrayList<QueryResultViewModel> aoWasdiResults = new ArrayList<QueryResultViewModel>();
			
			// Convert the response in the relative JSON Map representation
			TypeReference<HashMap<String,Object>> oMapType = new TypeReference<HashMap<String,Object>>() {};
			HashMap<String,Object> oResults = MongoRepository.s_oMapper.readValue(sResponse, oMapType);
			
			// Get the list of all the items
			List<HashMap<String,Object>> aoPlanetResults = (List<HashMap<String,Object>>) oResults.get("features");
			
			// For all the items 
			for (HashMap<String, Object> oPlanetResult : aoPlanetResults) {
				
				try {
					// Create the WASDI result
					QueryResultViewModel oWasdiResult = new QueryResultViewModel();
					
					// Initialize the file name
					String sFileName = Platforms.PLANET;
					
					// Read item id
					String sId = (String) oPlanetResult.get("id");
					HashMap<String, Object> oProperties = (HashMap<String, Object>) oPlanetResult.get("properties");
					
					// Read Item Date
					String sDateTime = "";
					
					if (oProperties.containsKey("acquired")) {
						sDateTime = (String) oProperties.get("acquired");
					}
					
					// Read Item Instrument
					String sInstrument = "";
					if (oProperties.containsKey("instrument")) {
						sInstrument = (String) oProperties.get("instrument");
					}
					
					// Read Item Type
					String sItemType = "";
					
					if (oProperties.containsKey("item_type")) {
						sItemType = (String) oProperties.get("item_type");
					}
					
					// Extract only the date from datetime
					String [] asDateParts = sDateTime.split("T");
					
					String sDateForFile = "YYYYMMDD";
					if (asDateParts!=null) {
						if (asDateParts.length>0) {
							sDateForFile = asDateParts[0];
						}
					}
					
					// Create a filename
					sFileName += "_" + sItemType + "_" + sDateForFile + "_" + sId;
					
					String sSummary = "Date: " + sDateTime + ", Instrument: " + sInstrument + ", Mode: NA, Satellite: " + sItemType + ", Size: NA";
					
					// Set file name summary and id
					oWasdiResult.setTitle(sFileName);
					oWasdiResult.setId(sId);
					oWasdiResult.setSummary(sSummary);
					
					oWasdiResult.setLink(sId);
					
					if (oPlanetResult.containsKey("_links")) {
						HashMap<String, Object> oLinks = (HashMap<String, Object>) oPlanetResult.get("_links");
						
						if (oLinks.containsKey("assets")) {
							oWasdiResult.setLink(oLinks.get("assets").toString());
						}
					}
					
					// Set date and intrument
					oWasdiResult.getProperties().put("date", sDateTime);
					oWasdiResult.getProperties().put("instrument", sInstrument);
					oWasdiResult.getProperties().put("relativeOrbit", "0");
					oWasdiResult.getProperties().put("platformname", sItemType);
					
					if (oPlanetResult.containsKey("geometry")) {
						HashMap<String, Object> oGeometry = (HashMap<String, Object>) oPlanetResult.get("geometry");
						
						if (oGeometry.containsKey("coordinates")) {
							
							try {
								ArrayList<ArrayList<ArrayList<Double>>> aoCoordinates = (ArrayList<ArrayList<ArrayList<Double>>>) oGeometry.get("coordinates");
								
								ArrayList<ArrayList<Double>> aoPoints = aoCoordinates.get(0);
								
								String sFootprint = "POLYGON((";
								
								int iPoints = 0;
								
								for (ArrayList<Double> oPoint : aoPoints) {
									
									if (iPoints>0) sFootprint += ", ";
									
									sFootprint += oPoint.get(0).toString() + " " + oPoint.get(1).toString();
									
									iPoints++;
								}
								
								sFootprint += "))";
								
								oWasdiResult.setFootprint(sFootprint);
							}
							catch (Exception oEx) {
								WasdiLog.debugLog("ResponseTranslatorPLANET.translateBatch: Exception reding footprint of one result = " + oEx.toString());
							}							
							
						}
					}
					
					aoWasdiResults.add(oWasdiResult);
				}
				catch (Exception oEx) {
					WasdiLog.debugLog("ResponseTranslatorPLANET.translateBatch: Exception converting one result = " + oEx.toString());
				}							

			}
			
			return aoWasdiResults;
			
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("ResponseTranslatorPLANET.translateBatch: Exception = " + oEx.toString());
		}			
		return null;
	}

	@Override
	public int getCountResult(String sQueryResult) {
		try {
			TypeReference<HashMap<String,Object>> oMapType = new TypeReference<HashMap<String,Object>>() {};
			
			HashMap<String,Object> oResults = MongoRepository.s_oMapper.readValue(sQueryResult, oMapType);			
			
			// TODO: not precise at all, we do not have a count
			return oResults.size();
			
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("ResponseTranslatorPLANET.getCountResult: Exception = " + oEx.toString());
		}			
		return -1;
	}
}
