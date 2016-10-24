package wasdi;
import org.apache.commons.io.FileUtils;
import org.esa.snap.core.datamodel.Product;
import wasdi.filebuffer.DownloadFile;
import org.apache.commons.cli.*;
import wasdi.geoserver.Publisher;
import wasdi.shared.LauncherOperations;
import wasdi.shared.parameters.DownloadFileParameter;
import wasdi.shared.parameters.PublishParameters;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.snapopearations.ReadProduct;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by s.adamo on 23/09/2016.
 */
public class LauncherMain {

    //-operation <operation> -elaboratefile <file>
    public static void main(String[] args) throws Exception {


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
                // print the value of block-size
                sOperation  = oLine.getOptionValue("operation");

            }

            if (oLine.hasOption("parameter")) {
                // print the value of block-size
                sParameter = oLine.getOptionValue("parameter");

            }

            LauncherMain oLauncher = new LauncherMain();
            oLauncher.ExecuteOperation(sOperation,sParameter);

        }
        catch( ParseException exp ) {
            // oops, something went wrong
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
            System.exit(-1);
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
                        System.out.println( sText);
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
                case LauncherOperations.PUBLISH: {

                    // Deserialize Parameters
                    PublishParameters oPublishParameter = (PublishParameters) SerializationUtils.deserializeXMLToObject(sParameter);
                    Publish(oPublishParameter);
                }
            }
        }
        catch (Exception oEx) {
            System.out.println("ExecuteOperation Exception " + oEx.toString());
        }
    }

    public String Download(DownloadFileParameter oParameter, String sDownloadPath) {
        String sFileName = "";
        try {
            // Generate the Path adding user id and workspace
            sDownloadPath += oParameter.getUserId()+"/"+oParameter.getWorkspace();
            // Download file
            DownloadFile oDownloadFile = new DownloadFile();
            sFileName = oDownloadFile.ExecuteDownloadFile(oParameter.getUrl(), sDownloadPath);
        }
        catch (Exception oEx) {
            System.out.println("LauncherMain.Download: Exception " + oEx.toString());
        }

        return  sFileName;
    }

    public String Publish(PublishParameters oParameter) {
        String sLayerId = "";

        try {
            // Read File Name
            String sFile = oParameter.getFileName();

            // Check integrity
            if (Utils.isNullOrEmpty(sFile))
            {
                System.out.println( "LauncherMain.Publish: file is null or empty");
                return  sLayerId;
            }

            // Create a file object for the downloaded file
            File oDownloadedFile = new File(sFile);
            String sInputFileNameOnly = oDownloadedFile.getName();

            sLayerId = sInputFileNameOnly;

            // Create a clean layer id: the file name without any extension
            String [] asLayerIdSplit = sInputFileNameOnly.split("\\.");
            if (asLayerIdSplit!=null) {
                if (asLayerIdSplit.length>0){
                    sLayerId = asLayerIdSplit[0];
                }
            }

            // Copy fie to GeoServer Data Dir
            String sGeoServerDataDir = ConfigReader.getPropValue("GEOSERVER_DATADIR");
            String sTargetDir = sGeoServerDataDir;

            if (!(sTargetDir.endsWith("/")||sTargetDir.endsWith("\\"))) sTargetDir+="/";
            sTargetDir+=sLayerId+"/";

            String sTargetFile = sTargetDir + sInputFileNameOnly;

            File oTargetFile = new File(sTargetFile);

            System.out.println("LauncherMain.publish: InputFile: " + sFile + " TargetFile: " + sTargetDir + " LayerId " + sLayerId);

            FileUtils.copyFile(oDownloadedFile,oTargetFile);

            //TODO: Here recognize the file type and run the right procedure. At the moment assume Sentinel1A

            System.out.println("LauncherMain.publish: Write Big Tiff");
            // Convert the product in a Tiff file
            ReadProduct oReadProduct = new ReadProduct();
            String sTiffFile = oReadProduct.writeBigTiff(oTargetFile.getAbsolutePath(), sTargetDir);
            //String sTiffFile = "C:\\Program Files (x86)\\GeoServer 2.9.2\\data_dir\\data\\S1A_IW_GRDH_1SSV_20150213T095824_20150213T095849_004603_005AB7_5539\\S1A_IW_GRDH_1SSV_20150213T095824_20150213T095849_004603_005AB7_5539.zip.tif";

            System.out.println("LauncherMain.publish: TiffFile: " +sTiffFile);

            // Check result
            if (Utils.isNullOrEmpty(sTiffFile))
            {
                System.out.println( "LauncherMain.Publish: Tiff File is null or empty");
                return sLayerId;
            }

            // Ok publish
            Publisher.PYTHON_PATH = ConfigReader.getPropValue("PYTHON_PATH");
            Publisher.TARGET_DIR_BASE = ConfigReader.getPropValue("PYRAMID_BASE_FOLDER");
            Publisher.GDALBasePath = ConfigReader.getPropValue("GDAL_PATH")+"/"+ConfigReader.getPropValue("GDAL_RETILE");
            Publisher.PYRAMYD_ENV_OPTIONS = ConfigReader.getPropValue("PYRAMYD_ENV_OPTIONS");

            System.out.println("LauncherMain.publish: call PublishImage");
            Publisher oPublisher = new Publisher();
            sLayerId = oPublisher.publishImage(sTiffFile,ConfigReader.getPropValue("GEOSERVER_ADDRESS"),ConfigReader.getPropValue("GEOSERVER_USER"),ConfigReader.getPropValue("GEOSERVER_PASSWORD"),ConfigReader.getPropValue("GEOSERVER_WORKSPACE"), sLayerId);

            // Deletes the copy of the Zip file
            File oZipTargetFile = new File(oTargetFile.getPath()+"/"+oDownloadedFile.getName());
            oZipTargetFile.delete();
        }
        catch (Exception oEx) {
            System.out.println("LauncherMain.Publish: exception " + oEx.toString());
        }

        return sLayerId;
    }
}
