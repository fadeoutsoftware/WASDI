package wasdi;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

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
import com.fasterxml.jackson.core.JsonProcessingException;

import sun.management.VMManagement;
import wasdi.asynch.SaveMetadataThread;
import wasdi.filebuffer.DownloadFile;
import wasdi.geoserver.Publisher;
import wasdi.processors.WasdiProcessorEngine;
import wasdi.rabbit.Send;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.DownloadedFileCategory;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.ProductWorkspace;
import wasdi.shared.business.PublishedBand;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProductWorkspaceRepository;
import wasdi.shared.data.PublishedBandsRepository;
import wasdi.shared.geoserver.GeoServerManager;
import wasdi.shared.parameters.ApplyOrbitParameter;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.CalibratorParameter;
import wasdi.shared.parameters.DeployProcessorParameter;
import wasdi.shared.parameters.DownloadFileParameter;
import wasdi.shared.parameters.FilterParameter;
import wasdi.shared.parameters.FtpUploadParameters;
import wasdi.shared.parameters.GraphParameter;
import wasdi.shared.parameters.IngestFileParameter;
import wasdi.shared.parameters.MultilookingParameter;
import wasdi.shared.parameters.NDVIParameter;
import wasdi.shared.parameters.OperatorParameter;
import wasdi.shared.parameters.PublishBandParameter;
import wasdi.shared.parameters.RangeDopplerGeocodingParameter;
import wasdi.shared.parameters.RasterGeometricResampleParameter;
import wasdi.shared.utils.FtpClient;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.BandViewModel;
import wasdi.shared.viewmodels.MetadataViewModel;
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
import wasdi.snapopearations.WasdiGraph;
import wasdi.snapopearations.WriteProduct;




/**
 * Created by s.adamo on 23/09/2016.
 */
public class LauncherMain {

    // Define a static s_oLogger variable so that it references the
    // Logger instance named "MyApp".
    public static Logger s_oLogger = Logger.getLogger(LauncherMain.class);
    
    public static Send s_oSendToRabbit = null;

    /**
     * Launcher Main Entry Point
     * 
     * @param args -operation <operation> -elaboratefile <file>
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {


        try {
            //get jar directory
            File oCurrentFile = new File(LauncherMain.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            //configure log
            String sThisFilePath = oCurrentFile.getParentFile().getPath();
            DOMConfigurator.configure(sThisFilePath + "/log4j.xml");

        }
        catch(Exception exp)
        {
            //no log4j configuration
            System.err.println( "Launcher Main - Error loading log.  Reason: " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(exp) );
            System.exit(-1);
        }

        s_oLogger.debug("Launcher Main Start");


        // create the parser
        CommandLineParser oParser = new DefaultParser();

        // create Options object
        Options oOptions = new Options();


        Option oOptOperation   = OptionBuilder.withArgName( "operation" ).hasArg().withDescription(  "" ).create( "operation" );
        Option oOptParameter   = OptionBuilder.withArgName( "parameter" ).hasArg().withDescription(  "" ).create( "parameter" );


        oOptions.addOption(oOptOperation);
        oOptions.addOption(oOptParameter);
        
        String sOperation = "";
        String sParameter = "";

        try {

            // parse the command line arguments
            CommandLine oLine = oParser.parse( oOptions, args );
            
            if (oLine.hasOption("operation")) {
                // Get the Operation Code
                sOperation  = oLine.getOptionValue("operation");

            }

            if (oLine.hasOption("parameter")) {
                // Get the Parameter File
                sParameter = oLine.getOptionValue("parameter");
            }

            // Create Launcher Instance
            LauncherMain.s_oSendToRabbit = new Send();
            LauncherMain oLauncher = new LauncherMain();

            s_oLogger.debug("Executing " + sOperation + " Parameter " + sParameter);

            String sSnapLogActive = ConfigReader.getPropValue("SNAPLOGACTIVE", "0");
            
            if (sSnapLogActive.equals("1") || sSnapLogActive.equalsIgnoreCase("true")) {
                String sSnapLogFolder = ConfigReader.getPropValue("SNAPLOGFOLDER", "/usr/lib/wasdi/launcher/logs/snap.log");

                FileHandler oFileHandler = new FileHandler(sSnapLogFolder,true);
                //ConsoleHandler handler = new ConsoleHandler();
                oFileHandler.setLevel(Level.ALL);
                SimpleFormatter oSimpleFormatter = new SimpleFormatter();
                oFileHandler.setFormatter(oSimpleFormatter);
                SystemUtils.LOG.setLevel(Level.ALL);
                SystemUtils.LOG.addHandler(oFileHandler);            	
            }

            // And Run
            oLauncher.ExecuteOperation(sOperation,sParameter);

            s_oLogger.debug("Operation Done, bye");
        }
        catch( ParseException exp ) {
            s_oLogger.error("Launcher Main Exception " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(exp));
            // oops, something went wrong
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
            
            
            try {
            	System.err.println("LauncherMain: try to put process in Safe ERROR state");
            	BaseParameter oBaseParameter = (BaseParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
                ProcessWorkspace oProcessWorkspace = oProcessWorkspaceRepository.GetProcessByProcessObjId(oBaseParameter.getProcessObjId());
                
                if (oProcessWorkspace != null) {
    				oProcessWorkspace.setProgressPerc(100);
    				oProcessWorkspace.setOperationEndDate(Utils.GetFormatDate(new Date()));
    				oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
    		        if (!oProcessWorkspaceRepository.UpdateProcess(oProcessWorkspace)) {
    		        	s_oLogger.debug("LauncherMain FINAL catch: Error during process update (terminated)");
    		        }

                }
            }
            catch (Exception e) {
            	s_oLogger.error("Launcher Main FINAL Exception " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e));
			}
            System.exit(-1);
        }
        finally {
        	LauncherMain.s_oSendToRabbit.Free();
		}

    }

    /**
     * Constructor
     */
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

            Path oPropFile = Paths.get(ConfigReader.getPropValue("SNAP_AUX_PROPERTIES"));
            Config.instance("snap.auxdata").load(oPropFile);
            Config.instance().load();

            SystemUtils.init3rdPartyLibs(null);
            Engine.start(false);

        }
        catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * Call the right method to execute sOperation with sParameter
     * @param sOperation Operation to be done
     * @param sParameter Parameter
     */
    public void ExecuteOperation(String sOperation, String sParameter) {

    	String sWorkspace = "";
    	String sExchange = "";
    	
    	if (sOperation.equals("MAGIC_UPDATE_BANDS")) {
    		System.out.println("MAGIC UPDATE BANDS CYCLE");
    		LauncherMain.UpdateBandViewModelsCycle();
    		return;
    	}
    	
    	try {
			BaseParameter oBaseParameter = (BaseParameter) SerializationUtils.deserializeXMLToObject(sParameter);
			sWorkspace = oBaseParameter.getWorkspace();
			sExchange = oBaseParameter.getExchange();
		} catch (Exception e) {
			String sError = "Impossible to deserialize Operation Parameters: operation aborted";
			if (s_oSendToRabbit!=null) s_oSendToRabbit.SendRabbitMessage(false, sOperation, sWorkspace,sError,sExchange);			
        	s_oLogger.error("ExecuteOperation Exception", e);
        	return;
		}
    	
        try {
        	//XXX refactor this switch
        	//suggestions:
        	// https://www.developer.com/java/data/seven-ways-to-refactor-java-switch-statements.html
        	// https://www.google.it/search?q=java+get+rid+of+switch
        	LauncherOperations op = LauncherOperations.valueOf(sOperation);
            switch (op)
            {
	            case INGEST: {
	                // Deserialize Parameters
	                IngestFileParameter oIngestFileParameter = (IngestFileParameter) SerializationUtils.deserializeXMLToObject(sParameter);
	                Ingest(oIngestFileParameter, ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH"));
	            }
	            break;
                case DOWNLOAD: {

                    // Deserialize Parameters
                    DownloadFileParameter oDownloadFileParameter = (DownloadFileParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    Download(oDownloadFileParameter, ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH"));
                }
                break;
                case FTPUPLOAD: {
                	s_oLogger.debug("ExecuteOperation: UPLOADVIAFT case: load parameters");
                	FtpUploadParameters oFtpTransferParameters = (FtpUploadParameters) SerializationUtils.deserializeXMLToObject(sParameter);
                	s_oLogger.debug("ExecuteOperation: UPLOADVIAFT case: try ftp transfer");
                	ftpTransfer(oFtpTransferParameters);
                	s_oLogger.debug("ExecuteOperation: UPLOADVIAFT case: done");
                }
                break;
                case PUBLISHBAND: {
                    // Deserialize Parameters
                    PublishBandParameter oPublishBandParameter = (PublishBandParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    PublishBandImage(oPublishBandParameter);
                }
                break;
                case APPLYORBIT:{
                    // Deserialize Parameters
                    ApplyOrbitParameter oParameter = (ApplyOrbitParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    ExecuteOperator(oParameter, new ApplyOrbit(), LauncherOperations.APPLYORBIT);
                }
                break;
                case CALIBRATE:{
                    // Deserialize Parameters
                    CalibratorParameter oParameter = (CalibratorParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    ExecuteOperator(oParameter, new Calibration(), LauncherOperations.CALIBRATE);
                }
                break;
                case MULTILOOKING:{
                    // Deserialize Parameters
                    MultilookingParameter oParameter = (MultilookingParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    ExecuteOperator(oParameter, new Multilooking(), LauncherOperations.MULTILOOKING);
                }
                break;
                case TERRAIN:{
                    // Deserialize Parameters
                    RangeDopplerGeocodingParameter oParameter = (RangeDopplerGeocodingParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    ExecuteOperator(oParameter, new TerrainCorrection(), LauncherOperations.TERRAIN);
                }
                break;
                case FILTER:{
                    // Deserialize Parameters
                    FilterParameter oParameter = (FilterParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    ExecuteOperator(oParameter, new Filter(), LauncherOperations.FILTER);
                }
                break;
                case NDVI:{
                    // Deserialize Parameters
                    NDVIParameter oParameter = (NDVIParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    ExecuteOperator(oParameter, new NDVI(), LauncherOperations.NDVI);
                }
                break;
                case RASTERGEOMETRICRESAMPLE:{
                    // Deserialize Parameters
                    RasterGeometricResampleParameter oParameter = (RasterGeometricResampleParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    RasterGeometricResample(oParameter);
                }
                break;
                case GRAPH: {
                	GraphParameter oParameter = (GraphParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                	ExecuteGraph(oParameter);                	
                }
                break;
                case DEPLOYPROCESSOR: {
                	DeployProcessorParameter oParameter = (DeployProcessorParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                	
                	WasdiProcessorEngine oEngine = WasdiProcessorEngine.GetProcessorEngine(oParameter.getProcessorType(), ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH"), ConfigReader.getPropValue("DOCKER_TEMPLATE_PATH"));
                	oEngine.DeployProcessor(oParameter);
                }
                break;
                case RUNPROCESSOR: {
                	DeployProcessorParameter oParameter = (DeployProcessorParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                	
                	WasdiProcessorEngine oEngine = WasdiProcessorEngine.GetProcessorEngine(oParameter.getProcessorType(), ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH"), ConfigReader.getPropValue("DOCKER_TEMPLATE_PATH"));
                	oEngine.run(oParameter);
                }
                break;
                default:
                    s_oLogger.debug("Operation Not Recognized. Nothing to do");
                    break;
            }
        }
        catch (Exception oEx) {
			String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);
			if (s_oSendToRabbit!=null) s_oSendToRabbit.SendRabbitMessage(false, sOperation, sWorkspace,sError,sExchange);			
        	s_oLogger.error("ExecuteOperation Exception", oEx);
        }
    }


	public void ExecuteGraph(GraphParameter params) throws Exception {
		try {
			WasdiGraph graphManager = new WasdiGraph(params, s_oSendToRabbit);
			graphManager.execute();			
		}
		catch (Exception oEx) {
			s_oLogger.error("ExecuteGraph Exception", oEx);
			String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);
			
			// P.Campanella 2018/03/30: handle exception and close the process
			ProcessWorkspaceRepository oRepo = new ProcessWorkspaceRepository();
			ProcessWorkspace oProcessWorkspace = oRepo.GetProcessByProcessObjId(params.getProcessObjId());
			updateProcessStatus(oRepo, oProcessWorkspace, ProcessStatus.ERROR, 100);
			
			if (s_oSendToRabbit!=null) s_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.GRAPH.name(), params.getWorkspace(),sError,params.getExchange());			
		}
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
        DownloadFile oDownloadFile = DownloadFile.getDownloadFile(oParameter.getProvider());
        
        ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
        ProcessWorkspace oProcessWorkspace = oProcessWorkspaceRepository.GetProcessByProcessObjId(oParameter.getProcessObjId());

        try {
        	updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);
        	
            s_oLogger.debug("LauncherMain.Download: Download Start");
            
            if (oDownloadFile == null) throw new Exception("Donwload File is null. Check the provider name");

            oDownloadFile.setProviderUser(oParameter.getDownloadUser());
            oDownloadFile.setProviderPassword(oParameter.getDownloadPassword());
            
            if (oProcessWorkspace != null) {
                //get file size
                long lFileSizeByte = oDownloadFile.GetDownloadFileSize(oParameter.getUrl());
                //set file size
                SetFileSizeToProcess(lFileSizeByte, oProcessWorkspace);
                
                //get process pid
                oProcessWorkspace.setPid(GetProcessId());
                
                updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 2);
                
            } else {
            	s_oLogger.debug("LauncherMain.Download: process not found: " + oParameter.getProcessObjId());
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

                updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 10);
                
                if (oAlreadyDownloaded == null) {
                    s_oLogger.debug("LauncherMain.Download: File not already downloaded. Download it");
                    
                    String sProcessFileName = sFileNameWithoutPath;
                    
                    if (!Utils.isNullOrEmpty(sProcessFileName)) {
                        oProcessWorkspace.setProductName(sProcessFileName);
                        //update the process
                        if (!oProcessWorkspaceRepository.UpdateProcess(oProcessWorkspace))
                            s_oLogger.debug("LauncherMain.Download: Error during process update with file name");

                        //send update process message
                        if (s_oSendToRabbit!=null && !s_oSendToRabbit.SendUpdateProcessMessage(oProcessWorkspace)) {
                            s_oLogger.debug("LauncherMain.Download: Error sending rabbitmq message to update process list");
                        }
                    }
                    

                    // No: it isn't: download it
                    sFileName = oDownloadFile.ExecuteDownloadFile(oParameter.getUrl(), oParameter.getDownloadUser(), oParameter.getDownloadPassword(), sDownloadPath, oProcessWorkspace);
                    
                    if (Utils.isNullOrEmpty(sFileName)) {
                    	int iLastError = oDownloadFile.getLastServerError();
                    	String sError = "There was an error contacting the provider";
                    	if (iLastError>0) sError+=": query obtained HTTP Erro Code " + iLastError;
                    	throw new Exception(sError);
                    }

                    // Get The product view Model
                    ReadProduct oReadProduct = new ReadProduct();
                    File oProductFile = new File(sFileName);
                    Product oProduct = oReadProduct.ReadProduct(oProductFile, null);
                    oVM = oReadProduct.getProductViewModel(oProduct, oProductFile);
	                // Save Metadata
	                oVM.setMetadataFileReference(AsynchSaveMetadata(oReadProduct,oProductFile));


                    // Save it in the register
                    oAlreadyDownloaded = new DownloadedFile();
                    oAlreadyDownloaded.setFileName(sFileNameWithoutPath);
                    oAlreadyDownloaded.setFilePath(sFileName);
                    oAlreadyDownloaded.setProductViewModel(oVM);
                    oAlreadyDownloaded.setBoundingBox(oParameter.getBoundingBox());
                    oAlreadyDownloaded.setRefDate(oProduct.getStartTime().getAsDate());
                    oAlreadyDownloaded.setCategory(DownloadedFileCategory.DOWNLOAD.name());
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
                
                updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 80);
            }
            else {
                s_oLogger.debug("LauncherMain.Download: Debug Option Active: file not really downloaded, using configured one");

                sFileName = sDownloadPath + File.separator + ConfigReader.getPropValue("DOWNLOAD_FAKE_FILE");

            }

            if (Utils.isNullOrEmpty(sFileName)) {
                s_oLogger.debug("LauncherMain.Download: file is null there must be an error");
                
                String sError = "The name of the file to download result null";
                if (s_oSendToRabbit!=null) s_oSendToRabbit.SendRabbitMessage(false,LauncherOperations.DOWNLOAD.name(),oParameter.getWorkspace(),sError,oParameter.getExchange());
                if (oProcessWorkspace != null) oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
            } else {
                AddProductToDbAndSendToRabbit(oVM, sFileName, oParameter.getWorkspace(), oParameter.getExchange(), LauncherOperations.DOWNLOAD.name(), oParameter.getBoundingBox());
                if (oProcessWorkspace != null) oProcessWorkspace.setStatus(ProcessStatus.DONE.name());                
            }
        }
        catch (Exception oEx) {
        	oEx.printStackTrace();
            s_oLogger.error("LauncherMain.Download: Exception " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
            
            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);
            
            if (oProcessWorkspace != null) oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
            if (s_oSendToRabbit!=null) s_oSendToRabbit.SendRabbitMessage(false,LauncherOperations.DOWNLOAD.name(),oParameter.getWorkspace(),sError,oParameter.getExchange());
        }
        finally{
            //update process status and send rabbit updateProcess message
            CloseProcessWorkspace(oProcessWorkspaceRepository, oProcessWorkspace);
        }

        return  sFileName;
    }
    
    
    //XXX sprout class
    //XXX notify client via rabbit of the transfer status
	public Boolean ftpTransfer(FtpUploadParameters oParam) throws IOException {
		s_oLogger.debug("ftpTransfer begin");
		if(null == oParam) {
			s_oLogger.debug("ftpTransfer: null input");
			return false;
		}
		if(null == oParam.getProcessObjId()) {
			s_oLogger.debug("ftpTransfer: null ProcessObjId");
			return false;
		}
		ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
		ProcessWorkspace oProcessWorkspace = oProcessWorkspaceRepository.GetProcessByProcessObjId(oParam.getProcessObjId());

        if(null == oProcessWorkspace ) {
        	s_oLogger.debug("ftpTransfer: null Process Workspace");
        	return false;
        }
        updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);
		if(!Utils.isFilePathPlausible(oParam.getFullLocalPath())) {
			s_oLogger.debug("ftpTransfer: null local path");
			oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
			CloseProcessWorkspace(oProcessWorkspaceRepository, oProcessWorkspace);
			return false;
		}
		updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 1);
			
		DownloadedFilesRepository oDownRepo = new DownloadedFilesRepository();
		String sFullLocalPath = oDownRepo.GetDownloadedFile(oParam.getM_sLocalFileName()).getFilePath();

		//String fullLocalPath = oParam.getM_sLocalPath();
		File oFile = new File(sFullLocalPath);
		if(!oFile.exists()) {
			s_oLogger.debug("ftpTransfer: local file does not exist");
			oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
			CloseProcessWorkspace(oProcessWorkspaceRepository, oProcessWorkspace);
			return false;
		}
		updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 2);
		String sFtpServer = oParam.getFtpServer();
		//TODO move here checks on server name
		if(!(	Utils.isServerNamePlausible( sFtpServer ) &&
				Utils.isPortNumberPlausible(oParam.getM_iPort()) &&
				!Utils.isNullOrEmpty(oParam.getM_sUsername()) &&
				//actually password might be empty
				(null != oParam.getM_sPassword())
				)){
			s_oLogger.debug("ftpTransfer: invalid FTP parameters");
			oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
			CloseProcessWorkspace(oProcessWorkspaceRepository, oProcessWorkspace);
			return false;
		}
		updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 3);

		FtpClient oFtpClient = new FtpClient(oParam.getFtpServer(),
				oParam.getM_iPort(),
				oParam.getM_sUsername(),
				oParam.getM_sPassword() );
		
		if(!oFtpClient.open() ) {
			s_oLogger.debug("ftpTransfer: could not connect to FTP server with these credentials:");
			s_oLogger.debug("server: "+oParam.getFtpServer());
			s_oLogger.debug("por: "+oParam.getM_iPort());
			s_oLogger.debug("username: "+oParam.getM_sUsername());
			s_oLogger.debug("password: " + oParam.getM_sPassword());
			oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
			CloseProcessWorkspace(oProcessWorkspaceRepository, oProcessWorkspace);
			return false;
		}
		updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 4);
		//XXX see how to modify FTP client to update status
		Boolean bPut = oFtpClient.putFileToPath(oFile, oParam.getM_sRemotePath() );
		if(!bPut) {
			s_oLogger.debug("ftpTransfer: put failed");
			oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
			CloseProcessWorkspace(oProcessWorkspaceRepository, oProcessWorkspace);
			return false;
		}
		updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 95);
		//String sRemotePath = oFtpTransferParameters.getM_sRemotePath();
		String sRemotePath = ".";
		Boolean bCheck = oFtpClient.FileIsNowOnServer(sRemotePath, oFile.getName()); 
		if(!bCheck) {
			s_oLogger.debug("ftpTransfer: could not find file on server");
			oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
			CloseProcessWorkspace(oProcessWorkspaceRepository, oProcessWorkspace);
			return false;
		}
		oFtpClient.close();
		updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);
		CloseProcessWorkspace(oProcessWorkspaceRepository, oProcessWorkspace);
		s_oLogger.debug("ftpTransfer completed successfully");
		return true;
	}
    
    public String SaveMetadata(ReadProduct oReadProduct, File oProductFile) {
    			
		// Write Metadata to file system
        try {
            // Get Metadata Path a Random File Name
            String sMetadataPath = ConfigReader.getPropValue("METADATA_PATH");
    		if (!sMetadataPath.endsWith("/")) sMetadataPath += "/";
    		String sMetadataFileName = Utils.GetRandomName();
    		
    		s_oLogger.debug("SaveMetadata: file = " + sMetadataFileName);

			SerializationUtils.serializeObjectToXML(sMetadataPath+sMetadataFileName, oReadProduct.getProductMetadataViewModel(oProductFile));
			
			s_oLogger.debug("SaveMetadata: file = saved");
			
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
    
    public String AsynchSaveMetadata(ReadProduct oReadProduct, File oProductFile) {
		
 		// Write Metadata to file system
        try {
        	
        	 
            // Get Metadata Path a Random File Name
            String sMetadataPath = ConfigReader.getPropValue("METADATA_PATH");
            if (!sMetadataPath.endsWith("/")) sMetadataPath += "/";
            String sMetadataFileName = Utils.GetRandomName();
     		
     		s_oLogger.debug("SaveMetadata: file = " + sMetadataFileName);
     		
     		SaveMetadataThread oThread = new SaveMetadataThread(sMetadataPath+sMetadataFileName, oReadProduct, oProductFile);
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


    
    public String Ingest(IngestFileParameter oParameter, String sDownloadPath) throws Exception {
        
    	File oFilePath = new File(oParameter.getFilePath());
    	if (!oFilePath.canRead()) {
			System.out.println("LauncherMain.Ingest: ERROR: unable to access uploaded file " + oFilePath.getAbsolutePath());
			throw new Exception("unable to access uploaded file " + oFilePath.getName());
		}

        ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
        ProcessWorkspace oProcessWorkspace = oProcessWorkspaceRepository.GetProcessByProcessObjId(oParameter.getProcessObjId());
	
		try {	
	        if (oProcessWorkspace != null) {
	            //get file size
	            long lFileSizeByte = oFilePath.length(); 
	            //set file size
	            SetFileSizeToProcess(lFileSizeByte, oProcessWorkspace);
	            
	            //get process pid
	            oProcessWorkspace.setPid(GetProcessId());
	            
	            updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);
	        } else {
	        	s_oLogger.debug("LauncherMain.Ingest: process not found: " + oParameter.getProcessObjId());
	        }
	
			File oRootPath = new File(sDownloadPath);
			File oDstDir = new File(new File(oRootPath, oParameter.getUserId()), oParameter.getWorkspace());
			
			if (!oDstDir.exists()) {
	            oDstDir.mkdirs();
			}
			
			if (!oDstDir.isDirectory() || !oDstDir.canWrite()) {
				System.out.println("LauncherMain.Ingest: ERROR: unable to access destination directory " + oDstDir.getAbsolutePath());
				throw new Exception("Unable to access destination directory for the Workspace");
			}
			
	        // Product view Model
	        ReadProduct oReadProduct = new ReadProduct();
	        ProductViewModel oVM = oReadProduct.getProductViewModel(oFilePath);
	        
	        updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 25);
	        
            // Save Metadata
            oVM.setMetadataFileReference(AsynchSaveMetadata(oReadProduct, oFilePath));
        
	        updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 50);	        
	        
			//copy file to workspace directory
	        if (!oFilePath.getParent().equals(oDstDir.getAbsolutePath())) {
	        	s_oLogger.debug("File in another folder make a copy");
	        	FileUtils.copyFileToDirectory(oFilePath, oDstDir);
	        }
	        else {
	        	s_oLogger.debug("File already in place");
	        }
			
			File oDstFile = new File(oDstDir, oFilePath.getName());
			
			updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 75);
			
			if (oVM.getName().equals("geotiff")) {
				oVM.setName(oVM.getFileName());
			}
            // Save it in the register
			DownloadedFilesRepository oDownloadedRepo = new DownloadedFilesRepository();
			DownloadedFile oAlreadyDownloaded = new DownloadedFile();
            oAlreadyDownloaded.setFileName(oFilePath.getName());
            oAlreadyDownloaded.setFilePath(oDstFile.getAbsolutePath());
            oAlreadyDownloaded.setProductViewModel(oVM);
            oAlreadyDownloaded.setRefDate(new Date());
            oAlreadyDownloaded.setCategory(DownloadedFileCategory.INGESTION.name());
            String oBB = oReadProduct.getProductBoundingBox(oFilePath);
			oAlreadyDownloaded.setBoundingBox(oBB);
            oDownloadedRepo.InsertDownloadedFile(oAlreadyDownloaded);            
            
            updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 90);
            
            //add product to db
            AddProductToDbAndSendToRabbit(oVM, oFilePath.getAbsolutePath(), oParameter.getWorkspace(), oParameter.getExchange(), LauncherOperations.INGEST.name(), oBB);
            
            if (oProcessWorkspace != null) {
            	oProcessWorkspace.setStatus(ProcessStatus.DONE.name());
            }
			
			return oDstFile.getAbsolutePath();
			
		} catch (Exception e) {
			System.out.println("LauncherMain.Ingest: ERROR: Exception occurrend during file ingestion");
			e.printStackTrace();
			                        
            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(e);
    
            if (oProcessWorkspace != null) oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
            if (s_oSendToRabbit!=null) s_oSendToRabbit.SendRabbitMessage(false,LauncherOperations.INGEST.name(),oParameter.getWorkspace(),sError,oParameter.getExchange());

		} finally{
            //update process status and send rabbit updateProcess message
            CloseProcessWorkspace(oProcessWorkspaceRepository, oProcessWorkspace);
        }

		
        return "";
            
    }

	public static void updateProcessStatus(ProcessWorkspaceRepository oProcessWorkspaceRepository,
			ProcessWorkspace oProcessWorkspace,
			ProcessStatus status,
			int progressPerc
			) throws JsonProcessingException {
		
		if (oProcessWorkspace == null) {
			s_oLogger.error("LauncherMain.updateProcessStatus oProcessWorkspace is null");
			return;
		}
		if (oProcessWorkspaceRepository == null) {
			s_oLogger.error("LauncherMain.updateProcessStatus oProcessWorkspace is null");
			return;
		}
		
		oProcessWorkspace.setStatus(status.name());
		oProcessWorkspace.setProgressPerc(progressPerc);
		//update the process
		if (!oProcessWorkspaceRepository.UpdateProcess(oProcessWorkspace)) {
			s_oLogger.debug("Error during process update");
		}	                
		//send update process message
		if(null == s_oSendToRabbit) {
			 s_oSendToRabbit = new Send();
		}
		if (!s_oSendToRabbit.SendUpdateProcessMessage(oProcessWorkspace)) {
		    s_oLogger.debug("Error sending rabbitmq message to update process list");
		}
	}
    
    
    /**
     * Generic Execute Operation Method
     * @param oParameter
     * @return
     */
    public void ExecuteOperator(OperatorParameter oParameter, BaseOperation oOperation, LauncherOperations operation) {

        s_oLogger.debug("LauncherMain.ExecuteOperation: Start operation " + operation);

        ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
        ProcessWorkspace oProcessWorkspace = oProcessWorkspaceRepository.GetProcessByProcessObjId(oParameter.getProcessObjId());
        
        s_oLogger.debug("LauncherMain.ExecuteOperation: Process found: " + oParameter.getProcessObjId() + " == " + oProcessWorkspace.getProcessObjId());
        
        try {
            if (oProcessWorkspace != null) {
            	
                //get process pid
                oProcessWorkspace.setPid(GetProcessId());
                oProcessWorkspace.setStatus(ProcessStatus.RUNNING.name());
                oProcessWorkspace.setProgressPerc(0);
                //update the process
                if (!oProcessWorkspaceRepository.UpdateProcess(oProcessWorkspace)) {
                	s_oLogger.debug("LauncherMain.ExecuteOperation: Error during process update (starting)");
                } else {
                	s_oLogger.debug("LauncherMain.ExecuteOperation: Updated process  " + oProcessWorkspace.getProcessObjId());
                }
            }        	
        	
        	
            //send update process message
            if (s_oSendToRabbit!=null && !s_oSendToRabbit.SendUpdateProcessMessage(oProcessWorkspace)) {
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
                
                String sError = "The name of input file for the operation is null";
                if (s_oSendToRabbit!=null) s_oSendToRabbit.SendRabbitMessage(false,operation.name(),oParameter.getWorkspace(),sError,oParameter.getExchange());

                return;
            }

            File oSourceFile = new File(sFile);
            
            //set file size            
            SetFileSizeToProcess(oSourceFile, oProcessWorkspace);
            
            WriteProduct oWriter = new WriteProduct(oProcessWorkspaceRepository, oProcessWorkspace);

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
            DownloadedFile oAlreadyDownloaded = oDownloadedRepo.GetDownloadedFile(oParameter.getSourceProductName()) ;
            String sBB = "";
            
            if (oAlreadyDownloaded != null) {
            	sBB = oAlreadyDownloaded.getBoundingBox();
            }
            
            AddProductToDbAndSendToRabbit(null, sTargetAbsFileName, oParameter.getWorkspace(), oParameter.getExchange(), operation.name(), sBB);

            //this.PublishOnGeoserver(oParameter.getPublishParameter(), oTerrainProduct.getName(), sBandName);
            if (oProcessWorkspace != null) oProcessWorkspace.setStatus(ProcessStatus.DONE.name());

        }
        catch (Throwable oEx) {
            s_oLogger.error("LauncherMain.ExecuteOperation: exception " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
            String sErrorMessage = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);
            
            if (s_oSendToRabbit!=null) s_oSendToRabbit.SendRabbitMessage(false,operation.name(),oParameter.getWorkspace(),sErrorMessage,oParameter.getExchange());
            if (oProcessWorkspace != null) oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
        }
        finally{
            s_oLogger.debug("LauncherMain.ExecuteOperation: End");

            CloseProcessWorkspace(oProcessWorkspaceRepository, oProcessWorkspace);
            
        }
    }


    /**
     * Publish single band image
     * @param oParameter
     * @return
     */
    public String PublishBandImage(PublishBandParameter oParameter) {

        String sLayerId = "";
        ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
        ProcessWorkspace oProcessWorkspace = oProcessWorkspaceRepository.GetProcessByProcessObjId(oParameter.getProcessObjId());
                
        try {

            if (oProcessWorkspace != null) {
                //get process pid
                oProcessWorkspace.setPid(GetProcessId());
                updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);
            }

            // Read File Name
            String sFile = oParameter.getFileName();
            
            DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
            DownloadedFile oDownloadedFile = oDownloadedFilesRepository.GetDownloadedFile(sFile);

            // Keep the product name
            String sProductName = oDownloadedFile.getProductViewModel().getName();
            
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
                String sError = "Input File path is null";
                
                // Send KO to Rabbit
                if (s_oSendToRabbit!=null) s_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.PUBLISHBAND.name(),oParameter.getWorkspace(),sError,oParameter.getExchange());

                return  sLayerId;
            }

            s_oLogger.debug( "LauncherMain.PublishBandImage:  File = " + sFile);

            // Create file
            File oFile = new File(sFile);
            String sInputFileNameOnly = oFile.getName();
            
            //set file size
            SetFileSizeToProcess(oFile, oProcessWorkspace);

            // Generate Layer Id
            sLayerId = sInputFileNameOnly;
            sLayerId = Utils.GetFileNameWithoutExtension(sFile);
            sLayerId +=  "_" + oParameter.getBandName();

            // Is already published?
            PublishedBandsRepository oPublishedBandsRepository = new PublishedBandsRepository();            
            PublishedBand oAlreadyPublished = oPublishedBandsRepository.GetPublishedBand(sProductName,oParameter.getBandName());


            updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 10);
            
            if (oAlreadyPublished != null) {
                // Yes !!
                s_oLogger.debug( "LauncherMain.PublishBandImage:  Band already published. Return result" );

                // Generate the View Model
                PublishBandResultViewModel oVM = new PublishBandResultViewModel();
                oVM.setBandName(oParameter.getBandName());
                oVM.setProductName(sProductName);
                oVM.setLayerId(sLayerId);

                boolean bRet = s_oSendToRabbit!=null && s_oSendToRabbit.SendRabbitMessage(true,LauncherOperations.PUBLISHBAND.name(),oParameter.getWorkspace(),oVM,oParameter.getExchange());

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
            Product oProduct = oReadProduct.ReadProduct(oFile, null);            
            String sEPSG = CRS.lookupIdentifier(oProduct.getSceneCRS(),true);
            
            updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 20);
            
            // P.Campanella 13/02/2018: write the data directly to GeoServer Data Dir
            String sGeoServerDataDir = ConfigReader.getPropValue("GEOSERVER_DATADIR");
            String sTargetDir = sGeoServerDataDir;

            if (!(sTargetDir.endsWith("/")||sTargetDir.endsWith("\\"))) sTargetDir+="/";
            sTargetDir+=sLayerId+"/";

            File oTargetDir = new File(sTargetDir);
            if (!oTargetDir.exists()) oTargetDir.mkdirs();
            
            String sOutputFilePath = sTargetDir + sLayerId + ".tif";

            File oOutputFile = new File(sOutputFilePath);

            s_oLogger.debug("LauncherMain.PublishBandImage: to " + sOutputFilePath + " [LayerId] = " + sLayerId);

            if (oProduct.getProductType().startsWith("S2") && oProduct.getProductReader().getClass().getName().startsWith("org.esa.s2tbx")) {
            	
            	s_oLogger.debug( "LauncherMain.PublishBandImage:  Managing S2 Product");
				s_oLogger.debug( "LauncherMain.PublishBandImage:  Getting Band " + oParameter.getBandName());
				
				Band oBand = oProduct.getBand(oParameter.getBandName());            
				Product oGeotiffProduct = new Product(oParameter.getBandName(), "GEOTIFF");
				oGeotiffProduct.addBand(oBand);                 
				sOutputFilePath = new WriteProduct(oProcessWorkspaceRepository, oProcessWorkspace).WriteGeoTiff(oGeotiffProduct, sTargetDir, sLayerId);
				oOutputFile = new File(sOutputFilePath);
				s_oLogger.debug( "LauncherMain.PublishBandImage:  Geotiff File Created (EPSG=" + sEPSG + "): " + sOutputFilePath);
			
            } else {
            	
            	s_oLogger.debug( "LauncherMain.PublishBandImage:  Managing NON S2 Product");
            	s_oLogger.debug( "LauncherMain.PublishBandImage:  Getting Band " + oParameter.getBandName());
            	
    			// Get the Geocoding and Band
    			GeoCoding oGeoCoding = oProduct.getSceneGeoCoding();;
    			if (oGeoCoding==null) throw new Exception("unable to obtain scene geocoding from product " + oProduct.getName());
    			Band oBand = oProduct.getBand(oParameter.getBandName());
    			
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
			
            updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 50);
            
            // Ok publish
            s_oLogger.debug("LauncherMain.PublishBandImage: call PublishImage");
            
            GeoServerManager manager = new GeoServerManager(ConfigReader.getPropValue("GEOSERVER_ADDRESS"),ConfigReader.getPropValue("GEOSERVER_USER"),ConfigReader.getPropValue("GEOSERVER_PASSWORD"));            
            
            Publisher oPublisher = new Publisher();
            
            try {
            	oPublisher.m_lMaxMbTiffPyramid = Long.parseLong(ConfigReader.getPropValue("MAX_GEOTIFF_DIMENSION_PYRAMID","50"));
            }
            catch (Exception e) {
            	s_oLogger.error("LauncherMain.PublishBandImage: wrong MAX_GEOTIFF_DIMENSION_PYRAMID, setting default to 50");
            	oPublisher.m_lMaxMbTiffPyramid = 50L;
			}
            
            s_oLogger.debug("Call publish geotiff sOutputFilePath = " + sOutputFilePath + " , sLayerId = " + sLayerId);
            sLayerId = oPublisher.publishGeoTiff(sOutputFilePath, sLayerId, sEPSG, sStyle, manager);
            
            s_oLogger.debug("Obtained sLayerId = " + sLayerId);
            
            updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 90);
            
            boolean bResultPublishBand = true;
            
            if (sLayerId == null) {
                bResultPublishBand = false;
                s_oLogger.debug("LauncherMain.PublishBandImage: Image not published . ");
            }
            else {
            	s_oLogger.debug("LauncherMain.PublishBandImage: Image published. ");
            }

            
            s_oLogger.debug("LauncherMain.PublishBandImage: Get Image Bounding Box");

            //get bounding box from data base
            String sBBox = oDownloadedFile.getBoundingBox();

            String sGeoserverBBox = manager.getLayerBBox(sLayerId);//GeoserverUtils.GetBoundingBox(sLayerId, "json");
            		

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

            boolean bRet = s_oSendToRabbit!=null && s_oSendToRabbit.SendRabbitMessage(bResultPublishBand,LauncherOperations.PUBLISHBAND.name(), oParameter.getWorkspace(),oVM,oParameter.getExchange());

            if (bRet == false) {
                s_oLogger.debug("LauncherMain.PublishBandImage: Error sending Rabbit Message");
            }

            if (oProcessWorkspace != null) oProcessWorkspace.setStatus(ProcessStatus.DONE.name());
        }
        catch (Exception oEx) {
            s_oLogger.error( "LauncherMain.PublishBandImage: Exception " + oEx.toString() + " " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
            oEx.printStackTrace();
            
            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);

            boolean bRet = s_oSendToRabbit!=null && s_oSendToRabbit.SendRabbitMessage(false,LauncherOperations.PUBLISHBAND.name(),oParameter.getWorkspace(),sError,oParameter.getExchange());

            if (bRet == false) {
                s_oLogger.error("LauncherMain.PublishBandImage:  Error sending exception Rabbit Message");
            }

            if (oProcessWorkspace != null) oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
        }
        finally{
            CloseProcessWorkspace(oProcessWorkspaceRepository, oProcessWorkspace);
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
        ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
        ProcessWorkspace oProcessWorkspace = oProcessWorkspaceRepository.GetProcessByProcessObjId(oParameter.getProcessObjId());

        try {

        	if (oProcessWorkspace != null) {
                //get process pid
                oProcessWorkspace.setPid(GetProcessId());
                oProcessWorkspace.setStatus(ProcessStatus.RUNNING.name());
                oProcessWorkspace.setProgressPerc(0);
                //update the process
                oProcessWorkspaceRepository.UpdateProcess(oProcessWorkspace);
                //send update process message
                if (s_oSendToRabbit!=null && !s_oSendToRabbit.SendUpdateProcessMessage(oProcessWorkspace)) {
                    s_oLogger.debug("LauncherMain.RasterGeometricResample: Error sending rabbitmq message to update process list");
                }
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
            WriteProduct oWriter = new WriteProduct(oProcessWorkspaceRepository, oProcessWorkspace);

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

            AddProductToDbAndSendToRabbit(null, sOutFile, oParameter.getWorkspace(), oParameter.getExchange(), LauncherOperations.RASTERGEOMETRICRESAMPLE.name(), null);
            if (oProcessWorkspace != null) oProcessWorkspace.setStatus(ProcessStatus.DONE.name());

        }
        catch (Exception oEx) {
            s_oLogger.error("LauncherMain.RasterGeometricResample: exception " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
            if (oProcessWorkspace != null) oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
            
            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);
            if (s_oSendToRabbit!=null) s_oSendToRabbit.SendRabbitMessage(false,LauncherOperations.RASTERGEOMETRICRESAMPLE.name(),oParameter.getWorkspace(),sError,oParameter.getExchange());
        	
        }
        finally{
            s_oLogger.debug("LauncherMain.RasterGeometricResample: End");
            CloseProcessWorkspace(oProcessWorkspaceRepository, oProcessWorkspace);
        }
    }
    
    /**
     * Adds a product to a Workspace. If it is alredy added it will not be duplicated.
     * @param sProductName Product to Add
     * @param sWorkspaceId Workspace Id
     * @return True if the product is already or have been added to the WS. False otherwise
     */
    public boolean AddProductToWorkspace(String sProductName, String sWorkspaceId) {
    	
    	try {
    		
    		// Create Repository
    		ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
    		
    		// Check if product is already in the Workspace
    		if (oProductWorkspaceRepository.ExistsProductWorkspace(sProductName, sWorkspaceId) == false) {
    			
        		// Create the entity
        		ProductWorkspace oProductWorkspace = new ProductWorkspace();
        		oProductWorkspace.setProductName(sProductName);
        		oProductWorkspace.setWorkspaceId(sWorkspaceId);
        		
        		// Try to insert
        		if (oProductWorkspaceRepository.InsertProductWorkspace(oProductWorkspace)) {
        			
        			s_oLogger.debug("LauncherMain.AddProductToWorkspace:  Inserted [" +sProductName + "] in WS: [" + sWorkspaceId+ "]");
        			return true;
        		}
        		else {
        			
        			s_oLogger.debug("LauncherMain.AddProductToWorkspace:  Error adding ["  +sProductName + "] in WS: [" + sWorkspaceId+ "]");
        			return false;
        		}
    		}
    		else {
    			s_oLogger.debug("LauncherMain.AddProductToWorkspace: Product [" +sProductName + "] Already exists in WS: [" + sWorkspaceId+ "]");
    			return true;
    		}
    	}
    	catch (Exception e) {
    		s_oLogger.error("LauncherMain.AddProductToWorkspace: Exception " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e) );
		}
    	
    	return false;
    }

    /**
     * Close the Process on the mongo Db. Set progress to 100 and end date time 
     * @param oProcessWorkspaceRepository Repository 
     * @param oProcessWorkspace Process to close
     */
	private void CloseProcessWorkspace(ProcessWorkspaceRepository oProcessWorkspaceRepository, ProcessWorkspace oProcessWorkspace) {
		try{
			if (oProcessWorkspace != null) {
		        //update the process
				oProcessWorkspace.setProgressPerc(100);
				oProcessWorkspace.setOperationEndDate(Utils.GetFormatDate(new Date()));
		        if (!oProcessWorkspaceRepository.UpdateProcess(oProcessWorkspace)) {
		        	s_oLogger.debug("LauncherMain: Error during process update (terminated)");
		        }
		        //send update process message
				if (s_oSendToRabbit!=null && !s_oSendToRabbit.SendUpdateProcessMessage(oProcessWorkspace)) {
				    s_oLogger.debug("LauncherMain: Error sending rabbitmq message to update process list");
				}
			}
		} catch (Exception oEx) {
		    s_oLogger.debug("LauncherMain: Exception deleting process " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
		}
	}
	
    /**
     * Set the file size to the process Object
     * @param oFile File to read the size from
     * @param oProcessWorkspace Process to update
     */
    public static void SetFileSizeToProcess(File oFile, ProcessWorkspace oProcessWorkspace) {
    	
    	if (oFile== null) {
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
    		}
    		catch (Exception oEx) {
    			s_oLogger.error("LauncherMain.SetFileSizeToProcess: Error computing folder size");
    			oEx.printStackTrace();
			}
    	}
    	
    	SetFileSizeToProcess(lSize, oProcessWorkspace);
    }

    /**
     * Set the file size to the process Object
     * @param lSize Size
     * @param oProcessWorkspace Process to update
     */
    public static void SetFileSizeToProcess(Long lSize, ProcessWorkspace oProcessWorkspace) {
    	
    	if (oProcessWorkspace == null) {
    		s_oLogger.error("LauncherMain.SetFileSizeToProcess: input process is null");
    		return;
    	}
    	
        s_oLogger.debug("LauncherMain.SetFileSizeToProcess: File size  = " + Utils.GetFormatFileDimension(lSize));
        oProcessWorkspace.setFileSize(Utils.GetFormatFileDimension(lSize));
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
    private void AddProductToDbAndSendToRabbit(ProductViewModel oVM, String sFileName, String sWorkspace, String sExchange, String sOperation, String sBBox) throws Exception
    {
        s_oLogger.debug("LauncherMain.AddProductToDbAndSendToRabbit: File Name = " + sFileName);

        // Get The product view Model            
        if (oVM == null) {
            ReadProduct oReadProduct = new ReadProduct();
            s_oLogger.debug("LauncherMain.AddProductToDbAndSendToRabbit: call read product");
            File oProductFile = new File(sFileName);
            oVM = oReadProduct.getProductViewModel(oProductFile);
            oVM.setMetadataFileReference(SaveMetadata(oReadProduct, oProductFile));
            
            if (oVM.getBandsGroups() == null) {
            	s_oLogger.debug("LauncherMain.AddProductToDbAndSendToRabbit: Band Groups is NULL");
            } else if (oVM.getBandsGroups().getBands() == null) {
            	s_oLogger.debug("LauncherMain.AddProductToDbAndSendToRabbit: bands is NULL");
            } else {
                s_oLogger.debug("LauncherMain.AddProductToDbAndSendToRabbit: bands " + oVM.getBandsGroups().getBands().size());
            }

            s_oLogger.debug("LauncherMain.AddProductToDbAndSendToRabbit: done read product");
        }
        
        
        // P.Campanella 12/05/2017: it looks it is done before. Let leave here a check
        DownloadedFilesRepository oDownloadedRepo = new DownloadedFilesRepository();
        
        DownloadedFile oCheck = oDownloadedRepo.GetDownloadedFile(oVM.getFileName());
        File oFile = new File(sFileName);
        
        boolean bAddProductToWS = true;
        
        if (oCheck == null) {
        	s_oLogger.debug("AddProductToDbAndSendToRabbit: Insert in db");
        	
            // Save it in the register
            DownloadedFile oAlreadyDownloaded = new DownloadedFile();
            
            oAlreadyDownloaded.setFileName(oFile.getName());
            oAlreadyDownloaded.setFilePath(sFileName);
            oAlreadyDownloaded.setProductViewModel(oVM);
            oAlreadyDownloaded.setBoundingBox(sBBox);
            oAlreadyDownloaded.setRefDate(new Date());
            oAlreadyDownloaded.setCategory(DownloadedFileCategory.COMPUTED.name());
            
            if (!oDownloadedRepo.InsertDownloadedFile(oAlreadyDownloaded)) {
            	s_oLogger.error("Impossible to Insert the new Product " + oFile.getName() + " in the database.");
            }
            else {
            	s_oLogger.info("Product Inserted");
            }
        }
        
        if (bAddProductToWS) {
        	AddProductToWorkspace(oFile.getName(), sWorkspace);
        }
        else {
        	s_oLogger.error("Product NOT added to the Workspace");
        }
        
        s_oLogger.debug("OK DONE");

        s_oLogger.debug("LauncherMain.AddProductToDbAndSendToRabbit: Image added. Send Rabbit Message Exchange = " + sExchange);

        if (s_oSendToRabbit!=null) s_oSendToRabbit.SendRabbitMessage(true,sOperation,sWorkspace,oVM,sExchange);
    }
    
    /**
     * Get the id of the process
     * @return
     */
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
            s_oLogger.error("LauncherMain.GetProcessId: Error getting processId: " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
        }

        return iPid;
    }
    
	public static void UpdateBandViewModelsCycle() {
		// Create repo
		ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
		DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
		
		List<DownloadedFile> aoDownloaded = oDownloadedFilesRepository.getList();
		
		// For all the downloaded files
		for (int i=0; i<aoDownloaded.size(); i++) {
			
			DownloadedFile oDownloadedFileEntity = aoDownloaded.get(i);
			
			System.out.println("Product [" + i + "] - " + oDownloadedFileEntity.getFilePath());
			
			if (oDownloadedFileEntity.getFileName()!= null) {
				if (oDownloadedFileEntity.getFileName().startsWith("SMCItaly")) {
					System.out.println("MIDA Public Product, jump");
					continue;
				}
			}
			
			
			// Read The Product
            ReadProduct oReadSNAPProduct = new ReadProduct();
            File oProductFile = new File(oDownloadedFileEntity.getFilePath());
            Product oProduct = oReadSNAPProduct.ReadProduct(oProductFile, null);
            
            if (Utils.isNullOrEmpty(oDownloadedFileEntity.getFileName())) {
            	System.out.println("Fixing DownloadedFile - FileName");
            	oDownloadedFileEntity.setFileName(oProductFile.getName());
            	oDownloadedFilesRepository.UpdateDownloadedFile(oDownloadedFileEntity);
            }
            
            // There was any problem?
            if (oProduct == null) {
            	
            	// Product NULL
            	
            	// Check if the file exists
            	if (oProductFile.exists()==false) {
            		
            		System.out.println("File does not Exists: delete it");
            		oProductWorkspaceRepository.DeleteByProductName(oDownloadedFileEntity.getFileName());
            		oDownloadedFilesRepository.DeleteByFilePath(oDownloadedFileEntity.getFilePath());
            		System.out.println("File does not Exists: deleted");
            	}
            	else {            		
            		
            		// Clear Metadata anyway
            		System.out.println("File Exists but could not be read");
            		
            		if (oDownloadedFileEntity.getProductViewModel() != null && oDownloadedFileEntity.getProductViewModel().getMetadata() != null) {
            			oDownloadedFileEntity.getProductViewModel().setMetadata(null);
                        oDownloadedFilesRepository.UpdateDownloadedFile(oDownloadedFileEntity);
                        System.out.println("Cleared Metadata");
            		}
            		
            	}
            	continue;
            }
            
            try {
                ProductViewModel oVM = oReadSNAPProduct.getProductViewModel(oProduct, oProductFile);
                
                if (oVM == null) {
                	System.out.println("WARNING - Product View Model NULL FOR " +  oDownloadedFileEntity.getFilePath());
                	continue;
                }
                
                //oVM.setMetadata(oReadSNAPProduct.getProductMetadataViewModel(oProductFile));
                MetadataViewModel oMetadataViewModel = oReadSNAPProduct.getProductMetadataViewModel(oProductFile);
                
                if (oDownloadedFileEntity.getProductViewModel().getMetadataFileReference() == null || oDownloadedFileEntity.getProductViewModel().getMetadataFileReference().isEmpty()) {
                    System.out.println("Serialize Metadata");
                    String sMetadataFile = Utils.GetRandomName();
                    SerializationUtils.serializeObjectToXML("/data/wasdi/metadata/"+sMetadataFile, oMetadataViewModel);
                    oDownloadedFileEntity.getProductViewModel().setBandsGroups(oVM.getBandsGroups());
                    oDownloadedFileEntity.getProductViewModel().setMetadataFileReference(sMetadataFile);                		                	
                }
                else {
                	System.out.println("Metadata Already Serialized");
                }
                
                System.out.println("Check Band Dimensions. Bands count = " + oDownloadedFileEntity.getProductViewModel().getBandsGroups().getBands().size());
                
                for (int iBands = 0; iBands < oDownloadedFileEntity.getProductViewModel().getBandsGroups().getBands().size(); iBands++) {
                	BandViewModel oBandVM = oDownloadedFileEntity.getProductViewModel().getBandsGroups().getBands().get(iBands);
                	if (oBandVM.getWidth() == 0 || oBandVM.getHeight() == 0) {
                		System.out.println("PRODUCT " +oDownloadedFileEntity.getProductViewModel().getName() + " BAND " + oBandVM.getName() + " DIMENSION PROBLEMS [W, H] = [" + oBandVM.getWidth() + ", " + oBandVM.getHeight()+ "]");
                	}
                }
                
                oDownloadedFileEntity.getProductViewModel().setMetadata(null);
                oDownloadedFilesRepository.UpdateDownloadedFile(oDownloadedFileEntity);
                
                //MetadataViewModel oReloaded = (MetadataViewModel) SerializationUtils.deserializeXMLToObject("C:\\Temp\\wasdi\\metadata\\"+oDownloadedFileEntity.getProductViewModel().getMetadataFileReference());
            }
            catch (Exception oEx) {
            	System.out.println("Exception " + oEx);
            }
		}
		
		
		System.out.println("Clearing Product Workspace Table..");
		
		List<ProductWorkspace> aoProductsWSs = oProductWorkspaceRepository.getList();
		
		for (int i=0; i<aoProductsWSs.size(); i++) {
			ProductWorkspace oPW = aoProductsWSs.get(i);
			
			DownloadedFile oDF = oDownloadedFilesRepository.GetDownloadedFile(oPW.getProductName());
			
			if (oDF == null) {
				System.out.println("\nINVALID Product : " + oPW.getProductName() + " WS : " + oPW.getWorkspaceId());
				oProductWorkspaceRepository.DeleteByProductNameWorkspace(oPW.getProductName(), oPW.getWorkspaceId());
				System.out.println("DELETED");
			}
			else {
				System.out.print(".");
			}
		}


		System.out.println("\nBye Bye");
	}
}
