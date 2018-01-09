import java.io.File;
import java.io.IOException;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ConvolutionFilterBand;
import org.esa.snap.core.datamodel.FilterBand;
import org.esa.snap.core.datamodel.GeneralFilterBand;
import org.esa.snap.core.datamodel.Kernel;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.rcp.imgfilter.model.Filter;
import org.esa.snap.rcp.imgfilter.model.StandardFilters;

import com.bc.ceres.core.ProgressMonitor;

public class Proofs_FilterBand {

	
	public static void main(String[] args) throws Exception {
		
		
//		System.setProperty("user.home", "/home/doy");
//        Path propFile = Paths.get("/home/doy/workspaces/wasdi/server/launcher/resources/config.properties");
//        Config.instance("snap.auxdata").load(propFile);
//        Config.instance().load();
//        SystemUtils.init3rdPartyLibs(null);
//        Engine.start(false);

		File file = new File("/home/doy/tmp/wasdi/tmp/S1A_IW_GRDH_1SDV_20171128T054335_20171128T054400_019461_02104F_DFC1.zip");

		Product product = ProductIO.readProduct(file);
		String bandName = "Amplitude_VH";
		
		RasterDataNode node = product.getRasterDataNode(bandName); 
		Filter filter = StandardFilters.SMOOTHING_FILTERS[0];
	
		String newBandName = bandName + "_" + filter.getShorthand();
		File fileOut = new File("/home/doy/tmp/wasdi/tmp/" + newBandName + ".tif");
		
		FilterBand computedBand = getFilterBand(node, newBandName, filter, 10);
		
		Band realBand = new Band(newBandName, computedBand.getDataType(), computedBand.getRasterWidth(), computedBand.getRasterHeight());
		realBand.setSourceImage(computedBand.getSourceImage());
		
		System.out.println("filter band created");
		
		// Get TIFF Metadata
		System.out.println("create product");
		Product outProduct = new Product(newBandName, "GEOTIFF");
		ProductUtils.copyGeoCoding(product, outProduct);
		outProduct.addBand(realBand);
		System.out.println("create geotiff");
		ProductIO.writeProduct(outProduct, fileOut, "GEOTIFF", true, new ProgressMonitor() {
			
			@Override
			public void worked(int work) {
				System.out.println("worked: " + work);
			}
			
			@Override
			public void setTaskName(String taskName) {
				System.out.println("task name: " + taskName);				
			}
			
			@Override
			public void setSubTaskName(String subTaskName) {
				System.out.println("sub task name: " + subTaskName);
			}
			
			@Override
			public void setCanceled(boolean canceled) {
				System.out.println("cancelled: " + canceled);
			}
			
			@Override
			public boolean isCanceled() {
				return false;
			}
			
			@Override
			public void internalWorked(double work) {
				System.out.println("internal worked: " + work);
			}
			
			@Override
			public void done() {
				System.out.println("done");
			}
			
			@Override
			public void beginTask(String taskName, int totalWork) {
				System.out.println("begin task " + taskName + ". work: " + totalWork);
			}
		} );
		System.out.println("geotiff created");
		
		
		
//		MultiLevelImage bandImage = computedBand.getSourceImage();
//		System.out.println("surce image created");
//		GeoTIFFMetadata metadata = ProductUtils.createGeoTIFFMetadata(product);
//		System.out.println("geotiff metadata created");
//	    GeoTIFF.writeImage(bandImage, fileOut, metadata);
//	    System.out.println("geotiff created");

		
		
//		System.out.println("create product");
//		Product outProduct = new Product(newBandName, "GEOTIFF");
//		ProductUtils.copyGeoCoding(product, outProduct);
//		outProduct.addBand(computedBand);
//		System.out.println("create geotiff");
//		ProductIO.writeProduct(outProduct, fileOut, "GEOTIFF", true);
//		System.out.println("geotiff created");

		
		
		
		
		
		
		
		
//		Band realBand = new Band(newBandName, computedBand.getDataType(), computedBand.getRasterWidth(), computedBand.getRasterHeight());
//        realBand.setSourceImage(computedBand.getSourceImage());
////        realBand.setModified(true);
//        realBand.ensureRasterData();
//        realBand.readRasterDataFully(new ProgressMonitor() {
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
//				System.out.println("internal worked: " + work);
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
//		});
		
        
        
//		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
//		ImageOutputStream output = ImageIO.createImageOutputStream(bytes);
//		ProductData data = realBand.getData();
//		
//		
//		System.out.println("writing...");
//		data.writeTo(output);
//		System.out.println("written");
//		
//		
//		
//		
//		System.out.println("size = " + dataStream.size());
		

		
		
		
//		RasterDataNode raster = fb.getSource();
//		ColoredBandImageMultiLevelSource imgSource = ColoredBandImageMultiLevelSource.create(raster, ProgressMonitor.NULL);
//		RenderedImage img = imgSource.getImage(0);

//		ImageIO.write(img, "jpg", new File("/home/doy/tmp/wasdi/tmp/" + newBandName + ".jpg"));
//		System.out.println("jpg created");
		
//		GeoTIFFMetadata metadata = ProductUtils.createGeoTIFFMetadata(product);//GeoCoding2GeoTIFFMetadata.createGeoTIFFMetadata(geoCoding, bandImage.getWidth(),bandImage.getHeight());
//		System.out.println("geotiff metadata created");
//	    GeoTIFF.writeImage(img, fileOut, metadata);
//	    System.out.println("geotiff created");
		
//		writeBandGeotiff(product, newBandName, fileOut, realBand);
		
		
//		MultiLevelImage bandImage = fb.getSourceImage();
//		
//		System.out.println("surce image created");
//		
//		// Get TIFF Metadata
//		GeoTIFFMetadata metadata = ProductUtils.createGeoTIFFMetadata(product);//GeoCoding2GeoTIFFMetadata.createGeoTIFFMetadata(geoCoding, bandImage.getWidth(),bandImage.getHeight());
//		
//		System.out.println("geotiff metadata created");
//		
//	    GeoTIFF.writeImage(bandImage, fileOut, metadata);
//	    
//	    System.out.println("geotiff created");
	
	}


	private static void writeBandGeotiff(Product product, String name, File fileOut, Band band) throws IOException {
		System.out.println("create product");
		Product outProduct = new Product(name, "GEOTIFF");
		ProductUtils.copyGeoCoding(product, outProduct);
		outProduct.addBand(band);
//		GeoTiffProductWriter writer = (GeoTiffProductWriter)new GeoTiffProductWriterPlugIn().createWriterInstance();
		System.out.println("create geotiff");
//		writer.writeProductNodes(outProduct, fileOut);
		ProductIO.writeProduct(outProduct, fileOut, "GEOTIFF", true);
		System.out.println("geotiff created");
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
	
    private static FilterBand getFilterBand(RasterDataNode sourceRaster, String bandName, Filter filter, int iterationCount) {
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
