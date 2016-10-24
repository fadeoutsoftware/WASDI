package it.fadeout;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

import it.fadeout.rest.resources.AuthResource;
import it.fadeout.rest.resources.DownloadResource;
import it.fadeout.rest.resources.OpenSearchResource;
import it.fadeout.rest.resources.WasdiResource;
import wasdi.shared.business.User;
import wasdi.shared.business.UserSession;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.data.UserRepository;

public class Wasdi extends Application {
	@Context
	ServletConfig m_oServletConfig;
	
	@Context
	ServletContext m_oContext;	

	@Override
	public Set<Class<?>> getClasses() {
		final Set<Class<?>> classes = new HashSet<Class<?>>();
		// register resources and features
		classes.add(DownloadResource.class);
		classes.add(OpenSearchResource.class);
		classes.add(WasdiResource.class);
		classes.add(AuthResource.class);
		
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
		
		if (oSession != null) {
			// Create User Repo
			UserRepository oUserRepo = new UserRepository();
			// Get the user from the session
			User oUser = oUserRepo.GetUser(oSession.getUserId());
			
			return oUser;
		}
		
		// No Session, No User
		return null;
	}
	
}
