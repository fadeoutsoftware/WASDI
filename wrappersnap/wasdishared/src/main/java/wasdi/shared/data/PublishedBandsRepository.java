package wasdi.shared.data;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;

import wasdi.shared.business.PublishedBand;
import wasdi.shared.utils.Utils;

/**
 * Created by p.campanella on 17/11/2016.
 */
public class PublishedBandsRepository extends MongoRepository {
	
	public PublishedBandsRepository() {
		m_sThisCollection = "publishedbands";
	}
	
	/**
	 * Insert a published band
	 * @param oFile
	 * @return
	 */
    public boolean insertPublishedBand(PublishedBand oFile) {
        try {
            String sJSON = s_oMapper.writeValueAsString(oFile);
            getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }

    /**
     * Get a published band by product and band name
     * @param sProductName
     * @param sBandName
     * @return
     */
    public PublishedBand getPublishedBand(String sProductName, String sBandName) {
        try {
            BasicDBObject oQuery = new BasicDBObject();
            List<BasicDBObject> aoAndList = new ArrayList<>();
            aoAndList.add(new BasicDBObject("productName", sProductName));
            aoAndList.add(new BasicDBObject("bandName", sBandName));
            oQuery.put("$and", aoAndList);

            Document oSessionDocument = getCollection(m_sThisCollection).find(oQuery).first();

            if (oSessionDocument==null) return  null;

            String sJSON = oSessionDocument.toJson();

            PublishedBand oPublishedBand = s_oMapper.readValue(sJSON,PublishedBand.class);

            return oPublishedBand;
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  null;
    }
    
    /**
     * Get all the published bands of a product
     * @param sProductName
     * @return
     */
    public List<PublishedBand> getPublishedBandsByProductName(String sProductName) {

        final ArrayList<PublishedBand> aoReturnList = new ArrayList<PublishedBand>();
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(Filters.eq("productName", sProductName));
            
            fillList(aoReturnList, oWSDocuments, PublishedBand.class);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  aoReturnList;
    }
    
    /**
     * Get all the published bands
     * @return
     */
    public List<PublishedBand> getList() {

        final ArrayList<PublishedBand> aoReturnList = new ArrayList<PublishedBand>();
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find();

            fillList(aoReturnList, oWSDocuments, PublishedBand.class);
            
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  aoReturnList;
    }

    /**
     * Delete all the Published Bands of a Product
     * @param sProductName
     * @return
     */
    public int deleteByProductName(String sProductName) {
    	
    	if (Utils.isNullOrEmpty(sProductName)) return 0;

        try {

            DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteMany(Filters.eq("productName", sProductName));

            if (oDeleteResult != null)
            {
                return  (int) oDeleteResult.getDeletedCount();
            }

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return 0;
    }
    
    /**
     * Delete a specific Published band
     * @param sProductName
     * @param sLayerId
     * @return
     */
    public int deleteByProductNameLayerId(String sProductName, String sLayerId) {
    	
    	if (Utils.isNullOrEmpty(sProductName)) return 0;
    	if (Utils.isNullOrEmpty(sLayerId)) return 0;
    	

        try {

            DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteOne(Filters.and(Filters.eq("productName", sProductName), Filters.eq("layerId", sLayerId)));

            if (oDeleteResult != null)
            {
                return  (int) oDeleteResult.getDeletedCount();
            }

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return 0;
    }

}
