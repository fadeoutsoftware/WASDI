package wasdi.wpsclient;

import java.io.IOException;
import java.util.HashMap;

import org.apache.xpath.XPathAPI;
import org.apache.xpath.objects.XObject;
import org.geotools.feature.FeatureCollection;
import org.n52.wps.client.ExecuteResponseAnalyser;
import org.n52.wps.client.WPSClientException;
import org.n52.wps.client.WPSClientSession;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;

import net.opengis.wps.x100.CapabilitiesDocument;
import net.opengis.wps.x100.ExecuteDocument;
import net.opengis.wps.x100.ExecuteResponseDocument;
import net.opengis.wps.x100.InputDescriptionType;
import net.opengis.wps.x100.ProcessBriefType;
import net.opengis.wps.x100.ProcessDescriptionType;

/**
 * Hello world!
 *
 */
public class WPSClientExample 
{
	
    public String testDownload() {

        String wpsURL = "http://www.wasdi.net/geoserver/wps";

        String processID = "gs:WASDIImportEOImage";

        try {
            ProcessDescriptionType describeProcessDocument = requestDescribeProcess(wpsURL, processID);
            //System.out.println(describeProcessDocument);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        try {
        	
            CapabilitiesDocument capabilitiesDocument = requestGetCapabilities(wpsURL);
            
            ProcessDescriptionType describeProcessDocument = requestDescribeProcess(wpsURL, processID);
            // define inputs
            HashMap<String, Object> inputs = new HashMap<String, Object>();
            
            // complex data by reference
            inputs.put("EOImageLink", "https://scihub.copernicus.eu/dhus/odata/v1/Products('3aec97a5-e991-42b2-84e9-ece81e04c08f')/$value");
            inputs.put("RESTCallback", "none");
            
            String sOutput = executeProcess(wpsURL, processID, describeProcessDocument, inputs, "WasdiProcessId");
            
            System.out.println("ANSWER:");
            System.out.println(sOutput);

            return sOutput;

        } catch (WPSClientException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return "";
    }
    
    
    public String testGetStatus(String sProcessId) {

        String wpsURL = "http://www.wasdi.net/geoserver/wps";

        String processID = "gs:WASDIProcessStatus";

        try {
            ProcessDescriptionType describeProcessDocument = requestDescribeProcess(wpsURL, processID);
            //System.out.println(describeProcessDocument);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        try {
        	
            CapabilitiesDocument capabilitiesDocument = requestGetCapabilities(wpsURL);
            
            ProcessDescriptionType describeProcessDocument = requestDescribeProcess(wpsURL, processID);
            // define inputs
            HashMap<String, Object> inputs = new HashMap<String, Object>();
            
            // complex data by reference
            inputs.put("processId", sProcessId);
            
            String sOutput = executeProcess(wpsURL, processID, describeProcessDocument, inputs, "ProcessStatus");
            
            System.out.println("ANSWER:");
            System.out.println(sOutput);

            return sOutput;
            
        } catch (WPSClientException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return "NONE_WPASDI ERROR";
    }

    public CapabilitiesDocument requestGetCapabilities(String url) throws WPSClientException {

        WPSClientSession wpsClient = WPSClientSession.getInstance();

        wpsClient.connect(url);

        CapabilitiesDocument capabilities = wpsClient.getWPSCaps(url);

        ProcessBriefType[] processList = capabilities.getCapabilities().getProcessOfferings().getProcessArray();

//        for (ProcessBriefType process : processList) {
//            System.out.println(process.getIdentifier().getStringValue());
//        }
        return capabilities;
    }

    public ProcessDescriptionType requestDescribeProcess(String url,String processID) throws IOException {

        WPSClientSession wpsClient = WPSClientSession.getInstance();

        ProcessDescriptionType processDescription = wpsClient.getProcessDescription(url, processID);

        InputDescriptionType[] inputList = processDescription.getDataInputs().getInputArray();

//        for (InputDescriptionType input : inputList) {
//            System.out.println(input.getIdentifier().getStringValue());
//        }
        return processDescription;
    }

    public String executeProcess(String url, String processID, ProcessDescriptionType processDescription,HashMap<String, Object> inputs, String sSimpleResultName) throws Exception {
    	
        org.n52.wps.client.ExecuteRequestBuilder executeBuilder = new org.n52.wps.client.ExecuteRequestBuilder(processDescription);

        for (InputDescriptionType input : processDescription.getDataInputs().getInputArray()) {
        	
            String inputName = input.getIdentifier().getStringValue();
            Object inputValue = inputs.get(inputName);
            
            if (input.getLiteralData() != null) {
                if (inputValue instanceof String) {
                    executeBuilder.addLiteralData(inputName,(String) inputValue);
                }
            } 
            else if (input.getComplexData() != null) {
                // Complexdata by value
                if (inputValue instanceof FeatureCollection) {
                    IData data = new GTVectorDataBinding(
                            (FeatureCollection) inputValue);
                    executeBuilder
                            .addComplexData(
                                    inputName,
                                    data,
                                    "http://schemas.opengis.net/gml/3.1.1/base/feature.xsd",
                                    null, "text/xml");
                }
                // Complexdata Reference
                if (inputValue instanceof String) {
                    executeBuilder
                            .addComplexDataReference(
                                    inputName,
                                    (String) inputValue,
                                    "http://schemas.opengis.net/gml/3.1.1/base/feature.xsd",
                                    null, "text/xml");
                }

                if (inputValue == null && input.getMinOccurs().intValue() > 0) {
                    throw new IOException("Property not set, but mandatory: "
                            + inputName);
                }
            }
        }
        //executeBuilder.setMimeTypeForOutput("text/xml", "result");
        //executeBuilder.setSchemaForOutput("http://schemas.opengis.net/gml/3.1.1/base/feature.xsd","result");
        executeBuilder.setResponseDocument(sSimpleResultName, null, null, null);
        ExecuteDocument execute = executeBuilder.getExecute();
        execute.getExecute().setService("WPS");
        WPSClientSession wpsClient = WPSClientSession.getInstance();
        Object responseObject = wpsClient.execute(url, execute);
        
        if (responseObject instanceof ExecuteResponseDocument) {
            ExecuteResponseDocument response = (ExecuteResponseDocument) responseObject;
            
            //ExecuteResponseAnalyser analyser = new ExecuteResponseAnalyser(execute, response, processDescription);
            //IData data = (IData) analyser.getComplexDataByIndex(0,String.class);
            
            XObject data = XPathAPI.eval(response.getDomNode(), "//wps:LiteralData");
            String output = data.toString();
            
            
            return output;
        }
        throw new Exception("Exception: " + responseObject.toString());
    }

    
    
    public static void main( String[] args ) throws InterruptedException
    {
        System.out.println( "Hello World!" );
        
        WPSClientExample client = new WPSClientExample();
        String sProcess = client.testDownload();
        
        String sStatus = client.testGetStatus(sProcess);
        System.out.println(sStatus);
        
        
        while (sStatus.equals("CREATED") || sStatus.equals("WAITING") || sStatus.equals("RUNNING")) {
        	Thread.sleep(1000l);
        	sStatus = client.testGetStatus(sProcess);
        	System.out.println(sStatus);
        }
        
    }
}
