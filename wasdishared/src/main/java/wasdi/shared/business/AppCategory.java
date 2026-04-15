package wasdi.shared.business;

/**
 * Application Catogory
 * 
 * Each app can have one more catogories associated. 
 * It is just a name, a sort of tag, that can be used to filter applications
 * 
 * @author p.campanella
 *
 */
public class AppCategory {
	
	/**
	 * Category Id: guid
	 */
	private String id;
	
	/**
	 * Name of the category
	 */
	private String category;

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
