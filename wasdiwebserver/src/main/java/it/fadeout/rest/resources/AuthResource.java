package it.fadeout.rest.resources;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import it.fadeout.Wasdi;
import it.fadeout.mercurius.business.Message;
import it.fadeout.mercurius.client.MercuriusAPI;
import it.fadeout.sftp.SFTPManager;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.User;
import wasdi.shared.business.UserSession;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.parameters.IngestFileParameter;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.LoginInfo;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.UserViewModel;

@Path("/auth")
public class AuthResource {
	
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
			
			User oWasdiUser = oUserRepository.Login(oLoginInfo.getUserId(), oLoginInfo.getUserPassword());
			
			if (oWasdiUser != null) {
				
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
	
	
	@PUT
	@Path("/upload/ingest")
	@Produces({"application/json", "text/xml"})
	public Response IngestFile(@HeaderParam("x-session-token") String sSessionId, @QueryParam("file") String sFile, @QueryParam("workspace") String sWorkspace) {
		
		Wasdi.DebugLog("AuthService.IngestFile");
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId())) return null;		
		String sAccount = oUser.getUserId();		
		
		String sUserBaseDir = m_oServletConfig.getInitParameter("sftpManagementUserDir");
		File oUserBaseDir = new File(sUserBaseDir);
		File oFilePath = new File(new File(new File(oUserBaseDir, sAccount), "uploads"), sFile);
		if (!oFilePath.canRead()) {
			System.out.println("AuthResource.IngestFile: ERROR: unable to access uploaded file " + oFilePath.getAbsolutePath());
			return Response.serverError().build();
		}
		try {
			ProcessWorkspace oProcess = null;
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			IngestFileParameter oParameter = new IngestFileParameter();
			oParameter.setWorkspace(sWorkspace);
			oParameter.setUserId(sAccount);
			oParameter.setExchange(sWorkspace);
			oParameter.setFilePath(oFilePath.getAbsolutePath());
			try
			{
				oProcess = new ProcessWorkspace();
				oProcess.setOperationDate(Wasdi.GetFormatDate(new Date()));
				oProcess.setOperationType(LauncherOperations.INGEST.name());
				oProcess.setProductName(oFilePath.getName());
				oProcess.setWorkspaceId(sWorkspace);
				oProcess.setUserId(sAccount);
				oProcess.setProcessObjId(Utils.GetRandomName());
				oProcess.setStatus(ProcessStatus.CREATED.name());
				oRepository.InsertProcessWorkspace(oProcess);
				//set the process object Id to params
				oParameter.setProcessObjId(oProcess.getProcessObjId());
			}
			catch(Exception oEx){
				System.out.println("DownloadResource.Download: Error updating process list " + oEx.getMessage());
				oEx.printStackTrace();
				return Response.serverError().build();
			}
	
			String sPath = m_oServletConfig.getInitParameter("SerializationPath") + oProcess.getProcessObjId();
			SerializationUtils.serializeObjectToXML(sPath, oParameter);
	
			String sLauncherPath = m_oServletConfig.getInitParameter("LauncherPath");
			String sJavaExe = m_oServletConfig.getInitParameter("JavaExe");
	
			String sShellExString = sJavaExe + " -jar " + sLauncherPath +" -operation " + LauncherOperations.INGEST + " -parameter " + sPath;
	
			System.out.println("DownloadResource.Download: shell exec " + sShellExString);
	
			Process oProc = Runtime.getRuntime().exec(sShellExString);
			
			return Response.ok().build();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return Response.serverError().build();
		
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
