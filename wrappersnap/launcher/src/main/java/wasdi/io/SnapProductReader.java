package wasdi.io;

import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;

import wasdi.LauncherMain;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.products.*;

public class SnapProductReader extends WasdiProductReader {

	public SnapProductReader(File oProductFile) {
		super(oProductFile);
	}
	public ProductViewModel getProductViewModel() {
		
		LauncherMain.s_oLogger.debug("SnapProductReader.getProductViewModel: start");
		
		// Create View Model
		ProductViewModel oViewModel = new ProductViewModel();

        // Get Bands
        this.getSnapProductBandsViewModel(oViewModel, getSnapProduct());
        
        // Set name and path
        if (m_oProduct != null) oViewModel.setName(m_oProduct.getName());
        if (m_oProductFile!=null) oViewModel.setFileName(m_oProductFile.getName());

        LauncherMain.s_oLogger.debug("SnapProductReader.getProductViewModel: done");
		return oViewModel;
	}

    /**
     * Fills Band View Models
     * @param oProductViewModel
     * @param oProduct
     */
    protected void getSnapProductBandsViewModel(ProductViewModel oProductViewModel, Product oProduct)
    {
        if (oProductViewModel == null) {
            LauncherMain.s_oLogger.debug("SnapProductReader.FillBandsViewModel: ViewModel null return");
            return;
        }

        if (oProduct == null) {
            LauncherMain.s_oLogger.debug("SnapProductReader.FillBandsViewModel: Product null");
            return;
        }

        if (oProductViewModel.getBandsGroups() == null) oProductViewModel.setBandsGroups(new NodeGroupViewModel("Bands"));

        LauncherMain.s_oLogger.debug("SnapProductReader.FillBandsViewModel: add bands");
        
        for (Band oBand : oProduct.getBands()) {

            if (oProductViewModel.getBandsGroups().getBands() == null)
                oProductViewModel.getBandsGroups().setBands(new ArrayList<BandViewModel>());

            BandViewModel oViewModel = new BandViewModel(oBand.getName());
            oViewModel.setWidth(oBand.getRasterWidth());
            oViewModel.setHeight(oBand.getRasterHeight());
            oProductViewModel.getBandsGroups().getBands().add(oViewModel);
        }

    }
	@Override
	public String getProductBoundingBox() {
		
		Product oProduct = getSnapProduct();
		
		if (oProduct == null) {
			LauncherMain.s_oLogger.info("SnapProductReader.getProductBoundingBox: product is null return empty ");
			return "";
		}
		String sBB = "";
		try {
			
			// Snap Bounding Box
			GeoCoding oGeocoding = oProduct.getSceneGeoCoding();
			if (oGeocoding!=null) {
				Dimension oDim = oProduct.getSceneRasterSize();		
				GeoPos oMin = oGeocoding.getGeoPos(new PixelPos(0,0), null);
				GeoPos oMax = oGeocoding.getGeoPos(new PixelPos(oDim.getWidth(), oDim.getHeight()), null);
				float fMinX = (float) Math.min(oMin.lon, oMax.lon);
				float fMinY = (float) Math.min(oMin.lat, oMax.lat);
				float fMaxX = (float) Math.max(oMin.lon, oMax.lon);
				float fMaxY = (float) Math.max(oMin.lat, oMax.lat);
				
				sBB = String.format("%f,%f,%f,%f,%f,%f,%f,%f,%f,%f", fMinY, fMinX, fMinY, fMaxX, fMaxY, fMaxX, fMaxY, fMinX, fMinY, fMinX);
			}

			return sBB;
		} catch (Exception e) {
			LauncherMain.s_oLogger.error("SnapProductReader.getProductBoundingBox: Exception " + e.getMessage());
		}
		
		return "";
	}
	@Override
	public MetadataViewModel getProductMetadataViewModel() {
		m_oProduct = getSnapProduct();
		if (m_oProduct == null) return null;
		return  getSnapProductMetadataViewModel(m_oProduct.getMetadataRoot(), new MetadataViewModel("Metadata"));
	}
	
    /**
     * Recursive Metadata Explorer Function
     * @param oElement
     * @param oSourceViewModel
     * @return
     */
    private MetadataViewModel getSnapProductMetadataViewModel(MetadataElement oElement, MetadataViewModel oSourceViewModel) {

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
            
            oSourceViewModel.getElements().add(getSnapProductMetadataViewModel(oMetadataElement, oElementViewModel));
        }

        return  oSourceViewModel;
    }
	
}
