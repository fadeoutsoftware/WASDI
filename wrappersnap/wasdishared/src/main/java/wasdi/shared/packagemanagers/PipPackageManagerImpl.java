package wasdi.shared.packagemanagers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.json.JSONArray;
import org.json.JSONObject;

import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.LoggerWrapper;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.processors.PackageManagerViewModel;
import wasdi.shared.viewmodels.processors.PackageViewModel;

public class PipPackageManagerImpl implements IPackageManager {

	/**
	 * Static logger reference
	 */
	public static LoggerWrapper s_oLogger = new LoggerWrapper(LogManager.getLogger(PipPackageManagerImpl.class));

	private String m_sTargetIp;
	private int m_iTargetPort;

	public PipPackageManagerImpl(String sTargetIp, int iTargetPort) {
		m_sTargetIp = sTargetIp;
		m_iTargetPort = iTargetPort;
	}

	@Override
	public List<PackageViewModel> listPackages(String sFlag) {
		List<PackageViewModel> aoPackages = new ArrayList<>();

		String sUrl = "http://" + m_sTargetIp + ":" + m_iTargetPort + "/packageManager/listPackages/";

		if (!Utils.isNullOrEmpty(sFlag)) {
			sUrl += sFlag + "/";
		}

		WasdiLog.debugLog("PipPackageManagerImpl.listPackages | sUrl:" + sUrl + ".");

		Map<String, String> asHeaders = Collections.emptyMap();

		HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sUrl, asHeaders);
		Integer iResult = oHttpCallResponse.getResponseCode();
		String sJsonResponse = oHttpCallResponse.getResponseBody();

		if (iResult == null || iResult.intValue() != 200) {
			WasdiLog.errorLog("PipPackageManagerImpl.listPackages | iResult:" + iResult + ".");
			WasdiLog.errorLog("PipPackageManagerImpl.listPackages | sJsonResponse:" + sJsonResponse + ".");

			return null;
		}

		JSONArray aoJsonArray = new JSONArray(sJsonResponse);

		for (Object oItem : aoJsonArray) {
			if (null != oItem) {
				JSONObject oJsonItem = (JSONObject) oItem;

				String sManagerName = oJsonItem.optString("manager", "pip");
				String sPackageName = oJsonItem.optString("package", null);
				String sCurrentBuild = oJsonItem.optString("build", null);
				String sCurrentVersion = oJsonItem.optString("version", null);
				String sLatestVersion = oJsonItem.optString("latest", null);
				String sType = oJsonItem.optString("type", null);
				String sChannel = oJsonItem.optString("channel", null);

				aoPackages.add(new PackageViewModel(sManagerName, sPackageName, sCurrentVersion, sCurrentBuild, sLatestVersion, sType, sChannel));
			}
		}

		WasdiLog.debugLog("PipPackageManagerImpl.listPackages | aoPackages.size():" + aoPackages.size() + ".");

		return aoPackages;
	}

	@Override
	public PackageManagerViewModel getManagerVersion() {
		String sUrl = "http://" + m_sTargetIp + ":" + m_iTargetPort + "/packageManager/managerVersion/";

		WasdiLog.debugLog("PipPackageManagerImpl.getManagerVersion | sUrl:" + sUrl);

		Map<String, String> asHeaders = Collections.emptyMap();

		HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sUrl, asHeaders);
		Integer iResult = oHttpCallResponse.getResponseCode();
		String sJsonResponse = oHttpCallResponse.getResponseBody();

		if (iResult == null || iResult.intValue() != 200) {
			WasdiLog.errorLog("PipPackageManagerImpl.getManagerVersion | iResult:" + iResult + ".");
			WasdiLog.errorLog("PipPackageManagerImpl.getManagerVersion | sJsonResponse:" + sJsonResponse + ".");

			return null;
		}

		JSONObject oJsonItem = new JSONObject(sJsonResponse);

		String sName = oJsonItem.optString("name", "pip");
		String sVersion = oJsonItem.optString("version", null);
		int iMajor = oJsonItem.optInt("major", 0);
		int iMinor = oJsonItem.optInt("minor", 0);
		int iPatch = oJsonItem.optInt("patch", 0);

		PackageManagerViewModel oPackageManagerVM = new PackageManagerViewModel(sName, sVersion, iMajor, iMinor, iPatch);

		WasdiLog.debugLog("PipPackageManagerImpl.getManagerVersion | oPackageManager:" + oPackageManagerVM.toString() + ".");

		return oPackageManagerVM;
	}

	@Override
	public Map<String, Object> getPackagesInfo() {
		PackageManagerViewModel oPackageManagerVM = getManagerVersion();

		List<PackageViewModel> aoPackagesOutdated = listPackages("o");

		List<PackageViewModel> aoPackagesUptodate = listPackages("u");

		Map<String, Object> aoPackagesInfo = new HashMap<>();
		aoPackagesInfo.put("packageManager", oPackageManagerVM);
		aoPackagesInfo.put("outdated", aoPackagesOutdated);
		aoPackagesInfo.put("uptodate", aoPackagesUptodate);

		return aoPackagesInfo;
	}

	@Override
	public boolean operatePackageChange(String sUpdateCommand) {
		// Call localhost:port
		String sUrl = "http://" + m_sTargetIp + ":" + m_iTargetPort + "/packageManager/" + sUpdateCommand;
		WasdiLog.debugLog("PipPackageManagerImpl.operatePackageChange: sUrl: " + sUrl);

		Map<String, String> asHeaders = Collections.emptyMap();

		HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sUrl, asHeaders);
		Integer iResult = oHttpCallResponse.getResponseCode();
		String sResponse = oHttpCallResponse.getResponseBody();

		WasdiLog.debugLog("PipPackageManagerImpl.operatePackageChange: iResult: " + iResult);
		WasdiLog.debugLog("PipPackageManagerImpl.operatePackageChange: " + sResponse);

		if (iResult != null && (200 <= iResult.intValue() && 299 >= iResult.intValue())) {
			WasdiLog.infoLog("PipPackageManagerImpl.operatePackageChange: Output from Server .... \n");
			WasdiLog.infoLog("PipPackageManagerImpl.operatePackageChange: " + sResponse);
			WasdiLog.debugLog("PipPackageManagerImpl.operatePackageChange: env updated");
			return true;
		} else {
			return false;
		}
	}

}
