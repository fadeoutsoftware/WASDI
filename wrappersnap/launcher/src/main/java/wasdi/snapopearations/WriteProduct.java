package wasdi.snapopearations;

import java.io.File;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.dimap.DimapProductWriterPlugIn;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dataio.bigtiff.BigGeoTiffProductReaderPlugIn;
import org.esa.snap.engine_utilities.util.MemUtils;

import wasdi.LauncherMain;

/**
 * Created by s.adamo on 24/05/2016.
 */
public class WriteProduct {

    public String WriteBigTiff(Product oProduct, String sFilePath, String sFileName) throws Exception
    {
        String sFormat = BigGeoTiffProductReaderPlugIn.FORMAT_NAME;

        return WriteProduct(oProduct, sFilePath, sFileName, sFormat, ".tif");
    }

    public String WriteBEAMDIMAP(Product oProduct, String sFilePath, String sFileName) throws Exception
    {
        String sFormat = DimapProductWriterPlugIn.DIMAP_FORMAT_NAME;

        return WriteProduct(oProduct, sFilePath, sFileName, sFormat, ".dim");
    }

    public String WriteGeoTiff(Product oProduct, String sFilePath, String sFileName) throws Exception
    {
        return WriteProduct(oProduct, sFilePath, sFileName, "GeoTIFF", ".tif");
    }

    public String WriteProduct(Product oProduct, String sFilePath, String sFileName, String sFormat, String sExtension)
    {
        try {
            if (!sFilePath.endsWith("/")) sFilePath += "/";
            File newFile = new File(sFilePath + sFileName + sExtension);
            LauncherMain.s_oLogger.debug("WriteProduct: Output File: " + newFile.getAbsolutePath());
            ProductIO.writeProduct(oProduct, newFile.getAbsolutePath(), sFormat);
            MemUtils.freeAllMemory();
            return newFile.getAbsolutePath();
        }
        catch (Exception oEx)
        {
        	oEx.printStackTrace();
            LauncherMain.s_oLogger.debug("WriteProduct: Error writing product. " + oEx.getMessage());
        }

        return null;
    }



}
