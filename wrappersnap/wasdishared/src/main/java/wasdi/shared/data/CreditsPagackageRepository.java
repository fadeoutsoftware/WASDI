package wasdi.shared.data;

import org.bson.Document;

import com.mongodb.client.model.Filters;

import wasdi.shared.business.CreditPackage;
import wasdi.shared.utils.log.WasdiLog;

public class CreditsPagackageRepository extends MongoRepository {

		public CreditsPagackageRepository() {
			m_sThisCollection = "creditspackages";
		}
		
		
		/**
		 * Get a credit package by its id.
		 * @param sCreditPackageId the id of the credit package
		 * @return the credit package if found, null otherwise
		 */
		public CreditPackage getCreditPackageById(String sCreditPackageId) {
			try {
				Document oWSDocument = getCollection(m_sThisCollection)
						.find(new Document("creditPackageId", sCreditPackageId))
						.first();

				if (oWSDocument != null) {
					String sJSON = oWSDocument.toJson();

					return s_oMapper.readValue(sJSON, CreditPackage.class);
				}
			} catch (Exception oEx) {
				WasdiLog.errorLog("CreditsPagackageRepository.getCreditPackageById: error ", oEx);
			}

			return null;
		}
		
		/**
		 * Get a credit package by its name and the user id
		 * @param sCreditPackageName the name credits package
		 * @param sUserId the id of the user
		 * @return the credit package if found, null otherwise
		 */
		public CreditPackage getCreditPackageByNameAndUserId(String sCreditPackageName, String sUserId) {
			try {
				Document oWSDocument = getCollection(m_sThisCollection)
						.find(Filters.and(Filters.eq("userId", sUserId), Filters.eq("name", sCreditPackageName)))
						.first();

				if (oWSDocument != null) {
					String sJSON = oWSDocument.toJson();

					return s_oMapper.readValue(sJSON, CreditPackage.class);
				}
			} catch (Exception oEx) {
				WasdiLog.errorLog("CreditsPagackageRepository.getCreditPackageByNameAndUserId: error ", oEx);
			}

			return null;
		}
		
		/**
		 * Insert a new credit package.
		 * 
		 * @param oCreditPackage the credit package to be inserted
		 * @return true if the operation succeeded, false otherwise
		 */
		public boolean insertCreditPackage(CreditPackage oCreditPackage) {

			try {
				String sJSON = s_oMapper.writeValueAsString(oCreditPackage);
				getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

				return true;
			} catch (Exception oEx) {
				WasdiLog.errorLog("CreditsPagackageRepository.getCreditPackageByNameAndUserId: error ", oEx);
			}

			return false;
		}
		
		
}
