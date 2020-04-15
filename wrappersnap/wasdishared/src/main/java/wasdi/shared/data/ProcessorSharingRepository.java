package wasdi.shared.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;

import wasdi.shared.business.ProcessorSharing;

public class ProcessorSharingRepository  extends  MongoRepository {
	
	
    public boolean insertProcessorSharing(ProcessorSharing oProcessorSharing) {

        try {
            String sJSON = s_oMapper.writeValueAsString(oProcessorSharing);
            getCollection("processorsharing").insertOne(Document.parse(sJSON));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }

    public List<ProcessorSharing> getProcessorSharingByOwner(String sUserId) {

        final ArrayList<ProcessorSharing> aoReturnList = new ArrayList<ProcessorSharing>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processorsharing").find(new Document("ownerId", sUserId));

            oWSDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    ProcessorSharing oProcessorSharing = null;
                    try {
                        oProcessorSharing = s_oMapper.readValue(sJSON,ProcessorSharing.class);
                        aoReturnList.add(oProcessorSharing);
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
    
    public List<ProcessorSharing> getProcessorSharingByUser(String sUserId) {

        final ArrayList<ProcessorSharing> aoReturnList = new ArrayList<ProcessorSharing>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processorsharing").find(new Document("userId", sUserId));

            oWSDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    ProcessorSharing oProcessorSharing = null;
                    try {
                        oProcessorSharing = s_oMapper.readValue(sJSON,ProcessorSharing.class);
                        aoReturnList.add(oProcessorSharing);
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


    public List<ProcessorSharing> getProcessorSharingByProcessorId(String sProcessorId) {

        final ArrayList<ProcessorSharing> aoReturnList = new ArrayList<ProcessorSharing>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processorsharing").find(new Document("processorId", sProcessorId));

            oWSDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    ProcessorSharing oProcessorSharing = null;
                    try {
                        oProcessorSharing = s_oMapper.readValue(sJSON,ProcessorSharing.class);
                        aoReturnList.add(oProcessorSharing);
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
    
    public ProcessorSharing getProcessorSharingByUserIdProcessorId(String sUserId, String sProcessorId) {

        final ArrayList<ProcessorSharing> aoReturnList = new ArrayList<ProcessorSharing>();
        try {
        	
            FindIterable<Document> oWSDocuments = getCollection("processorsharing").find(Filters.and(Filters.eq("userId", sUserId), Filters.eq("processorId", sProcessorId)));

            oWSDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    ProcessorSharing oProcessorSharing = null;
                    try {
                        oProcessorSharing = s_oMapper.readValue(sJSON,ProcessorSharing.class);
                        aoReturnList.add(oProcessorSharing);
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

    public int deleteByProcessorId(String sProcessorId) {

        try {

            DeleteResult oDeleteResult = getCollection("processorsharing").deleteMany(new Document("processorId", sProcessorId));

            if (oDeleteResult != null)
            {
                return  (int) oDeleteResult.getDeletedCount();
            }

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return 0;
    }

    public int deleteByUserId(String sUserId) {

        try {

            DeleteResult oDeleteResult = getCollection("processorsharing").deleteMany(new Document("userId", sUserId));

            if (oDeleteResult != null)
            {
                return  (int) oDeleteResult.getDeletedCount();
            }

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return 0;
    }
    
    public int deleteByUserIdProcessorId(String sUserId, String sProcessorId) {
        try {

            DeleteResult oDeleteResult = getCollection("processorsharing").deleteMany(Filters.and(Filters.eq("userId", sUserId), Filters.eq("processorId", sProcessorId)));

            if (oDeleteResult != null)
            {
                return  (int) oDeleteResult.getDeletedCount();
            }

        } 
        catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return 0;
    } 
}
