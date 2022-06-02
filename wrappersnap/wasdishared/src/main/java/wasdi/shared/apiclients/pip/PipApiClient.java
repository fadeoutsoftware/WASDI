package wasdi.shared.apiclients.pip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import wasdi.shared.business.Package;
import wasdi.shared.business.PackageManager;

import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;

public class PipApiClient {

	private String m_sTargetIp;
	private int m_iTargetPort;

	public PipApiClient(String sTargetIp, int iTargetPort) {
		m_sTargetIp = sTargetIp;
		m_iTargetPort = iTargetPort;
	}

	public List<Package> listPackages(String sFlag) {
		List<Package> aoPackages = new ArrayList<>();

		String sJsonResponse = callGetPackages(sFlag);

		Utils.debugLog("PipApiClient.listPackages | sJsonResponse:" + sJsonResponse + ".");

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

		Utils.debugLog("PipApiClient.listPackages | aoPackages.size():" + aoPackages.size() + ".");

		return aoPackages;
	}

	public PackageManager getManagerVersion() {
		String sJsonResponse = callGetManagerVersion();

		Utils.debugLog("PipApiClient.getManagerVersion | sJsonResponse:" + sJsonResponse + ".");

		JSONObject oJsonItem = new JSONObject(sJsonResponse);

		String sName = oJsonItem.optString("name", "pip");
		String sVersion = oJsonItem.optString("version", null);
		int iMajor = oJsonItem.optInt("major", 0);
		int iMinor = oJsonItem.optInt("minor", 0);
		int iPatch = oJsonItem.optInt("patch", 0);

		PackageManager oPackageManager = new PackageManager(sName, sVersion, iMajor, iMinor, iPatch);

		Utils.debugLog("PipApiClient.getManagerVersion | oPackageManager:" + oPackageManager.toString() + ".");

		return oPackageManager;
	}

	private String callGetPackages(String sFlag) {
		String sUrl = "http://" + m_sTargetIp + ":" + m_iTargetPort + "/packageManager/listPackages/";

		if (!Utils.isNullOrEmpty(sFlag)) {
			sUrl += sFlag + "/";
		}

		Map<String, String> asHeaders = Collections.emptyMap();

		String sJsonResponse = HttpUtils.standardHttpGETQuery(sUrl, asHeaders);

		return sJsonResponse;
	}

	private String callGetManagerVersion() {
		String sUrl = "http://" + m_sTargetIp + ":" + m_iTargetPort + "/packageManager/managerVersion/";

		Map<String, String> asHeaders = Collections.emptyMap();

		String sJsonResponse = HttpUtils.standardHttpGETQuery(sUrl, asHeaders);

		return sJsonResponse;
	}

}
