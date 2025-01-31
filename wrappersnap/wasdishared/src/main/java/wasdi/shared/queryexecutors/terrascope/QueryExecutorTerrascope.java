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
		int iCount = -1;
		
		try {

			String sQueryDatesAdjusted = adjustUserProvidedDatesTo2020(sQuery);
	
			QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(sQueryDatesAdjusted);
			
			String sPlatformName = oQueryViewModel.platformName;
	
			if (!m_asSupportedPlatforms.contains(sPlatformName)) {
				return iCount;
			}
			
			if (sPlatformName.equals(Platforms.FCOVER)) {
				iCount = executCountWithPythonDataProvider(oQueryViewModel);
			}
			else {
	
				List<String> aoTerrascopeQuery = ((QueryTranslatorTerrascope) m_oQueryTranslator).translateMultiple(sQueryDatesAdjusted);
				int iResCount = 0;
				for (String sTerrascopeQuery : aoTerrascopeQuery) {
					if (!Utils.isNullOrEmpty(sTerrascopeQuery)) {
						String encodedUrl = ((QueryTranslatorTerrascope) m_oQueryTranslator).encode(sTerrascopeQuery);
		
						// Make the call
						HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(encodedUrl); 
						String sTerrascopeResults = oHttpCallResponse.getResponseBody();
		
						WasdiLog.debugLog("QueryExecutorTerrascope: get Results, extract the total count");
		
						iResCount += m_oResponseTranslator.getCountResult(sTerrascopeResults);
					}
				}
				iCount = iResCount;
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("QueryExecutor.executeCount: error ", oE);
		}

		return iCount;
	}
	
	protected List<String> getCommandLineArgs(String sCommand, QueryViewModel oQueryViewModel) {
		
		List<String> asArgs = new ArrayList<>();
		
		String sInputFullPath = "";
		String sOutputFullPath = "";
		
		try {
				
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
	
	private int executCountWithPythonDataProvider(QueryViewModel oQueryViewModel) {
		String sInputFullPath = "";
		String sOutputFullPath = "";
		
		try {
			List<String> asArgs = getCommandLineArgs("1", oQueryViewModel);
			
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
			FileUtils.deleteQuietly(new File(sInputFullPath));
			FileUtils.deleteQuietly(new File(sOutputFullPath));
		}
		return -1;
	}

	/**
	 * Execute an Terrascope Data Center Query and return the result in WASDI format
	 */
	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		
		List<QueryResultViewModel> aoReturnList = new ArrayList<>();
		
		try {

			String sQuery = oQuery.getQuery();

			QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(sQuery);
			
			String sPlatformName = oQueryViewModel.platformName;
	
			if (!m_asSupportedPlatforms.contains(sPlatformName)) {
				return null;
			}
				
			if (sPlatformName.equals(Platforms.FCOVER)) {
				
				int iLimit = !Utils.isNullOrEmpty(oQuery.getOffset()) 
						? Integer.parseInt(oQuery.getLimit()) 
						: -1;
				int iOffset = !Utils.isNullOrEmpty(oQuery.getOffset()) 
						? Integer.parseInt(oQuery.getOffset()) 
						: -1;
				
				oQueryViewModel.limit = iLimit;
				oQueryViewModel.offset = iOffset;

				return executeAndRetrieveWithPythonDataprovider(oQueryViewModel, bFullViewModel);
			}

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

			if (Utils.isNullOrEmpty(sTerrascopeQuery)) 
				return aoReturnList;

			// Make the query
			HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sTerrascopeQuery); 
			String sTerrascopeResults = oHttpCallResponse.getResponseBody();

			WasdiLog.debugLog("QueryExecutorTerrascope.executeAndRetrieve: got result, start conversion");

			return m_oResponseTranslator.translateBatch(sTerrascopeResults, bFullViewModel);
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorTerrascope.executeAndRetrieve: Exception = ", oEx);
		}

		return null;
	}
	
	
	private List<QueryResultViewModel> executeAndRetrieveWithPythonDataprovider(QueryViewModel oQuery, boolean bFullViewModel) {
		
		List<QueryResultViewModel> aoReturnList = new ArrayList<>();
		
		String sInputFullPath = "";
		String sOutputFullPath = "";
		
		try {
			List<String> asArgs = getCommandLineArgs("0", oQuery);
			
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
			
			aoReturnList = MongoRepository.s_oMapper.readValue(sOutput, new TypeReference<List<QueryResultViewModel>>(){});
			
			WasdiLog.debugLog("QueryExecutorTerrascope.executeAndRetrieveWithPythonDataprovider: return elements list " + aoReturnList.size() );
			
			return aoReturnList;
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorTerrascope.executeAndRetrieveWithPythonDataprovider: error ",oEx);
		}		
		finally {
			FileUtils.deleteQuietly(new File(sInputFullPath));
			FileUtils.deleteQuietly(new File(sOutputFullPath));
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
