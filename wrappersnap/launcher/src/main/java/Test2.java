/**
 * Created by a.corrado on 05/09/2016.
 */

//import publish.Publisher;
import wasdi.snapopearations.Calibration;
import wasdi.snapopearations.Filter;
import wasdi.snapopearations.Multilooking;
import wasdi.snapopearations.TerrainCorrection;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dataio.bigtiff.BigGeoTiffProductReaderPlugIn;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class Test2 {
    public static void main(String[] args) throws Exception
    {

/*
        final JFileChooser fc = new JFileChooser();

        //In response to a button click:
        int returnVal = fc.showOpenDialog(null);

        if (returnVal == JFileChooser.APPROVE_OPTION)
        {
            String bigGeoTiffFormatName = BigGeoTiffProductReaderPlugIn.FORMAT_NAME;
            String formatName = "SENTINEL-1";
            File file = fc.getSelectedFile();
            //This is where a real application would open the file.

            Product exportProduct = null;
            Product oCalibratedProduct = null;
            Product oFilteredProduct = null;
            Product oMultilookedProduct = null;
            Product terrainProduct = null;

            Calibration oCalibration = new Calibration();
            Filter oFilter = new Filter();
            Multilooking oMultilooking = new Multilooking();
            TerrainCorrection oTerrainCorrection = new TerrainCorrection();
            try
            {
                //read product
                exportProduct = ProductIO.readProduct(file, formatName);

                System.out.println("Calibrate");
                oCalibratedProduct = oCalibration.getCalibration(exportProduct, null);

                returnVal = fc.showOpenDialog(null);
                    if (returnVal != JFileChooser.APPROVE_OPTION)
                        System.out.println("Errore");
                file = fc.getSelectedFile();
                exportProduct = ProductIO.readProduct(file, formatName);

                System.out.println("Filter");
                oFilteredProduct = oFilter.getFilter(exportProduct, null);
                System.out.println("PostFilter");

                returnVal = fc.showOpenDialog(null);
                if (returnVal != JFileChooser.APPROVE_OPTION)
                    System.out.println("Errore");
                file = fc.getSelectedFile();
                exportProduct = ProductIO.readProduct(file, formatName);

                System.out.println("Multilook");
                oMultilookedProduct = oMultilooking.getMultilooking(exportProduct, null);

                    //multi look product
                    File multiFile = new File("C:\\Users\\a.corrado\\Documents\\test_multi.tif");
                    ProductIO.writeProduct(oMultilookedProduct, multiFile.getAbsolutePath(), bigGeoTiffFormatName);
                    MemUtils.freeAllMemory();

                System.out.println("PostMultilook");

                returnVal = fc.showOpenDialog(null);
                if (returnVal != JFileChooser.APPROVE_OPTION)
                    System.out.println("Errore");
                file = fc.getSelectedFile();
                exportProduct = ProductIO.readProduct(file, formatName);
                System.out.println("terrainProduct");
                terrainProduct=oTerrainCorrection.getTerrainCorrection(exportProduct, null);
                System.out.println("PostterrainProduct");

                //String bigGeoTiffFormatName = BigGeoTiffProductReaderPlugIn.FORMAT_NAME;


                File terrainFile = new File("C:\\Users\\a.corrado\\Documents\\test_terrain.tif");
                ProductIO.writeProduct(terrainProduct, terrainFile.getAbsolutePath(), bigGeoTiffFormatName);
                MemUtils.freeAllMemory();
                System.out.println("Prova son oarrivato in fondo");
            }catch(IOException e)
            {
                e.printStackTrace();

            }
        }
        else
        {

        }
        */
    }


}
