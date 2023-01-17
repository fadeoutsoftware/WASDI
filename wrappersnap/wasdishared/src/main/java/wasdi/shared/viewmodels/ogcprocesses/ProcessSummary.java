package wasdi.shared.viewmodels.ogcprocesses;

import java.util.ArrayList;
import java.util.List;

public class ProcessSummary extends DescriptionType {
	private String id = null;
	private String version = null;
	private List<JobControlOptions> jobControlOptions = new ArrayList<JobControlOptions>();
	private List<TransmissionMode> outputTransmission = new ArrayList<TransmissionMode>();
	private List<Link> links = new ArrayList<Link>();
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public List<JobControlOptions> getJobControlOptions() {
		return jobControlOptions;
	}
	public void setJobControlOptions(List<JobControlOptions> jobControlOptions) {
		this.jobControlOptions = jobControlOptions;
	}
	public List<TransmissionMode> getOutputTransmission() {
		return outputTransmission;
	}
	public void setOutputTransmission(List<TransmissionMode> outputTransmission) {
		this.outputTransmission = outputTransmission;
	}
	public List<Link> getLinks() {
		return links;
	}
	public void setLinks(List<Link> links) {
		this.links = links;
	}

}
