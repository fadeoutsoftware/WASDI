/**
 * 
 */
package wasdi.io;

import java.io.File;

import org.esa.snap.core.datamodel.Product;

import wasdi.LauncherMain;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.ZipFileUtils;


/**
 * @author c.nattero
 *
 */
public class Sentinel3ProductReader extends SnapProductReader {

	/**
	 * @param oProductFile the Sentinel-3 (zip) file to be read
	 */
	public Sentinel3ProductReader(File oProductFile) {
		super(oProductFile);
	}

	@Override
	public String adjustFileAfterDownload(String sDownloadedFileFullPath, String sFileNameFromProvider) {
		try {
			if(Utils.isNullOrEmpty(sDownloadedFileFullPath)) {
				LauncherMain.s_oLogger.error("Sentinel3ProductReader.adjustFileAfterDownload: sDownloadedFileFullPath null or empty, aborting");
				return null;
			}
			if(Utils.isNullOrEmpty(sFileNameFromProvider)){
				LauncherMain.s_oLogger.error("Sentinel3ProductReader.adjustFileAfterDownload: sFileNameFromProvider null or empty, aborting");
				return null;
			}
			if(!sFileNameFromProvider.toLowerCase().startsWith("s3") || !sFileNameFromProvider.toLowerCase().endsWith(".zip")) {
				LauncherMain.s_oLogger.error("Sentinel3ProductReader.adjustFileAfterDownload: " + sFileNameFromProvider + " does not look like a Sentinel-3 file name");
				return null;
			}
		} catch (Exception oE) {
			LauncherMain.s_oLogger.error("Sentinel3ProductReader.adjustFileAfterDownload: arguments checking failed due to: " + oE + ", aborting");
			return null;
		}

		try {
			String sDownloadPath = new File(sDownloadedFileFullPath).getParentFile().getPath();
			LauncherMain.s_oLogger.debug("Sentinel3ProductReader.adjustFileAfterDownload: File is a Sentinel 3 image, start unzip");
			ZipFileUtils oZipExtractor = new ZipFileUtils();

			//remove .SEN3 from the file name -> required for CREODIAS
			String sNewFileName = sFileNameFromProvider.replaceAll(".SEN3", "");
			
			oZipExtractor.unzip(sDownloadPath + File.separator + sNewFileName, sDownloadPath);
			deleteDownloadedZipFile(sDownloadedFileFullPath);
			
			//remove .zip and add .SEN3 if required
			sNewFileName = sFileNameFromProvider.substring(0, sFileNameFromProvider.toLowerCase().lastIndexOf(".zip"));
			if(!sNewFileName.endsWith(".SEN3")) {
				sNewFileName = sNewFileName + ".SEN3";
			}
			
			String sFolderName = sDownloadPath + File.separator + sNewFileName;
			LauncherMain.s_oLogger.debug("Sentinel3ProductReader.adjustFileAfterDownload: Unzip done, folder name: " + sFolderName);
			LauncherMain.s_oLogger.debug("Sentinel3ProductReader.adjustFileAfterDownload: File Name changed in: " + sFolderName);

			m_oProductFile = new File(sFolderName);
			return sFolderName;
		}
		catch (Exception oEx) {
			LauncherMain.s_oLogger.error("Sentinel3ProductReader.adjustFileAfterDownload: error ", oEx);
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
				LauncherMain.s_oLogger.error("Sentinel3ProductReader.deleteZipFile: cannot delete zip file");
			} else {
				LauncherMain.s_oLogger.debug("Sentinel3ProductReader.deleteZipFile: file zip successfully deleted");
			}
		} catch (Exception oE) {
			LauncherMain.s_oLogger.error("Sentinel3ProductReader.deleteZipFile: exception while trying to delete zip file: " + oE ); 
		}
	}

	
	/**
	 * Get the SNAP product or null if this is not a product readable by Snap
	 * 
	 * @return
	 */
	@Override
	public Product getSnapProduct() {
		try {
			//save File pointing to directory
			m_oProductFile = new File(m_oProductFile.getAbsolutePath() + File.separator + "xfdumanifest.xml");
			//business as usual
			if (m_bSnapReadAlreadyDone == false) {
				m_oProduct = readSnapProduct();
			}
			//reset the File pointer
			m_oProductFile = m_oProductFile.getParentFile();
			
			return m_oProduct;
		}
		catch (Exception oE) {
			LauncherMain.s_oLogger.error("Sentinel3ProductReader.getSnapProduct: " + oE);
		}
		return null;
	}
}
