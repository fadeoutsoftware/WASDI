package wasdi.shared.queryexecutors.skywatch.models;

import java.util.Date;
import java.util.List;

public class GetListPipelinesResponse {
	
	public GetListPipelinesResponse() {
		
	}

	private List<Datum> data;
	private Pagination pagination;

	public static class Aoi {
		public Aoi() {
			
		}
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

	public static class Cursor {
		public Cursor() {
			
		}
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
		public Datum() {
			
		}
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
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
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
		public Double getBudget_per_km2() {
			return budget_per_km2;
		}
		public void setBudget_per_km2(Double budget_per_km2) {
			this.budget_per_km2 = budget_per_km2;
		}
		public Double getMax_cost() {
			return max_cost;
		}
		public void setMax_cost(Double max_cost) {
			this.max_cost = max_cost;
		}
		public Double getResolution_low() {
			return resolution_low;
		}
		public void setResolution_low(Double resolution_low) {
			this.resolution_low = resolution_low;
		}
		public Double getResolution_high() {
			return resolution_high;
		}
		public void setResolution_high(Double resolution_high) {
			this.resolution_high = resolution_high;
		}
		public Double getCloud_cover_percentage() {
			return cloud_cover_percentage;
		}
		public void setCloud_cover_percentage(Double cloud_cover_percentage) {
			this.cloud_cover_percentage = cloud_cover_percentage;
		}
		public Double getMin_aoi_coverage_percentage() {
			return min_aoi_coverage_percentage;
		}
		public void setMin_aoi_coverage_percentage(Double min_aoi_coverage_percentage) {
			this.min_aoi_coverage_percentage = min_aoi_coverage_percentage;
		}
		public String getInterval() {
			return interval;
		}
		public void setInterval(String interval) {
			this.interval = interval;
		}
		public ResultDelivery getResult_delivery() {
			return result_delivery;
		}
		public void setResult_delivery(ResultDelivery result_delivery) {
			this.result_delivery = result_delivery;
		}
		public Aoi getAoi() {
			return aoi;
		}
		public void setAoi(Aoi aoi) {
			this.aoi = aoi;
		}
		public Double getArea_km2() {
			return area_km2;
		}
		public void setArea_km2(Double area_km2) {
			this.area_km2 = area_km2;
		}
		public Output getOutput() {
			return output;
		}
		public void setOutput(Output output) {
			this.output = output;
		}
		public List<Tag> getTags() {
			return tags;
		}
		public void setTags(List<Tag> tags) {
			this.tags = tags;
		}
		public String getStatus() {
			return status;
		}
		public void setStatus(String status) {
			this.status = status;
		}
		public StatusReason getStatus_reason() {
			return status_reason;
		}
		public void setStatus_reason(StatusReason status_reason) {
			this.status_reason = status_reason;
		}
		public String getSearch_id() {
			return search_id;
		}
		public void setSearch_id(String search_id) {
			this.search_id = search_id;
		}
		public List<String> getSearch_results() {
			return search_results;
		}
		public void setSearch_results(List<String> search_results) {
			this.search_results = search_results;
		}
		public Sources getSources() {
			return sources;
		}
		public void setSources(Sources sources) {
			this.sources = sources;
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

	public static class Output {
		public Output() {
			
		}
		private String id;
		private String format;
		private String mosaic;
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public String getFormat() {
			return format;
		}
		public void setFormat(String format) {
			this.format = format;
		}
		public String getMosaic() {
			return mosaic;
		}
		public void setMosaic(String mosaic) {
			this.mosaic = mosaic;
		}
	}

	public static class Pagination {
		public Pagination() {
			
		}
		private int per_page;
		private Cursor cursor;
		private int total;
		private int count;
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
		public int getTotal() {
			return total;
		}
		public void setTotal(int total) {
			this.total = total;
		}
		public int getCount() {
			return count;
		}
		public void setCount(int count) {
			this.count = count;
		}
	}

	public static class Sources {
		public Sources() {
			
		}
		private List<String> include;
		public List<String> getInclude() {
			return include;
		}
		public void setInclude(List<String> include) {
			this.include = include;
		}
	}

	public static class StatusReason {
		public StatusReason() {
			
		}
		private String reason;
		private String description;
		public String getReason() {
			return reason;
		}
		public void setReason(String reason) {
			this.reason = reason;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
	}

	public static class Tag {
		public Tag() {
			
		}
		private String label;
		private String value;
		public String getLabel() {
			return label;
		}
		public void setLabel(String label) {
			this.label = label;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
	}

	public static class ResultDelivery {
		public ResultDelivery() {
			
		}
		public String max_latency;
		public List<String> priorities;
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

}