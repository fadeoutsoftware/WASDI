package wasdi.shared.queryexecutors.terrascope;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import com.fasterxml.jackson.core.type.TypeReference;

import wasdi.shared.config.DataProviderConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.queryexecutors.ConcreteQueryTranslator;
import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.runtime.RunTimeUtils;
import wasdi.shared.utils.runtime.ShellExecReturn;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

/**
 * Query Executor for Terrascope Data Center.
 * 
 * @author PetruPetrescu
 *
 */
public class QueryExecutorTerrascope extends QueryExecutor {

	boolean m_bAuthenticated = false;
	
	private static DataProviderConfig s_oDataProviderConfig;
	protected String m_sPythonScript = "";
	protected String m_sExchangeFolder = "";
	
	private static final Object s_oTempFolderLock = new Object();

	public QueryExecutorTerrascope() {
		m_sProvider="TERRASCOPE";
		this.m_oQueryTranslator = new QueryTranslatorTerrascope();
		this.m_oResponseTranslator = new ResponseTranslatorTerrascope();

		m_asSupportedPlatforms.add(Platforms.SENTINEL1);
//		m_asSupportedPlatforms.add(Platforms.SENTINEL2);
//		m_asSupportedPlatforms.add(Platforms.PROBAV);
		m_asSupportedPlatforms.add(Platforms.DEM);
		m_asSupportedPlatforms.add(Platforms.WORLD_COVER);
		m_asSupportedPlatforms.add(Platforms.FCOVER);
		
		
		s_oDataProviderConfig = WasdiConfig.Current.getDataProviderConfig(m_sProvider);
		WasdiLog.debugLog("QueryExecutorTerrascope: got data provider config");
		
		m_sParserConfigPath = s_oDataProviderConfig.parserConfig;
		WasdiLog.debugLog("QueryExecutorTerrascope:  parser config path is " + m_sParserConfigPath );
		
		m_oQueryTranslator = new ConcreteQueryTranslator();
		
		try {
			JSONObject oAppConf = JsonUtils.loadJsonFromFile(m_sParserConfigPath);
			m_sPythonScript = oAppConf.getString("pythonScript");
			WasdiLog.debugLog("QueryExecutorTerrascope: python script path " + m_sPythonScript);
			
			m_sExchangeFolder = WasdiConfig.Current.paths.wasdiTempFolder; // oAppConf.getString("exchangeFolder");
			WasdiLog.debugLog("QueryExecutorTerrascope: exchange folder: " + m_sExchangeFolder);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorTerrascope: exception reading config files ", oEx);
		}
	}
	
	/**
	 * Overload of the get URI from Product Name method.
	 * For Terrascope, we need just the original link..
	 */
	@Override
	public String getUriFromProductName(String sProduct, String sProtocol, String sOriginalUrl) {
		if (sProduct.toUpperCase().startsWith("COPERNICUS_DSM_COG_")
				|| sProduct.toUpperCase().startsWith("ESA_WORLDCOVER")
				|| sProduct.startsWith("c_gls_FCOVER300")) {
			return sOriginalUrl;
		}
		return null;
	}

	/**
	 * Executes the count 
	 */
	@Override
	public int executeCount(String sQuery) {
		int iCount = 0;

		String sQueryDatesAdjusted = adjustUserProvidedDatesTo2020(sQuery);

		QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(sQueryDatesAdjusted);
		
		String sPlatformName = oQueryViewModel.platformName;

		if (!m_asSupportedPlatforms.contains(sPlatformName)) {
			return 0;
		}
		
		if (sPlatformName.equals(Platforms.FCOVER)) {
			return executCountWithPythonDataProvider(sQuery);
		}

		List<String> aoTerrascopeQuery = ((QueryTranslatorTerrascope) m_oQueryTranslator).translateMultiple(sQueryDatesAdjusted);
		for (String sTerrascopeQuery : aoTerrascopeQuery) {
			if (!Utils.isNullOrEmpty(sTerrascopeQuery)) {
				String encodedUrl = ((QueryTranslatorTerrascope) m_oQueryTranslator).encode(sTerrascopeQuery);

				// Make the call
				HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(encodedUrl); 
				String sTerrascopeResults = oHttpCallResponse.getResponseBody();

				WasdiLog.debugLog("QueryExecutorTerrascope: get Results, extract the total count");

				iCount += m_oResponseTranslator.getCountResult(sTerrascopeResults);
			}
		}

		return iCount;
	}
	
	protected List<String> getCommandLineArgs(String sCommand, String sQuery) {
		
		List<String> asArgs = new ArrayList<>();
		
		String sInputFullPath = "";
		String sOutputFullPath = "";
		
		try {
				
			QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(sQuery);
			String sQueryViewModel = JsonUtils.stringify(oQueryViewModel);
			
			String sInputFile = Utils.getRandomName();
			String sOutputFile = Utils.getRandomName();
			
			synchronized (s_oTempFolderLock) {
				if (!Utils.isNullOrEmpty(m_sExchangeFolder)) {
					
					boolean bIsInputFileUnique = false;
					
					while (!bIsInputFileUnique) {
						sInputFullPath = m_sExchangeFolder;
						if (!m_sExchangeFolder.endsWith("/")) {
							sInputFullPath += "/";
						}
						sInputFullPath += sInputFile;
						
						if (new File(sInputFullPath).exists()) {
							sInputFile = Utils.getRandomName();
						} else {
							bIsInputFileUnique = true;
						}
					}
					
					WasdiFileUtils.writeFile(sQueryViewModel, sInputFullPath);
					
					boolean bOutputFileUnique = false;
					
					while (!bOutputFileUnique) {
						sOutputFullPath = m_sExchangeFolder;
						if (!m_sExchangeFolder.endsWith("/")) {
							sOutputFullPath += "/";
						}
						sOutputFullPath += sOutputFile;
						
						if (new File(sOutputFullPath).exists()) {
							sOutputFile = Utils.getRandomName();
						} else {
							bOutputFileUnique = true;
						}
					}	
					
					File oOutputFile = new File(sOutputFullPath);
					oOutputFile.createNewFile();
				}
			}			
		
			asArgs.add(m_sPythonScript);									// arg[1] - name of the python data provider
			asArgs.add(sCommand);											// arg[2] - code for the operation (e.g. count, search, download)
			asArgs.add(sInputFullPath);										// arg[3] - path of the input file
			asArgs.add(sOutputFullPath);									// arg[4] - path of the output file
			asArgs.add(WasdiConfig.Current.paths.wasdiConfigFilePath);		// arg[5] - wasdiConfig file path
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorTerrascope.getCommandLineArgs: error ",oEx);
		}	
		return asArgs;
	}
	
	private int executCountWithPythonDataProvider(String sQuery) {
		String sInputFullPath = "";
		String sOutputFullPath = "";
		
		try {
			List<String> asArgs = getCommandLineArgs("1", sQuery);
			
			if (asArgs == null) {
				WasdiLog.errorLog("QueryExecutorTerrascope.executCountWithPythonDataProvider: no args!!");
				return -1;
			}
			if (asArgs.size()<5) {
				WasdiLog.errorLog("QueryExecutorTerrascope.executCountWithPythonDataProvider: not enough args");
				return -1;				
			}
			
			sInputFullPath = asArgs.get(2);
			sOutputFullPath = asArgs.get(3);
			
			ShellExecReturn oShellExecReturn = RunTimeUtils.shellExec(asArgs, true, true, true, true);
			WasdiLog.debugLog("QueryExecutorTerrascope.executCountWithPythonDataProvider: python output = = " + oShellExecReturn.getOperationLogs());
			
			File oOutputFile = new File(sOutputFullPath);
			
			if (!oOutputFile.exists()) {
				WasdiLog.warnLog("QueryExecutorTerrascope.executCountWithPythonDataProvider: impossible to read the output from python script");
				return -1;
			}
			
			WasdiLog.debugLog("QueryExecutorTerrascope.executCountWithPythonDataProvider: got output file");
			
			JSONObject oOutput = JsonUtils.loadJsonFromFile(sOutputFullPath);
			int iCount = oOutput.getInt("count");
			
			WasdiLog.debugLog("QueryExecutorTerrascope.executCountWithPythonDataProvider: return count " + iCount);
			
			return iCount;
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorTerrascope.executCountWithPythonDataProvider: error ", oEx);
		}		
		finally {
//			FileUtils.deleteQuietly(new File(sInputFullPath));
//			FileUtils.deleteQuietly(new File(sOutputFullPath));
		}
		return -1;
	}

	/**
	 * Execute an Terrascope Data Center Query and return the result in WASDI format
	 */
	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		List<QueryResultViewModel> aoReturnList = new ArrayList<>();

		QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(oQuery.getQuery());
		
		String sPlatformName = oQueryViewModel.platformName;

		if (!m_asSupportedPlatforms.contains(sPlatformName)) {
			return aoReturnList;
		}

		try {
			
			if (sPlatformName.equals(Platforms.FCOVER)) {
				return executeAndRetrieveWithPythonDataprovider(oQuery, bFullViewModel);
			}
			
			String sQuery = oQuery.getQuery();

			sQuery = adjustUserProvidedDatesTo2020(sQuery);

			if (!sQuery.contains("&offset")) sQuery += "&offset=" + oQuery.getOffset();
			if (!sQuery.contains("&limit")) sQuery += "&limit=" + oQuery.getLimit();

			List<String> aoTerrascopeQuery = ((QueryTranslatorTerrascope) m_oQueryTranslator).translateMultiple(sQuery);
			for (String sTerrascopeQuery : aoTerrascopeQuery) {
				if (!Utils.isNullOrEmpty(sTerrascopeQuery)) {
					String encodedUrl = ((QueryTranslatorTerrascope) m_oQueryTranslator).encode(sTerrascopeQuery);

					// Make the call
					HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(encodedUrl); 
					String sTerrascopeResults = oHttpCallResponse.getResponseBody();

					WasdiLog.debugLog("QueryExecutorTerrascope.executeAndRetrieve: got result, start conversion");

					aoReturnList.addAll(m_oResponseTranslator.translateBatch(sTerrascopeResults, bFullViewModel));
				}
			}

			String sTerrascopeQuery = m_oQueryTranslator.translateAndEncodeParams(sQuery);

			if (Utils.isNullOrEmpty(sTerrascopeQuery)) return aoReturnList;

			// Make the query
			HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sTerrascopeQuery); 
			String sTerrascopeResults = oHttpCallResponse.getResponseBody();

			WasdiLog.debugLog("QueryExecutorTerrascope.executeAndRetrieve: got result, start conversion");

			return m_oResponseTranslator.translateBatch(sTerrascopeResults, bFullViewModel);
		} catch (Exception oEx) {
			WasdiLog.debugLog("QueryExecutorTerrascope.executeAndRetrieve: Exception = " + oEx.toString());
		}

		return aoReturnList;
	}
	
	
	private List<QueryResultViewModel> executeAndRetrieveWithPythonDataprovider(PaginatedQuery oQuery, boolean bFullViewModel) {
		
		List<QueryResultViewModel> aoReturnList = new ArrayList<>();
		
		String sInputFullPath = "";
		String sOutputFullPath = "";
		
		try {
			List<String> asArgs = getCommandLineArgs("0", oQuery.getQuery());
			
			if (asArgs == null) {
				WasdiLog.errorLog("QueryExecutorTerrascope.executeAndRetrieveWithPythonDataprovider: no args!!");
				return aoReturnList;
			}
			
			
			if (asArgs.size()<5) {
				WasdiLog.errorLog("QueryExecutorTerrascope.executeAndRetrieveWithPythonDataprovider: not enough args");
				return aoReturnList;				
			}
			
			sInputFullPath = asArgs.get(2);
			sOutputFullPath = asArgs.get(3);			
			
			ShellExecReturn oShellExecReturn = RunTimeUtils.shellExec(asArgs, true, true, true, true);
			
			WasdiLog.debugLog("QueryExecutorTerrascope.executeAndRetrieveWithPythonDataprovider: python output = " + oShellExecReturn.getOperationLogs());
			
			
			File oOutputFile = new File(sOutputFullPath);
			
			if (!oOutputFile.exists()) {
				WasdiLog.warnLog("QueryExecutorTerrascope.executeAndRetrieveWithPythonDataprovider: impossible to read the output from python script");
				return aoReturnList;
			}
			
			WasdiLog.debugLog("QueryExecutorTerrascope.executeAndRetrieveWithPythonDataprovider: got output file");
			
			String sOutput = WasdiFileUtils.fileToText(sOutputFullPath);
			//String sOutput = "[{\"footprint\":\"POLYGON ((-70.386436 18.443495, -70.061836 19.952312, -72.427658 20.375757, -72.729576 18.870117, -70.386436 18.443495))\",\"id\":\"1af712e9-062d-45f5-bb77-ca2c80e0b27c\",\"link\":\"https://datahub.creodias.eu/odata/v1/Products(1af712e9-062d-45f5-bb77-ca2c80e0b27c)/$value,S1A_IW_GRDH_1SDV_20240103T103921_20240103T103946_051941_0646A1_0E08.zip,1.695165533E9,/eodata/Sentinel-1/SAR/IW_GRDH_1S/2024/01/03/S1A_IW_GRDH_1SDV_20240103T103921_20240103T103946_051941_0646A1_0E08.SAFE,\",\"preview\":null,\"properties\":{\"date\":\"2024-01-03T12:36:21.266Z\",\"sliceProductFlag\":\"false\",\"instrumentShortName\":\"SAR\",\"origin\":\"ESA\",\"processingDate\":\"2024-01-03T12:25:13.093702+00:00\",\"link\":\"https://datahub.creodias.eu/odata/v1/Products(1af712e9-062d-45f5-bb77-ca2c80e0b27c)/$value,S1A_IW_GRDH_1SDV_20240103T103921_20240103T103946_051941_0646A1_0E08.zip,1.695165533E9,/eodata/Sentinel-1/SAR/IW_GRDH_1S/2024/01/03/S1A_IW_GRDH_1SDV_20240103T103921_20240103T103946_051941_0646A1_0E08.SAFE,\",\"sliceNumber\":\"2\",\"orbitNumber\":\"51941\",\"datatakeID\":\"411297\",\"relativeorbitnumber\":\"69\",\"processingLevel\":\"LEVEL1\",\"beginningDateTime\":\"2024-01-03T10:39:21.648Z\",\"platformShortName\":\"SENTINEL-1\",\"processorName\":\"Sentinel-1 IPF\",\"startTimeFromAscendingNode\":\"2639257.0\",\"segmentStartTime\":\"2024-01-03T10:38:52.947000+00:00\",\"completionTimeFromAscendingNode\":\"2664254.0\",\"productType\":\"IW_GRDH_1S\",\"instrumentshortname\":\"SAR\",\"productClass\":\"S\",\"sensoroperationalmode\":\"IW\",\"relativeOrbitNumber\":\"69\",\"polarisationChannels\":\"VV&VH\",\"productComposition\":\"Slice\",\"processorVersion\":\"003.71\",\"swathIdentifier\":\"IW\",\"endingDateTime\":\"2024-01-03T10:39:46.646Z\",\"platformname\":\"SENTINEL-1A\",\"platformSerialIdentifier\":\"A\",\"operationalMode\":\"IW\",\"processingCenter\":\"Production Service-SERCO\",\"size\":\"1.58 GB\",\"timeliness\":\"Fast-24h\",\"totalSlices\":\"3\",\"instrumentConfigurationID\":\"7\",\"orbitDirection\":\"DESCENDING\",\"cycleNumber\":\"311\"},\"provider\":\"AUTO\",\"summary\":\"Date: 2024-01-03T12:36:21.266Z, Instrument: SAR, Mode: IW, Satellite: SENTINEL-1A, Size: 1.58 GB\",\"title\":\"S1A_IW_GRDH_1SDV_20240103T103921_20240103T103946_051941_0646A1_0E08\",\"volumeName\":null,\"volumePath\":null},{\"footprint\":\"POLYGON ((-70.696312 16.904287, -70.386459 18.443405, -72.729324 18.86998, -73.018723 17.334553, -70.696312 16.904287))\",\"id\":\"eb36bf17-d597-4838-90d8-7a36a4dac086\",\"link\":\"https://datahub.creodias.eu/odata/v1/Products(eb36bf17-d597-4838-90d8-7a36a4dac086)/$value,S1A_IW_GRDH_1SDV_20240103T103946_20240103T104012_051941_0646A1_4AF5.zip,1.724535903E9,/eodata/Sentinel-1/SAR/IW_GRDH_1S/2024/01/03/S1A_IW_GRDH_1SDV_20240103T103946_20240103T104012_051941_0646A1_4AF5.SAFE,\",\"preview\":null,\"properties\":{\"date\":\"2024-01-03T12:36:24.337Z\",\"sliceProductFlag\":\"false\",\"instrumentShortName\":\"SAR\",\"origin\":\"ESA\",\"processingDate\":\"2024-01-03T12:25:15.354965+00:00\",\"link\":\"https://datahub.creodias.eu/odata/v1/Products(eb36bf17-d597-4838-90d8-7a36a4dac086)/$value,S1A_IW_GRDH_1SDV_20240103T103946_20240103T104012_051941_0646A1_4AF5.zip,1.724535903E9,/eodata/Sentinel-1/SAR/IW_GRDH_1S/2024/01/03/S1A_IW_GRDH_1SDV_20240103T103946_20240103T104012_051941_0646A1_4AF5.SAFE,\",\"sliceNumber\":\"3\",\"orbitNumber\":\"51941\",\"datatakeID\":\"411297\",\"relativeorbitnumber\":\"69\",\"processingLevel\":\"LEVEL1\",\"beginningDateTime\":\"2024-01-03T10:39:46.648Z\",\"platformShortName\":\"SENTINEL-1\",\"processorName\":\"Sentinel-1 IPF\",\"startTimeFromAscendingNode\":\"2664256.0\",\"segmentStartTime\":\"2024-01-03T10:38:52.947000+00:00\",\"completionTimeFromAscendingNode\":\"2689691.0\",\"productType\":\"IW_GRDH_1S\",\"instrumentshortname\":\"SAR\",\"productClass\":\"S\",\"sensoroperationalmode\":\"IW\",\"relativeOrbitNumber\":\"69\",\"polarisationChannels\":\"VV&VH\",\"productComposition\":\"Slice\",\"processorVersion\":\"003.71\",\"swathIdentifier\":\"IW\",\"endingDateTime\":\"2024-01-03T10:40:12.083Z\",\"platformname\":\"SENTINEL-1A\",\"platformSerialIdentifier\":\"A\",\"operationalMode\":\"IW\",\"processingCenter\":\"Production Service-SERCO\",\"size\":\"1.61 GB\",\"timeliness\":\"Fast-24h\",\"totalSlices\":\"3\",\"instrumentConfigurationID\":\"7\",\"orbitDirection\":\"DESCENDING\",\"cycleNumber\":\"311\"},\"provider\":\"AUTO\",\"summary\":\"Date: 2024-01-03T12:36:24.337Z, Instrument: SAR, Mode: IW, Satellite: SENTINEL-1A, Size: 1.61 GB\",\"title\":\"S1A_IW_GRDH_1SDV_20240103T103946_20240103T104012_051941_0646A1_4AF5\",\"volumeName\":null,\"volumePath\":null}]";
			
			aoReturnList = MongoRepository.s_oMapper.readValue(sOutput, new TypeReference<List<QueryResultViewModel>>(){});
			
			WasdiLog.debugLog("QueryExecutorTerrascope.executeAndRetrieveWithPythonDataprovider: return elements list " + aoReturnList.size() );
			
			return aoReturnList;
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorTerrascope.executeAndRetrieveWithPythonDataprovider: error ",oEx);
		}		
		finally {
//			FileUtils.deleteQuietly(new File(sInputFullPath));
//			FileUtils.deleteQuietly(new File(sOutputFullPath));
		}
		return aoReturnList;
	}

	/**
	 * Adjust the user provided dates (beginPosition & endPosition) to 2020-01-01 in order to make sure that the search produces valid results
	 * @param sQuery the initial query
	 * @return the query with the adjusted dates
	 */
	private static String adjustUserProvidedDatesTo2020(String sQuery) {
		if (!sQuery.contains("platformname:WorldCover") && !sQuery.contains("platformname:DEM")) {
			return sQuery;
		}

		if (!sQuery.contains("beginPosition") || !sQuery.contains("endPosition")) {
			return sQuery;
		}

		String sOriginalDateToReplace = sQuery.substring(sQuery.indexOf("[", sQuery.indexOf("beginPosition")) + 1, sQuery.indexOf("TO", sQuery.indexOf("beginPosition"))).trim();
		String sReplacedWithDate = "2020-01-01T00:00:00.000Z";

		sQuery = sQuery.replace(sOriginalDateToReplace, sReplacedWithDate);

		sOriginalDateToReplace = sQuery.substring(sQuery.indexOf("TO", sQuery.indexOf("beginPosition")) + 3, sQuery.indexOf("]", sQuery.indexOf("beginPosition"))).trim();
		sReplacedWithDate = Utils.formatToYyyyDashMMDashdd(new Date()) + "T23:59:59.999Z";

		sQuery = sQuery.replace(sOriginalDateToReplace, sReplacedWithDate);

		return sQuery;
	}

}
