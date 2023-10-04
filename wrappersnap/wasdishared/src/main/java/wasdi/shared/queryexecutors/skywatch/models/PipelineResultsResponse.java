package wasdi.shared.queryexecutors.skywatch.models;

import java.util.Date;
import java.util.List;

public class PipelineResultsResponse {

	private List<Datum> data;
	private Pagination pagination;

	private List<Error> errors;

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
		public int getIndex() {
			return index;
		}
		public void setIndex(int index) {
			this.index = index;
		}
		public String getSearch_result_id() {
			return search_result_id;
		}
		public void setSearch_result_id(String search_result_id) {
			this.search_result_id = search_result_id;
		}
		public String getPreview_url() {
			return preview_url;
		}
		public void setPreview_url(String preview_url) {
			this.preview_url = preview_url;
		}
		public PreviewBbox getPreview_bbox() {
			return preview_bbox;
		}
		public void setPreview_bbox(PreviewBbox preview_bbox) {
			this.preview_bbox = preview_bbox;
		}
		public String getCapture_date() {
			return capture_date;
		}
		public void setCapture_date(String capture_date) {
			this.capture_date = capture_date;
		}
		public Double getResolution() {
			return resolution;
		}
		public void setResolution(Double resolution) {
			this.resolution = resolution;
		}
		public Double getPrice_per_km2() {
			return price_per_km2;
		}
		public void setPrice_per_km2(Double price_per_km2) {
			this.price_per_km2 = price_per_km2;
		}
		public Double getMax_interval_cost() {
			return max_interval_cost;
		}
		public void setMax_interval_cost(Double max_interval_cost) {
			this.max_interval_cost = max_interval_cost;
		}
		public Double getAoi_coverage_percentage() {
			return aoi_coverage_percentage;
		}
		public void setAoi_coverage_percentage(Double aoi_coverage_percentage) {
			this.aoi_coverage_percentage = aoi_coverage_percentage;
		}
		public Double getFilled_coverage_percentage() {
			return filled_coverage_percentage;
		}
		public void setFilled_coverage_percentage(Double filled_coverage_percentage) {
			this.filled_coverage_percentage = filled_coverage_percentage;
		}
		public String getSource() {
			return source;
		}
		public void setSource(String source) {
			this.source = source;
		}
		public String getProcessing_status() {
			return processing_status;
		}
		public void setProcessing_status(String processing_status) {
			this.processing_status = processing_status;
		}
	}

	public static class Band {
		private String name;
		private int no_data_value;
		private String data_type;
		private String unit;
		private int spectral_wavelength;
		private int raster_width;
		private int raster_height;
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public int getNo_data_value() {
			return no_data_value;
		}
		public void setNo_data_value(int no_data_value) {
			this.no_data_value = no_data_value;
		}
		public String getData_type() {
			return data_type;
		}
		public void setData_type(String data_type) {
			this.data_type = data_type;
		}
		public String getUnit() {
			return unit;
		}
		public void setUnit(String unit) {
			this.unit = unit;
		}
		public int getSpectral_wavelength() {
			return spectral_wavelength;
		}
		public void setSpectral_wavelength(int spectral_wavelength) {
			this.spectral_wavelength = spectral_wavelength;
		}
		public int getRaster_width() {
			return raster_width;
		}
		public void setRaster_width(int raster_width) {
			this.raster_width = raster_width;
		}
		public int getRaster_height() {
			return raster_height;
		}
		public void setRaster_height(int raster_height) {
			this.raster_height = raster_height;
		}
	}

	public static class Cursor {
		private String next;
		private String self;
		public String getNext() {
			return next;
		}
		public void setNext(String next) {
			this.next = next;
		}
		public String getSelf() {
			return self;
		}
		public void setSelf(String self) {
			this.self = self;
		}
	}

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
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public String getOutput_id() {
			return output_id;
		}
		public void setOutput_id(String output_id) {
			this.output_id = output_id;
		}
		public String getPipeline_id() {
			return pipeline_id;
		}
		public void setPipeline_id(String pipeline_id) {
			this.pipeline_id = pipeline_id;
		}
		public String getStatus() {
			return status;
		}
		public void setStatus(String status) {
			this.status = status;
		}
		public String getMessage() {
			return message;
		}
		public void setMessage(String message) {
			this.message = message;
		}
		public Interval getInterval() {
			return interval;
		}
		public void setInterval(Interval interval) {
			this.interval = interval;
		}
		public int getTotal_interval_cost() {
			return total_interval_cost;
		}
		public void setTotal_interval_cost(int total_interval_cost) {
			this.total_interval_cost = total_interval_cost;
		}
		public ProbabilityOfCollection getProbability_of_collection() {
			return probability_of_collection;
		}
		public void setProbability_of_collection(ProbabilityOfCollection probability_of_collection) {
			this.probability_of_collection = probability_of_collection;
		}
		public OverallMetadata getOverall_metadata() {
			return overall_metadata;
		}
		public void setOverall_metadata(OverallMetadata overall_metadata) {
			this.overall_metadata = overall_metadata;
		}
		public List<Result> getResults() {
			return results;
		}
		public void setResults(List<Result> results) {
			this.results = results;
		}
		public List<AlternateSearchResult> getAlternate_search_results() {
			return alternate_search_results;
		}
		public void setAlternate_search_results(List<AlternateSearchResult> alternate_search_results) {
			this.alternate_search_results = alternate_search_results;
		}
		public Date getCreated_at() {
			return created_at;
		}
		public void setCreated_at(Date created_at) {
			this.created_at = created_at;
		}
		public Date getUpdated_at() {
			return updated_at;
		}
		public void setUpdated_at(Date updated_at) {
			this.updated_at = updated_at;
		}
	}

	public static class Interval {
		private String start_date;
		private String end_date;
		public String getStart_date() {
			return start_date;
		}
		public void setStart_date(String start_date) {
			this.start_date = start_date;
		}
		public String getEnd_date() {
			return end_date;
		}
		public void setEnd_date(String end_date) {
			this.end_date = end_date;
		}
	}

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
		public ProcessingApplied getProcessing_applied() {
			return processing_applied;
		}
		public void setProcessing_applied(ProcessingApplied processing_applied) {
			this.processing_applied = processing_applied;
		}
		public List<Band> getBands() {
			return bands;
		}
		public void setBands(List<Band> bands) {
			this.bands = bands;
		}
		public int getResolution_x() {
			return resolution_x;
		}
		public void setResolution_x(int resolution_x) {
			this.resolution_x = resolution_x;
		}
		public int getResolution_y() {
			return resolution_y;
		}
		public void setResolution_y(int resolution_y) {
			this.resolution_y = resolution_y;
		}
		public String getMap_crs() {
			return map_crs;
		}
		public void setMap_crs(String map_crs) {
			this.map_crs = map_crs;
		}
		public String getSensor_mode() {
			return sensor_mode;
		}
		public void setSensor_mode(String sensor_mode) {
			this.sensor_mode = sensor_mode;
		}
		public String getSource() {
			return source;
		}
		public void setSource(String source) {
			this.source = source;
		}
		public int getValid_pixels_percentage() {
			return valid_pixels_percentage;
		}
		public void setValid_pixels_percentage(int valid_pixels_percentage) {
			this.valid_pixels_percentage = valid_pixels_percentage;
		}
		public int getCloud_cover_percentage() {
			return cloud_cover_percentage;
		}
		public void setCloud_cover_percentage(int cloud_cover_percentage) {
			this.cloud_cover_percentage = cloud_cover_percentage;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public int getSize_in_mb() {
			return size_in_mb;
		}
		public void setSize_in_mb(int size_in_mb) {
			this.size_in_mb = size_in_mb;
		}
		public List<List<Double>> getCorner_coordinates() {
			return corner_coordinates;
		}
		public void setCorner_coordinates(List<List<Double>> corner_coordinates) {
			this.corner_coordinates = corner_coordinates;
		}
	}

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
		public int getScene_height() {
			return scene_height;
		}
		public void setScene_height(int scene_height) {
			this.scene_height = scene_height;
		}
		public int getScene_width() {
			return scene_width;
		}
		public void setScene_width(int scene_width) {
			this.scene_width = scene_width;
		}
		public int getFilled_area_km2() {
			return filled_area_km2;
		}
		public void setFilled_area_km2(int filled_area_km2) {
			this.filled_area_km2 = filled_area_km2;
		}
		public int getFilled_area_percentage_of_aoi() {
			return filled_area_percentage_of_aoi;
		}
		public void setFilled_area_percentage_of_aoi(int filled_area_percentage_of_aoi) {
			this.filled_area_percentage_of_aoi = filled_area_percentage_of_aoi;
		}
		public int getCloud_cover_percentage() {
			return cloud_cover_percentage;
		}
		public void setCloud_cover_percentage(int cloud_cover_percentage) {
			this.cloud_cover_percentage = cloud_cover_percentage;
		}
		public int getCloud_cover_percentage_of_aoi() {
			return cloud_cover_percentage_of_aoi;
		}
		public void setCloud_cover_percentage_of_aoi(int cloud_cover_percentage_of_aoi) {
			this.cloud_cover_percentage_of_aoi = cloud_cover_percentage_of_aoi;
		}
		public int getVisible_area_km2() {
			return visible_area_km2;
		}
		public void setVisible_area_km2(int visible_area_km2) {
			this.visible_area_km2 = visible_area_km2;
		}
		public int getVisible_area_percentage() {
			return visible_area_percentage;
		}
		public void setVisible_area_percentage(int visible_area_percentage) {
			this.visible_area_percentage = visible_area_percentage;
		}
		public int getVisible_area_percentage_of_aoi() {
			return visible_area_percentage_of_aoi;
		}
		public void setVisible_area_percentage_of_aoi(int visible_area_percentage_of_aoi) {
			this.visible_area_percentage_of_aoi = visible_area_percentage_of_aoi;
		}
	}

	public static class Pagination {
		private int per_page;
		private Cursor cursor;
		public int getPer_page() {
			return per_page;
		}
		public void setPer_page(int per_page) {
			this.per_page = per_page;
		}
		public Cursor getCursor() {
			return cursor;
		}
		public void setCursor(Cursor cursor) {
			this.cursor = cursor;
		}
	}

	public static class PreviewBbox {
		private String type;
		private List<List<List<Double>>> coordinates;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public List<List<List<Double>>> getCoordinates() {
			return coordinates;
		}
		public void setCoordinates(List<List<List<Double>>> coordinates) {
			this.coordinates = coordinates;
		}
	}

	public static class ProbabilityOfCollection {
		private int probability_percent;
		private String probability_reason;
		public int getProbability_percent() {
			return probability_percent;
		}
		public void setProbability_percent(int probability_percent) {
			this.probability_percent = probability_percent;
		}
		public String getProbability_reason() {
			return probability_reason;
		}
		public void setProbability_reason(String probability_reason) {
			this.probability_reason = probability_reason;
		}
	}

	public static class ProcessingApplied {
		private boolean atmospheric_correction_toa;
		private boolean pansharpening;
		private boolean cloud_masking;
		private boolean haze_masking;
		public boolean isAtmospheric_correction_toa() {
			return atmospheric_correction_toa;
		}
		public void setAtmospheric_correction_toa(boolean atmospheric_correction_toa) {
			this.atmospheric_correction_toa = atmospheric_correction_toa;
		}
		public boolean isPansharpening() {
			return pansharpening;
		}
		public void setPansharpening(boolean pansharpening) {
			this.pansharpening = pansharpening;
		}
		public boolean isCloud_masking() {
			return cloud_masking;
		}
		public void setCloud_masking(boolean cloud_masking) {
			this.cloud_masking = cloud_masking;
		}
		public boolean isHaze_masking() {
			return haze_masking;
		}
		public void setHaze_masking(boolean haze_masking) {
			this.haze_masking = haze_masking;
		}
	}

	public static class RasterFile {
		private String name;
		private String description;
		private String uri;
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		public String getUri() {
			return uri;
		}
		public void setUri(String uri) {
			this.uri = uri;
		}
	}

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
		public String getPreview_url() {
			return preview_url;
		}
		public void setPreview_url(String preview_url) {
			this.preview_url = preview_url;
		}
		public String getVisual_url() {
			return visual_url;
		}
		public void setVisual_url(String visual_url) {
			this.visual_url = visual_url;
		}
		public String getAnalytics_url() {
			return analytics_url;
		}
		public void setAnalytics_url(String analytics_url) {
			this.analytics_url = analytics_url;
		}
		public String getMetadata_url() {
			return metadata_url;
		}
		public void setMetadata_url(String metadata_url) {
			this.metadata_url = metadata_url;
		}
		public Date getCapture_time() {
			return capture_time;
		}
		public void setCapture_time(Date capture_time) {
			this.capture_time = capture_time;
		}
		public List<VectorFile> getVector_files() {
			return vector_files;
		}
		public void setVector_files(List<VectorFile> vector_files) {
			this.vector_files = vector_files;
		}
		public List<RasterFile> getRaster_files() {
			return raster_files;
		}
		public void setRaster_files(List<RasterFile> raster_files) {
			this.raster_files = raster_files;
		}
		public Metadata getMetadata() {
			return metadata;
		}
		public void setMetadata(Metadata metadata) {
			this.metadata = metadata;
		}
		public Date getCreated_at() {
			return created_at;
		}
		public void setCreated_at(Date created_at) {
			this.created_at = created_at;
		}
		public Date getUpdated_at() {
			return updated_at;
		}
		public void setUpdated_at(Date updated_at) {
			this.updated_at = updated_at;
		}
	}

	public static class VectorFile {
		private String name;
		private String description;
		private String uri;
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		public String getUri() {
			return uri;
		}
		public void setUri(String uri) {
			this.uri = uri;
		}
	}

	public static class Error {
		
		public Error() {
			
		}
		
		public Error(String sMessage) {
			this.message = sMessage;
		}
		private String message;

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}

	public List<Datum> getData() {
		return data;
	}

	public void setData(List<Datum> data) {
		this.data = data;
	}

	public Pagination getPagination() {
		return pagination;
	}

	public void setPagination(Pagination pagination) {
		this.pagination = pagination;
	}

	public List<Error> getErrors() {
		return errors;
	}

	public void setErrors(List<Error> errors) {
		this.errors = errors;
	}

}
