package wasdi.shared.data.interfaces;

public interface IDeletedUserRepositoryBackend {

    boolean markDeleted(String sUserId);

    boolean isDeleted(String sUserId);

    boolean remove(String sUserId);
}
