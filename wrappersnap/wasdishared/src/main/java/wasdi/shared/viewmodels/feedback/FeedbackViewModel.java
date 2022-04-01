package wasdi.shared.viewmodels.feedback;

import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * View model class to pass data from Feedback to UI 
 * aka Styles
 * @author PetruPetrescu on 15/03/2022
 *
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FeedbackViewModel {

	private String title;
	private String message;

}
