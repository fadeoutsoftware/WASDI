package wasdi.io;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import wasdi.LauncherMain;
import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.viewmodels.products.BandViewModel;
import wasdi.shared.viewmodels.products.GeorefProductViewModel;
import wasdi.shared.viewmodels.products.MetadataViewModel;
import wasdi.shared.viewmodels.products.NodeGroupViewModel;
import wasdi.shared.viewmodels.products.ProductViewModel;

public class AdsNetcdfZipProductReader extends WasdiProductReader {

	public AdsNetcdfZipProductReader(File oProductFile) {
		super(oProductFile);
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

		String sDownloadedFileFullPath = m_oProductFile.getAbsolutePath();
		String sFileNameFromProvider = m_oProductFile.getName();
		List<String> osFileNames = unzipAndReadFiles(sDownloadedFileFullPath, sFileNameFromProvider);

		List<BandViewModel> aoBands = new ArrayList<>();

		for (String sFileName : osFileNames) {
			aoBands.add(toBand(sFileName));
		}

		oNodeGroupViewModel.setBands(aoBands);
		oRetViewModel.setBandsGroups(oNodeGroupViewModel);

		return oRetViewModel;
	}

	private static BandViewModel toBand(String sFileName) {
		// Create the single band representing the shape
		BandViewModel oBandViewModel = new BandViewModel();
		oBandViewModel.setPublished(false);
		oBandViewModel.setGeoserverBoundingBox("");
		oBandViewModel.setHeight(0);
		oBandViewModel.setWidth(0);
		oBandViewModel.setPublished(false);
		oBandViewModel.setName(sFileName);

		return oBandViewModel;
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
	public String adjustFileAfterDownload(String sDownloadedFileFullPath, String sFileNameFromProvider) {
		String sFileName = sDownloadedFileFullPath;

		return sFileName;
	}

	private List<String> unzipAndReadFiles(String sDownloadedFileFullPath, String sFileNameFromProvider) {
		List<String> osFileNames = new ArrayList<>();

		try {
			String sDownloadPath = new File(sDownloadedFileFullPath).getParentFile().getPath();
			String sTargetDirectoryPath = sDownloadPath;
//			File oSourceFile = new File(sDownloadedFileFullPath);
			File oTargetDirectory = new File(sTargetDirectoryPath + "/tmp/");

			ZipFileUtils oZipExtractor = new ZipFileUtils();
			oZipExtractor.unzip(sDownloadedFileFullPath, sTargetDirectoryPath + "/tmp/");

			if (oTargetDirectory.isDirectory()) {
				File[] aoFiles = oTargetDirectory.listFiles();

				for (File oFile : aoFiles) {
					osFileNames.add(oFile.getName());
				}
			}
		} catch (Exception oEx) {
			LauncherMain.s_oLogger.error("AdsNetcdfZipProductReader.unzipAndReadFiles: error ", oEx);
		}

		return osFileNames;
	}

	@Override
	public File getFileForPublishBand(String sBand, String sLayerId) {
		return m_oProductFile;
	}

}
