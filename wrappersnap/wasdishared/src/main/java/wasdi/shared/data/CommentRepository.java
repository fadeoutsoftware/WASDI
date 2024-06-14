package wasdi.shared.data;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;

import wasdi.shared.business.Comment;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class CommentRepository extends MongoRepository {

	public CommentRepository() {
		m_sThisCollection = "comments";
	}

    public List<Comment> getComments(String sReviewId) {

        final ArrayList<Comment> aoReturnList = new ArrayList<Comment>();
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(new Document("reviewId", sReviewId)).sort(new Document("date", 1));
            fillList(aoReturnList, oWSDocuments, Comment.class);

        } catch (Exception oEx) {
        	WasdiLog.errorLog("CommentRepository.getComments: error", oEx);
        }

        return aoReturnList;
    }

    /**
     * Get a comment by Id
     * @param sCommentId id of the review
     * @return Entity
     */
    public Comment getComment(String sCommentId) {

        try {
            Document oWSDocument = getCollection(m_sThisCollection).find(new Document("commentId", sCommentId)).first();

            if (null != oWSDocument) {
            	String sJSON = oWSDocument.toJson();
            	return s_oMapper.readValue(sJSON, Comment.class);
            }
        } catch (Exception oEx) {
        	WasdiLog.errorLog("CommentRepository.getComment: error", oEx);
        }

        return  null;
    }

	public String addComment(Comment oComment) {
		return add(oComment, m_sThisCollection, "CommentRepository.InsertComment");
	}

    public int deleteComment(String sReviewId, String sCommentId) {
    	if (Utils.isNullOrEmpty(sReviewId)) return 0;
    	if (Utils.isNullOrEmpty(sCommentId)) return 0;

		BasicDBObject oCriteria = new BasicDBObject();
		oCriteria.append("reviewId", sReviewId);
		oCriteria.append("commentId", sCommentId);

        return delete(oCriteria, m_sThisCollection);
    }

    public int deleteComments(String sReviewId) {
    	if (Utils.isNullOrEmpty(sReviewId)) return 0;

		BasicDBObject oCriteria = new BasicDBObject();
		oCriteria.append("reviewId", sReviewId);

        return deleteMany(oCriteria, m_sThisCollection);
    }

    public boolean updateComment(Comment oComment) {
		BasicDBObject oCriteria = new BasicDBObject();
		oCriteria.append("commentId", oComment.getCommentId());
		oCriteria.append("reviewId", oComment.getReviewId());

        return  update(oCriteria, oComment, m_sThisCollection);
    }
    
	public boolean isTheOwnerOfTheComment(String sReviewId, String sCommentId, String sUserId) {
		boolean bIsTheOwner = false;

        final ArrayList<Comment> aoReturnList = new ArrayList<Comment>();

        try {
    		BasicDBObject oCriteria = new BasicDBObject();
    		oCriteria.append("reviewId", sReviewId);
    		oCriteria.append("commentId", sCommentId);
    		oCriteria.append("userId", sUserId);

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(oCriteria);
            fillList(aoReturnList, oWSDocuments, Comment.class);
        } catch (Exception oEx) {
        	WasdiLog.errorLog("CommentRepository.isTheOwnerOfTheComment: error", oEx);
        }

        if (aoReturnList.size() > 0) {
        	bIsTheOwner = true;
        }
		
		return bIsTheOwner;
	}

	/**
	 * Delete all the comments of a specific user
	 * @param sUserId
	 * @return
	 */
    public int deleteCommentsByUser(String sUserId) {
    	if (Utils.isNullOrEmpty(sUserId)) return 0;

		BasicDBObject oCriteria = new BasicDBObject();
		oCriteria.append("userId", sUserId);

        return deleteMany(oCriteria, m_sThisCollection);
    }
}
