
//import publish.Publisher;
import it.fadeout.snapopearations.ReadProduct;
import it.fadeout.viewmodels.ProductViewModel;
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
import org.esa.snap.engine_utilities.util.MemUtils;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

/**
 * Created by s.adamo on 18/05/2016.
 */
public class Test {
    public static void main(String[] args) throws Exception {

        /*---Test-------------------------------*/

        String sPath = "C:\\temp\\ImagePyramidTest\\Dati Sentinel\\";
        //String sPath = "C:\\temp\\";

        String sName = "S1A_IW_GRDH_1SDV_20160217T170557_20160217T170622_009989_00EAE9_507D.zip";

        ReadProduct oRead = new ReadProduct();
        ProductViewModel oProductViewModel = oRead.getProduct(new File(sPath+sName));

        //oRead.writeBigTiff(sName);

     /*   Publisher oPublisher = new Publisher();


        oPublisher.publishImage(sName + ".tif");*/

        /*-------------------------------------*/

        final JFileChooser fc = new JFileChooser();

        //In response to a button click:
        int returnVal = fc.showOpenDialog(null);

        if (returnVal == JFileChooser.APPROVE_OPTION) {

            String formatName = "SENTINEL-1";
            File file = fc.getSelectedFile();
            //This is where a real application would open the file.
            Product exportProduct = null;
            Product terrainProduct = null;
            try {
                //read product
                exportProduct = ProductIO.readProduct(file, formatName);
                //calibration
                OperatorSpi spi = new CalibrationOp.Spi();
                Operator op = spi.createOperator();
                op.setSourceProduct(exportProduct);
                Product calibrateProduct = op.getTargetProduct();

                String bigGeoTiffFormatName = BigGeoTiffProductReaderPlugIn.FORMAT_NAME;

                //File calFile = new File("C:\\Users\\s.adamo\\Documents\\test_cal.tif");
                File calFile = new File("C:\\Users\\a.corrado\\Documents\\test_cal.tif");
                ProductIO.writeProduct(calibrateProduct, calFile.getAbsolutePath(), bigGeoTiffFormatName);
                //System.setProperty("com.sun.media.jai.disableMediaLib", "true");

                MemUtils.freeAllMemory();
               /* String bigGeoTiffFormatName = BigGeoTiffProductReaderPlugIn.FORMAT_NAME;
                Calibration oCalibration= new Calibration();
                Product calibrateProduct=oCalibration.getCalibration(exportProduct,"C:\\Users\\a.corrado\\Documents\\");
                */
                exportProduct = ProductIO.readProduct(calFile, bigGeoTiffFormatName);

                //filter
                OperatorSpi spiFilter = new SpeckleFilterOp.Spi();
                SpeckleFilterOp opFilter = (SpeckleFilterOp) spiFilter.createOperator();
                opFilter.setSourceProduct(calibrateProduct);
                opFilter.SetFilter("Refined Lee");
                Product filterProduct = opFilter.getTargetProduct();

                //File filterFile = new File("C:\\Users\\s.adamo\\Documents\\test_filter.tif");
                File filterFile = new File("C:\\Users\\a.corrado\\Documents\\test_filter.tif");
                //ProductIO.writeProduct(filterProduct, filterFile.getAbsolutePath(), bigGeoTiffFormatName)<
                ProductIO.writeProduct(calibrateProduct, filterFile.getAbsolutePath(), bigGeoTiffFormatName);
                MemUtils.freeAllMemory();
                exportProduct = ProductIO.readProduct(filterFile, bigGeoTiffFormatName);

                //Multilooking
                OperatorSpi spiMulti = new MultilookOp.Spi();
                MultilookOp opMulti = (MultilookOp) spiMulti.createOperator();
                opMulti.setSourceProduct(filterProduct);
                opMulti.setNumRangeLooks(4);
                MultilookOp.DerivedParams param = new MultilookOp.DerivedParams();
                param.nRgLooks = 4;
                MultilookOp.getDerivedParameters(filterProduct, param);
                //opMulti.setNumAzimuthLooks(param.nAzLooks);


                Product multiProduct = opMulti.getTargetProduct();
                File multiFile = new File("C:\\Users\\a.corrado\\Documents\\test_multi.tif");
                ProductIO.writeProduct(multiProduct, multiFile.getAbsolutePath(), bigGeoTiffFormatName);
                MemUtils.freeAllMemory();

                //Terrain Correction
                OperatorSpi spiTerrain = new RangeDopplerGeocodingOp.Spi();
                RangeDopplerGeocodingOp opTerrain = (RangeDopplerGeocodingOp) spiTerrain.createOperator();
                opTerrain.setSourceProduct(multiProduct);
                terrainProduct = opTerrain.getTargetProduct();

                //String bigGeoTiffFormatName = BigGeoTiffProductReaderPlugIn.FORMAT_NAME;
                //File newFile = new File("C:\\Users\\s.adamo\\Documents\\test.tif");
                File terrainFile = new File("C:\\Users\\a.corrado\\Documents\\test_terrain.tif");
                ProductIO.writeProduct(terrainProduct, terrainFile.getAbsolutePath(), bigGeoTiffFormatName);
                MemUtils.freeAllMemory();

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            //formatName = "GeoTIFF-BigTIFF";
            //File newFile = new File("C:\\Users\\s.adamo\\Documents\\test.tif");
            //WriteProductOperation operation = new WriteProductOperation(terrainProduct, newFile, formatName, false);
            //operation.run();
            //new java.lang.Thread(operation).start();
        } else {

        }

    }



    private static int within(final int val, final int max) {
        return Math.max(0, Math.min(val, max));
    }

    private static String createNewProductName(String sourceProductName, int productIndex) {
        String newNameBase = "";
        if (sourceProductName != null && sourceProductName.length() > 0) {
            newNameBase = FileUtils.exchangeExtension(sourceProductName, "");
        }
        String newNamePrefix = "subset";
        String newProductName;
        if (newNameBase.length() > 0) {
            newProductName = newNamePrefix + "_" + productIndex + "_" + newNameBase;
        } else {
            newProductName = newNamePrefix + "_" + productIndex;
        }
        return newProductName;
    }

    private static void updateSubsetDefRegion(ProductSubsetDef productSubsetDef, int x1, int y1, int x2, int y2, int sx, int sy) {
        productSubsetDef.setRegion(x1, y1, x2 - x1 + 1, y2 - y1 + 1);
        productSubsetDef.setSubSampling(sx, sy);
    }
}
