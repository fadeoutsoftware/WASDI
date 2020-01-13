/**
 * Created by Cristiano Nattero on 2020-01-13
 * 
 * Fadeout software
 *
 */
package wasdi.filebuffer;

import org.apache.log4j.Logger;

import com.google.java.contract.Ensures;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.opensearch.DiasResponseTranslatorCREODIAS;
import wasdi.shared.utils.Utils;

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
	@Ensures("sResult!=null")
	public String GetFileName(String sFileURL) throws Exception {
		if (Utils.isNullOrEmpty(sFileURL)) {
			m_oLogger.debug("CREODIASProviderAdapter.GetFileName: sFileURL is null or Empty");
			m_oLogger.fatal("CREODIASProviderAdapter.GetFileName: sFileURL is null or Empty");
			return "";
		}
		
		String sResult = "";
		if(sFileURL.contains( DiasResponseTranslatorCREODIAS.SLINK_SEPARATOR_CREODIAS ) ) {
			int iStart = sFileURL.indexOf(DiasResponseTranslatorCREODIAS.SLINK_SEPARATOR_CREODIAS);
			sResult = sFileURL.substring(iStart + DiasResponseTranslatorCREODIAS.SLINK_SEPARATOR_CREODIAS.length());
		}
		
		return sResult;
	}

}
