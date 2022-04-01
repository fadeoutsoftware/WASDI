package wasdi.shared.business;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Style Entity
 * Represent a sld style imported in WASDI
 * @author PetruPetrescu on 23/02/2022
 *
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Style {

	/** Style Identifier */
	private String styleId;

	/** Name */
	private String name;

	/** Description */
	private String description;

	/** User Owner */
	private String userId;

	/** Full sld file path */
	private String filePath;

	/** Flag to know if the style is public or not */
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private boolean isPublic;

	public boolean getIsPublic() {
		return isPublic;
	}

	public void setIsPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}

}
