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

	String m_sPrefix = "http:file:";
	
	/**
	 * 
	 */
	public ONDADownloadFile() {
		
	}

	/**
	 * @param logger
	 */
	public ONDADownloadFile(Logger logger) {
		super(logger);
	}

	/* (non-Javadoc)
	 * @see wasdi.filebuffer.DownloadFile#GetDownloadFileSize(java.lang.String)
	 */
	@Override
	public long GetDownloadFileSize(String sFileURL) throws Exception {
		//TODO get rid of the "http:" part
		//http:file:/mnt/OPTICAL/LEVEL-1C/2018/12/12/S2B_MSIL1C_20181212T010259_N0207_R045_T54PZA_20181212T021706.zip.value
		long lLenght = 0L;
		
		if(sFileURL.startsWith(m_sPrefix)) {
			
			// Remove the prefix
			int iStart = sFileURL.indexOf(m_sPrefix) +m_sPrefix.length();
			String sSourceFilePath = sFileURL.substring(iStart);
			
			// remove the .value
			sSourceFilePath = sSourceFilePath.substring(0, sSourceFilePath.lastIndexOf('.'));
			
			// This is the folder: we need the .value file
			sSourceFilePath += "/.value";
			File oSourceFile = new File(sSourceFilePath);
			lLenght = oSourceFile.length();
		}
		
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
		
		//@Cristiano: No, lo fa prima la downlaod. Cancelliamo il commento :) : check if the file exists somewhere else on the local file system, i.e., in some other workspaces 
		if(sFileURL.startsWith(m_sPrefix)) {
			
			// Remove the prefix
			int iStart = sFileURL.indexOf(m_sPrefix) +m_sPrefix.length();
			String sSourceFilePath = sFileURL.substring(iStart);
			
			// remove the .value
			sSourceFilePath = sSourceFilePath.substring(0, sSourceFilePath.lastIndexOf('.'));
			
			// This is the folder: we need the .value file
			sSourceFilePath += "/.value";
			File oSourceFile = new File(sSourceFilePath);
			
			// Destination file name: start from the simple name
			String sDestinationFileName = GetFileName(sFileURL);
			// set the destination folder
			if (sSaveDirOnServer.endsWith("/") == false) sSaveDirOnServer += "/";
			sDestinationFileName = sSaveDirOnServer + sDestinationFileName;
			
			// copy the product from file system
			FileUtils.copyFile(oSourceFile, new File(sDestinationFileName));
			return sDestinationFileName;
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
		
		// Format check
		if(sFileURL.startsWith("http:file:")) {
			
			// Remove prefix
			int iStart = sFileURL.indexOf(m_sPrefix) +m_sPrefix.length();
			String sSourceFilePath = sFileURL.substring(iStart);
			
			// Create a file to have the name
			File oInputFile = new File(sSourceFilePath);
			String sOnlyName = oInputFile.getName(); 
			
			// Remove the ONDA .value extension
			return sOnlyName.substring(0, sOnlyName.lastIndexOf('.'));
		}
		
		return null;
	}

}
