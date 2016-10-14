package wasdi.snapopearations;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dataio.bigtiff.BigGeoTiffProductReaderPlugIn;

import java.io.File;

/**
 * Created by s.adamo on 24/05/2016.
 */
public class WriteProduct {

    public void WriteBigTiff(Product terrainProduct, String sFilePath, String sFileName, String format) throws Exception
    {
        if (format == null)
            format = BigGeoTiffProductReaderPlugIn.FORMAT_NAME;
        File newFile = new File(sFilePath + sFileName + ".tif");
        ProductIO.writeProduct(terrainProduct, newFile.getAbsolutePath(), format);
    }
}
