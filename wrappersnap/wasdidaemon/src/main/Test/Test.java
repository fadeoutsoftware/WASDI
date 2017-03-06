import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.esa.s1tbx.sar.gpf.geometric.CRSGeoCodingHandler;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.graphbuilder.rcp.dialogs.support.GraphExecuter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import search.SentinelInfo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by s.adamo on 21/02/2017.
 */
public class Test {
    public static void main(String[] args) throws Exception {

        //UpdateGraphXml();
        //Rename();
        //Read();
        Operation();
    }

    private static void Read()
    {
        try {
            Product oProduct = ProductIO.readProduct(new File("C:\\Users\\s.adamo\\.snap\\auxdata\\dem\\SRTM 3Sec\\srtm_39_04.zip"));
            System.out.println("oProduct.getSceneGeoCoding().toString()");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void UpdateGraphXml()
    {

        File oFile = new File("c:\\temp\\wasdi\\Graph.xml");
        try {
            GraphExecuter m_oGraphEx = new GraphExecuter();
            String fileContext = FileUtils.readFileToString(oFile, "UTF-8");
            Product oProduct = ProductIO.readProduct(new File("C:/Users/s.adamo/Documents/S1B_IW_GRDH_1SDV_20170302T050413_20170302T050438_004525_007E0D_0EB9.zip"));
            final CRSGeoCodingHandler crsHandler = new CRSGeoCodingHandler(oProduct, "AUTO:42001", 20, 20);
            CoordinateReferenceSystem targetCRS = crsHandler.getTargetCRS();
            fileContext = fileContext.replace("{InputFile}", "C:/Users/s.adamo/Documents/S1B_IW_GRDH_1SDV_20170302T050413_20170302T050438_004525_007E0D_0EB9.zip");
            fileContext = fileContext.replace("{OutputFile}", "C:\\temp\\wasdi\\" + oProduct.getName());
            fileContext = fileContext.replace("{MPROJ}", targetCRS.toString());
            InputStream in = IOUtils.toInputStream(fileContext, "UTF-8");
            m_oGraphEx.loadGraph(in, oFile, false);
            m_oGraphEx.InitGraph();
            m_oGraphEx.executeGraph(ProgressMonitor.NULL);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GraphException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private static void Operation() throws IOException {
        Product oProduct = ProductIO.readProduct(new File("C:/Users/s.adamo/Documents/S1B_IW_GRDH_1SDV_20170302T050413_20170302T050438_004525_007E0D_0EB9.zip"));
        SentinelThread othread = new SentinelThread("C:/Users/s.adamo/Documents/S1B_IW_GRDH_1SDV_20170302T050413_20170302T050438_004525_007E0D_0EB9.zip");
        othread.ExecuteSNAPOperation(oProduct, "C:\\temp\\wasdi\\Mulesme\\S1A_IW_GRDH_1SDV_20170222T051945_20170222T052010_015392_019421_6625_TC\\");



    }

    private static void Rename()
    {
        SentinelInfo oSentinelInfo = new SentinelInfo();
        oSentinelInfo.setSceneCenterLat("45.8");
        oSentinelInfo.setSceneCenterLon("8.6");
        oSentinelInfo.setOrbit("44");
        oSentinelInfo.setFileName("Test.txt");
        oSentinelInfo.setDownloadLink("www.prova.it");
        SentinelThread oSentinelThread = new SentinelThread(null);
        oSentinelThread.setSentinelInfo(oSentinelInfo);
        try {
            oSentinelThread.SerializeObjectToXML("C:\\temp\\wasdi\\Mulesme\\", oSentinelInfo);
            oSentinelThread.SARToMulesmeFormat("S1A_IW_GRDH_1SDV_20170222T051945_20170222T052010_015392_019421_6625", "C:\\temp\\wasdi\\Mulesme\\S1A_IW_GRDH_1SDV_20170222T051945_20170222T052010_015392_019421_6625_TC\\", "C:\\temp\\wasdi\\Mulesme\\");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
