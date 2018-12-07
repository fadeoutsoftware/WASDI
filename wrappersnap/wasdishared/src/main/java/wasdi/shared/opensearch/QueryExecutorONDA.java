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

		//String sUrl = "https://catalogue.onda-dias.eu/dias-catalogue/Products/$count?$search=%22S2A_MSIL1C_20160719T094032_N0204_R036_T33TYH_20160719T094201%20AND%20(%20name:S1*%20AND%20name:S1A_*%20AND%20name:*%20AND%20name:*%20AND%20name:*%20)%22";
		//String sUrl = URLEncoder.encode(getCountUrl(sQuery), m_sEnconding);
		String sUrl = getCountUrl(sQuery);
		
		
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

	
//	//the following request returns 2 results
//	//http://127.0.0.1:8080/wasdiwebserver/rest/search/query?sQuery=(%20beginPosition:%5B2018-11-30T09:53:56.000Z%20TO%202018-12-01T00:00:00.000Z%5D%20AND%20endPosition:%5B2018-11-30T00:00:00.000Z%20TO%202018-12-01T00:00:00.000Z%5D%20)%20AND%20%20%20(platformname:Sentinel-1%20AND%20relativeorbitnumber:68.8)&providers=ONDA
	//https://catalogue.onda-dias.eu/dias-catalogue/Products?$search=%22(%20beginPosition:[2018-11-30T09:53:56.000Z%20TO%202018-12-01T00:00:00.000Z]%20AND%20endPosition:[2018-11-30T00:00:00.000Z%20TO%202018-12-01T00:00:00.000Z]%20)%20AND%20(name:S1*%20AND%20relativeOrbitNumber:68)%22&$top=10&$format=atom
	@Override
	public ArrayList<QueryResultViewModel> execute(String sQuery) throws IOException {

		//String sUrl = "https://catalogue.onda-dias.eu/dias-catalogue/Products/$count?$search=%22S2A_MSIL1C_20160719T094032_N0204_R036_T33TYH_20160719T094201%20AND%20(%20name:S1*%20AND%20name:S1A_*%20AND%20name:*%20AND%20name:*%20AND%20name:*%20)%22";
		//String sUrl = URLEncoder.encode(getCountUrl(sQuery), m_sEnconding);
		String sUrl = buildUrl(sQuery);
		//TODO remove after implementation is completed
		sUrl = "https://catalogue.onda-dias.eu/dias-catalogue/Products?$search=%22(%20(%20name:S1*%20AND%20name:*%20AND%20name:*%20AND%20name:*%20AND%20name:*%20)%20)%20AND%20(%20(%20beginPosition:[2018-11-01T00:00:00.000Z%20TO%202018-11-01T23:59:59.999Z]%20AND%20endPosition:[2018-11-01T00:00:00.000Z%20TO%202018-11-01T23:59:59.999Z]%20)%20)%20AND%20footprint:%22Intersects(POLYGON((8.437671661376955%2047.29099010963179,8.437671661376955%2047.4249516177179,8.811206817626955%2047.4249516177179,8.811206817626955%2047.29099010963179,8.437671661376955%2047.29099010963179)))%22%22&$top=10&$format=json&$skip=0&$orderby=creationDate";
		
		
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
	
			return buildResultLightViewModel(oResponse.toString());
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
					aoResult.add(oRes);
				}
			}
			return aoResult;
		}
		return null;
	}
	

}

