package wasdi.shared.parameters;

/**
 * Parameter of the DOWNLOAD Operation
 * Created by s.adamo on 10/10/2016.
 */
public class DownloadFileParameter extends BaseParameter{

    /**
     * Download url
     */
    private String url;
    
    /**
     * File name
     */
    private String name;
    
	/**
     * download user
     */
    private String downloadUser;
    
    /**
     * download password
     */
    private String downloadPassword;
    
    /**
     * SessionId
     */
    private String queue;
    
    /**
     * Product Bounding Box
     */
    private String boundingBox;
    
    /**
     * Selected download provider
     */
    private String provider;
    
    /**
     * Platform (aka mission) of the file to import
     */
    private String platform;
    
    
    /**
     * Number of Retry
     */
    private int maxRetry = 5;


	public int getMaxRetry() {
		return maxRetry;
	}

	public void setMaxRetry(int maxRetry) {
		this.maxRetry = maxRetry;
	}

	public String getUrl() { return url; }

    public void setUrl(String sUrl) {
        this.url = sUrl;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String sQueue) {
        this.queue = sQueue;
    }

    public String getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(String boundingBox) {
        this.boundingBox = boundingBox;
    }

	public String getDownloadUser() {
		return downloadUser;
	}

	public void setDownloadUser(String downloadUser) {
		this.downloadUser = downloadUser;
	}

	public String getDownloadPassword() {
		return downloadPassword;
	}

	public void setDownloadPassword(String downloadPassword) {
		this.downloadPassword = downloadPassword;
	}    
    
    public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}
	
    public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPlatform() {
		return platform;
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}	
}
