package wasdi.shared.packagemanagers;

import java.util.List;
import java.util.Map;

import wasdi.shared.viewmodels.processors.PackageManagerViewModel;
import wasdi.shared.viewmodels.processors.PackageViewModel;

/**
 * One-shot Conda applications have no running server to query: the executor writes
 * packagesInfo.json directly at refresh time, so this class only satisfies the
 * IPackageManager contract required by DockerProcessorEngine.
 */
public class CondaOneShotPackageManager implements IPackageManager {

	public CondaOneShotPackageManager(String sTargetIp, int iTargetPort) {
	}

	public CondaOneShotPackageManager(String sBaseUrl) {
	}

	@Override
	public List<PackageViewModel> listPackages(String sFlag) {
		return null;
	}

	@Override
	public PackageManagerViewModel getManagerVersion() {
		return null;
	}

	@Override
	public Map<String, Object> getPackagesInfo() {
		return null;
	}

	@Override
	public boolean operatePackageChange(String sUpdateCommand) {
		return false;
	}

	@Override
	public boolean isValidPackage(String sPackageName) {
		return true;
	}

}
