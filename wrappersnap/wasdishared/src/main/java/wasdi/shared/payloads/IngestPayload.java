package wasdi.shared.payloads;

import wasdi.shared.LauncherOperations;

/**
 * Payload of the Ingest Operation
 * 
 * @author p.campanella
 *
 */
public class IngestPayload extends OperationPayload {
	
	/**
	 * Name of ingested file 
	 */
	private String file;
	
	/**
	 * Destination Workspace
	 */
	private String workspace;
	
	public IngestPayload() {
		operation = LauncherOperations.INGEST.name();
	}
		
	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public String getWorkspace() {
		return workspace;
	}

	public void setWorkspace(String workspace) {
		this.workspace = workspace;
	}

}
