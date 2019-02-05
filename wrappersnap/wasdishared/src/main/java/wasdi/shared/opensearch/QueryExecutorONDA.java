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

import wasdi.shared.viewmodels.QueryResultViewModel;

/**
 * @author c.nattero
 *
 */
public class QueryExecutorONDA extends QueryExecutor {

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
		String sUrl = "https://catalogue.onda-dias.eu/dias-catalogue/Products/$count?$search=%22";
		sUrl+=m_oQueryTranslator.translateAndEncode(sQuery);
		sUrl+="%22";
		return sUrl;
	}
	
	//append:
	// /Products/$count?$search="name:S2*"
	@Override
	protected String buildUrl(String sQuery){
		String sUrl = "https://catalogue.onda-dias.eu/dias-catalogue/Products?$search=%22";
		sUrl+=m_oQueryTranslator.translateAndEncode(sQuery);
		//TODO get rid of the $top=10 and introduce pagination
		sUrl+="%22&$top=" + m_sLimit + "&$skip="+ m_sOffset +"&$format=json"+ "&$orderby=creationDate";
		return sUrl;
	}
	
	@Override
	public int executeCount(String sQuery) throws IOException {
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
	public ArrayList<QueryResultViewModel> execute(String sQuery) throws IOException {


		String sUrl = buildUrl(sQuery);
		
		String sResult = httpGetResults(sUrl);
		
		ArrayList<QueryResultViewModel> aoResult = null;
		if(sResult!= null) {
			aoResult = buildResultLightViewModel(sResult);
		}
		return aoResult;

		
		////use this to test with just 3 results
		////sUrl = "https://catalogue.onda-dias.eu/dias-catalogue/Products?$search=%22(%20(%20name:S1*%20AND%20name:S1A_*%20AND%20name:*SLC*%20AND%20name:*%20AND%20sensorOperationalMode:SM%20)%20)%20AND%20(%20(%20beginPosition:[2018-12-02T00:00:00.000Z%20TO%202018-12-02T23:59:59.999Z]%20AND%20endPosition:[2018-12-02T00:00:00.000Z%20TO%202018-12-02T23:59:59.999Z]%20)%20)%22&$orderby=creationDate%20desc&$top=15&$skip=0&$format=json";
		
		
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
//			StringBuffer oResponse = new StringBuffer();
//	
//			while ((inputLine = in.readLine()) != null) {
//				oResponse.append(inputLine);
//			}
//			in.close();
//		
//
//			//print result
//			System.out.println("Count Done: Response " + oResponse.toString());
//	
//			ArrayList<QueryResultViewModel> aoResult = buildResultLightViewModel(oResponse.toString());
//			//MAYBE filter aoResult using info from the query
//			return aoResult;
//		} else {
//			String sMessage = oConnection.getResponseMessage();
//			System.out.println(sMessage);
//			return null;
//		}
	}
	
	protected ArrayList<QueryResultViewModel> buildResultLightViewModel(String sJson){
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
				for (Object oObject : aoJsonArray) {
					if(null!=oObject) {
						JSONObject oOndaFullEntry = new JSONObject("{}");
						String sEntryKey = "entry";
						JSONObject oOndaEntry = (JSONObject)(oObject);
						oOndaFullEntry.put(sEntryKey, oOndaEntry);

						String sId = oOndaEntry.optString("id");
						if(null!=sId) {
							String sBaseUrl = "https://catalogue.onda-dias.eu/dias-catalogue/Products(";
							sBaseUrl += sId;
							sBaseUrl += ")";
							String sFormat = "?$format=json";
							String sProductInfoUrl =  sBaseUrl + sFormat;
							String sMetadataUrl = sBaseUrl + "/Metadata" + sFormat;

							//not necessary
//							String sProductInfoJson = httpGetResults(sProductInfoUrl);
//							if(null!=sProductInfoJson) {
//								JSONObject oProductInfo = new JSONObject(sProductInfoJson); 
//								oOndaFullEntry.put("productInfo", oProductInfo);
//							}
							//XXX is it possible to query metadata for all products at once, instead of performing a call each time?
							if(m_bMustCollectMetadata) {
								String sMetadataJson = httpGetResults(sMetadataUrl);
								if(null!=sMetadataJson) {
									JSONObject oMetadata = new JSONObject(sMetadataJson);
									oOndaFullEntry.put("metadata", oMetadata);
								}
							}
						}
						QueryResultViewModel oRes = m_oResponseTranslator.translate(oOndaFullEntry, m_sDownloadProtocol);
						aoResult.add(oRes);
					}
				}
			}
			return aoResult;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private String httpGetResults(String sUrl) {
		String sResult = null;
		try {
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();
	
			// optional default is GET
			oConnection.setRequestMethod("GET");
			oConnection.setRequestProperty("Accept", "*/*");
	
			System.out.println("\nSending 'GET' request to URL : " + sUrl);
	
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
			
	
				//print result
				System.out.println("Count Done: Response " + oResponse.toString());
		
				sResult = oResponse.toString();
			} else {
				String sMessage = oConnection.getResponseMessage();
				System.out.println(sMessage);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sResult;
	}
	
}

