package wasdi.shared.queryexecutors.skywatch.models;

import java.util.Date;
import java.util.List;

@lombok.Data
public class GetListPipelinesResponse {

	private List<Datum> data;
	private Pagination pagination;

	@lombok.Data
	public static class Aoi {
		private String type;
		private List<List<List<Double>>> coordinates;
	}

	@lombok.Data
	public static class Cursor {
		private String next;
		private String self;
	}

	@lombok.Data
	public static class Datum {
		private String id;
		private String name;
		private String start_date;
		private String end_date;
		private Double budget_per_km2;
		private Double max_cost;
		private Double resolution_low;
		private Double resolution_high;
		private Double cloud_cover_percentage;
		private Double min_aoi_coverage_percentage;
		private String interval;
		private ResultDelivery result_delivery;
		private Aoi aoi;
		private Double area_km2;
		private Output output;
		private List<Tag> tags;
		private String status;
		private StatusReason status_reason;
		private String search_id;
		private List<String> search_results;
		private Sources sources;
		private Date created_at;
		private Date updated_at;
	}

	@lombok.Data
	public static class Output {
		private String id;
		private String format;
		private String mosaic;
	}

	@lombok.Data
	public static class Pagination {
		private int per_page;
		private Cursor cursor;
		private int total;
		private int count;
	}

	@lombok.Data
	public static class Sources {
		private List<String> include;
	}

	@lombok.Data
	public static class StatusReason {
		private String reason;
		private String description;
	}

	@lombok.Data
	public static class Tag {
		private String label;
		private String value;
	}

	@lombok.Data
	public static class ResultDelivery {
		public String max_latency;
		public List<String> priorities;
	}

}