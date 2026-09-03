package wasdi.shared.data.no2;

import wasdi.shared.data.interfaces.IDeletedUserRepositoryBackend;

public class No2DeletedUserRepositoryBackend extends No2Repository implements IDeletedUserRepositoryBackend {

    @Override
    public boolean markDeleted(String sUserId) {
        return true;
    }

    @Override
    public boolean isDeleted(String sUserId) {
        return false;
    }

    @Override
    public boolean remove(String sUserId) {
        return true;
    }
}
