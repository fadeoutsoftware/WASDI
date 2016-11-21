package wasdi.shared.data;

import com.mongodb.BasicDBObject;
import org.bson.Document;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.PublishedBand;

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
}
