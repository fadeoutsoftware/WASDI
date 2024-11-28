package wasdi.trigger;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import wasdi.jwasdilib.WasdiLib;
import wasdi.shared.business.Schedule;
import wasdi.shared.business.users.UserSession;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.ScheduleRepository;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.utils.EndMessageProvider;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.LoggerWrapper;
import wasdi.shared.utils.log.WasdiLog;

public class Trigger {
	
	/**
	 * Static Logger that references the "Trigger" logger
	 */
	public static Logger s_oLogger = LogManager.getLogger(Trigger.class);
	
	
	public Trigger() {
		try {
			// Read Mongo configuration
			MongoRepository.readConfig();
		} 
		catch (Throwable oEx) {
			System.out.println("TRIGGER ERROR " + oEx.toString());
		} 
	}

	
	public static void main(String[] args) {
		System.out.println("WASDI TRIGGER START");	
		
		try {
			//get jar directory
			File oCurrentFile = new File(Trigger.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
			String sThisFilePath = oCurrentFile.getParentFile().getPath();
			WasdiFileUtils.loadLogConfigFile(sThisFilePath);

		}
		catch(Exception oEx)
		{
			//no log4j configuration
			System.err.println( "Trigger - Error loading log.  Reason: " + ExceptionUtils.getStackTrace(oEx) );
			System.exit(-1);
		}

		WasdiLog.setLoggerWrapper(new LoggerWrapper(s_oLogger));

		WasdiLog.debugLog("Trigger Start");


		// create the parser
		CommandLineParser oParser = new DefaultParser();

		// create Options object
		Options oOptions = new Options();
		
		oOptions.addOption("s","scheduleid", true, "schedule object id");
		oOptions.addOption("c", "config", true, "WASDI Configuration File Path");

		String sScheduleId = "";
		String sConfigFilePath = "/etc/wasdi/wasdiConfig.json";

		try {

			// parse the command line arguments
			CommandLine oLine = oParser.parse( oOptions, args );

			if (oLine.hasOption("scheduleid")) {
				// Get the Operation Code
				sScheduleId  = oLine.getOptionValue("scheduleid");
			}
			else {
				WasdiLog.debugLog("No Schedule ID No party. Bye");
				System.exit(-1);
			}
			
	        if (oLine.hasOption("config")) {
	            // Get the Parameter File
	        	sConfigFilePath = oLine.getOptionValue("config");
	        }
			
	        if (!WasdiConfig.readConfig(sConfigFilePath)) {
	            System.err.println("Trigger - config file not found. Exit");
	            System.exit(-1);        	
	        }	        
			
	        WasdiLog.initLogger(WasdiConfig.Current.logLevelTrigger);
						
			Trigger oTrigger = new Trigger();

			WasdiLog.debugLog("Executing Schedule for " + sScheduleId);

			// And Run
			oTrigger.executeTrigger(sScheduleId);

			WasdiLog.debugLog(new EndMessageProvider().getGood());
		}
		catch( ParseException oEx ) {
			WasdiLog.errorLog("Trigger Exception " + ExceptionUtils.getStackTrace(oEx));
			System.exit(-1);
		}
	}

	public void executeTrigger(String sScheduleId) {
		WasdiLog.debugLog("executeTrigger start ");
		
		// Create the Session and Schedule Repo
		SessionRepository oSessionRepository = new SessionRepository();
		ScheduleRepository oScheduleRepository = new ScheduleRepository();
		
		// Get the schedule
		Schedule oSchedule = oScheduleRepository.getSchedule(sScheduleId);
		
		if (oSchedule == null) {
			WasdiLog.debugLog("schedule does not exists " + sScheduleId);
			return;
		}
		
		WasdiLog.debugLog("got schedule id " + sScheduleId);

		// Create the session
		UserSession oUserSession = new UserSession();
		oUserSession.setSessionId(Utils.getRandomName());
		oUserSession.setUserId(oSchedule.getUserId());
		oUserSession.setLoginDate(Utils.nowInMillis());
		oUserSession.setLastTouch(Utils.nowInMillis());
		
		oSessionRepository.insertSession(oUserSession);
		
		// Create the wasdi lib
		WasdiLib oWasdiLib = new WasdiLib();
		
		// Set min config to start
		oWasdiLib.setUser(oSchedule.getUserId());
		oWasdiLib.setActiveWorkspace(oSchedule.getWorkspaceId());
		oWasdiLib.setSessionId(oUserSession.getSessionId());
		oWasdiLib.setBasePath(PathsConfig.getWasdiBasePath());
		oWasdiLib.setIsOnServer(true);
		oWasdiLib.setDownloadActive(false);
		oWasdiLib.setBaseUrl(WasdiConfig.Current.baseUrl);
		
		// Init the lib
		if (oWasdiLib.internalInit()) {
			WasdiLog.debugLog("wasdi lib initialized");
			
			// Trigger the processor
			String sProcId = oWasdiLib.asynchExecuteProcessor(oSchedule.getProcessorName(), oSchedule.getParams(), oSchedule.isNotifyOwnerByMail());
			
			WasdiLog.debugLog("PROCESS SCHEDULED: got ProcId " + sProcId);
		}
		else {
			WasdiLog.debugLog("Error Initializing the WASDI Lib. Exit without scheduling");
		}
	}
}
