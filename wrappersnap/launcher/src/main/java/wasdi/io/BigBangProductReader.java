package wasdi.io;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.esa.snap.core.datamodel.Product;

import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.products.BandViewModel;
import wasdi.shared.viewmodels.products.MetadataViewModel;
import wasdi.shared.viewmodels.products.NodeGroupViewModel;
import wasdi.shared.viewmodels.products.ProductViewModel;

public class BigBangProductReader extends WasdiProductReader {

	public BigBangProductReader(File oProductFile) {
		super(oProductFile);
	}

	@Override
	public ProductViewModel getProductViewModel() {
		ProductViewModel oViewModel = new ProductViewModel();

        String sFileName = m_oProductFile != null ? m_oProductFile.getName() : "no_file_name";
        
    	oViewModel.setFileName(sFileName);
    	oViewModel.setName(WasdiFileUtils.getFileNameWithoutLastExtension(sFileName));
		oViewModel.setProductFriendlyName(WasdiFileUtils.getFileNameWithoutLastExtension(sFileName));
        
        NodeGroupViewModel oNodeGroupViewModel = new NodeGroupViewModel();
    	oNodeGroupViewModel.setNodeName("Bands");
    	
    	// so far, we do not try to read the bands
    	List<BandViewModel> oBands = new ArrayList<>();
    	oNodeGroupViewModel.setBands(oBands);
    	oViewModel.setBandsGroups(oNodeGroupViewModel);
        	
		return oViewModel;
	}

	@Override
	public String getProductBoundingBox() {
		float fS = 35.4922F;
		float fN = 47.0920F;
		float fW = 6.6267F;
		float fE = 18.5205F;
		
		return String.format("%f,%f,%f,%f,%f,%f,%f,%f,%f,%f", 
				fS, fW, fS, fE, fN, fE, fN, fW, fS, fW);
		
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
    	WasdiLog.debugLog("BigBangProductReader.getFileForPublishBand: no bands for BigBang products");
		return null;
	}
	
	@Override
	protected Product readSnapProduct() {
    	WasdiLog.debugLog("BigBangProductReader.readSnapProduct: we do not want SNAP to read BigBang products, return null ");
    	return null;        	
	}

}
