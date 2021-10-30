package wasdi.shared.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Collectors;

import wasdi.shared.data.MongoRepository;
import wasdi.shared.utils.Utils;

public class WasdiConfig {
	public String NODECODE = "wasdi";
	public String USERS_DEFAULT_NODE;
	public String TOMCAT_USER = "tomcat8";
	public String BASE_URL = "https://www.wasdi.net/wasdiwebserver/rest/";
	public String CONNECTION_TIMEOUT = "10000";
	public String READ_TIMEOUT = "10000";
	
	public MongoConfig mongo;
	
	public KeycloackConfig keycloack;
	
	public PathsConfig paths;
	
	public RabbitConfig rabbit;
	
	public SnapConfig snap;
	
	public SftpConfig sftp;
	
	public NotificationsConfig notifications;
	
	public ArrayList<DataProviderConfig> dataProviders;
	
	public PlanConfig plan;
	
	public GeoServerConfig geoserver;
	
	public GdalConfig gdal;
	
	public DockersConfig dockers;
	
	public SchedulerConfig scheduler;
	
	
	public DataProviderConfig getDataProviderConfig(String sDataProvider) {
		
		if (dataProviders == null) return null;
		
		for (DataProviderConfig oItem : dataProviders) {
			if (oItem.name.equals(sDataProvider)) {
				return oItem;
			}
		}
		
		return null;
	}
	
	public static boolean readConfig(String sConfigFilePath) {
        try {
			String sJson = Files.lines(Paths.get(sConfigFilePath), StandardCharsets.UTF_8).collect(Collectors.joining(System.lineSeparator()));
			
			s_oConfig = MongoRepository.s_oMapper.readValue(sJson,WasdiConfig.class);
			
			return true;
		} catch (Exception e) {
			Utils.debugLog("WasdiConfig.readConfig: exception " + e.toString());
		}
        
        return false;
	}
	
	public static WasdiConfig s_oConfig;
}
