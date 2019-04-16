package wasdi.snapopearations;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import wasdi.LauncherMain;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.AttributeViewModel;
import wasdi.shared.viewmodels.BandViewModel;
import wasdi.shared.viewmodels.MetadataViewModel;
import wasdi.shared.viewmodels.NodeGroupViewModel;
import wasdi.shared.viewmodels.ProductViewModel;

/**
 * Read SNAP Product utility class
 * Created by s.adamo on 18/05/2016.
 */
public class ReadProduct {


    /**
     * Read a Satellite Product 
     * @param oFile File to open
     * @param sFormatName Format, if known.
     * @return Product object
     */
    public Product readSnapProduct(File oFile, String sFormatName) {
    	
        Product oProduct = null;

        // P.Campanella 2019/04/16: deleted the static cache. There is a new instance of this class every time is used
        // so the cache was useless and could have memory problems
        
        try {

            LauncherMain.s_oLogger.debug("ReadProduct.ReadProduct: begin read");

            if (sFormatName != null) {
                oProduct = ProductIO.readProduct(oFile, sFormatName);
            } 
            else {
                oProduct = ProductIO.readProduct(oFile);
            }
                
            return oProduct;
            
        } catch (Exception oEx) {
            oEx.printStackTrace();
            LauncherMain.s_oLogger.debug("ReadProduct.ReadProduct: excetpion: " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
        }


        return null;
    }

    /**
     * discover the format of the product contained in oFile
     * @param oFile
     * @return the format name if the reader plugin manage one and only one format. null otherwise
     */
    public String getProductFormat(File oFile) {
        ProductReader oProductReader = ProductIO.getProductReaderForInput(oFile);
        ProductReaderPlugIn oPlugin = oProductReader.getReaderPlugIn();
        String[] asFormats = oPlugin.getFormatNames();    	        
        if (asFormats==null || asFormats.length != 1) return null;        
        return asFormats[0];
    }
    
    /**
     * Converts a product in a View Model
     * @param oFile
     * @return
     * @throws IOException
     */
    public ProductViewModel getProductViewModel(File oFile) throws IOException
    {
        LauncherMain.s_oLogger.debug("ReadProduct.getProductViewModel: start");

        Product exportProduct = readSnapProduct(oFile, null);

        if (exportProduct == null) {
            LauncherMain.s_oLogger.debug("ReadProduct.getProductViewModel: read product returns null");
            return null;
        }

        ProductViewModel oViewModel = getProductViewModel(exportProduct, oFile);

        return  oViewModel;
    }

    /**
     * Converts a product in a View Model
     * @param exportProduct
     * @return
     */
	public ProductViewModel getProductViewModel(Product exportProduct, File oFile) {
		
		// Create View Model
		ProductViewModel oViewModel = new ProductViewModel();

        LauncherMain.s_oLogger.debug("ReadProduct.getProductViewModel: call fill bands view model");

        // Get Bands
        this.FillBandsViewModel(oViewModel, exportProduct);

        LauncherMain.s_oLogger.debug("ReadProduct.getProductViewModel: setting Name and Path");

//        File oFile = exportProduct.getFileLocation();
        
        // Set name and path
        oViewModel.setName(exportProduct.getName());
        if (oFile!=null) oViewModel.setFileName(oFile.getName());

        LauncherMain.s_oLogger.debug("ReadProduct.getProductViewModel: end");
		return oViewModel;
	}

	/**
	 * Get Product Bounding Box from File
	 * @param oProductFile
	 * @return "%f,%f,%f,%f,%f,%f,%f,%f,%f,%f", minY, minX, minY, maxX, maxY, maxX, maxY, minX, minY, minX
	 */
	public String getProductBoundingBox(File oProductFile) {
		
		try {
			Product oProduct = ProductIO.readProduct(oProductFile);
			
//			CoordinateReferenceSystem crs = oProduct.getSceneCRS();
			GeoCoding geocoding = oProduct.getSceneGeoCoding();
			if (geocoding!=null) {
				Dimension dim = oProduct.getSceneRasterSize();		
				GeoPos min = geocoding.getGeoPos(new PixelPos(0,0), null);
				GeoPos max = geocoding.getGeoPos(new PixelPos(dim.getWidth(), dim.getHeight()), null);
				float minX = (float) Math.min(min.lon, max.lon);
				float minY = (float) Math.min(min.lat, max.lat);
				float maxX = (float) Math.max(min.lon, max.lon);
				float maxY = (float) Math.max(min.lat, max.lat);
				
//				Integer epsgCode = CRS.lookupEpsgCode(crs, true);
//				String epsg = "EPSG:" + (epsgCode==null ? 4326 : epsgCode);
//				return String.format("{\"miny\":%f,\"minx\":%f,\"crs\":\"%s\",\"maxy\":%f,\"maxx\":%f", minY, minX, epsg, maxY, maxX);
				
				return String.format("%f,%f,%f,%f,%f,%f,%f,%f,%f,%f", minY, minX, minY, maxX, maxY, maxX, maxY, minX, minY, minX);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return "";
	}
	
	
	/**
	 * Get the metadata View Model of a Product
	 * @param oFile
	 * @return
	 * @throws IOException
	 */
    public MetadataViewModel getProductMetadataViewModel(File oFile) throws IOException
    {
        Product exportProduct = readSnapProduct(oFile, null);

        if (exportProduct == null) return null;

        return  GetMetadataViewModel(exportProduct.getMetadataRoot(), new MetadataViewModel("Metadata"));
    }

    /**
     * Recursive Metadata Explorer Function
     * @param oElement
     * @param oSourceViewModel
     * @return
     */
    private MetadataViewModel GetMetadataViewModel(MetadataElement oElement, MetadataViewModel oSourceViewModel) {

    	// For Each Attribute
        for (MetadataAttribute oMetadataAttribute : oElement.getAttributes()) {
        	
        	// Data Exists ?
            if (oMetadataAttribute.getData() != null) {
            	
            	// Create Attribute View Model
                AttributeViewModel oAttributeViewModel = new AttributeViewModel();
                
                // Leave the name: this is a code...
                //oAttributeViewModel.setName(oMetadataAttribute.getName());
                
                if (!Utils.isNullOrEmpty(oMetadataAttribute.getDescription())) {
                	oAttributeViewModel.setDescription(oMetadataAttribute.getDescription());
                }
                else if (!Utils.isNullOrEmpty(oMetadataAttribute.getName())) {
                	oAttributeViewModel.setDescription(oMetadataAttribute.getName());
                }
                
            	oAttributeViewModel.setData(oMetadataAttribute.getData().toString());
            	
                if (oSourceViewModel.getAttributes() == null) oSourceViewModel.setAttributes(new ArrayList<AttributeViewModel>());
                
                oSourceViewModel.getAttributes().add(oAttributeViewModel);
            }
        }


        for (MetadataElement oMetadataElement : oElement.getElements()) {
            MetadataViewModel oElementViewModel = new MetadataViewModel(oMetadataElement.getName());
            
            if (oSourceViewModel.getElements() == null) {
            	oSourceViewModel.setElements(new ArrayList<MetadataViewModel>());
            }
            
            oSourceViewModel.getElements().add(GetMetadataViewModel(oMetadataElement, oElementViewModel));
        }

        return  oSourceViewModel;
    }

    /**
     * Fills Band View Models
     * @param oProductViewModel
     * @param oProduct
     */
    private void FillBandsViewModel(ProductViewModel oProductViewModel, Product oProduct)
    {
        if (oProductViewModel == null) {
            LauncherMain.s_oLogger.debug("ReadProduct.FillBandsViewModel: ViewModel null return");
            return;
        }

        if (oProduct == null) {
            LauncherMain.s_oLogger.debug("ReadProduct.FillBandsViewModel: Product null");
            return;
        }

        if (oProductViewModel.getBandsGroups() == null) oProductViewModel.setBandsGroups(new NodeGroupViewModel("Bands"));

        for (Band oBand : oProduct.getBands()) {
            LauncherMain.s_oLogger.debug("ReadProduct.FillBandsViewModel: add band " + oBand.getName());

            if (oProductViewModel.getBandsGroups().getBands() == null)
                oProductViewModel.getBandsGroups().setBands(new ArrayList<BandViewModel>());

            BandViewModel oViewModel = new BandViewModel(oBand.getName());
            oViewModel.setWidth(oBand.getRasterWidth());
            oViewModel.setHeight(oBand.getRasterHeight());
            oProductViewModel.getBandsGroups().getBands().add(oViewModel);
        }

    }

}
