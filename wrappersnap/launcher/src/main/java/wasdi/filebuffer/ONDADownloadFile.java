/**
 * Created by Cristiano Nattero on 2018-12-18
 * 
 * Fadeout software
 *
 */
package wasdi.filebuffer;

import org.apache.log4j.Logger;

import wasdi.shared.business.ProcessWorkspace;

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
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see wasdi.filebuffer.DownloadFile#ExecuteDownloadFile(java.lang.String, java.lang.String, java.lang.String, java.lang.String, wasdi.shared.business.ProcessWorkspace)
	 */
	@Override
	public String ExecuteDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace) throws Exception {
		// TODO Auto-generated method stub
		return null;
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
