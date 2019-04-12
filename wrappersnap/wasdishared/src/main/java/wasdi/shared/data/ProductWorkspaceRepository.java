package wasdi.shared.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;

import wasdi.shared.business.ProductWorkspace;

/**
 * Created by p.campanella on 18/11/2016.
 */
public class ProductWorkspaceRepository extends MongoRepository {

    public boolean InsertProductWorkspace(ProductWorkspace oProductWorkspace) {

        try {
            //check if product exists
            boolean bExists = ExistsProductWorkspace(oProductWorkspace.getProductName(), oProductWorkspace.getWorkspaceId());
            if (bExists) {
                return true;
            }

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

    public List<String> getWorkspaces(String sProductId) {    	
    	List<String> asWorkspaces = new ArrayList<String>();
    	
    	FindIterable<Document> aoDocuments = getCollection("productworkpsace").find(new Document("productName", sProductId));
    	aoDocuments.forEach(new Block<Document>() {
    		public void apply(Document oDocument) {
                String sJson = oDocument.toJson();
                try {
                	ProductWorkspace pw = s_oMapper.readValue(sJson,ProductWorkspace.class);
                    asWorkspaces.add(pw.getWorkspaceId());
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
		});
    	
		return asWorkspaces ;
    }

    public boolean ExistsProductWorkspace(String sProductId, String sWorkspaceId) {

        final ArrayList<ProductWorkspace> aoReturnList = new ArrayList<ProductWorkspace>();
        boolean bExists = false;
        try {

            FindIterable<Document> oWSDocuments = getCollection("productworkpsace").find(Filters.and(Filters.eq("productName", sProductId), Filters.eq("workspaceId", sWorkspaceId)));

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

        if (aoReturnList.size() > 0)
            bExists = true;

        return bExists;
    }

    public int DeleteByWorkspaceId(String sWorkspaceId) {

        try {

            DeleteResult oDeleteResult = getCollection("productworkpsace").deleteMany(new Document("wokspaceId", sWorkspaceId));

            if (oDeleteResult != null)
            {
                return  (int) oDeleteResult.getDeletedCount();
            }

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return 0;
    }

    public int DeleteByProductNameWorkspace(String sProductName, String sWorkspaceId) {

        try {

            DeleteResult oDeleteResult = getCollection("productworkpsace").deleteOne(Filters.and(Filters.eq("productName", sProductName), Filters.eq("workspaceId",sWorkspaceId)));

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

            DeleteResult oDeleteResult = getCollection("productworkpsace").deleteMany(Filters.and(Filters.eq("productName", sProductName)));

            if (oDeleteResult != null)
            {
                return  (int) oDeleteResult.getDeletedCount();
            }

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return 0;
    }    
    
    
    public List<ProductWorkspace> getList() {
        final ArrayList<ProductWorkspace> aoReturnList = new ArrayList<ProductWorkspace>();
        try {

            FindIterable<Document> oDFDocuments = getCollection("productworkpsace").find();

            oDFDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    ProductWorkspace oProductWS = null;
                    try {
                        oProductWS = s_oMapper.readValue(sJSON,ProductWorkspace.class);
                        aoReturnList.add(oProductWS);
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
}
