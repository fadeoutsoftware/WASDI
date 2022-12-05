package wasdi.dataproviders;

import static wasdi.shared.queryexecutors.skywatch.SkywatchHttpUtils.*;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.skywatch.models.CreatePipelineRequest;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class SkywatchProviderAdapter extends ProviderAdapter {

	/**
	 * Basic constructor
	 */
	public SkywatchProviderAdapter() {
		m_sDataProviderCode = "SKYWATCH";
	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		WasdiLog.debugLog("SkywatchProviderAdapter.getDownloadFileSize: try to get " + sFileURL);

		if (Utils.isNullOrEmpty(m_oDataProviderConfig.link)) {
			WasdiLog.errorLog("SkywatchProviderAdapter.getDownloadFileSize: provider URL is null or Empty");
			return 0L;
		}

		if (Utils.isNullOrEmpty(sFileURL)) {
			WasdiLog.errorLog("SkywatchProviderAdapter.getDownloadFileSize: sFileURL is null or Empty");
			return 0L;
		}

		CreatePipelineRequest oCreatePipelineRequest = convertUrlToCreatePipelineRequest(sFileURL);


		int iMaxRetry = 5;

		if (oCreatePipelineRequest == null) {
			WasdiLog.errorLog("SkywatchProviderAdapter.executeDownloadFile: could not convert sFileURL");
			return 0L;
		}

		return getFileSize(oCreatePipelineRequest, iMaxRetry);
	}

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {

		WasdiLog.debugLog("SkywatchProviderAdapter.executeDownloadFile: try to get " + sFileURL);

		if (Utils.isNullOrEmpty(m_oDataProviderConfig.link)) {
			WasdiLog.errorLog("SkywatchProviderAdapter.executeDownloadFile: provider URL is null or Empty");
			return null;
		}

		if (Utils.isNullOrEmpty(sFileURL)) {
			WasdiLog.errorLog("SkywatchProviderAdapter.executeDownloadFile: sFileURL is null or Empty");
			return null;
		}

		CreatePipelineRequest oCreatePipelineRequest = convertUrlToCreatePipelineRequest(sFileURL);

		if (oCreatePipelineRequest == null) {
			WasdiLog.errorLog("SkywatchProviderAdapter.executeDownloadFile: could not convert sFileURL");
			return null;
		}

		String sDownloadProductResult = downloadProduct(oCreatePipelineRequest, sSaveDirOnServer, iMaxRetry);

		return sDownloadProductResult;
	}

	private static CreatePipelineRequest convertUrlToCreatePipelineRequest(String sFileURL) {

		if (!sFileURL.contains("&search_id=") || !sFileURL.contains("&product_id=")) {
			return null;
		}

		Map<String, String> aoWasdiPayload = extractWasdiPayloadFromUrl(sFileURL);
		if (aoWasdiPayload == null) {
			return null;
		}

		String sProductName = aoWasdiPayload.get("product_name");
		String sSearchId = aoWasdiPayload.get("search_id");
		String sProductId = aoWasdiPayload.get("product_id");

		CreatePipelineRequest oPipelineRequest = new CreatePipelineRequest();
		oPipelineRequest.setName(sProductName);
		oPipelineRequest.setSearch_id(sSearchId);
		oPipelineRequest.setSearch_results(Arrays.asList(sProductId));

		return oPipelineRequest;
	}

	private static Map<String, String> extractWasdiPayloadFromUrl(String sUrl) {
		String sDecodedUrl = deodeUrl(sUrl);

		Map<String, String> asPayload = new HashMap<>();

		if (sDecodedUrl != null) {
			if (sDecodedUrl.contains("?product_name=")) {
				String sProductName = sDecodedUrl.substring(sDecodedUrl.indexOf("?product_name=") + 14, sDecodedUrl.indexOf("&", sDecodedUrl.indexOf("product_name=")));
				asPayload.put("product_name", sProductName);
			}

			if (sDecodedUrl.contains("&search_id=")) {
				String sSearchId = sDecodedUrl.substring(sDecodedUrl.indexOf("&search_id=") + 11, sDecodedUrl.indexOf("&", sDecodedUrl.indexOf("search_id=")));
				asPayload.put("search_id", sSearchId);
			}

			if (sDecodedUrl.contains("&product_id=")) {
				String sId = sDecodedUrl.substring(sDecodedUrl.indexOf("&product_id=") + 12);
				asPayload.put("product_id", sId);
			}
		}

		return asPayload;
	}

	private static String deodeUrl(String sUrlEncoded) {
		try {
			return URLDecoder.decode(sUrlEncoded, java.nio.charset.StandardCharsets.UTF_8.toString());
		} catch (UnsupportedEncodingException oE) {
			WasdiLog.debugLog("Wasdi.deodeUrl: could not decode URL due to " + oE + ".");
		}

		return sUrlEncoded;
	}

	@Override
	public String getFileName(String sFileURL) throws Exception {
		if (Utils.isNullOrEmpty(sFileURL)) return "";

		Map<String, String> aoWasdiPayload = extractWasdiPayloadFromUrl(sFileURL);
		if (aoWasdiPayload == null) {
			return null;
		}

		String sProductName = aoWasdiPayload.get("product_name");

		String sFileName = sProductName + ".tif";

		return sFileName;
	}


	@Override
	protected void internalReadConfig() {
	}

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		if (sPlatformType.equals(Platforms.EARTHCACHE)) {
			return DataProviderScores.SLOW_DOWNLOAD.getValue();
		}

		return 0;
	}

}
