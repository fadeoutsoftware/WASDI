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
package org.n52.geoprocessing.wps.client;

import java.io.IOException;
import java.util.List;

import org.n52.geoprocessing.wps.client.ExecuteRequestBuilder;
import org.n52.geoprocessing.wps.client.WPSClientException;
import org.n52.geoprocessing.wps.client.WPSClientSession;
import org.n52.geoprocessing.wps.client.model.InputDescription;
import org.n52.geoprocessing.wps.client.model.Process;
import org.n52.geoprocessing.wps.client.model.Result;
import org.n52.geoprocessing.wps.client.model.StatusInfo;
import org.n52.geoprocessing.wps.client.model.WPSCapabilities;
import org.n52.geoprocessing.wps.client.model.execution.BoundingBox;
import org.n52.geoprocessing.wps.client.model.execution.Data;
import org.n52.geoprocessing.wps.client.model.execution.Execute;

public class WPSClientExample {

    private static final String MIME_TYPE_TEXT_CSV = "text/csv";
    private static final String MIME_TYPE_TEXT_XML = "text/xml";

    public void testExecute(String version) {

//        String wpsURL = "http://localhost:8080/wps/WebProcessingService";
         String wpsURL =
         "http://geoprocessing.demo.52north.org:8080/wps/WebProcessingService";

        String processID = "org.n52.wps.server.algorithm.test.DummyTestClass";

        // try {
        // ProcessDescriptionType describeProcessDocument =
        // requestDescribeProcess(
        // wpsURL, processID);
        // System.out.println(describeProcessDocument);
        // } catch (IOException e) {
        // e.printStackTrace();
        // }
        try {
            WPSCapabilities cpbDoc = requestGetCapabilities(wpsURL, version);

            String sCaps = cpbDoc.toString();
            System.out.println(sCaps);

            Process describeProcessDocument = requestDescribeProcess(wpsURL, processID, version);
            String sDescr = describeProcessDocument.toString();
            
            System.out.println(sDescr);

            ExecuteRequestBuilder builder = new ExecuteRequestBuilder(describeProcessDocument);

            builder.addComplexData("ComplexInputData", "a,b,c", "", "", MIME_TYPE_TEXT_CSV);

            builder.addLiteralData("LiteralInputData", "0.05", "", "", MIME_TYPE_TEXT_XML);

            BoundingBox boundingBox = new BoundingBox();

            boundingBox.setMinY(50.0);
            boundingBox.setMinX(7.0);
            boundingBox.setMaxY(51.0);
            boundingBox.setMaxX(7.1);

            boundingBox.setCrs("EPSG:4326");

            boundingBox.setDimensions(2);

            builder.addBoundingBoxData("BBOXInputData", boundingBox, "", "", MIME_TYPE_TEXT_XML);

            builder.addOutput("LiteralOutputData", "", "", MIME_TYPE_TEXT_XML);
            builder.addOutput("BBOXOutputData", "", "", MIME_TYPE_TEXT_XML);

            builder.setResponseDocument("ComplexOutputData", "", "", MIME_TYPE_TEXT_CSV);

            builder.setAsynchronousExecute();

            execute(wpsURL, builder.getExecute(), version);

        } catch (WPSClientException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void execute(String url,
            Execute execute,
            String version) {

        WPSClientSession wpsClient = WPSClientSession.getInstance();

        try {
            Object o = wpsClient.execute(url, execute, version);

            System.out.println(o);

            if (o instanceof Result) {
                printOutputs((Result) o);
            } else if (o instanceof StatusInfo) {
                printOutputs(((StatusInfo) o).getResult());
            }

        } catch (WPSClientException | IOException e) {
            System.out.println(e.getMessage());
        }

    }

    private void printOutputs(Result result) {

        List<Data> outputs = result.getOutputs();

        for (Data data : outputs) {
            // if(data instanceof ComplexData){
            // ComplexData complexData = (ComplexData)data;
            // System.out.println(complexData);
            //
            // }
            System.out.println(data);
        }

    }

    public WPSCapabilities requestGetCapabilities(String url,
            String version) throws WPSClientException {

        WPSClientSession wpsClient = WPSClientSession.getInstance();

        wpsClient.connect(url, version);

        WPSCapabilities capabilities = wpsClient.getWPSCaps(url);

        List<Process> processList = capabilities.getProcesses();

        System.out.println("Processes in capabilities:");
        for (Process process : processList) {
            System.out.println(process.getId());
        }
        return capabilities;
    }

    public Process requestDescribeProcess(String url,
            String processID,
            String version) throws IOException {

        WPSClientSession wpsClient = WPSClientSession.getInstance();

        Process processDescription = wpsClient.getProcessDescription(url, processID, version);

        List<InputDescription> inputList = processDescription.getInputs();

        for (InputDescription input : inputList) {
            System.out.println(input.getId());
        }
        return processDescription;
    }

    public static void main(String[] args) {

        // TODO find way to initialize parsers/generators

    	System.setProperty("javax.xml.stream.XMLOutputFactory", "com.sun.xml.internal.stream.XMLOutputFactoryImpl");
        WPSClientExample client = new WPSClientExample();
        client.testExecute("2.0.0");
    }

}