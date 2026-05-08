package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.FluentFilter.where;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.Document;

import com.mongodb.client.MongoCursor;

import org.dizitart.no2.collection.NitriteCollection;

import wasdi.shared.business.users.User;
import wasdi.shared.business.users.UserApplicationRole;
import wasdi.shared.data.interfaces.IUserRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * NO2 backend implementation for user repository.
 */
public class No2UserRepositoryBackend extends No2Repository implements IUserRepositoryBackend {

	private static final String s_sCollectionName = "users";

	@Override
	public boolean insertUser(User oUser) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oUser == null) {
				return false;
			}

			oCollection.insert(toDocument(oUser));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2UserRepositoryBackend.insertUser: error", oEx);
		}

		return false;
	}

	@Override
	public User getUser(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return null;
		}

		try {
			String sLookup = sUserId.toLowerCase();
			for (User oUser : getAllUsers()) {
				if (oUser != null && equalsSafe(lower(oUser.getUserId()), sLookup)) {
					return oUser;
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2UserRepositoryBackend.getUser: error", oEx);
		}

		return null;
	}

	@Override
	public User login(String sUserId, String sPassword) {
		try {
			User oUser = getUser(sUserId);
			if (oUser != null && oUser.getPassword() != null && oUser.getPassword().equals(sPassword)) {
				return oUser;
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2UserRepositoryBackend.login: error", oEx);
		}

		return null;
	}

	@Override
	public boolean deleteUser(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return false;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return false;
			}

			oCollection.remove(where("userId").eq(sUserId));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2UserRepositoryBackend.deleteUser: error", oEx);
		}

		return false;
	}

	@Override
	public boolean updateUser(User oUser) {
		if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId())) {
			return false;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return false;
			}

			oCollection.update(where("userId").eq(oUser.getUserId()), toDocument(oUser));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2UserRepositoryBackend.updateUser: error", oEx);
		}

		return false;
	}

	@Override
	public ArrayList<User> getAllUsers() {
		return new ArrayList<>(getUsersListSafe());
	}

	@Override
	public MongoCursor<Document> getIteratorOverAllUsers() {
		WasdiLog.warnLog("No2UserRepositoryBackend.getIteratorOverAllUsers: Mongo cursor is not supported on NO2 backend.");
		return null;
	}

	@Override
	public ArrayList<User> getAdminUsers() {
		ArrayList<User> aoAdmins = new ArrayList<>();
		for (User oUser : getUsersListSafe()) {
			if (oUser != null && equalsSafe(oUser.getRole(), UserApplicationRole.ADMIN.name())) {
				aoAdmins.add(oUser);
			}
		}
		return aoAdmins;
	}

	@Override
	public ArrayList<User> getAllUsersSorted(String sOrderBy, int iOrder) {
		ArrayList<User> aoUsers = getAllUsers();
		Comparator<User> oComparator = comparatorForField(sOrderBy);
		if (iOrder < 0) {
			oComparator = oComparator.reversed();
		}
		aoUsers.sort(oComparator);
		return aoUsers;
	}

	@Override
	public List<User> findUsersByPartialName(String sPartialName) {
		return findUsersByPartialName(sPartialName, "userId", 1);
	}

	@Override
	public List<User> findUsersByPartialName(String sPartialName, String sOrderBy, int iOrder) {
		if (Utils.isNullOrEmpty(sPartialName) && !Utils.isNullOrEmpty(sOrderBy)) {
			return getAllUsersSorted(sOrderBy, iOrder);
		}
		if (Utils.isNullOrEmpty(sPartialName) && Utils.isNullOrEmpty(sOrderBy)) {
			return getAllUsers();
		}

		List<User> aoReturnList = new ArrayList<>();
		Pattern oRegex = Pattern.compile(Pattern.quote(sPartialName), Pattern.CASE_INSENSITIVE);

		for (User oUser : getUsersListSafe()) {
			if (oUser == null) {
				continue;
			}

			if (matches(oRegex, oUser.getUserId())
					|| matches(oRegex, oUser.getName())
					|| matches(oRegex, oUser.getSurname())
					|| matches(oRegex, oUser.getPublicNickName())) {
				aoReturnList.add(oUser);
			}
		}

		Comparator<User> oComparator = comparatorForField(sOrderBy);
		if (iOrder < 0) {
			oComparator = oComparator.reversed();
		}
		aoReturnList.sort(oComparator);

		return aoReturnList;
	}

	@Override
	public long getUsersCount() {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			return oCollection != null ? oCollection.size() : 0L;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2UserRepositoryBackend.getUsersCount: error", oEx);
		}
		return 0L;
	}

	private List<User> getUsersListSafe() {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			return toList(oCollection != null ? oCollection.find() : null, User.class);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2UserRepositoryBackend.getUsersListSafe: error", oEx);
			return new ArrayList<>();
		}
	}

	private boolean matches(Pattern oPattern, String sValue) {
		return sValue != null && oPattern.matcher(sValue).find();
	}

	private Comparator<User> comparatorForField(String sOrderBy) {
		String sField = Utils.isNullOrEmpty(sOrderBy) ? "userId" : sOrderBy;
		return (oLeft, oRight) -> compareNullableString(getSortableField(oLeft, sField), getSortableField(oRight, sField));
	}

	private String getSortableField(User oUser, String sField) {
		if (oUser == null) {
			return null;
		}

		switch (sField) {
			case "name":
				return oUser.getName();
			case "surname":
				return oUser.getSurname();
			case "publicNickName":
				return oUser.getPublicNickName();
			case "role":
				return oUser.getRole();
			case "type":
				return oUser.getType();
			case "userId":
			default:
				return oUser.getUserId();
		}
	}

	private int compareNullableString(String sA, String sB) {
		if (sA == null && sB == null) {
			return 0;
		}
		if (sA == null) {
			return 1;
		}
		if (sB == null) {
			return -1;
		}
		return sA.compareToIgnoreCase(sB);
	}

	private boolean equalsSafe(String sA, String sB) {
		return sA == null ? sB == null : sA.equals(sB);
	}

	private String lower(String sValue) {
		return sValue != null ? sValue.toLowerCase() : null;
	}
}
