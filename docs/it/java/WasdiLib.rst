.. java:import:: java.io BufferedInputStream

.. java:import:: java.io BufferedOutputStream

.. java:import:: java.io BufferedReader

.. java:import:: java.io ByteArrayOutputStream

.. java:import:: java.io DataOutputStream

.. java:import:: java.io File

.. java:import:: java.io FileInputStream

.. java:import:: java.io FileOutputStream

.. java:import:: java.io IOException

.. java:import:: java.io InputStream

.. java:import:: java.io InputStreamReader

.. java:import:: java.io OutputStream

.. java:import:: java.io OutputStreamWriter

.. java:import:: java.net HttpURLConnection

.. java:import:: java.net MalformedURLException

.. java:import:: java.net ProtocolException

.. java:import:: java.net URL

.. java:import:: java.net URLConnection

.. java:import:: java.net URLEncoder

.. java:import:: java.util ArrayList

.. java:import:: java.util Enumeration

.. java:import:: java.util HashMap

.. java:import:: java.util List

.. java:import:: java.util Map

.. java:import:: java.util UUID

.. java:import:: java.util.zip ZipEntry

.. java:import:: java.util.zip ZipFile

.. java:import:: org.apache.commons.net.io Util

.. java:import:: com.fasterxml.jackson.core.type TypeReference

.. java:import:: com.fasterxml.jackson.databind ObjectMapper

.. java:import:: wasdi.jwasdilib.utils MosaicSetting

Java WasdiLib
==============

.. java:package:: java
   :noindex:

.. java:type:: public class WasdiLib

Fields
------
s_oMapper
^^^^^^^^^

.. java:field:: protected static ObjectMapper s_oMapper
   :outertype: WasdiLib

Constructors
------------
WasdiLib
^^^^^^^^

.. java:constructor:: public WasdiLib()
   :outertype: WasdiLib

   Self constructor. If there is a config file initializes the class members

Methods
-------
addFileToWASDI
^^^^^^^^^^^^^^

.. java:method:: public String addFileToWASDI(String sFileName)
   :outertype: WasdiLib

   Ingest a new file in the Active WASDI Workspace waiting for the result The method takes a file saved in the workspace root (see getSaveFilePath) not already added to the WS To work be sure that the file is on the server

   :param sFileName: Name of the file to add
   :return: Output state of the ingestion process

addParam
^^^^^^^^

.. java:method:: public void addParam(String sKey, String sParam)
   :outertype: WasdiLib

   Add Param

   :param sKey:
   :param sParam:

asynchAddFileToWASDI
^^^^^^^^^^^^^^^^^^^^

.. java:method:: public String asynchAddFileToWASDI(String sFileName)
   :outertype: WasdiLib

   Ingest a new file in the Active WASDI Workspace in an asynch way The method takes a file saved in the workspace root (see getSaveFilePath) not already added to the WS To work be sure that the file is on the server

   :param sFileName: Name of the file to add
   :return: Process Id of the ingestion process

asynchExecuteProcessor
^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public String asynchExecuteProcessor(String sProcessorName, HashMap<String, Object> aoParams)
   :outertype: WasdiLib

   Execute a WASDI processor in Asynch way

   :param sProcessorName: Processor Name
   :param aoParams: Dictionary of Params
   :return: ProcessWorkspace Id

asynchExecuteProcessor
^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public String asynchExecuteProcessor(String sProcessorName, String sEncodedParams)
   :outertype: WasdiLib

   Execute a WASDI processor in Asynch way

   :param sProcessorName: Processor Name
   :param sEncodedParams: Already JSON Encoded Params
   :return: ProcessWorkspace Id

asynchExecuteWorkflow
^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public String asynchExecuteWorkflow(String[] asInputFileName, String[] asOutputFileName, String sWorkflowName)
   :outertype: WasdiLib

   Executes a WASDI SNAP Workflow in a asynch mode

   :param sInputFileName:
   :param sOutputFileName:
   :param sWorkflowName:
   :return: Workflow Process Id if every thing is ok, '' if there was any problem

asynchMosaic
^^^^^^^^^^^^

.. java:method:: public String asynchMosaic(List<String> asInputFiles, String sOutputFile)
   :outertype: WasdiLib

   Asynch Mosaic with minimum parameters

   :param asInputFiles: List of input files to mosaic
   :param sOutputFile: Name of the mosaic output file
   :return: Process id

asynchMosaic
^^^^^^^^^^^^

.. java:method:: public String asynchMosaic(List<String> asInputFiles, String sOutputFile, String sNoDataValue, String sInputIgnoreValue)
   :outertype: WasdiLib

   Asynch Mosaic with also Bands Parameters

   :param asInputFiles: List of input files to mosaic
   :param sOutputFile: Name of the mosaic output file
   :return: Process id

asynchMosaic
^^^^^^^^^^^^

.. java:method:: public String asynchMosaic(List<String> asInputFiles, String sOutputFile, String sNoDataValue, String sInputIgnoreValue, List<String> asBands)
   :outertype: WasdiLib

   Asynch Mosaic with also Bands Parameters

   :param asInputFiles: List of input files to mosaic
   :param sOutputFile: Name of the mosaic output file
   :param asBands: List of the bands to use for the mosaic
   :return: Process id

asynchMosaic
^^^^^^^^^^^^

.. java:method:: public String asynchMosaic(List<String> asInputFiles, String sOutputFile, String sNoDataValue, String sInputIgnoreValue, List<String> asBands, double dPixelSizeX, double dPixelSizeY)
   :outertype: WasdiLib

   Asynch Mosaic with also Pixel Size Parameters

   :param asInputFiles: List of input files to mosaic
   :param sOutputFile: Name of the mosaic output file
   :param asBands: List of the bands to use for the mosaic
   :param dPixelSizeX: X Pixel Size
   :param dPixelSizeY: Y Pixel Size
   :return: Process id

asynchMosaic
^^^^^^^^^^^^

.. java:method:: public String asynchMosaic(List<String> asInputFiles, String sOutputFile, String sNoDataValue, String sInputIgnoreValue, List<String> asBands, double dPixelSizeX, double dPixelSizeY, String sCrs)
   :outertype: WasdiLib

   Asynch Mosaic with also CRS Input

   :param asInputFiles: List of input files to mosaic
   :param sOutputFile: Name of the mosaic output file
   :param asBands: List of the bands to use for the mosaic
   :param dPixelSizeX: X Pixel Size
   :param dPixelSizeY: Y Pixel Size
   :param sCrs: WKT of the CRS to use
   :return: Process id

asynchMosaic
^^^^^^^^^^^^

.. java:method:: public String asynchMosaic(List<String> asInputFiles, String sOutputFile, String sNoDataValue, String sInputIgnoreValue, List<String> asBands, double dPixelSizeX, double dPixelSizeY, String sCrs, double dSouthBound, double dNorthBound, double dEastBound, double dWestBound, String sOverlappingMethod, boolean bShowSourceProducts, String sElevationModelName, String sResamplingName, boolean bUpdateMode, boolean bNativeResolution, String sCombine)
   :outertype: WasdiLib

   Asynch Mosaic with all the input parameters

   :param asInputFiles: List of input files to mosaic
   :param sOutputFile: Name of the mosaic output file
   :param asBands: List of the bands to use for the mosaic
   :param dPixelSizeX: X Pixel Size
   :param dPixelSizeY: Y Pixel Size
   :param sCrs: WKT of the CRS to use
   :param dSouthBound: South Bound
   :param dNorthBound: North Bound
   :param dEastBound: East Bound
   :param dWestBound: West Bound
   :param sOverlappingMethod: Overlapping Method
   :param bShowSourceProducts: Show Source Products Flag
   :param sElevationModelName: DEM Model Name
   :param sResamplingName: Resampling Method Name
   :param bUpdateMode: Update Mode Flag
   :param bNativeResolution: Native Resolution Flag
   :param sCombine: Combine verb
   :return: Process id

checkSession
^^^^^^^^^^^^

.. java:method:: public String checkSession(String sSessionID)
   :outertype: WasdiLib

   Call CheckSession API

   :param sSessionID: actual session Id
   :return: Session Id or "" if there are problems

copyStream
^^^^^^^^^^

.. java:method:: protected void copyStream(InputStream oInputStream, OutputStream oOutputStream) throws IOException
   :outertype: WasdiLib

copyStreamAndClose
^^^^^^^^^^^^^^^^^^

.. java:method:: protected void copyStreamAndClose(InputStream oInputStream, OutputStream oOutputStream) throws IOException
   :outertype: WasdiLib

   Copy Input Stream in Output Stream

   :param oInputStream:
   :param oOutputStream:
   :throws IOException:

deleteProduct
^^^^^^^^^^^^^

.. java:method:: public String deleteProduct(String sProduct)
   :outertype: WasdiLib

   Delete a Product in the active Workspace

   :param sProduct:

downloadFile
^^^^^^^^^^^^

.. java:method:: protected String downloadFile(String sFileName)
   :outertype: WasdiLib

   Download a file on the local PC

   :param sFileName: File Name
   :return: Full Path

executeWorkflow
^^^^^^^^^^^^^^^

.. java:method:: public String executeWorkflow(String[] asInputFileName, String[] asOutputFileName, String sWorkflowName)
   :outertype: WasdiLib

   Executes a WASDI SNAP Workflow waiting for the process to finish

   :param sInputFileName:
   :param sOutputFileName:
   :param sWorkflowName:
   :return: output status of the Workflow Process

getActiveWorkspace
^^^^^^^^^^^^^^^^^^

.. java:method:: public String getActiveWorkspace()
   :outertype: WasdiLib

   Get Active Workspace

getBasePath
^^^^^^^^^^^

.. java:method:: public String getBasePath()
   :outertype: WasdiLib

   Set Base Path

getBaseUrl
^^^^^^^^^^

.. java:method:: public String getBaseUrl()
   :outertype: WasdiLib

   Get Base Url

getDownloadActive
^^^^^^^^^^^^^^^^^

.. java:method:: public Boolean getDownloadActive()
   :outertype: WasdiLib

   Get Download Active flag

getFoundProductLink
^^^^^^^^^^^^^^^^^^^

.. java:method:: public String getFoundProductLink(Map<String, Object> oProduct)
   :outertype: WasdiLib

   Get the direct download link of a Product found by searchEOImage

   :param oProduct: JSON Dictionary Product as returned by searchEOImage
   :return: Name of the product

getFoundProductName
^^^^^^^^^^^^^^^^^^^

.. java:method:: public String getFoundProductName(Map<String, Object> oProduct)
   :outertype: WasdiLib

   Get the name of a Product found by searchEOImage

   :param oProduct: JSON Dictionary Product as returned by searchEOImage
   :return: Name of the product

getFullProductPath
^^^^^^^^^^^^^^^^^^

.. java:method:: public String getFullProductPath(String sProductName)
   :outertype: WasdiLib

   Get the full local path of a product given the product name. Use the output of this API to open the file

   :param sProductName: Product Name
   :return: Product Full Path as a String ready to open file

getIsOnServer
^^^^^^^^^^^^^

.. java:method:: public Boolean getIsOnServer()
   :outertype: WasdiLib

   Get is on server flag

getMyProcId
^^^^^^^^^^^

.. java:method:: public String getMyProcId()
   :outertype: WasdiLib

   Get my own Process Id

getParam
^^^^^^^^

.. java:method:: public String getParam(String sKey)
   :outertype: WasdiLib

   Get Param

   :param sKey:

getParametersFilePath
^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public String getParametersFilePath()
   :outertype: WasdiLib

   Get Parameters file path

   :return: parameters file path

getParams
^^^^^^^^^

.. java:method:: public HashMap<String, String> getParams()
   :outertype: WasdiLib

   Get Params HashMap

   :return: Params Dictionary

getPassword
^^^^^^^^^^^

.. java:method:: public String getPassword()
   :outertype: WasdiLib

   Get Password

getPath
^^^^^^^

.. java:method:: public String getPath(String sProductName)
   :outertype: WasdiLib

   Get the local path of a file

   :param sProductName: Name of the file
   :return: Full local path

getProcessStatus
^^^^^^^^^^^^^^^^

.. java:method:: public String getProcessStatus(String sProcessId)
   :outertype: WasdiLib

   Get WASDI Process Status

   :param sProcessId: Process Id
   :return: Process Status as a String: CREATED, RUNNING, STOPPED, DONE, ERROR, WAITING, READY

getProcessorPath
^^^^^^^^^^^^^^^^

.. java:method:: public String getProcessorPath()
   :outertype: WasdiLib

   Get the processor Path

getProductsByActiveWorkspace
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public List<String> getProductsByActiveWorkspace()
   :outertype: WasdiLib

   Get a List of the products in the active Workspace

   :return: List of Strings representing the product names

getProductsByWorkspace
^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public List<String> getProductsByWorkspace(String sWorkspaceName)
   :outertype: WasdiLib

   Get a List of the products in a Workspace

   :param sWorkspaceName: Workspace Name
   :return: List of Strings representing the product names

getSavePath
^^^^^^^^^^^

.. java:method:: public String getSavePath()
   :outertype: WasdiLib

   Get the local Save Path to use to save custom generated files

   :return: Local Path to use to save a custom generated file

getSessionId
^^^^^^^^^^^^

.. java:method:: public String getSessionId()
   :outertype: WasdiLib

   Get Session Id

getStandardHeaders
^^^^^^^^^^^^^^^^^^

.. java:method:: protected HashMap<String, String> getStandardHeaders()
   :outertype: WasdiLib

   Get the standard headers for a WASDI call

getStreamingHeaders
^^^^^^^^^^^^^^^^^^^

.. java:method:: protected HashMap<String, String> getStreamingHeaders()
   :outertype: WasdiLib

   Get the headers for a Streming POST call

getUser
^^^^^^^

.. java:method:: public String getUser()
   :outertype: WasdiLib

   Get User

   :return: User

getVerbose
^^^^^^^^^^

.. java:method:: public Boolean getVerbose()
   :outertype: WasdiLib

   Get Verbose Flag

getWorkflows
^^^^^^^^^^^^

.. java:method:: public List<Map<String, Object>> getWorkflows()
   :outertype: WasdiLib

   Get the list of Workflows for the user Return None if there is any error Return an array of WASI Workspace JSON Objects if everything is ok: { "description":STRING, "name": STRING, "workflowId": STRING }

getWorkspaceBaseUrl
^^^^^^^^^^^^^^^^^^^

.. java:method:: public String getWorkspaceBaseUrl()
   :outertype: WasdiLib

getWorkspaceIdByName
^^^^^^^^^^^^^^^^^^^^

.. java:method:: public String getWorkspaceIdByName(String sWorkspaceName)
   :outertype: WasdiLib

   Get Id of a Workspace from the name Return the WorkspaceId as a String, "" if there is any error

   :param sWorkspaceName: Workspace Name
   :return: Workspace Id if found, "" if there is any error

getWorkspaceOwnerByName
^^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public String getWorkspaceOwnerByName(String sWorkspaceName)
   :outertype: WasdiLib

   Get User Id of the owner of a Workspace from the name Return the userId as a String, "" if there is any error

   :param sWorkspaceName: Workspace Name
   :return: User Id if found, "" if there is any error

getWorkspaceOwnerByWSId
^^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public String getWorkspaceOwnerByWSId(String sWorkspaceId)
   :outertype: WasdiLib

   Get userId of the owner of a Workspace from the workspace Id Return the userId as a String, "" if there is any error

   :param WorkspaceId: Workspace Id
   :return: userId if found, "" if there is any error

getWorkspaces
^^^^^^^^^^^^^

.. java:method:: public List<Map<String, Object>> getWorkspaces()
   :outertype: WasdiLib

   get the list of workspaces of the logged user

   :return: List of Workspace as JSON representation

httpGet
^^^^^^^

.. java:method:: public String httpGet(String sUrl, Map<String, String> asHeaders)
   :outertype: WasdiLib

   Http get Method Helper

   :param sUrl: Url to call
   :param asHeaders: Headers Dictionary
   :return: Server response

httpPost
^^^^^^^^

.. java:method:: public String httpPost(String sUrl, String sPayload, Map<String, String> asHeaders)
   :outertype: WasdiLib

   Standard http post uility function

   :param sUrl: url to call
   :param sPayload: payload of the post
   :param asHeaders: headers dictionary
   :return: server response

importProduct
^^^^^^^^^^^^^

.. java:method:: public String importProduct(Map<String, Object> oProduct)
   :outertype: WasdiLib

   Import a Product from a Provider in WASDI.

   :param oProduct: Product Map JSON representation as returned by searchEOImage
   :return: status of the Import process

importProduct
^^^^^^^^^^^^^

.. java:method:: public String importProduct(String sFileUrl)
   :outertype: WasdiLib

   Import a Product from a Provider in WASDI.

   :param sFileUrl: Direct link of the product
   :return: status of the Import process

importProduct
^^^^^^^^^^^^^

.. java:method:: public String importProduct(String sFileUrl, String sBoundingBox)
   :outertype: WasdiLib

   Import a Product from a Provider in WASDI.

   :param sFileUrl: Direct link of the product
   :param sBoundingBox: Bounding Box of the product
   :return: status of the Import process

init
^^^^

.. java:method:: public Boolean init(String sConfigFilePath)
   :outertype: WasdiLib

   Init the WASDI Library starting from a configuration file

   :param sConfigFilePath: full path of the configuration file
   :return: True if the system is initialized, False if there is any error

init
^^^^

.. java:method:: public Boolean init()
   :outertype: WasdiLib

internalAddFileToWASDI
^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: protected String internalAddFileToWASDI(String sFileName, Boolean bAsynch)
   :outertype: WasdiLib

   Private version of the add file to wasdi function. Adds a generated file to current open workspace

   :param sFileName: File Name to add to the open workspace
   :param bAsynch: true if the process has to be asynch, false to wait for the result

internalExecuteWorkflow
^^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: protected String internalExecuteWorkflow(String[] asInputFileNames, String[] asOutputFileNames, String sWorkflowName, Boolean bAsynch)
   :outertype: WasdiLib

   Internal execute workflow

   :param asInputFileNames:
   :param asOutputFileNames:
   :param sWorkflowName:
   :param bAsynch: true if asynch, false for synch
   :return: if Asynch, the process Id else the ouput status of the workflow process

internalInit
^^^^^^^^^^^^

.. java:method:: public Boolean internalInit()
   :outertype: WasdiLib

   Call this after base parameters settings to init the system Needed at least: Base Path User Password or SessionId

internalMosaic
^^^^^^^^^^^^^^

.. java:method:: protected String internalMosaic(boolean bAsynch, List<String> asInputFiles, String sOutputFile)
   :outertype: WasdiLib

   Protected Mosaic with minimum parameters

   :param bAsynch: True to return after the triggering, False to wait the process to finish
   :param asInputFiles: List of input files to mosaic
   :param sOutputFile: Name of the mosaic output file
   :return: Process id or end status of the process

internalMosaic
^^^^^^^^^^^^^^

.. java:method:: protected String internalMosaic(boolean bAsynch, List<String> asInputFiles, String sOutputFile, String sNoDataValue, String sInputIgnoreValue)
   :outertype: WasdiLib

   Protected Mosaic with also nodata value parameters

   :param bAsynch: True to return after the triggering, False to wait the process to finish
   :param asInputFiles: List of input files to mosaic
   :param sOutputFile: Name of the mosaic output file
   :param sNoDataValue: Value to use in output as no data
   :param sInputIgnoreValue: Value to use as input no data
   :return: Process id or end status of the process

internalMosaic
^^^^^^^^^^^^^^

.. java:method:: protected String internalMosaic(boolean bAsynch, List<String> asInputFiles, String sOutputFile, String sNoDataValue, String sInputIgnoreValue, List<String> asBands)
   :outertype: WasdiLib

   Protected Mosaic with also Bands Parameters

   :param bAsynch: True to return after the triggering, False to wait the process to finish
   :param asInputFiles: List of input files to mosaic
   :param sOutputFile: Name of the mosaic output file
   :param asBands: List of the bands to use for the mosaic
   :return: Process id or end status of the process

internalMosaic
^^^^^^^^^^^^^^

.. java:method:: protected String internalMosaic(boolean bAsynch, List<String> asInputFiles, String sOutputFile, String sNoDataValue, String sInputIgnoreValue, List<String> asBands, double dPixelSizeX, double dPixelSizeY)
   :outertype: WasdiLib

   Protected Mosaic with also Pixel Size Parameters

   :param bAsynch: True to return after the triggering, False to wait the process to finish
   :param asInputFiles: List of input files to mosaic
   :param sOutputFile: Name of the mosaic output file
   :param asBands: List of the bands to use for the mosaic
   :param dPixelSizeX: X Pixel Size
   :param dPixelSizeY: Y Pixel Size
   :return: Process id or end status of the process

internalMosaic
^^^^^^^^^^^^^^

.. java:method:: protected String internalMosaic(boolean bAsynch, List<String> asInputFiles, String sOutputFile, String sNoDataValue, String sInputIgnoreValue, List<String> asBands, double dPixelSizeX, double dPixelSizeY, String sCrs)
   :outertype: WasdiLib

   Protected Mosaic with also CRS Input

   :param bAsynch: True to return after the triggering, False to wait the process to finish
   :param asInputFiles: List of input files to mosaic
   :param sOutputFile: Name of the mosaic output file
   :param asBands: List of the bands to use for the mosaic
   :param dPixelSizeX: X Pixel Size
   :param dPixelSizeY: Y Pixel Size
   :param sCrs: WKT of the CRS to use
   :return: Process id or end status of the process

internalMosaic
^^^^^^^^^^^^^^

.. java:method:: protected String internalMosaic(boolean bAsynch, List<String> asInputFiles, String sOutputFile, String sNoDataValue, String sInputIgnoreValue, List<String> asBands, double dPixelSizeX, double dPixelSizeY, String sCrs, double dSouthBound, double dNorthBound, double dEastBound, double dWestBound, String sOverlappingMethod, boolean bShowSourceProducts, String sElevationModelName, String sResamplingName, boolean bUpdateMode, boolean bNativeResolution, String sCombine)
   :outertype: WasdiLib

   Protected Mosaic with all the input parameters

   :param bAsynch: True to return after the triggering, False to wait the process to finish
   :param asInputFiles: List of input files to mosaic
   :param sOutputFile: Name of the mosaic output file
   :param asBands: List of the bands to use for the mosaic
   :param dPixelSizeX: X Pixel Size
   :param dPixelSizeY: Y Pixel Size
   :param sCrs: WKT of the CRS to use
   :param dSouthBound: South Bound
   :param dNorthBound: North Bound
   :param dEastBound: East Bound
   :param dWestBound: West Bound
   :param sOverlappingMethod: Overlapping Method
   :param bShowSourceProducts: Show Source Products Flag
   :param sElevationModelName: DEM Model Name
   :param sResamplingName: Resampling Method Name
   :param bUpdateMode: Update Mode Flag
   :param bNativeResolution: Native Resolution Flag
   :param sCombine: Combine verb
   :return: Process id or end status of the process

log
^^^

.. java:method:: protected void log(String sLog)
   :outertype: WasdiLib

   Log

   :param sLog: Log row

login
^^^^^

.. java:method:: public String login(String sUser, String sPassword)
   :outertype: WasdiLib

   Call Login API

   :param sUser:
   :param sPassword:

mosaic
^^^^^^

.. java:method:: public String mosaic(List<String> asInputFiles, String sOutputFile)
   :outertype: WasdiLib

   Mosaic with minimum parameters: input and output files

   :param asInputFiles: List of input files to mosaic
   :param sOutputFile: Name of the mosaic output file
   :return: End status of the process

mosaic
^^^^^^

.. java:method:: public String mosaic(List<String> asInputFiles, String sOutputFile, String sNoDataValue, String sInputIgnoreValue)
   :outertype: WasdiLib

   Mosaic with also NoData Parameters

   :param asInputFiles: List of input files to mosaic
   :param sOutputFile: Name of the mosaic output file
   :param sNoDataValue: Value to use in output as no data
   :param sInputIgnoreValue: Value to use as input no data
   :return: End status of the process

mosaic
^^^^^^

.. java:method:: public String mosaic(List<String> asInputFiles, String sOutputFile, String sNoDataValue, String sInputIgnoreValue, List<String> asBands)
   :outertype: WasdiLib

   Mosaic with also Bands Parameters

   :param asInputFiles: List of input files to mosaic
   :param sOutputFile: Name of the mosaic output file
   :param asBands: List of the bands to use for the mosaic
   :return: End status of the process

mosaic
^^^^^^

.. java:method:: public String mosaic(List<String> asInputFiles, String sOutputFile, String sNoDataValue, String sInputIgnoreValue, List<String> asBands, double dPixelSizeX, double dPixelSizeY)
   :outertype: WasdiLib

   Mosaic with also Pixel Size Parameters

   :param asInputFiles: List of input files to mosaic
   :param sOutputFile: Name of the mosaic output file
   :param asBands: List of the bands to use for the mosaic
   :param dPixelSizeX: X Pixel Size
   :param dPixelSizeY: Y Pixel Size
   :return: End status of the process

mosaic
^^^^^^

.. java:method:: public String mosaic(List<String> asInputFiles, String sOutputFile, String sNoDataValue, String sInputIgnoreValue, List<String> asBands, double dPixelSizeX, double dPixelSizeY, String sCrs)
   :outertype: WasdiLib

   Mosaic with also CRS Input

   :param asInputFiles: List of input files to mosaic
   :param sOutputFile: Name of the mosaic output file
   :param asBands: List of the bands to use for the mosaic
   :param dPixelSizeX: X Pixel Size
   :param dPixelSizeY: Y Pixel Size
   :param sCrs: WKT of the CRS to use
   :return: End status of the process

mosaic
^^^^^^

.. java:method:: public String mosaic(List<String> asInputFiles, String sOutputFile, String sNoDataValue, String sInputIgnoreValue, List<String> asBands, double dPixelSizeX, double dPixelSizeY, String sCrs, double dSouthBound, double dNorthBound, double dEastBound, double dWestBound, String sOverlappingMethod, boolean bShowSourceProducts, String sElevationModelName, String sResamplingName, boolean bUpdateMode, boolean bNativeResolution, String sCombine)
   :outertype: WasdiLib

   Mosaic with all the input parameters

   :param asInputFiles: List of input files to mosaic
   :param sOutputFile: Name of the mosaic output file
   :param asBands: List of the bands to use for the mosaic
   :param dPixelSizeX: X Pixel Size
   :param dPixelSizeY: Y Pixel Size
   :param sCrs: WKT of the CRS to use
   :param dSouthBound: South Bound
   :param dNorthBound: North Bound
   :param dEastBound: East Bound
   :param dWestBound: West Bound
   :param sOverlappingMethod: Overlapping Method
   :param bShowSourceProducts: Show Source Products Flag
   :param sElevationModelName: DEM Model Name
   :param sResamplingName: Resampling Method Name
   :param bUpdateMode: Update Mode Flag
   :param bNativeResolution: Native Resolution Flag
   :param sCombine: Combine verb
   :return: End status of the process

openWorkspace
^^^^^^^^^^^^^

.. java:method:: public String openWorkspace(String sWorkspaceName)
   :outertype: WasdiLib

   Open a workspace

   :param sWorkspaceName: Workspace name to open
   :return: WorkspaceId as a String, '' if there is any error

refreshParameters
^^^^^^^^^^^^^^^^^

.. java:method:: public void refreshParameters()
   :outertype: WasdiLib

   Refresh Parameters reading again the file

searchEOImages
^^^^^^^^^^^^^^

.. java:method:: public List<Map<String, Object>> searchEOImages(String sPlatform, String sDateFrom, String sDateTo, Double dULLat, Double dULLon, Double dLRLat, Double dLRLon, String sProductType, Integer iOrbitNumber, String sSensorOperationalMode, String sCloudCoverage)
   :outertype: WasdiLib

   Search EO-Images

   :param sPlatform: Satellite Platform. Accepts "S1","S2"
   :param sDateFrom: Starting date in format "YYYY-MM-DD"
   :param sDateTo: End date in format "YYYY-MM-DD"
   :param dULLat: Upper Left Lat Coordinate. Can be null.
   :param dULLon: Upper Left Lon Coordinate. Can be null.
   :param dLRLat: Lower Right Lat Coordinate. Can be null.
   :param dLRLon: Lower Right Lon Coordinate. Can be null.
   :param sProductType: Product Type. If Platform = "S1" -> Accepts "SLC","GRD", "OCN". If Platform = "S2" -> Accepts "S2MSI1C","S2MSI2Ap","S2MSI2A". Can be null.
   :param iOrbitNumber: Sentinel Orbit Number. Can be null.
   :param sSensorOperationalMode: Sensor Operational Mode. ONLY for S1. Accepts -> "SM", "IW", "EW", "WV". Can be null. Ignored for Platform "S2"
   :param sCloudCoverage: Cloud Coverage. Sample syntax: [0 TO 9.4]
   :return: List of the available products as a LIST of Dictionary representing JSON Object: { footprint =  id =  link =  provider =  Size =  title =  properties = < Another JSON Object containing other product-specific info > }

setActiveWorkspace
^^^^^^^^^^^^^^^^^^

.. java:method:: public void setActiveWorkspace(String sActiveWorkspace)
   :outertype: WasdiLib

   Set Active Workspace

   :param sActiveWorkspace:

setBasePath
^^^^^^^^^^^

.. java:method:: public void setBasePath(String sBasePath)
   :outertype: WasdiLib

   Get Base Path

   :param sBasePath:

setBaseUrl
^^^^^^^^^^

.. java:method:: public void setBaseUrl(String sBaseUrl)
   :outertype: WasdiLib

   Set Base URl

   :param sBaseUrl:

setDownloadActive
^^^^^^^^^^^^^^^^^

.. java:method:: public void setDownloadActive(Boolean bDownloadActive)
   :outertype: WasdiLib

   Set Download Active Flag

   :param bDownloadActive:

setIsOnServer
^^^^^^^^^^^^^

.. java:method:: public void setIsOnServer(Boolean bIsOnServer)
   :outertype: WasdiLib

   Set is on server flag

   :param bIsOnServer:

setMyProcId
^^^^^^^^^^^

.. java:method:: public void setMyProcId(String sMyProcId)
   :outertype: WasdiLib

   Set My own process ID

   :param m_sMyProcId:

setParametersFilePath
^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public void setParametersFilePath(String sParametersFilePath)
   :outertype: WasdiLib

   Set Parameters file path

   :param sParametersFilePath: parameters file path

setPassword
^^^^^^^^^^^

.. java:method:: public void setPassword(String sPassword)
   :outertype: WasdiLib

   Set Password

   :param sPassword:

setProcessPayload
^^^^^^^^^^^^^^^^^

.. java:method:: public String setProcessPayload(String sProcessId, String sData)
   :outertype: WasdiLib

   Adds output payload to a process

   :param sProcessId:
   :param sData:

setSessionId
^^^^^^^^^^^^

.. java:method:: public void setSessionId(String sSessionId)
   :outertype: WasdiLib

   Set Session Id

   :param sSessionId:

setUser
^^^^^^^

.. java:method:: public void setUser(String sUser)
   :outertype: WasdiLib

   Set User

   :param sUser: User

setVerbose
^^^^^^^^^^

.. java:method:: public void setVerbose(Boolean bVerbose)
   :outertype: WasdiLib

   Set Verbose flag

   :param bVerbose:

setWorkspaceBaseUrl
^^^^^^^^^^^^^^^^^^^

.. java:method:: public void setWorkspaceBaseUrl(String m_sWorkspaceBaseUrl)
   :outertype: WasdiLib

subset
^^^^^^

.. java:method:: public String subset(String sInputFile, String sOutputFile, double dLatN, double dLonW, double dLatS, double dLonE)
   :outertype: WasdiLib

   Make a Subset (tile) of an input image in a specified Lat Lon Rectangle

   :param sInputFile: Name of the input file
   :param sOutputFile: Name of the output file
   :param dLatN: Lat North Coordinate
   :param dLonW: Lon West Coordinate
   :param dLatS: Lat South Coordinate
   :param dLonE: Lon East Coordinate
   :return: Status of the operation

updateProcessStatus
^^^^^^^^^^^^^^^^^^^

.. java:method:: public String updateProcessStatus(String sProcessId, String sStatus, int iPerc)
   :outertype: WasdiLib

   Update the status of a process

   :param sProcessId: Process Id
   :param sStatus: Status to set
   :param iPerc: Progress in %
   :return: updated status as a String or '' if there was any problem

updateProgressPerc
^^^^^^^^^^^^^^^^^^

.. java:method:: public String updateProgressPerc(int iPerc)
   :outertype: WasdiLib

   Update the status of a process

   :param sProcessId: Process Id
   :param sStatus: Status to set
   :param iPerc: Progress in %
   :return: updated status as a String or '' if there was any problem

updateStatus
^^^^^^^^^^^^

.. java:method:: public String updateStatus(String sStatus)
   :outertype: WasdiLib

   Update the status of the current process

   :param sStatus: Status to set
   :param iPerc: Progress in %
   :return: updated status as a String or '' if there was any problem

updateStatus
^^^^^^^^^^^^

.. java:method:: public String updateStatus(String sStatus, int iPerc)
   :outertype: WasdiLib

   Update the status of the current process

   :param sStatus: Status to set
   :param iPerc: Progress in %
   :return: updated status as a String or '' if there was any problem

uploadFile
^^^^^^^^^^

.. java:method:: public void uploadFile(String sFileName)
   :outertype: WasdiLib

   :param sFileName:

waitForResume
^^^^^^^^^^^^^

.. java:method:: protected void waitForResume()
   :outertype: WasdiLib

   Wait for a process to finish

   :param sProcessId:

waitProcess
^^^^^^^^^^^

.. java:method:: public String waitProcess(String sProcessId)
   :outertype: WasdiLib

   Wait for a process to finish

   :param sProcessId:

