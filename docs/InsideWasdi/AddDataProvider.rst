.. TestReadTheDocs documentation master file, created by
   sphinx-quickstart on Mon Apr 19 16:00:28 2021.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.
.. _AddDataProviderTutorial:

Add a Data Provider to WASDI
==============================

Introduction
---------------------------

A Data Provider is an external service that can be used to query and import Data in WASDI. The main business entities involved in this operation are:

* Platform: this is the type of data. Usually identified as a Satellite Mission. Platorms are for example Sentinel1, Sentinel2, ENVISat etc. Each Platform, in general, can be found in one or more data providers.
* Query Executors / catalogue: the Query executor is the WASDI hierarchy used to query the Data Provider Catalogue
* Provider Adapters: Objects used by the launcher to download/import files from an external service.


.. note::
	This tutorial assume that you already have the WASDI Project up and running in your environment

Getting Started
---------------------------
To start adding a Data Provider the first thing to check is the Platform's support. 
Supported platforms are listed in the the class:

.. code-block:: java

	wasdi.shared.queryexecutors.Platforms

Each platform is represented by a static String that declares the Platform Code. 

In case of a new platform, add also the support to: 

.. code-block:: java

	wasdi.shared.utils.MissionUtils.getPlatformFromSatelliteImageFileName

This method must be able to infer to platform type from the file name.

If the Data Provider support a new Platform, the code must be added in the :code:`Platforms` class.


Client Filter
---------------------------

The client search page is configured using the JSON config file :code:`config/appconfig.json`, which contains an array of "missions" objects. 
Each mission is a JSON similar to this one:

.. code-block:: json

        {
            "name": "S1",
            "indexname": "platformname",
            "indexvalue": "Sentinel-1",
            "selected": true,
            "filters": [
                {
                    "indexname": "filename",
                    "indexlabel": "Satellite Platform",
                    "indexvalues": "S1A_*|S1B_*",
					"indexvalue": "S1A_*"
                    "regex": ".*"

                },
                {
                    "indexname": "producttype",
                    "indexlabel": "Product Type",
                    "indexvalues": "SLC|GRD|OCN",
					"indexvalue": "GRD"
                    "regex": ".*"

                }
            ]
        }

**name** will be used to create the corresponding tab in the WASDI search section.

Filters can be added to the search form of the data provider. Each filter has an **indexname** that represents the name of the filter and 
a **indexvalue** will contain the value of filter selected by the user.

**indexname- indexvalues** is an array used to create a new variable from the client to the server. 

**indexname:"platformname"**  is a filter that *must* be used to set the Platform Code as defined in the Java :code:`Platforms` object.  
Accordingly, the **indexvalue** for a ceratin *platformname* will correspond to the code of the Platform in WASDI. 

You can add as many filters as required/supported by the Data Provider. 

WASDI automatically handles the date interval, the bounding box, platformname and, if supplied, the producttype. 

Other filters can be added and will have to be supported server side by your own QueryTranslator.

Query Executor, Query Translator and Response Translator
---------------------------
This section is needed to make WASDI search the new Data Provider. WASDI receives always the query as string that must be translated in for the provider. Results must then be converted to the WASDI format.

When the user wants to donwload a file, QueryExecutor will pass to the ProviderAdapter the link and the file name that must be imported. 

In general, the name is the key element: since WASDI supports automatic data provider selection, the system will search the highest priority provider adapter that supports that plaform. The Download Operation will use the QueryExecutor to obtain the url to use for the download from the filename. 
Since a platform can be supported by many Data Providers, this method assures to get always the right file, even from different sources.

In the particular situation where a single platform is supported only by One Data Provider, in the name and in the link, the developer can decide to store more complete informations that may be needed to interoperate with the external API.

To create a new QueryExecutor, add a new package in

.. code-block:: java

 wasdi.shared.queryexecutors

Create 3 objects:

* The new QueryExecutor deriving from QueryExecutor
* The new QueryTranslator deriving from QueryTranslator
* The new ResponseTranslator deriving from ResponseTranslator

Query Executor
~~~~~~~~~~~~~~~~~~~~~~

The QueryExecutor MUST define its own unique code in the variable :code:`m_sProvider`, inside the constructor. 
Usually, it must also instantiate its own QueryTranslator and ResponseTranslator in the constructor.

.. code-block:: java

	public QueryExecutorPLANET() {
		m_sProvider="PLANET";
		this.m_oQueryTranslator = new QueryTranslatorPLANET();
		this.m_oResponseTranslator = new ResponseTranslatorPLANET();		
	}

QueryExecutor must implement:

.. code-block:: java

	public int executeCount(String sQuery): receive in input the WASDI query, must return the number of results for the provider
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel): receive in input the WASDI query, must return the list of provider's results  as a list of QueryResultViewModel. 

The boolean parameter :code:`bFullViewModel` taken as an input by the method :code:`executeAndRetrive` depends on the type of search
being executed. Indeed, WASDI supports two types of search:

* the paginated search, used by the WASDI Web client and implemented by the REST endpoint :code:`/search/query`
* the not paginated search, returning the whole list of restults at once, which is used by the WASDI librares and implmented by the REST endpoint :code:`/search/querylist`.

When the type of search being executed is a not-paginated one, the value of the boolean is set to *false*, in order to add only the essential information
to the list of :code:`QueryResultViewModel` objects returned by the method. By contrast, the boolean is set to *true* when the search is paginated. In that case, the 
:code:`QueryResultViewModel` objects will contain exhaustive information about each product returned by the search.

In the :code:`QueryResultViewModel` the most important fields are:

* *title* : name of the file
* *link* : url for the direct download of the file

:code:`QueryExecutor` base class implements:

.. code-block:: java

	public String getUriFromProductName(String sProduct, String sProtocol, String sOriginalUrl) 
	
This method is very important for the auto data provider selection: it takes the name of the product returned by any catalogue that supports that platform, the original url returned by the same catalogue and must return the URI to access the file for the Provider Adapter. 
URI is usually an http link but can be a file path or a ftp link or other, depending on the linked DataProvider that takes the file with that URI in the executeDownloadFile method.

The basic implementantion just performs a query filtering by the exact product name and uses the result to get the relative URI: it MUST be overridden if this does not work.

There are at least 2 QueryExecutors base classes that can be used other than the abstract one:

*QueryExecutorHttpGet*

Each Query Exeuctor that uses standard get http calls, should derive from this class and implement the abstracts methods of QueryTranslator to get Search and Count URL and of Response Translator to convert the return of the search query in WASDI View Models 
:code:`executeCount` steps are:

 * Check if the platform is supported
 * call :code:`QueryTranslator.getCountUrl`
 * execute std http get call with that url
 * call :code:`m_oResponseTranslator.getCountResult` to get the number of results.

:code:`executeAndRetrive` steps are:

 * Check if the platform is supported
 * call :code:`QueryTranslator.getSearchUrl`
 * execute std http get call with that url
 * call :code:`m_oResponseTranslator.translateBatch` to get the number of results.


*QueryExecutorOpenSearch*

Base class for Providers supporting Open Search.

QueryTranslator
~~~~~~~~~~~~~~~~~~~~~~

QueryTranslator has the goal to convert the WASDI query in a valid provider query. The user must implement 2 methods:

.. code-block:: java

	String getCountUrl(String sQuery)
	String getSearchUrl(PaginatedQuery oQuery)

The base class contains the :code:`parseWasdiClientQuery` method.

.. code-block:: java

	QueryViewModel oQuery = parseWasdiClientQuery(sQuery);
	
In its implementation, the WASDI query is parsed and transformed in the corrisponding view model. 
If the Platform or the Data Provider have special filters, these must be supported (parsed) there. 

It is important to CHECK that :code:`parseWasdiClientQuery` is able to detect the :code:`platformName` attribute of :code:`QueryViewModel`, since it is mandatory.

ResponseTranslator
~~~~~~~~~~~~~~~~~~~~~~

The ResponseTranslator must translate the results of the API call made to the data provider into the WASDI format.

.. code-block:: java

	public class ResponseTranslatorPLANET extends ResponseTranslator {

		@Override
		public List<QueryResultViewModel> translateBatch(String sResponse, boolean bFullViewModel) {
			return null;
		}

		@Override
		public int getCountResult(String sQueryResult) {
			return 0;
		}
	}

The WASDI format is a list of :code:`QueryResultViewModel` objects. 
The basic information contained in those objects is:

* *title* : name of the file,
* *summary* : description. Supports a sort of std like: "Date: 2021-12-25T18:25:03.242Z, Instrument: SAR, Mode: IW, Satellite: S1A, Size: 0.95 GB" but is not mandatory,
* *id* : provider unique id,
* *link* : link to download the file,
* *footprint* : bounding box in WKT,
* *provider* : provider used to get this information.

The :code:`QueryResultViewModel` object also contains a field :code:`properties`, which is a dictionary filled with all the properties supported by the data provider.
It can be seen with the "info" button in the client.

Some commonly used properties, shown in the client, are:

* *date* : reference date,
* *Satellite* : platform,
* *instrument* : used instrument,
* *sensorMode* : sensing mode,
* *size* : image size as string,
* *relativeOrbit*: relative orbit of the acquisition.

To add the query executor to WASDI, remember to add it to the factory

.. code-block:: java

	wasdi.shared.queryexecutors.QueryExecutorFactory

.. code-block:: java

	static {
		Utils.debugLog("QueryExecutorFactory");
		final Map<String, Supplier<QueryExecutor>> aoMap = new HashMap<>();

		aoMap.put("ONDA", QueryExecutorONDA::new);
		aoMap.put("SENTINEL", QueryExecutorSENTINEL::new);
		aoMap.put("SOBLOO", QueryExecutorSOBLOO::new);
		aoMap.put("EODC", QueryExecutorEODC::new);
		aoMap.put("CREODIAS", QueryExecutorCREODIAS::new);
		aoMap.put("LSA", QueryExecutorLSA::new);
		aoMap.put("VIIRS", QueryExecutorVIIRS::new);
		aoMap.put("CDS", QueryExecutorCDS::new);
		aoMap.put("PROBAV", QueryExecutorPROBAV::new);
		aoMap.put("PLANET", QueryExecutorPLANET::new);
		
		s_aoExecutors = Collections.unmodifiableMap(aoMap);
		
		Utils.debugLog("QueryExecutorFactory.static constructor, s_aoExecutors content:");
		for (String sKey : s_aoExecutors.keySet()) {
			Utils.debugLog("QueryExecutorFactory.s_aoExecutors key: " + sKey);
		}
	}

Provider Adapter
---------------------------

The ProviderAdapter has the goal to ingest the file, either being it a downloaded file or a file copy. 
Each ProviderAdapter is linked to the relative QueryExecutor using the same DataProviderCode.

WASDI supports automatic DataProvider selection, consequently each ProviderAdapter must be able to get the URI link of a file from the file name. 
The ProviderAdapter must also be able to declare its "score" in the ability to fetch a file: 
this score will be used by WASDI to select the best DataProvider for the file to be downloaded.

Scores are defined as integers by the enum in 

.. code-block:: java
	
	wasdi.dataproviders.DataProviderScores 

A higher number means the best possibility to get the file. At the moment values are:
FILE_ACCESS(100), SAME_CLOUD_DOWNLOAD(90), DOWNLOAD(80), SLOW_DOWNLOAD(50), LTA(10);

The typical empty implementation of a ProviderAdapter is:

.. code-block:: java

	public class PLANETProviderAdapter extends ProviderAdapter {

		public PLANETProviderAdapter() {
			super();
			m_sDataProviderCode = "PLANET";
		}
		
		public PLANETProviderAdapter(LoggerWrapper logger) {
			super(logger);
			m_sDataProviderCode = "PLANET";
		}
		
		@Override
		protected void internalReadConfig() {
			
		}

		@Override
		public long getDownloadFileSize(String sFileURL) throws Exception {
			return 0;
		}

		@Override
		public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
				String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
			return null;
		}

		@Override
		public String getFileName(String sFileURL) throws Exception {
			return null;
		}

		@Override
		protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
			return 0;
		}
	}

In the constructor, the provider MUST set its own code in :code:`m_sDataProviderCode`, that must correspond to the code used by the linked QueryExecutor.

The methods of the class are:

* :code:`internalReadConfig` can be used to read from WasdiConfig specific configurations.

* :code:`getDownloadFileSize` receives the file URI and must return the size of the file. It is useful to give progress to the user.

* :code:`executeDownloadFile`. It is the main method: it receives the sFileURL OBTAINED BY THE LINKED DATA PROVIDER, the credentials, the local folder, the process workspace and the max number of retry allowed. Must return the valid file full path or "" if the download was not possible.

* :code:`getFileName` extracts the file name from the URL

* :code:`internalGetScoreForFile` returns the score auto-evaluated by the Provider Adapter to download :code:`sFileName` of :code:`sPlatformType`.

The base class has many utility functions ready for many common cases:

* :code:`downloadViaHttp`: std http download
* :code:`getFileSizeViaHttp`: request file size to http
* :code:`copyStream`: copy a stream to another
* :code:`localFileCopy`: makes a local file copy
* :code:`getFileNameViaHttp`: extracts name from http call
* :code:`isWorkspaceOnSameCloud`: state if the workpsace is on the same cloud of the DataProvider (useful for score)
	
The provider adapter MUST be added to:

.. code-block:: java
	
	wasdi.dataproviders.ProviderAdapterFactory

When executing the download of a file, many methods implemented by the provider adapter will be used by the class

.. code-block:: java
	
	wasdi.operations.Download

Specifically:

* :code:`executeOperation` relies on the data provider implementation to retrieve the name of the file to download as well as its size and executing the actual download of the file;
* :code:`getBestProviderAdapter` implements the scoring mechanism to select the best data provider to downlad a file, 
* :code:`doesProviderAdapterFindFile` is used to double-check the availability of the file in the data provider, before executing the actual download.


Configuration
~~~~~~~~~~~~~~~~~~~~~~
Each Data provider is listed in the `dataProviders` section of wasdiConfig.json. 
An example is:

.. code-block:: json

		{
			"name": "LSA",
			"description": "LSA DATA CENTER",
			"link": "https://www.collgs.lu/",
			"searchListPageSize": "50",
			"defaultProtocol": "https://",
			"parserConfig": "/tmp/lsaParserConfig.json",
			"user": "USER",
			"password": "PASSWORD",
			"localFilesBasePath": "/mount/data/",
			"urlDomain": "https://collgs.lu/repository/",
			"connectionTimeout": "",
			"readTimeout": "",
			"adapterConfig": "",
			"cloudProvider": "AdwaisEO",
			"supportedPlatforms":["Sentinel-1","Sentinel-2"]
		}

* **name** is the unique code of the data provider
* **parserConfig** and **adapterConfig** are 2 possible specific config file that can be used by the Data Provider, one for the QueryExecutor and the other for the Provider Adapter. 
* **user** and **password**, if present, are the credentials of the Data Provider.
* **cloudProvider** is the unique code of the cloud where the DataProvider is hosted. Can be used to set the score of the performance for a specific file download. 
* **supportedPlatforms** is an array if strings. Each String is a valid entry of the Plaforms supported by WASDI: here is written the list of plaforms that this DataProvider supports.

Since each Platform can be supported by many data providers, among which we can select the best data provider, WASDI also defines the best catalogue to use to query that specific Platform. This is done in the `catalogues` section of wasdiConfig.

.. code-block:: json

	"catalogues": [
		{
			"platform": "Sentinel-1",
			"catalogues": ["LSA","CREODIAS","SENTINEL","ONDA","EODC"]
		}
		
In the example, we see that the Platform  Sentinel-1 is supported by 6 catalogues (DataProviders) and the priority one is LSA Data Center.

To enable the new data provider to download products, we also need to add and configure a new queue to the arrays `schedulers`, under the JSON field `scheduler`.

.. code-block:: json

	"schedulers": [
		{
			"name": "DOWNLOAD.PROVIDERNAME",
			"maxQueue": "10",
			"timeoutMs": "1111111",
			"opTypes": "DOWNLOAD",
			"opSubType": "PROVIDERNAME",
			"enabled": "1"
		}

* **name** is the unique code of the queue, following the pattern DOWNLOAD.NAME_OF_THE_DATAPROVIDER.
* **maxQueue** is the number of elements that can be put in the queue.
* **timeOutMS** is the default queue timeout, in milliseconds.
* **opTypes** it is a comma separated list of OperationTypes supported by the queue. In this specific case of adding a new data provider, the operation supported by the queue should be "DOWNLOAD".
* **opSubType** must be a valid subtype of opType. In this case, the field is used to store the name of the data provider the queue refers to.
* **enabled** is a flag to enable ("1") or disable ("0") the queue.

Welcome to Space, Have fun!

