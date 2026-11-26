package wasdi.shared.config;

import java.util.HashMap;
import java.util.Map;

public class RuntimeDriverConfig {
	
	public String name;
	public String classpath;
	public String apiKey;
	public String registryId;
	public String gpuPool = "ADA_24";
	public String registryUser;
	public String registryPassword;
	public String runtimeUrl;
	public String user;
	public String password;
	public String workingDirectory;
	public String startScript;
	public Map<String, String> options = new HashMap<>();

}
