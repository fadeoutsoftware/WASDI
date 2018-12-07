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
import wasdi.shared.viewmodels.QueryResultViewModel;

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
		sUrl+="%22&$top=10&$format=atom&$skip=0&$orderby=creationDate";
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
			StringBuffer response = new StringBuffer();
	
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
		

			//print result
			System.out.println("Count Done: Response " + response.toString());
	
			return Integer.parseInt(response.toString());
		} else {
			return -1;
		}
	}

	
//	//the following request returns 2 results
//	//http://127.0.0.1:8080/wasdiwebserver/rest/search/query?sQuery=(%20beginPosition:%5B2018-11-30T09:53:56.000Z%20TO%202018-12-01T00:00:00.000Z%5D%20AND%20endPosition:%5B2018-11-30T00:00:00.000Z%20TO%202018-12-01T00:00:00.000Z%5D%20)%20AND%20%20%20(platformname:Sentinel-1%20AND%20relativeorbitnumber:68.8)&providers=ONDA
	//https://catalogue.onda-dias.eu/dias-catalogue/Products?$search=%22(%20beginPosition:[2018-11-30T09:53:56.000Z%20TO%202018-12-01T00:00:00.000Z]%20AND%20endPosition:[2018-11-30T00:00:00.000Z%20TO%202018-12-01T00:00:00.000Z]%20)%20AND%20(name:S1*%20AND%20relativeOrbitNumber:68)%22&$top=10&$format=atom
//	@Override
//	public ArrayList<QueryResultViewModel> execute(String sQuery, boolean bFullViewModel) throws IOException {
//		String sUrl = buildUrl(sQuery);
//		
//		
//		URL oURL = new URL(sUrl);
//		HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();
//
//		// optional default is GET
//		oConnection.setRequestMethod("GET");
//
//		//oConnection.setRequestProperty("Accept", "application/json, text/plain, */*");
//		oConnection.setRequestProperty("Accept", "*/*");
//		//oConnection.setRequestProperty("Content-type", "application/json, text/plain");
//
//		//XXX add user and password
//
//		System.out.println("\nSending 'GET' request to URL : " + sUrl);
//		
//		
//		int responseCode =  oConnection.getResponseCode();
//		System.out.println("Response Code : " + responseCode);
//
//		if(200 == responseCode) {
//			BufferedReader in = new BufferedReader(new InputStreamReader(oConnection.getInputStream()));
//			String inputLine;
//			StringBuffer response = new StringBuffer();
//	
//			while ((inputLine = in.readLine()) != null) {
//				response.append(inputLine);
//			}
//			in.close();
//		
//
//			//print result
//			System.out.println("Count Done: Response " + response.toString());
//	
//			// Get The Result as a string
////			BufferedReader oBuffRead = new BufferedReader(response.getReader());
////			String sResponseLine = null;
////			StringBuilder oResponseStringBuilder = new StringBuilder();
////			while ((sResponseLine = oBuffRead.readLine()) != null) {
////			    oResponseStringBuilder.append(sResponseLine);
////			}
//			
//			//TODO parse
//			//TODO return something meaningful
//			return null;
//		} else {
//			BufferedReader in = new BufferedReader(new InputStreamReader(oConnection.getErrorStream()));
//			String inputLine;
//			StringBuffer response = new StringBuffer();
//	
//			while ((inputLine = in.readLine()) != null) {
//				response.append(inputLine);
//			}
//			in.close();
//			return null;
//		}
//		
//		/*
//		//create abdera client
//		Abdera oAbdera = new Abdera();
//		AbderaClient oClient = new AbderaClient(oAbdera);
//		oClient.setConnectionTimeout(15000);
//		oClient.setSocketTimeout(40000);
//		oClient.setConnectionManagerTimeout(20000);
//		oClient.setMaxConnectionsTotal(200);
//		oClient.setMaxConnectionsPerHost(50);
//		
//		// get default request option
//		RequestOptions oOptions = oClient.getDefaultRequestOptions();
//		
//		// build the parser
//		Parser oParser = oAbdera.getParser();
//		ParserOptions oParserOptions = oParser.getDefaultParserOptions();
//		oParserOptions.setCharset("UTF-8");
//		//options.setCompressionCodecs(CompressionCodec.GZIP);
//		oParserOptions.setFilterRestrictedCharacterReplacement('_');
//		oParserOptions.setFilterRestrictedCharacters(true);
//		oParserOptions.setMustPreserveWhitespace(false);
//		oParserOptions.setParseFilter(null);
//		// set authorization
//		if (m_sUser!=null && m_sPassword!=null) {
//			String sUserCredentials = m_sUser + ":" + m_sPassword;
//			String sBasicAuth = "Basic " + Base64.getEncoder().encodeToString(sUserCredentials.getBytes());
//			oOptions.setAuthorization(sBasicAuth);			
//		}
//		
////		System.out.println("\nSending 'GET' request to URL : " + sUrl);
//		ClientResponse response = oClient.get(sUrl, oOptions);
//		
//		Document<Feed> oDocument = null;
//		
//		
//		if (response.getType() != ResponseType.SUCCESS) {
//			System.out.println("Response ERROR: " + response.getType());
//			return null;
//		}
//
//		System.out.println("Response Success");		
//		
//		// Get The Result as a string
//		BufferedReader oBuffRead = new BufferedReader(response.getReader());
//		String sResponseLine = null;
//		StringBuilder oResponseStringBuilder = new StringBuilder();
//		while ((sResponseLine = oBuffRead.readLine()) != null) {
//		    oResponseStringBuilder.append(sResponseLine);
//		}
//		
//		String sResultAsString = oResponseStringBuilder.toString();
//		
////		System.out.println(sResultAsString);
//
//		oDocument = oParser.parse(new StringReader(sResultAsString), oParserOptions);
//
//		if (oDocument == null) {
//			System.out.println("OpenSearchQuery.ExecuteQuery: Document response null");
//			return null;
//		}
//		
//		if (bFullViewModel) return buildResultViewModel(oDocument, oClient, oOptions);
//		else return buildResultLightViewModel(oDocument, oClient, oOptions);
//		*/
//	}


}

