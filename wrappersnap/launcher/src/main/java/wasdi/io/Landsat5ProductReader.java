package wasdi.io;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.esa.snap.core.datamodel.Product;

import com.google.common.io.Files;

import wasdi.shared.utils.Utils;
import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.utils.log.WasdiLog;

public class Landsat5ProductReader extends SnapProductReader {
	
	/**
	 * @param oProductFile the envisat (zip) file to be read
	 */
	public Landsat5ProductReader(File oProductFile) {
		super(oProductFile); 
	}
	


	@Override
	public String adjustFileAfterDownload(String sDownloadedFileFullPath, String sFileNameFromProvider) {
		try {
			if(Utils.isNullOrEmpty(sDownloadedFileFullPath)) {
				WasdiLog.errorLog("Landsat5ProductReader.adjustFileAfterDownload: sDownloadedFileFullPath null or empty, aborting");
				return null;
			}
			if(Utils.isNullOrEmpty(sFileNameFromProvider)){
				WasdiLog.errorLog("Landsat5ProductReader.adjustFileAfterDownload: sFileNameFromProvider null or empty, aborting");
				return null;
			}
			if(!sFileNameFromProvider.toUpperCase().startsWith("LS05_") || !sFileNameFromProvider.toLowerCase().endsWith(".zip")) {
				WasdiLog.errorLog("Landsat5ProductReader.adjustFileAfterDownload: " + sFileNameFromProvider + " does not look like a LANDSAT-5 file name");
				return null;
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("Landsat5ProductReader.adjustFileAfterDownload: arguments checking failed due to: " + oE.getMessage() + ", aborting");
			return null;
		}

		try {
			String sDownloadPath = new File(sDownloadedFileFullPath).getParentFile().getPath();
			WasdiLog.debugLog("Landsat5ProductReader.adjustFileAfterDownload: File is a ENVISAT-5 image, start unzip");
			ZipFileUtils oZipExtractor = new ZipFileUtils();

			oZipExtractor.unzip(sDownloadPath + File.separator + sFileNameFromProvider, sDownloadPath);
			//remove .zip
			deleteDownloadedZipFile(sDownloadedFileFullPath);

			String sNewFileName = sFileNameFromProvider.substring(0, sFileNameFromProvider.toLowerCase().lastIndexOf(".zip"));
			String sFolderName = sDownloadPath + File.separator + sNewFileName;
			String sTIFFSubfolder = sFolderName + File.separator + sNewFileName + ".TIFF";
			WasdiLog.debugLog("Landsat5ProductReader.adjustFileAfterDownload: Product folder path: " + sFolderName);
			WasdiLog.debugLog("Landsat5ProductReader.adjustFileAfterDownload: TIFF subfolder path: " + sTIFFSubfolder);

			WasdiLog.debugLog("Landsat5ProductReader.adjustFileAfterDownload: starting deletion of all files in the main product folder");
			File oMainFolder = new File(sFolderName);
			// delete all the files in the main directory
			for (File oFile : oMainFolder.listFiles()) {
				if (!(oFile.isDirectory() && oFile.getName().endsWith(".TIFF"))) {
					oFile.delete();
				}
			}
			if (oMainFolder.listFiles().length == 1 && oMainFolder.listFiles()[0].isDirectory() && oMainFolder.listFiles()[0].getName().endsWith(".TIFF")) {
				WasdiLog.debugLog("Landsat5ProductReader.adjustFileAfterDownload: all files have been deleted, execpt the folder with the TIFF.");
				WasdiLog.debugLog("Landsat5ProductReader.adjustFileAfterDownload: starting moving the TIFF files to the main folder");
			
				// copy all the files in the TIFF folder in the main directory
				String sTargetDir = sFolderName;
				File oTIFFSubfolder = new File(sTIFFSubfolder);
				for (File oFile : oTIFFSubfolder.listFiles()) {
					File oTargetFile = new File(sTargetDir + File.separator + oFile.getName());
					Files.move(oFile, oTargetFile);
				}
				if (oTIFFSubfolder.listFiles().length == 0) {
					WasdiLog.debugLog("Landsat5ProductReader.adjustFileAfterDownload: all files have been moved. Subsfolder can be deleted");
					if (oTIFFSubfolder.delete()) {
						WasdiLog.debugLog("Landsat5ProductReader.adjustFileAfterDownload: .TIFF subsfolder has been deleted");
					}
				}
			}

			m_oProductFile = new File(sFolderName); // here we change the pointer to the file, from the zip file to the _MTL.txt file, which provides the information for reading the bands
			sDownloadedFileFullPath = sFolderName; // here I should put the name of the first folder

		} catch (Exception oEx) {
			WasdiLog.errorLog("Landsat5ProductReader.adjustFileAfterDownload: error ", oEx);
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
				WasdiLog.errorLog("Landsat5ProductReader.deleteZipFile: cannot delete zip file");
			} else {
				WasdiLog.debugLog("Landsat5ProductReader.deleteZipFile: file zip successfully deleted");
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("Landsat5ProductReader.deleteZipFile: exception while trying to delete zip file: " + oE ); 
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
			WasdiLog.errorLog("Landsat5ProductReader.getSnapProduct: setting paths failed due to: " + oE);
			return null;
		}
		
		if (m_oProductFile.isDirectory()) {
			File[] aoFiles = m_oProductFile.listFiles();
			for (int i = 0; i < aoFiles.length; i++) {
				File oCurrentFile = aoFiles[i];
				if (oCurrentFile.isFile() && oCurrentFile.getName().endsWith("_MTL.txt")) {
					String sFileName = oCurrentFile.getName();
					WasdiLog.errorLog("Landsat5ProductReader.getSnapProduct: found a txt file: " + sFileName);
					readProductBandFromFile(sBase, sFileName);
				}
			}
		} else {
			WasdiLog.debugLog("Landsat5ProductReader.getSnapProduct: " + sBase + "is not a folder");
			return null;
		}
		
		//reset the File pointer
		m_oProductFile = oOriginalProductFile;

		return m_oProduct;
	}

	/**
	 * @param sBase
	 * @param sFileName
	 */
	private void readProductBandFromFile(String sBase, String sFileName) {
		WasdiLog.debugLog("Landsat5ProductReader.readProductBandFromFile: base path " + sBase + ", file name: " + sFileName);
		try {
			m_oProductFile = new File(sBase + File.separator + sFileName);
			if(!m_oProductFile.exists()) {
				WasdiLog.warnLog("Landsat5ProductReader.readProductBandFromFile: file " + sBase + File.separator + sFileName + " does not exist");
				return;
			}
			if (m_bSnapReadAlreadyDone == false) {
				m_oProduct = readSnapProduct();
				WasdiLog.debugLog("Landsat5ProductReader.readProductBandFromFile. snap product has been read: " + m_oProduct.getName());
			}
			if(m_oProduct != null) {
				m_bSnapReadAlreadyDone = true;
				WasdiLog.debugLog("Landsat5ProductReader.readProductBandFromFile: snap product has been already read");
			} else {
				WasdiLog.debugLog("Landsat5ProductReader.readProductBandFromFile: snap product not yet read");
				m_bSnapReadAlreadyDone = false;
			}
		}
		catch (Exception oE) {
			WasdiLog.errorLog("Landsat5ProductReader.readProductBandFromFile: tried to read " + sFileName + " but failed: " + oE);
			m_bSnapReadAlreadyDone = false;
		}
	}
	

}
