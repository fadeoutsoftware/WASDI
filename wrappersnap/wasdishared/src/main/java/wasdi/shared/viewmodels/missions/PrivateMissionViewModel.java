package wasdi.shared.viewmodels.missions;

public class PrivateMissionViewModel {
	
	private String missionName;
	private String missionIndexValue;
	private String missionOwner;
	private String userId;
	private String permissionType;
	private Double permissionCreationDate;
	private String permissionCreatedBy;
	
	public PrivateMissionViewModel() {
		
	}

	public PrivateMissionViewModel(String missionName, String missionIndexName, String missionIndexValue, String missionOwner, String userId,
			String permissionType, Double permissionCreationDate, String permissionCreatedBy) {
		super();
		this.missionName = missionName;
		this.missionIndexValue = missionIndexValue;
		this.missionOwner = missionOwner;
		this.userId = userId;
		this.permissionType = permissionType;
		this.permissionCreationDate = permissionCreationDate;
		this.permissionCreatedBy = permissionCreatedBy;
	}

	public String getMissionName() {
		return missionName;
	}

	public void setMissionName(String sMissionName) {
		this.missionName = sMissionName;
	}
	
	public String getMissionIndexValue() {
		return missionIndexValue;
	}

	public void setMissionIndexValue(String missionIndexValue) {
		this.missionIndexValue = missionIndexValue;
	}

	public String getMissionOwner() {
		return missionOwner;
	}

	public void setMissionOwner(String sMissionOwner) {
		this.missionOwner = sMissionOwner;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getPermissionType() {
		return permissionType;
	}

	public void setPermissionType(String permissionType) {
		this.permissionType = permissionType;
	}

	public Double getPermissionCreationDate() {
		return permissionCreationDate;
	}

	public void setPermissionCreationDate(Double permissionCreationDate) {
		this.permissionCreationDate = permissionCreationDate;
	}

	public String getPermissionCreatedBy() {
		return permissionCreatedBy;
	}

	public void setPermissionCreatedBy(String permissionCreatedBy) {
		this.permissionCreatedBy = permissionCreatedBy;
	}


}
