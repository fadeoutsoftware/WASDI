package wasdi.shared.utils;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ConvolutionFilterBand;
import org.esa.snap.core.datamodel.FilterBand;
import org.esa.snap.core.datamodel.GeneralFilterBand;
import org.esa.snap.core.datamodel.Kernel;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.image.ColoredBandImageMultiLevelSource;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.layer.MaskCollectionLayerType;
import org.esa.snap.core.layer.MaskLayerType;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.rcp.imgfilter.model.Filter;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.support.LayerUtils;
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
	
	private static Thread cacheThread = null;
	
	static {
		
		System.out.println("BandImageManager.buildImage: laucnhing cached sources thread!");
		
		cacheThread  = new Thread(new Runnable() {
			
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
					} catch (Exception e) {
						e.printStackTrace();
					}
					try {
						Thread.sleep(10000); //sleep for 10 seconds
					} catch (InterruptedException e) {
						break;
					}
				}
			}
		});
		
		cacheThread.start();
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
	
	
	public BufferedImage buildImage(RasterDataNode oInputBand, Dimension oOutputImageSize, Rectangle oInputImageViewPortToRender) {
		
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
		WasdiProgressMonitorStub oProgressMonitor = new WasdiProgressMonitorStub();
		ImageManager.getInstance().prepareImageInfos(new RasterDataNode[] {oInputBand}, oProgressMonitor);
		
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
	        // Nelle sentinel 1 invece � come ci aspettiamo esattamente delle dimensioni del raster.
	        	        
	        if (oInputImageViewPortToRender!=null) {
		        AffineTransform oImageToModel = oSnapImageLayer.getImageToModelTransform();
		        
		        double [] adInputRect = new double[4];
		        double [] adOutputRect = new double[4];
		        
		        adInputRect[0] = oInputImageViewPortToRender.getMinX();
		        adInputRect[1] = oInputImageViewPortToRender.getMinY();
		        adInputRect[2] = oInputImageViewPortToRender.getMaxX();
		        adInputRect[3] = oInputImageViewPortToRender.getMaxY();
		        
		        adOutputRect[0] = 0.0;
		        adOutputRect[1] = 0.0;
		        adOutputRect[2] = 0.0;
		        adOutputRect[3] = 0.0;
		        
		        oImageToModel.transform(adInputRect, 0, adOutputRect, 0, 2);
		        
		        double dMinX = adOutputRect[0];
		        double dMaxX = adOutputRect[2];
		        if (dMinX>dMaxX) {
		        	dMinX = adOutputRect[2];
		        	dMaxX = adOutputRect[0];
		        }
		        
		        double dMinY = adOutputRect[1];
		        double dMaxY = adOutputRect[3];
		        
		        if (dMinY>dMaxY) {
			        dMinY = adOutputRect[3];
			        dMaxY = adOutputRect[1];		        	
		        }
		        
		        Rectangle2D oModelViewPortToRender = new Rectangle2D.Double(dMinX, dMinY, dMaxX-dMinX, dMaxY-dMinY);
		        

	        	oImageRendering.getViewport().zoom(oModelViewPortToRender);
	        }
	        
	        System.out.println("BandImageManager.buildImage: init render: " + (System.currentTimeMillis() - lStartTime) + " ms + (" + oSnapImageLayer.getClass().getName() + ")");
	        oSnapImageLayer.render(oImageRendering);
	        System.out.println("BandImageManager.buildImage: render done: " + (System.currentTimeMillis() - lStartTime) + " ms");			
		}
		
        return oOutputBufferedImage;
	}

	
	public BufferedImage buildImageWithMasks(RasterDataNode oInputBand, Dimension oOutputImageSize, Rectangle oInputImageViewPortToRender) {
		
		BufferedImage oOutputBufferedImage = null;
		
		if (oInputBand==null) {
			System.out.println("BandImageManager.buildImage: band null");
			return null;
		}
		if (oInputBand.getProduct()==null) {
			System.out.println("BandImageManager.buildImage: band product null");
			return null;
		}

		CollectionLayer rootLayer = new CollectionLayer();
		
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
	        // Nelle sentinel 1 invece � come ci aspettiamo esattamente delle dimensioni del raster.
	        	        
	        if (oInputImageViewPortToRender!=null) {
		        AffineTransform oImageToModel = oSnapImageLayer.getImageToModelTransform();
		        
		        double [] adInputRect = new double[4];
		        double [] adOutputRect = new double[4];
		        
		        adInputRect[0] = oInputImageViewPortToRender.getMinX();
		        adInputRect[1] = oInputImageViewPortToRender.getMinY();
		        adInputRect[2] = oInputImageViewPortToRender.getMaxX();
		        adInputRect[3] = oInputImageViewPortToRender.getMaxY();
		        
		        adOutputRect[0] = 0.0;
		        adOutputRect[1] = 0.0;
		        adOutputRect[2] = 0.0;
		        adOutputRect[3] = 0.0;
		        
		        oImageToModel.transform(adInputRect, 0, adOutputRect, 0, 2);
		        
		        double dMinX = adOutputRect[0];
		        double dMaxX = adOutputRect[2];
		        if (dMinX>dMaxX) {
		        	dMinX = adOutputRect[2];
		        	dMaxX = adOutputRect[0];
		        }
		        
		        double dMinY = adOutputRect[1];
		        double dMaxY = adOutputRect[3];
		        
		        if (dMinY>dMaxY) {
			        dMinY = adOutputRect[3];
			        dMaxY = adOutputRect[1];		        	
		        }
		        
		        Rectangle2D oModelViewPortToRender = new Rectangle2D.Double(dMinX, dMinY, dMaxX-dMinX, dMaxY-dMinY);
		        

	        	oImageRendering.getViewport().zoom(oModelViewPortToRender);
	        }
	        
	        rootLayer.getChildren().add(oSnapImageLayer);
	        
	        //add all the masks
	        rootLayer.getChildren().add(getFirstImageLayerIndex(rootLayer), createMaskCollectionLayer(oInputBand));
	        
	        System.out.println("BandImageManager.buildImage: init render: " + (System.currentTimeMillis() - lStartTime) + " ms + (" + oSnapImageLayer.getClass().getName() + ")");
	        rootLayer.render(oImageRendering);
	        System.out.println("BandImageManager.buildImage: render done: " + (System.currentTimeMillis() - lStartTime) + " ms");			
		}
		
        return oOutputBufferedImage;
	}

	
	int getFirstImageLayerIndex(Layer rootLayer) {
        return LayerUtils.getChildLayerIndex(rootLayer, LayerUtils.SEARCH_DEEP, 0, new ImageLayerFilter());
    }
	
	private Layer createMaskCollectionLayer(RasterDataNode band) {
        final LayerType maskCollectionType = LayerTypeRegistry.getLayerType(MaskCollectionLayerType.class);
        final PropertySet layerConfig = maskCollectionType.createLayerConfig(null);
        layerConfig.setValue(MaskCollectionLayerType.PROPERTY_NAME_RASTER, band);
        final Layer maskCollectionLayer = maskCollectionType.createLayer(null, layerConfig);
        ProductNodeGroup<Mask> masks = band.getOverlayMaskGroup();
        for (int i = 0; i < masks.getNodeCount(); i++) {
			Mask mask = masks.get(i);
			Layer layer = MaskLayerType.createLayer(band, mask);
			System.out.println("BandImageManager.buildImage: adding mask: " + mask.getDisplayName());
			maskCollectionLayer.getChildren().add(layer);
        }
        return maskCollectionLayer;
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
	
	public static void quit() {
		if (cacheThread!=null) cacheThread.interrupt();
	}
	
	private static class ImageLayerFilter implements LayerFilter {

        @Override
        public boolean accept(Layer layer) {
            return layer instanceof ImageLayer;
        }
    }
}
