package wasdi.io;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.products.BandViewModel;
import wasdi.shared.viewmodels.products.MetadataViewModel;
import wasdi.shared.viewmodels.products.NodeGroupViewModel;
import wasdi.shared.viewmodels.products.ProductViewModel;

public class TeresaZipFileReader extends WasdiProductReader {

	public TeresaZipFileReader(File oProductFile) {
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
		String sFileName = m_oProductFile.getName();
		
		if (sFileName.contains("Po")) {
			float fS = 44.05F;
			float fN = 46.62F;
			float fW = 6.55F;
			float fE = 12.55F;
			
			return String.format("%f,%f,%f,%f,%f,%f,%f,%f,%f,%f", 
					fS, fW, fS, fE, fN, fE, fN, fW, fS, fW);
		}
		else if (sFileName.contains("Dan")) {
			float fS = 43.00F;
			float fN = 50.00F;
			float fW = 15.00F;
			float fE = 27.50F;
			
			return String.format("%f,%f,%f,%f,%f,%f,%f,%f,%f,%f", 
					fS, fW, fS, fE, fN, fE, fN, fW, fS, fW);
		}
		else if (sFileName.contains("Oum")) {
			float fS = 31.33F;
			float fN = 33.32F;
			float fW = -9.32F;
			float fE = -5.06F;
			
			return String.format("%f,%f,%f,%f,%f,%f,%f,%f,%f,%f", 
					fS, fW, fS, fE, fN, fE, fN, fW, fS, fW);
		}
		else if (sFileName.contains("Guadalquivr")) {
			float fS = 37.00F;
			float fN = 39.00F;
			float fW = -4.50F;
			float fE = -2.00F;
			
			return String.format("%f,%f,%f,%f,%f,%f,%f,%f,%f,%f", 
					fS, fW, fS, fE, fN, fE, fN, fW, fS, fW);
			
		}
		
		return "";
	}

	@Override
	public MetadataViewModel getProductMetadataViewModel() {
		WasdiLog.warnLog("TeresaZipFileReader.getProductMetadataViewModel. No metadata available for file " + m_oProductFile.getName());
		return null;
	}
	
	@Override
	public File getFileForPublishBand(String sBand, String sLayerId, String sPlatform) {
    	WasdiLog.debugLog("TeresaZipFileReader.getFileForPublishBand: no bands for Teresa zip file products");
		return null;
	}

}
