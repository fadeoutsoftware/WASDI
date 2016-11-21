package wasdi.shared.data;

import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import wasdi.shared.business.ProductWorkspace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by p.campanella on 18/11/2016.
 */
public class ProductWorkspaceRepository extends MongoRepository {

    public boolean InsertProductWorkspace(ProductWorkspace oProductWorkspace) {

        try {
            String sJSON = s_oMapper.writeValueAsString(oProductWorkspace);
            getCollection("productworkpsace").insertOne(Document.parse(sJSON));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }

    public List<ProductWorkspace> GetProductsByWorkspace(String sWorkspaceId) {

        final ArrayList<ProductWorkspace> aoReturnList = new ArrayList<ProductWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("productworkpsace").find(new Document("workspaceId", sWorkspaceId));

            oWSDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    ProductWorkspace oProductWorkspace = null;
                    try {
                        oProductWorkspace = s_oMapper.readValue(sJSON,ProductWorkspace.class);
                        aoReturnList.add(oProductWorkspace);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            });

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }


    public List<ProductWorkspace> GetWorkspaceByProduct(String sProductId) {

        final ArrayList<ProductWorkspace> aoReturnList = new ArrayList<ProductWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("productworkspace").find(new Document("productName", sProductId));

            oWSDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    ProductWorkspace oProductWorkspace = null;
                    try {
                        oProductWorkspace = s_oMapper.readValue(sJSON,ProductWorkspace.class);
                        aoReturnList.add(oProductWorkspace);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            });

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }

    public int DeleteByWorkspaceId(String sWorkspaceId) {

        try {

            DeleteResult oDeleteResult = getCollection("productworkspace").deleteMany(new Document("wokspaceId", sWorkspaceId));

            if (oDeleteResult != null)
            {
                return  (int) oDeleteResult.getDeletedCount();
            }

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return 0;
    }

    public int DeleteByProductName(String sProductName) {

        try {

            DeleteResult oDeleteResult = getCollection("productworkspace").deleteMany(new Document("productName", sProductName));

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
