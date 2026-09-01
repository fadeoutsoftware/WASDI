package it.fadeout.rest.resources;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.json.JSONObject;

import io.swagger.v3.oas.annotations.Operation;
import it.fadeout.Wasdi;
import wasdi.shared.business.PasswordAuthentication;
import wasdi.shared.business.Project;
import wasdi.shared.business.Subscription;
import wasdi.shared.business.missions.ClientConfig;
import wasdi.shared.business.missions.Mission;
import wasdi.shared.business.users.User;
import wasdi.shared.business.users.UserApplicationRole;
import wasdi.shared.business.users.UserResourcePermission;
import wasdi.shared.business.users.UserSession;
import wasdi.shared.business.users.UserType;
import wasdi.shared.config.SkinConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ProjectRepository;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.data.SubscriptionRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.data.missions.MissionsRepository;
import wasdi.shared.utils.CredentialPolicy;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.MailUtils;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.auth.KeycloakUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.missions.PrivateMissionViewModel;
import wasdi.shared.viewmodels.organizations.SubscriptionType;
import wasdi.shared.viewmodels.users.ChangeUserPasswordViewModel;
import wasdi.shared.viewmodels.users.LoginInfo;
import wasdi.shared.viewmodels.users.RegistrationInfoViewModel;
import wasdi.shared.viewmodels.users.RefreshTokenViewModel;
import wasdi.shared.viewmodels.users.SkinViewModel;
import wasdi.shared.viewmodels.users.UserViewModel;

/**
 * Authorization Resource.
 * 
 * Hosts the API for:
 * 	.User login management
 *  .Sessions
 *  .User sftp accounts
 *  .User registration
 *  
 *  Exposes:
 *  
 *  /config
 *  /lostPassword
 *  /changePassword
 *  /editUserDetails
 *  /validateNewUser
 *  /register
 *  /upload/updatepassword
 *  /upload/removeaccount
 *  /upload/list
 *  /upload/existsaccount
 *  /upload/createaccount
 *  /logout
 *  /checksession
 *  /login
 *  
 * @author p.campanella
 *
 */
@Path("/auth")
public class AuthResource {
	
	/**
	 * Authentication Helper
	 */
	PasswordAuthentication m_oPasswordAuthentication = new PasswordAuthentication();

	/**
	 * Credential Policy
	 */
	CredentialPolicy m_oCredentialPolicy = new CredentialPolicy();
		
	/**
	 * Login API
	 * The system will try to login with Keycloak first. Then with the old WASDI login.
	 * 
	 * @param oLoginInfo LoginInfo object. View model with info to login
	 * @return UserViewModel View Model of the user logged. Can be invalid if noit logged
	 */
	@POST
	@Path("/login")
	@Produces({"application/xml", "application/json", "text/xml"})
	@Operation(summary = "Authenticate user with credentials", description="Authenticates a user with their credentials. The system tries Keycloak first and falls back to the legacy WASDI password store. On first login of a Keycloak-verified user, their account is automatically registered in WASDI. On success, a new session token is created and returned inside the UserViewModel. Note: On failure, returns an invalid UserViewModel (userId empty, boolValue false) rather than an HTTP error code. If disableAuthentication is set in server config, a default admin user is returned without credential checks (development only).")
	public UserViewModel login(LoginInfo oLoginInfo) {

		try {
			if (WasdiConfig.Current.disableAuthentication) {
				WasdiLog.warnLog("AuthResource.login: auth disabled, return default user");
				
				//populate view model
				UserViewModel oUserVM = new UserViewModel();
				oUserVM.setName("user");
				oUserVM.setSurname("");
				oUserVM.setUserId("user");
				oUserVM.setAuthProvider("wasdi");
				oUserVM.setType(UserType.PROFESSIONAL.name());
				oUserVM.setPublicNickName("user");
				oUserVM.setSkin("wasdi");
				oUserVM.setRole(UserApplicationRole.ADMIN.getRole());
				oUserVM.setLastWorkspace("");
				
				// Create a new session
				SessionRepository oSessionRepository = new SessionRepository();
				UserSession oSession = oSessionRepository.insertUniqueSession(oUserVM.getUserId());
				
				if(null==oSession || Utils.isNullOrEmpty(oSession.getSessionId())) {
					WasdiLog.debugLog("AuthResource.login: could not insert session in DB, aborting");
					return UserViewModel.getInvalid();
				}
				
				oUserVM.setSessionId(oSession.getSessionId());
				

				WasdiLog.debugLog("AuthResource.login: access succeeded, sSessionId: "+oSession.getSessionId());
				
				return oUserVM;
				
			}			
			// Validate inputs
			if (oLoginInfo == null) {
				WasdiLog.warnLog("AuthResource.login: login info null, user not authenticated");
				return UserViewModel.getInvalid();
			}
			
			if(Utils.isNullOrEmpty(oLoginInfo.getUserId())){
				WasdiLog.warnLog("AuthResource.login: userId null or empty, user not authenticated");
				return UserViewModel.getInvalid();	
			}
			
			if(Utils.isNullOrEmpty(oLoginInfo.getUserPassword())){
				WasdiLog.warnLog("AuthResource.login: password null or empty, user not authenticated");
				return UserViewModel.getInvalid();	
			}

			WasdiLog.debugLog("AuthResource.login: requested access from " + oLoginInfo.getUserId());
			
			// Check if the user exists
			UserRepository oUserRepository = new UserRepository();
			String sLowerCaseUserId = oLoginInfo.getUserId().toLowerCase();
			WasdiLog.debugLog("AuthResource.login: user id forced to be lower case: " + sLowerCaseUserId);
			User oUser = oUserRepository.getUser(sLowerCaseUserId);
			
			if( oUser == null ) {
				// User not in the wasdi db
				WasdiLog.debugLog("AuthResource.login: user not found: " + sLowerCaseUserId + ", check if this is the first access");
				
				// Try to retrieve info about this user 
				String sUserInfo = KeycloakUtils.getUserData(KeycloakUtils.getToken(), sLowerCaseUserId);
				
				if (Utils.isNullOrEmpty(sUserInfo)) {
					// No, something did not work well
					WasdiLog.warnLog("AuthResource.login: user not found in keycloak, return invalid");
					return UserViewModel.getInvalid();
				}
				
				// Convert the json to a map: here we have a list
				List<Map<String, Object>> aoKeyCloakUsers = JsonUtils.jsonToListOfMapOfObjects(sUserInfo);
				
				if (aoKeyCloakUsers == null) {
					// No, something did not work well
					WasdiLog.warnLog("AuthResource.login: user not found in keycloak, return invalid");
					return UserViewModel.getInvalid();					
				}
				
				if (aoKeyCloakUsers.size()<=0) {
					// No, something did not work well
					WasdiLog.warnLog("AuthResource.login: user not found in keycloak, return invalid");
					return UserViewModel.getInvalid();					
				}
				
				Boolean bMailVerified = (Boolean) JsonUtils.getProperty(aoKeyCloakUsers.get(0), "emailVerified");
				
				if (bMailVerified == null) {
					// No, something did not work well
					WasdiLog.warnLog("AuthResource.login: user not found in keycloak, return invalid");
					return UserViewModel.getInvalid();
				}
				
				if (!bMailVerified) {
					// The user exists but did not verify the mail yet
					WasdiLog.warnLog("AuthResource.login: user found in keycloak, but the mail is still not verified, return invalid");
					return UserViewModel.getInvalid();
				}
				else {
					WasdiLog.debugLog("AuthResource.login: user found in keycloak and mail verified: we can register the new user!!");
					
					RegistrationInfoViewModel oRegistrationInfoViewModel = new RegistrationInfoViewModel();
					oRegistrationInfoViewModel.setUserId(sLowerCaseUserId);
					PrimitiveResult oRegistrationResult = this.userRegistration(oRegistrationInfoViewModel);

					if (oRegistrationResult==null) {
						WasdiLog.warnLog("AuthResource.login: we had a problem registering the user, return invalid");
						return UserViewModel.getInvalid();						
					}
					
					if (oRegistrationResult.getBoolValue()==null) {
						WasdiLog.warnLog("AuthResource.login: we had a problem registering the user, return invalid");
						return UserViewModel.getInvalid();						
					}

					if (oRegistrationResult.getBoolValue()==false) {
						WasdiLog.warnLog("AuthResource.login: we had a problem registering the user, return invalid");
						return UserViewModel.getInvalid();						
					}
				}
				
				// Read again the user to proceed
				oUser = oUserRepository.getUser(sLowerCaseUserId);
				
				if (oUser==null) {
					WasdiLog.warnLog("AuthResource.login: we had a problem reading again the user in the db after registration, return invalid");
					return UserViewModel.getInvalid();						
				}				
			}

			if(oUser.getValidAfterFirstAccess() == null) {
				// this is to fix legacy users for which confirmation has never been activated
				WasdiLog.debugLog("AuthResource.login: hotfix: legacy wasdi user " + oUser.getUserId() + " did not have the 'valid after first access' flag, setting its value to true");
				oUser.setValidAfterFirstAccess(true);
			}

			// First try to Authenticate using keycloak
			String sAuthResult = KeycloakUtils.login(sLowerCaseUserId, oLoginInfo.getUserPassword());
			
			boolean bLoginSuccess = false;
			JSONObject oKeycloakLoginResponse = getKeycloakLoginResponse(sAuthResult);

			if(oKeycloakLoginResponse != null) {
				bLoginSuccess = true;
			} 
			else {
				// Try to log in with the WASDI old password
				bLoginSuccess = m_oPasswordAuthentication.authenticate(oLoginInfo.getUserPassword().toCharArray(), oUser.getPassword() );
			}
			
			if(bLoginSuccess) {
				// If the user is logged, update last login
				oUser.setLastLogin((new Date()).toString());
				oUserRepository.updateUser(oUser);
				
				//Clear all old, expired sessions
				Wasdi.clearUserExpiredSessions(oUser);
				
				//populate view model
				UserViewModel oUserVM = new UserViewModel();
				oUserVM.setName(oUser.getName());
				oUserVM.setSurname(oUser.getSurname());
				oUserVM.setUserId(oUser.getUserId());
				oUserVM.setAuthProvider(oUser.getAuthServiceProvider());
				oUserVM.setType(PermissionsUtils.getUserType(oUser));
				oUserVM.setPublicNickName(oUser.getPublicNickName());
				oUserVM.setSkin(oUser.getSkin());
				if (Utils.isNullOrEmpty(oUserVM.getPublicNickName())) {
					String sPublicNick = oUserVM.getName();
					oUserVM.setPublicNickName(sPublicNick);
				}				

				if (oUser.getRole() != null) {
					oUserVM.setRole(oUser.getRole());
				}
				else {
					oUserVM.setRole(UserApplicationRole.USER.getRole());
				}
				
				oUserVM.setLastWorkspace(oUser.getLastWorkspace());
				if (oKeycloakLoginResponse != null) {
					oUserVM.setAccessToken(oKeycloakLoginResponse.optString("access_token", ""));
					oUserVM.setRefreshToken(oKeycloakLoginResponse.optString("refresh_token", ""));
					oUserVM.setExpiresIn(oKeycloakLoginResponse.optInt("expires_in", 0));
					WasdiLog.debugLog("AuthResource.login: Keycloak access succeeded");
				} else {
					SessionRepository oSessionRepository = new SessionRepository();
					UserSession oSession = oSessionRepository.insertUniqueSession(oUser.getUserId());
					if(null==oSession || Utils.isNullOrEmpty(oSession.getSessionId())) {
						WasdiLog.debugLog("AuthResource.login: could not insert session in DB, aborting");
						return UserViewModel.getInvalid();
					}
					oUserVM.setSessionId(oSession.getSessionId());
					WasdiLog.debugLog("AuthResource.login: legacy access succeeded, sSessionId: "+oSession.getSessionId());
				}
				
				return oUserVM;
			} else {
				WasdiLog.debugLog("AuthResource.login: access failed");
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("AuthResource.login: " + oEx);
		}

		return UserViewModel.getInvalid();
	}

	/**
	 * Refresh a Keycloak access token without exposing the confidential-client secret.
	 *
	 * @param oRefreshRequest request containing a Keycloak refresh token
	 * @return refreshed access and refresh tokens, or an invalid user view model
	 */
	@POST
	@Path("/refresh")
	@Produces({"application/xml", "application/json", "text/xml"})
	@Operation(summary = "Refresh Keycloak tokens", description = "Exchanges a Keycloak refresh token for a new access-token pair using WASDI's confidential client credentials. Returns an invalid UserViewModel when the refresh token is expired or invalid.")
	public UserViewModel refresh(RefreshTokenViewModel oRefreshRequest) {
		try {
			if (oRefreshRequest == null) {
				return UserViewModel.getInvalid();
			}

			String sRefreshToken = oRefreshRequest.getRefreshToken();
			String sAuthResult = KeycloakUtils.refreshToken(sRefreshToken);
			JSONObject oKeycloakRefreshResponse = getKeycloakLoginResponse(sAuthResult);
			if (oKeycloakRefreshResponse == null) {
				return UserViewModel.getInvalid();
			}

			String sAccessToken = oKeycloakRefreshResponse.optString("access_token", "");
			String sUserId = KeycloakUtils.validateJwtAndGetUserId(sAccessToken);
			if (Utils.isNullOrEmpty(sUserId)) {
				return UserViewModel.getInvalid();
			}

			User oUser = new UserRepository().getUser(sUserId);
			if (oUser == null) {
				return UserViewModel.getInvalid();
			}

			UserViewModel oUserVM = new UserViewModel();
			oUserVM.setUserId(oUser.getUserId());
			oUserVM.setAccessToken(sAccessToken);
			oUserVM.setRefreshToken(oKeycloakRefreshResponse.optString("refresh_token", ""));
			oUserVM.setExpiresIn(oKeycloakRefreshResponse.optInt("expires_in", 0));
			return oUserVM;
		} catch (Exception oEx) {
			WasdiLog.warnLog("AuthResource.refresh: " + oEx);
		}

		return UserViewModel.getInvalid();
	}

	protected JSONObject getKeycloakLoginResponse(String sAuthResult) {
		if (Utils.isNullOrEmpty(sAuthResult)) {
			return null;
		}

		try {
			JSONObject oAuthResponse = new JSONObject(sAuthResult);
			String sAccessToken = oAuthResponse.optString("access_token", "");
			String sRefreshToken = oAuthResponse.optString("refresh_token", "");
			if (!Utils.isNullOrEmpty(sAccessToken) && !Utils.isNullOrEmpty(sRefreshToken)) {
				return oAuthResponse;
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("AuthResource.getKeycloakLoginResponse: could not parse login response: " + oE);
		}

		return null;
	}
	
	/**
	 * Extracts the Refresh Token from the login response of keyCloak
	 * @param sAuthResult
	 * @return
	 */
	protected String getRefreshTokenFromLoginResponse(String sAuthResult) {
		
		if(Utils.isNullOrEmpty(sAuthResult)) {
			return "";
		}

		try {
			JSONObject oAuthResponse = new JSONObject(sAuthResult);
			
			String sRefreshToken = oAuthResponse.optString("refresh_token", null);
			return sRefreshToken;
			
		} catch (Exception oE) {
			WasdiLog.errorLog("AuthResource.getRefreshTokenFromLoginResponse: could not parse response due to " + oE + ", aborting");
		}
		return "";
	}

	/**
	 * Check user session.
	 * This check first in Keycloak and later with wasdi embedded
	 * @param sSessionId Session id to check
	 * @return User View Model associated to this session if valid, otherwise the invalid one
	 */
	@GET
	@Path("/checksession")
	@Produces({"application/xml", "application/json", "text/xml"})
	@Operation(summary = "Validate an existing session token", description="Validates an existing session token and returns the user profile associated with it. Used by the client to verify that a stored session is still active. Returns an invalid UserViewModel (userId empty) when the session is not valid rather than an HTTP error code.")
	public UserViewModel checkSession(@Context ContainerRequestContext oRequestContext) {
		try {
			// Check if we can see the user from the session
			User oUser = (User) oRequestContext.getProperty("authenticated-user");
			
			if (oUser == null) {
				WasdiLog.debugLog("AuthResource.checkSession: invalid session");
				return UserViewModel.getInvalid();
			}

			// Ok session is valid
			UserViewModel oUserVM = new UserViewModel();
			oUserVM.setName(oUser.getName());
			oUserVM.setSurname(oUser.getSurname());
			oUserVM.setUserId(oUser.getUserId());
			oUserVM.setType(PermissionsUtils.getUserType(oUser));
			oUserVM.setPublicNickName(oUser.getPublicNickName());
			oUserVM.setSkin(oUser.getSkin());
			
			if (Utils.isNullOrEmpty(oUserVM.getPublicNickName())) {
				String sPublicNick = oUserVM.getName();
				oUserVM.setPublicNickName(sPublicNick);
			}	
			
			oUserVM.setLastWorkspace(oUser.getLastWorkspace());
			
			return oUserVM;
		} catch (Exception oE) {
			WasdiLog.errorLog("AuthResource.checkSession: " + oE);
		}
		return UserViewModel.getInvalid();
	}	
	
	/**
	 * Log out a user
	 * @param sSessionId Session id to logout
	 * @return Primitive Result with boolValue = true if logout is ok, false otherwise
	 */
	@GET
	@Path("/logout")
	@Produces({"application/xml", "application/json", "text/xml"})
	@Operation(summary = "Invalidate the current session", description="Invalidates the given session, deleting the session record from the database. Returns a PrimitiveResult indicating whether the operation succeeded. Returns an invalid PrimitiveResult when the session is not found.")
	public PrimitiveResult logout(@Context ContainerRequestContext oRequestContext) {
		WasdiLog.debugLog("AuthResource.logout");
		
		// Try to get the user
		User oUser = (User) oRequestContext.getProperty("authenticated-user");

		if (oUser == null) {
			return PrimitiveResult.getInvalid();
		}
		
		String sSessionId = (String) oRequestContext.getProperty("session-id");

		PrimitiveResult oResult = null;
		
		// Check if we need to delete the WASDI session
		SessionRepository oSessionRepository = new SessionRepository();
		UserSession oSession = oSessionRepository.getSession(sSessionId);
		if(oSession != null) {
			oResult = new PrimitiveResult();
			oResult.setStringValue(sSessionId);
			if(oSessionRepository.deleteSession(oSession)) {

				WasdiLog.debugLog("AuthResource.logout: Session data base deleted.");
				oResult.setBoolValue(true);
			} else {

				WasdiLog.debugLog("AuthResource.logout: Error deleting session data base.");
				oResult.setBoolValue(false);
			}

		}
		else {
			return PrimitiveResult.getInvalid();
		}
		return oResult;
	}	

	/**
	 * Register a new user
	 * @param oRegistrationInfoViewModel Registration Informations
	 * @return Primitive Result: if all is ok it has boolValue = true, intValue = 200 and a welcome message in stringValue. Otherwise it has in intValue the http error code. 
	 */
	@POST
	@Path("/register")
	@Produces({"application/json", "text/xml"})
	@Operation(summary = "Register a new WASDI user", description="Registers a new WASDI user. The userId (e-mail) must exist and be verified in Keycloak; the endpoint creates the user record in the WASDI database and automatically assigns a 90-day FREE trial subscription. Returns PrimitiveResult with boolValue=true and intValue=200 on success, or intValue=304 if already registered, 400 for bad request, 404 if user not found in Keycloak, or 500 on server error.")
	public PrimitiveResult userRegistration(RegistrationInfoViewModel oRegistrationInfoViewModel) 
	{
		try{
			WasdiLog.debugLog("AuthResource.userRegistration"); 

			//filter bad cases out
			if(null == oRegistrationInfoViewModel) {
				WasdiLog.debugLog("AuthResource.userRegistration: view model is null");
				PrimitiveResult oPrimitiveResult = new PrimitiveResult();
				oPrimitiveResult.setIntValue(400);
				return oPrimitiveResult;
			}
			
			if(Utils.isNullOrEmpty(oRegistrationInfoViewModel.getUserId())) {
				WasdiLog.debugLog("AuthResource.userRegistration: userid in view model is null");
				PrimitiveResult oPrimitiveResult = new PrimitiveResult();
				oPrimitiveResult.setIntValue(400);
				return oPrimitiveResult;
			}

			WasdiLog.debugLog("AuthResource.userRegistration: checking if " + oRegistrationInfoViewModel.getUserId() + " is already in wasdi ");
			UserRepository oUserRepository = new UserRepository();
			// user id should be unique, independently from the upper and lower case letters they use
			String sLowerCasedUserId = oRegistrationInfoViewModel.getUserId().toLowerCase();
			User oWasdiUser = oUserRepository.getUser(sLowerCasedUserId);

			//do we already have this user in our DB?
			if(oWasdiUser != null){
				//yes, it's a well known user. Stop here
				PrimitiveResult oResult = new PrimitiveResult();
				//not modified
				oResult.setIntValue(304);
				WasdiLog.debugLog("AuthResource.userRegistration: " + oRegistrationInfoViewModel.getUserId() + " already in wasdi");
				return oResult;
			} 
			else {
				WasdiLog.debugLog("AuthResource.userRegistration: " + oRegistrationInfoViewModel.getUserId() + " is a new user");
				//no, it's a new user! :)
				//let's check it's a legit one (against kc)  
				//otherwise someone might call this api even if the user is not registered on KC
				
				User oNewUser = KeycloakUtils.getUser(sLowerCasedUserId);
				
				if(null==oNewUser) {
					PrimitiveResult oResult = new PrimitiveResult();
					//not found
					oResult.setIntValue(404);
					WasdiLog.debugLog("AuthResource.userRegistration: " + oRegistrationInfoViewModel.getUserId() + " not found in keycloak, aborting");
					return oResult;
				}
				
				//populate remaining fields
				oNewUser.setValidAfterFirstAccess(true);
				oNewUser.setAuthServiceProvider("keycloak");
				WasdiLog.debugLog("AuthResource.userRegistration: user details parsed");
				
				String sDefaultNode = "wasdi";
				try {					
					sDefaultNode = WasdiConfig.Current.usersDefaultNode;
					if (Utils.isNullOrEmpty(sDefaultNode)) {
						sDefaultNode = "wasdi";
					}
				}
				catch (Exception oEx) {
					WasdiLog.errorLog("AuthResource.userRegistration: Exception reading Users default node " + oEx);
				}
				
				oNewUser.setDefaultNode(sDefaultNode);
				oNewUser.setSkin(WasdiConfig.Current.defaultSkin);
				

				//store user in DB
				if(oUserRepository.insertUser(oNewUser)) {
					//success: the user is stored in DB!
					WasdiLog.debugLog("AuthResource.userRegistration: user " + oNewUser.getUserId() + " added to wasdi");
					
					try {
						createFirstSubscription(oNewUser);
					}
					catch (Exception oEx) {
						WasdiLog.debugLog("AuthResource.userRegistration: error in   createFirstSubscription " + oEx.toString());
					}
					
					notifyNewUserInWasdi(oNewUser, true);
					PrimitiveResult oResult = new PrimitiveResult();
					oResult.setBoolValue(true);					
					oResult.setIntValue(200);
					oResult.setStringValue("Welcome to space");
					return oResult;
				} else {
					//insert failed: log, mail and throw
					String sMessage = "could not insert new user " + oNewUser.getUserId() + " in DB";
					WasdiLog.debugLog("AuthResource.userRegistration: " + sMessage + ", aborting");
					notifyNewUserInWasdi(oNewUser, false);
					throw new RuntimeException(sMessage);
				}
			}
		} 
		catch(Exception oE) {
			WasdiLog.errorLog("AuthResource.userRegistration: " + oE + ", aborting");
		}

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		oResult.setIntValue(500);
		return oResult;
	}


	/**
	 * Validate the user registration: this is the landing of verification link
	 * @param sUserId User Id
	 * @param sToken Validation Token
	 * @return Primitive Result with boolValue = true and stringValue = user id if all is ok
	 */
	@GET
	@Path("/validateNewUser")
	@Produces({"application/xml", "application/json", "text/xml"})
	@Operation(summary = "Complete email-based account activation", description="Completes the legacy e-mail-based account activation flow. The link embedded in the confirmation e-mail points to this endpoint. When the validation code matches the stored token the user account is activated and a FREE trial subscription is created. Returns PrimitiveResult with boolValue=true and stringValue=userId on success, or invalid result on validation failure.")
	public PrimitiveResult validateNewUser(@QueryParam("email") String sUserId, @QueryParam("validationCode") String sToken  ) {
		WasdiLog.debugLog("AuthResource.validateNewUser UserId: " + sUserId + " Token: " + sToken);

		String sLowerCaseUser = sUserId.toLowerCase();
		
		if(! (m_oCredentialPolicy.validUserId(sUserId) && m_oCredentialPolicy.validEmail(sLowerCaseUser)) ) {
			WasdiLog.debugLog("AuthResources.validateNewUser: invalid userId");
			return PrimitiveResult.getInvalid();
		}
		
		if(!m_oCredentialPolicy.validFirstAccessUUID(sToken)) {
			WasdiLog.debugLog("AuthResources.validateNewUser: invalid token");
			return PrimitiveResult.getInvalid();
		}

		UserRepository oUserRepo = new UserRepository();
		User oUser = oUserRepo.getUser(sLowerCaseUser);
		if( null == oUser.getValidAfterFirstAccess()) {
			WasdiLog.debugLog("AuthResources.validateNewUser: unexpected null first access validation flag");
			return PrimitiveResult.getInvalid();
		} 
		else if( oUser.getValidAfterFirstAccess() ) {
			WasdiLog.debugLog("AuthResources.validateNewUser: unexpected true first access validation flag");
			return PrimitiveResult.getInvalid();
		} 
		else if( !oUser.getValidAfterFirstAccess() ) {

			String sDBToken = oUser.getFirstAccessUUID();

			if(m_oCredentialPolicy.validFirstAccessUUID(sToken)) {
				if(sDBToken.equals(sToken)) {
					oUser.setValidAfterFirstAccess(true);
					oUser.setConfirmationDate( (new Date()).toString() );
					oUserRepo.updateUser(oUser);
					PrimitiveResult oResult = new PrimitiveResult();
					oResult.setBoolValue(true);
					oResult.setStringValue(oUser.getUserId());

					notifyNewUserInWasdi(oUser, true);
					
					createFirstSubscription(oUser);

					return oResult;
				} else {
					WasdiLog.debugLog("AuthResources.validateNewUser: registration token mismatch");
					PrimitiveResult.getInvalid();
				}
			}
		}
		return PrimitiveResult.getInvalid();
	}
	
	/**
	 * Creates the first FREE Subscription for the actual User
	 * @param oUser
	 */
	private void createFirstSubscription(User oUser) {
		try {
			Subscription oSubscription = new Subscription();
			
			oSubscription.setType(SubscriptionType.Free.getTypeName());
			oSubscription.setBuyDate(null);
			oSubscription.setUserId(oUser.getUserId());
			oSubscription.setSubscriptionId(Utils.getRandomName());
			oSubscription.setName("WASDI Trial");
			oSubscription.setBuySuccess(true);
			oSubscription.setBuyDate(Utils.getDateAsDouble(new Date()));
			oSubscription.setDescription("WASDI Trial");
			oSubscription.setDurationDays(90);
			double dStartDate = Utils.getDateAsDouble(new Date());
			oSubscription.setStartDate(dStartDate);
			double dEndDate = dStartDate + 90.0*24.0*60.0*60.0*1000.0;
			oSubscription.setEndDate(dEndDate);
			
			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
			oSubscriptionRepository.insertSubscription(oSubscription);
			
			Project oProject = new Project();
			oProject.setDescription("WASDI Trial");
			oProject.setName("WASDI Trial");
			oProject.setSubscriptionId(oSubscription.getSubscriptionId());
			oProject.setProjectId(Utils.getRandomName());
			
			ProjectRepository oProjectRepository = new  ProjectRepository();
			oProjectRepository.insertProject(oProject);
			
			UserRepository oUserRepository = new UserRepository();
			oUser.setActiveProjectId(oProject.getProjectId());
			oUser.setActiveSubscriptionId(oSubscription.getSubscriptionId());
			oUserRepository.updateUser(oUser);
			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("AuthResource.createFirstSubscription: exception " + oEx.toString());
		}
		
	}

	/**
	 * Edit user info
	 * @param sSessionId User Session
	 * @param oInputUserVM View Model of user info
	 * @return Updated User View Model
	 */
	@POST
	@Path("/editUserDetails")
	@Produces({"application/json", "text/xml"})
	@Operation(summary = "Update authenticated user profile", description="Allows an authenticated user to update their own profile fields: name, surname, link, description, and public nick name. Returns the updated UserViewModel. Returns invalid UserViewModel on validation failure or invalid session.")
	public UserViewModel editUserDetails(@Context ContainerRequestContext oRequestContext, UserViewModel oInputUserVM ) {

		WasdiLog.debugLog("AuthResource.editUserDetails");
		//note: sSessionId validity is automatically checked later
		//note: only name and surname can be changed, so far. Other fields are ignored

		if(null == oInputUserVM ) {
			WasdiLog.debugLog("AuthResource.editUserDetails: invalid User View Model");
			return UserViewModel.getInvalid();
		}
		//check only name and surname: they are the only fields that must be valid,
		//the others will typically be null, including userId
		if(!m_oCredentialPolicy.validName(oInputUserVM.getName()) || !m_oCredentialPolicy.validSurname(oInputUserVM.getSurname())) {
			WasdiLog.debugLog("AuthResource.editUserDetails: invalid user name");
			return UserViewModel.getInvalid();
		}

		try {
			//note: session validity is automatically checked		
			User oUser = (User) oRequestContext.getProperty("authenticated-user");
			if(null == oUser) {
				//Maybe the user didn't exist, or failed for some other reasons
				WasdiLog.debugLog("AuthResource.editUserDetails: invalid session");
				return UserViewModel.getInvalid();
			}
			
			String sSessionId = (String) oRequestContext.getProperty("session-id");

			//update
			oUser.setName(oInputUserVM.getName());
			oUser.setSurname(oInputUserVM.getSurname());
			oUser.setLink(oInputUserVM.getLink());
			oUser.setDescription(oInputUserVM.getDescription());

			if (oInputUserVM.getRole() != null) {
				oUser.setRole(oInputUserVM.getRole());
			}
			
			oUser.setPublicNickName(oInputUserVM.getPublicNickName());

			UserRepository oUR = new UserRepository();
			oUR.updateUser(oUser);

			//respond
			UserViewModel oOutputUserVM = new UserViewModel();
			oOutputUserVM.setUserId(oUser.getUserId());
			oOutputUserVM.setName(oUser.getName());
			oOutputUserVM.setSurname(oUser.getSurname());
			oOutputUserVM.setSessionId(sSessionId);
			oOutputUserVM.setType(PermissionsUtils.getUserType(oUser));
			
			oOutputUserVM.setPublicNickName(oUser.getPublicNickName());
			if (Utils.isNullOrEmpty(oOutputUserVM.getPublicNickName())) {
				String sPublicNick = oOutputUserVM.getName();
				oOutputUserVM.setPublicNickName(sPublicNick);
			}			
			return oOutputUserVM;

		} catch(Exception oEx) {
			WasdiLog.errorLog("AuthResource.editUserDetails: Exception " + oEx.toString());
		}
		//should not get here
		return UserViewModel.getInvalid();
	}


	/**
	 * Change WASDI Password
	 * @param sSessionId Session Id 
	 * @param oChangePasswordViewModel Change User Password View Model
	 * @return Primitive Result with boolValue = true if ok
	 */
	@POST
	@Path("/changePassword")
	@Produces({"application/json", "text/xml"})
	@Operation(summary = "Change WASDI password for authenticated user", description="Changes the WASDI password of the authenticated user. Requires the current password for verification before accepting the new one. Returns PrimitiveResult with boolValue=true on success, or invalid result on invalid session, wrong current password, or policy violation.")
	public PrimitiveResult changePassword(@Context ContainerRequestContext oRequestContext, ChangeUserPasswordViewModel oChangePasswordViewModel) {

		WasdiLog.debugLog("AuthResource.changePassword");

		//input validation
		if(null == oChangePasswordViewModel) {
			WasdiLog.debugLog("AuthResource.changePassword: ChangeUserPasswordViewModel is null, aborting");
			return PrimitiveResult.getInvalid();
		}

		if(!m_oCredentialPolicy.satisfies(oChangePasswordViewModel)) {
			WasdiLog.debugLog("AuthResource.changePassword: invalid input");
			return PrimitiveResult.getInvalid();
		}

		try {
			//validity is automatically checked		
			User oUserId = (User) oRequestContext.getProperty("authenticated-user");
			if(null == oUserId) {
				//Maybe the user didn't exist, or failed for some other reasons
				WasdiLog.debugLog("AuthResource.changePassword: invalid session");
				return PrimitiveResult.getInvalid();
			}

			String sOldPassword = oUserId.getPassword();
			boolean bPasswordCorrect = m_oPasswordAuthentication.authenticate(oChangePasswordViewModel.getCurrentPassword().toCharArray(), sOldPassword);

			if( !bPasswordCorrect ) {
				WasdiLog.debugLog("AuthResource.changePassword: Wrong current password for user " + oUserId);
				return PrimitiveResult.getInvalid();
			} else {
				//todo create new user in keycloak
				//todo set the user without need for email confirmation
				//todo set new password for newly created user in keycloak
				
				oUserId.setPassword(m_oPasswordAuthentication.hash(oChangePasswordViewModel.getNewPassword().toCharArray()));
				UserRepository oUR = new UserRepository();
				oUR.updateUser(oUserId);
				PrimitiveResult oResult = new PrimitiveResult();
				oResult.setBoolValue(true);
				return oResult;
			}
		} catch(Exception oE) {
			WasdiLog.errorLog("AuthResource.changePassword: " + oE);
		}

		return PrimitiveResult.getInvalid();

	} 	
	
	/**
	 * Recover password
	 * @param sUserId User Id
	 * @return
	 */
	@GET
	@Path("/lostPassword")
	@Produces({"application/xml", "application/json", "text/xml"})
	@Operation(summary = "Initiate password recovery flow", description="Initiates the password recovery flow. For WASDI-native accounts a new random password is generated and sent by e-mail. For Keycloak accounts a password-reset e-mail is triggered via Keycloak. Returns PrimitiveResult with boolValue=true and intValue=0 on success, or intValue=400 for bad request, or intValue=500 on server error.")
	public PrimitiveResult lostPassword(@QueryParam("userId") String sUserId ) {

		WasdiLog.debugLog("AuthResource.lostPassword: sUserId: " + sUserId);
		try {

			if(Utils.isNullOrEmpty(sUserId)) {
				WasdiLog.debugLog("AuthResource.lostPassword: User id is null or empty, aborting");
				PrimitiveResult oResult = new PrimitiveResult();
				oResult.setStringValue("Bad Request");
				oResult.setIntValue(400);
				oResult.setBoolValue(false);
				return oResult;
			}

			if(!m_oCredentialPolicy.validUserId(sUserId)) {
				WasdiLog.debugLog("AuthResource.lostPassword: User id not valid, aborting");
				PrimitiveResult oResult = new PrimitiveResult();
				oResult.setStringValue("Bad Request");
				oResult.setIntValue(400);
				oResult.setBoolValue(false);
				return oResult;
			}

		} catch (Exception oE) {
			WasdiLog.errorLog("AuthResource.lostPassword: preliminary checks broken due to: " + oE + ", aborting");
			PrimitiveResult oResult = new PrimitiveResult();
			oResult.setStringValue("Internal Server Error");
			oResult.setIntValue(500);
			oResult.setBoolValue(false);
			return oResult;
		}

		UserRepository oUserRepository = null;
		User oUser = null;
		try {
			oUserRepository = new UserRepository();
			oUser = oUserRepository.getUser(sUserId);

			if(null == oUser) {
				WasdiLog.debugLog("AuthResource.lostPassword: User not found, aborting");
				PrimitiveResult oResult = new PrimitiveResult();
				oResult.setStringValue("Bad Request");
				oResult.setIntValue(400);
				oResult.setBoolValue(false);
				return oResult;
			}
			WasdiLog.debugLog("AuthResource.lostPassword: user " + sUserId + " found");

			if(Utils.isNullOrEmpty(oUser.getAuthServiceProvider())) {
				//todo check if user is on keycloak
				WasdiLog.debugLog("AuthResource.lostPassword: auth service provider null or empty, aborting");
				PrimitiveResult oResult = new PrimitiveResult();
				oResult.setStringValue("Internal Server Error");
				oResult.setIntValue(500);
				oResult.setBoolValue(false);
				return oResult;
			}

			//now, providers!
			switch(oUser.getAuthServiceProvider().toUpperCase()) {
			case "WASDI":
				String sPassword = Utils.generateRandomPassword();
				String sHashedPassword = m_oPasswordAuthentication.hash( sPassword.toCharArray() ); 
				oUser.setPassword(sHashedPassword);

				if(oUserRepository.updateUser(oUser)) {
					if(!sendPasswordEmail(sUserId, sUserId, sPassword) ) {
						return PrimitiveResult.getInvalid(); 
					}
					PrimitiveResult oResult = new PrimitiveResult();
					oResult.setBoolValue(true);
					oResult.setIntValue(0);
					return oResult;
				}
				//else nothing is returned here and in the end 500 is returned
				break;
			case "KEYCLOAK":
				return KeycloakUtils.requirePasswordUpdateViaEmail(sUserId);
			default:
				break;
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("AuthResource.lostPassword: could not complete the password recovery due to: " + oE);
		}

		//apparently things did not work well
		WasdiLog.debugLog("AuthResource.lostPassword( " + sUserId + "): could not change user password, about to end");
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setStringValue("Internal Server Error");
		oResult.setIntValue(500);
		oResult.setBoolValue(false);
		return oResult;
	}


	/**
	 * Send the new password via mail.
	 * To send the mail uses the Mercurius service installed at CIMA.
	 * 
	 * @param sRecipientEmail Recipient of the mail, should be the WASDI user
	 * @param sAccount User Id, should be same of Recipient mail? 
	 * @param sPassword New Password
	 * @return
	 */
	private Boolean sendPasswordEmail(String sRecipientEmail, String sAccount, String sPassword) {
		WasdiLog.debugLog("AuthResource.sendPasswordEmail");
		
		if(null == sRecipientEmail || null == sPassword ) {
			WasdiLog.errorLog("AuthResource.sendPasswordEmail: null input, not enough information to send email");
			return false;
		}
		String sTitle = WasdiConfig.Current.notifications.pwRecoveryMailTitle;

		if (Utils.isNullOrEmpty(sTitle)) {
			sTitle = "WASDI Password Recovery";
		}

		String sMessage = WasdiConfig.Current.notifications.pwRecoveryMailText;

		if (Utils.isNullOrEmpty(sMessage)) {
			sMessage = "Your password has been regenerated. Please find here your new credentials:";
		}

		sMessage += "\n\nUSER: " + sAccount + " - PASSWORD: " + sPassword;

		return MailUtils.sendEmail(WasdiConfig.Current.notifications.pwRecoveryMailSender, sRecipientEmail, sTitle, sMessage);
	}
	
	/**
	 * Send the sftp password via mail
	 * @param sRecipientEmail Mail recipient
	 * @param sAccount user id
	 * @param sPassword new password
	 * @return
	 */
	private Boolean sendSftpPasswordEmail(String sRecipientEmail, String sAccount, String sPassword) {
		WasdiLog.debugLog("AuthResource.sendSftpPasswordEmail");
		if(null == sRecipientEmail || null == sPassword ) {
			WasdiLog.errorLog("AuthResource.sendSftpPasswordEmail: null input, not enough information to send email");
			return false;
		}

		String sTitle = WasdiConfig.Current.notifications.sftpMailTitle;

		if (Utils.isNullOrEmpty(sTitle)) {
			sTitle = "WASDI SFTP Account";
		}

		String sMessage = WasdiConfig.Current.notifications.sftpMailText;

		if (Utils.isNullOrEmpty(sMessage)) {
			sMessage = "Your password has been regenerated. Please find here your new credentials:";
		}

		sMessage += "\n\nUSER: " + sAccount + " - PASSWORD: " + sPassword;

		return MailUtils.sendEmail(WasdiConfig.Current.notifications.sftpManagementMailSender, sRecipientEmail, sTitle, sMessage);
	}

	/**
	 * Send a notification email to the administrators
	 * @param oUser
	 * @return
	 */
	private Boolean notifyNewUserInWasdi(User oUser, boolean bConfirmed) {

		WasdiLog.debugLog("AuthResource.notifyNewUserInWasdi");

		if (oUser == null) {
			WasdiLog.debugLog("AuthResource.notifyNewUserInWasdi: user null, return false");
			return false;
		}

		try {

			String sTitle = "New WASDI User";

			String sMessage = "A new user registered in WASDI. User Name: " + oUser.getUserId();

			if (bConfirmed) {
				sMessage = "The new User " + oUser.getUserId() + " has been added to wasdi DB"; 
			} else {
				sMessage = "Confirmation failed: " + oUser.getUserId() + " is in kc but could not be added to wasdi DB";
			}

			String sWasdiAdminMail = WasdiConfig.Current.notifications.wasdiAdminMail;

			if (Utils.isNullOrEmpty(sWasdiAdminMail)) {
				sWasdiAdminMail = "team@wasdi.cloud";
			}
			
			MailUtils.sendEmail(WasdiConfig.Current.notifications.sftpManagementMailSender, sWasdiAdminMail, sTitle, sMessage);
		} catch(Exception oEx) {
			WasdiLog.errorLog("AuthResource.notifyNewUserInWasdi error "+oEx.getMessage());
			return false;
		}
		return true;
	}
	
	@GET
	@Path("/config")
	@Produces({ "application/xml", "application/json", "text/xml" })
	@Operation(summary = "Get client UI configuration", description="Returns the client UI configuration object for the authenticated user. The configuration is resolved from the missions repository and includes data-provider settings and feature flags relevant to the user's context.")
	public Response getClientConfig(@Context ContainerRequestContext oRequestContext) {

		WasdiLog.debugLog("AuthResource.getClientConfig");

		User oUser = (User) oRequestContext.getProperty("authenticated-user");

		// Domain Check
		if (oUser == null) {
			WasdiLog.warnLog("AuthResource.getClientConfig: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();
		}		

		try {
			MissionsRepository oMissionsRepository = new MissionsRepository();
			ClientConfig oClientConfig = oMissionsRepository.getClientConfig(oUser.getUserId());

			return Response.ok(oClientConfig).build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("AuthResource.getClientConfig error: " + oEx);
			return Response.serverError().build();
		}
	}
	
	@GET
	@Path("/privatemissions")
	@Produces({ "application/xml", "application/json", "text/xml" })
	@Operation(summary = "Get private missions accessible to user", description="Returns the list of private missions accessible to the authenticated user, including missions they own and missions that have been shared with them (with their permission level).")
	public Response getPrivateMissions(@Context ContainerRequestContext oRequestContext) {
		
		WasdiLog.debugLog("AuthResource.getPrivateMissions");

		User oUser = (User) oRequestContext.getProperty("authenticated-user");

		// domain Check
		if (oUser == null) {
			WasdiLog.warnLog("AuthResource.getPrivateMissions: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();
		}		

		try {
			String sUserId = oUser.getUserId();
			MissionsRepository oMissionsRepository = new MissionsRepository();
			
			// get the missions owned by the user
			List<Mission> asMissionsOwnedByUser = oMissionsRepository.getMissionsOwnedBy(sUserId);
			List<PrivateMissionViewModel> aoPrivateMissionsList = new ArrayList<>(); 
			if (!asMissionsOwnedByUser.isEmpty()) {
				List<PrivateMissionViewModel> aoPrivateOwnedMissions = asMissionsOwnedByUser.stream()
							.map(oMission -> createPrivateMissionViewModel(oMission, sUserId))
							.collect(Collectors.toList());
				aoPrivateMissionsList.addAll(aoPrivateOwnedMissions);
			}
			
			// get the missions shared with the user (in read or write)
			UserResourcePermissionRepository oUserResourcePermissionRepo = new UserResourcePermissionRepository();
			List<UserResourcePermission> aoUserPermissionsOnMissions = oUserResourcePermissionRepo.getMissionsharingByUserId(sUserId);
			
	
			if (!aoUserPermissionsOnMissions.isEmpty()) {
				HashMap<String, String> aoMissionsIndexValuesNamesMappings = oMissionsRepository.getMissionIndexValueNameMapping();
				List<PrivateMissionViewModel>  aoPrivateMissionsSharedWithUser = aoUserPermissionsOnMissions.stream()
						.filter(oPermission -> !Utils.isNullOrEmpty(oPermission.getOwnerId()) && !oPermission.getOwnerId().equals(oPermission.getUserId()))								// first we make sure that the user is not the owner of the mission
						.map(oPermission -> createPrivateMissionViewModel(oPermission, sUserId, aoMissionsIndexValuesNamesMappings))
						.collect(Collectors.toList());
				aoPrivateMissionsList.addAll(aoPrivateMissionsSharedWithUser);
			}
			
			return Response.ok(aoPrivateMissionsList).build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("AuthResource.getPrivateMissions error: ", oEx);
			return Response.serverError().build();
		}
		
	}
	
	@GET
	@Path("/skin")
	@Operation(summary = "Get branding and UI configuration for skin", description="Returns the branding and UI configuration for the specified skin name. Used by the client to apply the correct colours, logos, and feature flags on start-up. Query parameter 'skin' is optional and defaults to the server-configured default skin.")
	public Response getSkin(@Context ContainerRequestContext oRequestContext, @QueryParam("skin") String sSkin) {
		try {
			
			if (Utils.isNullOrEmpty(sSkin)) sSkin = WasdiConfig.Current.defaultSkin;
			
			WasdiLog.debugLog("AuthResource.getSkin( skin: " + sSkin + ")");
			
			User oUser = (User) oRequestContext.getProperty("authenticated-user");

			if (oUser==null) {
				WasdiLog.warnLog("AuthResource.getSkin: invalid user or session");
				return Response.status(Status.UNAUTHORIZED).build();
			}		
			
			SkinConfig oSelectedSkin = new SkinConfig();
			
			// iterate over the list of skins to look for the one in the query
			for (SkinConfig oSkinConfig : WasdiConfig.Current.skins) {
				if (oSkinConfig.name.equals(sSkin)) {
					oSelectedSkin = oSkinConfig;
					break;
				}
			}
						
			SkinViewModel oSkinViewModel = new SkinViewModel();
			oSkinViewModel.setLogoImage(oSelectedSkin.logoImage);
			oSkinViewModel.setLogoText(oSelectedSkin.logoText);
			oSkinViewModel.setHelpLink(oSelectedSkin.helpLink);
			oSkinViewModel.setSupportLink(oSelectedSkin.supportLink);
			oSkinViewModel.setBrandMainColor(oSelectedSkin.brandMainColor);
			oSkinViewModel.setBrandSecondaryColor(oSelectedSkin.brandSecondaryColor);
			oSkinViewModel.setDefaultCategories(oSelectedSkin.defaultCategories);
			oSkinViewModel.setTabTitle(oSelectedSkin.tabTitle);
			oSkinViewModel.setActivateSubscriptions(WasdiConfig.Current.activateSubscriptionChecks);
			oSkinViewModel.setFavIcon(oSelectedSkin.favIcon);
			
			return Response.ok(oSkinViewModel).build();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("AuthResource.getSkin exception ", oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	/** 
	 * Fill the view model for the missions owned by the user
	 * @param sMissionOwnerUserId: id of the user owning a mission
	 * @return the view model representing a private mission
	 */
	private PrivateMissionViewModel createPrivateMissionViewModel(Mission oMission, String sMissionOwnerUserId) {
		PrivateMissionViewModel oPrivateMissionVM = new PrivateMissionViewModel();
		oPrivateMissionVM.setMissionName(oMission.getName());
		oPrivateMissionVM.setMissionIndexValue(oMission.getIndexvalue());
		oPrivateMissionVM.setMissionOwner(sMissionOwnerUserId);
		oPrivateMissionVM.setUserId(sMissionOwnerUserId);
		
		return oPrivateMissionVM;
	}
	
	/** 
	 * Fill the view model for the missions owned by the user
	 * @param sMissionOwnerUserId: id of the user owning a mission
	 * @return the view model representing a private mission
	 */
	private PrivateMissionViewModel createPrivateMissionViewModel(UserResourcePermission oPermission, String sUserId, HashMap<String, String> aoMissionsIndexValuesNamesMappings) {
		PrivateMissionViewModel oPrivateMissionVM = new PrivateMissionViewModel();
		oPrivateMissionVM.setMissionName(aoMissionsIndexValuesNamesMappings.getOrDefault(oPermission.getResourceId(), "null"));
		oPrivateMissionVM.setMissionIndexValue(oPermission.getResourceId());
		oPrivateMissionVM.setMissionOwner(oPermission.getOwnerId());
		oPrivateMissionVM.setUserId(sUserId);
		oPrivateMissionVM.setPermissionCreatedBy(oPermission.getCreatedBy());
		oPrivateMissionVM.setPermissionCreationDate(oPermission.getCreatedDate());
		oPrivateMissionVM.setPermissionType(oPermission.getPermissions());
		return oPrivateMissionVM;	
	}
	
	

}
