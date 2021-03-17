package it.fadeout.services;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;

import org.json.JSONObject;

import it.fadeout.Wasdi;
import wasdi.shared.utils.Utils;

public class KeycloakService implements AuthProviderService {

	@Context
	ServletConfig m_oServletConfig;

	private static final String s_sACCESS_TOKEN = "access_token";

	//just to hide the implicit one
	//private KeycloakService() {}

	public String getToken() {
		try {
			String sAuthUrl = m_oServletConfig.getInitParameter("keycloak_server");
			String sCliSecret = m_oServletConfig.getInitParameter("keycloak_CLI_Secret");
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

	public String getUserData(String sKcTokenId, String sUserId) {
		// build keycloak API URL
		String sUrl = m_oServletConfig.getInitParameter("keycloak_server");
		if(!sUrl.endsWith("/")) {
			sUrl += "/";
		}
		sUrl += "admin/realms/wasdi/users?username=";
		sUrl += sUserId;
		Utils.debugLog("KeycloakService.userRegistration: about to GET to " + sUrl);
		Map<String, String> asHeaders = new HashMap<>();
		asHeaders.clear();
		asHeaders.put("Authorization", "Bearer " + sKcTokenId);
		return Wasdi.httpGet(sUrl, asHeaders);
	}

	@Override
	public String login(String sUser, String sPassword) {
		//authenticate against keycloak
		String sUrl = m_oServletConfig.getInitParameter("keycloak_auth");

		String sPayload = "client_id=";
		sPayload += m_oServletConfig.getInitParameter("keycloak_confidentialClient");
		sPayload += "&client_secret=" + m_oServletConfig.getInitParameter("keycloak_clientSecret");
		sPayload += "&grant_type=password&username=" + sUser;
		sPayload += "&password=" + sPassword;
		Map<String, String> asHeaders = new HashMap<>();
		asHeaders.put("Content-Type", "application/x-www-form-urlencoded");
		String sAuthResult = Wasdi.httpPost(sUrl, sPayload, asHeaders);

		return sAuthResult;
	}
}
