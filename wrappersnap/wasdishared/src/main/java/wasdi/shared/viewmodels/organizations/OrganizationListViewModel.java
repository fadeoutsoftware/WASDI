package wasdi.shared.viewmodels.organizations;

//import java.util.ArrayList;
//import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationListViewModel {

	private String organizationId;
	private String ownerUserId;
	private String name;
//	private String description;
//	private String address;
//	private String email;
//	private String url;
//	private String logo;
	private boolean adminRole;

//	private List<String> sharedUsers = new ArrayList<>();

}
