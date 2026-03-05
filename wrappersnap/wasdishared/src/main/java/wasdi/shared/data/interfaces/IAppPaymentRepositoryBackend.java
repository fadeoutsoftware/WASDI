package wasdi.shared.data.interfaces;

import java.util.List;

import wasdi.shared.business.AppPayment;

/**
 * Backend contract for app payment repository.
 */
public interface IAppPaymentRepositoryBackend {

	boolean insertAppPayment(AppPayment oAppPayment);

	AppPayment getAppPaymentById(String sAppPaymentId);

	List<AppPayment> getAppPaymentByProcessorAndUser(String sProcessorId, String sUserId);

	List<AppPayment> getAppPaymentByNameAndUser(String sName, String sUserId);

	boolean updateAppPayment(AppPayment oAppPayment);
}
