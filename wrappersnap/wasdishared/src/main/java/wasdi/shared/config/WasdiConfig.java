package wasdi.shared.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import wasdi.shared.config.openEO.OpenEO;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.utils.log.WasdiLog;

/**
 * WASDI Configuration
 * 
 * Contains all the WASDI configuration settings, for all the WASDI Components:
 * 	.Launcher
 * 	.Trigger
 * 	.Scheduler
 * 	.Web Server 
 * 	.db Utils
 * 
 * Config is stored in a unique JSON file that represents this object and all its' childs.
 * 
 * @author p.campanella
 *
 */
public class WasdiConfig {
		
	/**
	 * Node Code
	 */
	public String nodeCode = "wasdi";
	/**
	 * Wasdi print server url
	 */
	public String printServerAddress="";
	
	/**
	 * Cloud of the main node
	 */
	public String mainNodeCloud = "CREODIAS";
	
	/**
	 * Default node assigned to users when they create a new node
	 */
	public String usersDefaultNode;
	
	/**
	 * System name of the wasdi user
	 */
	public String systemUserName = "appwasdi";
	
	/**
	 * Id of the system user
	 */
	public Integer systemUserId = 2042;
	
	/**
	 * System name of the wasdi group
	 */
	public String systemGroupName = "appwasdi";
	
	/**
	 * Id of the system group
	 */
	public Integer systemGroupId = 2042;
	
	/**
	 * Base url of WASDI API
	 */
	public String baseUrl = "https://www.wasdi.net/wasdiwebserver/rest/";
	
	/**
	 * Connection timeout when we call a third party API
	 */
	public int connectionTimeout = 10000;
	
	/**
	 * Read timout when we call a third party API
	 */
	public int readTimeout = 10000;
	
	/**
	 * Number of Milliseconds to sleep after a chmod command to let it be applied
	 */
	public int msWaitAfterChmod = 1000;
	
	/**
	 * Set it WASDI should shell exec external components using the local system or using corresponding docker images.
	 * External components are for example gdal, sen2core, snaphu..
	 * Set this flag false to use the fully dockerized wasdi 
	 */
	public boolean shellExecLocally=true;
	
	/**
	 * Set to true to use the log4j configuration to configure the loggers.
	 * If it is false, the app will just log on the standard output
	 */
	public boolean useLog4J = true;
	
	/**
	 * Set true to activate the logs of the http calls
	 */
	public boolean logHttpCalls=true;
	
	/**
	 * General Log level. Used if Log 4 J is NOT USED
	 */
	public String logLevel = "INFO";
	
	/**
	 * Spefic Log Level for the web-server. Overrides logLevel. Used if Log 4 J is NOT USED
	 */
	public String logLevelServer = "";
	
	/**
	 * Spefic Log Level for the launcher. Overrides logLevel. Used if Log 4 J is NOT USED
	 */
	public String logLevelLauncher = "";
	
	/**
	 * Spefic Log Level for the scheduler. Overrides logLevel. Used if Log 4 J is NOT USED
	 */
	public String logLevelScheduler = "";
	
	/**
	 * Spefic Log Level for the trigger. Overrides logLevel. Used if Log 4 J is NOT USED
	 */
	public String logLevelTrigger = "";
	
	/**
	 * Set to true to add date time to log lines
	 */
	public boolean addDateTimeToLogs=false;
	
	/**
	 * Configuration of the parameters checking invalid subscriptions in WASDI and deleting the workspaces accordingly
	 */
	public StorageUsageControl storageUsageControl;
	
	/**
	 * Set true to NOT filter the internal http calls (keycloak, docker..).
	 * If the general logHttpCalls is False, this does not change
	 */
	public boolean filterInternalHttpCalls=true;
	
	/**
	 * Set true if the wasdi tomcat node web server must use the internal docker name instead
	 * of the public http address of the Jupyter Notebook container.
	 * Is due to the configuration of Adwaiseo that does not resolve the public ip of the server.
	 */
	public boolean useNotebooksDockerAddress = false;
	
	/**
	 * Set true if the node has an NVIDIA GPU that we want to make available to our Apps dockers
	 */
	public boolean nvidiaGPUAvailable = false;
	
	/**
	 * Mongo db Configuration for the main node
	 */
	public MongoConfig mongoMain;
	
	/**
	 * Mongo db Configuration for the local node 
	 */
	public MongoConfig mongoLocal;	
	
	/**
	 * Mongo db Configuration for Ecostress Catalogue
	 */
	public MongoConfig mongoEcostress;
	
	/**
	 * Mongo db Configuration for the centralized statistics db
	 */
	public MongoConfig mongoStatistics;	
	
	/**
	 * Keycloack auth server Configuration
	 */
	public KeycloackConfig keycloack;
	
	/**
	 * All the paths needed by WASDI
	 */
	public PathsConfig paths;
	
	/**
	 * Rabbit messaging configuration
	 */
	public RabbitConfig rabbit;
	
	/**
	 * S3 Bucket configuration
	 */
	public S3BucketConfig s3Bucket;
	
	/**
	 * Snap configuration
	 */
	public SnapConfig snap;
	
	/**
	 * SFTP server and management configuration
	 */
	public SftpConfig sftp;
	
	/**
	 * Stripe configuration
	 */
	public StripeConfig stripe;
	
	/**
	 * Notifications configuration (mails sent from WASDI to users)
	 */
	public NotificationsConfig notifications;
	
	/**
	 * List of Catalogues for each Platform type
	 */
	public ArrayList<CatalogueConfig> catalogues;
	
	/**
	 * List of supported Data Providers
	 */
	public ArrayList<DataProviderConfig> dataProviders;
	
	/**
	 * Plan configurations
	 */
	public PlanConfig plan;
	
	/**
	 * Geoserver config
	 */
	public GeoServerConfig geoserver;
	
	/**
	 * Dockers config
	 */
	public DockersConfig dockers;
	
	/**
	 * Schedulers config
	 */
	public SchedulerConfig scheduler;
	
	/**
	 * Multi Cloud Balancer Config
	 */
	public LoadBalancerConfig loadBalancer = new LoadBalancerConfig();
	
	/**
	 * Configuration of the traefik proxy to reach notebooks inside the workspaces
	 */
	public TraefikConfig traefik = new TraefikConfig();
	
	/**
	 * Configuration of the OGC Processes API WASDI Server implementation
	 */
	public OGCProcessesAPIConfig ogcProcessesApi = new OGCProcessesAPIConfig();
	
	/**
	 * Configuration of the openEO Backend WASDI Server Implementation
	 */
	public OpenEO openEO = new OpenEO();
		
	/**
	 * Get the Catalogue Config for the specified Platform Type
	 * @param sPlatformType Platform of interest
	 * @return Catalogue configuration for the specified platform
	 */
	public CatalogueConfig getCatalogueConfig(String sPlatformType) {
		
		if (catalogues == null) return null;
		
		for (CatalogueConfig oCatalogue : catalogues) {
			if (oCatalogue.platform.equals(sPlatformType)) {
				return oCatalogue;
			}
		}
		
		return null;
	}
	
	/**
	 * Get a Data Provider config from the Data Provider Code
	 * @param sDataProvider Code of the Data Provider
	 * @return Data Provider Config
	 */
	public DataProviderConfig getDataProviderConfig(String sDataProvider) {
		
		if (dataProviders == null) return null;
		
		for (DataProviderConfig oItem : dataProviders) {
			if (oItem.name.equals(sDataProvider)) {
				return oItem;
			}
		}
		
		return null;
	}
	
	/**
	 * Read the config from file
	 * @param sConfigFilePath json file path
	 * @return true if ok, false in case of problems
	 */
	public static boolean readConfig(String sConfigFilePath) {
		Stream<String> oLinesStream = null;
		boolean bRes = false;
		
        try {
        	
        	oLinesStream = Files.lines(Paths.get(sConfigFilePath), StandardCharsets.UTF_8);
			String sJson = oLinesStream.collect(Collectors.joining(System.lineSeparator()));
			Current = MongoRepository.s_oMapper.readValue(sJson,WasdiConfig.class);
			Current.paths.wasdiConfigFilePath = sConfigFilePath;
			bRes = true;
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("WasdiConfig.readConfig: exception ", oEx);
		} finally {
			if (oLinesStream != null) 
				oLinesStream.close();
		}
        
        return bRes;
	}
	
	/**
	 * Return true if this is the main WASDI node
	 * false otherwise
	 * @return True if this is the main WASDI Node
	 */
	public boolean isMainNode() {
		try {
			return WasdiConfig.Current.nodeCode.equals("wasdi");
		}
		catch (Exception e) {
			WasdiLog.errorLog("WasdiConfig.isMainNode: exception " + e.toString());
		}
		
		return false;
	}
	
	/**
	 * Static reference to the Current WASDI Config
	 */
	public static WasdiConfig Current;
}
