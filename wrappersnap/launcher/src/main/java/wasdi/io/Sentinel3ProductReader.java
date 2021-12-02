/**
 * 
 */
package wasdi.io;

import java.io.File;

import wasdi.shared.viewmodels.products.MetadataViewModel;
import wasdi.shared.viewmodels.products.ProductViewModel;

/**
 * @author c.nattero
 *
 */
public class Sentinel3ProductReader extends WasdiProductReader {

	/**
	 * @param oProductFile the Sentinel-3 (zip) file to be read
	 */
	public Sentinel3ProductReader(File oProductFile) {
		super(oProductFile);
	}

	@Override
	public ProductViewModel getProductViewModel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getProductBoundingBox() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MetadataViewModel getProductMetadataViewModel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String adjustFileAfterDownload(String sDownloadedFileFullPath, String sFileNameFromProvider) {
		// TODO Auto-generated method stub
		return null;
	}

}
