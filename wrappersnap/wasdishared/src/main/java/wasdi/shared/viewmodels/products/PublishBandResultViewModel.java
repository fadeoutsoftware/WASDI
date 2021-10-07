package wasdi.shared.viewmodels.products;

/**
 * Created by p.campanella on 04/11/2016.
 */
public class PublishBandResultViewModel {
    private String layerId;
    private String bandName;
    private String productName;
    private String boundingBox;
    private String geoserverBoundingBox;
    private String geoserverUrl;

    public String getGeoserverUrl() {
		return geoserverUrl;
	}

	public void setGeoserverUrl(String geoserverUrl) {
		this.geoserverUrl = geoserverUrl;
	}

	public String getLayerId() {
        return layerId;
    }

    public void setLayerId(String layerId) {
        this.layerId = layerId;
    }

    public String getBandName() {
        return bandName;
    }

    public void setBandName(String bandName) {
        this.bandName = bandName;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(String boundingBox) {
        this.boundingBox = boundingBox;
    }

    public String getGeoserverBoundingBox() {
        return geoserverBoundingBox;
    }

    public void setGeoserverBoundingBox(String geoserverBoundingBox) {
        this.geoserverBoundingBox = geoserverBoundingBox;
    }
}
