package wasdi.shared.queryexecutors.skywatch.models;

import java.util.Date;
import java.util.List;

public class SearchResponse {

	private Data data;

	private List<Error> errors;

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
		public List<String> getResolution() {
			return resolution;
		}
		public void setResolution(List<String> resolution) {
			this.resolution = resolution;
		}
		public Double getCoverage() {
			return coverage;
		}
		public void setCoverage(Double coverage) {
			this.coverage = coverage;
		}
		public int getInterval_length() {
			return interval_length;
		}
		public void setInterval_length(int interval_length) {
			this.interval_length = interval_length;
		}
		public List<String> getOrder_by() {
			return order_by;
		}
		public void setOrder_by(List<String> order_by) {
			this.order_by = order_by;
		}
		public Date getCreated_at() {
			return created_at;
		}
		public void setCreated_at(Date created_at) {
			this.created_at = created_at;
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

	public static class Error {
		private String message;

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}

	public Data getData() {
		return data;
	}

	public void setData(Data data) {
		this.data = data;
	}

	public List<Error> getErrors() {
		return errors;
	}

	public void setErrors(List<Error> errors) {
		this.errors = errors;
	}

}