/**
 * Created by Cristiano Nattero on 2020-01-13
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
public class CREODIASProviderAdapter extends ProviderAdapter {

	/**
	 * 
	 */
	public CREODIASProviderAdapter() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param logger
	 */
	public CREODIASProviderAdapter(Logger logger) {
		super(logger);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see wasdi.filebuffer.ProviderAdapter#GetDownloadFileSize(java.lang.String)
	 */
	@Override
	public long GetDownloadFileSize(String sFileURL) throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see wasdi.filebuffer.ProviderAdapter#ExecuteDownloadFile(java.lang.String, java.lang.String, java.lang.String, java.lang.String, wasdi.shared.business.ProcessWorkspace)
	 */
	@Override
	public String ExecuteDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see wasdi.filebuffer.ProviderAdapter#GetFileName(java.lang.String)
	 */
	@Override
	public String GetFileName(String sFileURL) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
