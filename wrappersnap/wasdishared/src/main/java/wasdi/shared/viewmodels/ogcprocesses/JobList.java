package wasdi.shared.viewmodels.ogcprocesses;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JobList {
	@JsonProperty("jobs")
	private List<StatusInfo> jobs = new ArrayList<StatusInfo>();

	@JsonProperty("links")
	private List<Link> links = new ArrayList<Link>();

	public List<StatusInfo> getJobs() {
		return jobs;
	}

	public void setJobs(List<StatusInfo> jobs) {
		this.jobs = jobs;
	}

	public List<Link> getLinks() {
		return links;
	}

	public void setLinks(List<Link> links) {
		this.links = links;
	}

}
