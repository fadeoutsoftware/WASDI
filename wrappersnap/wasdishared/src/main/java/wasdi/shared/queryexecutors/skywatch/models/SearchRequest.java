package wasdi.shared.queryexecutors.skywatch.models;

import java.util.List;

import lombok.Data;

@Data
public class SearchRequest {

    private Location location;
    private String start_date;
    private String end_date;
    private List<String> resolution;
    private Double coverage;
    private int interval_length;
    private List<String> order_by;


	@Data
	public static class Location {
		private String type;
		private List<List<List<Double>>> coordinates;
//		private List<Double> bbox;
	}

}
