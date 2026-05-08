package wasdi.shared.data;

import java.util.List;

import wasdi.shared.business.AppCategory;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.IAppsCategoriesRepositoryBackend;

/**
 * AppCategory Repository
 * 
 * @author p.campanella
 *
 */
public class AppsCategoriesRepository {

    private final IAppsCategoriesRepositoryBackend m_oBackend;

	public AppsCategoriesRepository() {
        m_oBackend = createBackend();
	}

    private IAppsCategoriesRepositoryBackend createBackend() {
        // For now keep Mongo backend only. Next step will select by config.
        return DataRepositoryFactoryProvider.getFactory().createAppsCategoriesRepository();
    }

    /**
     * Get List of Process Workspaces in a Workspace
     * @param sWorkspaceId
     * @return
     */
    public List<AppCategory> getCategories() {
        return m_oBackend.getCategories();
    }
    
    public AppCategory getCategoryById(String sCategoryId) {
        return m_oBackend.getCategoryById(sCategoryId);
    }
    
    public boolean insertCategory(AppCategory oCategory) {
        return m_oBackend.insertCategory(oCategory);
    }
    
    public boolean deleteCategory(String sCategoryId) {
        return m_oBackend.deleteCategory(sCategoryId);
    }


}

