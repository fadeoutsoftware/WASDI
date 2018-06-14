package it.fadeout;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import javax.annotation.PostConstruct;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Config;
import org.esa.snap.runtime.Engine;

import it.fadeout.business.DownloadsThread;
import it.fadeout.business.ProcessingThread;
import it.fadeout.rest.resources.AuthResource;
import it.fadeout.rest.resources.CatalogResources;
import it.fadeout.rest.resources.FileBufferResource;
import it.fadeout.rest.resources.OpenSearchResource;
import it.fadeout.rest.resources.OpportunitySearchResource;
import it.fadeout.rest.resources.ProcessWorkspaceResource;
import it.fadeout.rest.resources.ProcessingResources;
import it.fadeout.rest.resources.ProductResource;
import it.fadeout.rest.resources.WasdiResource;
import it.fadeout.rest.resources.WorkspaceResource;
import wasdi.shared.business.User;
import wasdi.shared.business.UserSession;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.utils.Utils;

public class Wasdi extends Application {
	@Context
	ServletConfig m_oServletConfig;
	
	@Context
	ServletContext m_oContext;	
	
	/**
	 * Flag for Debug Log: if true Authentication is disabled
	 */
	private static boolean s_bDebug = false;
	
	/**
	 * Flag to activate debug logs	
	 */
	private static boolean s_bDebugLog = false;

	/**
	 * Process queue scheduler
	 */
	private static ProcessingThread s_oProcessingThread = null;

	/**
	 * Downloads queue scheduler
	 */
	private static DownloadsThread s_oDownloadsThread = null;

	
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
		classes.add(ProcessingResources.class);
		classes.add(ProcessWorkspaceResource.class);
		classes.add(CatalogResources.class);
		return classes;
	}
	
	
	@PostConstruct
	public void initWasdi() {		
		
		System.out.println("-----------welcome to WASDI - Web Advanced Space Developer Interface");

		if (getInitParameter("DebugVersion", "false").equalsIgnoreCase("true")) {
			s_bDebug = true;
			System.out.println("-------Debug Version on");
		}
		
		if (getInitParameter("DebugLog", "false").equalsIgnoreCase("true")) {
			s_bDebugLog = true;
			System.out.println("-------Debug Log on");
		}


		try {
			Utils.m_iSessionValidityMinutes = Integer.parseInt(getInitParameter("SessionValidityMinutes", ""+Utils.m_iSessionValidityMinutes));
			System.out.println("-------Session Validity [minutes]: " + Utils.m_iSessionValidityMinutes);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		
		//set nfs properties download
		String userHome = System.getProperty( "user.home");
		String Nfs = System.getProperty( "nfs.data.download" );
		if (Nfs == null) System.setProperty( "nfs.data.download", userHome + "/nfs/download");

		System.out.println("-------nfs dir " + System.getProperty( "nfs.data.download" ));

		if (s_oProcessingThread==null) {
			try {
				
				System.out.println("-------Starting Processing and Download Schedulers...");
				
				
				if (getInitParameter("EnableProcessingScheduler", "true").toLowerCase().equals("true")) {
					s_oProcessingThread = new ProcessingThread(m_oServletConfig);
					s_oProcessingThread.start();
					System.out.println("-------processing thread STARTED");
				}
				else {
					System.out.println("-------processing thread DISABLED");
				}
				
				
				if (getInitParameter("EnableDownloadScheduler", "true").toLowerCase().equals("true")) {
					s_oDownloadsThread = new DownloadsThread(m_oServletConfig);
					s_oDownloadsThread.start();
					System.out.println("-------downloads thread STARTED");
				}
				else {
					System.out.println("-------downloads thread DISABLED");
				}
				
				
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("-------ERROR: CANNOT START PROCESSING THREAD!!!");
			}
		}
		
		System.out.println("-------initializing snap...");
		
		try {
			String snapAuxPropPath = getInitParameter("SNAP_AUX_PROPERTIES", null);
			System.out.println("snap aux properties file: " + snapAuxPropPath);
			Path propFile = Paths.get(snapAuxPropPath);
			Config.instance("snap.auxdata").load(propFile);
			Config.instance().load();

			SystemUtils.init3rdPartyLibs(null);
			SystemUtils.LOG.setLevel(Level.ALL);
			
			Engine.start(false);
			
		} catch (Throwable e) {
			e.printStackTrace();
		}
		
		
		System.out.println("------- WASDI Init done ");
		System.out.println("-----------------------------------------");
		System.out.println("------- 	 Welcome to space     -------");
		System.out.println("-----------------------------------------");
	}
	
	/**
	 * Server Shut down procedure
	 */
	public static void shutDown() {
		try {
			Wasdi.DebugLog("-------Shutting Down Wasdi");
			
			s_oProcessingThread.stopThread();
			s_oDownloadsThread.stopThread();
			MongoRepository.shutDownConnection();
		}
		catch (Exception e) {
			System.out.println("WASDI SHUTDOWN EXCEPTION: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Safe Read Init Parameter
	 * @param sParmaneter
	 * @param sDefault
	 * @return
	 */
	private String getInitParameter(String sParmaneter, String sDefault) {		
		String sParameterValue = m_oServletConfig.getInitParameter(sParmaneter);		
		return sParameterValue==null ? sDefault : sParameterValue;
	}
	
	/**
	 * Get Safe Random file name
	 * @return
	 */
	public static String GetSerializationFileName() {
		return UUID.randomUUID().toString();
	}
	
	/**
	 * Get Common Date Time Format
	 * @param oDate
	 * @return
	 */
	public static String GetFormatDate(Date oDate) {
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
	
	/**
	 * Get the OS PID of a process
	 * @param oProc
	 * @return
	 */
	public static Integer getPIDProcess(Process oProc) {
		Integer oPID = null;
		
		if(oProc.getClass().getName().equals("java.lang.UNIXProcess")) {
			// get the PID on unix/linux systems
			try {
				Field oField = oProc.getClass().getDeclaredField("pid");
				oField.setAccessible(true);
				oPID = oField.getInt(oProc);
				System.out.println("WASDI.getPIDProcess: found PID " + oPID);
			} catch (Throwable e) {
				System.out.println("WASDI.getPIDProcess: Error getting PID " + e.getMessage());
			}
		}
		
		return oPID;
	}
	
	/**
	 * Debug Log
	 * @param sMessage
	 */
	public static void DebugLog(String sMessage) {
		if (s_bDebugLog) {
			System.out.println(sMessage);
		}
	}
	
	
}
