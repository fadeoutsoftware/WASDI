package wasdi.shared.queryexecutors.cm2;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import com.fasterxml.jackson.core.type.TypeReference;

import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.queryexecutors.ConcreteQueryTranslator;
import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.runtime.RunTimeUtils;
import wasdi.shared.utils.runtime.ShellExecReturn;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

public class QueryExecutorCM2 extends QueryExecutor {
	
	protected String m_sPythonScript = "";
	protected String m_sExchangeFolder = "";
	
	private static final Object s_oTempFolderLock = new Object();
	
	public QueryExecutorCM2() {

	}
	
	@Override
	public void init() {
		super.init();
				
		m_sParserConfigPath = m_oDataProviderConfig.parserConfig;
		WasdiLog.debugLog("QueryExecutorCM2:  parser config path is " + m_sParserConfigPath );
		
		m_oQueryTranslator = new ConcreteQueryTranslator();
		
		try {
			JSONObject oAppConf = JsonUtils.loadJsonFromFile(m_sParserConfigPath);
			m_sPythonScript = oAppConf.getString("pythonScript");
			WasdiLog.debugLog("QueryExecutorCM2: python script path " + m_sPythonScript);
			
			m_sExchangeFolder = WasdiConfig.Current.paths.wasdiTempFolder; // oAppConf.getString("exchangeFolder");
			WasdiLog.debugLog("QueryExecutorCM2: exchange folder: " + m_sExchangeFolder);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorCM2: exception reading config files ", oEx);
		}		
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
			asArgs.add(sCommand);											// arg[2] - code for the download operation
			asArgs.add(sInputFullPath);										// arg[3] - path of the input file
			asArgs.add(sOutputFullPath);									// arg[4] - path of the output file
			asArgs.add(WasdiConfig.Current.paths.wasdiConfigFilePath);		// arg[5] - wasdiConfig file path
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorCM2.getCommandLineArgs: error ",oEx);
		}
		
		return asArgs;
	}

	@Override
	public int executeCount(String sQuery) {
		
		String sInputFullPath = "";
		String sOutputFullPath = "";
		
		try {
			List<String> asArgs = getCommandLineArgs("1", sQuery);
			
			
			if (asArgs == null) {
				WasdiLog.errorLog("QueryExecutorCM2.executeCount: no args!!");
				return -1;
			}
			
			if (asArgs.size()<5) {
				WasdiLog.errorLog("QueryExecutorCM2.executeCount: not enough args");
				return -1;				
			}
			
			sInputFullPath = asArgs.get(2);
			sOutputFullPath = asArgs.get(3);
			
			ShellExecReturn oShellExecReturn = RunTimeUtils.shellExec(asArgs, true, true, true, true);
			WasdiLog.debugLog("QueryExecutorCM2.executeCount: python output = = " + oShellExecReturn.getOperationLogs());
			
			
			File oOutputFile = new File(sOutputFullPath);
			
			if (!oOutputFile.exists()) {
				WasdiLog.warnLog("QueryExecutorCM2.executeCount: impossible to read the output from python script");
				return -1;
			}
			
			WasdiLog.debugLog("QueryExecutorCM2.executeCount: got output file");
			
			JSONObject oOutput = JsonUtils.loadJsonFromFile(sOutputFullPath);
			
			int iCount = oOutput.getInt("count");
			
			WasdiLog.debugLog("QueryExecutorCM2.executeCount: return count " + iCount);
			
			return iCount;
			//return 2;
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorCM2.executeCount: error ",oEx);
		}		
		finally {
			FileUtils.deleteQuietly(new File(sInputFullPath));
			FileUtils.deleteQuietly(new File(sOutputFullPath));
		}
		return -1;
	}

	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		
		List<QueryResultViewModel> aoReturnList = new ArrayList<>();
		
		String sInputFullPath = "";
		String sOutputFullPath = "";
		
		try {
			List<String> asArgs = getCommandLineArgs("0", oQuery.getQuery());
			
			if (asArgs == null) {
				WasdiLog.errorLog("QueryExecutorCM2.executeAndRetrieve: no args!!");
				return aoReturnList;
			}
			
			
			if (asArgs.size()<5) {
				WasdiLog.errorLog("QueryExecutorCM2.executeAndRetrieve: not enough args");
				return aoReturnList;				
			}
			
			sInputFullPath = asArgs.get(2);
			sOutputFullPath = asArgs.get(3);			
			
			ShellExecReturn oShellExecReturn = RunTimeUtils.shellExec(asArgs, true, true, true, true);
			
			WasdiLog.debugLog("QueryExecutorCM2.executeAndRetrieve: python output = " + oShellExecReturn.getOperationLogs());
			
			
			File oOutputFile = new File(sOutputFullPath);
			
			if (!oOutputFile.exists()) {
				WasdiLog.warnLog("QueryExecutorCM2.executeAndRetrieve: impossible to read the output from python script");
				return aoReturnList;
			}
			
			WasdiLog.debugLog("QueryExecutorCM2.executeAndRetrieve: got output file");
			
			String sOutput = WasdiFileUtils.fileToText(sOutputFullPath);
			//String sOutput = "[{\"footprint\":\"POLYGON ((-70.386436 18.443495, -70.061836 19.952312, -72.427658 20.375757, -72.729576 18.870117, -70.386436 18.443495))\",\"id\":\"1af712e9-062d-45f5-bb77-ca2c80e0b27c\",\"link\":\"https://datahub.creodias.eu/odata/v1/Products(1af712e9-062d-45f5-bb77-ca2c80e0b27c)/$value,S1A_IW_GRDH_1SDV_20240103T103921_20240103T103946_051941_0646A1_0E08.zip,1.695165533E9,/eodata/Sentinel-1/SAR/IW_GRDH_1S/2024/01/03/S1A_IW_GRDH_1SDV_20240103T103921_20240103T103946_051941_0646A1_0E08.SAFE,\",\"preview\":null,\"properties\":{\"date\":\"2024-01-03T12:36:21.266Z\",\"sliceProductFlag\":\"false\",\"instrumentShortName\":\"SAR\",\"origin\":\"ESA\",\"processingDate\":\"2024-01-03T12:25:13.093702+00:00\",\"link\":\"https://datahub.creodias.eu/odata/v1/Products(1af712e9-062d-45f5-bb77-ca2c80e0b27c)/$value,S1A_IW_GRDH_1SDV_20240103T103921_20240103T103946_051941_0646A1_0E08.zip,1.695165533E9,/eodata/Sentinel-1/SAR/IW_GRDH_1S/2024/01/03/S1A_IW_GRDH_1SDV_20240103T103921_20240103T103946_051941_0646A1_0E08.SAFE,\",\"sliceNumber\":\"2\",\"orbitNumber\":\"51941\",\"datatakeID\":\"411297\",\"relativeorbitnumber\":\"69\",\"processingLevel\":\"LEVEL1\",\"beginningDateTime\":\"2024-01-03T10:39:21.648Z\",\"platformShortName\":\"SENTINEL-1\",\"processorName\":\"Sentinel-1 IPF\",\"startTimeFromAscendingNode\":\"2639257.0\",\"segmentStartTime\":\"2024-01-03T10:38:52.947000+00:00\",\"completionTimeFromAscendingNode\":\"2664254.0\",\"productType\":\"IW_GRDH_1S\",\"instrumentshortname\":\"SAR\",\"productClass\":\"S\",\"sensoroperationalmode\":\"IW\",\"relativeOrbitNumber\":\"69\",\"polarisationChannels\":\"VV&VH\",\"productComposition\":\"Slice\",\"processorVersion\":\"003.71\",\"swathIdentifier\":\"IW\",\"endingDateTime\":\"2024-01-03T10:39:46.646Z\",\"platformname\":\"SENTINEL-1A\",\"platformSerialIdentifier\":\"A\",\"operationalMode\":\"IW\",\"processingCenter\":\"Production Service-SERCO\",\"size\":\"1.58 GB\",\"timeliness\":\"Fast-24h\",\"totalSlices\":\"3\",\"instrumentConfigurationID\":\"7\",\"orbitDirection\":\"DESCENDING\",\"cycleNumber\":\"311\"},\"provider\":\"AUTO\",\"summary\":\"Date: 2024-01-03T12:36:21.266Z, Instrument: SAR, Mode: IW, Satellite: SENTINEL-1A, Size: 1.58 GB\",\"title\":\"S1A_IW_GRDH_1SDV_20240103T103921_20240103T103946_051941_0646A1_0E08\",\"volumeName\":null,\"volumePath\":null},{\"footprint\":\"POLYGON ((-70.696312 16.904287, -70.386459 18.443405, -72.729324 18.86998, -73.018723 17.334553, -70.696312 16.904287))\",\"id\":\"eb36bf17-d597-4838-90d8-7a36a4dac086\",\"link\":\"https://datahub.creodias.eu/odata/v1/Products(eb36bf17-d597-4838-90d8-7a36a4dac086)/$value,S1A_IW_GRDH_1SDV_20240103T103946_20240103T104012_051941_0646A1_4AF5.zip,1.724535903E9,/eodata/Sentinel-1/SAR/IW_GRDH_1S/2024/01/03/S1A_IW_GRDH_1SDV_20240103T103946_20240103T104012_051941_0646A1_4AF5.SAFE,\",\"preview\":null,\"properties\":{\"date\":\"2024-01-03T12:36:24.337Z\",\"sliceProductFlag\":\"false\",\"instrumentShortName\":\"SAR\",\"origin\":\"ESA\",\"processingDate\":\"2024-01-03T12:25:15.354965+00:00\",\"link\":\"https://datahub.creodias.eu/odata/v1/Products(eb36bf17-d597-4838-90d8-7a36a4dac086)/$value,S1A_IW_GRDH_1SDV_20240103T103946_20240103T104012_051941_0646A1_4AF5.zip,1.724535903E9,/eodata/Sentinel-1/SAR/IW_GRDH_1S/2024/01/03/S1A_IW_GRDH_1SDV_20240103T103946_20240103T104012_051941_0646A1_4AF5.SAFE,\",\"sliceNumber\":\"3\",\"orbitNumber\":\"51941\",\"datatakeID\":\"411297\",\"relativeorbitnumber\":\"69\",\"processingLevel\":\"LEVEL1\",\"beginningDateTime\":\"2024-01-03T10:39:46.648Z\",\"platformShortName\":\"SENTINEL-1\",\"processorName\":\"Sentinel-1 IPF\",\"startTimeFromAscendingNode\":\"2664256.0\",\"segmentStartTime\":\"2024-01-03T10:38:52.947000+00:00\",\"completionTimeFromAscendingNode\":\"2689691.0\",\"productType\":\"IW_GRDH_1S\",\"instrumentshortname\":\"SAR\",\"productClass\":\"S\",\"sensoroperationalmode\":\"IW\",\"relativeOrbitNumber\":\"69\",\"polarisationChannels\":\"VV&VH\",\"productComposition\":\"Slice\",\"processorVersion\":\"003.71\",\"swathIdentifier\":\"IW\",\"endingDateTime\":\"2024-01-03T10:40:12.083Z\",\"platformname\":\"SENTINEL-1A\",\"platformSerialIdentifier\":\"A\",\"operationalMode\":\"IW\",\"processingCenter\":\"Production Service-SERCO\",\"size\":\"1.61 GB\",\"timeliness\":\"Fast-24h\",\"totalSlices\":\"3\",\"instrumentConfigurationID\":\"7\",\"orbitDirection\":\"DESCENDING\",\"cycleNumber\":\"311\"},\"provider\":\"AUTO\",\"summary\":\"Date: 2024-01-03T12:36:24.337Z, Instrument: SAR, Mode: IW, Satellite: SENTINEL-1A, Size: 1.61 GB\",\"title\":\"S1A_IW_GRDH_1SDV_20240103T103946_20240103T104012_051941_0646A1_4AF5\",\"volumeName\":null,\"volumePath\":null}]";
			
			aoReturnList = MongoRepository.s_oMapper.readValue(sOutput, new TypeReference<List<QueryResultViewModel>>(){});
			
			WasdiLog.debugLog("QueryExecutorCM2.executeAndRetrieve: return elements list " + aoReturnList.size() );
			
			return aoReturnList;
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorCM2.executeAndRetrieve: error ",oEx);
		}		
		finally {
			FileUtils.deleteQuietly(new File(sInputFullPath));
			FileUtils.deleteQuietly(new File(sOutputFullPath));
		}
		return aoReturnList;
	}
		
	@Override
	public String getUriFromProductName(String sProduct, String sProtocol, String sOriginalUrl) {
		return sOriginalUrl;
	}
	

}
