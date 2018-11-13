import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import wasdi.shared.utils.BandImageManager;

public class Proofs_Mask {

	public static void main(String[] args) throws Exception {
		
		String fileName = (args.length==3) ? args[0]: "/home/doy/tmp/wasdi/tmp/PROBAV_L2A_20180101_011405_2_100M_V101.HDF5";
		String bandName = (args.length==3) ? args[1]: "SAA";
		String maskName = (args.length==3) ? args[2]: "CLEAR";		
		
		File file = new File(fileName);
		
		Product product = ProductIO.readProduct(file);
		BandImageManager manager = new BandImageManager(product);

		try {
		
			Band band = product.getBand(bandName);
			
			
//			Mask mask = null;
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
	

			Mask mask = product.getMaskGroup().get(maskName);
			band.getOverlayMaskGroup().add(mask);
			
			
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
			
//			String code = "SAA > 0.3";
//	        maskName = UUID.randomUUID().toString();
//	        Dimension maskSize = new Dimension(product.getSceneRasterWidth(), product.getSceneRasterHeight());
//	        mask = new Mask(maskName, maskSize.width, maskSize.height, Mask.BandMathsType.INSTANCE);
//	        mask.setImageColor(Color.BLUE);
//	        mask.setImageTransparency(0.5);
//			String externalName = Tokenizer.createExternalName(bandName);
//	        PropertyContainer imageConfig = mask.getImageConfig();
//	        imageConfig.setValue(Mask.BandMathsType.PROPERTY_NAME_EXPRESSION, code);
//	        product.addMask(mask);
//	        band.getOverlayMaskGroup().add(mask);
			
					
			BufferedImage img = manager.buildImageWithMasks(band, new Dimension(600, 600), null, true);
			
			ImageIO.write(img, "jpg", new File(fileName + "_" + band.getName() + "_MASKED.jpg"));
		} finally {
			manager.quit();
		}
	}
	
	
	
	
}
