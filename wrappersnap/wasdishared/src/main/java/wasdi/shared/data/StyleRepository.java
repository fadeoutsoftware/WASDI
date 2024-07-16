package wasdi.shared.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;

import wasdi.shared.business.Style;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

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
			WasdiLog.errorLog("StyleRepository.insertStyle : error ", oEx);
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
			WasdiLog.errorLog("StyleRepository.getStyle : error ", oEx);
		}

		return null;
	}

	/**
	 * Get a style by name.
	 *
	 * @param sName style name
	 * @return the style corresponding to the name
	 */
	public Style getStyleByName(String sName) {

		try {
			long lCounter = getCollection(m_sThisCollection).countDocuments(new Document("name", sName));

			if (lCounter == 0) {
				return null;
			}

			Document oWSDocument = getCollection(m_sThisCollection).find(new Document("name", sName)).first();

			String sJSON = oWSDocument.toJson();

			Style oStyle = s_oMapper.readValue(sJSON, Style.class);

			return oStyle;
		} catch (Exception oEx) {
			WasdiLog.errorLog("StyleRepository.getStyleByName : error ", oEx);
		}

		return null;
	}

	/**
	 * Check if the name is already taken by other style.
	 *
	 * @param sName style name
	 * @return true if there is already a style with the same style, false otherwise
	 */
	public boolean isStyleNameTaken(String sName) {

		try {
			long lCounter = getCollection(m_sThisCollection).countDocuments(new Document("name", sName));

			return (lCounter > 0);
		} catch (Exception oEx) {
			WasdiLog.errorLog("StyleRepository.isStyleNameTaken : error ", oEx);
		}

		return true;
	}

	/**
	 * Get all the style that can be accessed by UserId
	 *
	 * @param sUserId
	 * @return List of private style of users plus all the public ones
	 */
	public List<Style> getStylePublicAndByUser(String sUserId) {
		// migrated to set in order to avoid redundancy
		final Set<Style> aoReturnSet = new HashSet<>();

		try {
			// Then search all the other style using UserId of the current user
			// - OR -
			// the public ones
			Bson oOrFilter = Filters.or(new Document("userId", sUserId), new Document("isPublic", true));

			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(oOrFilter).sort(new Document("name", 1));

			fillList(aoReturnSet, oWSDocuments, Style.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("StyleRepository.getStylePublicAndByUser : error ", oEx);
		}

		return new ArrayList<>(aoReturnSet);
	}

	/**
	 * Get the list of all styles
	 *
	 * @return
	 */
	public List<Style> getList() {
		final List<Style> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find().sort(new Document("name", 1));

			fillList(aoReturnList, oWSDocuments, Style.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("StyleRepository.getList : error ", oEx);
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
			WasdiLog.errorLog("StyleRepository.updateStyle : error ", oEx);
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
			WasdiLog.errorLog("StyleRepository.deleteStyle : error ", oEx);
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
			WasdiLog.errorLog("StyleRepository.deleteStyleByUser : error ", oEx);
		}

		return 0;
	}

	/**
	 * Check if the user is the owner of the style workspace
	 * @param sUserId a valid user id
	 * @param sStyleId a valid style id
	 * @return true if the user launched the style, false otherwise
	 */
	public boolean isOwnedByUser(String sUserId, String sStyleId) {
		try {
			Document oDoc = getCollection(m_sThisCollection).find(Filters.and(
					Filters.eq("userId", sUserId),
					Filters.eq("styleId", sStyleId)
					)).first();

			if (null != oDoc) {
				return true;
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("StyleRepository.isStyleOwnedByUser( " + sUserId + ", " + sStyleId + " ): ", oE);
		}

		return false;
	}
	
    /**
     * Find a style by partial name, by partial description or by partial id
     * @return the list of styles that partially match the name, the description or the id
     */
    public List<Style> findStylesByPartialName(String sPartialName) {

        final ArrayList<Style> aoReturnList = new ArrayList<Style>();
        
        if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
        	return null;
        }
        
		Pattern regex = Pattern.compile(Pattern.quote(sPartialName), Pattern.CASE_INSENSITIVE);
		
		Bson oFilterLikeStyleId= Filters.eq("styleId", regex);
		Bson oFilterLikeName = Filters.eq("name", regex);
		Bson oFilterLikeDescription = Filters.eq("description", regex);
		
		Bson oFilter = Filters.or(oFilterLikeStyleId, oFilterLikeName, oFilterLikeDescription);

        try {

            FindIterable<Document> oRetrievedDocuments = getCollection(m_sThisCollection)
            		.find(oFilter)
            		.sort(new Document("name", 1));
            
            fillList(aoReturnList, oRetrievedDocuments, Style.class);
            
        } catch (Exception oEx) {
        	WasdiLog.errorLog("StyleWorkspaceRepository.findStylesByPartialName : error ", oEx);
        }

        return aoReturnList;
    }

}
