package wasdi.shared.apiclients.pip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import wasdi.shared.business.Package;
import wasdi.shared.business.PackageManager;

import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.LoggerWrapper;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.HttpCallResponse;

public class PipApiClient {

	/**
	 * Static logger reference
	 */
	public static LoggerWrapper s_oLogger = new LoggerWrapper(Logger.getLogger(PipApiClient.class));

	private String m_sTargetIp;
	private int m_iTargetPort;

	public PipApiClient(String sTargetIp, int iTargetPort) {
		m_sTargetIp = sTargetIp;
		m_iTargetPort = iTargetPort;
	}

	public List<Package> listPackages(String sFlag) {
		List<Package> aoPackages = new ArrayList<>();

		String sUrl = "http://" + m_sTargetIp + ":" + m_iTargetPort + "/packageManager/listPackages/";

		if (!Utils.isNullOrEmpty(sFlag)) {
			sUrl += sFlag + "/";
		}

		s_oLogger.debug("PipApiClient.callGetPackages | sUrl:" + sUrl + ".");

		Map<String, String> asHeaders = Collections.emptyMap();

		HttpCallResponse oHttpCallResponse = HttpUtils.newStandardHttpGETQuery(sUrl, asHeaders);
		Integer iResult = oHttpCallResponse.getResponseCode();
		String sJsonResponse = oHttpCallResponse.getResponseBody();

		if (iResult == null || iResult.intValue() != 200) {
			s_oLogger.error("PipApiClient.listPackages | iResult:" + iResult + ".");
			s_oLogger.error("PipApiClient.listPackages | sJsonResponse:" + sJsonResponse + ".");

			return null;
		}

		JSONArray aoJsonArray = new JSONArray(sJsonResponse);

		for (Object oItem : aoJsonArray) {
			if (null != oItem) {
				JSONObject oJsonItem = (JSONObject) oItem;

				String sManagerName = oJsonItem.optString("manager", "pip");
				String sPackageName = oJsonItem.optString("package", null);
				String sCurrentVersion = oJsonItem.optString("version", null);
				String sLatestVersion = oJsonItem.optString("latest", null);
				String sType = oJsonItem.optString("type", null);

				aoPackages.add(new Package(sManagerName, sPackageName, sCurrentVersion, sLatestVersion, sType));
			}
		}

		s_oLogger.debug("PipApiClient.listPackages | aoPackages.size():" + aoPackages.size() + ".");

		return aoPackages;
	}

	public PackageManager getManagerVersion() {
		String sUrl = "http://" + m_sTargetIp + ":" + m_iTargetPort + "/packageManager/managerVersion/";

		s_oLogger.debug("PipApiClient.callGetManagerVersion | sUrl:" + sUrl);

		Map<String, String> asHeaders = Collections.emptyMap();

		HttpCallResponse oHttpCallResponse = HttpUtils.newStandardHttpGETQuery(sUrl, asHeaders);
		Integer iResult = oHttpCallResponse.getResponseCode();
		String sJsonResponse = oHttpCallResponse.getResponseBody();

		if (iResult == null || iResult.intValue() != 200) {
			s_oLogger.error("PipApiClient.getManagerVersion | iResult:" + iResult + ".");
			s_oLogger.error("PipApiClient.getManagerVersion | sJsonResponse:" + sJsonResponse + ".");

			return null;
		}

		JSONObject oJsonItem = new JSONObject(sJsonResponse);

		String sName = oJsonItem.optString("name", "pip");
		String sVersion = oJsonItem.optString("version", null);
		int iMajor = oJsonItem.optInt("major", 0);
		int iMinor = oJsonItem.optInt("minor", 0);
		int iPatch = oJsonItem.optInt("patch", 0);

		PackageManager oPackageManager = new PackageManager(sName, sVersion, iMajor, iMinor, iPatch);

		s_oLogger.debug("PipApiClient.getManagerVersion | oPackageManager:" + oPackageManager.toString() + ".");

		return oPackageManager;
	}

	public Map<String, Object> getPackagesInfo() {
		PackageManager oPackageManager = getManagerVersion();

		List<Package> aoPackagesOutdated = listPackages("o");

		List<Package> aoPackagesUptodate = listPackages("u");

		Map<String, Object> aoPackagesInfo = new HashMap<>();
		aoPackagesInfo.put("packageManager", oPackageManager);
		aoPackagesInfo.put("outdated", aoPackagesOutdated);
		aoPackagesInfo.put("uptodate", aoPackagesUptodate);

		return aoPackagesInfo;
	}

}
