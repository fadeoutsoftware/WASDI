package wasdi;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.geotiff.GeoCoding2GeoTIFFMetadata;
import org.esa.snap.core.util.geotiff.GeoTIFF;
import org.esa.snap.core.util.geotiff.GeoTIFFMetadata;
import org.esa.snap.runtime.Config;
import org.esa.snap.runtime.Engine;
import org.geotools.referencing.CRS;

import com.bc.ceres.glevel.MultiLevelImage;

import sun.management.VMManagement;
import wasdi.filebuffer.DownloadFile;
import wasdi.geoserver.Publisher;
import wasdi.rabbit.Send;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.PublishedBand;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.PublishedBandsRepository;
import wasdi.shared.parameters.ApplyOrbitParameter;
import wasdi.shared.parameters.CalibratorParameter;
import wasdi.shared.parameters.DownloadFileParameter;
import wasdi.shared.parameters.FilterParameter;
import wasdi.shared.parameters.MultilookingParameter;
import wasdi.shared.parameters.NDVIParameter;
import wasdi.shared.parameters.OperatorParameter;
import wasdi.shared.parameters.PublishBandParameter;
import wasdi.shared.parameters.RangeDopplerGeocodingParameter;
import wasdi.shared.parameters.RasterGeometricResampleParameter;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.ProductViewModel;
import wasdi.shared.viewmodels.PublishBandResultViewModel;
import wasdi.snapopearations.ApplyOrbit;
import wasdi.snapopearations.BaseOperation;
import wasdi.snapopearations.Calibration;
import wasdi.snapopearations.Filter;
import wasdi.snapopearations.Multilooking;
import wasdi.snapopearations.NDVI;
import wasdi.snapopearations.RasterGeometricResampling;
import wasdi.snapopearations.ReadProduct;
import wasdi.snapopearations.TerrainCorrection;
import wasdi.snapopearations.WriteProduct;




/**
 * Created by s.adamo on 23/09/2016.
 */
public class LauncherMain {

    // Define a static s_oLogger variable so that it references the
    // Logger instance named "MyApp".
    public static Logger s_oLogger = Logger.getLogger(LauncherMain.class);
    
    public static Send oSendToRabbit = null;

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
            LauncherMain.oSendToRabbit = new Send();
            LauncherMain oLauncher = new LauncherMain();

            s_oLogger.debug("Executing " + sOperation + " Parameter " + sParameter);

            // And Run
            oLauncher.ExecuteOperation(sOperation,sParameter);

            s_oLogger.debug("Operation Done, bye");
            LauncherMain.oSendToRabbit.Free();

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
            Publisher.GDAL_Retile_Command = ConfigReader.getPropValue("GDAL_RETILE", Publisher.GDAL_Retile_Command);
            MongoRepository.SERVER_ADDRESS = ConfigReader.getPropValue("MONGO_ADDRESS");
            MongoRepository.SERVER_PORT = Integer.parseInt(ConfigReader.getPropValue("MONGO_PORT"));
            MongoRepository.DB_NAME = ConfigReader.getPropValue("MONGO_DBNAME");
            MongoRepository.DB_USER = ConfigReader.getPropValue("MONGO_DBUSER");
            MongoRepository.DB_PWD = ConfigReader.getPropValue("MONGO_DBPWD");

            System.setProperty("user.home", ConfigReader.getPropValue("USER_HOME"));

            Path propFile = Paths.get(ConfigReader.getPropValue("SNAP_AUX_PROPERTIES"));
            Config.instance("snap.auxdata").load(propFile);
            Config.instance().load();

            //JAI.getDefaultInstance().getTileScheduler().setParallelism(Runtime.getRuntime().availableProcessors());
            //MemUtils.configureJaiTileCache();
            
            SystemUtils.init3rdPartyLibs(null);
            Engine.start(false);

        }
        catch (IOException e) {
            e.printStackTrace();
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
                case LauncherOperations.APPLYORBIT:{

                    // Deserialize Parameters
                    ApplyOrbitParameter oParameter = (ApplyOrbitParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    ExecuteOperator(oParameter, new ApplyOrbit(), LauncherOperations.APPLYORBIT);

                }
                break;
                case LauncherOperations.CALIBRATE:{

                    // Deserialize Parameters
                    CalibratorParameter oParameter = (CalibratorParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    ExecuteOperator(oParameter, new Calibration(), LauncherOperations.CALIBRATE);

                }
                break;
                case LauncherOperations.MULTILOOKING:{

                    // Deserialize Parameters
                    MultilookingParameter oParameter = (MultilookingParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    ExecuteOperator(oParameter, new Multilooking(), LauncherOperations.MULTILOOKING);

                }
                break;
                case LauncherOperations.TERRAIN:{

                    // Deserialize Parameters
                    RangeDopplerGeocodingParameter oParameter = (RangeDopplerGeocodingParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    ExecuteOperator(oParameter, new TerrainCorrection(), LauncherOperations.TERRAIN);

                }
                break;
                case LauncherOperations.FILTER:{

                    // Deserialize Parameters
                    FilterParameter oParameter = (FilterParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    ExecuteOperator(oParameter, new Filter(), LauncherOperations.FILTER);

                }
                break;
                case LauncherOperations.NDVI:{

                    // Deserialize Parameters
                    NDVIParameter oParameter = (NDVIParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    ExecuteOperator(oParameter, new NDVI(), LauncherOperations.NDVI);

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

            //send update process message
            if (!oSendToRabbit.SendUpdateProcessMessage(oParameter.getWorkspace())) {
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
                    s_oLogger.debug("LauncherMain.Download: File not already downloaded. Download it");

                    // No: it isn't: download it
                    sFileName = oDownloadFile.ExecuteDownloadFile(oParameter.getUrl(), oParameter.getDownloadUser(), oParameter.getDownloadPassword(), sDownloadPath);

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
                        }
                    }

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
     * Acquire a file in a workspace. Normally used for an uploaded file
     * @param oParameter
     * @param sDownloadPath
     * @return
     */
    public String Acquire(DownloadFileParameter oParameter, String sAcquisitionBasePath, String sLocalFilePath) {
        String sFileName = "";

        try {
            s_oLogger.debug("LauncherMain.Acquire: Start");

            //send update process message
            if (oSendToRabbit.SendUpdateProcessMessage(oParameter.getWorkspace()) == false) {
                s_oLogger.debug("LauncherMain.Download: Error sending rabbitmq message to update process list");
            }

            
            File oAcquisitionPath = new File(new File(sAcquisitionBasePath), oParameter.getUserId()+"/"+oParameter.getWorkspace());
            File oFileToAcquire = new File(sLocalFilePath);
            
            s_oLogger.debug("LauncherMain.AcquisitionPath: " + oAcquisitionPath.getAbsolutePath());

            // Product view Model
            ProductViewModel oVM = null;

            s_oLogger.debug("LauncherMain.AcquisitionPath: Check if file exists");
            
            if (!oAcquisitionPath.isDirectory() || !oFileToAcquire.canRead()) {
                s_oLogger.debug("LauncherMain.AcquisitionPath: acquisition path is not a directory or cannot read local file " + oFileToAcquire.getAbsolutePath());
                oSendToRabbit.SendRabbitMessage(false,LauncherOperations.DOWNLOAD,oParameter.getWorkspace(),null,oParameter.getExchange());
            }
            
            File oDstFile = new File(oAcquisitionPath, oFileToAcquire.getName());
			FileUtils.copyFile(oFileToAcquire, oDstFile);
            sFileName = oDstFile.getAbsolutePath();

            ConvertProductToViewModelAndSendToRabbit(oVM, sFileName, oParameter.getWorkspace(), oParameter.getExchange(), LauncherOperations.DOWNLOAD, oParameter.getBoundingBox());
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
     * @param oVM View Model... if null, read it from the product in sFileName
     * @param sFileName File Name
     * @param sWorkspace Workspace
     * @param sExchange Queue Id
     * @param sOperation Operation Done
     * @param sBBox Bounding Box
     */
    private void ConvertProductToViewModelAndSendToRabbit(ProductViewModel oVM, String sFileName, String sWorkspace, String sExchange, String sOperation, String sBBox) throws Exception
    {
        s_oLogger.debug("LauncherMain.ConvertProductToViewModelAndSendToRabbit: File Name = " + sFileName);

        // Get The product view Model            
        if (oVM == null) {
            ReadProduct oReadProduct = new ReadProduct();
            s_oLogger.debug("LauncherMain.ConvertProductToViewModelAndSendToRabbit: call read product");
            File oProductFile = new File(sFileName);
            oVM = oReadProduct.getProductViewModel(new File(sFileName));
            oVM.setMetadata(oReadProduct.getProductMetadataViewModel(oProductFile));

            if (oVM.getBandsGroups() == null) {
            	s_oLogger.debug("LauncherMain.ConvertProductToViewModelAndSendToRabbit: Band Groups is NULL");
            } else if (oVM.getBandsGroups().getBands() == null) {
            	s_oLogger.debug("LauncherMain.ConvertProductToViewModelAndSendToRabbit: bands is NULL");
            } else {
                s_oLogger.debug("LauncherMain.ConvertProductToViewModelAndSendToRabbit: bands " + oVM.getBandsGroups().getBands().size());
            }

            s_oLogger.debug("LauncherMain.ConvertProductToViewModelAndSendToRabbit: done read product");
        }
        
        
        // P.Campanella 12/05/2017: it looks it is done before. Let leave here a check
        DownloadedFilesRepository oDownloadedRepo = new DownloadedFilesRepository();
        
        DownloadedFile oCheck = oDownloadedRepo.GetDownloadedFile(oVM.getFileName());
        
        if (oCheck == null) {
        	s_oLogger.debug("Insert in db");
        	
            // Save it in the register
            DownloadedFile oAlreadyDownloaded = new DownloadedFile();
            File oFile = new File(sFileName);
            oAlreadyDownloaded.setFileName(oFile.getName());
            oAlreadyDownloaded.setFilePath(sFileName);
            oAlreadyDownloaded.setProductViewModel(oVM);
            oAlreadyDownloaded.setBoundingBox(sBBox);
            
            oDownloadedRepo.InsertDownloadedFile(oAlreadyDownloaded);            	
        }
        

        s_oLogger.debug("OK DONE");

        s_oLogger.debug("LauncherMain.ConvertProductToViewModelAndSendToRabbit: Image downloaded. Send Rabbit Message");

        s_oLogger.debug("LauncherMain.ConvertProductToViewModelAndSendToRabbit: Exchange = " + sExchange);
        
        //P.Campanella 12/05/2017: Metadata are saved in the DB but sent back to the client with a dedicated API. 
        // So here metadata are nulled
        oVM.setMetadata(null);

        oSendToRabbit.SendRabbitMessage(true,sOperation,sWorkspace,oVM,sExchange);
            
//        }
//        catch(Exception oEx)
//        {
//            s_oLogger.error(oEx.toString());
//            oEx.printStackTrace();
//        }
    }

    /**
     * Generic Execute Operation Method
     * @param oParameter
     * @return
     */
    public void ExecuteOperator(OperatorParameter oParameter, BaseOperation oOperation, String sTypeOperation) {

        s_oLogger.debug("LauncherMain.ExecuteOperation: Start operation " + sTypeOperation);

        try {
        	
        	// P.Campanella 12/05/2017: add the process id to the table
            ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
            ProcessWorkspace oProcessWorkspace = oProcessWorkspaceRepository.GetProcessByProductName(oParameter.getSourceProductName());
            
            if (oProcessWorkspace != null) {
            	
                //get process pid
                oProcessWorkspace.setPid(GetProcessId());
                //update the process
                if (!oProcessWorkspaceRepository.UpdateProcess(oProcessWorkspace))
                    s_oLogger.debug("LauncherMain.ExecuteOperation: Error during process update (add PID)");
            }        	
        	
        	
            //send update process message
            if (!oSendToRabbit.SendUpdateProcessMessage(oParameter.getWorkspace())) {
                s_oLogger.debug("LauncherMain.ExecuteOperation: Error sending rabbitmq message to update process list");
            }

            // Read File Name
            String sFile = oParameter.getSourceProductName();

            String sRootPath = ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH");
            if (!sRootPath.endsWith("/")) sRootPath += "/";
            final String sPath = sRootPath + oParameter.getUserId() + "/" + oParameter.getWorkspace() + "/";
            sFile = sPath + sFile;

            // Check integrity
            if (Utils.isNullOrEmpty(sFile)) {
                s_oLogger.debug("LauncherMain.ExecuteOperation: file is null or empty");

                oSendToRabbit.SendRabbitMessage(false,sTypeOperation,oParameter.getWorkspace(),null,oParameter.getExchange());

                return;
            }

            File oSourceFile = new File(sFile);
            WriteProduct oWriter = new WriteProduct();

            ReadProduct oReadProduct = new ReadProduct();
            s_oLogger.debug("LauncherMain.ExecuteOperation: Read Product");
            Product oSourceProduct = oReadProduct.ReadProduct(oSourceFile, null);

            if (oSourceProduct == null)
            {
                throw new Exception("LauncherMain.ExecuteOperation: Source Product null");
            }

            //Operation
            s_oLogger.debug("LauncherMain.ExecuteOperation: Execute Operation");
            Product oTargetProduct = oOperation.getOperation(oSourceProduct, oParameter.getSettings());
            if (oTargetProduct == null)
            {
                throw new Exception("LauncherMain.ExecuteOperation: Output Product is null");
            }

            String sTargetFileName = oTargetProduct.getName();

            if (!Utils.isNullOrEmpty(oParameter.getDestinationProductName()))
                sTargetFileName = oParameter.getDestinationProductName();

            s_oLogger.debug("LauncherMain.ExecuteOperation: Save Output Product " + sTargetFileName);

            //writing product in default snap format
            String sTargetAbsFileName = oWriter.WriteBEAMDIMAP(oTargetProduct, sPath, sTargetFileName);

            if (Utils.isNullOrEmpty(sTargetAbsFileName))
            {
                throw new Exception("LauncherMain.ExecuteOperation: Tiff not created");
            }

            s_oLogger.debug("LauncherMain.ExecuteOperation: convert product to view model");

            // P.Campanella 12/05/2017: get the BB from the orginal product
            // Get the original Bounding Box
            DownloadedFilesRepository oDownloadedRepo = new DownloadedFilesRepository();
            DownloadedFile oAlreadyDownloaded = oDownloadedRepo.GetDownloadedFile(sFile) ;
            String sBB = "";
            
            if (oAlreadyDownloaded != null) {
            	sBB = oAlreadyDownloaded.getBoundingBox();
            }
            
            ConvertProductToViewModelAndSendToRabbit(null, sTargetAbsFileName, oParameter.getWorkspace(), oParameter.getExchange(), sTypeOperation, sBB);

            //this.PublishOnGeoserver(oParameter.getPublishParameter(), oTerrainProduct.getName(), sBandName);

        }
        catch (Exception oEx) {
            s_oLogger.debug("LauncherMain.ExecuteOperation: exception " + oEx.toString());
            oSendToRabbit.SendRabbitMessage(false,sTypeOperation,oParameter.getWorkspace(),null,oParameter.getExchange());
        }
        finally{
            s_oLogger.debug("LauncherMain.ExecuteOperation: End");

            //delete process from list
            try{
                ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
                oProcessWorkspaceRepository.DeleteProcessWorkspace(oParameter.getProcessObjId());
            }
            catch (Exception oEx) {
                s_oLogger.debug("LauncherMain.ExecuteOperation: Exception deleting process " + oEx.toString());
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

        try {

            ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
            ProcessWorkspace oProcessWorkspace = oProcessWorkspaceRepository.GetProcessByProductName(oParameter.getFileName());

            if (oProcessWorkspace != null) {
                //get process pid
                oProcessWorkspace.setPid(GetProcessId());
                //update the process
                oProcessWorkspaceRepository.UpdateProcess(oProcessWorkspace);
            }

            //send update process message
            if (!oSendToRabbit.SendUpdateProcessMessage(oParameter.getWorkspace())) {
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
//            String sEPSG = "EPSG:4326";
            // Default Style: can be changed in the following lines depending by the product
            String sStyle = "raster";

            s_oLogger.debug( "LauncherMain.PublishBandImage:  Generating Band Image...");

            // Read the product
            ReadProduct oReadProduct = new ReadProduct();
            Product oSentinel = oReadProduct.ReadProduct(oFile, null);
            String sEPSG = CRS.lookupIdentifier(oSentinel.getSceneGeoCoding().getMapCRS(),true);

            String sOutputFilePath = sPath + sLayerId + ".tif";
            File oOutputFile = new File(sOutputFilePath);

            
            if (oSentinel.getProductType().startsWith("S2") && oSentinel.getProductReader().getClass().getName().startsWith("org.esa.s2tbx")) {
            	
            	s_oLogger.debug( "LauncherMain.PublishBandImage:  Managing S2 Product");
            	
				s_oLogger.debug( "LauncherMain.PublishBandImage:  Getting Band " + oParameter.getBandName());
				Band oBand = oSentinel.getBand(oParameter.getBandName());            
				Product oGeotiffProduct = new Product(oParameter.getBandName(), "GEOTIFF");
				oGeotiffProduct.addBand(oBand);                 
				sOutputFilePath = new WriteProduct().WriteGeoTiff(oGeotiffProduct, sPath, sLayerId);
				oOutputFile = new File(sOutputFilePath);
				s_oLogger.debug( "LauncherMain.PublishBandImage:  Geotiff File Created (EPSG=" + sEPSG + "): " + sOutputFilePath);
				
            } else {
            	
            	s_oLogger.debug( "LauncherMain.PublishBandImage:  Managing NON S2 Product");
            	
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
            boolean bResultPublishBand = true;
            if (sLayerId == null) {
                bResultPublishBand = false;
                s_oLogger.debug("LauncherMain.PublishBandImage: Image not published . ");
            }

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

            boolean bRet = oSendToRabbit.SendRabbitMessage(bResultPublishBand,LauncherOperations.PUBLISHBAND, oParameter.getWorkspace(),oVM,oParameter.getExchange());

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

        try {

            //send update process message
            if (!oSendToRabbit.SendUpdateProcessMessage(oParameter.getWorkspace())) {
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

            ConvertProductToViewModelAndSendToRabbit(null, sOutFile, oParameter.getWorkspace(), oParameter.getExchange(), LauncherOperations.RASTERGEOMETRICRESAMPLE, null);

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


}
