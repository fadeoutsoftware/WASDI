package wasdi.shared.business;

/**
 * Created by p.campanella on 21/10/2016.
 */
public class UserSession {
    private String userId;
    private Double loginDate;
    private Double lastTouch;
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
