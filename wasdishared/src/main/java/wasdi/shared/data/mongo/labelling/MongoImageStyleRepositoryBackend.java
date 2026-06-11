package wasdi.shared.data.mongo.labelling;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.result.DeleteResult;

import wasdi.shared.business.labelling.ImageStyle;
import wasdi.shared.data.interfaces.labelling.IImageStyleRepositoryBackend;
import wasdi.shared.data.mongo.MongoRepository;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class MongoImageStyleRepositoryBackend  extends MongoRepository implements IImageStyleRepositoryBackend {

	public MongoImageStyleRepositoryBackend() {
		m_sThisCollection = "labelling_image_styles";
	}

	@Override
	public boolean insertImageStyle(ImageStyle oImageStyle) {
		try {
			if (oImageStyle == null) {
				return false;
			}

			String sJSON = s_oMapper.writeValueAsString(oImageStyle);
			getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

			return true;
		} catch (Exception oEx) {
			WasdiLog.errorLog("MongoImageStyleRepositoryBackend.insertImageStyle : error ", oEx);
		}

		return false;
	}

	@Override
	public ImageStyle getImageStyle(String sImageStyleId) {
		try {
			if (Utils.isNullOrEmpty(sImageStyleId)) {
				return null;
			}

			Document oDocument = getCollection(m_sThisCollection).find(new Document("id", sImageStyleId)).first();

			if (oDocument == null) {
				return null;
			}

			String sJSON = oDocument.toJson();
			return s_oMapper.readValue(sJSON, ImageStyle.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("MongoImageStyleRepositoryBackend.getImageStyle : error ", oEx);
		}

		return null;
	}

	@Override
	public boolean updateImageStyle(ImageStyle oImageStyle) {
		try {
			if (oImageStyle == null || Utils.isNullOrEmpty(oImageStyle.getId())) {
				return false;
			}

			String sJSON = s_oMapper.writeValueAsString(oImageStyle);
			Document oFilter = new Document("id", oImageStyle.getId());
			Document oUpdate = new Document("$set", new Document(Document.parse(sJSON)));
			getCollection(m_sThisCollection).updateOne(oFilter, oUpdate);

			return true;
		} catch (Exception oEx) {
			WasdiLog.errorLog("MongoImageStyleRepositoryBackend.updateImageStyle : error ", oEx);
		}

		return false;
	}

	@Override
	public boolean deleteImageStyle(String sImageStyleId) {
		if (Utils.isNullOrEmpty(sImageStyleId)) {
			return false;
		}

		try {
			DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteOne(new Document("id", sImageStyleId));

			return oDeleteResult != null && oDeleteResult.getDeletedCount() == 1;
		} catch (Exception oEx) {
			WasdiLog.errorLog("MongoImageStyleRepositoryBackend.deleteImageStyle : error ", oEx);
		}

		return false;
	}

	@Override
	public List<ImageStyle> getAll() {
		final List<ImageStyle> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oDocuments = getCollection(m_sThisCollection).find().sort(new Document("id", 1));
			fillList(aoReturnList, oDocuments, ImageStyle.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("MongoImageStyleRepositoryBackend.getAll : error ", oEx);
		}

		return aoReturnList;
	}

}
