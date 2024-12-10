package it.fadeout.rest.resources;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.HashMap;

import javax.inject.Inject;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.json.JSONObject;

import it.fadeout.Wasdi;
import it.fadeout.services.AuthProviderService;
import it.fadeout.services.KeycloakService;
import it.fadeout.sftp.SFTPManager;
import wasdi.shared.business.PasswordAuthentication;
import wasdi.shared.business.Project;
import wasdi.shared.business.Subscription;
import wasdi.shared.business.missions.ClientConfig;
import wasdi.shared.business.missions.Mission;
import wasdi.shared.business.users.User;
import wasdi.shared.business.users.UserApplicationRole;
import wasdi.shared.business.users.UserResourcePermission;
import wasdi.shared.business.users.UserSession;
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
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.missions.PrivateMissionViewModel;
import wasdi.shared.viewmodels.organizations.SubscriptionType;
import wasdi.shared.viewmodels.users.ChangeUserPasswordViewModel;
import wasdi.shared.viewmodels.users.LoginInfo;
import wasdi.shared.viewmodels.users.RegistrationInfoViewModel;
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
	 * Keycloak Auth Provider Service
	 */
	@Inject
	AuthProviderService m_oKeycloakService;

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
	public UserViewModel login(LoginInfo oLoginInfo) {

		try {
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
				String sUserInfo = m_oKeycloakService.getUserData(m_oKeycloakService.getToken(), sLowerCaseUserId); // TODO - not sure if this will still work: for the user with multiple accounts yes, but what about the other two?
				
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
			String sAuthResult = m_oKeycloakService.login(sLowerCaseUserId, oLoginInfo.getUserPassword());  // not sure
			
			boolean bLoginSuccess = false;
			
			String sRefreshToken = getRefreshTokenFromLoginResponse(sAuthResult);

			if(!Utils.isNullOrEmpty(sRefreshToken)) { 
				bLoginSuccess = true;
				m_oKeycloakService.logout(sRefreshToken);
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
				
				// Create a new session
				SessionRepository oSessionRepository = new SessionRepository();
				UserSession oSession = oSessionRepository.insertUniqueSession(oUser.getUserId());
				
				if(null==oSession || Utils.isNullOrEmpty(oSession.getSessionId())) {
					WasdiLog.debugLog("AuthResource.login: could not insert session in DB, aborting");
					return UserViewModel.getInvalid();
				}
				
				//populate view model
				UserViewModel oUserVM = new UserViewModel();
				oUserVM.setName(oUser.getName());
				oUserVM.setSurname(oUser.getSurname());
				oUserVM.setUserId(oUser.getUserId());
				oUserVM.setAuthProvider(oUser.getAuthServiceProvider());
				oUserVM.setSessionId(oSession.getSessionId());
				oUserVM.setType(PermissionsUtils.getUserType(oUser));

				if (oUser.getRole() != null) {
					oUserVM.setRole(oUser.getRole());
				}
				else {
					oUserVM.setRole(UserApplicationRole.USER.getRole());
				}

				WasdiLog.debugLog("AuthResource.login: access succeeded, sSessionId: "+oSession.getSessionId());
				
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
	public UserViewModel checkSession(@HeaderParam("x-session-token") String sSessionId) {
		try {
			// Check if we can see the user from the session
			User oUser = Wasdi.getUserFromSession(sSessionId);
			
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
	public PrimitiveResult logout(@HeaderParam("x-session-token") String sSessionId) {
		WasdiLog.debugLog("AuthResource.logout");
		
		// Try to get the user
		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			return PrimitiveResult.getInvalid();
		}

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

		} else {
			
			//boolean bResult = m_oKeycloakService.logout(sSessionId);
			//oResult.setBoolValue(bResult);
			
			return PrimitiveResult.getInvalid();
		}
		return oResult;
	}	
	
	/**
	 * create an sftp account for the user
	 * @param sSessionId User session
	 * @param sEmail mail of the user
	 * @return http response
	 */
	@POST
	@Path("/upload/createaccount")
	@Produces({"application/json", "text/xml"})
	public Response createSftpAccount(@HeaderParam("x-session-token") String sSessionId, String sEmail) {
		
		WasdiLog.debugLog("AuthResource.createSftpAccount: Called for Mail " + sEmail);
		
		// Validate the inputs
		if(Utils.isNullOrEmpty(sEmail)) {
			WasdiLog.debugLog("AuthResource.createSftpAccount: email null or empty, aborting");
			return Response.status(Status.BAD_REQUEST).build();
		}
		try {
			InternetAddress oEmailAddr = new InternetAddress(sEmail);
			oEmailAddr.validate();
		} catch (AddressException oEx) {
			WasdiLog.errorLog("AuthResource.createSftpAccount: email is invalid, aborting");
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		try {	
			
			// Check the user
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if (oUser == null) {
				WasdiLog.debugLog("AuthResource.createSftpAccount: session invalid or user not found, aborting");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			// Get the User Id
			String sAccount = oUser.getUserId();
			if(Utils.isNullOrEmpty(sAccount)) {
				WasdiLog.debugLog("AuthResource.createSftpAccount: userid is null, aborting");
				return Response.serverError().build();
			}
	
			// Search for the sftp service
			String sWsAddress = WasdiConfig.Current.sftp.sftpManagementWSServiceAddress;
			if (Utils.isNullOrEmpty(sWsAddress)) {
				sWsAddress = "ws://localhost:6703";
				WasdiLog.debugLog("AuthResource.createSftpAccount: sWsAddress is null or empty, defaulting to " + sWsAddress);
			}
	
			// Manager instance
			SFTPManager oManager = new SFTPManager(sWsAddress);
			String sPassword = Utils.generateRandomPassword();
	
			// Try to create the account
			if (!oManager.createAccount(sAccount, sPassword)) {
	
				WasdiLog.debugLog("AuthResource.createSftpAccount: error creating sftp account");
				return Response.serverError().build();
			}
	
			// Sent the credentials to the user
			if(!sendSftpPasswordEmail(sEmail, sAccount, sPassword)) {
				return Response.serverError().build();
			}
	
			// All is done
			return Response.ok().build();
		}catch (Exception oE) {
			WasdiLog.errorLog("AuthResource.createSftpAccount: " + oE);
		}
		return Response.serverError().build();
	}
	
	/**
	 * Check if an sftp account exists for the user
	 * @param sSessionId User session
	 * @return true if exists, false otherwise
	 */
	@GET
	@Path("/upload/existsaccount")
	@Produces({"application/json", "text/xml"})
	public boolean existsSftpAccount(@HeaderParam("x-session-token") String sSessionId) {
		WasdiLog.debugLog("AuthResource.ExistsSftpAccount");

		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null) {
			WasdiLog.debugLog("AuthResource.existsSftpAccount: invalid session");
			return false;
		}
		String sAccount = oUser.getUserId();		

		// Get the service address
		String wsAddress = WasdiConfig.Current.sftp.sftpManagementWSServiceAddress;
		if (wsAddress==null) wsAddress = "ws://localhost:6703"; 
		SFTPManager oManager = new SFTPManager(wsAddress);

		Boolean bRes = null;
		try{
			// Check the user
			bRes = oManager.checkUser(sAccount);
		} catch (Exception oEx) {
			WasdiLog.errorLog("AuthResource.existsSftpAccount: error " + oEx.toString());
		}
		return bRes;
	}
	
	/**
	 * get the list of files in the sftp of the user
	 * @param sSessionId user session
	 * @return list of string, each representing the name of a file in the user sftp account
	 */
	@GET
	@Path("/upload/list")
	@Produces({"application/json", "text/xml"})
	public String[] listSftpAccount(@HeaderParam("x-session-token") String sSessionId) {

		WasdiLog.debugLog("AuthResource.ListSftpAccount");

		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null) {
			WasdiLog.debugLog("AuthResource.listSftpAccount: invalid session");
			return null;
		}	
		String sAccount = oUser.getUserId();		

		// Get Service Address
		String wsAddress = WasdiConfig.Current.sftp.sftpManagementWSServiceAddress;
		if (wsAddress==null) wsAddress = "ws://localhost:6703"; 
		SFTPManager oManager = new SFTPManager(wsAddress);

		// Return the list
		return oManager.list(sAccount);
	}
	
	/**
	 * Remove the sftp account of the user
	 * @param sSessionId user session
	 * @return http standard response
	 */
	@DELETE
	@Path("/upload/removeaccount")
	@Produces({"application/json", "text/xml"})
	public Response removeSftpAccount(@HeaderParam("x-session-token") String sSessionId) {

		WasdiLog.debugLog("AuthResource.removeSftpAccount");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("AuthResource.removeSftpAccount: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();
		}

		String sAccount = oUser.getUserId();

		// Get service address
		String wsAddress = WasdiConfig.Current.sftp.sftpManagementWSServiceAddress;
		if (wsAddress==null) wsAddress = "ws://localhost:6703"; 
		SFTPManager oManager = new SFTPManager(wsAddress);

		// Remove the account
		return oManager.removeAccount(sAccount) ? Response.ok().build() : Response.status(Status.INTERNAL_SERVER_ERROR).build();
	}
	
	/**
	 * Update sftp password of a user: it creates a new password and 
	 * send it to the mail received in input
	 * @param sSessionId user session
	 * @param sEmail user id /mail used for sftp account
	 * @return std http response
	 */
	@POST
	@Path("/upload/updatepassword")
	@Produces({"application/json", "text/xml"})
	public Response updateSftpPassword(@HeaderParam("x-session-token") String sSessionId, String sEmail) {

		WasdiLog.debugLog("AuthResource.updateSftpPassword Mail: " + sEmail);

		if(!m_oCredentialPolicy.validEmail(sEmail)) {
			WasdiLog.debugLog("AuthResource.updateSftpPassword Mail: invalid mail");
			return Response.status(Status.BAD_REQUEST).build();
		}

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if(null == oUser) {
			WasdiLog.debugLog("AuthResource.updateSftpPassword Mail: invalid session");
			return Response.status(Status.UNAUTHORIZED).build(); 
		}

		String sAccount = oUser.getUserId();

		// Get the service address
		String wsAddress = WasdiConfig.Current.sftp.sftpManagementWSServiceAddress;
		if (wsAddress==null) wsAddress = "ws://localhost:6703"; 
		SFTPManager oManager = new SFTPManager(wsAddress);

		// New Password
		String sPassword = Utils.generateRandomPassword();

		// Try to update
		if (!oManager.updatePassword(sAccount, sPassword)) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

		// Send password to the user
		if(!sendSftpPasswordEmail(sEmail, sAccount, sPassword)) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

		return Response.ok().build();
	}
	
	/**
	 * Register a new user
	 * @param oRegistrationInfoViewModel Registration Informations
	 * @return Primitive Result: if all is ok it has boolValue = true, intValue = 200 and a welcome message in stringValue. Otherwise it has in intValue the http error code. 
	 */
	@POST
	@Path("/register")
	@Produces({"application/json", "text/xml"})
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
			} else {
				WasdiLog.debugLog("AuthResource.userRegistration: " + oRegistrationInfoViewModel.getUserId() + " is a new user");
				//no, it's a new user! :)
				//let's check it's a legit one (against kc)  
				//otherwise someone might call this api even if the user is not registered on KC
				
				if (m_oKeycloakService==null) {
					WasdiLog.debugLog("AuthResource.userRegistration: m_oKeycloakService is NULL!! Creating it...");
					m_oKeycloakService = new KeycloakService();
				}
				
				User oNewUser = m_oKeycloakService.getUser(sLowerCasedUserId);
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
	public UserViewModel editUserDetails(@HeaderParam("x-session-token") String sSessionId, UserViewModel oInputUserVM ) {

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
			User oUserId = Wasdi.getUserFromSession(sSessionId);
			if(null == oUserId) {
				//Maybe the user didn't exist, or failed for some other reasons
				WasdiLog.debugLog("AuthResource.editUserDetails: invalid session");
				return UserViewModel.getInvalid();
			}

			//update
			oUserId.setName(oInputUserVM.getName());
			oUserId.setSurname(oInputUserVM.getSurname());
			oUserId.setLink(oInputUserVM.getLink());
			oUserId.setDescription(oInputUserVM.getDescription());

			if (oInputUserVM.getRole() != null) {
				oUserId.setRole(oInputUserVM.getRole());
			}

			UserRepository oUR = new UserRepository();
			oUR.updateUser(oUserId);

			//respond
			UserViewModel oOutputUserVM = new UserViewModel();
			oOutputUserVM.setUserId(oUserId.getUserId());
			oOutputUserVM.setName(oUserId.getName());
			oOutputUserVM.setSurname(oUserId.getSurname());
			oOutputUserVM.setSessionId(sSessionId);
			oOutputUserVM.setType(PermissionsUtils.getUserType(oUserId));
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
	public PrimitiveResult changePassword(@HeaderParam("x-session-token") String sSessionId, ChangeUserPasswordViewModel oChangePasswordViewModel) {

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
			User oUserId = Wasdi.getUserFromSession(sSessionId);
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
				return m_oKeycloakService.requirePasswordUpdateViaEmail(sUserId);
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
	public Response getClientConfig(@HeaderParam("x-session-token") String sSessionId) {

		WasdiLog.debugLog("AuthResource.getClientConfig");

		User oUser = Wasdi.getUserFromSession(sSessionId);

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
	public Response getPrivateMissions(@HeaderParam("x-session-token") String sSessionId) {
		
		WasdiLog.debugLog("AuthResource.getPrivateMissions");

		User oUser = Wasdi.getUserFromSession(sSessionId);

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
