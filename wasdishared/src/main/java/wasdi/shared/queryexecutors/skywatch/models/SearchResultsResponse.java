package wasdi.shared.queryexecutors.skywatch.models;

import java.util.Date;
import java.util.List;

public class SearchResultsResponse {

    private List<Datum> data;
    private Pagination pagination;
   
    private List<Status> status;

	private List<Error> errors;

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
		public String getSearchId() {
			return searchId;
		}
		public void setSearchId(String searchId) {
			this.searchId = searchId;
		}
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public Location getLocation() {
			return location;
		}
		public void setLocation(Location location) {
			this.location = location;
		}
		public Date getStart_time() {
			return start_time;
		}
		public void setStart_time(Date start_time) {
			this.start_time = start_time;
		}
		public Date getEnd_time() {
			return end_time;
		}
		public void setEnd_time(Date end_time) {
			this.end_time = end_time;
		}
		public String getPreview_uri() {
			return preview_uri;
		}
		public void setPreview_uri(String preview_uri) {
			this.preview_uri = preview_uri;
		}
		public String getThumbnail_uri() {
			return thumbnail_uri;
		}
		public void setThumbnail_uri(String thumbnail_uri) {
			this.thumbnail_uri = thumbnail_uri;
		}
		public Double getResolution() {
			return resolution;
		}
		public void setResolution(Double resolution) {
			this.resolution = resolution;
		}
		public String getSource() {
			return source;
		}
		public void setSource(String source) {
			this.source = source;
		}
		public String getProduct_name() {
			return product_name;
		}
		public void setProduct_name(String product_name) {
			this.product_name = product_name;
		}
		public Double getLocation_coverage_percentage() {
			return location_coverage_percentage;
		}
		public void setLocation_coverage_percentage(Double location_coverage_percentage) {
			this.location_coverage_percentage = location_coverage_percentage;
		}
		public Double getArea_sq_km() {
			return area_sq_km;
		}
		public void setArea_sq_km(Double area_sq_km) {
			this.area_sq_km = area_sq_km;
		}
		public Double getCost() {
			return cost;
		}
		public void setCost(Double cost) {
			this.cost = cost;
		}
		public Double getAvailable_credit() {
			return available_credit;
		}
		public void setAvailable_credit(Double available_credit) {
			this.available_credit = available_credit;
		}
		public Double getResult_cloud_cover_percentage() {
			return result_cloud_cover_percentage;
		}
		public void setResult_cloud_cover_percentage(Double result_cloud_cover_percentage) {
			this.result_cloud_cover_percentage = result_cloud_cover_percentage;
		}
	}

	public static class Location {
		private String type;
		private List<List<List<Double>>> coordinates;
		private List<Double> bbox;
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
		public List<Double> getBbox() {
			return bbox;
		}
		public void setBbox(List<Double> bbox) {
			this.bbox = bbox;
		}
	}

	public static class Pagination {
		private Integer per_page;
		private Cursor cursor;
		private Integer total;
		private Integer count;
		public Integer getPer_page() {
			return per_page;
		}
		public void setPer_page(Integer per_page) {
			this.per_page = per_page;
		}
		public Cursor getCursor() {
			return cursor;
		}
		public void setCursor(Cursor cursor) {
			this.cursor = cursor;
		}
		public Integer getTotal() {
			return total;
		}
		public void setTotal(Integer total) {
			this.total = total;
		}
		public Integer getCount() {
			return count;
		}
		public void setCount(Integer count) {
			this.count = count;
		}
	}

	public static class Cursor {
		private Integer next;
		private Integer self;
		public Integer getNext() {
			return next;
		}
		public void setNext(Integer next) {
			this.next = next;
		}
		public Integer getSelf() {
			return self;
		}
		public void setSelf(Integer self) {
			this.self = self;
		}
	}

	public static class Status{
		private String message;

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
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

	public List<Status> getStatus() {
		return status;
	}

	public void setStatus(List<Status> status) {
		this.status = status;
	}

	public List<Error> getErrors() {
		return errors;
	}

	public void setErrors(List<Error> errors) {
		this.errors = errors;
	}

}
