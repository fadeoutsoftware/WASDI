package wasdi.io;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.utils.log.WasdiLog;
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
		List<String> asDates = extractDates(sDownloadedFileFullPath, sFileNameFromProvider);

		List<BandViewModel> aoBands = new ArrayList<>();

		for (String sDate : asDates) {
			aoBands.add(toBand(sDate));
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

	private List<String> extractDates(String sDownloadedFileFullPath, String sFileNameFromProvider) {
		List<String> asDates = new ArrayList<>();

		try {
			List<String> asFileNames = ZipFileUtils.peepZipArchiveContent(sDownloadedFileFullPath);

			for (String sFileName : asFileNames) {
				asDates.add(extractDateFromFileName(sFileName));
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("AdsNetcdfZipProductReader.readZipArchiveFileNames: error ", oEx);
		}

		Collections.sort(asDates);

		return asDates;
	}

	private static String extractDateFromFileName(String sFileName) {
		return sFileName.replace("date_", "").replace(".nc", "");
	}


}
