package wasdi.shared.data.mongo.labelling;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.result.DeleteResult;

import wasdi.shared.business.labelling.Label;
import wasdi.shared.data.interfaces.labelling.ILabelRepositoryBackend;
import wasdi.shared.data.mongo.MongoRepository;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class MongoLabelRepositoryBackend  extends MongoRepository implements ILabelRepositoryBackend {

	public MongoLabelRepositoryBackend() {
		m_sThisCollection = "labelling_labels";
	}

	@Override
	public boolean insertLabel(Label oLabel) {
		try {
			if (oLabel == null) {
				return false;
			}

			String sJSON = s_oMapper.writeValueAsString(oLabel);
			getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

			return true;
		} catch (Exception oEx) {
			WasdiLog.errorLog("MongoLabelRepositoryBackend.insertLabel : error ", oEx);
		}

		return false;
	}

	@Override
	public Label getLabel(String sLabelId) {
		try {
			if (Utils.isNullOrEmpty(sLabelId)) {
				return null;
			}

			Document oDocument = getCollection(m_sThisCollection).find(new Document("id", sLabelId)).first();

			if (oDocument == null) {
				return null;
			}

			String sJSON = oDocument.toJson();
			return s_oMapper.readValue(sJSON, Label.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("MongoLabelRepositoryBackend.getLabel : error ", oEx);
		}

		return null;
	}

	@Override
	public boolean updateLabel(Label oLabel) {
		try {
			if (oLabel == null || Utils.isNullOrEmpty(oLabel.getId())) {
				return false;
			}

			String sJSON = s_oMapper.writeValueAsString(oLabel);
			Document oFilter = new Document("id", oLabel.getId());
			Document oUpdate = new Document("$set", new Document(Document.parse(sJSON)));
			getCollection(m_sThisCollection).updateOne(oFilter, oUpdate);

			return true;
		} catch (Exception oEx) {
			WasdiLog.errorLog("MongoLabelRepositoryBackend.updateLabel : error ", oEx);
		}

		return false;
	}

	@Override
	public boolean deleteLabel(String sLabelId) {
		if (Utils.isNullOrEmpty(sLabelId)) {
			return false;
		}

		try {
			DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteOne(new Document("id", sLabelId));

			return oDeleteResult != null && oDeleteResult.getDeletedCount() == 1;
		} catch (Exception oEx) {
			WasdiLog.errorLog("MongoLabelRepositoryBackend.deleteLabel : error ", oEx);
		}

		return false;
	}

	@Override
	public List<Label> getLabelsByImage(String sDatasetId, String sImage) {
		final List<Label> aoReturnList = new ArrayList<>();

		if (Utils.isNullOrEmpty(sDatasetId) || Utils.isNullOrEmpty(sImage)) {
			return aoReturnList;
		}

		try {
			Document oFilter = new Document("datasetId", sDatasetId).append("image", sImage);
			FindIterable<Document> oDocuments = getCollection(m_sThisCollection)
					.find(oFilter)
					.sort(new Document("id", 1));
			fillList(aoReturnList, oDocuments, Label.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("MongoLabelRepositoryBackend.getLabelsByImage : error ", oEx);
		}

		return aoReturnList;
	}

	@Override
	public List<Label> getAll() {
		final List<Label> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oDocuments = getCollection(m_sThisCollection).find().sort(new Document("id", 1));
			fillList(aoReturnList, oDocuments, Label.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("MongoLabelRepositoryBackend.getAll : error ", oEx);
		}

		return aoReturnList;
	}

}
