package wasdi.io;

import java.io.File;

import org.esa.snap.core.datamodel.Product;

import wasdi.shared.utils.Utils;
import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.utils.log.WasdiLog;

public class Sentinel6ProductReader extends SnapProductReader {
	
	/**
	 * @param oProductFile the envisat (zip) file to be read
	 */
	public Sentinel6ProductReader(File oProductFile) {
		super(oProductFile); 
	}
	


	@Override
	public String adjustFileAfterDownload(String sDownloadedFileFullPath, String sFileNameFromProvider) {
		try {
			if(Utils.isNullOrEmpty(sDownloadedFileFullPath)) {
				WasdiLog.errorLog("Sentinel6ProductReader.adjustFileAfterDownload. sDownloadedFileFullPath null or empty, aborting");
				return null;
			}
			if(Utils.isNullOrEmpty(sFileNameFromProvider)){
				WasdiLog.errorLog("Sentinel6ProductReader.adjustFileAfterDownload. sFileNameFromProvider null or empty, aborting");
				return null;
			}
			if(!sFileNameFromProvider.toUpperCase().startsWith("S6") || !sFileNameFromProvider.toLowerCase().endsWith(".zip")) {
				WasdiLog.errorLog("Sentinel6ProductReader.adjustFileAfterDownload: " + sFileNameFromProvider + " does not look like a Sentinel-6 file name");
				return null;
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("Sentinel6ProductReader.adjustFileAfterDownload. arguments checking failed due to: " + oE.getMessage() + ", aborting");
			return null;
		}

		try {
			String sDownloadPath = new File(sDownloadedFileFullPath).getParentFile().getPath();
			WasdiLog.debugLog("Sentinel6ProductReader.adjustFileAfterDownload. File is a Sentinel-6 image, start unzip of file: " + sDownloadedFileFullPath);
			ZipFileUtils oZipExtractor = new ZipFileUtils();

			oZipExtractor.unzip(sDownloadPath + File.separator + sFileNameFromProvider, sDownloadPath);
			//remove .zip
			deleteDownloadedZipFile(sDownloadedFileFullPath);

			String sNewFolderName = sFileNameFromProvider.substring(0, sFileNameFromProvider.toLowerCase().lastIndexOf(".zip"));
			String sNewFolderPath = sDownloadPath + File.separator + sNewFolderName;
			File oNewFolder = new File(sNewFolderPath);
			
			WasdiLog.debugLog("Sentinel6ProductReader.adjustFileAfterDownload. product folder : " + sDownloadedFileFullPath);

			m_oProductFile = oNewFolder; // here we change the pointer to the file, from the zip file to the _MTL.txt file, which provides the information for reading the bands
			sDownloadedFileFullPath = sNewFolderPath; // here I should put the name of the first folder

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
			if(!oZipFile.delete()) {
				WasdiLog.errorLog("Sentinel6ProductReader.deleteZipFile. cannot delete zip file");
			} else {
				WasdiLog.debugLog("Sentinel6ProductReader.deleteZipFile. file zip successfully deleted");
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("Sentinel6ProductReader.deleteZipFile. exception while trying to delete zip file: " + oE ); 
		}
	}
	

	/**
	 * Get the SNAP product or null if this is not a product readable by Snap
	 * 
	 * @return
	 */
	@Override
	public Product getSnapProduct() {
		if(m_bSnapReadAlreadyDone) {
			return m_oProduct;
		}
		
		//prepare path
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
					String sFileName = oCurrentFile.getName();
					WasdiLog.debugLog("Sentinel6ProductReader.getSnapProduct. found a nc file: " + sFileName);
					readProductBandFromFile(sBase, sFileName);
				}
			}
		} else {
			WasdiLog.debugLog("Sentinel6ProductReader.getSnapProduct. " + sBase + "is not a folder");
			return null;
		}
		
		//reset the File pointer
		m_oProductFile = oOriginalProductFile;

		return m_oProduct;
	}

	/**
	 * @param sBase base folder
	 * @param sFileName name of the file in the base folder from which the band should be read
	 */
	private void readProductBandFromFile(String sBase, String sFileName) {
		WasdiLog.debugLog("Sentinel6ProductReader.readProductBandFromFile. base path " + sBase + ", file name: " + sFileName);
		try {
			m_oProductFile = new File(sBase + File.separator + sFileName);
			if(!m_oProductFile.exists()) {
				WasdiLog.warnLog("Sentinel6ProductReader.readProductBandFromFile. file " + sBase + File.separator + sFileName + " does not exist");
				return;
			}
			if (m_bSnapReadAlreadyDone == false) {
				m_oProduct = readSnapProduct();
				WasdiLog.debugLog("Sentinel6ProductReader.readProductBandFromFile. snap product has been read: " + m_oProduct.getName());
			}
			if(m_oProduct != null) {
				m_bSnapReadAlreadyDone = true;
				WasdiLog.debugLog("Sentinel6ProductReader.readProductBandFromFile. snap product has been already read");
			} else {
				WasdiLog.debugLog("Sentinel6ProductReader.readProductBandFromFile. snap product not yet read");
				m_bSnapReadAlreadyDone = false;
			}
		}
		catch (Exception oE) {
			WasdiLog.errorLog("Sentinel6ProductReader.readProductBandFromFile. tried to read " + sFileName + " but failed: " + oE);
			m_bSnapReadAlreadyDone = false;
		}
	}
	

}
