/**
 * Created by Cristiano Nattero on 2020-05-25
 * 
 * Fadeout software
 *
 */
package wasdi.shared.utils;

import org.apache.commons.lang3.EnumUtils;

import wasdi.shared.LauncherOperations;

/**
 * @author c.nattero
 *
 */
public class LauncherOperationsUtils {
	public static boolean canOperationSpawnChildren(String sOperation) {
		
		if (Utils.isNullOrEmpty(sOperation)) return false;
		
		switch(sOperation.toUpperCase()) {
		case "RUNPROCESSOR":
		case "RUNIDL":
		case "RUNMATLAB":
			return true;
		default:
			return false;
		}
	}
	
	public static boolean doesOperationLaunchApplication(String sOperation) {
		
		if (Utils.isNullOrEmpty(sOperation)) return false;
		
		switch(sOperation.toUpperCase()) {
		case "RUNPROCESSOR":
		case "RUNIDL":
		case "RUNMATLAB":
			return true;
		default:
			return false;
		}
	}
	/**
	 * Returns true if the string sOperation is a valid 
	 * operation in WASDI. The valid operation are stored in
	 * /wasdishared/src/main/java/wasdi/shared/LauncherOperations.java
	 * The method can be used to filter out non compliant user input
	 * @param sOperation the string that must be checked
	 * @return true if valid, false instead
	 */
	public static boolean isValidLauncherOperation(String sOperation) {
		return EnumUtils.isValidEnum(LauncherOperations.class, sOperation);
	}
	
}
