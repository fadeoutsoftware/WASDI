package wasdi.shared.business;

/**
 * Style Sharing Entity
 * Represent the association between a style, his owner and the user that can access it
 * Styles are referred as "snapStyle"
 * 
 * @author PetruPetrescu on 23/02/2022
 *
 */
public class StyleSharing {

	/**
	 * Style Id
	 */
    private  String styleId;
    
    /**
     * User that can access
     */
    private  String userId;
    
    /**
     * Style Owner
     */
    private  String ownerId;
    
    /**
     * Sharing grant timestamp
     */
    private Double shareDate;

	public String getStyleId() {
		return styleId;
	}

	public void setStyleId(String styleId) {
		this.styleId = styleId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public Double getShareDate() {
		return shareDate;
	}

	public void setShareDate(Double shareDate) {
		this.shareDate = shareDate;
	}
}
