package wasdi.dataproviders;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class STATICSProviderAdapter extends ProviderAdapter {
	
	public STATICSProviderAdapter() {
		super();
		m_sDataProviderCode = "STATICS";
	}
	
	@Override
	protected void internalReadConfig() {
		
	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		return 0;
	}
	
	/**
	 * Download file using WCS server.
	 * The sFileURL name is supposed to be:
	 * 	sFileURL  = geoserver_address;LayerId;CRS;dWest,dSouth,dEast,dNorth;sOutputFileName
	 * 
	 */
	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
		
		WasdiLog.debugLog("STATICSProviderAdapter.executeDownloadFile: try to get " + sFileURL);
		
		if (Utils.isNullOrEmpty(sFileURL)) return "";
		
		try {
			String [] asQueryParts = sFileURL.split(";");
			
			String sLayerId = asQueryParts[1];
			String sCrs = asQueryParts[2];
			String sBbox = asQueryParts[3];
			String sOutputFileName = asQueryParts[4];
			
			String []asBboxParts = sBbox.split(",");
	    	
	    	String sUrl = asQueryParts[0];
	    	
	    	if (!sUrl.endsWith("/")) {
	    		sUrl += "/";
	    	}
	    	
	    	sUrl += "wcs?service=WCS&version=2.0.0&request=GetCoverage&coverageId=wasdi:";
	    	sUrl += sLayerId;
	    	sUrl += "&crs="+"urn:ogc:def:crs:"+sCrs;
	    	//sUrl += "&bbox=" + sBbox;
	    	sUrl += "&subset=Lat(" + asBboxParts[1] + "," + asBboxParts[3] +")";
	    	sUrl += "&subset=Long(" + asBboxParts[0] + "," + asBboxParts[2] +")";
	    	sUrl += "&FORMAT=image/tiff;application=geotiff&GEOTIFF:COMPRESSION=LZW";
	    	
	    	WasdiLog.debugLog("STATICSProviderAdapter.executeDownloadFile: Generated URL: " + sUrl);
	    	
	    	m_iHttpDownloadReadTimeoutMultiplier = 10;
	    	
	    	WasdiLog.debugLog("STATICSProviderAdapter.executeDownloadFile: setting read timeout multiplier to " + m_iHttpDownloadReadTimeoutMultiplier);
	    	
	    	return downloadViaHttp(sUrl, sDownloadUser, sDownloadPassword, sSaveDirOnServer, sOutputFileName);			
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("STATICSProviderAdapter.executeDownloadFile: exception: " + oEx.toString());
		}
		
		return "";
	}

	@Override
	public String getFileName(String sFileURL) throws Exception {
		try {
			String [] asQueryParts = sFileURL.split(";");
			
			String sOutputFileName = asQueryParts[4];
	    	
	    	return sOutputFileName;			
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("STATICSProviderAdapter.getFileName: exception: " + oEx.toString());
		}
		
		return "";
	}

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		
		if (sPlatformType.equals(Platforms.STATICS)) {
			return DataProviderScores.DOWNLOAD.getValue();
		}
		
		return 0;
	}
}


