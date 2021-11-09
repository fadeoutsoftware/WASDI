package wasdi.shared.parameters;

/**
 * Parameter for the PUBLISHBAND Operation
 * Created by p.campanella on 31/10/2016.
 */
public class PublishBandParameter extends BaseParameter{
	
	/**
	 * Name of the product
	 */
    private String fileName;
    
    /**
     * Legacy name of the rabbit queue
     */
    private String queue;
    
    /**
     * Name of the band
     */
    private String bandName;
    
    /**
     * Name of the geoserver style to use
     */
    private String style;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public String getBandName() {
        return bandName;
    }

    public void setBandName(String bandName) {
        this.bandName = bandName;
    }

	public String getStyle() {
		return style;
	}

	public void setStyle(String style) {
		this.style = style;
	}
}
