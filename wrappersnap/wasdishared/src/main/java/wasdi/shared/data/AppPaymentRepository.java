package wasdi.shared.data;


import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.UpdateResult;

import wasdi.shared.business.AppPayment;
import wasdi.shared.utils.log.WasdiLog;

public class AppPaymentRepository extends MongoRepository {

	public AppPaymentRepository() {
		m_sThisCollection = "appspayments";
	}
	
    public boolean insertAppPayment(AppPayment oAppPayment) {

        try {
            String sJSON = s_oMapper.writeValueAsString(oAppPayment);
            getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

            return true;

        } catch (Exception oEx) {
        	WasdiLog.errorLog("AppPaymentRepository.insertAppPayment: error", oEx);
        }

        return false;
    }
	
    
    public AppPayment getAppPaymentById(String sAppPaymentId) {

        try {

        	Document oWSDocument = getCollection(m_sThisCollection).find(new Document("appPaymentId", sAppPaymentId)).first();
        	String sJSON = oWSDocument.toJson();
        	
        	AppPayment oAppPayment = s_oMapper.readValue(sJSON, AppPayment.class);
        	
        	return oAppPayment;
        } catch (Exception oEx) {
        	WasdiLog.errorLog("AppPaymentRepository.getCategoryById: error", oEx);
        }

        return null;
    }
    
	
    public List<AppPayment> getAppPaymentByProcessorAndUser(String sProcessorId, String sUserId) {
    	
    	List<AppPayment> aoReturnList = new ArrayList<>();

        try {

        	FindIterable<Document> oWSDocument = getCollection(m_sThisCollection).find(Filters.and(Filters.eq("userId", sUserId), Filters.eq("processorId", sProcessorId)));        	
        	
        	fillList(aoReturnList, oWSDocument, AppPayment.class);
        	
        	return aoReturnList;
        	
        } catch (Exception oEx) {
        	WasdiLog.errorLog("AppPaymentRepository.getAppPaymentByProcessorAndUser: error", oEx);
        }

        return null;
    }
    
    public List<AppPayment> getAppPaymentByNameAndUser(String sName, String sUserId) {
    	
    	List<AppPayment> aoReturnList = new ArrayList<>();

        try {

        	FindIterable<Document> oWSDocument = getCollection(m_sThisCollection).find(Filters.and(Filters.eq("userId", sUserId), Filters.eq("name", sName)));        	
        	
        	fillList(aoReturnList, oWSDocument, AppPayment.class);
        	
        	return aoReturnList;
        	
        } catch (Exception oEx) {
        	WasdiLog.errorLog("AppPaymentRepository.getAppPaymentByNameAndUser: error", oEx);
        }

        return null;
    }
    
    
	/**
	 * Update the information about a payment.
	 * 
	 * @param oAppPayment the payment to be updated
	 * @return true if the operation succeeded, false otherwise
	 */
	public boolean updateAppPayment(AppPayment oAppPayment) {

		try {
			String sJSON = s_oMapper.writeValueAsString(oAppPayment);

			Bson oFilter = new Document("appPaymentId", oAppPayment.getAppPaymentId());
			Bson oUpdateOperationDocument = new Document("$set", new Document(Document.parse(sJSON)));

			UpdateResult oResult = getCollection(m_sThisCollection).updateOne(oFilter, oUpdateOperationDocument);

			if (oResult.getModifiedCount() == 1)
				return true;
		} catch (Exception oEx) {
			WasdiLog.errorLog("AppPaymentRepository.updateAppPayment: error ", oEx);
		}

		return false;
	}
    
}
