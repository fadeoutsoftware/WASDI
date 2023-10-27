package wasdi.shared.data.statistics;

import org.bson.Document;

import com.mongodb.BasicDBObject;

import wasdi.shared.business.statistics.Job;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Jobs Repository.
 * Jobs are derived from Process Workspaces and used in the wasdi-stats db
 * The table is used for centralized statistics about the WASDI Operations 
 * 
 * @author p.campanella
 *
 */
public class JobsRepository extends MongoRepository{
	
	public JobsRepository() {
		m_sThisCollection = "jobs";
		m_sRepoDb = "wasdi-stats";
	}
	
	/**
	 * Get the last job saved on the db for a specific original node
	 * @param sNodeCode Node Code
	 * @return Last job inserted
	 */
	public Job getLastJobFromNode(String sNodeCode) {
        try {
        	
        	Document oDocument = getCollection(m_sThisCollection).find(new Document("nodeCode", sNodeCode)).sort(new BasicDBObject("createdTimestamp", -1)).allowDiskUse(true).first();

            if (oDocument==null) return  null;

            String sJSON = oDocument.toJson();

            Job oEntity = s_oMapper.readValue(sJSON,Job.class);

            return oEntity;
        } catch (Exception oEx) {
            WasdiLog.errorLog("JobsRepository.getLastJobFromNode: error ", oEx);
        }

        return  null;		
	}
	
	/**
	 * Inserts a Job in the db
	 * @param oJob
	 * @return
	 */
	public boolean insertJob(Job oJob) {
        try {
            String sJSON = s_oMapper.writeValueAsString(oJob);
            Document oDocument = Document.parse(sJSON);
        	
        	this.add(oDocument);
            return true;
        } catch (Exception oEx) {
        	WasdiLog.errorLog("insertJob: error ", oEx);
        }
        return false;
	}
	
	
}
