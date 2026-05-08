package wasdi.shared.viewmodels.processors;

import java.util.ArrayList;

/**
 * App Stats View Model
 * 
 * Reports only the stats of an application
 * Stats are server-generated with a query to all nodes
 * 
 * @author p.campanella
 *
 */
public class AppStatsViewModel {
	private String applicationName; 
	private int runs=0;
	private int error=0;
	private int done=0;
	private int stopped=0;
	private int mediumTime=0;
	private int uniqueUsers=0;
	ArrayList<String> users = new ArrayList<String>(); 
	
	public String getApplicationName() {
		return applicationName;
	}
	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}
	public int getRuns() {
		return runs;
	}
	public void setRuns(int runs) {
		this.runs = runs;
	}
	public ArrayList<String> getUsers() {
		return users;
	}
	public void setUsers(ArrayList<String> users) {
		this.users = users;
	}
	public int getError() {
		return error;
	}
	public void setError(int error) {
		this.error = error;
	}
	public int getDone() {
		return done;
	}
	public void setDone(int done) {
		this.done = done;
	}
	public int getStopped() {
		return stopped;
	}
	public void setStopped(int stopped) {
		this.stopped = stopped;
	}
	public int getMediumTime() {
		return mediumTime;
	}
	public void setMediumTime(int mediumTime) {
		this.mediumTime = mediumTime;
	}
	public int getUniqueUsers() {
		return uniqueUsers;
	}
	public void setUniqueUsers(int uniqueUsers) {
		this.uniqueUsers = uniqueUsers;
	}
}
