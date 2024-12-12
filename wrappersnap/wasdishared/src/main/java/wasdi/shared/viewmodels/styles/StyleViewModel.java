package wasdi.shared.viewmodels.styles;

import wasdi.shared.business.Style;

/**
 * View model class to pass data from Style to UI 
 * aka Styles
 * @author PetruPetrescu on 23/02/2022
 *
 */

public class StyleViewModel {

	private String styleId;
	private String name;
	private String description;
	private boolean isPublic;
	private String userId;
	private boolean readOnly;
	private String imgLink;

	// This field should be initialized before return the view model checking in the style sharing
	// through the repositories 
	// default value to false;
	private boolean sharedWithMe = false;
	
	public StyleViewModel() {
		
	}

	// Parameterized constructor with all fields except sharing  
	public StyleViewModel(String styleId, String name, String description, boolean isPublic, String userId) {
		super();
		this.styleId = styleId;
		this.name = name;
		this.description = description;
		this.isPublic = isPublic;
		this.userId = userId;
	}

	static public StyleViewModel getFromStyle(Style oStyle) {
		StyleViewModel oVM = new StyleViewModel();
		oVM.setName(oStyle.getName());
		oVM.setDescription(oStyle.getDescription());
		oVM.setStyleId(oStyle.getStyleId());
		oVM.setPublic(oStyle.getIsPublic());
		oVM.setUserId(oStyle.getUserId());

		return oVM;
	}

	public String getStyleId() {
		return styleId;
	}

	public void setStyleId(String styleId) {
		this.styleId = styleId;
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

	public boolean isPublic() {
		return isPublic;
	}

	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public boolean isSharedWithMe() {
		return sharedWithMe;
	}

	public void setSharedWithMe(boolean sharedWithMe) {
		this.sharedWithMe = sharedWithMe;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public String getImgLink() {
		return imgLink;
	}

	public void setImgLink(String imgLink) {
		this.imgLink = imgLink;
	}

}
