C# WasdiLib
==============

.. c#:solution:: C#
   :noindex:

.. java:type:: public class Wasdi

Fields
------
m_sUser
^^^^^^^^^

.. java:field:: private string m_sUser
   :outertype: WasdiLib

Constructors
------------
Wasdi
^^^^^^^^

.. java:constructor:: public Wasdi()
   :outertype: Wasdi

   Self constructor. If there is a config file initializes the class members

Methods
-------

Init
^^^^

.. java:method:: public bool Init(string sConfigFilePath)

   Init the WASDI Library starting from a configuration file.
   If the path is not provided, the application will attempt to use the conventional appsettings.json file.

   :param sConfigFilePath: the name of the file to be added to the open workspace
   :return: True if the system is initialized, False if there is any error

InternalInit
^^^^^^^^^^^^

.. java:method:: public bool InternalInit(string sConfigFilePath)

   Call this after base parameters settings to init the system.
   Needed at least: Base Path, User, Password or SessionId.

   :return: True if the system is initialized, False if there is any error

AddFileToWASDI
^^^^^^^^^^^^^^

.. java:method:: public string AddFileToWASDI(string sFileName)

   Adds a generated file to current open workspace in a synchronous way.

   :param sFileName: the name of the file to be added to the open workspace

   :return: the process Id or empty string in case of any issues

GetDefaultProvider
^^^^^^^^^^^^^^^^^^

.. java:method:: public string GetDefaultProvider()

   Explicit accessor for the defaultProvider property.

   :return: the defaultProvider

SetDefaultProvider
^^^^^^^^^^^^^^^^^^

.. java:method:: public void SetDefaultProvider(string sProvider)

   Explicit mutator for the defaultProvider property.

   :param sProvider: the provider to be used by default

GetUser
^^^^^^^

.. java:method:: public string GetUser()

   Explicit accessor for the user property.

   :return: the user

SetUser
^^^^^^^

.. java:method:: public void SetUser(string sUser)

   Explicit mutator for the user property.

   :param sUser: the user

GetPassword
^^^^^^^^^^^

.. java:method:: public string GetPassword()

   Explicit accessor for the password property.

   :return: the password

SetPassword
^^^^^^^^^^^

.. java:method:: public void SetPassword(string sPassword)

   Explicit mutator for the password property.

   :param sPassword: the password

GetActiveWorkspace
^^^^^^^^^^^^^^^^^^

.. java:method:: public string GetActiveWorkspace()

   Explicit accessor for the activeWorkspace property.

   :return: the activeWorkspace

SetActiveWorkspace
^^^^^^^^^^^^^^^^^^

.. java:method:: public void SetActiveWorkspace(string sNewActiveWorkspaceId)

   Explicit mutator for the activeWorkspace property.
   If the new active workspace is not null, sets also the workspace owner.

   :param sNewActiveWorkspaceId: the new Id of the activeWorkspace

GetSessionId
^^^^^^^^^^^^

.. java:method:: public string GetSessionId()

   Explicit accessor for the sessionId property.

   :return: the sessionId

SetSessionId
^^^^^^^^^^^^

.. java:method:: public void SetSessionId(string sSessionId)

   Explicit mutator for the sessionId property.
   Sets the sessionId only if the input is not null.

   :param sSessionId: the sessionId

GetBaseUrl
^^^^^^^^^^

.. java:method:: public string GetBaseUrl()

   Explicit accessor for the baseUrl property.

   :return: the baseUrl

SetBaseUrl
^^^^^^^^^^

.. java:method:: public void SetBaseUrl(string sBaseUrl)

   Explicit mutator for the baseUrl property.
   Sets the baseUrl only if the input is not null and if it represents a valid URI.

   :param sBaseUrl: the new baseUrl

GetIsOnServer
^^^^^^^^^^^^^

.. java:method:: public bool GetIsOnServer()

   Explicit accessor for the isOnServer property.

   :return: True if the application is deployed on server, False if it is running on local development machine

SetIsOnServer
^^^^^^^^^^^^^

.. java:method:: public void SetIsOnServer(bool bIsOnServer)

   Explicit mutator for the isOnServer property.

   :param bIsOnServer: Indicates whether the application is deployed on server or running on local development machine

GetDownloadActive
^^^^^^^^^^^^^^^^^

.. java:method:: public bool GetDownloadActive()

   Explicit accessor for the downloadActive property.

   :return: the value of the downloadActive flag

SetDownloadActive
^^^^^^^^^^^^^^^^^

.. java:method:: public void SetDownloadActive(bool bDownloadActive)

   Explicit mutator for the downloadActive property.

   :param bDownloadActive: the desired value of the downloadActive flag

GetBasePath
^^^^^^^^^^^

.. java:method:: public string GetBasePath()

   Explicit accessor for the basePath property.

   :return: the basePath

SetBasePath
^^^^^^^^^^^

.. java:method:: public void SetBasePath(string sBasePath)

   Explicit mutator for the basePath property.
   Sets the basePath only if the input is not null and if it represents a valid path and the user has the permissions to read and write.

   :param sBasePath: the new basePath

GetMyProcId
^^^^^^^^^^^

.. java:method:: public string GetMyProcId()

   Explicit accessor for the myProcId property.

   :return: the myProcId

SetMyProcId
^^^^^^^^^^^

.. java:method:: public void SetMyProcId(string sMyProcId)

   Explicit mutator for the myProcId property.
   Set the myProcessId only if the input is not null or empty.

   :param sMyProcId: the value of myProcId

GetVerbose
^^^^^^^^^^

.. java:method:: public bool GetVerbose()

   Explicit accessor for the verbose property.

   :return: the value of the verbose flag

SetVerbose
^^^^^^^^^^

.. java:method:: public void SetVerbose(bool bVerbose)

   Explicit mutator for the verbose property.
   If the verbose flag is set to True, the level of logging is INFORMATION.
   If the verbose flag is set to False, the level of logging is ERROR.

   :param bVerbose: the desired value of the verbose flag

GetParams
^^^^^^^^^

.. java:method:: public Dictionary<string, string> GetParams()

   Get the parameters (except for the user, sessionId and workspaceid).

   :return: the Params dictionary

GetParamsAsJsonString
^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public string GetParamsAsJsonString()

   Get the parameters in Json format.

   :return: the parameters as a Json string

AddParam
^^^^^^^^

.. java:method:: public void AddParam(string sKey, string sParam)

   Add a parameter to the parameters dictionary.

   :param sKey: the new key
   :param sParam: the new value

GetParam
^^^^^^^^

.. java:method:: public string GetParam(string sKey)

   Get a specific parameter from the parameters dictionary.
   If the key is not contained by the dictionary, an empty string is returned.

   :param sKey: the key

   :return: the value corresponding to the key or an empty string

GetParametersFilePath
^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public string GetParametersFilePath()

   Explicit accessor for the parametersFilePath property.

   :return: the parameters file path

SetParametersFilePath
^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public void SetParametersFilePath(string sParametersFilePath)

   Sets the parametersFilePath only if the input is not null and if it represents a valid path.

   :param sParametersFilePath: the parameters file path

CreateSession
^^^^^^^^^^^^^

.. java:method:: public string CreateSession(string sUser, string sPassword)

   Create a new session and return its Id.

   :param sUser: the username
   :param sPassword: the password

   :return: the newly created sessionId

CheckSession
^^^^^^^^^^^^

.. java:method:: public string CheckSession(string sSessionId, string sUser)

   Check the session.

   :param sSessionId: the actual session Id
   :param sUser: the username of the expected user

   :return: True if the actual user is the same as the expected user, false otherwise

GetWorkspaceBaseUrl
^^^^^^^^^^^^^^^^^^^

.. java:method:: public string GetWorkspaceBaseUrl()

   Explicit accessor for the workspaceBaseUrl property.

   :return: the workspace's baseUrl

SetWorkspaceBaseUrl
^^^^^^^^^^^^^^^^^^^

.. java:method:: public void SetWorkspaceBaseUrl(string sWorkspaceBaseUrl)

   Sets the workspace's baseUrl only if the input is not null and if it represents a valid URI.

   :param sWorkspaceBaseUrl: the new baseUrl of the workspace

Hello
^^^^^

.. java:method:: public string Hello()

   Call the hello endpoint and return thre response.

   :return: the response of the server or null in case of any error

GetWorkspaces
^^^^^^^^^^^^^

.. java:method:: public List<Workspace> GetWorkspaces()

   Get the list of workspaces of the logged user.

   :return: the list of workspaces or null in case of any error

GetWorkspacesNames
^^^^^^^^^^^^^^^^^^

.. java:method:: public List<string> GetWorkspaces()

   Get the list of workspaces' names of the logged user.

   :return: the list of workspaces' names or an empty list in case of any error

GetWorkspaceIdByName
^^^^^^^^^^^^^^^^^^^^

.. java:method:: public string GetWorkspaceIdByName(string sWorkspaceName)

   Get the Id of a workspace identified by name.

   :param sWorkspaceName: the name of the workspace

   :return: the Id of the workspace or an empty string in case of an error or if there is no workspace with the name indicated

GetWorkspaceNameById
^^^^^^^^^^^^^^^^^^^^

.. java:method:: public string GetWorkspaceNameById(string sWorkspacesId)

   Get the name of a workspace identified by Id.

   :param sWorkspacesId: the Id of the workspace

   :return: the name of the workspace or an empty string in case of an error or if there is no workspace with the Id indicated

GetWorkspaceOwnerByName
^^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public string GetWorkspaceOwnerByName(string sWorkspacesId)

   Get the userId of the owner of a workspace identified by name.

   :param sWorkspacesId: the name of the workspace

   :return: the user Id of the workspace's owner or an empty string in case of an error or if there is no workspace with the name indicated

GetWorkspaceOwnerByWSId
^^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public string GetWorkspaceOwnerByWSId(string sWorkspaceId)

   Get the userId of the owner of a workspace identified by Id.

   :param sWorkspaceId: the Id of the workspace

   :return: the user Id of the workspace's owner or an empty string in case of an error or if there is no workspace with the Id indicated

GetWorkspaceUrlByWsId
^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public string GetWorkspaceUrlByWsId(string sWorkspaceId)

   Get the workspace's URL of a workspace identified by Id.

   :param sWorkspaceId: the Id of the workspace

   :return: the workspace's URL or an empty string in case of an error or if there is no workspace with the Id indicated

OpenWorkspaceById
^^^^^^^^^^^^^^^^^

.. java:method:: public string OpenWorkspaceById(string sWorkspaceId)

   Open a workspace given its Id.

   :param sWorkspaceId: the Id of the workspace

   :return: the workspace Id if opened successfully, empty string otherwise

OpenWorkspace
^^^^^^^^^^^^^

.. java:method:: public string OpenWorkspace(string sWorkspaceName)

   Open a workspace.

   :param sWorkspaceName: Workspace name to open

   :return: the workspace Id if opened successfully, empty string otherwise

GetProductsByWorkspace
^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public List<string> GetProductsByWorkspace(string sWorkspaceName)

   Get a List of the products in a workspace.

   :param sWorkspaceName: the name of the workspace

   :return: List of Strings representing the product names

GetProductsByWorkspaceId
^^^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public List<string> GetProductsByWorkspaceId(string sWorkspaceId)

   Get a List of the products in a Workspace

   :param sWorkspaceId: the Id of the workspace

   :return: List of Strings representing the product names

GetProductsByActiveWorkspace
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public List<string> GetProductsByWorkspaceId()

   Get a List of the products in the active workspace

   :return: List of Strings representing the product names

GetProductName
^^^^^^^^^^^^^^

.. java:method:: public string GetProductName(Dictionary<string, object> oProduct)

   Get the name of the product provided.
   For the names starting with S1 or S2, add the .zip extension in case it is missing.

   :param oProduct: the product

   :return: the name of the product or null in case of any error

ImportAndPreprocessWithLinks
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public void ImportAndPreprocessWithLinks(List<string> asProductsLink, List<string> asProductsNames, string sWorkflow, string sPreProcSuffix)

   Import and pre-process with links.

   :param asProductsLink: the list of product links
   :param asProductsNames: the list of product names
   :param sWorkflow: the workflow
   :param sPreProcSuffix: the pre-process suffix

ImportAndPreprocessWithLinks
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public void ImportAndPreprocessWithLinks(List<string> asProductsLink, List<string> asProductsNames, string sWorkflow, string sPreProcSuffix, string sProvider)

   Import and pre-process with links.

   :param asProductsLink: the list of product links
   :param asProductsNames: the list of product names
   :param sWorkflow: the workflow
   :param sPreProcSuffix: the pre-process suffix
   :param sProvider: the provider

ImportAndPreprocess
^^^^^^^^^^^^^^^^^^^

.. java:method:: public void ImportAndPreprocess(List<Dictionary<string, object>> aoProductsToImport, string sWorkflow, string sPreProcSuffix)

   Import and pre-process.

   :param aoProductsToImport: the list of products
   :param sWorkflow: the workflow
   :param sPreProcSuffix: the pre-process suffix

ImportAndPreprocess
^^^^^^^^^^^^^^^^^^^

.. java:method:: public void ImportAndPreprocess(List<Dictionary<string, object>> aoProductsToImport, string sWorkflow, string sPreProcSuffix, string sProvider)

   Import and pre-process.

   :param aoProductsToImport: the list of products
   :param sWorkflow: the workflow
   :param sPreProcSuffix: the pre-process suffix
   :param sProvider: the provider

AsynchPreprocessProductsOnceDownloaded
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public List<string> AsynchPreprocessProductsOnceDownloaded(List<Dictionary<string, object>> aoProductsToImport, string sWorkflow, string sPreProcSuffix, List<string> asDownloadIds)

   Asynchronously pre-process products once ther are downloaded.

   :param aoProductsToImport: the list of products
   :param sWorkflow: the workflow
   :param sPreProcSuffix: the pre-process suffix
   :param asDownloadIds: the list of downloads Ids

   :return: the list of workflow ids

AsynchPreprocessProductsOnceDownloadedWithNames
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public List<string> AsynchPreprocessProductsOnceDownloadedWithNames(List<string> asProductsNames, string sWorkflow, string sPreProcSuffix, List<string> asDownloadIds)

   Asynchronously pre-process products once ther are downloaded and names are provided.

   :param asProductsNames: the list of product names
   :param sWorkflow: the workflow
   :param sPreProcSuffix: the pre-process suffix
   :param asDownloadIds: the list of downloads Ids

   :return: the list of workflow ids

GetPath
^^^^^^^

.. java:method:: public string GetPath(string sProductName)

   Get the local path of a file.

   :param sProductName: the name of the file

   :return: the full local path

InternalGetFullProductPath
^^^^^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public string InternalGetFullProductPath(string sProductName)

   Get the full local path of a product given the product name. Use the output of this API to open the file.

   :param sProductName: the product name

   :return: the product's full path as a String ready to open file

GetSavePath
^^^^^^^^^^^

.. java:method:: public string GetSavePath()

   Get the local Save Path to use to save custom generated files.

   :return: the local path to use to save a custom generated file

GetWorkflows
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public List<Workflow> GetWorkflows()

   Get the list of workflows for the user.

   :return: the list of the workflows or null in case of any error

AsynchExecuteWorkflow
^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public string AsynchExecuteWorkflow(List<string> asInputFileNames, List<string> asOutputFileNames, string sWorkflowName)

   Executes a WASDI SNAP Workflow in a asynch mode.

   :param asInputFileNames: the list of input file names
   :param asOutputFileNames: the list of output file names
   :param sWorkflowName: the workflow's name

   :return: the Id of the workflow process or empty string in case of any issue

ExecuteWorkflow
^^^^^^^^^^^^^^^

.. java:method:: public string AsynchExecuteWorkflow(List<string> asInputFileNames, List<string> asOutputFileNames, string sWorkflowName)

   Executes a WASDI SNAP Workflow waiting for the process to finish.

   :param asInputFileNames: the list of input file names
   :param asOutputFileNames: the list of output file names
   :param sWorkflowName: the workflow's name

   :return: output status of the Workflow Process

GetProcessStatus
^^^^^^^^^^^^^^^^

.. java:method:: public string GetProcessStatus(string sProcessId)

   Get WASDI Process Status.

   :param sProcessId: the process's Id

   :return: process Status as a String: CREATED,  RUNNING,  STOPPED,  DONE,  ERROR, WAITING, READY

GetProcessesStatus
^^^^^^^^^^^^^^^^^^

.. java:method:: public string GetProcessesStatus(List<string> asIds)

   Get the status of a List of WASDI processes.

   :param asIds: the list of processes Ids

   :return: Process Status as a String: CREATED,  RUNNING,  STOPPED,  DONE,  ERROR, WAITING, READY

GetProcessesStatusAsList
^^^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public string GetProcessesStatusAsList(List<string> asIds)

   Get the status of a List of WASDI processes.

   :param asIds: the list of processes Ids

   :return: Process Status as a String: CREATED,  RUNNING,  STOPPED,  DONE,  ERROR, WAITING, READY

UpdateStatus
^^^^^^^^^^^^

.. java:method:: public string UpdateStatus(string sStatus)

   Update the status of the current process.

   :param sStatus: the status to set

   :return: updated status as a String or empty string in case of any issue

UpdateStatus
^^^^^^^^^^^^

.. java:method:: public string UpdateStatus(string sStatus, int iPerc)

   Update the status of the current process.

   :param sStatus: the status to set
   :param iPerc: the progress in %

   :return: updated status as a String or empty string in case of any issue

UpdateProcessStatus
^^^^^^^^^^^^^^^^^^^

.. java:method:: public string UpdateProcessStatus(string sProcessId, string sStatus, int iPerc)

   Update the status of a process.

   :param sProcessId: the process Id
   :param sStatus: the status to set
   :param iPerc: the progress in %

   :return: updated status as a String or empty string in case of any issue

UpdateProgressPerc
^^^^^^^^^^^^^^^^^^^

.. java:method:: public string UpdateProgressPerc(int iPerc)

   Update the status of a process.

   :param iPerc: the progress in %

   :return: updated status as a String or empty string in case of any issue

WaitProcess
^^^^^^^^^^^

.. java:method:: public string WaitProcess(string sProcessId)

   Wait for a process to finish.

   :param sProcessId: the process Id

   :return: the process status

WaitProcesses
^^^^^^^^^^^^^

.. java:method:: public  List<string> WaitProcesses(List<string> asIds)

   Wait for a collection of processes to finish.

   :param asIds: the list of processes Ids

   :return: the list of process statuses

SetPayload
^^^^^^^^^^

.. java:method:: public void SetPayload(string sData)

   Set the payload of the current process (only if the input is not null or empty).

   :param sData: the payload as a String. JSON format recommended

SetProcessPayload
^^^^^^^^^^^^^^^^^

.. java:method:: public string SetProcessPayload(string sProcessId, string sData)

   Set the payload of the current process (only if the input is not null or empty).

   :param sProcessId: the process Id
   :param sData: the payload as a String. JSON format recommended

   :return: the status of the process or empty string in case of any issues

RefreshParameters
^^^^^^^^^^^^^^^^^

.. java:method:: public void RefreshParameters()

   Refresh Parameters reading again the file.

AddFileToWASDI
^^^^^^^^^^^^^^

.. java:method:: public string AddFileToWASDI(string sFileName, string sStyle)

   Adds a generated file to current open workspace in synchronous way.

   :param sFileName: the name of the file to be added to the open workspace
   :param sStyle: name of a valid WMS style

   :return: the process Id or empty string in case of any issues

AsynchAddFileToWASDI
^^^^^^^^^^^^^^^^^^^^

.. java:method:: public string AsynchAddFileToWASDI(string sFileName, string sStyle)

   Adds a generated file to current open workspace in asynchronous way.

   :param sFileName: the name of the file to be added to the open workspace
   :param sStyle: name of a valid WMS style

   :return: the process Id or empty string in case of any issues

AddFileToWASDI
^^^^^^^^^^^^^^

.. java:method:: public string AddFileToWASDI(string sFileName)

   Adds a generated file to current open workspace in synchronous way.

   :param sFileName: the name of the file to be added to the open workspace

   :return: the process Id or empty string in case of any issues

AsynchAddFileToWASDI
^^^^^^^^^^^^^^^^^^^^

.. java:method:: public string AsynchAddFileToWASDI(string sFileName)

   Adds a generated file to current open workspace in asynchronous way.

   :param sFileName: the name of the file to be added to the open workspace

   :return: the process Id or empty string in case of any issues

Mosaic
^^^^^^

.. java:method:: public string Mosaic(List<string> asInputFiles, string sOutputFile)

   Mosaic with minimum parameters: input and output files.

   :param asInputFiles: the list of input files to mosaic
   :param sOutputFile: the name of the mosaic output file

   :return: the end status of the process

Mosaic
^^^^^^

.. java:method:: public string Mosaic(List<string> asInputFiles, string sOutputFile, string sNoDataValue, string sInputIgnoreValue)

   Mosaic with minimum parameters: input and output files.

   :param asInputFiles: the list of input files to mosaic
   :param sOutputFile: the name of the mosaic output file
   :param sNoDataValue: the value to use in output as no data
   :param sInputIgnoreValue: the value to use as input no data

   :return: the end status of the process

Mosaic
^^^^^^

.. java:method:: public string Mosaic(List<string> asInputFiles, string sOutputFile, string sNoDataValue, string sInputIgnoreValue, double dPixelSizeX, double dPixelSizeY)

   Mosaic with minimum parameters: input and output files.

   :param asInputFiles: the list of input files to mosaic
   :param sOutputFile: the name of the mosaic output file
   :param sNoDataValue: the value to use in output as no data
   :param sInputIgnoreValue: the value to use as input no data
   :param dPixelSizeX: the X Pixel Size
   :param dPixelSizeY: the Y Pixel Size

   :return: the end status of the process

AsynchMosaic
^^^^^^^^^^^^

.. java:method:: public string Mosaic(List<string> asInputFiles, string sOutputFile)

   Asynch Mosaic with minimum parameters: input and output files.

   :param asInputFiles: the list of input files to mosaic
   :param sOutputFile: the name of the mosaic output file

   :return: the end status of the process

AsynchMosaic
^^^^^^^^^^^^

.. java:method:: public string Mosaic(List<string> asInputFiles, string sOutputFile, string sNoDataValue, string sInputIgnoreValue)

   Asynch Mosaic with minimum parameters: input and output files.

   :param asInputFiles: the list of input files to mosaic
   :param sOutputFile: the name of the mosaic output file
   :param sNoDataValue: the value to use in output as no data
   :param sInputIgnoreValue: the value to use as input no data

   :return: the end status of the process

AsynchMosaic
^^^^^^^^^^^^

.. java:method:: public string Mosaic(List<string> asInputFiles, string sOutputFile, string sNoDataValue, string sInputIgnoreValue, double dPixelSizeX, double dPixelSizeY)

   Asynch Mosaic with minimum parameters: input and output files.

   :param asInputFiles: the list of input files to mosaic
   :param sOutputFile: the name of the mosaic output file
   :param sNoDataValue: the value to use in output as no data
   :param sInputIgnoreValue: the value to use as input no data
   :param dPixelSizeX: the X Pixel Size
   :param dPixelSizeY: the Y Pixel Size

   :return: the end status of the process

SearchEOImages
^^^^^^^^^^^^^^

.. java:method:: public List<QueryResult> SearchEOImages(string sPlatform, string sDateFrom, string sDateTo, Double dULLat, Double dULLon, Double dLRLat, Double dLRLon, string sProductType, int iOrbitNumber, string sSensorOperationalMode, string sCloudCoverage)

   Search EO-Images

   :param sPlatform: the Satellite Platform. Accepts "S1","S2","S3","S5P","ENVI","L8","VIIRS","ERA5"
   :param sDateFrom: the Starting date in format "YYYY-MM-DD"
   :param sDateTo: the End date in format "YYYY-MM-DD"
   :param dULLat: Upper Left Lat Coordinate. Can be null.
   :param dULLon: Upper Left Lon Coordinate. Can be null.
   :param dLRLat: Lower Right Lat Coordinate. Can be null.
   :param dLRLon: Lower Right Lon Coordinate. Can be null.
   :param sProductType: the Product Type. If Platform = "S1" -> Accepts "SLC","GRD", "OCN". If Platform = "S2" -> Accepts "S2MSI1C","S2MSI2Ap","S2MSI2A". Can be null.
   :param iOrbitNumber: the Sentinel Orbit Number. Can be null.
   :param sSensorOperationalMode: the Sensor Operational Mode. ONLY for S1. Accepts -> "SM", "IW", "EW", "WV". Can be null. Ignored for Platform "S2"
   :param sCloudCoverage: the Cloud Coverage. Sample syntax: [0 TO 9.4]

   :return: the list of the available products

GetFoundProductName
^^^^^^^^^^^^^^^^^^^

.. java:method:: public string GetFoundProductName(QueryResult oProduct)

   Get the name of a Product found by searchEOImage.

   :param oProduct: the Product as returned by searchEOImage

   :return: the name of the product

GetFoundProductName
^^^^^^^^^^^^^^^^^^^

.. java:method:: public string GetFoundProductName(Dictionary<string, object> oProduct)

   Get the name of a Product found by searchEOImage.

   :param oProduct: the JSON Dictionary Product as returned by searchEOImage

   :return: the name of the product

GetFoundProductLink
^^^^^^^^^^^^^^^^^^^

.. java:method:: public string GetFoundProductLink(QueryResult oProduct)

   Get the direct download link of a Product found by searchEOImage.

   :param oProduct: the Product as returned by searchEOImage

   :return: the link of the product

GetFoundProductLink
^^^^^^^^^^^^^^^^^^^

.. java:method:: public string GetFoundProductLink(Dictionary<string, object> oProduct)

   Get the direct download link of a Product found by searchEOImage.

   :param oProduct: the JSON Dictionary Product as returned by searchEOImage

   :return: the link of the product

GetFoundProductFootprint
^^^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public string GetFoundProductFootprint(QueryResult oProduct)

   Get the footprint of a Product found by searchEOImage.

   :param oProduct: the Product as returned by searchEOImage

   :return: the footprint of the product

GetFoundProductFootprint
^^^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public string GetFoundProductFootprint(Dictionary<string, object> oProduct)

   Get the footprint of a Product found by searchEOImage.

   :param oProduct: the JSON Dictionary Product as returned by searchEOImage

   :return: the footprint of the product

AsynchImportProduct
^^^^^^^^^^^^^^^^^^^

.. java:method:: public string AsynchImportProduct(Dictionary<string, object> oProduct)

   Asynchronously import a product.

   :param oProduct: the product to be imported

   :return: the status of the Import process

AsynchImportProduct
^^^^^^^^^^^^^^^^^^^

.. java:method:: public string AsynchImportProduct(Dictionary<string, object> oProduct, string sProvider)

   Asynchronously import a product.

   :param oProduct: the product to be imported
   :param sProvider: the provider of choice. If null, the default provider will be used

   :return: the status of the Import process

ImportProduct
^^^^^^^^^^^^^

.. java:method:: public string ImportProduct(Dictionary<string, object> oProduct)

   Import a Product from a Provider in WASDI.

   :param oProduct: the product to be imported

   :return: the status of the Import process

ImportProduct
^^^^^^^^^^^^^

.. java:method:: public string ImportProduct(QueryResult oProduct)

   Import a Product from a Provider in WASDI.

   :param oProduct: the product to be imported

   :return: the status of the Import process

ImportProduct
^^^^^^^^^^^^^

.. java:method:: public string ImportProduct(string sFileUrl, string sFileName)

   Import a Product from a Provider in WASDI.

   :param sFileUrl: the Direct link of the product
   :param sFileName: the name of the file

   :return: the status of the Import process

ImportProduct
^^^^^^^^^^^^^

.. java:method:: public string ImportProduct(string sFileUrl, string sFileName, string sBoundingBox)

   Import a Product from a Provider in WASDI.

   :param sFileUrl: the Direct link of the product
   :param sFileName: the name of the file
   :param sBoundingBox: the bounding box

   :return: the status of the Import process

ImportProduct
^^^^^^^^^^^^^

.. java:method:: public string ImportProduct(string sFileUrl, string sFileName, string sBoundingBox, string sProvider)

   Import a Product from a Provider in WASDI.

   :param sFileUrl: the Direct link of the product
   :param sFileName: the name of the file
   :param sBoundingBox: the bounding box
   :param sProvider: the provider

   :return: the status of the Import process

AsynchImportProduct
^^^^^^^^^^^^^^^^^^^

.. java:method:: public string AsynchImportProduct(string sFileUrl, string sFileName)

   Import a Product from a Provider in WASDI asynchronously.

   :param sFileUrl: the Direct link of the product
   :param sFileName: the name of the file

   :return: the status of the Import process

AsynchImportProduct
^^^^^^^^^^^^^^^^^^^

.. java:method:: public string AsynchImportProduct(string sFileUrl, string sFileName, string sBoundingBox)

   Import a Product from a Provider in WASDI asynchronously.

   :param sFileUrl: the Direct link of the product
   :param sFileName: the name of the file
   :param sBoundingBox: the bounding box

   :return: the status of the Import process

AsynchImportProduct
^^^^^^^^^^^^^^^^^^^

.. java:method:: public string AsynchImportProduct(string sFileUrl, string sFileName, string sBoundingBox, string sProvider)

   Import a Product from a Provider in WASDI asynchronously.

   :param sFileUrl: the Direct link of the product
   :param sFileName: the name of the file
   :param sBoundingBox: the bounding box
   :param sProvider: the provider

   :return: the status of the Import process

AsynchImportProductListWithMaps
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public List<string> AsynchImportProductListWithMaps(List<Dictionary<string, object>> aoProductsToImport)

   Imports a list of product asynchronously.

   :param aoProductsToImport: the list of products to import

   :return: a list of String containing the WASDI process ids of all the imports

AsynchImportProductList
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public List<string> AsynchImportProductList(List<string> asProductsToImport, List<string> asNames)

   Imports a list of product asynchronously.

   :param asProductsToImport: the list of products to import
   :param asNames: the list of names

   :return: a list of String containing the WASDI process ids of all the imports

ImportProductListWithMaps
^^^^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public List<string> ImportProductListWithMaps(List<Dictionary<string, object>> aoProductsToImport)

   Imports a list of product.

   :param aoProductsToImport: the list of products to import

   :return: a list of String containing the WASDI process ids of all the imports

ImportProductList
^^^^^^^^^^^^^^^^^

.. java:method:: public List<string> ImportProductList(List<string> asProductsToImport, List<string> asNames)

   Imports a list of product.

   :param asProductsToImport: the list of products to import
   :param asNames: the list of names

   :return: a list of String containing the WASDI process ids of all the imports

Subset
^^^^^^

.. java:method:: public string Subset(string sInputFile, string sOutputFile, double dLatN, double dLonW, double dLatS, double dLonE)

   Make a Subset (tile) of an input image in a specified Lat Lon Rectangle.

   :param sInputFile: the name of the input file
   :param sOutputFile: the name of the output file
   :param dLatN: the Lat North Coordinate
   :param dLonW: the Lon West Coordinate
   :param dLatS: the Lat South Coordinate
   :param dLonE: the Lon East Coordinate

   :return: the status of the operation

MultiSubset
^^^^^^^^^^^

.. java:method:: public string MultiSubset(string sInputFile, List<string> asOutputFiles, List<Double> adLatN, List<Double> adLonW, List<Double> adLatS, List<Double> adLonE)

   Creates a Many Subsets from an image. MAX 10 TILES PER CALL. Assumes big tiff format by default.

   :param sInputFile: the name of the input file
   :param asOutputFiles: the name of the output file
   :param adLatN: the list of Lat North Coordinates
   :param adLonW: the list of Lon West Coordinates
   :param adLatS: the list of Lat South Coordinates
   :param adLonE: the list of Lon East Coordinates

   :return: the status of the operation

MultiSubset
^^^^^^^^^^^

.. java:method:: public string MultiSubset(string sInputFile, List<string> asOutputFiles, List<Double> adLatN, List<Double> adLonW, List<Double> adLatS, List<Double> adLonE, bool bBigTiff)

   Creates a Many Subsets from an image. MAX 10 TILES PER CALL.

   :param sInputFile: the name of the input file
   :param asOutputFiles: the name of the output file
   :param adLatN: the list of Lat North Coordinates
   :param adLonW: the list of Lon West Coordinates
   :param adLatS: the list of Lat South Coordinates
   :param adLonE: the list of Lon East Coordinates
   :param bBigTiff: flag indicating whether to use the bigtiff format, for files bigger than 4 GB

   :return: the status of the operation

AsynchMultiSubset
^^^^^^^^^^^^^^^^^

.. java:method:: public string MultiSubset(string sInputFile, List<string> asOutputFiles, List<Double> adLatN, List<Double> adLonW, List<Double> adLatS, List<Double> adLonE)

   Asynchronous multisubset: creates a Many Subsets from an image. MAX 10 TILES PER CALL. Assumes big tiff format by default.

   :param sInputFile: the name of the input file
   :param asOutputFiles: the name of the output file
   :param adLatN: the list of Lat North Coordinates
   :param adLonW: the list of Lon West Coordinates
   :param adLatS: the list of Lat South Coordinates
   :param adLonE: the list of Lon East Coordinates

   :return: the status of the operation

AsynchMultiSubset
^^^^^^^^^^^^^^^^^

.. java:method:: public string MultiSubset(string sInputFile, List<string> asOutputFiles, List<Double> adLatN, List<Double> adLonW, List<Double> adLatS, List<Double> adLonE, bool bBigTiff)

   Asynchronous multisubset: creates a Many Subsets from an image. MAX 10 TILES PER CALL.

   :param sInputFile: the name of the input file
   :param asOutputFiles: the name of the output file
   :param adLatN: the list of Lat North Coordinates
   :param adLonW: the list of Lon West Coordinates
   :param adLatS: the list of Lat South Coordinates
   :param adLonE: the list of Lon East Coordinates
   :param bBigTiff: flag indicating whether to use the bigtiff format, for files bigger than 4 GB

   :return: the status of the operation

ExecuteProcessor
^^^^^^^^^^^^^^^^

.. java:method:: public string ExecuteProcessor(string sProcessorName, Dictionary<string, object> aoParams)

   Executes a synchronous process, i.e., runs the process and waits for it to complete.

   :param sProcessorName: the name of the processor
   :param aoParams: the dictionary of params

   :return: the WASDI processor Id

AsynchExecuteProcessor
^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public string AsynchExecuteProcessor(string sProcessorName, Dictionary<string, object> aoParams)

   Execute a WASDI processor in Asynch way.

   :param sProcessorName: the name of the processor
   :param aoParams: the dictionary of params

   :return: the WASDI processor Id

ExecuteProcessor
^^^^^^^^^^^^^^^^

.. java:method:: public string ExecuteProcessor(string sProcessorName, string sEncodedParams)

   Executes a synchronous process, i.e., runs the process and waits for it to complete.

   :param sProcessorName: the name of the processor
   :param sEncodedParams: a JSON formatted string of parameters for the processor

   :return: the WASDI processor Id

AsynchExecuteProcessor
^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public string AsynchExecuteProcessor(string sProcessorName, string sEncodedParams)

   Execute a WASDI processor in Asynch way.

   :param sProcessorName: the name of the processor
   :param sEncodedParams: a JSON formatted string of parameters for the processor

   :return: the WASDI processor Id

DeleteProduct
^^^^^^^^^^^^^

.. java:method:: public string DeleteProduct(string sProduct)

   Delete a Product in the active Workspace.

   :param sProduct: the product's name

   :return: the status of the operation

WasdiLog
^^^^^^^^

.. java:method:: public void WasdiLog(string sLogRow)

   Write one row of Log.

   :param sLogRow: the text to log

GetProcessorPath
^^^^^^^^^^^^^^^^

.. java:method:: public string GetProcessorPath()

   Get the processor Path.
   The value should resemble the following path: C:/dev/WasdiLib.Client/bin/Debug/net6.0

   :return: the processor path

CreateWorkspace
^^^^^^^^^^^^^^^

.. java:method:: public string CreateWorkspace(string sWorkspaceName)

   Create a workspace using the provided name.
   Once the workspace is created, it is also opened.

   :param sWorkspaceName: the name of the workspace

   :return: the Id of the newly created workspace or empty string in case of any issues

CreateWorkspace
^^^^^^^^^^^^^^^

.. java:method:: public string CreateWorkspace(string sWorkspaceName, string nodeCode)

   Create a workspace using the provided name on the indicated node.
   Once the workspace is created, it is also opened.

   :param sWorkspaceName: the name of the workspace
   :param nodeCode: the node on which to create the workspace

   :return: the Id of the newly created workspace or empty string in case of any issues

DeleteWorkspace
^^^^^^^^^^^^^^^

.. java:method:: public string DeleteWorkspace(string workspaceId)

   Deletes the workspace given its Id.

   :param workspaceId: the Id of the workspace

   :return: the Id of the workspace as a String if succesful, empty string otherwise

GetProcessWorkspacesByWorkspaceId
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public List<ProcessWorkspace> GetProcessWorkspacesByWorkspaceId(string workspaceId)

   Get the process workspaces by workspace id

   :param workspaceId: the Id of the workspace

   :return: the list of process workspaces or an empty list in case of any issues

GetProcessesByWorkspaceAsListOfJson
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public List<string> GetProcessesByWorkspaceAsListOfJson(int iStartIndex, Int32 iEndIndex, string sStatus, string sOperationType, string sNamePattern)

   Get a paginated list of processes in the active workspace, each element of which is a JSON string.

   :param iStartIndex: the start index of the process (0 by default is the last one)
   :param iEndIndex: the end index of the process (optional)
   :param sStatus: the status filter, null by default. Can be CREATED,  RUNNING,  STOPPED,  DONE,  ERROR, WAITING, READY
   :param sOperationType: the Operation Type Filter, null by default. Can be RUNPROCESSOR, RUNIDL, RUNMATLAB, INGEST, DOWNLOAD, GRAPH, DEPLOYPROCESSOR
   :param sNamePattern: the Name filter. The name meaning depends by the operation type, null by default. For RUNPROCESSOR, RUNIDL and RUNMATLAB is the name of the application

   :return: a list of process IDs

GetProcessesByWorkspace
^^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public List<string> GetProcessesByWorkspaceAsListOfJson(int iStartIndex, Int32 iEndIndex, string sStatus, string sOperationType, string sNamePattern)

   Get a paginated list of processes in the active workspace.

   :param iStartIndex: the start index of the process (0 by default is the last one)
   :param iEndIndex: the end index of the process (optional)
   :param sStatus: the status filter, null by default. Can be CREATED,  RUNNING,  STOPPED,  DONE,  ERROR, WAITING, READY
   :param sOperationType: the Operation Type Filter, null by default. Can be RUNPROCESSOR, RUNIDL, RUNMATLAB, INGEST, DOWNLOAD, GRAPH, DEPLOYPROCESSOR
   :param sNamePattern: the Name filter. The name meaning depends by the operation type, null by default. For RUNPROCESSOR, RUNIDL and RUNMATLAB is the name of the application

   :return: a list of process IDs

GetProcessorPayload
^^^^^^^^^^^^^^^^^^^

.. java:method:: public Dictionary<string, object> GetProcessorPayload(string sProcessObjId)

   Gets the processor payload as a dictionary.

   :param sProcessObjId: the Id of the processor

   :return: the processor payload as a dictionary

GetProcessorPayloadAsJSON
^^^^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: public Dictionary<string, object> GetProcessorPayloadAsJSON(string sProcessObjId)

   Retrieve the payload of a processor formatted as a JSON string.

   :param sProcessObjId: the Id of the processor

   :return: the payload as a JSON string, or null if error occurred

GetProductBbox
^^^^^^^^^^^^^^

.. java:method:: public string GetProductBbox(string sFileName)

   Get the product bounding box.

   :param sFileName: the file name

   :return: the product bounding box

DownloadFile
^^^^^^^^^^^^

.. java:method:: public string DownloadFile(string sFileName)

   Download a file on the local PC.

   :param sFileName: the name of the file

   :return: the full path of the file

UploadFile
^^^^^^^^^^

.. java:method:: public bool UploadFile(string sFileName)

   Uploads and ingest a file in WASDI.

   :param sFileName: the name of the file to upload

   :return: True if the file was uploaded, False otherwise
   
   :throws Exception: in case of any issues

CopyFileToSftp
^^^^^^^^^^^^^^

.. java:method:: public string CopyFileToSftp(string sFileName)

   Copy a file from a workspace to the WASDI user's SFTP Folder in a synchronous way.

   :param sFileName: the filename to move to the SFTP folder

   :return: the Process Id is synchronous execution, end status otherwise. An empty string is returned in case of failure

CopyFileToSftp
^^^^^^^^^^^^^^

.. java:method:: public string CopyFileToSftp(string sFileName, string sRelativePath)

   Copy a file from a workspace to the WASDI user's SFTP Folder in a synchronous way.

   :param sFileName: the filename to move to the SFTP folder
   :param sRelativePath: the relative path in the SFTP root

   :return: the Process Id is synchronous execution, end status otherwise. An empty string is returned in case of failure

AsynchCopyFileToSftp
^^^^^^^^^^^^^^^^^^^^

.. java:method:: public string CopyFileToSftp(string sFileName)

   Copy a file from a workspace to the WASDI user's SFTP Folder in a asynchronous way.

   :param sFileName: the filename to move to the SFTP folder

   :return: the Process Id is asynchronous execution, end status otherwise. An empty string is returned in case of failure

AsynchCopyFileToSftp
^^^^^^^^^^^^^^^^^^^^

.. java:method:: public string CopyFileToSftp(string sFileName, string sRelativePath)

   Copy a file from a workspace to the WASDI user's SFTP Folder in a asynchronous way.

   :param sFileName: the filename to move to the SFTP folder
   :param sRelativePath: the relative path in the SFTP root

   :return: the Process Id is asynchronous execution, end status otherwise. An empty string is returned in case of failure

SetSubPid
^^^^^^^^^

.. java:method:: public string SetSubPid(string sProcessId, int iSubPid)

   Sets the sub pid.

   :param sProcessId: the process Id
   :param iSubPid: the subPid of the process

   :return: the updated status of the processs

PrintStatus
^^^^^^^^^^^

.. java:method:: public void PrintStatus()

   Print the status information ot the Wasdi application.
