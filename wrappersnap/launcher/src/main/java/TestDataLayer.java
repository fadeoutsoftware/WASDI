import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Config;
import org.esa.snap.runtime.Engine;

import wasdi.ConfigReader;
import wasdi.shared.business.ProcessorLog;
import wasdi.shared.business.SnapWorkflow;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.ProcessorLogRepository;
import wasdi.shared.data.SnapWorkflowRepository;
import wasdi.shared.utils.Utils;

public class TestDataLayer {

	public static void main(String[] args) throws IOException {
		
		
		System.setProperty("user.home", "C:\\Users\\p.campanella.FADEOUT");
        Path oPropFile = Paths.get("C:\\Codice\\Progetti\\WASDI\\Codice\\\\wrappersnap\\launcher\\target\\config.properties");
        //Config.instance("snap.auxdata").load(oPropFile);
        //Config.instance().load();
        //SystemUtils.init3rdPartyLibs(null);
        //Engine.start(false);
        
		MongoRepository.SERVER_ADDRESS = ConfigReader.getPropValue("MONGO_ADDRESS");
        MongoRepository.SERVER_PORT = Integer.parseInt(ConfigReader.getPropValue("MONGO_PORT"));
        MongoRepository.DB_NAME = ConfigReader.getPropValue("MONGO_DBNAME");
        MongoRepository.DB_USER = ConfigReader.getPropValue("MONGO_DBUSER");
        MongoRepository.DB_PWD = ConfigReader.getPropValue("MONGO_DBPWD");

        
		ProcessorLog oTestLog = new ProcessorLog();
		oTestLog.setProcessWorkspaceId("fb99a0b1-93cb-40ab-9d44-9701a7b11b9b");
		oTestLog.setLogRow("Now put the second one!");
		oTestLog.setLogDate(Utils.GetFormatDate(new Date()));
		
		ProcessorLogRepository oProcessorLogRepository = new ProcessorLogRepository();
		oProcessorLogRepository.InsertProcessLog(oTestLog);
		
		List<ProcessorLog> aoLogs = oProcessorLogRepository.GetLogsByProcessWorkspaceId("fb99a0b1-93cb-40ab-9d44-9701a7b11b9b");
		
		for (ProcessorLog oLog : aoLogs) {
			System.out.println(oLog.getLogRow());
		}


	}

}
