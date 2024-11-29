package wasdi.shared.data.missions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;

import com.fasterxml.jackson.core.type.TypeReference;

import wasdi.shared.business.missions.ClientConfig;
import wasdi.shared.business.missions.Mission;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;

public class MissionsRepository {
	
	/**
	 * Values of the app config file
	 */
	static ClientConfig s_oClientConfig = null;
	
	/**
	 * Timestamp of the last update of the config file
	 */
	static long s_lLastAppConfigFileUpdate = 0;
	
	
	/**
	 * Create a Mission Repository.
	 */
	public MissionsRepository() {
		
		// Do we have the values from the json file?
		if (s_oClientConfig == null) {
			// No, read the app config file
			readAppConfig();
		}
		else {
			// Yes, but are up to date?
			String sConfigFilePath = WasdiConfig.Current.paths.missionsConfigFilePath;
			File oAppConfigFile = new File(sConfigFilePath);
			
			if (!oAppConfigFile.exists()) {
				WasdiLog.errorLog("MissionsRepository.MissionsRepository: appconfig file not found!");
			}
			else {
				// When was last modified?
				if (oAppConfigFile.lastModified()>s_lLastAppConfigFileUpdate) {
					// We need to refresh!
					readAppConfig();
				}
			}
		}
	}
	
	/**
	 * Read the app config json file
	 */
	protected void readAppConfig() {
		
		// Take the path
		String sConfigFilePath = WasdiConfig.Current.paths.missionsConfigFilePath;
		// Create the file
		File oAppConfigFile = new File(sConfigFilePath);
		
		// We expect this to exists
		if (!oAppConfigFile.exists()) {
			// If not we create an empty one
			WasdiLog.errorLog("MissionsRepository.readAppConfig: appconfig file not found!");
			s_oClientConfig = new ClientConfig();
		}
		else {
			// Keep trace of the last modified timestamp of the file
			s_lLastAppConfigFileUpdate = oAppConfigFile.lastModified();
			// Read the content
			String sAppConfigContent = WasdiFileUtils.fileToText(sConfigFilePath);
			
			try {
				// And convert it in our entity
				s_oClientConfig = MongoRepository.s_oMapper.readValue(sAppConfigContent, new TypeReference<ClientConfig>(){});
			} 
			catch (Throwable oE) {
				WasdiLog.debugLog("MissionsRepository.readAppConfig: could not parse the JSON payload due to " + oE + ".");
			}
		}
	}
	
	/**
	 * Get the client config for a specific user
	 * @param sUserId
	 * @return
	 */
	public ClientConfig getClientConfig(String sUserId) {
		try {
			
			// We create a new instance
			ClientConfig oClientConfig = new ClientConfig();
			
			// We set orbit search
			oClientConfig.setOrbitsearch(s_oClientConfig.getOrbitsearch());
			
			// For all the missions
            for (Mission oMission: s_oClientConfig.getMissions()) {
            	
            	// If the user can access it
            	if (PermissionsUtils.canUserAccessMission(sUserId, oMission)) {
            		 // We add it
            		oClientConfig.getMissions().add(oMission);
            	}
			}
            
            return oClientConfig;
			
		}
		catch (Throwable oE) {
			WasdiLog.debugLog("MissionsRepository.readAppConfig: could not parse the JSON payload due to " + oE + ".");
		}
		
		// Here something did not work properly
		return null;
	}
	
	/**
	 * Get a list of missions owned by the user
	 * @param sUserId
	 * @return
	 */
	public List<Mission> getMissionsOwnedBy(String sUserId) {
		
		ArrayList<Mission> aoMissions = new ArrayList<>();
		
		try {
			
            for (Mission oMission: s_oClientConfig.getMissions()) {
            	
            	if (!Utils.isNullOrEmpty(oMission.getUserid())) {
            		if (oMission.getUserid().equals(sUserId)) {
            			aoMissions.add(oMission);
                		continue;       			
            		}
            	}
			}
            
			
		}
		catch (Throwable oE) {
			WasdiLog.debugLog("MissionsRepository.readAppConfig: could not parse the JSON payload due to " + oE + ".");
		}
		return aoMissions;
	}
	
	
	/**
	 * Get a Mission Entity by the Mission Index Value
	 * @param sMissionIndexValue
	 * @return
	 */
	public Mission getMissionsByIndexValue(String sMissionIndexValue) {
		
		try {
			
            for (Mission oMission: s_oClientConfig.getMissions()) {
            	
            	if (!Utils.isNullOrEmpty(oMission.getIndexvalue())) {
            		if (oMission.getIndexvalue().equals(sMissionIndexValue)) {
            			return oMission;			
            		}
            	}
			}
		}
		catch (Throwable oE) {
			WasdiLog.debugLog("MissionsRepository.readAppConfig: could not parse the JSON payload due to " + oE + ".");
		}
		return null;
	}
	
	public HashMap<String, String> getMissionIndexValueNameMapping() {
		HashMap<String, String> oMissionIndexValueNameMapping = new HashMap<String, String>();
		
		oMissionIndexValueNameMapping.putAll(
			    s_oClientConfig.getMissions().stream()
			        .collect(Collectors.toMap(Mission::getIndexvalue, Mission::getName, (aExistingValue, sNewValue) -> aExistingValue)) 
			);
		
		return oMissionIndexValueNameMapping;
		
	}
	

}
