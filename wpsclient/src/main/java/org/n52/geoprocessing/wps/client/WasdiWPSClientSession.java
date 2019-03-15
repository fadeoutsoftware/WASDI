/*
 * ï»¿Copyright (C) 2018 52Â°North Initiative for Geospatial Open Source
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration2.io.ClasspathLocationStrategy;
import org.apache.commons.configuration2.io.CombinedLocationStrategy;
import org.apache.commons.configuration2.io.DefaultFileSystem;
import org.apache.commons.configuration2.io.FileLocationStrategy;
import org.apache.commons.configuration2.io.FileLocator;
import org.apache.commons.configuration2.io.FileLocatorUtils;
import org.apache.commons.configuration2.io.FileSystem;
import org.apache.commons.configuration2.io.FileSystemLocationStrategy;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.xmlbeans.XmlObject;
import org.n52.geoprocessing.wps.client.ClientCapabiltiesRequest;
import org.n52.geoprocessing.wps.client.ClientDescribeProcessRequest;
import org.n52.geoprocessing.wps.client.WPSClientException;
import org.n52.geoprocessing.wps.client.encoder.stream.ExecuteRequest100Encoder;
import org.n52.geoprocessing.wps.client.encoder.stream.ExecuteRequest20Encoder;
import org.n52.geoprocessing.wps.client.model.Process;
import org.n52.geoprocessing.wps.client.model.ResponseMode;
import org.n52.geoprocessing.wps.client.model.StatusInfo;
import org.n52.geoprocessing.wps.client.model.WPSCapabilities;
import org.n52.geoprocessing.wps.client.model.execution.ExecutionMode;
import org.n52.geoprocessing.wps.client.xml.WPSResponseReader;
import org.n52.janmayen.Json;
import org.n52.shetland.ogc.wps.JobStatus;
import org.n52.svalbard.encode.exception.EncodingException;
import org.n52.svalbard.encode.stream.xml.ElementXmlStreamWriterRepository;
import org.n52.svalbard.encode.stream.xml.XmlStreamWritingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import httpHelper.WasdiGpodHttpHelper;
import httpHelper.WasdiUTepHttpHelper;
import httpHelper.WasdiWpsHttpHelper;

/**
 * Contains some convenient methods to access and manage Web Processing Services
 * in a very generic way.
 *
 * This is implemented as a singleton.
 *
 * @author bpross,foerster
 */

public final class WasdiWPSClientSession {

    public static final String VERSION_100 = "1.0.0";

    public static final String VERSION_200 = "2.0.0";

    public static final String SERVICE = "WPS";

    private static final String AUTHORIZATION = "Authorization";

    private static final String GOT_HTTP_ERROR = "Got HTTP error code, response: ";

	public static final String s_sDefaultVersion = "1.0.0";

    private static Logger LOGGER = LoggerFactory.getLogger(WasdiWPSClientSession.class);

    private static WasdiWPSClientSession s_oSession;

    private static int maxNumberOfAsyncRequests = 100;

    private static int delayForAsyncRequests = 1000;

    private Map<String, WPSCapabilities> m_aoLoggedServices;

    private boolean bCancel;

    // a Map of <url, all available process descriptions>
    private Map<String, List<Process>> m_aoProcessDescriptions;

    private String m_sBearerToken = "";

    private boolean m_bUseBearerToken;
    
    protected WasdiWpsHttpHelper m_oHttpHelper;
    protected String m_sProvider;
    
    public WasdiWPSClientSession() {
    	this(null);
    }

    /**
     * Initializes a WPS client session.
     *
     */
    public WasdiWPSClientSession(String sWpsProvider) {
        m_aoLoggedServices = new HashMap<String, WPSCapabilities>();
        m_aoProcessDescriptions = new HashMap<String, List<Process>>();
        loadProperties();
        
        m_sProvider = sWpsProvider;
        
        if (sWpsProvider == null) {
			m_oHttpHelper = new WasdiWpsHttpHelper();
		}
		else if (sWpsProvider.equals("GPOD")) m_oHttpHelper = new WasdiGpodHttpHelper();
		else if (sWpsProvider.equals("UTEP")) m_oHttpHelper = new WasdiUTepHttpHelper();
    }
    
    public static WasdiWPSClientSession getInstance() {
    	return getInstance(null);
    }
    
    /*
     * @result An instance of a WPS Client session.
     */
    public static WasdiWPSClientSession getInstance(String sWpsProvider) {
    	//TODO practically unused. maybe handle providers here, otherwise remove
        synchronized (WasdiWPSClientSession.class) {
            if (s_oSession == null) {
                s_oSession = new WasdiWPSClientSession(sWpsProvider);
            }
        }
        return s_oSession;
    }

    /**
     * This resets the WasdiWPSClientSession. This might be necessary, to get rid of
     * old service entries/descriptions. However, the session has to be
     * repopulated afterwards.
     */
    public static void reset() {
        s_oSession = new WasdiWPSClientSession();
    }

    public boolean connect(String sUrl) throws WPSClientException {
    	return connect(sUrl, s_sDefaultVersion);
    }
    
    /**
     * Connects to a WPS and retrieves Capabilities plus puts all available
     * Descriptions into cache.
     *
     * @param sUrl
     *            the entry point for the service. This is used as id for
     *            further identification of the service.
     * @param sVersion
     *            the version of the WPS
     * @return true, if connect succeeded, false else.
     * @throws WPSClientException
     *             if the capabilities could not be requested
     */
    public boolean connect(String sUrl, String sVersion) throws WPSClientException {
        LOGGER.info("CONNECT");
        if (m_aoLoggedServices.containsKey(sUrl)) {
            LOGGER.info("Service already registered: " + sUrl);
            return false;
        }
        m_oHttpHelper.authenticate();
        
        WPSCapabilities capsDoc = retrieveCapsViaGET(sUrl, sVersion);
        //TODO can capsDoc be null? 
        m_aoLoggedServices.put(sUrl, capsDoc);
        return true;
    }

    /**
     * removes a service from the session
     *
     * @param sUrl
     *            WPS URL
     */
    public void disconnect(String sUrl) {
        if (m_aoLoggedServices.containsKey(sUrl)) {
            m_aoLoggedServices.remove(sUrl);
            m_aoProcessDescriptions.remove(sUrl);
            LOGGER.info("service removed successfully: " + sUrl);
        }
    }

    /**
     * returns the serverIDs of all loggedServices
     *
     * @return List of logged service URLs
     */
    public List<String> getLoggedServices() {
        return new ArrayList<String>(m_aoLoggedServices.keySet());
    }

    /**
     * informs you if the descriptions for the specified service is already in
     * the session. in normal case it should return true :)
     *
     * @param sServerID
     *            WPS URL
     * @return success true if the the process descriptions of the WPS are
     *         cached
     */
    public boolean descriptionsAvailableInCache(String sServerID) {
        return m_aoProcessDescriptions.containsKey(sServerID);
    }

    /**
     * return the processDescription for a specific process from Cache.
     *
     * @param sServerID
     *            WPS URL
     * @param sProcessID
     *            id of the process
     * @param sVersion
     *            the version of the WPS
     * @return a ProcessDescription for a specific process from Cache.
     */
    public Process getProcessDescription(String sServerID,
            String sProcessID,
            String sVersion) {
        List<Process> aoProcesses = getProcessDescriptionsFromCache(sServerID);
        for (Process oProcess : aoProcesses) {
            if (oProcess.getId().equals(sProcessID)) {
                if (oProcess.getInputs() == null || oProcess.getInputs().isEmpty()) {
                    try {
                        return describeProcess(new String[] { sProcessID }, sServerID, sVersion).get(0);
                    } catch (WPSClientException e) {
                        LOGGER.error("Could not fetch processdescription for process: " + sProcessID, e);
                    }
                }
                return oProcess;
            }
        }
        return null;
    }

    /**
     * Delivers all ProcessDescriptions from a WPS
     *
     * @param sWpsUrl
     *            the URL of the WPS
     * @return An Array of ProcessDescriptions
     */
    public List<Process> getAllProcessDescriptions(String sWpsUrl) {
        return getProcessDescriptionsFromCache(sWpsUrl);
    }

    /**
     * looks up, if the service exists already in session.
     *
     * @param sServerID
     *            the URL of the WPS
     * @return true if the service exists in this session
     */
    public boolean serviceAlreadyRegistered(String sServerID) {
        return m_aoLoggedServices.containsKey(sServerID);
    }

    /**
     * provides you the cached capabilities for a specified service.
     *
     * @param sUrl
     *            WPS URL
     * @return WPSCapabilities object
     */
    public WPSCapabilities getWPSCaps(String sUrl) {
        return m_aoLoggedServices.get(sUrl);
    }

    /**
     * retrieves the desired description for a service. the retrieved
     * information will not be held in cache!
     *
     * @param asProcessIDs
     *            one or more process IDs
     * @param sServerID
     *            WPS URL
     * @param sVersion
     *            WPS version
     * @throws WPSClientException
     *             of the process description could not be requested
     * @return list of process objects
     */
    public List<Process> describeProcess(String[] asProcessIDs,
            String sServerID,
            String sVersion) throws WPSClientException {
        return retrieveDescriptionViaGET(asProcessIDs, sServerID, sVersion);
    }

    /**
     * Executes a process at a WPS
     *
     * @param sUrl
     *            url of server not the entry additionally defined in the caps.
     * @param oExecute
     *            Execute document
     * @param sVersion
     *            the version of the WPS
     *
     * @return either an ExecuteResponseDocument or an InputStream if asked for
     *         RawData or an Exception Report
     * @throws WPSClientException
     *             if the initial execute request failed
     * @throws IOException
     *             if subsequent requests failed in async mode
     */
    public Object execute(String sUrl,
            org.n52.geoprocessing.wps.client.model.execution.Execute oExecute,
            String sVersion) throws WPSClientException, IOException {

        boolean bRequestRawData = oExecute.getResponseMode() == ResponseMode.RAW;
        boolean bRequestAsync = oExecute.getExecutionMode() == ExecutionMode.ASYNC;
        // TODO: what about AUTO mode?

        Object oExecuteObject = encode(oExecute, sVersion);

        return execute(sUrl, oExecuteObject, bRequestRawData, bRequestAsync);
    }
        
     private Object execute(String sUrl,
            Object oExecuteObject,
            boolean bRawData,
            boolean bRequestAsync) throws WPSClientException, IOException {
        return retrieveExecuteResponseViaPOST(sUrl, oExecuteObject, bRawData, bRequestAsync);
    }

    public String[] getProcessNames(String sUrl) throws IOException {
        List<Process> aoProcesses = getProcessDescriptionsFromCache(sUrl);
        String[] asProcessNames = new String[aoProcesses.size()];
        for (int i = 0; i < asProcessNames.length; i++) {
            asProcessNames[i] = aoProcesses.get(i).getId();
        }
        return asProcessNames;
    }

    public void cancelAsyncExecute() {
        setCancel(true);
    }

    public int checkService(String sUrl,
            String sPayload) {

        String sException = "IOException while trying to access: ";

        CloseableHttpResponse oResponse = null;

        if (sPayload == null || sPayload.isEmpty()) {


            try {
            	oResponse = m_oHttpHelper.httpGet(sUrl);
            } catch (Exception e) {
                LOGGER.error(sException + sUrl);
            }
        } else {

//            HttpPost oPost = new HttpPost(sUrl);

//            StringEntity oStringEntity = null;
//            try {
//                oStringEntity = new StringEntity(sPayload);
//            } catch (UnsupportedEncodingException e) {
//                LOGGER.error("Unsupported encoding in payload: " + sPayload);
//            }

//            oPost.setEntity(oStringEntity);

            try {
//                oResponse = m_oHttpClient.execute(oPost);
            	//TODO check encoding appropriateness
                m_oHttpHelper.httpPost(sUrl, sPayload, null);
            } catch (Exception e) {
                LOGGER.error(sException + sUrl);
            }
        }

        int result = -1;

        if (oResponse != null) {
            result = oResponse.getStatusLine().getStatusCode();

            try {
                oResponse.close();
            } catch (IOException e) {
                LOGGER.error("Could not close HTTPResponse ", e);
            }
        }

        return result;
    }

    public String getBearerToken() {
        return m_sBearerToken;
    }

    public void setBearerToken(String sBearerToken) {
        this.m_sBearerToken = sBearerToken;
    }

    public boolean isUseBearerToken() {
        return m_bUseBearerToken;
    }

    public void setUseBearerToken(boolean bUseBearerToken) {
        this.m_bUseBearerToken = bUseBearerToken;
    }

    private synchronized boolean isCancel() {
        return bCancel;
    }

    private synchronized void setCancel(boolean bCancel) {
        this.bCancel = bCancel;
    }

    private List<Process> getProcessDescriptionsFromCache(String sWpsUrl) {
        return m_aoLoggedServices.get(sWpsUrl).getProcesses();
    }

    private OutputStream encode(org.n52.geoprocessing.wps.client.model.execution.Execute oExecute,
            String sVersion) {

        OutputStream oOut = new ByteArrayOutputStream();

        switch (sVersion) {
        case VERSION_100:

            ExecuteRequest100Encoder oExecuteRequestWriter = new ExecuteRequest100Encoder();

            try {
                oExecuteRequestWriter.setContext(new XmlStreamWritingContext(oOut,
                        new ElementXmlStreamWriterRepository(Arrays.asList(ExecuteRequest100Encoder::new))::get));
                oExecuteRequestWriter.writeElement(oExecute);
            } catch (EncodingException | XMLStreamException e) {
                LOGGER.error(e.getMessage());
            }

            return oOut;

        case VERSION_200:

            ExecuteRequest20Encoder oExecuteRequestWriter20 = new ExecuteRequest20Encoder();

            try {
                oExecuteRequestWriter20.setContext(new XmlStreamWritingContext(oOut,
                        new ElementXmlStreamWriterRepository(Arrays.asList(ExecuteRequest20Encoder::new))::get));
                oExecuteRequestWriter20.writeElement(oExecute);
            } catch (EncodingException | XMLStreamException e) {
                LOGGER.error(e.getMessage());
            }

            return oOut;

        default:
            return oOut;
        }
    }

    private WPSCapabilities retrieveCapsViaGET(String sUrl,
            String sVersion) throws WPSClientException {
        ClientCapabiltiesRequest oCapabilitiesRequest = new ClientCapabiltiesRequest(sVersion);
        String sGetRequestURL = oCapabilitiesRequest.getRequest(sUrl);
        try {
            URL oUrlObj = new URL(sGetRequestURL);
            Object oResponseObject = retrieveResponseOrExceptionReportInpustream(oUrlObj);
            if (oResponseObject instanceof WPSCapabilities) {
                return (WPSCapabilities) oResponseObject;
            } else {
                throw new WPSClientException("Did not get (valid) capabilities, got: " + oResponseObject);
            }
        } catch (MalformedURLException e) {
            throw new WPSClientException("Capabilities URL seems to be unvalid: " + sUrl, e);
        } catch (IOException e) {
            throw new WPSClientException("Error occured while retrieving capabilities from url: " + sUrl, e);
        }
    }

    private Object retrieveResponseOrExceptionReportInpustream(URL oUrl) throws WPSClientException, IOException {

        Map<String, String> asHeaders = new HashMap<>();
        if (isUseBearerToken()) {
        	asHeaders.put(AUTHORIZATION, getBearerToken());
        }
        
        CloseableHttpResponse oResponse = m_oHttpHelper.httpGet(oUrl.toString(), asHeaders);

        //Object oResponseObject = parseInputStreamToString(oResponse.getEntity().getContent());
        Object oResponseObject = parseInputStreamToString(m_oHttpHelper.getBodyAsStream());

        try {
            checkStatusCode(oResponse);
        } catch (Exception e) {
            throw new WPSClientException(GOT_HTTP_ERROR + oResponseObject);
        } finally {
            oResponse.close();
        }

        return oResponseObject;
    }

    private Object retrieveResponseOrExceptionReportInpustream(URL oUrl,
            String sExecuteObject) throws WPSClientException, IOException {

//        HttpPost oPost = new HttpPost(oUrl.toString());

        Map<String, String> asHeaders = new HashMap<>();
        asHeaders.put("Accept-Encoding", "gzip");
        asHeaders.put("Content-Type", "text/xml");

        if (isUseBearerToken()) {
            asHeaders.put(AUTHORIZATION, getBearerToken());
        }

        CloseableHttpResponse oHttpResponse = m_oHttpHelper.httpPost(oUrl.toString(), sExecuteObject, asHeaders);

        //Object oResponseObject = parseInputStreamToString(oHttpResponse.getEntity().getContent());
        Object oResponseObject = parseInputStreamToString(m_oHttpHelper.getBodyAsStream());

        try {
            checkStatusCode(oHttpResponse);
        } catch (Exception e) {
            throw new WPSClientException(GOT_HTTP_ERROR + oResponseObject);
        } finally {
            oHttpResponse.close();
        }

        return oResponseObject;
    }

    private void checkStatusCode(CloseableHttpResponse oHttpResponse) throws WPSClientException {

        int iStatusCode = oHttpResponse.getStatusLine().getStatusCode();

        if (iStatusCode >= 400) {
            throw new WPSClientException("Got HTTP error code: " + iStatusCode);
        }

    }

    private Object parseInputStreamToString(InputStream oInputStream) throws IOException, WPSClientException {

        XMLEventReader oXmlReader = null;
//        Object oResult = null;
        try {
//        	XMLInputFactory oXMLInputFactory = XMLInputFactory.newInstance();
//        	InputStreamReader oReader = new InputStreamReader(oInputStream, StandardCharsets.UTF_8);
//            oXmlReader = oXMLInputFactory.createXMLEventReader(oReader);
        	
        	oXmlReader = XMLInputFactory.newInstance()
        			.createXMLEventReader(new InputStreamReader(oInputStream, StandardCharsets.UTF_8));
            
//            WPSResponseReader oWPSResponseReader = new WPSResponseReader();
//            oResult = oWPSResponseReader.readElement(oXmlReader);
            //WAS:
            return new WPSResponseReader().readElement(oXmlReader);
        } catch (XMLStreamException e) {
            throw new WPSClientException("Could not decode Inputstream.", e);
        }
//        return oResult;
    }

    @SuppressWarnings("unchecked")
    private List<Process> retrieveDescriptionViaGET(String[] asProcessIDs,
            String sUrl,
            String sVersion) throws WPSClientException {
        ClientDescribeProcessRequest oReq = new ClientDescribeProcessRequest(sVersion);
        oReq.setIdentifier(asProcessIDs);
        String sRequestURL = oReq.getRequest(sUrl);
        try {
            URL oUrlObj = new URL(sRequestURL);
            Object oResponseObject = retrieveResponseOrExceptionReportInpustream(oUrlObj);

            if (oResponseObject instanceof List) {

                return (List<Process>) oResponseObject;
            }
        } catch (MalformedURLException e) {
            throw new WPSClientException("URL seems not to be valid: " + sUrl, e);
        } catch (IOException e) {
            throw new WPSClientException("Error occured while receiving data", e);
        }
        LOGGER.info("No valid ProcessDescription found. Returning empty list.");
        return new ArrayList<Process>();
    }

    private Object retrieveDataViaPOST(Object oExecuteObject,
            String sUrlString) throws WPSClientException {
        try {
            URL oUrl = new URL(sUrlString);

            String sContent = "";

            if (oExecuteObject instanceof ByteArrayOutputStream) {
                sContent = ((ByteArrayOutputStream) oExecuteObject).toString(StandardCharsets.UTF_8.name());
            } else if(oExecuteObject instanceof String) {
            	sContent = (String) oExecuteObject;
            }
            
            return retrieveResponseOrExceptionReportInpustream(oUrl, sContent);
        } catch (MalformedURLException e) {
            throw new WPSClientException("URL seems to be invalid: " + sUrlString, e);
        } catch (IOException e) {
            throw new WPSClientException("Error while transmission", e);
        }
    }

    /**
     * either an ExecuteResponseDocument or an InputStream if asked for RawData
     * or an Exception Report
     *
     * @param sUrl
     *            WPS url
     * @param oExecuteObject
     *            encoded execute request
     * @param bRawData
     *            indicates if raw data should be requested
     * @param bRequestAsync
     *            indicates if request should be async
     * @return The execute response
     * @throws WPSClientException
     *             if the initial execute request failed
     * @throws IOException
     *             if subsequent requests failed in async mode
     */
    private Object retrieveExecuteResponseViaPOST(String sUrl,
            Object oExecuteObject,
            boolean bRawData,
            boolean bRequestAsync) throws WPSClientException, IOException {

        Object oResponseObject = retrieveDataViaPOST(oExecuteObject, sUrl);

        if (bRawData && !bRequestAsync) {
            return oResponseObject;
        }

        if (oResponseObject instanceof StatusInfo) {

            if (bRequestAsync) {
                return getAsyncDoc(sUrl, oResponseObject);
            }

            return (StatusInfo) oResponseObject;
        }
        // TODO when does this happen?!
        return oResponseObject;
    }

    private String createGetStatusURLWPS20(String sUrl,
            String sJobID) throws MalformedURLException {

        String sUrlSpec = sUrl.endsWith("?") ? sUrl : sUrl.concat("?");

        sUrlSpec = sUrlSpec.concat("service=WPS&version=2.0.0&request=GetStatus&jobID=" + sJobID);

        return sUrlSpec;

    }

    private String createGetResultURLWPS20(String sUrl,
            String sJobID) throws MalformedURLException {

        String sUrlSpec = sUrl.endsWith("?") ? sUrl : sUrl.concat("?");

        sUrlSpec = sUrlSpec.concat("service=WPS&version=2.0.0&request=GetResult&jobID=" + sJobID);

        return sUrlSpec;

    }

    private Object getAsyncDoc(String sUrl,
            Object oResponseObject) throws IOException, WPSClientException {

        String sGetStatusURL = "";
        boolean bProcessSuceeded = false;
        boolean bProcessFailed = false;

        if (oResponseObject instanceof StatusInfo) {

            StatusInfo oStatusInfoDocument = (StatusInfo) oResponseObject;
            String sJobID = oStatusInfoDocument.getJobId();
            bProcessSuceeded = oStatusInfoDocument.getStatus().equals(JobStatus.succeeded());
            bProcessFailed = oStatusInfoDocument.getStatus().equals(JobStatus.failed());

            // if succeeded, return result, otherwise GetResult operation will
            // return ExceptionReport
            if (bProcessSuceeded || bProcessFailed) {

                String sGetResultURL = oStatusInfoDocument.getStatusLocation();

                String sStatusLocation = oStatusInfoDocument.getStatusLocation();

                if (sStatusLocation == null || sStatusLocation.isEmpty()) {
                    sGetResultURL = createGetResultURLWPS20(sUrl, sJobID);
                }

                return retrieveResponseOrExceptionReportInpustream(new URL(sGetResultURL));
            }

            String sStatusLocation = oStatusInfoDocument.getStatusLocation();

            if (sStatusLocation != null && !(sStatusLocation.isEmpty())) {
                sGetStatusURL = sStatusLocation;
            } else {
                sGetStatusURL = createGetStatusURLWPS20(sUrl, sJobID);
            }
        }

        if (isCancel()) {
            LOGGER.info("Asynchronous Execute operation canceled.");
            // TODO
            return XmlObject.Factory.newInstance();
        }

        //TODO implement iterative version... isn't it dangerous to do this recursively?!?
        // assume process is still running, pause configured amount of time
        try {
            LOGGER.info("Let Thread sleep millis: " + delayForAsyncRequests);
            Thread.sleep(delayForAsyncRequests);
        } catch (InterruptedException e) {
            LOGGER.error("Could not let Thread sleep millis: " + delayForAsyncRequests, e);
        }

        return getAsyncDoc(sUrl, retrieveResponseOrExceptionReportInpustream(new URL(sGetStatusURL)));
    }

    private void loadProperties() {

        String sFileName = "wps-client.properties";
        String sDefaultFileName = "wps-client-default.properties";

        Optional<JsonNode> oPropertyNodeOptional = Optional.empty();

        URL oFileURL = null;
        // always check propertyFilename first
        oFileURL = locateFile(sFileName);
        // check if the strategies found something
        if (oFileURL != null) {
            try {
                oPropertyNodeOptional = Optional.of(Json.loadURL(oFileURL));
            } catch (IOException e) {
                LOGGER.error("Could not read property file.", e);
            }
        }
        oFileURL = locateFile(sDefaultFileName);
        try {
            oPropertyNodeOptional = Optional.of(Json.loadURL(oFileURL));
        } catch (IOException e) {
            LOGGER.error("Could not read  default property file.", e);
        }

        if (oPropertyNodeOptional.isPresent()) {
            JsonNode propertyNode = oPropertyNodeOptional.get();
            setProperties(propertyNode);
        }
    }

    private URL locateFile(String sfileName) {
        FileLocationStrategy oStrategy = new CombinedLocationStrategy(
                Arrays.asList(new FileSystemLocationStrategy(), new ClasspathLocationStrategy()));
        FileSystem oFileSystem = new DefaultFileSystem();
        FileLocator oLocator = FileLocatorUtils.fileLocator().locationStrategy(oStrategy).fileName(sfileName).create();
        return oStrategy.locate(oFileSystem, oLocator);
    }

    private void setProperties(JsonNode oPropertyNode) {

        JsonNode oSettingsNode = oPropertyNode.get("settings");
        String sJsonMalformed = "Properties JSON malformed.";
        String sValue = "value";

        if (oSettingsNode == null) {
            LOGGER.info(sJsonMalformed);
            return;
        }

        JsonNode oMaxNumberOfAsyncRequestsNode = oSettingsNode.get("maxNumberOfAsyncRequests");

        if (oMaxNumberOfAsyncRequestsNode != null) {
            JsonNode oValueNode = oMaxNumberOfAsyncRequestsNode.get(sValue);
            if (oValueNode == null) {
                LOGGER.info(sJsonMalformed);
            } else {
                maxNumberOfAsyncRequests = oValueNode.asInt();
            }
        } else {
            LOGGER.info("Property maxNumberOfAsyncRequests not present, defaulting to: " + maxNumberOfAsyncRequests);
        }

        JsonNode oDelayForAsyncRequestsNode = oSettingsNode.get("delayForAsyncRequests");

        if (oDelayForAsyncRequestsNode != null) {
            JsonNode oValueNode = oDelayForAsyncRequestsNode.get(sValue);
            if (oValueNode == null) {
                LOGGER.info(sJsonMalformed);
            } else {
                delayForAsyncRequests = oValueNode.asInt();
            }
        } else {
            LOGGER.info("Property delayForAsyncRequests not present, defaulting to: " + delayForAsyncRequests);
        }
    }

	public String rawExecute(String sUrl, String sPayload, Map<String, String> asHeaders) {
		m_oHttpHelper.httpPost(sUrl, sPayload, asHeaders);
		return m_oHttpHelper.getBodyAsString();
	}
}
