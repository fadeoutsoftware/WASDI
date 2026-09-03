package wasdi.shared.data.mongo;

import org.bson.Document;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;

import wasdi.shared.data.interfaces.IDeletedUserRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class MongoDeletedUserRepositoryBackend extends MongoRepository implements IDeletedUserRepositoryBackend {

    private static final String COLLECTION_NAME = "deletedUsers";

    public MongoDeletedUserRepositoryBackend() {
        m_sThisCollection = COLLECTION_NAME;
    }

    @Override
    public boolean markDeleted(String sUserId) {
        if (Utils.isNullOrEmpty(sUserId)) {
            return false;
        }

        try {
            getCollection(COLLECTION_NAME).updateOne(
                    Filters.eq("userId", sUserId.toLowerCase()),
                    Updates.setOnInsert("deletedAt", Utils.nowInMillis()),
                    new UpdateOptions().upsert(true));
            return true;
        } catch (Exception oEx) {
            WasdiLog.errorLog("MongoDeletedUserRepositoryBackend.markDeleted: error for user " + sUserId, oEx);
            return false;
        }
    }

    @Override
    public boolean isDeleted(String sUserId) {
        if (Utils.isNullOrEmpty(sUserId)) {
            return false;
        }

        try {
            return getCollection(COLLECTION_NAME).find(new Document("userId", sUserId.toLowerCase())).first() != null;
        } catch (Exception oEx) {
            WasdiLog.errorLog("MongoDeletedUserRepositoryBackend.isDeleted: error for user " + sUserId, oEx);
            return false;
        }
    }

    @Override
    public boolean remove(String sUserId) {
        if (Utils.isNullOrEmpty(sUserId)) {
            return false;
        }

        try {
            return getCollection(COLLECTION_NAME).deleteMany(Filters.eq("userId", sUserId.toLowerCase())).getDeletedCount() > 0;
        } catch (Exception oEx) {
            WasdiLog.errorLog("MongoDeletedUserRepositoryBackend.remove: error for user " + sUserId, oEx);
            return false;
        }
    }
}
