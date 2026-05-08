package wasdi.shared.data.interfaces;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.MongoCursor;

import wasdi.shared.business.users.User;

/**
 * Backend contract for user repository.
 */
public interface IUserRepositoryBackend {

	boolean insertUser(User oUser);

	User getUser(String sUserId);

	User login(String sUserId, String sPassword);

	boolean deleteUser(String sUserId);

	boolean updateUser(User oUser);

	ArrayList<User> getAllUsers();

	MongoCursor<Document> getIteratorOverAllUsers();

	ArrayList<User> getAdminUsers();

	ArrayList<User> getAllUsersSorted(String sOrderBy, int iOrder);

	List<User> findUsersByPartialName(String sPartialName);

	List<User> findUsersByPartialName(String sPartialName, String sOrderBy, int iOrder);

	long getUsersCount();
}
