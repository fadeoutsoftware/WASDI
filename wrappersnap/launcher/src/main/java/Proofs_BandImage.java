import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;
import javax.media.jai.JAI;
import javax.media.jai.OperationRegistry;
import javax.media.jai.RegistryElementDescriptor;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FilterBand;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.imgfilter.model.Filter;
import org.esa.snap.rcp.imgfilter.model.StandardFilters;
import org.esa.snap.runtime.Config;
import org.esa.snap.runtime.Engine;

import wasdi.shared.utils.BandImageManager;

public class Proofs_BandImage {

	public static void main(String[] args) throws Exception {
		createJpg(new Dimension(1219, 1063), null, args[0], args[1], args[2]);
		
    }

	private static void createJpg(Dimension d, Rectangle vp, String filePath, String bandName, String outPath) throws IOException, InterruptedException {
		File file = new File(filePath);
		
		Product product = ProductIO.readProduct(file);
		
		Band band = product.getBand(bandName);
		
		long t = System.currentTimeMillis();
		
		BandImageManager manager = new BandImageManager(product);
		
		RasterDataNode raster = band;
		BufferedImage img;
		
		if (vp==null) {
			vp = new Rectangle(new Point(0, 0), raster.getRasterSize());
		}
		img = manager.buildImageWithMasks(raster, d, vp, false);				
		ImageIO.write(img, "jpg", new File(outPath));
		
		manager.quit();
	}
	
}
