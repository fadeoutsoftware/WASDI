package wasdi.snapopearations;

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.geotiff.GeoCoding2GeoTIFFMetadata;
import org.esa.snap.core.util.geotiff.GeoTIFF;
import org.esa.snap.core.util.geotiff.GeoTIFFMetadata;
import org.esa.snap.dataio.bigtiff.BigGeoTiffProductReaderPlugIn;
import org.esa.snap.engine_utilities.util.MemUtils;
import wasdi.LauncherMain;

import java.io.*;

/**
 * Created by s.adamo on 24/05/2016.
 */
public class WriteProduct {

    public String WriteBigTiff(Product oProduct, String sFilePath, String sFileName, String format) throws Exception
    {
        if (format == null) {
            format = BigGeoTiffProductReaderPlugIn.FORMAT_NAME;
        }
        LauncherMain.s_oLogger.debug("WriteProduct: Format: " + format);
        if (!sFilePath.endsWith("/")) sFilePath += "/";
        File newFile = new File(sFilePath + sFileName + ".tif");
        LauncherMain.s_oLogger.debug("WriteProduct: Otuput File: " + newFile.getAbsolutePath());
        ProductIO.writeProduct(oProduct, newFile.getAbsolutePath(), format);
        MemUtils.freeAllMemory();
        return newFile.getAbsolutePath();
    }

    public String WriteBigTiff(Product oProduct, String sLayerId, String sPath) throws Exception
    {
        String sBandName = oProduct.getBandAt(0).getName();
        sLayerId += "_" + sBandName;
        String sTiffFile = sPath + sLayerId + ".tif";
        LauncherMain.s_oLogger.debug("LauncherMain.publish: get geocoding ");
        GeoCoding oGeoCoding = oProduct.getSceneGeoCoding();
        LauncherMain.s_oLogger.debug("LauncherMain.publish: get band image");
        Band oBand = oProduct.getBand(oProduct.getBandAt(0).getName());
        MultiLevelImage oBandImage = oBand.getSourceImage();
        LauncherMain.s_oLogger.debug("LauncherMain.publish: get metadata");
        GeoTIFFMetadata oMetadata = GeoCoding2GeoTIFFMetadata.createGeoTIFFMetadata(oGeoCoding, oBandImage.getWidth(),oBandImage.getHeight());
        GeoTIFF.writeImage(oBandImage, new File(sTiffFile), oMetadata);
        return sTiffFile;

    }

    public void DumpProduct(Product oProduct, String sPathName) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        FileOutputStream fos = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(oProduct);
            out.flush();
            byte[] abProduct = bos.toByteArray();

            //dump to file
            fos = new FileOutputStream(sPathName);
            fos.write(abProduct);
            fos.close();

        } catch (IOException ex) {
            // ignore close exception
        }
        finally {
            bos.close();
            fos.close();
        }

    }


}
