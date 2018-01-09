import java.awt.Dimension;
import java.awt.print.Printable;
import java.io.File;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import wasdi.snapopearations.WriteProduct;

public class Proofs {

	
	public static void main(String[] args) throws Exception {
		
		
		File file = new File("/home/doy/tmp/wasdi/tmp/S1A_IW_GRDH_1SDV_20171128T054335_20171128T054400_019461_02104F_DFC1.zip");
		
		Product oProduct = ProductIO.readProduct(file);
		
		CoordinateReferenceSystem crs = oProduct.getSceneCRS();
		System.out.println(crs);
		
		GeoCoding geocoding = oProduct.getSceneGeoCoding();
		if (geocoding == null) {			
			Band[] bands = oProduct.getBands();
			for (Band band : bands) {
				System.out.println("BAND: " + band.getName());
				geocoding = band.getGeoCoding();
				if (geocoding != null) System.out.println("FOUND!!!");
			}
			
		}
		
		
		
		if (geocoding != null) {
			Dimension dim = oProduct.getSceneRasterSize();
			
			System.out.println(dim);
			
			GeoPos min = geocoding.getGeoPos(new PixelPos(0,0), null);
			GeoPos max = geocoding.getGeoPos(new PixelPos(dim.getWidth(), dim.getHeight()), null);
			float minX = (float) Math.min(min.lon, max.lon);
			float minY = (float) Math.min(min.lat, max.lat);
			float maxX = (float) Math.max(min.lon, max.lon);
			float maxY = (float) Math.max(min.lat, max.lat);
			
			Integer epsgCode = CRS.lookupEpsgCode(crs, true);
			String epsg = "EPSG:" + (epsgCode==null ? 4326 : epsgCode);
			System.out.println(String.format("{\"miny\":%f,\"minx\":%f,\"crs\":\"%s\",\"maxy\":%f,\"maxx\":%f}", 
	    			minY, minX, epsg, maxY, maxX));			
		}
		

		String sEPSG = CRS.lookupIdentifier(oProduct.getSceneCRS(),true);
		System.out.println("EPSG --> " + sEPSG);
		
		String bandName = "Amplitude_VH";
		Band band = oProduct.getBand(bandName);            
		Product geotiffProduct = new Product(bandName, "GEOTIFF");
		geotiffProduct.addBand(band);                 
		String outFilePath = new WriteProduct(null, null).WriteGeoTiff(geotiffProduct, "/home/doy/tmp/wasdi/tmp/", "pippo");
		
		System.out.println("Geotiff --> " + outFilePath);
		
	}
	
}
