package wasdi.shared.packagemanagers;

import java.util.List;
import java.util.Map;

import wasdi.shared.utils.packagemanagers.PackageManagerUtils;
import wasdi.shared.viewmodels.processors.PackageManagerViewModel;
import wasdi.shared.viewmodels.processors.PackageViewModel;

public class PipOneShotPackageManager implements IPackageManager {
	
	private String m_sTargetIp;
	private int m_iTargetPort;
	private String m_sBaseUrl = "";

	public PipOneShotPackageManager(String sTargetIp, int iTargetPort) {
		m_sTargetIp = sTargetIp;
		m_iTargetPort = iTargetPort;
		m_sBaseUrl = "http://" + m_sTargetIp + ":" + m_iTargetPort;
	}
	
	public PipOneShotPackageManager(String sBaseUrl) {
		m_sBaseUrl = sBaseUrl;
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
		return PackageManagerUtils.checkPipPackage(sPackageName);
	}

}
