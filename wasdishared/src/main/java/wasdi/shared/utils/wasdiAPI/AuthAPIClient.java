package wasdi.shared.utils.wasdiAPI;

import java.util.Map;

import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.users.LoginInfo;

public class AuthAPIClient {
	
	public static String login(String sUserName, String sUserPassword, String sWasdiUrl) {
		
		try {
			
			if (Utils.isNullOrEmpty(sWasdiUrl)) {
				sWasdiUrl = WasdiConfig.Current.baseUrl;
			}
			
			if (!sWasdiUrl.endsWith("/")) sWasdiUrl += "/";
			
			LoginInfo oLoginInfo = new LoginInfo();
			oLoginInfo.setUserId(sUserName);
			oLoginInfo.setUserPassword(sUserPassword);
			
			String sUrl = sWasdiUrl+"auth/login";
			
			Map<String, String> aoHeaders = HttpUtils.getStandardHeaders("");
			aoHeaders.put("Accept", "application/json");
			
			HttpCallResponse oResponse = HttpUtils.httpPost(sUrl, JsonUtils.stringify(oLoginInfo), aoHeaders);
			
			if (oResponse == null) {
				WasdiLog.errorLog("AuthAPIClient.login: got null response");
				return "";
			}
			
			String sResponseBody = oResponse.getResponseBody();
			
			Map<String, Object> aoUserViewModel = JsonUtils.jsonToMapOfObjects(sResponseBody);
			
			String sSessionId = (String) aoUserViewModel.get("sessionId");
			return sSessionId;
			
		}
		catch(Exception oEx) {
			WasdiLog.errorLog("AuthAPIClient.login: exception ", oEx);
		}
		
		return "";
	}
	
	public static void logout(String sSessionId, String sWasdiUrl) {
		try {
			
			if (Utils.isNullOrEmpty(sWasdiUrl)) {
				sWasdiUrl = WasdiConfig.Current.baseUrl;
			}
			
			if (!sWasdiUrl.endsWith("/")) sWasdiUrl += "/";
			
			
			String sUrl = sWasdiUrl+"logout";
			
			HttpCallResponse oResponse = HttpUtils.httpGet(sUrl, HttpUtils.getStandardHeaders(sSessionId));
			
			if (oResponse == null) {
				WasdiLog.errorLog("AuthAPIClient.logout: got null response");
				return;
			}
		}
		catch(Exception oEx) {
			WasdiLog.errorLog("AuthAPIClient.logout: exception ", oEx);
		}
	}

}
