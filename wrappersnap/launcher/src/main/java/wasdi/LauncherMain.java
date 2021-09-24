package wasdi;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.net.io.Util;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.geotiff.GeoTIFF;
import org.esa.snap.core.util.geotiff.GeoTIFFMetadata;
import org.esa.snap.dataio.geotiff.GeoTiffProductWriterPlugIn;
import org.esa.snap.runtime.Config;
import org.esa.snap.runtime.Engine;
import org.esa.snap.runtime.EngineConfig;
import org.geotools.referencing.CRS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import sun.management.VMManagement;
import wasdi.asynch.SaveMetadataThread;
import wasdi.filebuffer.ProviderAdapter;
import wasdi.filebuffer.ProviderAdapterFactory;
import wasdi.geoserver.Publisher;
import wasdi.io.WasdiProductReader;
import wasdi.io.WasdiProductWriter;
import wasdi.processors.WasdiProcessorEngine;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.DownloadedFileCategory;
import wasdi.shared.business.Node;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Processor;
import wasdi.shared.business.ProductWorkspace;
import wasdi.shared.business.PublishedBand;
import wasdi.shared.business.Workspace;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.ProductWorkspaceRepository;
import wasdi.shared.data.PublishedBandsRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.geoserver.GeoServerManager;
import wasdi.shared.launcherOperations.LauncherOperationsUtils;
import wasdi.shared.parameters.*;
import wasdi.shared.payload.DownloadPayload;
import wasdi.shared.payload.FTPUploadPayload;
import wasdi.shared.payload.IngestPayload;
import wasdi.shared.payload.MosaicPayload;
import wasdi.shared.payload.MultiSubsetPayload;
import wasdi.shared.payload.PublishBandPayload;
import wasdi.shared.rabbit.RabbitFactory;
import wasdi.shared.rabbit.Send;
import wasdi.shared.utils.BandImageManager;
import wasdi.shared.utils.EndMessageProvider;
import wasdi.shared.utils.FtpClient;
import wasdi.shared.utils.LoggerWrapper;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.ShapeFileUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.ZipExtractor;
import wasdi.shared.viewmodels.GeorefProductViewModel;
import wasdi.shared.viewmodels.MetadataViewModel;
import wasdi.shared.viewmodels.ProductViewModel;
import wasdi.shared.viewmodels.PublishBandResultViewModel;
import wasdi.snapopearations.Mosaic;
import wasdi.snapopearations.WasdiGraph;

/**
 * WASDI Launcher Main Class
 */
public class LauncherMain implements ProcessWorkspaceUpdateSubscriber {

    /**
     * Static Logger that references the "MyApp" logger
     */
    public static LoggerWrapper s_oLogger = new LoggerWrapper(Logger.getLogger(LauncherMain.class));

    /**
     * Static reference to Send To Rabbit utility class
     */
    public static Send s_oSendToRabbit = null;

    /**
     * Actual node, main by default
     */
    public static String s_sNodeCode = "wasdi";

    /**
     * Flag to know if update or not the progress of download operations in the database
     */
    protected boolean m_bNotifyDownloadUpdateActive = true;

    /**
     * Process Workspace Logger
     */
    protected ProcessWorkspaceLogger m_oProcessWorkspaceLogger;

    /**
     * Object mapper to convert Java - JSON
     */
    public static ObjectMapper s_oMapper = new ObjectMapper();

    protected static ProcessWorkspace s_oProcessWorkspace;

    /*
     * System tomcat user
     */
    private String m_sTomcatUser = "tomcat8";

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

            // Set the process object id
            s_oLogger.setPrefix("[" + s_oProcessWorkspace.getProcessObjId() + "]");
            s_oLogger.debug("Executing " + sOperation + " Parameter " + sParameter);

            // Set the process as running
            s_oLogger.debug("LauncherMain: setting ProcessWorkspace start date to now");
            s_oProcessWorkspace.setOperationStartDate(Utils.getFormatDate(new Date()));
            s_oProcessWorkspace.setStatus(ProcessStatus.RUNNING.name());
            s_oProcessWorkspace.setPid(getProcessId());

            if (!oProcessWorkspaceRepository.updateProcess(s_oProcessWorkspace)) {
                s_oLogger.error("LauncherMain: ERROR setting ProcessWorkspace start date and RUNNING STATE");
            } else {
                s_oLogger.debug("LauncherMain: RUNNING state and operationStartDate updated");
            }

            /*
             * s_oLogger.
             * debug("******************************Environment Vars*****************************"
             * ); Map<String, String> enviorntmentVars = System.getenv();
             *
             * for (String string : enviorntmentVars.keySet()) { s_oLogger.debug(string + ": " + enviorntmentVars.get(string)); }
             */

            // And Run
            oLauncher.executeOperation(sOperation, sParameter);

            s_oLogger.debug(getBye());
        } catch (Throwable oException) {
            s_oLogger.error("Launcher Main Exception " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oException));

            try {
                System.err.println("LauncherMain: try to put process [" + sParameter + "] in Safe ERROR state");

                ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();

                if (s_oProcessWorkspace != null) {
                    s_oProcessWorkspace.setProgressPerc(100);
                    s_oProcessWorkspace.setOperationEndDate(Utils.getFormatDate(new Date()));
                    s_oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
                    if (!oProcessWorkspaceRepository.updateProcess(s_oProcessWorkspace)) {
                        s_oLogger.debug(
                                "LauncherMain FINAL catch: Error during process update (terminated) " + sParameter);
                    }
                }
            } catch (Exception e) {
                s_oLogger.error("Launcher Main FINAL Exception " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e));
            }
            System.exit(-1);
        } finally {

            // Final Check of the Process Workspace Status
            ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();

            if (s_oProcessWorkspace != null) {

                // Read again the process workspace
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

            m_sTomcatUser = ConfigReader.getPropValue("TOMCAT_USER", "tomcat8");

            // Read this node code
            LauncherMain.s_sNodeCode = ConfigReader.getPropValue("WASDI_NODE", "wasdi");

            s_oLogger.debug("NODE CODE: " + LauncherMain.s_sNodeCode);

            // If this is not the main node
            if (!LauncherMain.s_sNodeCode.equals("wasdi")) {
                s_oLogger.debug("Adding local mongo config");
                // Configure also the local connection: by default is the "wasdi" port + 1
                MongoRepository.addMongoConnection("local", MongoRepository.DB_USER, MongoRepository.DB_PWD, MongoRepository.SERVER_ADDRESS, MongoRepository.SERVER_PORT + 1, MongoRepository.DB_NAME);
            }

            System.setProperty("user.home", ConfigReader.getPropValue("USER_HOME"));

            String sSnapAuxProperties = ConfigReader.getPropValue("SNAP_AUX_PROPERTIES");
            Path oPropFile = Paths.get(sSnapAuxProperties);
            Config.instance("snap.auxdata").load(oPropFile);
            Config.instance().load();

            SystemUtils.init3rdPartyLibs(null);

            Engine.start(false);

            // Snap Log
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

            // Flag to know if update the process workspace progress during download operations or not
            String sNotifyDownloadUpdateActive = ConfigReader.getPropValue("DOWNLOAD_UPDATE_ACTIVE", "1");

            if (sNotifyDownloadUpdateActive.equals("1")) {
                m_bNotifyDownloadUpdateActive = true;
            } else {
                m_bNotifyDownloadUpdateActive = false;
            }

        } catch (Throwable oEx) {
            s_oLogger.error("Launcher Main Constructor Exception " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
        }
    }

    /**
     * Call the right method to execute sOperation with sParameter
     *
     * @param sOperation Operation to be done
     * @param sParameter Parameter
     */
    public void executeOperation(String sOperation, String sParameter) {

        String sWorkspace = "";
        String sExchange = "";

        try {

            // Create the base Parameter
            BaseParameter oBaseParameter = (BaseParameter) SerializationUtils.deserializeXMLToObject(sParameter);
            sWorkspace = oBaseParameter.getWorkspace();
            sExchange = oBaseParameter.getExchange();

            // Create the process workspace logger
            m_oProcessWorkspaceLogger = new ProcessWorkspaceLogger(oBaseParameter.getProcessObjId());
        } catch (Exception e) {

            String sError = "LauncherMain.executeOperation: Impossible to deserialize Operation Parameters: operation aborted";

            if (s_oSendToRabbit != null) {
                s_oSendToRabbit.SendRabbitMessage(false, sOperation, sWorkspace, sError, sExchange);
            }

            s_oLogger.error("LauncherMain.executeOperation:  Exception", e);
            return;
        }

        try {

            LauncherOperations oLauncherOperation = LauncherOperations.valueOf(sOperation);

            switch (oLauncherOperation) {
                case INGEST: {
                    // Deserialize Parameters
                    IngestFileParameter oIngestFileParameter = (IngestFileParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    ingest(oIngestFileParameter, ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH"));
                }
                break;
                case DOWNLOAD: {
                    // Deserialize Parameters
                    DownloadFileParameter oDownloadFileParameter = (DownloadFileParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    download(oDownloadFileParameter, ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH"));
                }
                break;
                case FTPUPLOAD: {
                    // FTP Upload
                    FtpUploadParameters oFtpTransferParameters = (FtpUploadParameters) SerializationUtils.deserializeXMLToObject(sParameter);
                    ftpUpload(oFtpTransferParameters);
                }
                break;
                case PUBLISHBAND: {
                    // Deserialize Parameters
                    PublishBandParameter oPublishBandParameter = (PublishBandParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    publishBandImage(oPublishBandParameter);
                }
                break;
                case GRAPH: {
                    // Execute SNAP Workflow
                    GraphParameter oParameter = (GraphParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    executeGraph(oParameter);
                }
                break;
                case DEPLOYPROCESSOR: {
                    // Deploy new user processor
                    ProcessorParameter oParameter = (ProcessorParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    WasdiProcessorEngine oEngine = WasdiProcessorEngine.getProcessorEngine(oParameter.getProcessorType(),
                            ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH"),
                            ConfigReader.getPropValue("DOCKER_TEMPLATE_PATH"),
                            m_sTomcatUser);
                    oEngine.setParameter(oParameter);
                    oEngine.setProcessWorkspaceLogger(m_oProcessWorkspaceLogger);
                    oEngine.setProcessWorkspace(s_oProcessWorkspace);
                    oEngine.deploy(oParameter);
                }
                break;
                case RUNIDL:
                case RUNPROCESSOR: {
                    // Execute User Processor
                    ProcessorParameter oParameter = (ProcessorParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    WasdiProcessorEngine oEngine = WasdiProcessorEngine.getProcessorEngine(oParameter.getProcessorType(),
                            ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH"),
                            ConfigReader.getPropValue("DOCKER_TEMPLATE_PATH"),
                            m_sTomcatUser);
                    oEngine.setParameter(oParameter);
                    oEngine.setProcessWorkspaceLogger(m_oProcessWorkspaceLogger);
                    oEngine.setProcessWorkspace(s_oProcessWorkspace);
                    oEngine.run(oParameter);
                }
                break;
                case DELETEPROCESSOR: {
                    // Delete User Processor
                    ProcessorParameter oParameter = (ProcessorParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    WasdiProcessorEngine oEngine = WasdiProcessorEngine.getProcessorEngine(oParameter.getProcessorType(),
                            ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH"),
                            ConfigReader.getPropValue("DOCKER_TEMPLATE_PATH"),
                            m_sTomcatUser);
                    oEngine.setParameter(oParameter);
                    oEngine.setProcessWorkspaceLogger(m_oProcessWorkspaceLogger);
                    oEngine.setProcessWorkspace(s_oProcessWorkspace);
                    oEngine.delete(oParameter);
                }
                break;
                case REDEPLOYPROCESSOR: {
                    // Delete User Processor
                    ProcessorParameter oParameter = (ProcessorParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    WasdiProcessorEngine oEngine = WasdiProcessorEngine.getProcessorEngine(oParameter.getProcessorType(),
                            ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH"),
                            ConfigReader.getPropValue("DOCKER_TEMPLATE_PATH"),
                            m_sTomcatUser);
                    oEngine.setParameter(oParameter);
                    oEngine.setProcessWorkspaceLogger(m_oProcessWorkspaceLogger);
                    oEngine.setProcessWorkspace(s_oProcessWorkspace);
                    oEngine.redeploy(oParameter);
                }
                break;
                case LIBRARYUPDATE: {
                    // Delete User Processor
                    ProcessorParameter oParameter = (ProcessorParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    WasdiProcessorEngine oEngine = WasdiProcessorEngine.getProcessorEngine(oParameter.getProcessorType(),
                            ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH"),
                            ConfigReader.getPropValue("DOCKER_TEMPLATE_PATH"),
                            m_sTomcatUser);
                    oEngine.setParameter(oParameter);
                    oEngine.setProcessWorkspaceLogger(m_oProcessWorkspaceLogger);
                    oEngine.setProcessWorkspace(s_oProcessWorkspace);
                    oEngine.libraryUpdate(oParameter);
                }
                break;
                case RUNMATLAB: {
                    // Run Matlab Processor
                    MATLABProcParameters oParameter = (MATLABProcParameters) SerializationUtils.deserializeXMLToObject(sParameter);
                    executeMATLABProcessor(oParameter);
                }
                break;
                case MOSAIC: {
                    // Execute Mosaic Operation
                    MosaicParameter oParameter = (MosaicParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    executeMosaic(oParameter);
                }
                break;
                case SUBSET: {
                    // Execute Subset Operation
                    SubsetParameter oParameter = (SubsetParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    executeSubset(oParameter);
                }
                break;
                case MULTISUBSET: {
                    // Execute Multi Subset Operation
                    MultiSubsetParameter oParameter = (MultiSubsetParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    executeGDALMultiSubset(oParameter);
                }
                break;
                case REGRID: {
                    // TODO: STILL HAVE TO FIND PIXEL SPACING
                    RegridParameter oParameter = (RegridParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    executeGDALRegrid(oParameter);
                }
                break;
                case COPYTOSFTP: {
                    IngestFileParameter oIngestFileParameter = (IngestFileParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    copyToSfpt(oIngestFileParameter, ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH"), ConfigReader.getPropValue("SFTP_ROOT_PATH", "/data/sftpuser"));
                }
                break;
                case KILLPROCESSTREE: {
                    KillProcessTreeParameter oKillProcessTreeParameter = (KillProcessTreeParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    killProcessTree(oKillProcessTreeParameter);
                }
                break;
                case READMETADATA: {
                    ReadMetadataParameter oReadMetadataParameter = (ReadMetadataParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    readMetadata(oReadMetadataParameter);
                }
                break;
                case SEN2COR: {
                    Sen2CorParameters oSen2CorParameters = (Sen2CorParameters) SerializationUtils.deserializeXMLToObject(sParameter);
                    sen2Cor(oSen2CorParameters);
                }
                break;
                default:
                    s_oLogger.debug("Operation Not Recognized. Nothing to do");
                    break;
            }
        } catch (Exception oEx) {
            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);
            if (s_oSendToRabbit != null)
                s_oSendToRabbit.SendRabbitMessage(false, sOperation, sWorkspace, sError, sExchange);
            s_oLogger.error("LauncherMain.executeOperation Exception", oEx);
        }

        s_oLogger.debug("Launcher did his job. Bye bye, see you soon. [" + sParameter + "]");
    }

    /**
     * Operation to make the conversion from Sentinel-2 L1 images to L2 images,
     * using the image correction algorithm L2A_Process from sen2core package.
     * This commands, before launching the convertion itself, checks the availability
     * of the L2A_Process on the host machine.
     *
     * @param oSen2CorParameters contains parameters to initialize Sen2Cor conversion
     */
    private void sen2Cor(Sen2CorParameters oSen2CorParameters) throws Exception{

        // 0 - Create ad-hoc parameter
        // 1 - Access workspace
        // 2 - Checks whether the product file is present on FS
        // 3 - Unzip -> obtain L1C.SAFE
        // 4 - Convert -> obain L2A.SAFE
        // 5 - ZipIt -> L2A.zip
        // 6 - delete intermediary files
        // 7 - Add file to wasdi(L2A.zip)


        String sSen2CorePath = ConfigReader.getPropValue("SEN2CORE");
        ProcessBuilder oProcess = new ProcessBuilder(sSen2CorePath + "/L2A_Process");
        oProcess.start();
        if(oProcess.redirectOutput().toString().contains("no L2A_Process")){
            s_oLogger.debug("LauncherMain.sen2Cor: L2A_Process not available on the host machine, checks installation and configuration");
            return; // interrupt execution
        };

        s_oLogger.info("Sen2Cor");
        if (oSen2CorParameters == null ){
            s_oLogger.debug("LauncherMain.sen2Cor: Null pointer exception");
            m_oProcessWorkspaceLogger.log("Null parameters passed to Launcher for conversion");
            throw new NullPointerException("Null parameters passed to Launcher for conversion");
        }
        if (oSen2CorParameters.isValid()){
            String sProductId = oSen2CorParameters.getProductName();


        }
        else{
            Utils.debugLog("Sen2Cor invalid parameters");
        }

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
    public static String getWorspacePath(BaseParameter oParameter) {
        try {
            return getWorspacePath(oParameter, ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH"));
        } catch (IOException e) {
            e.printStackTrace();
            return getWorspacePath(oParameter, "/data/wasdi");
        }
    }

    /**
     * Get the full workspace path for this parameter
     *
     * @param oParameter
     * @param sRootPath
     * @return full workspace path
     */
    public static String getWorspacePath(BaseParameter oParameter, String sRootPath) {
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
     * Downloads a new product
     *
     * @param oParameter
     * @param sDownloadPath
     * @return
     */
    public String download(DownloadFileParameter oParameter, String sDownloadPath) {

        String sFileName = "";

        ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
        ProcessWorkspace oProcessWorkspace = s_oProcessWorkspace;

        try {
            updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);

            s_oLogger.debug("LauncherMain.Download: Download Start");
            m_oProcessWorkspaceLogger.log("Fetch Start - PROVIDER " + oParameter.getProvider());

            ProviderAdapter oProviderAdapter = new ProviderAdapterFactory().supplyProviderAdapter(oParameter.getProvider());

            if (oProviderAdapter != null) {
                oProviderAdapter.subscribe(this);
            } else {
                throw new Exception("LauncherMain.Download: Provider Adapter is null. Check the provider name");
            }
            oProviderAdapter.setProviderUser(oParameter.getDownloadUser());
            oProviderAdapter.setProviderPassword(oParameter.getDownloadPassword());

            if (oProcessWorkspace != null) {
                oProviderAdapter.setProcessWorkspace(oProcessWorkspace);
                // get file size
                long lFileSizeByte = oProviderAdapter.getDownloadFileSize(oParameter.getUrl());
                // set file size
                setFileSizeToProcess(lFileSizeByte, oProcessWorkspace);

            } else {
                s_oLogger.debug("LauncherMain.Download: process not found: " + oParameter.getProcessObjId());
            }

            sDownloadPath = getWorspacePath(oParameter, sDownloadPath);

            s_oLogger.debug("LauncherMain.DownloadPath: " + sDownloadPath);

            // Product view Model
            ProductViewModel oVM = null;

            // Download file
            if (ConfigReader.getPropValue("DOWNLOAD_ACTIVE").equals("true")) {

                // Get the file name
                String sFileNameWithoutPath = oProviderAdapter.getFileName(oParameter.getUrl());
                s_oLogger.debug("LauncherMain.Download: File to download: " + sFileNameWithoutPath);
                m_oProcessWorkspaceLogger.log("FILE " + sFileNameWithoutPath);

                DownloadedFile oAlreadyDownloaded = null;
                DownloadedFilesRepository oDownloadedRepo = new DownloadedFilesRepository();

                if (!Utils.isNullOrEmpty(sFileNameWithoutPath)) {

                    // First check if it is already in this workspace:
                    oAlreadyDownloaded = oDownloadedRepo.getDownloadedFileByPath(sDownloadPath + sFileNameWithoutPath);

                    if (oAlreadyDownloaded == null) {

                        s_oLogger.debug("LauncherMain.Download: Product NOT found in the workspace, search in other workspaces");
                        // Check if it is already downloaded, in any workpsace
                        List<DownloadedFile> aoExistingList = oDownloadedRepo.getDownloadedFileListByName(sFileNameWithoutPath);

                        // Check if any of this is in this node
                        for (DownloadedFile oDownloadedCandidate : aoExistingList) {

                            if (new File(oDownloadedCandidate.getFilePath()).exists()) {
                                oAlreadyDownloaded = oDownloadedCandidate;
                                s_oLogger.debug("LauncherMain.Download: found already existing copy on this computing node");
                                break;
                            }
                        }

                    } else {

                        File oAlreadyDownloadedFileCheck = new File(oAlreadyDownloaded.getFilePath());

                        if (oAlreadyDownloadedFileCheck.exists() == false) {
                            s_oLogger.debug("LauncherMain.Download: Product already found in the database but the file does not exists in the node");
                            oAlreadyDownloaded = null;
                        } else {
                            s_oLogger.debug("LauncherMain.Download: Product already found in the node");
                        }
                    }
                }

                if (oAlreadyDownloaded == null) {
                    s_oLogger.debug("LauncherMain.Download: File not already downloaded. Download it");

                    if (!Utils.isNullOrEmpty(sFileNameWithoutPath)) {
                        oProcessWorkspace.setProductName(sFileNameWithoutPath);
                        // update the process
                        if (!oProcessWorkspaceRepository.updateProcess(oProcessWorkspace))
                            s_oLogger.debug("LauncherMain.Download: Error during process update with file name");

                        // send update process message
                        if (s_oSendToRabbit != null && !s_oSendToRabbit.SendUpdateProcessMessage(oProcessWorkspace)) {
                            s_oLogger.debug("LauncherMain.Download: Error sending rabbitmq message to update process list");
                        }
                    } else {
                        s_oLogger.error("LauncherMain.Download: sFileNameWithoutPath is null or empty!!");
                    }

                    // No: it isn't: download it
                    sFileName = oProviderAdapter.executeDownloadFile(oParameter.getUrl(), oParameter.getDownloadUser(), oParameter.getDownloadPassword(), sDownloadPath, oProcessWorkspace, oParameter.getMaxRetry());

                    if (Utils.isNullOrEmpty(sFileName)) {

                        int iLastError = oProviderAdapter.getLastServerError();
                        String sError = "There was an error contacting the provider";

                        if (iLastError > 0)
                            sError += ": query obtained HTTP Error Code " + iLastError;

                        m_oProcessWorkspaceLogger.log(sError);

                        throw new Exception(sError);
                    }

                    oProviderAdapter.unsubscribe(this);

                    m_oProcessWorkspaceLogger.log("Got File, try to read");

                    // Control Check for the file Name
                    sFileName = WasdiFileUtils.fixPathSeparator(sFileName);

                    if (sFileNameWithoutPath.startsWith("S3") && sFileNameWithoutPath.toLowerCase().endsWith(".zip")) {
                        s_oLogger.debug("LauncherMain.download: File is a Sentinel 3 image, start unzip");
                        ZipExtractor oZipExtractor = new ZipExtractor(oParameter.getProcessObjId());
                        oZipExtractor.unzip(sDownloadPath + File.separator + sFileNameWithoutPath, sDownloadPath);
                        String sFolderName = sDownloadPath + sFileNameWithoutPath.replace(".zip", ".SEN3");
                        s_oLogger.debug("LauncherMain.download: Unzip done, folder name: " + sFolderName);
                        sFileName = sFolderName + "/" + "xfdumanifest.xml";
                        s_oLogger.debug("LauncherMain.download: File Name changed in: " + sFileName);
                    }

                    // Get The product view Model
                    WasdiProductReader oReadProduct = new WasdiProductReader();
                    File oProductFile = new File(sFileName);
                    Product oProduct = oReadProduct.readSnapProduct(oProductFile, null);
                    oVM = oReadProduct.getProductViewModel(oProduct, oProductFile);

                    if (oVM != null) {
                        // Snap set the name of geotiff files as geotiff: let replace with the file name
                        if (oVM.getName().equals("geotiff")) {
                            oVM.setName(oVM.getFileName());
                        }
                    }

                    // Save Metadata
                    //oVM.setMetadataFileReference(asynchSaveMetadata(sFileName));

                    if (Utils.isNullOrEmpty(sFileNameWithoutPath)) {
                        sFileNameWithoutPath = oProductFile.getName();
                        s_oLogger.debug("LauncherMain.download: sFileNameWithoutPath still null, forced to: " + sFileNameWithoutPath);
                    }

                    // Save it in the register
                    oAlreadyDownloaded = new DownloadedFile();
                    oAlreadyDownloaded.setFileName(sFileNameWithoutPath);
                    oAlreadyDownloaded.setFilePath(sFileName);
                    oAlreadyDownloaded.setProductViewModel(oVM);

                    String sBoundingBox = oParameter.getBoundingBox();

                    if (!Utils.isNullOrEmpty(sBoundingBox)) {
                        if (sBoundingBox.startsWith("POLY") || sBoundingBox.startsWith("MULTI")) {
                            sBoundingBox = Utils.polygonToBounds(sBoundingBox);
                        }

                        oAlreadyDownloaded.setBoundingBox(sBoundingBox);
                    } else {
                        s_oLogger.info("LauncherMain.download: bounding box not available in the parameter");
                    }

                    if (oProduct.getStartTime() != null) {
                        oAlreadyDownloaded.setRefDate(oProduct.getStartTime().getAsDate());
                    }

                    oAlreadyDownloaded.setCategory(DownloadedFileCategory.DOWNLOAD.name());
                    oDownloadedRepo.insertDownloadedFile(oAlreadyDownloaded);
                } else {
                    s_oLogger.debug("LauncherMain.Download: File already downloaded: make a copy");

                    // Yes!! Here we have the path
                    sFileName = oAlreadyDownloaded.getFilePath();

                    s_oLogger.debug("LauncherMain.Download: Check if file exists");

                    // Check the path where we want the file
                    String sDestinationFileWithPath = sDownloadPath + sFileNameWithoutPath;

                    // Is it different?
                    if (sDestinationFileWithPath.equals(sFileName) == false) {
                        // if file doesn't exist
                        if (!new File(sDestinationFileWithPath).exists()) {
                            // Yes, make a copy
                            FileUtils.copyFile(new File(sFileName), new File(sDestinationFileWithPath));
                            // Files.createLink(link, existing)
                            sFileName = sDestinationFileWithPath;
                        } else {
                            // If it exists...
                            sFileName = sDestinationFileWithPath;
                        }
                    }

                }
            } else {
                s_oLogger.debug("LauncherMain.Download: Debug Option Active: file not really downloaded, using configured one");

                sFileName = sDownloadPath + File.separator + ConfigReader.getPropValue("DOWNLOAD_FAKE_FILE");
            }

            if (Utils.isNullOrEmpty(sFileName)) {
                s_oLogger.debug("LauncherMain.Download: file is null there must be an error");

                String sError = "The name of the file to download result null";
                if (s_oSendToRabbit != null)
                    s_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.DOWNLOAD.name(), oParameter.getWorkspace(), sError, oParameter.getExchange());
                if (oProcessWorkspace != null) oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
            } else {

                addProductToDbAndWorkspaceAndSendToRabbit(oVM, sFileName, oParameter.getWorkspace(), oParameter.getExchange(), LauncherOperations.DOWNLOAD.name(), oParameter.getBoundingBox());

                s_oLogger.debug("LauncherMain.Download: Add Product to Db and Send to Rabbit Done");

                if (oProcessWorkspace != null) {
                    s_oLogger.debug("LauncherMain.Download: Set process workspace state as done");

                    oProcessWorkspace.setStatus(ProcessStatus.DONE.name());

                    m_oProcessWorkspaceLogger.log("Operation Completed");
                    m_oProcessWorkspaceLogger.log(new EndMessageProvider().getGood());

                    DownloadPayload oDownloadPayload = new DownloadPayload();
                    oDownloadPayload.setFileName(Utils.getFileNameWithoutLastExtension(sFileName));
                    oDownloadPayload.setProvider(oParameter.getProvider());

                    try {
                        String sPayload = s_oMapper.writeValueAsString(oDownloadPayload);
                        oProcessWorkspace.setPayload(sPayload);
                    } catch (Exception oPayloadEx) {
                        s_oLogger.error("LauncherMain.Download: payload exception: " + oPayloadEx.toString());
                    }

                    s_oLogger.debug("LauncherMain.Download: Set process workspace passed");
                } else {
                    s_oLogger.error("LauncherMain.Download: unexpected null oProcessWorkspace");
                }
            }
        } catch (Exception oEx) {
            oEx.printStackTrace();
            s_oLogger.error("LauncherMain.Download: Exception "
                    + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));

            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);

            if (oProcessWorkspace != null)
                oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
            if (s_oSendToRabbit != null)
                s_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.DOWNLOAD.name(), oParameter.getWorkspace(),
                        sError, oParameter.getExchange());
        } finally {
            s_oLogger.debug("LauncherMain.Download: finally call CloseProcessWorkspace ");
            // update process status and send rabbit updateProcess message
            closeProcessWorkspace(oProcessWorkspaceRepository, oProcessWorkspace);

            s_oLogger.debug("LauncherMain.Download: CloseProcessWorkspace done");
        }

        s_oLogger.debug("LauncherMain.Download: return file name " + sFileName);

        return sFileName;
    }

    /**
     * FTP File Transfer
     *
     * @param oParam
     * @return
     * @throws IOException
     */
    public void ftpUpload(FtpUploadParameters oParam) throws IOException {
        s_oLogger.info("ftpUpload");
        try {
            Preconditions.checkNotNull(oParam, "null parameter");
            Preconditions.checkNotNull(oParam.getProcessObjId(), "null ProcessObjId");

            ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
            ProcessWorkspace oProcessWorkspace = s_oProcessWorkspace;
            try {
                updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);
                if (null == oProcessWorkspace) {
                    throw new NullPointerException("null Process Workspace");
                }


                //check server parameters are OK before trying connection
                if (!Utils.isServerNamePlausible(oParam.getFtpServer())) {

                    m_oProcessWorkspaceLogger.log("FTP server name not plausible " + oParam.getFtpServer());

                    throw new Exception("FTP server name \"" + oParam.getFtpServer() + "\" not plausible");
                }
                if (!Utils.isPortNumberPlausible(oParam.getPort())) {
                    m_oProcessWorkspaceLogger.log("FTP server port not plausible " + oParam.getPort().toString());
                    throw new Exception("FTP server port \"" + oParam.getPort() + "\" not plausible");
                }
                updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 2);

                m_oProcessWorkspaceLogger.log("Moving " + oParam.getLocalFileName() + " to " + oParam.getFtpServer() + ":" + oParam.getPort().toString());

                String sFullLocalPath = getWorspacePath(oParam) + oParam.getLocalFileName();

                updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 3);
                File oFile = new File(sFullLocalPath);
                if (!oFile.exists()) {
                    throw new IOException("local file " + oFile.getName() + "does not exist ");
                }

                if (!oParam.getRemotePath().endsWith("/") && !oParam.getRemotePath().endsWith("\\")) {
                    oParam.setRemotePath(oParam.getRemotePath() + "/");
                }

                m_oProcessWorkspaceLogger.log("Remote path " + oParam.getRemotePath());

                if (oParam.getSftp()) {

                    m_oProcessWorkspaceLogger.log("SFTP protocol");

                    s_oLogger.debug("ftpUpload: SFTP");
                    try (SSHClient oClient = new SSHClient()) {
                        oClient.addHostKeyVerifier(new PromiscuousVerifier());
                        s_oLogger.debug("ftpUpload: SFTP: connecting to " + oParam.getFtpServer());
                        oClient.connect(oParam.getFtpServer());
                        s_oLogger.debug("ftpUpload: SFTP: authenticating as " + oParam.getUsername());
                        oClient.authPassword(oParam.getUsername(), oParam.getPassword());

                        try (SFTPClient sftpClient = oClient.newSFTPClient()) {
                            updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 4);
                            s_oLogger.debug("ftpUpload: SFTP: transferring file");
                            m_oProcessWorkspaceLogger.log("Start transfer");
                            sftpClient.put(sFullLocalPath, oParam.getRemotePath() + oParam.getLocalFileName());
                            //todo check that the file is there
                            updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 95);
                            s_oLogger.debug("ftpUpload: SFTP: closing SFTP client");
                            sftpClient.close();
                            oClient.disconnect();
                            oClient.close();

                            m_oProcessWorkspaceLogger.log("Transfer done");
                        }
                    }

                } else {
                    m_oProcessWorkspaceLogger.log("FTP Protocol");
                    s_oLogger.debug("ftpUpload: FTP");
                    FtpClient oFtpClient = new FtpClient(oParam.getFtpServer(), oParam.getPort(), oParam.getUsername(), oParam.getPassword());

                    s_oLogger.debug("ftpUpload: FTP: opening connection");

                    if (!oFtpClient.open()) {
                        throw new IOException("could not connect to FTP");
                    }
                    updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 4);

                    s_oLogger.debug("ftpUpload: FTP: transferring file");
                    m_oProcessWorkspaceLogger.log("Start transfer");

                    // XXX see how to modify FTP client to update status
                    Boolean bPut = oFtpClient.putFileToPath(oFile, oParam.getRemotePath());
                    if (!bPut) {
                        throw new IOException("put failed");
                    }
                    updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 95);
                    // String sRemotePath = oFtpTransferParameters.getM_sRemotePath();
                    String sRemotePath = ".";
                    s_oLogger.debug("ftpUpload: FTP: checking the file is on server");
                    Boolean bCheck = oFtpClient.fileIsNowOnServer(sRemotePath, oFile.getName());

                    if (!bCheck) {
                        m_oProcessWorkspaceLogger.log("Error checking if the file is on the server");
                        throw new IOException("could not find file on server");
                    }
                    s_oLogger.debug("ftpUpload: FTP: closing client");
                    oFtpClient.close();

                    m_oProcessWorkspaceLogger.log("Transfer done");
                }

                try {
                    FTPUploadPayload oPayload = new FTPUploadPayload();
                    oPayload.setFile(oParam.getLocalFileName());
                    oPayload.setRemotePath(oParam.getRemotePath());
                    oPayload.setServer(oParam.getFtpServer());
                    oPayload.setPort(oParam.getPort());

                    String sPayload = s_oMapper.writeValueAsString(oPayload);
                    oProcessWorkspace.setPayload(sPayload);
                } catch (Exception oPayloadEx) {
                    s_oLogger.error("LauncherMain.ftpUpload: payload exception: " + oPayloadEx.toString());
                }

                updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);
                closeProcessWorkspace(oProcessWorkspaceRepository, oProcessWorkspace);
                s_oLogger.info("ftpUpload: completed successfully");
            } catch (Throwable oEx) {
                s_oLogger.error("ftpUpload: could not complete due to: " + oEx);
                oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
                closeProcessWorkspace(oProcessWorkspaceRepository, oProcessWorkspace);
            } finally {
                closeProcessWorkspace(oProcessWorkspaceRepository, oProcessWorkspace);
                s_oLogger.debug("ftpUpload: workspace closed");
            }
        } catch (Throwable oEx) {
            s_oLogger.error("ftpUpload: " + oEx);
            s_oLogger.error("ftpUpload: warning: cannot update process status");
            oEx.printStackTrace();
        }
    }

    public String saveMetadata(WasdiProductReader oReadProduct, File oProductFile) {

        // Write Metadata to file system
        try {
            // Get Metadata Path a Random File Name
            String sMetadataPath = ConfigReader.getPropValue("METADATA_PATH");
            if (!sMetadataPath.endsWith("/"))
                sMetadataPath += "/";
            String sMetadataFileName = Utils.GetRandomName();

            MetadataViewModel oMetadataViewModel = oReadProduct.getProductMetadataViewModel();

            if (oMetadataViewModel != null) {
                s_oLogger.debug("SaveMetadata: file = " + sMetadataFileName);
                SerializationUtils.serializeObjectToXML(sMetadataPath + sMetadataFileName, oMetadataViewModel);
                s_oLogger.debug("SaveMetadata: file saved");
            } else {
                s_oLogger.debug("SaveMetadata: No Metadata Available");
            }

            return sMetadataFileName;

        } catch (IOException e) {
            s_oLogger.debug("SaveMetadata: Exception = " + e.toString());
            e.printStackTrace();
        } catch (Exception e) {
            s_oLogger.debug("SaveMetadata: Exception = " + e.toString());
            e.printStackTrace();
        }

        // There was an error...
        return "";
    }

    public String asynchSaveMetadata(String sProductFile) {

        // Write Metadata to file system
        try {

            // Get Metadata Path a Random File Name
            String sMetadataPath = ConfigReader.getPropValue("METADATA_PATH");
            if (!sMetadataPath.endsWith("/"))
                sMetadataPath += "/";
            String sMetadataFileName = Utils.GetRandomName();

            s_oLogger.debug("SaveMetadata: file = " + sMetadataFileName);

            SaveMetadataThread oThread = new SaveMetadataThread(sMetadataPath + sMetadataFileName, sProductFile);
            oThread.start();

            s_oLogger.debug("SaveMetadata: thread started");

            return sMetadataFileName;

        } catch (IOException e) {
            s_oLogger.debug("SaveMetadata: Exception = " + e.toString());
            e.printStackTrace();
        } catch (Exception e) {
            s_oLogger.debug("SaveMetadata: Exception = " + e.toString());
            e.printStackTrace();
        }

        // There was an error...
        return "";
    }

    /**
     * Ingest an existing file in a workspace
     *
     * @param oParameter
     * @param sRootPath
     * @return
     * @throws Exception
     */
    public String ingest(IngestFileParameter oParameter, String sRootPath) throws Exception {
        s_oLogger.debug("LauncherMain.ingest");

        if (null == oParameter) {
            String sMsg = "LauncherMain.ingest: null parameter";
            s_oLogger.error(sMsg);
            throw new NullPointerException(sMsg);
        }

        if (null == sRootPath) {
            String sMsg = "LauncherMain.ingest: null download path";
            s_oLogger.error(sMsg);
            throw new NullPointerException(sMsg);
        }

        File oFileToIngestPath = new File(oParameter.getFilePath());

        if (!oFileToIngestPath.canRead()) {
            String sMsg = "LauncherMain.Ingest: ERROR: unable to access file to Ingest " + oFileToIngestPath.getAbsolutePath();
            s_oLogger.error(sMsg);
            throw new IOException(sMsg);
        }

        ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
        ProcessWorkspace oProcessWorkspace = s_oProcessWorkspace;

        try {
            if (oProcessWorkspace != null) {
                // get file size
                long lFileSizeByte = oFileToIngestPath.length();

                // set file size
                setFileSizeToProcess(lFileSizeByte, oProcessWorkspace);
                // Update status
                updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);
            } else {
                s_oLogger.error("LauncherMain.Ingest: process not found: " + oParameter.getProcessObjId());
            }

            String sDestinationPath = getWorspacePath(oParameter, sRootPath);

            File oDstDir = new File(sDestinationPath);

            if (!oDstDir.exists()) {
                oDstDir.mkdirs();
            }

            m_oProcessWorkspaceLogger.log("Ingest Start - File " + oFileToIngestPath.getName() + " in Workspace " + oParameter.getWorkspace());

            if (!oDstDir.isDirectory() || !oDstDir.canWrite()) {
                s_oLogger.error("LauncherMain.Ingest: ERROR: unable to access destination directory " + oDstDir.getAbsolutePath());
                m_oProcessWorkspaceLogger.log("Error accessing destination directory");
                throw new IOException("Unable to access destination directory for the Workspace");
            }

            // Usually, we do not unzip after the copy
            boolean bUnzipAfterCopy = false;

            // Try to read the Product view Model
            WasdiProductReader oReadProduct = new WasdiProductReader();
            ProductViewModel oImportProductViewModel = oReadProduct.getProductViewModel(oFileToIngestPath);

            String sDestinationFileName = oFileToIngestPath.getName();

            // Did we got the View Model ?
            if (oImportProductViewModel == null) {

                s_oLogger.warn("Impossible to read the Product View Model");

                // Check if this is a Zipped Shape File
                if (oFileToIngestPath.getName().toLowerCase().endsWith(".zip")) {
                    ShapeFileUtils oShapeFileUtils = new ShapeFileUtils(oParameter.getProcessObjId());
                    if (oShapeFileUtils.isShapeFileZipped(oFileToIngestPath.getPath(), 30)) {

                        // May be.
                        s_oLogger.info("File to ingest looks can be a zipped shape file, try to unzip");

                        // Unzip
                        ZipExtractor oZipExtractor = new ZipExtractor(oParameter.getProcessObjId());
                        oZipExtractor.unzip(oFileToIngestPath.getCanonicalPath(), oFileToIngestPath.getParent());

                        // Get the name of shp from the zip file (case safe)
                        String sShapeFileTest = oShapeFileUtils.getShpFileNameFromZipFile(oFileToIngestPath.getPath(), 30);

                        if (Utils.isNullOrEmpty(sShapeFileTest) == false) {
                            // Ok, we have our file
                            File oShapeFileIngestPath = new File(oFileToIngestPath.getParent() + "/" + sShapeFileTest);
                            // Now get the view model again
                            oImportProductViewModel = oReadProduct.getProductViewModel(oShapeFileIngestPath);
                            bUnzipAfterCopy = true;
                            s_oLogger.info("Ok, zipped shape file found");

                            m_oProcessWorkspaceLogger.log("Found shape file");

                            sDestinationFileName = sShapeFileTest;
                        }
                    }
                }
            }

            // If we do not have the view model here, we were not able to open the file
            if (oImportProductViewModel == null) {

                m_oProcessWorkspaceLogger.log("Error reading the input product.");

                s_oLogger.error("LauncherMain.Ingest: ERROR: unable to get the product view model");
                throw new IOException("Unable to get the product view model");
            }

            updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 50);

            // copy file to workspace directory
            if (!oFileToIngestPath.getParent().equals(oDstDir.getAbsolutePath())) {

                s_oLogger.debug("File in another folder make a copy");
                FileUtils.copyFileToDirectory(oFileToIngestPath, oDstDir);

                m_oProcessWorkspaceLogger.log("File ingestion done");

                // Must be unzipped?
                if (bUnzipAfterCopy) {

                    s_oLogger.debug("File must be unzipped");
                    ZipExtractor oZipExtractor = new ZipExtractor(oParameter.getProcessObjId());
                    oZipExtractor.unzip(oFileToIngestPath.getCanonicalPath(), oDstDir.getCanonicalPath());
                    s_oLogger.debug("Unzip done");

                    m_oProcessWorkspaceLogger.log("File unzipped");
                }
            } else {
                s_oLogger.debug("File already in the right path no need to copy");
                m_oProcessWorkspaceLogger.log("File already in place");
            }

            File oDstFile = new File(oDstDir, sDestinationFileName);

            updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 75);

            // Snap set the name of geotiff files as geotiff: let replace with the file name
            if (oImportProductViewModel.getName().equals("geotiff")) {
                oImportProductViewModel.setName(oImportProductViewModel.getFileName());
            }

            // add product to db
            addProductToDbAndWorkspaceAndSendToRabbit(oImportProductViewModel, oDstFile.getAbsolutePath(),
                    oParameter.getWorkspace(), oParameter.getExchange(), LauncherOperations.INGEST.name(), null, true, true, oParameter.getStyle());

            try {
                IngestPayload oPayload = new IngestPayload();
                oPayload.setFile(oFileToIngestPath.getName());
                oPayload.setWorkspace(oParameter.getWorkspace());

                String sPayload = s_oMapper.writeValueAsString(oPayload);
                oProcessWorkspace.setPayload(sPayload);
            } catch (Exception oPayloadEx) {
                s_oLogger.error("LauncherMain.Ingest: payload exception: " + oPayloadEx.toString());
            }

            updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);

            m_oProcessWorkspaceLogger.log("Done - " + new EndMessageProvider().getGood());

            return oDstFile.getAbsolutePath();

        } catch (Exception e) {
            String sMsg = "LauncherMain.Ingest: ERROR: Exception occurrend during file ingestion";

            m_oProcessWorkspaceLogger.log("Expection ingesting the file");

            System.out.println(sMsg);
            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(e);
            s_oLogger.error(sMsg);
            s_oLogger.error(sError);
            e.printStackTrace();

            if (oProcessWorkspace != null) {
                oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
            }
            if (s_oSendToRabbit != null) {
                s_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.INGEST.name(), oParameter.getWorkspace(), sError, oParameter.getExchange());
            }

        } catch (Throwable e) {
            String sMsg = "LauncherMain.Ingest: ERROR: Throwable occurrend during file ingestion";
            m_oProcessWorkspaceLogger.log("Expection ingesting the file");
            System.out.println(sMsg);
            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(e);
            s_oLogger.error(sMsg);
            s_oLogger.error(sError);
            e.printStackTrace();

            if (oProcessWorkspace != null) {
                oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
            }
            if (s_oSendToRabbit != null) {
                s_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.INGEST.name(), oParameter.getWorkspace(), sError, oParameter.getExchange());
            }
        } finally {
            // update process status and send rabbit updateProcess message
            closeProcessWorkspace(oProcessWorkspaceRepository, oProcessWorkspace);
        }

        return "";
    }

    /**
     * Copy a file from a workspace to the user sftp folder
     *
     * @param oParameter IngestFileParameter with the reference of the file to move
     * @param sRootPath
     * @param sSftpPath
     * @return
     * @throws Exception
     */
    public String copyToSfpt(IngestFileParameter oParameter, String sRootPath, String sSftpPath) throws Exception {
        s_oLogger.debug("LauncherMain.copyToSfpt");

        if (null == oParameter) {
            String sMsg = "LauncherMain.copyToSfpt: null parameter";
            s_oLogger.error(sMsg);
            throw new NullPointerException(sMsg);
        }

        if (null == sRootPath) {
            String sMsg = "LauncherMain.copyToSfpt: null download path";
            s_oLogger.error(sMsg);
            throw new NullPointerException(sMsg);
        }

        File oFileToMovePath = new File(oParameter.getFilePath());

        if (!oFileToMovePath.canRead()) {
            String sMsg = "LauncherMain.copyToSfpt: ERROR: unable to access file to Move " + oFileToMovePath.getAbsolutePath();
            s_oLogger.error(sMsg);
            throw new IOException(sMsg);
        }

        ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
        ProcessWorkspace oProcessWorkspace = s_oProcessWorkspace;

        try {
            if (oProcessWorkspace != null) {
                // get file size
                long lFileSizeByte = oFileToMovePath.length();

                // set file size
                setFileSizeToProcess(lFileSizeByte, oProcessWorkspace);

                updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 5);
            } else {
                s_oLogger.error("LauncherMain.copyToSfpt: process workspace not found: " + oParameter.getProcessObjId());
            }

            String sDestinationPath = sSftpPath;
            if (!sDestinationPath.endsWith("/")) sDestinationPath += "/";

            // Is there a relative path?
            String sRelativePath = oParameter.getRelativePath();

            if (Utils.isNullOrEmpty(sRelativePath)) {
                // No, just go in the user default folder
                sDestinationPath += oParameter.getUserId();
                sDestinationPath += "/uploads/";
            } else {

                // Yes: this can have also a different user path
                String sUserPath = oParameter.getUserId();
                String sRelativePart = sRelativePath;

                // Do we have the user or not?
                String[] asSplitted = sRelativePath.split(";");
                if (asSplitted != null) {
                    if (asSplitted.length > 1) {
                        sUserPath = asSplitted[0];
                        sRelativePart = asSplitted[1];
                    }
                }

                // Add the user
                sDestinationPath += sUserPath;
                // Add the path
                if (!sRelativePart.startsWith("/")) sDestinationPath += "/";
                sDestinationPath += sRelativePart;
                if (!sDestinationPath.endsWith("/")) sDestinationPath += "/";

            }


            File oDstDir = new File(sDestinationPath);

            if (!oDstDir.exists()) {
                oDstDir.mkdirs();
            }

            if (!oDstDir.isDirectory() || !oDstDir.canWrite()) {
                s_oLogger.error("LauncherMain.copyToSfpt: ERROR: unable to access destination directory " + oDstDir.getAbsolutePath());
                //throw new IOException("Unable to access destination directory for the Workspace");
                return "";
            }

            updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 50);

            // copy file to workspace directory
            if (!oFileToMovePath.getParent().equals(oDstDir.getAbsolutePath())) {
                s_oLogger.debug("LauncherMain.copyToSfpt: File in another folder make a copy");
                FileUtils.copyFileToDirectory(oFileToMovePath, oDstDir);
            } else {
                s_oLogger.debug("LauncherMain.copyToSfpt: File already in place");
            }

            File oDstFile = new File(oDstDir, oFileToMovePath.getName());

            updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);

            return oDstFile.getAbsolutePath();

        } catch (Exception e) {
            String sMsg = "LauncherMain.copyToSfpt: ERROR: Exception in copy file to sftp";
            System.out.println(sMsg);
            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(e);
            s_oLogger.error(sMsg);
            s_oLogger.error(sError);
            e.printStackTrace();

            if (oProcessWorkspace != null) oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
            if (s_oSendToRabbit != null)
                s_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.INGEST.name(), oParameter.getWorkspace(), sError, oParameter.getExchange());

        } catch (Throwable e) {
            String sMsg = "LauncherMain.copyToSfpt: ERROR: Throwable occurrend during file ingestion";
            System.out.println(sMsg);
            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(e);
            s_oLogger.error(sMsg);
            s_oLogger.error(sError);
            e.printStackTrace();

            if (oProcessWorkspace != null) oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
            if (s_oSendToRabbit != null)
                s_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.INGEST.name(), oParameter.getWorkspace(), sError, oParameter.getExchange());
        } finally {
            // update process status and send rabbit updateProcess message
            closeProcessWorkspace(oProcessWorkspaceRepository, oProcessWorkspace);
        }

        return "";
    }

    public static void updateProcessStatus(ProcessWorkspaceRepository oProcessWorkspaceRepository,
                                           ProcessWorkspace oProcessWorkspace, ProcessStatus oProcessStatus, int iProgressPerc)
            throws JsonProcessingException {

        if (oProcessWorkspace == null) {
            s_oLogger.error("LauncherMain.updateProcessStatus oProcessWorkspace is null");
            return;
        }
        if (oProcessWorkspaceRepository == null) {
            s_oLogger.error("LauncherMain.updateProcessStatus oProcessWorkspace is null");
            return;
        }

        oProcessWorkspace.setStatus(oProcessStatus.name());
        oProcessWorkspace.setProgressPerc(iProgressPerc);
        // update the process
        if (!oProcessWorkspaceRepository.updateProcess(oProcessWorkspace)) {
            s_oLogger.debug("Error during process update");
        }

        // send update process message
        if (null == s_oSendToRabbit) {
            try {
                s_oSendToRabbit = new Send(ConfigReader.getPropValue("RABBIT_EXCHANGE", "amq.topic"));
            } catch (IOException e) {
                s_oLogger.debug("Error creating Rabbit Send " + e.toString());
            }
        }

        if (s_oSendToRabbit != null) {
            if (!s_oSendToRabbit.SendUpdateProcessMessage(oProcessWorkspace)) {
                s_oLogger.debug("Error sending rabbitmq message to update process list");
            }
        }
    }

    /**
     * Publish single band image
     *
     * @param oParameter
     * @return
     */
    public String publishBandImage(PublishBandParameter oParameter) {

        String sLayerId = "";
        ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
        ProcessWorkspace oProcessWorkspace = s_oProcessWorkspace;

        try {

            if (oProcessWorkspace != null) {
                updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);
            }

            // Read File Name
            String sFile = oParameter.getFileName();

            // Generate full path name
            String sPath = getWorspacePath(oParameter);
            sFile = sPath + sFile;

            DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
            DownloadedFile oDownloadedFile = oDownloadedFilesRepository.getDownloadedFileByPath(sFile);

            if (oDownloadedFile == null) {
                s_oLogger.error("Downloaded file is null!! Return empyt layer id for [" + sFile + "]");
                return sLayerId;
            }

            // Keep the product name
            String sProductName = oDownloadedFile.getProductViewModel().getName();

            m_oProcessWorkspaceLogger.log("Publish Band " + sProductName + " - " + oParameter.getBandName());

            // Check integrity
            if (Utils.isNullOrEmpty(sFile)) {
                // File not good!!
                s_oLogger.debug("LauncherMain.PublishBandImage: file is null or empty");
                String sError = "Input File path is null";

                m_oProcessWorkspaceLogger.log("Input file is null...");

                // Send KO to Rabbit
                if (s_oSendToRabbit != null) {
                    s_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.PUBLISHBAND.name(), oParameter.getWorkspace(), sError, oParameter.getExchange());
                }

                updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);

                return sLayerId;
            }

            s_oLogger.debug("LauncherMain.PublishBandImage:  File = " + sFile);

            // Create file object
            File oFile = new File(sFile);
            String sInputFileNameOnly = oFile.getName();

            // set file size
            setFileSizeToProcess(oFile, oProcessWorkspace);

            // Generate Layer Id
            sLayerId = sInputFileNameOnly;
            sLayerId = Utils.getFileNameWithoutLastExtension(sFile);
            sLayerId += "_" + oParameter.getBandName();

            // Is already published?
            PublishedBandsRepository oPublishedBandsRepository = new PublishedBandsRepository();
            PublishedBand oAlreadyPublished = oPublishedBandsRepository.getPublishedBand(sFile, oParameter.getBandName());

            updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 10);

            if (oAlreadyPublished != null) {
                // Yes !!
                s_oLogger.debug("LauncherMain.PublishBandImage:  Band already published. Return result");

                m_oProcessWorkspaceLogger.log("Band already published.");

                // Generate the View Model
                PublishBandResultViewModel oVM = new PublishBandResultViewModel();
                oVM.setBandName(oParameter.getBandName());
                oVM.setProductName(sProductName);
                oVM.setLayerId(sLayerId);

                boolean bRet = s_oSendToRabbit != null && s_oSendToRabbit.SendRabbitMessage(true, LauncherOperations.PUBLISHBAND.name(), oParameter.getWorkspace(), oVM, oParameter.getExchange());

                if (!bRet) {
                    s_oLogger.debug("LauncherMain.PublishBandImage: Error sending Rabbit Message");
                }

                return sLayerId;
            }

            // Default Style: can be changed in the following lines depending by the product
            String sStyle = "raster";

            // Hard Coded set Flood Style - STYLES HAS TO BE MANAGED
            if (sFile.toUpperCase().contains("FLOOD")) {
                sStyle = "DDS_FLOODED_AREAS";
            }
            // Hard Coded set NDVI Style - STYLES HAS TO BE MANAGED
            if (sFile.toUpperCase().contains("NDVI")) {
                sStyle = "NDVI";
            }
            // Hard Coded set Burned Areas Style - STYLES HAS TO BE MANAGED
            if (sFile.toUpperCase().contains("BURNEDAREA")) {
                sStyle = "burned_areas";
            }
            // Hard Coded set Flood Risk Style - STYLES HAS TO BE MANAGED
            if (sFile.toUpperCase().contains("FRISK")) {
                sStyle = "frisk";
            }
            // Hard Coded set rgb Style - STYLES HAS TO BE MANAGED
            if (sFile.toUpperCase().contains("_rgb")) {
                sStyle = "wasdi_s2_rgb";
            }

            if (Utils.isNullOrEmpty(oParameter.getStyle()) == false) {
                sStyle = oParameter.getStyle();
            }

            m_oProcessWorkspaceLogger.log("Using style " + sStyle);

            s_oLogger.debug("LauncherMain.PublishBandImage:  Generating Band Image...");

            m_oProcessWorkspaceLogger.log("Generate Band Image");

            // Read the product
            WasdiProductReader oReadProduct = new WasdiProductReader();
            Product oProduct = oReadProduct.readSnapProduct(oFile, null);

            if (oProduct == null) {

                // TODO: HERE CHECK IF IT IS A SHAPE FILE!!!!!

                m_oProcessWorkspaceLogger.log("Impossible to read the input file sorry");

                s_oLogger.error("Not a SNAP Product Return empyt layer id for [" + sFile + "]");
                return sLayerId;
            }

            String sEPSG = CRS.lookupIdentifier(oProduct.getSceneCRS(), true);

            updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 20);

            // write the data directly to GeoServer Data Dir
            String sGeoServerDataDir = ConfigReader.getPropValue("GEOSERVER_DATADIR");
            String sTargetDir = sGeoServerDataDir;

            if (!(sTargetDir.endsWith("/") || sTargetDir.endsWith("\\"))) sTargetDir += "/";
            sTargetDir += sLayerId + "/";

            File oTargetDir = new File(sTargetDir);
            if (!oTargetDir.exists())
                oTargetDir.mkdirs();

            // Output file Path
            String sOutputFilePath = sTargetDir + sLayerId + ".tif";

            // Output File
            File oOutputFile = new File(sOutputFilePath);

            s_oLogger.debug("LauncherMain.PublishBandImage: to " + sOutputFilePath + " [LayerId] = " + sLayerId);

            // Check if is already a .tif image
            if ((sFile.toLowerCase().endsWith(".tif") || sFile.toLowerCase().endsWith(".tiff")) == false) {

                // Check if it is a S2
                if (oProduct.getProductType().startsWith("S2")
                        && oProduct.getProductReader().getClass().getName().startsWith("org.esa.s2tbx")) {

                    s_oLogger.debug("LauncherMain.PublishBandImage:  Managing S2 Product");
                    s_oLogger.debug("LauncherMain.PublishBandImage:  Getting Band " + oParameter.getBandName());

                    Band oBand = oProduct.getBand(oParameter.getBandName());
                    Product oGeotiffProduct = new Product(oParameter.getBandName(), "GEOTIFF");
                    oGeotiffProduct.addBand(oBand);
                    sOutputFilePath = new WasdiProductWriter(oProcessWorkspaceRepository, oProcessWorkspace)
                            .WriteGeoTiff(oGeotiffProduct, sTargetDir, sLayerId);
                    oOutputFile = new File(sOutputFilePath);
                    s_oLogger.debug("LauncherMain.PublishBandImage:  Geotiff File Created (EPSG=" + sEPSG + "): "
                            + sOutputFilePath);

                } else {

                    s_oLogger.debug("LauncherMain.PublishBandImage:  Managing NON S2 Product");
                    s_oLogger.debug("LauncherMain.PublishBandImage:  Getting Band " + oParameter.getBandName());

                    // Get the Band
                    Band oBand = oProduct.getBand(oParameter.getBandName());
                    // Get Image
                    // MultiLevelImage oBandImage = oBand.getSourceImage();
                    RenderedImage oBandImage = oBand.getSourceImage();

                    // Check if the Colour Model is present
                    ColorModel oColorModel = oBandImage.getColorModel();

                    // Tested for Copernicus Marine - netcdf files
                    if (oColorModel == null) {

                        // Colour Model not present: try a different way to get the Image
                        BandImageManager oImgManager = new BandImageManager(oProduct);

                        // Create full dimension and View port
                        Dimension oOutputImageSize = new Dimension(oBand.getRasterWidth(), oBand.getRasterHeight());

                        // Render the image
                        oBandImage = oImgManager.buildImageWithMasks(oBand, oOutputImageSize, null, false, true);
                    }

                    // Get TIFF Metadata
                    GeoTIFFMetadata oMetadata = ProductUtils.createGeoTIFFMetadata(oProduct);

                    s_oLogger.debug("LauncherMain.PublishBandImage:  Output file: " + sOutputFilePath);

                    // Write the Band Tiff
                    if (ConfigReader.getPropValue("CREATE_BAND_GEOTIFF_ACTIVE").equals("true")) {
                        s_oLogger.debug("LauncherMain.PublishBandImage:  Writing Image");

                        GeoTIFF.writeImage(oBandImage, oOutputFile, oMetadata);
                    } else {
                        s_oLogger.debug("LauncherMain.PublishBandImage:  Debug on. Jump GeoTiff Generate");
                    }
                }
            } else {
                // This is a geotiff, just copy
                FileUtils.copyFile(oFile, oOutputFile);
            }

            m_oProcessWorkspaceLogger.log("Publish on geoserver");

            updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 50);

            // Ok publish
            GeoServerManager oGeoServerManager = new GeoServerManager(ConfigReader.getPropValue("GEOSERVER_ADDRESS"), ConfigReader.getPropValue("GEOSERVER_USER"), ConfigReader.getPropValue("GEOSERVER_PASSWORD"));

            // Do we have the style in this Geoserver?
            if (!oGeoServerManager.styleExists(sStyle)) {

                // Not yet: obtain styles root path
                String sStylePath = ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH");
                if (!sStylePath.endsWith(File.separator)) sStylePath += File.separator;
                sStylePath += "styles" + File.separator;

                // Set the style file
                sStylePath += sStyle + ".sld";

                File oStyleFile = new File(sStylePath);

                // Do we have the file?
                if (!oStyleFile.exists()) {
                    // No, Download style
                    s_oLogger.info("LauncherMain.PublishBandImage: download style " + sStyle + " from main node");
                    String sRet = downloadStyle(sStyle, oParameter.getSessionID(), sStylePath);

                    // Check download result
                    if (!sRet.equals(sStylePath)) {
                        // Not good...
                        s_oLogger.error("LauncherMain.PublishBandImage: error downloading style " + sStyle);
                    }
                }

                // Publish the style
                if (oGeoServerManager.publishStyle(sStylePath)) {
                    s_oLogger.info("LauncherMain.PublishBandImage: published style " + sStyle + " on local geoserver");
                } else {
                    s_oLogger.error("LauncherMain.PublishBandImage: error publishing style " + sStyle + " reset on raster");
                    sStyle = "raster";
                }
            }

            Publisher oPublisher = new Publisher();

            try {
                oPublisher.m_lMaxMbTiffPyramid = Long.parseLong(ConfigReader.getPropValue("MAX_GEOTIFF_DIMENSION_PYRAMID", "1024"));
            } catch (Exception e) {
                s_oLogger.error("LauncherMain.PublishBandImage: wrong MAX_GEOTIFF_DIMENSION_PYRAMID, setting default to 1024");
                oPublisher.m_lMaxMbTiffPyramid = 1024L;
            }

            s_oLogger.debug("LauncherMain.PublishBandImage: Call publish geotiff sOutputFilePath = " + sOutputFilePath + " , sLayerId = " + sLayerId + " Style = " + sStyle);
            sLayerId = oPublisher.publishGeoTiff(sOutputFilePath, sLayerId, sEPSG, sStyle, oGeoServerManager);

            s_oLogger.debug("Obtained sLayerId = " + sLayerId);

            updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 90);

            boolean bResultPublishBand = true;

            if (sLayerId == null) {
                m_oProcessWorkspaceLogger.log("Error publishing in Geoserver... :(");
                bResultPublishBand = false;
                s_oLogger.debug("LauncherMain.PublishBandImage: Image not published . ");
                throw new Exception("Layer Id is null. Image not published");
            } else {

                m_oProcessWorkspaceLogger.log("Ok got layer id " + sLayerId);

                s_oLogger.debug("LauncherMain.PublishBandImage: Image published.");

                // get bounding box from data base
                String sBBox = oDownloadedFile.getBoundingBox();
                String sGeoserverBBox = oGeoServerManager.getLayerBBox(sLayerId);

                s_oLogger.debug("LauncherMain.PublishBandImage: Bounding Box: " + sBBox);
                s_oLogger.debug("LauncherMain.PublishBandImage: Geoserver Bounding Box: " + sGeoserverBBox + " for Layer Id " + sLayerId);

                // Create Entity
                PublishedBand oPublishedBand = new PublishedBand();
                oPublishedBand.setLayerId(sLayerId);
                oPublishedBand.setProductName(oDownloadedFile.getFilePath());
                oPublishedBand.setBandName(oParameter.getBandName());
                oPublishedBand.setUserId(oParameter.getUserId());
                oPublishedBand.setWorkspaceId(oParameter.getWorkspace());
                oPublishedBand.setBoundingBox(sBBox);
                oPublishedBand.setGeoserverBoundingBox(sGeoserverBBox);

                Node oNode = getWorkspaceNode(oParameter.getWorkspace());

                if (oNode != null) {
                    s_oLogger.debug("LauncherMain.PublishBandImage: node code: " + oNode.getNodeCode());
                    oPublishedBand.setGeoserverUrl(oNode.getNodeGeoserverAddress());
                }

                // Add it the the db
                oPublishedBandsRepository.insertPublishedBand(oPublishedBand);

                s_oLogger.debug("LauncherMain.PublishBandImage: Index Updated");

                // Create the View Model
                PublishBandResultViewModel oVM = new PublishBandResultViewModel();
                oVM.setBandName(oParameter.getBandName());
                oVM.setProductName(sProductName);
                oVM.setLayerId(sLayerId);
                oVM.setBoundingBox(sBBox);
                oVM.setGeoserverBoundingBox(sGeoserverBBox);
                oVM.setGeoserverUrl(oPublishedBand.getGeoserverUrl());

                // P.Campanella 2019/05/02: Wait a little bit to make GeoServer "finish" the
                // process
                Thread.sleep(5000);

                boolean bRet = s_oSendToRabbit != null && s_oSendToRabbit.SendRabbitMessage(bResultPublishBand, LauncherOperations.PUBLISHBAND.name(), oParameter.getWorkspace(), oVM, oParameter.getExchange());

                if (bRet == false) {
                    s_oLogger.debug("LauncherMain.PublishBandImage: Error sending Rabbit Message");
                }

                m_oProcessWorkspaceLogger.log("Band published " + new EndMessageProvider().getGood());

                try {
                    PublishBandPayload oPayload = new PublishBandPayload();

                    oPayload.setBand(oParameter.getBandName());
                    oPayload.setProduct(sProductName);
                    oPayload.setLayerId(sLayerId);

                    String sPayload = s_oMapper.writeValueAsString(oPayload);
                    oProcessWorkspace.setPayload(sPayload);
                } catch (Exception oPayloadEx) {
                    s_oLogger.error("LauncherMain.PublishBandImage: payload exception: " + oPayloadEx.toString());
                }


                if (oProcessWorkspace != null)
                    oProcessWorkspace.setStatus(ProcessStatus.DONE.name());
            }
        } catch (Exception oEx) {

            m_oProcessWorkspaceLogger.log("Exception");

            s_oLogger.error("LauncherMain.PublishBandImage: Exception " + oEx.toString() + " " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
            oEx.printStackTrace();

            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);

            boolean bRet = s_oSendToRabbit != null && s_oSendToRabbit.SendRabbitMessage(false,
                    LauncherOperations.PUBLISHBAND.name(), oParameter.getWorkspace(), sError, oParameter.getExchange());

            if (bRet == false) {
                s_oLogger.error("LauncherMain.PublishBandImage:  Error sending exception Rabbit Message");
            }

            if (oProcessWorkspace != null)
                oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
        } finally {

            closeProcessWorkspace(oProcessWorkspaceRepository, oProcessWorkspace);
            BandImageManager.stopCacheThread();
        }

        return sLayerId;
    }


    public void executeMATLABProcessor(MATLABProcParameters oParameter) {
        s_oLogger.debug("LauncherMain.executeMATLABProcessor: Start");
        ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
        ProcessWorkspace oProcessWorkspace = s_oProcessWorkspace;

        try {

            if (oProcessWorkspace != null) {

                oProcessWorkspace.setStatus(ProcessStatus.RUNNING.name());
                oProcessWorkspace.setProgressPerc(0);
                // update the process
                oProcessWorkspaceRepository.updateProcess(oProcessWorkspace);
                // send update process message
                if (s_oSendToRabbit != null && !s_oSendToRabbit.SendUpdateProcessMessage(oProcessWorkspace)) {
                    s_oLogger.debug("LauncherMain.executeMATLABProcessor: Error sending rabbitmq message to update process list");
                }
            }

            String sBasePath = ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH");

            if (!sBasePath.endsWith("/")) sBasePath += "/";

            String sRunPath = sBasePath + "processors/" + oParameter.getProcessorName() + "/run_" + oParameter.getProcessorName() + ".sh";

            String sMatlabRunTimePath = ConfigReader.getPropValue("MATLAB_RUNTIME_PATH", "/usr/local/MATLAB/MATLAB_Runtime/v95");
            String sConfigFilePath = sBasePath + "processors/" + oParameter.getProcessorName() + "/config.properties";

            String asCmd[] = new String[]{sRunPath, sMatlabRunTimePath, sConfigFilePath};

            s_oLogger.debug("LauncherMain.executeMATLABProcessor: shell exec " + Arrays.toString(asCmd));
            ProcessBuilder oProcBuilder = new ProcessBuilder(asCmd);

            oProcBuilder.directory(new File(sBasePath + "processors/" + oParameter.getProcessorName()));
            Process oProc = oProcBuilder.start();

            BufferedReader oInput = new BufferedReader(new InputStreamReader(oProc.getInputStream()));

            String sLine;
            while ((sLine = oInput.readLine()) != null) {
                s_oLogger.debug("LauncherMain.executeMATLABProcessor: script stdout: " + sLine);
            }

            s_oLogger.debug("LauncherMain.executeMATLABProcessor: waiting for the process to exit");

            if (oProc.waitFor() == 0) {
                // ok
                s_oLogger.debug("LauncherMain.executeMATLABProcessor: process done with code 0");
                if (oProcessWorkspace != null)
                    oProcessWorkspace.setStatus(ProcessStatus.DONE.name());
            } else {
                // error
                s_oLogger.debug("LauncherMain.executeMATLABProcessor: process done with code != 0");
                if (oProcessWorkspace != null)
                    oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());

            }

        } catch (Exception oEx) {
            s_oLogger.error("LauncherMain.executeMATLABProcessor: exception " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
            if (oProcessWorkspace != null)
                oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());

            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);
            if (s_oSendToRabbit != null)
                s_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.RUNMATLAB.name(), oParameter.getWorkspace(), sError, oParameter.getExchange());

        } finally {
            s_oLogger.debug("LauncherMain.executeMATLABProcessor: End");
            closeProcessWorkspace(oProcessWorkspaceRepository, oProcessWorkspace);
        }
    }

    /**
     * Computes and save a subset of an image (a tile or clip)
     *
     * @param oParameter
     */
    public void executeSubset(SubsetParameter oParameter) {

        s_oLogger.debug("LauncherMain.executeSubset: Start");

        ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
        ProcessWorkspace oProcessWorkspace = s_oProcessWorkspace;

        try {

            if (oProcessWorkspace != null) {
                updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);
            }

            String sSourceProduct = oParameter.getSourceProductName();
            String sOutputProduct = oParameter.getDestinationProductName();

            SubsetSetting oSettings = (SubsetSetting) oParameter.getSettings();

            WasdiProductReader oReadProduct = new WasdiProductReader();
            File oProductFile = new File(getWorspacePath(oParameter) + sSourceProduct);
            Product oInputProduct = oReadProduct.readSnapProduct(oProductFile, null);

            if (oInputProduct == null) {
                s_oLogger.error("LauncherMain.executeSubset: product is not a SNAP product ");
                updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
                return;
            }

            if (oProcessWorkspace != null) {
                updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 30);
            }

            // Take the Geo Coding
            final GeoCoding oGeoCoding = oInputProduct.getSceneGeoCoding();

            // Create 2 GeoPos points
            GeoPos oGeoPosNW = new GeoPos(oSettings.getLatN(), oSettings.getLonW());
            GeoPos oGeoPosSE = new GeoPos(oSettings.getLatS(), oSettings.getLonE());

            // Convert to Pixel Position
            PixelPos oPixelPosNW = oGeoCoding.getPixelPos(oGeoPosNW, null);
            if (!oPixelPosNW.isValid()) {
                oPixelPosNW.setLocation(0, 0);
            }

            PixelPos oPixelPosSW = oGeoCoding.getPixelPos(oGeoPosSE, null);
            if (!oPixelPosSW.isValid()) {
                oPixelPosSW.setLocation(oInputProduct.getSceneRasterWidth(), oInputProduct.getSceneRasterHeight());
            }

            // Create the final region
            Rectangle.Float oRegion = new Rectangle.Float();
            oRegion.setFrameFromDiagonal(oPixelPosNW.x, oPixelPosNW.y, oPixelPosSW.x, oPixelPosSW.y);

            // Create the product bound rectangle
            Rectangle.Float oProductBounds = new Rectangle.Float(0, 0, oInputProduct.getSceneRasterWidth(),
                    oInputProduct.getSceneRasterHeight());

            // Intersect
            Rectangle2D oSubsetRegion = oProductBounds.createIntersection(oRegion);

            ProductSubsetDef oSubsetDef = new ProductSubsetDef();
            oSubsetDef.setRegion(oSubsetRegion.getBounds());
            oSubsetDef.setIgnoreMetadata(false);
            oSubsetDef.setSubSampling(1, 1);
            oSubsetDef.setSubsetName("subset");
            oSubsetDef.setTreatVirtualBandsAsRealBands(false);
            oSubsetDef.setNodeNames(oInputProduct.getBandNames());
            oSubsetDef.addNodeNames(oInputProduct.getTiePointGridNames());

            Product oSubsetProduct = oInputProduct.createSubset(oSubsetDef, sOutputProduct,
                    oInputProduct.getDescription());

            if (oProcessWorkspace != null) {
                updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 50);
            }

            String sOutputPath = getWorspacePath(oParameter) + sOutputProduct;

            ProductIO.writeProduct(oSubsetProduct, sOutputPath, GeoTiffProductWriterPlugIn.GEOTIFF_FORMAT_NAME);

            s_oLogger.debug("LauncherMain.executeSubset done");

            if (oProcessWorkspace != null) {
                updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);
            }

            s_oLogger.debug("LauncherMain.executeSubset adding product to Workspace");

            addProductToDbAndWorkspaceAndSendToRabbit(null, sOutputPath, oParameter.getWorkspace(),
                    oParameter.getWorkspace(), LauncherOperations.SUBSET.toString(), null);

            s_oLogger.debug("LauncherMain.executeSubset: product added to workspace");

        } catch (Exception oEx) {
            s_oLogger.error("LauncherMain.executeSubset: exception "
                    + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
            if (oProcessWorkspace != null)
                oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());

            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);
            if (s_oSendToRabbit != null)
                s_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.SUBSET.name(), oParameter.getWorkspace(),
                        sError, oParameter.getExchange());

        } finally {
            s_oLogger.debug("LauncherMain.executeSubset: End");
            closeProcessWorkspace(oProcessWorkspaceRepository, oProcessWorkspace);
        }

    }

    /**
     * Computes and save a list subset all from an Input image (a tile or clip)
     *
     * @param oParameter
     */
    public void executeGDALMultiSubset(MultiSubsetParameter oParameter) {

        s_oLogger.debug("LauncherMain.executeGDALMultiSubset: Start");

        ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
        ProcessWorkspace oProcessWorkspace = s_oProcessWorkspace;

        try {

            if (oProcessWorkspace != null) {
                updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);
            }

            m_oProcessWorkspaceLogger.log("Starting multisubset");

            String sSourceProduct = oParameter.getSourceProductName();
            MultiSubsetSetting oSettings = (MultiSubsetSetting) oParameter.getSettings();

            int iTileCount = oSettings.getOutputNames().size();

            if (iTileCount > 15) {
                m_oProcessWorkspaceLogger.log("Sorry, no more than 15 tiles... " + new EndMessageProvider().getBad());
                s_oLogger.error("LauncherMain.executeGDALMultiSubset: More than 15 tiles: it hangs, need to refuse");
                updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
                return;
            }

            int iStepPerTile = 100;

            if (iTileCount > 0) {
                iStepPerTile = 100 / iTileCount;
            }

            int iProgress = 0;

            // For all the tiles
            for (int iTiles = 0; iTiles < oSettings.getOutputNames().size(); iTiles++) {

                // Get the output name
                String sOutputProduct = oSettings.getOutputNames().get(iTiles);

                m_oProcessWorkspaceLogger.log("Generating tile " + sOutputProduct);

                // Check th bbox
                if (oSettings.getLatNList().size() <= iTiles) {
                    m_oProcessWorkspaceLogger.log("Invalid coordinates, jump");
                    s_oLogger.debug("Lat N List does not have " + iTiles + " element. continue");
                    continue;
                }

                if (oSettings.getLatSList().size() <= iTiles) {
                    m_oProcessWorkspaceLogger.log("Invalid coordinates, jump");
                    s_oLogger.debug("Lat S List does not have " + iTiles + " element. continue");
                    continue;
                }

                if (oSettings.getLonEList().size() <= iTiles) {
                    m_oProcessWorkspaceLogger.log("Invalid coordinates, jump");
                    s_oLogger.debug("Lon E List does not have " + iTiles + " element. continue");
                    continue;
                }

                if (oSettings.getLonWList().size() <= iTiles) {
                    m_oProcessWorkspaceLogger.log("Invalid coordinates, jump");
                    s_oLogger.debug("Lon W List does not have " + iTiles + " element. continue");
                    continue;
                }

                s_oLogger.debug("Computing tile " + sOutputProduct);

                // Translate
                String sGdalTranslateCommand = "gdal_translate";

                sGdalTranslateCommand = LauncherMain.adjustGdalFolder(sGdalTranslateCommand);

                ArrayList<String> asArgs = new ArrayList<String>();
                asArgs.add(sGdalTranslateCommand);

                // Output format
                asArgs.add("-of");
                asArgs.add("GTiff");
                asArgs.add("-co");
                // TO BE TESTED
                asArgs.add("COMPRESS=LZW");

                asArgs.add("-projwin");
                // ulx uly lrx lry:
                asArgs.add(oSettings.getLonWList().get(iTiles).toString());
                asArgs.add(oSettings.getLatNList().get(iTiles).toString());
                asArgs.add(oSettings.getLonEList().get(iTiles).toString());
                asArgs.add(oSettings.getLatSList().get(iTiles).toString());

                m_oProcessWorkspaceLogger.log("Tile LonW= " + oSettings.getLonWList().get(iTiles).toString() + " LatN= " + oSettings.getLatNList().get(iTiles).toString() + " LonE=" + oSettings.getLonEList().get(iTiles).toString() + " LatS= " + oSettings.getLatSList().get(iTiles).toString());

                if (oSettings.getBigTiff()) {
                    asArgs.add("-co");
                    asArgs.add("BIGTIFF=YES");
                }

                asArgs.add(getWorspacePath(oParameter) + sSourceProduct);
                asArgs.add(getWorspacePath(oParameter) + sOutputProduct);

                // Execute the process
                ProcessBuilder oProcessBuidler = new ProcessBuilder(asArgs.toArray(new String[0]));
                Process oProcess;

                String sCommand = "";
                for (String sArg : asArgs) {
                    sCommand += sArg + " ";
                }

                s_oLogger.debug("LauncherMain.executeGDALMultiSubset Command Line " + sCommand);

                // oProcessBuidler.redirectErrorStream(true);
                oProcess = oProcessBuidler.start();

                oProcess.waitFor();

                File oTileFile = new File(getWorspacePath(oParameter) + sOutputProduct);

                if (oTileFile.exists()) {
                    String sOutputPath = getWorspacePath(oParameter) + sOutputProduct;

                    s_oLogger.debug("LauncherMain.executeGDALMultiSubset done for index " + iTiles);

                    m_oProcessWorkspaceLogger.log("adding output to the workspace");

                    addProductToDbAndWorkspaceAndSendToRabbit(null, sOutputPath, oParameter.getWorkspace(), oParameter.getWorkspace(), LauncherOperations.MULTISUBSET.toString(), null, false, false);

                    s_oLogger.debug("LauncherMain.executeGDALMultiSubset: product added to workspace");

                } else {
                    s_oLogger.debug("LauncherMain.executeGDALMultiSubset Subset null for index " + iTiles);
                }

                if (oProcessWorkspace != null) {
                    iProgress = iProgress + iStepPerTile;
                    if (iProgress > 100) iProgress = 100;
                    updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, iProgress);
                }

            }

            m_oProcessWorkspaceLogger.log("All tiles done");

            if (oProcessWorkspace != null) {
                oProcessWorkspace.setStatus(ProcessStatus.DONE.name());

                try {
                    MultiSubsetPayload oMultiSubsetPayload = new MultiSubsetPayload();
                    oMultiSubsetPayload.setInputFile(sSourceProduct);
                    oMultiSubsetPayload.setOutputFiles(oSettings.getOutputNames().toArray(new String[0]));

                    oProcessWorkspace.setPayload(s_oMapper.writeValueAsString(oMultiSubsetPayload));
                } catch (Exception oPayloadException) {
                    s_oLogger.error("Error creating operation payload: ", oPayloadException);
                }
            }


            if (s_oSendToRabbit != null)
                s_oSendToRabbit.SendRabbitMessage(true, LauncherOperations.MULTISUBSET.name(), oParameter.getWorkspace(), "Multisubset Done", oParameter.getExchange());
        } catch (Exception oEx) {

            s_oLogger.error("LauncherMain.executeGDALMultiSubset: exception " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));

            if (oProcessWorkspace != null) {
                oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
            }


            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);

            if (s_oSendToRabbit != null) {
                s_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.MULTISUBSET.name(), oParameter.getWorkspace(), sError, oParameter.getExchange());
            }


        } finally {

            String sProcWSId = "";
            if (oProcessWorkspace != null) sProcWSId = oProcessWorkspace.getProcessObjId();

            s_oLogger.debug("LauncherMain.executeGDALMultiSubset: calling close Process Workspace");

            closeProcessWorkspace(oProcessWorkspaceRepository, oProcessWorkspace);

            s_oLogger.debug("LauncherMain.executeGDALMultiSubset: End [" + sProcWSId + "]");
        }

    }

    /**
     * TODO: TO FINISH
     *
     * @param oParameter
     */
    public void executeGDALRegrid(RegridParameter oParameter) {

        s_oLogger.debug("LauncherMain.executeGDALRegrid: Start");

        ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
        ProcessWorkspace oProcessWorkspace = s_oProcessWorkspace;

        try {

            if (oProcessWorkspace != null) {
                updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);
            }

            String sSourceProduct = oParameter.getSourceProductName();
            String sDestinationProduct = oParameter.getDestinationProductName();

            RegridSetting oSettings = (RegridSetting) oParameter.getSettings();
            String sReferenceProduct = oSettings.getReferenceFile();

            File oReferenceFile = new File(getWorspacePath(oParameter) + sReferenceProduct);

            WasdiProductReader oRead = new WasdiProductReader(oReferenceFile);

            if (oRead.getSnapProduct() == null) {
                s_oLogger.error("LauncherMain.executeGDALRegrid: product is not a SNAP product ");
                updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
                return;
            }

            // minY, minX, minY, maxX, maxY, maxX, maxY, minX, minY, minX
            String sBBox = oRead.getProductBoundingBox();

            String[] asBBox = sBBox.split(",");
            double[] adBBox = new double[10];

            // It it is null, we will have handled excpetion
            if (asBBox.length >= 10) {
                for (int iStrings = 0; iStrings < 10; iStrings++) {
                    try {
                        adBBox[iStrings] = Double.parseDouble(asBBox[iStrings]);
                    } catch (Exception e) {
                        s_oLogger.error("LauncherMain.executeGDALRegrid: error convering bbox " + e.toString());
                        adBBox[iStrings] = 0.0;
                    }
                }
            }

            double dXOrigin = adBBox[1];
            double dYOrigin = adBBox[0];
            double dXEnd = adBBox[3];
            double dYEnd = adBBox[4];

            Dimension oDim = oRead.getSnapProduct().getSceneRasterSize();

            // STILL HAVE TO FIND THE SCALE: THIS IS NOT PRECISE
            double dXScale = (dXEnd - dXOrigin) / oDim.getWidth();
            double dYScale = (dYEnd - dYOrigin) / oDim.getHeight();

            if (oProcessWorkspace != null) {
                updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 20);
            }

            // SPAWN, ['gdalwarp', '-r', 'near', '-tr', STRING(xscale, Format='(D)'),
            // STRING(yscale, Format='(D)'), '-te', STRING(xOrigin, Format='(D)'),
            // STRING(yOrigin, Format='(D)'), STRING(xEnd, Format='(D)'), STRING(yEnd,
            // Format='(D)'), flood_in, flood_temp,'-co','COMPRESS=LZW'], /NOSHELL
            String sGdalWarpCommand = "gdalwarp";

            sGdalWarpCommand = LauncherMain.adjustGdalFolder(sGdalWarpCommand);

            ArrayList<String> asArgs = new ArrayList<String>();
            asArgs.add(sGdalWarpCommand);

            // Output format
            asArgs.add("-r");
            asArgs.add("near");

            asArgs.add("-tr");
            asArgs.add("" + dXScale);
            asArgs.add("" + dYScale);

            asArgs.add("-te");
            asArgs.add("" + dXOrigin);
            asArgs.add("" + dYOrigin);
            asArgs.add("" + dXEnd);
            asArgs.add("" + dYEnd);

            asArgs.add(getWorspacePath(oParameter) + sSourceProduct);
            asArgs.add(getWorspacePath(oParameter) + sDestinationProduct);

            asArgs.add("-co");
            asArgs.add("COMPRESS=LZW");

            // Execute the process
            ProcessBuilder oProcessBuidler = new ProcessBuilder(asArgs.toArray(new String[0]));
            Process oProcess;

            String sCommand = "";
            for (String sArg : asArgs) {
                sCommand += sArg + " ";
            }

            s_oLogger.debug("LauncherMain.executeGDALMultiSubset Command Line " + sCommand);

            oProcess = oProcessBuidler.start();

            oProcess.waitFor();

            addProductToDbAndWorkspaceAndSendToRabbit(null, getWorspacePath(oParameter) + sDestinationProduct,
                    oParameter.getWorkspace(), oParameter.getExchange(), LauncherOperations.REGRID.name(), sBBox, false,
                    true);
            if (oProcessWorkspace != null) {
                updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 100);
            }

            if (oProcessWorkspace != null)
                oProcessWorkspace.setStatus(ProcessStatus.DONE.name());

            if (s_oSendToRabbit != null)
                s_oSendToRabbit.SendRabbitMessage(true, LauncherOperations.MULTISUBSET.name(),
                        oParameter.getWorkspace(), "Multisubset Done", oParameter.getExchange());
        } catch (Exception oEx) {
            s_oLogger.error("LauncherMain.executeGDALMultiSubset: exception "
                    + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
            if (oProcessWorkspace != null)
                oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());

            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);
            if (s_oSendToRabbit != null)
                s_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.MULTISUBSET.name(),
                        oParameter.getWorkspace(), sError, oParameter.getExchange());

        } finally {

            String sProcWSId = "";
            if (oProcessWorkspace != null)
                sProcWSId = oProcessWorkspace.getProcessObjId();

            s_oLogger.debug("LauncherMain.executeGDALMultiSubset: calling close Process Workspace");

            closeProcessWorkspace(oProcessWorkspaceRepository, oProcessWorkspace);

            s_oLogger.debug("LauncherMain.executeGDALMultiSubset: End [" + sProcWSId + "]");
        }

    }

    /**
     * Execute Mosaic Processor
     *
     * @param oParameter
     */
    public void executeMosaic(MosaicParameter oParameter) {

        s_oLogger.debug("LauncherMain.executeMosaic: Start");
        ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
        ProcessWorkspace oProcessWorkspace = s_oProcessWorkspace;

        try {
            String sBasePath = ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH");

            if (oProcessWorkspace != null) {
                updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);
            }

            m_oProcessWorkspaceLogger.log("Creating Mosaic Util");

            Mosaic oMosaic = new Mosaic(oParameter, sBasePath);
            // Set the proccess workspace logger
            oMosaic.setProcessWorkspaceLogger(m_oProcessWorkspaceLogger);

            // Run the gdal mosaic
            if (oMosaic.runGDALMosaic()) {
                s_oLogger.debug("LauncherMain.executeMosaic done");
                if (oProcessWorkspace != null) {
                    oProcessWorkspace.setProgressPerc(100);
                    oProcessWorkspace.setStatus(ProcessStatus.DONE.name());
                }

                // Log here and to the user
                s_oLogger.debug("LauncherMain.executeMosaic adding product to Workspace");
                m_oProcessWorkspaceLogger.log("Adding output file to the workspace");

                // Get the full path of the output
                String sFileOutputFullPath = getWorspacePath(oParameter) + oParameter.getDestinationProductName();

                // And add it to the db
                addProductToDbAndWorkspaceAndSendToRabbit(null, sFileOutputFullPath, oParameter.getWorkspace(),
                        oParameter.getWorkspace(), LauncherOperations.MOSAIC.toString(), null);

                m_oProcessWorkspaceLogger.log("Done " + new EndMessageProvider().getGood());

                try {
                    // Create the payload
                    MosaicPayload oMosaicPayload = new MosaicPayload();
                    // Get the settings
                    MosaicSetting oSettings = (MosaicSetting) oParameter.getSettings();
                    oMosaicPayload.setOutput(oParameter.getDestinationProductName());
                    oMosaicPayload.setInputs(oSettings.getSources().toArray(new String[0]));
                    oProcessWorkspace.setPayload(s_oMapper.writeValueAsString(oMosaicPayload));
                } catch (Exception oPayloadException) {
                    s_oLogger.error("LauncherMain.executeMosaic: Exception creating operation payload: ", oPayloadException);
                }

                s_oLogger.debug("LauncherMain.executeMosaic: product added to workspace");
            } else {
                // error
                s_oLogger.debug("LauncherMain.executeMosaic: error");
                if (oProcessWorkspace != null)
                    oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
            }

        } catch (Exception oEx) {
            s_oLogger.error("LauncherMain.executeMosaic: exception "
                    + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
            if (oProcessWorkspace != null)
                oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());

            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);
            if (s_oSendToRabbit != null)
                s_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.MOSAIC.name(), oParameter.getWorkspace(),
                        sError, oParameter.getExchange());

        } finally {
            String sStatus = "ND";
            if (oProcessWorkspace != null) {
                sStatus = oProcessWorkspace.getStatus().toString();
            }
            s_oLogger.debug("LauncherMain.executeMosaic: End Closing Process Workspace with status " + sStatus);
            closeProcessWorkspace(oProcessWorkspaceRepository, oProcessWorkspace);
        }

    }

    /**
     * Execute a SNAP Workflow
     *
     * @param oGraphParams
     * @throws Exception
     */
    public void executeGraph(GraphParameter oGraphParams) throws Exception {

        try {
            WasdiGraph oGraphManager = new WasdiGraph(oGraphParams, s_oSendToRabbit, m_oProcessWorkspaceLogger, s_oProcessWorkspace);
            oGraphManager.execute();
        } catch (Exception oEx) {
            s_oLogger.error("ExecuteGraph Exception", oEx);
            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);

            // P.Campanella 2018/03/30: handle exception and close the process
            ProcessWorkspaceRepository oRepo = new ProcessWorkspaceRepository();
            ProcessWorkspace oProcessWorkspace = oRepo.getProcessByProcessObjId(oGraphParams.getProcessObjId());
            updateProcessStatus(oRepo, oProcessWorkspace, ProcessStatus.ERROR, 100);

            if (s_oSendToRabbit != null) {
                s_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.GRAPH.name(), oGraphParams.getWorkspace(), sError, oGraphParams.getExchange());
            }

        }
    }

    /**
     * Adds a product to a Workspace. If it is already added it will not be
     * duplicated.
     *
     * @param sProductFullPath Product to Add
     * @param sWorkspaceId     Workspace Id
     * @return True if the product is already or have been added to the WS. False
     * otherwise
     */
    public boolean addProductToWorkspace(String sProductFullPath, String sWorkspaceId, String sBbox) {

        try {

            // Create Repository
            ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();

            // Check if product is already in the Workspace
            if (oProductWorkspaceRepository.existsProductWorkspace(sProductFullPath, sWorkspaceId) == false) {

                // Create the entity
                ProductWorkspace oProductWorkspace = new ProductWorkspace();
                oProductWorkspace.setProductName(sProductFullPath);
                oProductWorkspace.setWorkspaceId(sWorkspaceId);
                oProductWorkspace.setBbox(sBbox);

                // Try to insert
                if (oProductWorkspaceRepository.insertProductWorkspace(oProductWorkspace)) {

                    s_oLogger.debug("LauncherMain.AddProductToWorkspace:  Inserted [" + sProductFullPath + "] in WS: [" + sWorkspaceId + "]");
                    return true;
                } else {

                    s_oLogger.debug("LauncherMain.AddProductToWorkspace:  Error adding [" + sProductFullPath + "] in WS: [" + sWorkspaceId + "]");
                    return false;
                }
            } else {
                s_oLogger.debug("LauncherMain.AddProductToWorkspace: Product [" + sProductFullPath + "] Already exists in WS: [" + sWorkspaceId + "]");
                return true;
            }
        } catch (Exception e) {
            s_oLogger.error("LauncherMain.AddProductToWorkspace: Exception " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e));
        }

        return false;
    }

    /**
     * Close the Process on the mongo Db. Set progress to 100 and end date time
     *
     * @param oProcessWorkspaceRepository Repository
     * @param oProcessWorkspace           Process to close
     */
    private void closeProcessWorkspace(ProcessWorkspaceRepository oProcessWorkspaceRepository,
                                       ProcessWorkspace oProcessWorkspace) {
        try {
            s_oLogger.debug("LauncherMain.CloseProcessWorkspace");
            if (oProcessWorkspace != null) {
                // update the process
                oProcessWorkspace.setProgressPerc(100);
                oProcessWorkspace.setOperationEndDate(Utils.getFormatDate(new Date()));
                if (!oProcessWorkspaceRepository.updateProcess(oProcessWorkspace)) {
                    s_oLogger.debug("LauncherMain.CloseProcessWorkspace: Error during process update (terminated)");
                }
                // send update process message
                if (s_oSendToRabbit != null && !s_oSendToRabbit.SendUpdateProcessMessage(oProcessWorkspace)) {
                    s_oLogger.debug(
                            "LauncherMain.CloseProcessWorkspace: Error sending rabbitmq message to update process list");
                }
            }
        } catch (Exception oEx) {
            s_oLogger.debug("LauncherMain.CloseProcessWorkspace: Exception deleting process "
                    + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
        }
    }

    /**
     * Set the file size to the process Object
     *
     * @param oFile             File to read the size from
     * @param oProcessWorkspace Process to update
     */
    public static void setFileSizeToProcess(File oFile, ProcessWorkspace oProcessWorkspace) {

        if (oFile == null) {
            s_oLogger.error("LauncherMain.SetFileSizeToProcess: input file is null");
            return;
        }

        if (!oFile.exists()) {
            s_oLogger.error("LauncherMain.SetFileSizeToProcess: input file does not exists");
            return;
        }

        // Take file size
        long lSize = oFile.length();

        // Check if it is a ".dim" file
        if (oFile.getName().endsWith(".dim")) {
            try {
                // Ok so the real size is the folder
                String sFolder = oFile.getAbsolutePath();

                // Get folder path
                sFolder = sFolder.replace(".dim", ".data");
                File oDataFolder = new File(sFolder);

                // Get folder size
                lSize = FileUtils.sizeOfDirectory(oDataFolder);
            } catch (Exception oEx) {
                s_oLogger.error("LauncherMain.SetFileSizeToProcess: Error computing folder size");
                oEx.printStackTrace();
            }
        }

        setFileSizeToProcess(lSize, oProcessWorkspace);
    }

    /**
     * Set the file size to the process Object
     *
     * @param lSize             Size
     * @param oProcessWorkspace Process to update
     */
    public static void setFileSizeToProcess(Long lSize, ProcessWorkspace oProcessWorkspace) {

        if (oProcessWorkspace == null) {
            s_oLogger.error("LauncherMain.SetFileSizeToProcess: input process is null");
            return;
        }

        s_oLogger.debug("LauncherMain.SetFileSizeToProcess: File size  = " + Utils.GetFormatFileDimension(lSize));
        oProcessWorkspace.setFileSize(Utils.GetFormatFileDimension(lSize));
    }

    /**
     * Converts a product in a ViewModel, add it to the workspace and send it to the
     * rabbit queue The method is Safe: controls if the products already exists and
     * if it is already added to the workspace
     *
     * @param oVM               View Model... if null, read it from the product in sFileName
     * @param sFullPathFileName File Name
     * @param sWorkspace        Workspace
     * @param sExchange         Queue Id
     * @param sOperation        Operation Done
     * @param sBBox             Bounding Box
     */
    private void addProductToDbAndWorkspaceAndSendToRabbit(ProductViewModel oVM, String sFullPathFileName,
                                                           String sWorkspace, String sExchange, String sOperation, String sBBox) throws Exception {
        addProductToDbAndWorkspaceAndSendToRabbit(oVM, sFullPathFileName, sWorkspace, sExchange, sOperation, sBBox, true);
    }

    /**
     * Converts a product in a ViewModel, add it to the workspace and send it to the
     * rabbit queue The method is Safe: controls if the products already exists and
     * if it is already added to the workspace
     *
     * @param oVM               View Model... if null, read it from the product in sFileName
     * @param sFullPathFileName File Name
     * @param sWorkspace        Workspace
     * @param sExchange         Queue Id
     * @param sOperation        Operation Done
     * @param sBBox             Bounding Box
     * @param bAsynchMetadata   Flag to know if save metadata in asynch or synch way
     * @throws Exception
     */
    private void addProductToDbAndWorkspaceAndSendToRabbit(ProductViewModel oVM, String sFullPathFileName,
                                                           String sWorkspace, String sExchange, String sOperation, String sBBox, Boolean bAsynchMetadata)
            throws Exception {
        addProductToDbAndWorkspaceAndSendToRabbit(oVM, sFullPathFileName, sWorkspace, sExchange, sOperation, sBBox, bAsynchMetadata, true);
    }

    /**
     * Converts a product in a ViewModel, add it to the workspace and send it to the
     * rabbit queue The method is Safe: controls if the products already exists and
     * if it is already added to the workspace
     *
     * @param oVM               View Model... if null, read it from the product in sFileName
     * @param sFullPathFileName File Name
     * @param sWorkspace        Workspace
     * @param sExchange         Queue Id
     * @param sOperation        Operation Done
     * @param sBBox             Bounding Box
     * @param bAsynchMetadata   Flag to know if save metadata in asynch or synch way
     * @param bSendToRabbit     Flag to know if we need to notify rabbit
     * @throws Exception
     */
    private void addProductToDbAndWorkspaceAndSendToRabbit(ProductViewModel oVM, String sFullPathFileName,
                                                           String sWorkspace, String sExchange, String sOperation, String sBBox, Boolean bAsynchMetadata,
                                                           Boolean bSendToRabbit) throws Exception {
        addProductToDbAndWorkspaceAndSendToRabbit(oVM, sFullPathFileName, sWorkspace, sExchange, sOperation, sBBox, bAsynchMetadata, bSendToRabbit, "");
    }

    /**
     * Converts a product in a ViewModel, add it to the workspace and send it to the
     * rabbit queue. The method is Safe: it controls if the products already exists and
     * if it is already added to the workspace
     *
     * @param oProductViewModel View Model... if null, read it from the product in sFileName
     * @param sFullPathFileName File Name
     * @param sWorkspace        Workspace
     * @param sExchange         Queue Id
     * @param sOperation        Operation Done
     * @param sBBox             Bounding Box
     * @param bAsynchMetadata   Flag to know if save metadata in asynch or synch way
     * @param bSendToRabbit     Flat to know it we need to send update on rabbit or not
     * @throws Exception
     */
    private void addProductToDbAndWorkspaceAndSendToRabbit(ProductViewModel oProductViewModel, String sFullPathFileName,
                                                           String sWorkspace, String sExchange, String sOperation, String sBBox, Boolean bAsynchMetadata,
                                                           Boolean bSendToRabbit, String sStyle) throws Exception {
        s_oLogger.debug("LauncherMain.AddProductToDbAndSendToRabbit: File Name = " + sFullPathFileName);

        // Check if the file is really to Add
        DownloadedFilesRepository oDownloadedRepo = new DownloadedFilesRepository();
        DownloadedFile oCheckAlreadyExists = oDownloadedRepo.getDownloadedFileByPath(sFullPathFileName);

        File oFile = new File(sFullPathFileName);

        WasdiProductReader oReadProduct = new WasdiProductReader(oFile);

        // Get the Bounding Box
        if (Utils.isNullOrEmpty(sBBox)) {
            try {
                GeorefProductViewModel oGeorefProductViewModel = (GeorefProductViewModel) oProductViewModel;
                sBBox = oGeorefProductViewModel.getBbox();
            } catch (Exception oE) {
                s_oLogger.warn("LauncherMain.AddProductToDbAndSendToRabbit: could not extract BBox from GeorefProductViewModel due to: " + oE);
            }
        }
        if (Utils.isNullOrEmpty(sBBox)) {
            s_oLogger.debug("LauncherMain.AddProductToDbAndSendToRabbit: bbox not set. Try to auto get it ");
            sBBox = oReadProduct.getProductBoundingBox();
        }

        if (oCheckAlreadyExists == null) {

            // The VM Is Available?
            if (oProductViewModel == null) {

                // Get The product view Model
                s_oLogger.debug("LauncherMain.AddProductToDbAndSendToRabbit: read View Model");
                oProductViewModel = oReadProduct.getProductViewModel();

                s_oLogger.debug("LauncherMain.AddProductToDbAndSendToRabbit: done read product");
            }

            s_oLogger.debug("AddProductToDbAndSendToRabbit: Insert in db");

            // Save it in the register
            DownloadedFile oDownloadedProduct = new DownloadedFile();

            oDownloadedProduct.setFileName(oFile.getName());
            oDownloadedProduct.setFilePath(sFullPathFileName);
            oDownloadedProduct.setProductViewModel(oProductViewModel);
            oDownloadedProduct.setBoundingBox(sBBox);
            oDownloadedProduct.setRefDate(new Date());
            oDownloadedProduct.setDefaultStyle(sStyle);
            oDownloadedProduct.setCategory(DownloadedFileCategory.COMPUTED.name());

            // Insert in the Db
            if (!oDownloadedRepo.insertDownloadedFile(oDownloadedProduct)) {
                s_oLogger.error("Impossible to Insert the new Product " + oFile.getName() + " in the database.");
            } else {
                s_oLogger.info("Product Inserted");
            }
        } else {

            // The product is already there. No need to add
            if (oProductViewModel == null) {
                oProductViewModel = oCheckAlreadyExists.getProductViewModel();
            }

            // Update the Product View Model
            oCheckAlreadyExists.setProductViewModel(oProductViewModel);
            oDownloadedRepo.updateDownloadedFile(oCheckAlreadyExists);

            // TODO: Update metadata?

            s_oLogger.debug("AddProductToDbAndSendToRabbit: Product Already in the Database. Do not add");
        }

        // The Add Product to Workspace is safe. No need to check if the product is
        // already in the workspace
        addProductToWorkspace(oFile.getAbsolutePath(), sWorkspace, sBBox);

        if (bSendToRabbit) {
            s_oLogger.debug("LauncherMain.AddProductToDbAndSendToRabbit: Image added. Send Rabbit Message Exchange = " + sExchange);

            if (s_oSendToRabbit != null)
                s_oSendToRabbit.SendRabbitMessage(true, sOperation, sWorkspace, oProductViewModel, sExchange);
        }

        if (oReadProduct.getSnapProduct() != null) {
            oReadProduct.getSnapProduct().dispose();
        }

        s_oLogger.debug("LauncherMain.AddProductToDbAndSendToRabbit: Method finished");
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

    @Override
    public void notify(ProcessWorkspace oProcessWorkspace) {

        if (oProcessWorkspace == null) return;

        if (!m_bNotifyDownloadUpdateActive) return;

        try {
            ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();

            // update the process
            if (!oProcessWorkspaceRepository.updateProcess(oProcessWorkspace))
                s_oLogger.error("LauncherMain.DownloadFile: Error during process update with process Perc");

            // send update process message
            if (LauncherMain.s_oSendToRabbit != null) {
                if (!LauncherMain.s_oSendToRabbit.SendUpdateProcessMessage(oProcessWorkspace)) {
                    s_oLogger.error("LauncherMain.DownloadFile: Error sending rabbitmq message to update process list");
                }
            }
        } catch (Exception oEx) {
            s_oLogger.error("LauncherMain.DownloadFile: Exception: " + oEx);
            s_oLogger.debug("LauncherMain.DownloadFile: " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
        }
    }

    public static String snapFormat2GDALFormat(String sFormatName) {

        if (Utils.isNullOrEmpty(sFormatName)) {
            return "";
        }

        switch (sFormatName) {
            case GeoTiffProductWriterPlugIn.GEOTIFF_FORMAT_NAME:
                return "GTiff";
            case "BEAM-DIMAP":
                return "DIMAP";
            case "VRT":
                return "VRT";
            default:
                return "GTiff";
        }
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


    /**
     * Kills a process and, if required its subtree
     *
     * @param oKillProcessTreeParameter the parameters
     */
    private void killProcessTree(KillProcessTreeParameter oKillProcessTreeParameter) {
        s_oLogger.info("killProcessTree");

        try {
            Preconditions.checkNotNull(oKillProcessTreeParameter, "parameter is null");
            Preconditions.checkArgument(!Utils.isNullOrEmpty(oKillProcessTreeParameter.getProcessToBeKilledObjId()), "ObjId of process to be killed is null or empty");

            String sProcessObjId = oKillProcessTreeParameter.getProcessToBeKilledObjId();
            ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
            ProcessWorkspace oProcessToKill = oRepository.getProcessByProcessObjId(sProcessObjId);

            if (null == oProcessToKill) {
                //if the kill operation has been instantiated by the webserver, then this should never happen, so, it's just to err on the side of safety...
                throw new NullPointerException("Process not found in DB");
            }

            s_oLogger.info("killProcessTree: collecting and killing processes for " + oProcessToKill.getProcessObjId());
            LinkedList<ProcessWorkspace> aoProcessesToBeKilled = new LinkedList<>();
            //new element added at the end
            aoProcessesToBeKilled.add(oProcessToKill);
            //todo check: kill the parent first (breadth first?)
            //accumulation loop
            while (aoProcessesToBeKilled.size() > 0) {

                ProcessWorkspace oProcess = aoProcessesToBeKilled.removeFirst();

                if (null == oProcess) {
                    s_oLogger.error("killProcessTree: a null process was added, skipping");
                    continue;
                }

                s_oLogger.info("killProcessTree: killing " + oProcess.getProcessObjId());

                //kill the process immediately
                killProcessAndDocker(oProcess);

                if (!oKillProcessTreeParameter.getKillTree()) {
                    s_oLogger.debug("killProcessTree: process tree must not be killed, ending here");
                    break;
                }

                LauncherOperationsUtils oLauncherOperationsUtils = new LauncherOperationsUtils();
                boolean bCanSpawnChildren = oLauncherOperationsUtils.canOperationSpawnChildren(oProcess.getOperationType());
                if (!bCanSpawnChildren) {
                    s_oLogger.debug("killProcessTree: process " + oProcess.getProcessObjId() + " cannot spawn children, skipping");
                    continue;
                }

                //now that the process cannot spawn any more children, it's time to add them to the set of processes to be killed
                String sParentId = oProcess.getProcessObjId();
                if (Utils.isNullOrEmpty(sParentId)) {
                    s_oLogger.error("killProcessTree: process has null or empty ObjId, skipping");
                    continue;
                }
                List<ProcessWorkspace> aoChildren = oRepository.getProcessByParentId(sParentId);
                if (null != aoChildren && aoChildren.size() > 0) {
                    //append at the end
                    aoProcessesToBeKilled.addAll(aoChildren);
                }
            }

            s_oLogger.error("killProcessTree: Kill loop done");


            ProcessWorkspace oMyProcess = oRepository.getProcessByProcessObjId(oKillProcessTreeParameter.getProcessObjId());

            updateProcessStatus(oRepository, oMyProcess, ProcessStatus.DONE, 100);

        } catch (Exception oE) {
            s_oLogger.error("killProcessTree: " + oE);
        } finally {
            ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
            ProcessWorkspace oMyProcess = oRepository.getProcessByProcessObjId(oKillProcessTreeParameter.getProcessObjId());
            if (!oMyProcess.getStatus().equals("DONE")) {
                oMyProcess.setStatus("ERROR");
                oRepository.updateProcess(oMyProcess);
            }

        }

        s_oLogger.info("killProcessTree: done");
    }

    /**
     * @param oProcessToKill
     * @throws IOException
     * @throws InterruptedException
     */
    private void killProcessAndDocker(ProcessWorkspace oProcessToKill) {
        try {
            LauncherOperationsUtils oLauncherOperationsUtils = new LauncherOperationsUtils();

            if (oLauncherOperationsUtils.doesOperationLaunchDocker(oProcessToKill.getOperationType())) {
                s_oLogger.info("killProcessAndDocker: about to kill docker instance of process " + oProcessToKill.getProcessObjId());
                killDocker(oProcessToKill);
            }

            killProcess(oProcessToKill);
        } catch (Exception oE) {
            s_oLogger.error("killProcessAndDocker: " + oE);
        }
    }

    /**
     * @param oProcessToKill the process to be killed
     */
    private void killProcess(ProcessWorkspace oProcessToKill) {
        //kill the process
        //(code ported from webserver)

        try {
            int iPid = oProcessToKill.getPid();

            if (iPid > 0) {
                // Pid exists, kill the process
                String sShellExString = ConfigReader.getPropValue("KillCommand");
                if (Utils.isNullOrEmpty(sShellExString)) sShellExString = "kill -9";
                sShellExString += " " + iPid;

                s_oLogger.info("killProcess: shell exec " + sShellExString);
                Process oProc = Runtime.getRuntime().exec(sShellExString);
                s_oLogger.info("killProcess: kill result: " + oProc.waitFor());

            } else {
                s_oLogger.error("killProcess: Process pid not in data");
            }

            // set process state to STOPPED only if CREATED or RUNNING
            String sPrevSatus = oProcessToKill.getStatus();

            if (sPrevSatus.equalsIgnoreCase(ProcessStatus.CREATED.name()) ||
                    sPrevSatus.equalsIgnoreCase(ProcessStatus.RUNNING.name()) ||
                    sPrevSatus.equalsIgnoreCase(ProcessStatus.WAITING.name()) ||
                    sPrevSatus.equalsIgnoreCase(ProcessStatus.READY.name())) {

                oProcessToKill.setStatus(ProcessStatus.STOPPED.name());
                oProcessToKill.setOperationEndDate(Utils.getFormatDate(new Date()));

                ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
                if (!oRepository.updateProcess(oProcessToKill)) {
                    s_oLogger.error("killProcess: Unable to update process status of process " + oProcessToKill.getProcessObjId());
                }

            } else {
                s_oLogger.info("killProcess: Process " + oProcessToKill.getProcessObjId() + " already terminated: " + sPrevSatus);
            }
        } catch (Exception oE) {
            s_oLogger.error("killProcess( " + oProcessToKill.getProcessObjId() + " ): " + oE);
        }
    }


    /**
     * @param oProcessToKill the process for which the corresponding docker must be killed
     */
    private void killDocker(ProcessWorkspace oProcessToKill) {
        try {
            String sProcessorName = oProcessToKill.getProductName();
            ProcessorRepository oProcessorRepository = new ProcessorRepository();
            Processor oProcessorToKill = oProcessorRepository.getProcessorByName(sProcessorName);

            // Call localhost:port
            String sUrl = "http://localhost:" + oProcessorToKill.getPort() + "/run/--kill" + "_" + oProcessToKill.getSubprocessPid();

            URL oProcessorUrl = new URL(sUrl);
            HttpURLConnection oConnection = (HttpURLConnection) oProcessorUrl.openConnection();
            oConnection.setDoOutput(true);
            oConnection.setRequestMethod("POST");
            oConnection.setRequestProperty("Content-Type", "application/json");
            OutputStream oOutputStream = oConnection.getOutputStream();
            oOutputStream.write("{}".getBytes());
            oOutputStream.flush();

            if (!(oConnection.getResponseCode() == HttpURLConnection.HTTP_OK || oConnection.getResponseCode() == HttpURLConnection.HTTP_CREATED)) {
                throw new RuntimeException("Failed : HTTP error code : " + oConnection.getResponseCode());
            }
            BufferedReader oBufferedReader = new BufferedReader(new InputStreamReader((oConnection.getInputStream())));
            String sOutputResult;
            String sOutputCumulativeResult = "";

            while ((sOutputResult = oBufferedReader.readLine()) != null) {
                s_oLogger.debug("killDocker: " + sOutputResult);
                if (!Utils.isNullOrEmpty(sOutputResult)) sOutputCumulativeResult += sOutputResult;
            }
            oConnection.disconnect();

            s_oLogger.info(sOutputCumulativeResult);
            s_oLogger.info("Kill docker done for " + oProcessToKill.getProcessObjId() + " SubPid: " + oProcessToKill.getSubprocessPid());
        } catch (Exception oE) {
            s_oLogger.error("killDocker( " + oProcessToKill.getProcessObjId() + " ): " + oE);
        }
    }

    public void readMetadata(ReadMetadataParameter oReadMetadataParameter) {
        try {
            s_oLogger.info("readMetadata: start");

            String sProductName = oReadMetadataParameter.getProductName();

            ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
            ProcessWorkspace oProcessWorkspace = s_oProcessWorkspace;

            if (oProcessWorkspace == null) {
                s_oLogger.error("readMetadata: Impossible to find the process workspace, exit");
                return;
            }

            if (sProductName == null) {
                s_oLogger.error("readMetadata: Product Path is null");
                updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
                return;
            }

            DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();

            String sProductPath = LauncherMain.getWorspacePath(oReadMetadataParameter) + sProductName;

            DownloadedFile oDownloadedFile = oDownloadedFilesRepository.getDownloadedFileByPath(sProductPath);
            if (oDownloadedFile == null) {
                s_oLogger.error("readMetadata: Downloaded file not found for path " + sProductPath);
                updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
                return;
            }

            if (oDownloadedFile.getProductViewModel() == null) {
                s_oLogger.error("readMetadata: Product View Model is null");
                updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
                return;

            }

            if (LauncherMain.s_oSendToRabbit != null) {
                String sInfo = "Read Metadata Operation<br>Retriving File Metadata<br>Try again later";
                LauncherMain.s_oSendToRabbit.SendRabbitMessage(true, LauncherOperations.INFO.name(), oProcessWorkspace.getWorkspaceId(), sInfo, oProcessWorkspace.getWorkspaceId());
            }

            if (Utils.isNullOrEmpty(oDownloadedFile.getProductViewModel().getMetadataFileReference())) {
                if (oDownloadedFile.getProductViewModel().getMetadataFileCreated() == false) {

                    s_oLogger.info("readMetadata: Metadata File still not created. Generate it");

                    oDownloadedFile.getProductViewModel().setMetadataFileCreated(true);
                    oDownloadedFile.getProductViewModel().setMetadataFileReference(asynchSaveMetadata(sProductPath));

                    s_oLogger.info("readMetadata: Metadata File Creation Thread started. Saving Metadata in path " + oDownloadedFile.getProductViewModel().getMetadataFileReference());

                    oDownloadedFilesRepository.updateDownloadedFile(oDownloadedFile);
                } else {
                    s_oLogger.info("readMetadata: attemp to create metadata file has already been done");
                }
            } else {
                s_oLogger.info("readMetadata: metadata file reference already present " + oDownloadedFile.getProductViewModel().getMetadataFileReference());
            }

            updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);

            s_oLogger.info("readMetadata: done, bye");
        } catch (Exception oEx) {
            s_oLogger.error("readMetadata Exception " + oEx.toString());
        }
    }


    /**
     * Download Processor on the local PC
     *
     *
     * @param sSessionId
     * @return
     */
    protected String downloadStyle(String sStyle, String sSessionId, String sDestinationFileFullPath) {
        try {

            if (sStyle == null) {
                System.out.println("sStyle must not be null");
                return "";
            }

            if (sStyle.equals("")) {
                System.out.println("sStyle must not be empty");
                return "";
            }

            String sBaseUrl = "https://www.wasdi.net/wasdiwebserver/rest";

            String sUrl = sBaseUrl + "/filebuffer/downloadstyle?style=" + sStyle;

            HashMap<String, String> asHeaders = WasdiProcessorEngine.getStandardHeaders(sSessionId);

            try {
                URL oURL = new URL(sUrl);
                HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();

                // optional default is GET
                oConnection.setRequestMethod("GET");

                if (asHeaders != null) {
                    for (String sKey : asHeaders.keySet()) {
                        oConnection.setRequestProperty(sKey, asHeaders.get(sKey));
                    }
                }

                int responseCode = oConnection.getResponseCode();

                if (responseCode == 200) {

                    Map<String, List<String>> aoHeaders = oConnection.getHeaderFields();
                    List<String> asContents = null;
                    if (null != aoHeaders) {
                        asContents = aoHeaders.get("Content-Disposition");
                    }
                    String sAttachmentName = null;
                    if (null != asContents) {
                        String sHeader = asContents.get(0);
                        sAttachmentName = sHeader.split("filename=")[1];
                        if (sAttachmentName.startsWith("\"")) {
                            sAttachmentName = sAttachmentName.substring(1);
                        }
                        if (sAttachmentName.endsWith("\"")) {
                            sAttachmentName = sAttachmentName.substring(0, sAttachmentName.length() - 1);
                        }
                        System.out.println(sAttachmentName);

                    }

                    File oTargetFile = new File(sDestinationFileFullPath);
                    File oTargetDir = oTargetFile.getParentFile();
                    oTargetDir.mkdirs();

                    // opens an output stream to save into file
                    try (FileOutputStream oOutputStream = new FileOutputStream(sDestinationFileFullPath)) {
                        InputStream oInputStream = oConnection.getInputStream();

                        Util.copyStream(oInputStream, oOutputStream);

                        if (null != oOutputStream) {
                            oOutputStream.close();
                        }
                        if (null != oInputStream) {
                            oInputStream.close();
                        }
                    }


                    return sDestinationFileFullPath;
                } else {
                    String sMessage = oConnection.getResponseMessage();
                    System.out.println(sMessage);
                    return "";
                }

            } catch (Exception oEx) {
                oEx.printStackTrace();
                return "";
            }

        } catch (Exception oEx) {
            oEx.printStackTrace();
            return "";
        }
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


}


