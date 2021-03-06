package wasdi.shared.payload;

public class IngestPayload extends OperationPayload {
	public IngestPayload() {
		operation = "INGEST";
	}
	
	private String file;
	private String workspace;
	
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
