package wasdi.shared.viewmodels.processors;

import java.util.ArrayList;

/**
 * App filter view model
 * 
 * Contains all the filters that are available to search for an application.
 * 
 * Used by the marketplace
 * 
 * @author p.campanella
 *
 */
public class AppFilterViewModel {
	
	/**
	 * Selected Categories
	 */
	private ArrayList<String> categories = new ArrayList<String>();
	/**
	 * Selected Publishers
	 */
	private ArrayList<String> publishers = new ArrayList<String>();
	
	/**
	 * Name filter
	 */
	private String name;
	
	/**
	 * Min score filter
	 */
	private Integer score = 0;
	
	/**
	 * Min Price
	 */
	private Integer minPrice = -1;
	/**
	 * Max Price
	 */
	private Integer maxPrice = -1;
	
	/**
	 * Items per page
	 */
	private Integer itemsPerPage = 12;
	
	/**
	 * Actual Page Zero Based
	 */
	private Integer page = 0;
	
	/**
	 * Sorting Order Column
	 */
	private String orderBy;
	
	/**
	 * Sorting Direction (1 = ascending, -1 = descending)
	 */
	private int orderDirection = 1;	
	
	public int getOrderDirection() {
		return orderDirection;
	}
	public void setOrderDirection(int orderDirection) {
		this.orderDirection = orderDirection;
	}
	public ArrayList<String> getCategories() {
		return categories;
	}
	public void setCategories(ArrayList<String> categories) {
		this.categories = categories;
	}
	public ArrayList<String> getPublishers() {
		return publishers;
	}
	public void setPublishers(ArrayList<String> publishers) {
		this.publishers = publishers;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Integer getScore() {
		return score;
	}
	public void setScore(Integer score) {
		this.score = score;
	}
	public Integer getMinPrice() {
		return minPrice;
	}
	public void setMinPrice(Integer minPrice) {
		this.minPrice = minPrice;
	}
	public Integer getMaxPrice() {
		return maxPrice;
	}
	public void setMaxPrice(Integer maxPrice) {
		this.maxPrice = maxPrice;
	}
	public Integer getItemsPerPage() {
		return itemsPerPage;
	}
	public void setItemsPerPage(Integer itemsPerPage) {
		this.itemsPerPage = itemsPerPage;
	}
	public Integer getPage() {
		return page;
	}
	public void setPage(Integer page) {
		this.page = page;
	}
	public String getOrderBy() {
		return orderBy;
	}
	public void setOrderBy(String orderBy) {
		this.orderBy = orderBy;
	}

}
