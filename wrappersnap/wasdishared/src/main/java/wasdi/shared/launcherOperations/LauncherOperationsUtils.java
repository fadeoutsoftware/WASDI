/**
 * Created by Cristiano Nattero on 2020-05-25
 * 
 * Fadeout software
 *
 */
package wasdi.shared.launcherOperations;

/**
 * @author c.nattero
 *
 */
public class LauncherOperationsUtils {
	public boolean canOperationSpawnChildren(String sOperation) {
		switch(sOperation.toUpperCase()) {
		case "RUNPROCESSOR":
		case "RUNIDL":
		case "RUNMATLAB":
			return true;
		default:
			return false;
		}
	}
	
	public boolean doesOperationLaunchDocker(String sOperation) {
		switch(sOperation.toUpperCase()) {
		case "RUNPROCESSOR":
		//case "RUNIDL":
		//case "RUNMATLAB":
			return true;
		default:
			return false;
		}
	}
}
