package wasdi.shared.business;

/**
 * Processor Sharing Entity
 * Represent the association between a processor and the user that can access it
 * @author p.campanella on 10/04/2020
 *
 */
public class ProcessorSharing {
	/**
	 * Workspace Id
	 */
    private  String processorId;
    
    /**
     * User that can access
     */
    private  String userId;
    
    /**
     * Workspace Owner
     */
    private  String ownerId;
    
    /**
     * Sharing grant timestamp
     */
    private Double shareDate;

	public String getProcessorId() {
		return processorId;
	}

	public void setProcessorId(String processorId) {
		this.processorId = processorId;
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
