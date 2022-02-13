package wasdi.shared.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Collectors;

import wasdi.shared.data.MongoRepository;
import wasdi.shared.utils.Utils;

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
	 * Cloud of the main node
	 */
	public String mainNodeCloud = "CREODIAS";
	
	/**
	 * Default node assigned to users when they create a new node
	 */
	public String usersDefaultNode;
	
	/**
	 * System name of the tomcat user
	 */
	public String tomcatUser = "tomcat8";
	
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
	 * Mongo db Configuration
	 */
	public MongoConfig mongoMain;
	
	/**
	 * Mongo db Configuration
	 */
	public MongoConfig mongoLocal;	
	
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
	 * Snap configuration
	 */
	public SnapConfig snap;
	
	/**
	 * SFTP server and management configuration
	 */
	public SftpConfig sftp;
	
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
        try {
			String sJson = Files.lines(Paths.get(sConfigFilePath), StandardCharsets.UTF_8).collect(Collectors.joining(System.lineSeparator()));
			
			Current = MongoRepository.s_oMapper.readValue(sJson,WasdiConfig.class);
			
			return true;
		} catch (Exception e) {
			Utils.debugLog("WasdiConfig.readConfig: exception " + e.toString());
		}
        
        return false;
	}
	
	/**
	 * Static reference to the Current WASDI Config
	 */
	public static WasdiConfig Current;
}
