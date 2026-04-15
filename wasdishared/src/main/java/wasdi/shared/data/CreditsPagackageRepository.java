package wasdi.shared.data;

import java.util.List;

import wasdi.shared.business.CreditsPackage;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.ICreditsPagackageRepositoryBackend;

public class CreditsPagackageRepository {

		private final ICreditsPagackageRepositoryBackend m_oBackend;

		public CreditsPagackageRepository() {
			m_oBackend = createBackend();
		}

		private ICreditsPagackageRepositoryBackend createBackend() {
			// For now keep Mongo backend only. Next step will select by config.
			return DataRepositoryFactoryProvider.getFactory().createCreditsPagackageRepository();
		}
		
		
		/**
		 * Get a credit package by its id.
		 * @param sCreditPackageId the id of the credit package
		 * @return the credit package if found, null otherwise
		 */
		public CreditsPackage getCreditPackageById(String sCreditPackageId) {
			return m_oBackend.getCreditPackageById(sCreditPackageId);
		}
		
		/**
		 * Get a credit package by its name and the user id
		 * @param sCreditPackageName the name credits package
		 * @param sUserId the id of the user
		 * @return the credit package if found, null otherwise
		 */
		public CreditsPackage getCreditPackageByNameAndUserId(String sCreditPackageName, String sUserId) {
			return m_oBackend.getCreditPackageByNameAndUserId(sCreditPackageName, sUserId);
		}
		
		/**
		 * Get the total credits remaining for a user
		 * @param sUserId the user id 
		 * @return the total number of credits available for a user
		 */
		public Double getTotalCreditsByUser(String sUserId) {
			return m_oBackend.getTotalCreditsByUser(sUserId);
		}
		
		/**
		 * List the credits packages of a user
		 * @param sUserId the user id 
		 * @param bAscendingOrder if true the results will be returned in ascending order according to the purchase date, otherwise in descending order
		 * @return the total number of credits available for a user
		 */
		public List<CreditsPackage> listByUser(String sUserId, boolean bAscendingOrder) {
			return m_oBackend.listByUser(sUserId, bAscendingOrder);
		}
		
		/**
		 * Insert a new credit package.
		 * 
		 * @param oCreditPackage the credit package to be inserted
		 * @return true if the operation succeeded, false otherwise
		 */
		public boolean insertCreditPackage(CreditsPackage oCreditPackage) {
			return m_oBackend.insertCreditPackage(oCreditPackage);
		}
		
		/**
		 * Update an credits package.
		 * 
		 * @param oCreditPackage the credit package to be updated
		 * @return true if the operation succeeded, false otherwise
		 */
		public boolean updateCreditPackage(CreditsPackage oCreditPackage) {
			return m_oBackend.updateCreditPackage(oCreditPackage);
		}
		
}

