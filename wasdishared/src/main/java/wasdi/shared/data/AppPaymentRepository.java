package wasdi.shared.data;

import java.util.List;

import wasdi.shared.business.AppPayment;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.IAppPaymentRepositoryBackend;

public class AppPaymentRepository {

	private final IAppPaymentRepositoryBackend m_oBackend;

	public AppPaymentRepository() {
		m_oBackend = createBackend();
	}

	private IAppPaymentRepositoryBackend createBackend() {
		// For now keep Mongo backend only. Next step will select by config.
		return DataRepositoryFactoryProvider.getFactory().createAppPaymentRepository();
	}
	
    public boolean insertAppPayment(AppPayment oAppPayment) {
		return m_oBackend.insertAppPayment(oAppPayment);
    }
	
    
    public AppPayment getAppPaymentById(String sAppPaymentId) {
		return m_oBackend.getAppPaymentById(sAppPaymentId);
    }
    
	
    public List<AppPayment> getAppPaymentByProcessorAndUser(String sProcessorId, String sUserId) {
		return m_oBackend.getAppPaymentByProcessorAndUser(sProcessorId, sUserId);
    }
    
    public List<AppPayment> getAppPaymentByNameAndUser(String sName, String sUserId) {
		return m_oBackend.getAppPaymentByNameAndUser(sName, sUserId);
    }
    
    
	/**
	 * Update the information about a payment.
	 * 
	 * @param oAppPayment the payment to be updated
	 * @return true if the operation succeeded, false otherwise
	 */
	public boolean updateAppPayment(AppPayment oAppPayment) {
		return m_oBackend.updateAppPayment(oAppPayment);
	}
    
}

