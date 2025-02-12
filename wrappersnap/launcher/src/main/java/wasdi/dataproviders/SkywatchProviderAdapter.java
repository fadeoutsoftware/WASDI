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

	private static final String s_sSearchIdParam = "&search_id=";
	private static final String s_sProductIdParam = "&product_id=";
	private static final String s_sProductName = "product_name";
	/**
	 * Basic constructor
	 */
	public SkywatchProviderAdapter() {
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

		if (!sFileURL.contains(s_sSearchIdParam) || !sFileURL.contains(s_sProductIdParam)) {
			return null;
		}

		Map<String, String> aoWasdiPayload = extractWasdiPayloadFromUrl(sFileURL);
		if (aoWasdiPayload.isEmpty()) {
			return null;
		}

		String sProductName = aoWasdiPayload.get(s_sProductName);
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
				asPayload.put(s_sProductName, sProductName);
			}

			if (sDecodedUrl.contains(s_sSearchIdParam)) {
				String sSearchId = sDecodedUrl.substring(sDecodedUrl.indexOf(s_sSearchIdParam) + 11, sDecodedUrl.indexOf("&", sDecodedUrl.indexOf("search_id=")));
				asPayload.put("search_id", sSearchId);
			}

			if (sDecodedUrl.contains(s_sProductIdParam)) {
				String sId = sDecodedUrl.substring(sDecodedUrl.indexOf(s_sProductIdParam) + 12);
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
		if (aoWasdiPayload.isEmpty()) {
			return null;
		}

		String sProductName = aoWasdiPayload.get(s_sProductName);

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
