
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

/**
 * Created by s.adamo on 18/05/2016.
 */
public class Test3 {
    public static void main(String[] args) throws Exception {

        /*---Test-------------------------------*/

      // String sPath = "C:\\temp\\ImagePyramidTest\\Dati Sentinel\\";
        //String sName = "S1A_IW_GRDH_1SDV_20160217T170557_20160217T170622_009989_00EAE9_507D.zip";

       // ReadProduct oRead = new ReadProduct();
        //ProductViewModel oProductViewModel = oRead.getProductViewModel(new File(sPath+sName));

        //oRead.writeBigTiff(sName);

          //Publisher oPublisher = new Publisher();

          //String sName = "test_cal";
          //oPublisher.publishGeoTiff(sName + ".tif", "http://localhost:8080/geoserver", "admin", "geoserver", "wasdi", "pyramidstore");

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

                File calFile = new File("C:\\Users\\s.adamo\\Documents\\test_cal.tif");
                //File calFile = new File("C:\\Users\\a.corrado\\Documents\\test_cal.tif");
                ProductIO.writeProduct(calibrateProduct, calFile.getAbsolutePath(), bigGeoTiffFormatName);

                exportProduct = ProductIO.readProduct(calFile, bigGeoTiffFormatName);

                //filter
                OperatorSpi spiFilter = new SpeckleFilterOp.Spi();
                SpeckleFilterOp opFilter = (SpeckleFilterOp) spiFilter.createOperator();
                opFilter.setSourceProduct(exportProduct);//calibrateProduct
                opFilter.SetFilter("Refined Lee");
                Product filterProduct = opFilter.getTargetProduct();

                File filterFile = new File("C:\\Users\\s.adamo\\Documents\\test_filter.tif");
                //File filterFile = new File("C:\\Users\\a.corrado\\Documents\\test_filter.tif");
                //calibrateProduct
                ProductIO.writeProduct(exportProduct, filterFile.getAbsolutePath(), bigGeoTiffFormatName);

                exportProduct = ProductIO.readProduct(filterFile, bigGeoTiffFormatName);

                //Multilooking
                OperatorSpi spiMulti = new MultilookOp.Spi();
                MultilookOp opMulti = (MultilookOp) spiMulti.createOperator();
                opMulti.setSourceProduct(exportProduct);//filterProduct
                opMulti.setNumRangeLooks(4);
                MultilookOp.DerivedParams param = new MultilookOp.DerivedParams();
                param.nRgLooks = 4;
                opMulti.getDerivedParameters(exportProduct, param);//filterProduct
                //opMulti.setNumAzimuthLooks(param.nAzLooks);
                Product multiProduct = opMulti.getTargetProduct();

                //multi look product
                File multiFile = new File("C:\\Users\\s.adamo\\Documents\\test_multi.tif");
                ProductIO.writeProduct(exportProduct, multiFile.getAbsolutePath(), bigGeoTiffFormatName);

                exportProduct = ProductIO.readProduct(multiFile, bigGeoTiffFormatName);
                //Terrain Correction
                OperatorSpi spiTerrain = new RangeDopplerGeocodingOp.Spi();
                RangeDopplerGeocodingOp opTerrain = (RangeDopplerGeocodingOp) spiTerrain.createOperator();
                opTerrain.setSourceProduct(exportProduct);
                terrainProduct = opTerrain.getTargetProduct();

                //String bigGeoTiffFormatName = BigGeoTiffProductReaderPlugIn.FORMAT_NAME;
                File terrainFile = new File("C:\\Users\\s.adamo\\Documents\\test.tif");
                //File terrainFile = new File("C:\\Users\\a.corrado\\Documents\\test_terrain.tif");
                ProductIO.writeProduct(exportProduct, terrainFile.getAbsolutePath(), bigGeoTiffFormatName);

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
