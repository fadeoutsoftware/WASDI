/**
 * Created by Cristiano Nattero on 2019-01-25
 * 
 * Fadeout software
 *
 */
package httpHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.net.ssl.SSLContext;

import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

/**
 * @author c.nattero
 *
 */
public class WasdiWpsHttpHelper {

	protected HttpClientBuilder m_oBuilder;
	protected RequestConfig m_oGlobalConfig;
	protected CookieStore m_oCookieStore;
	protected CloseableHttpClient m_oHttpClient;
	protected HttpHost m_oProxy;
	protected SSLContext m_oSslContext;
	protected HttpClientContext m_oContext; 
	
	protected HttpGet m_oHttpGet;
	protected HttpPost m_oHttpPost;
	
	protected CloseableHttpResponse m_oLastResponse;
	protected int m_iLastStatus;
	protected String m_sLastBody;
	protected InputStream m_oLastStream;
	
	protected Map<String,Object> m_aoAuthParams;


	/**
	 * @throws KeyStoreException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException 
	 * 
	 */
	public WasdiWpsHttpHelper() {
		this(null,-1);
	}

	public WasdiWpsHttpHelper(String sProxy, int iPort ) {		
		m_oBuilder = HttpClients.custom();
		if(sProxy!=null && 0<= iPort ) {
			m_oProxy = new HttpHost(sProxy, iPort);
			m_oBuilder.setProxy(m_oProxy);
		}
		m_oBuilder = addSslPropertiesToBuilder(m_oBuilder);		
		m_oGlobalConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.DEFAULT).build();
		m_oCookieStore = new BasicCookieStore();
		m_oBuilder = m_oBuilder					
				.setDefaultRequestConfig(m_oGlobalConfig)
				.setDefaultCookieStore(m_oCookieStore);
		m_oHttpClient = m_oBuilder.build();
		m_oContext = HttpClientContext.create();
		m_oContext.setCookieStore(m_oCookieStore);
	}
	
	public void authenticate() {
		//do nothing, use only in derived classes
	}

	//maybe implement a "direct version", which does not cache the result?
	public CloseableHttpResponse httpGet( String sInUrl, Map<String,String> asHeaders ) {
		//TODO safety checks
		if(null==sInUrl) {
			throw new NullPointerException("WasdiWpsHttpHelper.httpGet: URL is null, cannot GET");
		}
		try {
			// TODO: if needed create virtual getAuth method
			//Authenticate();
			String sUrl = encode(sInUrl);
			m_oHttpGet = new HttpGet(sUrl);
			
			Set<String> asKeyset = asHeaders.keySet();
			//TODO safety checks
			for(String sKey: asKeyset) {
				String sValue = asHeaders.get(sKey);
				m_oHttpGet.addHeader(sKey, sValue);
			}
			
			m_oLastResponse = m_oHttpClient.execute(m_oHttpGet,m_oContext);
		} catch (IOException e) {
			e.printStackTrace();
		}
		saveStatusAndBody();
		return m_oLastResponse;
	}
	
	//maybe implement a "direct version", which does not cache the result?
	public CloseableHttpResponse httpGet( String sInUrl ) {
		if(null==sInUrl) {
			throw new NullPointerException("WasdiWpsHttpHelper.httpGet: URL is null, cannot GET");
		}
		try {
			// TODO: if needed create virtual getAuth method
			//Authenticate();
			String sUrl = encode(sInUrl);
			m_oHttpGet = new HttpGet(sUrl);
			m_oLastResponse = m_oHttpClient.execute(m_oHttpGet,m_oContext);
		} catch (IOException e) {
			e.printStackTrace();
		}
		saveStatusAndBody();
		return m_oLastResponse;
	}
	
	//maybe implement a "direct version", which does not cache the result?
	public CloseableHttpResponse httpPost( String sInUrl, Map<String, String> asFormData ) {
		String sUrl = encode(sInUrl);
		
		List<NameValuePair> asListFormData = new ArrayList<NameValuePair>();
		for (String sKey : asFormData.keySet()) {
			NameValuePair oNameValuePair = new BasicNameValuePair(sKey, asFormData.get(sKey));
			asListFormData.add(oNameValuePair);
		}
		try {
			UrlEncodedFormEntity oForm = new UrlEncodedFormEntity(asListFormData);
			m_oHttpPost = new HttpPost(sUrl);
			m_oHttpPost.setEntity(oForm);
			m_oLastResponse = m_oHttpClient.execute(m_oHttpPost,m_oContext);
			saveStatusAndBody();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return m_oLastResponse;
	}
	
	
	//maybe implement a "direct version", which does not cache the result?
	public CloseableHttpResponse httpPost( String sInUrl, String sPayload, Map<String, String> asHeaders ) {
		System.out.println("WasdiWpsHttpHelper.httpPost");
		if(null==sInUrl) {
			throw new NullPointerException("WasdiWpsHttpHelper.httpPost: url is null, cannot post");
		}
		try {

			String sUrl = encode(sInUrl);
		
			m_oHttpPost = new HttpPost(sUrl);
			if(null!=sPayload) {
				StringEntity oEntity = new StringEntity(sPayload);
				oEntity.setContentType("text/xml");
				System.out.println(oEntity.getContentType().toString());
				m_oHttpPost.setEntity(oEntity);
			}
			if(null!=asHeaders) {
				Set<String> asKeys = asHeaders.keySet();
				for (String sKey : asKeys) {
					String sValue = asHeaders.get(sKey);
					m_oHttpPost.addHeader(sKey, sValue);
				}
			}
			m_oLastResponse = m_oHttpClient.execute(m_oHttpPost,m_oContext);
			saveStatusAndBody();
			System.out.println("WasdiWpsHttpHelper.httpPost: server returned status code "+m_iLastStatus);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return m_oLastResponse;
	}

	protected void saveStatusAndBody() {
		m_iLastStatus = m_oLastResponse.getStatusLine().getStatusCode();
		HttpEntity oHttpEntity = m_oLastResponse.getEntity();
		try {
			//TODO can we get rid of this and retrieve the body as stream on demand?
			//this way it is hard on time and memory resources, as it stores a local copy
			m_sLastBody = EntityUtils.toString(oHttpEntity, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	protected String encode(String sInUrl) {
		String sUrl = sInUrl;
		/*
		String sProtocol = "http";
		if(sInUrl.startsWith("https")) {
			sProtocol += "s";
		}
		sProtocol += ":";
		sUrl = sInUrl.substring(sInUrl.indexOf(sProtocol) + sProtocol.length() );
		try {
			sUrl = URLEncoder.encode(sInUrl, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		sUrl = sProtocol+sUrl;
		return sUrl;
		*/
		
		//String sUrl = sInUrl.replaceAll("=", "%3D").replaceAll(" ", "%20").replaceAll(",", "%2C");
		return sUrl;
	}

	public CloseableHttpResponse getLastResponse() {
		return m_oLastResponse;
	}

	public void printAll() {
		System.out.println("Status = " + m_iLastStatus);
		printCookies();
		printHeaders();			
		printBody();
	}

	public void printBody() {
		printBody(m_oLastResponse);
	}

	public void printBody(CloseableHttpResponse oResponse) {
		try {
			System.out.println("<!-- response -->"); System.out.println("");
			System.out.println(oResponse);
			System.out.println("");System.out.println("<!-- end of response -->"); System.out.println("");

			System.out.println(getBodyAsString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void printHeaders() {
		printHeaders(m_oLastResponse);
	}

	public void printHeaders(CloseableHttpResponse oGetResponse) {
		//headers
		System.out.println("<!-- headers -->"); System.out.println("");
		HeaderIterator oHeaderIterator = oGetResponse.headerIterator();
		while(oHeaderIterator.hasNext()) {
			System.out.println(oHeaderIterator.next());
		}
		System.out.println("");System.out.println("<!-- end of headers -->"); System.out.println("");
	}

	public void printCookies() {
		printCookies(m_oContext);
	}

	public void printCookies(HttpClientContext oContext) {
		//cookies
		System.out.println("<!-- cookies -->"); System.out.println("");
		System.out.println(oContext.getCookieStore().getCookies());
		System.out.println(""); System.out.println("<!-- end of cookies -->"); System.out.println("");
	}

	public String getBodyAsString() {
		return m_sLastBody;
	}
	
	public InputStream getBodyAsStream() throws UnsupportedOperationException, IOException {
		/*
		InputStream oInputStream = null;
		try {
			oInputStream = m_oLastResponse.getEntity().getContent();
		} catch (UnsupportedOperationException | IOException e) {
			e.printStackTrace();
		}
		return oInputStream;
		*/
		//return m_oLastResponse.getEntity().getContent();
		m_oLastStream = new ByteArrayInputStream(m_sLastBody.getBytes(StandardCharsets.UTF_8));
		return m_oLastStream;

	}

	protected HttpClientBuilder addSslPropertiesToBuilder(HttpClientBuilder oBuilder) {
		try {
			m_oSslContext = new SSLContextBuilder()
					.loadTrustMaterial(null, (certificate, authType) -> true)
					.build();
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
			e.printStackTrace();
		}
		oBuilder = oBuilder
				.setSSLContext(m_oSslContext)
				.setSSLHostnameVerifier(new NoopHostnameVerifier());
		return oBuilder;
	}

		
}
