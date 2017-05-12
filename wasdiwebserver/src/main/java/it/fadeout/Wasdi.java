package it.fadeout;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

import it.fadeout.rest.resources.AuthResource;
import it.fadeout.rest.resources.CatalogResources;
import it.fadeout.rest.resources.FileBufferResource;
import it.fadeout.rest.resources.OpenSearchResource;
import it.fadeout.rest.resources.OpportunitySearchResource;
import it.fadeout.rest.resources.ProcessWorkspaceResource;
import it.fadeout.rest.resources.ProductResource;
import it.fadeout.rest.resources.SnapOperationsResources;
import it.fadeout.rest.resources.WasdiResource;
import it.fadeout.rest.resources.WorkspaceResource;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.User;
import wasdi.shared.business.UserSession;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.utils.Utils;

public class Wasdi extends Application {
	@Context
	ServletConfig m_oServletConfig;
	
	@Context
	ServletContext m_oContext;	
	
	private static boolean s_bDebug = false;
	
	private static String s_sDownloadRootPath = "";

	@Override
	public Set<Class<?>> getClasses() {
		final Set<Class<?>> classes = new HashSet<Class<?>>();
		// register resources and features
		classes.add(FileBufferResource.class);
		classes.add(OpenSearchResource.class);
		classes.add(WasdiResource.class);
		classes.add(AuthResource.class);
		classes.add(WorkspaceResource.class);
		classes.add(ProductResource.class);
		classes.add(OpportunitySearchResource.class);
		classes.add(SnapOperationsResources.class);
		classes.add(ProcessWorkspaceResource.class);
		classes.add(CatalogResources.class);
		return classes;
	}
	
	
	@PostConstruct
	public void initWasdi() {
		
		if (m_oServletConfig.getInitParameter("DebugVersion").equalsIgnoreCase("true")) {
			s_bDebug = true;
		}
		
	}
	
	public static String GetSerializationFileName()
	{
		return UUID.randomUUID().toString();
	}
	
	public static String GetFormatDate(Date oDate){
		
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(oDate);
	}
	
	
	/**
	 * Get the User object from the session Id
	 * @param sSessionId
	 * @return
	 */
	public static User GetUserFromSession(String sSessionId){
		
		if (s_bDebug) {
			User oUser = new User();
			oUser.setId(1);
			oUser.setUserId("paolo");
			oUser.setName("Paolo");
			oUser.setSurname("Campanella");
			oUser.setPassword("password");
			return oUser;
		}
		else {
			// Create Session Repository
			SessionRepository oSessionRepo = new SessionRepository();
			// Get The User Session
			UserSession oSession = oSessionRepo.GetSession(sSessionId);
			
			if (Utils.isValidSession(oSession)) {
				// Create User Repo
				UserRepository oUserRepo = new UserRepository();
				// Get the user from the session
				User oUser = oUserRepo.GetUser(oSession.getUserId());
				
				oSessionRepo.TouchSession(oSession);
				
				return oUser;
			}
			
			//Session not valid
			oSessionRepo.DeleteSession(oSession);
			
			// No Session, No User
			return null;
			
		}		
	}
	
	public static Integer getPIDProcess(Process oProc)
	{
		Integer oPID = null;
		
		if(oProc.getClass().getName().equals("java.lang.UNIXProcess")) {
			// get the PID on unix/linux systems
			try {
				Field oField = oProc.getClass().getDeclaredField("pid");
				oField.setAccessible(true);
				oPID = oField.getInt(oProc);
				System.out.println("DownloadResource.DownloadAndPublish: PID " + oPID);
			} catch (Throwable e) {
				System.out.println("DownloadResource.DownloadAndPublish: Error getting PID " + e.getMessage());
			}
		}
		
		return oPID;
	}	
	
	
}
