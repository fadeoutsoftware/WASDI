package wasdi.shared.business;

/**
 * Style Entity
 * Represent a sld style imported in WASDI
 * @author PetruPetrescu on 23/02/2022
 *
 */
public class Style {

	/**
	 * Style Identifier
	 */
	private String styleId;

	/**
	 * Name
	 */
	private String name;

	/**
	 * Description 
	 */
	private String description;

	/**
	 * User Owner
	 */
	private String userId;

	/**
	 * Full sld file path
	 */
	private String filePath;

	/**
	 * Flag to know if the style is public or not
	 */
	private boolean isPublic;

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

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getStyleId() {
		return styleId;
	}

	public void setStyleId(String styleId) {
		this.styleId = styleId;
	}

	public boolean getIsPublic() {
		return isPublic;
	}

	public void setIsPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}

}
