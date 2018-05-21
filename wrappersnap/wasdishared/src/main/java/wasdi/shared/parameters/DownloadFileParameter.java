package wasdi.shared.parameters;

/**
 * Created by s.adamo on 10/10/2016.
 */



public class DownloadFileParameter extends BaseParameter{

    /**
     * Download url
     */
    private String url;

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

    private String boundingBox;
    
    private String provider;

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
}
