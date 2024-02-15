package wasdi.shared.queryexecutors.cm2;

import java.util.Arrays;
import java.util.List;

import wasdi.shared.config.DataProviderConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.queryexecutors.cm.QueryTranslatorCM;
import wasdi.shared.queryexecutors.cm.ResponseTranslatorCM;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.runtime.RunTimeUtils;
import wasdi.shared.utils.runtime.ShellExecReturn;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

public class QueryExecutorCM2 extends QueryExecutor {
	
	private static DataProviderConfig s_oDataProviderConfig;
	
	public QueryExecutorCM2() {
		m_sProvider = "COPERNICUSMARINE";
		s_oDataProviderConfig = WasdiConfig.Current.getDataProviderConfig(m_sProvider);

		this.m_oQueryTranslator = new QueryTranslatorCM();
		this.m_oResponseTranslator = new ResponseTranslatorCM();
	}

	@Override
	public int executeCount(String sQuery) {
		// TODO Auto-generated method stub
		List<String> asArgs = Arrays.asList("data_provider_sample.py", "1", "input_file.json", "output_file.json");
		ShellExecReturn oShellExecReturn = RunTimeUtils.shellExec(asArgs, true, true, true, true);
		WasdiLog.debugLog("output = " + oShellExecReturn.getOperationLogs());
		return 0;
	}

	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		// TODO Auto-generated method stub
		return null;
	}

}
