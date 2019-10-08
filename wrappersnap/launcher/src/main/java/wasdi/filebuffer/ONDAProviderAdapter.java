/**
 * Created by Cristiano Nattero on 2018-12-18
 * 
 * Fadeout software
 *
 */
package wasdi.filebuffer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.apache.commons.net.io.Util;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.sleepycat.je.rep.impl.TextProtocol.OK;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.utils.Utils;

/**
 * @author c.nattero
 *
 */
public class ONDAProviderAdapter extends ProviderAdapter {

	/**
	 * 
	 */
	public ONDAProviderAdapter() {

	}

	/**
	 * @param logger
	 */
	public ONDAProviderAdapter(Logger logger) {
		super(logger);
	}

	/* (non-Javadoc)
	 * @see wasdi.filebuffer.DownloadFile#GetDownloadFileSize(java.lang.String)
	 */
	@Override
	public long GetDownloadFileSize(String sFileURL) throws Exception {
		//file:/mnt/OPTICAL/LEVEL-1C/2018/12/12/S2B_MSIL1C_20181212T010259_N0207_R045_T54PZA_20181212T021706.zip/.value

		m_oLogger.debug("ONDAProviderAdapter.GetDownloadSize: start " + sFileURL);

		long lLenght = 0L;

		if(sFileURL.startsWith("file:")) {

			String sPrefix = "file:";
			// Remove the prefix
			int iStart = sFileURL.indexOf(sPrefix) +sPrefix.length();
			String sPath = sFileURL.substring(iStart);

			m_oLogger.debug("ONDAProviderAdapter.GetDownloadSize: full path " + sPath);
			File oSourceFile = new File(sPath);
			lLenght = oSourceFile.length();
			if (!oSourceFile.exists()) {
				m_oLogger.debug("ONDAProviderAdapter.GetDownloadSize: FILE DOES NOT EXISTS");
			}
			m_oLogger.debug("ONDAProviderAdapter.GetDownloadSize: Found length " + lLenght);
		} else if(sFileURL.startsWith("https:")) {
			//lLenght = getSizeViaHttp(sFileURL);
			lLenght = getDownloadFileSizeViaHttp(sFileURL);
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
			m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: sFileURL is null");
			return "";
		}
		if (Utils.isNullOrEmpty(sSaveDirOnServer)) {
			m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: sSaveDirOnServer is null");
			return "";
		}
		
		m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: start");
		
		setProcessWorkspace(oProcessWorkspace);

		if(sFileURL.startsWith("file:")) {
			//  file:/mnt/OPTICAL/LEVEL-1C/2018/12/12/S2B_MSIL1C_20181212T010259_N0207_R045_T54PZA_20181212T021706.zip/.value		
			m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: this is a \"file:\" protocol, get file name");
			
			String sPrefix = "file:";
			// Remove the prefix
			int iStart = sFileURL.indexOf(sPrefix) +sPrefix.length();
			String sPath = sFileURL.substring(iStart);

			m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: source file: " + sPath);
			File oSourceFile = new File(sPath);
			
			// Destination file name: start from the simple name
			String sDestinationFileName = GetFileName(sFileURL);
			// set the destination folder
			if (sSaveDirOnServer.endsWith("/") == false) sSaveDirOnServer += "/";
			sDestinationFileName = sSaveDirOnServer + sDestinationFileName;
			
			m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: destination file: " + sDestinationFileName);

			// copy the product from file system
			try {
				File oDestionationFile = new File(sDestinationFileName);
				
				if (oDestionationFile.getParentFile() != null) { 
					if (oDestionationFile.getParentFile().exists() == false) {
						oDestionationFile.getParentFile().mkdirs();
					}
				}
				
				InputStream oInputStream = new FileInputStream(oSourceFile);
				OutputStream oOutputStream = new FileOutputStream(oDestionationFile);
				
				m_oLogger.debug("ONDAProviderAdapter.ExecuteDownloadFile: start copy stream");
				
				//TODO change method signature and check result: if it fails try https or retry if timeout
				//MAYBE pass the entire pseudopath so that if one does not work, another one can be tried
				copyStream(oProcessWorkspace, oSourceFile.length(), oInputStream, oOutputStream);

			} catch (Exception e) {
				m_oLogger.info("ONDAProviderAdapter.ExecuteDownloadFile: " + e);
			}
			//TODO else - i.e., if it fails - try get the file from https instead
			//	- in this case the sUrl must be modified in order to include http, so that it can be retrieved  
			return sDestinationFileName;
		} else if(sFileURL.startsWith("https://")) {
			//  https://catalogue.onda-dias.eu/dias-catalogue/Products(357ae76d-f1c4-4f25-b535-e278c3f937af)/$value
			
			Boolean bAvailable = false;
			String sResult = null;
			//TODO parameter
			int iAttempts = 3;
			boolean bOrdered = false;
			while(!bAvailable) {
				bAvailable = checkProductAvailability(sFileURL, sDownloadUser, sDownloadPassword);
				if(null == bAvailable) {
					//TODO error handling
				} else if(bAvailable) {
					sResult = downloadViaHttp(sFileURL, sDownloadUser, sDownloadPassword, sSaveDirOnServer);
				} else {
					//TODO parameter
					long lDeltaT = 10;
					String sDate = placeOrder(sFileURL, sDownloadUser, sDownloadPassword);
					if(null!=sDate) {
						//TODO inform the client
						LocalDateTime oNow = LocalDateTime.now();
						LocalDateTime oExpected = LocalDateTime.parse(sDate);
						lDeltaT = Math.max(lDeltaT, oExpected.toEpochSecond(null) - oNow.toEpochSecond(null));
						//add some padding
						//TODO parameter
						lDeltaT += 10;
					}
					if(iAttempts < 0) {
						break;
					}
					iAttempts++;
					TimeUnit.SECONDS.sleep(lDeltaT);
				}
			}
			//TODO else - i.e., if it fails - try get the file from the file system or retry if timeout
			//  - in this case the sUrl must be modified in order to include file id, so that it can be retrieved			
			return sResult;
			

		}
		return "";
	}

	
	private Boolean checkProductAvailability(String sFileURL, String sDownloadUser, String sDownloadPassword) {
		m_oLogger.debug( "ONDAProviderAdapter.checkProductAvailability( " + sFileURL + ", " + sDownloadUser + ", " + sDownloadPassword + " )");
		String sUUID = extractProductUUID(sFileURL);
		return checkProductAvailabilityFromUUID(sUUID, sDownloadUser, sDownloadPassword);
	}
	
	private Boolean checkProductAvailabilityFromUUID(String sUUID, String sDownloadUser, String sDownloadPassword) {
		m_oLogger.debug( "ONDAProviderAdapter.checkProductAvailability( " + sUUID + ", " + sDownloadUser + ", " + sDownloadPassword + " )");
		
		String sCheckUrl = "https://catalogue.onda-dias.eu/dias-catalogue/Products(" + sUUID + ")?$select=offline,downloadable";
		try {
				
			URL oUrl = new URL(sCheckUrl);
	        HttpURLConnection oHttpConn = (HttpURLConnection) oUrl.openConnection();
	        oHttpConn.setRequestMethod("GET");
			oHttpConn.setRequestProperty("Accept", "application/json");
	        oHttpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:57.0) Gecko/20100101 Firefox/57.0");
	        int iResponseCode = oHttpConn.getResponseCode();
	        if (iResponseCode == HttpURLConnection.HTTP_OK) {
	        	//get json
	        	String sJson = null;
	        	try {
	    			InputStream oInputStream = oHttpConn.getInputStream();		
					if(null!=oInputStream) {
						ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
						Util.copyStream(oInputStream, oBytearrayOutputStream);
						sJson = oBytearrayOutputStream.toString();
					}
					oHttpConn.disconnect();
	        	}catch (Exception oE) {
	    			m_oLogger.error( "ONDAProviderAdapter.checkProductAvailabilityFromUUID( " + sUUID + ", " + sDownloadUser + ", " + sDownloadPassword +
" ): error retrieving  reply: " + oE);
	    			return null;
	        	}
	        	//then parse json
	        	try {
	        		Boolean bAvailable = null;
					if(!Utils.isNullOrEmpty(sJson)) {
						JSONObject oJson = new JSONObject(sJson);
						if( oJson.has("offline") ) {
							bAvailable = oJson.optBoolean("offline");
							if(null!=bAvailable) {
								//if offline then not available
								bAvailable = !bAvailable;
							}
						} else if(oJson.has("downloadable")) {
							bAvailable = oJson.optBoolean("downloadable"); 
						} else {
							m_oLogger.error( "ONDAProviderAdapter.checkProductAvailabilityFromUUID( " + sUUID + ", " + sDownloadUser + ", " + sDownloadPassword + "): cannot infer availability from json" );
						}
					}
					return bAvailable;
	        	}catch (Exception oE) {
	        		m_oLogger.error( "ONDAProviderAdapter.checkProductAvailabilityFromUUID( " + sUUID + ", " + sDownloadUser + ", " + sDownloadPassword + "): during parse: " + oE);
	    			return null;
	        	}
	        } else {
	        	m_oLogger.info( "ONDAProviderAdapter.checkProductAvailabilityFromUUID( " + sUUID + ", " + sDownloadUser + ", " + sDownloadPassword + "): Server replied with HTTP code: " + iResponseCode);
	            InputStream oErrorStream = oHttpConn.getErrorStream();
	            if(null!=oErrorStream) {
					ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
					Util.copyStream(oErrorStream, oBytearrayOutputStream);
					String sError = oBytearrayOutputStream.toString();
					if(!Utils.isNullOrEmpty(sError)) {
						m_oLogger.error( "ONDAProviderAdapter.checkProductAvailabilityFromUUID( " + sUUID + ", " + sDownloadUser + ", " + sDownloadPassword + "): additional error info: " + sError );
					}
				}
	            oHttpConn.disconnect();
	            return null;
	        }
			
		} catch (Exception oE) {
			m_oLogger.error( "ONDAProviderAdapter.checkProductAvailabilityFromUUID( " + sUUID + " ): error on get: " + oE );
			return null;
		}
	}
	
	private String extractProductUUID(String sUrl) {
		// we expect a link in the form
		// https://catalogue.onda-dias.eu/dias-catalogue/Products(865b6925-59ba-49be-8444-8c99d3f0c3c4)/$value
		int iStart = sUrl.indexOf('(') + 1;
		int iEnd = sUrl.indexOf(')');
		String sUUID = sUrl.substring(iStart, iEnd);
		return sUUID;
	}
	
	private String placeOrder(String sFileUrl, String sDownloadUser, String sDownloadPassword) {
		String sUUID = extractProductUUID(sFileUrl);
		return placeOrderWithUUID(sUUID, sDownloadUser, sDownloadPassword);
	}
	
	private String placeOrderWithUUID(String sUUID, String sDownloadUser, String sDownloadPassword) {
		String sOrderUrl = "https://catalogue.onda-dias.eu/dias-catalogue/Products(" + sUUID + ")/Ens.Order";
		
		m_oLogger.debug("ONDAProviderAdapter.placeOrder( " + sOrderUrl + ", " + sDownloadUser + ", " + sDownloadPassword + " )");
		
		if (sDownloadUser != null) {
			Authenticator.setDefault(new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					try {
						return new PasswordAuthentication(sDownloadUser, sDownloadPassword.toCharArray());
					} catch (Exception oEx) {
						m_oLogger.error("ONDAProviderAdapter.placeOrder( " + sOrderUrl + ", " + sDownloadUser + ", " + sDownloadPassword + " ): " + oEx);
					}
					return null;
				}
			});
		}
		URL oUrl;
		try {
			oUrl = new URL(sOrderUrl);
	        HttpURLConnection oHttpConn = (HttpURLConnection) oUrl.openConnection();
	        oHttpConn.setRequestMethod("POST");
			oHttpConn.setRequestProperty("Accept", "application/json");
	        oHttpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:57.0) Gecko/20100101 Firefox/57.0");
	        int iResponseCode = oHttpConn.getResponseCode();
	        if (iResponseCode == HttpURLConnection.HTTP_OK) {
	        	String sJson = null;
	        	InputStream oInputStream = oHttpConn.getInputStream();
	        	ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
				Util.copyStream(oInputStream, oBytearrayOutputStream);
				sJson = oBytearrayOutputStream.toString();
	        	if(!Utils.isNullOrEmpty(sJson)) {
	        		try {
	        			String sDate = null;
	        			JSONObject oJson = new JSONObject(sJson);
	        			if(oJson.has("EstimatedTime")) {
							sDate = oJson.optString("EstimatedTime", null);
						}
			        	return sDate;
	        		} catch (Exception oEx) {
	        			m_oLogger.error("ONDAProviderAdapter.placeOrder( " + sOrderUrl + ", " + sDownloadUser + ", " + sDownloadPassword + " ): JSON creation failed: " + oEx);
	        		}	        		
	        	} else {
	        		m_oLogger.error("ONDAProviderAdapter.placeOrder( " + sOrderUrl + ", " + sDownloadUser + ", " + sDownloadPassword + " ): JSON string is null or empty");
	        	}
	        } else {
	        	m_oLogger.error("ONDAProviderAdapter.placeOrder( " + sOrderUrl + ", " + sDownloadUser + ", " + sDownloadPassword + " ): server returned status: " + iResponseCode );
        		InputStream  oErrorStream = oHttpConn.getErrorStream();
        		ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
        		Util.copyStream(oErrorStream, oBytearrayOutputStream);
        		String sError = oBytearrayOutputStream.toString();
        		m_oLogger.error("ONDAProviderAdapter.placeOrder( " + sOrderUrl + ", " + sDownloadUser + ", " + sDownloadPassword + " ): additional message: " + sError );
        		return null;
	        }
		} catch (Exception oEx) {
			m_oLogger.debug("ONDAProviderAdapter.placeOrder( " + sOrderUrl + ", " + sDownloadUser + ", " + sDownloadPassword + " ): connection failed: " + oEx);
			return null;
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see wasdi.filebuffer.DownloadFile#GetFileName(java.lang.String)
	 */
	@Override
	public String GetFileName(String sFileURL) throws Exception {
		//check whether the file has already been downloaded, else return null

		if (Utils.isNullOrEmpty(sFileURL)) {
			m_oLogger.debug("ONDAProviderAdapter.GetFileName: sFileURL is null or Empty");
			m_oLogger.fatal("ONDAProviderAdapter.GetFileName: sFileURL is null or Empty");
			return "";
		}

		if(sFileURL.startsWith("file:")) {
			
			// In Onda, the real file is .value but here we need the name of Satellite image that, in ONDA is the parent folder name
			String sPrefix = "file:";
			String sSuffix = "/.value";
			// Remove the prefix
			int iStart = sFileURL.indexOf(sPrefix) +sPrefix.length();
			String sPath = sFileURL.substring(iStart);

			// remove the ".value" suffix
			sPath = sPath.substring(0, sPath.lastIndexOf(sSuffix));

			// Destination file name: start from the simple name, i.e., exclude the containing dir, slash included:
			String sDestinationFileName = sPath.substring( sPath.lastIndexOf("/") + 1);
			return sDestinationFileName;

		} else if(sFileURL.startsWith("https://")) {
			return getFileNameViaHttp(sFileURL);
		} 
		return null;	
	}
	
}
