package wasdi.dataproviders;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.viirs.QueryExecutorVIIRS;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class VIIRSProviderAdapter extends ProviderAdapter {
	
	private static final String s_sNewSubstring = ".part";
	private static final String s_sOldSubstring = "_part";
	
	private String m_sDataProviderVolumePath = "";
	
	public VIIRSProviderAdapter() {
		super();
	}


	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		long lSize = 0L;
		
		String sFileUrlForOnlineDataProvider = sFileURL.replace(s_sOldSubstring, s_sNewSubstring);
		
		WasdiLog.debugLog("VIIRSProviderAdapter.getDownloadFileSize: modified file url " + sFileUrlForOnlineDataProvider);
		
		List<String> asProductInfo = getProductInformation(sFileUrlForOnlineDataProvider);
		
		if (asProductInfo.isEmpty() || asProductInfo.stream().anyMatch(sInfo -> Utils.isNullOrEmpty(sInfo))) {
			WasdiLog.warnLog("VIIRSProviderAdapter.getDownloadFileSize: could not find relevant information about product from URL");
			return lSize;
		}
			
		String sProductDate  = asProductInfo.get(1);
		String sTileNumber = asProductInfo.get(2);
		String sFilePrefixOnS3 = asProductInfo.get(3);

		if (!isDateMoreThan30DaysOld(sProductDate)) {
			lSize = getDownloadFileSizeViaHttp(sFileUrlForOnlineDataProvider);
			
		} else {
			try {
				String sProductPathOnS3Volume = getFilePathOnS3Volume(sFilePrefixOnS3, sProductDate, sTileNumber);
				Path oPath = Paths.get(sProductPathOnS3Volume);
				lSize = Files.size(oPath);
			} catch (Exception oEx) {
	            WasdiLog.errorLog("VIIRSProviderAdapter.getDownloadFileSize: exception ", oEx);
	        }
		}
		return lSize;
	}

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
		// this is the example of a link that we receive: 
		// https://floodlight.ssec.wisc.edu/composite/RIVER-FLDglobal-composite1_20240416_000000.part003.tif

		WasdiLog.debugLog("VIIRSProviderAdapter.executeDownloadFile: url " + sFileURL + ", path to file on server " + sSaveDirOnServer);
				
		String sResult = "";
		
		try {
			String sFileUrlForOnlineDataProvider = sFileURL.replace(s_sOldSubstring, s_sNewSubstring);
			
			WasdiLog.debugLog("VIIRSProviderAdapter.executeDownloadFile: modified file url " + sFileUrlForOnlineDataProvider);
			
			File oSaveDir = new File(sSaveDirOnServer);
			
			boolean oDirCreated = oSaveDir.mkdirs();
			if (oDirCreated)
				WasdiLog.debugLog("VIIRSProviderAdapter.executeDownloadFile. Workspace directory has been crated");
			
			List<String> asProductInfo = getProductInformation(sFileUrlForOnlineDataProvider);
			
			if (asProductInfo.isEmpty() || asProductInfo.stream().anyMatch(sInfo -> Utils.isNullOrEmpty(sInfo))) {
				WasdiLog.warnLog("VIIRSProviderAdapter.executeDownloadFile: could not find relevant information about product from URL");
				return sResult;
			}
				
			String sWasdiFileName = asProductInfo.get(0).replace(s_sNewSubstring, s_sOldSubstring);
			String sProductDate  = asProductInfo.get(1);
			String sTileNumber = asProductInfo.get(2);
			String sFilePrefixOnS3 = asProductInfo.get(3);
	
			if (!isDateMoreThan30DaysOld(sProductDate)) {
				WasdiLog.debugLog("VIIRSProviderAdapter.executeDownloadFile: the requested product is less than one month old. Downloading from Floodlight website");
				sResult = executeDownloadFileFromFloodlight(sFileUrlForOnlineDataProvider, sSaveDirOnServer, iMaxRetry);
				
			} 
			
			// if the download from the online data provider does not succeed, we always try to download the product from the volume, as a backup solution
			if (Utils.isNullOrEmpty(sResult)) {
				WasdiLog.debugLog("VIIRSProviderAdapter.executeDownloadFile: downloading from S3 volume");
				
				String sProductPathOnS3Volume = getFilePathOnS3Volume(sFilePrefixOnS3, sProductDate, sTileNumber);
				
				if (!Utils.isNullOrEmpty(sProductPathOnS3Volume)) {
					File oSourceProduct = new File(sProductPathOnS3Volume);
					File oDestinationProduct = new File(sSaveDirOnServer + "/" + sWasdiFileName);
					FileUtils.copyFile(oSourceProduct, oDestinationProduct);
	
					sResult =  oDestinationProduct.getAbsolutePath();
					WasdiLog.debugLog("VIIRSProviderAdapter.executeDownloadFile: product copied from S3 to " + sResult);
					
				} else {
					WasdiLog.warnLog("VIIRSProviderAdapter.executeDownloadFile: product not found on S3 volume");
				}			
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("VIIRSProviderAdapter.executeDownloadFile: error downloading file", oEx);
		}
		
		return sResult;
	}
	
	/**
	 * Return a list of three elements representing the information related to the product to download
	 * @param sFileURL the URL of the file to download
	 * @return a list of three elements representing the information related to the product to download: 
	 * (i) the name that the downloaded file should have on WASDI
	 * (ii) the date of the product in the format YYYYMMDD
	 * (iii) the tile number as a fixed-length string of three digits (e.g. 001)
	 * (iv) prefix file on S3 bucket
	 */
	private static List<String> getProductInformation(String sFileURL) {
		String sWasdiFileName = "";
		String sProductDate  = "";
		String sTileNumber = "";
		String sFilePrefixOnS3 = "";
		
		List<String> asResults = new ArrayList<>();
		
		try {
			String sURLPrefix = QueryExecutorVIIRS.s_sLINK_PREFIX;
			if (!sURLPrefix.endsWith("/"))
				sURLPrefix += "/";
			
			sWasdiFileName = sFileURL.replace(sURLPrefix, "");
			
			sProductDate = sWasdiFileName.split("_")[1];
			sTileNumber = sWasdiFileName.split("\\.")[1].replace("part", "");	
			
			if (sFileURL.contains("-composite_"))
				sFilePrefixOnS3 = "VFM_5day_GLB";
			else if (sFileURL.contains("-composite1_")) 
				sFilePrefixOnS3 = "VFM_1day_GLB";
			else
				WasdiLog.warnLog("VIIRSProviderAdapter.getProductInformation: can not determine time span of pruduct (1 or 5 days composite) from file URL " + sFileURL);
			
			asResults.add(sWasdiFileName);
			asResults.add(sProductDate);
			asResults.add(sTileNumber);
			asResults.add(sFilePrefixOnS3);
		} catch (Exception oEx) {
			WasdiLog.errorLog("VIIRSProviderAdapter.getProductInformation: exception retrieving the product date and tile number from the url", oEx);
		}
		
		return asResults;
	}
	
	/**
	 * Downloads the product from the data provider online
	 * @param sFileURL URL of the data provider 
	 * @param sSaveDirOnServer the directory where the downloaded file should be saved
	 * @param iMaxRetry maximum number of times the download should be attempted
	 * @return the path to the downloaded product
	 */
	private String executeDownloadFileFromFloodlight(String sFileURL, String sSaveDirOnServer, int iMaxRetry) {
		String sResult = "";
		int iAttemp = 0;
		
		while (Utils.isNullOrEmpty(sResult) && iAttemp < iMaxRetry) {
			
			WasdiLog.debugLog("VIIRSProviderAdapter.executeDownloadFileFromFloodlight: attemp #" + iAttemp);
			
			try {
				sResult = downloadViaHttp(sFileURL, "", "", sSaveDirOnServer);
				
				File oOriginalFile = new File(sResult);
				File oRenamedFile = new File(sResult.replace(s_sNewSubstring, s_sOldSubstring));
				boolean bIsFileRenamed = oOriginalFile.renameTo(oRenamedFile);
				
				if (!bIsFileRenamed)
					WasdiLog.debugLog("VIIRSProviderAdapter.executeDownloadFileFromFloodlight. File was not renamed.");
				
				sResult = sResult.replace(s_sNewSubstring, s_sOldSubstring);
				
			}
			catch (Exception oEx) {
				WasdiLog.errorLog("VIIRSProviderAdapter.executeDownloadFileFromFloodlight: exception in download via http call: ", oEx);
			}
			
			iAttemp ++;
		}
		
		return sResult;
	}
	
	
	/**
	 * Get the path of a VIIRS product on the S3 volume
	 * @param sS3FileNamePrefix prefix of the VIIRS product name on the S3 volume, it can be "VFM_5day_GLB" or "VFM_1day_GLB"
	 * @param sProductDate the date the product refers to, in the format YYYYMMDD
	 * @param sTileNumber  the tile number as a fixed-length string of three digits (e.g. 001)
	 * @return the path of the VIIRS product on the S3 volume
	 */
	private String getFilePathOnS3Volume(String sS3FileNamePrefix, String sProductDate, String sTileNumber) {
		String sResult = "";
		
		String sS3VolumePath = m_sDataProviderVolumePath;
		
		if (!sS3VolumePath.endsWith("/"))
			sS3VolumePath += "/";
		
		try {
			String sYear = sProductDate.substring(0, 4);
			String sMonth = sProductDate.substring(4, 6);
			String sDay = sProductDate.substring(6, 8);
			
			WasdiLog.debugLog("VIIRSProviderAdapter.getFilePathOnS3Volume: product refers to date " + sYear + "/" + sMonth + "/" + sDay + " and tile number " + sTileNumber);

			String sFilePrefix = sS3FileNamePrefix;
			if (!sS3FileNamePrefix.endsWith("/"))
				sFilePrefix += "/";
			
			sS3VolumePath += sFilePrefix + "/TIF/" + sYear + "/" + sMonth + "/" + sDay;
			File sProductFolder = new File(sS3VolumePath);
			final String sTileSubstring = "-GLB" + sTileNumber + "_";
			
			WasdiLog.debugLog("VIIRSProviderAdapter.getFilePathOnS3Volume: looking for the product in the folder " + sProductFolder);
			
			if (sProductFolder.exists() && sProductFolder.isDirectory()) {
				// we can proceed to look for the product
				List<String> asProducts = Arrays.asList(sProductFolder.listFiles()).stream()
						.map(File::getName)
						.filter(sFileName -> sFileName.contains(sTileSubstring))
						.collect(Collectors.toList());
				
				if (asProducts.isEmpty()) {
					WasdiLog.warnLog("VIIRSProviderAdapter.getFilePathOnS3Volume: no products found with substrings \"" + sTileSubstring + "\"");
				} else {
					// in case of more than one result, it will return the first one
					if (asProducts.size() > 1) {
						WasdiLog.warnLog("VIIRSProviderAdapter.getFilePathOnS3Volume: " + asProducts.size()+ " products found. Only one of them will be returned");
					}
					
					sResult = sS3VolumePath + "/" + asProducts.get(0);
				}
			}  else {
				WasdiLog.warnLog("VIIRSProviderAdapter.getFilePathOnS3Volume: folder " + sProductFolder + " does not exist on S3 volume");
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("VIIRSProviderAdapter.getFilePathOnS3Volume: error while trying to retrieve file from S3 volume", oEx);
		}
		
		return sResult;

	}
	
	
	/**
	 * Determines if a string in the format YYYYMMDD is earlier than 30 days from the current date
	 * @param sDateString the date string in the format YYYYMMDD
	 * @return return true if the given date is more than 30 days earlier before the current date
	 */
	private boolean isDateMoreThan30DaysOld(String sDateString) {
		SimpleDateFormat sSimpleDateFormat = new SimpleDateFormat("yyyyMMdd");
		try {
			Date sFormattedDate = sSimpleDateFormat.parse(sDateString);
			Calendar oCalendarGivenDate = Calendar.getInstance();
			oCalendarGivenDate.setTime(sFormattedDate);
			
			Calendar oCalendarCurrentDate = Calendar.getInstance();
			// subtracting 30 days from the current date
			oCalendarCurrentDate.add(Calendar.DAY_OF_MONTH, -30);
			return oCalendarGivenDate.before(oCalendarCurrentDate);
		} catch (ParseException oEx) {
			WasdiLog.errorLog("VIIRSProviderAdapter.isDateMoreThan30DaysOld: exception when parsing date: ", oEx);
			return false;
		}
		
	}

	@Override
	public String getFileName(String sFileURL) throws Exception {
		if (Utils.isNullOrEmpty(sFileURL)) 
			return "";
		
		WasdiLog.debugLog("VIIRSProviderAdapter.getFileName: file url " + sFileURL);

		
		String sFileName = "";
		
		String [] asParts = sFileURL.split("/");
		
		if (asParts != null) {
			sFileName = asParts[asParts.length-1];
			
			sFileName = sFileName.replace(s_sNewSubstring, s_sOldSubstring);
		}
		
		WasdiLog.debugLog("VIIRSProviderAdapter.getFileName: file name " + sFileName);
		
		return sFileName;
	}

	@Override
	protected void internalReadConfig() {
		String sAdapterConfigPath = "";
		try {
			sAdapterConfigPath = m_oDataProviderConfig.adapterConfig;
			JSONObject oAppConf = JsonUtils.loadJsonFromFile(sAdapterConfigPath);
			m_sDataProviderVolumePath = oAppConf.getString("dataProviderVolumePath");
		} catch(Exception oEx) {
			WasdiLog.errorLog("VIIRSProviderAdapter.internalReadConfig: exception reading parser config file " + sAdapterConfigPath);
		}
		
	}

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		
		if (sPlatformType.equals(Platforms.VIIRS) && sFileName.toUpperCase().startsWith("RIVER-FLD")) {
			return DataProviderScores.DOWNLOAD.getValue();
		}
		
		return 0;
	}
		
}
