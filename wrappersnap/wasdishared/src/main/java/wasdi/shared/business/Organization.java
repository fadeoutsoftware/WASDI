package wasdi.shared.business;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Organization {

	private String organizationId;
	private String userId;
	private String name;
	private String description;
	private String address;
	private String email;
	private String url;
//	private String logo;

}
