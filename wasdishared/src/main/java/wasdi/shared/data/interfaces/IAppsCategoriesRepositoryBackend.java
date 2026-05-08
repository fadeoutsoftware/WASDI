package wasdi.shared.data.interfaces;

import java.util.List;

import wasdi.shared.business.AppCategory;

/**
 * Backend contract for apps categories repository.
 */
public interface IAppsCategoriesRepositoryBackend {

	List<AppCategory> getCategories();

	AppCategory getCategoryById(String sCategoryId);

	boolean insertCategory(AppCategory oCategory);

	boolean deleteCategory(String sCategoryId);
}
