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

public class CondaPackageManagerImpl implements IPackageManager {

	/**
	 * Static logger reference
	 */
	public static LoggerWrapper s_oLogger = new LoggerWrapper(Logger.getLogger(CondaPackageManagerImpl.class));

	private String m_sTargetIp;
	private int m_iTargetPort;

	public CondaPackageManagerImpl(String sTargetIp, int iTargetPort) {
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

		s_oLogger.debug("CondaPackageManagerImpl.callGetPackages | sUrl:" + sUrl + ".");

		Map<String, String> asHeaders = Collections.emptyMap();

		HttpCallResponse oHttpCallResponse = HttpUtils.newStandardHttpGETQuery(sUrl, asHeaders);
		Integer iResult = oHttpCallResponse.getResponseCode();
		String sJsonResponse = oHttpCallResponse.getResponseBody();

		if (iResult == null || iResult.intValue() != 200) {
			s_oLogger.error("CondaPackageManagerImpl.listPackages | iResult:" + iResult + ".");
			s_oLogger.error("CondaPackageManagerImpl.listPackages | sJsonResponse:" + sJsonResponse + ".");

			return null;
		}

		JSONArray aoJsonArray = new JSONArray(sJsonResponse);

		for (Object oItem : aoJsonArray) {
			if (null != oItem) {
				JSONObject oJsonItem = (JSONObject) oItem;

				String sManagerName = oJsonItem.optString("manager", "conda");
				String sPackageName = oJsonItem.optString("name", null);
				String sCurrentVersion = oJsonItem.optString("version", null);
				String sCurrentBuild = oJsonItem.optString("build", null);
				String sLatestVersion = oJsonItem.optString("latest", null);
				String sType = oJsonItem.optString("type", null);
				String sChannel = oJsonItem.optString("channel", null);

				aoPackages.add(new PackageViewModel(sManagerName, sPackageName, sCurrentVersion, sCurrentBuild, sLatestVersion, sType, sChannel));
			}
		}

		s_oLogger.debug("CondaPackageManagerImpl.listPackages | aoPackages.size():" + aoPackages.size() + ".");

		return aoPackages;
	}

	@Override
	public PackageManagerViewModel getManagerVersion() {
		String sUrl = "http://" + m_sTargetIp + ":" + m_iTargetPort + "/packageManager/managerVersion/";

		s_oLogger.debug("CondaPackageManagerImpl.callGetManagerVersion | sUrl:" + sUrl);

		Map<String, String> asHeaders = Collections.emptyMap();

		HttpCallResponse oHttpCallResponse = HttpUtils.newStandardHttpGETQuery(sUrl, asHeaders);
		Integer iResult = oHttpCallResponse.getResponseCode();
		String sJsonResponse = oHttpCallResponse.getResponseBody();

		if (iResult == null || iResult.intValue() != 200) {
			s_oLogger.error("CondaPackageManagerImpl.getManagerVersion | iResult:" + iResult + ".");
			s_oLogger.error("CondaPackageManagerImpl.getManagerVersion | sJsonResponse:" + sJsonResponse + ".");

			return null;
		}

		JSONObject oJsonItem = new JSONObject(sJsonResponse);

		String sName = oJsonItem.optString("name", "conda");
		String sVersion = oJsonItem.optString("version", null);
		int iMajor = oJsonItem.optInt("major", 0);
		int iMinor = oJsonItem.optInt("minor", 0);
		int iPatch = oJsonItem.optInt("patch", 0);

		PackageManagerViewModel oPackageManagerVM = new PackageManagerViewModel(sName, sVersion, iMajor, iMinor, iPatch);

		s_oLogger.debug("CondaPackageManagerImpl.getManagerVersion | oPackageManager:" + oPackageManagerVM.toString() + ".");

		return oPackageManagerVM;
	}

	@Override
	public Map<String, Object> getPackagesInfo() {
		PackageManagerViewModel oPackageManagerVM = getManagerVersion();

		List<PackageViewModel> aoPackages = listPackages(null);

		Map<String, Object> aoPackagesInfo = new HashMap<>();
		aoPackagesInfo.put("packageManager", oPackageManagerVM);
		aoPackagesInfo.put("all", aoPackages);

		return aoPackagesInfo;
	}

	@Override
	public boolean operatePackageChange(String sUpdateCommand) {
		// Call localhost:port
		String sUrl = "http://" + m_sTargetIp + ":" + m_iTargetPort + "/packageManager/" + sUpdateCommand;
		s_oLogger.debug("CondaPackageManagerImpl.operatePackageChange: sUrl: " + sUrl);

		Map<String, String> asHeaders = Collections.emptyMap();

		HttpCallResponse oHttpCallResponse = HttpUtils.newStandardHttpGETQuery(sUrl, asHeaders);
		Integer iResult = oHttpCallResponse.getResponseCode();
		String sResponse = oHttpCallResponse.getResponseBody();

		s_oLogger.debug("CondaPackageManagerImpl.operatePackageChange: iResult: " + iResult);
		//s_oLogger.debug("CondaPackageManagerImpl.operatePackageChange: " + sResponse);

		if (iResult != null && (200 <= iResult.intValue() && 299 >= iResult.intValue())) {
			//s_oLogger.info("CondaPackageManagerImpl.operatePackageChange: Output from Server .... \n");
			//
			s_oLogger.debug("CondaPackageManagerImpl.operatePackageChange: env updated");
			
			return true;
		} else {
			s_oLogger.error("CondaPackageManagerImpl.error: " + iResult + " response "+ sResponse);
			return false;
		}
	}
}
