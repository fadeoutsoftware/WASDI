package wasdi.io;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.products.BandViewModel;
import wasdi.shared.viewmodels.products.GeorefProductViewModel;
import wasdi.shared.viewmodels.products.MetadataViewModel;
import wasdi.shared.viewmodels.products.NodeGroupViewModel;
import wasdi.shared.viewmodels.products.ProductViewModel;

public class GpmZipProductReader extends WasdiProductReader {

	public GpmZipProductReader(File oProductFile) {
		super(oProductFile);
	}

	@Override
	public String adjustFileAfterDownload(String sDownloadedFileFullPath, String sFileNameFromProvider) {
		String sFileName = sDownloadedFileFullPath;

		try {
			if (sFileNameFromProvider.toUpperCase().startsWith("3B-") || sFileNameFromProvider.toUpperCase().contains("IMERG")) {
				WasdiLog.debugLog("GpmZipProductReader.adjustFileAfterDownload: File is a zip file, start unzip");
				String sDownloadPath = new File(sDownloadedFileFullPath).getParentFile().getPath();

				String sTargetDirectoryPath = sDownloadPath;

				File oSourceFile = new File(sDownloadedFileFullPath);
				File oTargetDirectory = new File(sTargetDirectoryPath);
				ZipFileUtils.extractInnerZipFileAndCleanZipFile(oSourceFile, oTargetDirectory);

				String sFolderName = sDownloadPath + File.separator + sFileNameFromProvider.replace(".zip", "");
				WasdiLog.debugLog("Sentinel5ProductReader.adjustFileAfterDownload: Unzip done, folder name: " + sFolderName);

				sFileName = sFolderName + ".tif";
				WasdiLog.debugLog("GpmZipProductReader.adjustFileAfterDownload: File Name changed in: " + sFileName);

				m_oProductFile = new File(sFileName);
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("GpmZipProductReader.adjustFileAfterDownload: error ", oEx);
		}

		return sFileName;
	}

	@Override
	public ProductViewModel getProductViewModel() {
		if (m_oProductFile == null) return null;

		// Create the return value
		GeorefProductViewModel oRetViewModel = null;
		
		// Create the Product View Model
		oRetViewModel = new GeorefProductViewModel();

		// Set name values
		oRetViewModel.setFileName(m_oProductFile.getName());

		oRetViewModel.setName(m_oProductFile.getName());
		oRetViewModel.setProductFriendlyName(oRetViewModel.getName());

		NodeGroupViewModel oNodeGroupViewModel = new NodeGroupViewModel();
		oNodeGroupViewModel.setNodeName("Bands");

		List<BandViewModel> aoBands = new ArrayList<>();
		
    	// Create the single band representing the shape
    	BandViewModel oBandViewModel = new BandViewModel();
    	oBandViewModel.setPublished(false);
    	oBandViewModel.setGeoserverBoundingBox("");
    	oBandViewModel.setHeight(0);
    	oBandViewModel.setWidth(0);
    	oBandViewModel.setPublished(false);
    	oBandViewModel.setName(WasdiFileUtils.getFileNameWithoutLastExtension(oRetViewModel.getName()));
		
		aoBands.add(oBandViewModel);

		oNodeGroupViewModel.setBands(aoBands);
		oRetViewModel.setBandsGroups(oNodeGroupViewModel);
		
		return oRetViewModel;
	}

	@Override
	public String getProductBoundingBox() {
		float fLatN = 90f;
		float fLonW = -180f;
		float fLatS = -90f;
		float fLonE = 180f;

		String sBBox = fLatN + "," + fLonW + "," + fLatS + "," + fLonE;

		return sBBox;
	}

	@Override
	public MetadataViewModel getProductMetadataViewModel() {
		return new MetadataViewModel("Metadata");
	}

	@Override
	public File getFileForPublishBand(String sBand, String sLayerId, String sPlatform) {
		return m_oProductFile;
	}

}
