package wasdi.shared.viewmodels;

public class ProductMaskViewModel extends MaskViewModel {

	String name;
	String maskType;
	String description;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getMaskType() {
		return maskType;
	}
	public void setMaskType(String maskType) {
		this.maskType = maskType;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
}
