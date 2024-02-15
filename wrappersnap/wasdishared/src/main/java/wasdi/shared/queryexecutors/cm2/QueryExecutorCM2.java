package wasdi.shared.queryexecutors.cm2;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import com.fasterxml.jackson.core.type.TypeReference;

import wasdi.shared.config.DataProviderConfig;
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
import wasdi.shared.viewmodels.processors.PackageManagerFullInfoViewModel;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

public class QueryExecutorCM2 extends QueryExecutor {
	
	private static DataProviderConfig s_oDataProviderConfig;
	protected String m_sPythonScript = "";
	protected String m_sExchangeFolder = "";
	
	public QueryExecutorCM2() {
		m_sProvider = "COPERNICUSMARINE";
		
		s_oDataProviderConfig = WasdiConfig.Current.getDataProviderConfig(m_sProvider);
		m_sParserConfigPath = s_oDataProviderConfig.parserConfig;
		
		m_oQueryTranslator = new ConcreteQueryTranslator();
		
		try {
			JSONObject oAppConf = WasdiFileUtils.loadJsonFromFile(m_sParserConfigPath);
			m_sPythonScript = oAppConf.getString("pythonScript");
			m_sExchangeFolder = oAppConf.getString("exchangeFolder");
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("QueryExecutorCM2.executeCount(): exception reading parser config file " + m_sParserConfigPath);
		}
	}
	
	protected List<String> getCommandLineArgs(String sCommand, String sQuery) {
		
		List<String> asArgs = new ArrayList<>();
		
		String sInputFullPath = "";
		String sOutputFullPath = "";
		
		try {
			
	
			String sInputFile = Utils.getRandomName();
			String sOutputFile = Utils.getRandomName();
			
			QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(sQuery);
			String sQueryViewModel = JsonUtils.stringify(oQueryViewModel);
			
			if (!Utils.isNullOrEmpty(m_sExchangeFolder)) {
				sInputFullPath = m_sExchangeFolder;
				sOutputFullPath = m_sExchangeFolder;
				if (!m_sExchangeFolder.endsWith("/")) {
					sInputFullPath += "/";
					sOutputFullPath += "/";
				}
			}
			
			sInputFullPath += sInputFile;
			sOutputFullPath += sOutputFile;
		
			WasdiFileUtils.writeFile(sQueryViewModel, sInputFullPath);
		
			asArgs.add(m_sPythonScript);
			asArgs.add(sCommand);
			asArgs.add(sInputFullPath);
			asArgs.add(sOutputFullPath);
			asArgs.add(WasdiConfig.Current.myPath);
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
			
			JSONObject oOutput = WasdiFileUtils.loadJsonFromFile(sOutputFullPath);
			
			int iCount = oOutput.getInt("count");
			
			WasdiLog.debugLog("QueryExecutorCM2.executeCount: return count " + iCount);
			
			return iCount;
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorCM2.executeCount: error ",oEx);
		}		
		finally {
			FileUtils.deleteQuietly(new File(sInputFullPath));
			FileUtils.deleteQuietly(new File(sOutputFullPath));
		}
		return 0;
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

}
