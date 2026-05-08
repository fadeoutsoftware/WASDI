package wasdi.shared.queryexecutors.gpm;

import java.util.Date;

public class QueryRetrieveResponseEntry {
	public QueryRetrieveResponseEntry() {
		
	}

	private String name;
	private String duration;
	private String accumulation;
	private String extension;
	private String lastModified;
	private Date date;
	private String size;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDuration() {
		return duration;
	}
	public void setDuration(String duration) {
		this.duration = duration;
	}
	public String getAccumulation() {
		return accumulation;
	}
	public void setAccumulation(String accumulation) {
		this.accumulation = accumulation;
	}
	public String getExtension() {
		return extension;
	}
	public void setExtension(String extension) {
		this.extension = extension;
	}
	public String getLastModified() {
		return lastModified;
	}
	public void setLastModified(String lastModified) {
		this.lastModified = lastModified;
	}
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public String getSize() {
		return size;
	}
	public void setSize(String size) {
		this.size = size;
	}

}
