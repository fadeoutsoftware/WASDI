package wasdi.io;

import java.io.File;
import java.util.ArrayList;

import wasdi.shared.viewmodels.products.BandViewModel;
import wasdi.shared.viewmodels.products.MetadataViewModel;
import wasdi.shared.viewmodels.products.NodeGroupViewModel;
import wasdi.shared.viewmodels.products.ProductViewModel;

/**
 * Last-resort reader for unrecognized files.
 * It provides a minimal VM so files can still be imported and handled by apps.
 */
public class UnknownProductReader extends WasdiProductReader {

	public UnknownProductReader(File oProductFile) {
		super(oProductFile);
	}

	@Override
	public ProductViewModel getProductViewModel() {
		ProductViewModel oViewModel = new ProductViewModel();

		String sName = (m_oProductFile != null) ? m_oProductFile.getName() : "unknown-product";
		oViewModel.setFileName(sName);
		oViewModel.setName(sName);
		oViewModel.setProductFriendlyName(sName);

		NodeGroupViewModel oBandsGroup = new NodeGroupViewModel("Bands");
		oBandsGroup.setBands(new ArrayList<BandViewModel>());
		oViewModel.setBandsGroups(oBandsGroup);

		return oViewModel;
	}

	@Override
	public String getProductBoundingBox() {
		return "";
	}

	@Override
	public MetadataViewModel getProductMetadataViewModel() {
		return new MetadataViewModel("Metadata");
	}

	@Override
	public File getFileForPublishBand(String sBand, String sLayerId, String sPlatform) {
		return null;
	}
}
