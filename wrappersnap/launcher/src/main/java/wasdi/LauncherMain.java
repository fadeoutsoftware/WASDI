package wasdi;
import com.bc.ceres.glevel.MultiLevelImage;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.geotiff.GeoCoding2GeoTIFFMetadata;
import org.esa.snap.core.util.geotiff.GeoTIFF;
import org.esa.snap.core.util.geotiff.GeoTIFFMetadata;
import wasdi.filebuffer.DownloadFile;
import org.apache.commons.cli.*;
import wasdi.geoserver.Publisher;
import wasdi.rabbit.Send;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.PublishedBand;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.PublishedBandsRepository;
import wasdi.shared.parameters.DownloadFileParameter;
import wasdi.shared.parameters.PublishBandParameter;
import wasdi.shared.parameters.PublishParameters;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.ProductViewModel;
import wasdi.shared.viewmodels.PublishBandResultViewModel;
import wasdi.shared.viewmodels.RabbitMessageViewModel;
import wasdi.snapopearations.ReadProduct;

import java.io.File;
import java.io.IOException;


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


        Option oOptOperation   = OptionBuilder.withArgName( "operation" )
                .hasArg()
                .withDescription(  "" )
                .create( "operation" );

        Option oOptParameter   = OptionBuilder.withArgName( "parameter" )
                .hasArg()
                .withDescription(  "" )
                .create( "parameter" );


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

        } catch (IOException e) {
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
                case LauncherOperations.DOWNLOADANDPUBLISH: {

                    // Deserialize Parameters
                    DownloadFileParameter oDownloadFileParameter = (DownloadFileParameter) SerializationUtils.deserializeXMLToObject(sParameter);

                    String sFile = Download(oDownloadFileParameter, ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH"));

                    // Check result
                    if (Utils.isNullOrEmpty(sFile))
                    {
                        // File not downloaded
                        String sUrl = oDownloadFileParameter.getUrl();
                        String sText = "LauncherMain: file not downloaded URL = ";
                        if (sUrl ==null) sText += " NULL";
                        else sText += sUrl;
                        s_oLogger.debug( sText);
                    }
                    else {
                        // Ok file downloaded, let publish it

                        // Recreate the download parameter
                        PublishParameters oPublishParameter = new PublishParameters();
                        oPublishParameter.setFileName(sFile);
                        oPublishParameter.setUserId(oDownloadFileParameter.getUserId());
                        oPublishParameter.setWorkspace(oDownloadFileParameter.getWorkspace());
                        oPublishParameter.setQueue(oDownloadFileParameter.getQueue());

                        Publish(oPublishParameter);
                    }
                }
                break;
                case LauncherOperations.PUBLISH: {

                    // Deserialize Parameters
                    PublishParameters oPublishParameter = (PublishParameters) SerializationUtils.deserializeXMLToObject(sParameter);
                    Publish(oPublishParameter);
                }
                break;
                case LauncherOperations.PUBLISHBAND: {

                    // Deserialize Parameters
                    PublishBandParameter oPublishBandParameter = (PublishBandParameter) SerializationUtils.deserializeXMLToObject(sParameter);
                    PublishBandImage(oPublishBandParameter);
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

    public String Download(DownloadFileParameter oParameter, String sDownloadPath) {
        String sFileName = "";
        try {
            s_oLogger.debug("LauncherMain.Download: Download Start");

            if (!sDownloadPath.endsWith("/")) sDownloadPath+="/";

            // Generate the Path adding user id and workspace
            sDownloadPath += oParameter.getUserId()+"/"+oParameter.getWorkspace();

            s_oLogger.debug("LauncherMain.DownloadPath: " + sDownloadPath);

            // Product view Model
            ProductViewModel oVM = null;


            // Download file
            if (ConfigReader.getPropValue("DOWNLOAD_ACTIVE").equals("true")) {

                // Download handler
                DownloadFile oDownloadFile = new DownloadFile();

                // Get the file name
                String sFileNameWithoutPath = oDownloadFile.GetFileName(oParameter.getUrl());

                // Check if it is already downloaded
                DownloadedFilesRepository oDownloadedRepo = new DownloadedFilesRepository();
                DownloadedFile oAlreadyDownloaded = oDownloadedRepo.GetDownloadedFile(sFileNameWithoutPath);

                if (oAlreadyDownloaded == null) {
                    s_oLogger.debug("LauncherMain.Download: File not already downloaded. Download it");

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

                    oDownloadedRepo.InsertDownloadedFile(oAlreadyDownloaded);
                }
                else {
                    s_oLogger.debug("LauncherMain.Download: File already downloaded: make a copy");

                    // Yes!! Here we have the path
                    sFileName = oAlreadyDownloaded.getFilePath();

                    // Check the path where we want the file
                    String sDestinationFileWithPath = sDownloadPath+"/" + sFileNameWithoutPath;

                    // Is it different?
                    if (sDestinationFileWithPath.equals(sFileName) == false) {
                        // Yes, make a copy
                        FileUtils.copyFile(new File(sFileName), new File(sDestinationFileWithPath));
                        sFileName = sDestinationFileWithPath;
                    }
                }
            }
            else {
                s_oLogger.debug("LauncherMain.Download: Debug Option Active: file not really downloaded, using configured one");

                sFileName = sDownloadPath + File.separator + ConfigReader.getPropValue("DOWNLOAD_FAKE_FILE");

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

                DownloadedFilesRepository oDownloadedRepo = new DownloadedFilesRepository();
                oDownloadedRepo.InsertDownloadedFile(oAlreadyDownloaded);

                s_oLogger.debug("OK DONE");
            }

            // Rabbit Sender
            Send oSendToRabbit = new Send();

            if (Utils.isNullOrEmpty(sFileName)) {
                s_oLogger.debug("LauncherMain.Download: file is null there must be an error");

                RabbitMessageViewModel oMessageViewModel = new RabbitMessageViewModel();
                oMessageViewModel.setMessageCode(LauncherOperations.DOWNLOAD);
                oMessageViewModel.setMessageResult("KO");
                String sJSON = MongoRepository.s_oMapper.writeValueAsString(oMessageViewModel);

                oSendToRabbit.SendMsg(oParameter.getQueue(),sJSON);
            }
            else {
                s_oLogger.debug("LauncherMain.Download: Image downloaded. Send Rabbit Message");

                if (oVM!=null) {

                    s_oLogger.debug("LauncherMain.Download: Queue = " + oParameter.getQueue());

                    RabbitMessageViewModel oMessageViewModel = new RabbitMessageViewModel();
                    oMessageViewModel.setMessageCode(LauncherOperations.DOWNLOAD);
                    oMessageViewModel.setMessageResult("OK");
                    oMessageViewModel.setPayload(oVM);

                    String sJSON = MongoRepository.s_oMapper.writeValueAsString(oMessageViewModel);

                    if (oSendToRabbit.SendMsg(oParameter.getQueue(),sJSON)==false) {
                        s_oLogger.debug("LauncherMain.Download: Error sending Rabbit Message");
                    }
                }
                else {
                    s_oLogger.debug("LauncherMain.Download: Unable to read image. Send Rabbit Message");

                    RabbitMessageViewModel oMessageViewModel = new RabbitMessageViewModel();
                    oMessageViewModel.setMessageCode(LauncherOperations.DOWNLOAD);
                    oMessageViewModel.setMessageResult("KO");
                    String sJSON = MongoRepository.s_oMapper.writeValueAsString(oMessageViewModel);

                    oSendToRabbit.SendMsg(oParameter.getQueue(),sJSON);
                }

            }
        }
        catch (Exception oEx) {
            s_oLogger.debug("LauncherMain.Download: Exception " + oEx.toString());
            // Rabbit Sender
            Send oSendToRabbit = new Send();
            RabbitMessageViewModel oMessageViewModel = new RabbitMessageViewModel();
            oMessageViewModel.setMessageCode(LauncherOperations.DOWNLOAD);
            oMessageViewModel.setMessageResult("KO");

            try {
                String sJSON = MongoRepository.s_oMapper.writeValueAsString(oMessageViewModel);
                oSendToRabbit.SendMsg(oParameter.getQueue(),sJSON);
            }
            catch (Exception oEx2) {
                s_oLogger.debug("LauncherMain.Download: Inner Exception " + oEx2.toString());
            }
        }

        return  sFileName;
    }

    /**
     * Generic publish function. NOTE: probably will not be used, use publish band instead
     * @param oParameter
     * @return
     */
    public String Publish(PublishParameters oParameter) {
        String sLayerId = "";

        try {
            // Read File Name
            String sFile = oParameter.getFileName();

            String sPath = ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH");
            if (!sPath.endsWith("/")) sPath += "/";
            sPath += oParameter.getUserId() + "/" + oParameter.getWorkspace()+ "/";
            sFile = sPath + sFile;


            // Check integrity
            if (Utils.isNullOrEmpty(sFile))
            {
                s_oLogger.debug( "LauncherMain.Publish: file is null or empty");
                return  sLayerId;
            }

            // Create a file object for the downloaded file
            File oDownloadedFile = new File(sFile);
            String sInputFileNameOnly = oDownloadedFile.getName();
            sLayerId = sInputFileNameOnly;

            sLayerId = Utils.GetFileNameWithoutExtension(sFile);

            // Copy fie to GeoServer Data Dir
            String sGeoServerDataDir = ConfigReader.getPropValue("GEOSERVER_DATADIR");
            String sTargetDir = sGeoServerDataDir;

            if (!(sTargetDir.endsWith("/")||sTargetDir.endsWith("\\"))) sTargetDir+="/";
            sTargetDir+=sLayerId+"/";

            String sTargetFile = sTargetDir + sInputFileNameOnly;

            File oTargetFile = new File(sTargetFile);

            s_oLogger.debug("LauncherMain.publish: InputFile: " + sFile + " TargetFile: " + sTargetDir + " LayerId " + sLayerId);

            FileUtils.copyFile(oDownloadedFile,oTargetFile);

            //TODO: Here recognize the file type and run the right procedure. At the moment assume Sentinel1A

            s_oLogger.debug("LauncherMain.publish: Write Big Tiff");
            // Convert the product in a Tiff file
            ReadProduct oReadProduct = new ReadProduct();
            String sTiffFile = oReadProduct.writeBigTiff(oTargetFile.getAbsolutePath(), sTargetDir, sLayerId);
            //String sTiffFile = "C:\\Program Files (x86)\\GeoServer 2.9.2\\data_dir\\data\\S1A_IW_GRDH_1SSV_20150213T095824_20150213T095849_004603_005AB7_5539\\S1A_IW_GRDH_1SSV_20150213T095824_20150213T095849_004603_005AB7_5539.zip.tif";

            s_oLogger.debug("LauncherMain.publish: TiffFile: " +sTiffFile);

            // Check result
            if (Utils.isNullOrEmpty(sTiffFile))
            {
                s_oLogger.debug( "LauncherMain.Publish: Tiff File is null or empty");
                return sLayerId;
            }

            // Ok publish
            s_oLogger.debug("LauncherMain.publish: call PublishImage");
            Publisher oPublisher = new Publisher();
            sLayerId = oPublisher.publishGeoTiff(sTiffFile,ConfigReader.getPropValue("GEOSERVER_ADDRESS"),ConfigReader.getPropValue("GEOSERVER_USER"),ConfigReader.getPropValue("GEOSERVER_PASSWORD"),ConfigReader.getPropValue("GEOSERVER_WORKSPACE"), sLayerId);

            s_oLogger.debug("LauncherMain.publish: Image published. Send Rabbit Message");
            Send oSendLayerId = new Send();
            s_oLogger.debug("LauncherMain.publish: Queue = " + oParameter.getQueue() + " LayerId = " + sLayerId);

            RabbitMessageViewModel oMessageViewModel = new RabbitMessageViewModel();
            oMessageViewModel.setMessageCode(LauncherOperations.PUBLISH);
            oMessageViewModel.setMessageResult("OK");
            String sJSON = MongoRepository.s_oMapper.writeValueAsString(oMessageViewModel);

            if (oSendLayerId.SendMsg(oParameter.getQueue(),sJSON)==false) {
                s_oLogger.debug("LauncherMain.publish: Error sending Rabbit Message");
            }

            // Deletes the copy of the Zip file
            s_oLogger.debug("LauncherMain.publish: delete Zip File Copy " + oTargetFile.getPath());

            if (oTargetFile.delete()==false) {
                s_oLogger.debug("LauncherMain.publish: impossible to delete zip file");
            }
        }
        catch (Exception oEx) {
            s_oLogger.debug("LauncherMain.Publish: exception " + oEx.toString());

            // Rabbit Sender
            Send oSendToRabbit = new Send();
            RabbitMessageViewModel oMessageViewModel = new RabbitMessageViewModel();
            oMessageViewModel.setMessageCode(LauncherOperations.PUBLISH);
            oMessageViewModel.setMessageResult("KO");

            try {
                String sJSON = MongoRepository.s_oMapper.writeValueAsString(oMessageViewModel);
                oSendToRabbit.SendMsg(oParameter.getQueue(),sJSON);
            }
            catch (Exception oEx2) {
                s_oLogger.debug("LauncherMain.Publish: Inner Exception " + oEx2.toString());
            }
        }

        return sLayerId;
    }

    /**
     * Publish single band image
     * @param oParameter
     * @return
     */
    public String PublishBandImage(PublishBandParameter oParameter) {

        String sLayerId = "";

        try {
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

                // Send KO to rabbit
                Send oSendToRabbit = new Send();
                RabbitMessageViewModel oRabbitVM = new RabbitMessageViewModel();
                oRabbitVM.setMessageCode(LauncherOperations.PUBLISHBAND);
                oRabbitVM.setMessageResult("KO");

                try {
                    String sJSON = MongoRepository.s_oMapper.writeValueAsString(oRabbitVM);
                    oSendToRabbit.SendMsg(oParameter.getQueue(),sJSON);
                }
                catch (Exception oEx2) {
                    s_oLogger.debug("LauncherMain.Download: Inner Exception " + oEx2.toString());
                }

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
                s_oLogger.debug( "LauncherMain.PublishBandImage:  Band already published. Return result");

                // Generate the View Model
                PublishBandResultViewModel oVM = new PublishBandResultViewModel();
                oVM.setBandName(oParameter.getBandName());
                oVM.setProductName(sProductName);
                oVM.setLayerId(sLayerId);

                RabbitMessageViewModel oRabbitVM = new RabbitMessageViewModel();
                oRabbitVM.setMessageCode(LauncherOperations.PUBLISHBAND);
                oRabbitVM.setMessageResult("OK");
                oRabbitVM.setPayload(oVM);

                String sJSON = MongoRepository.s_oMapper.writeValueAsString(oRabbitVM);

                Send oSendLayerId = new Send();

                if (oSendLayerId.SendMsg(oParameter.getQueue(),sJSON)==false) {
                    s_oLogger.debug("LauncherMain.PublishBandImage: Error sending Rabbit Message");
                }

                return sLayerId;
            }

            s_oLogger.debug( "LauncherMain.PublishBandImage:  Generating Band Image...");

            s_oLogger.debug( "LauncherMain.PublishBandImage:  Read Product");
            // Read the product
            ReadProduct oReadProduct = new ReadProduct();
            Product oSentinel = oReadProduct.ReadProduct(oFile);

            s_oLogger.debug( "LauncherMain.PublishBandImage:  Get GeoCoding");

            // Get the Geocoding and Band
            GeoCoding oGeoCoding = oSentinel.getSceneGeoCoding();

            s_oLogger.debug( "LauncherMain.PublishBandImage:  Getting Band " + oParameter.getBandName());

            Band oBand = oSentinel.getBand(oParameter.getBandName());

            // Get Image
            MultiLevelImage oBandImage = oBand.getSourceImage();
            // Get TIFF Metadata
            GeoTIFFMetadata oMetadata = GeoCoding2GeoTIFFMetadata.createGeoTIFFMetadata(oGeoCoding, oBandImage.getWidth(),oBandImage.getHeight());

            // Generate file output name
            String sOutputFilePath = sPath + sLayerId + ".tif";
            File oOutputFile = new File(sOutputFilePath);

            s_oLogger.debug( "LauncherMain.PublishBandImage:  Output file: " + sOutputFilePath);

            // Write the Band Tiff
            if (ConfigReader.getPropValue("CREATE_BAND_GEOTIFF_ACTIVE").equals("true"))  {
                s_oLogger.debug( "LauncherMain.PublishBandImage:  Writing Image");
                GeoTIFF.writeImage(oBandImage, oOutputFile, oMetadata);
            }
            else {
                s_oLogger.debug( "LauncherMain.PublishBandImage:  Debug on. Jump GeoTiff Generate");
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
            sLayerId = oPublisher.publishGeoTiff(sTargetFile,ConfigReader.getPropValue("GEOSERVER_ADDRESS"),ConfigReader.getPropValue("GEOSERVER_USER"),ConfigReader.getPropValue("GEOSERVER_PASSWORD"),ConfigReader.getPropValue("GEOSERVER_WORKSPACE"), sLayerId);

            s_oLogger.debug("LauncherMain.PublishBandImage: Image published. Update index and Send Rabbit Message");

            // Create Entity
            PublishedBand oPublishedBand = new PublishedBand();
            oPublishedBand.setLayerId(sLayerId);
            oPublishedBand.setProductName(sProductName);
            oPublishedBand.setBandName(oParameter.getBandName());

            // Add it the the db
            oPublishedBandsRepository.InsertPublishedBand(oPublishedBand);

            s_oLogger.debug("LauncherMain.PublishBandImage: Index Updated" );

            // Rabbit Sender
            Send oSendLayerId = new Send();

            s_oLogger.debug("LauncherMain.PublishBandImage: Queue = " + oParameter.getQueue() + " LayerId = " + sLayerId);

            // Create the View Model
            PublishBandResultViewModel oVM = new PublishBandResultViewModel();
            oVM.setBandName(oParameter.getBandName());
            oVM.setProductName(sProductName);
            oVM.setLayerId(sLayerId);

            RabbitMessageViewModel oRabbitVM = new RabbitMessageViewModel();
            oRabbitVM.setMessageCode(LauncherOperations.PUBLISHBAND);
            oRabbitVM.setMessageResult("OK");
            oRabbitVM.setPayload(oVM);

            // Convert in JSON
            String sJSON = MongoRepository.s_oMapper.writeValueAsString(oRabbitVM);

            // Send it
            if (oSendLayerId.SendMsg(oParameter.getQueue(),sJSON)==false) {
                s_oLogger.debug("LauncherMain.PublishBandImage: Error sending Rabbit Message");
            }
        }
        catch (Exception oEx) {

            s_oLogger.debug( "LauncherMain.PublishBandImage: Exception " + oEx.toString() + " " + oEx.getMessage());

            oEx.printStackTrace();

            // Rabbit Sender
            Send oSendToRabbit = new Send();
            RabbitMessageViewModel oMessageViewModel = new RabbitMessageViewModel();
            oMessageViewModel.setMessageCode(LauncherOperations.PUBLISHBAND);
            oMessageViewModel.setMessageResult("KO");

            try {
                String sJSON = MongoRepository.s_oMapper.writeValueAsString(oMessageViewModel);
                oSendToRabbit.SendMsg(oParameter.getQueue(),sJSON);
            }
            catch (Exception oEx2) {
                s_oLogger.debug("LauncherMain.Download: Inner Exception " + oEx2.toString());
            }
        }

        return  sLayerId;
    }
}
