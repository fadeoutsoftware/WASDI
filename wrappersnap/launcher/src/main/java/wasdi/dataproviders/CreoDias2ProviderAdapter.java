package wasdi.dataproviders;

import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.IDN;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipOutputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;


import com.google.common.base.Preconditions;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.creodias2.ResponseTranslatorCreoDias2;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;

public class CreoDias2ProviderAdapter extends ProviderAdapter {
	
	private static final String SODATA_ATTRIBUTES = "Attributes";
	private static final String SODATA_NAME = "Name";
	private static final String SODATA_VALUE = "Value";
	
	private static final String SAUTHENTICATION_URL = "https://identity.cloudferro.com/auth/realms/wekeo-elasticity/protocol/openid-connect/token";
	private static final String SDOWNLOAD_URL_START = "https://zipper.dataspace.copernicus.eu/odata/v1/Products(";
	private static final String SDOWNLOAD_URL_END = "?token=";
	
	private static final String SODATA_FILE_URL_START = "https://datahub.creodias.eu/odata/v1/Products(";
	private static final String SODATA_FILE_URL_END = ")/$value";
	
	private static final String SCREODIAS_BASE_URL = "https://datahub.creodias.eu/odata/v1/Products?";
	
	/**
	 * Base path of the folder mounted with EO Data
	 */
	private String m_sProviderBasePath = "";
	
	
	public CreoDias2ProviderAdapter() {
		super();
		m_sDataProviderCode = "CREODIAS2";
	}
	

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		// receives the file URI and must return the size of the file. Useful to give progress to the user
		// TODO: are we are talking about the overall file size? or just the size has been downloaded until a certain point?
		
		WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize. Retrieving information for product: " + sFileURL);
		
		// create a creodias query like: https://datahub.creodias.eu/odata/v1/Products?$filter=Id eq '79d7807b-66e5-41d2-a586-1628eb82c3e0'&$expand=Attributes
		
		
		long lSizeInBytes = 0L;

		
		if (isFileProtocol(m_sDefaultProtocol)) {
			String sPath = null;
			if (isFileProtocol(sFileURL)) {
				sPath = removePrefixFile(sFileURL);
				WasdiLog.debugLog("CREODIASProviderAdapter.getDownloadFileSize: file protocol detected. File path " + sFileURL);
			} else if(isHttpsProtocol(sFileURL)) {
				sPath = extractFilePathFromHttpsUrl(sFileURL);
				WasdiLog.debugLog("CREODIASProviderAdapter.getDownloadFileSize: http protocol detected. File path " + sFileURL);
			} else {
				WasdiLog.debugLog("CREODIASProviderAdapter.getDownloadFileSize: unknown protocol " + sFileURL);
			}

			if (sPath != null) {
				File oSourceFile = new File(sPath);

				if (oSourceFile != null && oSourceFile.exists()) {
					lSizeInBytes = getSourceFileLength(oSourceFile);
					WasdiLog.debugLog("CREODIASProviderAdapter.getDownloadFileSize: detected file size: " + lSizeInBytes);
					return lSizeInBytes;
				}
			}
		}
		

		if (isHttpsProtocol(sFileURL)) {
			
			String sDownloadUrl = getODataDownloadUrl(sFileURL);
			String sProductId = getProductIdFromURL(sDownloadUrl);
			String sUrl = SCREODIAS_BASE_URL + "$filter=Id eq '" + sProductId + "'&$expand=Attributes";
			URL oURL = new URL(sUrl);
			URI oURI = new URI(oURL.getProtocol(), oURL.getUserInfo(), IDN.toASCII(oURL.getHost()), oURL.getPort(), oURL.getPath(), oURL.getQuery(), oURL.getRef());
			
			HttpCallResponse sResponse = null;
			
			try {
				sResponse = HttpUtils.httpGet(oURI.toASCIIString());
				if (sResponse == null || sResponse.getResponseCode() < 200 || sResponse.getResponseCode() > 299 || Utils.isNullOrEmpty(sResponse.getResponseBody())) {
					WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize. Error retrieving the information about the product from the provider. " + sUrl);
					return -1L;
				}
			} catch (Exception oEx) {
				WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize. An exception occurred while retrieving the product information from the provider. " + oEx.getMessage());
				return -1L;
			}
			
			WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize. Anwer well received from the provider");
			
			
			JSONObject jsonO = new JSONObject(sResponse.getResponseBody());
			JSONArray arr = jsonO.getJSONArray("value");
			System.out.println(arr.length());
			JSONObject jsonObject = (JSONObject) arr.get(0);
			System.out.println(jsonObject.optString("ContentLength"));	
			
			
			JSONObject oJsonBody = new JSONObject(sResponse.getResponseBody());
			JSONArray aoValues = oJsonBody.optJSONArray("value");
			if (aoValues == null || aoValues.length() != 1) {
				WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize. Array of attributes not found on CreoDias");
				return 0;
			}
			
			JSONObject oValue = (JSONObject) aoValues.get(0);
			String sFileSize = oValue.optString(ResponseTranslatorCreoDias2.SODATA_SIZE, "0");
			
			
			if (Utils.isNullOrEmpty(sFileSize)) {
				WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize. Array of attributes not found for key " + ResponseTranslatorCreoDias2.SODATA_SIZE);
			}
			
			lSizeInBytes = Long.parseLong(sFileSize);
			WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize. File size: " + lSizeInBytes);

		}

		return lSizeInBytes;
		
	}
	
	
	
	/**
	 * Extract the file-system path of the file out of an HTTPS URL.
	 * @param sHttpsURL the HTTPS URL containing the product identifier (i.e. /eodata/Sentinel-1/SAR/GRD/2021/01/01/S1B_S1_GRDH_1SDH_20210101T152652_20210101T152706_024962_02F890_39B4.SAFE)
	 * @return the file-system path (i.e. C:/temp/wasdi//eodata/Sentinel-1/SAR/GRD/2021/01/01/S1B_S1_GRDH_1SDH_20210101T152652_20210101T152706_024962_02F890_39B4.SAFE)
	 */
	private String extractFilePathFromHttpsUrl(String sHttpsURL) {
		String sProductIdentifier = extractProductIdentifierFromURL(sHttpsURL);
		String filesystemPath = m_sProviderBasePath + sProductIdentifier;

		WasdiLog.debugLog("CREODIASProviderAdapter.extractFilePathFromHttpsUrl: HTTPS URL: " + sProductIdentifier);
		WasdiLog.debugLog("CREODIASProviderAdapter.extractFilePathFromHttpsUrl: file path: " + filesystemPath);

		return filesystemPath;
	}
	
	private String extractProductIdentifierFromURL(String sFileURL) {
		Preconditions.checkNotNull(sFileURL, "URL is null");
		try {
			String[] asParts = sFileURL.split(ResponseTranslatorCreoDias2.SLINK_SEPARATOR_CREODIAS2);
			String sProductIdentifier = asParts[ResponseTranslatorCreoDias2.IPOSITIONOF_PRODUCTIDENTIFIER];
			return sProductIdentifier;
		} catch (Exception oE) {
			WasdiLog.errorLog("CREODIASProviderAdapter.extractProductIdentifierFromURL: " + oE);
		}
		return null;
	}
	
	/**
	 * Get The length, in bytes, of the source-file.
	 * @param oSourceFile the source-file
	 * @return the length, in bytes, of the source-file, or 0L if the file does not exist
	 */
	protected long getSourceFileLength(File oSourceFile) {
		if (oSourceFile == null) {
			WasdiLog.debugLog("ProviderAdapter.getSourceFileLength: FILE DOES NOT EXISTS");
			return 0L;
		}
		
		WasdiLog.debugLog("ProviderAdapter.getSourceFileLength: Get file lengh for file " + oSourceFile.getAbsolutePath());

		long lLenght;
		if (oSourceFile.isDirectory()) {
			lLenght = 0L;

			for (File f : oSourceFile.listFiles()) {
				lLenght += getSourceFileLength(f);
			}
		} else {
			lLenght = oSourceFile.length();
		}
		
		if (!oSourceFile.exists()) {
			WasdiLog.debugLog("ProviderAdapter.getSourceFileLength: FILE DOES NOT EXISTS");
		}
		WasdiLog.debugLog("ProviderAdapter.getSourceFileLength: Found length " + lLenght);

		return lLenght;
	}
	
	
	
	@Override
	protected void internalReadConfig() {
		try {
			m_sDefaultProtocol = m_oDataProviderConfig.defaultProtocol; 
		} catch (Exception e) {
			WasdiLog.errorLog("CreoDias2ProvierAdapter: Config reader is null");
		}
	}
	
	
	private String getAttribute(JSONArray aoAttributes, String sAttributeName) {
		for (Object oAtt : aoAttributes) {
			JSONObject oJsonAtt = (JSONObject) oAtt;
			if (oJsonAtt.get(SODATA_NAME).equals(sAttributeName))
				return oJsonAtt.optString(SODATA_VALUE);
		}
		return "";
	}
	
	
	private String getProductIdFromURL(String sFileUrl) {
		// url in input is something like: https://datahub.creodias.eu/odata/v1/Products(a6212de3-f2e4-58c2-840b-7f42c3c8c612)/$value
		return sFileUrl.replace(SODATA_FILE_URL_START, "").replace(SODATA_FILE_URL_END, "");
	}
	

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
		// main method. sFileUrl is the url received by the lined data provider. sSaveDirOnServer is the local folder. 
		// must return the valid file full path or "" if the download was not possible. 
		
		WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile. Execute download for file: " + sFileURL);
		
		
		String sResult = null;

		if (isFileProtocol(m_sDefaultProtocol)) {
			WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile: detected file protocol ");

			String sFilesystemPath = null;
			if (isFileProtocol(sFileURL)) {
				sFilesystemPath = removePrefixFile(sFileURL);
				WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile:  file system path from file protocol " + sFilesystemPath);
			} else if (isHttpsProtocol(sFileURL)) {
				sFilesystemPath = extractFilePathFromHttpsUrl(sFileURL);
				WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile:  file system path from http protocol " + sFilesystemPath);
			} else {
				WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile: unknown protocol " + sFileURL);
			}

			if (sFilesystemPath != null) {
				File oSourceFile = new File(sFilesystemPath);

				if (oSourceFile != null && oSourceFile.exists()) {
					sResult = copyFile("file:" + sFilesystemPath, sDownloadUser, sDownloadPassword, sSaveDirOnServer, oProcessWorkspace, iMaxRetry);
					WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile: destination file path " + sResult);

					return sResult;
				}
			}
			WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile: no operation was effective for file protocol");

		}
		

		if(isHttpsProtocol(sFileURL)) { 
			int iAttemptCount = 0;
			int iWaitDelta = 100;
			long lLowerWatingTime = 1L;
			long lUpperWatingTime = 10L;
			
			while (iAttemptCount < iMaxRetry) {
				String sAccessToken = getAuthenticationToken(sDownloadUser, sDownloadPassword);
				
				if (Utils.isNullOrEmpty(sAccessToken)) {
					WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile. Error retrieving the access token. Impossible to continue.");
					// TODO: better to return an empty string or null?
					return "";
				}
				
				WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile. Access token correctly received.");
				String sODataDownloadUrl = getODataDownloadUrl(sFileURL);
//				String sProductId = getProductIdFromURL(sODataDownloadUrl);
				// with the auth token, we can send the download request
				String sDownloadUrl = sODataDownloadUrl + SDOWNLOAD_URL_END + sAccessToken;
				
				WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile. Access token correctly received. Download url (withouth accesso token): " + sODataDownloadUrl + SDOWNLOAD_URL_END);
	
				
				// TODO: understand if I should also pass the name of the file - I think no. The method already parses the "File-disposition" attribute in the header. Should also work for Creodias2
				String sDownloadedFilePath = downloadViaHttp(sDownloadUrl, Collections.emptyMap(), sSaveDirOnServer);
				
				if(Utils.isNullOrEmpty(sDownloadedFilePath)) {
					// we will try again
					iAttemptCount++;
					long lRandomWaitSeconds = new SecureRandom().longs(lLowerWatingTime, lUpperWatingTime).findFirst().getAsLong();
					//prepare to wait longer next time
					lLowerWatingTime = lRandomWaitSeconds;
					lUpperWatingTime += iWaitDelta;
					WasdiLog.warnLog("CreoDias2ProviderAdapter.executeDownloadFile. Download failed. Trying again after waiting  " + lRandomWaitSeconds +" seconds.");
					TimeUnit.SECONDS.sleep(lRandomWaitSeconds);
				} else {
					// download completed
					WasdiLog.debugLog("CreoDias2ProviderAdapter.executeDownloadFile. Download completed: " + sDownloadedFilePath);
					return sDownloadedFilePath;
				}
			}
		}
		// TODO: better to return an empty string or null? 
		return "";
	}
	
	private String getODataDownloadUrl(String sFileURL) {
		if (Utils.isNullOrEmpty(sFileURL)) {
			WasdiLog.errorLog("CREODIASProviderAdapter.getODataDownloadUrl: Empty file URL");
			return "";
		}
		try {
			return sFileURL.split(ResponseTranslatorCreoDias2.SLINK_SEPARATOR_CREODIAS2)[ResponseTranslatorCreoDias2.IPOSITIONOF_LINK];
		} catch (Exception oE) {
			WasdiLog.errorLog("CREODIASProviderAdapter.getODataDownloadUrl: " + oE);
		}
		return "";
	}
	
	
	
	
	
	private String copyFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {

		String sResult = "";
		// Domain check
		if (Utils.isNullOrEmpty(sFileURL)) {
			WasdiLog.debugLog("CREODIASProviderAdapter.ExecuteDownloadFile: sFileURL is null");
			return "";
		}
		if (Utils.isNullOrEmpty(sSaveDirOnServer)) {
			WasdiLog.debugLog("CREODIASProviderAdapter.ExecuteDownloadFile: sSaveDirOnServer is null");
			return "";
		}
		
		WasdiLog.debugLog("CREODIASProviderAdapter.ExecuteDownloadFile: start");
		
		setProcessWorkspace(oProcessWorkspace);

		if (isFileProtocol(sFileURL)) {
			if (isZipFile(sFileURL)) {
				sResult = localFileCopy(sFileURL, sSaveDirOnServer, iMaxRetry);
			} else if (isSafeDirectory(sFileURL)) {
				String sourceFile = removePrefixFile(sFileURL);
				String destinationFile = getFileName(sFileURL);

				// set the destination folder
				if (sSaveDirOnServer.endsWith("/") == false) sSaveDirOnServer += "/";
				destinationFile = sSaveDirOnServer + destinationFile;

				destinationFile = addZipExtension(removeSafeTermination(destinationFile));

				downloadZipFile(sourceFile, destinationFile);
				
				sResult = destinationFile;
			}
		} 

		return sResult;
	}
	
	
	private void downloadZipFile(String sourceFile, String destinationFile) throws Exception {
        FileOutputStream fos = new FileOutputStream(destinationFile);
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        File fileToZip = new File(sourceFile);

        ZipFileUtils.zipFile(fileToZip, fileToZip.getName(), zipOut);
        zipOut.close();
        fos.close();
	}
	
	
	
	
	private String getAuthenticationToken(String sUsername, String sPassword) {
		String sPayload = "client_id=CLOUDFERRO_PUBLIC&password=" + sPassword + "&username=" + sUsername + "&grant_type=password";
		HttpCallResponse oResponse = null;
		try {
			oResponse = HttpUtils.httpPost(SAUTHENTICATION_URL, sPayload);
			if (oResponse == null ||  oResponse.getResponseCode() < 200 || oResponse.getResponseCode() > 299) {
				WasdiLog.debugLog("CreoDias2ProviderAdaper.getAuthenticationToken. Error code while trying to retrieve the auth token.");
				return "";
			} 
		} catch (Exception oEx) {
			WasdiLog.debugLog("CreoDias2ProviderAdaper.getAuthenticationToken. Error while trying to retrieve the authentication token." + oEx.getMessage());
			return "";
		}
		
		JSONObject oResponseBody = new JSONObject(oResponse.getResponseBody());
		String sAccessToken = oResponseBody.optString("access_token", "");
		
		if (Utils.isNullOrEmpty(sAccessToken)) {
			WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile. No access token found.");
			return "";
		}
		
		return sAccessToken;
	}

	@Override
	public String getFileName(String sFileURL) throws Exception {
		// extracts the file name from the URL
		WasdiLog.debugLog("CreoDias2ProviderAdaper.getFileName. Retrieving the product's file name from URL: " + sFileURL);
		
		if (Utils.isNullOrEmpty(sFileURL)) {
			WasdiLog.errorLog("CreoDias2ProviderAdaper.getFileName: sFileURL is null or Empty");
			return "";
		}

		String sResult = "";
		if (isHttpsProtocol(sFileURL)) {
			WasdiLog.debugLog("CreoDias2ProviderAdaper.getFileName. Retrieve file at local path: " + sFileURL);
			try {			
				String[] asTokens = sFileURL.split(ResponseTranslatorCreoDias2.SLINK_SEPARATOR_CREODIAS2);
				String sS3Path = asTokens[ResponseTranslatorCreoDias2.IPOSITIONOF_PRODUCTIDENTIFIER];
				String[] asPathTokens = sS3Path.split("/");
				return asPathTokens[asPathTokens.length-1];
			} catch (Exception oEx) {
				WasdiLog.errorLog("CreoDias2ProviderAdaper.getFileName: exception while trying to retrieve the file name." + oEx.getMessage());

			}
			/*
			try {
				String sProductId = getProductIdFromURL(sFileURL);
				String sUrl = SCREODIAS_BASE_URL + "$filter=Id eq '" + sProductId;
				HttpCallResponse sResponse = null;
				
				WasdiLog.debugLog("CreoDias2ProviderAdaper.getFileName. Retrieve file at http URL: " + sUrl);

				try {
					sResponse = HttpUtils.httpGet(sUrl);
					if (sResponse == null || sResponse.getResponseCode() < 200 || sResponse.getResponseCode() > 299 || Utils.isNullOrEmpty(sResponse.getResponseBody())) {
						WasdiLog.debugLog("CreoDias2ProviderAdaper.getFileName. Error retrieving the information about the product from the provider. " + sUrl);
						return "";
					}
				} catch (Exception oEx) {
					WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize. An exception occurred while retrieving the product information from the provider. " + oEx.getMessage());
					return "";
				}
				
				WasdiLog.debugLog("CreoDias2ProviderAdaper.getFileName. Response correctly received from the data provider.");

				
				JSONObject oJsonBody = new JSONObject(sResponse.getResponseBody());
				
				String sFileName = oJsonBody.optString(SODATA_NAME);
				
				if (Utils.isNullOrEmpty(sFileName)) 
					WasdiLog.debugLog("CreoDias2ProviderAdaper.getFileName. File name was not found under the key " + SODATA_NAME);
				
				sResult = ResponseTranslatorCreoDias2.removeExtensionFromProductTitle(sFileName);
				
				WasdiLog.debugLog("CreoDias2ProviderAdaper.getFileName. File name without extension: " + sResult);
			
			} catch (Exception oEx) {
				WasdiLog.debugLog("CreoDias2ProviderAdaper.getFileName. Exception during the retrieval of the file name from http url " + oEx.getMessage());
				return "";
			}
			*/
		} else if (isFileProtocol(sFileURL)) {
			
			WasdiLog.debugLog("CreoDias2ProviderAdaper.getFileName. Retrieve file at local path: " + sFileURL);

			String[] asParts = sFileURL.split("/");

			if (asParts != null && asParts.length > 1) {
				sResult = asParts[asParts.length-1];
			}
			
			WasdiLog.debugLog("CreoDias2ProviderAdaper.getFileName. File name without extension: " + sResult);
		}

		return sResult;
	}

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		// TODO Auto-generated method stub
		// returns the score auto-evaluated by the Provider Adapter to download sFileName of sPlatformType.
		boolean bOnCloud = isWorkspaceOnSameCloud();
		
		if (sPlatformType.equals(Platforms.SENTINEL1)) {
			String sType = WasdiFileUtils.getProductTypeSatelliteImageFileName(sFileName);
			
			if (sType.equals("GRD") || sType.equals("OCN")) {
				if (bOnCloud) return DataProviderScores.FILE_ACCESS.getValue();
				else return DataProviderScores.DOWNLOAD.getValue();
			}
			
			// TODO: SLC in europe are available. Look if we can get the bbox
			return DataProviderScores.LTA.getValue();
		}
		else if (sPlatformType.equals(Platforms.SENTINEL2)) {
			String sType = WasdiFileUtils.getProductTypeSatelliteImageFileName(sFileName);
			
			if (sType.equals("MSIL1C")) {
				if (bOnCloud) return DataProviderScores.FILE_ACCESS.getValue();
				else return DataProviderScores.DOWNLOAD.getValue();				
			}
			
			return DataProviderScores.LTA.getValue();
		}
		else if (sPlatformType.equals(Platforms.ENVISAT)) {
			if (sFileName.startsWith("ASA_")) {
				return -1;
			}
			if (bOnCloud) return DataProviderScores.FILE_ACCESS.getValue();
			else return DataProviderScores.DOWNLOAD.getValue();			
		}
		else if (sPlatformType.equals(Platforms.SENTINEL3) || sPlatformType.equals(Platforms.SENTINEL5P)
				|| sPlatformType.equals(Platforms.LANDSAT8)) {
			if (bOnCloud) return DataProviderScores.FILE_ACCESS.getValue();
			else return DataProviderScores.DOWNLOAD.getValue();
		}
		
		return 0;
	}
	
}
