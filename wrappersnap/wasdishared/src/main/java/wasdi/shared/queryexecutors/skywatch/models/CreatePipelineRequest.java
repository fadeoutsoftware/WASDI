package wasdi.shared.queryexecutors.skywatch.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreatePipelineRequest {

	private String name;
	private String start_date;
	private String end_date;
	private Double budget_per_km2;
	private Double max_cost;
	private Double cloud_cover_percentage;
	private Double min_aoi_coverage_percentage;
	private Double resolution_low;
	private Double resolution_high;
	private String interval;
	private ResultDelivery result_delivery;
	private Aoi aoi;
	private Output output;
	private List<Tag> tags;
	private String status;
	private Sources sources;
	private String search_id;
	private List<String> search_results;


	@Data
	public static class Aoi {
	    private String type;
	    private List<List<List<Double>>> coordinates;
	}

	@Data
	public static class Output {
		private String id;
		private String format;
		private String mosaic;
	}

	@Data
	public static class ResultDelivery {
		private String max_latency;
		private List<String> priorities;
	}

	@Data
	public static class Sources {
		private List<String> include;
	}

	@Data
	public static class Tag {
		private String label;
		private String value;
	}

}
