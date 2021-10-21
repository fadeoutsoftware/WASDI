package wasdi.filebuffer;

import java.io.File;

import wasdi.io.WasdiProductReaderFactory;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.utils.Utils;

public class EODCProviderAdapter extends ProviderAdapter{

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		m_oLogger.debug("EODCProviderAdapter.GetDownloadSize: start " + sFileURL);

		long lLenght = 0L;

		if (isFileProtocol(sFileURL)) {

			String sPath = removePrefixFile(sFileURL);
			File oSourceFile = new File(sPath);

			lLenght = getSourceFileLength(oSourceFile);
		} else if(isHttpsProtocol(sFileURL)) {
			lLenght = getDownloadFileSizeViaHttp(sFileURL);
		}

		return lLenght;
	}

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {

		// Domain check
		if (Utils.isNullOrEmpty(sFileURL)) {
			m_oLogger.debug("EODCProviderAdapter.ExecuteDownloadFile: sFileURL is null");
			return "";
		}
		if (Utils.isNullOrEmpty(sSaveDirOnServer)) {
			m_oLogger.debug("EODCProviderAdapter.ExecuteDownloadFile: sSaveDirOnServer is null");
			return "";
		}
		
		m_oLogger.debug("EODCProviderAdapter.ExecuteDownloadFile: start");
		
		setProcessWorkspace(oProcessWorkspace);

		if (isFileProtocol(sFileURL)) {
			return localFileCopy(sFileURL, sSaveDirOnServer, iMaxRetry);
		}
		
		return "";
	}

	@Override
	public String getFileName(String sFileURL) throws Exception {
		
		//extract file name

		if (Utils.isNullOrEmpty(sFileURL)) {
			m_oLogger.error("EODCProviderAdapter.GetFileName: sFileURL is null or Empty");
			return "";
		}

		if (isFileProtocol(sFileURL)) {
			
			// In Onda, the real file is .value but here we need the name of Satellite image that, in ONDA is the parent folder name

			String sPath = removePrefixFile(sFileURL);
			//sPath = removeSuffixValue(sPath);

			return extractDestinationFileName(sPath);

		} 		
		
		return "";
	}
	
	@Override
	public void readConfig() {
		
	}
	

}
