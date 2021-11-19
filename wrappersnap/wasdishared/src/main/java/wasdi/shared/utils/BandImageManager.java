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
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.image.ColoredBandImageMultiLevelSource;
import org.esa.snap.core.layer.MaskCollectionLayerType;
import org.esa.snap.core.layer.MaskLayerType;
import org.esa.snap.core.util.ProductUtils;

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

	Product m_oProduct;
	
	private static class CachedSource {
		public CachedSource(ColoredBandImageMultiLevelSource oSource) {
			this.m_oMultiLevelSource = oSource;
			this.m_lTimestamp = System.currentTimeMillis();
		}
		MultiLevelSource m_oMultiLevelSource;
		long m_lTimestamp;
	}
	
	private static Map<String, CachedSource> m_aoSourceCacheMap = new ConcurrentHashMap<String, CachedSource>();
	private static Object m_oCacheSyncObj = new Object();
	
	private static Thread m_oCacheThread = null;
	
	public static void stopCacheThread() {
		if (m_oCacheThread != null) {
			try {
				m_oCacheThread.interrupt();
			}
			catch (Exception e) {
				Utils.debugLog("stopChacheThread " + e.toString());
			}
			
		}
	}
	
	static {
		
		Utils.debugLog("BandImageManager.buildImage: launching cached sources thread!");
		
		m_oCacheThread  = new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				int iCycleCount = 0;
				
				int iLastValue = 0;
				
				while (true) {
					
					try {
						synchronized (m_oCacheSyncObj) {
							
							long lActualTimestamp = System.currentTimeMillis();
							
							ArrayList<String> asElementsToRemove = new ArrayList<String>();
							
							for (Entry<String, CachedSource> oCacheEntry : m_aoSourceCacheMap.entrySet()) {
								
								//not accessed by 10 minutes
								if (lActualTimestamp - oCacheEntry.getValue().m_lTimestamp > 600000L) { 
									asElementsToRemove.add(oCacheEntry.getKey());
								}
							}
							
							for (String sKey : asElementsToRemove) {
								CachedSource oRemoved = m_aoSourceCacheMap.remove(sKey);
								oRemoved.m_oMultiLevelSource = null;
								
								Utils.debugLog("BandImageManager.buildImage: removed from cache: " + sKey);
							}
							
							iCycleCount++;
							
							if ((iCycleCount%6)==0) {
								
								if (m_aoSourceCacheMap.entrySet().size() != iLastValue) {
									Utils.debugLog("------------ BandImageManager cache size: " + m_aoSourceCacheMap.entrySet().size());
									iLastValue = m_aoSourceCacheMap.entrySet().size();
								}
							}
							
						}						
					} catch (Exception e) {
						e.printStackTrace();
					}
					try {
						Thread.sleep(10000); //sleep for 10 seconds
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}
		});
		
		m_oCacheThread.start();

	}
	

	public BandImageManager(Product oProduct) {
		this.m_oProduct = oProduct;
	}	

	
	public BufferedImage buildImageWithMasks(RasterDataNode oInputBand, Dimension oOutputImageSize, Rectangle oInputImageViewPortToRender, boolean bUseCache, boolean bAlphaChannel) {
		
		BufferedImage oOutputBufferedImage = null;
		
		if (oInputBand==null) {
			Utils.debugLog("BandImageManager.buildImage: band null");
			return null;
		}
		if (oInputBand.getProduct()==null) {
			Utils.debugLog("BandImageManager.buildImage: band product null");
			return null;
		}

		CollectionLayer rootLayer = new CollectionLayer();
		
		synchronized (m_oCacheSyncObj) {
			
			long lStartTime = System.currentTimeMillis();

			//check if MultiLevelSource has already computed
			String sProductKey = oInputBand.getProduct().getName() + "_" + oInputBand.getName();
	        CachedSource oCachedObj = m_aoSourceCacheMap.get(sProductKey); 
	        if (!bUseCache || oCachedObj == null) {
	        	oCachedObj = new CachedSource(ColoredBandImageMultiLevelSource.create(oInputBand, ProgressMonitor.NULL));
	        	Utils.debugLog("BandImageManager.buildImage: multi level source not found in cache... created: " + (System.currentTimeMillis() - lStartTime) + " ms");
	        	m_aoSourceCacheMap.put(sProductKey, oCachedObj);
	        }
	        if (!bUseCache) {
	        	m_aoSourceCacheMap.remove(sProductKey);
	        }
	        
	        // Get the Source
	        MultiLevelSource oMultiLevelSource = oCachedObj.m_oMultiLevelSource;
	        
	        // update cache timestamp
	        oCachedObj.m_lTimestamp = System.currentTimeMillis();
	        Utils.debugLog("BandImageManager.buildImage: multi level source obtained: " + (System.currentTimeMillis() - lStartTime) + " ms");

	        // Create the Output buffered Image
	        int iOutputImageWidth = oOutputImageSize.width;
	        int iOutputImageHeight = oOutputImageSize.height;
	        int iOutputImageType = BufferedImage.TYPE_3BYTE_BGR;
	        
	        if (bAlphaChannel) iOutputImageType = BufferedImage.TYPE_4BYTE_ABGR;
	        
	        oOutputBufferedImage = new BufferedImage(iOutputImageWidth, iOutputImageHeight, iOutputImageType);
	        
	        // Create Image Layer
	        final ImageLayer oSnapImageLayer = new ImageLayer(oMultiLevelSource);
	        Utils.debugLog("BandImageManager.buildImage: imageLayer created: " + (System.currentTimeMillis() - lStartTime) + " ms");
	        
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
	        // Nelle sentinel 1 invece e' come ci aspettiamo esattamente delle dimensioni del raster.
	        	        
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
	        
	        Utils.debugLog("BandImageManager.buildImage: init render: " + (System.currentTimeMillis() - lStartTime) + " ms + (" + oSnapImageLayer.getClass().getName() + ")");
	        rootLayer.render(oImageRendering);
	        Utils.debugLog("BandImageManager.buildImage: render done: " + (System.currentTimeMillis() - lStartTime) + " ms");			
		}
		
        return oOutputBufferedImage;
	}
    
    private static boolean isModelYAxisDown(ImageLayer baseImageLayer) {
        return baseImageLayer.getImageToModelTransform().getDeterminant() > 0.0;
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
			Utils.debugLog("BandImageManager.buildImage: adding mask: " + mask.getDisplayName());
			maskCollectionLayer.getChildren().add(layer);
        }
        return maskCollectionLayer;
    }
	
	public void saveGeotiff(Band band, File fileOut, ProgressMonitor pm) throws IOException {
		Product outProduct = new Product("-", "GEOTIFF");
		ProductUtils.copyGeoCoding(m_oProduct, outProduct);
        Band realBand = new Band("-", band.getDataType(), band.getRasterWidth(), band.getRasterHeight());
		realBand.setSourceImage(band.getSourceImage());
        outProduct.addBand(realBand);
		if (pm==null) pm = ProgressMonitor.NULL;
		ProductIO.writeProduct(outProduct, fileOut, "GEOTIFF", true, pm);
	}
	
	public static void quit() {
		if (m_oCacheThread!=null) m_oCacheThread.interrupt();
	}
	
	private static class ImageLayerFilter implements LayerFilter {

        @Override
        public boolean accept(Layer layer) {
            return layer instanceof ImageLayer;
        }
    }
	
}
