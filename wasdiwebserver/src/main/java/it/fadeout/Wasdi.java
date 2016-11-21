package it.fadeout;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

import com.mongodb.util.Util;

import it.fadeout.rest.resources.AuthResource;
import it.fadeout.rest.resources.FileBufferResource;
import it.fadeout.rest.resources.OpenSearchResource;
import it.fadeout.rest.resources.ProductResource;
import it.fadeout.rest.resources.WasdiResource;
import it.fadeout.rest.resources.WorkspaceResource;
import wasdi.shared.business.User;
import wasdi.shared.business.UserSession;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.utils.Utils;

public class Wasdi extends Application {
	@Context
	ServletConfig m_oServletConfig;
	
	@Context
	ServletContext m_oContext;	

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
		
		return classes;
	}
	
	
	@PostConstruct
	public void initWasdi() {
		
	}
	
	public static String GetSerializationFileName()
	{
		return UUID.randomUUID().toString();
	}
	
	/**
	 * Get the User object from the session Id
	 * @param sSessionId
	 * @return
	 */
	public static User GetUserFromSession(String sSessionId){
		
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
		
		// No Session, No User
		return null;
	}
	
}
