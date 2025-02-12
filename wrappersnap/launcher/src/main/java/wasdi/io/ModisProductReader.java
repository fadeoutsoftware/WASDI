package wasdi.io;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.esa.snap.core.datamodel.Product;

import wasdi.shared.business.modis11a2.ModisItemForReading;
import wasdi.shared.business.modis11a2.ModisLocation;
import wasdi.shared.data.modis11a2.ModisRepository;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.products.BandViewModel;
import wasdi.shared.viewmodels.products.MetadataViewModel;
import wasdi.shared.viewmodels.products.NodeGroupViewModel;
import wasdi.shared.viewmodels.products.ProductViewModel;

public class ModisProductReader extends WasdiProductReader {

	public ModisProductReader(File oProductFile) {
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
    	List<BandViewModel> oBands = new ArrayList<>();
    	oNodeGroupViewModel.setBands(oBands);
    	oViewModel.setBandsGroups(oNodeGroupViewModel);
        	
		return oViewModel;
	}

	@Override
	public String getProductBoundingBox() {
		if (m_oProductFile != null)  {
			
	    	WasdiLog.debugLog("ModisProductReader.getProductBoundingBox: reading the product bounding box from the db");
			ModisRepository oRepo = new ModisRepository();
			List<ModisItemForReading> oRes = oRepo.getModisItemList(null, null, null, null, null, null, 0, 10, m_oProductFile.getName());
			if (oRes.size() == 1) {
				ModisItemForReading oItem = oRes.get(0);
				ModisLocation oLocation = oItem.getBoundingBox();
				List<List<List<Double>>> aoCoordinate = oLocation.getCoordinates();
				if (!aoCoordinate.isEmpty() && aoCoordinate.size() == 1) {
					List<List<Double>> aoPointsPairs = aoCoordinate.get(0);
					List<Double> adPointsX = new ArrayList<>();
					List<Double> adPointsY = new ArrayList<>();
					for (List<Double> adPoints : aoPointsPairs) {
						if (adPoints.size() == 2) {
							adPointsX.add(adPoints.get(0));
							adPointsY.add(adPoints.get(1));
						}
					}
					if (!adPointsX.isEmpty() && !adPointsY.isEmpty()) {
						double fMinX = Collections.min(adPointsX);
						double fMinY = Collections.min(adPointsY);
						double fMaxX = Collections.max(adPointsX);
						double fMaxY = Collections.max(adPointsY);
						
						String sRes = String.format("%f,%f,%f,%f,%f,%f,%f,%f,%f,%f", 
								(float) fMinY, (float) fMinX, (float) fMinY, (float) fMaxX, (float) fMaxY, (float) fMaxX, (float) fMaxY, (float) fMinX, (float) fMinY, (float) fMinX);
						
						WasdiLog.debugLog("ModisProductReader.getProductBoundingBox: reading the product bounding box from the db"); 
						
						return  sRes;
					}
				}
 			}
		}
    	WasdiLog.debugLog("ModisProductReader.getProductBoundingBox: bounding box not read");
		return "";
	}

	@Override
	public MetadataViewModel getProductMetadataViewModel() {
		return new MetadataViewModel("Metadata");
	}

	@Override
	public File getFileForPublishBand(String sBand, String sLayerId) {
		return null;
	}
	
	
	@Override
	protected Product readSnapProduct() {
    	WasdiLog.debugLog("ModisProductReader.readSnapProduct: we do not want SNAP to read MODIS products, return null ");
    	return null;        	
	}
	

}
