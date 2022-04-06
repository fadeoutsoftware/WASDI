package wasdi.shared.business;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Style Sharing Entity
 * Represent the association between a style, his owner and the user that can access it
 * Styles are referred as "snapStyle"
 * 
 * @author PetruPetrescu on 23/02/2022
 *
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StyleSharing {

	/** Style Id */
	private  String styleId;

	/** User that can access */
	private  String userId;

	/** Style Owner */
    private  String ownerId;

    /** Sharing grant timestamp */
    private Double shareDate;

}
