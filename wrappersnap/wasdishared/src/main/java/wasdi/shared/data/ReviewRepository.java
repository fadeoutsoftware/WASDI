package wasdi.shared.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;

import wasdi.shared.business.AppCategory;
import wasdi.shared.business.Counter;
import wasdi.shared.business.PublishedBand;
import wasdi.shared.business.Review;
import wasdi.shared.utils.Utils;

public class ReviewRepository extends MongoRepository {
	
    public List<Review> getReviews(String sProcessorId) {

        final ArrayList<Review> aoReturnList = new ArrayList<Review>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("reviews").find(new Document("processorId", sProcessorId));
            fillList(aoReturnList, oWSDocuments, "ReviewRepository", Review.class);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
	public String insertReview(Review oReview) {
		String sResult = "";
		if(oReview != null) {
			try {
				String sJSON = s_oMapper.writeValueAsString(oReview);
				Document oDocument = Document.parse(sJSON);
				getCollection("reviews").insertOne(oDocument);
				sResult = oDocument.getObjectId("_id").toHexString();
	
			} catch (Exception oEx) {
				Utils.debugLog("ReviewRepository.InsertReview: " + oEx);
			}
		}
		return sResult;
	}
    
}
