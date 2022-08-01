package wasdi.shared.managers;

import java.util.List;
import java.util.Map;

import wasdi.shared.viewmodels.processors.PackageManagerViewModel;
import wasdi.shared.viewmodels.processors.PackageViewModel;

public interface IPackageManager {

	List<PackageViewModel> listPackages(String sFlag);

	PackageManagerViewModel getManagerVersion();

	Map<String, Object> getPackagesInfo();

	void operatePackageChange(String sUpdateCommand);

	String executeCommand(String sCommand);

}
