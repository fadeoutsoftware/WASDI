import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeGroup;

import wasdi.shared.utils.BandImageManager;

public class Proofs_Mask {

	public static void main(String[] args) throws Exception {

		File file = new File("/home/doy/tmp/wasdi/tmp/S2B_MSIL1C_20180117T102339_N0206_R065_T32TMQ_20180117T122826.zip");
		
		Product product = ProductIO.readProduct(file);
		
		String bandName = "B1";
		Band band = product.getBand(bandName);
		
		String maskName = "detector_footprint";//"detector_footprint-B01-02";
		Mask mask = null;
		final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
		for (int i = 0; i < maskGroup.getNodeCount(); i++) {
			mask = maskGroup.get(i);
			if (mask.getRasterWidth() == band.getRasterWidth() &&
				mask.getRasterHeight() == band.getRasterHeight()) {
				System.out.println(mask.getDisplayName()+","+ mask.getImageType().getName() +","+ mask.getImageColor().toString() +","+ mask.getDescription());
				if (mask.getName().startsWith(maskName)) {
					band.getOverlayMaskGroup().add(mask);
				}
			}
		}

		BandImageManager manager = new BandImageManager(product);		
		BufferedImage img = manager.buildImageWithMasks(band, new Dimension(600, 600), null);
		
		
		ImageIO.write(img, "jpg", new File("/home/doy/tmp/wasdi/tmp/" + band.getName() + "_" + mask.getName() + ".jpg"));
		manager.quit();
	}
	
}
