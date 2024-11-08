package wasdi.shared.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import wasdi.shared.business.processors.Processor;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class ProcessorRepository extends  MongoRepository {
	
	public ProcessorRepository() {
		m_sThisCollection = "processors";
	}
	
	/**
	 * Create a new processor
	 * @param oProcessor Processor Entity
	 * @return True or False in case of exception
	 */
    public boolean insertProcessor(Processor oProcessor) {

        try {
            String sJSON = s_oMapper.writeValueAsString(oProcessor);
            getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

            return true;

        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessorRepository.insertProcessor :error ", oEx);
        }

        return false;
    }
    
    /**
     * Get a processor from the WASDI Id
     * @param sProcessorId WASDI id of the processor
     * @return Entity
     */
    public Processor getProcessor(String sProcessorId) {

        try {
            Document oWSDocument = getCollection(m_sThisCollection).find(new Document("processorId", sProcessorId)).first();

            if (null != oWSDocument) {
            	String sJSON = oWSDocument.toJson();
            	return s_oMapper.readValue(sJSON,Processor.class);
            }
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessorRepository.getProcessor :error ", oEx);
        }

        return  null;
    }
    
    /**
     * Get a Processor by Name
     * @param sName Name of the processor
     * @return Processor Entity
     */
    public Processor getProcessorByName(String sName) {

    	if (Utils.isNullOrEmpty(sName)) return null;
    	
        try {
            Document oWSDocument = getCollection(m_sThisCollection).find(new Document("name", sName)).first();
            if (null != oWSDocument) {
            	String sJSON = oWSDocument.toJson();
            	return s_oMapper.readValue(sJSON,Processor.class);
            }
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessorRepository.getProcessorByName :error ", oEx);
        }

        return  null;
    }

    /**
     * Update a processor
     * @param oProcessor Entity to update
     * @return True or False in case of exception
     */
    public boolean updateProcessor(Processor oProcessor) {
        try {
            String sJSON = s_oMapper.writeValueAsString(oProcessor);
            
            Bson oFilter = new Document("processorId", oProcessor.getProcessorId());
            Bson oUpdateOperationDocument = new Document("$set", new Document(Document.parse(sJSON)));
            
            UpdateResult oResult = getCollection(m_sThisCollection).updateOne(oFilter, oUpdateOperationDocument);

            if (oResult.getModifiedCount()==1) return  true;
        }
        catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessorRepository.updateProcessor :error ", oEx);
        }

        return  false;
    }

    /**
     * Get the processor owned by sUserId
     * @param sUserId Id User of the processor's owner
     * @return List of processors of the user
     */
    public List<Processor> getProcessorByUser(String sUserId) {

        final ArrayList<Processor> aoReturnList = new ArrayList<Processor>();
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(new Document("userId", sUserId));
            
            fillList(aoReturnList, oWSDocuments, Processor.class);
            
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessorRepository.getProcessorByUser :error ", oEx);
        }

        return aoReturnList;
    }

	public List<Processor> findProcessorsByPartialName(String sPartialName) {
		List<Processor> aoReturnList = new ArrayList<>();

		if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
			return aoReturnList;
		}

		Pattern regex = Pattern.compile(Pattern.quote(sPartialName), Pattern.CASE_INSENSITIVE);

		Bson oFilterLikeProcessorId = Filters.eq("processorId", regex);
		Bson oFilterLikeName = Filters.eq("name", regex);
		Bson oFilterLikeFriendlyName = Filters.eq("friendlyName", regex);
		Bson oFilterLikeDescription = Filters.eq("description", regex);

		Bson oFilter = Filters.or(oFilterLikeProcessorId, oFilterLikeName, oFilterLikeFriendlyName, oFilterLikeDescription);

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(oFilter)
					.sort(new Document("name", 1));

			fillList(aoReturnList, oWSDocuments, Processor.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorRepository.findProcessorsByPartialName :error ", oEx);
		}

		return aoReturnList;
	}
    
    /**
     * Get the next http port available for a processor
     * @return New Port. 
     */
    public int getNextProcessorPort() {
    	
    	int iPort = -1;

        try {
        	Document oWSDocument = getCollection(m_sThisCollection).find().sort(new Document("port", -1)).first();
            String sJSON = oWSDocument.toJson();
            Processor oProcessor = s_oMapper.readValue(sJSON,Processor.class);
            iPort = oProcessor.getPort();
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessorRepository.getNextProcessorPort :error ", oEx);
        }

        if (iPort == -1) iPort = WasdiConfig.Current.dockers.processorsInternalPort;
        else iPort++;
        
        return iPort;
    }
    
    /**
     * Delete a processor from WASDI Id
     * @param sProcessorId Processor Id
     * @return True or false in case of exception
     */
    public boolean deleteProcessor(String sProcessorId) {
    	
    	if (Utils.isNullOrEmpty(sProcessorId)) return false;

        try {

            DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteOne(new Document("processorId", sProcessorId));

            if (oDeleteResult != null)
            {
                if (oDeleteResult.getDeletedCount() == 1 )
                {
                    return  true;
                }
            }

        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessorRepository.deleteProcessor :error ", oEx);
        }

        return  false;
    }
    
    /**
     * Delete all the processors of a user
     * @param sUserId Owner of the procs to delete
     * @return Number of deleted processors
     */
    public int deleteProcessorByUser(String sUserId) {
    	
    	if (Utils.isNullOrEmpty(sUserId)) return 0;

        try {

            DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteMany(new Document("userId", sUserId));

            if (oDeleteResult != null)
            {
                return  (int) oDeleteResult.getDeletedCount();
            }

        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessorRepository.deleteProcessorByUser :error ", oEx);
        }

        return 0;
    }
    
    /**
     * Get the list of all the deployed processors
     * @return List of all the processors
     */
    public List<Processor> getDeployedProcessors() {
        return getDeployedProcessors("_id");
    }
    
    public List<Processor> getDeployedProcessors(String sOrderBy) {
    	return getDeployedProcessors(sOrderBy, 1);
    }
    
    public List<Processor> getDeployedProcessors(String sOrderBy, int iDirection) {

        final ArrayList<Processor> aoReturnList = new ArrayList<Processor>();
        try {
        	
        	FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find().sort(new Document(sOrderBy, iDirection));
        	
        	fillList(aoReturnList, oWSDocuments, Processor.class);
        	
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessorRepository.getDeployedProcessors :error ", oEx);
        }

        return aoReturnList;
    }    

	public void updateProcessorDate(Processor oProcessor){
		Date oDate = new Date();
		oProcessor.setUpdateDate( (double) oDate.getTime());
		updateProcessor(oProcessor);
	}

	public long countProcessors() {
		return getCollection(m_sThisCollection).countDocuments();
	}
	
	public long countProcessors(boolean bPublicOnly) {
		return countProcessors(false, bPublicOnly);
	}
	
	public long countProcessors(boolean bInAppStoreOnly, boolean bPublicOnly) {
		try {
			BasicDBObject oQuery = new BasicDBObject();
			
			BasicDBObject oPublicOnlyQuery = null;
			if(bPublicOnly) {
				oPublicOnlyQuery = new BasicDBObject("isPublic", 1);
				
			}
			
			BasicDBObject oInStoreOnlyQuery = null;
			if(bInAppStoreOnly) {
				oInStoreOnlyQuery = new BasicDBObject("showInStore", true);
			}
			
			if(null==oPublicOnlyQuery && null==oInStoreOnlyQuery) {
				return getCollection(m_sThisCollection).countDocuments();
			}
			
			if(null!=oPublicOnlyQuery && null==oInStoreOnlyQuery) {
				return getCollection(m_sThisCollection).countDocuments(oPublicOnlyQuery);
			}
			
			if(null==oPublicOnlyQuery && null!=oInStoreOnlyQuery) {
				return getCollection(m_sThisCollection).countDocuments(oInStoreOnlyQuery);
			}
			
			if(null!=oPublicOnlyQuery && null!=oInStoreOnlyQuery) {
				List<BasicDBObject> aoAndList = new ArrayList<>();
				aoAndList.add(oPublicOnlyQuery);
				aoAndList.add(oInStoreOnlyQuery);
				oQuery.put("$and", aoAndList);
				return getCollection(m_sThisCollection).countDocuments(oQuery);
			}
	
		} catch (Exception oE) {
			WasdiLog.errorLog("ProcessorRepository.countProcessors( " + bInAppStoreOnly + ", " + bPublicOnly + " ): ", oE);
		}
		return -1l;
	}
	
}
