package wasdi.shared.config;

import java.util.ArrayList;

import wasdi.shared.utils.Utils;

public class RuntimeDriversConfig {
	public ArrayList<RuntimeDriverConfig> drivers = new ArrayList<>();
	
	public RuntimeDriverConfig getDriverConfig(String sName) {
		if (Utils.isNullOrEmpty(sName)) return null;
		
		if (drivers != null) {
			for (int iDrivers = 0; iDrivers<drivers.size(); iDrivers++) {
				RuntimeDriverConfig oDriver = drivers.get(iDrivers);
				if (oDriver.name.equals(sName)) return oDriver;
			}
		}
		
		return null;
	}
}
