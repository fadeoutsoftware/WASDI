package wasdi.shared.utils;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ConvolutionFilterBand;
import org.esa.snap.core.datamodel.FilterBand;
import org.esa.snap.core.datamodel.GeneralFilterBand;
import org.esa.snap.core.datamodel.Kernel;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.dataop.barithm.RasterDataEvalEnv;
import org.esa.snap.core.image.ColoredBandImageMultiLevelSource;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.rcp.imgfilter.model.Filter;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.support.BufferedImageRendering;
import com.bc.ceres.grender.support.DefaultViewport;

public class BandImageManager {

	Product product;
	
	private static class CachedSource {
		public CachedSource(ColoredBandImageMultiLevelSource source) {
			this.source = source;
			this.ts = System.currentTimeMillis();
		}
		MultiLevelSource source;
		long ts;
	}
	
	private static Map<String, CachedSource> sourceCache = new ConcurrentHashMap<String, CachedSource>();
	private static Object cacheSyncObj = new Object();
	
	static {
		
		System.out.println("BandImageManager.buildImage: laucnhing cached sources thread!");
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				while (true) {
					try {
						synchronized (cacheSyncObj) {
							long ts = System.currentTimeMillis();
							ArrayList<String> toRemove = new ArrayList<String>();
							for (Entry<String, CachedSource> entry : sourceCache.entrySet()) {
								if (ts - entry.getValue().ts > 600000L) { //not accessed by 10 minutes
									toRemove.add(entry.getKey());
								}
							}
							for (String key : toRemove) {
								CachedSource removed = sourceCache.remove(key);
								removed.source = null;
								System.out.println("BandImageManager.buildImage: removed from cache: " + key);
							}
						}
						Thread.sleep(10000); //sleep for 10 seconds
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}
	

	public BandImageManager(Product product) {
		this.product = product;
	}
	
	public FilterBand getFilterBand(String bandName, Filter filter, int iterationCount) {
        FilterBand targetBand;
        String newBandName = bandName + "_" + filter.getShorthand();
        RasterDataNode sourceRaster = product.getRasterDataNode(bandName); 
        
        if (filter.getOperation() == Filter.Operation.CONVOLVE) {
            targetBand = new ConvolutionFilterBand(newBandName, sourceRaster, getKernel(filter), iterationCount);
            if (sourceRaster instanceof Band) {
                ProductUtils.copySpectralBandProperties((Band) sourceRaster, targetBand);
            }
        } else {
            GeneralFilterBand.OpType opType = getOpType(filter.getOperation());
            targetBand = new GeneralFilterBand(newBandName, sourceRaster, opType, getKernel(filter), iterationCount);
            if (sourceRaster instanceof Band) {
                ProductUtils.copySpectralBandProperties((Band) sourceRaster, targetBand);
            }
        }

        targetBand.setDescription(String.format("Filter '%s' (=%s) applied to '%s'", filter.getName(), filter.getOperation(), sourceRaster.getName()));
        if (sourceRaster instanceof Band) {
            ProductUtils.copySpectralBandProperties((Band) sourceRaster, targetBand);
        }
        
//        Band realBand = new Band(newBandName, targetBand.getDataType(), targetBand.getRasterWidth(), targetBand.getRasterHeight());
//		realBand.setSourceImage(targetBand.getSourceImage());
        ProductUtils.copyImageGeometry(sourceRaster, targetBand, false);
        targetBand.fireProductNodeDataChanged();
        product.addBand(targetBand);
        return targetBand;
    }
	
	public BufferedImage buildImageNoScale(RasterDataNode band, Dimension imgSize, Point center) {		
		Rectangle vp = new Rectangle(center.x - imgSize.width/2, center.y - imgSize.height/2, imgSize.width, imgSize.height);
		return buildImage(band, imgSize, vp);
	}

	public BufferedImage buildImageScaledWithRatio(RasterDataNode band, int width, Rectangle vp) {		
		Dimension bandSize = band.getRasterSize();
		Dimension imgSize = new Dimension(width, (int)(bandSize.height * (float)((float)width/(float)bandSize.width)));		
		return buildImage(band, imgSize, vp);
	}

	public BufferedImage buildImageScaled(RasterDataNode band, int downscaleFactor, Rectangle vp) {		
		Dimension bandSize = band.getRasterSize();
		Dimension imgSize = new Dimension(bandSize.width / downscaleFactor, bandSize.height / downscaleFactor);		
		return buildImage(band, imgSize, vp);
	}
	
	public BufferedImage buildImage(RasterDataNode band, Dimension imgSize, Rectangle vp) {
		
		BufferedImage image = null;
		
		if (band==null) {
			System.out.println("BandImageManager.buildImage: band null");
			return null;
		}
		if (band.getProduct()==null) {
			System.out.println("BandImageManager.buildImage: band product null");
			return null;
		}
		
		synchronized (cacheSyncObj) {
			
			long t = System.currentTimeMillis();

			//check if MultiLevelSource has already computed
			String key = band.getProduct().getName() + "_" + band.getName();
	        CachedSource cachedObj = sourceCache.get(key); 
	        if (cachedObj == null) {	        	
	        	cachedObj = new CachedSource(ColoredBandImageMultiLevelSource.create(band, ProgressMonitor.NULL));
	        	System.out.println("BandImageManager.buildImage: multi level source not found in cache... created: " + (System.currentTimeMillis() - t) + " ms");
	        	sourceCache.put(key, cachedObj);
	        }
	        
	        MultiLevelSource multiLevelSource = cachedObj.source;
	        // P.Campanella 05/02/2018: update cache timestamp
	        cachedObj.ts = System.currentTimeMillis();
	        
	        System.out.println("BandImageManager.buildImage: multi level source obtained: " + (System.currentTimeMillis() - t) + " ms");
	        final ImageLayer imageLayer = new ImageLayer(multiLevelSource);
	        System.out.println("BandImageManager.buildImage: imageLayer created: " + (System.currentTimeMillis() - t) + " ms");
	        final int imageWidth = imgSize.width;
	        final int imageHeight = imgSize.height;
	        final int imageType = BufferedImage.TYPE_3BYTE_BGR;
	        image = new BufferedImage(imageWidth, imageHeight, imageType);
//	        Viewport snapshotVp = vp==null ? new DefaultViewport(isModelYAxisDown(imageLayer)) : new DefaultViewport(vp, isModelYAxisDown(imageLayer));
	        Viewport snapshotVp = new DefaultViewport(isModelYAxisDown(imageLayer));
	        final BufferedImageRendering imageRendering = new BufferedImageRendering(image, snapshotVp);

	        final Graphics2D graphics = imageRendering.getGraphics();
	        graphics.setColor(Color.BLACK);
	        graphics.fillRect(0, 0, imageWidth, imageHeight);

	        snapshotVp.zoom(imageLayer.getModelBounds());
	        snapshotVp.moveViewDelta(snapshotVp.getViewBounds().x, snapshotVp.getViewBounds().y);
	        if (vp!=null) imageRendering.getViewport().zoom(vp);
	        System.out.println("BandImageManager.buildImage: init render: " + (System.currentTimeMillis() - t) + " ms + (" + imageLayer.getClass().getName() + ")");
	        imageLayer.render(imageRendering);
	        System.out.println("BandImageManager.buildImage: render done: " + (System.currentTimeMillis() - t) + " ms");			
		}
		
        return image;
	}
	
	
	public BufferedImage buildImage2(RasterDataNode oInputBand, Dimension oOutputImageSize, Rectangle oInputImageViewPortToRender) {
		
		BufferedImage oOutputBufferedImage = null;
		
		if (oInputBand==null) {
			System.out.println("BandImageManager.buildImage: band null");
			return null;
		}
		if (oInputBand.getProduct()==null) {
			System.out.println("BandImageManager.buildImage: band product null");
			return null;
		}
		
		// NOTE: Codice che crea istogramma
		//WasdiProgressMonitorStub oProgressMonitor = new WasdiProgressMonitorStub();
		//ImageManager.getInstance().prepareImageInfos(new RasterDataNode[] {band}, oProgressMonitor);
		
		synchronized (cacheSyncObj) {
			
			long lStartTime = System.currentTimeMillis();

			//check if MultiLevelSource has already computed
			String sProductKey = oInputBand.getProduct().getName() + "_" + oInputBand.getName();
	        CachedSource oCachedObj = sourceCache.get(sProductKey); 
	        if (oCachedObj == null) {
	        	oCachedObj = new CachedSource(ColoredBandImageMultiLevelSource.create(oInputBand, ProgressMonitor.NULL));
	        	System.out.println("BandImageManager.buildImage: multi level source not found in cache... created: " + (System.currentTimeMillis() - lStartTime) + " ms");
	        	sourceCache.put(sProductKey, oCachedObj);
	        }
	        
	        // Get the Source
	        MultiLevelSource oMultiLevelSource = oCachedObj.source;
	        
	        // update cache timestamp
	        oCachedObj.ts = System.currentTimeMillis();
	        System.out.println("BandImageManager.buildImage: multi level source obtained: " + (System.currentTimeMillis() - lStartTime) + " ms");

	        // Create the Output buffered Image
	        final int iOutputImageWidth = oOutputImageSize.width;
	        final int iOutputImageHeight = oOutputImageSize.height;
	        final int iOutputImageType = BufferedImage.TYPE_3BYTE_BGR;
	        oOutputBufferedImage = new BufferedImage(iOutputImageWidth, iOutputImageHeight, iOutputImageType);
	        
	        // Create Image Layer
	        final ImageLayer oSnapImageLayer = new ImageLayer(oMultiLevelSource);
	        System.out.println("BandImageManager.buildImage: imageLayer created: " + (System.currentTimeMillis() - lStartTime) + " ms");
	        
//	        Viewport snapshotVp = vp==null ? new DefaultViewport(isModelYAxisDown(imageLayer)) : new DefaultViewport(vp, isModelYAxisDown(imageLayer));
	        
	        Viewport oSnapshotViewPort = new DefaultViewport(new Rectangle(oOutputBufferedImage.getWidth(), oOutputBufferedImage.getHeight()),isModelYAxisDown(oSnapImageLayer));
	        oSnapshotViewPort.zoom(oSnapImageLayer.getModelBounds());
	        
	        final BufferedImageRendering oImageRendering = new BufferedImageRendering(oOutputBufferedImage, oSnapshotViewPort);

	        // Clear the background
	        final Graphics2D graphics = oImageRendering.getGraphics();
	        graphics.setColor(Color.BLACK);
	        graphics.fillRect(0, 0, iOutputImageWidth, iOutputImageHeight);
	        
	        oSnapshotViewPort.moveViewDelta(oSnapshotViewPort.getViewBounds().x, oSnapshotViewPort.getViewBounds().y);
	        
	        // P.CAMPANELLA 23/02/2018: E' questo che rende nere le immagini sentinel 2. In effetti in debug si vede che lo zoom fatto nella riga oSnapshotViewPort.zoom(oSnapImageLayer.getModelBounds());
	        // Ha dei numeri completamente diversi. Sembra che in qualche modo l'immagine sia come traslata..
	        //if (oInputImageViewPortToRender!=null) oImageRendering.getViewport().zoom(oInputImageViewPortToRender);
	        // Nelle sentinel 1 invece è come ci aspettiamo esattamente delle dimensioni del raster.
	        	        
	        if (oInputImageViewPortToRender!=null) {
		        double dRasterWidth = (double) oInputBand.getRasterWidth();
		        double dRasterHeight = (double) oInputBand.getRasterHeight();

		        Rectangle2D oModelBounds = oSnapImageLayer.getModelBounds();
		        
		        double dModelX = oModelBounds.getMinX();
		        double dModelWidth = oModelBounds.getWidth();
		        double dTransformedModelX = dModelX + ( (oInputImageViewPortToRender.getMinX()/dRasterWidth) * dModelWidth);

		        double dModelY = oModelBounds.getMinY();
		        double dModelHeigth = oModelBounds.getHeight();
		        double dMul = 1.0;
		        if (isModelYAxisDown(oSnapImageLayer)) dMul = -1.0;
		        
		        double dTransformedModelY = dModelY + dMul * ( (oInputImageViewPortToRender.getMinY()/dRasterHeight) * dModelHeigth);
		        
		        double dTransformedWidth = (oInputImageViewPortToRender.getWidth()/dRasterWidth) * oModelBounds.getWidth();
		        double dTransformedHeight = (oInputImageViewPortToRender.getHeight()/dRasterHeight) * oModelBounds.getHeight();
		        
		        oInputImageViewPortToRender.setBounds((int) dTransformedModelX, (int) dTransformedModelY, (int) dTransformedWidth, (int) dTransformedHeight);
	        	oImageRendering.getViewport().zoom(oInputImageViewPortToRender);
	        }
	        
	        System.out.println("BandImageManager.buildImage: init render: " + (System.currentTimeMillis() - lStartTime) + " ms + (" + oSnapImageLayer.getClass().getName() + ")");
	        oSnapImageLayer.render(oImageRendering);
	        System.out.println("BandImageManager.buildImage: render done: " + (System.currentTimeMillis() - lStartTime) + " ms");			
		}
		
        return oOutputBufferedImage;
	}
	
	public void saveGeotiff(Band band, File fileOut, ProgressMonitor pm) throws IOException {
		Product outProduct = new Product("-", "GEOTIFF");
		ProductUtils.copyGeoCoding(product, outProduct);
        Band realBand = new Band("-", band.getDataType(), band.getRasterWidth(), band.getRasterHeight());
		realBand.setSourceImage(band.getSourceImage());
        outProduct.addBand(realBand);
		if (pm==null) pm = ProgressMonitor.NULL;
		ProductIO.writeProduct(outProduct, fileOut, "GEOTIFF", true, pm);
	}
	
	private boolean isModelYAxisDown(ImageLayer baseImageLayer) {
        return baseImageLayer.getImageToModelTransform().getDeterminant() > 0.0;
    }
	
	private Kernel getKernel(Filter filter) {
        return new Kernel(filter.getKernelWidth(),
                filter.getKernelHeight(),
                filter.getKernelOffsetX(),
                filter.getKernelOffsetY(),
                1.0 / filter.getKernelQuotient(),
                filter.getKernelElements());
    }
	
	private GeneralFilterBand.OpType getOpType(Filter.Operation operation) {
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
	
}
