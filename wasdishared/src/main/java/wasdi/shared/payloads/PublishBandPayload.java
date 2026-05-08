package wasdi.shared.payloads;

import wasdi.shared.LauncherOperations;

/**
 * Publish band operation payload
 * @author p.campanella
 *
 */
public class PublishBandPayload extends OperationPayload {
	
	/**
	 * Name of the product
	 */
	private String product;
	
	/**
	 * Band published
	 */
	private String band;
	
	/**
	 * Geoserver Layer ID
	 */
	private String layerId;
	
	public PublishBandPayload() {
		operation = LauncherOperations.PUBLISHBAND.name();
	}
		
	public String getProduct() {
		return product;
	}
	public void setProduct(String product) {
		this.product = product;
	}
	public String getBand() {
		return band;
	}
	public void setBand(String band) {
		this.band = band;
	}
	public String getLayerId() {
		return layerId;
	}
	public void setLayerId(String layerId) {
		this.layerId = layerId;
	}
}
