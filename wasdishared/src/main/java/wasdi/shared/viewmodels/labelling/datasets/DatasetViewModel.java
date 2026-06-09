package wasdi.shared.viewmodels.labelling.datasets;

import java.util.ArrayList;

public class DatasetViewModel {
	public String id;
	public String name;
	public String description;
	public boolean isGlobal;
	public String bbox;
	public boolean isPublic;
	public long creationDate;
	public String link;
	public long startDate;
	public long endDate;
	public boolean annotatorSeeAllLabels;
	public boolean reviewRequired;
	public int minReviewCount;
	public String missions;
	public ArrayList<String> tasks = new ArrayList<>();
	public String ownersIds;
	public String templateId;
}
