/**
 * Created by Cristiano Nattero on 2018-12-18
 * 
 * Fadeout software
 *
 */
package wasdi.filebuffer;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.utils.Utils;

/**
 * @author c.nattero
 *
 */
public class ONDADownloadFile extends DownloadFile {

	/**
	 * 
	 */
	public ONDADownloadFile() {
		//Auto-generated constructor stub
	}

	/**
	 * @param logger
	 */
	public ONDADownloadFile(Logger logger) {
		super(logger);
		//Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see wasdi.filebuffer.DownloadFile#GetDownloadFileSize(java.lang.String)
	 */
	@Override
	public long GetDownloadFileSize(String sFileURL) throws Exception {
		//TODO get rid of the "http:" part
		//http:file:/mnt/OPTICAL/LEVEL-1C/2018/12/12/S2B_MSIL1C_20181212T010259_N0207_R045_T54PZA_20181212T021706.zip.value
		long lLenght = 0L;
		//TODO read file length from file system
		return lLenght;
	}

	/* (non-Javadoc)
	 * @see wasdi.filebuffer.DownloadFile#ExecuteDownloadFile(java.lang.String, java.lang.String, java.lang.String, java.lang.String, wasdi.shared.business.ProcessWorkspace)
	 */
	@Override
	public String ExecuteDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace) throws Exception {
		// Domain check
		if (Utils.isNullOrEmpty(sFileURL)) {
			logger.debug("ONDAVDownloadFile.ExecuteDownloadFile: sFileURL is null");
			return "";
		}
		if (Utils.isNullOrEmpty(sSaveDirOnServer)) {
			logger.debug("ONDADownloadFile.ExecuteDownloadFile: sSaveDirOnServer is null");
			return "";
		}
		
		
		//TODO get rid of the "http:" part
		//http:file:/mnt/OPTICAL/LEVEL-1C/2018/12/12/S2B_MSIL1C_20181212T010259_N0207_R045_T54PZA_20181212T021706.zip.value
		
		//TODO check string format
		
		//TODO check if the file exists somewhere else on the local file system, i.e., in some other workspaces 
		if(sFileURL.startsWith("http:file:")) {
			String sPrefix = "http:file:";
			//TODO get the product from file system
			int iStart = sFileURL.indexOf(sPrefix) +sPrefix.length();
			String sSourceFilePath = sFileURL.substring(iStart);
			File oSourceFile = new File(sSourceFilePath);
			FileUtils.copyFile(oSourceFile, new File(sSaveDirOnServer));
			return "";
		} else if(sFileURL.startsWith("http:")) {
			//TODO download the file
			return "";
		}
		return "";
	}

	/* (non-Javadoc)
	 * @see wasdi.filebuffer.DownloadFile#GetFileName(java.lang.String)
	 */
	@Override
	public String GetFileName(String sFileURL) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
