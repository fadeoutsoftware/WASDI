package wasdi.shared.queryexecutors.skywatch.models;

import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class SearchResultsResponse {

    private List<Datum> data;
    private Pagination pagination;
   
    private List<Status> status;

	private List<Error> errors;

	@Data
	public static class Datum {
		private String searchId;

		private String id;
		private Location location;
		private Date start_time;
		private Date end_time;
		private String preview_uri;
		private String thumbnail_uri;
		private Double resolution;
		private String source;
		private String product_name;
		private Double location_coverage_percentage;
		private Double area_sq_km;
		private Double cost;
		private Double available_credit;
		private Double result_cloud_cover_percentage;
	}

	@Data
	public static class Location {
		private String type;
		private List<List<List<Double>>> coordinates;
		private List<Double> bbox;
	}

	@Data
	public static class Pagination {
		private Integer per_page;
		private Cursor cursor;
		private Integer total;
		private Integer count;
	}

	@Data
	public static class Cursor {
		private Integer next;
		private Integer self;
	}

	@Data
	public static class Status{
		private String message;
	}

	@Data
	@AllArgsConstructor
	public static class Error {
		private String message;
	}

}
