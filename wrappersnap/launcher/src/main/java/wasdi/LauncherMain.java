package wasdi;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.lib.openjpeg.utils.OpenJpegExecRetriever;
import org.esa.snap.runtime.Config;
import org.esa.snap.runtime.Engine;
import org.esa.snap.runtime.EngineConfig;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import wasdi.operations.Operation;
import wasdi.shared.business.Node;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Workspace;
import wasdi.shared.business.users.User;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.ParametersRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.rabbit.RabbitFactory;
import wasdi.shared.rabbit.Send;
import wasdi.shared.utils.EndMessageProvider;
import wasdi.shared.utils.MailUtils;
import wasdi.shared.utils.ProcessWorkspaceLogger;
import wasdi.shared.utils.TimeEpochUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.LoggerWrapper;
import wasdi.shared.utils.log.WasdiLog;

/**
 * WASDI Launcher Main Class
 *
 * This class is the executor of all the WASDI Operations.
 * Some operations are done by the Launcher itself, others are User Processors that are triggered by the launcher.
 *
 * The Launcher takes as input:
 *
 * -operation -> Uppercase Name of the WASDI Operation. ie "DOWNLOAD"
 * -parameter -> Path of the parameter file. ie /data/wasdi/params/fab5028a-341b-4bd3-ba7e-a321d6eb54ca
 * -config -> Path of te WASDI JSON Config file. ie /etc/wasdi/config.json
 *
 * The parameter file should be named as guid that is the same guid of the corresponding ProcessWorkspace.
 *
 *
 */
public class LauncherMain  {

    /**
     * Static Logger that references the "MyApp" logger
     */
    public static LoggerWrapper s_oLogger = new LoggerWrapper(LogManager.getLogger(LauncherMain.class));

    /**
     * Static reference to Send To Rabbit utility class.
     * It is here created to be used "safe" in all the code.
     * The launcher will try to recreate with the real configuration in the main function.
     */
    public static Send s_oSendToRabbit = new Send(null);

    /**
     * Process Workspace Logger: this object allow to write the logs
     * linked to the process workspace that can be seen by the users in the web client
     * or using the libraries.
     */
    protected ProcessWorkspaceLogger m_oProcessWorkspaceLogger;

    /**
     * Object mapper to convert Java - JSON. This is an heavy object
     * so it is istanciated once as static object to be reused where ever
     * it is needed
     */
    public static ObjectMapper s_oMapper = new ObjectMapper();

    /**
     * Actual Process Workspace
     */
    protected static ProcessWorkspace s_oProcessWorkspace;

    /**
     * WASDI Launcher Main Entry Point
     *
     * @param args -o <operation> -p <parameterfile> -c <configfile>
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        try {
        	// Set crypto Policy for sftp connections
            Security.setProperty("crypto.policy", "unlimited");
            
            // configure log4j2
            File oCurrentFile = new File(LauncherMain.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            String sThisFilePath = oCurrentFile.getParentFile().getPath();
            
            WasdiFileUtils.loadLogConfigFile(sThisFilePath);
            
        } catch (Exception exp) {
            System.err.println("Launcher Main - Error setting the crypto policy.  Reason: " + ExceptionUtils.getStackTrace(exp));
        }

        WasdiLog.debugLog("WASDI Launcher Main Start");

        // We need to read the command line parameters.

        // create the parser
        CommandLineParser oParser = new DefaultParser();

        // create Options object
        Options oOptions = new Options();

        // Operation to be executed
        oOptions.addOption("o", "operation", true, "WASDI Launcher Operation");
        // Parameter file path
        oOptions.addOption("p", "parameter", true, "WASDI Operation Parameter");
        // Config file path
        oOptions.addOption("c", "config", true, "WASDI Configuration File Path");

        // Default initialization
        String sOperation = "ND";
        String sParameter = "ND";
        String sConfigFilePath = "/etc/wasdi/wasdiConfig.json";

        // parse the command line arguments
        CommandLine oLine = oParser.parse(oOptions, args);

        // Get the Operation Code
        if (oLine.hasOption("operation")) {
            sOperation = oLine.getOptionValue("operation");
        }

        // Check if it is available
        if (sOperation.equals("ND")) {
            System.err.println("Launcher Main - operation not available. Exit");
            System.exit(-1);
        }

        // Get the Parameter File
        if (oLine.hasOption("parameter")) {
            sParameter = oLine.getOptionValue("parameter");
        }

        // Check if it is available
        if (sParameter.equals("ND")) {
            System.err.println("Launcher Main - parameter file not available. Exit");
            System.exit(-1);
        }

        // Get the Config File
        if (oLine.hasOption("config")) {
        	sConfigFilePath = oLine.getOptionValue("config");
        }

        // Check if it is available
        if (!WasdiConfig.readConfig(sConfigFilePath)) {
            System.err.println("Launcher Main - config file not found. Exit");
            System.exit(-1);
        }
        
        if (WasdiConfig.Current.useLog4J) {
            // Set the logger for the shared lib
            WasdiLog.setLoggerWrapper(s_oLogger);
            WasdiLog.debugLog("Launcher Main - Logger added");
        }
        else { 
        	WasdiLog.debugLog("Launcher Main - WASDI Configured to log on console");
        	WasdiLog.initLogger(WasdiConfig.Current.logLevelLauncher);
        }

        // Filter the mongodb logs
  		try {
  			System.setProperty("DEBUG.MONGO", "false");
  			ch.qos.logback.classic.Logger oMongoLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.mongodb.driver");
  			oMongoLogger.setLevel(ch.qos.logback.classic.Level.WARN);  			
  		}
  		catch (Exception oEx) {
  			WasdiLog.errorLog("Disabling mongo driver logging exception " + oEx.toString());
  		} 
  		
        // Filter the apache logs
  		try {
  			ch.qos.logback.classic.Logger oLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("httpclient");
  			oLogger.setLevel(ch.qos.logback.classic.Level.WARN);  			
  		}
  		catch (Exception oEx) {
  			WasdiLog.errorLog("Disabling mongo driver logging exception " + oEx.toString());
  		}   		
  		
        try {

            // Set Rabbit Factory Params
        	RabbitFactory.readConfig();
        	
            // Set the Mongo Config
            MongoRepository.readConfig();

            // Create the Rabbit Sender Object. The object is safe: in case of any problem
            // It does not send to rabbit but only logs on console.
            LauncherMain.s_oSendToRabbit = new Send(WasdiConfig.Current.rabbit.exchange);

            // Create Launcher Instance
            LauncherMain oLauncher = new LauncherMain();

            // Deserialize the parameter
            ParametersRepository oParametersRepository = new ParametersRepository();
            BaseParameter oBaseParameter = oParametersRepository.getParameterByProcessObjId(sParameter);

            // Read the Process Workspace from the db
            ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
            s_oProcessWorkspace = oProcessWorkspaceRepository.getProcessByProcessObjId(oBaseParameter.getProcessObjId());

            // This is the operation we have to do, it must exists
            if (s_oProcessWorkspace == null) {
                WasdiLog.errorLog("Process Workspace null for parameter [" + sParameter + "]. Are you sure the configured Node is correct? Exit");
                System.exit(-1);
            }

            // Set the process object id as Logger Prefix: it will help to filter logs
            WasdiLog.setPrefix("[" + s_oProcessWorkspace.getProcessObjId() + "]");
            WasdiLog.infoLog("Executing " + sOperation + " Parameter " + sParameter);

            // Set the ProcessWorkspace STATUS as running
            WasdiLog.debugLog("LauncherMain: setting ProcessWorkspace start date to now");
            s_oProcessWorkspace.setOperationStartTimestamp(Utils.nowInMillis());
            s_oProcessWorkspace.setStatus(ProcessStatus.RUNNING.name());
            s_oProcessWorkspace.setProgressPerc(0);
            s_oProcessWorkspace.setPid(getProcessId());

            if (!oProcessWorkspaceRepository.updateProcess(s_oProcessWorkspace)) {
                WasdiLog.errorLog("LauncherMain: ERROR setting ProcessWorkspace start date and RUNNING STATE");
            } else {
                WasdiLog.debugLog("LauncherMain: RUNNING state and operationStartDate updated");
            }
            
            // Run the operation
            oLauncher.executeOperation(sOperation, oBaseParameter);

            // Operation Done
            WasdiLog.infoLog(getBye());

        }
        catch (Throwable oException) {

        	// ERROR: we need to put the ProcessWorkspace in a end-state, error in this case
            WasdiLog.errorLog("Launcher Main Exception " + ExceptionUtils.getStackTrace(oException));

            try {
                System.err.println("LauncherMain: try to put process [" + sParameter + "] in Safe ERROR state");

                if (s_oProcessWorkspace != null) {

                    s_oProcessWorkspace.setProgressPerc(100);
                    s_oProcessWorkspace.setOperationEndTimestamp(Utils.nowInMillis());
                    s_oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());

                    ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();

                    if (!oProcessWorkspaceRepository.updateProcess(s_oProcessWorkspace)) {
                        WasdiLog.debugLog("LauncherMain FINAL: Error during process update (terminated) " + sParameter);
                    }
                }
            } catch (Exception oInnerEx) {
                WasdiLog.errorLog("Launcher Main FINAL-catch Exception " + ExceptionUtils.getStackTrace(oInnerEx));
            }

            // Exit with error
            System.exit(-1);

        } finally {

            // Final Check of the Process Workspace Status: it is in safe state?
            if (s_oProcessWorkspace != null) {

            	// Read again the process workspace
            	ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();

                s_oProcessWorkspace = oProcessWorkspaceRepository.getProcessByProcessObjId(s_oProcessWorkspace.getProcessObjId());

                WasdiLog.errorLog("Launcher Main FINAL: process status [" + s_oProcessWorkspace.getProcessObjId() + "]: " + s_oProcessWorkspace.getStatus());

                // If it is not in a runnig state
                if (s_oProcessWorkspace.getStatus().equals(ProcessStatus.RUNNING.name())
                        || s_oProcessWorkspace.getStatus().equals(ProcessStatus.CREATED.name())
                        || s_oProcessWorkspace.getStatus().equals(ProcessStatus.WAITING.name())
                        || s_oProcessWorkspace.getStatus().equals(ProcessStatus.READY.name())) {


                	// Force the closing
                    WasdiLog.errorLog("Launcher Main FINAL: process status not closed [" + s_oProcessWorkspace.getProcessObjId() + "]: " + s_oProcessWorkspace.getStatus());
                    WasdiLog.errorLog("Launcher Main FINAL: force status as ERROR [" + s_oProcessWorkspace.getProcessObjId() + "]");

                    s_oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());

                    if (!oProcessWorkspaceRepository.updateProcess(s_oProcessWorkspace)) {
                        WasdiLog.debugLog("LauncherMain FINAL : Error during process update (terminated) " + sParameter);
                    }
                }
            }

            // Free Rabbit resources
            LauncherMain.s_oSendToRabbit.Free();

			try {
				// Stop SNAP Engine
				Engine.getInstance().stop();
			} catch (Exception oE) {
				WasdiLog.errorLog("main: while doing Engine.getInstance().stop(): " + oE);
			}
        }
    }

    /**
     * Get the bye message for logger
     *
     * @return
     */
    private static String getBye() {
        return new EndMessageProvider().getGood();
    }

    /**
     * Constructor
     */
    public LauncherMain() {
        try {

            // Read this node code
            WasdiLog.debugLog("NODE CODE: " + WasdiConfig.Current.nodeCode);

            // If this is not the main node
            if (!WasdiConfig.Current.isMainNode()) {
            	// Configure also the local connection
                WasdiLog.debugLog("Adding local mongo config");
                MongoRepository.addMongoConnection("local", WasdiConfig.Current.mongoLocal.user, WasdiConfig.Current.mongoLocal.password, WasdiConfig.Current.mongoLocal.address, WasdiConfig.Current.mongoLocal.replicaName, WasdiConfig.Current.mongoLocal.dbName);
            }

            // Set the java/system user home folder
            System.setProperty("user.home", WasdiConfig.Current.paths.userHomePath);

            // Configure SNAP
            configureSNAP();

        } catch (Throwable oEx) {
            WasdiLog.errorLog("Launcher Main Constructor Exception " + ExceptionUtils.getStackTrace(oEx));
        }
    }

    /**
     * Configure SNAP: it set needed system variables and configure
     * the specific Logger
     */
    protected void configureSNAP() {
    	try {
            // Configure snap to read aux data folder
            String sSnapAuxProperties = WasdiConfig.Current.snap.auxPropertiesFile;
            Path oPropFile = Paths.get(sSnapAuxProperties);
            Config.instance("snap.auxdata").load(oPropFile);
            Config.instance().load();

            // Init Snap
            SystemUtils.init3rdPartyLibs(null);
            Engine.start(false);

            if (WasdiConfig.Current.snap.launcherLogActive) {

                String sSnapLogLevel = WasdiConfig.Current.snap.launcherLogLevel;
                String sSnapLogFile = WasdiConfig.Current.snap.launcherLogFile;

                WasdiLog.debugLog("SNAP Log file active with level " + sSnapLogLevel + " file: " + sSnapLogFile);

                Level oLogLevel = Level.SEVERE;

                try {
                    oLogLevel = Level.parse(sSnapLogLevel);
                } catch (Exception oEx) {
                    WasdiLog.errorLog("LauncherMain.configureSNAP: exception configuring SNAP log file Level " + oEx.toString());
                }

                try {

                    SimpleFormatter oSimpleFormatter = new SimpleFormatter();
                    
                    if (WasdiConfig.Current.shellExecLocally) {
                        FileHandler oFileHandler = new FileHandler(sSnapLogFile, true);

                        oFileHandler.setLevel(oLogLevel);
                        oFileHandler.setFormatter(oSimpleFormatter);

                        EngineConfig oSnapConfig = Engine.getInstance().getConfig();
                        oSnapConfig.logLevel(oLogLevel);
                        java.util.logging.Logger oSnapLogger = oSnapConfig.logger();

                        oSnapLogger.addHandler(oFileHandler);                    	
                    }
                    else {
                    	WasdiLog.debugLog("LauncherMain.configureSNAP: dockerized version is on: we set a console handler instead of the file one");
                    	
                        ConsoleHandler oConsoleHandler = new ConsoleHandler();

                        oConsoleHandler.setLevel(oLogLevel);
                        oConsoleHandler.setFormatter(oSimpleFormatter);

                        EngineConfig oSnapConfig = Engine.getInstance().getConfig();
                        oSnapConfig.logLevel(oLogLevel);
                        java.util.logging.Logger oSnapLogger = oSnapConfig.logger();

                        oSnapLogger.addHandler(oConsoleHandler);                    	
                    }
                } 
                catch (Exception oEx) {
                	WasdiLog.errorLog("LauncherMain.configureSNAP: exception configuring SNAP log file " + oEx.toString());
                }
            } 
            else {
                WasdiLog.debugLog("SNAP Log file not active: clean log handlers");
                
                try {

                    EngineConfig oSnapConfig = Engine.getInstance().getConfig();
                    java.util.logging.Logger oSnapLogger = oSnapConfig.logger();
                    
                    Handler[] aoHandlers = oSnapLogger.getHandlers();
                    
                    for (Handler oHandler : aoHandlers) {
                    	oSnapLogger.removeHandler(oHandler);
						
					}

                } catch (Exception oEx) {
                	WasdiLog.errorLog("LauncherMain.configureSNAP: exception cleaning SNAP log Handlers " + oEx.toString());
                }                
            }
            
            try {
            	// Print the openjpeg path
            	Path oPath = OpenJpegExecRetriever.getOpenJPEGAuxDataPath();
            	
            	if (oPath != null) {
            		WasdiLog.debugLog("getOpenJPEGAuxDataPath = " + oPath.toString());
            	}
            	else {
            		WasdiLog.debugLog("getOpenJPEGAuxDataPath = null");
            	}
            }
            catch (Throwable oEx) {
            	WasdiLog.errorLog("LauncherMain.configureSNAP Exception OpenJpegExecRetriever.getOpenJPEGAuxDataPath(): " + ExceptionUtils.getStackTrace(oEx));
			}
    	}
    	catch (Throwable oEx) {
            WasdiLog.errorLog("LauncherMain.configureSNAP Exception " + ExceptionUtils.getStackTrace(oEx));
        }
    }

    /**
     * Executes the Operation.
     * Uses reflection to create the Operation Class and call the executeOperation specialized method.
     *
     * @param sOperation Operation to be done validated above the ones from enumeration
     * @param sParameter Parameter passed as file location of the parameter
     */
    public void executeOperation(String sOperation, BaseParameter oBaseParameter) {

        String sWorkspace = "";
        String sExchange = "";

        try {

        	//Get the name of the class from the Operation
        	String sClassName = toTitleCase(sOperation.toLowerCase());

        	// Re contrusct the full package class name
        	sClassName = "wasdi.operations." + sClassName;

            // Read Workspace and Exchange for Rabbit
            sWorkspace = oBaseParameter.getWorkspace();
            sExchange = oBaseParameter.getExchange();

            // Create the process workspace logger
            m_oProcessWorkspaceLogger = new ProcessWorkspaceLogger(oBaseParameter.getProcessObjId());

            // Create the operation class
        	Operation oOperation = (Operation) Class.forName(sClassName).getDeclaredConstructor().newInstance();
        	
        	// Set the process workspace logger
        	oOperation.setProcessWorkspaceLogger(m_oProcessWorkspaceLogger);
        	// Set the send to rabbit object
        	oOperation.setSendToRabbit(s_oSendToRabbit);

        	// Call the execute operation method
        	boolean bOperationResult = oOperation.executeOperation(oBaseParameter, s_oProcessWorkspace);
        	
        	// Re-Read the process workspace; may have been changed from the Operation
            ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
            s_oProcessWorkspace = oProcessWorkspaceRepository.getProcessByProcessObjId(s_oProcessWorkspace.getProcessObjId());
        	
        	// If the process workspace is not in a safe state
        	if (!s_oProcessWorkspace.getStatus().equals("DONE") && !s_oProcessWorkspace.getStatus().equals("ERROR") && !s_oProcessWorkspace.getStatus().equals("STOPPED")) {
            	// Check the result of the operation and set the status
            	if (bOperationResult) s_oProcessWorkspace.setStatus(ProcessStatus.DONE.name());
            	else s_oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());        		
        	}

        	WasdiLog.infoLog("LauncherMain.executeOperation: Operation Result " + bOperationResult);
        	
        	// Check if we have to send a notification
        	if (s_oProcessWorkspace.isNotifyOwnerByMail()) {
        		
        		WasdiLog.debugLog("LauncherMain.executeOperation: Notify the owner about this run with an mail");
        		
        		String sTitle = "WASDI Operation Report - " + s_oProcessWorkspace.getProductName() + " - " + s_oProcessWorkspace.getStatus();
        		
        		UserRepository oUserRepository = new UserRepository();
        		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
        		User oUser = oUserRepository.getUser(s_oProcessWorkspace.getUserId());
        		Workspace oWorkspace = oWorkspaceRepository.getWorkspace(s_oProcessWorkspace.getWorkspaceId());
        		
        		
        		String sMessage = "Hi " + oUser.getSafeUserName()+",\n";
        		sMessage += "\tYour process of type " + s_oProcessWorkspace.getOperationType() + " - " + s_oProcessWorkspace.getProductName() + " [Operation Id: " + s_oProcessWorkspace.getProcessObjId()+"] ";
        		sMessage += " in the workspace " + oWorkspace.getName() + " [Workspace Id: " + oWorkspace.getWorkspaceId() + "]";
        		sMessage += " finished at " + TimeEpochUtils.fromEpochToDateString(Utils.nowInMillis());
        		sMessage += " with Status " + s_oProcessWorkspace.getStatus() + "\n\n";
        		sMessage += "You are receiving this mail since you are the ownwer of the Process and a Notification Request has been added.\n";
        		sMessage += "Check the configuration in WASDI or contact us to stop receiving these mails.\n";
        		sMessage += "Best Regards\nWASDI";
        		
        		
        		if (!MailUtils.sendEmail(s_oProcessWorkspace.getUserId(), sTitle, sMessage)) {
        			WasdiLog.errorLog("LauncherMain.executeOperation: error sending mail to the owner of the operation");
        		}
        	}

        }
        catch (Exception oEx) {

            String sError = ExceptionUtils.getMessage(oEx);

            WasdiLog.errorLog("LauncherMain.executeOperation Exception ", oEx);

            s_oSendToRabbit.SendRabbitMessage(false, sOperation, sWorkspace, sError, sExchange);

        }
        finally {
            // update process status and send rabbit updateProcess message
            closeProcessWorkspace();
            WasdiLog.debugLog("LauncherMain.executeOperation: CloseProcessWorkspace done");
        }
        
        String sParameter = "ND";
        if (oBaseParameter!=null) {
        	if (oBaseParameter.getProcessObjId()!=null) {
        		sParameter = oBaseParameter.getProcessObjId();
        	}
        }

        WasdiLog.infoLog("Launcher did his job. Bye bye, see you soon. [" + sParameter + "]");
    }

    /**
     * Get The node corresponding to the workspace
     *
     * @param sWorkspaceId Id of the Workspace
     * @return Node object
     */
    public static Node getWorkspaceNode(String sWorkspaceId) {

        WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
        Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);

        if (oWorkspace == null)
            return null;

        String sNodeCode = oWorkspace.getNodeCode();

        if (Utils.isNullOrEmpty(sNodeCode))
            return null;

        NodeRepository oNodeRepo = new NodeRepository();
        Node oNode = oNodeRepo.getNodeByCode(sNodeCode);

        return oNode;
    }


    /**
     * Static helper function to update status and progress of a Process Workspace.
     * Used mainly by Operation class, that has a wrapper, can be used also by other classes not
     * directly derived from Operation, like the DataProviders or the Processor Hierarcy.
     *
     * @param oProcessWorkspaceRepository Repo to access db
     * @param oProcessWorkspace Process Workspace Object
     * @param oProcessStatus Updated Status
     * @param iProgressPerc Updated Progress percentage
     */
    public static void updateProcessStatus(ProcessWorkspaceRepository oProcessWorkspaceRepository,
                                           ProcessWorkspace oProcessWorkspace, ProcessStatus oProcessStatus, int iProgressPerc)
    {

        if (oProcessWorkspace == null) {
            WasdiLog.errorLog("LauncherMain.updateProcessStatus oProcessWorkspace is null");
            return;
        }
        if (oProcessWorkspaceRepository == null) {
            WasdiLog.errorLog("LauncherMain.updateProcessStatus oProcessWorkspace is null");
            return;
        }

        try {
            oProcessWorkspace.setStatus(oProcessStatus.name());
            oProcessWorkspace.setProgressPerc(iProgressPerc);

            // update the process
            if (!oProcessWorkspaceRepository.updateProcess(oProcessWorkspace)) {
                WasdiLog.debugLog("Error during process update");
            }

            if (!s_oSendToRabbit.SendUpdateProcessMessage(oProcessWorkspace)) {
                WasdiLog.debugLog("Error sending rabbitmq message to update process list");
            }
        }
        catch (Exception oEx) {
        	WasdiLog.debugLog("LauncherMain.updateProcessStatus Exception "+oEx.toString());
		}
    }

    /**
     * Close the Process on the mongo Db. Set progress to 100 and end date time
     */
    private void closeProcessWorkspace() {
        try {
            WasdiLog.debugLog("LauncherMain.CloseProcessWorkspace");

            if (s_oProcessWorkspace != null) {

                // Set Progress Perc and Operation End Date
            	s_oProcessWorkspace.setProgressPerc(100);
            	s_oProcessWorkspace.setOperationEndTimestamp(Utils.nowInMillis());

                ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();

                if (!oProcessWorkspaceRepository.updateProcess(s_oProcessWorkspace)) {
                    WasdiLog.debugLog("LauncherMain.CloseProcessWorkspace: Error during process workspace update");
                }

                s_oSendToRabbit.SendUpdateProcessMessage(s_oProcessWorkspace);
            }
        } catch (Exception oEx) {
            WasdiLog.debugLog("LauncherMain.CloseProcessWorkspace: Exception closing process workspace " + ExceptionUtils.getStackTrace(oEx));
        }
    }

    /**
     * Get the id of the process
     *
     * @return
     */
	private static Integer getProcessId() {
        Integer iPid = 0;
        try {
            iPid = (int) ProcessHandle.current().pid();
        } catch (Throwable oEx) {
            try {
                WasdiLog.errorLog("LauncherMain.GetProcessId: Error getting processId: " + oEx.toString());
            } finally {
                WasdiLog.errorLog("LauncherMain.GetProcessId: finally here");
            }
        }

        return iPid;
    }

    /**
     * Wait for a process to be resumed in a state like RUNNING, ERROR or DONE
     *
     * @param oProcessWorkspace Process Workspace to wait that should be in READY
     * @return output status of the process
     */
    public static String waitForProcessResume(ProcessWorkspace oProcessWorkspace) {
        try {

            ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();

            while (true) {
                if (oProcessWorkspace.getStatus().equals(ProcessStatus.RUNNING.name()) || oProcessWorkspace.getStatus().equals(ProcessStatus.ERROR.name()) || oProcessWorkspace.getStatus().equals(ProcessStatus.STOPPED.name())) {
                    return oProcessWorkspace.getStatus();
                }

                Thread.sleep(5000);
                oProcessWorkspace = oProcessWorkspaceRepository.getProcessByProcessObjId(oProcessWorkspace.getProcessObjId());
            }
        } 
        catch (InterruptedException oEx){
        	Thread.currentThread().interrupt();
        	WasdiLog.errorLog("LauncherMain.waitForProcessResume: current thread was interrupted", oEx);
        }
        catch (Exception oEx) {
            WasdiLog.errorLog("LauncherMain.waitForProcessResume: " + oEx.toString());
        }

        return "ERROR";
    }
	
	public static String toTitleCase(String sInput) {
	    StringBuilder sTitleCase = new StringBuilder(sInput.length());
	    boolean bNextTitleCase = true;

	    for (char cChar : sInput.toCharArray()) {
	        if (Character.isSpaceChar(cChar)) {
	            bNextTitleCase = true;
	        } else if (bNextTitleCase) {
	            cChar = Character.toTitleCase(cChar);
	            bNextTitleCase = false;
	        }

	        sTitleCase.append(cChar);
	    }

	    return sTitleCase.toString();
	}
}


