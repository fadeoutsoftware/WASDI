package wasdi.io;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.util.ZipUtils;

import com.google.common.io.Files;

import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
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
	public File getFileForPublishBand(String sBand, String sLayerId) {
		// not yet supported
		return null;
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
			String sDownloadPath = new File(sDownloadedFileFullPath).getParentFile().getAbsolutePath(); // this should be the path of the workspace
			WasdiLog.debugLog("Landsat5ProductReader.adjustFileAfterDownload: File is a ENVISAT-5 product, start unzip");

			String sFileName = Utils.getFileNameWithoutLastExtension(sFileNameFromProvider); // file name without extension
			File oZipFile = new File(sDownloadedFileFullPath); 	// zip file downloaded from the data provider
			
			File oUnzippedFolder = new File(sDownloadPath + File.separator + sFileName);	// folder where the file shoulf be unzipped
			ZipFileUtils.cleanUnzipFile(oZipFile, oUnzippedFolder);

			WasdiLog.debugLog("Landsat5ProductReader.adjustFileAfterDownload: starting deletion of all files in the main product folder");
			
			// delete all the files in the main directory
			for (File oFile : oUnzippedFolder.listFiles()) {
				if (!(oFile.isDirectory() && oFile.getName().endsWith(".TIFF"))) {
					oFile.delete();
				}
			}
			
			// we make sure that the only file left after deletion is a the ".TIFF" folder
			if (oUnzippedFolder.listFiles().length == 1 
					&& oUnzippedFolder.listFiles()[0].isDirectory() 
					&& oUnzippedFolder.listFiles()[0].getName().endsWith(".TIFF")) {
				WasdiLog.debugLog("Landsat5ProductReader.adjustFileAfterDownload: starting moving the TIFF files to the main folder");
			
				// copy all the files in the TIFF folder in the main directory
				File oTIFFSubfolder = new File(oUnzippedFolder.getAbsolutePath() + "/.TIFF");
				for (File oFile : oTIFFSubfolder.listFiles()) {
					File oTargetFile = new File(oUnzippedFolder.getAbsolutePath() + File.separator + oFile.getName());
					Files.move(oFile, oTargetFile);
				}
				
				if (oTIFFSubfolder.listFiles().length == 0) {
					WasdiLog.debugLog("Landsat5ProductReader.adjustFileAfterDownload: all files have been moved. Subsfolder can be deleted");
					if (oTIFFSubfolder.delete()) {
						WasdiLog.debugLog("Landsat5ProductReader.adjustFileAfterDownload: .TIFF subsfolder has been deleted");
					}
				} else {
					WasdiLog.debugLog("Landsat5ProductReader.adjustFileAfterDownload: Be aware that the .TIFF subfolder still had some files inside and it has not been deleted.");
				}
			}
			
			// now that we moved all the files in ".TIFF" folder to the main unzipped directory, we can proceed to zip the folder, cleaned up from un-necessary files
			ZipFileUtils oZipUtils = new ZipFileUtils();
			String sSanitizedZipPath = sDownloadPath + File.separator + sFileName + ".zip";
			oZipUtils.zipFolder(oUnzippedFolder.getAbsolutePath(), sSanitizedZipPath);
			
			// once the zip has been produced, we can remove the unzipped folder
			WasdiFileUtils.deleteFile(sDownloadedFileFullPath);
			
			m_oProductFile = new File(sSanitizedZipPath); // here we change the pointer to the file, from the zip file to the _MTL.txt file, which provides the information for reading the bands
			sDownloadedFileFullPath = sSanitizedZipPath; // here I should put the name of the first folder

		} catch (Exception oEx) {
			WasdiLog.errorLog("Landsat5ProductReader.adjustFileAfterDownload: error ", oEx);
		}

		return sDownloadedFileFullPath;
	}
		

//	/**
//	 * Get the SNAP product or null if this is not a product readable by Snap
//	 * 
//	 * @return
//	 */
//	@Override
//	public Product getSnapProduct() {
//		if(m_bSnapReadAlreadyDone) {
//			return m_oProduct;
//		}
//		
//		//prepare path
//		String sBase = null;
//		File oOriginalProductFile = m_oProductFile;
//		try {
//			sBase = m_oProductFile.getAbsolutePath();
//			
//		} catch (Exception oE) {
//			WasdiLog.errorLog("Landsat5ProductReader.getSnapProduct: setting paths failed due to: " + oE);
//			return null;
//		}
//		
//		if (m_oProductFile.isDirectory()) {
//			File[] aoFiles = m_oProductFile.listFiles();
//			for (int i = 0; i < aoFiles.length; i++) {
//				File oCurrentFile = aoFiles[i];
//				if (oCurrentFile.isFile() && oCurrentFile.getName().endsWith("_MTL.txt")) {
//					String sFileName = oCurrentFile.getName();
//					WasdiLog.errorLog("Landsat5ProductReader.getSnapProduct: found a txt file: " + sFileName);
//					readProductBandFromFile(sBase, sFileName);
//				}
//			}
//		} else {
//			WasdiLog.debugLog("Landsat5ProductReader.getSnapProduct: " + sBase + "is not a folder");
//			return null;
//		}
//		
//		//reset the File pointer
//		m_oProductFile = oOriginalProductFile;
//
//		return m_oProduct;
//	}

//	/**
//	 * @param sBase
//	 * @param sFileName
//	 */
//	private void readProductBandFromFile(String sBase, String sFileName) {
//		WasdiLog.debugLog("Landsat5ProductReader.readProductBandFromFile: base path " + sBase + ", file name: " + sFileName);
//		try {
//			m_oProductFile = new File(sBase + File.separator + sFileName);
//			if(!m_oProductFile.exists()) {
//				WasdiLog.warnLog("Landsat5ProductReader.readProductBandFromFile: file " + sBase + File.separator + sFileName + " does not exist");
//				return;
//			}
//			if (m_bSnapReadAlreadyDone == false) {
//				m_oProduct = readSnapProduct();
//				WasdiLog.debugLog("Landsat5ProductReader.readProductBandFromFile. snap product has been read: " + m_oProduct.getName());
//			}
//			if(m_oProduct != null) {
//				m_bSnapReadAlreadyDone = true;
//				WasdiLog.debugLog("Landsat5ProductReader.readProductBandFromFile: snap product has been already read");
//			} else {
//				WasdiLog.debugLog("Landsat5ProductReader.readProductBandFromFile: snap product not yet read");
//				m_bSnapReadAlreadyDone = false;
//			}
//		}
//		catch (Exception oE) {
//			WasdiLog.errorLog("Landsat5ProductReader.readProductBandFromFile: tried to read " + sFileName + " but failed: " + oE);
//			m_bSnapReadAlreadyDone = false;
//		}
//	}
	

}
