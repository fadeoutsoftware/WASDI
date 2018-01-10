import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.SubsampleAverageDescriptor;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ConvolutionFilterBand;
import org.esa.snap.core.datamodel.FilterBand;
import org.esa.snap.core.datamodel.GeneralFilterBand;
import org.esa.snap.core.datamodel.Kernel;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.image.ColoredBandImageMultiLevelSource;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.imgfilter.model.Filter;
import org.esa.snap.rcp.imgfilter.model.StandardFilters;
import org.esa.snap.runtime.Config;
import org.esa.snap.runtime.Engine;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.support.BufferedImageRendering;
import com.bc.ceres.grender.support.DefaultViewport;

public class Proofs_FilterBand {

	
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
		
		RasterDataNode node = product.getRasterDataNode(bandName); 
		Filter filter = StandardFilters.SMOOTHING_FILTERS[0];
	
		String newBandName = bandName + "_" + filter.getShorthand();
		File fileOut = new File("/home/doy/tmp/wasdi/tmp/" + newBandName + ".tif");
		
		Product outProduct = new Product(newBandName, "GEOTIFF");
		ProductUtils.copyGeoCoding(product, outProduct);
		
		FilterBand computedBand = getFilterBand(node, newBandName, filter, 1, outProduct);
		System.out.println("filter band created");
		
		
		long t = System.currentTimeMillis();
		RasterDataNode raster = computedBand.getSource();
//		ColoredBandImageMultiLevelSource imgSource = ColoredBandImageMultiLevelSource.create(raster, ProgressMonitor.NULL);
//		System.out.println("image created: " + (System.currentTimeMillis()-t) + " ms");
//		RenderedImage img = scale(imgSource.getImage(0), 0.25F);
		RenderedImage img = createThumbNailImage(new Dimension(6601, 4168), raster);
		System.out.println("image scaled: " + (System.currentTimeMillis()-t) + " ms");
		ImageIO.write(img, "jpg", new File("/home/doy/tmp/wasdi/tmp/" + newBandName + ".jpg"));
		System.out.println("jpg created: " + (System.currentTimeMillis()-t) + " ms");

		
//		System.out.println("create geotiff");
//		t = System.currentTimeMillis();
//		ProductIO.writeProduct(outProduct, fileOut, "GEOTIFF", true, new ProgressMonitor() {
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
//		} );
//		System.out.println("geotiff created: " + (System.currentTimeMillis()-t) + " ms");
		
	}
	
	private static BufferedImage createThumbNailImage(Dimension imgSize, RasterDataNode thumbNailBand) {
        BufferedImage image = null;
        MultiLevelSource multiLevelSource = ColoredBandImageMultiLevelSource.create(thumbNailBand, ProgressMonitor.NULL);
        final ImageLayer imageLayer = new ImageLayer(multiLevelSource);
        final int imageWidth = imgSize.width;
        final int imageHeight = imgSize.height;
        final int imageType = BufferedImage.TYPE_3BYTE_BGR;
        image = new BufferedImage(imageWidth, imageHeight, imageType);
        Viewport snapshotVp = new DefaultViewport(isModelYAxisDown(imageLayer));
        final BufferedImageRendering imageRendering = new BufferedImageRendering(image, snapshotVp);

        final Graphics2D graphics = imageRendering.getGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, imageWidth, imageHeight);

        snapshotVp.zoom(imageLayer.getModelBounds());
        snapshotVp.moveViewDelta(snapshotVp.getViewBounds().x, snapshotVp.getViewBounds().y);
        imageLayer.render(imageRendering);
        return image;
    }
	
	private static boolean isModelYAxisDown(ImageLayer baseImageLayer) {
        return baseImageLayer.getImageToModelTransform().getDeterminant() > 0.0;
    }
	
	public static RenderedImage scale(RenderedImage image, float scaleFactor) {
	    RenderingHints hints = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
	    RenderedOp resizeOp = SubsampleAverageDescriptor.create(image, Double.valueOf(scaleFactor), Double.valueOf(scaleFactor), hints);
	    BufferedImage bufferedResizedImage = resizeOp.getAsBufferedImage();
	    return bufferedResizedImage;
	}	
	
	static GeneralFilterBand.OpType getOpType(Filter.Operation operation) {
        if (operation == Filter.Operation.OPEN) {
            return GeneralFilterBand.OpType.OPENING;
        } else if (operation == Filter.Operation.CLOSE) {
            return GeneralFilterBand.OpType.CLOSING;
        } else if (operation == Filter.Operation.ERODE) {
            return GeneralFilterBand.OpType.EROSION;
        } else if (operation == Filter.Operation.DILATE) {
            return GeneralFilterBand.OpType.DILATION;
        } else if (operation == Filter.Operation.MIN) {
            return GeneralFilterBand.OpType.MIN;
        } else if (operation == Filter.Operation.MAX) {
            return GeneralFilterBand.OpType.MAX;
        } else if (operation == Filter.Operation.MEAN) {
            return GeneralFilterBand.OpType.MEAN;
        } else if (operation == Filter.Operation.MEDIAN) {
            return GeneralFilterBand.OpType.MEDIAN;
        } else if (operation == Filter.Operation.STDDEV) {
            return GeneralFilterBand.OpType.STDDEV;
        } else {
            throw new IllegalArgumentException("illegal operation: " + operation);
        }
    }
	
    private static FilterBand getFilterBand(RasterDataNode sourceRaster, String bandName, Filter filter, int iterationCount, Product outProduct) {
        FilterBand targetBand;

        if (filter.getOperation() == Filter.Operation.CONVOLVE) {
            targetBand = new ConvolutionFilterBand(bandName, sourceRaster, getKernel(filter), iterationCount);
            if (sourceRaster instanceof Band) {
                ProductUtils.copySpectralBandProperties((Band) sourceRaster, targetBand);
            }
        } else {
            GeneralFilterBand.OpType opType = getOpType(filter.getOperation());
            targetBand = new GeneralFilterBand(bandName, sourceRaster, opType, getKernel(filter), iterationCount);
            if (sourceRaster instanceof Band) {
                ProductUtils.copySpectralBandProperties((Band) sourceRaster, targetBand);
            }
        }

        targetBand.setDescription(String.format("Filter '%s' (=%s) applied to '%s'", filter.getName(), filter.getOperation(), sourceRaster.getName()));
        if (sourceRaster instanceof Band) {
            ProductUtils.copySpectralBandProperties((Band) sourceRaster, targetBand);
        }
        
        Band realBand = new Band(bandName, targetBand.getDataType(), targetBand.getRasterWidth(), targetBand.getRasterHeight());
		realBand.setSourceImage(targetBand.getSourceImage());
        outProduct.addBand(realBand);
        ProductUtils.copyImageGeometry(sourceRaster, targetBand, false);
        targetBand.fireProductNodeDataChanged();
        
        return targetBand;
    }

    private static Kernel getKernel(Filter filter) {
        return new Kernel(filter.getKernelWidth(),
                filter.getKernelHeight(),
                filter.getKernelOffsetX(),
                filter.getKernelOffsetY(),
                1.0 / filter.getKernelQuotient(),
                filter.getKernelElements());
    }

	
	
}
