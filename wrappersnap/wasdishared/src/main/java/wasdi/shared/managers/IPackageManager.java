package wasdi.shared.managers;

import java.util.List;
import java.util.Map;

import wasdi.shared.viewmodels.processors.PackageManagerViewModel;
import wasdi.shared.viewmodels.processors.PackageViewModel;

/**
 * Abstract Package Manager Interface.
 * This interface is used to interact with the docker environments where WASDI Apps are running.
 * It allows to list the packages and manipulate the environment.
 * Every application type that has a package Manager shall implement this interface
 * 
 * @author p.campanella
 *
 */
public interface IPackageManager {

	/**
	 * Get a list of active packages
	 * @param sFlag 
	 * @return
	 */
	List<PackageViewModel> listPackages(String sFlag);

	/**
	 * Get the version of the package manager itself
	 * @return
	 */
	PackageManagerViewModel getManagerVersion();

	/**
	 * Get detailed info about packages
	 * @return
	 */
	Map<String, Object> getPackagesInfo();

	/**
	 * Perfrom an operation in the package manager
	 * @param sUpdateCommand Commands can be: [addPackage|upgradePackage|removePackage]/[name]/[version]. Version is optional: by default it means latest.
	 */
	boolean operatePackageChange(String sUpdateCommand);

}
