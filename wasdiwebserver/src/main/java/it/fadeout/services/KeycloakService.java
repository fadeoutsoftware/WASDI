package it.fadeout.services;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import wasdi.shared.business.users.User;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.TimeEpochUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.PrimitiveResult;

/**
 * Implements the AuthService Provider for Keycloak
 * @author p.campanella
 *
 */
public class KeycloakService implements AuthProviderService {

	private static final String s_sACCESS_TOKEN = "access_token";
	

	public String getToken() {
		try {
			String sAuthUrl = WasdiConfig.Current.keycloack.address;
			String sCliSecret =  WasdiConfig.Current.keycloack.cliSecret;
			//URL
			if(!sAuthUrl.endsWith("/")) {
				sAuthUrl += "/";
			}
			sAuthUrl += "realms/wasdi/protocol/openid-connect/token";
			//payload
			String sPayload = "client_id=admin-cli" +
					"&grant_type=client_credentials" +
					"&client_secret=" + sCliSecret;
			//headers
			Map<String, String> asHeaders = new HashMap<>();
			asHeaders.put("Content-Type", "application/x-www-form-urlencoded");
			//POST -> authenticate on keycloak 
			WasdiLog.debugLog("KeycloakService.getToken: about to get token: " + sAuthUrl + ", " + sPayload);
			HttpCallResponse oHttpCallResponse = HttpUtils.httpPost(sAuthUrl, sPayload, asHeaders);
			String sAuthResult = oHttpCallResponse.getResponseBody();
			if(Utils.isNullOrEmpty(sAuthResult)) {
				throw new RuntimeException("could not login into keycloak");
			}
			//read response and get token
			JSONObject oJson = new JSONObject(sAuthResult);
			if( null == oJson||
					!oJson.has(s_sACCESS_TOKEN) ||							
					Utils.isNullOrEmpty(oJson.optString(s_sACCESS_TOKEN, null))
					) {
				throw new NullPointerException("Missing access token");
			}
			String sKcTokenId = "";
			sKcTokenId = oJson.optString(s_sACCESS_TOKEN, null);
			if(Utils.isNullOrEmpty(sKcTokenId)) {
				throw new NullPointerException("Token id null or empty");
			}
			WasdiLog.debugLog("KeycloakService.getKeycloakAdminCliToken: admin token obtained :-)");
			return sKcTokenId;
		} catch (Exception oE) {
			WasdiLog.debugLog("KeycloakService.getKeycloakAdminCliToken: " + oE);
		}
		return null;
	}

	public String getUserData(String sToken, String sUserId) {
		// build keycloak API URL
		String sUrl = WasdiConfig.Current.keycloack.address;
		if(!sUrl.endsWith("/")) {
			sUrl += "/";
		}
		sUrl += "admin/realms/wasdi/users?exact=true&username=";
		sUrl += sUserId;
		WasdiLog.debugLog("KeycloakService.getUserData: about to GET to " + sUrl);
		Map<String, String> asHeaders = new HashMap<>();
		asHeaders.clear();
		asHeaders.put("Authorization", "Bearer " + sToken);
		
		HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sUrl, asHeaders); 
		String sResponse = oHttpCallResponse.getResponseBody();
		
		WasdiLog.debugLog("KeycloakService.getUserData: user data: " + sResponse);
		return sResponse;
	}

	@Override
	public String login(String sUser, String sPassword) {
		//authenticate against keycloak
		String sUrl = WasdiConfig.Current.keycloack.authTokenAddress;

		String sPayload = "client_id=";
		sPayload += WasdiConfig.Current.keycloack.confidentialClient;
		sPayload += "&client_secret=" + WasdiConfig.Current.keycloack.clientSecret;
		sPayload += "&grant_type=password&username=" + sUser;
		// Url Encoding of the password 
		try{
		sPayload += "&password=" + URLEncoder.encode(sPassword, StandardCharsets.UTF_8.toString());
		}
		catch (Exception e){
		WasdiLog.debugLog("KeycloakService.login: Exception on encoding URL " + e.getMessage());	
		}
		
		Map<String, String> asHeaders = new HashMap<>();
		asHeaders.put("Content-Type", "application/x-www-form-urlencoded");
		HttpCallResponse oHttpCallResponse = HttpUtils.httpPost(sUrl, sPayload, asHeaders); 
		String sAuthResult = oHttpCallResponse.getResponseBody();
		WasdiLog.debugLog("KeycloakService.login: auth result: " + sAuthResult);
		return sAuthResult;
	}
	
	@Override
	public String getUserDbId(String sUserId) {
		if(Utils.isNullOrEmpty(sUserId)) {
			WasdiLog.debugLog("KeycloakService.getUserDbId: user id null or empty, aborting");
			return null;
		}
		String sToken = getToken();
		if(Utils.isNullOrEmpty(sToken)) {
			WasdiLog.debugLog("KeycloakService.getUserDbId: token null or empty, aborting");
			return null;
		}
		String sResponse = getUserData(sToken, sUserId);
		if(Utils.isNullOrEmpty(sResponse)) {
			WasdiLog.debugLog("KeycloakService.getUserDbId: response null or empty, aborting");
			return null;
		}
		try {
			JSONArray oResponseArray = new JSONArray(sResponse);
			if(oResponseArray.length() != 1) {
				WasdiLog.debugLog("KeycloakService.getUserDbId: response array has length " + oResponseArray.length() + ", but lenght of exactly 1 was expected, aborting");
				return null;
			}
			JSONObject oEntry = oResponseArray.getJSONObject(0);
			String sUserDbId = oEntry.optString("id", null);
			WasdiLog.debugLog("KeycloakService.getUserDbId: user DB id: " + sUserDbId );
			return sUserDbId;
		} catch (Exception oE) {
			WasdiLog.debugLog("KeycloakService.getUserDbId: could not parse response due to " + oE + ", aborting");
		}
		return null;
	}
	
	@Override
	public PrimitiveResult requirePasswordUpdateViaEmail(String sUserId) {
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		oResult.setIntValue(500);
		if(Utils.isNullOrEmpty(sUserId)) {
			WasdiLog.debugLog("KeycloakService.requirePasswordUpdateViaEmail: user id null or empty, aborting");
			return oResult;
		}
		String sUserDbId = getUserDbId(sUserId);
		if(Utils.isNullOrEmpty(sUserDbId)) {
			WasdiLog.debugLog("KeycloakService.requirePasswordUpdateViaEmail: user DB id null or empty, aborting");
			return oResult;
		}
		
		String sBaseUrl = WasdiConfig.Current.keycloack.address;
		
		if (!sBaseUrl.endsWith("/")) sBaseUrl += "/";
		
		StringBuilder oUrlBuilder = new StringBuilder()
				.append(sBaseUrl)
				.append("admin/realms/wasdi/users/")
				.append(sUserDbId)
				.append("/execute-actions-email?redirect_uri=")
				.append(WasdiConfig.Current.baseUrl)
				.append("&client_id=wasdi_api\"");
		String sUrl = oUrlBuilder.toString();
		String sPayload = "[\"UPDATE_PASSWORD\"]";
		try {
			String sToken = getToken();
			Map<String, String> asHeaders = new HashMap<>();
			asHeaders.put("Authorization", "Bearer " + sToken);
			asHeaders.put("Content-Type", "application/json");
			String sResponse = HttpUtils.httpPut(sUrl, sPayload, asHeaders);
			WasdiLog.debugLog("KeycloakService.getUserDbId: response (should be empty): \"" + sResponse + "\"");
			oResult.setIntValue(200);
			return oResult;
		} catch (Exception oE) {
			WasdiLog.debugLog("KeycloakService.requirePasswordUpdateViaEmail: could not establish connection due to: " + oE + ", aborting");
		}
		
				
		return oResult;
	}
	

	@Override
	public User getUser(String sUserId) {
		//first: authenticate on keycloak as admin and get the token
		String sKcTokenId = getToken();
		
		// second: check the user exists on keycloak
		String sBody = getUserData(sKcTokenId, sUserId);

		//validate
		if(Utils.isNullOrEmpty(sBody)) {
			throw new NullPointerException("keycloak returned null body");
		}
		JSONArray oJsonArray = new JSONArray(sBody);
		if(oJsonArray.length() < 1 ) {
			throw new IllegalStateException("Returned JSON array has 0 or less elements");
		}
		if(oJsonArray.length() > 1 ) {
			throw new IllegalStateException("Returned JSON array has more than 1 element");
		}

		//then get the user
		JSONObject oJsonResponse = (JSONObject)oJsonArray.get(0);
		User oNewUser = new User();
		if(oJsonResponse.has("firstName")) {
			oNewUser.setName(oJsonResponse.optString("firstName", null));
		}
		if(oJsonResponse.has("lastName")) {
			oNewUser.setSurname(oJsonResponse.optString("lastName", null));
		}
		if(oJsonResponse.has("createdTimestamp")) {
			oNewUser.setRegistrationDate(TimeEpochUtils.fromEpochToDateString(oJsonResponse.optLong("createdTimestamp", System.currentTimeMillis())));
		}
		oNewUser.setUserId(sUserId);
		return oNewUser;
	}
	
	@Override
	public boolean logout(String sSessionId) {
		
		try {
			String sUrl = WasdiConfig.Current.keycloack.address;
			//URL
			if(!sUrl.endsWith("/")) {
				sUrl += "/";
			}
			sUrl += "realms/wasdi/protocol/openid-connect/logout";
			//payload
			String sPayload = "client_id=wasdi_api" +
					"&client_secret=" + WasdiConfig.Current.keycloack.clientSecret + 
					"&refresh_token=" + sSessionId;
			//headers
			Map<String, String> asHeaders = new HashMap<>();
			asHeaders.put("Content-Type", "application/x-www-form-urlencoded");
			//POST -> authenticate on keycloak 
			WasdiLog.debugLog("KeycloakService.logout: about to logout: " + sUrl + ", " + sPayload);
			HttpCallResponse oHttpCallResponse = HttpUtils.httpPost(sUrl, sPayload, asHeaders); 
			String sLogoutResult = oHttpCallResponse.getResponseBody();
			WasdiLog.debugLog("KeycloakService.logout: logout result " + sLogoutResult);
			return true;
		} catch (Exception oE) {
			WasdiLog.debugLog("KeycloakService.getKeycloakAdminCliToken: " + oE);
		}
		return false;
	}
}






