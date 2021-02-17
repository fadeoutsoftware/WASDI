package wasdi.shared.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import wasdi.shared.business.ProductWorkspace;
import wasdi.shared.utils.Utils;

/**
 * Created by p.campanella on 18/11/2016.
 */
public class ProductWorkspaceRepository extends MongoRepository {
	
	public ProductWorkspaceRepository() {
		m_sThisCollection = "productworkpsace";
	}
	
	/**
	 * Insert a new product Workspace
	 * @param oProductWorkspace Entity
	 * @return
	 */
    public boolean insertProductWorkspace(ProductWorkspace oProductWorkspace) {

        try {
            //check if product exists
            boolean bExists = existsProductWorkspace(oProductWorkspace.getProductName(), oProductWorkspace.getWorkspaceId());
            if (bExists) {
                return true;
            }

            String sJSON = s_oMapper.writeValueAsString(oProductWorkspace);
            getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }
    
    /**
     * Get the list of products in a Workspace
     * @param sWorkspaceId
     * @return
     */
    public List<ProductWorkspace> getProductsByWorkspace(String sWorkspaceId) {
        final ArrayList<ProductWorkspace> aoReturnList = new ArrayList<ProductWorkspace>();
        if(Utils.isNullOrEmpty(sWorkspaceId)) {
        	Utils.debugLog("ProductWorkspaceRepository.GetProductsByWorkspace( "+sWorkspaceId + " ): null workspace");
        	return aoReturnList;
        }
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(new Document("workspaceId", sWorkspaceId));

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
    
    /**
     * Get all the workpsaces where ProductId is present
     * @param sProductId id of the product
     * @return List of string with the workspaceId where product is present
     */
    public List<String> getWorkspaces(String sProductId) {    	
    	List<String> asWorkspaces = new ArrayList<String>();
    	
    	FindIterable<Document> aoDocuments = getCollection(m_sThisCollection).find(new Document("productName", sProductId));
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
    
    /**
     * Check if a product workspace exists by the two linked id
     * @param sProductId
     * @param sWorkspaceId
     * @return
     */
    public boolean existsProductWorkspace(String sProductId, String sWorkspaceId) {

        final ArrayList<ProductWorkspace> aoReturnList = new ArrayList<ProductWorkspace>();
        boolean bExists = false;
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(Filters.and(Filters.eq("productName", sProductId), Filters.eq("workspaceId", sWorkspaceId)));

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
    
    /**
     * Get a Product Workpsace from product id an workspace id
     * @param sProductId
     * @param sWorkspaceId
     * @return
     */
    public ProductWorkspace getProductWorkspace(String sProductId, String sWorkspaceId) {

        final ArrayList<ProductWorkspace> aoReturnList = new ArrayList<ProductWorkspace>();
        
        
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(Filters.and(Filters.eq("productName", sProductId), Filters.eq("workspaceId", sWorkspaceId)));

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
            return aoReturnList.get(0);

        return null;
    }
    
    /**
     * Delete all product workspace of the workpsaceId
     * @param sWorkspaceId
     * @return
     */
    public int deleteByWorkspaceId(String sWorkspaceId) {
    	
    	if (Utils.isNullOrEmpty(sWorkspaceId)) return 0;

        try {

            DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteMany(new Document("wokspaceId", sWorkspaceId));

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
     * Delete a specific product workspace
     * @param sProductName
     * @param sWorkspaceId
     * @return
     */
    public int deleteByProductNameWorkspace(String sProductName, String sWorkspaceId) {
    	
    	if (Utils.isNullOrEmpty(sProductName)) return 0;
    	if (Utils.isNullOrEmpty(sWorkspaceId)) return 0;

        try {

            DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteOne(Filters.and(Filters.eq("productName", sProductName), Filters.eq("workspaceId",sWorkspaceId)));

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
     * Delete all product workspaces by product name
     * @param sProductName
     * @return
     */
    public int deleteByProductName(String sProductName) {
    	
    	if (Utils.isNullOrEmpty(sProductName)) return 0;

        try {

            DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteMany(Filters.and(Filters.eq("productName", sProductName)));

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
     * Get the full list of product workspaces
     * @return
     */
    public List<ProductWorkspace> getList() {
        final ArrayList<ProductWorkspace> aoReturnList = new ArrayList<ProductWorkspace>();
        try {

            FindIterable<Document> oDFDocuments = getCollection(m_sThisCollection).find();

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
    
    /**
     * Update a Product Workpsace
     * @param oProductWorkspace
     * @param sOldProductName
     * @return
     */
    public boolean updateProductWorkspace(ProductWorkspace oProductWorkspace, String sOldProductName) {
        try {
            String sJSON = s_oMapper.writeValueAsString(oProductWorkspace);
            
            Bson oFilter = new Document("productName", sOldProductName);
            
            Bson oUpdateOperationDocument = new Document("$set", new Document(Document.parse(sJSON)));
            
            UpdateResult oResult = getCollection(m_sThisCollection).updateOne(oFilter, oUpdateOperationDocument);

            if (oResult.getModifiedCount()==1) return  true;
        }
        catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  false;
    }
    
    /**
     * Update a Product Workpsace
     * @param oProductWorkspace
     * @return
     */
    public boolean updateProductWorkspace(ProductWorkspace oProductWorkspace) {
        try {
            String sJSON = s_oMapper.writeValueAsString(oProductWorkspace);
            
            // Select by Product Name and Workspace Id
        	DBObject oQuery = QueryBuilder.start().and(
					QueryBuilder.start().put("productName").is(oProductWorkspace.getProductName()).get(),
					QueryBuilder.start().and(
						QueryBuilder.start().put("workspaceId").is(oProductWorkspace.getWorkspaceId()).get()
					).get()
		    ).get();
            
        	BasicDBObject oFilter = new BasicDBObject();
        	oFilter.putAll(oQuery);
        	
            Bson oUpdateOperationDocument = new Document("$set", new Document(Document.parse(sJSON)));
            
            UpdateResult oResult = getCollection(m_sThisCollection).updateOne(oFilter, oUpdateOperationDocument);

            if (oResult.getModifiedCount()==1) return  true;
        }
        catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  false;
    }
    
    /**
     * Get the product workpsace list of elements having the specified path
     * @param sFilePath
     * @return
     */
    public List<ProductWorkspace> getProductWorkspaceListByPath(String sFilePath) {
        final ArrayList<ProductWorkspace> aoReturnList = new ArrayList<ProductWorkspace>();
        try {

        	BasicDBObject oLikeQuery = new BasicDBObject();
        	Pattern oRegEx = Pattern.compile(sFilePath);
        	oLikeQuery.put("productName", oRegEx);
        	
            FindIterable<Document> oDFDocuments = getCollection(m_sThisCollection).find(oLikeQuery);

            oDFDocuments.forEach(new Block<Document>() {
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
            
}
