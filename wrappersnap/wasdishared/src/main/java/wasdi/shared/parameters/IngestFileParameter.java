package wasdi.shared.parameters;

/**
 * Created by s.adamo on 10/10/2016.
 */



public class IngestFileParameter extends BaseParameter{

    private String processObjId;
    
    private String filePath;

	public String getProcessObjId() {
		return processObjId;
	}

	public void setProcessObjId(String processObjId) {
		this.processObjId = processObjId;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

    
    
}
