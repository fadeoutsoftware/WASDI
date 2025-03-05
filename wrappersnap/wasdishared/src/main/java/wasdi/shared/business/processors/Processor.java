package wasdi.shared.business.processors;

import java.util.ArrayList;

/**
 * Processor Entity
 * Represents a User Processor uploaded to WASDI 
 * 
 * @author p.campanella
 *
 */
public class Processor {
	
	/**
	 * Identifier of the processor
	 */
	private String processorId;

	/**
	 * User owner of the processor
	 */
	private String userId;
	/**
	 * Processor Name
	 */
	private String name;
	/**
	 * Processor Friendly Name
	 */
	private String friendlyName;
	/**
	 * Processor Version
	 */
	private String version;
	/**
	 * Processor Description
	 */
	private String description;
	/**
	 * http port assigned to the processor
	 */
	private int port;
	/**
	 * Processor type
	 */
	private String type;
	/**
	 * Processor first deploy nodeCode
	 */
	private String nodeCode;
	
	/**
	 * Processor first deploy nodeUrl
	 */
	private String nodeUrl;

	/**
	 * timeoutMs: 3 hours by default
	 */
	private long timeoutMs = 1000l*60l*60l*3l;
	
	/**
	 * Price for on-demand execution
	 */
	private Float ondemandPrice = 0.0f;
	
	/**
	 * Price for a subscription
	 */
	private Float subscriptionPrice = 0.0f; 
	
	/**
	 * Price per square kilometre
	 */
	private Float pricePerSquareKm = 0.0f;
	
	/**
	 * 
	 */
	private String areaParameterName = "";
	
	/**
	 * Product id on Stripe
	 */
	private String stripeProductId = "";
	
	/**
	 * Stripe on demand payment link
	 */
	private String stripePaymentLinkId = "";
	
	/**
	 * Application external link
	 */
	private String link = "";
	
	/**
	 * Application reference email
	 */
	private String email = "";
	
	/**
	 * Upload Date
	 */
	private Double uploadDate;
	
	/**
	 * Last Update Date
	 */
	private Double updateDate;
	
	/**
	 * Flag to know if it is public or not
	 */
	private int isPublic = 1;
		
	/**
	 * Sample JSON Parameter
	 */
	private String parameterSample="";
	
	/**
	 * Flag to know if the application must be shown in the store or not
	 */
	private boolean showInStore = false;
	
	/**
	 * Long description
	 */
	private String longDescription = "";
	
	/**
	 * If the application has no logo it has a placeholder image. Here there is stored 
	 * the random placeholder image index assigned once for each processor
	 */
	private int noLogoPlaceholderIndex = -1;
	
	/**
	 * Name of the logo image if present
	 */
	private String logo = "";
	
	/**
	 * Log of the build operations
	 */
	private ArrayList<String> buildLogs = new ArrayList<String>();
	
	/**
	 * Flag to keep track of an ongoing deployment/redeployment
	 */
	private boolean isDeploymentOngoing = false;
	
	/**
	 * List of associated categories
	 */
	private ArrayList<String> categories = new ArrayList<String>();
	
	public ArrayList<String> getCategories() {
		return categories;
	}
	public void setCategories(ArrayList<String> categories) {
		this.categories = categories;
	}
	public String getParameterSample() {
		return parameterSample;
	}
	public void setParameterSample(String parameterSample) {
		this.parameterSample = parameterSample;
	}
			
	public String getLink() {
		return link;
	}
	public void setLink(String link) {
		this.link = link;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getProcessorId() {
		return processorId;
	}
	public void setProcessorId(String processorId) {
		this.processorId = processorId;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public long getTimeoutMs() {
		return timeoutMs;
	}
	public void setTimeoutMs(long timeoutMs) {
		this.timeoutMs = timeoutMs;
	}	
	
	public int getIsPublic() {
		return isPublic;
	}
	public void setIsPublic(int isPublic) {
		this.isPublic = isPublic;
	}
	
	public Double getUploadDate() {
		return uploadDate;
	}
	public void setUploadDate(Double uploadDate) {
		this.uploadDate = uploadDate;
	}
	public Double getUpdateDate() {
		return updateDate;
	}
	public void setUpdateDate(Double updateDate) {
		this.updateDate = updateDate;
	}	
	
	public String getNodeCode() {
		return nodeCode;
	}
	public void setNodeCode(String nodeCode) {
		this.nodeCode = nodeCode;
	}
	public String getNodeUrl() {
		return nodeUrl;
	}
	public void setNodeUrl(String nodeUrl) {
		this.nodeUrl = nodeUrl;
	}
	public String getFriendlyName() {
		return friendlyName;
	}
	public void setFriendlyName(String friendlyName) {
		this.friendlyName = friendlyName;
	}
	public Float getOndemandPrice() {
		return ondemandPrice;
	}
	public void setOndemandPrice(Float ondemandPrice) {
		this.ondemandPrice = ondemandPrice;
	}
	public Float getSubscriptionPrice() {
		return subscriptionPrice;
	}
	public void setSubscriptionPrice(Float subscriptionPrice) {
		this.subscriptionPrice = subscriptionPrice;
	}
	public Float getPricePerSquareKm() {
		return pricePerSquareKm;
	}
	public void setPricePerSquareKm(Float pricePerSquareKm) {
		this.pricePerSquareKm = pricePerSquareKm;
	}
	public String getAreaParameterName() {
		return areaParameterName;
	}
	public void setAreaParameterName(String areaParameterName) {
		this.areaParameterName = areaParameterName;
	}
	public String getStripeProductId() {
		return stripeProductId;
	}
	public void setStripeProductId(String stripeProductId) {
		this.stripeProductId = stripeProductId;
	}
	public String getStripePaymentLinkId() {
		return stripePaymentLinkId;
	}
	public void setStripePaymentLinkId(String stripePaymentLinkId) {
		this.stripePaymentLinkId = stripePaymentLinkId;
	}
	public boolean getShowInStore() {
		return showInStore;
	}
	public void setShowInStore(boolean showInStore) {
		this.showInStore = showInStore;
	}
	public String getLongDescription() {
		return longDescription;
	}
	public void setLongDescription(String longDescription) {
		this.longDescription = longDescription;
	}
	public int getNoLogoPlaceholderIndex() {
		return noLogoPlaceholderIndex;
	}
	public void setNoLogoPlaceholderIndex(int noLogoPlaceholderIndex) {
		this.noLogoPlaceholderIndex = noLogoPlaceholderIndex;
	}
	public String getLogo() {
		return logo;
	}
	public void setLogo(String logo) {
		this.logo = logo;
	}
	public ArrayList<String> getBuildLogs() {
		return buildLogs;
	}
	public void setBuildLogs(ArrayList<String> buildLogs) {
		this.buildLogs = buildLogs;
	}
	public boolean isDeploymentOngoing() {
		return isDeploymentOngoing;
	}
	public void setDeploymentOngoing(boolean isDeploymentOngoing) {
		this.isDeploymentOngoing = isDeploymentOngoing;
	}
}
