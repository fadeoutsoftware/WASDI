import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.media.jai.JAI;
import javax.media.jai.OperationRegistry;
import javax.media.jai.RegistryElementDescriptor;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FilterBand;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.rcp.imgfilter.model.Filter;
import org.esa.snap.rcp.imgfilter.model.StandardFilters;

import wasdi.shared.utils.BandImageManager;

public class Proofs_FilterBand2 {

	public static void main(String[] args) throws Exception {
		
//		System.setProperty("user.home", "/home/doy");
//        Path propFile = Paths.get("/home/doy/workspaces/wasdi/server/launcher/resources/config.properties");
//        Config.instance("snap.auxdata").load(propFile);
//        Config.instance().load();
//        SystemUtils.init3rdPartyLibs(null);
//        Engine.start(false);
		
		createJpg(new Dimension(600, 600), null, "preview");
		
		System.out.println("-------------------------------------------------------------------------");
		
    }

	private static void createJpg(Dimension d, Rectangle vp, String suffix) throws IOException, InterruptedException {
		//File file = new File("C:\\Temp\\wasdi\\data\\paolo\\dcbb272f-c4c4-4ef7-8bc4-bd3e24dfe93a\\S2A_MSIL1C_20180207T104211_N0206_R008_T31TGN_20180207T142820.zip");
		//File file = new File("C:\\Temp\\wasdi\\data\\paolo\\2c1271a4-9e2b-4291-aabd-caf3074adb25\\S1A_IW_GRDH_1SDV_20180129T052722_20180129T052747_020365_022CA8_9D99.zip");
//		File file = new File("C:\\Temp\\wasdi\\data\\paolo\\f205f454-7ab2-4285-b7ab-1b18b0d4b4eb\\S1B_IW_GRDH_1SDV_20170624T055025_20170624T055050_006188_00ADF2_0EB5.zip");
		//File file = new File("/home/doy/tmp/wasdi/tmp/S2B_MSIL1C_20180117T102339_N0206_R065_T32TMQ_20180117T122826.zip");
		File file = new File("/home/doy/tmp/wasdi/tmp/S2B_MSIL1C_20180117T102339_N0206_R065_T32TMQ_20180117T122826.zip");
		Product product = ProductIO.readProduct(file);
		String bandName = "B1";
//		String bandName = "AMPLITUDE_VH";
		Filter filter = StandardFilters.SMOOTHING_FILTERS[0];
		
		Band band = product.getBand(bandName);
		
		long t = System.currentTimeMillis();
		
		BandImageManager manager = new BandImageManager(product);
		
		//FILTER
//		FilterBand filteredBand = manager.getFilterBand(bandName, filter, 1);
//		RasterDataNode raster = filteredBand;//.getSource();
//		System.out.println("filtered band created: " + (System.currentTimeMillis()-t) + " ms");

		//HISTOGRAM
		
		
		RasterDataNode raster = band;
		BufferedImage img;
		
//		d = new Dimension(600, 600);
		if (vp==null) {
			vp = new Rectangle(new Point(0, 0), raster.getRasterSize());
			//vp = new Rectangle(900,900,900,900);
		}
		img = manager.buildImage(raster, d, vp);				
		ImageIO.write(img, "jpg", new File("C:\\Temp\\wasdi\\" + bandName + "_" + suffix + ".jpg"));		
		
	}
	
}
