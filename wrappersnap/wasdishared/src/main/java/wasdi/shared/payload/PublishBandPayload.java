package wasdi.shared.payload;

/**
 * Publish band operation payload
 * @author p.campanella
 *
 */
public class PublishBandPayload extends OperationPayload {
	public PublishBandPayload() {
		operation = "PUBLISHBAND";
	}
	
	private String product;
	private String band;
	private String layerId;
	
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
