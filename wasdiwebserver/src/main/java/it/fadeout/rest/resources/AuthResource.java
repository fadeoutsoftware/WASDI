package it.fadeout.rest.resources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import it.fadeout.Wasdi;
import it.fadeout.business.PasswordAuthentication;
import it.fadeout.mercurius.business.Message;
import it.fadeout.mercurius.client.MercuriusAPI;
import it.fadeout.sftp.SFTPManager;

import wasdi.shared.business.User;
import wasdi.shared.business.UserSession;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.ChangeUserPasswordViewModel;
import wasdi.shared.viewmodels.LoginInfo;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.RegistrationInfoViewModel;
import wasdi.shared.viewmodels.UserViewModel;
import wasdi.shared.utils.DButils;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.sun.javafx.webkit.UtilitiesImpl;


@Path("/auth")
public class AuthResource {
	
	private static int MINPASSWORDLENGTH = 8;
	
	PasswordAuthentication m_oPasswordAuthentication = new PasswordAuthentication();
	
	@Context
	ServletConfig m_oServletConfig;
	
	@POST
	@Path("/login")
	@Produces({"application/xml", "application/json", "text/xml"})
	public UserViewModel Login(LoginInfo oLoginInfo) {
		Wasdi.DebugLog("AuthResource.Login");
		
		UserViewModel oUserVM = new UserViewModel();
		oUserVM.setUserId("");
		
		try {
			if (oLoginInfo == null) {
				return oUserVM;
			}
			if (Utils.isNullOrEmpty(oLoginInfo.getUserId())) {
				return oUserVM;
			}
			if (Utils.isNullOrEmpty(oLoginInfo.getUserPassword())) {
				return oUserVM;
			}
			
			System.out.println("AuthResource.Login: requested access from " + oLoginInfo.getUserId());
			
			UserRepository oUserRepository = new UserRepository();

			
			//User oWasdiUser = oUserRepository.Login(oLoginInfo.getUserId(), oLoginInfo.getUserPassword());
			User oWasdiUser = oUserRepository.GetUser(oLoginInfo.getUserId());
			if( oWasdiUser == null ) {
				return oUserVM;
			}
			
			if(null != oWasdiUser.getFirstAccessValidated()) {
				if(oWasdiUser.getFirstAccessValidated() ) {
					String sToken = oWasdiUser.getPassword();
					Boolean bIsLogged = m_oPasswordAuthentication.authenticate(oLoginInfo.getUserPassword().toCharArray(), sToken);
					if (bIsLogged == true) {
						
						//get all expired sessions
						SessionRepository oSessionRepository = new SessionRepository();
						List<UserSession> aoEspiredSessions = oSessionRepository.GetAllExpiredSessions(oWasdiUser.getUserId());
						for (UserSession oUserSession : aoEspiredSessions) {
							//delete data base session
							if (!oSessionRepository.DeleteSession(oUserSession)) {
								System.out.println("AuthService.Login: Error deleting session.");
							}
						}
						
						oUserVM.setName(oWasdiUser.getName());
						oUserVM.setSurname(oWasdiUser.getSurname());
						oUserVM.setUserId(oWasdiUser.getUserId());
						
						UserSession oSession = new UserSession();
						oSession.setUserId(oWasdiUser.getUserId());
						//TODO check: can two users have the same sessionid?
						String sSessionId = UUID.randomUUID().toString();
						oSession.setSessionId(sSessionId);
						oSession.setLoginDate((double) new Date().getTime());
						oSession.setLastTouch((double) new Date().getTime());
						
						Boolean bRet = oSessionRepository.InsertSession(oSession);
						if (!bRet)
							return oUserVM;
						
						oUserVM.setSessionId(sSessionId);
						System.out.println("AuthService.Login: access succeeded");
					} else {
						System.out.println("AuthService.Login: access failed");
					}	
				} else {
					System.err.println("AuthService.Login: registration not validated yet");
				}
				System.err.println("AuthService.Login: registration flag is null");
			}
				
		}
		catch (Exception oEx) {
			System.out.println("AuthService.Login: Error");
			oEx.printStackTrace();
			
		}
		
		return oUserVM;
	}
	
	@GET
	@Path("/checksession")
	@Produces({"application/xml", "application/json", "text/xml"})
	public UserViewModel CheckSession(@HeaderParam("x-session-token") String sSessionId) {
		UserViewModel oUserVM = new UserViewModel();
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null) return null;
		if (Utils.isNullOrEmpty(oUser.getUserId())) return null;
		
		oUserVM.setName(oUser.getName());
		oUserVM.setSurname(oUser.getSurname());
		oUserVM.setUserId(oUser.getUserId());
		
		return oUserVM;
	}	
	

	@GET
	@Path("/logout")
	@Produces({"application/xml", "application/json", "text/xml"})
	public PrimitiveResult Logout(@HeaderParam("x-session-token") String sSessionId) {
		
		Wasdi.DebugLog("AuthResource.Logout");
		
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		
		if (sSessionId == null) return oResult;
		if (sSessionId.isEmpty()) return oResult;
		
		SessionRepository oSessionRepository = new SessionRepository();
		UserSession oSession = oSessionRepository.GetSession(sSessionId);
		if(oSession != null) {
			if(oSessionRepository.DeleteSession(oSession)) {
				System.out.println("AuthService.Logout: Session data base deleted.");
				oResult.setBoolValue(true);
			}
			else {
				System.out.println("AuthService.Logout: Error deleting session data base.");
			}
			
		}
		
		return oResult;
	}	

	
	
	@POST
	@Path("/upload/createaccount")
	@Produces({"application/json", "text/xml"})
	public Response CreateSftpAccount(@HeaderParam("x-session-token") String sSessionId, String sEmail) {
		
		Wasdi.DebugLog("AuthService.CreateSftpAccount: Called for Mail " + sEmail);
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();
		String sAccount = oUser.getUserId();
		
		String wsAddress = m_oServletConfig.getInitParameter("sftpManagementWSServiceAddress");
		if (wsAddress==null) wsAddress = "ws://localhost:6703"; 
		SFTPManager oManager = new SFTPManager(wsAddress);
		String sPassword = UUID.randomUUID().toString().split("-")[0];
		
		if (!oManager.createAccount(sAccount, sPassword)) {
			System.out.println("AuthService.CreateSftpAccount: error creating sftp account");
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		sendPasswordEmail(sEmail, sAccount, sPassword);
	    
		return Response.ok().build();
	}
	
	@GET
	@Path("/upload/existsaccount")
	@Produces({"application/json", "text/xml"})
	public boolean ExixtsSftpAccount(@HeaderParam("x-session-token") String sSessionId) {
		
		Wasdi.DebugLog("AuthService.ExistsSftpAccount");
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId())) return false;		
		String sAccount = oUser.getUserId();		
		
		String wsAddress = m_oServletConfig.getInitParameter("sftpManagementWSServiceAddress");
		if (wsAddress==null) wsAddress = "ws://localhost:6703"; 
		SFTPManager oManager = new SFTPManager(wsAddress);

		return oManager.checkUser(sAccount);
	}


	@GET
	@Path("/upload/list")
	@Produces({"application/json", "text/xml"})
	public String[] ListSftpAccount(@HeaderParam("x-session-token") String sSessionId) {
		
		Wasdi.DebugLog("AuthService.ListSftpAccount");
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId())) return null;		
		String sAccount = oUser.getUserId();		
		
		String wsAddress = m_oServletConfig.getInitParameter("sftpManagementWSServiceAddress");
		if (wsAddress==null) wsAddress = "ws://localhost:6703"; 
		SFTPManager oManager = new SFTPManager(wsAddress);

		return oManager.list(sAccount);
	}
	

	@DELETE
	@Path("/upload/removeaccount")
	@Produces({"application/json", "text/xml"})
	public Response RemoveSftpAccount(@HeaderParam("x-session-token") String sSessionId) {
		
		Wasdi.DebugLog("AuthService.RemoveSftpAccount");
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();		
		String sAccount = oUser.getUserId();
		
		String wsAddress = m_oServletConfig.getInitParameter("sftpManagementWSServiceAddress");
		if (wsAddress==null) wsAddress = "ws://localhost:6703"; 
		SFTPManager oManager = new SFTPManager(wsAddress);

		return oManager.removeAccount(sAccount) ? Response.ok().build() : Response.status(Status.INTERNAL_SERVER_ERROR).build();
	}


	@POST
	@Path("/upload/updatepassword")
	@Produces({"application/json", "text/xml"})
	public Response UpdateSftpPassword(@HeaderParam("x-session-token") String sSessionId, String sEmail) {
		
		Wasdi.DebugLog("AuthService.UpdateSftpPassword");
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();		
		String sAccount = oUser.getUserId();
		
		String wsAddress = m_oServletConfig.getInitParameter("sftpManagementWSServiceAddress");
		if (wsAddress==null) wsAddress = "ws://localhost:6703"; 
		SFTPManager oManager = new SFTPManager(wsAddress);
		
		String sPassword = UUID.randomUUID().toString().split("-")[0];
		
		if (!oManager.updatePassword(sAccount, sPassword)) return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		
		sendPasswordEmail(sEmail, sAccount, sPassword);

		return Response.ok().build();
	}
	
	//CHECK USER ID TOKEN BY GOOGLE 
	@POST
	@Path("/logingoogleuser")
	@Produces({"application/xml", "application/json", "text/xml"})
	public UserViewModel LoginGoogleUser(LoginInfo oLoginInfo) {
		Wasdi.DebugLog("AuthResource.CheckGoogleUserId");
		UserViewModel oUserVM = new UserViewModel();
		oUserVM.setUserId("");
		
		try 
		{
			if (oLoginInfo == null) {
				return oUserVM;
			}
			if (Utils.isNullOrEmpty(oLoginInfo.getUserId())) {
				return oUserVM;
			}
			if (Utils.isNullOrEmpty(oLoginInfo.getGoogleIdToken())) {
				return oUserVM;
			}
			
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
			if (oIdToken != null) 
			{
			  Payload oPayload = oIdToken.getPayload();

			  // Print user identifier
			  String userId = oPayload.getSubject();
			 
			  // Get profile information from payload
			  String sEmail = oPayload.getEmail();
			 /* boolean bEmailVerified = Boolean.valueOf(oPayload.getEmailVerified());
			  String sName = (String) oPayload.get("name");
			  String sPictureUrl = (String) oPayload.get("picture");
			  String sLocale = (String) oPayload.get("locale");
			  String sGivenName = (String) oPayload.get("given_name");
			  String sFamilyName = (String) oPayload.get("family_name");*/
			  
			  // store profile information and create session
			  System.out.println("AuthResource.LoginGoogleUser: requested access from " + userId);
			

			  UserRepository oUserRepository = new UserRepository();
			  String sAuthProvider = "google";
			  User oWasdiUser = oUserRepository.GoogleLogin(userId , sEmail, sAuthProvider);
			  //save new user 
			  if(oWasdiUser == null)
			  {
				  User oUser = new User();
				  oUser.setAuthServiceProvider(sAuthProvider);
				  oUser.setUserId(userId);
				  oUser.setEmail(sEmail);
				  
				  if(oUserRepository.InsertUser(oUser) == true)
				  {
					  //the user is stored in DB
					  //get user from database (i do it only for consistency)
					  oWasdiUser = oUserRepository.GoogleLogin(userId , sEmail, sAuthProvider);
				  }
			  }
			  
			  if (oWasdiUser != null) 
			  {

				  //get all expired sessions
				  SessionRepository oSessionRepository = new SessionRepository();
				  List<UserSession> aoEspiredSessions = oSessionRepository.GetAllExpiredSessions(oWasdiUser.getUserId());
				  for (UserSession oUserSession : aoEspiredSessions) {
					  //delete data base session
					  if (!oSessionRepository.DeleteSession(oUserSession)) {
						  System.out.println("AuthService.LoginGoogleUser: Error deleting session.");
					  }
				  }

				  oUserVM.setName(oWasdiUser.getName());
				  oUserVM.setSurname(oWasdiUser.getSurname());
				  oUserVM.setUserId(oWasdiUser.getUserId());
				  oUserVM.setEmail(oWasdiUser.getEmail());
				  
				  UserSession oSession = new UserSession();
				  oSession.setUserId(oWasdiUser.getUserId());
				  String sSessionId = UUID.randomUUID().toString();
				  oSession.setSessionId(sSessionId);
				  oSession.setLoginDate((double) new Date().getTime());
				  oSession.setLastTouch((double) new Date().getTime());

				  Boolean bRet = oSessionRepository.InsertSession(oSession);
				  if (!bRet)
					  return oUserVM;

				  oUserVM.setSessionId(sSessionId);
				  System.out.println("AuthService.LoginGoogleUser: access succeeded");
			  }
			  else {
				  System.out.println("AuthService.LoginGoogleUser: access failed");
			  }

			} 
			else 
			{
			  System.out.println("Invalid ID token.");
			}
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		return oUserVM;

	}
		
	@POST
	@Path("/signin")
	@Produces({"application/json", "text/xml"})
	/******************USER REGISTRATION*******************/
	public PrimitiveResult userRegistration(RegistrationInfoViewModel oUserViewModel) 
	{	
		Wasdi.DebugLog("AuthService.RegistrationUser"  );
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		if(oUserViewModel != null)
		{
			try
			{
				//Check User properties
				if(Utils.isNullOrEmpty(oUserViewModel.getUserId()) || Utils.isValidEmail(oUserViewModel.getUserId()) == false )
				{
					return oResult;
				}
				if(Utils.isNullOrEmpty(oUserViewModel.getName()))
				{
					return oResult;
				}
				if(Utils.isNullOrEmpty(oUserViewModel.getSurname()))
				{
					return oResult;
				}
				if(Utils.isNullOrEmpty(oUserViewModel.getPassword()) || oUserViewModel.getPassword().length() < MINPASSWORDLENGTH)
				{
					return oResult;
				}
				
				UserRepository oUserRepository = new UserRepository();
				User oWasdiUser = oUserRepository.GetUser(oUserViewModel.getUserId());
				
				//if oWasdiUser is a new user -> oWasdiUser == null
				if(oWasdiUser == null)
				{
					//save new user 
					String sAuthProvider = "wasdi";
					User oNewUser = new User();
					oNewUser.setAuthServiceProvider(sAuthProvider);
					oNewUser.setUserId(oUserViewModel.getUserId());
					oNewUser.setEmail(oUserViewModel.getUserId());
					oNewUser.setName(oUserViewModel.getName());
					oNewUser.setSurname(oUserViewModel.getSurname());
					oNewUser.setPassword(m_oPasswordAuthentication.hash(oUserViewModel.getPassword().toCharArray()));
					//set signup first validation token
					//TODO generate unique GUID
					UUID uuid = UUID.randomUUID();
					oNewUser.setFirstAccessUUID(uuid.toString());
					//TODO send link via email to the user
					if(oUserRepository.InsertUser(oNewUser) == true)
					{
						//the user is stored in DB
						oResult.setBoolValue(true);
					}
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
				oResult.setBoolValue(false);
			}
			


		}
		
		return oResult;
	}
	

	//TODO get api after 
	

@POST
	@Path("/signin")
	@Produces({"application/json", "text/xml"})
	public PrimitiveResult ChangeUserName(@HeaderParam("x-session-token") String sSessionId, String sNewName) {
		Wasdi.DebugLog("AuthService.ChangeUserPassword"  );
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		//note: sSessionId validity is automatically checked later
		//MAYBE: refactor: method for validating user name
		if(null == sNewName ) {
			oResult.setStringValue("new name is null");
			System.err.println(oResult.getStringValue());
			return oResult;
		}
		if(sNewName.length() < 1) {
			oResult.setStringValue("new name is too short");
			System.err.println(oResult.getStringValue());
			return oResult;
		}
		
		try {
			//validity is automatically checked		
			User oUserId = Wasdi.GetUserFromSession(sSessionId);
			if(null == oUserId) {
				//Maybe the user didn't exist, or failed for some other reasons
				System.err.print("Null user from session id (does the user exist?)");
				return oResult;
			}
	
			oUserId.setName(sNewName);
			UserRepository oUR = new UserRepository();
			oUR.UpdateUser(oUserId);
			oResult.setBoolValue(true);
			
		} catch(Exception e) {
			System.err.println("AuthService.ChangeUserPassword: Exception");
			e.printStackTrace();
		}
		
		return oResult;
	}

	
	
	@POST
	@Path("/changePassword")
	@Produces({"application/json", "text/xml"})
	public PrimitiveResult ChangeUserPassword(@HeaderParam("x-session-token") String sSessionId,
			ChangeUserPasswordViewModel oChPasswViewModel) {
		
		Wasdi.DebugLog("AuthService.ChangeUserPassword"  );
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		
		//input validation
		//(just oChPasswViewModel, sSessionId validity is automatically checked later on)
		if(null == oChPasswViewModel) {
			oResult.setStringValue("AuthService.ChangeUserPassword: null ChangeUserPasswordViewModel");
			System.err.println(oResult.getStringValue());
			return oResult;
		}
		if(null == oChPasswViewModel.getNewPassword() ) {
			oResult.setStringValue("AuthService.ChangeUserPassword: null new password!");
			System.err.println(oResult.getStringValue());
			return oResult;
		}
		if(null == oChPasswViewModel.getCurrentPassword() ) {
			oResult.setStringValue("AuthService.ChangeUserPassword: null current password!");
			System.err.println(oResult.getStringValue());
			return oResult;
		}
		if( oChPasswViewModel.getNewPassword().length() < MINPASSWORDLENGTH ) {
			oResult.setStringValue("AuthService.ChangeUserPassword: password is too short");
			System.err.println(oResult.getStringValue());
			return oResult;
		}
		
		
		try {
			//validity is automatically checked		
			User oUserId = Wasdi.GetUserFromSession(sSessionId);
			if(null == oUserId) {
				//Maybe the user didn't exist, or failed for some other reasons
				System.err.print("Null user from session id (does the user exist?)");
				return oResult;
			}
	
			String sOldPassword = oUserId.getPassword();
			Boolean bPasswordCorrect = m_oPasswordAuthentication.authenticate(oChPasswViewModel.getCurrentPassword().toCharArray(), sOldPassword);
			
			if( !bPasswordCorrect ) {
				System.err.println("Wrong current password for user " + oUserId);
				return oResult;
			} else {
				oUserId.setPassword(m_oPasswordAuthentication.hash(oChPasswViewModel.getNewPassword().toCharArray()));
				UserRepository oUR = new UserRepository();
				oUR.UpdateUser(oUserId);
				oResult.setBoolValue(true);
			}
		} catch(Exception e) {
			System.err.println("AuthService.ChangeUserPassword: Exception");
			e.printStackTrace();
		}
		
		return oResult;
		
	} 
	
	private void updatePasswordInDB(UserRepository oUserRepository)
	{
		//update password
		ArrayList<User> aoUsers = oUserRepository.getAllUsers();
		aoUsers = UpdateHashUsersPassword(aoUsers);
		oUserRepository.UpdateAllUsers(aoUsers);
	}
	
	private ArrayList<User> UpdateHashUsersPassword(ArrayList<User> aoUsers)
	{
		for (int i = 0; i < aoUsers.size(); i++) 
		{
			User oUser = aoUsers.get(i);
			if( oUser.getAuthServiceProvider() == null || oUser.getAuthServiceProvider().contains("google") == false)
			{
				oUser.setPassword(m_oPasswordAuthentication.hash(oUser.getPassword().toCharArray()));
			}
			
		}
		return aoUsers;
	}
	
	private void sendPasswordEmail(String sEmail, String sAccount, String sPassword) {
		//send email with new password
		String sMercuriusAPIAddress = m_oServletConfig.getInitParameter("mercuriusAPIAddress");
		MercuriusAPI oAPI = new MercuriusAPI(sMercuriusAPIAddress);			
		Message oMessage = new Message();
		String sTitle = m_oServletConfig.getInitParameter("sftpMailTitle");
		oMessage.setTilte(sTitle);
		String sSenser = m_oServletConfig.getInitParameter("sftpManagementMailSenser");
		if (sSenser==null) sSenser = "adminwasdi@wasdi.org";
		oMessage.setSender(sSenser);
		
		String sMessage = m_oServletConfig.getInitParameter("sftpMailText");
		sMessage += "\n\nUSER: " + sAccount + " - PASSWORD: " + sPassword;
		oMessage.setMessage(sMessage);
		oAPI.sendMailDirect(sEmail, oMessage);
	}

}
