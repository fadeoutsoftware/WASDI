/**
 * Created by Cristiano Nattero and Alessio Corrado on 2018-11-27
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import org.apache.abdera.i18n.templates.Template;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.JsonObject;

import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.QueryResultViewModel;

/**
 * @author c.nattero
 *
 */
public class QueryExecutorONDA extends QueryExecutor {

	public QueryExecutorONDA() {
		System.out.println("QueryExecutorONDA");
		m_sProvider="ONDA";
		this.m_oQueryTranslator = new DiasQueryTranslatorONDA();
		this.m_oResponseTranslator = new DiasResponseTranslatorONDA();
	}

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.QueryExecutor#getUrlPath()
	 */
	@Override
	protected String[] getUrlPath() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.QueryExecutor#getTemplate()
	 */
	@Override
	protected Template getTemplate() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.QueryExecutor#getCountUrl(java.lang.String)
	 */
	@Override
	protected String getCountUrl(String sQuery) {
		System.out.println("QueryExecutorONDA.getCountUrl");
		if(Utils.isNullOrEmpty(sQuery)) {
			System.out.println("QueryExecutorONDA.getCountUrl: sQuery is null");
		}
		String sUrl = "https://catalogue.onda-dias.eu/dias-catalogue/Products/$count?$search=%22";
		sUrl+=m_oQueryTranslator.translateAndEncode(sQuery);
		sUrl+="%22";
		return sUrl;
	}

	//append:
	// /Products/$count?$search="name:S2*"
	@Override
	protected String buildUrl(PaginatedQuery oQuery){
		System.out.println("QueryExecutorONDA.BuildUrl");
		if(null==oQuery) {
			System.out.println("QueryExecutorONDA.buildUrl: oQuery is null");
		}
		String sUrl = buildUrlPrefix(oQuery);		

		String sMetadata = "$expand=Metadata";
		if(!Utils.isNullOrEmpty(sMetadata)) {
			sUrl += "&" + sMetadata;
		}
		sUrl = buildUrlSuffix(oQuery, sUrl);
		return sUrl;
	}

	private String buildUrlForList(PaginatedQuery oQuery) {
		System.out.println("QueryExecutorONDA.buildUrlForList");
		if(null==oQuery) {
			System.out.println("QueryExecutorONDA.buildUrlForList: oQuery is null");
		}
		String sUrl = buildUrlPrefix(oQuery);

		sUrl += "&$select=id,name,creationDate,beginPosition,offline,size,pseudopath,footprint";

		sUrl = buildUrlSuffix(oQuery, sUrl);
		return sUrl;
	}

	private String buildUrlPrefix(PaginatedQuery oQuery) {
		System.out.println("QueryExecutorONDA.BuildBaseUrl");
		if(null==oQuery) {
			throw new NullPointerException("QueryExecutorONDA.buildBaseUrl: oQuery is null");
		}
		String sUrl = "https://catalogue.onda-dias.eu/dias-catalogue/Products?$search=%22";
		sUrl+=m_oQueryTranslator.translateAndEncode(oQuery.getQuery()) + "%22";
		return sUrl;
	}

	protected String buildUrlSuffix(PaginatedQuery oQuery, String sInUrl) {
		String sUrl = sInUrl;
		sUrl+="&$top=" + oQuery.getLimit() + "&$skip="+ oQuery.getOffset();

		String sFormat = "$format=json";
		if(!Utils.isNullOrEmpty(sFormat)) {
			sUrl += "&" + sFormat;
		}

		String sOrderBy = oQuery.getSortedBy();
		//TODO do not use hardcoded values
		sOrderBy = "$orderby=creationDate";
		if(!Utils.isNullOrEmpty(sOrderBy)) {
			sUrl += "&" + sOrderBy;
		}
		return sUrl;
	}


	@Override
	public int executeCount(String sQuery) throws IOException {
		System.out.println("QueryExecutorONDA.executeCount");
		//note: the following parameters specified by WASDI are not supported by ONDA:
		//polarisation
		//relative orbit
		//Swath
		//
		//XXX is it possible to filter results accordingly using info from the query?

		//Naming conventions:
		// https://sentinel.esa.int/web/sentinel/user-guides
		//sentinel 1:
		// https://sentinel.esa.int/web/sentinel/user-guides/sentinel-1-sar/naming-conventions
		// https://sentinel.esa.int/web/sentinel/user-guides/sentinel-2-msi/naming-convention
		//sentinel 2:
		// https://sentinel.esa.int/web/sentinel/user-guides/sentinel-2-msi/naming-convention
		//sentinel 3:
		//...


		String sUrl = getCountUrl(sQuery);
		////use this to test with just 3 results
		////sUrl = "https://catalogue.onda-dias.eu/dias-catalogue/Products/$count?$search=%22(%20(%20name:S1*%20AND%20name:S1A_*%20AND%20name:*SLC*%20AND%20name:*%20AND%20sensorOperationalMode:SM%20)%20)%20AND%20(%20(%20beginPosition:[2018-12-02T00:00:00.000Z%20TO%202018-12-02T23:59:59.999Z]%20AND%20endPosition:[2018-12-02T00:00:00.000Z%20TO%202018-12-02T23:59:59.999Z]%20)%20)%22";

		int iResult = -1;
		String sResult = httpGetResults(sUrl);
		if(null!=sResult) {
			try {
				iResult = Integer.parseInt(sResult);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return iResult;


		//		URL oURL = new URL(sUrl);
		//		HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();
		//
		//
		//		// optional default is GET
		//		oConnection.setRequestMethod("GET");
		//		oConnection.setRequestProperty("Accept", "*/*");
		//
		//		//XXX add user and password
		//
		//		System.out.println("\nSending 'GET' request to URL : " + sUrl);
		//
		//		int responseCode =  oConnection.getResponseCode();
		//		System.out.println("Response Code : " + responseCode);
		//
		//		if(200 == responseCode) {
		//			BufferedReader in = new BufferedReader(new InputStreamReader(oConnection.getInputStream()));
		//			String inputLine;
		//			StringBuffer sResponse = new StringBuffer();
		//	
		//			while ((inputLine = in.readLine()) != null) {
		//				sResponse.append(inputLine);
		//			}
		//			in.close();
		//		
		//
		//			//print result
		//			System.out.println("Count Done: Response " + sResponse.toString());
		//	
		//			return Integer.parseInt(sResponse.toString());
		//		} else {
		//			String sMessage = oConnection.getResponseMessage();
		//			System.out.println(sMessage);
		//			return -1;
		//		}
	}


	@Override
	public ArrayList<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) throws IOException {
		System.out.println("QueryExecutorONDA.executeAndRetrieve(2 args)");
		String sUrl = null;
		if(bFullViewModel) {
			sUrl = buildUrl(oQuery);
		} else {
			sUrl = buildUrlForList(oQuery);
		}
		String sResult = httpGetResults(sUrl);		
		ArrayList<QueryResultViewModel> aoResult = null;
		if(!Utils.isNullOrEmpty(sResult)) {
			aoResult = buildResultViewModel(sResult, bFullViewModel);
			if(null==aoResult) {
				throw new NullPointerException("QueryExecutorONDA.executeAndRetrieve: aoResult is null"); 
			}
			if(!bFullViewModel) {
				//XXX we can probably get rid of this, but let's keep it for safety until thoroughly tested
				for (QueryResultViewModel oViewModel : aoResult) {
					oViewModel.setPreview(null);
				}
			}
		} else {
			System.out.println("QueryExecutorONDA.executeAndRetrieve: no result for the following query:");
			System.out.print(sUrl);
		}
		return aoResult;
	}


	@Override
	public ArrayList<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery_AssumeFullviewModel) throws IOException {
		System.out.println("QueryExecutorONDA.executeAndRetrieve(1 arg)");
		return executeAndRetrieve(oQuery_AssumeFullviewModel, true);

		////use this to test with just 3 results
		////sUrl = "https://catalogue.onda-dias.eu/dias-catalogue/Products?$search=%22(%20(%20name:S1*%20AND%20name:S1A_*%20AND%20name:*SLC*%20AND%20name:*%20AND%20sensorOperationalMode:SM%20)%20)%20AND%20(%20(%20beginPosition:[2018-12-02T00:00:00.000Z%20TO%202018-12-02T23:59:59.999Z]%20AND%20endPosition:[2018-12-02T00:00:00.000Z%20TO%202018-12-02T23:59:59.999Z]%20)%20)%22&$orderby=creationDate%20desc&$top=15&$skip=0&$format=json";
	}

	protected ArrayList<QueryResultViewModel> buildResultViewModel(String sJson, boolean bFullViewModel){
		System.out.println("QueryExecutor.buildResultLightViewModel");
		if(null==sJson ) {
			System.out.println("QueryExecutor.buildResultLightViewModel: passed a null string");
			return null;
		}
		try {
			JSONObject oJsonOndaResponse = new JSONObject(sJson);
			ArrayList<QueryResultViewModel> aoResult = new ArrayList<QueryResultViewModel>();
			JSONArray aoJsonArray = oJsonOndaResponse.optJSONArray("value");
			if(null!=aoJsonArray) {
				if(aoJsonArray.length()<=0) {
					System.out.println("QueryExecutorONDA.buildResultViewModel: JSON string contains an empty array");
				} else {
					for (Object oObject : aoJsonArray) {
						if(null!=oObject) {
							JSONObject oOndaFullEntry = new JSONObject("{}");
							String sEntryKey = "entry";
							JSONObject oOndaEntry = (JSONObject)(oObject);
							if(!bFullViewModel) {
								String sQuicklook = oOndaEntry.optString("quicklook");
								if(!Utils.isNullOrEmpty(sQuicklook)) {
									oOndaEntry.put("quicklook", (String)null);
								}
							}
							oOndaFullEntry.put(sEntryKey, oOndaEntry);

							//TODO remove, metadata are already downloaded
							//						String sId = oOndaEntry.optString("id");
							//						if(null!=sId) {
							//							String sBaseUrl = "https://catalogue.onda-dias.eu/dias-catalogue/Products(";
							//							sBaseUrl += sId;
							//							sBaseUrl += ")";
							//							String sFormat = "?$format=json";
							//
							//							//XXX is it possible to query metadata for all products at once, instead of performing a call each time?
							//							String sMetadataUrl = sBaseUrl + "/Metadata" + sFormat;
							//							if(m_bMustCollectMetadata && bFullViewModel) {
							//								String sMetadataJson = httpGetResults(sMetadataUrl);
							//								if(null!=sMetadataJson) {
							//									JSONObject oMetadata = new JSONObject(sMetadataJson);
							//									oOndaFullEntry.put("metadata", oMetadata);
							//								}
							//							}
							//						}
							QueryResultViewModel oRes = m_oResponseTranslator.translate(oOndaFullEntry, m_sDownloadProtocol);
							aoResult.add(oRes);
						}
					}
				}
			}
			if(aoResult.isEmpty()) {
				System.out.println("QueryExecutorONDA.buildResultViewModel: no results");
			}
			return aoResult;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private String httpGetResults(String sUrl) {
		System.out.println("QueryExecutorONDA.httpGetResults");
		String sResult = null;
		try {
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();

			// optional default is GET
			oConnection.setRequestMethod("GET");
			oConnection.setRequestProperty("Accept", "*/*");

			System.out.println("\nSending 'GET' request to URL : " + sUrl);

			long lStart = System.nanoTime();
			int responseCode =  oConnection.getResponseCode();
			System.out.println("Response Code : " + responseCode);

			if(200 == responseCode) {
				BufferedReader in = new BufferedReader(new InputStreamReader(oConnection.getInputStream()));
				String inputLine;
				StringBuffer oResponse = new StringBuffer();

				while ((inputLine = in.readLine()) != null) {
					oResponse.append(inputLine);
				}
				in.close();

				sResult = oResponse.toString();
				if(!Utils.isNullOrEmpty(sResult)) {
					System.out.println("QueryExecutorONDA.httpGetResults: Response " + sResult.substring(0, Math.min(200, oResponse.length())) + "...");
				} else {
					System.out.println("QueryExecutorONDA.httpGetResults: reponse is empty");
				}

			} else {
				System.out.println("QueryExecutorONDA.httpGetResults: ONDA did not return 200 but "+responseCode+" and the following message:");
				String sMessage = oConnection.getResponseMessage();
				System.out.println(sMessage);
			}
			long lEnd = System.nanoTime();
			long lTimeElapsed = lEnd - lStart;
			long lMillis = lTimeElapsed / 1000000;
			System.out.println("QueryExecutionONDA.httpGetResults took "+lMillis+" milliseconds");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sResult;
	}

}

