package wasdi.shared.viewmodels.processors;

/**
 * Application Catogory
 * Categories are on the db.
 * Each app can have one more catogories associated
 * 
 * @author p.campanella
 *
 */
public class AppCategoryViewModel {
	
	private String id;
	
	private String category;
	
	private int count = 0;

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}
	
}
