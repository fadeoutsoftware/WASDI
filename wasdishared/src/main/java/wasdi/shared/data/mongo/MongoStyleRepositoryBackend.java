package wasdi.shared.data.mongo;

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
import wasdi.shared.data.interfaces.IStyleRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Mongo backend implementation for style repository.
 */
public class MongoStyleRepositoryBackend extends MongoRepository implements IStyleRepositoryBackend {

	public MongoStyleRepositoryBackend() {
		m_sThisCollection = "styles";
	}

	@Override
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

	@Override
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

	@Override
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

	@Override
	public boolean isStyleNameTaken(String sName) {

		try {
			long lCounter = getCollection(m_sThisCollection).countDocuments(new Document("name", sName));

			return (lCounter > 0);
		} catch (Exception oEx) {
			WasdiLog.errorLog("StyleRepository.isStyleNameTaken : error ", oEx);
		}

		return true;
	}

	@Override
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

	@Override
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

	@Override
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

	@Override
	public boolean deleteStyle(String sStyleId) {
		if (Utils.isNullOrEmpty(sStyleId)) {
			return false;
		}

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

	@Override
	public int deleteStyleByUser(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return 0;
		}

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

	@Override
	public boolean isOwnedByUser(String sUserId, String sStyleId) {
		try {
			Document oDoc = getCollection(m_sThisCollection).find(Filters.and(Filters.eq("userId", sUserId), Filters.eq("styleId", sStyleId))).first();

			if (null != oDoc) {
				return true;
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("StyleRepository.isStyleOwnedByUser( " + sUserId + ", " + sStyleId + " ): ", oE);
		}

		return false;
	}

	@Override
	public List<Style> findStylesByPartialName(String sPartialName) {

		final ArrayList<Style> aoReturnList = new ArrayList<Style>();

		if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
			return null;
		}

		Pattern regex = Pattern.compile(Pattern.quote(sPartialName), Pattern.CASE_INSENSITIVE);

		Bson oFilterLikeStyleId = Filters.eq("styleId", regex);
		Bson oFilterLikeName = Filters.eq("name", regex);
		Bson oFilterLikeDescription = Filters.eq("description", regex);

		Bson oFilter = Filters.or(oFilterLikeStyleId, oFilterLikeName, oFilterLikeDescription);

		try {

			FindIterable<Document> oRetrievedDocuments = getCollection(m_sThisCollection).find(oFilter).sort(new Document("name", 1));

			fillList(aoReturnList, oRetrievedDocuments, Style.class);

		} catch (Exception oEx) {
			WasdiLog.errorLog("StyleWorkspaceRepository.findStylesByPartialName : error ", oEx);
		}

		return aoReturnList;
	}
}
