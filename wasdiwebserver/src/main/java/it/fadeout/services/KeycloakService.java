package it.fadeout.services;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

import it.fadeout.Wasdi;
import wasdi.shared.business.User;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.TimeEpochUtils;
import wasdi.shared.utils.Utils;
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
			Utils.debugLog("KeycloakService.getToken: about to get token: " + sAuthUrl + ", " + sPayload);
			String sAuthResult = Wasdi.httpPost(sAuthUrl, sPayload, asHeaders);
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
			Utils.debugLog("KeycloakService.getKeycloakAdminCliToken: admin token obtained :-)");
			return sKcTokenId;
		} catch (Exception oE) {
			Utils.debugLog("KeycloakService.getKeycloakAdminCliToken: " + oE);
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
		Utils.debugLog("KeycloakService.getUserData: about to GET to " + sUrl);
		Map<String, String> asHeaders = new HashMap<>();
		asHeaders.clear();
		asHeaders.put("Authorization", "Bearer " + sToken);
		String sResponse = Wasdi.httpGet(sUrl, asHeaders);
		Utils.debugLog("KeycloakService.getUserData: user data: " + sResponse);
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
		sPayload += "&password=" + sPassword;
		Map<String, String> asHeaders = new HashMap<>();
		asHeaders.put("Content-Type", "application/x-www-form-urlencoded");
		String sAuthResult = Wasdi.httpPost(sUrl, sPayload, asHeaders);
		Utils.debugLog("KeycloakService.login: auth result: " + sAuthResult);
		return sAuthResult;
	}
	
	@Override
	public String getUserDbId(String sUserId) {
		if(Utils.isNullOrEmpty(sUserId)) {
			Utils.debugLog("KeycloakService.getUserDbId: user id null or empty, aborting");
			return null;
		}
		String sToken = getToken();
		if(Utils.isNullOrEmpty(sToken)) {
			Utils.debugLog("KeycloakService.getUserDbId: token null or empty, aborting");
			return null;
		}
		String sResponse = getUserData(sToken, sUserId);
		if(Utils.isNullOrEmpty(sResponse)) {
			Utils.debugLog("KeycloakService.getUserDbId: response null or empty, aborting");
			return null;
		}
		try {
			JSONArray oResponseArray = new JSONArray(sResponse);
			if(oResponseArray.length() != 1) {
				Utils.debugLog("KeycloakService.getUserDbId: response array has length " + oResponseArray.length() + ", but lenght of exactly 1 was expected, aborting");
				return null;
			}
			JSONObject oEntry = oResponseArray.getJSONObject(0);
			String sUserDbId = oEntry.optString("id", null);
			Utils.debugLog("KeycloakService.getUserDbId: user DB id: " + sUserDbId );
			return sUserDbId;
		} catch (Exception oE) {
			Utils.debugLog("KeycloakService.getUserDbId: could not parse response due to " + oE + ", aborting");
		}
		return null;
	}
	
	@Override
	public PrimitiveResult requirePasswordUpdateViaEmail(String sUserId) {
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		oResult.setIntValue(500);
		if(Utils.isNullOrEmpty(sUserId)) {
			Utils.debugLog("KeycloakService.requirePasswordUpdateViaEmail: user id null or empty, aborting");
			return oResult;
		}
		String sUserDbId = getUserDbId(sUserId);
		if(Utils.isNullOrEmpty(sUserDbId)) {
			Utils.debugLog("KeycloakService.requirePasswordUpdateViaEmail: user DB id null or empty, aborting");
			return oResult;
		}
		StringBuilder oUrlBuilder = new StringBuilder()
				.append(WasdiConfig.Current.keycloack.address)
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
			String sResponse = Wasdi.httpPut(sUrl, sPayload, asHeaders);
			Utils.debugLog("KeycloakService.getUserDbId: response (should be empty): \"" + sResponse + "\"");
			oResult.setIntValue(200);
			return oResult;
		} catch (Exception oE) {
			Utils.debugLog("KeycloakService.requirePasswordUpdateViaEmail: could not establish connection due to: " + oE + ", aborting");
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
	
	public String getOldStyleRandomSession() {
		return UUID.randomUUID().toString();
	}
	
	public String insertOldStyleSession(String sUserId){
		
		return "";
	}
}






