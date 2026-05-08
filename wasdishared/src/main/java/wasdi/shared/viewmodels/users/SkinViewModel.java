package wasdi.shared.viewmodels.users;

import java.util.ArrayList;

public class SkinViewModel {
	
	private String logoImage;
	private String logoText;
	private String helpLink;
	private String supportLink;
	private String brandMainColor;
	private String brandSecondaryColor;
	private String tabTitle;
	private String favIcon;
	private boolean activateSubscriptions=true;
	
	private ArrayList<String> defaultCategories;
	
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
	
	public String getBrandMainColor() {
		return brandMainColor;
	}
	
	public void setBrandMainColor(String brandMainColor) {
		this.brandMainColor = brandMainColor;
	}
	
	public String getBrandSecondaryColor() {
		return brandSecondaryColor;
	}
	
	public void setBrandSecondaryColor(String brandSecondaryColor) {
		this.brandSecondaryColor = brandSecondaryColor;
	}

	public ArrayList<String> getDefaultCategories() {
		return defaultCategories;
	}

	public void setDefaultCategories(ArrayList<String> defaultCategories) {
		this.defaultCategories = defaultCategories;
	}

	public String getTabTitle() {
		return tabTitle;
	}

	public void setTabTitle(String tabTitle) {
		this.tabTitle = tabTitle;
	}

	public String getFavIcon() {
		return favIcon;
	}

	public void setFavIcon(String favIcon) {
		this.favIcon = favIcon;
	}

	public boolean isActivateSubscriptions() {
		return activateSubscriptions;
	}

	public void setActivateSubscriptions(boolean activateSubscriptions) {
		this.activateSubscriptions = activateSubscriptions;
	}
	
	

}
