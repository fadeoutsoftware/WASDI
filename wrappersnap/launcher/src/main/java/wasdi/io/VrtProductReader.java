package wasdi.io;

import java.io.File;
import java.util.ArrayList;

import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.products.BandViewModel;
import wasdi.shared.viewmodels.products.GeorefProductViewModel;
import wasdi.shared.viewmodels.products.MetadataViewModel;
import wasdi.shared.viewmodels.products.NodeGroupViewModel;
import wasdi.shared.viewmodels.products.ProductViewModel;

public class VrtProductReader extends WasdiProductReader {

	public VrtProductReader(File oProductFile) {
		super(oProductFile);
	}

	@Override
	public ProductViewModel getProductViewModel() {
		
		if (m_oProductFile == null) return null;
		
    	// Create the return value
    	GeorefProductViewModel oRetViewModel = null;
    	
    	try {
    		
        	// Create the Product View Model
        	oRetViewModel = new GeorefProductViewModel();
        	
        	// Set name values
        	oRetViewModel.setFileName(m_oProductFile.getName());
        	oRetViewModel.setName(WasdiFileUtils.getFileNameWithoutLastExtension(m_oProductFile.getName()));
        	oRetViewModel.setProductFriendlyName(oRetViewModel.getName());
        	
        	// Create the sub folder
        	NodeGroupViewModel oNodeGroupViewModel = new NodeGroupViewModel();
        	oNodeGroupViewModel.setNodeName("VRT");
        	
        	// Create the single band representing the shape
        	BandViewModel oBandViewModel = new BandViewModel();
        	oBandViewModel.setPublished(false);
        	oBandViewModel.setGeoserverBoundingBox("");
        	oBandViewModel.setHeight(0);
        	oBandViewModel.setWidth(0);
        	oBandViewModel.setPublished(false);
        	oBandViewModel.setName("VRT Fake Band");
        	
        	ArrayList<BandViewModel> oBands = new ArrayList<BandViewModel>();
        	oBands.add(oBandViewModel);
        	
        	oNodeGroupViewModel.setBands(oBands);
        	
        	oRetViewModel.setBandsGroups(oNodeGroupViewModel);
    	}
    	catch (Exception oEx) {
    		WasdiLog.debugLog("WasdiProductReader.getShapeFileProduct: exception reading the shape file");
		}
    	
    	return oRetViewModel;
	}

	@Override
	public String getProductBoundingBox() {
		return null;
	}

	@Override
	public MetadataViewModel getProductMetadataViewModel() {
		return new MetadataViewModel("Metadata");
	}

	@Override
	public String adjustFileAfterDownload(String sDownloadedFileFullPath, String sFileNameFromProvider) {
		return sDownloadedFileFullPath;
	}

	@Override
	public File getFileForPublishBand(String sBand, String sLayerId) {
		return null;
	}
}
