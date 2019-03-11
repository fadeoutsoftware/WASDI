/**
 * Created by Cristiano Nattero on 2019-02-20
 * 
 * Fadeout software
 *
 */
package wasdi.wps;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author c.nattero
 *
 */
public abstract class WpsAdapter {
	
	protected static String s_sWpsHost;
	protected static String s_sVersion;
	protected String m_sService;
	
	protected String m_sResponse;
	protected String m_sXmlPayload;
	
	protected String m_sJobId;
	

	public WpsAdapter(){
		m_sService = "WPS";
	}
		
	public void setM_sXmlPayload(String sXmlPayload) {
		this.m_sXmlPayload = sXmlPayload;
	}
	
	// http://cite.opengeospatial.org/pub/cite/files/edu/wps/text/operations.html#getcapabilities
	//MAYBE getCapabilities

	// http://cite.opengeospatial.org/pub/cite/files/edu/wps/text/operations.html#describeprocess
	//MAYBE describeProcess

//	public int n52Execute() {
//		System.out.println("WpsAdapter.n52Execute");
//		int iResponseCode = -1;
//		try {
//			XmlObject oXmlObject = XmlObject.Factory.parse(m_sXmlPayload);
//			ExecuteDocument oRequest = (ExecuteDocument) oXmlObject;
//			oRequest.getExecute().setService(m_sService);
//			WPSClientSession oClientSession = WPSClientSession.getInstance();
//			String sUrl = buildExecuteUrl();
//			Object oResponseObject = oClientSession.execute(sUrl, oRequest);
//			
//			if (oResponseObject instanceof ExecuteResponseDocument) {
//	            ExecuteResponseDocument oResponse = (ExecuteResponseDocument) oResponseObject;
//	            
////	            ExecuteResponseAnalyser analyser = new ExecuteResponseAnalyser(oExecute, oResponse, oProcessDescription);
////	            IData oIData = (IData) analyser.getComplexDataByIndex(0,String.class);
//	            
//	            XObject oXObject = XPathAPI.eval(oResponse.getDomNode(), "//wps:LiteralData");
//	            String sOutput = oXObject.toString();
//	            
//	            //return sOutput;
//	        }
//			
//			
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return iResponseCode;
//	}
	
	// http://cite.opengeospatial.org/pub/cite/files/edu/wps/text/operations.html#execute
	public int execute() {
		System.out.println("WpsAdapter.execute");
		int iResponseCode = -1;
		try {
			
			
			
			String sUrl = buildExecuteUrl();
			
			
			URL oUrl = new URL(sUrl);
			Object oConnectionObject = oUrl.openConnection();
			HttpURLConnection oConnection = (HttpURLConnection)oConnectionObject;
			oConnection.setRequestMethod("POST");
			oConnection.setRequestProperty("Accept", "application/xml");
			oConnection.setRequestProperty("Content-Type", "application/xml");
			oConnection.setRequestProperty("User-Agent", "Mozilla/5.0");

			oConnection.setDoOutput(true);
			DataOutputStream oOutputStream = new DataOutputStream(oConnection.getOutputStream());
			OutputStreamWriter oWriter = new OutputStreamWriter(oOutputStream, "UTF-8");
			oWriter.write(m_sXmlPayload);
			oWriter.flush();
			oWriter.close();
			oOutputStream.flush();
			oOutputStream.close();
			
			oConnection.connect();
			
			iResponseCode = oConnection.getResponseCode();
			if(HttpURLConnection.HTTP_OK == iResponseCode) {
				m_sResponse = null;
				BufferedInputStream oBufferedReader = new BufferedInputStream(oConnection.getInputStream());
				ByteArrayOutputStream oByteArrayOutputStream = new ByteArrayOutputStream();
				int iResult = oBufferedReader.read();
				while(iResult != -1) {
				    oByteArrayOutputStream.write((byte) iResult);
				    iResult = oBufferedReader.read();
				}
				m_sResponse = oByteArrayOutputStream.toString();
				System.out.println(m_sResponse);
				//TODO set job ID
			} else {
				System.out.println("WpsAdapter.execute: response status: " + iResponseCode);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return iResponseCode;
	}

	protected String buildExecuteUrl() {
		String sUrl = s_sWpsHost + "?" +
				"service=" + m_sService + "&" +
				"version=" + s_sVersion;
				//and nothing else, pass the XML payload instead
		return sUrl;
	}

	public String getResponse() {
		System.out.println("WpsAdapter.getLastStatus");
		return m_sResponse;
	}
	
	// http://cite.opengeospatial.org/pub/cite/files/edu/wps/text/operations.html#getstatus
	public String getStatus() {
		//TODO GET status URL
		//TODO refactor URL + query param + maybe headers and cookies for authentication
		return null;
	}
	
	// http://cite.opengeospatial.org/pub/cite/files/edu/wps/text/operations.html#getresult
	public abstract String getResult();
	
	// http://cite.opengeospatial.org/pub/cite/files/edu/wps/text/operations.html#dismiss
	//MAYBE public abstract String dismiss();
}
