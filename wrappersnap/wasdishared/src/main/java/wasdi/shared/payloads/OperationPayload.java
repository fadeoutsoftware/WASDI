package wasdi.shared.payloads;

/**
 * Operation Paylod base class.
 * 
 * Each WASDI Operation runs in a Process Workpsace. 
 * These payloads are the info stored in the process workspace payload for each operation.
 * 
 * The user or the libs can access these payloads for each operation done in WASDI.
 * 
 * The base class has only the type of operation, one of the LauncherOperations enum value.
 * All the derived class adds the info that are of interest of that operation.
 * 
 * @author p.campanella
 *
 */
public class OperationPayload {
	protected String operation;
	
	public String getOperation() {
		return operation;
	}
	public void setOperation(String operation) {
		this.operation = operation;
	}
	

}
