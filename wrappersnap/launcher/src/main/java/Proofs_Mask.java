import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.UUID;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.jexp.impl.Tokenizer;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.rcp.SnapApp;

import com.bc.ceres.binding.PropertyContainer;

import wasdi.shared.utils.BandImageManager;

public class Proofs_Mask {

	public static void main(String[] args) throws Exception {
		

		File file = new File("/home/doy/tmp/wasdi/tmp/S2B_MSIL1C_20180117T102339_N0206_R065_T32TMQ_20180117T122826.zip");
		
		Product product = ProductIO.readProduct(file);
		BandImageManager manager = new BandImageManager(product);

		try {
		
			String bandName = "B1";
			Band band = product.getBand(bandName);
			
			String maskName = null;
			Mask mask = null;

//			maskName = "detector_footprint";//"detector_footprint-B01-02";
//			final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
//			for (int i = 0; i < maskGroup.getNodeCount(); i++) {
//				mask = maskGroup.get(i);
//				if (mask.getRasterWidth() == band.getRasterWidth() &&
//					mask.getRasterHeight() == band.getRasterHeight()) {
//					System.out.println(mask.getDisplayName()+","+ mask.getImageType().getName() +","+ mask.getImageColor().toString() +","+ mask.getDescription());
//					if (mask.getName().startsWith(maskName)) {
//						band.getOverlayMaskGroup().add(mask);
//					}
//				}
//			}
	
			
//			double minValue = 0.0;
//			double maxValue = 0.2;			
//	        maskName = UUID.randomUUID().toString();
//	        Dimension maskSize = new Dimension(product.getSceneRasterWidth(), product.getSceneRasterHeight());
//	        mask = new Mask(maskName, maskSize.width, maskSize.height, Mask.RangeType.INSTANCE);
//	        mask.setImageColor(Color.RED);
//	        mask.setImageTransparency(0.5);
//			String externalName = Tokenizer.createExternalName(bandName);
//	        PropertyContainer imageConfig = mask.getImageConfig();
//	        imageConfig.setValue(Mask.RangeType.PROPERTY_NAME_MINIMUM, minValue);
//	        imageConfig.setValue(Mask.RangeType.PROPERTY_NAME_MAXIMUM, maxValue);
//	        imageConfig.setValue(Mask.RangeType.PROPERTY_NAME_RASTER, externalName);
//	        product.addMask(mask);
//	        band.getOverlayMaskGroup().add(mask);
			
			String code = "B1 > 0.3";
	        maskName = UUID.randomUUID().toString();
	        Dimension maskSize = new Dimension(product.getSceneRasterWidth(), product.getSceneRasterHeight());
	        mask = new Mask(maskName, maskSize.width, maskSize.height, Mask.BandMathsType.INSTANCE);
	        mask.setImageColor(Color.BLUE);
	        mask.setImageTransparency(0.5);
			String externalName = Tokenizer.createExternalName(bandName);
	        PropertyContainer imageConfig = mask.getImageConfig();
	        imageConfig.setValue(Mask.BandMathsType.PROPERTY_NAME_EXPRESSION, code);
	        product.addMask(mask);
	        band.getOverlayMaskGroup().add(mask);
			
					
			BufferedImage img = manager.buildImageWithMasks(band, new Dimension(600, 600), null, true);
			
			ImageIO.write(img, "jpg", new File("/home/doy/tmp/wasdi/tmp/" + band.getName() + "_MASKED.jpg"));
			manager.quit();
		} finally {
			manager.quit();
		}
	}
	
	
	
	
}
