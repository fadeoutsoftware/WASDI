package it.fadeout.rest.resources;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import wasdi.shared.opensearch.OpenSearchQuery;
import wasdi.shared.opensearch.PaginatedQuery;
import wasdi.shared.opensearch.QueryExecutor;
import wasdi.shared.opensearch.QueryExecutorFactory;
import wasdi.shared.utils.AuthenticationCredentials;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.QueryResultViewModel;
import wasdi.shared.viewmodels.SearchProviderViewModel;

@Path("/search")
public class OpenSearchResource {

	private static QueryExecutorFactory s_oQueryExecutorFactory;
	private static String s_sClassName;
	private Map<String,AuthenticationCredentials> m_aoCredentials;

	static {
		s_oQueryExecutorFactory = new QueryExecutorFactory();
		s_sClassName = "OpenSearchResource";
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
	public String searchSentinel(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sQuery") String sQuery,
			@QueryParam("offset") String sOffset, @QueryParam("limit") String sLimit,
			@QueryParam("sortedby") String sSortedBy, @QueryParam("order") String sOrder) {
		
		Utils.debugLog(s_sClassName + ".SearchSentinel( Session: "+sSessionId+", Query: " + sQuery + ", Offset: " +
				sOffset + ", Limit: " + sLimit + ", SortedBy: " + sSortedBy + ", Order: " + sOrder + " )");
		if (Utils.isNullOrEmpty(sSessionId)) {
			Utils.debugLog(s_sClassName + ".SearchSentinel: session is null");
			return null;
		}
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null) {
			Utils.debugLog(s_sClassName + ".SearchSentinel: user corresponding to session is null");
			return null;
		}
		if (Utils.isNullOrEmpty(oUser.getUserId())) {
			Utils.debugLog(s_sClassName + ".SearchSentinel: user corresponding to session is not null, but it's userid is null");
			return null;
		}
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

			Utils.debugLog(s_sClassName + ".SearchSentinel, user: " + oUser.getUserId() + " execute query " + sQuery);

			// return OpenSearchQuery.ExecuteQuerySentinel(sQuery, asParams.toArray(new String[asParams.size()]));
			String sResult = null;
			try {
				sResult = OpenSearchQuery.ExecuteQuery(sQuery, asParameterMap);
				return sResult;
			} catch (NumberFormatException oE) {
				Utils.debugLog(s_sClassName + ".SearchSentinel: caught NumberFormatException: " + oE);
			}
		} catch (URISyntaxException | IOException oE) {
			Utils.debugLog(s_sClassName + ".SearchSentinel: caught Exception: " + oE);
		}
		return null;
	}

	//legacy
	@GET
	@Path("/sentinel/count")
	@Produces({ "application/xml", "application/json", "text/html" })
	public String getProductsCountSentinel(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sQuery") String sQuery) {

		Utils.debugLog(s_sClassName + ".GetProductsCountSentinel( Session: " + sSessionId + ", Query: " + sQuery + " )");
		if (Utils.isNullOrEmpty(sSessionId)) {
			return null;
		}
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null) {
			return null;
		}
		if (Utils.isNullOrEmpty(oUser.getUserId())) {
			return null;
		}
		try {
			Utils.debugLog(s_sClassName + ".GetProductsCount, user: " + oUser.getUserId() + " Query: " + sQuery);
			String sResult = null;
			try {
				sResult = OpenSearchQuery.ExecuteQueryCount(sQuery, m_oServletConfig.getInitParameter("OSUser"),
						m_oServletConfig.getInitParameter("OSPwd"), m_oServletConfig.getInitParameter("OSProvider")); 
				return sResult;
			} catch (NumberFormatException oNumberFormatException) {
				Utils.debugLog(s_sClassName + ".getProductsCountSentinel: caught NumberFormatException: " + oNumberFormatException);
				return null;
			}
		} catch (URISyntaxException | IOException oE) {
			Utils.debugLog(s_sClassName + ".getProductsCountSentinel: caught Exception: " + oE);
		}
		return null;
	}

	@GET
	@Path("/query/count")
	@Produces({ "application/xml", "application/json", "text/html" })
	public int getProductsCount(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sQuery") String sQuery,
			@QueryParam("providers") String sProviders) {

		Utils.debugLog(s_sClassName + ".getProductsCount( Session: " + sSessionId + ", Query: " + sQuery + ", Providers: " + sProviders + " )");
		try {
			if (Utils.isNullOrEmpty(sSessionId)) {
				return -1;
			}
			User oUser = Wasdi.GetUserFromSession(sSessionId);
			if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId())) {
				return -1;
			}
	
			int iCounter = 0;
			if (sProviders != null) {
				Utils.debugLog(s_sClassName + ".getProductsCount, user: " + oUser.getUserId() + ", providers: " + sProviders + ", query: " + sQuery);
				try {
					Map<String, Integer> aiQueryCountResultsPerProvider = getQueryCountResultsPerProvider(sQuery, sProviders);
					for (Integer count : aiQueryCountResultsPerProvider.values()) {
						iCounter += count;
					}
				} catch (NumberFormatException oE) {
					Utils.debugLog(s_sClassName + ".getProductsCount: " + oE);
					return -1;
				}
			}
			return iCounter;
		} catch (Exception oE) {
			Utils.debugLog(s_sClassName + ".getProductsCount: " + oE);
		}
		return -1;
	}

	private Map<String, Integer> getQueryCountResultsPerProvider(String sQuery, String sProviders) {

		Utils.debugLog(s_sClassName + ".getQueryCounters( Query: " + sQuery + ", Providers: " + sProviders + " )");
		Map<String, Integer> aiQueryCountResultsPerProvider = new HashMap<>();
		try {
			String asProviders[] = sProviders.split(",|;");
			for (String sProvider : asProviders) {
				Integer iProviderCountResults = 0;
				try {
					QueryExecutor oExecutor = getExecutor(sProvider);
					try {
						iProviderCountResults = oExecutor.executeCount(sQuery);
					} catch (NumberFormatException oNumberFormatException) {
						Utils.debugLog(s_sClassName + ".getQueryCountResultsPerProvider: caught NumberFormatException: " + oNumberFormatException);
						iProviderCountResults = -1;
					} catch (IOException oIOException) {
						Utils.debugLog(s_sClassName + ".getQueryCountResultsPerProvider: caught IOException: " + oIOException);
						iProviderCountResults = -1;
					}catch (NullPointerException oNp) {
						Utils.debugLog(s_sClassName + ".getQueryCountResultsPerProvider: caught NullPointerException: " +oNp);
						iProviderCountResults = -1;
					}
				} catch (Exception oE) {
					Utils.debugLog(s_sClassName + ".getQueryCountResultsPerProvider: " +oE);
					iProviderCountResults = -1;
				}
				aiQueryCountResultsPerProvider.put(sProvider, iProviderCountResults);
			}
			return aiQueryCountResultsPerProvider;
		} catch (Exception oE) {
			Utils.debugLog(s_sClassName + ".getQueryCountResultsPerProvider: " +oE);
		}
		return null;
	}

	@GET
	@Path("/query")
	@Produces({ "application/json", "text/html" })
	public QueryResultViewModel[] search(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("providers") String sProviders, @QueryParam("sQuery") String sQuery,
			@QueryParam("offset") String sOffset, @QueryParam("limit") String sLimit,
			@QueryParam("sortedby") String sSortedBy, @QueryParam("order") String sOrder) {
		
		Utils.debugLog(s_sClassName + ".search( Session: " + sSessionId + ", Providers: " + sProviders + ", Query: " +
				sQuery + ", Offset: " + sOffset + ", Limit: " + sLimit + ", SortedBy: " + sSortedBy + ", Order: " + sOrder + " )");
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null) {
			return null;
		}
		if (Utils.isNullOrEmpty(oUser.getUserId())) {
			return null;
		}
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
				Utils.debugLog(s_sClassName + ".Search, counting. User: " + oUser.getUserId() + ", providers: " + sProviders + ", query: " + sQuery);
				aiCounterMap = getQueryCountResultsPerProvider(sQuery, sProviders);
			} catch (NumberFormatException oNumberFormatException) {
				Utils.debugLog(s_sClassName + ".search: caught NumberFormatException: " + oNumberFormatException);
				return null;
			} catch (Exception oException) {
				Utils.debugLog(s_sClassName + ".search: caught Exception: " + oException);
				return null;
			}
			ArrayList<QueryResultViewModel> aoResults = new ArrayList<>();
			int iLimit = 25;
			try {
				iLimit = Integer.parseInt(sLimit);
			} catch (NumberFormatException oE1) {
				Utils.debugLog(s_sClassName + ".search: caught NumberFormatException: " + oE1);
				return null;
			}
			int iOffset = 0;
			try {
				iOffset = Integer.parseInt(sOffset);
			} catch (NumberFormatException oE2) {
				Utils.debugLog(s_sClassName + ".search: caught NumberFormatException: " + oE2);
				return null;
			}
			int iSkipped = 0;
			for (Entry<String, Integer> oEntry : aiCounterMap.entrySet()) {
				String sProvider = oEntry.getKey();
				int iCount = oEntry.getValue();
				if (iCount < iOffset) {
					iSkipped += iCount;
					continue;
				}
				int iCurrentLimit = iLimit - aoResults.size();
				if (iCurrentLimit <= 0) {
					break;
				}
				String sCurrentLimit = "" + iCurrentLimit;
				int iCurrentOffset = Math.max(0, iOffset - iSkipped - aoResults.size());
				String sCurrentOffset = "" + iCurrentOffset;
				Utils.debugLog(s_sClassName + ".search, executing. User: " + oUser.getUserId() + ", " +
						sProvider + ": offset=" + sCurrentOffset + ": limit=" + sCurrentLimit);
				try {
					QueryExecutor oExecutor = getExecutor(sProviders);
					try {
						PaginatedQuery oQuery = new PaginatedQuery(sQuery, sCurrentOffset, sCurrentLimit, sSortedBy, sOrder);
						List<QueryResultViewModel> aoTmp = oExecutor.executeAndRetrieve(oQuery);
						if (aoTmp != null && !aoTmp.isEmpty()) {
							aoResults.addAll(aoTmp);
							Utils.debugLog(s_sClassName + ".search: found " + aoTmp.size() + " results for " + sProvider);
						} else {
							Utils.debugLog(s_sClassName + ".search: no results found for " + sProvider);
						}
					} catch (NumberFormatException oNumberFormatException) {
						Utils.debugLog(s_sClassName + ".search: " + oNumberFormatException);
						aoResults.add(null);
					} catch (IOException oIOException) {
						Utils.debugLog(s_sClassName + ".search: " + oIOException);
						aoResults.add(null);
					}
					iSkipped += iCount;
				} catch (Exception oE) {
					Utils.debugLog(s_sClassName + ".search: " + oE);
					aoResults.add(null);
				}
			}
			return aoResults.toArray(new QueryResultViewModel[aoResults.size()]);
		}
		return null;
	}

	@GET
	@Path("/providers")
	@Produces({ "application/json", "text/html" })
	public ArrayList<SearchProviderViewModel> getSearchProviders(@HeaderParam("x-session-token") String sSessionId) {
		Utils.debugLog(s_sClassName + ".getSearchProviders( Session: " + sSessionId +" )");
		try {
			if (Utils.isNullOrEmpty(sSessionId)) {
				return null;
			}
			User oUser = Wasdi.GetUserFromSession(sSessionId);
			if (oUser == null) {
				return null;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				return null;
			}
			ArrayList<SearchProviderViewModel> aoRetProviders = new ArrayList<>();
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
		} catch (Exception oE) {
			Utils.debugLog(s_sClassName + ".getSearchProviders: " + oE);
			return null;
		}
	}

	@POST
	@Path("/query/countlist")
	@Produces({ "application/xml", "application/json", "text/html" })
	public int getListProductsCount(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sQuery") String sQuery, @QueryParam("providers") String sProviders,
			ArrayList<String> asQueries) {

		Utils.debugLog(s_sClassName + ".GetListProductsCount( Session: " + sSessionId + ", Query: " + sQuery + ", Providers: " + sProviders + ", Queries: " + asQueries + " )");
		try {
			if (Utils.isNullOrEmpty(sSessionId)) {
				Utils.debugLog(s_sClassName + ".GetListProductsCount, session is null");
				return -1;
			}
			User oUser = Wasdi.GetUserFromSession(sSessionId);
			if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId())) {
				Utils.debugLog(s_sClassName + ".GetListProductsCount, session: "+sSessionId+", corresponding user is null");
				return -1;
			}
			if(null==asQueries || asQueries.size() <= 0) {
				Utils.debugLog(s_sClassName + ".GetListProductsCount, session: "+sSessionId+" of user " + oUser.getUserId() + ", asQueries is null");
				return -1;
			}
			int iCounter = 0;
	
			for (int iQueries = 0; iQueries < asQueries.size(); iQueries++) {
				sQuery = asQueries.get(iQueries);
				try {
					if (sProviders != null) {
						Map<String, Integer> pMap = getQueryCountResultsPerProvider(sQuery, sProviders);
						for (Integer count : pMap.values()) {
							iCounter += count;
						}
					}
				} catch (NumberFormatException oE) {
					Utils.debugLog(s_sClassName + ".getListProductsCount (maybe your request was ill-formatted: " + sQuery + " ?): " + oE);
					return -1;
				}
			}
			return iCounter;
		} catch (Exception oE) {
			Utils.debugLog(s_sClassName + ".getListProductsCount (maybe your request was ill-formatted: "+ sQuery + " ?): " + oE);
		}
		return -1;
	}

	@POST
	@Path("/querylist")
	@Produces({ "application/json", "text/html" })
	public QueryResultViewModel[] searchList(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("providers") String sProviders, @QueryParam("sQuery") String sQuery,
			@QueryParam("offset") String sOffset, @QueryParam("limit") String sLimit,
			@QueryParam("sortedby") String sSortedBy, @QueryParam("order") String sOrder, ArrayList<String> asQueries) {

		Utils.debugLog(s_sClassName + ".SearchList( Session: " + sSessionId + ", Providers: " + sProviders + ", Query: " + sQuery+
				", Offset: " + sOffset + ", Limit: " + sLimit + ", Sorted: " + sSortedBy + ", Order: " + sOrder + ", Queries: " + asQueries + " )");
		try {
			User oUser = Wasdi.GetUserFromSession(sSessionId);
			if (oUser == null) {
				Utils.debugLog(s_sClassName + ".SearchList, session: "+sSessionId+", null user");
				return null;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				Utils.debugLog(s_sClassName + ".SearchList, session: "+sSessionId+", session: "+sSessionId+", null userId");
				return null;
			}
			if(Utils.isNullOrEmpty(sProviders)) {
				Utils.debugLog(s_sClassName + ".SearchList, user: "+oUser.getUserId()+", sProviders is null");
				return null;
			}
			if(null==asQueries || asQueries.size()<= 0) {
				Utils.debugLog(s_sClassName + ".SearchList, user: "+oUser.getUserId()+", asQueries = "+asQueries);
				return null;
			}
	
			Utils.debugLog(s_sClassName + ".SearchList, user:" + oUser.getUserId() + ", providers: " + sProviders + ", queries " + asQueries.size());
			ArrayList<QueryResultViewModel> aoResults = new ArrayList<QueryResultViewModel>();
			for (int iQueries = 0; iQueries < asQueries.size(); iQueries++) {
				try {
					sQuery = asQueries.get(iQueries);
					Utils.debugLog(s_sClassName + ".SearchList, user:" + oUser.getUserId() + ", count: [" + sProviders + "] Query[" + iQueries + "] = " + asQueries.get(iQueries));
					Map<String, Integer> counterMap = getQueryCountResultsPerProvider(sQuery, sProviders);
					for (Entry<String, Integer> entry : counterMap.entrySet()) {
						String sProvider = entry.getKey();
						int iTotalResultsForProviders = entry.getValue();
						int iObtainedResults = 0;
						while (iObtainedResults < iTotalResultsForProviders) {
							String sCurrentOffset = "" + iObtainedResults;
							// TODO This limit should be a Provider Parameter
							int iLimit = 100;
		
							if ((iTotalResultsForProviders - iObtainedResults) < iLimit) {
								iLimit = iTotalResultsForProviders - iObtainedResults;
							}
		
							String sCurrentLimit = "" + iLimit;
							PaginatedQuery oQuery = new PaginatedQuery(sQuery, sCurrentOffset, sCurrentLimit, sSortedBy, sOrder);
							Utils.debugLog(s_sClassName + ".SearchList, user:" + oUser.getUserId() + ", execute: [" + sProviders + "] query: " + sQuery);
							QueryExecutor oExecutor = getExecutor(sProviders);
							try {
								List<QueryResultViewModel> aoTmp = oExecutor.executeAndRetrieve(oQuery, false);
								if (aoTmp != null && !aoTmp.isEmpty()) {
									iObtainedResults += aoTmp.size();
									aoResults.addAll(aoTmp);
									Utils.debugLog(s_sClassName + ".SearchList, user:" + oUser.getUserId() +", found " + aoTmp.size() +
											" results for Query#" + iQueries +" for " + sProvider);
								} else {
									Utils.debugLog(s_sClassName + ".SearchList, user:" + oUser.getUserId() +", NO results found for " + sProvider);
								}
							} catch (Exception oE4s) {
								Utils.debugLog(s_sClassName + ".SearchList: " + oE4s);
							}
						}
					}
				} catch (NumberFormatException oE) {
					Utils.debugLog(s_sClassName + ".SearchList: (maybe your request was ill-formatted: " + sQuery + " ?). : " + oE);
					aoResults.add(null); 
				}
			}
			return aoResults.toArray(new QueryResultViewModel[aoResults.size()]);
		} catch (Exception oE) {
			Utils.debugLog(s_sClassName + ".SearchList: " + oE);
		}
		return null;
	}

	/**
	 * Get the Query Executor for a specific provider
	 * @param sProvider Provider code
	 * @return QueryExecutor of the specific provider
	 */
	private QueryExecutor getExecutor(String sProvider) {
		Utils.debugLog(s_sClassName + ".getExecutor, provider: " + sProvider);
		QueryExecutor oExecutor = null;
		try {
			if(null!=sProvider) {
				AuthenticationCredentials oCredentials = getCredentials(sProvider);
				String sDownloadProtocol = m_oServletConfig.getInitParameter(sProvider+".downloadProtocol");
				String sGetMetadata = m_oServletConfig.getInitParameter("getProductMetadata");
	
				String sParserConfigPath = m_oServletConfig.getInitParameter(sProvider+".parserConfig");
				oExecutor = s_oQueryExecutorFactory.getExecutor(
						sProvider,
						oCredentials,
						//TODO change into config method
						sDownloadProtocol, sGetMetadata,
						sParserConfigPath);
			}
		} catch (Exception oE) {
			Utils.debugLog(s_sClassName + ".getExecutor( " + sProvider + " ): " + oE);
		}
		return oExecutor;

	}

	/**
	 * Get Auth Credentials for a specific provider
	 * @param sProvider Provider Code
	 * @return AuthenticationCredentials entity
	 */
	private AuthenticationCredentials getCredentials(String sProvider) {
		Utils.debugLog(s_sClassName + ".getCredentials( Provider: " + sProvider + " )");
		AuthenticationCredentials oCredentials = null;
		try {
			oCredentials = m_aoCredentials.get(sProvider);
			if(null == oCredentials) {
				String sUser = m_oServletConfig.getInitParameter(sProvider+".OSUser");
				String sPassword = m_oServletConfig.getInitParameter(sProvider+".OSPwd");
				oCredentials = new AuthenticationCredentials(sUser, sPassword);
				m_aoCredentials.put(sProvider, oCredentials);
			}
		} catch (Exception oE) {
			Utils.debugLog(s_sClassName + ".getCredentials( " + sProvider + " ): " + oE);
		}
		return oCredentials;
	}

}
