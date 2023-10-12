package wasdi.shared.viewmodels.organizations;

/**
 * View model class for Subscription Type
 * @author PetruPetrescu on 19/01/2023
 *
 */
public class SubscriptionTypeViewModel {

	private String typeId;
	private String name;
	private String description;
	
	public SubscriptionTypeViewModel() {
		
	}
	
	public SubscriptionTypeViewModel(String sId, String sName, String sDescription) {
		this.typeId = sId;
		this.name = sName;
		this.description = sDescription;
	}
	
	public String getTypeId() {
		return typeId;
	}
	public void setTypeId(String typeId) {
		this.typeId = typeId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

}
