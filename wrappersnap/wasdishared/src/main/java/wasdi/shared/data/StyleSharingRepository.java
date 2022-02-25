package wasdi.shared.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;

import wasdi.shared.business.StyleSharing;
import wasdi.shared.utils.Utils;

/**
 * @author PetruPetrescu on 23/02/2022
 */
public class StyleSharingRepository extends MongoRepository {

	public StyleSharingRepository() {
		m_sThisCollection = "stylessharing";
	}

	/**
	 * Insert a New Style sharing
	 * 
	 * @param oStyleSharing
	 * @return
	 */
	public boolean insertStyleSharing(StyleSharing oStyleSharing) {

		try {
			String sJSON = s_oMapper.writeValueAsString(oStyleSharing);
			getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

			return true;
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return false;
	}

	/**
	 * Get all the styles shared by this owner User
	 * 
	 * @param sUserId
	 * @return
	 */
	public List<StyleSharing> getStyleSharingByOwner(String sUserId) {
		final List<StyleSharing> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(new Document("ownerId", sUserId));

			fillList(aoReturnList, oWSDocuments, StyleSharing.class);
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return aoReturnList;
	}

	/**
	 * Get all the styles shared with this User
	 * 
	 * @param sUserId
	 * @return
	 */
	public List<StyleSharing> getStyleSharingByUser(String sUserId) {
		final List<StyleSharing> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(new Document("userId", sUserId));

			fillList(aoReturnList, oWSDocuments, StyleSharing.class);
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return aoReturnList;
	}

	/**
	 * Get all the sharings of this style
	 * 
	 * @param sStyleId
	 * @return
	 */
	public List<StyleSharing> getStyleSharingByStyle(String sStyleId) {
		final List<StyleSharing> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(new Document("styleId", sStyleId));

			fillList(aoReturnList, oWSDocuments, StyleSharing.class);

		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return aoReturnList;
	}

	/**
	 * Get all the sharings
	 * 
	 * @param sStyleId
	 * @return
	 */
	public List<StyleSharing> getStyleSharings() {
		final List<StyleSharing> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find();

			fillList(aoReturnList, oWSDocuments, StyleSharing.class);
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return aoReturnList;
	}

	/**
	 * Deletes all the instances of sharing of an user for a specific style
	 * 
	 * @param sStyleId The string representing the style
	 * @param sUserId The user id to identify the user
	 * @return An integer, reporting the count of the deleted items
	 */
	public int deleteByStyleIdUserId(String sStyleId, String sUserId) {
		if (Utils.isNullOrEmpty(sStyleId) || Utils.isNullOrEmpty(sUserId))
			return 0;

		try {
			DeleteResult oDeleteResult = getCollection(m_sThisCollection)
					.deleteMany(Filters.and(Filters.eq("userId", sUserId), Filters.eq("styleId", sStyleId)));

			if (oDeleteResult != null) {
				return (int) oDeleteResult.getDeletedCount();
			}
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return 0;
	}

	/**
	 * Delete all the sharings of a specific Style
	 * 
	 * @param sStyleId
	 * @return
	 */
	public int deleteByStyleId(String sStyleId) {
		if (Utils.isNullOrEmpty(sStyleId))
			return 0;

		try {
			DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteMany(new Document("styleId", sStyleId));

			if (oDeleteResult != null) {
				return (int) oDeleteResult.getDeletedCount();
			}
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return 0;
	}

	/**
	 * Delete all the sharings with User
	 * 
	 * @param sUserId
	 * @return
	 */
	public int deleteByUserId(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId))
			return 0;

		try {
			DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteMany(new Document("userId", sUserId));

			if (oDeleteResult != null) {
				return (int) oDeleteResult.getDeletedCount();
			}
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return 0;
	}

	/**
	 * Delete a specific Sharing of this style with this user
	 * 
	 * @param sUserId
	 * @param sStyleId
	 * @return
	 */
	public int deleteByUserIdStyleId(String sUserId, String sStyleId) {
		if (Utils.isNullOrEmpty(sStyleId))
			return 0;

		if (Utils.isNullOrEmpty(sUserId))
			return 0;

		try {
			DeleteResult oDeleteResult = getCollection(m_sThisCollection)
					.deleteMany(Filters.and(Filters.eq("userId", sUserId), Filters.eq("styleId", sStyleId)));

			if (oDeleteResult != null) {
				return (int) oDeleteResult.getDeletedCount();
			}
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return 0;
	}

	/**
	 * Checks if style is shared with user
	 * 
	 * @param sUserId
	 * @param sStyleId
	 * @return
	 */
	public boolean isSharedWithUser(String sUserId, String sStyleId) {
		return (getStyleSharingByUserIdStyleId(sUserId, sStyleId) != null);
	}

	/**
	 * Returns the StyleSharing for the user and style
	 * 
	 * @param sUserId
	 * @param sStyleId
	 * @return
	 */
	public StyleSharing getStyleSharingByUserIdStyleId(String sUserId, String sStyleId) {

		try {
			Document oWSDocument = getCollection(m_sThisCollection)
					.find(Filters.and(Filters.eq("userId", sUserId), Filters.eq("styleId", sStyleId))).first();

			if (null != oWSDocument) {
				String sJSON = oWSDocument.toJson();
				StyleSharing oStyleSharing;

				try {
					oStyleSharing = s_oMapper.readValue(sJSON, StyleSharing.class);
					return oStyleSharing;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (Exception oE) {
			Utils.debugLog("StyleSharingRepository.getStyleSharingByUserIdStyleId( " + sUserId + ", " + sStyleId
					+ "): error: " + oE);
		}

		return null;
	}

}
