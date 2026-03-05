package wasdi.shared.data;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.MongoCursor;

import wasdi.shared.business.users.User;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.IUserRepositoryBackend;

/**
 * Created by p.campanella on 21/10/2016.
 */
public class UserRepository {
	private final IUserRepositoryBackend m_oBackend;

	public UserRepository() {
		m_oBackend = createBackend();
	}

	private IUserRepositoryBackend createBackend() {
		return DataRepositoryFactoryProvider.getFactory().createUserRepository();
	}

	/**
	 * Insert a new User
	 * @param oUser
	 * @return
	 */
	public boolean insertUser(User oUser) {
		return m_oBackend.insertUser(oUser);
	}

	/**
	 * Get a user
	 * @param sUserId
	 * @return
	 */
	public User getUser(String sUserId) {
		return m_oBackend.getUser(sUserId);
	}

	/**
	 * Check login credentials
	 * @param sUserId
	 * @param sPassword
	 * @return
	 */
	public User login(String sUserId, String sPassword) {
		return m_oBackend.login(sUserId, sPassword);
	}

	/**
	 * Delete a user
	 * @param sUserId
	 * @return
	 */
	public boolean deleteUser(String sUserId) {
		return m_oBackend.deleteUser(sUserId);
	}

	/**
	 * Update a user
	 * @param oUser
	 * @return
	 */
	public boolean updateUser(User oUser) {
		return m_oBackend.updateUser(oUser);
	}

	/**
	 * Get the list of all users
	 * @return
	 */
	public ArrayList<User> getAllUsers() {
		return m_oBackend.getAllUsers();
	}

	/**
	 * Get an iterator over the collection of users
	 * @return an iterator for the Usersc collection
	 */
	public MongoCursor<Document> getIteratorOverAllUsers() {
		return m_oBackend.getIteratorOverAllUsers();
	}

	/**
	 * Get the list of all users
	 * @return
	 */
	public ArrayList<User> getAdminUsers() {
		return m_oBackend.getAdminUsers();
	}

	/**
	 * Get a sorted list of users
	 * @param sOrderBy db field on which to perform the sorting operation
	 * @param iOrder decreasing or increasing order
	 * @return
	 */
	public ArrayList<User> getAllUsersSorted(String sOrderBy, int iOrder) {
		return m_oBackend.getAllUsersSorted(sOrderBy, iOrder);
	}

	/**
	 * Get a list of users matching a partial name
	 * @param sPartialName
	 * @return
	 */
	public List<User> findUsersByPartialName(String sPartialName) {
		return m_oBackend.findUsersByPartialName(sPartialName);
	}

	public List<User> findUsersByPartialName(String sPartialName, String sOrderBy, int iOrder) {
		return m_oBackend.findUsersByPartialName(sPartialName, sOrderBy, iOrder);
	}

	public long getUsersCount() {
		return m_oBackend.getUsersCount();
	}
}

