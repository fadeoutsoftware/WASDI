package wasdi.shared.data.interfaces;

import java.util.List;

import wasdi.shared.business.CreditsPackage;

/**
 * Backend contract for credits package repository.
 */
public interface ICreditsPagackageRepositoryBackend {

	CreditsPackage getCreditPackageById(String sCreditPackageId);

	CreditsPackage getCreditPackageByNameAndUserId(String sCreditPackageName, String sUserId);

	Double getTotalCreditsByUser(String sUserId);

	List<CreditsPackage> listByUser(String sUserId, boolean bAscendingOrder);

	boolean insertCreditPackage(CreditsPackage oCreditPackage);

	boolean updateCreditPackage(CreditsPackage oCreditPackage);
}
