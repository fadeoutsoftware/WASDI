package wasdi.shared.utils.packagemanagers;

import java.util.HashMap;

import com.fasterxml.jackson.core.type.TypeReference;

import wasdi.shared.business.processors.Processor;
import wasdi.shared.business.processors.ProcessorTypes;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.packagemanagers.CondaPackageManagerImpl;
import wasdi.shared.packagemanagers.IPackageManager;
import wasdi.shared.packagemanagers.PipPackageManagerImpl;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;

public class PackageManagerUtils {
	/**
	 * Checks if a pip package is valid or not. It does this searching for the package json file
	 * @param sPackage
	 * @return
	 */
	public static boolean checkPipPackage(String sPackage) {
		try {
			
			if (Utils.isNullOrEmpty(sPackage)) {
				return false;
			}
			
			if (sPackage.startsWith("--")) {
				WasdiLog.infoLog("PipProcessorEngine.checkPipPackage: line [" + sPackage + "] looks a pip option, try to keep it");
				return true;
			}
			
			if (sPackage.contains("==")) {
				sPackage = sPackage.split("==")[0];
			}

			String sPipApi = "https://pypi.org/pypi/" + sPackage + "/json/";

			HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sPipApi, null); 
			String sResult = oHttpCallResponse.getResponseBody();

			if (Utils.isNullOrEmpty(sResult)==false) {
				try {
					TypeReference<HashMap<String,Object>> oMapType = new TypeReference<HashMap<String,Object>>() {};
					HashMap<String,Object> oResults = MongoRepository.s_oMapper.readValue(sResult, oMapType);			

					if (oResults.containsKey("info")) {
						return true;
					}
				}
				catch (Exception oEx) {
				}
			}
		}
		catch (Exception oExtEx) {
			WasdiLog.infoLog("PipProcessorEngine.checkPipPackage: exception " + oExtEx.toString());
		}

		return false;
	}
	
	/**
	 * Get the appropriate Package Manager Instance from the Processor (type)
	 * @param oProcessor The Processor we want to access the Package Manager
	 * @return Package Manager Instance
	 */
	public static IPackageManager getPackageManagerByProcessor(Processor oProcessor) {
		IPackageManager oPackageManager = null;

		String sType = oProcessor.getType();

		String sIp = WasdiConfig.Current.dockers.internalDockersBaseAddress;
		int iPort = oProcessor.getPort();

		if (sType.equals(ProcessorTypes.UBUNTU_PYTHON37_SNAP) || sType.equals(ProcessorTypes.PIP_ONESHOT) || sType.equals(ProcessorTypes.PYTHON_PIP_2) || sType.equals(ProcessorTypes.PYTHON_PIP_2_UBUNTU_20)) {
			oPackageManager = new PipPackageManagerImpl(sIp, iPort);
		} else if (sType.equals(ProcessorTypes.CONDA)) {
			oPackageManager = new CondaPackageManagerImpl(sIp, iPort);
		} else {
			WasdiLog.warnLog("Package Manager not supported for type " + sType);
		}

		return oPackageManager;
	}	
}
