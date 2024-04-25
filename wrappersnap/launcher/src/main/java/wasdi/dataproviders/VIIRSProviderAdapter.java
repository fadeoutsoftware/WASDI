package wasdi.dataproviders;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.viirs.QueryExecutorVIIRS;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class VIIRSProviderAdapter extends ProviderAdapter {
	
	private static final String s_sNewSubstring = ".part";
	private static final String s_sOldSubstring = "_part";
	
	public VIIRSProviderAdapter() {
		super();
		m_sDataProviderCode = "VIIRS";
	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		String sWasdiFileName = "";
		String sProductDate  = "";
		
		try {
	
			String sURLPrefix = QueryExecutorVIIRS.s_sLINK_PREFIX;
			if (!sURLPrefix.endsWith("/"))
				sURLPrefix += "/";
			
			sWasdiFileName = sFileURL.replace(sURLPrefix, "");
			
			sProductDate = sWasdiFileName.split("_")[1];
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("VIIRSProviderAdapter.getDownloadFileSize: exception retrieving the product date and tile number from the url", oEx);
		}
		
		if (isDateMoreThan30DaysOld(sProductDate)) {
			return 0L;
		}
	
		return getDownloadFileSizeViaHttp(sFileURL.replace(s_sOldSubstring, s_sNewSubstring));
	}

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {

		WasdiLog.debugLog("VIIRSProviderAdapter.executeDownloadFile: try to get " + sFileURL);
				
		String sResult = "";
				
		// this is the example of a link that we receive: 
		// https://floodlight.ssec.wisc.edu/composite/RIVER-FLDglobal-composite1_20240416_000000.part003.tif
			
		String sWasdiFileName = "";
		String sProductDate  = "";
		String sTileNumber = "";
		
		try {
	
			String sURLPrefix = QueryExecutorVIIRS.s_sLINK_PREFIX;
			if (!sURLPrefix.endsWith("/"))
				sURLPrefix += "/";
			
			sWasdiFileName = sFileURL.replace(sURLPrefix, "");
			
			sProductDate = sWasdiFileName.split("_")[1];
			sTileNumber = sWasdiFileName.split("\\.")[1].replace("part", "");	
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("VIIRSProviderAdapter.executeDownloadFile: exception retrieving the product date and tile number from the url", oEx);
		}
		
		if (Utils.isNullOrEmpty(sWasdiFileName) || Utils.isNullOrEmpty(sProductDate) || Utils.isNullOrEmpty(sTileNumber)) {
			WasdiLog.warnLog("VIIRSProviderAdapter.executeDownloadFile: could not find product date and tile number from url " + sFileURL);
			return sResult;
		}
		
		if (!isDateMoreThan30DaysOld(sProductDate)) {
			WasdiLog.debugLog("VIIRSProviderAdapter.executeDownloadFile: the requested product is less than one month old. Downloading from Floodlight website");
			sResult = executeDownloadFileFromFloodlight(sFileURL, sSaveDirOnServer, iMaxRetry);
			
		} else {
			WasdiLog.debugLog("VIIRSProviderAdapter.executeDownloadFile: the requested product is more than one month old. Downloading from S3 volume");

			if (sFileURL.contains("-composite_")) {
				// folder for 5-day composite
				WasdiLog.debugLog("VIIRSProviderAdapter.executeDownloadFile: product is a 5-day composite");
				sResult = copyFileFromS3Bucket("VFM_5day_GLB/TIF/", sProductDate, sTileNumber, sSaveDirOnServer, sWasdiFileName);
			
			} else if (sFileURL.contains("-composite1_")) {
				// folder for 1-day composite
				WasdiLog.debugLog("VIIRSProviderAdapter.executeDownloadFile: product is a 5-day composite");
				sResult = copyFileFromS3Bucket("VFM_1day_GLB/TIF/", sProductDate, sTileNumber, sSaveDirOnServer, sWasdiFileName);
				
			} else {
				WasdiLog.warnLog("VIIRSProviderAdapter.executeDownloadFile: can not determine time span of pruduct (1 or 5 days composite) from file URL " + sFileURL);
			}

			
		}
		
		return sResult;
	}
	
	private String executeDownloadFileFromFloodlight(String sFileURL, String sSaveDirOnServer, int iMaxRetry) {
		String sResult = "";
		int iAttemp = 0;
		
		while (Utils.isNullOrEmpty(sResult) && iAttemp < iMaxRetry) {
			
			WasdiLog.debugLog("VIIRSProviderAdapter.executeDownloadFileFromFloodlight: attemp #" + iAttemp);
			
			try {
				sResult = downloadViaHttp(sFileURL.replace(s_sOldSubstring, s_sNewSubstring), "", "", sSaveDirOnServer);
				
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
	
	private String copyFileFromS3Bucket(String sS3FileNamePrefix, String sProductDate, String sTileNumber, String sSaveDirOnServer, String sTargetProductName) {
		
		String sResult = "";
		
		String sS3VolumePath = "/mnt/wasdi/data-provider-volumes/noaa-jpss/";
		
		if (!sS3VolumePath.endsWith("/"))
			sS3VolumePath += "/";
		
		try {
			String sYear = sProductDate.substring(0, 4);
			String sMonth = sProductDate.substring(4, 6);
			String sDay = sProductDate.substring(6, 8);
			
			WasdiLog.debugLog("VIIRSProviderAdapter.copyFileFromS3Bucket: product refers to date " + sYear + "/" + sMonth + "/" + sDay + " and tile number " + sTileNumber);

			String sFilePrefix = sS3FileNamePrefix;
			if (!sS3FileNamePrefix.endsWith("/"))
				sFilePrefix += "/";
			
			sS3VolumePath += sFilePrefix + sYear + "/" + sMonth + "/" + sDay;
			File sProductFolder = new File(sS3VolumePath);
			final String sDateSubstring = "s" + sProductDate;
			final String sTileSubstring = "-GLB" + sTileNumber;
			
			WasdiLog.debugLog("VIIRSProviderAdapter.copyFileFromS3Bucket: looking for the product in the folder " + sProductFolder);
			
			if (sProductFolder.exists() && sProductFolder.isDirectory()) {
				// we can proceed to look for the product
				List<String> asProducts = Arrays.asList(sProductFolder.listFiles()).stream()
						.map(File::getName)
						.filter(sFileName -> sFileName.contains(sDateSubstring) && sFileName.contains(sTileSubstring))
						.collect(Collectors.toList());
				if (asProducts.size() > 1) {
					// what to do in this case?
					WasdiLog.warnLog("VIIRSProviderAdapter.copyFileFromS3Bucket: more than one products "
							+ "found with substrings \"" + sDateSubstring + "\" and \"" + sTileSubstring + "\"");
				} else {
					String sFileNameOnS3 = asProducts.get(0);
					File oSourceProduct = new File(sS3VolumePath + "/" + sFileNameOnS3);
					File oDestinationProduct = new File(sSaveDirOnServer + "/" + sTargetProductName);
					FileUtils.copyFile(oSourceProduct, oDestinationProduct);
	
					sResult =  oDestinationProduct.getAbsolutePath();
					WasdiLog.debugLog("VIIRSProviderAdapter.copyFileFromS3Bucket: product copied from S3 to " + sResult);

				}
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("VIIRSProviderAdapter.copyFileFromS3Bucket: error while trying to retrieve and copy the file from S3 ", oEx);
		}
		
		return sResult;
		
	}
	
	
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
		if (Utils.isNullOrEmpty(sFileURL)) return "";
		
		String sFileName = "";
		
		String [] asParts = sFileURL.split("/");
		
		if (asParts != null) {
			sFileName = asParts[asParts.length-1];
			
			sFileName = sFileName.replace(s_sNewSubstring, s_sOldSubstring);
		}
		
		return sFileName;
	}

	@Override
	protected void internalReadConfig() {
		
	}

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		
		if (sPlatformType.equals(Platforms.VIIRS)) {
			return DataProviderScores.DOWNLOAD.getValue();
		}
		
		return 0;
	}

}
