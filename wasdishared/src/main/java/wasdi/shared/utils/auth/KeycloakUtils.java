package wasdi.shared.utils.auth;

import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSAlgorithmFamilyJWSKeySelector;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.json.JSONArray;
import org.json.JSONObject;

import wasdi.shared.business.users.User;
import wasdi.shared.config.KeycloackConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.TimeEpochUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.PrimitiveResult;

public class KeycloakUtils {
	private static final String s_sACCESS_TOKEN = "access_token";
	private static ConfigurableJWTProcessor<SecurityContext> s_oJwtProcessor;
	private static String s_sJwtConfigurationKey = "";
	

	public static String getToken() {
		try {
			
			if (WasdiConfig.Current.keycloack==null) return "";
			
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
			WasdiLog.debugLog("KeycloakService.getToken: about to get token: " + sAuthUrl);
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

	public static String getUserData(String sToken, String sUserId) {
		
		if (WasdiConfig.Current.keycloack==null) return "";
		
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

	public static String login(String sUser, String sPassword) {
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
		
		return sAuthResult;
	}

	/**
	 * Exchanges a Keycloak refresh token for a new token pair.
	 *
	 * @param sRefreshToken refresh token supplied by the client
	 * @return Keycloak token response or an empty string in case of error
	 */
	public static String refreshToken(String sRefreshToken) {
		if (Utils.isNullOrEmpty(sRefreshToken) || WasdiConfig.Current.keycloack == null) {
			return "";
		}

		try {
			String sPayload = "client_id=" + WasdiConfig.Current.keycloack.confidentialClient
					+ "&client_secret=" + WasdiConfig.Current.keycloack.clientSecret
					+ "&grant_type=refresh_token&refresh_token=" + URLEncoder.encode(sRefreshToken, StandardCharsets.UTF_8.toString());
			Map<String, String> asHeaders = new HashMap<>();
			asHeaders.put("Content-Type", "application/x-www-form-urlencoded");
			return HttpUtils.httpPost(WasdiConfig.Current.keycloack.authTokenAddress, sPayload, asHeaders).getResponseBody();
		} catch (Exception oEx) {
			WasdiLog.warnLog("KeycloakUtils.refreshToken: " + oEx);
		}

		return "";
	}
	
	
	public static String getUserDbId(String sUserId) {
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
	
	
	public static PrimitiveResult requirePasswordUpdateViaEmail(String sUserId) {
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		oResult.setIntValue(500);
		
		if (WasdiConfig.Current.keycloack==null) {
			WasdiLog.debugLog("KeycloakService.requirePasswordUpdateViaEmail: keycloack config is null, aborting");
			return oResult;
		} 
		
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

	public static boolean updatePassword(String sUserId, String sCurrentPassword, String sNewPassword) {
		if (WasdiConfig.Current.keycloack == null) {
			WasdiLog.debugLog("KeycloakUtils.updatePassword: keycloack config is null, aborting");
			return false;
		}

		if (Utils.isNullOrEmpty(sUserId) || Utils.isNullOrEmpty(sCurrentPassword) || Utils.isNullOrEmpty(sNewPassword)) {
			WasdiLog.debugLog("KeycloakUtils.updatePassword: invalid input, aborting");
			return false;
		}

		String sAuthResult = login(sUserId, sCurrentPassword);
		if (Utils.isNullOrEmpty(sAuthResult)) {
			WasdiLog.debugLog("KeycloakUtils.updatePassword: current password is invalid for user " + sUserId);
			return false;
		}

		String sUserAccessToken;
		try {
			JSONObject oJson = new JSONObject(sAuthResult);
			sUserAccessToken = oJson.optString(s_sACCESS_TOKEN, null);
			if (Utils.isNullOrEmpty(sUserAccessToken)) {
				WasdiLog.debugLog("KeycloakUtils.updatePassword: keycloak login did not return an access token for user " + sUserId);
				return false;
			}
		} catch (Exception oEx) {
			WasdiLog.debugLog("KeycloakUtils.updatePassword: could not parse keycloak login response for user " + sUserId + ": " + oEx);
			return false;
		}

		String sBaseUrl = WasdiConfig.Current.keycloack.address;
		if (!sBaseUrl.endsWith("/")) {
			sBaseUrl += "/";
		}
		String sRealm = WasdiConfig.Current.keycloack.realm;
		if (Utils.isNullOrEmpty(sRealm)) {
			sRealm = "wasdi";
		}
		String sUrl = sBaseUrl + "realms/" + sRealm + "/account/credentials/password";

		JSONObject oPayload = new JSONObject();
		oPayload.put("currentPassword", sCurrentPassword);
		oPayload.put("newPassword", sNewPassword);
		oPayload.put("confirmation", sNewPassword);

		try {
			Map<String, String> asHeaders = new HashMap<>();
			asHeaders.put("Authorization", "Bearer " + sUserAccessToken);
			asHeaders.put("Content-Type", "application/json");
			HttpCallResponse oResponse = HttpUtils.httpPutResponse(sUrl, oPayload.toString(), asHeaders);
			boolean bSuccess = oResponse.getResponseCode() != null
					&& oResponse.getResponseCode() >= 200
					&& oResponse.getResponseCode() <= 299;
			if (bSuccess) {
				WasdiLog.infoLog("KeycloakUtils.updatePassword: password updated in Keycloak for user " + sUserId);
				return true;
			}

			WasdiLog.errorLog("KeycloakUtils.updatePassword: Keycloak password update failed for user " + sUserId
					+ ", response code: " + oResponse.getResponseCode() + ", body: " + oResponse.getResponseBody());
			return false;
		} catch (Exception oEx) {
			WasdiLog.errorLog("KeycloakUtils.updatePassword: exception while updating Keycloak password for user " + sUserId, oEx);
			return false;
		}
	}
	
	public static PrimitiveResult deleteUser(String sUserId) {
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		oResult.setIntValue(500);
		
		if (WasdiConfig.Current.keycloack == null) {
			WasdiLog.debugLog("KeycloakUtils.deleteUser: Keycloak config is null, skipping Keycloak cleanup");
			return oResult;
		}
		
		if (Utils.isNullOrEmpty(sUserId)) {
			WasdiLog.debugLog("KeycloakUtils.deleteUser: user id null or empty, aborting");
			return oResult;
		}
		
		String sUserDbId = getUserDbId(sUserId);
		if (Utils.isNullOrEmpty(sUserDbId)) {
			WasdiLog.warnLog("KeycloakUtils.deleteUser: user not found in Keycloak for userId " + sUserId + ", skipping Keycloak deletion");
			oResult.setBoolValue(true);
			oResult.setIntValue(200);
			return oResult;
		}
		
		String sBaseUrl = WasdiConfig.Current.keycloack.address;
		if (!sBaseUrl.endsWith("/")) {
			sBaseUrl += "/";
		}
		String sUrl = sBaseUrl + "admin/realms/wasdi/users/" + sUserDbId;
		
		try {
			String sToken = getToken();
			if (Utils.isNullOrEmpty(sToken)) {
				WasdiLog.errorLog("KeycloakUtils.deleteUser: could not get Keycloak admin token for userId " + sUserId);
				return oResult;
			}
			Map<String, String> asHeaders = new HashMap<>();
			asHeaders.put("Authorization", "Bearer " + sToken);
			HttpCallResponse oHttpCallResponse = HttpUtils.httpDelete(sUrl, asHeaders);
			if (oHttpCallResponse != null && oHttpCallResponse.getResponseCode() != null && oHttpCallResponse.getResponseCode() >= 200 && oHttpCallResponse.getResponseCode() <= 299) {
				oResult.setBoolValue(true);
				oResult.setIntValue(200);
				WasdiLog.infoLog("KeycloakUtils.deleteUser: user removed from Keycloak: " + sUserId);
				return oResult;
			}
			WasdiLog.errorLog("KeycloakUtils.deleteUser: Keycloak delete failed for userId " + sUserId + ", response code: " + (oHttpCallResponse != null ? oHttpCallResponse.getResponseCode() : "unknown") + ", body: " + (oHttpCallResponse != null ? oHttpCallResponse.getResponseBody() : ""));
			return oResult;
		} catch (Exception oEx) {
			WasdiLog.errorLog("KeycloakUtils.deleteUser: exception while deleting user from Keycloak: " + sUserId, oEx);
			return oResult;
		}
	}
	

	public static User getUser(String sUserId) {
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
	
	
	public static boolean logout(String sSessionId) {
		
		if (WasdiConfig.Current.keycloack==null) return true;
		
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

	/**
	 * Locally verifies a Keycloak-issued JWT using the realm JWKS cache.
	 *
	 * @param sJwtToken Keycloak access token
	 * @return the WASDI user ID in the token, or an empty string if validation fails
	 */
	public static String validateJwtAndGetUserId(String sJwtToken) {
		try {
			if (Utils.isNullOrEmpty(sJwtToken) || WasdiConfig.Current.keycloack == null) {
				return "";
			}

			KeycloackConfig oKeycloakConfig = WasdiConfig.Current.keycloack;
			ConfigurableJWTProcessor<SecurityContext> oJwtProcessor = getJwtProcessor(oKeycloakConfig);
			if (oJwtProcessor == null) {
				return "";
			}

			JWTClaimsSet oClaims = oJwtProcessor.process(sJwtToken, null);
			if (!isTokenForConfiguredClient(oClaims, oKeycloakConfig.client)) {
				WasdiLog.warnLog("KeycloakUtils.validateJwtAndGetUserId: token is not intended for the configured client");
				return "";
			}

			String sUserId = oClaims.getStringClaim("preferred_username");
			return Utils.isNullOrEmpty(sUserId) ? "" : sUserId;
		} catch (BadJOSEException | JOSEException oEx) {
			WasdiLog.warnLog("KeycloakUtils.validateJwtAndGetUserId: invalid JWT: " + oEx.getMessage());
		} catch (Exception oEx) {
			WasdiLog.warnLog("KeycloakUtils.validateJwtAndGetUserId: " + oEx);
		}

		return "";
	}

	private static synchronized ConfigurableJWTProcessor<SecurityContext> getJwtProcessor(KeycloackConfig oKeycloakConfig) {
		String sIssuer = getIssuer(oKeycloakConfig);
		String sJwksUrl = sIssuer + "/protocol/openid-connect/certs";
		String sConfigurationKey = sIssuer + "|" + oKeycloakConfig.client;

		if (s_oJwtProcessor != null && sConfigurationKey.equals(s_sJwtConfigurationKey)) {
			return s_oJwtProcessor;
		}

		try {
			JWKSource<SecurityContext> oJwkSource = JWKSourceBuilder.<SecurityContext>create(new URL(sJwksUrl)).cache(true).build();
			JWSKeySelector<SecurityContext> oKeySelector = new JWSAlgorithmFamilyJWSKeySelector<>(JWSAlgorithm.Family.RSA, oJwkSource);

			ConfigurableJWTProcessor<SecurityContext> oJwtProcessor = new DefaultJWTProcessor<>();
			oJwtProcessor.setJWSKeySelector(oKeySelector);
			oJwtProcessor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(new JWTClaimsSet.Builder().issuer(sIssuer).build(), null));

			s_oJwtProcessor = oJwtProcessor;
			s_sJwtConfigurationKey = sConfigurationKey;
			return s_oJwtProcessor;
		} catch (Exception oEx) {
			WasdiLog.errorLog("KeycloakUtils.getJwtProcessor: " + oEx);
			return null;
		}
	}

	private static String getIssuer(KeycloackConfig oKeycloakConfig) {
		String sKeycloakAddress = oKeycloakConfig.address;
		if (sKeycloakAddress.endsWith("/")) {
			sKeycloakAddress = sKeycloakAddress.substring(0, sKeycloakAddress.length() - 1);
		}
		return sKeycloakAddress + "/realms/" + oKeycloakConfig.realm;
	}

	private static boolean isTokenForConfiguredClient(JWTClaimsSet oClaims, String sClient) throws java.text.ParseException {
		List<String> asAudience = oClaims.getAudience();
		if (asAudience != null && asAudience.contains(sClient)) {
			return true;
		}
		return sClient.equals(oClaims.getStringClaim("azp"));
	}

}
