package wasdi;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Config;
import org.esa.snap.runtime.Engine;
import org.esa.snap.runtime.EngineConfig;

import com.fasterxml.jackson.databind.ObjectMapper;

import sun.management.VMManagement;
import wasdi.geoserver.Publisher;
import wasdi.operations.Operation;
import wasdi.shared.business.Node;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Workspace;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.rabbit.RabbitFactory;
import wasdi.shared.rabbit.Send;
import wasdi.shared.utils.EndMessageProvider;
import wasdi.shared.utils.LoggerWrapper;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;

/**
 * WASDI Launcher Main Class
 * 
 * This class is the executor of all the WASDI Operations.
 * Some operations are done by the Launcher itself, others are User Processors that are triggered by the launcher.
 * 
 * The Launcher takes as input:
 * 
 * -operation -> Uppercase Name of the WASDI Operation. ie "DOWNLOAD" 
 * -parameter -> Path of the parameter file. ie /usr/lib/wasdi/params/fab5028a-341b-4bd3-ba7e-a321d6eb54ca
 * 
 * The parameter file should be named as guid that is the same guid of the corresponding ProcessWorkspace.
 * 
 * 
 */
public class LauncherMain  {

    /**
     * Static Logger that references the "MyApp" logger
     */
    public static LoggerWrapper s_oLogger = new LoggerWrapper(Logger.getLogger(LauncherMain.class));

    /**
     * Static reference to Send To Rabbit utility class. 
     * It is here created to be used "safe" in all the code. 
     * The launcher will try to recreate with the real configuration in the main function.
     */
    public static Send s_oSendToRabbit = new Send(null);

    /**
     * Actual node where the Launcher is running, main by default
     */
    public static String s_sNodeCode = "wasdi";
    
    /**
     * Process Workspace Logger: this object allow to write the logs
     * linked to the process workspace that can be seen by the users in the web client
     * or using the libraries.
     */
    protected ProcessWorkspaceLogger m_oProcessWorkspaceLogger;

    /**
     * Object mapper to convert Java - JSON
     */
    public static ObjectMapper s_oMapper = new ObjectMapper();
    
    /**
     * Actual Process Workspace
     */
    protected static ProcessWorkspace s_oProcessWorkspace;

    /**
     * WASDI Launcher Main Entry Point
     *
     * @param args -o <operation> -p <parameterfile>
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
    	
        try {
            Security.setProperty("crypto.policy", "unlimited");
            // get jar directory
            File oCurrentFile = new File(LauncherMain.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            // configure log
            String sThisFilePath = oCurrentFile.getParentFile().getPath();
            DOMConfigurator.configure(sThisFilePath + "/log4j.xml");

        } catch (Exception exp) {
            // no log4j configuration
            System.err.println("Launcher Main - Error loading log configuration.  Reason: " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(exp));
        }

        s_oLogger.debug("WASDI Launcher Main Start");

        // create the parser
        CommandLineParser oParser = new DefaultParser();

        // create Options object
        Options oOptions = new Options();

        oOptions.addOption("o", "operation", true, "WASDI Launcher Operation");
        oOptions.addOption("p", "parameter", true, "WASDI Operation Parameter");

        String sOperation = "ND";
        String sParameter = "ND";

        // parse the command line arguments
        CommandLine oLine = oParser.parse(oOptions, args);

        if (oLine.hasOption("operation")) {
            // Get the Operation Code
            sOperation = oLine.getOptionValue("operation");
        }

        if (oLine.hasOption("parameter")) {
            // Get the Parameter File
            sParameter = oLine.getOptionValue("parameter");
        }

        if (sParameter.equals("ND")) {
            System.err.println("Launcher Main - parameter file not available. Exit");
            System.exit(-1);
        }

        try {

            // Set Rabbit Factory Params
            RabbitFactory.s_sRABBIT_QUEUE_USER = ConfigReader.getPropValue("RABBIT_QUEUE_USER");
            RabbitFactory.s_sRABBIT_QUEUE_PWD = ConfigReader.getPropValue("RABBIT_QUEUE_PWD");
            RabbitFactory.s_sRABBIT_HOST = ConfigReader.getPropValue("RABBIT_HOST");
            RabbitFactory.s_sRABBIT_QUEUE_PORT = ConfigReader.getPropValue("RABBIT_QUEUE_PORT");

            // Create Launcher Instance
            LauncherMain.s_oSendToRabbit = new Send(ConfigReader.getPropValue("RABBIT_EXCHANGE", "amq.topic"));
            LauncherMain oLauncher = new LauncherMain();

            // Deserialize the parameter referring the base class
            BaseParameter oBaseParameter = (BaseParameter) SerializationUtils.deserializeXMLToObject(sParameter);
            ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
            s_oProcessWorkspace = oProcessWorkspaceRepository.getProcessByProcessObjId(oBaseParameter.getProcessObjId());

            if (s_oProcessWorkspace == null) {
                s_oLogger.error("Process Workspace null for parameter [" + sParameter + "]. Exit");
                System.exit(-1);
            }

            // Set the process object id as Logger Prefix
            s_oLogger.setPrefix("[" + s_oProcessWorkspace.getProcessObjId() + "]");
            s_oLogger.debug("Executing " + sOperation + " Parameter " + sParameter);

            // Set the process as running
            s_oLogger.debug("LauncherMain: setting ProcessWorkspace start date to now");
            s_oProcessWorkspace.setOperationStartDate(Utils.getFormatDate(new Date()));
            s_oProcessWorkspace.setStatus(ProcessStatus.RUNNING.name());
            s_oProcessWorkspace.setProgressPerc(0);
            s_oProcessWorkspace.setPid(getProcessId());

            if (!oProcessWorkspaceRepository.updateProcess(s_oProcessWorkspace)) {
                s_oLogger.error("LauncherMain: ERROR setting ProcessWorkspace start date and RUNNING STATE");
            } else {
                s_oLogger.debug("LauncherMain: RUNNING state and operationStartDate updated");
            }

            // And Run
            oLauncher.executeOperation(sOperation, sParameter);

            s_oLogger.debug(getBye());
            
        } 
        catch (Throwable oException) {
            s_oLogger.error("Launcher Main Exception " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oException));

            try {
                System.err.println("LauncherMain: try to put process [" + sParameter + "] in Safe ERROR state");

                if (s_oProcessWorkspace != null) {
                    s_oProcessWorkspace.setProgressPerc(100);
                    s_oProcessWorkspace.setOperationEndDate(Utils.getFormatDate(new Date()));
                    s_oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
                    
                    ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
                    
                    if (!oProcessWorkspaceRepository.updateProcess(s_oProcessWorkspace)) {
                        s_oLogger.debug("LauncherMain FINAL: Error during process update (terminated) " + sParameter);
                    }
                }
            } catch (Exception oInnerEx) {
                s_oLogger.error("Launcher Main FINAL-catch Exception " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oInnerEx));
            }
            
            System.exit(-1);
            
        } finally {

            // Final Check of the Process Workspace Status
            if (s_oProcessWorkspace != null) {
            	
            	// Read again the process workspace
            	ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
                
                s_oProcessWorkspace = oProcessWorkspaceRepository.getProcessByProcessObjId(s_oProcessWorkspace.getProcessObjId());

                s_oLogger.error("Launcher Main FINAL: process status [" + s_oProcessWorkspace.getProcessObjId() + "]: " + s_oProcessWorkspace.getStatus());

                if (s_oProcessWorkspace.getStatus().equals(ProcessStatus.RUNNING.name())
                        || s_oProcessWorkspace.getStatus().equals(ProcessStatus.CREATED.name())
                        || s_oProcessWorkspace.getStatus().equals(ProcessStatus.WAITING.name())
                        || s_oProcessWorkspace.getStatus().equals(ProcessStatus.READY.name())) {

                    s_oLogger.error("Launcher Main FINAL: process status not closed [" + s_oProcessWorkspace.getProcessObjId() + "]: " + s_oProcessWorkspace.getStatus());
                    s_oLogger.error("Launcher Main FINAL: force status as ERROR [" + s_oProcessWorkspace.getProcessObjId() + "]");

                    s_oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());

                    if (!oProcessWorkspaceRepository.updateProcess(s_oProcessWorkspace)) {
                        s_oLogger.debug("LauncherMain FINAL : Error during process update (terminated) " + sParameter);
                    }
                }
            }

            LauncherMain.s_oSendToRabbit.Free();
			
			try {
				Engine.getInstance().stop();
			} catch (Exception e) {
				e.printStackTrace();
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

            // Set Global Settings
            Publisher.GDAL_Retile_Command = ConfigReader.getPropValue("GDAL_RETILE", Publisher.GDAL_Retile_Command);
            MongoRepository.SERVER_ADDRESS = ConfigReader.getPropValue("MONGO_ADDRESS");
            MongoRepository.SERVER_PORT = Integer.parseInt(ConfigReader.getPropValue("MONGO_PORT"));
            MongoRepository.DB_NAME = ConfigReader.getPropValue("MONGO_DBNAME");
            MongoRepository.DB_USER = ConfigReader.getPropValue("MONGO_DBUSER");
            MongoRepository.DB_PWD = ConfigReader.getPropValue("MONGO_DBPWD");

            // Read this node code
            LauncherMain.s_sNodeCode = ConfigReader.getPropValue("WASDI_NODE", "wasdi");

            s_oLogger.debug("NODE CODE: " + LauncherMain.s_sNodeCode);

            // If this is not the main node
            if (!LauncherMain.s_sNodeCode.equals("wasdi")) {
            	
            	// Configure also the local connection: by default is the "wasdi" port + 1
                s_oLogger.debug("Adding local mongo config");
                MongoRepository.addMongoConnection("local", MongoRepository.DB_USER, MongoRepository.DB_PWD, MongoRepository.SERVER_ADDRESS, MongoRepository.SERVER_PORT + 1, MongoRepository.DB_NAME);
            }
            
            // Set the java/system user home folder
            System.setProperty("user.home", ConfigReader.getPropValue("USER_HOME"));
            
            // Configure snap to read aux data folder
            String sSnapAuxProperties = ConfigReader.getPropValue("SNAP_AUX_PROPERTIES");
            Path oPropFile = Paths.get(sSnapAuxProperties);
            Config.instance("snap.auxdata").load(oPropFile);
            Config.instance().load();
            
            // Init Snap
            SystemUtils.init3rdPartyLibs(null);
            Engine.start(false);

            // Snap Log: is activated or not?
            String sSnapLogActive = ConfigReader.getPropValue("SNAPLOGACTIVE", "1");

            if (sSnapLogActive.equals("1") || sSnapLogActive.equalsIgnoreCase("true")) {

                String sSnapLogLevel = ConfigReader.getPropValue("SNAPLOGLEVEL", "SEVERE");
                String sSnapLogFile = ConfigReader.getPropValue("SNAPLOGFOLDER", "/usr/lib/wasdi/launcher/logs/snaplauncher.log");

                s_oLogger.debug("SNAP Log file active with level " + sSnapLogLevel + " file: " + sSnapLogFile);

                Level oLogLevel = Level.SEVERE;

                try {
                    oLogLevel = Level.parse(sSnapLogLevel);
                } catch (Exception oEx) {
                    System.out.println("LauncherMain Constructor: exception configuring SNAP log file Level " + oEx.toString());
                }

                try {

                    SimpleFormatter oSimpleFormatter = new SimpleFormatter();

                    FileHandler oFileHandler = new FileHandler(sSnapLogFile, true);

                    oFileHandler.setLevel(oLogLevel);
                    oFileHandler.setFormatter(oSimpleFormatter);

                    EngineConfig oSnapConfig = Engine.getInstance().getConfig();
                    oSnapConfig.logLevel(oLogLevel);
                    java.util.logging.Logger oSnapLogger = oSnapConfig.logger();

                    oSnapLogger.addHandler(oFileHandler);

                    //SystemUtils.LOG.setLevel(oLogLevel);
                    //SystemUtils.LOG.addHandler(oFileHandler);

                } catch (Exception oEx) {
                    System.out.println("LauncherMain Constructor: exception configuring SNAP log file " + oEx.toString());
                }
            } else {
                s_oLogger.debug("SNAP Log file not active");
            }

        } catch (Throwable oEx) {
            s_oLogger.error("Launcher Main Constructor Exception " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
        }
    }

    /**
     * Executes the Operation.
     * Uses reflection to create the Operation Class and call the executeOperation specialized method.
     *
     * @param sOperation Operation to be done validated above the ones from enumeration
     * @param sParameter Parameter passed as file location of the parameter
     */
    public void executeOperation(String sOperation, String sParameter) {

        String sWorkspace = "";
        String sExchange = "";

        try {
        	
        	//Get the name of the class from the Operation
        	String sClassName = toTitleCase(sOperation.toLowerCase());
        	
        	// Re contrusct the full package class name
        	sClassName = "wasdi.operations." + sClassName;
        	
            // Deserialize the Parameter
            BaseParameter oBaseParameter = (BaseParameter) SerializationUtils.deserializeXMLToObject(sParameter);
            
            // Read Workspace and Exchange for Rabbit
            sWorkspace = oBaseParameter.getWorkspace();
            sExchange = oBaseParameter.getExchange();

            // Create the process workspace logger
            m_oProcessWorkspaceLogger = new ProcessWorkspaceLogger(oBaseParameter.getProcessObjId());
            
            // Create the operation class
        	Operation oOperation = (Operation) Class.forName(sClassName).newInstance();
        	
        	// Set the local logger
        	oOperation.setLocalLogger(s_oLogger);
        	// Set the process workspace logger
        	oOperation.setProcessWorkspaceLogger(m_oProcessWorkspaceLogger);
        	// Set the send to rabbit object
        	oOperation.setSendToRabbit(s_oSendToRabbit);
        	
        	// Call the execute operation method
        	boolean bOperationResult = oOperation.executeOperation(oBaseParameter, s_oProcessWorkspace);
        	
        	// Check the result of the operation and set the status
        	if (bOperationResult) s_oProcessWorkspace.setStatus(ProcessStatus.DONE.name());
        	else s_oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
        	
        	s_oLogger.debug("LauncherMain.executeOperation: Operation Result " + bOperationResult);

        } 
        catch (Exception oEx) {
        	            
            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);
            
            s_oLogger.error("LauncherMain.executeOperation Exception ", oEx);
            
            s_oSendToRabbit.SendRabbitMessage(false, sOperation, sWorkspace, sError, sExchange);
            
        }
        finally {
            // update process status and send rabbit updateProcess message
            closeProcessWorkspace();
            s_oLogger.debug("LauncherMain.executeOperation: CloseProcessWorkspace done");        	
        }

        s_oLogger.debug("Launcher did his job. Bye bye, see you soon. [" + sParameter + "]");
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
     * Get the full workspace path for this parameter
     *
     * @param oParameter Base Parameter
     * @return full workspace path
     */
    public static String getWorkspacePath(BaseParameter oParameter) {
        try {
            return getWorkspacePath(oParameter, ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH"));
        } catch (IOException e) {
            e.printStackTrace();
            return getWorkspacePath(oParameter, "/data/wasdi");
        }
    }

    /**
     * Get the full workspace path for this parameter
     *
     * @param oParameter
     * @param sRootPath
     * @return full workspace path
     */
    public static String getWorkspacePath(BaseParameter oParameter, String sRootPath) {
        // Get Base Path
        String sWorkspacePath = sRootPath;

        if (!(sWorkspacePath.endsWith("/") || sWorkspacePath.endsWith("//")))
            sWorkspacePath += "/";

        String sUser = oParameter.getUserId();

        if (Utils.isNullOrEmpty(oParameter.getWorkspaceOwnerId()) == false) {
            sUser = oParameter.getWorkspaceOwnerId();
        }

        // Get Workspace path
        sWorkspacePath += sUser;
        sWorkspacePath += "/";
        sWorkspacePath += oParameter.getWorkspace();
        sWorkspacePath += "/";

        return sWorkspacePath;
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
            s_oLogger.error("LauncherMain.updateProcessStatus oProcessWorkspace is null");
            return;
        }
        if (oProcessWorkspaceRepository == null) {
            s_oLogger.error("LauncherMain.updateProcessStatus oProcessWorkspace is null");
            return;
        }
        
        try {
            oProcessWorkspace.setStatus(oProcessStatus.name());
            oProcessWorkspace.setProgressPerc(iProgressPerc);
            
            // update the process
            if (!oProcessWorkspaceRepository.updateProcess(oProcessWorkspace)) {
                s_oLogger.debug("Error during process update");
            }
            
            if (!s_oSendToRabbit.SendUpdateProcessMessage(oProcessWorkspace)) {
                s_oLogger.debug("Error sending rabbitmq message to update process list");
            }        	
        }
        catch (Exception oEx) {
        	s_oLogger.debug("LauncherMain.updateProcessStatus Exception "+oEx.toString());
		}
    }

    /**
     * Close the Process on the mongo Db. Set progress to 100 and end date time
     */
    private void closeProcessWorkspace() {
        try {
            s_oLogger.debug("LauncherMain.CloseProcessWorkspace");
            
            if (s_oProcessWorkspace != null) {
            	
                // Set Progress Perc and Operation End Date
            	s_oProcessWorkspace.setProgressPerc(100);
            	s_oProcessWorkspace.setOperationEndDate(Utils.getFormatDate(new Date()));
                
                ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
                
                if (!oProcessWorkspaceRepository.updateProcess(s_oProcessWorkspace)) {
                    s_oLogger.debug("LauncherMain.CloseProcessWorkspace: Error during process workspace update");
                }
                
                s_oSendToRabbit.SendUpdateProcessMessage(s_oProcessWorkspace);
            }
        } catch (Exception oEx) {
            s_oLogger.debug("LauncherMain.CloseProcessWorkspace: Exception closing process workspace "
                    + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
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
            RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
            Field jvmField = runtimeMXBean.getClass().getDeclaredField("jvm");
            jvmField.setAccessible(true);
            VMManagement vmManagement = (VMManagement) jvmField.get(runtimeMXBean);
            Method getProcessIdMethod = vmManagement.getClass().getDeclaredMethod("getProcessId");
            getProcessIdMethod.setAccessible(true);
            iPid = (Integer) getProcessIdMethod.invoke(vmManagement);

        } catch (Throwable oEx) {
            try {
                s_oLogger.error("LauncherMain.GetProcessId: Error getting processId: "
                        + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
            } finally {
                s_oLogger.error("LauncherMain.GetProcessId: finally here");
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
        } catch (Exception oEx) {
            s_oLogger.error("LauncherMain.waitForProcessResume: " + oEx.toString());
        }

        return "ERROR";
    }
    
    public static String adjustGdalFolder(String sGdalCommand) {
        try {
            String sGdalPath = ConfigReader.getPropValue("GDAL_PATH", "");

            if (!Utils.isNullOrEmpty(sGdalPath)) {
                File oGdalFolder = new File(sGdalPath);
                if (oGdalFolder.exists()) {
                    if (oGdalFolder.isDirectory()) {
                        if (!sGdalPath.endsWith("" + File.separatorChar)) sGdalPath = sGdalPath + File.separatorChar;
                        sGdalCommand = sGdalPath + sGdalCommand;
                    }
                }
            }
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }


        return sGdalCommand;

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


