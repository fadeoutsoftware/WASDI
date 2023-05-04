package wasdi.scheduler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.SchedulerQueueConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.utils.EndMessageProvider;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.LoggerWrapper;
import wasdi.shared.utils.log.WasdiLog;

/**
 * WASDI Scheduler
 *
 */
public class WasdiScheduler 
{	
	/**
	 * Static Logger that references the "MyApp" logger
	 */
	public static Logger s_oLogger = Logger.getLogger(WasdiScheduler.class);
	
	/**
	 * Process Workpsace Repository
	 */
	private static ProcessWorkspaceRepository s_oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
	
	/**
	 * Local WASDI node code
	 */
	public static String s_sWasdiNode;
	
	/**
	 * sleeping time between iterations
	 */
	private static long s_lSleepingTimeMS = 2000;
	
	/**
	 * Parameter to slow down the scheduler special checks that will be
	 * done only every m_iSometimesCounter cycles
	 */
	private static  int s_iSometimesCounter = 30;	
	
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
		
		String sConfigFilePath = "/etc/wasdi/wasdiConfig.json";
		
		try {
			
			// Parse the command line to find the path of the config file 
			if (args != null) {
				if (args.length>=2) {
					if (args[0].equals("-c") || args[0].equals("--config")) {
						sConfigFilePath = args[1];
					}
				}
			}			 
		}
		catch (Exception oEx) {
            System.err.println("WasdiScheduler.main - Exception paring args " + oEx.toString());			
		}
		
    	
        if (!WasdiConfig.readConfig(sConfigFilePath)) {
            System.err.println("WasdiScheduler.main - config file not available. Exit");
            System.exit(-1);            	
        }		

		
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
		
		LoggerWrapper oLoggerWrapper = new LoggerWrapper(s_oLogger);
		WasdiLog.setLoggerWrapper(oLoggerWrapper);
		
		WasdiLog.infoLog("main: Logger configured :-)\n");
		
		
		//mongo config
		try {
			WasdiLog.infoLog("main: Configuring mongo...");
			// Init Mongo Configuration
			MongoRepository.readConfig();	
		} 
		catch (Throwable oEx) {
			WasdiLog.errorLog("main: Mongo configuration failed. Reason: " + oEx);
			oEx.printStackTrace();
			System.exit(-1);
		}
		WasdiLog.infoLog("main: Mongo configured :-)\n");
		
		// Computational nodes need to configure also the local dababase
		try {
			// If this is not the main node
			if (!WasdiConfig.Current.nodeCode.equals("wasdi")) {
				
				// Configure also the local connection
				MongoRepository.addMongoConnection("local", WasdiConfig.Current.mongoLocal.user, WasdiConfig.Current.mongoLocal.password, WasdiConfig.Current.mongoLocal.address, WasdiConfig.Current.mongoLocal.replicaName, WasdiConfig.Current.mongoLocal.dbName);
				WasdiLog.debugLog("-------Addded Mongo Configuration local for " + WasdiConfig.Current.nodeCode);
			}			
		}
		catch (Throwable oEx) {
			WasdiLog.errorLog("main: Mongo configuration failed. Reason: " + oEx);
			oEx.printStackTrace();
		}		
		
		// Read the list of configured schedulers
		ArrayList<String> asSchedulers = new ArrayList<String>();
		try {
			WasdiLog.infoLog("main: reading schedulers configurations...");
			
			for (SchedulerQueueConfig oQueueConfig: WasdiConfig.Current.scheduler.schedulers) {
				asSchedulers.add(oQueueConfig.name);
				
			}
			
			// Read Node Code
			s_sWasdiNode = WasdiConfig.Current.nodeCode;
			
			// Read the sleep time beween steps
			try {
				long iThreadSleep = Long.parseLong(WasdiConfig.Current.scheduler.processingThreadSleepingTimeMS);
				if (iThreadSleep>0) {
					s_lSleepingTimeMS = iThreadSleep;
					WasdiLog.infoLog("main: CatNap Ms: " + s_lSleepingTimeMS);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}			
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog("Could not read schedulers configurations. Reason: " + oEx);
			oEx.printStackTrace();
			System.exit(-1);
		}
		
		WasdiLog.infoLog("main: preparing operations...");
		
		// List of active schedulers
		ArrayList<ProcessScheduler> aoProcessSchedulers = new ArrayList<ProcessScheduler>();
		
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
				WasdiLog.infoLog("main: Creating Scheduler: " + sScheduler);
				ProcessScheduler oProcessScheduler = new ProcessScheduler();
				oProcessScheduler.init(sScheduler);
				
				// Get the list of supported types
				List<String> asSchedulerTypes = new ArrayList<String>(oProcessScheduler.getSupportedTypes());
				
				for (String sSupportedType : asSchedulerTypes) {
					
					// Check if the type is "free"
					if (asWasdiOperationTypes.contains(sSupportedType) == false) {
						
						if (Utils.isNullOrEmpty(oProcessScheduler.getOperationSubType())) {
							// No: remove from the scheduler
							WasdiLog.errorLog("main: Scheduler " + sScheduler + " support type " + sSupportedType + " that does not exists or has already been supported by other scheduler. It will be removed");
							oProcessScheduler.removeSupportedType(sSupportedType);							
						}
						else {
							WasdiLog.infoLog("main: Assigning to Scheduler " + sScheduler + " support type " + sSupportedType + " SubType " + oProcessScheduler.getOperationSubType());
						}
					}
					else {
						// Check if here is also a subtype
						String sSubTypeLog = "";
						
						if (!Utils.isNullOrEmpty(oProcessScheduler.getOperationSubType())) {
							sSubTypeLog = " SubType " + oProcessScheduler.getOperationSubType();
						}
						
						// Yes: remove from the full list
						WasdiLog.infoLog("main: Assigning to Scheduler: " + sScheduler + " support type: " + sSupportedType + sSubTypeLog);
						asWasdiOperationTypes.remove(sSupportedType);
					}
				}
				
				SchedulerQueueConfig oSchedulerQueueConfig = WasdiConfig.Current.scheduler.getSchedulerQueueConfig(sScheduler.toUpperCase());
				
				String sSchedulerEnabled = oSchedulerQueueConfig.enabled;
				
				if (sSchedulerEnabled.equals("1")) {
					// Start the scheduler
					WasdiLog.infoLog("main: Adding Scheduler: " + sScheduler);
					aoProcessSchedulers.add(oProcessScheduler);		
				}
				else {
					WasdiLog.warnLog("main: Scheduler: " + sScheduler + " not enabled, will not start");
				}
			}
			
			// Do we have other types?
			if (asWasdiOperationTypes.size() > 0) {
				
				// Create and Init the Default Scheduler
				WasdiLog.infoLog("main: Creating DEFAULT Scheduler");
				ProcessScheduler oProcessScheduler = new ProcessScheduler();
				oProcessScheduler.init("DEFAULT");
				
				// Assign all the types
				for (String sOtherType : asWasdiOperationTypes) {
					WasdiLog.infoLog("main: Assigning to Scheduler: DEFAULT support type: " + sOtherType);
					oProcessScheduler.addSupportedType(sOtherType);
				}
				
				// Start
				WasdiLog.infoLog("main: Adding Scheduler: DEFAULT");
				aoProcessSchedulers.add(oProcessScheduler);
			}
			else {
				WasdiLog.infoLog("main: All types covered, do not start DEFAULT scheduler");
			}
			
		}
		catch( Exception oEx ) {
			WasdiLog.errorLog("Could not complete operations preparations. Reason: " + oEx);
			oEx.printStackTrace();
		}
		WasdiLog.infoLog("main: operations prepared, lets start \n");
		
		run(aoProcessSchedulers);
		
		WasdiLog.debugLog(new EndMessageProvider().getGood() + '\n');

	}
	
	/**
	 * Main infinite loop of the scheduler
	 * @param aoProcessSchedulers
	 */
	public static void run(ArrayList<ProcessScheduler> aoProcessSchedulers) {
		
		int iSometimes = 0;
		
		while(true) {
			
			iSometimes ++;
			
			
			List<ProcessWorkspace> aoProcessesList = s_oProcessWorkspaceRepository.getProcessesForSchedulerNode(s_sWasdiNode, "lastStateChangeDate");
			
			List<ProcessWorkspace> aoRunningList = getStateList(aoProcessesList, "RUNNING");
			List<ProcessWorkspace> aoReadyList = getStateList(aoProcessesList, "READY");
			List<ProcessWorkspace> aoCreatedList = getStateList(aoProcessesList, "CREATED");
			
			for (ProcessScheduler oScheduler : aoProcessSchedulers) {
				
				oScheduler.cycle(aoRunningList, aoReadyList, aoCreatedList);
			}
			
			if (iSometimes == s_iSometimesCounter) {
				iSometimes = 0;
				
				aoProcessesList = s_oProcessWorkspaceRepository.getProcessesForSchedulerNode(s_sWasdiNode, "lastStateChangeDate");
				
				aoRunningList = getStateList(aoProcessesList, "RUNNING");
				aoReadyList = getStateList(aoProcessesList, "READY");
				aoCreatedList = getStateList(aoProcessesList, "CREATED");
				List<ProcessWorkspace> aoWaitingList = getStateList(aoProcessesList, "WAITING");
				
				for (ProcessScheduler oScheduler : aoProcessSchedulers) {
					
					oScheduler.sometimesCheck(aoRunningList, aoCreatedList, aoReadyList, aoWaitingList);
				}				
				
			}
			
			try {
				//Sleep before starting next iteration
				catnap();
			} 
			catch (Exception oEx) {
				WasdiLog.errorLog("WasdiScheduler.run: exception with finally: " + oEx);
				oEx.printStackTrace();
			}
		}
		
	}
	
	/**
	 * Sleep method for the Scheduler Cycle
	 */
	protected static void catnap() {
		try {
			Thread.sleep(s_lSleepingTimeMS);
		} catch (Exception e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Get the list of running processes
	 * @return list of running processes
	 */
	protected static List<ProcessWorkspace> getStateList(List<ProcessWorkspace> aoProcessesList, String sStatus) {
		try {
			List<ProcessWorkspace> aoRunning = new ArrayList<>();
			
			for (ProcessWorkspace oProcessWorkspace : aoProcessesList) {
				if (oProcessWorkspace.getStatus().equals(sStatus)) aoRunning.add(oProcessWorkspace);
			}
			
			return aoRunning;
		}
		catch (Exception oE) {
			oE.printStackTrace();
			return null;
		}
	}
	
}
