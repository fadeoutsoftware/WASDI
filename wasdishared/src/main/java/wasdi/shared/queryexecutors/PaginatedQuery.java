/**
 * Created by Cristiano Nattero on 2019-02-07
 * 
 * Fadeout software
 *
 */
package wasdi.shared.queryexecutors;

/**
 * Paginated Query representation.
 * 
 * The Wasdi client paginates by default all the query for performance reasons.
 * 
 * This class represents one of this query containing the query text and the info
 * about pagination.
 * 
 * @author c.nattero
 *
 */
public class PaginatedQuery {
	
	/**
	 * WASDI Client Query Text
	 */
	String m_sQuery;
	/**
	 * Offset, starting result to get
	 */
	String m_sOffset;
	/**
	 * Max number of results to get. Can increase page after page
	 */
	String m_sLimit;
	/**
	 * Sorting field, if any
	 */
	String m_sSortedBy;
	/**
	 * Order filed, if any
	 */
	String m_sOrder;
	/**
	 * Original limit set (max elements per page)
	 */
	String m_sOriginalLimit;
	
	public PaginatedQuery(String sQuery, String sOffset, String sLimit, String sSortedBy, String sOrder) {
		this.internalInit(sQuery, sOffset, sLimit, sSortedBy, sOrder, sLimit);
	}
	
	public PaginatedQuery(String sQuery, String sOffset, String sLimit, String sSortedBy, String sOrder, String sOriginalLimit) {
		this.internalInit(sQuery, sOffset, sLimit, sSortedBy, sOrder, sOriginalLimit);
	}
	
	private void internalInit(String sQuery, String sOffset, String sLimit, String sSortedBy, String sOrder, String sOriginalLimit) {
		if (sSortedBy == null) {
			sSortedBy = "ingestiondate";
		}
		if (sOrder == null) {
			sOrder = "asc";
		}
		
		if (sOffset == null) sOffset = "0";
		if (sLimit == null) sLimit = "25";		
		
		int iLimit = 25;
		
		try {
			iLimit = Integer.parseInt(sLimit);
		} 
		catch (NumberFormatException oE1) {
		}
		
		if (iLimit < 0) {
			// Not possible: back to default:
			iLimit = 25;
		}
		
		int iOffset = 0;
		
		try {
			iOffset = Integer.parseInt(sOffset);
		} 
		catch (NumberFormatException oE2) {
		}
		
		
		this.m_sQuery = sQuery;
		this.m_sOffset = ""+iOffset;
		this.m_sLimit = ""+iLimit;
		this.m_sSortedBy = sSortedBy;
		this.m_sOrder = sOrder;
		this.m_sOriginalLimit = sOriginalLimit;
	}
	
	public String getQuery() {
		return m_sQuery;
	}
	public void setQuery(String sQuery) {
		this.m_sQuery = sQuery;
	}
	public String getOffset() {
		return m_sOffset;
	}
	public void setOffset(String sOffset) {
		this.m_sOffset = sOffset;
	}
	public String getLimit() {
		return m_sLimit;
	}
	public void setLimit(String sLimit) {
		this.m_sLimit = sLimit;
	}
	public String getSortedBy() {
		return m_sSortedBy;
	}
	public void setSortedBy(String sSortedBy) {
		this.m_sSortedBy = sSortedBy;
	}
	public String getOrder() {
		return m_sOrder;
	}
	public void setOrder(String sOrder) {
		this.m_sOrder = sOrder;
	}
	
	public String getOriginalLimit() {
		return m_sOriginalLimit;
	}

	public void setOriginalLimit(String sOriginalLimit) {
		this.m_sOriginalLimit = sOriginalLimit;
	}

}
