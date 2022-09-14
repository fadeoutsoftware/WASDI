package wasdi.shared.viewmodels.permissions;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResourcePermissionViewModel {

	private String resourceId;
	private String resourceType;
	private String userId;
	private String ownerId;

	private String permissions;

	private String createdBy;
	private Double createdDate;

}
