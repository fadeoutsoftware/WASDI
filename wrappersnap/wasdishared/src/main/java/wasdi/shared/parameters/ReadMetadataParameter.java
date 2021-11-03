package wasdi.shared.parameters;

/**
 * Parameter of the READMETADATA Operation
 * 
 * @author p.campanella
 *
 */
public class ReadMetadataParameter extends BaseParameter {
	
	/**
	 * Full server path of the product
	 */
	private String productName;

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}
	
	

}
