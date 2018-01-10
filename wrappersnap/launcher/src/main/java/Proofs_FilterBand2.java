import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.FilterBand;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.imgfilter.model.Filter;
import org.esa.snap.rcp.imgfilter.model.StandardFilters;
import org.esa.snap.runtime.Config;
import org.esa.snap.runtime.Engine;

import com.bc.ceres.core.ProgressMonitor;

import wasdi.shared.utils.BandImageManager;

public class Proofs_FilterBand2 {

	public static void main(String[] args) throws Exception {
		
		System.setProperty("user.home", "/home/doy");
        Path propFile = Paths.get("/home/doy/workspaces/wasdi/server/launcher/resources/config.properties");
        Config.instance("snap.auxdata").load(propFile);
        Config.instance().load();
        SystemUtils.init3rdPartyLibs(null);
        Engine.start(false);
		
		
		File file = new File("/home/doy/tmp/wasdi/tmp/S1A_IW_GRDH_1SDV_20171128T054335_20171128T054400_019461_02104F_DFC1.zip");
		Product product = ProductIO.readProduct(file);
		String bandName = "Amplitude_VH";
		Filter filter = StandardFilters.SMOOTHING_FILTERS[0];
		String newBandName = bandName + "_" + filter.getShorthand();
		
		long t = System.currentTimeMillis();
		
		BandImageManager manager = new BandImageManager(product);		
		FilterBand filteredBand = manager.getFilterBand(bandName, filter, 1);
		RasterDataNode raster = filteredBand.getSource();
		
		System.out.println("filtered band created: " + (System.currentTimeMillis()-t) + " ms");
		BufferedImage img;
		
		img = manager.buildImageScaled(raster, 10, null);				
		System.out.println("full image scaled created: " + (System.currentTimeMillis()-t) + " ms");		
		ImageIO.write(img, "jpg", new File("/home/doy/tmp/wasdi/tmp/" + newBandName + ".jpg"));		
		System.out.println("full image jpg created: " + (System.currentTimeMillis()-t) + " ms");
		
		Dimension d = new Dimension(200, 200);
		img = manager.buildImage(raster, d, new Rectangle(new Point(6000, 6000), new Dimension(2000, 2000)));
		System.out.println("cut image scaled created: " + (System.currentTimeMillis()-t) + " ms");		
		ImageIO.write(img, "jpg", new File("/home/doy/tmp/wasdi/tmp/" + newBandName + "_cutted.jpg"));		
		System.out.println("cut image jpg created: " + (System.currentTimeMillis()-t) + " ms");
		
		System.out.println("create geotiff");
		manager.saveGeotiff(filteredBand, new File("/home/doy/tmp/wasdi/tmp/pippo.tif"), new ProgressMonitor() {
			
			@Override
			public void worked(int work) {
				System.out.println("worked: " + work);
			}
			
			@Override
			public void setTaskName(String taskName) {
				System.out.println("task name: " + taskName);				
			}
			
			@Override
			public void setSubTaskName(String subTaskName) {
				System.out.println("sub task name: " + subTaskName);
			}
			
			@Override
			public void setCanceled(boolean canceled) {
				System.out.println("cancelled: " + canceled);
			}
			
			@Override
			public boolean isCanceled() {
				return false;
			}
			
			@Override
			public void internalWorked(double work) {
				//System.out.println("internal worked: " + work);
			}
			
			@Override
			public void done() {
				System.out.println("done");
			}
			
			@Override
			public void beginTask(String taskName, int totalWork) {
				System.out.println("begin task " + taskName + ". work: " + totalWork);
			}
		});
		System.out.println("geotiff created: " + (System.currentTimeMillis()-t) + " ms");
    }
	
}
