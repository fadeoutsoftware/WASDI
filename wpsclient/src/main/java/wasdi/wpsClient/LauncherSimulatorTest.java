package wasdi.wpsClient;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.n52.geoprocessing.wps.client.ExecuteRequestBuilder;
import org.n52.geoprocessing.wps.client.WasdiWPSClientSession;
import org.n52.geoprocessing.wps.client.model.Process;
import org.n52.geoprocessing.wps.client.model.Result;
import org.n52.geoprocessing.wps.client.model.StatusInfo;
import org.n52.geoprocessing.wps.client.model.WPSCapabilities;
import org.n52.geoprocessing.wps.client.model.execution.BoundingBox;
import org.n52.geoprocessing.wps.client.model.execution.Execute;
import org.n52.wps.client.WPSClientException;

import net.opengis.wps.x100.CapabilitiesDocument;
import net.opengis.wps.x100.ProcessDescriptionType;

public class LauncherSimulatorTest {

	private static final String MIME_TYPE_TEXT_CSV = "text/csv";
	private static final String MIME_TYPE_TEXT_XML = "text/xml";

	public static void main(String[] args) {
		System.setProperty("javax.xml.stream.XMLOutputFactory", "com.sun.xml.internal.stream.XMLOutputFactoryImpl");


		//callN52WPS();
//		callWasdiWPS();
		callGPODWPS();
		//TODO call UTEP
	}

	public static void callN52WPS() {
		String sVersion = "1.0.0";	
		String sWpsURL =
				"http://geoprocessing.demo.52north.org:8080/wps/WebProcessingService";
		//"http://178.22.66.96/geoserver/wps";
		try {

			//instantiate new client lib
			WasdiWpsClientLib oClient = new WasdiWpsClientLib("N52", sVersion);
			oClient.setServerUrl(sWpsURL);

			//TODO GetCapabilities(sUrl, sVersion)
			WPSCapabilities oCapabilities = oClient.requestGetCapabilities();

			//choose processId
			String sProcessID = "org.n52.wps.server.algorithm.test.DummyTestClass";
			//DescribeProcess(sProcessId)
			Process oDescribeProcessDocument = oClient.requestDescribeProcess(sProcessID);


			//test 0: build request using lib methods
			ExecuteRequestBuilder oBuilder = new ExecuteRequestBuilder(oDescribeProcessDocument);
			oBuilder.addComplexData("ComplexInputData", "a,b,c", "", "", MIME_TYPE_TEXT_CSV);
			oBuilder.addLiteralData("LiteralInputData", "0.05", "", "", MIME_TYPE_TEXT_XML);
			BoundingBox oBoundingBox = new BoundingBox();
			oBoundingBox.setMinY(50.0);
			oBoundingBox.setMinX(7.0);
			oBoundingBox.setMaxY(51.0);
			oBoundingBox.setMaxX(7.1);
			oBoundingBox.setCrs("EPSG:4326");
			oBoundingBox.setDimensions(2);
			oBuilder.addBoundingBoxData("BBOXInputData", oBoundingBox, "", "", MIME_TYPE_TEXT_XML);
			oBuilder.addOutput("LiteralOutputData", "", "", MIME_TYPE_TEXT_XML);
			oBuilder.addOutput("BBOXOutputData", "", "", MIME_TYPE_TEXT_XML);
			oBuilder.setResponseDocument("ComplexOutputData", "", "", MIME_TYPE_TEXT_CSV);
			oBuilder.setAsynchronousExecute();

			oClient.execute(oBuilder.getExecute());



			//TEST 1: plain url
			//TODO test URL

			//TEST 2: url + XML payload
			//          String sPayload = " <wps:Execute xmlns:wps=\"http://www.opengis.net/wps/2.0\"  xmlns:ows=\"http://www.opengis.net/ows/2.0\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.opengis.net/wps/2.0 ../wps.xsd\" service=\"WPS\" version=\"2.0.0\" response=\"document\" mode=\"sync\"><ows:Identifier>org.n52.wps.server.algorithm.SimpleBufferAlgorithm</ows:Identifier><wps:Input id=\"data\"><wps:Reference schema=\"http://schemas.opengis.net/gml/3.1.1/base/feature.xsd\" xlink:href=\"http://geoprocessing.demo.52north.org:8080/geoserver/wfs?SERVICE=WFS&amp;VERSION=1.0.0&amp;REQUEST=GetFeature&amp;TYPENAME=topp:tasmania_roads&amp;SRS=EPSG:4326&amp;OUTPUTFORMAT=GML3\"/></wps:Input><wps:Input id=\"width\"><wps:Data><wps:LiteralValue>0.05</wps:LiteralValue></wps:Data></wps:Input><wps:Output id=\"result\" transmission=\"value\"/></wps:Execute>";
			//          String sExecuteProcessOutput = oClient.rawExecute(sWpsURL, sPayload, null);
			//          System.out.println(sExecuteProcessOutput);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void callWasdiWPS() {
		String sWpsURL =
				"http://178.22.66.96/geoserver/wps";
		String sVersion = "1.0.0";
		try {

			//instantiate new client lib
			WasdiWpsClientLib oClient = new WasdiWpsClientLib("WASDI", sVersion);
			oClient.setServerUrl(sWpsURL);

			//TODO GetCapabilities(sUrl, sVersion)
			WPSCapabilities oCapabilities = oClient.requestGetCapabilities();
			//choose processId
			String sProcessID = "gs:WASDIHello";
			//DescribeProcess(sProcessId)
			Process oDescribeProcessDocument = oClient.requestDescribeProcess(sProcessID);

			//test 0: build request using lib methods
			ExecuteRequestBuilder oBuilder = new ExecuteRequestBuilder(oDescribeProcessDocument);
			oBuilder.addLiteralData("name", "Vader, Darth", "", "", MIME_TYPE_TEXT_XML);
			oClient.execute(oBuilder.getExecute());

		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

		public static void callGPODWPS() {
			String sWpsURL = "https://gpod.eo.esa.int/wps";
			String sVersion = "1.0.0";
			String sPayload = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><wps:Execute xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" service=\"WPS\" version=\"1.0.0\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 ../wpsExecute_request.xsd\"><ows:Identifier>SAROTEC_S1_WPS</ows:Identifier><wps:DataInputs><wps:Input><ows:Identifier>files</ows:Identifier><wps:Reference xlink:href=\"http://grid-eo-catalog.esrin.esa.int/catalogue/gpod/S1A_IW_GRDH/S1A_IW_GRDH_1SDV_20181207T035119_20181207T035144_024914_02BE8D_978D/rdf\"/></wps:Input><wps:Input><ows:Identifier>files</ows:Identifier><wps:Reference xlink:href=\"http://grid-eo-catalog.esrin.esa.int/catalogue/gpod/S1A_IW_GRDH/S1A_IW_GRDH_1SDV_20181125T035119_20181125T035144_024739_02B8B2_7433/rdf\"/></wps:Input><wps:Input><ows:Identifier>caption</ows:Identifier><wps:Data><wps:LiteralData>test</wps:LiteralData></wps:Data></wps:Input><wps:Input><ows:Identifier>master</ows:Identifier><wps:Data><wps:LiteralData>http://grid-eo-catalog.esrin.esa.int/catalogue/gpod/S1A_IW_GRDH/S1A_IW_GRDH_1SDV_20181125T035119_20181125T035144_024739_02B8B2_7433/rdf</wps:LiteralData></wps:Data></wps:Input></wps:DataInputs><wps:ResponseForm><wps:ResponseDocument storeExecuteResponse=\"true\" status=\"true\"><wps:Output mimeType=\"application/metalink+xml\"><ows:Identifier>ResultDescription</ows:Identifier></wps:Output></wps:ResponseDocument></wps:ResponseForm></wps:Execute>";
	    	try {
				//instantiate new client lib
				WasdiWpsClientLib oClient = new WasdiWpsClientLib("GPOD", sVersion);
				oClient.setServerUrl(sWpsURL);

				//TODO GetCapabilities(sUrl, sVersion)
				WPSCapabilities oCapabilities = oClient.requestGetCapabilities();
				//choose processId
				String sProcessID = "GPODTEST";
		    	//String sProcessID = "SAROTEC_S1_WPS";
				Process oDescribeProcessDocument = oClient.requestDescribeProcess(sProcessID);
				
				
				//test 0: build request using lib methods
				ExecuteRequestBuilder oBuilder = new ExecuteRequestBuilder(oDescribeProcessDocument);
				oBuilder.addComplexData("files", "http://grid-eo-catalog.esrin.esa.int/catalogue/gpod/dummy/dummy/rdf", "", "", MIME_TYPE_TEXT_CSV);				
				oBuilder.addLiteralData("caption", "WASDI testing GPOD WPS", "", "", MIME_TYPE_TEXT_XML);
//				oBuilder.setAsynchronousExecute();

				oClient.execute(oBuilder.getExecute());

	
	        } catch (IOException e) {
	            e.printStackTrace();
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        
	        return ;
		}

}
