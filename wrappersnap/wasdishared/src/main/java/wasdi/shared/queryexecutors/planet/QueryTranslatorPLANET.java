package wasdi.shared.queryexecutors.planet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;

import wasdi.shared.data.MongoRepository;
import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.QueryTranslator;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.search.QueryViewModel;

public class QueryTranslatorPLANET extends QueryTranslator {

	@Override
	public String getCountUrl(String sQuery) {
		
		return null;
	}

	@Override
	public String getSearchUrl(PaginatedQuery oQuery) {
		
		// Parse the WASDI query
		QueryViewModel oWasdiQuery = parseWasdiClientQuery(oQuery.getQuery());
		
		// Prepare the body
		Map<String, Object> oPlanetQueryBody = new HashMap<String, Object>();
		
		// Add the item-type
		String [] asItemTypes = new String[1];
		asItemTypes[0] = oWasdiQuery.productType; 
		
		Map<String, Object> oDateFilter = null;
		
		// Do we have a date filter?
		if (!Utils.isNullOrEmpty(oWasdiQuery.startFromDate) && ! Utils.isNullOrEmpty(oWasdiQuery.endToDate)) {
			oDateFilter = new HashMap<String, Object>();
			
			oDateFilter.put("type", "DateRangeFilter");
			oDateFilter.put("field_name", "acquired");
			
			Map<String, Object> oDateRange = new HashMap<String, Object>();
			oDateRange.put("gte", oWasdiQuery.startFromDate);
			oDateRange.put("lte", oWasdiQuery.endToDate);
			
			oDateFilter.put("config", oDateRange);
		}
		
		Map<String, Object> oGeometryFilter = null;
		
		if (oWasdiQuery.east != null && oWasdiQuery.west != null && oWasdiQuery.north != null && oWasdiQuery.south != null) {
			oGeometryFilter = new HashMap<String, Object>();
			
			oGeometryFilter.put("type", "GeometryFilter");
			oGeometryFilter.put("field_name", "geometry");
			
			Map<String, Object> oPolygon = new HashMap<String, Object>();
			oPolygon.put("type", "Polygon");
			
			oPolygon.put("coordinates", getCoordinateObject(oWasdiQuery));
			
			oGeometryFilter.put("config", oPolygon);
		}
		
		if (oDateFilter != null && oGeometryFilter != null) {
			Map<String, Object> oAndFilter = new HashMap<String, Object>();
			
			oAndFilter.put("type", "AndFilter");
			
			ArrayList<Map<String, Object>> aoFilters = new ArrayList<Map<String,Object>>();
			aoFilters.add(oDateFilter);
			aoFilters.add(oGeometryFilter);
			
			oAndFilter.put("config", aoFilters);
			
			oPlanetQueryBody.put("filter", oAndFilter);
		}
		else if (oDateFilter != null) {
			oPlanetQueryBody.put("filter", oDateFilter);
		}
		else if (oGeometryFilter != null) {
			oPlanetQueryBody.put("filter", oGeometryFilter);
		}
		
		oPlanetQueryBody.put("item_types", asItemTypes);
		
		String sOutputQuery = "";
		
		try {
			sOutputQuery = MongoRepository.s_oMapper.writeValueAsString(oPlanetQueryBody);
		} catch (JsonProcessingException e) {
			
		}
		
		return sOutputQuery;
	}
	
	Object getCoordinateObject(QueryViewModel oWasdiQuery) {
		
		Double[] oPoint1 = new Double[2];
		Double[] oPoint2 = new Double[2];
		Double[] oPoint3 = new Double[2];
		Double[] oPoint4 = new Double[2];
		Double[] oPoint5 = new Double[2];
		
		oPoint1[0] = oWasdiQuery.east;
		oPoint1[1] = oWasdiQuery.south;

		oPoint2[0] = oWasdiQuery.east;
		oPoint2[1] = oWasdiQuery.north;

		oPoint3[0] = oWasdiQuery.west;
		oPoint3[1] = oWasdiQuery.north;

		oPoint4[0] = oWasdiQuery.west;
		oPoint4[1] = oWasdiQuery.south;

		oPoint5[0] = oWasdiQuery.east;
		oPoint5[1] = oWasdiQuery.north;

		ArrayList<Double[]> aoPoints = new ArrayList<Double[]>();
		aoPoints.add(oPoint1);
		aoPoints.add(oPoint2);
		aoPoints.add(oPoint3);
		aoPoints.add(oPoint4);
		aoPoints.add(oPoint5);
		
		ArrayList<ArrayList<Double[]>> oReturn = new ArrayList<ArrayList<Double[]>>();
		
		oReturn.add(aoPoints);
		
		return oReturn;
	}

}


