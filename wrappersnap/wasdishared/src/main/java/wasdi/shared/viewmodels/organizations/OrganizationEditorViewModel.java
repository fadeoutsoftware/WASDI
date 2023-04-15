package wasdi.shared.viewmodels.organizations;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrganizationEditorViewModel {

	private String organizationId;
//	private String userId;
	private String name;
	private String description;
	private String address;
	private String email;
	private String url;
//	private String logo;

//	private List<String> sharedUsers = new ArrayList<>();

}
