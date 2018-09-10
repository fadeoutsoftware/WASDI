import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.util.MemUtils;
import org.esa.snap.graphbuilder.rcp.dialogs.support.GraphExecuter;
import org.esa.snap.runtime.Config;
import org.geotools.referencing.factory.epsg.HsqlEpsgDatabase;
import org.geotools.xml.xsi.XSISimpleTypes;
import org.json.JSONObject;
import search.SentinelInfo;
import search.SentinelSearch;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.Catalog;
import wasdi.shared.data.CatalogRepository;
import wasdi.shared.data.MongoRepository;

import javax.media.jai.JAI;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by s.adamo on 20/02/2017.
 */
public class Main {

    /**
     * This Filter represent a bounding box over Italy, Mission: Sentinel-1, Product Type: GRD, Sensing period:
     * ( footprint:"intersects(POLYGON((6.50501944 36.30000000,6.50501944 47.74058056,19.05000000 47.74058056,19.05000000 36.30000000,6.50501944 36.30000000)))" ) AND ( beginPosition:[2017-02-20T00:00:00.000Z TO 2017-02-20T23:59:59.999Z] AND endPosition:[2017-02-20T00:00:00.000Z TO 2017-02-20T23:59:59.999Z] ) AND( ingestionDate:[2017-02-20T00:00:00.000Z TO 2017-02-20T23:59:59.999Z ] ) AND   (platformname:Sentinel-1 AND producttype:GRD)
     */
    private static String s_sFilterTemplate = "( footprint:\"intersects(POLYGON((6.50501944 36.30000000,6.50501944 47.74058056,19.05000000 47.74058056,19.05000000 36.30000000,6.50501944 36.30000000)))\" ) AND ( beginPosition:[{SensingStart} TO {SensingEnd}] AND endPosition:[{SensingStart} TO {SensingEnd}] ) AND (platformname:Sentinel-1 AND producttype:GRD)";

    public static int s_iLimit = 25;

    public static String s_sSortedBy = "ingestiondate";

    public static String s_sOrder = "desc";

    public static Logger s_oLogger = Logger.getLogger(Main.class);


    /**
     * This is the max number of download allowed by Sentinel
     */
    //public static int s_iThreadNumber = 1;

    public static void main(String[] args) throws Exception {

        try {
            //get jar directory
            File oCurrentFile = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            //configure log
            DOMConfigurator.configure(oCurrentFile.getParentFile().getPath() + "/log4j.xml");

        } catch (Exception exp) {
            //no log4j configuration
            System.err.println("Error loading log.  Reason: " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(exp));
            System.exit(-1);
        }

        //To improve performance
        JAI.getDefaultInstance().getTileScheduler().setParallelism(Runtime.getRuntime().availableProcessors());
        MemUtils.configureJaiTileCache();

        //Init SNAP
        Path propFile = Paths.get(ConfigReader.getPropValue("SNAP_AUX_PROPERTIES"));
        Config.instance("snap.auxdata").load(propFile);
        Config.instance().load();
        SystemUtils.initGeoTools();
        System.setProperty(HsqlEpsgDatabase.DIRECTORY_KEY, ConfigReader.getPropValue("EPSG_DATABASE"));

        //we only take descending orbit so from 00:00:00 to 12:00:00
        String sStartTime = "00:00:00.000Z";
        String sEndTime = "12:00:00.000Z";
        SimpleDateFormat oDateFormat = new SimpleDateFormat("yyyy-MM-dd");

        //Date oNow = new Date();
        Calendar oCal = Calendar.getInstance();
        oCal.set(2017,Calendar.FEBRUARY,22);

        String sDateNow = oDateFormat.format(oCal.getTime());
        String sSensingStart = sDateNow + 'T' + sStartTime;
        String sSensingEnd = sDateNow + 'T' + sEndTime;


        s_sFilterTemplate = s_sFilterTemplate.replace("{SensingStart}", sSensingStart).replace("{SensingEnd}", sSensingEnd);

        s_oLogger.debug("Main: Begin");

        //ExecutorService oExecutor = Executors.newFixedThreadPool(Integer.valueOf(ConfigReader.getPropValue("THREAD_NUM")));

        // create the parser
        CommandLineParser parser = new DefaultParser();

        // create Options object
        Options oOptions = new Options();


        Option oOptOperation   = OptionBuilder.withArgName( "operation" ).hasArg().withDescription(  "" ).create( "operation" );

        Option oOptFile   = OptionBuilder.withArgName( "file" ).hasArg().withDescription(  "" ).create( "file" );


        oOptions.addOption(oOptOperation);
        oOptions.addOption(oOptFile);

        String sOperation = "";
        String sFile = "";

        // parse the command line arguments
        CommandLine oLine = parser.parse( oOptions, args );
        if (oLine.hasOption("operation")) {
            // Get the Operation Code
            sOperation  = oLine.getOptionValue("operation");

        }
        if (oLine.hasOption("file")) {
            // Get the Parameter File
            sFile = oLine.getOptionValue("file");
        }

        try {
            switch (sOperation) {
                case "DOWNLOAD": {
                    s_oLogger.debug("Main: Begin download");

                    String sCount = SentinelSearch.ExecuteQueryCount(s_sFilterTemplate, ConfigReader.getPropValue("DHUS_USER"), ConfigReader.getPropValue("DHUS_PASSWORD"), "SENTINEL");
                    s_oLogger.debug(String.format("Main: number of download %s", sCount));
                    if (sCount == null)
                        break;

                    List<SentinelInfo> aoSentinelInfo = new ArrayList<>();

                    if (sCount != null && sCount != "") {
                        int iCount = Integer.parseInt(sCount);
                        int iOffset = 0;
                        for (; iOffset < iCount; iOffset++) {
                            if (iOffset == 0 || iOffset % (s_iLimit - 1) == 0) {

                                Download(iOffset, aoSentinelInfo);
                            }
                        }
                    }

                    String sDownloadPath = ConfigReader.getPropValue("DOWNLOAD_PATH");
                    if (!sDownloadPath.endsWith("/"))
                        sDownloadPath += "/";

                    s_oLogger.debug("Main: SentinelInfos serilizing");
                    if(Boolean.valueOf(ConfigReader.getPropValue("SERILIZE_INFO"))) {
                        Utils.SerializeObjectToXML(sDownloadPath + "SentinelInfos.xml", aoSentinelInfo);
                    }

                    if(Boolean.valueOf(ConfigReader.getPropValue("CREATE_RUN_SCRIPT"))) {
                        PrintWriter oWriter = null;
                        try {
                            oWriter = new PrintWriter(sDownloadPath + "run.sh", "UTF-8");
                            String ProgramToLaunch = "java -jar " + ConfigReader.getPropValue("DAEMON_FILE");
                            String sRunOperation = " -operation RUN ";
                            String sFileRun = ProgramToLaunch + sRunOperation + " -file ";
                            for (SentinelInfo oInfo :
                                    aoSentinelInfo) {
                                if (oInfo.getProductFilePath() != null && oInfo.getProductFilePath() != "")
                                    oWriter.println(sFileRun + oInfo.getProductFilePath() + " ;");

                            }
                            //Launch mulesme
                            String sMulesmeOperation = " -operation MULESME ;";
                            oWriter.println(ProgramToLaunch + sMulesmeOperation);

                            //Catalog Operation
                            String sCatalogOperation = " -operation CATALOG ;";
                            oWriter.println(ProgramToLaunch + sCatalogOperation);

                        } catch (IOException e) {
                            s_oLogger.debug("Main: Error creating script " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e));
                        } finally {
                            if (oWriter != null)
                                oWriter.close();
                        }
                    }

                    try
                    {
                        s_oLogger.debug("Main: Launch script");
                        if(Boolean.valueOf(ConfigReader.getPropValue("RUN_SCRIPT"))) {
                            s_oLogger.debug("Main: Launch Script");
                            LaunchScript(sDownloadPath + "./run.sh");
                        }

                    }
                    catch(Exception oEx)
                    {
                        s_oLogger.debug("Main: Error launching script " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
                    }

                    s_oLogger.debug("Main: End download");



                    break;

                }

                case "RUN": {
                    s_oLogger.debug("Main: Begin run Sentinel elaboration");

                    String sDownloadPath = ConfigReader.getPropValue("DOWNLOAD_PATH");
                    if (!sDownloadPath.endsWith("/"))
                        sDownloadPath += "/";

                    List<SentinelInfo> aoSentinelInfo = (List<SentinelInfo>) Utils.DeserializeXMLToObject(sDownloadPath + "SentinelInfos.xml");

                    Elaboration(aoSentinelInfo, sFile);

                    s_oLogger.debug("Main: End elaboration");
                    break;
                }

                case "MULESME": {
                    s_oLogger.debug("Main: Begin Mulesme");

                    LaunchMulesme();

                    s_oLogger.debug("Main: End Mulesme");
                    break;
                }

                case "CATALOG": {
                    s_oLogger.debug("Main: Begin catalog creation");

                    CreateCatalog();

                    s_oLogger.debug("Main: End catalog creation");
                    break;
                }


            }}
        catch (Exception oEx)
        {
            s_oLogger.debug("Main: Error during switch operation " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
        }

        //wait termination
        //while(!oExecutor.isTerminated())
        //{}

        s_oLogger.debug("Main: End Daemon");
    }


    private static void Download(int iOffset, List<SentinelInfo> aoSentinelInfo) throws IOException {

        HashMap<String, String> oMap = SentinelSearch.CreateQueryParams(String.valueOf(iOffset), String.valueOf(Main.s_iLimit), Main.s_sSortedBy, Main.s_sOrder, ConfigReader.getPropValue("DHUS_USER"), ConfigReader.getPropValue("DHUS_PASSWORD"));
        s_oLogger.debug(String.format("Main.Download: Execute query %s", s_sFilterTemplate));
        JSONObject oJSON = null;
        try {
            oJSON = SentinelSearch.ExecuteQuery(s_sFilterTemplate, oMap);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            s_oLogger.debug(String.format("Main.Download: Error executing query search " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e)));
        } catch (IOException e) {
            e.printStackTrace();
            s_oLogger.debug(String.format("Main.Download: Error executing query search " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e)));
        }
        if (oJSON == null) {
            s_oLogger.debug(String.format("Main.Download: Returned JSON object is null"));
            return;
        }
        aoSentinelInfo.addAll(SentinelSearch.GetSentinelinfos(oJSON));

        //Downlod all products before elaborate
        if (aoSentinelInfo != null && aoSentinelInfo.size() > 0) {
            for (SentinelInfo oSentinelInfo :
                    aoSentinelInfo) {

                String sReturnFilePath = ExecuteDownloadFile(oSentinelInfo.getDownloadLink(), ConfigReader.getPropValue("DOWNLOAD_PATH"), false);
                oSentinelInfo.setProductFilePath(sReturnFilePath);
                s_oLogger.debug(String.format("Main.Download: Downloaded file %s", oSentinelInfo.getFileName()));

            }
        }

    }

    public static void Elaboration(List<SentinelInfo> aoSentinelInfo, String sProductFilePath)
    {
        if (aoSentinelInfo != null && aoSentinelInfo.size() > 0) {
            try {

                for (SentinelInfo oInfo :
                        aoSentinelInfo) {
                    if (oInfo.getProductFilePath().equals(sProductFilePath)) {
                        if (Files.exists(Paths.get(sProductFilePath))) {

                            s_oLogger.debug(String.format("Main.Elaboration: Begin operation for " + oInfo.getProductFilePath()));

                            SentinelThread oSentinelThread = new SentinelThread(oInfo.getProductFilePath());
                            oSentinelThread.setGraphEx(new GraphExecuter());
                            oSentinelThread.setSentinelInfo(oInfo);
                            oSentinelThread.call();

                            FileUtils.deleteQuietly(new File(oInfo.getProductFilePath()));
                        }
                    }

                }


            } catch (Exception oEx) {
                s_oLogger.error(String.format("Main.Elaboration: Error creating creating sentinel product ") + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
            } finally {
                MemUtils.freeAllMemory();
            }


            //FutureTask<String[]> oTask = new FutureTask(new SentinelThread(oSentinelInfo));
            //call thread
            //oExecutor.submit(oTask);

        }
    }


    public static String ExecuteDownloadFile(String sFileURL, String sSaveDirOnServer, boolean bForceDownload) throws IOException {

        // Domain check
        if (sFileURL == null || sFileURL == "") {
            return "";
        }
        if (sSaveDirOnServer == null || sSaveDirOnServer == "") {
            return "";
        }

        String sReturnFilePath = "";

        // TODO: Here we are assuming dhus authentication. But we have to find a general solution
        Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                try{
                    return new PasswordAuthentication(ConfigReader.getPropValue("DHUS_USER"), ConfigReader.getPropValue("DHUS_PASSWORD").toCharArray());
                }
                catch (Exception oEx){
                }
                return null;
            }
        });


        URL url = new URL(sFileURL);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        int responseCode = httpConn.getResponseCode();

        // always check HTTP response code first
        if (responseCode == HttpURLConnection.HTTP_OK) {


            String fileName = "";
            String disposition = httpConn.getHeaderField("Content-Disposition");
            String contentType = httpConn.getContentType();
            int contentLength = httpConn.getContentLength();

            if (disposition != null) {
                // extracts file name from header field
                int index = disposition.indexOf("filename=");
                if (index > 0) {
                    fileName = disposition.substring(index + 10,
                            disposition.length() - 1);
                }
            } else {
                // extracts file name from URL
                fileName = sFileURL.substring(sFileURL.lastIndexOf("/") + 1,
                        sFileURL.length());
            }


            // opens input stream from the HTTP connection
            InputStream inputStream = httpConn.getInputStream();
            String saveFilePath = sSaveDirOnServer;
            if (!sSaveDirOnServer.endsWith("/"))
                saveFilePath= sSaveDirOnServer + "/";

            saveFilePath += fileName;

            if (Files.exists(Paths.get(saveFilePath)) && !bForceDownload)
                return saveFilePath;

            File oTargetFile = new File(saveFilePath);
            File oTargetDir = oTargetFile.getParentFile();
            oTargetDir.mkdirs();

            // opens an output stream to save into file
            FileOutputStream outputStream = new FileOutputStream(saveFilePath);

            int bytesRead = -1;
            byte[] buffer = new byte[4096];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();

            sReturnFilePath = saveFilePath;

        } else {

        }
        httpConn.disconnect();

        return  sReturnFilePath;
    }

    private static void LaunchScript(String sScriptPath)
    {
        try
        {
            s_oLogger.debug("Main.LaunchScript: Launch script " + ConfigReader.getPropValue("MULESME_SHELL_SCRIPT"));

            Process oProcess = Runtime.getRuntime().exec(sScriptPath);
            BufferedReader input = new BufferedReader(new InputStreamReader(oProcess.getInputStream()));
            String sLine;

            while((sLine=input.readLine()) != null) {
                s_oLogger.debug("Main.LaunchScript: " + sLine);
            }

            oProcess.waitFor();

        }
        catch (Exception oEx){
            s_oLogger.debug("Main.LaunchScript: Error during script run." + oEx.getMessage());
        }
    }


    private static void LaunchMulesme()
    {
        try
        {
            s_oLogger.debug("Main.LaunchMulesme: Launch Mulesme " + ConfigReader.getPropValue("MULESME_SHELL_SCRIPT"));

            Process oProcess = Runtime.getRuntime().exec(ConfigReader.getPropValue("MULESME_SHELL_SCRIPT"));
            BufferedReader input = new BufferedReader(new InputStreamReader(oProcess.getInputStream()));
            String sLine;

            while((sLine=input.readLine()) != null) {
                s_oLogger.debug("Main.LaunchMulesme: " + sLine);
            }

            oProcess.waitFor();

        }
        catch (Exception oEx){
            s_oLogger.debug("Main.LaunchMulesme: Error during Mulesme run." + oEx.getMessage());
        }
    }

    private static void CreateCatalog()
    {

        try {
            //Init Mongo repository
            MongoRepository.SERVER_PORT = Integer.parseInt(ConfigReader.getPropValue("MONGO_PORT"));
            MongoRepository.DB_NAME = ConfigReader.getPropValue("MONGO_DBNAME");
            MongoRepository.DB_USER = ConfigReader.getPropValue("MONGO_DBUSER");
            MongoRepository.DB_PWD = ConfigReader.getPropValue("MONGO_DBPWD");

            String sPrefix = "smcitaly";
            File oEstimatesDir = new File(ConfigReader.getPropValue("MULESME_ESTIMATES_PATH"));
            File[] aoFiles = oEstimatesDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().startsWith(sPrefix);
                }
            });
            CatalogRepository oRepository = new CatalogRepository();

            for (File oFile :
                    aoFiles) {
                Catalog oCatalog = new Catalog();
                oCatalog.setFileName(oFile.getName());
                String sFileNameWithoutExtension = FilenameUtils.getBaseName(oFile.getName());
                try {
                    String sDate = sFileNameWithoutExtension.split("_")[1];
                    oCatalog.setDate(sDate.substring(0, 3) + "-" + sDate.substring(4, 5) + "-" + sDate.substring(6, 7));
                } catch (Exception oEx) {
                    oEx.printStackTrace();
                    s_oLogger.debug("Main.CreateCatalog: Error creating date for catalog." + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
                }

                //In any case, move Mulesme files from MULESME_ESTIMATES_PATH to MULESME_CATALOG
                s_oLogger.debug("Main.CreateCatalog: Move all SMCItaly*");
                String sMulesmeCatalogPath =  ConfigReader.getPropValue("MULESME_CATALOG");
                String sMulesmeEstimatePath =  ConfigReader.getPropValue("MULESME_ESTIMATES_PATH");
                if (!sMulesmeCatalogPath.endsWith("/"))
                    sMulesmeCatalogPath += "/";
                if (!sMulesmeEstimatePath.endsWith("/"))
                    sMulesmeEstimatePath += "/";
                if(!Files.exists(Paths.get(sMulesmeCatalogPath)))
                {
                    s_oLogger.debug("Main.CreateCatalog: Directory " +sMulesmeCatalogPath + " not exists");
                    if (Files.createDirectories(Paths.get(sMulesmeCatalogPath))== null)
                    {
                        System.out.println("Main.CreateCatalog: Directory " + sMulesmeCatalogPath + " not created");
                        return;
                    }
                }

                Path PathTragetFile = Files.move(Paths.get(sMulesmeEstimatePath + oFile.getName()), Paths.get(sMulesmeCatalogPath));
                s_oLogger.debug("Main.CreateCatalog: File " + sMulesmeEstimatePath + oFile.getName() + " moved to " + PathTragetFile );
                oCatalog.setFilePath(PathTragetFile.toString());

                if (!oRepository.InsertCatalogEntry(oCatalog))
                    s_oLogger.debug("Main.CreateCatalog: Error during inserting catalog entry");

            }


        } catch (IOException e) {
            e.printStackTrace();
            s_oLogger.debug("Main.CreateCatalog: Error during catalog creation" + e.getMessage());
        }
    }



}
