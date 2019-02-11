/**
 * Created by Cristiano Nattero on 2019-02-07
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch;

/**
 * @author c.nattero
 *
 */
public class PaginatedQuery {
	String sQuery;
	String sOffset;
	String sLimit;
	String sSortedBy;
	String sOrder;
	
	
	public PaginatedQuery(String sQuery, String sOffset, String sLimit, String sSortedBy, String sOrder) {
		this.sQuery = sQuery;
		this.sOffset = sOffset;
		this.sLimit = sLimit;
		this.sSortedBy = sSortedBy;
		this.sOrder = sOrder;
	}
	
	public String getQuery() {
		return sQuery;
	}
	public void setQuery(String sQuery) {
		this.sQuery = sQuery;
	}
	public String getOffset() {
		return sOffset;
	}
	public void setOffset(String sOffset) {
		this.sOffset = sOffset;
	}
	public String getLimit() {
		return sLimit;
	}
	public void setLimit(String sLimit) {
		this.sLimit = sLimit;
	}
	public String getSortedBy() {
		return sSortedBy;
	}
	public void setSortedBy(String sSortedBy) {
		this.sSortedBy = sSortedBy;
	}
	public String getOrder() {
		return sOrder;
	}
	public void setOrder(String sOrder) {
		this.sOrder = sOrder;
	}
}
