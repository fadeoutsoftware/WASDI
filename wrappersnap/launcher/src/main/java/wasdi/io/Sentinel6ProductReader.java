package wasdi.io;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.esa.snap.core.datamodel.Product;

import wasdi.shared.utils.Utils;
import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.utils.log.WasdiLog;

public class Sentinel6ProductReader extends CmNcProductReader {

	/**
	 * @param oProductFile the envisat (zip) file to be read
	 */
	public Sentinel6ProductReader(File oProductFile) {
		super(oProductFile);
		if (oProductFile.getName().endsWith(".zip")) {
			String sProductFilePath = adjustFileAfterDownload(oProductFile.getAbsolutePath(), oProductFile.getName());
			m_oProductFile = new File(sProductFilePath); 
		}
	}

	@Override
	public String adjustFileAfterDownload(String sDownloadedFileFullPath, String sFileNameFromProvider) {
		try {
			if (Utils.isNullOrEmpty(sDownloadedFileFullPath)) {
				WasdiLog.errorLog(
						"Sentinel6ProductReader.adjustFileAfterDownload. sDownloadedFileFullPath null or empty, aborting");
				return null;
			}
			if (Utils.isNullOrEmpty(sFileNameFromProvider)) {
				WasdiLog.errorLog(
						"Sentinel6ProductReader.adjustFileAfterDownload. sFileNameFromProvider null or empty, aborting");
				return null;
			}
			if (!sFileNameFromProvider.toUpperCase().startsWith("S6")
					|| !sFileNameFromProvider.toLowerCase().endsWith(".zip")) {
				WasdiLog.errorLog("Sentinel6ProductReader.adjustFileAfterDownload: " + sFileNameFromProvider
						+ " does not look like a Sentinel-6 file name");
				return null;
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("Sentinel6ProductReader.adjustFileAfterDownload. arguments checking failed due to: "
					+ oE.getMessage() + ", aborting");
			return null;
		}
		

		try {
			String sDownloadPath = new File(sDownloadedFileFullPath).getParentFile().getPath();
			
			String sNewFolderName = sFileNameFromProvider.substring(0,
					sFileNameFromProvider.toLowerCase().lastIndexOf(".zip"));
			
			String sNewFolderPath = sDownloadPath + File.separator + sNewFolderName;
			
			List<File> asFilesInDownloadFolder = Arrays.asList(new File(sDownloadPath).listFiles());
			if (asFilesInDownloadFolder.stream().anyMatch(oFile -> oFile.isDirectory() && oFile.getName().equals(sNewFolderName))) {
				WasdiLog.debugLog("Sentinel6ProductReader.adjustFileAfterDownload. File already unzipped in: " + sNewFolderPath);
				return m_oProductFile.getAbsolutePath();
				
			} else {
				WasdiLog.debugLog(
						"Sentinel6ProductReader.adjustFileAfterDownload. File is a Sentinel-6 image, start unzip of file: "
								+ sDownloadedFileFullPath);
				ZipFileUtils oZipExtractor = new ZipFileUtils();

				oZipExtractor.unzip(sDownloadPath + File.separator + sFileNameFromProvider, sDownloadPath);
				// remove .zip
				deleteDownloadedZipFile(sDownloadedFileFullPath);
				
				File oNewFolder = new File(sNewFolderPath);
				for (File oFile : oNewFolder.listFiles()) {
					if (oFile.isFile() && oFile.getName().endsWith(".nc")) {
						return oFile.getAbsolutePath();
					}
				}		
			}

		} catch (Exception oEx) {
			WasdiLog.errorLog("Sentinel6ProductReader.adjustFileAfterDownload. error ", oEx);
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
			if (!oZipFile.delete()) {
				WasdiLog.errorLog("Sentinel6ProductReader.deleteZipFile. cannot delete zip file");
			} else {
				WasdiLog.debugLog("Sentinel6ProductReader.deleteZipFile. file zip successfully deleted");
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("Sentinel6ProductReader.deleteZipFile. exception while trying to delete zip file: " + oE);
		}
	}

	/**
	 * Get the SNAP product or null if this is not a product readable by Snap
	 * 
	 * @return
	 */
	@Override
	public Product getSnapProduct() {
		
		
		if (m_bSnapReadAlreadyDone) {
			return m_oProduct;
		}

		// prepare path
		String sBase = null;
		File oOriginalProductFile = m_oProductFile;
		try {
			sBase = m_oProductFile.getAbsolutePath();

		} catch (Exception oE) {
			WasdiLog.errorLog("Sentinel6ProductReader.getSnapProduct. setting paths failed due to: " + oE);
			return null;
		}

		if (m_oProductFile.isDirectory()) {
			File[] aoFiles = m_oProductFile.listFiles();
			for (int i = 0; i < aoFiles.length; i++) {
				File oCurrentFile = aoFiles[i];
				if (oCurrentFile.isFile() && oCurrentFile.getName().endsWith(".nc")) {
					m_oProductFile = oCurrentFile;
					super.getSnapProduct();
					break;
				}
			}
		} else {
			WasdiLog.debugLog("Sentinel6ProductReader.getSnapProduct. " + sBase + "is not a folder");
			return null;
		}

		// reset the File pointer
		m_oProductFile = oOriginalProductFile;

		return m_oProduct;
	}

	/**
	 * @param sBase     base folder
	 * @param sFileName name of the file in the base folder from which the band
	 *                  should be read
	 */
	private void readProductBandFromFile(String sBase, String sFileName) {
		WasdiLog.debugLog(
				"Sentinel6ProductReader.readProductBandFromFile. base path " + sBase + ", file name: " + sFileName);
		try {
			m_oProductFile = new File(sBase + File.separator + sFileName);
			if (!m_oProductFile.exists()) {
				WasdiLog.warnLog("Sentinel6ProductReader.readProductBandFromFile. file " + sBase + File.separator
						+ sFileName + " does not exist");
				return;
			}
			if (m_bSnapReadAlreadyDone == false) {
				m_oProduct = readSnapProduct();
				WasdiLog.debugLog("Sentinel6ProductReader.readProductBandFromFile. snap product has been read: "
						+ m_oProduct.getName());
			}
			if (m_oProduct != null) {
				m_bSnapReadAlreadyDone = true;
				WasdiLog.debugLog("Sentinel6ProductReader.readProductBandFromFile. snap product has been already read");
			} else {
				WasdiLog.debugLog("Sentinel6ProductReader.readProductBandFromFile. snap product not yet read");
				m_bSnapReadAlreadyDone = false;
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("Sentinel6ProductReader.readProductBandFromFile. tried to read " + sFileName
					+ " but failed: " + oE);
			m_bSnapReadAlreadyDone = false;
		}
	}

	public static void main(String[] args) throws Exception {
		String sFilePath = "C:/Users/valentina.leone/.wasdi/S6A_P4_1B_LR______20211025T003221_20211025T012834_20211025T090135_3373_035_109_054_EUM__OPE_ST_F03.SEN6.zip";
		Sentinel6ProductReader pr = new Sentinel6ProductReader(new File(sFilePath));
		String res = pr.adjustFileAfterDownload(sFilePath,
				"S6A_P4_1B_LR______20211025T003221_20211025T012834_20211025T090135_3373_035_109_054_EUM__OPE_ST_F03.SEN6.zip");
		System.out.println(res);

	}

}
