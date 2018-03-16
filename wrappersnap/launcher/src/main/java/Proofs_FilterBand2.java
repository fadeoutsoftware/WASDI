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
		
		//createJpg(new Dimension(600, 600), new Rectangle(new Point(6000, 6000), new Dimension(2000, 2000)), "cut");
		
//		d = new Dimension(200, 200);
//		img = manager.buildImage(product.getBand(bandName), d, new Rectangle(new Point(6000, 6000), new Dimension(2000, 2000)));
//		System.out.println("cut image scaled created: " + (System.currentTimeMillis()-t) + " ms");		
//		ImageIO.write(img, "jpg", new File("/home/doy/tmp/wasdi/tmp/" + bandName + "_cutted.jpg"));		
//		System.out.println("cut image jpg created: " + (System.currentTimeMillis()-t) + " ms");

//		System.out.println("create geotiff");
//		manager.saveGeotiff(filteredBand, new File("/home/doy/tmp/wasdi/tmp/pippo.tif"), new ProgressMonitor() {
//			
//			@Override
//			public void worked(int work) {
//				System.out.println("worked: " + work);
//			}
//			
//			@Override
//			public void setTaskName(String taskName) {
//				System.out.println("task name: " + taskName);				
//			}
//			
//			@Override
//			public void setSubTaskName(String subTaskName) {
//				System.out.println("sub task name: " + subTaskName);
//			}
//			
//			@Override
//			public void setCanceled(boolean canceled) {
//				System.out.println("cancelled: " + canceled);
//			}
//			
//			@Override
//			public boolean isCanceled() {
//				return false;
//			}
//			
//			@Override
//			public void internalWorked(double work) {
//				//System.out.println("internal worked: " + work);
//			}
//			
//			@Override
//			public void done() {
//				System.out.println("done");
//			}
//			
//			@Override
//			public void beginTask(String taskName, int totalWork) {
//				System.out.println("begin task " + taskName + ". work: " + totalWork);
//			}
//		});
//		System.out.println("geotiff created: " + (System.currentTimeMillis()-t) + " ms");
    }

	private static void createJpg(Dimension d, Rectangle vp, String suffix) throws IOException, InterruptedException {
		//File file = new File("C:\\Temp\\wasdi\\data\\paolo\\2c1271a4-9e2b-4291-aabd-caf3074adb25\\S2A_MSIL1C_20180102T102421_N0206_R065_T32TMQ_20180102T123237.zip");
		File file = new File("C:\\Temp\\wasdi\\data\\paolo\\2c1271a4-9e2b-4291-aabd-caf3074adb25\\S1A_IW_GRDH_1SDV_20180129T052722_20180129T052747_020365_022CA8_9D99.zip");
		Product product = ProductIO.readProduct(file);
		//String bandName = "B1";
		String bandName = "AMPLITUDE_VH";
		Filter filter = StandardFilters.SMOOTHING_FILTERS[0];
		
		Band band = product.getBand(bandName);
		
		long t = System.currentTimeMillis();
		
		BandImageManager manager = new BandImageManager(product);		
		FilterBand filteredBand = manager.getFilterBand(bandName, filter, 1);
		RasterDataNode raster = filteredBand;//.getSource();
		raster = band;
		
		System.out.println("filtered band created: " + (System.currentTimeMillis()-t) + " ms");
		BufferedImage img;
		
//		d = new Dimension(600, 600);
		if (vp==null) vp = new Rectangle(new Point(0, 0), raster.getRasterSize());
		img = manager.buildImage(raster, d, vp);				
		ImageIO.write(img, "jpg", new File("C:\\Temp\\wasdi\\" + filteredBand.getName() + "_" + suffix + ".jpg"));		
		
	}
	
}
