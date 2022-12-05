package wasdi.shared.queryexecutors.skywatch.models;

import java.util.Date;
import java.util.List;

@lombok.Data
public class SearchResponse {

	private Data data;

	private List<Error> errors;

	@lombok.Data
	public class Data {
		private String id;
		private Location location;
		private String start_date;
		private String end_date;
		private List<String> resolution;
		private Double coverage;
		private int interval_length;
		private List<String> order_by;
		private Date created_at;
	}

	@lombok.Data
	public static class Location {
		private String type;
		private List<List<List<Double>>> coordinates;
		private List<Double> bbox;
	}

	@lombok.Data
	public static class Error {
		private String message;
	}

}