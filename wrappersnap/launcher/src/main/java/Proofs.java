import java.io.File;

import org.apache.commons.io.IOUtils;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.geotiff.GeoCoding2GeoTIFFMetadata;
import org.esa.snap.core.util.geotiff.GeoTIFF;
import org.esa.snap.core.util.geotiff.GeoTIFFMetadata;
import org.esa.snap.dataio.bigtiff.BigGeoTiffProductWriterPlugIn;
import org.esa.snap.dataio.geotiff.GeoTiffProductReaderPlugIn;
import org.geotools.referencing.CRS;
import org.json.JSONArray;
import org.json.JSONObject;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;

import wasdi.ConfigReader;
import wasdi.snapopearations.ReadProduct;
import wasdi.snapopearations.WriteProduct;

public class Proofs {

	
	public static void main(String[] args) throws Exception {
		File oFile = new File("/home/doy/tmp/wasdi/data/download/paolo/8e91a84c-3dcf-470d-8e36-3ad40de80d54/S2A_MSIL1C_20170226T102021_N0204_R065_T32TMP_20170226T102458.zip");
//		File oFile = new File("/home/doy/tmp/wasdi/tmp/S2A_MSIL1C_20170411T100031_N0204_R122_T33TUG_20170411T100025_NDVI.zip..dim");
		File oDirDst = new File("/home/doy/tmp/wasdi/tmp/");
		
		ReadProduct oReadProduct = new ReadProduct();
        Product oSentinel = oReadProduct.ReadProduct(oFile, null);
        
        System.out.println(oSentinel.getProductReader().getClass().getName());
        System.out.println(oSentinel.getProductReader().getClass().getCanonicalName());
        System.out.println(oSentinel.getProductReader().getClass().getTypeName());
        
        System.out.println("EPSG");
        
        String sEPSG = CRS.lookupIdentifier(oSentinel.getSceneGeoCoding().getMapCRS(),true);
        
        String[] asBandNames = oSentinel.getBandNames();
        for (String sBandName : asBandNames) {
        	
        	
        	
        	// Get the Geocoding and Band
			GeoCoding oGeoCoding = oSentinel.getSceneGeoCoding();
			
			Band oBand = oSentinel.getBand(sBandName);
			
			// Get Image
			MultiLevelImage oBandImage = oBand.getSourceImage();
			// Get TIFF Metadata
			GeoTIFFMetadata oMetadata = GeoCoding2GeoTIFFMetadata.createGeoTIFFMetadata(oGeoCoding, oBandImage.getWidth(),oBandImage.getHeight());
			File oOutputFile = new File(oDirDst, sBandName + ".tif");
		    GeoTIFF.writeImage(oBandImage, oOutputFile, oMetadata);
        	
        	
//            Band oBand = oSentinel.getBand(sBandName);
//            
//            System.out.println("exctractinb band " + oBand.getDisplayName() + " - " + oBand.getDescription());
//            
//            Product oGeotiffProduct = new Product("band_geotiff_" + sBandName, "GEOTIFF");
//            oGeotiffProduct.addBand(oBand);
//            
//            String sFileName = new WriteProduct().WriteGeoTiff(oGeotiffProduct, oDirDst.getAbsolutePath(), sBandName);
            
//            File oFileDst = new File(oDirDst, sBandName + ".tif");
//            ProductIO.writeProduct(oGeotiffProduct, oFileDst.getAbsolutePath(), "GeoTIFF");
            
            break;
		}
        
		
        System.out.println("ciao");
		
		
//		Runtime run = Runtime.getRuntime();
//		String sFileToConvert = "/home/doy/tmp/wasdi/T31TDM_20170202T104241_B02.jp2";
//		String sEPSG = "EPSG:32621";
//		
//        String sCmd = "gdalinfo " + sFileToConvert + " -json";
//        String sBandType = null;
//    	Process oGdalInfoProcess = run.exec(sCmd);
//        JSONObject oInfoJsonResult = new JSONObject(IOUtils.toString(oGdalInfoProcess.getInputStream()));
//        oGdalInfoProcess.waitFor();
//        try {
//			JSONArray oBandsJsonArray = oInfoJsonResult.getJSONArray("bands");
//			JSONObject oBandJsonObj = oBandsJsonArray.getJSONObject(0);
//			sBandType = oBandJsonObj.getString("type");
//		} catch (Exception oEx) {
//            oEx.printStackTrace();					}
//        
//        sCmd = "gdal_translate  -of GTiff -a_srs "+ sEPSG;
//        if (sBandType != null) sCmd += " -ot " + sBandType; 
//        sCmd += " " + sFileToConvert + " " + sFileToConvert+".tif";
//        
//        System.out.println(sCmd);

	}
	
}
