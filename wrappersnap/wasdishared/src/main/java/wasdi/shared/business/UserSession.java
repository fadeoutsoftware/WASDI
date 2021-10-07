package wasdi.shared.business;

/**
 * User Session Entity
 * Represent an active WASDI Session
 * 
 * Created by p.campanella on 21/10/2016.
 */
public class UserSession {
	
	/**
	 * User ID
	 */
    private String userId;
    
    /**
     * Login Date
     */
    private Double loginDate;
    
    /**
     * Last activity timestamp
     */
    private Double lastTouch;
    
    /**
     * Unique session ID
     */
    private String sessionId;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Double getLoginDate() { return loginDate; }

    public void setLoginDate(Double loginDate) {
        this.loginDate = loginDate;
    }

    public Double getLastTouch() {
        return lastTouch;
    }

    public void setLastTouch(Double lastTouch) {
        this.lastTouch = lastTouch;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
