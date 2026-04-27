/**
 * 
 */
package wasdi.io;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.products.BandViewModel;
import wasdi.shared.viewmodels.products.GeorefProductViewModel;
import wasdi.shared.viewmodels.products.MetadataViewModel;
import wasdi.shared.viewmodels.products.NodeGroupViewModel;
import wasdi.shared.viewmodels.products.ProductViewModel;


/**
 * @author c.nattero
 *
 */
public class Sentinel3ProductReader extends WasdiProductReader {
	
	private static final String s_sSen3Extention = ".SEN3";

	/**
	 * @param oProductFile the Sentinel-3 (zip) file to be read
	 */
	public Sentinel3ProductReader(File oProductFile) {
		super(oProductFile);
	}

	@Override
	public ProductViewModel getProductViewModel() {
		GeorefProductViewModel oProductVM = new GeorefProductViewModel();

		String sFileName = (m_oProductFile != null) ? m_oProductFile.getName() : "s3-product";
		oProductVM.setFileName(sFileName);
		oProductVM.setName(WasdiFileUtils.getFileNameWithoutLastExtension(sFileName));
		oProductVM.setProductFriendlyName(WasdiFileUtils.getFileNameWithoutLastExtension(sFileName));

		NodeGroupViewModel oBandsGroup = new NodeGroupViewModel("Bands");
		oBandsGroup.setBands(new ArrayList<BandViewModel>());
		oProductVM.setBandsGroups(oBandsGroup);

		try {
			File oNetCdf = getRelevantNetCdfFile();
			if (oNetCdf == null) {
				WasdiLog.warnLog("Sentinel3ProductReader.getProductViewModel: no relevant netcdf file found, returning empty VM");
				return oProductVM;
			}

			NetcdfFile oNetcdfFile = NetcdfFiles.open(oNetCdf.getAbsolutePath());
			LinkedHashSet<String> aoBandNames = new LinkedHashSet<String>();
			collectBandNames(oNetcdfFile.getRootGroup(), aoBandNames);

			for (String sBandName : aoBandNames) {
				BandViewModel oBand = new BandViewModel();
				oBand.setName(sBandName);
				oBandsGroup.getBands().add(oBand);
			}

			oNetcdfFile.close();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("Sentinel3ProductReader.getProductViewModel: error extracting bands, returning empty VM", oEx);
		}

		return oProductVM;
	}

	@Override
	public String getProductBoundingBox() {
		String sResBBox = "";
		File oManifestFile = findManifestFile();

		if (oManifestFile == null) {
			WasdiLog.warnLog("Sentinel3ProductReader.getProductBoundingBox: manifest file not found");
			return sResBBox;
		}

		double dMinY = Double.MAX_VALUE;
		double dMaxY = Double.MIN_VALUE;
		double dMinX = Double.MAX_VALUE;
		double dMaxX = Double.MIN_VALUE;

		try {
			DocumentBuilderFactory oFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder oBuilder = oFactory.newDocumentBuilder();
			Document oDocument = oBuilder.parse(oManifestFile);
			Element oRoot = oDocument.getDocumentElement();

			NodeList oNodeList = oRoot.getElementsByTagName("gml:posList");
			if (oNodeList.getLength() == 0) {
				WasdiLog.warnLog("Sentinel3ProductReader.getProductBoundingBox: no gml:posList found");
				return sResBBox;
			}

			int iItem = 0;
			while (iItem < oNodeList.getLength()) {
				String sCoordinates = oNodeList.item(iItem).getTextContent();
				String[] asCoordinates = sCoordinates.split(" ");
				List<Double> adLatitudes = new ArrayList<Double>();
				List<Double> adLongitudes = new ArrayList<Double>();

				int i = 0;
				while (i < asCoordinates.length) {
					if (!Utils.isNullOrEmpty(asCoordinates[i])) {
						if (i % 2 == 0) adLatitudes.add(Double.parseDouble(asCoordinates[i]));
						else adLongitudes.add(Double.parseDouble(asCoordinates[i]));
					}
					i++;
				}

				if (!adLatitudes.isEmpty() && !adLongitudes.isEmpty()) {
					double dMaxLat = Collections.max(adLatitudes);
					double dMinLat = Collections.min(adLatitudes);
					double dMaxLon = Collections.max(adLongitudes);
					double dMinLon = Collections.min(adLongitudes);

					if (dMaxLat > dMaxY) dMaxY = dMaxLat;
					if (dMinLat < dMinY) dMinY = dMinLat;
					if (dMaxLon > dMaxX) dMaxX = dMaxLon;
					if (dMinLon < dMinX) dMinX = dMinLon;
				}

				iItem++;
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("Sentinel3ProductReader.getProductBoundingBox: error reading manifest", oEx);
			return sResBBox;
		}

		if (dMinY >= -90 && dMinY <= 90
				&& dMaxY >= -90 && dMaxY <= 90
				&& dMinX >= -180 && dMinX <= 180
				&& dMaxX >= -180 && dMaxX <= 180) {
			sResBBox = String.format("%f,%f,%f,%f,%f,%f,%f,%f,%f,%f",
					(float) dMinY, (float) dMinX, (float) dMinY, (float) dMaxX,
					(float) dMaxY, (float) dMaxX, (float) dMaxY, (float) dMinX,
					(float) dMinY, (float) dMinX);
		}

		return sResBBox;
	}

	@Override
	public MetadataViewModel getProductMetadataViewModel() {
		return new MetadataViewModel("Metadata");
	}

	@Override
	public File getFileForPublishBand(String sBand, String sLayerId, String sPlatform) {
		WasdiLog.debugLog("Sentinel3ProductReader.getFileForPublishBand: currently not supported for Sentinel-3");
		return null;
	}

	@Override
	public String adjustFileAfterDownload(String sDownloadedFileFullPath, String sFileNameFromProvider) {
		try {
			if(Utils.isNullOrEmpty(sDownloadedFileFullPath)) {
				WasdiLog.errorLog("Sentinel3ProductReader.adjustFileAfterDownload: sDownloadedFileFullPath null or empty, aborting");
				return null;
			}
			if(Utils.isNullOrEmpty(sFileNameFromProvider)){
				WasdiLog.errorLog("Sentinel3ProductReader.adjustFileAfterDownload: sFileNameFromProvider null or empty, aborting");
				return null;
			}
			if(!sFileNameFromProvider.toLowerCase().startsWith("s3") || !sFileNameFromProvider.toLowerCase().endsWith(".zip")) {
				WasdiLog.errorLog("Sentinel3ProductReader.adjustFileAfterDownload: " + sFileNameFromProvider + " does not look like a Sentinel-3 file name");
				return null;
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("Sentinel3ProductReader.adjustFileAfterDownload: arguments checking failed due to: " + oE + ", aborting");
			return null;
		}

		try {
			String sDownloadPath = new File(sDownloadedFileFullPath).getParentFile().getPath();
			WasdiLog.debugLog("Sentinel3ProductReader.adjustFileAfterDownload: File is a Sentinel 3 image, start unzip");
			ZipFileUtils oZipExtractor = new ZipFileUtils();

			//remove .SEN3 from the file name -> required for CREODIAS
			String sNewFileName = sFileNameFromProvider.replaceAll(s_sSen3Extention, "");

			oZipExtractor.unzip(sDownloadPath + File.separator + sNewFileName, sDownloadPath);
			deleteDownloadedZipFile(sDownloadedFileFullPath);

			//remove .zip and add .SEN3 if required
			sNewFileName = sFileNameFromProvider.substring(0, sFileNameFromProvider.toLowerCase().lastIndexOf(".zip"));
			if(!sNewFileName.endsWith(s_sSen3Extention)) {
				sNewFileName = sNewFileName + s_sSen3Extention;
			}

			String sFolderName = sDownloadPath + File.separator + sNewFileName;
			WasdiLog.debugLog("Sentinel3ProductReader.adjustFileAfterDownload: Unzip done, folder name: " + sFolderName);
			WasdiLog.debugLog("Sentinel3ProductReader.adjustFileAfterDownload: File Name changed in: " + sFolderName);

			m_oProductFile = new File(sFolderName);
			return sFolderName;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("Sentinel3ProductReader.adjustFileAfterDownload: error ", oEx);
		}

		return sDownloadedFileFullPath;
	}

	/**
	 * @param sFileNameFromProvider
	 * @param sDownloadPath
	 */
	private void deleteDownloadedZipFile(String sDownloadedFileFullPath) {
		try {
			File oZipFile = new File(sDownloadedFileFullPath);
			if(!oZipFile.delete()) {
				WasdiLog.errorLog("Sentinel3ProductReader.deleteZipFile: cannot delete zip file");
			} else {
				WasdiLog.debugLog("Sentinel3ProductReader.deleteZipFile: file zip successfully deleted");
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("Sentinel3ProductReader.deleteZipFile: exception while trying to delete zip file: " + oE ); 
		}
	}

	private File findManifestFile() {
		if (m_oProductFile == null) return null;

		File oFolder = m_oProductFile;
		if (oFolder.isFile()) {
			oFolder = oFolder.getParentFile();
		}
		if (oFolder == null || !oFolder.isDirectory()) return null;

		Pattern oManifestNamePattern = Pattern.compile("^xfd.*\\.xml$", Pattern.CASE_INSENSITIVE);
		File[] aoFiles = oFolder.listFiles();
		if (aoFiles == null) return null;

		for (File oFile : aoFiles) {
			if (oFile.isFile() && oManifestNamePattern.matcher(oFile.getName()).matches()) {
				return oFile;
			}
		}

		return null;
	}

	private File getRelevantNetCdfFile() {
		if (m_oProductFile == null) return null;

		File oFolder = m_oProductFile;
		if (oFolder.isFile()) oFolder = oFolder.getParentFile();
		if (oFolder == null || !oFolder.isDirectory()) return null;

		String[] asCandidates = new String[] {
			"measurement.nc",
			"standard_measurement.nc",
			"enhanced_measurement.nc",
			"reduced_measurement.nc",
			"NRT_AOD.nc",
			"measurement_l1bs.nc"
		};

		for (String sCandidate : asCandidates) {
			File oCandidate = new File(oFolder, sCandidate);
			if (oCandidate.exists() && oCandidate.isFile()) {
				return oCandidate;
			}
		}

		File[] aoFiles = oFolder.listFiles();
		if (aoFiles != null) {
			for (File oFile : aoFiles) {
				if (oFile.isFile() && oFile.getName().toLowerCase().endsWith(".nc")) {
					return oFile;
				}
			}
		}

		return null;
	}

	private void collectBandNames(Group oGroup, LinkedHashSet<String> aoBandNames) {
		if (oGroup == null) return;

		List<Variable> aoVariables = oGroup.getVariables();
		if (aoVariables != null) {
			for (Variable oVariable : aoVariables) {
				String sName = oVariable.getShortName();
				if (isDisplayBandName(sName)) {
					aoBandNames.add(sName);
				}
			}
		}

		List<Group> aoGroups = oGroup.getGroups();
		if (aoGroups != null) {
			for (Group oChildGroup : aoGroups) {
				collectBandNames(oChildGroup, aoBandNames);
			}
		}
	}

	private boolean isDisplayBandName(String sName) {
		if (Utils.isNullOrEmpty(sName)) return false;
		String s = sName.toLowerCase();
		if (s.equals("latitude") || s.equals("lat") || s.equals("longitude") || s.equals("lon")) return false;
		if (s.equals("time") || s.equals("rows") || s.equals("columns") || s.equals("row") || s.equals("column")) return false;
		if (s.startsWith("quality") || s.endsWith("_flags") || s.endsWith("_flag")) return false;
		return true;
	}
}
