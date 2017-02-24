package it.fadeout.rest.resources;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import it.fadeout.Wasdi;
import wasdi.shared.business.User;
import wasdi.shared.business.UserSession;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.rabbit.RabbitMethods;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.LoginInfo;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.UserViewModel;

@Path("/auth")
public class AuthResource {
	
	@POST
	@Path("/login")
	@Produces({"application/xml", "application/json", "text/xml"})
	public UserViewModel Login(LoginInfo oLoginInfo) {
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
			
			User oWasdiUser = oUserRepository.Login(oLoginInfo.getUserId(), oLoginInfo.getUserPassword());
			
			if (oWasdiUser != null) {
				
				//get all expired sessions
				SessionRepository oSessionRepository = new SessionRepository();
				List<UserSession> aoEspiredSessions = oSessionRepository.GetAllExpiredSessions(oWasdiUser.getUserId());
				for (UserSession oUserSession : aoEspiredSessions) {
					//delete data base session
					if (oSessionRepository.DeleteSession(oUserSession))
					{
						//delete queue session on rabbit
						if (!RabbitMethods.QueueDelete(oUserSession.getSessionId(), oUserSession.getUserId()))
							System.out.println("AuthService.Login: Error deleting session queue rabbit.");
						else
							System.out.println("AuthService.Login: Queue deleted.");
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
			else {
				System.out.println("AuthService.Login: access failed");
			}		
		}
		catch (Exception oEx) {
			oEx.toString();
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
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		
		if (sSessionId == null) return oResult;
		if (sSessionId.isEmpty()) return oResult;
		
		SessionRepository oSessionRepository = new SessionRepository();
		UserSession oSession = oSessionRepository.GetSession(sSessionId);
		if(oSession != null) {
			if(oSessionRepository.DeleteSession(oSession))
			{
				System.out.println("AuthService.Logout: Session data base deleted.");
				//delete queue session on rabbit
				if (!RabbitMethods.QueueDelete(oSession.getSessionId(), oSession.getUserId()))
					System.out.println("AuthService.Logout: Error deleting session queue rabbit.");
				else
					System.out.println("AuthService.Logout: Queue deleted.");
				
				oResult.setBoolValue(true);
			}
			else
				System.out.println("AuthService.Logout: Error deleting session data base.");
			
		}
		
		return oResult;
	}	
	
}
