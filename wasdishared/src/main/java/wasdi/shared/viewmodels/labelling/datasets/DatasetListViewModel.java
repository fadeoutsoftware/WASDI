package wasdi.shared.viewmodels.labelling.datasets;

import java.util.ArrayList;

public class DatasetListViewModel {
	
	public String id;
	public String name;
	public String description;
	public boolean isGlobal;
	public String bbox;
	public String mission;
	public ArrayList<String> tasks = new ArrayList<>();
	public String userRole;
	public String workspaceId;
}
