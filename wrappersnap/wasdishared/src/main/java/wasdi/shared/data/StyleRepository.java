package wasdi.shared.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;

import wasdi.shared.business.Style;
import wasdi.shared.utils.Utils;

public class StyleRepository extends MongoRepository {

	public StyleRepository() {
		m_sThisCollection = "styles";
	}

	/**
	 * Insert a new style
	 *
	 * @param oStyle
	 * @return
	 */
	public boolean insertStyle(Style oStyle) {

		try {
			String sJSON = s_oMapper.writeValueAsString(oStyle);
			getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

			return true;
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return false;
	}

	/**
	 * Get a style by Id
	 *
	 * @param sStyleId
	 * @return
	 */
	public Style getStyle(String sStyleId) {

		try {
			long lCounter = getCollection(m_sThisCollection).countDocuments(new Document("styleId", sStyleId));

			if (lCounter == 0) {
				return null;
			}

			Document oWSDocument = getCollection(m_sThisCollection).find(new Document("styleId", sStyleId)).first();

			String sJSON = oWSDocument.toJson();

			Style oStyle = s_oMapper.readValue(sJSON, Style.class);

			return oStyle;
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return null;
	}

	/**
	 * Get all the style that can be accessed by UserId
	 *
	 * @param sUserId
	 * @return List of private style of users plus all the public ones
	 */
	public List<Style> getStylePublicAndByUser(String sUserId) {
		// migrated to set in order to avoid redundancy
		final Set<Style> aoReturnList = new HashSet<>();

		try {
			// Then search all the other style using UserId of the current user
			// - OR -
			// the public ones
			Bson oOrFilter = Filters.or(new Document("userId", sUserId), new Document("isPublic", true));

			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(oOrFilter);

			fillList(aoReturnList, oWSDocuments, Style.class);
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return new ArrayList<Style>(aoReturnList);
	}

	/**
	 * Get the list of all styles
	 *
	 * @return
	 */
	public List<Style> getList() {
		final List<Style> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find();

			fillList(aoReturnList, oWSDocuments, Style.class);
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return aoReturnList;
	}

	/**
	 * Update a Style
	 *
	 * @param oStyle
	 * @return
	 */
	public boolean updateStyle(Style oStyle) {

		try {
			String sJSON = s_oMapper.writeValueAsString(oStyle);
			Document filter = new Document("styleId", oStyle.getStyleId());
			Document update = new Document("$set", new Document(Document.parse(sJSON)));
			getCollection(m_sThisCollection).updateOne(filter, update);

			return true;
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return false;
	}

	/**
	 * Deletes a style
	 */
	public boolean deleteStyle(String sStyleId) {
		if (Utils.isNullOrEmpty(sStyleId))
			return false;

		try {
			DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteOne(new Document("styleId", sStyleId));

			if (oDeleteResult != null) {
				if (oDeleteResult.getDeletedCount() == 1) {
					return true;
				}
			}
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return false;
	}

	/**
	 * Delete all the styles of User
	 *
	 * @param sUserId
	 * @return
	 */
	public int deleteStyleByUser(String sUserId) {
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

}
