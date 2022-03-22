package wasdi.shared.viewmodels.styles;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import wasdi.shared.business.StyleSharing;

/**
 * Represents the sharing of a style with a user
 * @author PetruPetrescu on 23/02/2022
 *
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StyleSharingViewModel {

	private String userId;

	public StyleSharingViewModel(StyleSharing oSharing) {
		super();
		this.userId = oSharing.getUserId();
	}

}
