package wasdi.shared.parameters;

import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessorTypes;
import wasdi.shared.utils.Utils;

/**
 * Parameter for the Processor related Operations:
 * 	.RUNPROCESSOR
 * 	.DEPLOYPROCESSOR
 * 	.RUNIDL	
 * 	.REDEPLOYPROCESSOR
 * 	.LIBRARYUPDATE
 * 	.DELETEPROCESSOR
 * @author p.campanella
 *
 */
public class ProcessorParameter extends BaseParameter {
	/**
	 * Name of the processor
	 */
	private String name;
	/**
	 * Processor Id
	 */
	private String processorID;
	/**
	 * Processor Version
	 */
	private String version;	
	/**
	 * Json with the parameters
	 */
	private String json;
	/**
	 * Processor Type
	 */
	private String processorType;
	/**
	 * Flag to know if the process have been triggered using OGC Processes API endpoints.
	 * By default is false => WASDI native processes.
	 * The flag is mainly used to filter the list of jobs in the OGC Processes Endpoint.
	 */
	private boolean isOGCProcess = false;
	
	public String getProcessorType() {
		return processorType;
	}
	public void setProcessorType(String processorType) {
		this.processorType = processorType;
	}
	public String getJson() {
		return json;
	}
	public void setJson(String json) {
		this.json = json;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getProcessorID() {
		return processorID;
	}
	public void setProcessorID(String processorID) {
		this.processorID = processorID;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}	
	public boolean getOGCProcess() {
		return isOGCProcess;
	}
	public void setOGCProcess(boolean isOGCProcess) {
		this.isOGCProcess = isOGCProcess;
	}
	
	
	/**
	 * Get the LauncherOperations entry for this Processor.
	 * This is needed to have separate queues: A different queue can be done for each Operation Type. IDL needs a special queue due to licensing problems
	 * @return
	 */
	public String getLauncherOperation() {
		
		// NOTE: This is needed to have separate queues:
		// A different queue can be done for each Operation Type. IDL needs a special queue due to licensing problems
		
		if (!Utils.isNullOrEmpty(processorType)) {
			if (processorType.equals(ProcessorTypes.IDL)) {
				return LauncherOperations.RUNIDL.name();
			}
		}
		
		return LauncherOperations.RUNPROCESSOR.name();
	}
	
}
