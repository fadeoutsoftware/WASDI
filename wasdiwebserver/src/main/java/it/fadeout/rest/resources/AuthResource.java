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
import wasdi.shared.viewmodels.LoginInfo;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.UserViewModel;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;


@Path("/auth")
public class AuthResource {
	PasswordAuthentication oPasswordAuthentication = new PasswordAuthentication();
	
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

			String sToken = oWasdiUser.getPassword();
			Boolean bIsLogged = oPasswordAuthentication.authenticate(oLoginInfo.getUserPassword().toCharArray(), sToken);
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
				String sSessionId = UUID.randomUUID().toString();
				oSession.setSessionId(sSessionId);
				oSession.setLoginDate((double) new Date().getTime());
				oSession.setLastTouch((double) new Date().getTime());
				
				Boolean bRet = oSessionRepository.InsertSession(oSession);
				if (!bRet)
					return oUserVM;
				
				oUserVM.setSessionId(sSessionId);
				System.out.println("AuthService.Login: access succeeded");
			}
			else 
			{
				System.out.println("AuthService.Login: access failed");
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
	/******************REGISTRATION USER*******************/
	public PrimitiveResult RegistrationUser(User oUser) 
	{	
		Wasdi.DebugLog("AuthService.RegistrationUser"  );
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		if(oUser != null)
		{
			try
			{
				//Check User properties
				if(Utils.isNullOrEmpty(oUser.getUserId()))
				{
					return oResult;
				}
				if(Utils.isNullOrEmpty(oUser.getName()))
				{
					return oResult;
				}
				if(Utils.isNullOrEmpty(oUser.getSurname()))
				{
					return oResult;
				}
				if(Utils.isNullOrEmpty(oUser.getPassword()))
				{
					return oResult;
				}
				
				UserRepository oUserRepository = new UserRepository();
				User oWasdiUser = oUserRepository.GetUser(oUser.getUserId());
				
				//if oWasdiUser is a new user -> oWasdiUser == null
				if(oWasdiUser == null)
				{
					//save new user 
					String sAuthProvider = "wasdi";
					User oNewUser = new User();
					oNewUser.setAuthServiceProvider(sAuthProvider);
					oNewUser.setUserId(oUser.getUserId());
					oNewUser.setEmail(oUser.getUserId());
					oNewUser.setName(oUser.getName());
					oNewUser.setSurname(oUser.getSurname());
					oNewUser.setPassword(oPasswordAuthentication.hash(oUser.getPassword().toCharArray()));
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
				oUser.setPassword(oPasswordAuthentication.hash(oUser.getPassword().toCharArray()));
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
