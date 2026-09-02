package wasdi.shared.business.labelling;

import java.util.ArrayList;

public class DatasetImage {
	private String id;
	private String link;
	private String bbox;
	private long date;
	private ArrayList<String> labelIds  = new ArrayList<>();
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getLink() {
		return link;
	}
	public void setLink(String link) {
		this.link = link;
	}
	public String getBbox() {
		return bbox;
	}
	public void setBbox(String bbox) {
		this.bbox = bbox;
	}
	public long getDate() {
		return date;
	}
	public void setDate(long date) {
		this.date = date;
	}
	public ArrayList<String> getLabelIds() {
		return labelIds;
	}
	public void setLabelIds(ArrayList<String> labelIds) {
		this.labelIds = labelIds;
	}

}
