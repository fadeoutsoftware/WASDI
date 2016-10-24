package wasdi.shared.business;

import java.util.Date;

/**
 * Created by p.campanella on 21/10/2016.
 */
public class UserSession {
    private String userId;
    private Date loginDate;
    private Date lastTouch;
    private String sessionId;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Date getLoginDate() {
        return loginDate;
    }

    public void setLoginDate(Date loginDate) {
        this.loginDate = loginDate;
    }

    public Date getLastTouch() {
        return lastTouch;
    }

    public void setLastTouch(Date lastTouch) {
        this.lastTouch = lastTouch;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
