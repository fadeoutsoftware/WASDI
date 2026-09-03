package wasdi.shared.data;

import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.IDeletedUserRepositoryBackend;

public class DeletedUserRepository {
    private final IDeletedUserRepositoryBackend m_oBackend;

    public DeletedUserRepository() {
        m_oBackend = DataRepositoryFactoryProvider.getFactory().createDeletedUserRepository();
    }

    public boolean markDeleted(String sUserId) {
        return m_oBackend.markDeleted(sUserId);
    }

    public boolean isDeleted(String sUserId) {
        return m_oBackend.isDeleted(sUserId);
    }

    public boolean remove(String sUserId) {
        return m_oBackend.remove(sUserId);
    }
}
