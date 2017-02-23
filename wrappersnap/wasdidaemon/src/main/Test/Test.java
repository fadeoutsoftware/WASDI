import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.graphbuilder.rcp.dialogs.support.GraphExecuter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by s.adamo on 21/02/2017.
 */
public class Test {
    public static void main(String[] args) throws Exception {

        UpdateGraphXml();
    }

    private static void UpdateGraphXml()
    {

        File oFile = new File("c:\\temp\\wasdi\\Graph.xml");
        try {
            GraphExecuter m_oGraphEx = new GraphExecuter();
            String fileContext = FileUtils.readFileToString(oFile, "UTF-8");
            Product oProduct = ProductIO.readProduct(new File("C:\\temp\\wasdi\\S1A_IW_GRDH_1SDV_20160802T051857_20160802T051922_012417_013615_C75B.zip"));
            fileContext = fileContext.replace("{InputFile}", "C:\\temp\\wasdi\\S1A_IW_GRDH_1SDV_20160802T051857_20160802T051922_012417_013615_C75B.zip");
            fileContext = fileContext.replace("{OutputFile}", "C:\\temp\\wasdi\\" + oProduct.getName());
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
        }


    }
}
