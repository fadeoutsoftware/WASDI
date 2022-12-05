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
