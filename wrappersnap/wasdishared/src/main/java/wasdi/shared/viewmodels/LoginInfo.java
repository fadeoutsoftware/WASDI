package wasdi.shared.viewmodels;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by p.campanella on 21/10/2016.
 */
@XmlRootElement
public class LoginInfo {
    private String userId;
    private String userPassword;

    public LoginInfo() {

    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }
}
