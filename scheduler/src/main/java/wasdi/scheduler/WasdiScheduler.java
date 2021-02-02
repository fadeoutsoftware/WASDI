package wasdi.scheduler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import wasdi.shared.LauncherOperations;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.utils.EndMessageProvider;
import wasdi.shared.utils.Utils;

/**
 * Hello world!
 *
 */
public class WasdiScheduler 
{	
	/**
	 * Static Logger that references the "MyApp" logger
	 */
	public static Logger s_oLogger = Logger.getLogger(WasdiScheduler.class);
	
	
	/**
	 * Scheduler Constructor
	 */
	public WasdiScheduler() {
	}
	
	/**
	 * Wasdi Scheduler Entry Point
	 * @param args
	 */
	public static void main(String[] args) {		
		System.out.println("---------------------------- WASDI SCHEDULER START ----------------------------\n");
		
		//logger config
		try {
			System.out.println("WasdiScheduler.main: configuring logger...");
			//get jar directory
			File oCurrentFile = new File(WasdiScheduler.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
			//configure log
			String sThisFilePath = oCurrentFile.getParentFile().getPath();
			DOMConfigurator.configure(sThisFilePath + "/log4j.xml");
		}
		catch(Exception oEx)
		{
			//no log4j configuration
			System.err.println("WasdiScheduler.main: Error Configuring log.  Reason: " + oEx );
			oEx.printStackTrace();
		}
		s_oLogger.info("main: Logger configured :-)\n");
		
		
		//mongo config
		try {
			s_oLogger.info("main: Configuring mongo...");
			// Init Mongo Configuration
			MongoRepository.SERVER_PORT = Integer.parseInt(ConfigReader.getPropValue("MONGO_PORT"));
			MongoRepository.DB_NAME = ConfigReader.getPropValue("MONGO_DBNAME");
			MongoRepository.DB_USER = ConfigReader.getPropValue("MONGO_DBUSER");
			MongoRepository.DB_PWD = ConfigReader.getPropValue("MONGO_DBPWD");
			MongoRepository.SERVER_ADDRESS = ConfigReader.getPropValue("MONGO_ADDRESS");
		} 
		catch (Throwable oEx) {
			s_oLogger.fatal("main: Mongo configuration failed. Reason: " + oEx);
			oEx.printStackTrace();
			System.exit(-1);
		}
		s_oLogger.info("main: Mongo configured :-)\n");
		
		
		// Read the list of configured schedulers
		String sSchedulers = "";
		ArrayList<String> asSchedulers = new ArrayList<String>();
		try {
			s_oLogger.info("main: reading schedulers configurations...");
			// Get the list of schedulers from config
			sSchedulers = ConfigReader.getPropValue("SCHEDULERS", "");
			// Split on comma
			String [] asSplitted = sSchedulers.split(",");
			
			// Add to the list of schedulers
			if (asSplitted != null) {
				for (String sScheduler : asSplitted) {
					asSchedulers.add(sScheduler);
				}
			}
		} 
		catch (IOException oEx) {
			s_oLogger.fatal("Could not read schedulers configurations. Reason: " + oEx);
			oEx.printStackTrace();
			System.exit(-1);
		}
		s_oLogger.info("main: schedulers configurations read :-)\n");
		
		s_oLogger.info("main: preparing operations...");
		
		// Get the list of all WASDI operations
		LauncherOperations [] aoOperations = LauncherOperations.class.getEnumConstants();
		
		// Convert in a list of strings
		ArrayList<String> asWasdiOperationTypes = new ArrayList<String>(); 
		for (LauncherOperations oOp : aoOperations) {
			asWasdiOperationTypes.add(oOp.name());
		}

		try {
			
			// For each scheduler
			for (String sScheduler : asSchedulers) {
				
				// Create and init the scheduler
				s_oLogger.info("main: Creating Scheduler: " + sScheduler);
				ProcessScheduler oProcessScheduler = new ProcessScheduler();
				oProcessScheduler.init(sScheduler);
				
				// Get the list of supported types
				List<String> asSchedulerTypes = new ArrayList<String>(oProcessScheduler.getSupportedTypes());
				
				for (String sSupportedType : asSchedulerTypes) {
					
					// Check if the type is already "free"
					if (asWasdiOperationTypes.contains(sSupportedType) == false) {
						
						if (Utils.isNullOrEmpty(oProcessScheduler.getOperationSubType())) {
							// No: remove from the scheduler
							s_oLogger.error("main: Scheduler " + sScheduler + " support type " + sSupportedType + " that does not exists or has already been supported by other scheduler. It will be removed");
							oProcessScheduler.removeSupportedType(sSupportedType);							
						}
						else {
							s_oLogger.info("main: Assigning to Scheduler " + sScheduler + " support type " + sSupportedType + " SubType " + oProcessScheduler.getOperationSubType());
						}
					}
					else {
						String sSubTypeLog = "";
						if (!Utils.isNullOrEmpty(oProcessScheduler.getOperationSubType())) {
							sSubTypeLog = " SubType " + oProcessScheduler.getOperationSubType();
						}
						
						// Yes: remove from the full list
						s_oLogger.info("main: Assigning to Scheduler: " + sScheduler + " support type: " + sSupportedType + sSubTypeLog);
						asWasdiOperationTypes.remove(sSupportedType);
					}
				}
				
				String sSchedulerEnabled = ConfigReader.getPropValue(sScheduler.toUpperCase()+"_ENABLED", "1");
				
				if (sSchedulerEnabled.equals("1")) {
					// Start the scheduler
					s_oLogger.info("main: Starting Scheduler: " + sScheduler);
					oProcessScheduler.start();					
				}
				else {
					s_oLogger.warn("main: Scheduler: " + sScheduler + " not enabled, will not start");
				}
			}
			
			// Do we have other types?
			if (asWasdiOperationTypes.size() > 0) {
				
				// Create and Init the Default Scheduler
				s_oLogger.info("main: Creating DEFAULT Scheduler");
				ProcessScheduler oProcessScheduler = new ProcessScheduler();
				oProcessScheduler.init("DEFAULT");
				
				// Assign all the types
				for (String sOtherType : asWasdiOperationTypes) {
					s_oLogger.info("main: Assigning to Scheduler: DEFAULT support type: " + sOtherType);
					oProcessScheduler.addSupportedType(sOtherType);
				}
				
				// Start
				s_oLogger.info("main: Starting Scheduler: DEFAULT");
				oProcessScheduler.start();
			}
			else {
				s_oLogger.info("main: All types covered, do not start DEFAULT scheduler");
			}
			
		}
		catch( Exception oEx ) {
			s_oLogger.error("Could not complete operations preparations. Reason: " + oEx);
			oEx.printStackTrace();
		}
		s_oLogger.info("main: operations prepared, good to go :-)\n");
		
		s_oLogger.debug(new EndMessageProvider().getGood() + '\n');

	}
	
	/**
	 * Error Log thread safe method
	 * @param sLogRow
	 */
	public static void error(String sLogRow) {
		log(sLogRow,Level.ERROR);
	}
	
	/**
	 * Info Log thread safe method
	 * @param sLogRow
	 */
	public static void log(String sLogRow) {
		log(sLogRow,Level.INFO);
	}
	
	/**
	 * Warn Log thread safe method
	 * @param sLogRow
	 */
	public static void warn(String sLogRow) {
		log(sLogRow, Level.WARN);
		
	}
	
	/**
	 * Log thread safe method
	 * @param sLogRow
	 * @param oLogLevel
	 */
	public static void log(String sLogRow, Level oLogLevel) {
		synchronized (s_oLogger) {
			s_oLogger.log(oLogLevel, sLogRow);
		}
	}


	
}
