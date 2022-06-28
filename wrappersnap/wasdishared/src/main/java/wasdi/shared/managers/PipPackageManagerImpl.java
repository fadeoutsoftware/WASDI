package wasdi.shared.managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.LoggerWrapper;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.processors.PackageManagerViewModel;
import wasdi.shared.viewmodels.processors.PackageViewModel;

public class PipPackageManagerImpl implements IPackageManager {

	/**
	 * Static logger reference
	 */
	public static LoggerWrapper s_oLogger = new LoggerWrapper(Logger.getLogger(PipPackageManagerImpl.class));

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

		s_oLogger.debug("PipPackageManagerImpl.callGetPackages | sUrl:" + sUrl + ".");

		Map<String, String> asHeaders = Collections.emptyMap();

		HttpCallResponse oHttpCallResponse = HttpUtils.newStandardHttpGETQuery(sUrl, asHeaders);
		Integer iResult = oHttpCallResponse.getResponseCode();
		String sJsonResponse = oHttpCallResponse.getResponseBody();

		if (iResult == null || iResult.intValue() != 200) {
			s_oLogger.error("PipPackageManagerImpl.listPackages | iResult:" + iResult + ".");
			s_oLogger.error("PipPackageManagerImpl.listPackages | sJsonResponse:" + sJsonResponse + ".");

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

				aoPackages.add(new PackageViewModel(sManagerName, sPackageName, sCurrentVersion, sLatestVersion, sType));
			}
		}

		s_oLogger.debug("PipPackageManagerImpl.listPackages | aoPackages.size():" + aoPackages.size() + ".");

		return aoPackages;
	}

	@Override
	public PackageManagerViewModel getManagerVersion() {
		String sUrl = "http://" + m_sTargetIp + ":" + m_iTargetPort + "/packageManager/managerVersion/";

		s_oLogger.debug("PipPackageManagerImpl.callGetManagerVersion | sUrl:" + sUrl);

		Map<String, String> asHeaders = Collections.emptyMap();

		HttpCallResponse oHttpCallResponse = HttpUtils.newStandardHttpGETQuery(sUrl, asHeaders);
		Integer iResult = oHttpCallResponse.getResponseCode();
		String sJsonResponse = oHttpCallResponse.getResponseBody();

		if (iResult == null || iResult.intValue() != 200) {
			s_oLogger.error("PipPackageManagerImpl.getManagerVersion | iResult:" + iResult + ".");
			s_oLogger.error("PipPackageManagerImpl.getManagerVersion | sJsonResponse:" + sJsonResponse + ".");

			return null;
		}

		JSONObject oJsonItem = new JSONObject(sJsonResponse);

		String sName = oJsonItem.optString("name", "pip");
		String sVersion = oJsonItem.optString("version", null);
		int iMajor = oJsonItem.optInt("major", 0);
		int iMinor = oJsonItem.optInt("minor", 0);
		int iPatch = oJsonItem.optInt("patch", 0);

		PackageManagerViewModel oPackageManagerVM = new PackageManagerViewModel(sName, sVersion, iMajor, iMinor, iPatch);

		s_oLogger.debug("PipPackageManagerImpl.getManagerVersion | oPackageManager:" + oPackageManagerVM.toString() + ".");

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
	public void operatePackageChange(String sUpdateCommand) {
		// Call localhost:port
		String sUrl = "http://" + m_sTargetIp + ":" + m_iTargetPort + "/packageManager/" + sUpdateCommand;
		s_oLogger.debug("PipPackageManagerImpl.operatePackageChange: sUrl: " + sUrl);

		Map<String, String> asHeaders = Collections.emptyMap();

		HttpCallResponse oHttpCallResponse = HttpUtils.newStandardHttpGETQuery(sUrl, asHeaders);
		Integer iResult = oHttpCallResponse.getResponseCode();
		String sResponse = oHttpCallResponse.getResponseBody();

		s_oLogger.debug("PipPackageManagerImpl.operatePackageChange: iResult: " + iResult);
		s_oLogger.debug("PipPackageManagerImpl.operatePackageChange: " + sResponse);

		if (iResult != null && (200 <= iResult.intValue() && 299 >= iResult.intValue())) {
			s_oLogger.info("PipPackageManagerImpl.operatePackageChange: Output from Server .... \n");
			s_oLogger.info("PipPackageManagerImpl.operatePackageChange: " + sResponse);
			s_oLogger.debug("PipPackageManagerImpl.operatePackageChange: env updated");
		} else {
			throw new RuntimeException(sResponse);
		}
	}

}
