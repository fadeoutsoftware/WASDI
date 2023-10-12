package wasdi.shared.viewmodels.processors;

public class PackageViewModel {

	private String managerName;
	private String packageName;
	private String currentVersion;
	private String currentBuild;
	private String latestVersion;
	private String type;
	private String channel;
	
	public PackageViewModel() {
		
	}
	
	public PackageViewModel(String sManagerName,String sPackageName,String sCurrentVersion,String sCurrentBuild,String sLatestVersion,String sType,String sChannel) {
		this.managerName = sManagerName;
		this.packageName = sPackageName;
		this.currentVersion = sCurrentVersion;
		this.currentBuild = sCurrentBuild;
		this.latestVersion = sLatestVersion;
		this.type = sType;
		this.channel = sChannel;
	}
	
	public String getManagerName() {
		return managerName;
	}
	public void setManagerName(String managerName) {
		this.managerName = managerName;
	}
	public String getPackageName() {
		return packageName;
	}
	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}
	public String getCurrentVersion() {
		return currentVersion;
	}
	public void setCurrentVersion(String currentVersion) {
		this.currentVersion = currentVersion;
	}
	public String getCurrentBuild() {
		return currentBuild;
	}
	public void setCurrentBuild(String currentBuild) {
		this.currentBuild = currentBuild;
	}
	public String getLatestVersion() {
		return latestVersion;
	}
	public void setLatestVersion(String latestVersion) {
		this.latestVersion = latestVersion;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getChannel() {
		return channel;
	}
	public void setChannel(String channel) {
		this.channel = channel;
	}

}
