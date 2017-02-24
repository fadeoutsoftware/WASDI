import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.graphbuilder.rcp.dialogs.support.GraphExecuter;
import search.SentinelInfo;

import java.beans.XMLEncoder;
import java.io.*;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

/**
 * Created by s.adamo on 20/02/2017.
 */
public class SentinelThread implements Callable<Boolean> {

    private String m_sDownloadLink;

    private GraphExecuter m_oGraphEx;

    private SentinelInfo m_oSentinelInfo;


    public SentinelThread(SentinelInfo oInfo)
    {
        m_oGraphEx = new GraphExecuter();
        m_sDownloadLink = oInfo.getDownloadLink();
        m_oSentinelInfo = oInfo;
    }


    @Override
    public Boolean call() throws Exception {

        String sReturnFilePath = ExecuteDownloadFile(m_sDownloadLink, ConfigReader.getPropValue("DOWNLOAD_PATH"));
        Main.s_oLogger.debug(String.format("SentinelTread.call: Downloaded file %s", sReturnFilePath));
        //read product
        Product oProduct = ProductIO.readProduct(new File(sReturnFilePath));
        //get centerscene
        GeoPos oCenterGeoPo = ProductUtils.getCenterGeoPos(oProduct);
        Main.s_oLogger.debug(String.format("SentinelTread.call: Center Scene lat %s, Center Scene lon %s for product %s", String.valueOf(oCenterGeoPo.getLat()), String.valueOf(oCenterGeoPo.getLonString()),oProduct.getName() ));
        m_oSentinelInfo.setSceneCenterLat(String.valueOf(oCenterGeoPo.getLat()));
        m_oSentinelInfo.setSceneCenterLon(String.valueOf(oCenterGeoPo.getLonString()));
        String sOutputElaborationPath = ConfigReader.getPropValue("MULESME_WASDI_PATH") + oProduct.getName() + "_TC";
        //load graph.xml
        Main.s_oLogger.debug(String.format("SentinelTread.call: Destination output graph %s", sOutputElaborationPath));
        UpdateGraphXml(sReturnFilePath, sOutputElaborationPath);
        //execute graph
        m_oGraphEx.executeGraph(ProgressMonitor.NULL);
        //Serialize SentinelInfo
        SerializeObjectToXML(ConfigReader.getPropValue("MULESME_WASDI_PATH"), m_oSentinelInfo);
        Main.s_oLogger.debug(String.format("SentinelTread.call: Serialized object on path %s", ConfigReader.getPropValue("MULESME_WASDI_PATH")));
        //move file
        SARToMulesmeFormat(oProduct.getName(), sOutputElaborationPath, ConfigReader.getPropValue("MULESME_WASDI_PATH"));
        Main.s_oLogger.debug(String.format("SentinelTread.call: moved file %s from %s to %s", oProduct.getName(), sOutputElaborationPath, ConfigReader.getPropValue("MULESME_WASDI_PATH")));
        return new Boolean(true);
    }

    private void SerializeObjectToXML(String xmlFileLocation, Object objectToSerialize) throws Exception {
        FileOutputStream os = new FileOutputStream(xmlFileLocation);
        XMLEncoder encoder = new XMLEncoder(os);
        encoder.writeObject(objectToSerialize);
        encoder.close();
    }

    private String SARToMulesmeFormat(String sSentinelProductName, String sLocationSourcePath, String sLocationDestination) throws Exception {

        String sMulesmeFormat = "";
        String[] sSplittedName = sSentinelProductName.split("_");
        String sSatellitePlatform = sSplittedName[0];
        String sSensorMode = sSplittedName[1];
        String sProductType = sSplittedName[2];
        String sLevelClassPol = sSplittedName[3];
        String sStartTime = sSplittedName[4];
        String sEndTime = sSplittedName[5];
        String sAbsoluteOrbit = sSplittedName[6];
        String sMissionData = sSplittedName[7];
        String sProductUniqId = sSplittedName[8];

        String sPrefix = sSatellitePlatform + "_" + sStartTime + "_" + sProductUniqId;
        String sSuffix = "grd_pwr_geo_db";

        File oFolder = new File(sLocationSourcePath);
        File[] aoListOfFiles = oFolder.listFiles();
        for (File oFile :
                aoListOfFiles) {
            String sFileName = oFile.getName();
            String sExtension = FilenameUtils.getExtension(sFileName);
            String[] asFileInfo = sFileName.split("_");
            String sPolarization = asFileInfo[1];
            sMulesmeFormat = sPrefix + "_" + sPolarization + "_" + sSuffix + "." + sExtension;
            Main.s_oLogger.debug(String.format("Move %s to %s", sLocationSourcePath + sSentinelProductName, sLocationDestination + sMulesmeFormat));
            Files.move(Paths.get(sLocationSourcePath + sSentinelProductName), Paths.get(sLocationDestination + sMulesmeFormat));
        }
        Main.s_oLogger.debug(String.format("Deleting %s ", sLocationSourcePath));
        Files.delete(Paths.get(sLocationSourcePath));

        return sMulesmeFormat;

    }

    private void UpdateGraphXml(String sInputFilePath, String sOutputFilePath)
    {

        File oFile = null;
        try {
            oFile = new File(ConfigReader.getPropValue("GRAPH_FILE"));
        } catch (IOException oEx) {
            oEx.printStackTrace();
            Main.s_oLogger.debug("SentinelThread.UpdateGraphXml: Error opening Graph file. " +  oEx.getMessage());
        }
        try {

            String fileContext = FileUtils.readFileToString(oFile, "UTF-8");
            fileContext = fileContext.replace("{InputFile}", sInputFilePath);
            fileContext = fileContext.replace("{OutputFile}", sOutputFilePath);
            InputStream in = IOUtils.toInputStream(fileContext, "UTF-8");
            m_oGraphEx.loadGraph(in, oFile, false);
            m_oGraphEx.InitGraph();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GraphException e) {
            e.printStackTrace();
        }


    }

    private String ExecuteDownloadFile(String sFileURL, String sSaveDirOnServer) throws IOException {

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
                    return new PasswordAuthentication("sadamo", "***REMOVED***".toCharArray());
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
            String saveFilePath= sSaveDirOnServer + "/" + fileName;

            if (Files.exists(Paths.get(saveFilePath)))
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
}
