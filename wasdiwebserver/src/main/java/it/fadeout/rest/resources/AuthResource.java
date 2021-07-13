package it.fadeout.rest.resources;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.ServletConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FilenameUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

import it.fadeout.Wasdi;
import it.fadeout.business.ImageResourceUtils;
import it.fadeout.mercurius.business.Message;
import it.fadeout.mercurius.client.MercuriusAPI;
import it.fadeout.services.AuthProviderService;
import it.fadeout.sftp.SFTPManager;
import wasdi.shared.business.PasswordAuthentication;
import wasdi.shared.business.User;
import wasdi.shared.business.UserSession;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.utils.CredentialPolicy;
import wasdi.shared.utils.ImageFile;
import wasdi.shared.utils.TimeEpochUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.ChangeUserPasswordViewModel;
import wasdi.shared.viewmodels.LoginInfo;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.RegistrationInfoViewModel;
import wasdi.shared.viewmodels.UserViewModel;


@Path("/auth")
public class AuthResource {


	@Context
	ServletConfig m_oServletConfig;
	
	@Inject
	AuthProviderService m_oKeycloakService;

	/**
	 * Authentication Helper
	 */
	PasswordAuthentication m_oPasswordAuthentication = new PasswordAuthentication();

	/**
	 * Credential Policy
	 */
	//TODO get rid of these annoying policies once and for all
	CredentialPolicy m_oCredentialPolicy = new CredentialPolicy();

	final String[] IMAGE_PROCESSORS_ENABLE_EXTENSIONS = {"jpg", "png", "svg"};
	final String USER_IMAGE_FOLDER_NAME = "userImage";			
	final String DEFAULT_USER_IMAGE_NAME = "userimage";
	final UserRepository m_oUserRepository = new UserRepository();


	@POST
	@Path("/login")
	@Produces({"application/xml", "application/json", "text/xml"})
	public UserViewModel login(LoginInfo oLoginInfo) {
		Utils.debugLog("AuthResource.Login");

		try {
			if (oLoginInfo == null) {
				Utils.debugLog("Auth.Login: login info null, user not authenticated");
				return UserViewModel.getInvalid();
			}
			if(Utils.isNullOrEmpty(oLoginInfo.getUserId())){
				Utils.debugLog("Auth.Login: userId null or empty, user not authenticated");
				return UserViewModel.getInvalid();	
			}
			if(Utils.isNullOrEmpty(oLoginInfo.getUserPassword())){
				Utils.debugLog("Auth.Login: password null or empty, user not authenticated");
				return UserViewModel.getInvalid();	
			}

			Utils.debugLog("AuthResource.Login: requested access from " + oLoginInfo.getUserId());

			UserRepository oUserRepository = new UserRepository();
			User oUser = oUserRepository.getUser(oLoginInfo.getUserId());
			if( oUser == null ) {
				Utils.debugLog("AuthResource.Login: user not found: " + oLoginInfo.getUserId() + ", aborting");
				return UserViewModel.getInvalid();
			}

			if(null == oUser.getValidAfterFirstAccess()) {
				// this is to fix legacy users for which confirmation has never been activated
				Utils.debugLog("AuthResource.Login: hotfix: legacy wasdi user " + oUser.getUserId() + " did not have the 'valid after first access' flag, setting its value to true");
				oUser.setValidAfterFirstAccess(true);
			}


			//authenticate against keycloak
			String sAuthResult = m_oKeycloakService.login(oLoginInfo.getUserId(), oLoginInfo.getUserPassword());
			

			boolean bLoginSuccess = false;

			if(!Utils.isNullOrEmpty(sAuthResult)) { 
				bLoginSuccess = true;
			} else {
				bLoginSuccess = m_oPasswordAuthentication.authenticate(oLoginInfo.getUserPassword().toCharArray(), oUser.getPassword() );
			}
			
			if(bLoginSuccess) {
				oUser.setLastLogin((new Date()).toString());
				oUserRepository.updateUser(oUser);

				
				//SESSION
				Wasdi.clearUserExpiredSessions(oUser);
				SessionRepository oSessionRepository = new SessionRepository();
				UserSession oSession = oSessionRepository.insertUniqueSession(oUser.getUserId());
				if(null==oSession || Utils.isNullOrEmpty(oSession.getSessionId())) {
					Utils.debugLog("AuthResource.Login: could not insert session in DB, aborting");
					return UserViewModel.getInvalid();
				}
				
				//populate view model
				UserViewModel oUserVM = new UserViewModel();
				oUserVM.setName(oUser.getName());
				oUserVM.setSurname(oUser.getSurname());
				oUserVM.setUserId(oUser.getUserId());
				oUserVM.setAuthProvider(oUser.getAuthServiceProvider());
				oUserVM.setSessionId(oSession.getSessionId());

				Utils.debugLog("AuthService.Login: access succeeded, sSessionId: "+oSession.getSessionId());
				
				return oUserVM;
			} else {
				Utils.debugLog("AuthService.Login: access failed");
			}
		}
		catch (Exception oEx) {
			Utils.debugLog("AuthService.Login: " + oEx);
		}

		return UserViewModel.getInvalid();
	}


	@GET
	@Path("/checksession")
	@Produces({"application/xml", "application/json", "text/xml"})
	public UserViewModel checkSession(@HeaderParam("x-session-token") String sSessionId) {
		try {
			if(Utils.isNullOrEmpty(sSessionId)) {
				Utils.debugLog("AuthResource.CheckSession: SessionId is null or empty");
				return UserViewModel.getInvalid();
			}
	
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId())) {
				Utils.debugLog("AuthResource.CheckSession: invalid session");
				return UserViewModel.getInvalid();
			}
	
			UserViewModel oUserVM = new UserViewModel();
			oUserVM.setName(oUser.getName());
			oUserVM.setSurname(oUser.getSurname());
			oUserVM.setUserId(oUser.getUserId());
			return oUserVM;
		} catch (Exception oE) {
			Utils.debugLog("AuthResource.CheckSession: " + oE);
		}
		return UserViewModel.getInvalid();
	}	


	@GET
	@Path("/logout")
	@Produces({"application/xml", "application/json", "text/xml"})
	public PrimitiveResult logout(@HeaderParam("x-session-token") String sSessionId) {
		Utils.debugLog("AuthResource.Logout");

		if(null == sSessionId) {
			Utils.debugLog("AuthResource.CheckSession: null sSessionId");
			return PrimitiveResult.getInvalid();
		}

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			return PrimitiveResult.getInvalid();
		}

		PrimitiveResult oResult = null;
		SessionRepository oSessionRepository = new SessionRepository();
		UserSession oSession = oSessionRepository.getSession(sSessionId);
		if(oSession != null) {
			oResult = new PrimitiveResult();
			oResult.setStringValue(sSessionId);
			if(oSessionRepository.deleteSession(oSession)) {

				Utils.debugLog("AuthService.Logout: Session data base deleted.");
				oResult.setBoolValue(true);
			} else {

				Utils.debugLog("AuthService.Logout: Error deleting session data base.");
				oResult.setBoolValue(false);
			}

		} else {
			return PrimitiveResult.getInvalid();
		}
		return oResult;
	}	



	@POST
	@Path("/upload/createaccount")
	@Produces({"application/json", "text/xml"})
	public Response createSftpAccount(@HeaderParam("x-session-token") String sSessionId, String sEmail) {
		Utils.debugLog("AuthService.CreateSftpAccount: Called for Mail " + sEmail);
		if(Utils.isNullOrEmpty(sEmail)) {
			Utils.debugLog("AuthResource.createSftpAccount: email null or empty, aborting");
			return Response.status(Status.BAD_REQUEST).build();
		}
		try {
			InternetAddress emailAddr = new InternetAddress(sEmail);
			emailAddr.validate();
		} catch (AddressException oEx) {
			Utils.debugLog("AuthResource.createSftpAccount: email is invalid, aborting");
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		try {	
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if (oUser == null) {
				Utils.debugLog("AuthResource.createSftpAccount: session invalid or user not found, aborting");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			// Get the User Id
			String sAccount = oUser.getUserId();
			if(Utils.isNullOrEmpty(sAccount)) {
				Utils.debugLog("AuthResource.createSftpAccount: userid is null, aborting");
				return Response.serverError().build();
			}
	
			// Search for the sftp service
			String sWsAddress = m_oServletConfig.getInitParameter("sftpManagementWSServiceAddress");
			if (Utils.isNullOrEmpty(sWsAddress)) {
				sWsAddress = "ws://localhost:6703";
				Utils.debugLog("AuthResource.createSftpAccount: sWsAddress is null or empty, defaulting to " + sWsAddress);
			}
	
			// Manager instance
			SFTPManager oManager = new SFTPManager(sWsAddress);
			String sPassword = Utils.generateRandomPassword();
	
			// Try to create the account
			if (!oManager.createAccount(sAccount, sPassword)) {
	
				Utils.debugLog("AuthService.CreateSftpAccount: error creating sftp account");
				return Response.serverError().build();
			}
	
			// Sent the credentials to the user
			if(!sendSftpPasswordEmail(sEmail, sAccount, sPassword)) {
				return Response.serverError().build();
			}
	
			// All is done
			return Response.ok().build();
		}catch (Exception oE) {
			Utils.debugLog("AuthService.CreateSftpAccount: " + oE);
		}
		return Response.serverError().build();
	}

	@GET
	@Path("/upload/existsaccount")
	@Produces({"application/json", "text/xml"})
	public boolean exixtsSftpAccount(@HeaderParam("x-session-token") String sSessionId) {
		Utils.debugLog("AuthService.ExistsSftpAccount");

		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null) {
			return false;
		}
		String sAccount = oUser.getUserId();		

		// Get the service address
		String wsAddress = m_oServletConfig.getInitParameter("sftpManagementWSServiceAddress");
		if (wsAddress==null) wsAddress = "ws://localhost:6703"; 
		SFTPManager oManager = new SFTPManager(wsAddress);

		Boolean bRes = null;
		try{
			// Check the user
			bRes = oManager.checkUser(sAccount);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bRes;
	}


	@GET
	@Path("/upload/list")
	@Produces({"application/json", "text/xml"})
	public String[] listSftpAccount(@HeaderParam("x-session-token") String sSessionId) {

		Utils.debugLog("AuthService.ListSftpAccount");

		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null || !m_oCredentialPolicy.satisfies(oUser)) {
			return null;
		}	
		String sAccount = oUser.getUserId();		

		// Get Service Address
		String wsAddress = m_oServletConfig.getInitParameter("sftpManagementWSServiceAddress");
		if (wsAddress==null) wsAddress = "ws://localhost:6703"; 
		SFTPManager oManager = new SFTPManager(wsAddress);

		// Return the list
		return oManager.list(sAccount);
	}


	@DELETE
	@Path("/upload/removeaccount")
	@Produces({"application/json", "text/xml"})
	public Response removeSftpAccount(@HeaderParam("x-session-token") String sSessionId) {

		Utils.debugLog("AuthService.RemoveSftpAccount");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null || !m_oCredentialPolicy.satisfies(oUser)) {
			return Response.status(Status.UNAUTHORIZED).build();
		}

		String sAccount = oUser.getUserId();

		// Get service address
		String wsAddress = m_oServletConfig.getInitParameter("sftpManagementWSServiceAddress");
		if (wsAddress==null) wsAddress = "ws://localhost:6703"; 
		SFTPManager oManager = new SFTPManager(wsAddress);

		// Remove the account
		return oManager.removeAccount(sAccount) ? Response.ok().build() : Response.status(Status.INTERNAL_SERVER_ERROR).build();
	}


	@POST
	@Path("/upload/updatepassword")
	@Produces({"application/json", "text/xml"})
	public Response updateSftpPassword(@HeaderParam("x-session-token") String sSessionId, String sEmail) {

		Utils.debugLog("AuthService.UpdateSftpPassword Mail: " + sEmail);

		if(!m_oCredentialPolicy.validEmail(sEmail)) {
			return Response.status(Status.BAD_REQUEST).build();
		}

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if(null == oUser || !m_oCredentialPolicy.satisfies(oUser)) {
			return Response.status(Status.UNAUTHORIZED).build(); 
		}

		String sAccount = oUser.getUserId();

		// Get the service address
		String wsAddress = m_oServletConfig.getInitParameter("sftpManagementWSServiceAddress");
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


	@POST
	@Path("/upload/userimage")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadUserImage(@FormDataParam("image") InputStream fileInputStream, @FormDataParam("image") FormDataContentDisposition fileMetaData,
			@HeaderParam("x-session-token") String sSessionId ) {

		String sExt;
		String sFileName;

		User oUser = getUserFromSession(sSessionId);
		// Check the user session
		if(oUser == null){
			return Response.status(401).build();
		}

		//get filename and extension 
		if(fileMetaData != null && Utils.isNullOrEmpty(fileMetaData.getFileName()) == false){
			sFileName = fileMetaData.getFileName();
			sExt = FilenameUtils.getExtension(sFileName);
		} else {
			return Response.status(400).build();
		}

		if (!ImageResourceUtils.isValidExtension(sExt, IMAGE_PROCESSORS_ENABLE_EXTENSIONS)) {
			return Response.status(400).build();
		}
		String sPath = m_oServletConfig.getInitParameter("DownloadRootPath") + oUser.getUserId() + "/" + USER_IMAGE_FOLDER_NAME;
		ImageResourceUtils.createDirectory(sPath);
		String sOutputFilePath = sPath + "/" + DEFAULT_USER_IMAGE_NAME + "." + sExt.toLowerCase();
		ImageFile oOutputLogo = new ImageFile(sOutputFilePath);
		boolean bIsSaved = oOutputLogo.saveImage(fileInputStream);
		if(bIsSaved == false ){
			return Response.status(400).build();
		}
		return Response.status(200).build();
	}

	@GET
	@Path("/get/userimage")
	public Response getUserImage(@HeaderParam("x-session-token") String sSessionId ) {


		User oUser = getUserFromSession(sSessionId);
		// Check the user session
		if(oUser == null){
			return Response.status(401).build();
		}

		String sPath = m_oServletConfig.getInitParameter("DownloadRootPath") + oUser.getUserId() + "\\" + USER_IMAGE_FOLDER_NAME + "\\" + DEFAULT_USER_IMAGE_NAME;
		ImageFile oUserImage = ImageResourceUtils.getImageInFolder(sPath, IMAGE_PROCESSORS_ENABLE_EXTENSIONS);
		String sImageExtension = ImageResourceUtils.getExtensionOfImageInFolder(sPath  , IMAGE_PROCESSORS_ENABLE_EXTENSIONS);

		//Check the image and extension
		if(oUserImage == null || sImageExtension.isEmpty() ){
			return Response.status(204).build();
		}
		//prepare buffer and send the logo to the client 
		ByteArrayInputStream abImageLogo = oUserImage.getByteArrayImage();

		return Response.ok(abImageLogo).build();

	}

	@DELETE
	@Path("/delete/userimage")
	public Response deleteUserImage(@HeaderParam("x-session-token") String sSessionId ) {
		User oUser = getUserFromSession(sSessionId);
		// Check the user session
		if(oUser == null){
			return Response.status(401).build();
		}

		String sPathFolder = m_oServletConfig.getInitParameter("DownloadRootPath") + oUser.getUserId() + "\\" + USER_IMAGE_FOLDER_NAME;
		ImageResourceUtils.deleteFileInFolder(sPathFolder,DEFAULT_USER_IMAGE_NAME);
		return Response.status(200).build();
	}


	@POST
	@Path("/logingoogleuser")
	@Produces({"application/xml", "application/json", "text/xml"})
	public UserViewModel loginGoogleUser(LoginInfo oLoginInfo) {

		Utils.debugLog("AuthResource.CheckGoogleUserId");

		if (oLoginInfo == null) {
			return UserViewModel.getInvalid();
		}
		if(!m_oCredentialPolicy.satisfies(oLoginInfo)) {
			return UserViewModel.getInvalid();
		}

		try {	
			final NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
			final JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();
			GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
					// Specify the CLIENT_ID of the app that accesses the backend:
					.setAudience(Collections.singletonList(oLoginInfo.getUserId()))
					// Or, if multiple clients access the backend:
					//.setAudience(Arrays.asList(CLIENT_ID_1, CLIENT_ID_2, CLIENT_ID_3))
					.build();

			// (Receive idTokenString by HTTPS POST)
			GoogleIdToken oIdToken = verifier.verify(oLoginInfo.getGoogleIdToken());

			//check id token
			if (oIdToken != null) {
				Payload oPayload = oIdToken.getPayload();

				// Print user identifier
				String sGoogleIdToken = oPayload.getSubject();

				// Get profile information from payload
				String sEmail = oPayload.getEmail();


				// store profile information and create session
				Utils.debugLog("AuthResource.LoginGoogleUser: requested access from " + sGoogleIdToken);


				UserRepository oUserRepository = new UserRepository();
				String sAuthProvider = "google";
				User oWasdiUser = oUserRepository.getUser(sEmail);
				//save new user 
				if(oWasdiUser == null) {
					User oUser = new User();
					oUser.setAuthServiceProvider(sAuthProvider);
					oUser.setUserId(sEmail);
					oUser.setGoogleIdToken(sGoogleIdToken);
					if(oUserRepository.insertUser(oUser) == true) {
						//the user is stored in DB
						//get user from database (i do it only for consistency)
						oWasdiUser = oUserRepository.googleLogin(sGoogleIdToken , sEmail, sAuthProvider);

						notifyNewUserInWasdi(oWasdiUser, true, true);
					}
				}

				if (oWasdiUser != null && oWasdiUser.getAuthServiceProvider().equalsIgnoreCase("google")) {
					//get all expired sessions
					SessionRepository oSessionRepository = new SessionRepository();
					List<UserSession> aoEspiredSessions = oSessionRepository.getAllExpiredSessions(oWasdiUser.getUserId());
					for (UserSession oUserSession : aoEspiredSessions) {
						//delete data base session
						if (!oSessionRepository.deleteSession(oUserSession)) {
							//XXX log instead
							Utils.debugLog("AuthService.LoginGoogleUser: Error deleting session.");
						}
					}

					UserViewModel oUserVM = new UserViewModel();
					oUserVM.setName(oWasdiUser.getName());
					oUserVM.setSurname(oWasdiUser.getSurname());
					oUserVM.setUserId(oWasdiUser.getUserId());
					oUserVM.setAuthProvider(oWasdiUser.getAuthServiceProvider());

					UserSession oSession = new UserSession();
					oSession.setUserId(oWasdiUser.getUserId());
					String sSessionId = UUID.randomUUID().toString();
					oSession.setSessionId(sSessionId);
					oSession.setLoginDate((double) new Date().getTime());
					oSession.setLastTouch((double) new Date().getTime());

					Boolean bRet = oSessionRepository.insertSession(oSession);
					if (!bRet) {
						return UserViewModel.getInvalid();
					}
					oUserVM.setSessionId(sSessionId);

					Utils.debugLog("AuthService.LoginGoogleUser: access succeeded");
					return oUserVM;
				} else {
					Utils.debugLog("AuthService.LoginGoogleUser: access failed");
				}

			} else {

				Utils.debugLog("Invalid ID token.");
				UserViewModel.getInvalid();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return UserViewModel.getInvalid();
	}

	@POST
	@Path("/register")
	@Produces({"application/json", "text/xml"})
	public PrimitiveResult userRegistration(RegistrationInfoViewModel oRegistrationInfoViewModel) 
	{
		try{
			Utils.debugLog("AuthService.UserRegistration"); 

			//filter bad cases out
			if(null == oRegistrationInfoViewModel) {
				Utils.debugLog("AuthService.UserRegistration: view model is null");
				PrimitiveResult oPrimitiveResult = new PrimitiveResult();
				oPrimitiveResult.setIntValue(400);
				return oPrimitiveResult;
			}
			if(Utils.isNullOrEmpty(oRegistrationInfoViewModel.getUserId())) {
				Utils.debugLog("AuthService.UserRegistration: userid in view model is null");
				PrimitiveResult oPrimitiveResult = new PrimitiveResult();
				oPrimitiveResult.setIntValue(400);
				return oPrimitiveResult;
			}

			Utils.debugLog("AuthService.UserRegistration: input is good");

			Utils.debugLog("AuthService.UserRegistration: checking if " + oRegistrationInfoViewModel.getUserId() + " is already in wasdi ");
			UserRepository oUserRepository = new UserRepository();
			User oWasdiUser = oUserRepository.getUser(oRegistrationInfoViewModel.getUserId());

			//do we already have this user in our DB?
			if(oWasdiUser != null){
				//yes, it's a well known user. Stop here
				PrimitiveResult oResult = new PrimitiveResult();
				//not modified
				oResult.setIntValue(304);
				Utils.debugLog("AuthService.UserRegistration: " + oRegistrationInfoViewModel.getUserId() + " already in wasdi");
				return oResult;
			} else {
				Utils.debugLog("AuthService.UserRegistration: " + oRegistrationInfoViewModel.getUserId() + " is a new user");
				//no, it's a new user! :)
				//let's check it's a legit one (against kc)  
				//otherwise someone might call this api even if the user is not registered on KC

				String sUserId = oRegistrationInfoViewModel.getUserId();
				
				User oNewUser = m_oKeycloakService.getUser(sUserId);
				if(null==oNewUser) {
					PrimitiveResult oResult = new PrimitiveResult();
					//not found
					oResult.setIntValue(404);
					Utils.debugLog("AuthService.UserRegistration: " + oRegistrationInfoViewModel.getUserId() + " not found in keycloak, aborting");
					return oResult;
				}
				
				//populate remaining fields
				oNewUser.setValidAfterFirstAccess(true);
				oNewUser.setAuthServiceProvider("keycloak");
				Utils.debugLog("AuthResource.userRegistration: user details parsed");
				
				String sDefaultNode = "wasdi";
				try {					
					sDefaultNode = m_oServletConfig.getInitParameter("USERS_DEFAULT_NODE");
					if (Utils.isNullOrEmpty(sDefaultNode)) {
						sDefaultNode = "wasdi";
					}
				}
				catch (Exception oEx) {
					Utils.debugLog("Exception reading Users default node " + oEx);
				}
				oNewUser.setDefaultNode(sDefaultNode);
				

				//store user in DB
				if(oUserRepository.insertUser(oNewUser)) {
					//success: the user is stored in DB!
					Utils.debugLog("AuthResource.userRegistration: user " + oNewUser.getUserId() + " added to wasdi");
					notifyNewUserInWasdi(oNewUser, true);
					PrimitiveResult oResult = new PrimitiveResult();
					oResult.setBoolValue(true);					
					oResult.setIntValue(200);
					oResult.setStringValue("Welcome to space");
					return oResult;
				} else {
					//insert failed: log, mail and throw
					String sMessage = "could not insert new user " + oNewUser.getUserId() + " in DB";
					Utils.debugLog("AuthResource.userRegistration: " + sMessage + ", aborting");
					notifyNewUserInWasdi(oNewUser, false);
					throw new RuntimeException(sMessage);
				}
			}
		} catch(Exception oE) {
			Utils.debugLog("AuthResource.userRegistration: " + oE + ", aborting");
		}

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		oResult.setIntValue(500);
		return oResult;
	}



	@GET
	@Path("/validateNewUser")
	@Produces({"application/xml", "application/json", "text/xml"})
	public PrimitiveResult validateNewUser(@QueryParam("email") String sUserId, @QueryParam("validationCode") String sToken  ) {
		Utils.debugLog("AuthService.validateNewUser UserId: " + sUserId + " Token: " + sToken);


		if(! (m_oCredentialPolicy.validUserId(sUserId) && m_oCredentialPolicy.validEmail(sUserId)) ) {
			return PrimitiveResult.getInvalid();
		}
		if(!m_oCredentialPolicy.validFirstAccessUUID(sToken)) {
			return PrimitiveResult.getInvalid();
		}

		UserRepository oUserRepo = new UserRepository();
		User oUser = oUserRepo.getUser(sUserId);
		if( null == oUser.getValidAfterFirstAccess()) {
			Utils.debugLog("AuthResources.validateNewUser: unexpected null first access validation flag");
			return PrimitiveResult.getInvalid();
		} 
		else if( oUser.getValidAfterFirstAccess() ) {
			Utils.debugLog("AuthResources.validateNewUser: unexpected true first access validation flag");
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

					return oResult;
				} else {
					Utils.debugLog("AuthResources.validateNewUser: registration token mismatch");
					PrimitiveResult.getInvalid();
				}
			}
		}
		return PrimitiveResult.getInvalid();
	}


	@POST
	@Path("/editUserDetails")
	@Produces({"application/json", "text/xml"})
	public UserViewModel editUserDetails(@HeaderParam("x-session-token") String sSessionId, UserViewModel oInputUserVM ) {

		Utils.debugLog("AuthService.editUserDetails");
		//note: sSessionId validity is automatically checked later
		//note: only name and surname can be changed, so far. Other fields are ignored

		if(null == oInputUserVM ) {
			return UserViewModel.getInvalid();
		}
		//check only name and surname: they are the only fields that must be valid,
		//the others will typically be null, including userId
		if(!m_oCredentialPolicy.validName(oInputUserVM.getName()) || !m_oCredentialPolicy.validSurname(oInputUserVM.getSurname())) {
			return UserViewModel.getInvalid();
		}

		try {
			//note: session validity is automatically checked		
			User oUserId = Wasdi.getUserFromSession(sSessionId);
			if(null == oUserId) {
				//Maybe the user didn't exist, or failed for some other reasons
				Utils.debugLog("Null user from session id (does the user exist?)");
				return UserViewModel.getInvalid();
			}

			//update
			oUserId.setName(oInputUserVM.getName());
			oUserId.setSurname(oInputUserVM.getSurname());
			oUserId.setLink(oInputUserVM.getLink());
			oUserId.setDescription(oInputUserVM.getDescription());
			UserRepository oUR = new UserRepository();
			oUR.updateUser(oUserId);

			//respond
			UserViewModel oOutputUserVM = new UserViewModel();
			oOutputUserVM.setUserId(oUserId.getUserId());
			oOutputUserVM.setName(oUserId.getName());
			oOutputUserVM.setSurname(oUserId.getSurname());
			oOutputUserVM.setSessionId(sSessionId);
			return oOutputUserVM;

		} catch(Exception e) {
			Utils.debugLog("AuthService.ChangeUserPassword: Exception");
			e.printStackTrace();
		}
		//should not get here
		return UserViewModel.getInvalid();
	}



	@POST
	@Path("/changePassword")
	@Produces({"application/json", "text/xml"})
	public PrimitiveResult changePassword(@HeaderParam("x-session-token") String sSessionId, ChangeUserPasswordViewModel oChangePasswordViewModel) {

		Utils.debugLog("AuthService.ChangeUserPassword"  );

		//input validation
		if(null == oChangePasswordViewModel) {
			Utils.debugLog("AuthService.ChangeUserPassword: ChangeUserPasswordViewModel is null, aborting");
			return PrimitiveResult.getInvalid();
		}

		if(!m_oCredentialPolicy.satisfies(oChangePasswordViewModel)) {
			Utils.debugLog("AuthService.ChangeUserPassword: invalid input");
			return PrimitiveResult.getInvalid();
		}

		try {
			//validity is automatically checked		
			User oUserId = Wasdi.getUserFromSession(sSessionId);
			if(null == oUserId) {
				//Maybe the user didn't exist, or failed for some other reasons
				Utils.debugLog("Null user from session id (does the user exist?)");
				return PrimitiveResult.getInvalid();
			}

			String sOldPassword = oUserId.getPassword();
			boolean bPasswordCorrect = m_oPasswordAuthentication.authenticate(oChangePasswordViewModel.getCurrentPassword().toCharArray(), sOldPassword);

			if( !bPasswordCorrect ) {
				Utils.debugLog("Wrong current password for user " + oUserId);
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
			Utils.debugLog("AuthService.ChangeUserPassword: " + oE);
		}

		return PrimitiveResult.getInvalid();

	} 	


	@GET
	@Path("/lostPassword")
	@Produces({"application/xml", "application/json", "text/xml"})
	public PrimitiveResult lostPassword(@QueryParam("userId") String sUserId ) {

		Utils.debugLog("AuthService.lostPassword: sUserId: " + sUserId);
		try {

			if(Utils.isNullOrEmpty(sUserId)) {
				Utils.debugLog("AuthService.lostPassword: User id is null or empty, aborting");
				PrimitiveResult oResult = new PrimitiveResult();
				oResult.setStringValue("Bad Request");
				oResult.setIntValue(400);
				oResult.setBoolValue(false);
				return oResult;
			}

			if(!m_oCredentialPolicy.validUserId(sUserId)) {
				Utils.debugLog("AuthService.lostPassword: User id not valid, aborting");
				PrimitiveResult oResult = new PrimitiveResult();
				oResult.setStringValue("Bad Request");
				oResult.setIntValue(400);
				oResult.setBoolValue(false);
				return oResult;
			}

		} catch (Exception oE) {
			Utils.debugLog("AuthService.lostPassword: preliminary checks broken due to: " + oE + ", aborting");
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
				Utils.debugLog("AuthService.lostPassword: User not found, aborting");
				PrimitiveResult oResult = new PrimitiveResult();
				oResult.setStringValue("Bad Request");
				oResult.setIntValue(400);
				oResult.setBoolValue(false);
				return oResult;
			}
			Utils.debugLog("AuthService.lostPassword: user " + sUserId + " found");

			if(Utils.isNullOrEmpty(oUser.getAuthServiceProvider())) {
				//todo check if user is on keycloak
				Utils.debugLog("AuthService.lostPassword: auth service provider null or empty, aborting");
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
			Utils.debugLog("AuthService.lostPassword: could not complete the password recovery due to: " + oE);
		}

		//apparently things did not work well
		Utils.debugLog("AuthService.lostPassword( " + sUserId + "): could not change user password, about to end");
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setStringValue("Internal Server Error");
		oResult.setIntValue(500);
		oResult.setBoolValue(false);
		return oResult;
	}



	private Boolean sendPasswordEmail(String sRecipientEmail, String sAccount, String sPassword) {
		Utils.debugLog("AuthResource.sendPasswordEmail");
		if(null == sRecipientEmail || null == sPassword ) {
			Utils.debugLog("AuthResource.sendPasswordEmail: null input, not enough information to send email");
			return false;
		}
		//send email with new password
		String sMercuriusAPIAddress = m_oServletConfig.getInitParameter("mercuriusAPIAddress");
		MercuriusAPI oMercuriusAPI = new MercuriusAPI(sMercuriusAPIAddress);


		Message oMessage = new Message();
		String sTitle = m_oServletConfig.getInitParameter("PW_RECOVERY_MAIL_TITLE");

		if (Utils.isNullOrEmpty(sTitle)) {
			sTitle = "WASDI Password Recovery";
		}
		oMessage.setTilte(sTitle);


		String sSender = m_oServletConfig.getInitParameter("PW_RECOVERY_MAIL_SENDER");
		if (sSender==null) sSender = "wasdi@wasdi.net";
		oMessage.setSender(sSender);

		String sMessage = m_oServletConfig.getInitParameter("PW_RECOVERY_MAIL_TEXT");

		if (Utils.isNullOrEmpty(sMessage)) {
			sMessage = "Your password has been regenerated. Please find here your new credentials:";
		}

		sMessage += "\n\nUSER: " + sAccount + " - PASSWORD: " + sPassword;
		oMessage.setMessage(sMessage);
		try {
			if(oMercuriusAPI.sendMailDirect(sRecipientEmail, oMessage) >= 0) {
				return true;
			}
		} catch (Exception oE) {
			Utils.debugLog("AuthResource.sendPasswordEmail: " + oE);
			return false;
		}
		return false;
	}


	private Boolean sendSftpPasswordEmail(String sRecipientEmail, String sAccount, String sPassword) {
		Utils.debugLog("AuthResource.sendSFTPPasswordEmail");
		if(null == sRecipientEmail || null == sPassword ) {
			Utils.debugLog("AuthResource.sendPasswordEmail: null input, not enough information to send email");
			return false;
		}
		//send email with new password
		String sMercuriusAPIAddress = m_oServletConfig.getInitParameter("mercuriusAPIAddress");
		MercuriusAPI oMercuriusAPI = new MercuriusAPI(sMercuriusAPIAddress);


		Message oMessage = new Message();
		String sTitle = m_oServletConfig.getInitParameter("sftpMailTitle");

		if (Utils.isNullOrEmpty(sTitle)) {
			sTitle = "WASDI SFTP Account";
		}
		oMessage.setTilte(sTitle);


		String sSender = m_oServletConfig.getInitParameter("sftpManagementMailSenser");
		if (sSender==null) sSender = "wasdi@wasdi.net";
		oMessage.setSender(sSender);

		String sMessage = m_oServletConfig.getInitParameter("sftpMailText");

		if (Utils.isNullOrEmpty(sMessage)) {
			sMessage = "Your password has been regenerated. Please find here your new credentials:";
		}

		sMessage += "\n\nUSER: " + sAccount + " - PASSWORD: " + sPassword;
		oMessage.setMessage(sMessage);
		try {
			if(oMercuriusAPI.sendMailDirect(sRecipientEmail, oMessage) >= 0) {
				return true;
			}
		} catch (Exception oE) {
			Utils.debugLog("AuthResource.sendSFTPPasswordEmail: " + oE);
			return false;
		}
		return false;
	}


	/**
	 * Send a notification email to the administrators
	 * @param oUser
	 * @return
	 */
	private Boolean notifyNewUserInWasdi(User oUser, boolean bConfirmed) {
		return notifyNewUserInWasdi(oUser,bConfirmed,false);
	}

	/**
	 * Send a notification email to the administrators
	 * @param oUser
	 * @return
	 */
	private Boolean notifyNewUserInWasdi(User oUser, boolean bConfirmed, boolean bGoogle) {

		Utils.debugLog("AuthResource.notifyNewUserInWasdi");

		if (oUser == null) {
			Utils.debugLog("AuthResource.notifyNewUserInWasdi: user null, return false");
			return false;
		}

		try {

			String sMercuriusAPIAddress = m_oServletConfig.getInitParameter("mercuriusAPIAddress");

			if(Utils.isNullOrEmpty(sMercuriusAPIAddress)) {
				Utils.debugLog("AuthResource.sendRegistrationEmail: sMercuriusAPIAddress is null");
				return false;
			}

			MercuriusAPI oAPI = new MercuriusAPI(sMercuriusAPIAddress);			
			Message oMessage = new Message();

			String sTitle = "New WASDI User";

			if (bGoogle) sTitle = "New Google WASDI User";

			oMessage.setTilte(sTitle);

			String sSender = m_oServletConfig.getInitParameter("sftpManagementMailSenser");
			if (sSender==null) {
				sSender = "wasdi@wasdi.net";
			}
			oMessage.setSender(sSender);

			String sMessage = "A new user registered in WASDI. User Name: " + oUser.getUserId();

			if (bConfirmed) {
				sMessage = "The new User " + oUser.getUserId() + " has been added to wasdi DB"; 
			} else {
				sMessage = "Confirmation failed: " + oUser.getUserId() + " is in kc but could not be added to wasdi DB";
			}

			oMessage.setMessage(sMessage);

			Integer iPositiveSucceded = 0;

			String sWasdiAdminMail = m_oServletConfig.getInitParameter("WasdiAdminMail");

			if (Utils.isNullOrEmpty(sWasdiAdminMail)) {
				sWasdiAdminMail = "info@fadeout.biz";
			}

			iPositiveSucceded = oAPI.sendMailDirect(sWasdiAdminMail, oMessage);

			Utils.debugLog("AuthResource.notifyNewUserInWasdi: "+iPositiveSucceded.toString());

			if(iPositiveSucceded < 0 ) {

				Utils.debugLog("AuthResource.notifyNewUserInWasdi: error sending notification email to admin");
				return false;
			}
		} catch(Exception e) {
			Utils.debugLog("\n\n"+e.getMessage()+"\n\n" );
			return false;
		}
		return true;
	}

	protected User getUserFromSession(String sSessionId){

		if (Utils.isNullOrEmpty(sSessionId)) {
			return null;
		}
		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null) {
			return null;
		}
		if (Utils.isNullOrEmpty(oUser.getUserId())) {
			return null;
		}
		return oUser;	
	}

}
