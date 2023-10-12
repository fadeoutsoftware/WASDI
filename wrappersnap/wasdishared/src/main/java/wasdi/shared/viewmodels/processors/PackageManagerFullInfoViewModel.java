package wasdi.shared.viewmodels.processors;

import java.util.List;

public class PackageManagerFullInfoViewModel {

	private PackageManagerViewModel packageManager;
	private List<PackageViewModel> outdated;
	private List<PackageViewModel> uptodate;
	private List<PackageViewModel> all;
	public PackageManagerViewModel getPackageManager() {
		return packageManager;
	}
	public void setPackageManager(PackageManagerViewModel packageManager) {
		this.packageManager = packageManager;
	}
	public List<PackageViewModel> getOutdated() {
		return outdated;
	}
	public void setOutdated(List<PackageViewModel> outdated) {
		this.outdated = outdated;
	}
	public List<PackageViewModel> getUptodate() {
		return uptodate;
	}
	public void setUptodate(List<PackageViewModel> uptodate) {
		this.uptodate = uptodate;
	}
	public List<PackageViewModel> getAll() {
		return all;
	}
	public void setAll(List<PackageViewModel> all) {
		this.all = all;
	}

}
