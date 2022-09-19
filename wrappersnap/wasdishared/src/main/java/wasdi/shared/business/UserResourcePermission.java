package wasdi.shared.business;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import wasdi.shared.utils.Utils;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResourcePermission {

	private String resourceId;
	private String resourceType;
	private String userId;
	private String ownerId;

	private String permissions;

	private String createdBy;
	private Double createdDate;


	public UserResourcePermission(String sResourceType, String sResourceId, String sDestinationUserId, String sOwnerUserId, String sRequesterUserId) {
		this();

		this.resourceType = sResourceType;
		this.ownerId = sOwnerUserId;
		this.userId = sDestinationUserId;
		this.resourceId = sResourceId;
		this.createdBy = sRequesterUserId;
		this.createdDate = Utils.nowInMillis();
		this.permissions = "write";
	}

}
