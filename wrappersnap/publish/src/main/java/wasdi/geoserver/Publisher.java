package wasdi.geoserver;

import it.geosolutions.geoserver.rest.encoder.coverage.GSCoverageEncoder;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import wasdi.shared.utils.Utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by s.adamo on 24/05/2016.
 */
public class Publisher {

    // Define a static logger variable so that it references the
    // Logger instance named "MyApp".
    static Logger s_oLogger = Logger.getLogger(Publisher.class);

    public static final int LEVEL = 4;

    public static final int WIDTH = 2048;

    public static final int HEIGHT = 2048;

    public static String TARGET_DIR_BASE = "c:\\temp\\ImagePyramidTest\\";

    public static String TARGET_DIR_PYRAMID = "c:\\temp\\ImagePyramidTest\\TempImagePyramidCreation\\";

    public static String GDAL_Retile_Path = "\"C:\\Program Files\\GDAL\\bin\\gdal\\python\\scripts\\gdal_retile.py\"";

    public static String PYRAMYD_ENV_OPTIONS = "PYTHONPATH=C:/Program Files/GDAL/bin/gdal/python|PROJ_LIB=C:/Program Files/GDAL/bin/proj/SHARE|GDAL_DATA=C:/Program Files/GDAL/bin/gdal-data|GDAL_DRIVER_PATH=C:/Program Files/GDAL/bin/gdal/plugins|PATH=C:/Program Files/GDAL/bin;C:/Program Files/GDAL/bin/gdal/python/osgeo;C:/Program Files/GDAL/bin/proj/apps;C:/Program Files/GDAL/bin/gdal/apps;C:/Program Files/GDAL/bin/ms/apps;C:/Program Files/GDAL/bin/gdal/csharp;C:/Program Files/GDAL/bin/ms/csharp;C:/Program Files/GDAL/bin/curl;C:/Python34";

    public static String PYTHON_PATH = "c:/OSGeo4W64/bin/python";

    public Publisher()
    {
        try {
            //get jar directory
            File oCurrentFile = new File(Publisher.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            //configure log
            DOMConfigurator.configure(oCurrentFile.getParentFile().getPath() + "/log4j.xml");

        }catch(Exception exp)
        {
            //no log4j configuration
            System.err.println( "Error loading log.  Reason: " + exp.getMessage() );
            System.exit(-1);
        }
    }

    public Publisher(String sPyramidBaseFolder, String sGDALBasePath, String sPyramidEnvOptions)
    {
        TARGET_DIR_BASE = sPyramidBaseFolder;
        GDAL_Retile_Path = sGDALBasePath;
        PYRAMYD_ENV_OPTIONS = sPyramidEnvOptions;
    }


    /*

     */
    private boolean LaunchImagePyramidCreation(String sInputFile, Integer iLevel, Integer iWidth, Integer iHeight, String sPathName) {

        String sTargetDir = sPathName;
        if (!sTargetDir.endsWith("/"))
            sTargetDir += "/";
        Path oTargetPath = Paths.get(sTargetDir);
        if (!Files.exists(oTargetPath))
        {
            try {
                Files.createDirectory(oTargetPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            //fix permission
            Utils.fixUpPermissions(oTargetPath);
            //String sCmd = String.format("%s %s -v -r bilinear -levels %d -ps %s %s -co \"TILED=YES\" -targetDir %s %s", PYTHON_PATH, GDAL_Retile_Path, iLevel, iWidth, iHeight, "\""+sTargetDir +"\"","\""+ sInputFile+"\"");
            //String sCmd = String.format("%s %s -v -r bilinear -levels %d -ps %s %s -co TILED=YES -targetDir %s %s", PYTHON_PATH, GDAL_Retile_Path, iLevel, iWidth, iHeight, sTargetDir, sInputFile);
            //String sCmd = String.format("/usr/lib/wasdi/launcher/run_gdal_retile.sh %s %s", sTargetDir, sInputFile);
                        
            String sCmd = String.format("gdal_retile.py -v -r bilinear -levels %d -ps %d %d -co TILED=YES -targetDir %s %s", iLevel, iWidth, iHeight, sTargetDir, sInputFile);
            
            String[] asEnvp = PYRAMYD_ENV_OPTIONS.split("\\|");

            s_oLogger.debug("Publisher.LaunchImagePyramidCreation: Command: " + sCmd);

            Process oProcess = null;
            try {

                Runtime oRunTime = Runtime.getRuntime();
                oProcess = oRunTime.exec(sCmd);

                // any error?
                StreamProcessWriter oErrorWriter = new StreamProcessWriter(oProcess.getErrorStream(), "ERROR");

                // any output?
                StreamProcessWriter oOutputWriter = new StreamProcessWriter(oProcess.getInputStream(), "OUTPUT");

                oErrorWriter.start();
                oOutputWriter.start();

                int iValue = oProcess.waitFor();

                try {
                    if (oProcess.getOutputStream()!=null) oProcess.getOutputStream().flush();
                    if (oProcess.getOutputStream()!=null) oProcess.getOutputStream().close();
                    if (oProcess.getInputStream() !=null) oProcess.getInputStream().close();
                    if (oProcess.getErrorStream() !=null) oProcess.getErrorStream().close();
                }
                catch (Exception oEx) {
                    s_oLogger.debug("Publisher.LaunchImagePyramidCreation:  Exception closing Streams " + oEx.toString());
                }


                try  {
                    oOutputWriter.interrupt();
                }
                catch (Exception oEx) {
                    s_oLogger.debug("Publisher.LaunchImagePyramidCreation:  Exception interrupting Output Writer thread " + oEx.toString());
                }
                try  {
                    oErrorWriter.interrupt();
                }
                catch (Exception oEx) {
                    s_oLogger.debug("Publisher.LaunchImagePyramidCreation:  Exception interrupting Error Writer Thread " + oEx.toString());
                }


                s_oLogger.debug("Publisher.LaunchImagePyramidCreation:  Exit Value " + iValue);

                s_oLogger.debug("Publisher.LaunchImagePyramidCreation:  End ");


            } catch (IOException e) {
                s_oLogger.debug("Publisher.LaunchImagePyramidCreation: Error generating pyramid image: " + e.getMessage());

                return  false;
            } catch (InterruptedException e) {
                e.printStackTrace();
                s_oLogger.debug("Publisher.LaunchImagePyramidCreation: Error generating pyramid image: " + e.getMessage());

                return  false;
            }
            finally {
                if (oProcess != null) {
                    s_oLogger.debug("Publisher.LaunchImagePyramidCreation: finally destroy Process");
                    oProcess.destroy();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            s_oLogger.debug("Publisher.LaunchImagePyramidCreation: Error generating pyramid image: " + e.getMessage());
            return  false;
        }

        s_oLogger.debug("Publisher.LaunchImagePyramidCreation:  Return true");
        return  true;
    }

    private String PublishImagePyramidOnGeoServer(String sFileName, String sGeoServerAddress, String sGeoServerUser, String sGeoServerPassword, String sWorkspace, String sStoreName) throws Exception {

        File oFile = new File(sFileName);
        String sPath = oFile.getParent();


        // Create Pyramid
        if (!LaunchImagePyramidCreation(sFileName, LEVEL, WIDTH, HEIGHT, sPath))
            return null;

        s_oLogger.debug("Publisher.PublishImagePyramidOnGeoServer: Publish Image Pyramid");

        //Create GeoServer Manager
        GeoServerManager oManager = new GeoServerManager(sGeoServerAddress, sGeoServerUser, sGeoServerPassword);

        if (!oManager.getReader().existsWorkspace(sWorkspace)) {
            oManager.getPublisher().createWorkspace(sWorkspace);
        }

        //publish image pyramid
        try {

            // Storage Folder
            File oSourceDir = new File(sPath);

            //Pubblico il layer
            String slLayerName = sFileName;
            boolean bResult = oManager.publishImagePyramid(sWorkspace, sStoreName, oSourceDir);

            //configure coverage
            GSCoverageEncoder ce = new GSCoverageEncoder();
            ce.setEnabled(true); //abilito il coverage
            ce.setSRS("EPSG:4326");
            boolean exists = oManager.getReader().existsCoveragestore(sWorkspace, sStoreName);
            if (exists)
                exists = oManager.getReader().existsCoverage(sWorkspace, sStoreName, slLayerName);
            if(exists)
                oManager.getPublisher().configureCoverage(ce, sWorkspace, sStoreName, slLayerName);
        }catch (Exception oEx){}

        return sStoreName;

    }


    private String PublishGeoTiffImage(String sFileName, String sGeoServerAddress, String sGeoServerUser, String sGeoServerPassword, String sWorkspace, String sStoreName) throws Exception {
        return  PublishGeoTiffImage(sFileName,sGeoServerAddress,sGeoServerUser,sGeoServerPassword,sWorkspace,sStoreName, "EPSG:4326");
    }

    private String PublishGeoTiffImage(String sFileName, String sGeoServerAddress, String sGeoServerUser, String sGeoServerPassword, String sWorkspace, String sStoreName, String sEPSG) throws Exception {
        return  PublishGeoTiffImage(sFileName,sGeoServerAddress,sGeoServerUser,sGeoServerPassword,sWorkspace,sStoreName, sEPSG, "raster");
    }

    private String PublishGeoTiffImage(String sFileName, String sGeoServerAddress, String sGeoServerUser, String sGeoServerPassword, String sWorkspace, String sStoreName, String sEPSG, String sStyle) throws Exception {

        File oFile = new File(sFileName);

        //Create GeoServer Manager
        GeoServerManager oManager = new GeoServerManager(sGeoServerAddress, sGeoServerUser, sGeoServerPassword);

        if (!oManager.getReader().existsWorkspace(sWorkspace)) {
            oManager.getPublisher().createWorkspace(sWorkspace);
        }

        //publish image pyramid
        try {

            //Pubblico il layer
            String slLayerName = sFileName;
            oManager.publishStandardGeoTiff(sWorkspace, sStoreName, oFile, sEPSG, sStyle);

            //configure coverage
            GSCoverageEncoder ce = new GSCoverageEncoder();
            ce.setEnabled(true); //abilito il coverage
            ce.setSRS(sEPSG);

            boolean exists = oManager.getReader().existsCoveragestore(sWorkspace, sStoreName);
            if (exists)
                exists = oManager.getReader().existsCoverage(sWorkspace, sStoreName, slLayerName);
            if(exists)
                oManager.getPublisher().configureCoverage(ce, sWorkspace, sStoreName, slLayerName);
        }
        catch (Exception oEx)
        {
            oEx.printStackTrace();
        }

        return sStoreName;

    }

    public String publishGeoTiff(String sFileName, String sGeoServerAddress, String sGeoServerUser, String sGeoServerPassword, String sWorkspace, String sStore) throws Exception {
        return publishGeoTiff(sFileName, sGeoServerAddress, sGeoServerUser, sGeoServerPassword, sWorkspace, sStore, "EPSG:4326");
    }

    public String publishGeoTiff(String sFileName, String sGeoServerAddress, String sGeoServerUser, String sGeoServerPassword, String sWorkspace, String sStore, String sEPSG) throws Exception {
        return publishGeoTiff(sFileName, sGeoServerAddress, sGeoServerUser, sGeoServerPassword, sWorkspace, sStore, sEPSG,"raster");
    }

    public String publishGeoTiff(String sFileName, String sGeoServerAddress, String sGeoServerUser, String sGeoServerPassword, String sWorkspace, String sStore, String sEPSG, String sStyle) throws Exception {

        // Domain Check

        if (Utils.isNullOrEmpty(sFileName)) return  "";
        if (Utils.isNullOrEmpty(sGeoServerAddress)) return "";
        if (Utils.isNullOrEmpty(sWorkspace)) return "";
        if (Utils.isNullOrEmpty(sStore)) return  "";

        File oFile = new File(sFileName);
        if (oFile.exists()==false) return "";

        long lFileLenght = oFile.length();
        //long lMaxSize = 2L*1024L*1024L*1024L;
        long lMaxSize = 50L*1024L*1024L;

        // More than Gb => Pyramid, otherwise normal geotiff
        if (lFileLenght> lMaxSize) return this.PublishImagePyramidOnGeoServer(sFileName, sGeoServerAddress, sGeoServerUser, sGeoServerPassword, sWorkspace, sStore);
        else  return this.PublishGeoTiffImage(sFileName,sGeoServerAddress,sGeoServerUser,sGeoServerPassword,sWorkspace,sStore, sEPSG);
    }
}
