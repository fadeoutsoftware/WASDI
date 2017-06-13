
//import publish.Publisher;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;

import org.esa.s1tbx.calibration.gpf.CalibrationOp;
import org.esa.s1tbx.sar.gpf.MultilookOp;
import org.esa.s1tbx.sar.gpf.filtering.SpeckleFilterOp;
import org.esa.s1tbx.sar.gpf.geometric.RangeDopplerGeocodingOp;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.dataio.bigtiff.BigGeoTiffProductReaderPlugIn;

import com.sun.medialib.codec.jp2k.Params;

import wasdi.LauncherMain;
import wasdi.shared.parameters.IngestFileParameter;
import wasdi.shared.viewmodels.ProductViewModel;
import wasdi.snapopearations.ReadProduct;

/**
 * Created by s.adamo on 18/05/2016.
 */
public class Test {

	public static void main(String[] args) throws Exception {
		
		LauncherMain theMain = new  LauncherMain();
		IngestFileParameter params = new IngestFileParameter();
		params.setUserId("paolo");
		params.setExchange("8e91a84c-3dcf-470d-8e36-3ad40de80d54");
		params.setWorkspace("8e91a84c-3dcf-470d-8e36-3ad40de80d54");
		params.setProcessObjId("PPPP");
		params.setFilePath("/home/doy/tmp/wasdi/prova_snap/S1A_IW_GRDH_1SDV_20160802T051857_20160802T051922_012417_013615_C75B.zip");
		theMain.Ingest(params, "/home/doy/tmp/wasdi/data/download");
		
		
    }



}
