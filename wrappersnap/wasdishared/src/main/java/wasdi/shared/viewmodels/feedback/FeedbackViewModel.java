package wasdi.shared.viewmodels.feedback;

/**
 * View model class to pass data from Feedback to UI 
 * aka Styles
 * @author PetruPetrescu on 15/03/2022
 *
 */

public class FeedbackViewModel {

	private String title;
	private String message;
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}

}
