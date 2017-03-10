package wasdi;
import com.bc.ceres.glevel.MultiLevelImage;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.esa.s2tbx.dataio.s2.S2Config;
import org.esa.s2tbx.dataio.s2.structure.S2ProductStructureFactory;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.geotiff.GeoCoding2GeoTIFFMetadata;
import org.esa.snap.core.util.geotiff.GeoTIFF;
import org.esa.snap.core.util.geotiff.GeoTIFFMetadata;
import org.esa.snap.dataio.geotiff.GeoTiffProductReaderPlugIn;
import org.esa.snap.engine_utilities.util.MemUtils;
import org.esa.snap.runtime.Config;
import org.esa.snap.runtime.Launcher;
import org.geotools.referencing.CRS;
import sun.management.VMManagement;
import wasdi.filebuffer.DownloadFile;
import org.apache.commons.cli.*;
import wasdi.geoserver.Publisher;
import wasdi.rabbit.RabbitFactory;
import wasdi.rabbit.Send;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.PublishedBand;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.PublishedBandsRepository;
import wasdi.shared.parameters.*;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.ProductViewModel;
import wasdi.shared.viewmodels.PublishBandResultViewModel;
import wasdi.shared.viewmodels.RabbitMessageViewModel;
import wasdi.snapopearations.*;

import javax.media.jai.JAI;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.apache.commons.lang.SystemUtils.IS_OS_UNIX;


/**
 * Created by s.adamo on 23/09/2016.
 */
public class LauncherMain {

    // Define a static s_oLogger variable so that it references the
    // Logger instance named "MyApp".
    public static Logger s_oLogger = Logger.getLogger(LauncherMain.class);

    //-operation <operation> -elaboratefile <file>
    public static void main(String[] args) throws Exception {


        try {
            //get jar directory
            File oCurrentFile = new File(LauncherMain.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            //configure log
            DOMConfigurator.configure(oCurrentFile.getParentFile().getPath() + "/log4j.xml");

        }catch(Exception exp)
        {
            //no log4j configuration
            System.err.println( "Error loading log.  Reason: " + exp.getMessage() );
            System.exit(-1);
        }

        s_oLogger.debug("Launcher Main Start");


        // create the parser
        CommandLineParser parser = new DefaultParser();

        // create Options object
        Options oOptions = new Options();


        Option oOptOperation   = OptionBuilder.withArgName( "operation" ).hasArg().withDescription(  "" ).create( "operation" );

        Option oOptParameter   = OptionBuilder.withArgName( "parameter" ).hasArg().withDescription(  "" ).create( "parameter" );


        oOptions.addOption(oOptOperation);
        oOptions.addOption(oOptParameter);


        try {
            String sOperation = "";
            String sParameter = "";


            // parse the command line arguments
            CommandLine oLine = parser.parse( oOptions, args );
            if (oLine.hasOption("operation")) {
                // Get the Operation Code
                sOperation  = oLine.getOptionValue("operation");

            }

            if (oLine.hasOption("parameter")) {
                // Get the Parameter File
                sParameter = oLine.getOptionValue("parameter");
            }

            // Create Launcher Instance
            LauncherMain oLauncher = new LauncherMain();

            s_oLogger.debug("Executing " + sOperation + " Parameter " + sParameter);

            // And Run
            oLauncher.ExecuteOperation(sOperation,sParameter);

            s_oLogger.debug("Operation Done, bye");

        }
        catch( ParseException exp ) {
            s_oLogger.debug("Launcher Main Exception " + exp.toString());
            // oops, something went wrong
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
            System.exit(-1);
        }

    }

    public  LauncherMain() {
        try {

            // Set Global Settings
            Publisher.PYTHON_PATH = ConfigReader.getPropValue("PYTHON_PATH");
            Publisher.TARGET_DIR_BASE = ConfigReader.getPropValue("PYRAMID_BASE_FOLDER");
            Publisher.GDALBasePath = ConfigReader.getPropValue("GDAL_PATH")+"/"+ConfigReader.getPropValue("GDAL_RETILE");
            Publisher.PYRAMYD_ENV_OPTIONS = ConfigReader.getPropValue("PYRAMYD_ENV_OPTIONS");
            MongoRepository.SERVER_ADDRESS = ConfigReader.getPropValue("MONGO_ADDRESS");
            MongoRepository.SERVER_PORT = Integer.parseInt(ConfigReader.getPropValue("MONGO_PORT"));
            MongoRepository.DB_NAME = ConfigReader.getPropValue("MONGO_DBNAME");
            MongoRepository.DB_USER = ConfigReader.getPropValue("MONGO_DBUSER");
            MongoRepository.DB_PWD = ConfigReader.getPropValue("MONGO_DBPWD");

            System.setProperty("user.home", ConfigReader.getPropValue("USER_HOME"));

            Path propFile = Paths.get(ConfigReader.getPropValue("SNAP_AUX_PROPERTIES"));
            Config.instance("snap.auxdata").load(propFile);
            Config.instance().load();

            //s_oLogger.debug("OPJ_INFO_EXE" + S2Config.OPJ_INFO_EXE);
            //s_oLogger.debug("OPJ_DECOMPRESSOR_EXE" + S2Config.OPJ_DECOMPRESSOR_EXE);

            JAI.getDefaultInstance().getTileScheduler().setParallelism(Runtime.getRuntime().availableProcessors());
            MemUtils.configureJaiTileCache();

        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void fixUpPermissions(Path destPath) throws IOException {
        Stream<Path> files = Files.list(destPath);
        files.forEach(path -> {
            if (Files.isDirectory(path)) {
                try {
                    fixUpPermissions(path);
                } catch (IOException e) {
                    SystemUtils.LOG.severe("OpenJPEG configuration error: failed to fix permissions on " + path);
                }
            }
            else {
                setExecutablePermissions(path);
            }
        });
    }

    private static void setExecutablePermissions(Path executablePathName) {
        if (IS_OS_UNIX) {
            Set<PosixFilePermission> permissions = new HashSet<>(Arrays.asList(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ,
                    PosixFilePermission.OTHERS_EXECUTE));
            try {
                Files.setPosixFilePermissions(executablePathName, permissions);
            } catch (IOException e) {
                // can't set the permissions for this file, eg. the file was installed as root
                // send a warning message, user will have to do that by hand.
                SystemUtils.LOG.severe("Can't set execution permissions for executable " + executablePathName.toString() +
                        ". If required, please ask an authorised user to make the file executable.");
            }
        }
    }

    public void ExecuteOperation(String sOperation, String sParameter) {

        try {
            switch (sOperation)
            {
                case LauncherOperations.DOWNLOAD: {

                    // Deserialize Parameters
                    DownloadFileParameter oDownloadFileParameter = (DownloadFileParameter) SerializationUtils.deserializeXMLToObject(sParameter);

                    String sFile = Download(oDownloadFileParameter, ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH"));
                }
                break;
                case LauncherOperations.PUBLISHBAND: {

                    // Deserialize Parameters
                    PublishBandParameter oPublishBandParameter = (PublishBandParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    PublishBandImage(oPublishBandParameter);
                }
                break;
                case LauncherOperations.TERRAIN:{

                    // Deserialize Parameters
                    RangeDopplerGeocodingParameter oParameter = (RangeDopplerGeocodingParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    TerrainOperation(oParameter);

                }
                break;
                case LauncherOperations.RASTERGEOMETRICRESAMPLE:{

                    // Deserialize Parameters
                    RasterGeometricResampleParameter oParameter = (RasterGeometricResampleParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    RasterGeometricResample(oParameter);

                }
                break;
                default:
                    s_oLogger.debug("Operation Not Recognized. Nothing to do");
                    break;
            }
        }
        catch (Exception oEx) {
            s_oLogger.debug("ExecuteOperation Exception " + oEx.toString());
        }
    }

    private Integer GetProcessId()
    {
        Integer iPid = 0;
        try {
            RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
            Field jvmField = runtimeMXBean.getClass().getDeclaredField("jvm");
            jvmField.setAccessible(true);
            VMManagement vmManagement = (VMManagement) jvmField.get(runtimeMXBean);
            Method getProcessIdMethod = vmManagement.getClass().getDeclaredMethod("getProcessId");
            getProcessIdMethod.setAccessible(true);
            iPid = (Integer) getProcessIdMethod.invoke(vmManagement);

        } catch (Exception oEx) {
            s_oLogger.debug("LauncherMain.GetProcessId: Error getting processId: " + oEx.getMessage());
        }

        return iPid;
    }


    /**
     * Downloads a new product
     * @param oParameter
     * @param sDownloadPath
     * @return
     */
    public String Download(DownloadFileParameter oParameter, String sDownloadPath) {
        String sFileName = "";
        // Rabbit Sender
        Send oSendToRabbit = new Send();
        // Download handler
        DownloadFile oDownloadFile = new DownloadFile();

        try {
            s_oLogger.debug("LauncherMain.Download: Download Start");

            ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
            ProcessWorkspace oProcessWorkspace = oProcessWorkspaceRepository.GetProcessByProductName(oParameter.getUrl());
            if (oProcessWorkspace != null) {
                //get file size
                long lFileSizeByte = oDownloadFile.GetDownloadFileSize(oParameter.getUrl());
                long lFileSizeMega = lFileSizeByte / (1024 * 1024);
                s_oLogger.debug("LauncherMain.Download: File size = " + lFileSizeMega);
                //set file size
                oProcessWorkspace.setFileSize(Long.toString(lFileSizeMega));
                //get process pid
                oProcessWorkspace.setPid(GetProcessId());
                //update the process
                if (!oProcessWorkspaceRepository.UpdateProcess(oProcessWorkspace))
                    s_oLogger.debug("LauncherMain.Download: Error during process update");
            }

            //Init rabbit exchange and queue
            if (oSendToRabbit.Init(oParameter.getWorkspace(), oParameter.getUserId()) == false)
            {
                s_oLogger.debug("LauncherMain.Download: Failed initializing RabbitMQ");
                return sFileName;
            }

            //send update process message
            if (oSendToRabbit.SendUpdateProcessMessage(oParameter.getWorkspace()) == false)
            {
                s_oLogger.debug("LauncherMain.Download: Error sending rabbitmq message to update process list");
            }

            if (!sDownloadPath.endsWith("/")) sDownloadPath+="/";

            // Generate the Path adding user id and workspace
            sDownloadPath += oParameter.getUserId()+"/"+oParameter.getWorkspace();

            s_oLogger.debug("LauncherMain.DownloadPath: " + sDownloadPath);

            // Product view Model
            ProductViewModel oVM = null;


            // Download file
            if (ConfigReader.getPropValue("DOWNLOAD_ACTIVE").equals("true")) {

                // Get the file name
                String sFileNameWithoutPath = oDownloadFile.GetFileName(oParameter.getUrl());
                s_oLogger.debug("LauncherMain.Download: File not already downloaded. File Name: " + sFileNameWithoutPath);
                DownloadedFile oAlreadyDownloaded = null;
                DownloadedFilesRepository oDownloadedRepo = new DownloadedFilesRepository();
                if (!Utils.isNullOrEmpty(sFileNameWithoutPath)) {
                    // Check if it is already downloaded
                    oAlreadyDownloaded = oDownloadedRepo.GetDownloadedFile(sFileNameWithoutPath);
                }

                if (oAlreadyDownloaded == null) {
                    s_oLogger.debug("LauncherMain.Download: File not already downloaded. Download it on " + oParameter.getOpenSearchProvider() + " hub");

                    // No: it isn't: download it
                    sFileName = oDownloadFile.ExecuteDownloadFile(oParameter.getUrl(), sDownloadPath);

                    // Get The product view Model
                    ReadProduct oReadProduct = new ReadProduct();
                    File oProductFile = new File(sFileName);
                    oVM = oReadProduct.getProductViewModel(oProductFile);
                    oVM.setMetadata(oReadProduct.getProductMetadataViewModel(oProductFile));

                    // Save it in the register
                    oAlreadyDownloaded = new DownloadedFile();
                    oAlreadyDownloaded.setFileName(sFileNameWithoutPath);
                    oAlreadyDownloaded.setFilePath(sFileName);
                    oAlreadyDownloaded.setProductViewModel(oVM);
                    oAlreadyDownloaded.setBoundingBox(oParameter.getBoundingBox());
                    oDownloadedRepo.InsertDownloadedFile(oAlreadyDownloaded);
                }
                else {
                    s_oLogger.debug("LauncherMain.Download: File already downloaded: make a copy");

                    // Yes!! Here we have the path
                    sFileName = oAlreadyDownloaded.getFilePath();

                    s_oLogger.debug("LauncherMain.Download: Check if file exists");

                    // Check the path where we want the file
                    String sDestinationFileWithPath = sDownloadPath + "/" + sFileNameWithoutPath;

                    // Is it different?
                    if (sDestinationFileWithPath.equals(sFileName) == false) {
                        //if file doesn't exist
                        if (!new File(sDestinationFileWithPath).exists()) {
                            // Yes, make a copy
                            FileUtils.copyFile(new File(sFileName), new File(sDestinationFileWithPath));
                            sFileName = sDestinationFileWithPath;
                        }}

                }
            }
            else {
                s_oLogger.debug("LauncherMain.Download: Debug Option Active: file not really downloaded, using configured one");

                sFileName = sDownloadPath + File.separator + ConfigReader.getPropValue("DOWNLOAD_FAKE_FILE");

            }

            if (Utils.isNullOrEmpty(sFileName)) {
                s_oLogger.debug("LauncherMain.Download: file is null there must be an error");

                oSendToRabbit.SendRabbitMessage(false,LauncherOperations.DOWNLOAD,oParameter.getWorkspace(),null,oParameter.getExchange());

            }
            else {

                ConvertProductToViewModelAndSendToRabbit(oVM, sFileName, oParameter.getWorkspace(), oParameter.getExchange(), LauncherOperations.DOWNLOAD, oParameter.getBoundingBox());
            }
        }
        catch (Exception oEx) {
            s_oLogger.debug("LauncherMain.Download: Exception " + oEx.toString());

            oSendToRabbit.SendRabbitMessage(false,LauncherOperations.DOWNLOAD,oParameter.getWorkspace(),null,oParameter.getExchange());
        }
        finally{
            //delete process from list
            try{
                ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
                oProcessWorkspaceRepository.DeleteProcessWorkspace(oParameter.getProcessObjId());
            }
            catch (Exception oEx) {
                s_oLogger.debug("LauncherMain.Download: Exception deleting process " + oEx.toString());
            }
        }

        return  sFileName;
    }

    /**
     * Converts a product in a ViewModel and send it to the rabbit queue
     * @param oVM View Model to fill
     * @param sFileName File Name
     * @param sWorkspace Workspace
     * @param sExchange Queue Id
     * @param sOperation Operation Done
     * @param sBBox Bounding Box
     */
    private void ConvertProductToViewModelAndSendToRabbit(ProductViewModel oVM, String sFileName, String sWorkspace, String sExchange, String sOperation, String sBBox)
    {
        Send oSendToRabbit = new Send();
        try {
            s_oLogger.debug("LauncherMain.Download: File Name = " + sFileName);

            // Get The product view Model
            ReadProduct oReadProduct = new ReadProduct();
            s_oLogger.debug("LauncherMain.Download: call read product");
            File oProductFile = new File(sFileName);
            oVM = oReadProduct.getProductViewModel(new File(sFileName));
            oVM.setMetadata(oReadProduct.getProductMetadataViewModel(oProductFile));

            if (oVM.getBandsGroups() == null) s_oLogger.debug("LauncherMain.Download: Band Groups is NULL");
            else if (oVM.getBandsGroups().getBands() == null) s_oLogger.debug("LauncherMain.Download: bands is NULL");
            else {
                s_oLogger.debug("LauncherMain.Download: bands " + oVM.getBandsGroups().getBands().size());
            }

            s_oLogger.debug("LauncherMain.Download: done read product");

            if (oVM == null) s_oLogger.debug("LauncherMain.Download VM is null!!!!!!!!!!");

            s_oLogger.debug("Insert in db");
            // Save it in the register
            DownloadedFile oAlreadyDownloaded = new DownloadedFile();
            File oFile = new File(sFileName);
            oAlreadyDownloaded.setFileName(oFile.getName());
            oAlreadyDownloaded.setFilePath(sFileName);
            oAlreadyDownloaded.setProductViewModel(oVM);
            oAlreadyDownloaded.setBoundingBox(sBBox);
            DownloadedFilesRepository oDownloadedRepo = new DownloadedFilesRepository();
            oDownloadedRepo.InsertDownloadedFile(oAlreadyDownloaded);

            s_oLogger.debug("OK DONE");

            s_oLogger.debug("LauncherMain.Download: Image downloaded. Send Rabbit Message");

            if (oVM != null) {

                s_oLogger.debug("LauncherMain.Download: Exchange = " + sExchange);

                oSendToRabbit.SendRabbitMessage(true,sOperation,sWorkspace,oVM,sExchange);

            } else {
                s_oLogger.debug("LauncherMain.Download: Unable to read image. Send Rabbit Message");

                oSendToRabbit.SendRabbitMessage(false,sOperation,sWorkspace,null,sExchange);
            }
        }
        catch(Exception oEx)
        {
            s_oLogger.error(oEx.toString());
            oEx.printStackTrace();
        }
    }

    /**
     * Generic publish function. NOTE: probably will not be used, use publish band instead
     * @param oParameter
     * @return
     */
    public void TerrainOperation(RangeDopplerGeocodingParameter oParameter) {

        s_oLogger.debug("LauncherMain.TerrainOperation: Start");

        // Rabbit Sender
        Send oSendToRabbit = new Send();

        try {
            //Init rabbit exchange and queue
            if (oSendToRabbit.Init(oParameter.getWorkspace(), oParameter.getUserId()) == false)
            {
                s_oLogger.debug("LauncherMain.TerrainOperation: Failed initializing RabbitMQ");
                return;
            }

            //send update process message
            if (oSendToRabbit.SendUpdateProcessMessage(oParameter.getWorkspace()) == false)
            {
                s_oLogger.debug("LauncherMain.TerrainOperation: Error sending rabbitmq message to update process list");
            }

            // Read File Name
            String sFile = oParameter.getSourceProductName();

            String sRootPath = ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH");
            if (!sRootPath.endsWith("/")) sRootPath += "/";
            final String sPath = sRootPath + oParameter.getUserId() + "/" + oParameter.getWorkspace() + "/";
            sFile = sPath + sFile;

            // Check integrity
            if (Utils.isNullOrEmpty(sFile)) {
                s_oLogger.debug("LauncherMain.TerrainOperation: file is null or empty");
                return;
            }

            File oSourceFile = new File(sFile);
            WriteProduct oWriter = new WriteProduct();

            ReadProduct oReadProduct = new ReadProduct();
            s_oLogger.debug("LauncherMain.TerrainOperation: Read Product");
            Product oSourceProduct = oReadProduct.ReadProduct(oSourceFile, null);

            if (oSourceProduct == null)
            {
                throw new Exception("LauncherMain.TerrainOperation: Source Product null");
            }

            //Terrain Operation
            s_oLogger.debug("LauncherMain.TerrainOperation: Terrain Product");
            TerrainCorrection oTerrainCorrection = new TerrainCorrection();
            Product oTerrainProduct = oTerrainCorrection.getTerrainCorrection(oSourceProduct, oParameter.getSettings());
            if (oTerrainProduct == null)
            {
                throw new Exception("LauncherMain.TerrainOperation: Terrain product null");
            }

            s_oLogger.debug("LauncherMain.TerrainOperation: Write Big Tiff");
            String sTargetFileName = oTerrainProduct.getName();
            if (!Utils.isNullOrEmpty(oParameter.getDestinationProductName()))
                sTargetFileName = oParameter.getDestinationProductName();
            String sTiffFile = oWriter.WriteBigTiff(oTerrainProduct, sPath, sTargetFileName);

            if (Utils.isNullOrEmpty(sTiffFile))
            {
                throw new Exception("LauncherMain.TerrainOperation: Tiff not created");
            }

            s_oLogger.debug("LauncherMain.TerrainOperation: convert product to view model");
            ConvertProductToViewModelAndSendToRabbit(new ProductViewModel(), sTiffFile, oParameter.getWorkspace(), oParameter.getExchange(), LauncherOperations.TERRAIN, null);

            //this.PublishOnGeoserver(oParameter.getPublishParameter(), oTerrainProduct.getName(), sBandName);

        }
        catch (Exception oEx) {
            s_oLogger.debug("LauncherMain.TerrainOperation: exception " + oEx.toString());
            oSendToRabbit.SendRabbitMessage(false,LauncherOperations.TERRAIN,oParameter.getWorkspace(),null,oParameter.getExchange());
        }
        finally{
            s_oLogger.debug("LauncherMain.TerrainOperation: End");

            //delete process from list
            try{
                ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
                oProcessWorkspaceRepository.DeleteProcessWorkspace(oParameter.getProcessObjId());
            }
            catch (Exception oEx) {
                s_oLogger.debug("LauncherMain.TerrainOperation: Exception deleting process " + oEx.toString());
            }
        }
    }


    /**
     * Publish single band image
     * @param oParameter
     * @return
     */
    public String PublishBandImage(PublishBandParameter oParameter) {

        String sLayerId = "";

        // Rabbit Sender
        Send oSendToRabbit = new Send();

        try {

            ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
            ProcessWorkspace oProcessWorkspace = oProcessWorkspaceRepository.GetProcessByProductName(oParameter.getFileName());

            if (oProcessWorkspace != null) {
                //get process pid
                oProcessWorkspace.setPid(GetProcessId());
                //update the process
                oProcessWorkspaceRepository.UpdateProcess(oProcessWorkspace);
            }

            //Init rabbit exchange and queue
            if (oSendToRabbit.Init(oParameter.getWorkspace(), oParameter.getUserId()) == false)
            {
                s_oLogger.debug("LauncherMain.PublishBandImage: Failed initializing RabbitMQ");
                return sLayerId;
            }

            //send update process message
            if (oSendToRabbit.SendUpdateProcessMessage(oParameter.getWorkspace()) == false)
            {
                s_oLogger.debug("LauncherMain.PublishBandImage: Error sending rabbitmq message to update process list");
            }

            // Read File Name
            String sFile = oParameter.getFileName();

            // Keep the product name
            String sProductName = sFile;

            // Generate full path name
            String sPath = ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH");
            if (!sPath.endsWith("/")) sPath += "/";
            sPath += oParameter.getUserId() + "/" + oParameter.getWorkspace()+ "/";
            sFile = sPath + sFile;


            // Check integrity
            if (Utils.isNullOrEmpty(sFile))
            {
                // File not good!!
                s_oLogger.debug( "LauncherMain.PublishBandImage: file is null or empty");

                // Send KO to Rabbit
                oSendToRabbit.SendRabbitMessage(false, LauncherOperations.PUBLISHBAND,oParameter.getWorkspace(),null,oParameter.getExchange());

                return  sLayerId;
            }

            s_oLogger.debug( "LauncherMain.PublishBandImage:  File = " + sFile);

            // Create file
            File oFile = new File(sFile);
            String sInputFileNameOnly = oFile.getName();

            // Generate Layer Id
            sLayerId = sInputFileNameOnly;
            sLayerId = Utils.GetFileNameWithoutExtension(sFile);
            sLayerId +=  "_" + oParameter.getBandName();

            // Is already published?
            PublishedBandsRepository oPublishedBandsRepository = new PublishedBandsRepository();
            PublishedBand oAlreadyPublished = oPublishedBandsRepository.GetPublishedBand(oParameter.getFileName(),oParameter.getBandName());


            if (oAlreadyPublished != null) {
                // Yes !!
                s_oLogger.debug( "LauncherMain.PublishBandImage:  Band already published. Return result" );

                // Generate the View Model
                PublishBandResultViewModel oVM = new PublishBandResultViewModel();
                oVM.setBandName(oParameter.getBandName());
                oVM.setProductName(sProductName);
                oVM.setLayerId(sLayerId);

                boolean bRet = oSendToRabbit.SendRabbitMessage(true,LauncherOperations.PUBLISHBAND,oParameter.getWorkspace(),oVM,oParameter.getExchange());

                if (!bRet) s_oLogger.debug("LauncherMain.PublishBandImage: Error sending Rabbit Message");

                return sLayerId;
            }

            // Default EPSG: can be changed in the following lines if read from the Product
            String sEPSG = "EPSG:4326";
            // Default Style: can be changed in the following lines depending by the product
            String sStyle = "raster";

            s_oLogger.debug( "LauncherMain.PublishBandImage:  Generating Band Image...");

            // Read the product
            ReadProduct oReadProduct = new ReadProduct();
            Product oSentinel = oReadProduct.ReadProduct(oFile, null);

            // Generate file output name
            String sOutputFilePath = sPath + sLayerId + ".tif";
            File oOutputFile = new File(sOutputFilePath);

            // Is this a Sentinel 2?
            if (oSentinel.getProductType().startsWith("S2")) {

                // Need to resample to publish
                s_oLogger.debug( "LauncherMain.PublishBandImage:  S2 Image");

                //RasterGeometricResampling oRasterGeometricResample = new RasterGeometricResampling();
                //Product oResampledProduct = oRasterGeometricResample.getResampledProduct(oSentinel, oParameter.getBandName());
                //oSentinel = oResampledProduct;

                try {

                    // Get the file only name:
                    String sFileOnly = oFile.getName();
                    String [] asFileParts = sFileOnly.split(Pattern.quote("."));
                    if (asFileParts != null) {
                        if (asFileParts.length>0) sFileOnly = asFileParts[0];
                    }

                    s_oLogger.debug( "LauncherMain.PublishBandImage:  File Name without extension: " + sFileOnly);


                    Runtime run = Runtime.getRuntime();

                    // Check if has been already unzipped
                    File oUnzippedFolder = new File(sPath+"/"+sFileOnly+".SAFE");

                    if (oUnzippedFolder.exists() == false) {
                        String sCmd = "unzip " + sFile + " -d " + sPath;

                        s_oLogger.debug("S2 Image UNZIP: "+sCmd);
                        Process oUnzipProcess = run.exec(sCmd);
                        oUnzipProcess.waitFor();
                        s_oLogger.debug("S2 Image UNZIP done ");

                    }
                    else {
                        s_oLogger.debug("S2 Image already unzipped");
                    }

                    String sGranuleFolder = sPath+sFileOnly+".SAFE/GRANULE/";
                    File oGranuleFolder = new File(sGranuleFolder);
                    String [] asGranuleSubFolders = oGranuleFolder.list(new FilenameFilter() {
                        @Override
                        public boolean accept(File current, String name) {
                            return new File(current, name).isDirectory();
                        }
                    });

                    String sProductSubFolder = sFileOnly;

                    if (asGranuleSubFolders != null) {
                        if (asGranuleSubFolders.length > 0) {
                            sProductSubFolder = asGranuleSubFolders[0];
                        }
                    }

                    s_oLogger.debug("S2 Image detected product subfolder = " + sProductSubFolder);

                    String sCompletePathOfFileToConvert = sGranuleFolder + sProductSubFolder + "/IMG_DATA/";

                    File oCompletePathOfFileToConvert = new File(sCompletePathOfFileToConvert);

                    String [] asBandImages = oCompletePathOfFileToConvert.list(new FilenameFilter() {
                        @Override
                        public boolean accept(File current, String name) {
                            return new File(current, name).isFile();
                        }
                    });

                    String sImageEnd = oParameter.getBandName();

                    if (sImageEnd.length()==2) {
                        sImageEnd.replace("B", "B0");
                    }

                    sImageEnd += ".jp2";

                    String sBandFile = sFileOnly;
                    if (asBandImages != null) {
                        if (asBandImages.length>0) {
                            sBandFile = asBandImages[0];

                            for (int iFiles=0; iFiles<asBandImages.length; iFiles++) {
                                if (asBandImages[iFiles].endsWith(sImageEnd)) {
                                    s_oLogger.debug("S2 Image band image found");
                                    sBandFile = asBandImages[iFiles];
                                    break;
                                }
                            }
                        }
                    }

                    s_oLogger.debug("S2 Image detected band image = " + sBandFile);

                    String sFileToConvert = sCompletePathOfFileToConvert + sBandFile;

                    s_oLogger.debug("FILE TO CONVERT = " + sFileToConvert);

                    // Set EPSG
                    sEPSG = CRS.lookupIdentifier(oSentinel.getSceneGeoCoding().getMapCRS(),true);

                    String sCmd = "gdal_translate  -of GTiff -a_srs "+ sEPSG + " " + sFileToConvert + " " + sFileToConvert+".tif";
                    s_oLogger.debug("S2 Image Convert: "+sCmd);
                    Process oTransformProcess = run.exec(sCmd);
                    oTransformProcess .waitFor();
                    s_oLogger.debug("S2 Image TRANSFORM done ");

                    // Set S2 Style
                    sStyle = "S2Test";
                    oOutputFile = new File(sFileToConvert+".tif");
                }
                catch (Exception oEx) {
                    s_oLogger.error(oEx.toString());
                    oEx.printStackTrace();
                }
            }
            else {
                s_oLogger.debug( "LauncherMain.PublishBandImage:  Get GeoCoding");

                // Get the Geocoding and Band
                GeoCoding oGeoCoding = oSentinel.getSceneGeoCoding();

                s_oLogger.debug( "LauncherMain.PublishBandImage:  Getting Band " + oParameter.getBandName());

                Band oBand = oSentinel.getBand(oParameter.getBandName());

                // Get Image
                MultiLevelImage oBandImage = oBand.getSourceImage();
                // Get TIFF Metadata
                GeoTIFFMetadata oMetadata = GeoCoding2GeoTIFFMetadata.createGeoTIFFMetadata(oGeoCoding, oBandImage.getWidth(),oBandImage.getHeight());

                s_oLogger.debug( "LauncherMain.PublishBandImage:  Output file: " + sOutputFilePath);

                // Write the Band Tiff
                if (ConfigReader.getPropValue("CREATE_BAND_GEOTIFF_ACTIVE").equals("true")) {
                    s_oLogger.debug("LauncherMain.PublishBandImage:  Writing Image");
                    GeoTIFF.writeImage(oBandImage, oOutputFile, oMetadata);
                }
                else {
                    s_oLogger.debug( "LauncherMain.PublishBandImage:  Debug on. Jump GeoTiff Generate");
                }
            }

            s_oLogger.debug( "LauncherMain.PublishBandImage:  Moving Band Image...");

            // Copy fie to GeoServer Data Dir
            String sGeoServerDataDir = ConfigReader.getPropValue("GEOSERVER_DATADIR");
            String sTargetDir = sGeoServerDataDir;

            if (!(sTargetDir.endsWith("/")||sTargetDir.endsWith("\\"))) sTargetDir+="/";
            sTargetDir+=sLayerId+"/";

            String sTargetFile = sTargetDir + oOutputFile.getName();

            File oTargetFile = new File(sTargetFile);

            s_oLogger.debug("LauncherMain.PublishBandImage: InputFile: " + sOutputFilePath + " TargetFile: " + sTargetFile + " LayerId " + sLayerId);

            FileUtils.copyFile(oOutputFile,oTargetFile);

            // Ok publish
            s_oLogger.debug("LauncherMain.PublishBandImage: call PublishImage");
            Publisher oPublisher = new Publisher();
            sLayerId = oPublisher.publishGeoTiff(sTargetFile,ConfigReader.getPropValue("GEOSERVER_ADDRESS"),ConfigReader.getPropValue("GEOSERVER_USER"),ConfigReader.getPropValue("GEOSERVER_PASSWORD"),ConfigReader.getPropValue("GEOSERVER_WORKSPACE"), sLayerId, sEPSG, sStyle);

            s_oLogger.debug("LauncherMain.PublishBandImage: Image published. ");

            s_oLogger.debug("LauncherMain.PublishBandImage: Get Image Bounding Box");

            //get bounding box from data base
            DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
            String sBBox = oDownloadedFilesRepository.GetDownloadedFile(oParameter.getFileName()).getBoundingBox();

            String sGeoserverBBox = GeoserverUtils.GetBoundingBox(sLayerId, "json");

            s_oLogger.debug("LauncherMain.PublishBandImage: Bounding Box: " + sBBox);
            s_oLogger.debug("LauncherMain.PublishBandImage: Geoserver Bounding Box: " + sGeoserverBBox + " for Layer Id " + sLayerId);
            s_oLogger.debug("LauncherMain.PublishBandImage: Update index and Send Rabbit Message");

            // Create Entity
            PublishedBand oPublishedBand = new PublishedBand();
            oPublishedBand.setLayerId(sLayerId);
            oPublishedBand.setProductName(sProductName);
            oPublishedBand.setBandName(oParameter.getBandName());
            oPublishedBand.setUserId(oParameter.getUserId());
            oPublishedBand.setWorkspaceId(oParameter.getWorkspace());
            oPublishedBand.setBoundingBox(sBBox);
            oPublishedBand.setGeoserverBoundingBox(sGeoserverBBox);

            // Add it the the db
            oPublishedBandsRepository.InsertPublishedBand(oPublishedBand);

            s_oLogger.debug("LauncherMain.PublishBandImage: Index Updated" );
            s_oLogger.debug("LauncherMain.PublishBandImage: Queue = " + oParameter.getQueue() + " LayerId = " + sLayerId);

            // Create the View Model
            PublishBandResultViewModel oVM = new PublishBandResultViewModel();
            oVM.setBandName(oParameter.getBandName());
            oVM.setProductName(sProductName);
            oVM.setLayerId(sLayerId);
            oVM.setBoundingBox(sBBox);
            oVM.setGeoserverBoundingBox(sGeoserverBBox);

            boolean bRet = oSendToRabbit.SendRabbitMessage(true,LauncherOperations.PUBLISHBAND, oParameter.getWorkspace(),oVM,oParameter.getExchange());

            if (bRet == false) {
                s_oLogger.debug("LauncherMain.PublishBandImage: Error sending Rabbit Message");
            }
        }
        catch (Exception oEx) {

            s_oLogger.debug( "LauncherMain.PublishBandImage: Exception " + oEx.toString() + " " + oEx.getMessage());

            oEx.printStackTrace();

            boolean bRet = oSendToRabbit.SendRabbitMessage(false,LauncherOperations.PUBLISHBAND,oParameter.getWorkspace(),null,oParameter.getExchange());

            if (bRet == false) {
                s_oLogger.debug("LauncherMain.PublishBandImage:  Error sending exception Rabbit Message");
            }
        }
        finally{
            //delete process from list
            try{
                ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
                oProcessWorkspaceRepository.DeleteProcessWorkspace(oParameter.getProcessObjId());
            }
            catch (Exception oEx) {
                s_oLogger.debug("LauncherMain.PublishBandImage: Exception deleting process " + oEx.toString());
            }
        }

        return  sLayerId;
    }


    /**
     * Generic publish function. NOTE: probably will not be used, use publish band instead
     * @param oParameter
     * @return
     */
    public void RasterGeometricResample(RasterGeometricResampleParameter oParameter) {

        s_oLogger.debug("LauncherMain.RasterGeometricResample: Start");

        // Rabbit Sender
        Send oSendToRabbit = new Send();

        try {
            //Init rabbit exchange and queue
            if (oSendToRabbit.Init(oParameter.getWorkspace(), oParameter.getUserId()) == false)
            {
                s_oLogger.debug("LauncherMain.RasterGeometricResample: Failed initializing RabbitMQ");
                return;
            }

            //send update process message
            if (oSendToRabbit.SendUpdateProcessMessage(oParameter.getWorkspace()) == false)
            {
                s_oLogger.debug("LauncherMain.RasterGeometricResample: Error sending rabbitmq message to update process list");
            }

            // Read File Name
            String sFile = oParameter.getSourceProductName();
            String sFileNameOnly = sFile;

            String sRootPath = ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH");
            if (!sRootPath.endsWith("/")) sRootPath += "/";
            final String sPath = sRootPath + oParameter.getUserId() + "/" + oParameter.getWorkspace() + "/";
            sFile = sPath + sFile;

            // Check integrity
            if (Utils.isNullOrEmpty(sFile)) {
                s_oLogger.debug("LauncherMain.RasterGeometricResample: file is null or empty");
                return;
            }

            File oSourceFile = new File(sFile);

            //FileUtils.copyFile(oDownloadedFile, oTargetFile);
            WriteProduct oWriter = new WriteProduct();

            ReadProduct oReadProduct = new ReadProduct();

            s_oLogger.debug("LauncherMain.RasterGeometricResample: Read Product");
            Product oSourceProduct = oReadProduct.ReadProduct(oSourceFile, null);

            if (oSourceProduct == null)
            {
                throw new Exception("LauncherMain.RasterGeometricResample: Source Product null");
            }

            //Terrain Operation
            s_oLogger.debug("LauncherMain.RasterGeometricResample: RasterGeometricResample");
            RasterGeometricResampling oRasterGeometricResample = new RasterGeometricResampling();
            Product oResampledProduct = oRasterGeometricResample.getResampledProduct(oSourceProduct, oParameter.getBandName());

            if (oResampledProduct == null)
            {
                throw new Exception("LauncherMain.RasterGeometricResample: RasterGeometricResample product null");
            }

            s_oLogger.debug("LauncherMain.RasterGeometricResample: convert product to view model");
            String sOutFile = oWriter.WriteBEAMDIMAP(oResampledProduct, sPath, sFileNameOnly+"_resampled");

            ConvertProductToViewModelAndSendToRabbit(new ProductViewModel(), sOutFile, oParameter.getWorkspace(), oParameter.getExchange(), LauncherOperations.RASTERGEOMETRICRESAMPLE, null);

        }
        catch (Exception oEx) {
            s_oLogger.debug("LauncherMain.RasterGeometricResample: exception " + oEx.toString());

            oSendToRabbit.SendRabbitMessage(false,LauncherOperations.RASTERGEOMETRICRESAMPLE,oParameter.getWorkspace(),null,oParameter.getExchange());
        }
        finally{
            s_oLogger.debug("LauncherMain.RasterGeometricResample: End");

            //delete process from list
            try{
                ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
                oProcessWorkspaceRepository.DeleteProcessWorkspace(oParameter.getProcessObjId());
            }
            catch (Exception oEx) {
                s_oLogger.debug("LauncherMain.RasterGeometricResample: Exception deleting process " + oEx.toString());
            }
        }
    }


    /**
     * Generic publish function. NOTE: probably will not be used, use publish band instead
     * @param oParameter
     * @return
     */
    /*
    public String Publish(PublishParameters oParameter) {

        //System.setProperty("snap.home", "C:\\Codice\\esa\\wasdi\\wrappersnap\\snap-desktop\\snap-application\\target\\snap\\etc\\snap.properties");

        JAI.getDefaultInstance().getTileScheduler().setParallelism(Runtime.getRuntime().availableProcessors());
        MemUtils.configureJaiTileCache();

        String sLayerId = "";
        // Rabbit Sender
        Send oSendToRabbit = new Send();

        try {
            //Init rabbit exchange and queue
            if (oSendToRabbit.Init(oParameter.getWorkspace(), oParameter.getUserId()) == false)
            {
                s_oLogger.debug("LauncherMain.Publish: Failed initializing RabbitMQ");
                return sLayerId;
            }

            //send update process message
            if (oSendToRabbit.SendUpdateProcessMessage(oParameter.getWorkspace()) == false)
            {
                s_oLogger.debug("LauncherMain.Publish: Error sending rabbitmq message to update process list");
            }

            // Read File Name
            String sFile = oParameter.getFileName();

            String sPath = ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH");
            if (!sPath.endsWith("/")) sPath += "/";
            sPath += oParameter.getUserId() + "/" + oParameter.getWorkspace() + "/";
            sFile = sPath + sFile;


            // Check integrity
            if (Utils.isNullOrEmpty(sFile)) {
                s_oLogger.debug("LauncherMain.Publish: file is null or empty");
                return sLayerId;
            }

            // Create a file object for the downloaded file
            File oDownloadedFile = new File(sFile);
            String sInputFileNameOnly = oDownloadedFile.getName();
            sLayerId = sInputFileNameOnly;

            sLayerId = Utils.GetFileNameWithoutExtension(sFile);

            // Copy fie to GeoServer Data Dir
            String sGeoServerDataDir = ConfigReader.getPropValue("GEOSERVER_DATADIR");
            String sTargetDir = sGeoServerDataDir;

            if (!(sTargetDir.endsWith("/") || sTargetDir.endsWith("\\"))) sTargetDir += "/";
            sTargetDir += sLayerId + "/";

            String sTargetFile = sTargetDir + sInputFileNameOnly;

            File oTargetFile = new File(sTargetFile);

            s_oLogger.debug("LauncherMain.publish: InputFile: " + sFile + " TargetFile: " + sTargetDir + " LayerId " + sLayerId);

            //FileUtils.copyFile(oDownloadedFile, oTargetFile);
            WriteProduct oWriter = new WriteProduct();

            ReadProduct oReadProduct = new ReadProduct();
            //TODO: Here recognize the file type and run the right procedure. At the moment assume Sentinel1A
            s_oLogger.debug("LauncherMain.publish: Read Product");
            Product oOriginProduct = oReadProduct.ReadProduct(oDownloadedFile, null);

            //TODO: questo pezzo dell'anteprima bisognerà spostarlo nella lettura del prodotto, ora volevo vedere solo come funzionava
            //Quicklook oQuickLook = oOriginProduct.getDefaultQuicklook();
            //BufferedImage oImage = oQuickLook.getImage(ProgressMonitor.NULL);
            //----------------------------------------------------------------------------------

            if (oOriginProduct == null)
            {
                s_oLogger.debug("LauncherMain.publish: Product null");
                return null;
            }
            //Calibration
            s_oLogger.debug("LauncherMain.publish: Calibrate Product");
            Calibration oCalibration = new Calibration();
            String[] asBands = new String[]{oOriginProduct.getBandAt(0).getName()};
            Product oCalibratedProduct = oCalibration.getCalibration(oOriginProduct, asBands);
            String sCalibrateProduct = oWriter.WriteBigTiff(oCalibratedProduct, sPath, oCalibratedProduct.getName(), null);

            oCalibratedProduct = oReadProduct.ReadProduct(new File(sCalibrateProduct), null);

            if (oCalibratedProduct == null)
            {
                s_oLogger.debug("LauncherMain.publish: Calibrated Product null");
                return null;
            }
            //oWriter.WriteBigTiff(oCalibratedProduct, oCalibratedProduct.getName(), sPath);
            //Filter
            s_oLogger.debug("LauncherMain.publish: Filter Product");
            Filter oFilter = new Filter();
            asBands[0] = oCalibratedProduct.getBandAt(0).getName();
            Product oFilteredProduct = oFilter.getFilter(oCalibratedProduct, asBands);
            String sFilterProduct = oWriter.WriteBigTiff(oFilteredProduct, sPath, oFilteredProduct.getName(), null);
            if (oFilteredProduct == null)
            {
                s_oLogger.debug("LauncherMain.publish: Filtered Product null");
                return null;
            }
            oFilteredProduct = oReadProduct.ReadProduct(new File(sFilterProduct), null);
            //Multilooking
            s_oLogger.debug("LauncherMain.publish: Multilooking Product");
            Multilooking oMultilooking = new Multilooking();
            asBands[0] = oFilteredProduct.getBandAt(0).getName();
            Product oMultilookedProduct = oMultilooking.getMultilooking(oFilteredProduct, asBands);
            String sMultiProduct = oWriter.WriteBigTiff(oMultilookedProduct, sPath, oMultilookedProduct.getName(), null);
            if (oMultilookedProduct == null)
            {
                s_oLogger.debug("LauncherMain.publish: Multilook Product null");
                return null;
            }
            oMultilookedProduct = oReadProduct.ReadProduct(new File(sMultiProduct), null);
            //Terrain
            s_oLogger.debug("LauncherMain.publish: Terrain Product");
            TerrainCorrection oTerrainCorrection = new TerrainCorrection();
            asBands[0] = oMultilookedProduct.getBandAt(0).getName();
            Product oTerrainProduct = oTerrainCorrection.getTerrainCorrection(oMultilookedProduct, asBands);
            if (oTerrainProduct == null)
            {
                s_oLogger.debug("LauncherMain.publish: Terrain product null");
                return null;
            }

            //s_oLogger.debug("LauncherMain.publish: Write Big Tiff");
            String sTiffFile = oWriter.WriteBigTiff(oTerrainProduct, sPath, oTerrainProduct.getName(), null);

            // Generate file output name
            sLayerId = Utils.GetFileNameWithoutExtension(oTerrainProduct.getName());
            String sOutputFilePath = sPath + sLayerId + ".tif";
            //File oOutputFile = new File(sOutputFilePath);


            // Check result
            if (Utils.isNullOrEmpty(sTiffFile)) {
                s_oLogger.debug("LauncherMain.Publish: Tiff File is null or empty");
                return sLayerId;
            }

            // Ok publish
            //sLayerId = oTerrainProduct.getName();
            s_oLogger.debug("LauncherMain.publish: Layer id " + sLayerId);
            s_oLogger.debug("LauncherMain.publish: call PublishImage");
            Publisher oPublisher = new Publisher();
            sLayerId = oPublisher.publishGeoTiff(sOutputFilePath, ConfigReader.getPropValue("GEOSERVER_ADDRESS"), ConfigReader.getPropValue("GEOSERVER_USER"), ConfigReader.getPropValue("GEOSERVER_PASSWORD"), ConfigReader.getPropValue("GEOSERVER_WORKSPACE"), sLayerId);

            s_oLogger.debug("LauncherMain.publish: Image published. Send Rabbit Message");
            Send oSendLayerId = new Send();
            s_oLogger.debug("LauncherMain.publish: Queue = " + oParameter.getQueue() + " LayerId = " + sLayerId);

            RabbitMessageViewModel oMessageViewModel = new RabbitMessageViewModel();
            oMessageViewModel.setMessageCode(LauncherOperations.PUBLISH);
            oMessageViewModel.setWorkspaceId(oParameter.getWorkspace());
            oMessageViewModel.setMessageResult("OK");
            String sJSON = MongoRepository.s_oMapper.writeValueAsString(oMessageViewModel);

            if (oSendLayerId.SendMsgOnExchange(oParameter.getExchange(), sJSON) == false) {
                s_oLogger.debug("LauncherMain.publish: Error sending Rabbit Message");
            }

            // Deletes the copy of the Zip file
            s_oLogger.debug("LauncherMain.publish: delete Zip File Copy " + oTargetFile.getPath());

            if (oTargetFile.delete() == false) {
                s_oLogger.debug("LauncherMain.publish: impossible to delete zip file");
            }
        }
        catch (Exception oEx) {
            s_oLogger.debug("LauncherMain.Publish: exception " + oEx.toString());

            RabbitMessageViewModel oMessageViewModel = new RabbitMessageViewModel();
            oMessageViewModel.setMessageCode(LauncherOperations.PUBLISH);
            oMessageViewModel.setWorkspaceId(oParameter.getWorkspace());
            oMessageViewModel.setMessageResult("KO");

            try {
                String sJSON = MongoRepository.s_oMapper.writeValueAsString(oMessageViewModel);
                oSendToRabbit.SendMsgOnExchange(oParameter.getExchange(), sJSON);
            }
            catch (Exception oEx2) {
                s_oLogger.debug("LauncherMain.Publish: Inner Exception " + oEx2.toString());
            }
        }
        finally{
            //delete process from list
            try{
                ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
                oProcessWorkspaceRepository.DeleteProcessWorkspace(oParameter.getProcessObjId());
            }
            catch (Exception oEx) {
                s_oLogger.debug("LauncherMain.Publish: Exception deleting process " + oEx.toString());
            }
        }

        return sLayerId;
    }
    */


}
