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
		
		
		String sVersion = "2.0.0";
		
//		callN52WPS(sVersion);
		//callWasdiWPS(sVersion);
		//callGPODWPS(sVersion);
	}
	
////    public void execute(String sUrl,
////            Execute oExecute,
////            String sVersion,
////            WasdiWpsClientLib oClientLib) {
////
////        WasdiWPSClientSession oWpsClient = WasdiWPSClientSession.getInstance();
////
////        try {
////            Object oExecuteResponse = oWpsClient.execute(sUrl, oExecute, sVersion);
////
////            System.out.println(oExecuteResponse);
////
////            if (oExecuteResponse instanceof Result) {
////                printOutputs((Result) oExecuteResponse);
////            } else if (oExecuteResponse instanceof StatusInfo) {
////                printOutputs(((StatusInfo) oExecuteResponse).getResult());
////            }
////
////        } catch (WPSClientException | IOException e) {
////            System.out.println(e.getMessage());
////        }
//
////    }
//	
	public static void callN52WPS(String sVersion) {
       String sWpsURL =
       "http://geoprocessing.demo.52north.org:8080/wps/WebProcessingService";
	   //"http://178.22.66.96/geoserver/wps";
      try {
    	  
    	  //instantiate new client lib
    	  WasdiWpsClientLib oClient = new WasdiWpsClientLib();
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
    	  
    	  //TODO GetStatus
    	  //TODO
    	  
    	  
    	  
    	  
    	  
                    
          //TEST 1: plain url
          //TODO test URL
          
          //TEST 2: url + XML payload
          String sPayload = " <wps:Execute xmlns:wps=\"http://www.opengis.net/wps/2.0\"  xmlns:ows=\"http://www.opengis.net/ows/2.0\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.opengis.net/wps/2.0 ../wps.xsd\" service=\"WPS\" version=\"2.0.0\" response=\"document\" mode=\"sync\"><ows:Identifier>org.n52.wps.server.algorithm.SimpleBufferAlgorithm</ows:Identifier><wps:Input id=\"data\"><wps:Reference schema=\"http://schemas.opengis.net/gml/3.1.1/base/feature.xsd\" xlink:href=\"http://geoprocessing.demo.52north.org:8080/geoserver/wfs?SERVICE=WFS&amp;VERSION=1.0.0&amp;REQUEST=GetFeature&amp;TYPENAME=topp:tasmania_roads&amp;SRS=EPSG:4326&amp;OUTPUTFORMAT=GML3\"/></wps:Input><wps:Input id=\"width\"><wps:Data><wps:LiteralValue>0.05</wps:LiteralValue></wps:Data></wps:Input><wps:Output id=\"result\" transmission=\"value\"/></wps:Execute>";
          String sExecuteProcessOutput = oClient.rawExecute(sWpsURL, sPayload, null);
          System.out.println(sExecuteProcessOutput);

          //TEST 3: url + build XML from data
          
//          //let the lib construct XML from data 
//        	sProcessID = "org.n52.wps.server.algorithm.test.DummyTestClass";  
          
//          ExecuteRequestBuilder oBuilder = new ExecuteRequestBuilder(oDescribeProcessDocument);
//          oBuilder.addComplexData("ComplexInputData", "a,b,c", "", "", MIME_TYPE_TEXT_CSV);
//          oBuilder.addLiteralData("LiteralInputData", "0.05", "", "", MIME_TYPE_TEXT_XML);
//
//          //prepare data
//          
//          BoundingBox oBoundingBox = new BoundingBox();
//          oBoundingBox.setMinY(50.0);
//          oBoundingBox.setMinX(7.0);
//          oBoundingBox.setMaxY(51.0);
//          oBoundingBox.setMaxX(7.1);
//          oBoundingBox.setCrs("EPSG:4326");
//          oBoundingBox.setDimensions(2);
//
//          oBuilder.addBoundingBoxData("BBOXInputData", oBoundingBox, "", "", MIME_TYPE_TEXT_XML);
//          oBuilder.addOutput("LiteralOutputData", "", "", MIME_TYPE_TEXT_XML);
//          oBuilder.addOutput("BBOXOutputData", "", "", MIME_TYPE_TEXT_XML);
//          oBuilder.setResponseDocument("ComplexOutputData", "", "", MIME_TYPE_TEXT_CSV);
//          oBuilder.setAsynchronousExecute();

//          oClient.execute(sWpsURL, oBuilder.getExecute(), sVersion);

      } catch (IOException e) {
          e.printStackTrace();
      } catch (Exception e) {
          e.printStackTrace();
      }
		
	}

//	protected static void getCapsAndDescr(String sVersion, String sWpsURL, String sProcessID, WasdiWpsClientLib oClient)
//			throws org.n52.geoprocessing.wps.client.WPSClientException, IOException {
//		//TEST 0: GetCapabilities + DescribeProcess
//          WPSCapabilities oCpbDoc = oClient.requestGetCapabilities(sWpsURL, sVersion);
//          System.out.println(oCpbDoc);
//          Process oDescribeProcessDocument = oClient.requestDescribeProcess(sWpsURL, sProcessID, sVersion);
//          System.out.println(oDescribeProcessDocument);
//	}
//	
//
//	
//	public static void callWasdiWPS() {
//		WasdiWpsClientLib oClient = new WasdiWpsClientLib();
//		try {
//			String sWpsURL = "http://178.22.66.96/geoserver/wps";
//			CapabilitiesDocument oWasdiCap = oClient.requestGetCapabilities(sWpsURL);
//            System.out.println(oWasdiCap);
//            ProcessDescriptionType oDescribeProcessDocument = oClient.requestDescribeProcess(sWpsURL, "gs:WASDIHello");
//
//            System.out.println(oDescribeProcessDocument);
//			
//		} catch (WPSClientException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//	
//	public static void callGPODWPS() {
//		String sWpsURL = "https://gpod.eo.esa.int/wps";
//		//String sProcessID = "GPODTEST";
//    	String sProcessID = "SAROTEC_S1_WPS";
//		String sPayload = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><wps:Execute xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" service=\"WPS\" version=\"1.0.0\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 ../wpsExecute_request.xsd\"><ows:Identifier>SAROTEC_S1_WPS</ows:Identifier><wps:DataInputs><wps:Input><ows:Identifier>files</ows:Identifier><wps:Reference xlink:href=\"http://grid-eo-catalog.esrin.esa.int/catalogue/gpod/S1A_IW_GRDH/S1A_IW_GRDH_1SDV_20181207T035119_20181207T035144_024914_02BE8D_978D/rdf\"/></wps:Input><wps:Input><ows:Identifier>files</ows:Identifier><wps:Reference xlink:href=\"http://grid-eo-catalog.esrin.esa.int/catalogue/gpod/S1A_IW_GRDH/S1A_IW_GRDH_1SDV_20181125T035119_20181125T035144_024739_02B8B2_7433/rdf\"/></wps:Input><wps:Input><ows:Identifier>caption</ows:Identifier><wps:Data><wps:LiteralData>test</wps:LiteralData></wps:Data></wps:Input><wps:Input><ows:Identifier>master</ows:Identifier><wps:Data><wps:LiteralData>http://grid-eo-catalog.esrin.esa.int/catalogue/gpod/S1A_IW_GRDH/S1A_IW_GRDH_1SDV_20181125T035119_20181125T035144_024739_02B8B2_7433/rdf</wps:LiteralData></wps:Data></wps:Input></wps:DataInputs><wps:ResponseForm><wps:ResponseDocument storeExecuteResponse=\"true\" status=\"true\"><wps:Output mimeType=\"application/metalink+xml\"><ows:Identifier>ResultDescription</ows:Identifier></wps:Output></wps:ResponseDocument></wps:ResponseForm></wps:Execute>";
//    	
//    	
//    	
//		WasdiWPSClientSession oClient = WasdiWPSClientSession.getInstance("GPOD");
//    	
//    	
//        try {
//            CapabilitiesDocument oCapabilitiesDocument = oClient.requestGetCapabilities(sWpsURL);
//            System.out.println(oCapabilitiesDocument);
//            ProcessDescriptionType oDescribeProcessDocument = oClient.requestDescribeProcess(sWpsURL, sProcessID);
//            System.out.println(oDescribeProcessDocument);
//            
//            String sExecuteProcessOutput =  null;
//            
//            // define inputs
////            HashMap<String, Object> aoInputs = new HashMap<String, Object>();
////            ArrayList<String> asFiles = new ArrayList<>();
////            asFiles.add("myFile");
////            // complex data by reference
////            aoInputs.put("files", asFiles);
////            //aoInputs.put("msg_outfmt", "PNG");
////            aoInputs.put("msg_outfmt", "GEOTIFF_FMT");
////            aoInputs.put("proj_num", "0");
////            aoInputs.put("proj_sphere", "10");
////            aoInputs.put("kernel", "NN");
////            aoInputs.put("dtype", "INT16");
////            sExecuteProcessOutput = oClient.executeProcess(sWpsURL, sProcessID, oDescribeProcessDocument, aoInputs, "ResultDescription");
//           
//            
//            //URLEncoder.encode()
//            
//            
//            //just plain url
//            String sUrl = "https://gpod.eo.esa.int/wps/?service=WPS&request=Execute&version=1.0.0&Identifier=GPODTEST&storeExecuteResponse=true&status=true&DataInputs=;caption=Test;files=http://grid-eo-catalog.esrin.esa.int/catalogue/gpod/dummy/dummy/rdf";
//            sExecuteProcessOutput = oClient.executeProcess(sUrl, oDescribeProcessDocument, "ResultDescription");
//            
//            //url + String XML payload
//            //String sPayload = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><wps:Execute xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" service=\"WPS\" version=\"1.0.0\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 ../wpsExecute_request.xsd\"><ows:Identifier>GPODTEST</ows:Identifier><wps:DataInputs><wps:Input><ows:Identifier>files</ows:Identifier><wps:Reference xlink:href=\"http://grid-eo-catalog.esrin.esa.int/catalogue/gpod/dummy/dummy/rdf\"/></wps:Input><wps:Input><ows:Identifier>caption</ows:Identifier><wps:Data><wps:LiteralData>test</wps:LiteralData></wps:Data></wps:Input></wps:DataInputs><wps:ResponseForm><wps:ResponseDocument storeExecuteResponse=\"true\" status=\"true\"><wps:Output mimeType=\"application/metalink+xml\"><ows:Identifier>ResultDescription</ows:Identifier></wps:Output></wps:ResponseDocument></wps:ResponseForm></wps:Execute>";
//            //String sUrl = "https://gpod.eo.esa.int/wps/?service=WPS&request=Execute&version=1.0.0";
//            sExecuteProcessOutput = oClient.executeProcess(sWpsURL, sPayload, oDescribeProcessDocument, "ResultDescription");
//            
//            //url + (almost) automatic payload creation
////            Map<String, Object> aoInputs = new HashMap<String, Object>();
////            List<String> asFiles = new ArrayList<>();
////            asFiles.add("http://grid-eo-catalog.esrin.esa.int/catalogue/gpod/dummy/dummy/rdf");
////            aoInputs.put("files", asFiles);
////            aoInputs.put("caption", "test");
////            sExecuteProcessOutput = oClient.executeProcess(sWpsURL, sProcessID, oDescribeProcessDocument, aoInputs, "ResultDescription");
//            
//            System.out.println("ANSWER:");
//            System.out.println(sExecuteProcessOutput);
//
//        } catch (WPSClientException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        
//        return ;
//	}

}
