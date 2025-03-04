package wasdi.shared.parameters;

/**
 * Parameter of a INGEST Operation
 * Created by s.adamo on 10/10/2016.
 */
public class IngestFileParameter extends BaseParameter{
    
	/**
	 * File path to ingest
	 */
    private String filePath;
    
    /**
     * Default Geoserver style for the product
     */
    private String style;
    
    /**
     * Relative Path
     */
    private String relativePath;
    
    /**
     * Platform aka Mission
     */
    private String platform;

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getRelativePath() {
		return relativePath;
	}

	public void setRelativePath(String relativePath) {
		this.relativePath = relativePath;
	}

	public String getStyle() {
		return style;
	}

	public void setStyle(String style) {
		this.style = style;
	}

	public String getPlatform() {
		return platform;
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}    
    
}
