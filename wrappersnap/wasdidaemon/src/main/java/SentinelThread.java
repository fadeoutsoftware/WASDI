import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.esa.s1tbx.calibration.gpf.CalibrationOp;
import org.esa.s1tbx.sar.gpf.MultilookOp;
import org.esa.s1tbx.sar.gpf.geometric.RangeDopplerGeocodingOp;
import org.esa.s1tbx.sar.gpf.orbits.ApplyOrbitFileOp;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.dataio.envi.EnviProductWriterPlugIn;
import org.esa.snap.engine_utilities.util.MemUtils;
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

    private GraphExecuter m_oGraphEx;

    private SentinelInfo m_oSentinelInfo;

    private String m_sProductFilePath;

    public SentinelThread(String sProductFilePath)
    {
        m_sProductFilePath = sProductFilePath;
    }


    @Override
    public Boolean call() throws Exception {

        //read product
        Product oProduct = ProductIO.readProduct(new File(m_sProductFilePath));
        if (oProduct == null)
        {
            //try to download
            m_sProductFilePath = Main.ExecuteDownloadFile(getSentinelInfo().getDownloadLink(), ConfigReader.getPropValue("DOWNLOAD_PATH"), true);
            Main.s_oLogger.debug(String.format("SentinelTread.call: Downloaded file %s", m_sProductFilePath));
            //read product
            oProduct = ProductIO.readProduct(new File(m_sProductFilePath));
            if (oProduct == null)
                return false;
        }
        Main.s_oLogger.debug(String.format("SentinelTread.call: Product %s has been read", m_sProductFilePath));

        /*
        String sMapProjection = null;
        CoordinateReferenceSystem targetCRS = null;
        CRSGeoCodingHandler crsHandler = null;


        try {
            crsHandler = new CRSGeoCodingHandler(oProduct, "WGS84(DD)",
                    20, 20);


            if (crsHandler == null) {
                Main.s_oLogger.debug("SentinelTread.call: crsHandler is null");
                return false;
            }
            Main.s_oLogger.debug(String.format("SentinelTread.call: crsHandler ok", sReturnFilePath));
            targetCRS = crsHandler.getTargetCRS();
            if (targetCRS == null) {
                Main.s_oLogger.debug("SentinelTread.call: targetCRS is null");
                return false;
            }
        }
        catch(Exception oEX){

        }

        if(crsHandler != null)
            sMapProjection = targetCRS.toString();
        */

        //get centerscene
        GeoPos oCenterGeoPo = ProductUtils.getCenterGeoPos(oProduct);
        Main.s_oLogger.debug(String.format("SentinelTread.call: Center Scene lat %s, Center Scene lon %s for product %s", String.valueOf(oCenterGeoPo.getLat()), String.valueOf(oCenterGeoPo.getLon()),oProduct.getName() ));
        getSentinelInfo().setSceneCenterLat(String.valueOf(oCenterGeoPo.getLat()));
        getSentinelInfo().setSceneCenterLon(String.valueOf(oCenterGeoPo.getLon()));
        String sOutputElaborationPath = ConfigReader.getPropValue("MULESME_WASDI_PATH") + oProduct.getName() + "_TC";
        Main.s_oLogger.debug(String.format("SentinelTread.call: Execute operation " + sOutputElaborationPath));
        ExecuteSNAPOperation(oProduct, sOutputElaborationPath);
        //UpdateGraphXml(sReturnFilePath, sOutputElaborationPath, null);
        //execute graph
        //Main.s_oLogger.debug(String.format("SentinelTread.call: Execute graph for " + sOutputElaborationPath));
        //getGraphEx().executeGraph(ProgressMonitor.NULL);
        //move file
        SARToMulesmeFormat(oProduct.getName(), sOutputElaborationPath, ConfigReader.getPropValue("MULESME_WASDI_PATH"));
        Main.s_oLogger.debug(String.format("SentinelTread.call: moved file %s from %s to %s", oProduct.getName(), sOutputElaborationPath, ConfigReader.getPropValue("MULESME_WASDI_PATH")));
        return new Boolean(true);
    }


    public void ExecuteSNAPOperation(Product oSource, String sOutputFilePath) {
        String sEnviFormatOutput = EnviProductWriterPlugIn.FORMAT_NAME;

        //Apply Orbit
        OperatorSpi oApplyOrbitFileOp = new ApplyOrbitFileOp.Spi();
        ApplyOrbitFileOp oApplyOrbitOperator = (ApplyOrbitFileOp) oApplyOrbitFileOp.createOperator();
        oApplyOrbitOperator.setSourceProduct(oSource);
        Product oOrbitProduct = oApplyOrbitOperator.getTargetProduct();

        //calibration
        OperatorSpi spi = new CalibrationOp.Spi();
        Operator op = spi.createOperator();
        op.setSourceProduct(oOrbitProduct);
        Product oCalibrateProduct = op.getTargetProduct();

        //Multilooking
        OperatorSpi spiMulti = new MultilookOp.Spi();
        MultilookOp opMulti = (MultilookOp) spiMulti.createOperator();
        opMulti.setSourceProduct(oCalibrateProduct);
        opMulti.setNumRangeLooks(2);
        opMulti.setNumAzimuthLooks(2);
        MultilookOp.DerivedParams param = new MultilookOp.DerivedParams();
        param.nRgLooks = 2;
        param.nAzLooks = 2;
        try {
            opMulti.getDerivedParameters(oCalibrateProduct, param);//filterProduct
        } catch (Exception e) {
            e.printStackTrace();
        }
        Product oMultiProduct = opMulti.getTargetProduct();

        //Terrain Correction
        OperatorSpi spiTerrain = new RangeDopplerGeocodingOp.Spi();
        RangeDopplerGeocodingOp opTerrain = (RangeDopplerGeocodingOp) spiTerrain.createOperator();
        opTerrain.setSourceProduct(oMultiProduct);
        opTerrain.setParameter("pixelSpacingInMeter", 20.0);
        opTerrain.setParameter("mapProjection", "EPSG:32632"); //UTM Zone 32
        Product oTerrainProduct = opTerrain.getTargetProduct();

        try {
            ProductIO.writeProduct(oTerrainProduct, sOutputFilePath, sEnviFormatOutput);
            MemUtils.freeAllMemory();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }





    public String SARToMulesmeFormat(String sSentinelProductName, String sLocationSourcePath, String sLocationDestination) throws Exception {

        if (!sLocationSourcePath.endsWith("/"))
            sLocationSourcePath += "/";

        if (!sLocationDestination.endsWith("/"))
            sLocationDestination += "/";

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
            String sFileNameWithExtension = oFile.getName();
            String sFileName = FilenameUtils.getBaseName(sFileNameWithExtension);
            String sExtension = FilenameUtils.getExtension(sFileNameWithExtension);
            String[] asFileInfo = sFileName.split("_");
            String sPolarization = asFileInfo[1];
            if (!sExtension.equals("img"))
                sMulesmeFormat = sPrefix + "_" + sPolarization + "_" + sSuffix + "." + sExtension;
            else
                sMulesmeFormat = sPrefix + "_" + sPolarization + "_" + sSuffix;
            String sSentinelInfoFileName = sPrefix + "_" + sPolarization + "_" + sSuffix + ".sml";
            //Serialize SentinelInfo
            Utils.SerializeObjectToXML(sLocationDestination + sSentinelInfoFileName, getSentinelInfo());
            Main.s_oLogger.debug(String.format("SentinelTread.call: Serialized object on path %s", ConfigReader.getPropValue("MULESME_WASDI_PATH")));
            //Move files
            Main.s_oLogger.debug(String.format("Move %s to %s", sLocationSourcePath + sSentinelProductName, sLocationDestination + sMulesmeFormat));
            Files.move(Paths.get(sLocationSourcePath + sFileNameWithExtension), Paths.get(sLocationDestination + sMulesmeFormat));
        }
        Main.s_oLogger.debug(String.format("Deleting %s ", sLocationSourcePath));
        Files.delete(Paths.get(sLocationSourcePath));

        return sMulesmeFormat;

    }

    private void UpdateGraphXml(String sInputFilePath, String sOutputFilePath, String sMapProjection)
    {

        File oFile = null;
        try {
            oFile = new File(ConfigReader.getPropValue("GRAPH_FILE"));
        } catch (IOException oEx) {
            oEx.printStackTrace();
            Main.s_oLogger.debug("SentinelThread.UpdateGraphXml: Error opening Graph file. " +  oEx.getMessage());
        }
        try {

            Main.s_oLogger.debug("SentinelThread.UpdateGraphXml: Init Graph for " + sOutputFilePath);
            String fileContext = FileUtils.readFileToString(oFile, "UTF-8");
            fileContext = fileContext.replace("{InputFile}", sInputFilePath);
            fileContext = fileContext.replace("{OutputFile}", sOutputFilePath);
            if (sMapProjection  != null && sMapProjection != "")
                fileContext = fileContext.replace("{MPROJ}", sMapProjection);
            InputStream in = IOUtils.toInputStream(fileContext, "UTF-8");
            getGraphEx().loadGraph(in, oFile, false,true);
            getGraphEx().InitGraph();
            Main.s_oLogger.debug("SentinelThread.UpdateGraphXml: Graph Initialized");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Main.s_oLogger.debug("SentinelThread.UpdateGraphXml: Error Initializing graph." + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            Main.s_oLogger.debug("SentinelThread.UpdateGraphXml: Error Initializing graph." + e.getMessage());
        } catch (GraphException e) {
            e.printStackTrace();
            Main.s_oLogger.debug("SentinelThread.UpdateGraphXml: Error Initializing graph." + e.getMessage());
        }


    }



    public GraphExecuter getGraphEx() {
        return m_oGraphEx;
    }

    public void setGraphEx(GraphExecuter m_oGraphEx) {
        this.m_oGraphEx = m_oGraphEx;
    }

    public SentinelInfo getSentinelInfo() {
        return m_oSentinelInfo;
    }

    public void setSentinelInfo(SentinelInfo m_oSentinelInfo) {
        this.m_oSentinelInfo = m_oSentinelInfo;
    }
}
