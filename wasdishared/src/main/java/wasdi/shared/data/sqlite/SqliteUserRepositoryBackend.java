package wasdi.shared.data.sqlite;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.MongoCursor;

import wasdi.shared.business.users.User;
import wasdi.shared.business.users.UserApplicationRole;
import wasdi.shared.data.interfaces.IUserRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * SQLite backend implementation for user repository.
 */
public class SqliteUserRepositoryBackend extends SqliteRepository implements IUserRepositoryBackend {

	public SqliteUserRepositoryBackend() {
		m_sThisCollection = "users";
		this.ensureTable(m_sThisCollection);
	}

	@Override
	public boolean insertUser(User oUser) {

		try {
			if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId())) {
				return false;
			}
			return insert(oUser.getUserId(), oUser);

		} catch (Exception oEx) {
			WasdiLog.errorLog("UserRepository.insertUser : error ", oEx);
		}

		return false;
	}

	@Override
	public User getUser(String sUserId) {

		if (Utils.isNullOrEmpty(sUserId)) {
			return null;
		}

		try {
			sUserId = sUserId.toLowerCase();
			return findOneWhere("userId", sUserId, User.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("UserRepository.getUser : error ", oEx);
		}

		return null;
	}

	@Override
	public User login(String sUserId, String sPassword) {
		try {
			User oUser = getUser(sUserId);

			if (oUser != null) {
				if (oUser.getPassword() != null) {
					if (oUser.getPassword().equals(sPassword)) {
						return oUser;
					}
				}
			}

			return null;
		} catch (Exception oEx) {
			WasdiLog.errorLog("UserRepository.login : error ", oEx);
		}

		return null;
	}

	@Override
	public boolean deleteUser(String sUserId) {

		if (Utils.isNullOrEmpty(sUserId)) {
			return false;
		}

		try {
			return deleteWhere("userId", sUserId) > 0;

		} catch (Exception oEx) {
			WasdiLog.errorLog("UserRepository.deleteUser : error ", oEx);
		}

		return false;
	}

	@Override
	public boolean updateUser(User oUser) {
		try {
			if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId())) {
				return false;
			}
			return updateById(oUser.getUserId(), oUser);

		} catch (Exception oEx) {
			WasdiLog.errorLog("UserRepository.updateUser : error ", oEx);
		}
		return false;
	}

	@Override
	public ArrayList<User> getAllUsers() {
		return new ArrayList<>(findAll(User.class));
	}

	@Override
	public MongoCursor<Document> getIteratorOverAllUsers() {
		WasdiLog.warnLog("UserRepository.getIteratorOverAllUsers: Mongo cursor is not supported on SQLite backend.");
		return null;
	}

	@Override
	public ArrayList<User> getAdminUsers() {
		return new ArrayList<>(findAllWhere("role", UserApplicationRole.ADMIN.name(), User.class));
	}

	@Override
	public ArrayList<User> getAllUsersSorted(String sOrderBy, int iOrder) {

		ArrayList<User> aoReturnList = new ArrayList<User>();

		try {
			String sField = sanitizeOrderField(sOrderBy);
			String sDir = iOrder < 0 ? "DESC" : "ASC";
			aoReturnList.addAll(queryList(
					"SELECT data FROM " + m_sThisCollection +
					" ORDER BY json_extract(data,'$." + sField + "') " + sDir,
					new Object[]{}, User.class));
		} catch (Exception oEx) {
			WasdiLog.errorLog("UserRepository.getAllUsersSorted. Error retrieving users", oEx);
		}

		return aoReturnList;
	}

	@Override
	public List<User> findUsersByPartialName(String sPartialName) {
		return findUsersByPartialName(sPartialName, "userId", 1);
	}

	@Override
	public List<User> findUsersByPartialName(String sPartialName, String sOrderBy, int iOrder) {
		List<User> aoReturnList = new ArrayList<>();

		if (Utils.isNullOrEmpty(sPartialName) && !Utils.isNullOrEmpty(sOrderBy)) {
			return getAllUsersSorted(sOrderBy, iOrder);
		} else if (Utils.isNullOrEmpty(sPartialName) && Utils.isNullOrEmpty(sOrderBy)) {
			return getAllUsers();
		}

		try {
			String sField = sanitizeOrderField(sOrderBy);
			String sDir = iOrder < 0 ? "DESC" : "ASC";
			String sLike = "%" + sPartialName + "%";
			aoReturnList.addAll(queryList(
					"SELECT data FROM " + m_sThisCollection +
					" WHERE LOWER(json_extract(data,'$.userId')) LIKE LOWER(?)" +
					" OR LOWER(json_extract(data,'$.name')) LIKE LOWER(?)" +
					" OR LOWER(json_extract(data,'$.surname')) LIKE LOWER(?)" +
					" OR LOWER(json_extract(data,'$.publicNickName')) LIKE LOWER(?)" +
					" ORDER BY json_extract(data,'$." + sField + "') " + sDir,
					new Object[]{sLike, sLike, sLike, sLike},
					User.class));
		} catch (Exception oEx) {
			WasdiLog.errorLog("UserRepository.getAllUsers : error ", oEx);
		}

		return aoReturnList;
	}

	@Override
	public long getUsersCount() {
		return count();
	}

	private String sanitizeOrderField(String sOrderBy) {
		if (Utils.isNullOrEmpty(sOrderBy)) {
			return "userId";
		}
		if (!sOrderBy.matches("[A-Za-z0-9_]+")) {
			return "userId";
		}
		return sOrderBy;
	}
}
