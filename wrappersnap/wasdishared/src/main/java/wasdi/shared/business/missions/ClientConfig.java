package wasdi.shared.business.missions;

import java.util.ArrayList;
import java.util.List;

public class ClientConfig {
	
	private List<Mission> missions = new ArrayList<>();
	
	private OrbitSearch orbitsearch = new OrbitSearch();

	public List<Mission> getMissions() {
		return missions;
	}

	public void setMissions(List<Mission> missions) {
		this.missions = missions;
	}

	public OrbitSearch getOrbitsearch() {
		return orbitsearch;
	}

	public void setOrbitsearch(OrbitSearch orbitsearch) {
		this.orbitsearch = orbitsearch;
	}

}
