package wasdi;
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
                        oPublishParameter.setQueue(oDownloadFileParameter.getQueue());
                        oPublishParameter.setStore("pyramidstore");

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
            // Download file
            String sFile = oParameter.getFileName();

            // Check result
            if (Utils.isNullOrEmpty(sFile))
            {
                System.out.println( "LauncherMain.Publish: file is null or empty");
                return  sLayerId;
            }

            //TODO: Here recognize the file type and run the right procedure. At the moment assume Sentinel1A

            // Convert the product in a Tiff file
            ReadProduct oReadProduct = new ReadProduct();
            String sTiffFile = oReadProduct.writeBigTiff(sFile, ConfigReader.getPropValue("PYRAMID_BASE_FOLDER"));

            // Check result
            if (Utils.isNullOrEmpty(sTiffFile))
            {
                System.out.println( "LauncherMain.Publish: Tiff File is null or empty");
                return sLayerId;
            }

            // Ok publish
            Publisher oPublisher = new Publisher(ConfigReader.getPropValue("PYRAMID_BASE_FOLDER"), ConfigReader.getPropValue("GDAL_PATH")+"/"+ConfigReader.getPropValue("GDAL_RETILE"));
            sLayerId = oPublisher.publishImage(sTiffFile,ConfigReader.getPropValue("GEOSERVER_ADDRESS"),ConfigReader.getPropValue("GEOSERVER_USER"),ConfigReader.getPropValue("GEOSERVER_PASSWORD"),ConfigReader.getPropValue("GEOSERVER_WORKSPACE"), oParameter.getStore());
        }
        catch (Exception oEx) {
            System.out.println("LauncherMain.Publish: exception " + oEx.toString());
        }

        return sLayerId;
    }
}
