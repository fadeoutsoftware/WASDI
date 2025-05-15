package wasdi.shared.business.users;

public enum SkinType {
	WASDI("wasdi"),
	COPLAC("coplac");
	
	private final String skinType;
	
	SkinType(String skinType) {
		this.skinType = skinType;
	}
	
	public String getSkinType() {
		return this.skinType;
	}
	
}
