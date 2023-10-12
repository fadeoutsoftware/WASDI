package wasdi.shared.queryexecutors.gpm;

import java.util.Date;

public class QueryCountResponseEntry {
	
	public QueryCountResponseEntry() {
		
	}

	private String name;
	private String duration;
	private String accumulation;
	private String extension;
	private Date date;
	
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
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}

}
