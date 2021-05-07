package wasdi.shared.utils;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.media.jai.JAI;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.ColorPaletteDef.Point;
import org.esa.snap.core.datamodel.ConvolutionFilterBand;
import org.esa.snap.core.datamodel.FilterBand;
import org.esa.snap.core.datamodel.GeneralFilterBand;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.ImageInfo.HistogramMatching;
import org.esa.snap.core.datamodel.Kernel;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.Stx;
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
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.glayer.support.BackgroundLayer;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.support.LayerUtils;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.support.BufferedImageRendering;
import com.bc.ceres.grender.support.DefaultViewport;

import wasdi.shared.viewmodels.ColorManipulationViewModel;
import wasdi.shared.viewmodels.ColorViewModel;
import wasdi.shared.viewmodels.ColorWithValueViewModel;

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
	
	public FilterBand getFilterBand(String sBandName, Filter oFilter, int iIterationCount) {
		
        FilterBand oTargetBand;
        
        String sNewBandName = sBandName + "_" + oFilter.getShorthand();
        RasterDataNode oSourceRasterDataNode = m_oProduct.getRasterDataNode(sBandName); 
        
        if (oFilter.getOperation() == Filter.Operation.CONVOLVE) {
            oTargetBand = new ConvolutionFilterBand(sNewBandName, oSourceRasterDataNode, getKernel(oFilter), iIterationCount);
            if (oSourceRasterDataNode instanceof Band) {
                ProductUtils.copySpectralBandProperties((Band) oSourceRasterDataNode, oTargetBand);
            }
        } else {
            GeneralFilterBand.OpType opType = getOpType(oFilter.getOperation());
            oTargetBand = new GeneralFilterBand(sNewBandName, oSourceRasterDataNode, opType, getKernel(oFilter), iIterationCount);
            if (oSourceRasterDataNode instanceof Band) {
                ProductUtils.copySpectralBandProperties((Band) oSourceRasterDataNode, oTargetBand);
            }
        }

        oTargetBand.setDescription(String.format("Filter '%s' (=%s) applied to '%s'", oFilter.getName(), oFilter.getOperation(), oSourceRasterDataNode.getName()));
        if (oSourceRasterDataNode instanceof Band) {
            ProductUtils.copySpectralBandProperties((Band) oSourceRasterDataNode, oTargetBand);
        }
        
        ProductUtils.copyImageGeometry(oSourceRasterDataNode, oTargetBand, false);
        oTargetBand.fireProductNodeDataChanged();
        
        m_oProduct.addBand(oTargetBand);
        
        return oTargetBand;
    }
	
	
	public ColorManipulationViewModel getColorManipulation(String sBandName, boolean bAccurateStats) {
		
		RasterDataNode oInputBand = m_oProduct.getRasterDataNode(sBandName);
		
		WasdiProgressMonitorStub oProgressMonitor = new WasdiProgressMonitorStub();
		ImageManager.getInstance().prepareImageInfos(new RasterDataNode[] {oInputBand}, oProgressMonitor);
		
		Stx oStats = oInputBand.getStx(bAccurateStats, oProgressMonitor);
		ImageInfo oInfo = oInputBand.getImageInfo();

		ColorManipulationViewModel oModel = new ColorManipulationViewModel();
		
		//get the point colors
		Point[] oPoints = oInfo.getColorPaletteDef().getPoints();
		ColorWithValueViewModel oColorsModel[] = new ColorWithValueViewModel[oPoints.length];
		
		for (int i = 0; i < oColorsModel.length; i++) {
			oColorsModel[i] = new ColorWithValueViewModel((float)oPoints[i].getSample(), oPoints[i].getColor());
		}
		oModel.setColors(oColorsModel);
		
		//get the histogram
		oModel.setHistogramBins(oStats.getHistogramBins());
		oModel.setHistogramWidth((float)oStats.getHistogramBinWidth());
		oModel.setHistogramMin((float)oStats.getMinimum());
		oModel.setHistogramMax((float)oStats.getMaximum());
		
		//get the no data color
		oModel.setNoDataColor(new ColorViewModel(oInfo.getNoDataColor()));
		
		//get the histogram matching
		oModel.setHistogramMatching(oInfo.getHistogramMatching());		
		oModel.setHistogramMathcingValues(HistogramMatching.values());
		
		//get the discrete color option
		oModel.setDiscreteColor(oInfo.getColorPaletteDef().isDiscrete());
		
		return oModel;
	}
	
	
	public void applyColorManipulation(RasterDataNode oInputBand, ColorManipulationViewModel oModel) {
		
		WasdiProgressMonitorStub oProgressMonitor = new WasdiProgressMonitorStub();
		ImageManager.getInstance().prepareImageInfos(new RasterDataNode[] {oInputBand}, oProgressMonitor);
		
		ImageInfo oInfo = oInputBand.getImageInfo();

		//set the point colors
		ColorPaletteDef oPaletteDef = oInfo.getColorPaletteDef();

		Point[] oPoints = new Point[oModel.getColors().length];
		
		for (int i = 0; i < oPoints.length; i++) {
			
			float fSample = oModel.getColors()[i].getValue();
			oPoints[i] = new Point(fSample, oModel.getColors()[i].asColor());
			oPaletteDef.setAutoDistribute(false);
			
		}
		oPaletteDef.setPoints(oPoints);
		
		//set the no data color
		oInfo.setNoDataColor(oModel.getNoDataColor().asColor());
		
		//set the histogram matching
		oInfo.setHistogramMatching(oModel.getHistogramMatching());
		
		//set the discrete color option
		oPaletteDef.setDiscrete(oModel.isDiscreteColor());
		oInputBand.setImageInfo(oInfo);
	}
	
	
	public BufferedImage buildImage(RasterDataNode oInputBand, Dimension oOutputImageSize, Rectangle oInputImageViewPortToRender) {
		
		BufferedImage oOutputBufferedImage = null;
		
		if (oInputBand==null) {
			Utils.debugLog("BandImageManager.buildImage: band null");
			return null;
		}
		if (oInputBand.getProduct()==null) {
			Utils.debugLog("BandImageManager.buildImage: band product null");
			return null;
		}
		
		// NOTE: Codice che crea istogramma
		WasdiProgressMonitorStub oProgressMonitor = new WasdiProgressMonitorStub();
		ImageManager.getInstance().prepareImageInfos(new RasterDataNode[] {oInputBand}, oProgressMonitor);
		
		synchronized (m_oCacheSyncObj) {
			
			long lStartTime = System.currentTimeMillis();

			//check if MultiLevelSource has already computed
			
			//Band oBand = ((Band)oInputBand);
			
			String sProductKey = oInputBand.getProduct().getName() + "_" + oInputBand.getName();
			
	        CachedSource oCachedObj = m_aoSourceCacheMap.get(sProductKey);
	        
	        if (oCachedObj == null) {
	        	oCachedObj = new CachedSource(ColoredBandImageMultiLevelSource.create(oInputBand, ProgressMonitor.NULL));
	        	Utils.debugLog("BandImageManager.buildImage: multi level source not found in cache... created: " + (System.currentTimeMillis() - lStartTime) + " ms");
	        	m_aoSourceCacheMap.put(sProductKey, oCachedObj);
	        }
	        
	        // Get the Source
	        MultiLevelSource oMultiLevelSource = oCachedObj.m_oMultiLevelSource;
	        
	        // update cache timestamp
	        oCachedObj.m_lTimestamp = System.currentTimeMillis();
	        Utils.debugLog("BandImageManager.buildImage: multi level source obtained: " + (System.currentTimeMillis() - lStartTime) + " ms");

	        // Create the Output buffered Image
	        final int iOutputImageWidth = oOutputImageSize.width;
	        final int iOutputImageHeight = oOutputImageSize.height;
	        final int iOutputImageType = BufferedImage.TYPE_3BYTE_BGR;
	        
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
	        
	        Utils.debugLog("BandImageManager.buildImage: init render: " + (System.currentTimeMillis() - lStartTime) + " ms + (" + oSnapImageLayer.getClass().getName() + ")");
	        oSnapImageLayer.render(oImageRendering);
	        Utils.debugLog("BandImageManager.buildImage: render done: " + (System.currentTimeMillis() - lStartTime) + " ms");			
		}
		
        return oOutputBufferedImage;
	}
	
	public BufferedImage buildImageWithMasks(RasterDataNode oInputBand, Dimension oOutputImageSize, Rectangle oInputImageViewPortToRender, boolean bUseCache) {
		return buildImageWithMasks(oInputBand, oOutputImageSize, oInputImageViewPortToRender, bUseCache, false);
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
	
	/**
	 * Return a RGB rendered image from the specified Bands in the original size
	 * @param oRedBand Red Band
	 * @param oGreenBand Green Band
	 * @param oBlueBand Blue Band
	 * @return Rendered Image
	 * @throws IOException
	 */
	public RenderedImage buildRGBImage(RasterDataNode oRedBand, RasterDataNode oGreenBand, RasterDataNode oBlueBand) throws IOException {
		return buildRGBImage(oRedBand,oGreenBand,oBlueBand,null);
	}
	
	/**
	 * Return a scaled RGB rendered image from the specified Bands. If oOutputImageSize returns default dimension
	 * @param oRedBand Red Band
	 * @param oGreenBand Green Band
	 * @param oBlueBand Blue Band
	 * @param oOutputImageSize Ouput Size
	 * @return Rendered Image
	 * @throws IOException
	 */
	public BufferedImage buildRGBImage(RasterDataNode oRedBand, RasterDataNode oGreenBand, RasterDataNode oBlueBand, Dimension oOutputImageSize) throws IOException {
		
		RenderedImage oOutputImage = null;
		
		if (oRedBand==null || oBlueBand == null || oGreenBand == null) {
			Utils.debugLog("BandImageManager.buildImage: band null");
			return null;
		}
		if (oRedBand.getProduct()==null || oBlueBand.getProduct() == null || oGreenBand.getProduct() == null) {
			Utils.debugLog("BandImageManager.buildImage: band product null");
			return null;
		}

		synchronized (m_oCacheSyncObj) {
			
			long lStartTime = System.currentTimeMillis();

	        RasterDataNode aoRasters [] = { oRedBand, oGreenBand, oBlueBand };
	        
	        ImageInfo oInfo = ProductUtils.createImageInfo(aoRasters, true, ProgressMonitor.NULL);
	        oOutputImage = ImageManager.getInstance().createColoredBandImage(aoRasters, oInfo, 0);
	        
	        Utils.debugLog("BandImageManager.buildImage: render done: " + (System.currentTimeMillis() - lStartTime) + " ms");
	        
	        lStartTime = System.currentTimeMillis();
	        
	        if (oOutputImageSize == null) {
	        	oOutputImageSize = new Dimension(oOutputImage.getWidth(), oOutputImage.getHeight());
	        }
	        
	        BufferedImage oResizedImage = new BufferedImage((int) oOutputImageSize.getWidth(), (int) oOutputImageSize.getHeight(),BufferedImage.TYPE_INT_ARGB);
	    	Graphics2D oGraphics = oResizedImage.createGraphics();
	    	
	    	double dX = (oOutputImageSize.getWidth() / (double)oOutputImage.getWidth());
	    	double dY = (oOutputImageSize.getHeight() / (double)oOutputImage.getHeight());
	    	
	    	oGraphics.drawRenderedImage(oOutputImage, AffineTransform.getScaleInstance(dX, dY));
	    	oGraphics.dispose();
	        Utils.debugLog("BandImageManager.buildImage: scaling done: " + (System.currentTimeMillis() - lStartTime) + " ms");
	        
	        return oResizedImage;	        	
		}
	}
	
	
	
	
	/*
	
	public BufferedImage buildRGBImage(RasterDataNode oRedBand, RasterDataNode oGreenBand, RasterDataNode oBlueBand, Dimension oOutputImageSize, boolean bUseCache) throws IOException {
		
		BufferedImage oOutputBufferedImage = null;
		
		if (oRedBand==null || oBlueBand == null || oGreenBand == null) {
			Utils.debugLog("BandImageManager.buildImage: band null");
			return null;
		}
		if (oRedBand.getProduct()==null || oBlueBand.getProduct() == null || oGreenBand.getProduct() == null) {
			Utils.debugLog("BandImageManager.buildImage: band product null");
			return null;
		}
		
		synchronized (m_oCacheSyncObj) {
			
			long lStartTime = System.currentTimeMillis();
			
			//ProductSceneImage oProductSceneImage = new ProductSceneImage("RGB", oRedBand, oGreenBand, oBlueBand, new DefaultPropertyMap(), ProgressMonitor.NULL);
			

	        RasterDataNode aoRasters [] = { oRedBand, oGreenBand, oBlueBand };
	        
	        
	        ColoredBandImageMultiLevelSource oColoredBandImageMultiLevelSource = ColoredBandImageMultiLevelSource.create(aoRasters, ProgressMonitor.NULL);
	        
	        Utils.debugLog("BandImageManager.buildImage: multi level source obtained: " + (System.currentTimeMillis() - lStartTime) + " ms");
	        
	        CollectionLayer oRootLayer = new CollectionLayer();

	        // Create Image Layer
	        
	        final RgbImageLayerType oRgbLayerType = LayerTypeRegistry.getLayerType(RgbImageLayerType.class);
	        final Layer oLayer = oRgbLayerType.createLayer(aoRasters, oColoredBandImageMultiLevelSource);
	        Utils.debugLog("BandImageManager.buildImage: RGB imageLayer created: " + (System.currentTimeMillis() - lStartTime) + " ms");
	        oLayer.setName("RGB");
	        oLayer.setVisible(true);
	        oLayer.setId("org.esa.snap.layers.baseImage");
	        
	        oRootLayer.getChildren().add(oLayer);
	        	        
	        Utils.debugLog("BandImageManager.buildImage: init render: " + (System.currentTimeMillis() - lStartTime) + " ms ");
	        
	        // Create the Output buffered Image
	        final int iOutputImageWidth = oOutputImageSize.width;
	        final int iOutputImageHeight = oOutputImageSize.height;
	        final int iOutputImageType = BufferedImage.TYPE_3BYTE_BGR;
	        oOutputBufferedImage = new BufferedImage(iOutputImageWidth, iOutputImageHeight, iOutputImageType);
	        
	        final BufferedImageRendering oImageRendering = new BufferedImageRendering(oOutputBufferedImage);

	        // Clear the background
	        final Graphics2D graphics = oImageRendering.getGraphics();
	        graphics.setColor(Color.BLACK);
	        graphics.fillRect(0, 0, iOutputImageWidth, iOutputImageHeight);
	        
	        oRootLayer.render(oImageRendering);
	        Utils.debugLog("BandImageManager.buildImage: render done: " + (System.currentTimeMillis() - lStartTime) + " ms");			
		}
		
        return oOutputBufferedImage;
	}
	
	
	
	public BufferedImage buildRGBImage3(RasterDataNode oRedBand, RasterDataNode oGreenBand, RasterDataNode oBlueBand, Dimension oOutputImageSize, boolean bUseCache) throws IOException {
		
		BufferedImage oOutputBufferedImage = null;
		
		if (oRedBand==null || oBlueBand == null || oGreenBand == null) {
			Utils.debugLog("BandImageManager.buildImage: band null");
			return null;
		}
		if (oRedBand.getProduct()==null || oBlueBand.getProduct() == null || oGreenBand.getProduct() == null) {
			Utils.debugLog("BandImageManager.buildImage: band product null");
			return null;
		}
		
		synchronized (m_oCacheSyncObj) {
			
			long lStartTime = System.currentTimeMillis();
			
			ProductSceneImage oProductSceneImage = new ProductSceneImage("RGB", oRedBand, oGreenBand, oBlueBand, new DefaultPropertyMap(), ProgressMonitor.NULL);
			
			//DefaultViewport oViewport = new DefaultViewport(isModelYAxisDown((ImageLayer)oProductSceneImage.getRootLayer().getChildren().get(0)));
			
	        // Create the Output buffered Image
	        final int iOutputImageWidth = oOutputImageSize.width;
	        final int iOutputImageHeight = oOutputImageSize.height;
	        final int iOutputImageType = BufferedImage.TYPE_3BYTE_BGR;
	        oOutputBufferedImage = new BufferedImage(iOutputImageWidth, iOutputImageHeight, iOutputImageType);
	        
	        final BufferedImageRendering oImageRendering = new BufferedImageRendering(oOutputBufferedImage);

	        // Clear the background
	        final Graphics2D graphics = oImageRendering.getGraphics();
	        graphics.setColor(Color.BLACK);
	        graphics.fillRect(0, 0, iOutputImageWidth, iOutputImageHeight);
	        
	        oProductSceneImage.getRootLayer().render(oImageRendering);
	        Utils.debugLog("BandImageManager.buildImage: render done: " + (System.currentTimeMillis() - lStartTime) + " ms");			
		}
		
        return oOutputBufferedImage;
	}
*/
	
	public RenderedImage buildRGBImage4(RasterDataNode oRedBand, RasterDataNode oGreenBand, RasterDataNode oBlueBand, Dimension oOutputImageSize, boolean bUseCache) throws IOException {
		
		RenderedImage oRenderedImage = null;
		
		if (oRedBand==null || oBlueBand == null || oGreenBand == null) {
			Utils.debugLog("BandImageManager.buildImage: band null");
			return null;
		}
		if (oRedBand.getProduct()==null || oBlueBand.getProduct() == null || oGreenBand.getProduct() == null) {
			Utils.debugLog("BandImageManager.buildImage: band product null");
			return null;
		}

		synchronized (m_oCacheSyncObj) {
			
			long lStartTime = System.currentTimeMillis();

	        RasterDataNode aoRasters [] = { oRedBand, oGreenBand, oBlueBand };
	        
	        ImageInfo oInfo = ProductUtils.createImageInfo(aoRasters, true, ProgressMonitor.NULL);
	        oRenderedImage = ImageManager.getInstance().createColoredBandImage(aoRasters, oInfo, 0);
	        
	        Utils.debugLog("BandImageManager.buildImage: render done: " + (System.currentTimeMillis() - lStartTime) + " ms");
	        lStartTime = System.currentTimeMillis();
	        
	        CollectionLayer oCollectionLayer = new CollectionLayer();

	        //commented out the next two lines, as they are not used
//	        LayerContext oContext = new MyLayerContext (m_oProduct,oCollectionLayer);
	        
//	        SceneTransformProvider oProvider = oRedBand;
	        
	        // RGB
	        ImageLayer oRGBLayer = new ImageLayer(oRenderedImage);
	        BackgroundLayer oBackgroundLayer = new BackgroundLayer(Color.BLACK);
	        
	        BufferedImage buffered = new BufferedImage(oRenderedImage.getWidth(), oRenderedImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
	        BufferedImageRendering rendering = new BufferedImageRendering(buffered);
	        
	        oCollectionLayer.getChildren().add(oBackgroundLayer);
	        oCollectionLayer.getChildren().add(oRGBLayer);
	        
	        oCollectionLayer.render(rendering);
	        
	        Utils.debugLog("BandImageManager.buildImage: render 2 done: " + (System.currentTimeMillis() - lStartTime) + " ms");
	        lStartTime = System.currentTimeMillis();


	        JAI.create("filestore", rendering.getImage(), "C:\\temp\\ORAVEDIAMO2.JPG", "JPEG");
	        
	        Utils.debugLog("BandImageManager.buildImage: render 3 done: " + (System.currentTimeMillis() - lStartTime) + " ms");			
		}
		
        return oRenderedImage;
	}

    private static BufferedImageRendering createRendering(BufferedImage bufferedImage, MultiLevelModel multiLevelModel) {
    	
        AffineTransform m2iTransform = multiLevelModel.getModelToImageTransform(0);
        final Viewport vp2 = new DefaultViewport(new Rectangle(bufferedImage.getWidth(), bufferedImage.getHeight()),
                                                 m2iTransform.getDeterminant() > 0.0);
        vp2.zoom(multiLevelModel.getModelBounds());

        final BufferedImageRendering imageRendering = new BufferedImageRendering(bufferedImage, vp2);
        // because image to model transform is stored with the exported image we have to invert
        // image to view transformation
        final AffineTransform v2mTransform = vp2.getViewToModelTransform();
        v2mTransform.preConcatenate(m2iTransform);
        final AffineTransform v2iTransform = new AffineTransform(v2mTransform);

        final Graphics2D graphics2D = imageRendering.getGraphics();
        v2iTransform.concatenate(graphics2D.getTransform());
        graphics2D.setTransform(v2iTransform);
        return imageRendering;
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
		if (m_oCacheThread!=null) m_oCacheThread.interrupt();
	}
	
	private static class ImageLayerFilter implements LayerFilter {

        @Override
        public boolean accept(Layer layer) {
            return layer instanceof ImageLayer;
        }
    }
	
}
