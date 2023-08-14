package wasdi.io;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.esa.snap.core.datamodel.Product;

import wasdi.shared.utils.Utils;
import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.utils.log.WasdiLog;

public class Envisat5ProductReader extends SnapProductReader {
	
	/**
	 * @param oProductFile the envisat (zip) file to be read
	 */
	public Envisat5ProductReader(File oProductFile) {
		super(oProductFile);
	}
	


	@Override
	public String adjustFileAfterDownload(String sDownloadedFileFullPath, String sFileNameFromProvider) {
		try {
			if(Utils.isNullOrEmpty(sDownloadedFileFullPath)) {
				WasdiLog.errorLog("Envisat5ProductReader.adjustFileAfterDownload: sDownloadedFileFullPath null or empty, aborting");
				return null;
			}
			if(Utils.isNullOrEmpty(sFileNameFromProvider)){
				WasdiLog.errorLog("Envisat5ProductReader.adjustFileAfterDownload: sFileNameFromProvider null or empty, aborting");
				return null;
			}
			if(!sFileNameFromProvider.toUpperCase().startsWith("LS05_") || !sFileNameFromProvider.toLowerCase().endsWith(".zip")) {
				WasdiLog.errorLog("Envisat5ProductReader.adjustFileAfterDownload: " + sFileNameFromProvider + " does not look like a LANDSAT-5 file name");
				return null;
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("Envisat5ProductReader.adjustFileAfterDownload: arguments checking failed due to: " + oE.getMessage() + ", aborting");
			return null;
		}

		try {
			String sDownloadPath = new File(sDownloadedFileFullPath).getParentFile().getPath();
			WasdiLog.debugLog("Envisat5ProductReader.adjustFileAfterDownload: File is a ENVISAT-5 image, start unzip");
			ZipFileUtils oZipExtractor = new ZipFileUtils();

			oZipExtractor.unzip(sDownloadPath + File.separator + sFileNameFromProvider, sDownloadPath);
			deleteDownloadedZipFile(sDownloadedFileFullPath);

			//remove .zip
			String sNewFileName = sFileNameFromProvider.substring(0, sFileNameFromProvider.toLowerCase().lastIndexOf(".zip"));
			String sFolderName = sDownloadPath + File.separator + sNewFileName;
			String sTIFFSubfolder = sFolderName + File.separator + sNewFileName + ".TIFF";
			
			
			WasdiLog.debugLog("Envisat5ProductReader.adjustFileAfterDownload: Unzip done, folder name: " + sFolderName);
			WasdiLog.debugLog("Envisat5ProductReader.adjustFileAfterDownload: TIFF subfolder name: " + sTIFFSubfolder);

			m_oProductFile = new File(sTIFFSubfolder);
			return sTIFFSubfolder;
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
				WasdiLog.errorLog("Envisat5ProductReader.deleteZipFile: cannot delete zip file");
			} else {
				WasdiLog.debugLog("Envisat5ProductReader.deleteZipFile: file zip successfully deleted");
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("Envisat5ProductReader.deleteZipFile: exception while trying to delete zip file: " + oE ); 
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
		try {
			sBase = m_oProductFile.getAbsolutePath();
			
		} catch (Exception oE) {
			WasdiLog.errorLog("Envisat5ProductReader.getSnapProduct: setting paths failed due to: " + oE);
			return null;
		}
		
		if (m_oProductFile.isDirectory()) {
			File[] aoFiles = m_oProductFile.listFiles();
			for (int i = 0; i < aoFiles.length; i++) {
				File oCurrentFile = aoFiles[i];
				if (oCurrentFile.isFile() && oCurrentFile.getName().endsWith(".txt")) {
					String sFileName = oCurrentFile.getName();
					WasdiLog.errorLog("Envisat5ProductReader.getSnapProduct: found a txt file: " + sFileName);
					readProductBandFromFile(sBase, sFileName);
				}
			}
		} else {
			WasdiLog.debugLog("Envisat5ProductReader.getSnapProduct: " + sBase + "is not a folder");
			return null;
		}
		
		//reset the File pointer
		m_oProductFile = m_oProductFile.getParentFile();

		return m_oProduct;
	}

	/**
	 * @param sBase
	 * @param sFileName
	 */
	private void readProductBandFromFile(String sBase, String sFileName) {
		WasdiLog.debugLog("Envisat5ProductReader.readProductBandFromFile: base path " + sBase + ", file name: " + sFileName);
		try {
			m_oProductFile = new File(sBase + File.separator + sFileName);
			if(!m_oProductFile.exists()) {
				WasdiLog.warnLog("Envisat5ProductReader.readProductBandFromFile: file " + sBase + File.separator + sFileName + " does not exist");
				return;
			}
			if (m_bSnapReadAlreadyDone == false) {
				m_oProduct = readSnapProduct();
				WasdiLog.debugLog("Envisat5ProductReader.readProductBandFromFile. snap product has been read: " + m_oProduct.getName());
			}
			if(m_oProduct != null) {
				m_bSnapReadAlreadyDone = true;
				WasdiLog.debugLog("Envisat5ProductReader.readProductBandFromFile: snap product has been already read");
			} else {
				WasdiLog.debugLog("Envisat5ProductReader.readProductBandFromFile: snap product not yet read");
				m_bSnapReadAlreadyDone = false;
			}
		}
		catch (Exception oE) {
			WasdiLog.errorLog("Envisat5ProductReader.readProductBandFromFile: tried to read " + sFileName + " but failed: " + oE);
		}
	}
	

}
