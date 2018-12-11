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

import wasdi.shared.viewmodels.QueryResultViewModel;
import wasdi.shared.viewmodels.QueryResultViewModelONDA;

/**
 * @author c.nattero
 *
 */
public class QueryExecutorONDA extends QueryExecutor {

	DiasQueryTranslator m_oQTrans;
	
	public QueryExecutorONDA() {
		m_oQTrans = new OpenSearch2OdataTranslator();
	}
	
	
	public DiasQueryTranslator getM_oQTrans() {
		return m_oQTrans;
	}

	public void setM_oQTrans(DiasQueryTranslator m_oQTrans) {
		this.m_oQTrans = m_oQTrans;
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
		String sUrl = "https://catalogue.onda-dias.eu/dias-catalogue/Products/$count?$search=%22";
		sUrl+=m_oQTrans.translateAndEncode(sQuery);
		sUrl+="%22";
		return sUrl;
	}
	
	//append:
	// /Products/$count?$search="name:S2*"
	@Override
	protected String buildUrl(String sQuery){
		String sUrl = "https://catalogue.onda-dias.eu/dias-catalogue/Products?$search=%22";
		sUrl+=m_oQTrans.translateAndEncode(sQuery);
		sUrl+="%22&$top=10&$format=json&$skip=0&$orderby=creationDate";
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
		//use this to test with just 3 results
		//sUrl = "https://catalogue.onda-dias.eu/dias-catalogue/Products/$count?$search=%22(%20(%20name:S1*%20AND%20name:S1A_*%20AND%20name:*SLC*%20AND%20name:*%20AND%20sensorOperationalMode:SM%20)%20)%20AND%20(%20(%20beginPosition:[2018-12-02T00:00:00.000Z%20TO%202018-12-02T23:59:59.999Z]%20AND%20endPosition:[2018-12-02T00:00:00.000Z%20TO%202018-12-02T23:59:59.999Z]%20)%20)%22";
		
		URL oURL = new URL(sUrl);
		HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();


		// optional default is GET
		oConnection.setRequestMethod("GET");
		oConnection.setRequestProperty("Accept", "*/*");

		//XXX add user and password

		System.out.println("\nSending 'GET' request to URL : " + sUrl);

		int responseCode =  oConnection.getResponseCode();
		System.out.println("Response Code : " + responseCode);

		if(200 == responseCode) {
			BufferedReader in = new BufferedReader(new InputStreamReader(oConnection.getInputStream()));
			String inputLine;
			StringBuffer sResponse = new StringBuffer();
	
			while ((inputLine = in.readLine()) != null) {
				sResponse.append(inputLine);
			}
			in.close();
		

			//print result
			System.out.println("Count Done: Response " + sResponse.toString());
	
			return Integer.parseInt(sResponse.toString());
		} else {
			String sMessage = oConnection.getResponseMessage();
			System.out.println(sMessage);
			return -1;
		}
	}

	
	@Override
	public ArrayList<QueryResultViewModel> execute(String sQuery) throws IOException {


		String sUrl = buildUrl(sQuery);

		//use this to test with just 3 results
		//sUrl = "https://catalogue.onda-dias.eu/dias-catalogue/Products?$search=%22(%20(%20name:S1*%20AND%20name:S1A_*%20AND%20name:*SLC*%20AND%20name:*%20AND%20sensorOperationalMode:SM%20)%20)%20AND%20(%20(%20beginPosition:[2018-12-02T00:00:00.000Z%20TO%202018-12-02T23:59:59.999Z]%20AND%20endPosition:[2018-12-02T00:00:00.000Z%20TO%202018-12-02T23:59:59.999Z]%20)%20)%22&$orderby=creationDate%20desc&$top=15&$skip=0&$format=json";
		
		
		URL oURL = new URL(sUrl);
		HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();


		// optional default is GET
		oConnection.setRequestMethod("GET");
		oConnection.setRequestProperty("Accept", "*/*");

		//XXX add user and password

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
	
			ArrayList<QueryResultViewModel> aoResult = buildResultLightViewModel(oResponse.toString());
			//MAYBE filter aoResult using info from the query
			return aoResult;
		} else {
			String sMessage = oConnection.getResponseMessage();
			System.out.println(sMessage);
			return null;
		}
	}
	
	protected ArrayList<QueryResultViewModel> buildResultLightViewModel(String sJson){
		System.out.println("QueryExecutor.buildResultLightViewModel");
		if(null==sJson ) {
			System.out.println("QueryExecutor.buildResultLightViewModel: passed a null string");
			return null;
		}
		JSONObject oJson = new JSONObject(sJson);
		if(oJson.has("value")) {
			ArrayList<QueryResultViewModel> aoResult = new ArrayList<QueryResultViewModel>();
			JSONArray oJsonArray = oJson.getJSONArray("value");
			for (Object oObject : oJsonArray) {
				if(null!=oObject) {
					JSONObject oOndaEntry = (JSONObject)(oObject);
					QueryResultViewModelONDA oRes = new QueryResultViewModelONDA();
					oRes.populate(oOndaEntry);
					oRes.buildSummary();
					aoResult.add(oRes);
				}
			}
			return aoResult;
		}
		return null;
	}
	

}

