package wasdi.scheduler;

import java.io.File;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

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
			System.exit(-1);
		}

		s_oLogger.debug("Wasdi Scheduler Start");

		try {
			ProcessScheduler oProcessScheduler = new ProcessScheduler();
			oProcessScheduler.init("PROCESSSCHEDULER");
			
			oProcessScheduler.start();

		}
		catch( Exception oEx ) {
			
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
