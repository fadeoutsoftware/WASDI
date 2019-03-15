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
import java.util.Map;

import org.n52.geoprocessing.wps.client.WPSClientException;
import org.n52.geoprocessing.wps.client.WasdiWPSClientSession;
import org.n52.geoprocessing.wps.client.model.InputDescription;
import org.n52.geoprocessing.wps.client.model.Process;
import org.n52.geoprocessing.wps.client.model.Result;
import org.n52.geoprocessing.wps.client.model.StatusInfo;
import org.n52.geoprocessing.wps.client.model.WPSCapabilities;
import org.n52.geoprocessing.wps.client.model.execution.Data;
import org.n52.geoprocessing.wps.client.model.execution.Execute;

public class WasdiWpsClientLib {
    
    private WasdiWPSClientSession m_oWasdiWPSClientSession;
    private String m_sProvider;
    private String m_sVersion;
    
    public WasdiWpsClientLib() {
    	this(null);
    }
    
    public WasdiWpsClientLib(String sProvider) {
    	this(sProvider, "1.0.0");
    }
    
    public WasdiWpsClientLib(String sProvider, String sVersion) {
    	m_sProvider = sProvider;
    	m_sVersion = sVersion;
    	m_oWasdiWPSClientSession = WasdiWPSClientSession.getInstance(m_sProvider);
    }

    //TODO facilities for parsing result:
      // if it's a status, how to poll update?
      // how to collect result?
    
    
    public String rawExecute(String sUrl, String sPayload, Map<String, String> asHeaders) {
    	return m_oWasdiWPSClientSession.rawExecute(sUrl, sPayload, asHeaders);
    }
	
    
    //TODO facilities for building oExecute
    public Object execute(String sUrl, Execute oExecute, String sVersion) {

        WasdiWPSClientSession oWpsClient = WasdiWPSClientSession.getInstance();

        try {
            Object oExecuteResponse = oWpsClient.execute(sUrl, oExecute, sVersion);

            System.out.println(oExecuteResponse);

            if (oExecuteResponse instanceof Result) {
            	//TODO download results
                printOutputs((Result) oExecuteResponse);
            } else if (oExecuteResponse instanceof StatusInfo) {
            	//TODO update status in processWorkspace
                printOutputs(((StatusInfo) oExecuteResponse).getResult());
            }
            
            return oExecuteResponse;

        } catch (WPSClientException | IOException e) {
            System.out.println(e.getMessage());
        }

        return null;
    }
    

   
    public void printOutputs(Result oResult) {

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

        if(null!= oProcessDescription) {
	        List<InputDescription> aoInputList = oProcessDescription.getInputs();
	
	        for (InputDescription oInput : aoInputList) {
	            System.out.println(oInput.getId());
	        }
        }
        //else?
        //FIXME handle case when null is returned
        
        return oProcessDescription;
    }

}