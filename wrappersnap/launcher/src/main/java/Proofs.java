import java.awt.Dimension;
import java.io.File;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class Proofs {

	
	public static void main(String[] args) throws Exception {
		
		
		File file = new File("/home/doy/tmp/wasdi/prova_snap/S1A_IW_GRDH_1SDV_20160802T051857_20160802T051922_012417_013615_C75B.zip");
		
		Product oProduct = ProductIO.readProduct(file);
		
		CoordinateReferenceSystem crs = oProduct.getSceneCRS();
		System.out.println(crs);
		
		GeoCoding geocoding = oProduct.getSceneGeoCoding();
		Dimension dim = oProduct.getSceneRasterSize();		
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
	
}
