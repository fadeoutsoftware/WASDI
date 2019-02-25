/**
 * Created by Cristiano Nattero on 2019-02-25
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
import java.net.MalformedURLException;
import java.net.URL;

import wasdi.ProcessWorkspaceUpdateSubscriber;

/**
 * @author c.nattero
 *
 */
public class WasdiWpsExecutionclient extends WpsExecutionClient {
	//TODO implement ProcessWorkspaceUpdateNotifier

	/* (non-Javadoc)
	 * @see wasdi.wps.WpsExecutionClient#execute()
	 */
	@Override
	public int execute() {

		try {
		String sUrl = "http://178.22.66.96/geoserver/wps";
		String sXmlPayload = 
				"<wps:Execute service=\"WPS\" version=\"1.0.0\" 	xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" 	xmlns:ows=\"http://www.opengis.net/ows/1.1\" 	xmlns:xlink=\"http://www.w3.org/1999/xlink\" 	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" 	xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 	  http://schemas.opengis.net/wps/1.0.0/wpsExecute_request.xsd\"> " + 
				"	<ows:Identifier>gs:WASDIHello</ows:Identifier> " + 
				"	<wps:DataInputs> " + 
				"		<wps:Input> " + 
				"			<ows:Identifier>name</ows:Identifier> " + 
				"			<wps:Data> " + 
				"				<wps:LiteralData>ciaoCiaoCiao</wps:LiteralData> " + 
				"			</wps:Data> " + 
				"		</wps:Input> " + 
				"	</wps:DataInputs> " + 
				"	<wps:ResponseForm> " + 
				"		<wps:ResponseDocument storeExecuteResponse=\"true\" 	lineage=\"false\" status=\"true\"> " + 
				"			<wps:Output> " + 
				"				<ows:Identifier>result</ows:Identifier> " + 
				"			</wps:Output> " + 
				"		</wps:ResponseDocument> " + 
				"	</wps:ResponseForm> " + 
				"</wps:Execute>";
		
		
			URL oUrl = new URL(sUrl);
			Object oConnectionObject = oUrl.openConnection();
			HttpURLConnection oConnection = (HttpURLConnection)oConnectionObject;
			oConnection.setRequestMethod("POST");
			oConnection.setRequestProperty("Accept", "application/xml");
			oConnection.setRequestProperty("Content-Type", "application/xml");
			oConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
			oConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

			oConnection.setDoOutput(true);
			DataOutputStream oOutputStream = new DataOutputStream(oConnection.getOutputStream());
			OutputStreamWriter oWriter = new OutputStreamWriter(oOutputStream, "UTF-8");
			oWriter.write(sXmlPayload);
			oWriter.flush();
			oWriter.close();
			oOutputStream.flush();
			oOutputStream.close();
			
			oConnection.connect();
			
			int iResponseCode = oConnection.getResponseCode();
			if(HttpURLConnection.HTTP_OK == iResponseCode) {
				String sResult = null;
				BufferedInputStream oBufferedReader = new BufferedInputStream(oConnection.getInputStream());
				ByteArrayOutputStream oByteArrayOutputStream = new ByteArrayOutputStream();
				int iResult = oBufferedReader.read();
				while(iResult != -1) {
				    oByteArrayOutputStream.write((byte) iResult);
				    iResult = oBufferedReader.read();
				}
				sResult = oByteArrayOutputStream.toString();
				System.out.println(sResult);
			} else {
				System.out.println(x);
			}
			
			
		
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		
		return 0;
	}

}
