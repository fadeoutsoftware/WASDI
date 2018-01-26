package it.fadeout.rest.resources;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import it.fadeout.Wasdi;
import wasdi.shared.business.User;
import wasdi.shared.opensearch.OpenSearchQuery;
import wasdi.shared.opensearch.QueryExecutor;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.QueryResultViewModel;

@Path("/search")
public class OpenSearchResource {
	
	@Context
	ServletConfig m_oServletConfig;

	
	@GET
	@Path("/sentinel/result")
	@Produces({"application/xml", "application/json", "text/html"})
	public String SearchSentinel(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sQuery") String sQuery, @QueryParam("offset") String sOffset, @QueryParam("limit") String sLimit, @QueryParam("sortedby") String sSortedBy, @QueryParam("order") String sOrder )
	{
		Wasdi.DebugLog("OpenSearchResource.SearchSentinel");
		
		if (Utils.isNullOrEmpty(sSessionId)) return null;
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		
		if (oUser==null) return null;
		if (Utils.isNullOrEmpty(oUser.getUserId())) return null;
		
		try {
			HashMap<String, String> asParameterMap = new HashMap<>();
			ArrayList<String> asParams = new ArrayList<>();
			if (sOffset != null)
				asParameterMap.put("offset", sOffset);
			if (sLimit != null)
				asParameterMap.put("limit", sLimit);
			if (sSortedBy != null)
				asParameterMap.put("sortedby", sSortedBy);
			if (sOrder != null)
				asParameterMap.put("order", sOrder);
			
			
			asParameterMap.put("provider", m_oServletConfig.getInitParameter("OSProvider"));
			asParameterMap.put("OSUser", m_oServletConfig.getInitParameter("OSUser"));
			asParameterMap.put("OSPwd", m_oServletConfig.getInitParameter("OSPwd"));
			
			System.out.println("Search Sentinel: execute query " + sQuery);
			
			//return OpenSearchQuery.ExecuteQuerySentinel(sQuery, asParams.toArray(new String[asParams.size()]));
			return OpenSearchQuery.ExecuteQuery(sQuery, asParameterMap);
		} catch (URISyntaxException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
		
	}
	
	@GET
	@Path("/sentinel/count")
	@Produces({"application/xml", "application/json", "text/html"})
	public String GetProductsCountSentinel(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sQuery") String sQuery)
	{
		Wasdi.DebugLog("OpenSearchResource.GetProductsCountSentinel");
		
		if (Utils.isNullOrEmpty(sSessionId)) return null;
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		
		if (oUser==null) return null;
		if (Utils.isNullOrEmpty(oUser.getUserId())) return null;
		
		try {
			System.out.println("OpenSearchResource.GetProductsCount: Query: " + sQuery);
			return OpenSearchQuery.ExecuteQueryCount(sQuery, 
					m_oServletConfig.getInitParameter("OSUser"), 
					m_oServletConfig.getInitParameter("OSPwd"),
					m_oServletConfig.getInitParameter("OSProvider"));
		} catch (URISyntaxException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
		
	}
	

	@GET
	@Path("/query/count")
	@Produces({"application/xml", "application/json", "text/html"})
	public int GetProductsCount(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sQuery") String sQuery, @QueryParam("providers") String sProviders)
	{
		Wasdi.DebugLog("OpenSearchResource.GetProductsCount");
		
//		if (Utils.isNullOrEmpty(sSessionId)) return 0;		
//		User oUser = Wasdi.GetUserFromSession(sSessionId);		
//		if (oUser==null || Utils.isNullOrEmpty(oUser.getUserId())) return 0;
		
		int iCounter = 0;
		
		if (sProviders!=null) {
			Map<String, Integer> pMap = getQueryCounters(sQuery, sProviders);
			for (Integer count : pMap.values()) {
				iCounter += count;
			}
		}
		
		return iCounter;
		
	}

	private Map<String, Integer> getQueryCounters(String sQuery, String sProviders) {
		Map<String, Integer> pMap = new HashMap<String,Integer>();
		String asProviders[] = sProviders.split(",|;");
		for (String sProvider : asProviders) {
			String sUser = m_oServletConfig.getInitParameter(sProvider+".OSUser");
			String sPassword = m_oServletConfig.getInitParameter(sProvider+".OSPwd");

			QueryExecutor oExecutor = QueryExecutor.newInstance(sProvider, sUser, sPassword, null, null, null, null);
			try {
				pMap.put(sProvider, oExecutor.executeCount(sQuery));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}		
		return pMap ;
	}
	
	@GET
	@Path("/query")
	@Produces({"application/json", "text/html"})
	public QueryResultViewModel[] Search(@HeaderParam("x-session-token") String sSessionId, @QueryParam("providers") String sProviders,   
			@QueryParam("sQuery") String sQuery, @QueryParam("offset") String sOffset, @QueryParam("limit") String sLimit, 
			@QueryParam("sortedby") String sSortedBy, @QueryParam("order") String sOrder ) {
		
//		User oUser = Wasdi.GetUserFromSession(sSessionId);
//		if (oUser==null) return null;
//		if (Utils.isNullOrEmpty(oUser.getUserId())) return null;
		Wasdi.DebugLog("OpenSearchResource.Search");
		
		if (sProviders!=null) {

			if (sOffset == null) sOffset = "0";
			if (sLimit == null) sLimit = "25";
			if (sSortedBy == null) sSortedBy = "ingestiondate";
			if (sOrder == null) sOrder = "asc";
			
			Map<String, Integer> counterMap = getQueryCounters(sQuery, sProviders);			
			
			ArrayList<QueryResultViewModel> aoResults = new ArrayList<QueryResultViewModel>();
			
			int iLimit = 25;			
			try {
				iLimit = Integer.parseInt(sLimit);
			} catch (NumberFormatException e1) {
				e1.printStackTrace();
			}
			
			int iOffset = 0;
			try {
				iOffset = Integer.parseInt(sOffset);
			} catch (NumberFormatException e1) {
				e1.printStackTrace();
			}
			
			int iSkipped = 0;
			
			for (Entry<String, Integer> entry : counterMap.entrySet()) {

				String sProvider = entry.getKey();				
				int iCount = entry.getValue();
				
				if (iCount < iOffset) {
					iSkipped += iCount;
					continue;
				}
				
				int iActualLimit = iLimit-aoResults.size();				
				if (iActualLimit<=0) break;
				String sActualLimit = ""+iActualLimit;
				
				int iActualOffset = Math.max(0, iOffset-iSkipped-aoResults.size());
				String sActualOffset = ""+iActualOffset;
				
				String sUser = m_oServletConfig.getInitParameter(sProvider+".OSUser");
				String sPassword = m_oServletConfig.getInitParameter(sProvider+".OSPwd");
				
				System.out.println("Executing query for " + sProvider + ": offset=" + sActualOffset + ": limit=" + sActualLimit);
				
				QueryExecutor oExecutor = QueryExecutor.newInstance(sProvider, sUser, sPassword, sActualOffset, sActualLimit, sSortedBy, sOrder);
				try {
					ArrayList<QueryResultViewModel> aoTmp = oExecutor.execute(sQuery);
					if (aoTmp!=null && !aoTmp.isEmpty()) {
						aoResults.addAll(aoTmp);
						System.out.println("Found " + aoTmp.size() + " results for " + sProvider);
					} else {
						System.out.println("No results found for " + sProvider);
					}					
				} catch (IOException e) {
					e.printStackTrace();
				}				
				iSkipped += iCount;
				
			}
			
			return aoResults.toArray(new QueryResultViewModel[aoResults.size()]);
		}
		
		return null;
	}
	
	
	@GET
	@Path("/providers")
	@Produces({"application/json", "text/html"})
	public String[] GetSearchProviders(@HeaderParam("x-session-token") String sSessionId) {
		Wasdi.DebugLog("OpenSearchResource.GetSearchProviders");
		
//		if (Utils.isNullOrEmpty(sSessionId)) return null;
//		User oUser = Wasdi.GetUserFromSession(sSessionId);
//		if (oUser==null) return null;
//		if (Utils.isNullOrEmpty(oUser.getUserId())) return null;

		String sProviders = m_oServletConfig.getInitParameter("SearchProviders");
		if (sProviders!=null && sProviders.length()>0) {
			return sProviders.split(",|;");
		}
		
		return null;
	}
	
	
}
