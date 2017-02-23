package wasdi.shared.data;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.PublishedBand;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by p.campanella on 17/11/2016.
 */
public class PublishedBandsRepository extends MongoRepository {

    public boolean InsertPublishedBand(PublishedBand oFile) {
        try {
            String sJSON = s_oMapper.writeValueAsString(oFile);
            getCollection("publishedbands").insertOne(Document.parse(sJSON));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }

    public PublishedBand GetPublishedBand(String sProductName, String sBandName) {
        try {
            BasicDBObject oQuery = new BasicDBObject();
            List<BasicDBObject> aoAndList = new ArrayList<>();
            aoAndList.add(new BasicDBObject("productName", sProductName));
            aoAndList.add(new BasicDBObject("bandName", sBandName));
            oQuery.put("$and", aoAndList);

            Document oSessionDocument = getCollection("publishedbands").find(oQuery).first();

            if (oSessionDocument==null) return  null;

            String sJSON = oSessionDocument.toJson();

            PublishedBand oPublishedBand = s_oMapper.readValue(sJSON,PublishedBand.class);

            return oPublishedBand;
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  null;
    }

    public List<PublishedBand> GetPublishedBandsByProductName(String sProductName) {

        final ArrayList<PublishedBand> aoReturnList = new ArrayList<PublishedBand>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("publishedbands").find(Filters.eq("productName", sProductName));

            oWSDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    PublishedBand oPublishedBand = null;
                    try {
                        oPublishedBand = s_oMapper.readValue(sJSON,PublishedBand.class);
                        aoReturnList.add(oPublishedBand);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            });

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  aoReturnList;
    }

    public int DeleteByProductName(String sProductName) {

        try {

            DeleteResult oDeleteResult = getCollection("publishedbands").deleteMany(Filters.eq("productName", sProductName));

            if (oDeleteResult != null)
            {
                return  (int) oDeleteResult.getDeletedCount();
            }

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return 0;
    }

    public int DeleteByProductNameLayerId(String sProductName, String sLayerId) {

        try {

            DeleteResult oDeleteResult = getCollection("publishedbands").deleteOne(Filters.and(Filters.eq("productName", sProductName), Filters.eq("layerId", sLayerId)));

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
