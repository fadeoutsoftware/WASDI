package wasdi.shared.business;

/**
 * Published Band Entity
 * Represent a single band published on geoserver (WMS)
 * Created by p.campanella on 17/11/2016.
 */
public class PublishedBand {
	/**
	 * Parent Product Name
	 */
    private String productName;
    /**
     * Band Name
     */
    private String bandName;
    /**
     * Layer Identifier
     */
    private String layerId;
    /**
     * User owner
     */
    private String userId;
    /**
     * Workspace used
     */
    private String workspaceId;
    /**
     * Boundig box
     */
    private String boundingBox;
    /**
     * Boundig box in geoserver format
     */
    private String geoserverBoundingBox;
    /**
     * URL of the geoserver
     */
    private String geoserverUrl;

    public String getGeoserverUrl() {
		return geoserverUrl;
	}

	public void setGeoserverUrl(String geoserverUrl) {
		this.geoserverUrl = geoserverUrl;
	}

	public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getBandName() {
        return bandName;
    }

    public void setBandName(String bandName) {
        this.bandName = bandName;
    }

    public String getLayerId() {
        return layerId;
    }

    public void setLayerId(String layerId) {
        this.layerId = layerId;
    }

    public String getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(String boundingBox) {
        this.boundingBox = boundingBox;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getGeoserverBoundingBox() {
        return geoserverBoundingBox;
    }

    public void setGeoserverBoundingBox(String geoserverBoundingBox) {
        this.geoserverBoundingBox = geoserverBoundingBox;
    }
}
