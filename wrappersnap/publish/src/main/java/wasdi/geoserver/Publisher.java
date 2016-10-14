package wasdi.geoserver;

import it.geosolutions.geoserver.rest.encoder.coverage.GSCoverageEncoder;

import org.geotools.data.DataUtilities;
import org.geotools.factory.Hints;
import org.geotools.gce.imagepyramid.ImagePyramidFormat;
import org.geotools.referencing.crs.DefaultGeographicCRS;


import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Created by s.adamo on 24/05/2016.
 */
public class Publisher {

    public static final int LEVEL = 4;

    public static final int WIDTH = 2048;

    public static final int HEIGHT = 2048;

    public static String TARGET_DIR_BASE = "c:\\temp\\ImagePyramidTest\\";

    public static String TARGET_DIR_PYRAMID = "c:\\temp\\ImagePyramidTest\\TempImagePyramidCreation\\";

    public String GDALBasePath = "\"C:\\Program Files\\GDAL\\bin\\gdal\\python\\scripts\\gdal_retile.py\"";

    public Publisher()
    {

    }

    public Publisher(String sPyramidBaseFolder, String sGDALBasePath)
    {
        TARGET_DIR_BASE = sPyramidBaseFolder;
        GDALBasePath = sGDALBasePath;
    }


    /*

     */
    private void LaunchImagePyramidCreation(String sInputFile, Integer iLevel, Integer iWidth, Integer iHeight, String sPathName) {

        String sTargetDir = sPathName;
        Path oTargetPath = Paths.get(sTargetDir);
        if (!Files.exists(oTargetPath))
            try {
                Files.createDirectory(oTargetPath);

                String sCmd = String.format("python %s -v -r bilinear -levels %d -ps %s %s -co \"TILED=YES\" -targetDir %s %s", GDALBasePath, iLevel, iWidth, iHeight, sTargetDir, sInputFile);
                String[] envp = {"PYTHONPATH=C:\\Program Files\\GDAL\\bin\\gdal\\python", "PROJ_LIB=C:\\Program Files\\GDAL\\bin\\proj\\SHARE", "GDAL_DATA=C:\\Program Files\\GDAL\\bin\\gdal-data",
                        "GDAL_DRIVER_PATH=C:\\Program Files\\GDAL\\bin\\gdal\\plugins", "PATH=C:\\Program Files\\GDAL\\bin;C:\\Program Files\\GDAL\\bin\\gdal\\python\\osgeo;C:\\Program Files\\GDAL\\bin\\proj\\apps;C:\\Program Files\\GDAL\\bin\\gdal\\apps;" +
                        "C:\\Program Files\\GDAL\\bin\\ms\\apps;C:\\Program Files\\GDAL\\bin\\gdal\\csharp;C:\\Program Files\\GDAL\\bin\\ms\\csharp;C:\\Program Files\\GDAL\\bin\\curl;C:\\Python34"};

                try {

                    Process p;
                    Runtime oRunTime = Runtime.getRuntime();
                    p = oRunTime.exec(sCmd, envp);
                    InputStream stdin = p.getInputStream();
                    InputStreamReader isr = new InputStreamReader(stdin);
                    BufferedReader br = new BufferedReader(isr);
                    String line = null;
                    System.out.println("<OUTPUT>");
                    while ((line = br.readLine()) != null)
                        System.out.println(line);
                    System.out.println("</OUTPUT>");
                    int exitVal = p.waitFor();
                    p.destroy();


                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            } catch (IOException e) {
                e.printStackTrace();
                return;
            }


    }

    private String PublishImagePyramidOnGeoServer(String sFileName, String sGeoServerAddress, String sGeoServerUser, String sGeoServerPassword, String sWorkspace, String sStoreName) throws Exception {

        LaunchImagePyramidCreation(sFileName, LEVEL, WIDTH, HEIGHT, TARGET_DIR_BASE);

        //pubblico su geoserver
        GeoServerManager manager = new GeoServerManager(sGeoServerAddress, sGeoServerUser, sGeoServerPassword);

        if (!manager.getReader().existsWorkspace(sWorkspace)) {
            manager.getPublisher().createWorkspace(sWorkspace);
        }

        //publish image pyramid
        try {

            //creo le immagini piramidali
            // now make sure we can actually rebuild the mosaic
            final URL testFile = new URL("file:" + TARGET_DIR_PYRAMID + sFileName);
            File sourceDir = new File(DataUtilities.urlToFile(testFile).getPath());
            ImagePyramidFormat format = new ImagePyramidFormat();
            final Hints hints = new Hints(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, DefaultGeographicCRS.WGS84);

            //final ImagePyramidReader reader = format.getReader(sourceDir, hints);

            //Pubblico il layer
            String layerName = sFileName;
            manager.publishImagePyramid(sWorkspace, sStoreName, sourceDir);

            //configure coverage
            GSCoverageEncoder ce = new GSCoverageEncoder();
            ce.setEnabled(true); //abilito il coverage
            ce.setSRS("EPSG:4326");
            boolean exists = manager.getReader().existsCoveragestore(sWorkspace, sStoreName);
            if (exists)
                exists = manager.getReader().existsCoverage(sWorkspace, sStoreName, layerName);
            if(exists)
                manager.getPublisher().configureCoverage(ce, sWorkspace, sStoreName, layerName);
        }catch (Exception oEx){}

        return sFileName;

    }

    public String publishImage(String sFileName, String sGeoServerAddress, String sGeoServerUser, String sGeoServerPassword, String sWorkspace, String sStore) throws Exception {

        return this.PublishImagePyramidOnGeoServer(sFileName, sGeoServerAddress, sGeoServerUser, sGeoServerPassword, sWorkspace, sStore);

    }
}
