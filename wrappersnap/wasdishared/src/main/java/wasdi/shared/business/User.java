package wasdi.shared.business;

/**
 * WASDI User
 * Created by p.campanella on 21/10/2016.
 */
public class User {
	
	/**
	 * Unique int id
	 */
    private int id;
    /**
     * User ID
     */
    private String userId;
    /**
     * Name
     */
    private String name;
    /**
     * Surname
     */
    private String surname;
    /**
     * Password
     */
    private String password;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
