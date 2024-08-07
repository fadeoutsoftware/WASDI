package wasdi.shared.viewmodels.ogcprocesses;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public class StatusInfo extends OgcProcessesViewModel {
	/**
	 * Gets or Sets type
	 */
	public enum TypeEnum {
		PROCESS("process");

		private String value;

		TypeEnum(String sValue) {
			this.value = sValue;
		}

		@Override
		@JsonValue
		public String toString() {
			return String.valueOf(value);
		}

		@JsonCreator
		public static TypeEnum fromValue(String sText) {
			for (TypeEnum oType : TypeEnum.values()) {
				if (String.valueOf(oType.value).equals(sText)) {
					return oType;
				}
			}
			return null;
		}
	}
	
	@JsonProperty("processID")
	private String processID = null;

	@JsonProperty("type")
	private TypeEnum type = null;

	@JsonProperty("jobID")
	private String jobID = null;

	@JsonProperty("status")
	private StatusCode status = null;

	@JsonProperty("message")
	private String message = null;

	@JsonFormat(shape=JsonFormat.Shape.STRING)
	@JsonProperty("created")
	private Date created = null;

	@JsonFormat(shape=JsonFormat.Shape.STRING)
	@JsonProperty("started")
	private Date started = null;

	@JsonFormat(shape=JsonFormat.Shape.STRING)
	@JsonProperty("finished")
	private Date finished = null;

	@JsonFormat(shape=JsonFormat.Shape.STRING)
	@JsonProperty("updated")
	private Date updated = null;

	@JsonProperty("progress")
	private Integer progress = null;

	@JsonProperty("links")
	private List<Link> links = new ArrayList<Link>();

	public String getProcessID() {
		return processID;
	}

	public void setProcessID(String processID) {
		this.processID = processID;
	}

	public TypeEnum getType() {
		return type;
	}

	public void setType(TypeEnum type) {
		this.type = type;
	}

	public String getJobID() {
		return jobID;
	}

	public void setJobID(String jobID) {
		this.jobID = jobID;
	}

	public StatusCode getStatus() {
		return status;
	}

	public void setStatus(StatusCode status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Date getStarted() {
		return started;
	}

	public void setStarted(Date started) {
		this.started = started;
	}

	public Date getFinished() {
		return finished;
	}

	public void setFinished(Date finished) {
		this.finished = finished;
	}

	public Date getUpdated() {
		return updated;
	}

	public void setUpdated(Date updated) {
		this.updated = updated;
	}

	public Integer getProgress() {
		return progress;
	}

	public void setProgress(Integer progress) {
		this.progress = progress;
	}

	public List<Link> getLinks() {
		return links;
	}

	public void setLinks(List<Link> links) {
		this.links = links;
	}
}
