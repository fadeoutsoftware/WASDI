package wasdi.shared.viewmodels.users;

public class SkinViewModel {
	
	private String logoImage ="/assets/icons/logo-only.svg";
	private String logoText = "/assets/icons/logo-name.svg";
	private String helpLink = "https://wasdi.readthedocs.io/en/latest/";
	private String supportLink = "https://discord.gg/FkRu2GypSg";
	
	public String getLogoImage() {
		return logoImage;
	}
	public void setLogoImage(String logoImage) {
		this.logoImage = logoImage;
	}
	public String getHelpLink() {
		return helpLink;
	}
	public void setHelpLink(String helpLink) {
		this.helpLink = helpLink;
	}
	public String getSupportLink() {
		return supportLink;
	}
	public void setSupportLink(String supportLink) {
		this.supportLink = supportLink;
	}
	public String getLogoText() {
		return logoText;
	}
	public void setLogoText(String logoText) {
		this.logoText = logoText;
	}
	

}
