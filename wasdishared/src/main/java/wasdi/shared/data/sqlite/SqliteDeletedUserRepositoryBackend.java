package wasdi.shared.data.sqlite;

import wasdi.shared.data.interfaces.IDeletedUserRepositoryBackend;

public class SqliteDeletedUserRepositoryBackend extends SqliteRepository implements IDeletedUserRepositoryBackend {

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
