package wasdi.shared.queryexecutors.skywatch.models;

import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class PipelineResultsResponse {

	private List<Datum> data;
	private Pagination pagination;

	private List<Error> errors;

	@Data
	public class AlternateSearchResult {
		private int index;
		private String search_result_id;
		private String preview_url;
		private PreviewBbox preview_bbox;
		private String capture_date;
		private Double resolution;
		private Double price_per_km2;
		private Double max_interval_cost;
		private Double aoi_coverage_percentage;
		private Double filled_coverage_percentage;
		private String source;
		private String processing_status;
	}

	@Data
	public static class Band {
		private String name;
		private int no_data_value;
		private String data_type;
		private String unit;
		private int spectral_wavelength;
		private int raster_width;
		private int raster_height;
	}

	@Data
	public static class Cursor {
		private String next;
		private String self;
	}

	@Data
	public static class Datum {
		private String id;
		private String output_id;
		private String pipeline_id;
		private String status;
		private String message;
		private Interval interval;
		private int total_interval_cost;
		private ProbabilityOfCollection probability_of_collection;
		private OverallMetadata overall_metadata;
		private List<Result> results;
		private List<AlternateSearchResult> alternate_search_results;
		private Date created_at;
		private Date updated_at;
	}

	@Data
	public static class Interval {
		private String start_date;
		private String end_date;
	}

	@Data
	public static class Metadata {
		private ProcessingApplied processing_applied;
		private List<Band> bands;
		private int resolution_x;
		private int resolution_y;
		private String map_crs;
		private String sensor_mode;
		private String source;
		private int valid_pixels_percentage;
		private int cloud_cover_percentage;
		private String name;
		private int size_in_mb;
		private List<List<Double>> corner_coordinates;
	}

	@Data
	public static class OverallMetadata {
		private int scene_height;
		private int scene_width;
		private int filled_area_km2;
		private int filled_area_percentage_of_aoi;
		private int cloud_cover_percentage;
		private int cloud_cover_percentage_of_aoi;
		private int visible_area_km2;
		private int visible_area_percentage;
		private int visible_area_percentage_of_aoi;
	}

	@Data
	public static class Pagination {
		private int per_page;
		private Cursor cursor;
	}

	@Data
	public static class PreviewBbox {
		private String type;
		private List<List<List<Double>>> coordinates;
	}

	@Data
	public static class ProbabilityOfCollection {
		private int probability_percent;
		private String probability_reason;
	}

	@Data
	public static class ProcessingApplied {
		private boolean atmospheric_correction_toa;
		private boolean pansharpening;
		private boolean cloud_masking;
		private boolean haze_masking;
	}

	@Data
	public static class RasterFile {
		private String name;
		private String description;
		private String uri;
	}

	@Data
	public static class Result {
		private String preview_url;
		private String visual_url;
		private String analytics_url;
		private String metadata_url;
		private Date capture_time;
		private List<VectorFile> vector_files;
		private List<RasterFile> raster_files;
		private Metadata metadata;
		private Date created_at;
		private Date updated_at;
	}

	@Data
	public static class VectorFile {
		private String name;
		private String description;
		private String uri;
	}

	@Data
	@AllArgsConstructor
	public static class Error {
		private String message;
	}

}
