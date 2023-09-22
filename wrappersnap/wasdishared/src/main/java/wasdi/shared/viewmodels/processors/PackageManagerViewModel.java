package wasdi.shared.viewmodels.processors;

public class PackageManagerViewModel {
	
	public PackageManagerViewModel(String sName, String sVersion, int iMajor, int iMinor, int iPatch) {
		this.name = sName;
		this.version = sVersion;
		this.major = iMajor;
		this.minor = iMinor;
		this.patch = iPatch;
	}

	private String name;
	private String version;
	private int major;
	private int minor;
	private int patch;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public int getMajor() {
		return major;
	}
	public void setMajor(int major) {
		this.major = major;
	}
	public int getMinor() {
		return minor;
	}
	public void setMinor(int minor) {
		this.minor = minor;
	}
	public int getPatch() {
		return patch;
	}
	public void setPatch(int patch) {
		this.patch = patch;
	}

}
