package it.fadeout;
import it.fadeout.filebuffer.DownloadFile;
import org.apache.commons.cli.*;

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

        Option oOptElaborateFile   = OptionBuilder.withArgName( "elaboratefile" )
                .hasArg()
                .withDescription(  "file to elaborate" )
                .create( "elaboratefile" );

        Option oOptOperation   = OptionBuilder.withArgName( "operation" )
                .hasArg()
                .withDescription(  "operation to execute (CALIB,FILTER,MULTI,TERRAIN)" )
                .create( "operation" );

        Option oOptDownloadFile   = OptionBuilder.withArgName( "downloadFileUrl" )
                .hasArg()
                .withDescription(  "URL to dowload file" )
                .create( "downloadFileUrl" );

        oOptions.addOption(oOptElaborateFile);
        oOptions.addOption(oOptOperation);
        oOptions.addOption(oOptDownloadFile);

        try {
            Operation oOperation = null;
            String sElaborateFile = null;
            String sDownloadFile = null;
            // parse the command line arguments
            CommandLine oLine = parser.parse( oOptions, args );
            if (oLine.hasOption("operation")) {
                // print the value of block-size
                oOperation  = Operation.valueOf(oLine.getOptionValue("operation"));

            }
            if (oLine.hasOption("elaboratefile")) {
                // print the value of block-size
                sElaborateFile = oLine.getOptionValue("elaboratefile");

            }
            if (oLine.hasOption("downloadFileUrl")) {
                // print the value of block-size
                sDownloadFile = oLine.getOptionValue("downloadFileUrl");
                String sDownloadRootPath = ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH");
                DownloadFile oDownloadFile = new DownloadFile();
                //Execute download
                oDownloadFile.ExecuteDownloadFile(sDownloadFile, sDownloadRootPath);
                //TODO: publish file

                System.exit(0);
            }
            if (oOperation != null)
            {
                //Dispatcher oDispatcher = new Dispatcher(oOperation, sElaborateFile);
                //Thread oThread = new Thread(oDispatcher);
                //oThread.start();
            }


        }
        catch( ParseException exp ) {
            // oops, something went wrong
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
            System.exit(-1);
        }


    }
}
