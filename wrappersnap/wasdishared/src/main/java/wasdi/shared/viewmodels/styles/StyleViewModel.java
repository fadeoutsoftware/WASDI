package wasdi.shared.viewmodels.styles;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import wasdi.shared.business.Style;

/**
 * View model class to pass data from Style to UI 
 * aka Styles
 * @author PetruPetrescu on 23/02/2022
 *
 */
@Getter
@Setter
@NoArgsConstructor
public class StyleViewModel {

	private String styleId;
	private String name;
	private String description;
	private boolean isPublic;
	private String userId;

	// This field should be initialized before return the view model checking in the style sharing
	// through the repositories 
	// default value to false;
	private boolean sharedWithMe = false;

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

}
