package wasdi.io;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.esa.snap.core.datamodel.Product;

import wasdi.shared.viewmodels.products.BandViewModel;
import wasdi.shared.viewmodels.products.MetadataViewModel;
import wasdi.shared.viewmodels.products.NodeGroupViewModel;
import wasdi.shared.viewmodels.products.ProductViewModel;

public class ImagesProductReader extends WasdiProductReader {

	public ImagesProductReader(File oProductFile) {
		super(oProductFile);
	}

	@Override
	public ProductViewModel getProductViewModel() {
		ProductViewModel oPdfViewModel = new ProductViewModel();
		
		oPdfViewModel.setFileName(m_oProductFile.getName());
		oPdfViewModel.setName(m_oProductFile.getName());
		NodeGroupViewModel oNodeGroupViewModel = new NodeGroupViewModel();
		List<BandViewModel> aoBandsViewModels = new ArrayList<>();
		oNodeGroupViewModel.setNodeName("Bands");
		oNodeGroupViewModel.setBands(aoBandsViewModels);
		oPdfViewModel.setBandsGroups(oNodeGroupViewModel);
		
		return oPdfViewModel;
	}

	@Override
	public String getProductBoundingBox() {
		return "";
	}

	@Override
	public MetadataViewModel getProductMetadataViewModel() {
		return new MetadataViewModel();
	}

	@Override
	public File getFileForPublishBand(String sBand, String sLayerId) {
		return null;
	}

	
	@Override
	protected Product readSnapProduct() {
		return null;
	}
}
