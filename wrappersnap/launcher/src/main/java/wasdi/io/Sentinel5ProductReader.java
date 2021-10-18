package wasdi.io;

import java.io.File;

import wasdi.shared.viewmodels.MetadataViewModel;
import wasdi.shared.viewmodels.ProductViewModel;

public class Sentinel5ProductReader extends WasdiProductReader {

	public Sentinel5ProductReader(File oProductFile) {
		super(oProductFile);
	}

	@Override
	public ProductViewModel getProductViewModel() {
		
		return null;
	}

	@Override
	public String getProductBoundingBox() {
		
		return null;
	}

	@Override
	public MetadataViewModel getProductMetadataViewModel() {
		
		return new MetadataViewModel("Metadata");
	}

}
