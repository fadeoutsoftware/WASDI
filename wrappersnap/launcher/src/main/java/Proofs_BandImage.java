import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;

import wasdi.shared.utils.BandImageManager;

public class Proofs_BandImage {

	public static void main(String[] args) throws Exception {
		//createJpg(new Dimension(1219, 1063), null, args[0], args[1], args[2]);
		
		String sFilePath = "C:\\Temp\\wasdi\\data\\paolo\\2c1271a4-9e2b-4291-aabd-caf3074adb25\\S2A_MSIL1C_20180102T102421_N0206_R065_T32TMQ_20180102T123237.zip";
		String sRedBandName = "B4";
		String sGreenBandName = "B3";
		String sBlueBandName = "B2";
		String sOutPath = "C:\\Temp\\wasdi\\data\\paolo\\2c1271a4-9e2b-4291-aabd-caf3074adb25\\RGB_v2.png";
		//createRGB(new Dimension(1219, 1063), sFilePath, sRedBandName, sGreenBandName, sBlueBandName, sOutPath);
		//createRGB(new Dimension(600, 600), sFilePath, sRedBandName, sGreenBandName, sBlueBandName, sOutPath);
		createRGB(null, sFilePath, sRedBandName, sGreenBandName, sBlueBandName, sOutPath);
		System.out.println("TEST DONE");
    }

	@SuppressWarnings("unused")
	private static void createJpg(Dimension d, Rectangle vp, String filePath, String bandName, String outPath) throws IOException, InterruptedException {
		File file = new File(filePath);
		
		Product product = ProductIO.readProduct(file);
		
		Band band = product.getBand(bandName);
		
		//long t = System.currentTimeMillis();
		
		BandImageManager manager = new BandImageManager(product);
		
		RasterDataNode raster = band;
		BufferedImage img;
		
		if (vp==null) {
			vp = new Rectangle(new Point(0, 0), raster.getRasterSize());
		}
		img = manager.buildImageWithMasks(raster, d, vp, false);				
		ImageIO.write(img, "jpg", new File(outPath));
		
		BandImageManager.quit();
	}
	
	@SuppressWarnings("unused")
	private static void createRGB(Dimension oOutputDimension , String sFilePath, String sRedBandName, String sGreenBandName, String sBlueBandName, String sOutPath) throws IOException, InterruptedException {
		File file = new File(sFilePath);
		
		Product product = ProductIO.readProduct(file);
		
		Band oRedBand = product.getBand(sRedBandName);
		Band oGreenBand = product.getBand(sGreenBandName);
		Band oBlueBand = product.getBand(sBlueBandName);
		
		BandImageManager oManager = new BandImageManager(product);
		
		RenderedImage oOutImg;
		
		System.out.println("createRGB: start buildRGBImage");
		
		//oOutImg = oManager.buildRGBImage(oRedBand , oGreenBand, oBlueBand, oOutputDimension);
		oOutImg = oManager.buildRGBImage4(oRedBand , oGreenBand, oBlueBand, oOutputDimension,true);
		
		long lStartTime = System.currentTimeMillis();
		
		//JAI.create("filestore", oOutImg, sOutPath, "JPEG");
		ImageIO.setUseCache(false);
		ImageIO.write(oOutImg, "PNG", new File(sOutPath));
		
		System.out.println("createRGB: Saved in " + (System.currentTimeMillis() - lStartTime) + " ms");
		
		BandImageManager.quit();
	}
	
}
