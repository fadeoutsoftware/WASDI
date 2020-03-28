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
		System.out.println("WASDI SCHEDULER START");
		
		try {
			// Init Mongo Configuration
			MongoRepository.SERVER_PORT = Integer.parseInt(ConfigReader.getPropValue("MONGO_PORT"));
			MongoRepository.DB_NAME = ConfigReader.getPropValue("MONGO_DBNAME");
			MongoRepository.DB_USER = ConfigReader.getPropValue("MONGO_DBUSER");
			MongoRepository.DB_PWD = ConfigReader.getPropValue("MONGO_DBPWD");
		} 
		catch (Throwable oEx) {
			oEx.printStackTrace();
		} 
		
		
		try {
			//get jar directory
			File oCurrentFile = new File(WasdiScheduler.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
			//configure log
			String sThisFilePath = oCurrentFile.getParentFile().getPath();
			DOMConfigurator.configure(sThisFilePath + "/log4j.xml");
		}
		catch(Exception exp)
		{
			//no log4j configuration
			System.err.println( "Scheduler - Error Configuring log.  Reason: " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(exp) );
			//System.exit(-1);
		}

		s_oLogger.debug("Wasdi Scheduler Start");
		
		// Read the list of configured schedulers
		String sSchedulers = "";
		ArrayList<String> asSchedulers = new ArrayList<String>();
		
		try {
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
			s_oLogger.debug("Exception " + oEx.toString());
		}
		
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
				s_oLogger.debug("INFO: Creating Scheduler: " + sScheduler);
				ProcessScheduler oProcessScheduler = new ProcessScheduler();
				oProcessScheduler.init(sScheduler);
				
				// Get the list of supported types
				List<String> asSchedulerTypes = new ArrayList<String>(oProcessScheduler.getSupportedTypes());
				
				for (String sSupportedType : asSchedulerTypes) {
					
					// Check if the type is already "free"
					if (asWasdiOperationTypes.contains(sSupportedType) == false) {
						// No: remove from the scheduler
						s_oLogger.debug("ERROR: Scheduler " + sScheduler + " support type " + sSupportedType + " that does not exists or has already been supported by other scheduler. It will be removed");
						oProcessScheduler.removeSupportedType(sSupportedType);
					}
					else {
						// Yes: remove from the full list
						s_oLogger.debug("INFO: Assigning to Scheduler: " + sScheduler + " support type: " + sSupportedType);
						asWasdiOperationTypes.remove(sSupportedType);
					}
				}
				
				String sSchedulerEnabled = ConfigReader.getPropValue(sScheduler.toUpperCase()+"_ENABLED", "1");
				
				if (sSchedulerEnabled.equals("1")) {
					// Start the scheduler
					s_oLogger.debug("INFO: Starting Scheduler: " + sScheduler);
					oProcessScheduler.start();					
				}
				else {
					s_oLogger.debug("INFO: Scheduler: " + sScheduler + " not enabled, will not start");
				}
			}
			
			// Do we have other types?
			if (asWasdiOperationTypes.size() > 0) {
				
				// Create and Init the Default Scheduler
				s_oLogger.debug("INFO: Creating DEFAULT Scheduler");
				ProcessScheduler oProcessScheduler = new ProcessScheduler();
				oProcessScheduler.init("DEFAULT");
				
				// Assign all the types
				for (String sOtherType : asWasdiOperationTypes) {
					s_oLogger.debug("INFO: Assigning to Scheduler: DEFAULT support type: " + sOtherType);
					oProcessScheduler.addSupportedType(sOtherType);
				}
				
				// Start
				s_oLogger.debug("INFO: Starting Scheduler: DEFAULT");
				oProcessScheduler.start();
			}
			else {
				s_oLogger.debug("INFO: All types covered, do not start DEFAULT scheduler");
			}
			
		}
		catch( Exception oEx ) {
			s_oLogger.debug("Exception " + oEx.toString());
		}
		
		s_oLogger.debug(new EndMessageProvider().getGood());
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
