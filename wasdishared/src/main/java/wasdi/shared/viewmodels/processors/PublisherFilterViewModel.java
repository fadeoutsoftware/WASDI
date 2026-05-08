package wasdi.shared.viewmodels.processors;


/**
 * Publisher Filter View Model
 * 
 * Represents all the publishers: users that deployed at least one application.
 * Used in the marketplace
 * 
 * @author p.campanella
 *
 */
public class PublisherFilterViewModel {
	private String publisher;
	private String nickName;
	private int appCount;
	
	public String getPublisher() {
		return publisher;
	}
	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}
	public int getAppCount() {
		return appCount;
	}
	public void setAppCount(int appCount) {
		this.appCount = appCount;
	}
	public String getNickName() {
		return nickName;
	}
	public void setNickName(String nickName) {
		this.nickName = nickName;
	}
}
