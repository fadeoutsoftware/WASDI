/*
 * ﻿Copyright (C) 2019 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wasdi.wpsClient;

import java.io.IOException;
import java.util.List;

import org.n52.geoprocessing.wps.client.ExecuteRequestBuilder;
import org.n52.geoprocessing.wps.client.WPSClientException;
import org.n52.geoprocessing.wps.client.WasdiWPSClientSession;
import org.n52.geoprocessing.wps.client.model.InputDescription;
import org.n52.geoprocessing.wps.client.model.Process;
import org.n52.geoprocessing.wps.client.model.Result;
import org.n52.geoprocessing.wps.client.model.StatusInfo;
import org.n52.geoprocessing.wps.client.model.WPSCapabilities;
import org.n52.geoprocessing.wps.client.model.execution.BoundingBox;
import org.n52.geoprocessing.wps.client.model.execution.Data;
import org.n52.geoprocessing.wps.client.model.execution.Execute;

public class WasdiWPSClientExample {

    private static final String MIME_TYPE_TEXT_CSV = "text/csv";
    private static final String MIME_TYPE_TEXT_XML = "text/xml";

    public void testExecute(String sVersion) {

//        String wpsURL = "http://localhost:8080/wps/WebProcessingService";
         String sWpsURL =
         "http://geoprocessing.demo.52north.org:8080/wps/WebProcessingService";

        String sProcessID = "org.n52.wps.server.algorithm.test.DummyTestClass";

        // try {
        // ProcessDescriptionType describeProcessDocument =
        // requestDescribeProcess(
        // wpsURL, processID);
        // System.out.println(describeProcessDocument);
        // } catch (IOException e) {
        // e.printStackTrace();
        // }
        try {
            WPSCapabilities oCpbDoc = requestGetCapabilities(sWpsURL, sVersion);

            System.out.println(oCpbDoc);

            Process oDescribeProcessDocument = requestDescribeProcess(sWpsURL, sProcessID, sVersion);

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

            execute(sWpsURL, oBuilder.getExecute(), sVersion);

        } catch (WPSClientException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void execute(String sUrl,
            Execute oExecute,
            String sVersion) {

        WasdiWPSClientSession oWpsClient = WasdiWPSClientSession.getInstance();

        try {
            Object oExecuteResponse = oWpsClient.execute(sUrl, oExecute, sVersion);

            System.out.println(oExecuteResponse);

            if (oExecuteResponse instanceof Result) {
                printOutputs((Result) oExecuteResponse);
            } else if (oExecuteResponse instanceof StatusInfo) {
                printOutputs(((StatusInfo) oExecuteResponse).getResult());
            }

        } catch (WPSClientException | IOException e) {
            System.out.println(e.getMessage());
        }

    }

    private void printOutputs(Result oResult) {

        List<Data> aoOutputs = oResult.getOutputs();

        for (Data oData : aoOutputs) {
            // if(data instanceof ComplexData){
            // ComplexData complexData = (ComplexData)data;
            // System.out.println(complexData);
            //
            // }
            System.out.println(oData);
        }

    }

    public WPSCapabilities requestGetCapabilities(String sUrl) throws WPSClientException {
    	return requestGetCapabilities(sUrl, WasdiWPSClientSession.s_sDefaultVersion);
    }
    
    public WPSCapabilities requestGetCapabilities(String sUrl,
            String sVersion) throws WPSClientException {

        WasdiWPSClientSession oWpsClient = WasdiWPSClientSession.getInstance();

        oWpsClient.connect(sUrl, sVersion);

        WPSCapabilities oCapabilities = oWpsClient.getWPSCaps(sUrl);

        List<Process> aoProcessList = oCapabilities.getProcesses();

        System.out.println("Processes in capabilities:");
        for (Process oProcess : aoProcessList) {
            System.out.println(oProcess.getId());
        }
        return oCapabilities;
    }

    public Process requestDescribeProcess(String sUrl,
            String sProcessID,
            String sVersion) throws IOException {

        WasdiWPSClientSession oWpsClient = WasdiWPSClientSession.getInstance();

        Process oProcessDescription = oWpsClient.getProcessDescription(sUrl, sProcessID, sVersion);

        List<InputDescription> aoInputList = oProcessDescription.getInputs();

        for (InputDescription oInput : aoInputList) {
            System.out.println(oInput.getId());
        }
        return oProcessDescription;
    }

    public static void main(String[] args) {

        // TODO find way to initialize parsers/generators

        WasdiWPSClientExample client = new WasdiWPSClientExample();
        client.testExecute("2.0.0");
        
    }

}