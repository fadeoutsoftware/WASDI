package wasdi.shared.queryexecutors.cm2;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class CMHttpUtils {

	public static final String s_sCmemsCasUrl = "https://cmems-cas.cls.fr/cas/login";

	private static final String DIV_START_CONTENT = "<div id=\"content\">";
	private static final String DIV_END = "</div>";

	private CMHttpUtils() {
	}

	public static String getProductSize(String sService, String sProduct, String sQuery, String sUrl, String sUsername,
			String sPassword) {

		String sUrlGetSize = prepareUrlGetSize(sUrl, sService, sProduct, sQuery);

		Map<String, String> asCookies = acquireCookies(sUrlGetSize, sUsername, sPassword);

		if (asCookies != null) {
			String sResponseGetSize = callGetAndObtainResponse(sUrlGetSize, asCookies);

			if (sResponseGetSize != null) {
				if (sResponseGetSize.contains("requestSize")) {
					if (sResponseGetSize.contains("size") && sResponseGetSize.contains("unit")) {
						String sSize = parseValue(sResponseGetSize, "size");

						return sSize;
					}
				} else if (sResponseGetSize.contains(DIV_START_CONTENT)) {
					int iStartIndex = sResponseGetSize.indexOf(DIV_START_CONTENT) + DIV_START_CONTENT.length();
					int iEndIndex = sResponseGetSize.indexOf(DIV_END, sResponseGetSize.indexOf(DIV_START_CONTENT));
					String sErrorMessage = sResponseGetSize.substring(iStartIndex, iEndIndex);

					return "Error: " + sErrorMessage.replaceAll("\\n+", "").replaceAll("\\t+", "").trim();
				}
			}

		}

		return "0";
	}

	public static String downloadProduct(String sService, String sProduct, String sQuery, String sUrl, String sUsername,
			String sPassword, String sSaveDirOnServer) {
		String sUrlGetSize = prepareUrlGetSize(sUrl, sService, sProduct, sQuery);
		String sUrlProductDownload = prepareUrlProductDownload(sUrl, sService, sProduct, sQuery);
		String sUrlGetReqStatus = prepareUrlGetRequestStatus(sUrl, sService, sProduct, sQuery);

		Map<String, String> asCookies = acquireCookies(sUrlGetSize, sUsername, sPassword);

		if (!asCookies.isEmpty()) {
			String sRequestId = null;

			String sResponseProductDownload = callGetAndObtainResponse(sUrlProductDownload, asCookies);

			if (sResponseProductDownload == null) {
				WasdiLog.errorLog("CMHttpUtils.downloadProduct: callGetAndObtainResponse returned null URL " + sUrlProductDownload);
				return null;
			}

			if (!sResponseProductDownload.contains("statusModeResponse")) {
				WasdiLog.errorLog("CMHttpUtils.downloadProduct: impossible to find statusModeResponse " + sUrlProductDownload);
				return null;
			}

			if (!sResponseProductDownload.contains("requestId")) {
				WasdiLog.errorLog("CMHttpUtils.downloadProduct: impossible to find requestId URL " + sUrlProductDownload);
				return null;
			}

			sRequestId = parseValue(sResponseProductDownload, "requestId");

			if (sRequestId == null) {
				WasdiLog.errorLog("CMHttpUtils.downloadProduct: Impossible to parse Request Id URL " + sUrlProductDownload);
				return null;
			}
			
			WasdiLog.debugLog("CMHttpUtils.downloadProduct: got requestId: " + sRequestId);

			String sRemoteUri = null;

			while (true) {
				String sResponseGetReqStatus = callGetAndObtainResponse(sUrlGetReqStatus + sRequestId, asCookies);

				if (sResponseGetReqStatus == null) {
					WasdiLog.errorLog("CMHttpUtils.downloadProduct: impossible to get Request status URL " + sUrlGetReqStatus);
					return null;
				}
				if (!sResponseGetReqStatus.contains("statusModeResponse")) {
					WasdiLog.errorLog("CMHttpUtils.downloadProduct: impossible to get statusModeResponse Request status URL " + sUrlGetReqStatus);
					return null;
				}

				if (!sResponseGetReqStatus.contains("msg")) {
					WasdiLog.errorLog("CMHttpUtils.downloadProduct: impossible to get msg URL " + sUrlGetReqStatus);
					return null;
				}

				if (!sResponseGetReqStatus.contains("status" + "=")) {
					WasdiLog.errorLog("CMHttpUtils.downloadProduct: error finding status URL " + sUrlGetReqStatus);
					return null;
				}
				
				String sMsg = parseValue(sResponseGetReqStatus, "msg");
				String sStatus = parseValue(sResponseGetReqStatus, "status");

				if (sMsg.equalsIgnoreCase("request in progress") || sStatus.equals("0")) {

					try {
						Thread.sleep(1_000);
					} catch (InterruptedException oInterruptedException) {
						Thread.currentThread().interrupt();
						WasdiLog.errorLog("CMHttpUtils.downloadProduct. Current thread was interrupted: ", oInterruptedException);
					}

					continue;
				}

				if (!sResponseGetReqStatus.contains("remoteUri")) {
					WasdiLog.errorLog("CMHttpUtils.downloadProduct: remote Uri not found URL " + sUrlGetReqStatus);
					return null;
				}

				WasdiLog.debugLog("CMHttpUtils.downloadProduct: found a good response (in theory) extract remoteUri: " + sResponseGetReqStatus);
				sRemoteUri = parseValue(sResponseGetReqStatus, "remoteUri");

				break;
			}

			if (sRemoteUri == null) {
				WasdiLog.errorLog("CMHttpUtils.downloadProduct: impossible to parse remote Uri " + sUrlGetReqStatus);
				return null;
			}

			WasdiLog.debugLog("CMHttpUtils.downloadProduct: got sRemoteUri: " + sRemoteUri);

			String sFileName = sRemoteUri.substring(sRemoteUri.lastIndexOf("/") + 1);
			
			if (Utils.isNullOrEmpty(sFileName)) {
				return null;
			}

			String sDownloadPath = sSaveDirOnServer;
			if (!(sDownloadPath.endsWith("\\") || sDownloadPath.endsWith("/")
					|| sDownloadPath.endsWith(File.separator)))
				sDownloadPath += File.separator;

			String sOutputFilePath = sDownloadPath + sFileName;

			String sJsessionid = asCookies.get("JSESSIONID");

			Map<String, String> asHeaders = new HashMap<>();
			asHeaders.put("Cookie", "JSESSIONID=" + sJsessionid);

			String sActualOutputFilePath = HttpUtils.downloadFile(sRemoteUri, asHeaders, sOutputFilePath);

			if (!sOutputFilePath.equalsIgnoreCase(sActualOutputFilePath)) {
				WasdiLog.errorLog("CMHttpUtils.downloadProduct: impossible to download remote Uri " + sRemoteUri);
				return null;
			}

			return sActualOutputFilePath;
		} else {
			WasdiLog.errorLog("CMHttpUtils.downloadProduct: Impossible to get Cookies for authentication, return null");
		}

		return null;
	}

	public static String prepareUrlGetSize(String sUrl, String sService, String sProduct, String sQuery) {
		return sUrl + "?action=" + "getsize" + "&service=" + sService + "&product=" + sProduct + sQuery;
	}

	private static String prepareUrlProductDownload(String sUrl, String sService, String sProduct, String sQuery) {
		return sUrl + "?action=" + "productdownload" + "&service=" + sService + "&product=" + sProduct + sQuery
				+ "&output=netcdf&mode=status";
	}

	private static String prepareUrlGetRequestStatus(String sUrl, String sService, String sProduct, String sQuery) {
		return sUrl + "?action=" + "getreqstatus" + "&service=" + sService + "&product=" + sProduct + "&requestid=";
	}

	private static Map<String, String> acquireCookies(String sUrl, String sUsername, String sPassword) {
		Map<String, String> asParams = new HashMap<>();
		asParams.put("service", sUrl);

		CookieManager oCookieManager;
		if (CookieHandler.getDefault() == null) {
			oCookieManager = new CookieManager();
			CookieHandler.setDefault(oCookieManager);
		} else {
			oCookieManager = ((CookieManager) CookieHandler.getDefault());
		}

		Map<String, String> asCookies = new HashMap<>();

		List<HttpCookie> aoCookiesFromTheManager = oCookieManager.getCookieStore().getCookies();

		for (HttpCookie oCookie : aoCookiesFromTheManager) {
			asCookies.put(oCookie.getName(), oCookie.getValue());
		}

		if (asCookies.containsKey("JSESSIONID") && asCookies.containsKey("CASTGC")) {
			return asCookies;
		}

		String sResponse = callLoginGetAndObtainResponse(s_sCmemsCasUrl, asParams);

		Map<String, String> aoHiddenElementsFromHtml = extractHiddenElementsFromHtml(sResponse);

		String sJsessionidFromHtml = extractJsessionidFromHtml(sResponse);

		Map<String, String> asPayload = preparePayloadForLoginPost(aoHiddenElementsFromHtml, sUsername, sPassword);

		asCookies = callLoginPostAndObtainCookies(s_sCmemsCasUrl, asPayload, asParams, sJsessionidFromHtml);

		return asCookies;
	}

	private static String callLoginGetAndObtainResponse(String sCmemsCasUrl, Map<String, String> asParams) {
		String sResponse = null;

		try {
			if (asParams != null) {
				String sQuery = getQuery(asParams);
				sCmemsCasUrl = sCmemsCasUrl + "?" + sQuery;
			}

			URL oCmemsCasUrl = new URL(sCmemsCasUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oCmemsCasUrl.openConnection();

			oConnection.setConnectTimeout(60 * 60 * 1_000);
			oConnection.setReadTimeout(60 * 60 * 1_000);

			oConnection.setDoOutput(true);
			oConnection.setRequestMethod("GET");

			int iResponseCode = oConnection.getResponseCode();

			if (iResponseCode == HttpURLConnection.HTTP_OK) {
				sResponse = HttpUtils.readHttpResponse(oConnection);
			}

			oConnection.disconnect();
		} catch (Exception oException) {
			WasdiLog.errorLog("CMHttpUtils.callLoginGetAndObtainResponse: error", oException);
		}

		return sResponse;
	}

	private static Map<String, String> callLoginPostAndObtainCookies(String sCmemsCasUrl, Map<String, String> asPayload,
			Map<String, String> asParams, String sJsessionid) {
		Map<String, String> asCookies = new HashMap<>();

		try {
			if (asParams != null) {
				String sQuery = getQuery(asParams);
				sCmemsCasUrl = sCmemsCasUrl + "?" + sQuery;
			}

			if (asPayload != null) {
				String sQuery = getQuery(asPayload);
				sCmemsCasUrl = sCmemsCasUrl + "&" + sQuery;
			}

			CookieManager oCookieManager;
			if (CookieHandler.getDefault() == null) {
				oCookieManager = new CookieManager();
				CookieHandler.setDefault(oCookieManager);
			} else {
				oCookieManager = ((CookieManager) CookieHandler.getDefault());
			}

			URL oCmemsCasUrl = new URL(sCmemsCasUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oCmemsCasUrl.openConnection();
			oConnection.setRequestProperty("Accept", "*/*");
			oConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			oConnection.setRequestProperty("Connection", "keep-alive");
			oConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");
			oConnection.setRequestProperty("User-Agent", "WasdiLib.Java");

			oConnection.setConnectTimeout(60 * 60 * 1_000);
			oConnection.setReadTimeout(60 * 60 * 1_000);

			oConnection.setDoOutput(true);
			oConnection.setRequestMethod("POST");

			oConnection.addRequestProperty("Cookie", "JSESSIONID=" + sJsessionid);

			oConnection.connect();

			int iResponseCode = oConnection.getResponseCode();

			if (iResponseCode == HttpURLConnection.HTTP_OK) {
				List<HttpCookie> aoCookiesFromTheManager = oCookieManager.getCookieStore().getCookies();

				for (HttpCookie oCookie : aoCookiesFromTheManager) {
					asCookies.put(oCookie.getName(), oCookie.getValue());
				}
			}

			oConnection.disconnect();
		} catch (Exception oException) {
			WasdiLog.errorLog("CMHttpUtils.callLoginPostAndObtainCookies: error", oException);
		}

		return asCookies;
	}

	private static String callGetAndObtainResponse(String sUrl, Map<String, String> asCookies) {
		String sResponse = null;

		try {
			URL oUrl = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oUrl.openConnection();

			oConnection.setConnectTimeout(10 * 1_000);
			oConnection.setReadTimeout(10 * 1_000);

			oConnection.setRequestMethod("GET");

			for (Map.Entry<String, String> oEntry : asCookies.entrySet()) {
				oConnection.addRequestProperty("Cookie", oEntry.getKey() + "=" + oEntry.getValue());
			}

			int iResponseCode = oConnection.getResponseCode();

			if (iResponseCode == HttpURLConnection.HTTP_OK) {
				sResponse = HttpUtils.readHttpResponse(oConnection);

				if (sResponse.contains("You are registered but have forgotten your login/password?")
						|| sResponse.contains(
								"For security reasons, please Exit your web browser when you quit services requiring authentication!")) {
					WasdiLog.debugLog("CMHttpUtils.callGetAndObtainResponse: " + sResponse);
					resetCookies();

					sResponse = null;
				}
			}

			oConnection.disconnect();
		} 
		catch (Exception oException) {
			resetCookies();
			WasdiLog.errorLog("CMHttpUtils.callGetAndObtainResponse: error ", oException);
		}

		return sResponse;
	}

	private static void resetCookies() {
		if (CookieHandler.getDefault() != null) {
			CookieManager oCookieManager = ((CookieManager) CookieHandler.getDefault());
			oCookieManager.getCookieStore().removeAll();
		}
	}

	private static Map<String, String> extractHiddenElementsFromHtml(String sHtmlSource) {
		Map<String, String> asHiddenElementsFromHtml = new HashMap<>();

		if (!Utils.isNullOrEmpty(sHtmlSource)) {

			Document oDocument = Jsoup.parse(sHtmlSource);

			for (Element oForm : oDocument.select("form")) {
				for (Element oFieldset : oForm.select("fieldset")) {
					for (Element oInput : oFieldset.select("input")) {
						String sType = oInput.attr("type");

						if ("hidden".equalsIgnoreCase(sType)) {
							String sName = oInput.attr("name");
							String sValue = oInput.attr("value");

							asHiddenElementsFromHtml.put(sName, sValue);
						}
					}
				}
			}
		}

		return asHiddenElementsFromHtml;
	}

	private static String extractJsessionidFromHtml(String sHtmlSource) {
		String sJsessionid = null;

		Document oDocument = Jsoup.parse(sHtmlSource);

		for (Element oForm : oDocument.select("form")) {
			String sIdAttribute = oForm.attr("id");

			if ("authentification".equalsIgnoreCase(sIdAttribute)) {
				String sActionAttribute = oForm.attr("action");

				if (!Utils.isNullOrEmpty(sActionAttribute)) {
					sJsessionid = extractCookie(sActionAttribute);
				}
			}
		}

		return sJsessionid;
	}

	private static String extractCookie(String sAction) {
		return sAction.substring(sAction.indexOf("jsessionid=") + "jsessionid=".length(), sAction.indexOf("?"));
	}

	private static Map<String, String> preparePayloadForLoginPost(Map<String, String> asHiddenElementsFromHtml,
			String sUsername, String sPassword) {
		Map<String, String> asPayload = new HashMap<>();
		asPayload.put("username", sUsername);
		asPayload.put("password", sPassword);

		asHiddenElementsFromHtml.forEach(asPayload::put);

		return asPayload;
	}

	private static String parseValue(String sResponse, String sKey) {
		int iLengthOfKey = sKey.length();
		int iIndexOfKey = sResponse.indexOf(sKey + "=");
		int iIndexOfQuotes = sResponse.indexOf("\"", iIndexOfKey + iLengthOfKey + 2);

		return sResponse.substring(iIndexOfKey + iLengthOfKey + 2, iIndexOfQuotes);
	}

	private static String getQuery(Map<String, String> asParams) throws UnsupportedEncodingException {
		StringBuilder oResult = new StringBuilder();
		boolean first = true;

		for (Entry<String, String> asEntry : asParams.entrySet()) {
			if (first)
				first = false;
			else
				oResult.append("&");

			oResult.append(asEntry.getKey());
			oResult.append("=");
			oResult.append(asEntry.getValue());
		}

		return oResult.toString();
	}

}
