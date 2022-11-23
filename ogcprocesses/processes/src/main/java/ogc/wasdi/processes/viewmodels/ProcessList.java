package ogc.wasdi.processes.viewmodels;

import java.util.ArrayList;
import java.util.List;

public class ProcessList {
	
	private List<ProcessSummary> processes = new ArrayList<ProcessSummary>();
	private List<Link> links = new ArrayList<Link>();
	  
	  
	public List<ProcessSummary> getProcesses() {
		return processes;
	}
	public void setProcesses(List<ProcessSummary> processes) {
		this.processes = processes;
	}
	public List<Link> getLinks() {
		return links;
	}
	public void setLinks(List<Link> links) {
		this.links = links;
	}

}
