package it.fadeout.rest.resources;

import java.io.IOException;
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
	

	@GET
	@Path("/query/count")
	@Produces({ "application/xml", "application/json", "text/html" })
	public int getProductsCount(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sQuery") String sQuery,
			@QueryParam("providers") String sProviders) {

		Utils.debugLog(s_sClassName + ".getProductsCount( Query: " + sQuery + ", Providers: " + sProviders + " )");
		try {
			if (Utils.isNullOrEmpty(sSessionId)) {
				return -1;
			}
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId())) {
				Utils.debugLog(s_sClassName + ".getProductsCount: invalid session");
				return -1;
			}
	
			int iCounter = 0;
			if (sProviders != null) {
				Utils.debugLog(s_sClassName + ".getProductsCount, user: " + oUser.getUserId() + ", providers: " + sProviders + ", query: " + sQuery);
				try {
					Map<String, Integer> aiQueryCountResultsPerProvider = getQueryCountResultsPerProvider(sQuery, sProviders);
					
					if (aiQueryCountResultsPerProvider != null) {
						for (Integer count : aiQueryCountResultsPerProvider.values()) {
							iCounter += count;
						}						
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
					
					if (oExecutor == null) {
						Utils.debugLog(s_sClassName + ".getQueryCountResultsPerProvider: Query Executor = null ");
						aiQueryCountResultsPerProvider.put(sProvider, -1);
						continue;
					}
					
					try {
						iProviderCountResults = oExecutor.executeCount(sQuery);
					} 
					catch (NumberFormatException oNumberFormatException) {
						Utils.debugLog(s_sClassName + ".getQueryCountResultsPerProvider: caught NumberFormatException: " + oNumberFormatException);
						iProviderCountResults = -1;
					} 
					catch (IOException oIOException) {
						Utils.debugLog(s_sClassName + ".getQueryCountResultsPerProvider: caught IOException: " + oIOException);
						iProviderCountResults = -1;
					}
					catch (NullPointerException oNp) {
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
		return aiQueryCountResultsPerProvider;
	}

	@GET
	@Path("/query")
	@Produces({ "application/json", "text/html" })
	public QueryResultViewModel[] search(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("providers") String sProviders, @QueryParam("sQuery") String sQuery,
			@QueryParam("offset") String sOffset, @QueryParam("limit") String sLimit,
			@QueryParam("sortedby") String sSortedBy, @QueryParam("order") String sOrder) {
		
		Utils.debugLog(s_sClassName + ".search( Providers: " + sProviders + ", Query: " +
				sQuery + ", Offset: " + sOffset + ", Limit: " + sLimit + ", SortedBy: " + sSortedBy + ", Order: " + sOrder + " )");
		
		// Domain Check
		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null) {
			Utils.debugLog(s_sClassName + ".search: invalid session");
			return null;
		}
		if (Utils.isNullOrEmpty(oUser.getUserId())) {
			return null;
		}
		
		// If we have providers to query
		if (sProviders != null) {
			
			// Control and check input parameters for pagination
			
			if (sOffset == null) {
				sOffset = "0";
			}
				
			if (sLimit == null) {
				sLimit = "25";
			}
				
			if (sSortedBy == null) {
				sSortedBy = "ingestiondate";
			}
				
			if (sOrder == null) {
				sOrder = "asc";
			}
			
			// Get the number of elements per page
			ArrayList<QueryResultViewModel> aoResults = new ArrayList<>();
			int iLimit = 25;
			
			try {
				iLimit = Integer.parseInt(sLimit);
			} 
			catch (NumberFormatException oE1) {
				Utils.debugLog(s_sClassName + ".search: caught NumberFormatException: " + oE1);
				return null;
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
				Utils.debugLog(s_sClassName + ".search: caught NumberFormatException: " + oE2);
				return null;
			}
			
			// Query the result count for each provider
			Map<String, Integer> aiCounterMap = new HashMap<>();
			

			try {
				String asProviders[] = sProviders.split(",|;");
				for (String sProvider : asProviders) {
					Integer iProviderCountResults = iLimit;
					aiCounterMap.put(sProvider, iProviderCountResults);
				}
			} catch (Exception oE) {
				Utils.debugLog(s_sClassName + ".getQueryCountResultsPerProvider: " +oE);
			}
			
			// For each provider
			for (Entry<String, Integer> oEntry : aiCounterMap.entrySet()) {
				
				// Get the provider and the total count of its results
				String sProvider = oEntry.getKey();
				
				String sCurrentLimit = "" + iLimit;
				
				int iCurrentOffset = Math.max(0, iOffset);
				String sCurrentOffset = "" + iCurrentOffset;
				
				
				Utils.debugLog(s_sClassName + ".search, executing. User: " + oUser.getUserId() + ", " +sProvider + ": offset=" + sCurrentOffset + ": limit=" + sCurrentLimit);
				
				try {
					// Get the query executor
					QueryExecutor oExecutor = getExecutor(sProvider);
					
					if (oExecutor == null) {
						Utils.debugLog(s_sClassName + ".search: executor null for Provider: " + sProvider);
						aoResults.add(null);
						continue;
					}
					
					try {
						// Create the paginated query
						PaginatedQuery oQuery = new PaginatedQuery(sQuery, sCurrentOffset, sCurrentLimit, sSortedBy, sOrder);
						// Execute the query
						List<QueryResultViewModel> aoTmp = oExecutor.executeAndRetrieve(oQuery);
						
						// Do we have results?
						if (aoTmp != null && !aoTmp.isEmpty()) {
							// Yes perfect add all
							aoResults.addAll(aoTmp);
							Utils.debugLog(s_sClassName + ".search: found " + aoTmp.size() + " results for " + sProvider);
						} 
						else {
							// Nothing to add
							Utils.debugLog(s_sClassName + ".search: no results found for " + sProvider);
						}
					} 
					catch (NumberFormatException oNumberFormatException) {
						Utils.debugLog(s_sClassName + ".search: " + oNumberFormatException);
						aoResults.add(null);
					} 
					catch (IOException oIOException) {
						Utils.debugLog(s_sClassName + ".search: " + oIOException);
						aoResults.add(null);
					}
					
				}
				catch (Exception oE) {
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
		Utils.debugLog(s_sClassName + ".getSearchProviders");
		try {
			if (Utils.isNullOrEmpty(sSessionId)) {
				return null;
			}
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if (oUser == null) {
				Utils.debugLog(s_sClassName + ".getSearchProviders: invalid session");
				return null;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				return null;
			}

			// TODO use the ProviderCatalog and map the business objects to the expected view models

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

		Utils.debugLog(s_sClassName + ".GetListProductsCount( Query: " + sQuery + ", Providers: " + sProviders + ", Queries: " + asQueries + " )");
		try {
			if (Utils.isNullOrEmpty(sSessionId)) {
				Utils.debugLog(s_sClassName + ".GetListProductsCount, session is null");
				return -1;
			}
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId())) {
				Utils.debugLog(s_sClassName + ".GetListProductsCount, session: invalid");
				return -1;
			}
			if(null==asQueries || asQueries.size() <= 0) {
				Utils.debugLog(s_sClassName + ".GetListProductsCount, asQueries is null");
				return -1;
			}
			int iCounter = 0;
	
			for (int iQueries = 0; iQueries < asQueries.size(); iQueries++) {
				sQuery = asQueries.get(iQueries);
				try {
					if (sProviders != null) {
						Map<String, Integer> aoMap = getQueryCountResultsPerProvider(sQuery, sProviders);
						
						if (aoMap != null) {
							for (Integer iCount : aoMap.values()) {
								iCounter += iCount;
							}							
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

		Utils.debugLog(s_sClassName + ".SearchList( Providers: " + sProviders + ", Query: " + sQuery+
				", Offset: " + sOffset + ", Limit: " + sLimit + ", Sorted: " + sSortedBy + ", Order: " + sOrder + ", Queries: " + asQueries + " )");
		try { 
			
			// Validate the User
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if (oUser == null) {
				Utils.debugLog(s_sClassName + ".SearchList, session is invalid");
				return null;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				Utils.debugLog(s_sClassName + ".SearchList, null userId");
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
	
			// Prepare the output list
			ArrayList<QueryResultViewModel> aoResults = new ArrayList<QueryResultViewModel>();
			
			// For Each Input query
			for (int iQueries = 0; iQueries < asQueries.size(); iQueries++) {
				try {
					sQuery = asQueries.get(iQueries);
					
					// Get for each provider the total count
					Map<String, Integer> aoCounterMap = getQueryCountResultsPerProvider(sQuery, sProviders);
					
					if (aoCounterMap == null) {
						Utils.debugLog(s_sClassName + ".SearchList aoCounterMap null ");
						aoResults.add(null);
						continue;
					}
					
					// For each provider
					for (Entry<String, Integer> entry : aoCounterMap.entrySet()) {
						
						// Get the Provider Name
						String sProvider = entry.getKey();
						// Get the Provider Total Count
						int iTotalResultsForProviders = entry.getValue();
						
						Utils.debugLog(sProvider + " Images Found " + iTotalResultsForProviders);
						
						// Get the real results, paginated
						int iObtainedResults = 0;
						
						// Page size
						int iLimit = 100;
						
						String sProviderLimit = m_oServletConfig.getInitParameter(sProvider+".SearchListPageSize");
						
						if (!Utils.isNullOrEmpty(sProviderLimit)) {
							try {
								iLimit = Integer.parseInt(sProviderLimit);
								Utils.debugLog(sProvider + " using " + sProviderLimit + " Page Size ");
							}
							catch (Exception e) {
							}
						}
						
						// Check the value, never known...
						if (iLimit<=0) iLimit = 100;
						
						QueryExecutor oExecutor = getExecutor(sProvider);
						
						if (oExecutor == null) {
							Utils.debugLog(s_sClassName + ".SearchList: Executor Null for Provider: " + sProvider);
							continue;
						}
						
						
						float fMaxPages = iTotalResultsForProviders / (float)iLimit;
						int iMaxPages = (int) Math.ceil(fMaxPages);
						iMaxPages *= 2;
						Utils.debugLog(s_sClassName + ".SearchList: Augmentented Max Pages: " + iMaxPages + " Total Results: " + iTotalResultsForProviders + " Limit " + iLimit);
						
						
						int iActualPage = 0;
						
						// Until we do not get all the results
						while (iObtainedResults < iTotalResultsForProviders) {
							
							if (iActualPage>iMaxPages) {
								Utils.debugLog(s_sClassName + ".SearchList: cycle running out of control, actual page " + iActualPage + " , Max " + iMaxPages + " break");
								break;
							}
							
							// Actual Offset
							String sCurrentOffset = "" + iObtainedResults;
							
							String sOriginalLimit = "" + iLimit;
	
							// How many elements do we need yet?
							if ((iTotalResultsForProviders - iObtainedResults) < iLimit) {
								iLimit = iTotalResultsForProviders - iObtainedResults;
							}
		
							String sCurrentLimit = "" + iLimit;
							
							// Create the paginated Query
							PaginatedQuery oQuery = new PaginatedQuery(sQuery, sCurrentOffset, sCurrentLimit, sSortedBy, sOrder, sOriginalLimit);
							// Log the query
							Utils.debugLog(s_sClassName + ".SearchList, user:" + oUser.getUserId() + ", execute: [" + sProviders + "] query: " + sQuery);
							
							try {
								// Execute the query
								List<QueryResultViewModel> aoTmp = oExecutor.executeAndRetrieve(oQuery, false);
								
								// Did we got a result?
								if (aoTmp != null && !aoTmp.isEmpty()) {
									
									// Sum the grand total
									iObtainedResults += aoTmp.size();
									
									// Add the result to the output list
									//aoResults.addAll(aoTmp);
									
									int iAddedResults = 0;
									
									// Here add the results checking to avoid duplicates
									for (QueryResultViewModel oTempResult : aoTmp) {
										if (!aoResults.contains(oTempResult)) {
											aoResults.add(oTempResult);
											iAddedResults++;
										}
										else {
											Utils.debugLog(s_sClassName + ".SearchList: found duplicate image " + oTempResult.getTitle());
										}
									}
									
									Utils.debugLog(s_sClassName + ".SearchList added " + iAddedResults + " results for Query#" + iQueries +" for " + sProvider);
								} else {
									Utils.debugLog(s_sClassName + ".SearchList, NO results found for " + sProvider);
								}
							} catch (Exception oEx) {
								Utils.debugLog(s_sClassName + ".SearchList: " + oEx);
							}
							
							iActualPage ++;
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
				String sAppConfigPath = m_oServletConfig.getInitParameter("MissionsConfigFilePath");
				oExecutor = s_oQueryExecutorFactory.getExecutor(
						sProvider,
						oCredentials,
						//TODO change into config method
						sDownloadProtocol, sGetMetadata,
						sParserConfigPath, sAppConfigPath);
				
				oExecutor.init();
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
		//Utils.debugLog(s_sClassName + ".getCredentials( Provider: " + sProvider + " )");
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
