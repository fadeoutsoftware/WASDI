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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import it.fadeout.Wasdi;
import wasdi.shared.business.User;
import wasdi.shared.opensearch.AuthenticationCredentials;
import wasdi.shared.opensearch.OpenSearchQuery;
import wasdi.shared.opensearch.PaginatedQuery;
import wasdi.shared.opensearch.QueryExecutor;
import wasdi.shared.opensearch.QueryExecutorFactory;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.QueryResultViewModel;
import wasdi.shared.viewmodels.SearchProviderViewModel;

@Path("/search")
public class OpenSearchResource {
	
	private static QueryExecutorFactory s_oQueryExecutorFactory;
	private Map<String,AuthenticationCredentials> m_aoCredentials;
	
	static {
		s_oQueryExecutorFactory = new QueryExecutorFactory();
	}
	
	public OpenSearchResource() {
		m_aoCredentials = new HashMap<>();
	}

	@Context
	ServletConfig m_oServletConfig;

	//legacy
	@GET
	@Path("/sentinel/result")
	@Produces({ "application/xml", "application/json", "text/html" })
	public String SearchSentinel(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sQuery") String sQuery,
			@QueryParam("offset") String sOffset, @QueryParam("limit") String sLimit,
			@QueryParam("sortedby") String sSortedBy, @QueryParam("order") String sOrder) {
		Wasdi.DebugLog("OpenSearchResource.SearchSentinel, session: "+sSessionId);

		if (Utils.isNullOrEmpty(sSessionId))
			return null;

		User oUser = Wasdi.GetUserFromSession(sSessionId);

		if (oUser == null)
			return null;
		if (Utils.isNullOrEmpty(oUser.getUserId()))
			return null;

		try {
			HashMap<String, String> asParameterMap = new HashMap<>();
			//ArrayList<String> asParams = new ArrayList<>();
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

			Wasdi.DebugLog("OpenSearchResource.SearchSentinel, user: " + oUser.getUserId() + " execute query " + sQuery);

			// return OpenSearchQuery.ExecuteQuerySentinel(sQuery, asParams.toArray(new
			// String[asParams.size()]));
			return OpenSearchQuery.ExecuteQuery(sQuery, asParameterMap);
		} catch (URISyntaxException | IOException e) {
			e.printStackTrace();
		}

		return null;

	}

	//legacy
	@GET
	@Path("/sentinel/count")
	@Produces({ "application/xml", "application/json", "text/html" })
	public String GetProductsCountSentinel(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sQuery") String sQuery) {
		Wasdi.DebugLog("OpenSearchResource.GetProductsCountSentinel, session: " + sSessionId);

		if (Utils.isNullOrEmpty(sSessionId))
			return null;

		User oUser = Wasdi.GetUserFromSession(sSessionId);

		if (oUser == null)
			return null;
		if (Utils.isNullOrEmpty(oUser.getUserId()))
			return null;

		try {
			Wasdi.DebugLog("OpenSearchResource.GetProductsCount, user: " + oUser.getUserId() + " Query: " + sQuery);
			return OpenSearchQuery.ExecuteQueryCount(sQuery, m_oServletConfig.getInitParameter("OSUser"),
					m_oServletConfig.getInitParameter("OSPwd"), m_oServletConfig.getInitParameter("OSProvider"));
		} catch (URISyntaxException | IOException e) {
			e.printStackTrace();
		}

		return null;

	}

	@GET
	@Path("/query/count")
	@Produces({ "application/xml", "application/json", "text/html" })
	public int GetProductsCount(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sQuery") String sQuery,
			@QueryParam("providers") String sProviders) {
		Wasdi.DebugLog("OpenSearchResource.GetProductsCount, session: " + sSessionId);

		if (Utils.isNullOrEmpty(sSessionId))
			return 0;
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId()))
			return 0;

		int iCounter = 0;
		if (sProviders != null) {
			Wasdi.DebugLog("OpenSearchResource.GetProductsCount, user: " + oUser.getUserId() + ", providers: " + sProviders + ", query: " + sQuery);
			Map<String, Integer> aiQueryCountResultsPerProvider = getQueryCountResultsPerProvider(sQuery, sProviders);

			for (Integer count : aiQueryCountResultsPerProvider.values()) {
				iCounter += count;
			}
		}

		return iCounter;
	}

	private Map<String, Integer> getQueryCountResultsPerProvider(String sQuery, String sProviders) {
		Wasdi.DebugLog("OpenSearchResource.getQueryCounters");
		
		Map<String, Integer> aiQueryCountResultsPerProvider = new HashMap<String, Integer>();
		String asProviders[] = sProviders.split(",|;");
		for (String sProvider : asProviders) {
			
			QueryExecutor oExecutor = getExecutor(sProviders);

			try {
				Integer iProviderCountResults = 0;
				iProviderCountResults = oExecutor.executeCount(sQuery);
				aiQueryCountResultsPerProvider.put(sProvider, iProviderCountResults);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		return aiQueryCountResultsPerProvider;
	}

	@GET
	@Path("/query")
	@Produces({ "application/json", "text/html" })
	public QueryResultViewModel[] Search(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("providers") String sProviders, @QueryParam("sQuery") String sQuery,
			@QueryParam("offset") String sOffset, @QueryParam("limit") String sLimit,
			@QueryParam("sortedby") String sSortedBy, @QueryParam("order") String sOrder) {

		Wasdi.DebugLog("OpenSearchResource.Search, session: " + sSessionId);

		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null)
			return null;
		if (Utils.isNullOrEmpty(oUser.getUserId()))
			return null;

		if (sProviders != null) {

			if (sOffset == null)
				sOffset = "0";
			if (sLimit == null)
				sLimit = "25";
			if (sSortedBy == null)
				sSortedBy = "ingestiondate";
			if (sOrder == null)
				sOrder = "asc";

			Map<String, Integer> aiCounterMap = null;
			try {
				Wasdi.DebugLog("OpenSearchResource.Search, counting. User: " + oUser.getUserId() + ", providers: " + sProviders + ", query: " + sQuery);
				aiCounterMap = getQueryCountResultsPerProvider(sQuery, sProviders);
				// TEST
				// counterMap = new HashMap();
				// counterMap.put("SENTINEL", 10);
			} catch (Exception e) {
				e.printStackTrace();
			}

			ArrayList<QueryResultViewModel> aoResults = new ArrayList<QueryResultViewModel>();

			//XXX embed this code into a method
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

			for (Entry<String, Integer> oEntry : aiCounterMap.entrySet()) {

				String sProvider = oEntry.getKey();
				int iCount = oEntry.getValue();

				if (iCount < iOffset) {
					iSkipped += iCount;
					continue;
				}

				int iActualLimit = iLimit - aoResults.size();
				if (iActualLimit <= 0)
					break;
				String sActualLimit = "" + iActualLimit;

				int iActualOffset = Math.max(0, iOffset - iSkipped - aoResults.size());
				String sActualOffset = "" + iActualOffset;

				Wasdi.DebugLog("OpenSearchResource.Search, executing. User: " + oUser.getUserId() + ", " + sProvider + ": offset=" + sActualOffset + ": limit=" + sActualLimit);
				QueryExecutor oExecutor = getExecutor(sProviders);

				try {
					PaginatedQuery oQuery = new PaginatedQuery(sQuery, sActualOffset, sActualLimit, sSortedBy, sOrder);
					ArrayList<QueryResultViewModel> aoTmp = oExecutor.executeAndRetrieve(oQuery);
					if (aoTmp != null && !aoTmp.isEmpty()) {
						aoResults.addAll(aoTmp);
						Wasdi.DebugLog("Found " + aoTmp.size() + " results for " + sProvider);
					} else {
						Wasdi.DebugLog("No results found for " + sProvider);
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
	@Produces({ "application/json", "text/html" })
	public ArrayList<SearchProviderViewModel> GetSearchProviders(@HeaderParam("x-session-token") String sSessionId) {
		if (Utils.isNullOrEmpty(sSessionId))
			return null;
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null)
			return null;
		if (Utils.isNullOrEmpty(oUser.getUserId()))
			return null;

		ArrayList<SearchProviderViewModel> aoRetProviders = new ArrayList<>();

		Wasdi.DebugLog("OpenSearchResource.GetSearchProviders, session: " + sSessionId);

		String sProviders = m_oServletConfig.getInitParameter("SearchProviders");
		if (sProviders != null && sProviders.length() > 0) {
			String[] asProviders = sProviders.split(",|;");

			for (int iProviders = 0; iProviders < asProviders.length; iProviders++) {
				SearchProviderViewModel oSearchProvider = new SearchProviderViewModel();
				oSearchProvider.setCode(asProviders[iProviders]);
				String sDescription = m_oServletConfig.getInitParameter(asProviders[iProviders] + ".Description");
				if (Utils.isNullOrEmpty(sDescription))
					sDescription = asProviders[iProviders];
				oSearchProvider.setDescription(sDescription);
				String sLink = m_oServletConfig.getInitParameter(asProviders[iProviders] + ".Link");
				oSearchProvider.setLink(sLink);
				aoRetProviders.add(oSearchProvider);
			}
		}

		return aoRetProviders;
	}

	@POST
	@Path("/query/countlist")
	@Produces({ "application/xml", "application/json", "text/html" })
	public int GetListProductsCount(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sQuery") String sQuery, @QueryParam("providers") String sProviders,
			ArrayList<String> asQueries) {
		Wasdi.DebugLog("OpenSearchResource.GetListProductsCount, session: "+sSessionId+", providers: "+sProviders);

		if (Utils.isNullOrEmpty(sSessionId))
			return 0;
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId()))
			return 0;

		int iCounter = 0;

		for (int iQueries = 0; iQueries < asQueries.size(); iQueries++) {
			sQuery = asQueries.get(iQueries);
			if (sProviders != null) {
				Map<String, Integer> pMap = getQueryCountResultsPerProvider(sQuery, sProviders);
				for (Integer count : pMap.values()) {
					iCounter += count;
				}
			}
		}

		return iCounter;
	}

	@POST
	@Path("/querylist")
	@Produces({ "application/json", "text/html" })
	public QueryResultViewModel[] SearchList(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("providers") String sProviders, @QueryParam("sQuery") String sQuery,
			@QueryParam("offset") String sOffset, @QueryParam("limit") String sLimit,
			@QueryParam("sortedby") String sSortedBy, @QueryParam("order") String sOrder, ArrayList<String> asQueries) {

		Wasdi.DebugLog("OpenSearchResource.SearchList, session: "+sSessionId);

		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null)
			return null;
		if (Utils.isNullOrEmpty(oUser.getUserId()))
			return null;

		if (sProviders != null) {

			if (sSortedBy == null)
				sSortedBy = "ingestiondate";
			if (sOrder == null)
				sOrder = "asc";

			Wasdi.DebugLog("OpenSearchResource.SearchList, user:" + oUser.getUserId() + ", providers: " + sProviders + ", queries " + asQueries.size());

			ArrayList<QueryResultViewModel> aoResults = new ArrayList<QueryResultViewModel>();

			for (int iQueries = 0; iQueries < asQueries.size(); iQueries++) {

				sQuery = asQueries.get(iQueries);

				Wasdi.DebugLog("OpenSearchResource.SearchList, user:" + oUser.getUserId() + ", count: [" + sProviders + "] Query[" + iQueries + "] = " + asQueries.get(iQueries));

				Map<String, Integer> counterMap = getQueryCountResultsPerProvider(sQuery, sProviders);

				for (Entry<String, Integer> entry : counterMap.entrySet()) {

					String sProvider = entry.getKey();
					int iTotalResultsForProviders = entry.getValue();
					int iObtainedResults = 0;

					while (iObtainedResults < iTotalResultsForProviders) {

						String sActualOffset = "" + iObtainedResults;
						// NOTE: This limit should be a Provider Parameter
						int iLimit = 100;

						if ((iTotalResultsForProviders - iObtainedResults) < iLimit) {
							iLimit = iTotalResultsForProviders - iObtainedResults;
						}

						String sActualLimit = "" + iLimit;

						QueryExecutor oExecutor = getExecutor(sProviders);

						try {
							PaginatedQuery oQuery = new PaginatedQuery(sQuery, sActualOffset, sActualLimit, sSortedBy, sOrder);
							Wasdi.DebugLog("OpenSearchResource.SearchList, user:" + oUser.getUserId() +
									", execute: [" + sProviders + "] query: " + sQuery);
							ArrayList<QueryResultViewModel> aoTmp = oExecutor.executeAndRetrieve(oQuery, false);

							if (aoTmp != null && !aoTmp.isEmpty()) {
								iObtainedResults += aoTmp.size();
								aoResults.addAll(aoTmp);
								Wasdi.DebugLog("OpenSearchResource.SearchList, user:" + oUser.getUserId() +", found " + aoTmp.size() +
										" results for Query#" + iQueries +" for " + sProvider);
							} else {
								Wasdi.DebugLog("OpenSearchResource.SearchList, user:" + oUser.getUserId() +", NO results found for " + sProvider);
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}

				}

			}

			return aoResults.toArray(new QueryResultViewModel[aoResults.size()]);
		}

		return null;
	}
	
	private QueryExecutor getExecutor(String sProvider) {
		Wasdi.DebugLog("OpenSearchResource.getExecutor, provider: " + sProvider);
		QueryExecutor oExecutor = null;
		if(null!=sProvider) {
			AuthenticationCredentials oCredentials = getCredentials(sProvider);
			String sDownloadProtocol = m_oServletConfig.getInitParameter(sProvider+".downloadProtocol");
			String sGetMetadata = m_oServletConfig.getInitParameter("getProductMetadata");
			
			oExecutor = s_oQueryExecutorFactory.getExecutor(
					sProvider,
					oCredentials,
					//TODO change into config method
					sDownloadProtocol, sGetMetadata);
		}
		return oExecutor;
		
	}
	
	private AuthenticationCredentials getCredentials(String sProvider) {
		Wasdi.DebugLog("OpenSearchResource.getCredentials( " + sProvider + " )");
		AuthenticationCredentials oCredentials = m_aoCredentials.get(sProvider);
		if(null == oCredentials) {
			String sUser = m_oServletConfig.getInitParameter(sProvider+".OSUser");
			String sPassword = m_oServletConfig.getInitParameter(sProvider+".OSPwd");
			oCredentials = new AuthenticationCredentials(sUser, sPassword);
			m_aoCredentials.put(sProvider, oCredentials);
		}
		return oCredentials;
	}

}
